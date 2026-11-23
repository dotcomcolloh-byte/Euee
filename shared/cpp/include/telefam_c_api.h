#ifndef TELEFAM_C_API_H
#define TELEFAM_C_API_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct TelefamEngineHandle* TelefamEngineRef;

TelefamEngineRef telefam_engine_create(void);
void telefam_engine_destroy(TelefamEngineRef engine);
void telefam_free_string(const char* str);

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
    int64_t now_ms);

const char* telefam_set_email(TelefamEngineRef engine, const char* email);
const char* telefam_set_otp(TelefamEngineRef engine, const char* otp);
const char* telefam_go_back_to_email(TelefamEngineRef engine);

const char* telefam_update_profile_field(
    TelefamEngineRef engine,
    const char* field,
    const char* value,
    int32_t cur_year,
    int32_t cur_month,
    int32_t cur_day);

const char* telefam_set_home_tab(TelefamEngineRef engine, const char* tab);
const char* telefam_set_profile_content_tab(TelefamEngineRef engine, const char* tab);
const char* telefam_set_contacts_tab(TelefamEngineRef engine, const char* tab);
const char* telefam_set_contacts_search(TelefamEngineRef engine, const char* query);
bool telefam_check_and_record_follow_rate(TelefamEngineRef engine, int64_t now_ms);

bool telefam_apply_local_profile_edit(
    TelefamEngineRef engine,
    const char* edit_json,
    int32_t cur_year,
    int32_t cur_month,
    int32_t cur_day,
    int64_t now_ms);

const char* telefam_apply_cached_owner_profile(
    TelefamEngineRef engine,
    const char* cached_json,
    int64_t now_ms);

const char* telefam_serialize_owner_profile(TelefamEngineRef engine);

const char* telefam_tick(TelefamEngineRef engine, int64_t now_ms);
const char* telefam_cycle_theme(TelefamEngineRef engine);
const char* telefam_coming_soon(TelefamEngineRef engine, const char* feature_label);
const char* telefam_set_error(TelefamEngineRef engine, const char* raw_error);
const char* telefam_clear_error(TelefamEngineRef engine);
const char* telefam_clear_notice(TelefamEngineRef engine);
const char* telefam_logout(TelefamEngineRef engine, int64_t now_ms);

const char* telefam_prepare_send_otp(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

const char* telefam_prepare_verify_otp(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

const char* telefam_prepare_google_auth(
    TelefamEngineRef engine,
    const char* id_token,
    const char* nonce,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

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
    size_t random_16_len);

const char* telefam_prepare_check_username(
    TelefamEngineRef engine,
    const char* username,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

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
    size_t random_16_len);

const char* telefam_prepare_submit_profile(
    TelefamEngineRef engine,
    int32_t cur_year,
    int32_t cur_month,
    int32_t cur_day,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

const char* telefam_prepare_fetch_owner_profile(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

const char* telefam_prepare_sync_edited_profile(
    TelefamEngineRef engine,
    const char* profile_json,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

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
    size_t random_16_len);

const char* telefam_prepare_delete_background_photo(
    TelefamEngineRef engine,
    const char* photo_id,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

const char* telefam_prepare_fetch_contacts(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

const char* telefam_prepare_follow_action(
    TelefamEngineRef engine,
    const char* target_user_id,
    bool follow,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

const char* telefam_prepare_refresh_token(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

const char* telefam_prepare_logout(
    TelefamEngineRef engine,
    bool is_online,
    int64_t now_ms,
    const uint8_t* random_16,
    size_t random_16_len);

const char* telefam_on_http_result(
    TelefamEngineRef engine,
    const char* request_id,
    int32_t status_code,
    const char* response_body,
    bool network_error,
    bool is_online,
    int64_t now_ms,
    const uint8_t* vault_nonce_12,
    size_t vault_nonce_12_len);

const char* telefam_get_ui_state(TelefamEngineRef engine);
const char* telefam_get_countries(TelefamEngineRef engine);
const char* telefam_filter_contacts(TelefamEngineRef engine, const char* contacts_array_json);
const char* telefam_parse_contacts_sync(TelefamEngineRef engine, const char* server_response_json);

#ifdef __cplusplus
}
#endif

#endif
