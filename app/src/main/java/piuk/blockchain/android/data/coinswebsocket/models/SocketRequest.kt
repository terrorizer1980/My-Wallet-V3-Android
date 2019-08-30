package piuk.blockchain.android.data.coinswebsocket.models

import com.google.gson.annotations.SerializedName

sealed class SocketRequest(private val command: Command) {

    object PingRequest : SocketRequest(Command.PING)

    data class SubscribeRequest(val entity: Entity, val coin: Coin, val param: AddressParam?) :
        SocketRequest(Command.SUBSCRIBE)
}

enum class Command {
    @SerializedName("ping")
    PING,
    @SerializedName("subscribe")
    SUBSCRIBE,
    @SerializedName("unsubscribe")
    UNSUBSCRIBE
}

sealed class AddressParam {
    data class SimpleAddress(val address: String) : AddressParam()
    data class TokenedAddress(val address: String, @SerializedName("token_address") val tokenAddress: String) :
        AddressParam()
}