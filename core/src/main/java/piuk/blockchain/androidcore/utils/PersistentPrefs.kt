package piuk.blockchain.androidcore.utils

import com.blockchain.preferences.CurrencyPrefs

interface PersistentPrefs :
    CurrencyPrefs {
    val isLoggedOut: Boolean
    var isOnboardingComplete: Boolean

    val deviceId: String // Pre-IDV device identifier
    var devicePreIDVCheckFailed: Boolean // Pre-IDV check has failed! Don't show 'gold' announce cards etc

    fun getValue(name: String): String?
    fun getValue(name: String, defaultValue: String): String
    fun getValue(name: String, defaultValue: Int): Int
    fun getValue(name: String, defaultValue: Long): Long
    fun getValue(name: String, defaultValue: Boolean): Boolean

    fun setValue(name: String, value: String)
    fun setValue(name: String, value: Int)
    fun setValue(name: String, value: Long)
    fun setValue(name: String, value: Boolean)

    fun has(name: String): Boolean
    fun removeValue(name: String)

    fun clear()

    fun logOut()

    fun logIn()

    // Allow QA to randomise device ids when testing kyc
    var qaRandomiseDeviceId: Boolean

    companion object {

        const val KEY_PIN_IDENTIFIER = "pin_kookup_key"
        const val KEY_ENCRYPTED_PASSWORD = "encrypted_password"
        const val KEY_WALLET_GUID = "guid"
        const val KEY_SHARED_KEY = "sharedKey"
        const val KEY_PIN_FAILS = "pin_fails"

        const val KEY_EMAIL = "email"
        const val KEY_EMAIL_VERIFIED = "code_verified"
        const val KEY_SCHEME_URL = "scheme_url"
        const val KEY_METADATA_URI = "metadata_uri"
        const val KEY_NEWLY_CREATED_WALLET = "newly_created_wallet"
        const val KEY_SECURITY_TIME_ELAPSED = "security_time_elapsed"
        const val KEY_SECURITY_TWO_FA_NEVER = "security_two_fa_never"
        const val KEY_SECURITY_BACKUP_NEVER = "security_backup_never"
        const val KEY_ENCRYPTED_PIN_CODE = "encrypted_pin_code"
        const val KEY_FINGERPRINT_ENABLED = "fingerprint_enabled"
        const val KEY_RECEIVE_SHORTCUTS_ENABLED = "receive_shortcuts_enabled"
        const val KEY_FIREBASE_TOKEN = "firebase_token"
        const val KEY_SWIPE_TO_RECEIVE_ENABLED = "swipe_to_receive_enabled"
        const val KEY_APP_VISITS = "app_visits"
        const val KEY_SCREENSHOTS_ENABLED = "screenshots_enabled"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete_1"
        const val KEY_OVERLAY_TRUSTED = "overlay_trusted"
        const val KEY_PUSH_NOTIFICATION_ENABLED = "push_notification_enabled"

        const val KEY_LATEST_ANNOUNCEMENT_DISMISSED = "latest_announcement_dismissed"
        const val KEY_LATEST_ANNOUNCEMENT_SEEN = "latest_announcement_seen"

        // Send screen
        const val KEY_WARN_ADVANCED_FEE = "pref_warn_advanced_fee"
        const val KEY_WARN_WATCH_ONLY_SPEND = "pref_warn_watch_only_spend"
    }
}
