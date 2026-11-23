#include "crypto_engine.hpp"

#include <algorithm>
#include <cstring>

namespace telefam::crypto {

void secure_zero(void* ptr, size_t len) {
    if (!ptr || len == 0) return;
    volatile uint8_t* p = reinterpret_cast<volatile uint8_t*>(ptr);
    while (len--) {
        *p++ = 0;
    }
}

bool constant_time_eq(const uint8_t* a, const uint8_t* b, size_t len) {
    if (!a || !b) return false;
    uint8_t diff = 0;
    for (size_t i = 0; i < len; ++i) {
        diff |= static_cast<uint8_t>(a[i] ^ b[i]);
    }
    return diff == 0;
}

namespace {

inline uint32_t rotr32(uint32_t x, int n) {
    return (x >> n) | (x << (32 - n));
}

inline uint32_t rotl32(uint32_t x, int n) {
    return (x << n) | (x >> (32 - n));
}

inline uint32_t load_be32(const uint8_t* p) {
    return (static_cast<uint32_t>(p[0]) << 24) |
           (static_cast<uint32_t>(p[1]) << 16) |
           (static_cast<uint32_t>(p[2]) << 8) |
           static_cast<uint32_t>(p[3]);
}

inline void store_be32(uint8_t* p, uint32_t v) {
    p[0] = static_cast<uint8_t>((v >> 24) & 0xFF);
    p[1] = static_cast<uint8_t>((v >> 16) & 0xFF);
    p[2] = static_cast<uint8_t>((v >> 8) & 0xFF);
    p[3] = static_cast<uint8_t>(v & 0xFF);
}

inline uint32_t load_le32(const uint8_t* p) {
    return static_cast<uint32_t>(p[0]) |
           (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) |
           (static_cast<uint32_t>(p[3]) << 24);
}

inline void store_le32(uint8_t* p, uint32_t v) {
    p[0] = static_cast<uint8_t>(v & 0xFF);
    p[1] = static_cast<uint8_t>((v >> 8) & 0xFF);
    p[2] = static_cast<uint8_t>((v >> 16) & 0xFF);
    p[3] = static_cast<uint8_t>((v >> 24) & 0xFF);
}

inline void store_le64(uint8_t* p, uint64_t v) {
    for (int i = 0; i < 8; ++i) {
        p[i] = static_cast<uint8_t>((v >> (i * 8)) & 0xFF);
    }
}

constexpr uint32_t K256[64] = {
    0x428a2f98u, 0x71374491u, 0xb5c0fbcfu, 0xe9b5dba5u,
    0x3956c25bu, 0x59f111f1u, 0x923f82a4u, 0xab1c5ed5u,
    0xd807aa98u, 0x12835b01u, 0x243185beu, 0x550c7dc3u,
    0x72be5d74u, 0x80deb1feu, 0x9bdc06a7u, 0xc19bf174u,
    0xe49b69c1u, 0xefbe4786u, 0x0fc19dc6u, 0x240ca1ccu,
    0x2de92c6fu, 0x4a7484aau, 0x5cb0a9dcu, 0x76f988dau,
    0x983e5152u, 0xa831c66du, 0xb00327c8u, 0xbf597fc7u,
    0xc6e00bf3u, 0xd5a79147u, 0x06ca6351u, 0x14292967u,
    0x27b70a85u, 0x2e1b2138u, 0x4d2c6dfcu, 0x53380d13u,
    0x650a7354u, 0x766a0abbu, 0x81c2c92eu, 0x92722c85u,
    0xa2bfe8a1u, 0xa81a664bu, 0xc24b8b70u, 0xc76c51a3u,
    0xd192e819u, 0xd6990624u, 0xf40e3585u, 0x106aa070u,
    0x19a4c116u, 0x1e376c08u, 0x2748774cu, 0x34b0bcb5u,
    0x391c0cb3u, 0x4ed8aa4au, 0x5b9cca4fu, 0x682e6ff3u,
    0x748f82eeu, 0x78a5636fu, 0x84c87814u, 0x8cc70208u,
    0x90befffau, 0xa4506cebu, 0xbef9a3f7u, 0xc67178f2u
};

void sha256_transform(uint32_t state[8], const uint8_t block[64]) {
    uint32_t w[64];
    for (int i = 0; i < 16; ++i) {
        w[i] = load_be32(block + i * 4);
    }
    for (int i = 16; i < 64; ++i) {
        uint32_t s0 = rotr32(w[i - 15], 7) ^ rotr32(w[i - 15], 18) ^ (w[i - 15] >> 3);
        uint32_t s1 = rotr32(w[i - 2], 17) ^ rotr32(w[i - 2], 19) ^ (w[i - 2] >> 10);
        w[i] = w[i - 16] + s0 + w[i - 7] + s1;
    }

    uint32_t a = state[0], b = state[1], c = state[2], d = state[3];
    uint32_t e = state[4], f = state[5], g = state[6], h = state[7];

    for (int i = 0; i < 64; ++i) {
        uint32_t S1 = rotr32(e, 6) ^ rotr32(e, 11) ^ rotr32(e, 25);
        uint32_t ch = (e & f) ^ ((~e) & g);
        uint32_t temp1 = h + S1 + ch + K256[i] + w[i];
        uint32_t S0 = rotr32(a, 2) ^ rotr32(a, 13) ^ rotr32(a, 22);
        uint32_t maj = (a & b) ^ (a & c) ^ (b & c);
        uint32_t temp2 = S0 + maj;

        h = g;
        g = f;
        f = e;
        e = d + temp1;
        d = c;
        c = b;
        b = a;
        a = temp1 + temp2;
    }

    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;
    state[4] += e;
    state[5] += f;
    state[6] += g;
    state[7] += h;
    secure_zero(w, sizeof(w));
}

inline void quarter_round(uint32_t& a, uint32_t& b, uint32_t& c, uint32_t& d) {
    a += b; d ^= a; d = rotl32(d, 16);
    c += d; b ^= c; b = rotl32(b, 12);
    a += b; d ^= a; d = rotl32(d, 8);
    c += d; b ^= c; b = rotl32(b, 7);
}

void chacha20_block(
    const std::array<uint8_t, 32>& key,
    uint32_t counter,
    const std::array<uint8_t, 12>& nonce,
    uint8_t out[64]) {
    uint32_t st[16] = {
        0x61707865u, 0x3320646eu, 0x79622d32u, 0x6b206574u,
        load_le32(key.data() + 0),
        load_le32(key.data() + 4),
        load_le32(key.data() + 8),
        load_le32(key.data() + 12),
        load_le32(key.data() + 16),
        load_le32(key.data() + 20),
        load_le32(key.data() + 24),
        load_le32(key.data() + 28),
        counter,
        load_le32(nonce.data() + 0),
        load_le32(nonce.data() + 4),
        load_le32(nonce.data() + 8)
    };
    uint32_t x[16];
    std::memcpy(x, st, sizeof(st));

    for (int i = 0; i < 10; ++i) {
        quarter_round(x[0], x[4], x[8], x[12]);
        quarter_round(x[1], x[5], x[9], x[13]);
        quarter_round(x[2], x[6], x[10], x[14]);
        quarter_round(x[3], x[7], x[11], x[15]);
        quarter_round(x[0], x[5], x[10], x[15]);
        quarter_round(x[1], x[6], x[11], x[12]);
        quarter_round(x[2], x[7], x[8], x[13]);
        quarter_round(x[3], x[4], x[9], x[14]);
    }
    for (int i = 0; i < 16; ++i) {
        store_le32(out + i * 4, x[i] + st[i]);
    }
    secure_zero(st, sizeof(st));
    secure_zero(x, sizeof(x));
}

void chacha20_xor(
    const std::array<uint8_t, 32>& key,
    uint32_t initial_counter,
    const std::array<uint8_t, 12>& nonce,
    const uint8_t* in,
    size_t len,
    uint8_t* out) {
    uint8_t keystream[64];
    size_t offset = 0;
    uint32_t counter = initial_counter;
    while (offset < len) {
        chacha20_block(key, counter++, nonce, keystream);
        const size_t chunk = std::min<size_t>(64, len - offset);
        for (size_t i = 0; i < chunk; ++i) {
            out[offset + i] = in[offset + i] ^ keystream[i];
        }
        offset += chunk;
    }
    secure_zero(keystream, sizeof(keystream));
}

struct Poly1305 {
    uint32_t r[5]{};
    uint32_t h[5]{};
    uint32_t pad[4]{};

