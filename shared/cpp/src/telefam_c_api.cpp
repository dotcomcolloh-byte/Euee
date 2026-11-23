#include "telefam_c_api.h"

#include <cstdlib>
#include <cstring>
#include <mutex>
#include <string>
#include <vector>

#include "telefam_core_engine.hpp"

struct TelefamEngineHandle {
    std::mutex mutex;
    telefam::core::TelefamCoreEngine engine;
};

namespace {

std::string safe_str(const char* s) {
    return s ? std::string(s) : std::string();
}

std::vector<uint8_t> safe_bytes(const uint8_t* ptr, size_t len) {
    if (!ptr || len == 0) return {};
    return std::vector<uint8_t>(ptr, ptr + len);
}

const char* dup_cstr(const std::string& s) {
    char* out = static_cast<char*>(std::malloc(s.size() + 1));
    if (!out) return nullptr;
    if (!s.empty()) {
        std::memcpy(out, s.data(), s.size());
    }
    out[s.size()] = '\0';
    return out;
}

}  // namespace

extern "C" {

TelefamEngineRef telefam_engine_create(void) {
    return new TelefamEngineHandle();
}

void telefam_engine_destroy(TelefamEngineRef engine) {
    delete engine;
}

void telefam_free_string(const char* str) {
    std::free(const_cast<char*>(str));
}

const char* telefam_init(
    TelefamEngineRef engine,
    const char* base_url,
    bool is_debug_build,
    const char* hardware_name,
    const char* device_id_hash,
    const char* device_public_key_b64,
    const char* runtime_cert_sha256,
    const char* expected_release_cert_sha256,
    const uint8_t* master_key,
    size_t master_key_len,
    const char* sealed_vault_b64,
    int64_t now_ms) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.initialize(
        safe_str(base_url),
        is_debug_build,
        safe_str(hardware_name),
        safe_str(device_id_hash),
        safe_str(device_public_key_b64),
        safe_str(runtime_cert_sha256),
        safe_str(expected_release_cert_sha256),
        safe_bytes(master_key, master_key_len),
        safe_str(sealed_vault_b64),
        now_ms);
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_set_email(TelefamEngineRef engine, const char* email) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.set_email(safe_str(email));
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_set_otp(TelefamEngineRef engine, const char* otp) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.set_otp(safe_str(otp));
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_go_back_to_email(TelefamEngineRef engine) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.go_back_to_email();
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_update_profile_field(
    TelefamEngineRef engine,
    const char* field,
    const char* value,
    int32_t cur_year,
    int32_t cur_month,
    int32_t cur_day) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.update_profile_field(safe_str(field), safe_str(value), cur_year, cur_month, cur_day);
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_set_home_tab(TelefamEngineRef engine, const char* tab) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.set_home_tab(safe_str(tab));
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_set_profile_content_tab(TelefamEngineRef engine, const char* tab) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.set_profile_content_tab(safe_str(tab));
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_set_contacts_tab(TelefamEngineRef engine, const char* tab) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.set_contacts_tab(safe_str(tab));
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_set_contacts_search(TelefamEngineRef engine, const char* query) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.set_contacts_search(safe_str(query));
    return dup_cstr(engine->engine.get_ui_state_json());
}

bool telefam_check_and_record_follow_rate(TelefamEngineRef engine, int64_t now_ms) {
    if (!engine) return false;
    std::lock_guard<std::mutex> lock(engine->mutex);
    return engine->engine.check_and_record_follow_rate(now_ms);
}

bool telefam_apply_local_profile_edit(
    TelefamEngineRef engine,
    const char* edit_json,
    int32_t cur_year,
    int32_t cur_month,
    int32_t cur_day,
    int64_t now_ms) {
    if (!engine) return false;
    std::lock_guard<std::mutex> lock(engine->mutex);
    return engine->engine.apply_local_profile_edit_json(safe_str(edit_json), cur_year, cur_month, cur_day, now_ms);
}

const char* telefam_apply_cached_owner_profile(
    TelefamEngineRef engine,
    const char* cached_json,
    int64_t now_ms) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.apply_cached_owner_profile_json(safe_str(cached_json), now_ms);
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_serialize_owner_profile(TelefamEngineRef engine) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.serialize_owner_profile_json());
}

