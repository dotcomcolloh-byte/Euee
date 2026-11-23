#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace telefam::crypto {

void secure_zero(void* ptr, size_t len);
bool constant_time_eq(const uint8_t* a, const uint8_t* b, size_t len);

std::array<uint8_t, 32> sha256(const uint8_t* data, size_t len);
std::array<uint8_t, 32> sha256_str(const std::string& s);
std::string to_hex(const uint8_t* data, size_t len);
bool from_hex(const std::string& hex, std::vector<uint8_t>& out);

std::array<uint8_t, 32> hmac_sha256(
    const uint8_t* key, size_t key_len,
    const uint8_t* data, size_t data_len);

std::vector<uint8_t> hkdf_sha256(
    const uint8_t* ikm, size_t ikm_len,
    const uint8_t* salt, size_t salt_len,
    const uint8_t* info, size_t info_len,
    size_t out_len);

bool chacha20_poly1305_encrypt(
    const std::array<uint8_t, 32>& key,
    const std::array<uint8_t, 12>& nonce,
    const uint8_t* aad, size_t aad_len,
    const uint8_t* plaintext, size_t pt_len,
    std::vector<uint8_t>& ciphertext_out,
    std::array<uint8_t, 16>& tag_out);

bool chacha20_poly1305_decrypt(
    const std::array<uint8_t, 32>& key,
    const std::array<uint8_t, 12>& nonce,
    const uint8_t* aad, size_t aad_len,
    const uint8_t* ciphertext, size_t ct_len,
    const std::array<uint8_t, 16>& tag,
    std::vector<uint8_t>& plaintext_out);

std::string base64_encode(const uint8_t* data, size_t len);
bool base64_decode(const std::string& input, std::vector<uint8_t>& out);
std::string base64url_encode(const uint8_t* data, size_t len);

std::array<uint8_t, 32> derive_device_subkey(
    const std::vector<uint8_t>& master_key,
    const std::string& device_id_hash,
    const std::string& purpose);

bool seal_device_vault(
    const std::vector<uint8_t>& master_key,
    const std::string& device_id_hash,
    const std::string& hardware_name,
    const std::string& plaintext_json,
    const std::array<uint8_t, 12>& random_nonce,
    std::string& sealed_b64_out);

bool unseal_device_vault(
    const std::vector<uint8_t>& master_key,
    const std::string& device_id_hash,
    const std::string& hardware_name,
    const std::string& sealed_b64,
    std::string& plaintext_json_out);

std::string sign_device_request(
    const std::vector<uint8_t>& master_key,
    const std::string& device_id_hash,
    const std::string& method,
    const std::string& path,
    int64_t timestamp_ms,
    const std::string& nonce_b64,
    const std::string& body);

}  // namespace telefam::crypto
