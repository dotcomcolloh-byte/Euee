#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <string>

namespace telefam::vault {

template <std::size_t N, uint8_t Seed>
class ObfuscatedLiteral {
public:
    constexpr explicit ObfuscatedLiteral(const char (&str)[N]) : data_{} {
        for (std::size_t i = 0; i < N; ++i) {
            const uint8_t mask = static_cast<uint8_t>((Seed + i * 37u) ^ (i * 13u + 0x5Au));
            data_[i] = static_cast<uint8_t>(str[i]) ^ mask;
        }
    }

    std::string reveal() const {
        std::string out;
        if (N <= 1) return out;
        out.resize(N - 1);
        for (std::size_t i = 0; i < N - 1; ++i) {
            const uint8_t mask = static_cast<uint8_t>((Seed + i * 37u) ^ (i * 13u + 0x5Au));
            out[i] = static_cast<char>(data_[i] ^ mask);
        }
        return out;
    }

private:
    std::array<uint8_t, N> data_;
};

template <std::size_t N, uint8_t Seed>
constexpr auto make_obfuscated(const char (&str)[N]) {
    return ObfuscatedLiteral<N, Seed>(str);
}

#define TELEFAM_OBF(str, seed) \
    ([]() -> std::string { \
        static constexpr auto lit = ::telefam::vault::make_obfuscated<sizeof(str), (seed)>(str); \
        return lit.reveal(); \
    }())

}  // namespace telefam::vault
