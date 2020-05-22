package piuk.blockchain.androidcoreui.utils.logging

fun RecoverWalletEvent1(successful: Boolean) =
    LoggingEvent("Recover Wallet", mapOf(Pair("Success", successful)))

fun PairingEvent1(pairingMethod: PairingMethod) =
    LoggingEvent("Wallet Pairing", mapOf(Pair("Pairing method", pairingMethod.name)))

@Suppress("UNUSED_PARAMETER")
enum class PairingMethod(name: String) {
    MANUAL("Manual"),
    QR_CODE("Qr code"),
    REVERSE("Reverse")
}

fun ImportEvent1(addressType: AddressType) =
    LoggingEvent("Address Imported", mapOf(Pair("Address Type", addressType.name)))

@Suppress("UNUSED_PARAMETER")
enum class AddressType(name: String) {
    PRIVATE_KEY("Private key"),
    WATCH_ONLY("Watch Only")
}

fun CreateAccountEvent1(number: Int) =
    LoggingEvent("Account Created", mapOf(Pair("Number of Accounts", number)))


fun AppLaunchEvent1(playServicesFound: Boolean) =
    LoggingEvent("App Launched",
        mapOf(Pair("Play Services found", playServicesFound)))

fun SecondPasswordEvent1(secondPasswordEnabled: Boolean) =
    LoggingEvent("Second password event",
        mapOf(Pair("Second password enabled", secondPasswordEnabled)))

fun LauncherShortcutEvent1(type: String) =
    LoggingEvent("Launcher Shortcut", mapOf(Pair("Launcher Shortcut used", type)))

fun WalletUpgradeEvent1(successful: Boolean) =
    LoggingEvent("Wallet Upgraded", mapOf(Pair("Successful", successful)))