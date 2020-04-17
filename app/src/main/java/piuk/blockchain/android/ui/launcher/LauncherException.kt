package piuk.blockchain.android.ui.launcher

class CustomLogMessagedException(private val logMessage: String, private val throwable: Throwable) :
    Exception("$logMessage -- ${throwable.message}", throwable)