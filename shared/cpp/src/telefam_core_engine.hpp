#pragma once

#include <cstdint>
#include <map>
#include <string>
#include <vector>

#include "anti_tamper.hpp"
#include "country_rules.hpp"
#include "json_codec.hpp"

namespace telefam::core {

enum class Screen {
    AuthEmail,
    AuthOtp,
    ProfileSetup,
    Home
};

enum class ThemeMode {
    System,
    Light,
    Dark
};

enum class HomeTab {
    Chats,
    Contacts,
    Profile
};

enum class ProfileContentTab {
    Posts,
    Saved,
    Reposts,
    Locked
};

enum class ContactsTab {
    All,
    Suggestions,
    Followers,
    Following,
    Friends
};

enum class PendingOp {
    None,
    SendOtp,
    VerifyOtp,
    GoogleAuth,
    AppleAuth,
    CheckUsername,
    UploadPhoto,
    SubmitProfile,
    FetchProfile,
    SyncEditedProfile,
    UploadBackgroundPhoto,
    DeleteBackgroundPhoto,
    FetchContacts,
    FollowUser,
    UnfollowUser,
    RefreshToken,
    Logout
};

struct BackgroundPhotoItem {
    std::string id;
    std::string image_url;
    std::string sha256_hex;
    std::string dhash_hex;
    int width = 0;
    int height = 0;
};

struct ProfileDraft {
    std::string full_name;
    std::string username;
    bool username_checking = false;
    bool username_available = false;
    bool username_checked = false;
    std::string username_status_msg;

    std::string phone_country_iso = "KE";
    std::string phone_number;
    std::string phone_error;

    std::string gender;
    std::string date_of_birth;
    std::string dob_error;

    std::string location;
    std::string bio;

    std::string avatar_url;
    std::string avatar_fingerprint_sha256;
    std::string avatar_fingerprint_dhash;
    bool photo_uploading = false;

    int64_t username_changed_at_sec = 0;
    int64_t next_username_change_at_sec = 0;
    bool can_change_username = true;
    int followers_count = 0;
    int following_count = 0;
    int likes_count = 0;
    int posts_count = 0;
    std::vector<BackgroundPhotoItem> background_photos;
    bool background_uploading = false;
};

struct SessionTokens {
    std::string user_id;
    std::string email;
    std::string access_token;
    std::string refresh_token;
    std::string bound_device_id_hash;
    bool profile_completed = false;
};

class TelefamCoreEngine {
public:
    void initialize(
        const std::string& base_url,
        bool is_debug_build,
        const std::string& device_hardware_name,
        const std::string& device_id_hash,
        const std::string& device_public_key_b64,
        const std::string& runtime_cert_sha256,
        const std::string& expected_release_cert_sha256,
        const std::vector<uint8_t>& master_key,
        const std::string& sealed_vault_b64,
        int64_t now_ms);

    void set_email(const std::string& email);
    void set_otp(const std::string& otp);
    void go_back_to_email();

    void update_profile_field(const std::string& field, const std::string& value, int cur_year, int cur_month, int cur_day);
    bool apply_local_profile_edit_json(
        const std::string& edit_json,
        int cur_year,
        int cur_month,
        int cur_day,
        int64_t now_ms);
    void apply_cached_owner_profile_json(const std::string& cached_json, int64_t now_ms);
    std::string serialize_owner_profile_json() const;

    void set_home_tab(const std::string& tab);
    void set_profile_content_tab(const std::string& tab);
    void set_contacts_tab(const std::string& tab);
    void set_contacts_search(const std::string& query);
    bool check_and_record_follow_rate(int64_t now_ms);

    void set_coming_soon_notice(const std::string& feature_label);
    void set_error(const std::string& raw_error);
    void clear_error();
    void clear_notice();
    void cycle_theme_mode();
    void tick(int64_t now_ms);
    void logout(int64_t now_ms);

