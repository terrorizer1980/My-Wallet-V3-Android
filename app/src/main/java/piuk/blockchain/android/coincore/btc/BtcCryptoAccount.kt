package piuk.blockchain.android.coincore.btc

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountBase
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountCustodialBase
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase
import piuk.blockchain.android.coincore.impl.transactionFetchCount
import piuk.blockchain.android.coincore.impl.transactionFetchOffset
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal class BtcCryptoInterestAccount(
    override val label: String,
    val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.BTC)

    private val isConfigured = AtomicBoolean(false)

    override val receiveAddress: Single<String>
        get() = Single.error(NotImplementedError("Interest accounts don't support receive"))

    override val balance: Single<CryptoValue>
        get() = custodialWalletManager.getInterestDetails(cryptoAsset)
            .doOnSuccess { isConfigured.set(true) }
            .map { interestDetails ->
                CryptoValue(interestDetails.crypto,
                    interestDetails.balance.toString().toBigInteger())
            }.onErrorReturn {
                Timber.d("Unable to get interest balance: $it")
                CryptoValue.zero(cryptoAsset)
            }

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val isFunded: Boolean
        get() = isConfigured.get()

    override val isDefault: Boolean =
        false // Default is, presently, only ever a non-custodial account.

    override val actions: AvailableActions
        get() = availableActions

    private val availableActions = emptySet<AssetAction>()
}

internal class BtcCryptoAccountCustodial(
    override val label: String,
    override val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountCustodialBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.BTC)
}

internal class BtcCryptoAccountNonCustodial(
    override val label: String,
    private val address: String,
    private val payloadManager: PayloadManager,
    private val payloadDataManager: PayloadDataManager,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.BTC)

    override val balance: Single<CryptoValue>
        get() = Single.just(payloadManager.getAddressBalance(address))
            .map { CryptoValue.fromMinor(CryptoCurrency.BTC, it) }

    override val activity: Single<ActivitySummaryList>
        get() = Single.fromCallable {
                    payloadManager.getAccountTransactions(address, transactionFetchCount, transactionFetchOffset)
                    .map {
                        BtcActivitySummaryItem(
                            it,
                            payloadDataManager,
                            exchangeRates,
                            this
                        ) as ActivitySummaryItem
                }
        }
        .doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    constructor(
        jsonAccount: Account,
        payloadManager: PayloadManager,
        payloadDataManager: PayloadDataManager,
        isDefault: Boolean = false,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        jsonAccount.label,
        jsonAccount.xpub,
        payloadManager,
        payloadDataManager,
        isDefault,
        exchangeRates
    )

    constructor(
        legacyAccount: LegacyAddress,
        payloadManager: PayloadManager,
        payloadDataManager: PayloadDataManager,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        legacyAccount.label ?: legacyAccount.address,
        legacyAccount.address,
        payloadManager,
        payloadDataManager,
        false,
        exchangeRates
    )
}