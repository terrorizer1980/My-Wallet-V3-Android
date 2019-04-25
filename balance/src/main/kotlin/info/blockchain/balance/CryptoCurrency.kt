package info.blockchain.balance

enum class CryptoCurrency(
    val symbol: String,
    val unit: String,
    val dp: Int,                    // max decimal places
    internal val userDp: Int,       // user decimal places
    val requiredConfirmations: Int
) {
    BTC(
        symbol = "BTC",
        unit = "Bitcoin",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 3
    ),
    ETHER(
        symbol = "ETH",
        unit = "Ether",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 12
    ),
    BCH(
        symbol = "BCH",
        unit = "Bitcoin Cash",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 3
    ),
    XLM(
        symbol = "XLM",
        unit = "Stellar",
        dp = 7,
        userDp = 7,
        requiredConfirmations = 1
    ),
    PAX(
        symbol = "PAX",
        unit = "USD Pax",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 3 // Same as ETHER
    );

    companion object {

        fun fromSymbol(symbol: String?): CryptoCurrency? =
            CryptoCurrency.values().firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }

        fun fromSymbolOrThrow(symbol: String?): CryptoCurrency =
            fromSymbol(symbol) ?: throw IllegalArgumentException("Bad currency symbol \"$symbol\"")
    }
}
