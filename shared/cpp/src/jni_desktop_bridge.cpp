#include <jni.h>
#include <mutex>
#include <string>
#include <vector>

#include "telefam_core_engine.hpp"

namespace {

std::mutex g_desktop_engine_mutex;
telefam::core::TelefamCoreEngine g_desktop_engine;

std::string jstring_to_std(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    if (!chars) return "";
    std::string out(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return out;
}

std::vector<uint8_t> jbytearray_to_vec(JNIEnv* env, jbyteArray arr) {
    if (!arr) return {};
    jsize len = env->GetArrayLength(arr);
    if (len <= 0) return {};
    std::vector<uint8_t> out(static_cast<size_t>(len));
    env->GetByteArrayRegion(arr, 0, len, reinterpret_cast<jbyte*>(out.data()));
    return out;
}

jstring std_to_jstring(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

}  // namespace

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeInit(
    JNIEnv* env,
    jobject /* thiz */,
    jstring baseUrl,
    jboolean isDebugBuild,
    jstring hardwareName,
    jstring deviceIdHash,
    jstring devicePublicKeyB64,
    jstring runtimeCertSha256,
    jstring expectedReleaseCertSha256,
    jbyteArray masterKey,
    jstring sealedVaultB64,
    jlong nowMs) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.initialize(
        jstring_to_std(env, baseUrl),
        isDebugBuild == JNI_TRUE,
        jstring_to_std(env, hardwareName),
        jstring_to_std(env, deviceIdHash),
        jstring_to_std(env, devicePublicKeyB64),
        jstring_to_std(env, runtimeCertSha256),
        jstring_to_std(env, expectedReleaseCertSha256),
        jbytearray_to_vec(env, masterKey),
        jstring_to_std(env, sealedVaultB64),
        static_cast<int64_t>(nowMs));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeSetEmail(
    JNIEnv* env, jobject /* thiz */, jstring email) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.set_email(jstring_to_std(env, email));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeSetOtp(
    JNIEnv* env, jobject /* thiz */, jstring otp) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.set_otp(jstring_to_std(env, otp));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeGoBackToEmail(
    JNIEnv* env, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.go_back_to_email();
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeUpdateProfileField(
    JNIEnv* env, jobject /* thiz */,
    jstring field, jstring value, jint curYear, jint curMonth, jint curDay) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.update_profile_field(
        jstring_to_std(env, field),
        jstring_to_std(env, value),
        static_cast<int>(curYear),
        static_cast<int>(curMonth),
        static_cast<int>(curDay));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeTick(
    JNIEnv* env, jobject /* thiz */, jlong nowMs) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.tick(static_cast<int64_t>(nowMs));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeCycleTheme(
    JNIEnv* env, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.cycle_theme_mode();
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeComingSoon(
    JNIEnv* env, jobject /* thiz */, jstring feature) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.set_coming_soon_notice(jstring_to_std(env, feature));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeSetError(
    JNIEnv* env, jobject /* thiz */, jstring rawError) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.set_error(jstring_to_std(env, rawError));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeClearError(
    JNIEnv* env, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.clear_error();
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeClearNotice(
    JNIEnv* env, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.clear_notice();
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeLogout(
    JNIEnv* env, jobject /* thiz */, jlong nowMs) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.logout(static_cast<int64_t>(nowMs));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareSendOtp(
    JNIEnv* env, jobject /* thiz */, jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_send_otp(
        isOnline == JNI_TRUE, static_cast<int64_t>(nowMs), jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareVerifyOtp(
    JNIEnv* env, jobject /* thiz */, jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_verify_otp(
        isOnline == JNI_TRUE, static_cast<int64_t>(nowMs), jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareGoogleAuth(
    JNIEnv* env, jobject /* thiz */, jstring idToken, jstring nonce, jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_google_auth(
        jstring_to_std(env, idToken),
        jstring_to_std(env, nonce),
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareAppleAuth(
    JNIEnv* env, jobject /* thiz */,
    jstring identityToken, jstring authorizationCode, jstring nonce,
    jstring fullName, jstring emailHint,
    jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_apple_auth(
        jstring_to_std(env, identityToken),
        jstring_to_std(env, authorizationCode),
        jstring_to_std(env, nonce),
        jstring_to_std(env, fullName),
        jstring_to_std(env, emailHint),
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareCheckUsername(
    JNIEnv* env, jobject /* thiz */, jstring username, jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_check_username(
        jstring_to_std(env, username),
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareUploadPhoto(
    JNIEnv* env, jobject /* thiz */,
    jstring mimeType, jlong byteSize,
    jdouble cropX, jdouble cropY, jdouble cropW, jdouble cropH,
    jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_upload_photo(
        jstring_to_std(env, mimeType),
        static_cast<int64_t>(byteSize),
        static_cast<double>(cropX),
        static_cast<double>(cropY),
        static_cast<double>(cropW),
        static_cast<double>(cropH),
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareSubmitProfile(
    JNIEnv* env, jobject /* thiz */,
    jint curYear, jint curMonth, jint curDay,
    jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_submit_profile(
        static_cast<int>(curYear),
        static_cast<int>(curMonth),
        static_cast<int>(curDay),
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeOnHttpResult(
    JNIEnv* env, jobject /* thiz */,
    jstring requestId, jint statusCode, jstring responseBody,
    jboolean networkError, jboolean isOnline, jlong nowMs, jbyteArray vaultNonce12) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.on_http_result(
        jstring_to_std(env, requestId),
        static_cast<int>(statusCode),
        jstring_to_std(env, responseBody),
        networkError == JNI_TRUE,
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, vaultNonce12));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeGetUiState(
    JNIEnv* env, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeGetCountries(
    JNIEnv* env, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.get_countries_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeSetHomeTab(
    JNIEnv* env, jobject /* thiz */, jstring tab) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.set_home_tab(jstring_to_std(env, tab));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeSetContactsTab(
    JNIEnv* env, jobject /* thiz */, jstring tab) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.set_contacts_tab(jstring_to_std(env, tab));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeSetContactsSearch(
    JNIEnv* env, jobject /* thiz */, jstring query) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.set_contacts_search(jstring_to_std(env, query));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jboolean JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeCheckAndRecordFollowRate(
    JNIEnv* env, jobject /* thiz */, jlong nowMs) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return g_desktop_engine.check_and_record_follow_rate(static_cast<int64_t>(nowMs)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareFetchContacts(
    JNIEnv* env, jobject /* thiz */, jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_fetch_contacts(
        isOnline == JNI_TRUE, static_cast<int64_t>(nowMs), jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareFollowAction(
    JNIEnv* env, jobject /* thiz */,
    jstring targetUserId, jboolean follow, jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_follow_action(
        jstring_to_std(env, targetUserId),
        follow == JNI_TRUE,
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeFilterContacts(
    JNIEnv* env, jobject /* thiz */, jstring contactsArrayJson) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.filter_contacts_json(jstring_to_std(env, contactsArrayJson)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeParseContactsSyncPayload(
    JNIEnv* env, jobject /* thiz */, jstring serverResponseJson) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.parse_contacts_sync_json(jstring_to_std(env, serverResponseJson)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeSetProfileContentTab(
    JNIEnv* env, jobject /* thiz */, jstring tab) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.set_profile_content_tab(jstring_to_std(env, tab));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jboolean JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeApplyLocalProfileEdit(
    JNIEnv* env, jobject /* thiz */,
    jstring editJson, jint curYear, jint curMonth, jint curDay, jlong nowMs) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return g_desktop_engine.apply_local_profile_edit_json(
        jstring_to_std(env, editJson),
        static_cast<int>(curYear),
        static_cast<int>(curMonth),
        static_cast<int>(curDay),
        static_cast<int64_t>(nowMs)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeApplyCachedOwnerProfile(
    JNIEnv* env, jobject /* thiz */, jstring cachedJson, jlong nowMs) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    g_desktop_engine.apply_cached_owner_profile_json(jstring_to_std(env, cachedJson), static_cast<int64_t>(nowMs));
    return std_to_jstring(env, g_desktop_engine.get_ui_state_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativeSerializeOwnerProfile(
    JNIEnv* env, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.serialize_owner_profile_json());
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareFetchOwnerProfile(
    JNIEnv* env, jobject /* thiz */, jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_fetch_owner_profile(
        isOnline == JNI_TRUE, static_cast<int64_t>(nowMs), jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareSyncEditedProfile(
    JNIEnv* env, jobject /* thiz */, jstring profileJson, jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_sync_edited_profile(
        jstring_to_std(env, profileJson),
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareUploadBackgroundPhoto(
    JNIEnv* env, jobject /* thiz */,
    jstring mimeType, jlong byteSize,
    jdouble cropX, jdouble cropY, jdouble cropW, jdouble cropH,
    jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_upload_background_photo(
        jstring_to_std(env, mimeType),
        static_cast<int64_t>(byteSize),
        static_cast<double>(cropX),
        static_cast<double>(cropY),
        static_cast<double>(cropW),
        static_cast<double>(cropH),
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareDeleteBackgroundPhoto(
    JNIEnv* env, jobject /* thiz */,
    jstring photoId, jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_delete_background_photo(
        jstring_to_std(env, photoId),
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareRefreshToken(
    JNIEnv* env, jobject /* thiz */,
    jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_refresh_token(
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

JNIEXPORT jstring JNICALL
Java_com_telefam_company_desktop_nativecore_DesktopNativeCore_nativePrepareLogout(
    JNIEnv* env, jobject /* thiz */,
    jboolean isOnline, jlong nowMs, jbyteArray random16) {
    std::lock_guard<std::mutex> lock(g_desktop_engine_mutex);
    return std_to_jstring(env, g_desktop_engine.prepare_logout(
        isOnline == JNI_TRUE,
        static_cast<int64_t>(nowMs),
        jbytearray_to_vec(env, random16)));
}

}  // extern "C"