const char* telefam_tick(TelefamEngineRef engine, int64_t now_ms) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.tick(now_ms);
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_cycle_theme(TelefamEngineRef engine) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.cycle_theme_mode();
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_coming_soon(TelefamEngineRef engine, const char* feature_label) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.set_coming_soon_notice(safe_str(feature_label));
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_set_error(TelefamEngineRef engine, const char* raw_error) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.set_error(safe_str(raw_error));
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_clear_error(TelefamEngineRef engine) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.clear_error();
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_clear_notice(TelefamEngineRef engine) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.clear_notice();
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_logout(TelefamEngineRef engine, int64_t now_ms) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.logout(now_ms);
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_prepare_send_otp(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_send_otp(is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_verify_otp(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_verify_otp(is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_google_auth(
    TelefamEngineRef engine,
    const char* id_token,
    const char* nonce,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_google_auth(
        safe_str(id_token), safe_str(nonce), is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_apple_auth(
    TelefamEngineRef engine,
    const char* identity_token,
    const char* authorization_code,
    const char* nonce,
    const char* full_name,
    const char* email_hint,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_apple_auth(
        safe_str(identity_token),
        safe_str(authorization_code),
        safe_str(nonce),
        safe_str(full_name),
        safe_str(email_hint),
        is_online,
        now_ms,
        safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_check_username(
    TelefamEngineRef engine,
    const char* username,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_check_username(
        safe_str(username), is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_upload_photo(
    TelefamEngineRef engine,
    const char* mime_type,
    int64_t byte_size,
    double crop_x,
    double crop_y,
    double crop_w,
    double crop_h,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_upload_photo(
        safe_str(mime_type), byte_size, crop_x, crop_y, crop_w, crop_h,
        is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_submit_profile(
    TelefamEngineRef engine,
    int32_t cur_year,
    int32_t cur_month,
    int32_t cur_day,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_submit_profile(
        cur_year, cur_month, cur_day, is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_fetch_owner_profile(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_fetch_owner_profile(
        is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_sync_edited_profile(
    TelefamEngineRef engine,
    const char* profile_json,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_sync_edited_profile(
        safe_str(profile_json), is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_upload_background_photo(
    TelefamEngineRef engine,
    const char* mime_type,
    int64_t byte_size,
    double crop_x,
    double crop_y,
    double crop_w,
    double crop_h,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_upload_background_photo(
        safe_str(mime_type), byte_size, crop_x, crop_y, crop_w, crop_h,
        is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_delete_background_photo(
    TelefamEngineRef engine,
    const char* photo_id,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_delete_background_photo(
        safe_str(photo_id), is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_fetch_contacts(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_fetch_contacts(
        is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_follow_action(
    TelefamEngineRef engine,
    const char* target_user_id,
    bool follow,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_follow_action(
        safe_str(target_user_id), follow, is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_refresh_token(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_refresh_token(
        is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_prepare_logout(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len) {
    if (!engine) return dup_cstr("{\"action\":\"NONE\"}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.prepare_logout(
        is_online, now_ms, safe_bytes(random_16, random_16_len)));
}

const char* telefam_on_http_result(
    TelefamEngineRef engine,
    const char* request_id,
    int32_t status_code,
    const char* response_body,
    bool network_error,
    bool is_online,
    int64_t now_ms,
    const uint8_t* vault_nonce_12,
    size_t vault_nonce_12_len) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    engine->engine.on_http_result(
        safe_str(request_id),
        status_code,
        safe_str(response_body),
        network_error,
        is_online,
        now_ms,
        safe_bytes(vault_nonce_12, vault_nonce_12_len));
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_get_ui_state(TelefamEngineRef engine) {
    if (!engine) return dup_cstr("{}");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.get_ui_state_json());
}

const char* telefam_get_countries(TelefamEngineRef engine) {
    if (!engine) return dup_cstr("[]");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.get_countries_json());
}

const char* telefam_filter_contacts(TelefamEngineRef engine, const char* contacts_array_json) {
    if (!engine) return dup_cstr("[]");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.filter_contacts_json(safe_str(contacts_array_json)));
}

const char* telefam_parse_contacts_sync(TelefamEngineRef engine, const char* server_response_json) {
    if (!engine) return dup_cstr("[]");
    std::lock_guard<std::mutex> lock(engine->mutex);
    return dup_cstr(engine->engine.parse_contacts_sync_json(safe_str(server_response_json)));
}

}  // extern "C"
