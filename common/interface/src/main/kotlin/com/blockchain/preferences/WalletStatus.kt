package com.blockchain.preferences

interface WalletStatus {
    var lastBackupTime: Long // Seconds since epoch
    val isWalletBackedUp: Boolean

    val isWalletFunded: Boolean
    fun setWalletFunded()

    var lastSwapTime: Long
    val hasSwapped: Boolean

    val hasMadeBitPayTransaction: Boolean
    fun setBitPaySuccess()
}