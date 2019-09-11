package piuk.blockchain.androidcore.utils

import android.content.SharedPreferences
import android.support.annotation.VisibleForTesting
import info.blockchain.balance.CryptoCurrency

interface UUIDGenerator {
    fun generateUUID(): String
}

class PrefsUtil(
    private val store: SharedPreferences,
    private val idGenerator: DeviceIdGenerator,
    private val uuidGenerator: UUIDGenerator
) : PersistentPrefs {

    override val deviceId: String
        get() {
            return if (qaRandomiseDeviceId) {
                uuidGenerator.generateUUID()
            } else {
                var deviceId = getValue(KEY_PRE_IDV_DEVICE_ID, "")
                if (deviceId.isEmpty()) {
                    deviceId = idGenerator.generateId()
                    setValue(KEY_PRE_IDV_DEVICE_ID, deviceId)
                }
                deviceId
            }
        }

    override var devicePreIDVCheckFailed: Boolean
        get() = getValue(KEY_PRE_IDV_FAILED, false)
        set(value) = setValue(KEY_PRE_IDV_FAILED, value)

    override var isOnboardingComplete: Boolean
        get() = getValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, false)
        set(completed) = setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, completed)

    override val isLoggedOut: Boolean
        get() = getValue(KEY_LOGGED_OUT, true)

    override var qaRandomiseDeviceId: Boolean
        get() = getValue(KEY_IS_DEVICE_ID_RANDOMISED, false)
        set(value) = setValue(KEY_IS_DEVICE_ID_RANDOMISED, value)

    // Settings - TODO: Move to interface
    override var disableRootedWarning: Boolean
        get() = getValue(PersistentPrefs.KEY_ROOT_WARNING_DISABLED, false)
        set(v) = setValue(PersistentPrefs.KEY_ROOT_WARNING_DISABLED, v)

    // From CurrencyPrefs
    override var selectedFiatCurrency: String
        get() = getValue(KEY_SELECTED_FIAT, DEFAULT_FIAT_CURRENCY)
        set(fiat) {
            setValue(KEY_SELECTED_FIAT, fiat)
        }

    override var selectedCryptoCurrency: CryptoCurrency
        get() =
            try {
                CryptoCurrency.valueOf(getValue(KEY_SELECTED_CRYPTO, DEFAULT_CRYPTO_CURRENCY.name))
            } catch (e: IllegalArgumentException) {
                removeValue(KEY_SELECTED_CRYPTO)
                DEFAULT_CRYPTO_CURRENCY
            }
        set(crypto) = setValue(KEY_SELECTED_CRYPTO, crypto.name)

    // From ThePitLinkingPrefs
    override var pitToWalletLinkId: String
        get() = getValue(KEY_PIT_LINKING_LINK_ID, "")
        set(v) = setValue(KEY_PIT_LINKING_LINK_ID, v)

    override fun clearPitToWalletLinkId() {
        removeValue(KEY_PIT_LINKING_LINK_ID)
    }

    // From Onboarding
    override var swapIntroCompleted: Boolean
        get() = getValue(KEY_SWAP_INTRO_COMPLETED, false)
        set(v) = setValue(KEY_SWAP_INTRO_COMPLETED, v)

    // Wallet Status
    override var lastBackupTime: Long
        get() = getValue(BACKUP_DATE_KEY, 0L)
        set(v) = setValue(BACKUP_DATE_KEY, v)

    override val isWalletBackedUp: Boolean
        get() = lastBackupTime != 0L

    override val isWalletFunded: Boolean
        get() = getValue(WALLET_FUNDED_KEY, false)

    override fun setWalletFunded() = setValue(WALLET_FUNDED_KEY, true)

    override var lastSwapTime: Long
        get() = getValue(SWAP_DATE_KEY, 0L)
        set(v) = setValue(SWAP_DATE_KEY, v)

    override val hasSwapped: Boolean
        get() = lastSwapTime != 0L

    override val hasMadeBitPayTransaction: Boolean
        get() = getValue(BITPAY_TRANSACTION_SUCCEEDED, false)

    override fun setBitPaySuccess() = setValue(BITPAY_TRANSACTION_SUCCEEDED, true)

    // Raw accessors
    override fun getValue(name: String): String? =
        store.getString(name, null)

    override fun getValue(name: String, defaultValue: String): String =
        store.getString(name, defaultValue) ?: ""

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
        store.edit().putInt(name, if (value < 0) 0 else value).apply()
    }

    override fun setValue(name: String, value: Long) {
        store.edit().putLong(name, if (value < 0L) 0L else value).apply()
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

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DEFAULT_FIAT_CURRENCY = "USD"
        val DEFAULT_CRYPTO_CURRENCY = CryptoCurrency.BTC

        const val KEY_PRE_IDV_FAILED = "pre_idv_check_failed"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_SELECTED_FIAT = "ccurrency" // Historical misspelling, don't update
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_PRE_IDV_DEVICE_ID = "pre_idv_device_id"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_LOGGED_OUT = "logged_out"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_SELECTED_CRYPTO = "KEY_CURRENCY_CRYPTO_STATE"

        private const val KEY_PIT_LINKING_LINK_ID = "pit_wallet_link_id"

        private const val KEY_SWAP_INTRO_COMPLETED = "key_swap_intro_completed"

        private const val BACKUP_DATE_KEY = "BACKUP_DATE_KEY"
        private const val SWAP_DATE_KEY = "SWAP_DATE_KEY"
        private const val WALLET_FUNDED_KEY = "WALLET_FUNDED_KEY"
        private const val BITPAY_TRANSACTION_SUCCEEDED = "BITPAY_TRANSACTION_SUCCEEDED"
        // For QA:
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_IS_DEVICE_ID_RANDOMISED = "random_device_id"
    }
}
