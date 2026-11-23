#include "telefam_core_engine.hpp"

#include <algorithm>
#include <array>
#include <cctype>
#include <cstdio>
#include <cstring>

#include "crypto_engine.hpp"
#include "json_codec.hpp"
#include "string_vault.hpp"

namespace telefam::core {

namespace {

std::string trim_copy(const std::string& s) {
    size_t start = 0;
    while (start < s.size() && std::isspace(static_cast<unsigned char>(s[start]))) ++start;
    size_t end = s.size();
    while (end > start && std::isspace(static_cast<unsigned char>(s[end - 1]))) --end;
    return s.substr(start, end - start);
}

std::string lower_copy(std::string s) {
    for (char& c : s) c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
    return s;
}

}  // namespace

void TelefamCoreEngine::initialize(
    const std::string& base_url,
    bool is_debug_build,
    const std::string& device_hardware_name,
    const std::string& device_id_hash,
    const std::string& device_public_key_b64,
    const std::string& runtime_cert_sha256,
    const std::string& expected_release_cert_sha256,
    const std::vector<uint8_t>& master_key,
    const std::string& sealed_vault_b64,
    int64_t now_ms) {
    base_url_ = base_url;
    while (!base_url_.empty() && base_url_.back() == '/') {
        base_url_.pop_back();
    }
    is_debug_build_ = is_debug_build;
    device_hardware_name_ = device_hardware_name;
    device_id_hash_ = device_id_hash;
    device_public_key_b64_ = device_public_key_b64;
    master_key_ = master_key;

    integrity_ = security::inspect_runtime_integrity(
        is_debug_build,
        runtime_cert_sha256,
        expected_release_cert_sha256);

    if (!sealed_vault_b64.empty() && !integrity_.compromised) {
        restore_vault(sealed_vault_b64);
    }
    tick(now_ms);
}

void TelefamCoreEngine::restore_vault(const std::string& sealed_b64) {
    std::string pt;
    if (!crypto::unseal_device_vault(
            master_key_, device_id_hash_, device_hardware_name_, sealed_b64, pt)) {
        return;
    }
    json::Value root;
    if (!json::parse(pt, root) || !root.is_object()) {
        crypto::secure_zero(&pt[0], pt.size());
        return;
    }
    const std::string bound_dev = root.get_string("boundDeviceIdHash");
    if (bound_dev != device_id_hash_) {
        crypto::secure_zero(&pt[0], pt.size());
        return;
    }

    session_.user_id = root.get_string("userId");
    session_.email = root.get_string("email");
    session_.access_token = root.get_string("accessToken");
    session_.refresh_token = root.get_string("refreshToken");
    session_.bound_device_id_hash = bound_dev;
    session_.profile_completed = root.get_bool("profileCompleted");
    email_ = session_.email;

    const std::string theme = root.get_string("themeMode", "SYSTEM");
    if (theme == "LIGHT") theme_mode_ = ThemeMode::Light;
    else if (theme == "DARK") theme_mode_ = ThemeMode::Dark;
    else theme_mode_ = ThemeMode::System;

    profile_.full_name = root.get_string("fullName");
    profile_.username = root.get_string("username");
    profile_.phone_country_iso = root.get_string("phoneCountryIso", "KE");
    profile_.phone_number = root.get_string("phoneNumber");
    profile_.gender = root.get_string("gender");
    profile_.date_of_birth = root.get_string("dateOfBirth");
    profile_.location = root.get_string("location");
    profile_.bio = root.get_string("bio");
    profile_.avatar_url = root.get_string("avatarUrl");
    profile_.avatar_fingerprint_sha256 = root.get_string("avatarFingerprintSha256");
    profile_.username_changed_at_sec = root.get_int("usernameChangedAtEpochSec", 0);
    profile_.next_username_change_at_sec = root.get_int("nextUsernameChangeAtEpochSec", 0);
    profile_.followers_count = static_cast<int>(root.get_int("followersCount", 0));
    profile_.following_count = static_cast<int>(root.get_int("followingCount", 0));
    profile_.likes_count = static_cast<int>(root.get_int("likesCount", 0));
    profile_.posts_count = static_cast<int>(root.get_int("postsCount", 0));
    if (const json::Value* bgs = root.get("backgroundPhotos")) {
        if (bgs->is_array()) {
            profile_.background_photos.clear();
            for (const auto& item : bgs->arr_val) {
                if (!item.is_object() || profile_.background_photos.size() >= 5) continue;
                BackgroundPhotoItem bg;
                bg.id = item.get_string("id");
                bg.image_url = item.get_string("imageUrl");
                bg.sha256_hex = item.get_string("fingerprintSha256");
                bg.dhash_hex = item.get_string("fingerprintDhash");
                bg.width = static_cast<int>(item.get_int("width", 0));
                bg.height = static_cast<int>(item.get_int("height", 0));
                if (!bg.id.empty() && !bg.image_url.empty()) {
                    profile_.background_photos.push_back(std::move(bg));
                }
            }
        }
    }

    sealed_vault_b64_ = sealed_b64;
    if (!session_.access_token.empty()) {
        screen_ = session_.profile_completed ? Screen::Home : Screen::ProfileSetup;
        if (!session_.profile_completed && profile_.full_name.empty() && profile_.username.empty()) {
            apply_email_prefix_autofill();
        }
    }
    crypto::secure_zero(&pt[0], pt.size());
}

void TelefamCoreEngine::persist_vault(int64_t now_ms, const std::vector<uint8_t>& nonce_12) {
    if (nonce_12.size() < 12) return;
    json::Value root = json::Value::make_object();
    root.obj_val["userId"] = json::Value::make_string(session_.user_id);
    root.obj_val["email"] = json::Value::make_string(session_.email);
    root.obj_val["accessToken"] = json::Value::make_string(session_.access_token);
    root.obj_val["refreshToken"] = json::Value::make_string(session_.refresh_token);
    root.obj_val["boundDeviceIdHash"] = json::Value::make_string(device_id_hash_);
    root.obj_val["profileCompleted"] = json::Value::make_bool(session_.profile_completed);
    root.obj_val["sealedAtMs"] = json::Value::make_int(now_ms);
    std::string theme_str = "SYSTEM";
    if (theme_mode_ == ThemeMode::Light) theme_str = "LIGHT";
    else if (theme_mode_ == ThemeMode::Dark) theme_str = "DARK";
    root.obj_val["themeMode"] = json::Value::make_string(theme_str);

    root.obj_val["fullName"] = json::Value::make_string(profile_.full_name);
    root.obj_val["username"] = json::Value::make_string(profile_.username);
    root.obj_val["phoneCountryIso"] = json::Value::make_string(profile_.phone_country_iso);
    root.obj_val["phoneNumber"] = json::Value::make_string(profile_.phone_number);
    root.obj_val["gender"] = json::Value::make_string(profile_.gender);
    root.obj_val["dateOfBirth"] = json::Value::make_string(profile_.date_of_birth);
    root.obj_val["location"] = json::Value::make_string(profile_.location);
    root.obj_val["bio"] = json::Value::make_string(profile_.bio);
    root.obj_val["avatarUrl"] = json::Value::make_string(profile_.avatar_url);
    root.obj_val["avatarFingerprintSha256"] = json::Value::make_string(profile_.avatar_fingerprint_sha256);
    root.obj_val["usernameChangedAtEpochSec"] = json::Value::make_int(profile_.username_changed_at_sec);
    root.obj_val["nextUsernameChangeAtEpochSec"] = json::Value::make_int(profile_.next_username_change_at_sec);
    root.obj_val["followersCount"] = json::Value::make_int(profile_.followers_count);
    root.obj_val["followingCount"] = json::Value::make_int(profile_.following_count);
    root.obj_val["likesCount"] = json::Value::make_int(profile_.likes_count);
    root.obj_val["postsCount"] = json::Value::make_int(profile_.posts_count);

    json::Value bg_arr = json::Value::make_array();
    for (const auto& bg : profile_.background_photos) {
        json::Value o = json::Value::make_object();
        o.obj_val["id"] = json::Value::make_string(bg.id);
        o.obj_val["imageUrl"] = json::Value::make_string(bg.image_url);
        o.obj_val["fingerprintSha256"] = json::Value::make_string(bg.sha256_hex);
        o.obj_val["fingerprintDhash"] = json::Value::make_string(bg.dhash_hex);
        o.obj_val["width"] = json::Value::make_int(bg.width);
        o.obj_val["height"] = json::Value::make_int(bg.height);
        bg_arr.arr_val.push_back(std::move(o));
    }
    root.obj_val["backgroundPhotos"] = std::move(bg_arr);

    std::string json_pt = json::stringify(root);
    std::array<uint8_t, 12> nonce{};
    std::memcpy(nonce.data(), nonce_12.data(), 12);
    std::string sealed;
    if (crypto::seal_device_vault(master_key_, device_id_hash_, device_hardware_name_, json_pt, nonce, sealed)) {
        sealed_vault_b64_ = sealed;
        vault_dirty_ = true;
    }
    crypto::secure_zero(&json_pt[0], json_pt.size());
}

bool TelefamCoreEngine::check_integrity_gate() {
    if (integrity_.compromised) {
        error_message_ = sanitize_error("Integrity check failed", "Error has occurred");
        return false;
    }
    return true;
}

std::string TelefamCoreEngine::sanitize_error(const std::string& raw, const std::string& user_safe_fallback) const {
    if (!is_debug_build_) {
        return "Error has occurred";
    }
    return raw.empty() ? user_safe_fallback : raw;
}

void TelefamCoreEngine::set_email(const std::string& email) {
    email_ = trim_copy(email);
    error_message_.clear();
}

void TelefamCoreEngine::set_otp(const std::string& otp) {
    std::string digits;
    digits.reserve(6);
    for (unsigned char c : otp) {
        if (std::isdigit(c) && digits.size() < 6) {
            digits.push_back(static_cast<char>(c));
        }
    }
    otp_ = digits;
    error_message_.clear();
}

void TelefamCoreEngine::go_back_to_email() {
    otp_.clear();
    error_message_.clear();
    notice_message_.clear();
    screen_ = Screen::AuthEmail;
}

void TelefamCoreEngine::set_error(const std::string& raw_error) {
    error_message_ = sanitize_error(raw_error, "Error has occurred");
}

void TelefamCoreEngine::clear_error() {
    error_message_.clear();
}

void TelefamCoreEngine::clear_notice() {
    notice_message_.clear();
}

void TelefamCoreEngine::set_coming_soon_notice(const std::string& /* feature_label */) {
    notice_message_ = "Coming soon";
}

void TelefamCoreEngine::set_home_tab(const std::string& tab) {
    std::string lower = lower_copy(trim_copy(tab));
    if (lower == "contacts") home_tab_ = HomeTab::Contacts;
    else if (lower == "profile") home_tab_ = HomeTab::Profile;
    else home_tab_ = HomeTab::Chats;
    error_message_.clear();
}

void TelefamCoreEngine::set_profile_content_tab(const std::string& tab) {
    std::string lower = lower_copy(trim_copy(tab));
    if (lower == "saved") profile_content_tab_ = ProfileContentTab::Saved;
    else if (lower == "reposts") profile_content_tab_ = ProfileContentTab::Reposts;
    else if (lower == "locked") profile_content_tab_ = ProfileContentTab::Locked;
    else profile_content_tab_ = ProfileContentTab::Posts;
}

void TelefamCoreEngine::set_contacts_tab(const std::string& tab) {
    std::string lower = lower_copy(trim_copy(tab));
    if (lower == "suggestions") contacts_tab_ = ContactsTab::Suggestions;
    else if (lower == "followers") contacts_tab_ = ContactsTab::Followers;
    else if (lower == "following") contacts_tab_ = ContactsTab::Following;
    else if (lower == "friends") contacts_tab_ = ContactsTab::Friends;
    else contacts_tab_ = ContactsTab::All;
}

void TelefamCoreEngine::set_contacts_search(const std::string& query) {
    contacts_search_query_ = query.substr(0, 64);
}

bool TelefamCoreEngine::check_and_record_follow_rate(int64_t now_ms) {
    const int64_t cutoff = now_ms - 60000LL;
    follow_timestamps_ms_.erase(
        std::remove_if(
            follow_timestamps_ms_.begin(),
            follow_timestamps_ms_.end(),
            [cutoff](int64_t ts) { return ts < cutoff; }),
        follow_timestamps_ms_.end());
    if (follow_timestamps_ms_.size() >= 100) {
        notice_message_ = "Error has occurred. Try again later.";
        return false;
    }
    follow_timestamps_ms_.push_back(now_ms);
    return true;
}

void TelefamCoreEngine::cycle_theme_mode() {
    switch (theme_mode_) {
        case ThemeMode::System: theme_mode_ = ThemeMode::Light; break;
        case ThemeMode::Light: theme_mode_ = ThemeMode::Dark; break;
        case ThemeMode::Dark: theme_mode_ = ThemeMode::System; break;
    }
}

void TelefamCoreEngine::tick(int64_t now_ms) {
    if (resend_available_at_ms_ > now_ms) {
        resend_remaining_sec_ = static_cast<int>((resend_available_at_ms_ - now_ms + 999) / 1000);
    } else {
        resend_remaining_sec_ = 0;
    }
    if (locked_until_ms_ > now_ms) {
        lock_remaining_sec_ = static_cast<int>((locked_until_ms_ - now_ms + 999) / 1000);
    } else {
        lock_remaining_sec_ = 0;
    }
    const int64_t now_sec = now_ms / 1000LL;
    profile_.can_change_username =
        (profile_.next_username_change_at_sec <= 0 || now_sec >= profile_.next_username_change_at_sec);
}

void TelefamCoreEngine::logout(int64_t now_ms) {
    if (!session_.access_token.empty()) {
        crypto::secure_zero(&session_.access_token[0], session_.access_token.size());
    }
    if (!session_.refresh_token.empty()) {
        crypto::secure_zero(&session_.refresh_token[0], session_.refresh_token.size());
    }
    session_ = SessionTokens{};
    profile_ = ProfileDraft{};
    otp_.clear();
    sealed_vault_b64_.clear();
    follow_timestamps_ms_.clear();
    home_tab_ = HomeTab::Chats;
    profile_content_tab_ = ProfileContentTab::Posts;
    contacts_tab_ = ContactsTab::All;
    contacts_search_query_.clear();
    vault_dirty_ = true;
    screen_ = Screen::AuthEmail;
    tick(now_ms);
}

bool TelefamCoreEngine::is_valid_email(const std::string& email) {
    if (email.size() < 5 || email.size() > 254) return false;
    auto at_pos = email.find('@');
    if (at_pos == std::string::npos || at_pos == 0 || at_pos == email.size() - 1) return false;
    if (email.find('@', at_pos + 1) != std::string::npos) return false;
    std::string local = email.substr(0, at_pos);
    std::string domain = email.substr(at_pos + 1);
    if (local.size() > 64 || domain.size() < 3) return false;
    if (domain.find('.') == std::string::npos || domain.front() == '.' || domain.back() == '.') return false;
    if (domain.find("..") != std::string::npos || local.find("..") != std::string::npos) return false;
    for (unsigned char c : email) {
        if (c <= 0x20 || c >= 0x7F) return false;
    }
    return true;
}

bool TelefamCoreEngine::is_valid_username(const std::string& username) {
    if (username.size() < 3 || username.size() > 24) return false;
    for (unsigned char c : username) {
        if (!(std::islower(c) || std::isdigit(c) || c == '_')) return false;
    }
    return true;
}

bool TelefamCoreEngine::validate_dob_at_least_13(const std::string& dob, int cur_y, int cur_m, int cur_d, std::string& err_out) {
    if (dob.size() != 10 || dob[4] != '-' || dob[7] != '-') {
        err_out = "Select date of birth";
        return false;
    }
    int y = 0, m = 0, d = 0;
    if (std::sscanf(dob.c_str(), "%d-%d-%d", &y, &m, &d) != 3) {
        err_out = "Invalid date of birth";
        return false;
    }
    if (y < 1900 || m < 1 || m > 12 || d < 1 || d > 31) {
        err_out = "Invalid date of birth";
        return false;
    }
    int age = cur_y - y;
    if (cur_m < m || (cur_m == m && cur_d < d)) {
        age -= 1;
    }
    if (age < 13) {
        err_out = "Must be at least 13 years old";
        return false;
    }
    err_out.clear();
    return true;
}

std::string TelefamCoreEngine::format_mm_ss(int total_seconds) {
    if (total_seconds < 0) total_seconds = 0;
    int mins = total_seconds / 60;
    int secs = total_seconds % 60;
    char buf[16];
    std::snprintf(buf, sizeof(buf), "%02d:%02d", mins, secs);
    return std::string(buf);
}

void TelefamCoreEngine::apply_email_prefix_autofill() {
    auto at_pos = email_.find('@');
    if (at_pos == std::string::npos || at_pos == 0) return;
    std::string prefix = email_.substr(0, at_pos);

    std::string full_name;
    bool new_word = true;
    for (unsigned char c : prefix) {
        if (c == '.' || c == '_' || c == '-' || c == '+') {
            if (!full_name.empty() && full_name.back() != ' ') {
                full_name.push_back(' ');
            }
            new_word = true;
        } else if (std::isalpha(c)) {
            full_name.push_back(new_word ? static_cast<char>(std::toupper(c))
                                         : static_cast<char>(std::tolower(c)));
            new_word = false;
        }
    }
    full_name = trim_copy(full_name);
    if (!full_name.empty() && profile_.full_name.empty()) {
        profile_.full_name = full_name;
    }

    std::string uname;
    for (unsigned char c : prefix) {
        if (std::isalnum(c)) {
            uname.push_back(static_cast<char>(std::tolower(c)));
        } else if (c == '.' || c == '_' || c == '-') {
            if (!uname.empty() && uname.back() != '_') uname.push_back('_');
        }
        if (uname.size() >= 24) break;
    }
    while (!uname.empty() && uname.back() == '_') uname.pop_back();
    if (uname.size() >= 3 && profile_.username.empty()) {
        profile_.username = uname;
    }
}

void TelefamCoreEngine::update_profile_field(
    const std::string& field,
    const std::string& value,
    int cur_year,
    int cur_month,
    int cur_day) {
    error_message_.clear();
    if (field == "fullName") {
        profile_.full_name = value.substr(0, 64);
    } else if (field == "username") {
        profile_.username = lower_copy(trim_copy(value)).substr(0, 24);
        profile_.username_checked = false;
        profile_.username_available = false;
        profile_.username_status_msg.clear();
    } else if (field == "phoneCountryIso") {
        profile_.phone_country_iso = value;
        if (!profile_.phone_number.empty()) {
            auto res = countries::validate_phone(profile_.phone_country_iso, profile_.phone_number);
            profile_.phone_error = res.valid ? "" : res.error_message;
        }
    } else if (field == "phoneNumber") {
        profile_.phone_number = value.substr(0, 20);
        if (!profile_.phone_number.empty()) {
            auto res = countries::validate_phone(profile_.phone_country_iso, profile_.phone_number);
            profile_.phone_error = res.valid ? "" : res.error_message;
        } else {
            profile_.phone_error.clear();
        }
    } else if (field == "gender") {
        profile_.gender = value;
    } else if (field == "dateOfBirth") {
        profile_.date_of_birth = value;
        validate_dob_at_least_13(value, cur_year, cur_month, cur_day, profile_.dob_error);
    } else if (field == "location") {
        profile_.location = value.substr(0, 80);
    } else if (field == "bio") {
        profile_.bio = value.substr(0, 150);
    }
}

void TelefamCoreEngine::apply_profile_json_object(const json::Value* prof, int64_t now_ms) {
    if (!prof || !prof->is_object()) return;
    profile_.full_name = prof->get_string("fullName", profile_.full_name);
    profile_.username = prof->get_string("username", profile_.username);
    profile_.phone_country_iso = prof->get_string("phoneCountryIso", profile_.phone_country_iso.empty() ? "KE" : profile_.phone_country_iso);
    profile_.phone_number = prof->get_string("phoneNumber", profile_.phone_number);
    profile_.gender = prof->get_string("gender", profile_.gender);
    profile_.date_of_birth = prof->get_string("dateOfBirth", profile_.date_of_birth);
    profile_.location = prof->get_string("location", profile_.location);
    profile_.bio = prof->get_string("bio", profile_.bio);
    profile_.avatar_url = prof->get_string("avatarUrl", profile_.avatar_url);
    profile_.avatar_fingerprint_sha256 = prof->get_string("avatarFingerprintSha256", profile_.avatar_fingerprint_sha256);

    profile_.username_changed_at_sec = prof->get_int("usernameChangedAtEpochSec", profile_.username_changed_at_sec);
    profile_.next_username_change_at_sec = prof->get_int("nextUsernameChangeAtEpochSec", profile_.next_username_change_at_sec);
    const int64_t now_sec = now_ms / 1000LL;
    profile_.can_change_username =
        (profile_.next_username_change_at_sec <= 0 || now_sec >= profile_.next_username_change_at_sec);
    profile_.followers_count = static_cast<int>(prof->get_int("followersCount", profile_.followers_count));
    profile_.following_count = static_cast<int>(prof->get_int("followingCount", profile_.following_count));
    profile_.likes_count = static_cast<int>(prof->get_int("likesCount", profile_.likes_count));
    profile_.posts_count = static_cast<int>(prof->get_int("postsCount", profile_.posts_count));

    if (const json::Value* bgs = prof->get("backgroundPhotos")) {
        if (bgs->is_array()) {
            profile_.background_photos.clear();
            for (const auto& item : bgs->arr_val) {
                if (!item.is_object() || profile_.background_photos.size() >= 5) continue;
                BackgroundPhotoItem bg;
                bg.id = item.get_string("id");
                bg.image_url = item.get_string("imageUrl");
                bg.sha256_hex = item.get_string("fingerprintSha256");
                bg.dhash_hex = item.get_string("fingerprintDhash");
                bg.width = static_cast<int>(item.get_int("width", 0));
                bg.height = static_cast<int>(item.get_int("height", 0));
                if (!bg.id.empty() && !bg.image_url.empty()) {
                    profile_.background_photos.push_back(std::move(bg));
                }
            }
        }
    }
}

void TelefamCoreEngine::apply_cached_owner_profile_json(const std::string& cached_json, int64_t now_ms) {
    json::Value obj;
    if (!json::parse(cached_json, obj) || !obj.is_object()) return;
    apply_profile_json_object(&obj, now_ms);
}

std::string TelefamCoreEngine::serialize_owner_profile_json() const {
    json::Value prof = json::Value::make_object();
    prof.obj_val["fullName"] = json::Value::make_string(profile_.full_name);
    prof.obj_val["username"] = json::Value::make_string(profile_.username);
    prof.obj_val["phoneCountryIso"] = json::Value::make_string(profile_.phone_country_iso);
    prof.obj_val["phoneNumber"] = json::Value::make_string(profile_.phone_number);
    prof.obj_val["gender"] = json::Value::make_string(profile_.gender);
    prof.obj_val["dateOfBirth"] = json::Value::make_string(profile_.date_of_birth);
    prof.obj_val["location"] = json::Value::make_string(profile_.location);
    prof.obj_val["bio"] = json::Value::make_string(profile_.bio);
    prof.obj_val["avatarUrl"] = json::Value::make_string(profile_.avatar_url);
    prof.obj_val["avatarFingerprintSha256"] = json::Value::make_string(profile_.avatar_fingerprint_sha256);
    prof.obj_val["usernameChangedAtEpochSec"] = json::Value::make_int(profile_.username_changed_at_sec);
    prof.obj_val["nextUsernameChangeAtEpochSec"] = json::Value::make_int(profile_.next_username_change_at_sec);
    prof.obj_val["canChangeUsername"] = json::Value::make_bool(profile_.can_change_username);
    prof.obj_val["followersCount"] = json::Value::make_int(profile_.followers_count);
    prof.obj_val["followingCount"] = json::Value::make_int(profile_.following_count);
    prof.obj_val["likesCount"] = json::Value::make_int(profile_.likes_count);
    prof.obj_val["postsCount"] = json::Value::make_int(profile_.posts_count);

    json::Value bg_arr = json::Value::make_array();
    for (const auto& bg : profile_.background_photos) {
        json::Value o = json::Value::make_object();
        o.obj_val["id"] = json::Value::make_string(bg.id);
        o.obj_val["imageUrl"] = json::Value::make_string(bg.image_url);
        o.obj_val["fingerprintSha256"] = json::Value::make_string(bg.sha256_hex);
        o.obj_val["fingerprintDhash"] = json::Value::make_string(bg.dhash_hex);
        o.obj_val["width"] = json::Value::make_int(bg.width);
        o.obj_val["height"] = json::Value::make_int(bg.height);
        bg_arr.arr_val.push_back(std::move(o));
    }
    prof.obj_val["backgroundPhotos"] = std::move(bg_arr);
    return json::stringify(prof);
}

bool TelefamCoreEngine::apply_local_profile_edit_json(
    const std::string& edit_json,
    int cur_year,
    int cur_month,
    int cur_day,
    int64_t now_ms) {
    json::Value obj;
    if (!json::parse(edit_json, obj) || !obj.is_object()) {
        error_message_ = "Error has occurred";
        return false;
    }

    std::string full_name = trim_copy(obj.get_string("fullName")).substr(0, 64);
    std::string username = lower_copy(trim_copy(obj.get_string("username"))).substr(0, 24);
    std::string phone_iso = trim_copy(obj.get_string("phoneCountryIso", profile_.phone_country_iso));
    std::string phone_num = trim_copy(obj.get_string("phoneNumber"));
    std::string gender = trim_copy(obj.get_string("gender"));
    std::string dob = trim_copy(obj.get_string("dateOfBirth"));
    std::string loc = trim_copy(obj.get_string("location")).substr(0, 80);
    std::string bio = trim_copy(obj.get_string("bio")).substr(0, 150);

    if (full_name.size() < 2) {
        error_message_ = "Enter your full name";
        return false;
    }
    if (!is_valid_username(username)) {
        error_message_ = "Choose a valid username (3-24 chars)";
        return false;
    }
    const int64_t now_sec = now_ms / 1000LL;
    bool username_changed = !profile_.username.empty() && username != profile_.username;
    if (username_changed && profile_.next_username_change_at_sec > 0 && now_sec < profile_.next_username_change_at_sec) {
        error_message_ = "Username can only be changed every 14 days";
        return false;
    }

    auto phone_res = countries::validate_phone(phone_iso, phone_num);
    if (!phone_res.valid) {
        error_message_ = phone_res.error_message;
        return false;
    }
    if (gender.empty()) {
        error_message_ = "Select your gender";
        return false;
    }
    std::string dob_err;
    if (!validate_dob_at_least_13(dob, cur_year, cur_month, cur_day, dob_err)) {
        error_message_ = dob_err;
        return false;
    }
    if (loc.size() < 2) {
        error_message_ = "Enter your location";
        return false;
    }

    profile_.full_name = full_name;
    profile_.username = username;
    profile_.phone_country_iso = phone_iso;
    profile_.phone_number = phone_res.normalized_e164;
    profile_.phone_error.clear();
    profile_.gender = gender;
    profile_.date_of_birth = dob;
    profile_.dob_error.clear();
    profile_.location = loc;
    profile_.bio = bio;
    if (username_changed) {
        profile_.username_changed_at_sec = now_sec;
        profile_.next_username_change_at_sec = now_sec + 14LL * 86400LL;
        profile_.can_change_username = false;
    }
    error_message_.clear();
    return true;
}

std::string TelefamCoreEngine::build_http_action(
    PendingOp op,
    const std::string& method,
    const std::string& path,
    const std::string& query,
    const std::string& body,
    bool authenticated,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16,
    const std::string& extra_json) {
    ++req_seq_;
    std::string req_id = "req_" + std::to_string(req_seq_);
    pending_ops_[req_id] = op;

    std::string nonce_b64 = random_16.empty()
        ? std::to_string(now_ms)
        : crypto::base64url_encode(random_16.data(), random_16.size());
    std::string sign_path = query.empty() ? path : (path + "?" + query);
    std::string sig = crypto::sign_device_request(
        master_key_, device_id_hash_, method, sign_path, now_ms, nonce_b64, body);

    json::Value headers = json::Value::make_object();
    headers.obj_val["Content-Type"] = json::Value::make_string("application/json");
    headers.obj_val[TELEFAM_OBF("X-Telefam-Device-Id", 0x11)] = json::Value::make_string(device_id_hash_);
    headers.obj_val[TELEFAM_OBF("X-Telefam-Hardware", 0x12)] = json::Value::make_string(device_hardware_name_);
    headers.obj_val[TELEFAM_OBF("X-Telefam-Timestamp", 0x13)] = json::Value::make_string(std::to_string(now_ms));
    headers.obj_val[TELEFAM_OBF("X-Telefam-Nonce", 0x14)] = json::Value::make_string(nonce_b64);
    headers.obj_val[TELEFAM_OBF("X-Telefam-Signature", 0x15)] = json::Value::make_string(sig);
    headers.obj_val[TELEFAM_OBF("X-Telefam-Integrity", 0x16)] = json::Value::make_string(integrity_.digest_summary);
    if (authenticated && !session_.access_token.empty()) {
        headers.obj_val["Authorization"] = json::Value::make_string("Bearer " + session_.access_token);
    }

    json::Value act = json::Value::make_object();
    act.obj_val["action"] = json::Value::make_string("HTTP");
    act.obj_val["requestId"] = json::Value::make_string(req_id);
    act.obj_val["method"] = json::Value::make_string(method);
    act.obj_val["url"] = json::Value::make_string(base_url_ + sign_path);
    act.obj_val["headers"] = headers;
    act.obj_val["body"] = json::Value::make_string(body);
    if (!extra_json.empty()) {
        act.obj_val["extra"] = json::Value::make_string(extra_json);
    }
    return json::stringify(act);
}

std::string TelefamCoreEngine::prepare_send_otp(bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16) {
    tick(now_ms);
    if (!check_integrity_gate()) {
        return "{\"action\":\"NONE\"}";
    }
    if (!is_online) {
        error_message_ = "No internet connection";
        return "{\"action\":\"NONE\"}";
    }
    if (lock_remaining_sec_ > 0) {
        error_message_ = "Too many attempts. Try again in " + format_mm_ss(lock_remaining_sec_);
        return "{\"action\":\"NONE\"}";
    }
    if (screen_ == Screen::AuthOtp && resend_remaining_sec_ > 0) {
        error_message_ = "Resend available in " + format_mm_ss(resend_remaining_sec_);
        return "{\"action\":\"NONE\"}";
    }
    if (!is_valid_email(email_)) {
        error_message_ = "Enter a valid email address";
        return "{\"action\":\"NONE\"}";
    }

    is_loading_ = true;
    error_message_.clear();

    json::Value payload = json::Value::make_object();
    payload.obj_val["email"] = json::Value::make_string(lower_copy(email_));
    payload.obj_val["deviceIdHash"] = json::Value::make_string(device_id_hash_);
    payload.obj_val["deviceHardwareName"] = json::Value::make_string(device_hardware_name_);
    payload.obj_val["devicePublicKey"] = json::Value::make_string(device_public_key_b64_);

    return build_http_action(
        PendingOp::SendOtp,
        "POST",
        TELEFAM_OBF("/api/v1/auth/otp/send", 0x21),
        "",
        json::stringify(payload),
        false,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_verify_otp(bool is_online, int64_t now_ms, const std::vector<uint8_t>& random_16) {
    tick(now_ms);
    if (!check_integrity_gate()) {
        return "{\"action\":\"NONE\"}";
    }
    if (!is_online) {
        error_message_ = "No internet connection";
        return "{\"action\":\"NONE\"}";
    }
    if (lock_remaining_sec_ > 0) {
        error_message_ = "Locked for " + format_mm_ss(lock_remaining_sec_);
        return "{\"action\":\"NONE\"}";
    }
    if (otp_.size() != 6) {
        error_message_ = "Enter 6-digit OTP code";
        return "{\"action\":\"NONE\"}";
    }

    is_loading_ = true;
    error_message_.clear();

    json::Value payload = json::Value::make_object();
    payload.obj_val["email"] = json::Value::make_string(lower_copy(email_));
    payload.obj_val["otp"] = json::Value::make_string(otp_);
    payload.obj_val["deviceIdHash"] = json::Value::make_string(device_id_hash_);
    payload.obj_val["deviceHardwareName"] = json::Value::make_string(device_hardware_name_);
    payload.obj_val["devicePublicKey"] = json::Value::make_string(device_public_key_b64_);

    return build_http_action(
        PendingOp::VerifyOtp,
        "POST",
        TELEFAM_OBF("/api/v1/auth/otp/verify", 0x22),
        "",
        json::stringify(payload),
        false,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_google_auth(
    const std::string& id_token,
    const std::string& nonce,
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate()) {
        return "{\"action\":\"NONE\"}";
    }
    if (!is_online) {
        error_message_ = "No internet connection";
        return "{\"action\":\"NONE\"}";
    }
    if (id_token.empty()) {
        error_message_ = sanitize_error("Empty Google token", "Error has occurred");
        return "{\"action\":\"NONE\"}";
    }
    is_loading_ = true;
    error_message_.clear();

    json::Value payload = json::Value::make_object();
    payload.obj_val["idToken"] = json::Value::make_string(id_token);
    payload.obj_val["nonce"] = json::Value::make_string(nonce);
    payload.obj_val["deviceIdHash"] = json::Value::make_string(device_id_hash_);
    payload.obj_val["deviceHardwareName"] = json::Value::make_string(device_hardware_name_);
    payload.obj_val["devicePublicKey"] = json::Value::make_string(device_public_key_b64_);

    return build_http_action(
        PendingOp::GoogleAuth,
        "POST",
        TELEFAM_OBF("/api/v1/auth/google", 0x23),
        "",
        json::stringify(payload),
        false,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_apple_auth(
    const std::string& identity_token,
    const std::string& authorization_code,
    const std::string& nonce,
    const std::string& full_name,
    const std::string& email_hint,
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate()) {
        return "{\"action\":\"NONE\"}";
    }
    if (!is_online) {
        error_message_ = "No internet connection";
        return "{\"action\":\"NONE\"}";
    }
    if (identity_token.empty()) {
        error_message_ = sanitize_error("Empty Apple token", "Error has occurred");
        return "{\"action\":\"NONE\"}";
    }
    is_loading_ = true;
    error_message_.clear();

    std::string trimmed_name = trim_copy(full_name);
    if (!trimmed_name.empty() && profile_.full_name.empty()) {
        profile_.full_name = trimmed_name.substr(0, 64);
    }
    std::string trimmed_email = lower_copy(trim_copy(email_hint));
    if (is_valid_email(trimmed_email) && email_.empty()) {
        email_ = trimmed_email;
    }

    json::Value payload = json::Value::make_object();
    payload.obj_val["identityToken"] = json::Value::make_string(identity_token);
    if (!authorization_code.empty()) {
        payload.obj_val["authorizationCode"] = json::Value::make_string(authorization_code);
    }
    payload.obj_val["nonce"] = json::Value::make_string(nonce);
    if (!trimmed_name.empty()) {
        payload.obj_val["fullName"] = json::Value::make_string(trimmed_name);
    }
    if (!trimmed_email.empty()) {
        payload.obj_val["emailHint"] = json::Value::make_string(trimmed_email);
    }
    payload.obj_val["deviceIdHash"] = json::Value::make_string(device_id_hash_);
    payload.obj_val["deviceHardwareName"] = json::Value::make_string(device_hardware_name_);
    payload.obj_val["devicePublicKey"] = json::Value::make_string(device_public_key_b64_);

    return build_http_action(
        PendingOp::AppleAuth,
        "POST",
        TELEFAM_OBF("/api/v1/auth/apple", 0x30),
        "",
        json::stringify(payload),
        false,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_check_username(
    const std::string& username,
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    std::string norm = lower_copy(trim_copy(username));
    profile_.username = norm;
    if (!is_valid_username(norm)) {
        profile_.username_checking = false;
        profile_.username_checked = true;
        profile_.username_available = false;
        profile_.username_status_msg = "3-24 lowercase letters, numbers or _";
        return "{\"action\":\"NONE\"}";
    }
    if (!is_online) {
        profile_.username_checking = false;
        return "{\"action\":\"NONE\"}";
    }
    profile_.username_checking = true;
    profile_.username_checked = false;
    return build_http_action(
        PendingOp::CheckUsername,
        "GET",
        TELEFAM_OBF("/api/v1/profile/check-username", 0x24),
        "username=" + norm,
        "",
        true,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_upload_photo(
    const std::string& mime_type,
    int64_t byte_size,
    double crop_x,
    double crop_y,
    double crop_w,
    double crop_h,
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate()) {
        return "{\"action\":\"NONE\"}";
    }
    if (!is_online) {
        error_message_ = "No internet connection";
        return "{\"action\":\"NONE\"}";
    }
    std::string m = lower_copy(mime_type);
    if (m != "image/jpeg" && m != "image/jpg" && m != "image/png" && m != "image/webp") {
        error_message_ = "Use JPEG, PNG or WebP photo";
        return "{\"action\":\"NONE\"}";
    }
    if (byte_size <= 0 || byte_size > 10 * 1024 * 1024) {
        error_message_ = "Photo must be under 10 MB";
        return "{\"action\":\"NONE\"}";
    }

    auto clamp01 = [](double v) { return std::max(0.0, std::min(1.0, v)); };
    json::Value extra = json::Value::make_object();
    extra.obj_val["cropX"] = json::Value::make_double(clamp01(crop_x));
    extra.obj_val["cropY"] = json::Value::make_double(clamp01(crop_y));
    extra.obj_val["cropWidth"] = json::Value::make_double(std::max(0.1, clamp01(crop_w)));
    extra.obj_val["cropHeight"] = json::Value::make_double(std::max(0.1, clamp01(crop_h)));
    extra.obj_val["mimeType"] = json::Value::make_string(m);

    profile_.photo_uploading = true;
    error_message_.clear();

    return build_http_action(
        PendingOp::UploadPhoto,
        "POST",
        TELEFAM_OBF("/api/v1/profile/photo", 0x25),
        "",
        "",
        true,
        now_ms,
        random_16,
        json::stringify(extra));
}

std::string TelefamCoreEngine::prepare_submit_profile(
    int cur_year,
    int cur_month,
    int cur_day,
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate()) {
        return "{\"action\":\"NONE\"}";
    }
    if (!is_online) {
        error_message_ = "No internet connection";
        return "{\"action\":\"NONE\"}";
    }
    std::string full_name = trim_copy(profile_.full_name);
    if (full_name.size() < 2) {
        error_message_ = "Enter your full name";
        return "{\"action\":\"NONE\"}";
    }
    if (!is_valid_username(profile_.username)) {
        error_message_ = "Choose a valid username (3-24 chars)";
        return "{\"action\":\"NONE\"}";
    }
    if (profile_.username_checked && !profile_.username_available) {
        error_message_ = "Username is already taken";
        return "{\"action\":\"NONE\"}";
    }
    auto phone_res = countries::validate_phone(profile_.phone_country_iso, profile_.phone_number);
    if (!phone_res.valid) {
        profile_.phone_error = phone_res.error_message;
        error_message_ = phone_res.error_message;
        return "{\"action\":\"NONE\"}";
    }
    if (profile_.gender.empty()) {
        error_message_ = "Select your gender";
        return "{\"action\":\"NONE\"}";
    }
    std::string dob_err;
    if (!validate_dob_at_least_13(profile_.date_of_birth, cur_year, cur_month, cur_day, dob_err)) {
        profile_.dob_error = dob_err;
        error_message_ = dob_err;
        return "{\"action\":\"NONE\"}";
    }
    std::string loc = trim_copy(profile_.location);
    if (loc.size() < 2) {
        error_message_ = "Enter your location";
        return "{\"action\":\"NONE\"}";
    }

    is_loading_ = true;
    error_message_.clear();

    json::Value payload = json::Value::make_object();
    payload.obj_val["fullName"] = json::Value::make_string(full_name);
    payload.obj_val["username"] = json::Value::make_string(profile_.username);
    payload.obj_val["phoneCountryIso"] = json::Value::make_string(profile_.phone_country_iso);
    payload.obj_val["phoneNumber"] = json::Value::make_string(phone_res.normalized_e164);
    payload.obj_val["gender"] = json::Value::make_string(profile_.gender);
    payload.obj_val["dateOfBirth"] = json::Value::make_string(profile_.date_of_birth);
    payload.obj_val["location"] = json::Value::make_string(loc);
    payload.obj_val["bio"] = json::Value::make_string(trim_copy(profile_.bio));

    return build_http_action(
        PendingOp::SubmitProfile,
        "PUT",
        TELEFAM_OBF("/api/v1/profile", 0x26),
        "",
        json::stringify(payload),
        true,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_fetch_owner_profile(
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate() || !is_online || session_.access_token.empty()) {
        return "{\"action\":\"NONE\"}";
    }
    return build_http_action(
        PendingOp::FetchProfile,
        "GET",
        TELEFAM_OBF("/api/v1/profile", 0x2A),
        "",
        "",
        true,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_sync_edited_profile(
    const std::string& profile_json,
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate() || !is_online || session_.access_token.empty()) {
        return "{\"action\":\"NONE\"}";
    }
    json::Value obj;
    if (!json::parse(profile_json, obj) || !obj.is_object()) {
        return "{\"action\":\"NONE\"}";
    }
    json::Value payload = json::Value::make_object();
    payload.obj_val["fullName"] = json::Value::make_string(trim_copy(obj.get_string("fullName")));
    payload.obj_val["username"] = json::Value::make_string(lower_copy(trim_copy(obj.get_string("username"))));
    payload.obj_val["phoneCountryIso"] = json::Value::make_string(obj.get_string("phoneCountryIso", "KE"));
    payload.obj_val["phoneNumber"] = json::Value::make_string(trim_copy(obj.get_string("phoneNumber")));
    payload.obj_val["gender"] = json::Value::make_string(obj.get_string("gender"));
    payload.obj_val["dateOfBirth"] = json::Value::make_string(obj.get_string("dateOfBirth"));
    payload.obj_val["location"] = json::Value::make_string(trim_copy(obj.get_string("location")));
    payload.obj_val["bio"] = json::Value::make_string(trim_copy(obj.get_string("bio")));

    return build_http_action(
        PendingOp::SyncEditedProfile,
        "PUT",
        TELEFAM_OBF("/api/v1/profile", 0x2B),
        "",
        json::stringify(payload),
        true,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_upload_background_photo(
    const std::string& mime_type,
    int64_t byte_size,
    double crop_x,
    double crop_y,
    double crop_w,
    double crop_h,
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate()) {
        return "{\"action\":\"NONE\"}";
    }
    if (!is_online) {
        error_message_ = "No internet connection";
        return "{\"action\":\"NONE\"}";
    }
    if (profile_.background_photos.size() >= 5) {
        error_message_ = "Maximum 5 background photos allowed";
        return "{\"action\":\"NONE\"}";
    }
    std::string m = lower_copy(mime_type);
    if (m != "image/jpeg" && m != "image/jpg" && m != "image/png" && m != "image/webp") {
        error_message_ = "Use JPEG, PNG or WebP photo";
        return "{\"action\":\"NONE\"}";
    }
    if (byte_size <= 0 || byte_size > 10 * 1024 * 1024) {
        error_message_ = "Photo must be under 10 MB";
        return "{\"action\":\"NONE\"}";
    }

    auto clamp01 = [](double v) { return std::max(0.0, std::min(1.0, v)); };
    json::Value extra = json::Value::make_object();
    extra.obj_val["cropX"] = json::Value::make_double(clamp01(crop_x));
    extra.obj_val["cropY"] = json::Value::make_double(clamp01(crop_y));
    extra.obj_val["cropWidth"] = json::Value::make_double(std::max(0.1, clamp01(crop_w)));
    extra.obj_val["cropHeight"] = json::Value::make_double(std::max(0.1, clamp01(crop_h)));
    extra.obj_val["mimeType"] = json::Value::make_string(m);

    profile_.background_uploading = true;
    error_message_.clear();

    return build_http_action(
        PendingOp::UploadBackgroundPhoto,
        "POST",
        TELEFAM_OBF("/api/v1/profile/backgrounds", 0x2C),
        "",
        "",
        true,
        now_ms,
        random_16,
        json::stringify(extra));
}

std::string TelefamCoreEngine::prepare_delete_background_photo(
    const std::string& photo_id,
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate()) {
        return "{\"action\":\"NONE\"}";
    }
    if (!is_online) {
        error_message_ = "No internet connection";
        return "{\"action\":\"NONE\"}";
    }
    if (photo_id.empty() || photo_id.size() > 64) {
        return "{\"action\":\"NONE\"}";
    }
    for (unsigned char c : photo_id) {
        if (!(std::isalnum(c) || c == '-')) {
            return "{\"action\":\"NONE\"}";
        }
    }
    profile_.background_uploading = true;
    error_message_.clear();
    std::string path = TELEFAM_OBF("/api/v1/profile/backgrounds/", 0x2D) + photo_id;
    return build_http_action(
        PendingOp::DeleteBackgroundPhoto,
        "DELETE",
        path,
        "",
        "",
        true,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_fetch_contacts(
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate() || !is_online || session_.access_token.empty()) {
        return "{\"action\":\"NONE\"}";
    }
    return build_http_action(
        PendingOp::FetchContacts,
        "GET",
        TELEFAM_OBF("/api/v1/contacts", 0x27),
        "",
        "",
        true,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_follow_action(
    const std::string& target_user_id,
    bool follow,
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate() || !is_online || session_.access_token.empty()) {
        return "{\"action\":\"NONE\"}";
    }
    if (target_user_id.empty() || target_user_id.size() > 64) {
        return "{\"action\":\"NONE\"}";
    }
    for (unsigned char c : target_user_id) {
        if (!(std::isalnum(c) || c == '-')) {
            return "{\"action\":\"NONE\"}";
        }
    }
    std::string path = TELEFAM_OBF("/api/v1/contacts/", 0x28) + target_user_id + TELEFAM_OBF("/follow", 0x29);
    return build_http_action(
        follow ? PendingOp::FollowUser : PendingOp::UnfollowUser,
        follow ? "POST" : "DELETE",
        path,
        "",
        "",
        true,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_refresh_token(
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!check_integrity_gate() || !is_online || session_.refresh_token.empty()) {
        return "{\"action\":\"NONE\"}";
    }
    json::Value payload = json::Value::make_object();
    payload.obj_val["refreshToken"] = json::Value::make_string(session_.refresh_token);
    payload.obj_val["deviceIdHash"] = json::Value::make_string(device_id_hash_);
    payload.obj_val["deviceHardwareName"] = json::Value::make_string(device_hardware_name_);
    payload.obj_val["devicePublicKey"] = json::Value::make_string(device_public_key_b64_);
    return build_http_action(
        PendingOp::RefreshToken,
        "POST",
        TELEFAM_OBF("/api/v1/auth/refresh", 0x2E),
        "",
        json::stringify(payload),
        false,
        now_ms,
        random_16);
}

std::string TelefamCoreEngine::prepare_logout(
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& random_16) {
    if (!is_online || session_.access_token.empty()) {
        return "{\"action\":\"NONE\"}";
    }
    return build_http_action(
        PendingOp::Logout,
        "POST",
        TELEFAM_OBF("/api/v1/auth/logout", 0x2F),
        "",
        "{}",
        true,
        now_ms,
        random_16);
}

void TelefamCoreEngine::on_http_result(
    const std::string& request_id,
    int status_code,
    const std::string& response_body,
    bool network_error,
    bool is_online,
    int64_t now_ms,
    const std::vector<uint8_t>& vault_nonce_12) {
    auto it = pending_ops_.find(request_id);
    if (it == pending_ops_.end()) return;
    PendingOp op = it->second;
    pending_ops_.erase(it);

    const bool silent_bg_op =
        (op == PendingOp::FetchContacts || op == PendingOp::FollowUser ||
         op == PendingOp::UnfollowUser || op == PendingOp::FetchProfile ||
         op == PendingOp::SyncEditedProfile || op == PendingOp::RefreshToken ||
         op == PendingOp::Logout);
    if (!silent_bg_op) {
        is_loading_ = false;
    }
    if (op == PendingOp::UploadPhoto) profile_.photo_uploading = false;
    if (op == PendingOp::UploadBackgroundPhoto || op == PendingOp::DeleteBackgroundPhoto) {
        profile_.background_uploading = false;
    }
    if (op == PendingOp::CheckUsername) profile_.username_checking = false;

    if (!is_online || network_error) {
        if (!silent_bg_op) {
            error_message_ = is_online
                ? sanitize_error("Network transport failure", "Error has occurred")
                : "No internet connection";
        }
        return;
    }

    json::Value resp;
    bool parsed = json::parse(response_body, resp) && resp.is_object();
    std::string server_msg = parsed ? resp.get_string("message") : "";

    if (silent_bg_op) {
        if (op == PendingOp::FollowUser && status_code == 429) {
            notice_message_ = "Error has occurred. Try again later.";
        } else if ((op == PendingOp::FetchProfile || op == PendingOp::SyncEditedProfile) &&
                   status_code >= 200 && status_code < 300 && parsed) {
            apply_profile_json_object(resp.get("profile"), now_ms);
            persist_vault(now_ms, vault_nonce_12);
        } else if (op == PendingOp::RefreshToken) {
            if (status_code >= 200 && status_code < 300 && parsed) {
                session_.access_token = resp.get_string("accessToken", session_.access_token);
                session_.refresh_token = resp.get_string("refreshToken", session_.refresh_token);
                session_.profile_completed = resp.get_bool("profileCompleted", session_.profile_completed);
                if (const json::Value* prof = resp.get("profile")) {
                    apply_profile_json_object(prof, now_ms);
                }
                persist_vault(now_ms, vault_nonce_12);
                tick(now_ms);
            } else if (status_code == 401) {
                logout(now_ms);
            }
        }
        return;
    }

    if (op == PendingOp::SendOtp) {
        if (status_code >= 200 && status_code < 300) {
            int64_t cooldown_sec = parsed ? resp.get_int("resendCooldownSeconds", 60) : 60;
            resend_available_at_ms_ = now_ms + cooldown_sec * 1000LL;
            screen_ = Screen::AuthOtp;
            otp_.clear();
            error_message_.clear();
            tick(now_ms);
        } else if (status_code == 429 && parsed) {
            int64_t lock_rem = resp.get_int("lockRemainingSeconds", 0);
            int64_t resend_rem = resp.get_int("resendRemainingSeconds", 0);
            if (lock_rem > 0) {
                locked_until_ms_ = now_ms + lock_rem * 1000LL;
                error_message_ = "Locked for " + format_mm_ss(static_cast<int>(lock_rem));
            } else if (resend_rem > 0) {
                resend_available_at_ms_ = now_ms + resend_rem * 1000LL;
                error_message_ = "Resend in " + format_mm_ss(static_cast<int>(resend_rem));
            } else {
                error_message_ = server_msg.empty() ? "Too many requests" : server_msg;
            }
            tick(now_ms);
        } else {
            error_message_ = sanitize_error(server_msg, "Error has occurred");
        }
        return;
    }

    if (op == PendingOp::VerifyOtp || op == PendingOp::GoogleAuth || op == PendingOp::AppleAuth) {
        if (status_code >= 200 && status_code < 300 && parsed) {
            session_.user_id = resp.get_string("userId");
            session_.email = resp.get_string("email", email_);
            session_.access_token = resp.get_string("accessToken");
            session_.refresh_token = resp.get_string("refreshToken");
            session_.bound_device_id_hash = device_id_hash_;
            session_.profile_completed = resp.get_bool("profileCompleted");
            email_ = session_.email;

            failed_attempts_ = 0;
            lock_cycles_ = 0;
            locked_until_ms_ = 0;
            otp_.clear();
            error_message_.clear();

            if (const json::Value* prof = resp.get("profile")) {
                apply_profile_json_object(prof, now_ms);
            }

            if (session_.profile_completed) {
                screen_ = Screen::Home;
            } else {
                apply_email_prefix_autofill();
                screen_ = Screen::ProfileSetup;
            }
            persist_vault(now_ms, vault_nonce_12);
            tick(now_ms);
        } else if (op == PendingOp::VerifyOtp && (status_code == 401 || status_code == 429) && parsed) {
            failed_attempts_ = static_cast<int>(resp.get_int("failedAttempts", failed_attempts_ + 1));
            lock_cycles_ = static_cast<int>(resp.get_int("lockCycles", lock_cycles_));
            int64_t lock_rem = resp.get_int("lockRemainingSeconds", 0);
            if (lock_rem > 0) {
                locked_until_ms_ = now_ms + lock_rem * 1000LL;
                error_message_ = "Locked for " + format_mm_ss(static_cast<int>(lock_rem));
            } else {
                error_message_ = server_msg.empty() ? "Invalid OTP code" : server_msg;
            }
            tick(now_ms);
        } else {
            error_message_ = sanitize_error(server_msg, "Error has occurred");
        }
        return;
    }

    if (op == PendingOp::CheckUsername) {
        if (status_code >= 200 && status_code < 300 && parsed) {
            profile_.username_checked = true;
            profile_.username_available = resp.get_bool("available");
            profile_.username_status_msg = profile_.username_available ? "Available" : "Taken";
        }
        return;
    }

    if (op == PendingOp::UploadPhoto) {
        if (status_code >= 200 && status_code < 300 && parsed) {
            profile_.avatar_url = resp.get_string("avatarUrl");
            profile_.avatar_fingerprint_sha256 = resp.get_string("fingerprintSha256");
            profile_.avatar_fingerprint_dhash = resp.get_string("fingerprintDhash");
            persist_vault(now_ms, vault_nonce_12);
        } else {
            error_message_ = sanitize_error(server_msg, "Error has occurred");
        }
        return;
    }

    if (op == PendingOp::UploadBackgroundPhoto || op == PendingOp::DeleteBackgroundPhoto) {
        if (status_code >= 200 && status_code < 300 && parsed) {
            if (const json::Value* bgs = resp.get("backgroundPhotos")) {
                if (bgs->is_array()) {
                    profile_.background_photos.clear();
                    for (const auto& item : bgs->arr_val) {
                        if (!item.is_object() || profile_.background_photos.size() >= 5) continue;
                        BackgroundPhotoItem bg;
                        bg.id = item.get_string("id");
                        bg.image_url = item.get_string("imageUrl");
                        bg.sha256_hex = item.get_string("fingerprintSha256");
                        bg.dhash_hex = item.get_string("fingerprintDhash");
                        bg.width = static_cast<int>(item.get_int("width", 0));
                        bg.height = static_cast<int>(item.get_int("height", 0));
                        if (!bg.id.empty() && !bg.image_url.empty()) {
                            profile_.background_photos.push_back(std::move(bg));
                        }
                    }
                }
            }
            persist_vault(now_ms, vault_nonce_12);
        } else {
            error_message_ = sanitize_error(server_msg, "Error has occurred");
        }
        return;
    }

    if (op == PendingOp::SubmitProfile) {
        if (status_code >= 200 && status_code < 300 && parsed) {
            session_.profile_completed = true;
            if (const json::Value* prof = resp.get("profile")) {
                apply_profile_json_object(prof, now_ms);
            }
            screen_ = Screen::Home;
            error_message_.clear();
            persist_vault(now_ms, vault_nonce_12);
        } else if (status_code >= 400 && status_code < 500 && !server_msg.empty()) {
            error_message_ = server_msg;
        } else {
            error_message_ = sanitize_error(server_msg, "Error has occurred");
        }
        return;
    }
}

std::string TelefamCoreEngine::get_ui_state_json() const {
    json::Value root = json::Value::make_object();

    std::string screen_str = "AUTH_EMAIL";
    if (screen_ == Screen::AuthOtp) screen_str = "AUTH_OTP";
    else if (screen_ == Screen::ProfileSetup) screen_str = "PROFILE_SETUP";
    else if (screen_ == Screen::Home) screen_str = "HOME";
    root.obj_val["screen"] = json::Value::make_string(screen_str);

    std::string theme_str = "SYSTEM";
    if (theme_mode_ == ThemeMode::Light) theme_str = "LIGHT";
    else if (theme_mode_ == ThemeMode::Dark) theme_str = "DARK";
    root.obj_val["themeMode"] = json::Value::make_string(theme_str);

    root.obj_val["email"] = json::Value::make_string(email_);
    root.obj_val["otp"] = json::Value::make_string(otp_);
    root.obj_val["isLoading"] = json::Value::make_bool(is_loading_);
    root.obj_val["errorMessage"] = json::Value::make_string(error_message_);
    root.obj_val["noticeMessage"] = json::Value::make_string(notice_message_);

    root.obj_val["resendRemainingSec"] = json::Value::make_int(resend_remaining_sec_);
    root.obj_val["resendTimerText"] = json::Value::make_string(format_mm_ss(resend_remaining_sec_));
    root.obj_val["canResend"] = json::Value::make_bool(resend_remaining_sec_ == 0 && lock_remaining_sec_ == 0);

    root.obj_val["failedAttempts"] = json::Value::make_int(failed_attempts_);
    root.obj_val["lockCycles"] = json::Value::make_int(lock_cycles_);
    root.obj_val["lockRemainingSec"] = json::Value::make_int(lock_remaining_sec_);
    root.obj_val["lockTimerText"] = json::Value::make_string(format_mm_ss(lock_remaining_sec_));

    root.obj_val["integrityCompromised"] = json::Value::make_bool(integrity_.compromised);
    root.obj_val["sealedVaultB64"] = json::Value::make_string(sealed_vault_b64_);

    json::Value prof = json::Value::make_object();
    prof.obj_val["fullName"] = json::Value::make_string(profile_.full_name);
    prof.obj_val["username"] = json::Value::make_string(profile_.username);
    prof.obj_val["usernameChecking"] = json::Value::make_bool(profile_.username_checking);
    prof.obj_val["usernameChecked"] = json::Value::make_bool(profile_.username_checked);
    prof.obj_val["usernameAvailable"] = json::Value::make_bool(profile_.username_available);
    prof.obj_val["usernameStatusMsg"] = json::Value::make_string(profile_.username_status_msg);

    const auto* c_rule = countries::find_by_iso2(profile_.phone_country_iso);
    prof.obj_val["phoneCountryIso"] = json::Value::make_string(profile_.phone_country_iso);
    prof.obj_val["phoneDialCode"] = json::Value::make_string(c_rule ? ("+" + c_rule->dial_code) : "+254");
    prof.obj_val["phoneCountryName"] = json::Value::make_string(c_rule ? c_rule->name : "Kenya");
    prof.obj_val["phoneNumber"] = json::Value::make_string(profile_.phone_number);
    prof.obj_val["phoneError"] = json::Value::make_string(profile_.phone_error);

    prof.obj_val["gender"] = json::Value::make_string(profile_.gender);
    prof.obj_val["dateOfBirth"] = json::Value::make_string(profile_.date_of_birth);
    prof.obj_val["dobError"] = json::Value::make_string(profile_.dob_error);
    prof.obj_val["location"] = json::Value::make_string(profile_.location);
    prof.obj_val["bio"] = json::Value::make_string(profile_.bio);
    prof.obj_val["avatarUrl"] = json::Value::make_string(profile_.avatar_url);
    prof.obj_val["avatarFingerprintSha256"] = json::Value::make_string(profile_.avatar_fingerprint_sha256);
    prof.obj_val["photoUploading"] = json::Value::make_bool(profile_.photo_uploading);
    prof.obj_val["usernameChangedAtEpochSec"] = json::Value::make_int(profile_.username_changed_at_sec);
    prof.obj_val["nextUsernameChangeAtEpochSec"] = json::Value::make_int(profile_.next_username_change_at_sec);
    prof.obj_val["canChangeUsername"] = json::Value::make_bool(profile_.can_change_username);
    prof.obj_val["followersCount"] = json::Value::make_int(profile_.followers_count);
    prof.obj_val["followingCount"] = json::Value::make_int(profile_.following_count);
    prof.obj_val["likesCount"] = json::Value::make_int(profile_.likes_count);
    prof.obj_val["postsCount"] = json::Value::make_int(profile_.posts_count);
    prof.obj_val["backgroundUploading"] = json::Value::make_bool(profile_.background_uploading);

    json::Value bg_arr = json::Value::make_array();
    for (const auto& bg : profile_.background_photos) {
        json::Value o = json::Value::make_object();
        o.obj_val["id"] = json::Value::make_string(bg.id);
        o.obj_val["imageUrl"] = json::Value::make_string(bg.image_url);
        o.obj_val["fingerprintSha256"] = json::Value::make_string(bg.sha256_hex);
        o.obj_val["fingerprintDhash"] = json::Value::make_string(bg.dhash_hex);
        o.obj_val["width"] = json::Value::make_int(bg.width);
        o.obj_val["height"] = json::Value::make_int(bg.height);
        bg_arr.arr_val.push_back(std::move(o));
    }
    prof.obj_val["backgroundPhotos"] = std::move(bg_arr);

    std::string home_tab_str = "CHATS";
    if (home_tab_ == HomeTab::Contacts) home_tab_str = "CONTACTS";
    else if (home_tab_ == HomeTab::Profile) home_tab_str = "PROFILE";
    root.obj_val["userId"] = json::Value::make_string(session_.user_id);
    root.obj_val["homeTab"] = json::Value::make_string(home_tab_str);

    std::string ptab_str = "POSTS";
    switch (profile_content_tab_) {
        case ProfileContentTab::Saved: ptab_str = "SAVED"; break;
        case ProfileContentTab::Reposts: ptab_str = "REPOSTS"; break;
        case ProfileContentTab::Locked: ptab_str = "LOCKED"; break;
        default: ptab_str = "POSTS"; break;
    }
    root.obj_val["profileContentTab"] = json::Value::make_string(ptab_str);

    std::string ctab_str = "ALL";
    switch (contacts_tab_) {
        case ContactsTab::Suggestions: ctab_str = "SUGGESTIONS"; break;
        case ContactsTab::Followers: ctab_str = "FOLLOWERS"; break;
        case ContactsTab::Following: ctab_str = "FOLLOWING"; break;
        case ContactsTab::Friends: ctab_str = "FRIENDS"; break;
        default: ctab_str = "ALL"; break;
    }
    root.obj_val["contactsTab"] = json::Value::make_string(ctab_str);
    root.obj_val["contactsSearchQuery"] = json::Value::make_string(contacts_search_query_);

    root.obj_val["profile"] = prof;
    return json::stringify(root);
}

std::string TelefamCoreEngine::get_countries_json() const {
    json::Value arr = json::Value::make_array();
    for (const auto& c : countries::all_countries()) {
        json::Value obj = json::Value::make_object();
        obj.obj_val["iso2"] = json::Value::make_string(c.iso2);
        obj.obj_val["name"] = json::Value::make_string(c.name);
        obj.obj_val["dialCode"] = json::Value::make_string("+" + c.dial_code);
        obj.obj_val["placeholder"] = json::Value::make_string(c.placeholder);
        arr.arr_val.push_back(std::move(obj));
    }
    return json::stringify(arr);
}

std::string TelefamCoreEngine::parse_contacts_sync_json(const std::string& server_response_json) const {
    json::Value root;
    if (!json::parse(server_response_json, root) || !root.is_object()) {
        return "[]";
    }
    const json::Value* arr = root.get("contacts");
    if (!arr || !arr->is_array()) {
        return "[]";
    }
    json::Value out = json::Value::make_array();
    for (const auto& item : arr->arr_val) {
        if (!item.is_object()) continue;
        std::string uid = item.get_string("userId");
        std::string uname = item.get_string("username");
        if (uid.empty() || uname.empty()) continue;
        std::string iso = item.get_string("countryIso", "KE");
        std::string cname = item.get_string("countryName");
        if (cname.empty()) {
            const auto* rule = countries::find_by_iso2(iso);
            cname = rule ? rule->name : "Kenya";
        }
        bool i_follow = item.get_bool("iFollow", false);
        bool follows_me = item.get_bool("followsMe", false);
        json::Value normalized = json::Value::make_object();
        normalized.obj_val["userId"] = json::Value::make_string(uid);
        normalized.obj_val["fullName"] = json::Value::make_string(item.get_string("fullName"));
        normalized.obj_val["username"] = json::Value::make_string(uname);
        normalized.obj_val["countryIso"] = json::Value::make_string(iso);
        normalized.obj_val["countryName"] = json::Value::make_string(cname);
        normalized.obj_val["avatarUrl"] = json::Value::make_string(item.get_string("avatarUrl"));
        normalized.obj_val["iFollow"] = json::Value::make_bool(i_follow);
        normalized.obj_val["followsMe"] = json::Value::make_bool(follows_me);
        normalized.obj_val["isFriend"] = json::Value::make_bool(i_follow && follows_me);
        out.arr_val.push_back(std::move(normalized));
    }
    return json::stringify(out);
}

std::string TelefamCoreEngine::filter_contacts_json(const std::string& contacts_array_json) const {
    json::Value arr;
    if (!json::parse(contacts_array_json, arr) || !arr.is_array()) {
        return "[]";
    }
    std::string q = lower_copy(trim_copy(contacts_search_query_));
    if (!q.empty() && q[0] == '@') {
        q = q.substr(1);
    }

    std::vector<json::Value> matched;
    for (const auto& item : arr.arr_val) {
        if (!item.is_object()) continue;
        bool i_follow = item.get_bool("iFollow", false);
        bool follows_me = item.get_bool("followsMe", false);

        bool tab_ok = true;
        switch (contacts_tab_) {
            case ContactsTab::Suggestions: tab_ok = (!i_follow && !follows_me); break;
            case ContactsTab::Followers: tab_ok = follows_me; break;
            case ContactsTab::Following: tab_ok = i_follow; break;
            case ContactsTab::Friends: tab_ok = (i_follow && follows_me); break;
            case ContactsTab::All: default: tab_ok = true; break;
        }
        if (!tab_ok) continue;

        if (!q.empty()) {
            std::string fn = lower_copy(item.get_string("fullName"));
            std::string un = lower_copy(item.get_string("username"));
            std::string cn = lower_copy(item.get_string("countryName"));
            if (fn.find(q) == std::string::npos &&
                un.find(q) == std::string::npos &&
                cn.find(q) == std::string::npos) {
                continue;
            }
        }
        matched.push_back(item);
    }

    std::sort(matched.begin(), matched.end(), [](const json::Value& a, const json::Value& b) {
        std::string an = lower_copy(a.get_string("fullName"));
        std::string bn = lower_copy(b.get_string("fullName"));
        if (an != bn) return an < bn;
        return lower_copy(a.get_string("username")) < lower_copy(b.get_string("username"));
    });

    json::Value out = json::Value::make_array();
    out.arr_val = std::move(matched);
    return json::stringify(out);
}

}  // namespace telefam::core