    void init(const uint8_t key32[32]) {
        uint32_t t0 = load_le32(key32 + 0) & 0x0FFFFFFFu;
        uint32_t t1 = load_le32(key32 + 4) & 0x0FFFFFFCu;
        uint32_t t2 = load_le32(key32 + 8) & 0x0FFFFFFCu;
        uint32_t t3 = load_le32(key32 + 12) & 0x0FFFFFFCu;

        r[0] = t0 & 0x3FFFFFFu;
        r[1] = ((t0 >> 26) | (t1 << 6)) & 0x3FFFFFFu;
        r[2] = ((t1 >> 20) | (t2 << 12)) & 0x3FFFFFFu;
        r[3] = ((t2 >> 14) | (t3 << 18)) & 0x3FFFFFFu;
        r[4] = (t3 >> 8) & 0x3FFFFFFu;

        for (int i = 0; i < 5; ++i) h[i] = 0;
        for (int i = 0; i < 4; ++i) pad[i] = load_le32(key32 + 16 + i * 4);
    }

    void block(const uint8_t* m, size_t bytes, bool final_partial) {
        uint8_t buf[17]{};
        std::memcpy(buf, m, bytes);
        if (!final_partial) {
            buf[16] = 1;
        } else {
            buf[bytes] = 1;
        }
        uint32_t t0 = load_le32(buf + 0);
        uint32_t t1 = load_le32(buf + 4);
        uint32_t t2 = load_le32(buf + 8);
        uint32_t t3 = load_le32(buf + 12);

        h[0] += t0 & 0x3FFFFFFu;
        h[1] += ((t0 >> 26) | (t1 << 6)) & 0x3FFFFFFu;
        h[2] += ((t1 >> 20) | (t2 << 12)) & 0x3FFFFFFu;
        h[3] += ((t2 >> 14) | (t3 << 18)) & 0x3FFFFFFu;
        h[4] += (t3 >> 8) | (static_cast<uint32_t>(buf[16]) << 24);

        const uint64_t r0 = r[0], r1 = r[1], r2 = r[2], r3 = r[3], r4 = r[4];
        const uint64_t s1 = r1 * 5, s2 = r2 * 5, s3 = r3 * 5, s4 = r4 * 5;

        uint64_t d0 = h[0] * r0 + h[1] * s4 + h[2] * s3 + h[3] * s2 + h[4] * s1;
        uint64_t d1 = h[0] * r1 + h[1] * r0 + h[2] * s4 + h[3] * s3 + h[4] * s2;
        uint64_t d2 = h[0] * r2 + h[1] * r1 + h[2] * r0 + h[3] * s4 + h[4] * s3;
        uint64_t d3 = h[0] * r3 + h[1] * r2 + h[2] * r1 + h[3] * r0 + h[4] * s4;
        uint64_t d4 = h[0] * r4 + h[1] * r3 + h[2] * r2 + h[3] * r1 + h[4] * r0;

        uint64_t c;
        c = d0 >> 26; h[0] = static_cast<uint32_t>(d0 & 0x3FFFFFFu); d1 += c;
        c = d1 >> 26; h[1] = static_cast<uint32_t>(d1 & 0x3FFFFFFu); d2 += c;
        c = d2 >> 26; h[2] = static_cast<uint32_t>(d2 & 0x3FFFFFFu); d3 += c;
        c = d3 >> 26; h[3] = static_cast<uint32_t>(d3 & 0x3FFFFFFu); d4 += c;
        c = d4 >> 26; h[4] = static_cast<uint32_t>(d4 & 0x3FFFFFFu);
        h[0] += static_cast<uint32_t>(c * 5);
        c = h[0] >> 26; h[0] &= 0x3FFFFFFu;
        h[1] += static_cast<uint32_t>(c);
        secure_zero(buf, sizeof(buf));
    }

