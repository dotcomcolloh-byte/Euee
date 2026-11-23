#pragma once

#include <cstdint>
#include <string>

namespace telefam::security {

enum IntegrityFlags : uint32_t {
    INTEGRITY_OK = 0,
    FLAG_DEBUGGER_TRACER = 1u << 0,
    FLAG_HOOK_FRAMEWORK = 1u << 1,
    FLAG_SIGNATURE_MISMATCH = 1u << 2,
    FLAG_MISSING_SIGNATURE = 1u << 3,
    FLAG_ROOT_BINARY = 1u << 4,
    FLAG_RWX_TEXT_TAMPER = 1u << 5,
};

struct IntegrityReport {
    bool compromised = false;
    uint32_t flags = INTEGRITY_OK;
    std::string digest_summary;
};

IntegrityReport inspect_runtime_integrity(
    bool is_debug_build,
    const std::string& runtime_cert_sha256_hex,
    const std::string& expected_release_cert_sha256_hex);

}  // namespace telefam::security
