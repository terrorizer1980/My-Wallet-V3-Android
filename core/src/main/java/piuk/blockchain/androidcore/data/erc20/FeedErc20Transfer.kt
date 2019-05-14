package piuk.blockchain.androidcore.data.erc20

import java.math.BigInteger

data class FeedErc20Transfer(val transfer: Erc20Transfer, val gasUsed: BigInteger, val gasPrice: BigInteger)