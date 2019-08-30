package piuk.blockchain.android.data.coinswebsocket.service

interface MessagesSocketHandler {
    fun triggerNotification(title: String, marquee: String, text: String)
    fun sendBroadcast(intent: String)
}