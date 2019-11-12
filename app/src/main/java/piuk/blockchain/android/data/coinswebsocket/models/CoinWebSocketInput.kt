package piuk.blockchain.android.data.coinswebsocket.models

data class CoinWebSocketInput(
    val ethAddress: String,
    val erc20Address: String,
    val erc20ContractAddress: String,
    val receiveBtcAddresses: List<String>,
    val receiveBhcAddresses: List<String>,
    val xPubsBtc: List<String>,
    val xPubsBch: List<String>
)