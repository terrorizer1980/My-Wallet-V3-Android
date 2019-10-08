package com.blockchain.preferences

interface SecurityPrefs {
    var disableRootedWarning: Boolean
    var trustScreenOverlay: Boolean
    val areScreenshotsEnabled: Boolean

    fun setScreenshotsEnabled(enable: Boolean)
}
