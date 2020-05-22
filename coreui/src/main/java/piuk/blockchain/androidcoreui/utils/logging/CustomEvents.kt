package piuk.blockchain.androidcoreui.utils.logging

import com.crashlytics.android.answers.CustomEvent

fun RecoverWalletEvent1(successful: Boolean) =
    LoggingEvent("Recover Wallet", Pair("Success", successful))

class RecoverWalletEvent : CustomEvent("Recover Wallet") {

    fun putSuccess(successful: Boolean): RecoverWalletEvent {
        putCustomAttribute("Success", if (successful) "true" else "false")
        return this
    }
}

fun PairingEvent1(pairingMethod: PairingMethod) =
    LoggingEvent("Wallet Pairing", Pair("Pairing method", pairingMethod.name))

class PairingEvent : CustomEvent("Wallet Pairing") {

    fun putSuccess(successful: Boolean): PairingEvent {
        putCustomAttribute("Success", if (successful) "true" else "false")
        return this
    }

    fun putMethod(pairingMethod: PairingMethod): PairingEvent {
        putCustomAttribute("Pairing method", pairingMethod.name)
        return this
    }
}

@Suppress("UNUSED_PARAMETER")
enum class PairingMethod(name: String) {
    MANUAL("Manual"),
    QR_CODE("Qr code"),
    REVERSE("Reverse")
}

fun ImportEvent1(addressType: AddressType) =
    LoggingEvent("Address Imported", Pair("Address Type", addressType.name))

class ImportEvent(addressType: AddressType) : CustomEvent("Address Imported") {

    init {
        putCustomAttribute("Address Type", addressType.name)
    }
}

@Suppress("UNUSED_PARAMETER")
enum class AddressType(name: String) {
    PRIVATE_KEY("Private key"),
    WATCH_ONLY("Watch Only")
}


fun CreateAccountEvent1(number: Int) =
    LoggingEvent("Account Created", Pair("Number of Accounts", number))
class CreateAccountEvent(number: Int) : CustomEvent("Account Created") {

    init {
        putCustomAttribute("Number of Accounts", number)
    }
}

fun AppLaunchEvent1(playServicesFound: Boolean) =
    LoggingEvent("App Launched",
        Pair("Play Services found", playServicesFound))


fun SecondPasswordEvent1(secondPasswordEnabled: Boolean) =
    LoggingEvent("Second password event",
        Pair("Second password enabled", secondPasswordEnabled))
class SecondPasswordEvent(secondPasswordEnabled: Boolean) : CustomEvent("Second password event") {

    init {
        putCustomAttribute(
            "Second password enabled",
            if (secondPasswordEnabled) "true" else "false"
        )
    }
}

fun LauncherShortcutEvent1(type: String) =
    LoggingEvent("Launcher Shortcut", Pair("Launcher Shortcut used", type))
class LauncherShortcutEvent(type: String) : CustomEvent("Launcher Shortcut") {

    init {
        putCustomAttribute("Launcher Shortcut used", type)
    }
}

fun WalletUpgradeEvent1(successful: String) =
    LoggingEvent("Wallet Upgraded", Pair("Successful", successful))
class WalletUpgradeEvent(successful: Boolean) : CustomEvent("Wallet Upgraded") {

    init {
        putCustomAttribute("Successful", if (successful) "true" else "false")
    }
}
