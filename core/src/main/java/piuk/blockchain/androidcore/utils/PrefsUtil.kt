package piuk.blockchain.androidcore.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class PrefsUtil(
    context: Context,
    private val idGenerator: DeviceIdGenerator
) : PersistentPrefs {

    private val store: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    override val deviceId: String
        get() {
            var deviceId = getValue(KEY_PRE_IDV_DEVICE_ID, "")
            if (deviceId.isEmpty()) {
                deviceId = idGenerator.generateId()
                setValue(KEY_PRE_IDV_DEVICE_ID, deviceId)
            }
            return deviceId
        }

    override var isOnboardingComplete: Boolean
        get() = getValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, false)
        set(completed) = setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, completed)

    override val isLoggedOut: Boolean
        get() = getValue(KEY_LOGGED_OUT, true)

    override val selectedFiatCurrency: String
        get() = getValue(KEY_SELECTED_FIAT, DEFAULT_CURRENCY)

    override fun getValue(name: String): String? =
        store.getString(name, null)

    override fun getValue(name: String, defaultValue: String): String =
        store.getString(name, defaultValue)

    override fun getValue(name: String, defaultValue: Int): Int =
        store.getInt(name, defaultValue)

    override fun getValue(name: String, defaultValue: Long): Long =
        try {
            store.getLong(name, defaultValue)
        } catch (e: Exception) {
            store.getInt(name, defaultValue.toInt()).toLong()
        }

    override fun getValue(name: String, defaultValue: Boolean): Boolean =
        store.getBoolean(name, defaultValue)

    override fun setValue(name: String, value: String) {
        store.edit().putString(name, value).apply()
    }

    override fun setValue(name: String, value: Int) {
        val editor = store.edit()
        editor.putInt(name, if (value < 0) 0 else value)
        editor.apply()
    }

    override fun setValue(name: String, value: Long) {
        val editor = store.edit()
        editor.putLong(name, if (value < 0L) 0L else value)
        editor.apply()
    }

    override fun setValue(name: String, value: Boolean) {
        store.edit().putBoolean(name, value).apply()
    }

    override fun has(name: String): Boolean = store.contains(name)

    override fun removeValue(name: String) {
        store.edit().remove(name).apply()
    }

    override fun clear() {
        store.edit().clear().apply()
    }

    /**
     * Clears everything but the GUID for logging back in and the deviceId - for pre-IDV checking
     */
    override fun logOut() {
        val guid = getValue(PersistentPrefs.KEY_WALLET_GUID, "")
        val notificationsToken = getValue(PersistentPrefs.KEY_FIREBASE_TOKEN, "")
        val deviceId = getValue(KEY_PRE_IDV_DEVICE_ID, "")

        clear()

        setValue(KEY_LOGGED_OUT, true)
        setValue(PersistentPrefs.KEY_WALLET_GUID, guid)
        setValue(PersistentPrefs.KEY_FIREBASE_TOKEN, notificationsToken)
        setValue(KEY_PRE_IDV_DEVICE_ID, deviceId)
    }

    /**
     * Reset value once user logged in
     */
    override fun logIn() {
        setValue(KEY_LOGGED_OUT, false)
    }

    override fun setSelectedFiatCurrency(fiat: String) {
        setValue(KEY_SELECTED_FIAT, fiat)
    }

    companion object {
        private const val DEFAULT_CURRENCY = "USD"
        private const val KEY_SELECTED_FIAT = "ccurrency"
        private const val KEY_PRE_IDV_DEVICE_ID = "pre_idv_device_id"
        private const val KEY_LOGGED_OUT = "logged_out"
    }
}
