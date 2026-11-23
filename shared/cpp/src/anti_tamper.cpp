#include "anti_tamper.hpp"

#include <algorithm>
#include <cctype>
#include <fstream>
#include <sstream>

#if defined(_WIN32)
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <windows.h>
#include <sys/stat.h>
#elif defined(__APPLE__)
#include <TargetConditionals.h>
#include <sys/stat.h>
#include <sys/sysctl.h>
#include <unistd.h>
#else
#include <sys/stat.h>
#include <unistd.h>
#endif

#include "crypto_engine.hpp"
#include "string_vault.hpp"

namespace telefam::security {

namespace {

std::string to_lower_ascii(std::string s) {
    for (char& c : s) {
        c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
    }
    return s;
}

bool check_tracer_pid() {
#if defined(_WIN32)
    return ::IsDebuggerPresent() != FALSE;
#elif defined(__APPLE__)
    int mib[4] = {CTL_KERN, KERN_PROC, KERN_PROC_PID, ::getpid()};
    struct kinfo_proc info{};
    size_t size = sizeof(info);
    if (::sysctl(mib, 4, &info, &size, nullptr, 0) != 0) {
        return false;
    }
    return (info.kp_proc.p_flag & P_TRACED) != 0;
#else
    std::ifstream status_file(TELEFAM_OBF("/proc/self/status", 0x17));
    if (!status_file.is_open()) return false;
    const std::string needle = TELEFAM_OBF("TracerPid:", 0x23);
    std::string line;
    while (std::getline(status_file, line)) {
        if (line.compare(0, needle.size(), needle) == 0) {
            std::string rest = line.substr(needle.size());
            int pid = 0;
            try {
                pid = std::stoi(rest);
            } catch (...) {
                return false;
            }
            return pid > 0;
        }
    }
    return false;
#endif
}

uint32_t check_proc_maps() {
    uint32_t flags = INTEGRITY_OK;
#if defined(__linux__) || defined(__ANDROID__)
    std::ifstream maps_file(TELEFAM_OBF("/proc/self/maps", 0x31));
    if (!maps_file.is_open()) return flags;

    const std::string needles[] = {
        TELEFAM_OBF("frida", 0x41),
        TELEFAM_OBF("gadget", 0x42),
        TELEFAM_OBF("xposed", 0x43),
        TELEFAM_OBF("lsposed", 0x44),
        TELEFAM_OBF("edxposed", 0x45),
        TELEFAM_OBF("substrate", 0x46),
        TELEFAM_OBF("zygisk", 0x47),
    };
    const std::string self_lib = TELEFAM_OBF("libtelefam_core.so", 0x52);

    std::string line;
    while (std::getline(maps_file, line)) {
        const std::string lower = to_lower_ascii(line);
        for (const auto& n : needles) {
            if (lower.find(n) != std::string::npos) {
                flags |= FLAG_HOOK_FRAMEWORK;
                break;
            }
        }
        if (lower.find(self_lib) != std::string::npos &&
            lower.find("rwxp") != std::string::npos) {
            flags |= FLAG_RWX_TEXT_TAMPER;
        }
    }
#endif
    return flags;
}

bool check_su_binaries() {
#if defined(__ANDROID__)
    const std::string paths[] = {
        TELEFAM_OBF("/system/bin/su", 0x61),
        TELEFAM_OBF("/system/xbin/su", 0x62),
        TELEFAM_OBF("/sbin/su", 0x63),
        TELEFAM_OBF("/vendor/bin/su", 0x64),
    };
    struct stat st{};
    for (const auto& p : paths) {
        if (::stat(p.c_str(), &st) == 0) {
            return true;
        }
    }
#elif defined(__APPLE__) && TARGET_OS_IPHONE
    const std::string jailbreak_paths[] = {
        TELEFAM_OBF("/Applications/Cydia.app", 0x65),
        TELEFAM_OBF("/Library/MobileSubstrate/MobileSubstrate.dylib", 0x66),
        TELEFAM_OBF("/bin/bash", 0x67),
        TELEFAM_OBF("/usr/sbin/sshd", 0x68),
        TELEFAM_OBF("/etc/apt", 0x69),
    };
    struct stat st{};
    for (const auto& p : jailbreak_paths) {
        if (::stat(p.c_str(), &st) == 0) {
            return true;
        }
    }
#endif
    return false;
}

bool is_valid_sha256_hex(const std::string& hex) {
    if (hex.size() != 64) return false;
    return std::all_of(hex.begin(), hex.end(), [](unsigned char c) {
        return std::isxdigit(c) != 0;
    });
}

}  // namespace

IntegrityReport inspect_runtime_integrity(
    bool is_debug_build,
    const std::string& runtime_cert_sha256_hex,
    const std::string& expected_release_cert_sha256_hex) {
    IntegrityReport report;
    uint32_t flags = INTEGRITY_OK;

    if (check_tracer_pid()) {
        flags |= FLAG_DEBUGGER_TRACER;
    }

    flags |= check_proc_maps();

    if (check_su_binaries()) {
        flags |= FLAG_ROOT_BINARY;
    }

    const std::string norm_runtime = to_lower_ascii(runtime_cert_sha256_hex);
    const bool enforce_cert_signature =
#if defined(__ANDROID__)
        true;
#else
        !expected_release_cert_sha256_hex.empty();
#endif

    if (enforce_cert_signature) {
        if (!is_valid_sha256_hex(norm_runtime)) {
            flags |= FLAG_MISSING_SIGNATURE;
        } else if (!expected_release_cert_sha256_hex.empty()) {
            const std::string norm_expected = to_lower_ascii(expected_release_cert_sha256_hex);
            if (norm_runtime.size() != norm_expected.size() ||
                !crypto::constant_time_eq(
                    reinterpret_cast<const uint8_t*>(norm_runtime.data()),
                    reinterpret_cast<const uint8_t*>(norm_expected.data()),
                    norm_expected.size())) {
                flags |= FLAG_SIGNATURE_MISMATCH;
            }
        }
    }

    report.flags = flags;
    if (is_debug_build) {
        report.compromised = (flags & (FLAG_HOOK_FRAMEWORK | FLAG_RWX_TEXT_TAMPER)) != 0;
    } else {
        const uint32_t release_critical =
            FLAG_DEBUGGER_TRACER | FLAG_HOOK_FRAMEWORK | FLAG_SIGNATURE_MISMATCH |
            FLAG_MISSING_SIGNATURE | FLAG_RWX_TEXT_TAMPER;
        report.compromised = (flags & release_critical) != 0;
    }

    auto summary_hash = crypto::sha256_str(norm_runtime + ":" + std::to_string(flags));
    report.digest_summary = crypto::to_hex(summary_hash.data(), 16);
    return report;
}

}  // namespace telefam::security
