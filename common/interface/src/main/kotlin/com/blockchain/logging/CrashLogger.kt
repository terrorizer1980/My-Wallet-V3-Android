package com.blockchain.logging

interface CrashLogger {
    fun init(ctx: Any)
    fun log(msg: String) // Log something for crash debugging context
    fun logException(throwable: Throwable) // Log non-fatal exception catchLes

    // Log various app state information. Extend this as required
    fun onlineState(isOnline: Boolean)
    fun userLanguageLocale(locale: String)

    val isDebugBuild: Boolean
}