    void update(const uint8_t* data, size_t len) {
        size_t off = 0;
        while (off + 16 <= len) {
            block(data + off, 16, false);
            off += 16;
        }
        if (off < len) {
            block(data + off, len - off, true);
        }
    }

    void finish(uint8_t tag[16]) {
        uint32_t c;
        c = h[1] >> 26; h[1] &= 0x3FFFFFFu; h[2] += c;
        c = h[2] >> 26; h[2] &= 0x3FFFFFFu; h[3] += c;
        c = h[3] >> 26; h[3] &= 0x3FFFFFFu; h[4] += c;
        c = h[4] >> 26; h[4] &= 0x3FFFFFFu; h[0] += c * 5;
        c = h[0] >> 26; h[0] &= 0x3FFFFFFu; h[1] += c;

        uint32_t g[5];
        g[0] = h[0] + 5; c = g[0] >> 26; g[0] &= 0x3FFFFFFu;
        g[1] = h[1] + c; c = g[1] >> 26; g[1] &= 0x3FFFFFFu;
        g[2] = h[2] + c; c = g[2] >> 26; g[2] &= 0x3FFFFFFu;
        g[3] = h[3] + c; c = g[3] >> 26; g[3] &= 0x3FFFFFFu;
        g[4] = h[4] + c - (1u << 26);

        uint32_t mask = (g[4] >> 31) - 1;
        for (int i = 0; i < 5; ++i) {
            h[i] = (h[i] & ~mask) | (g[i] & mask);
        }

        uint64_t f0 = (static_cast<uint64_t>(h[0]) | (static_cast<uint64_t>(h[1]) << 26)) + pad[0];
        uint64_t f1 = ((static_cast<uint64_t>(h[1]) >> 6) | (static_cast<uint64_t>(h[2]) << 20)) + pad[1] + (f0 >> 32);
        uint64_t f2 = ((static_cast<uint64_t>(h[2]) >> 12) | (static_cast<uint64_t>(h[3]) << 14)) + pad[2] + (f1 >> 32);
        uint64_t f3 = ((static_cast<uint64_t>(h[3]) >> 18) | (static_cast<uint64_t>(h[4]) << 8)) + pad[3] + (f2 >> 32);

        store_le32(tag + 0, static_cast<uint32_t>(f0));
        store_le32(tag + 4, static_cast<uint32_t>(f1));
        store_le32(tag + 8, static_cast<uint32_t>(f2));
        store_le32(tag + 12, static_cast<uint32_t>(f3));

        secure_zero(r, sizeof(r));
        secure_zero(h, sizeof(h));
        secure_zero(pad, sizeof(pad));
    }
};

std::array<uint8_t, 16> compute_poly_tag(
    const uint8_t poly_key[32],
    const uint8_t* aad, size_t aad_len,
    const uint8_t* ct, size_t ct_len) {
    Poly1305 poly;
    poly.init(poly_key);
    static const uint8_t zeros[16]{};

    if (aad && aad_len > 0) {
        poly.update(aad, aad_len);
        if (aad_len % 16 != 0) {
            poly.update(zeros, 16 - (aad_len % 16));
        }
    }
    if (ct && ct_len > 0) {
        poly.update(ct, ct_len);
        if (ct_len % 16 != 0) {
            poly.update(zeros, 16 - (ct_len % 16));
        }
    }
    uint8_t lens[16];
    store_le64(lens, static_cast<uint64_t>(aad_len));
    store_le64(lens + 8, static_cast<uint64_t>(ct_len));
    poly.update(lens, 16);

    std::array<uint8_t, 16> tag{};
    poly.finish(tag.data());
    return tag;
}

}  // namespace

std::array<uint8_t, 32> sha256(const uint8_t* data, size_t len) {
    uint32_t state[8] = {
        0x6a09e667u, 0xbb67ae85u, 0x3c6ef372u, 0xa54ff53au,
        0x510e527fu, 0x9b05688cu, 0x1f83d9abu, 0x5be0cd19u
    };

    uint8_t block[64]{};
    size_t offset = 0;
    while (offset + 64 <= len) {
        sha256_transform(state, data + offset);
        offset += 64;
    }

    size_t rem = len - offset;
    if (rem > 0 && data) {
        std::memcpy(block, data + offset, rem);
    }
    block[rem] = 0x80;
    if (rem >= 56) {
        sha256_transform(state, block);
        std::memset(block, 0, 64);
    }
    uint64_t bit_len = static_cast<uint64_t>(len) * 8ULL;
    for (int i = 0; i < 8; ++i) {
        block[56 + i] = static_cast<uint8_t>((bit_len >> ((7 - i) * 8)) & 0xFF);
    }
    sha256_transform(state, block);

    std::array<uint8_t, 32> out{};
    for (int i = 0; i < 8; ++i) {
        store_be32(out.data() + i * 4, state[i]);
    }
    secure_zero(block, sizeof(block));
    secure_zero(state, sizeof(state));
    return out;
}

std::array<uint8_t, 32> sha256_str(const std::string& s) {
    return sha256(reinterpret_cast<const uint8_t*>(s.data()), s.size());
}

std::string to_hex(const uint8_t* data, size_t len) {
    static constexpr char kHex[] = "0123456789abcdef";
    std::string out;
    out.reserve(len * 2);
    for (size_t i = 0; i < len; ++i) {
        out.push_back(kHex[(data[i] >> 4) & 0xF]);
        out.push_back(kHex[data[i] & 0xF]);
    }
    return out;
}

bool from_hex(const std::string& hex, std::vector<uint8_t>& out) {
    if (hex.size() % 2 != 0) return false;
    out.clear();
    out.reserve(hex.size() / 2);
    auto nibble = [](char c) -> int {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    };
    for (size_t i = 0; i < hex.size(); i += 2) {
        int hi = nibble(hex[i]);
        int lo = nibble(hex[i + 1]);
        if (hi < 0 || lo < 0) return false;
        out.push_back(static_cast<uint8_t>((hi << 4) | lo));
    }
    return true;
}

std::array<uint8_t, 32> hmac_sha256(
    const uint8_t* key, size_t key_len,
    const uint8_t* data, size_t data_len) {
    uint8_t k_pad[64]{};
    if (key_len > 64) {
        auto hashed = sha256(key, key_len);
        std::memcpy(k_pad, hashed.data(), 32);
        secure_zero(hashed.data(), hashed.size());
    } else if (key && key_len > 0) {
        std::memcpy(k_pad, key, key_len);
    }

    std::vector<uint8_t> inner(64 + data_len);
    for (int i = 0; i < 64; ++i) inner[i] = k_pad[i] ^ 0x36u;
    if (data && data_len > 0) {
        std::memcpy(inner.data() + 64, data, data_len);
    }
    auto inner_hash = sha256(inner.data(), inner.size());
    secure_zero(inner.data(), inner.size());

    uint8_t outer[64 + 32];
    for (int i = 0; i < 64; ++i) outer[i] = k_pad[i] ^ 0x5cu;
    std::memcpy(outer + 64, inner_hash.data(), 32);
    auto mac = sha256(outer, sizeof(outer));

    secure_zero(k_pad, sizeof(k_pad));
    secure_zero(inner_hash.data(), inner_hash.size());
    secure_zero(outer, sizeof(outer));
    return mac;
}

std::vector<uint8_t> hkdf_sha256(
    const uint8_t* ikm, size_t ikm_len,
    const uint8_t* salt, size_t salt_len,
    const uint8_t* info, size_t info_len,
    size_t out_len) {
    uint8_t zero_salt[32]{};
    const uint8_t* actual_salt = (salt && salt_len > 0) ? salt : zero_salt;
    const size_t actual_salt_len = (salt && salt_len > 0) ? salt_len : 32;

    auto prk = hmac_sha256(actual_salt, actual_salt_len, ikm, ikm_len);

    std::vector<uint8_t> okm;
    okm.reserve(out_len);
    std::vector<uint8_t> t_prev;
    uint8_t counter = 1;
    while (okm.size() < out_len) {
        std::vector<uint8_t> buf;
        buf.reserve(t_prev.size() + info_len + 1);
        buf.insert(buf.end(), t_prev.begin(), t_prev.end());
        if (info && info_len > 0) {
            buf.insert(buf.end(), info, info + info_len);
        }
        buf.push_back(counter++);
        auto t_curr = hmac_sha256(prk.data(), prk.size(), buf.data(), buf.size());
        secure_zero(buf.data(), buf.size());
        const size_t copy_len = std::min<size_t>(32, out_len - okm.size());
        okm.insert(okm.end(), t_curr.begin(), t_curr.begin() + copy_len);
        t_prev.assign(t_curr.begin(), t_curr.end());
        secure_zero(t_curr.data(), t_curr.size());
    }
    secure_zero(prk.data(), prk.size());
    if (!t_prev.empty()) secure_zero(t_prev.data(), t_prev.size());
    return okm;
}

bool chacha20_poly1305_encrypt(
    const std::array<uint8_t, 32>& key,
    const std::array<uint8_t, 12>& nonce,
    const uint8_t* aad, size_t aad_len,
    const uint8_t* plaintext, size_t pt_len,
    std::vector<uint8_t>& ciphertext_out,
    std::array<uint8_t, 16>& tag_out) {
    uint8_t otk[64];
    chacha20_block(key, 0, nonce, otk);

    ciphertext_out.resize(pt_len);
    if (pt_len > 0 && plaintext) {
        chacha20_xor(key, 1, nonce, plaintext, pt_len, ciphertext_out.data());
    }
    tag_out = compute_poly_tag(otk, aad, aad_len, ciphertext_out.data(), ciphertext_out.size());
    secure_zero(otk, sizeof(otk));
    return true;
}

bool chacha20_poly1305_decrypt(
    const std::array<uint8_t, 32>& key,
    const std::array<uint8_t, 12>& nonce,
    const uint8_t* aad, size_t aad_len,
    const uint8_t* ciphertext, size_t ct_len,
    const std::array<uint8_t, 16>& tag,
    std::vector<uint8_t>& plaintext_out) {
    uint8_t otk[64];
    chacha20_block(key, 0, nonce, otk);

    auto expected_tag = compute_poly_tag(otk, aad, aad_len, ciphertext, ct_len);
    secure_zero(otk, sizeof(otk));

    if (!constant_time_eq(expected_tag.data(), tag.data(), 16)) {
        secure_zero(expected_tag.data(), expected_tag.size());
        return false;
    }
    secure_zero(expected_tag.data(), expected_tag.size());

    plaintext_out.resize(ct_len);
    if (ct_len > 0 && ciphertext) {
        chacha20_xor(key, 1, nonce, ciphertext, ct_len, plaintext_out.data());
    }
    return true;
}

std::string base64_encode(const uint8_t* data, size_t len) {
    static constexpr char kB64[] =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string out;
    out.reserve(((len + 2) / 3) * 4);
    for (size_t i = 0; i < len; i += 3) {
        uint32_t b = static_cast<uint32_t>(data[i]) << 16;
        if (i + 1 < len) b |= static_cast<uint32_t>(data[i + 1]) << 8;
        if (i + 2 < len) b |= static_cast<uint32_t>(data[i + 2]);
        out.push_back(kB64[(b >> 18) & 0x3F]);
        out.push_back(kB64[(b >> 12) & 0x3F]);
        out.push_back((i + 1 < len) ? kB64[(b >> 6) & 0x3F] : '=');
        out.push_back((i + 2 < len) ? kB64[b & 0x3F] : '=');
    }
    return out;
}

bool base64_decode(const std::string& input, std::vector<uint8_t>& out) {
    auto val = [](unsigned char c) -> int {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+' || c == '-') return 62;
        if (c == '/' || c == '_') return 63;
        return -1;
    };
    out.clear();
    uint32_t accum = 0;
    int bits = 0;
    for (unsigned char c : input) {
        if (c == '=') break;
        if (c == '\r' || c == '\n' || c == ' ') continue;
        int v = val(c);
        if (v < 0) return false;
        accum = (accum << 6) | static_cast<uint32_t>(v);
        bits += 6;
        if (bits >= 8) {
            bits -= 8;
            out.push_back(static_cast<uint8_t>((accum >> bits) & 0xFF));
        }
    }
    return true;
}

std::string base64url_encode(const uint8_t* data, size_t len) {
    std::string b64 = base64_encode(data, len);
    std::string out;
    out.reserve(b64.size());
    for (char c : b64) {
        if (c == '+') out.push_back('-');
        else if (c == '/') out.push_back('_');
        else if (c != '=') out.push_back(c);
    }
    return out;
}

std::array<uint8_t, 32> derive_device_subkey(
    const std::vector<uint8_t>& master_key,
    const std::string& device_id_hash,
    const std::string& purpose) {
    std::string info = "telefam:" + purpose + ":" + device_id_hash;
    auto okm = hkdf_sha256(
        master_key.data(), master_key.size(),
        reinterpret_cast<const uint8_t*>(device_id_hash.data()), device_id_hash.size(),
        reinterpret_cast<const uint8_t*>(info.data()), info.size(),
        32);
    std::array<uint8_t, 32> out{};
    std::memcpy(out.data(), okm.data(), 32);
    secure_zero(okm.data(), okm.size());
    return out;
}

bool seal_device_vault(
    const std::vector<uint8_t>& master_key,
    const std::string& device_id_hash,
    const std::string& hardware_name,
    const std::string& plaintext_json,
    const std::array<uint8_t, 12>& random_nonce,
    std::string& sealed_b64_out) {
    if (master_key.size() < 16 || device_id_hash.empty()) return false;
    auto vault_key = derive_device_subkey(master_key, device_id_hash, "vault-aead-v1");
    std::string aad = device_id_hash + "|" + hardware_name + "|TFV1";

    std::vector<uint8_t> ct;
    std::array<uint8_t, 16> tag{};
    chacha20_poly1305_encrypt(
        vault_key, random_nonce,
        reinterpret_cast<const uint8_t*>(aad.data()), aad.size(),
        reinterpret_cast<const uint8_t*>(plaintext_json.data()), plaintext_json.size(),
        ct, tag);
    secure_zero(vault_key.data(), vault_key.size());

    std::vector<uint8_t> envelope;
    envelope.reserve(4 + 12 + ct.size() + 16);
    envelope.push_back('T');
    envelope.push_back('F');
    envelope.push_back('V');
    envelope.push_back('1');
    envelope.insert(envelope.end(), random_nonce.begin(), random_nonce.end());
    envelope.insert(envelope.end(), ct.begin(), ct.end());
    envelope.insert(envelope.end(), tag.begin(), tag.end());
    sealed_b64_out = base64_encode(envelope.data(), envelope.size());
    secure_zero(envelope.data(), envelope.size());
    return true;
}

bool unseal_device_vault(
    const std::vector<uint8_t>& master_key,
    const std::string& device_id_hash,
    const std::string& hardware_name,
    const std::string& sealed_b64,
    std::string& plaintext_json_out) {
    if (master_key.size() < 16 || device_id_hash.empty() || sealed_b64.empty()) return false;
    std::vector<uint8_t> envelope;
    if (!base64_decode(sealed_b64, envelope)) return false;
    if (envelope.size() < 4 + 12 + 16) return false;
    if (envelope[0] != 'T' || envelope[1] != 'F' || envelope[2] != 'V' || envelope[3] != '1') {
        return false;
    }

    std::array<uint8_t, 12> nonce{};
    std::memcpy(nonce.data(), envelope.data() + 4, 12);
    const size_t ct_len = envelope.size() - 4 - 12 - 16;
    const uint8_t* ct_ptr = envelope.data() + 16;
    std::array<uint8_t, 16> tag{};
    std::memcpy(tag.data(), envelope.data() + 16 + ct_len, 16);

    auto vault_key = derive_device_subkey(master_key, device_id_hash, "vault-aead-v1");
    std::string aad = device_id_hash + "|" + hardware_name + "|TFV1";

    std::vector<uint8_t> pt;
    bool ok = chacha20_poly1305_decrypt(
        vault_key, nonce,
        reinterpret_cast<const uint8_t*>(aad.data()), aad.size(),
        ct_ptr, ct_len, tag, pt);
    secure_zero(vault_key.data(), vault_key.size());
    if (!ok) return false;

    plaintext_json_out.assign(reinterpret_cast<const char*>(pt.data()), pt.size());
    secure_zero(pt.data(), pt.size());
    return true;
}

std::string sign_device_request(
    const std::vector<uint8_t>& master_key,
    const std::string& device_id_hash,
    const std::string& method,
    const std::string& path,
    int64_t timestamp_ms,
    const std::string& nonce_b64,
    const std::string& body) {
    auto signing_key = derive_device_subkey(master_key, device_id_hash, "req-hmac-v1");
    auto body_hash = sha256_str(body);
    std::string canonical = method + "\n" + path + "\n" + std::to_string(timestamp_ms) +
                            "\n" + nonce_b64 + "\n" + to_hex(body_hash.data(), body_hash.size());
    auto mac = hmac_sha256(
        signing_key.data(), signing_key.size(),
        reinterpret_cast<const uint8_t*>(canonical.data()), canonical.size());
    secure_zero(signing_key.data(), signing_key.size());
    return to_hex(mac.data(), mac.size());
}

}  // namespace telefam::crypto