    std::string prepare_send_otp(bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);
    std::string prepare_verify_otp(bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);
    std::string prepare_google_auth(const std::string& id_token, const std::string& nonce, bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);
    std::string prepare_apple_auth(
        const std::string& identity_token,
        const std::string& authorization_code,
        const std::string& nonce,
        const std::string& full_name,
        const std::string& email_hint,
        bool is_online,
        int64_t now_ms,
        const std::vector<uint8_t>& random_16);
    std::string prepare_check_username(const std::string& username, bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);
    std::string prepare_upload_photo(
        const std::string& mime_type,
        int64_t byte_size,
        double crop_x,
        double crop_y,
        double crop_w,
        double crop_h,
        bool is_online,
        int64_t now_ms,
        const std::vector<uint8_t>& random_16);
    std::string prepare_submit_profile(int cur_year, int cur_month, int cur_day, bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);
    std::string prepare_fetch_owner_profile(bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);
    std::string prepare_sync_edited_profile(const std::string& profile_json, bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);
    std::string prepare_upload_background_photo(
        const std::string& mime_type,
        int64_t byte_size,
        double crop_x,
        double crop_y,
        double crop_w,
        double crop_h,
        bool is_online,
        int64_t now_ms,
        const std::vector<uint8_t>& random_16);
    std::string prepare_delete_background_photo(
        const std::string& photo_id,
        bool is_online,
        int64_t now_ms,
        const std::vector<uint8_t>& random_16);
    std::string prepare_fetch_contacts(bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);
    std::string prepare_follow_action(const std::string& target_user_id, bool follow, bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);
    std::string prepare_refresh_token(bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);
    std::string prepare_logout(bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16);

    void on_http_result(
        const std::string& request_id,
        int status_code,
        const std::string& response_body,
        bool network_error,
        bool is_online,
        int64_t now_ms,
        const std::vector<uint8_t>& vault_nonce_12);

    std::string get_ui_state_json() const;
    std::string get_sealed_vault_b64() const { return sealed_vault_b64_; }
    std::string get_countries_json() const;
    std::string filter_contacts_json(const std::string& contacts_array_json) const;
    std::string parse_contacts_sync_json(const std::string& server_response_json) const;

private:
    std::string base_url_;
    bool is_debug_build_ = false;
    std::string device_hardware_name_;
    std::string device_id_hash_;
    std::string device_public_key_b64_;
    std::vector<uint8_t> master_key_;
    security::IntegrityReport integrity_;

    Screen screen_ = Screen::AuthEmail;
    ThemeMode theme_mode_ = ThemeMode::System;
    HomeTab home_tab_ = HomeTab::Chats;
    ProfileContentTab profile_content_tab_ = ProfileContentTab::Posts;
    ContactsTab contacts_tab_ = ContactsTab::All;
    std::string contacts_search_query_;

    std::string email_;
    std::string otp_;
    bool is_loading_ = false;
    std::string error_message_;
    std::string notice_message_;

    int64_t resend_available_at_ms_ = 0;
    int resend_remaining_sec_ = 0;

    int failed_attempts_ = 0;
    int lock_cycles_ = 0;
    int64_t locked_until_ms_ = 0;
    int lock_remaining_sec_ = 0;

    SessionTokens session_;
    ProfileDraft profile_;
    std::string sealed_vault_b64_;
    bool vault_dirty_ = false;
    std::vector<int64_t> follow_timestamps_ms_;

    uint64_t req_seq_ = 0;
    std::map<std::string, PendingOp> pending_ops_;

    std::string sanitize_error(const std::string& raw, const std::string& user_safe_fallback) const;
    std::string build_http_action(
        PendingOp op,
        const std::string& method,
        const std::string& path,
        const std::string& query,
        const std::string& body,
        bool authenticated,
        int64_t now_ms,
        const std::vector<uint8_t>& random_16,
        const std::string& extra_json = "");
    void apply_email_prefix_autofill();
    void apply_profile_json_object(const json::Value* prof, int64_t now_ms);
    void persist_vault(int64_t now_ms, const std::vector<uint8_t>& nonce_12);
    void restore_vault(const std::string& sealed_b64);
    bool check_integrity_gate();
    static bool is_valid_email(const std::string& email);
    static bool is_valid_username(const std::string& username);
    static bool validate_dob_at_least_13(const std::string& dob, int cur_y, int cur_m, int cur_d, std::string& err_out);
    static std::string format_mm_ss(int total_seconds);
};

}  // namespace telefam::core
