package piuk.blockchain.android.coincore

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.erc20.Erc20Transfer
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus

class PAXTokensTest {

    private val ethDataManager: EthDataManager = mock()
    private val currencyState: CurrencyState = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val rxBus: RxBus = spy()

    private val paxAccount: Erc20Account = mock()
    private val stringUtils: StringUtils = mock()

    private val subject: AssetTokensBase = PAXTokens(
        paxAccount = paxAccount,
        exchangeRates = exchangeRates,
        currencyPrefs = currencyPrefs,
        custodialWalletManager = custodialWalletManager,
        stringUtils = stringUtils,
        rxBus = rxBus
    )

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setup() {
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.PAX)
        whenever(paxAccount.ethDataManager).thenReturn(ethDataManager)
    }

    @Test
    fun getErc20TransactionsList() {
        val erc20Transfer = Erc20Transfer(
            logIndex = "132",
            from = "0x4058a004dd718babab47e14dd0d744742e5b9903",
            to = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29",
            value = 10000.toBigInteger(),
            transactionHash = "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff",
            blockNumber = 7721219.toBigInteger(),
            timestamp = 1557334297
        )

        whenever(paxAccount.getTransactions()).thenReturn(Observable.just(listOf(erc20Transfer)))

        whenever(ethDataManager
            .getTransaction("0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff"))
            .thenReturn(Observable.just(
                EthTransaction().apply {
                    gasPrice = 100.toBigInteger()
                    gasUsed = 2.toBigInteger()
                }
            )
        )

        whenever(paxAccount.getAccountHash())
            .thenReturn(Observable.just("0x4058a004dd718babab47e14dd0d744742e5b9903"))

        whenever(ethDataManager.getLatestBlockNumber())
            .thenReturn(Observable.just(
                EthLatestBlockNumber().apply {
                    number = erc20Transfer.blockNumber.plus(3.toBigInteger())
                }
            )
        )

        val itemAccount = ItemAccount(
            "PAX",
            "1.0 PAX",
            null,
            1L,
            null,
            "AccountID"
        )

        subject.doFetchActivity(itemAccount)
            .test()
            .assertValueCount(1)
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.size == 1 && it[0].run {
                    this is Erc20ActivitySummaryItem &&
                    cryptoCurrency == CryptoCurrency.PAX &&
                    !doubleSpend &&
                    !isFeeTransaction &&
                    confirmations == 3 &&
                    timeStamp == 1557334297L &&
                    direction == TransactionSummary.Direction.SENT &&
                    hash == "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff" &&
                    confirmations == 3 &&
                    totalCrypto == CryptoValue.fromMinor(CryptoCurrency.PAX, 10000.toBigInteger()) &&
                    inputsMap["0x4058a004dd718babab47e14dd0d744742e5b9903"] ==
                        CryptoValue.fromMinor(CryptoCurrency.PAX, 10000.toBigInteger()) &&
                    outputsMap["0x2ca28ffadd20474ffe2705580279a1e67cd10a29"] ==
                        CryptoValue.fromMinor(CryptoCurrency.PAX, 10000.toBigInteger())
                }
            }

        verify(paxAccount).getTransactions()
    }
}

//@Test
//fun getTransactionValueStringReceivedPax() {
//    //  Arrange
//    val displayable: Erc20ActivitySummaryItem = mock()
//    whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.PAX)
//    whenever(displayable.direction).thenReturn(TransactionSummary.Direction.RECEIVED)
//    whenever(displayable.total).thenReturn(BigInteger.valueOf(1_000L))
//    whenever(exchangeRateFactory.getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any()))
//        .thenReturn(Single.just(1000.usd()))
//    whenever(stringUtils.getString(any())).thenReturn("Value when received: ")
//    //  Act
//    val observer = subject.getTransactionValueString("USD", displayable).test()
//    //  Assert
//    verify(exchangeRateFactory).getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any())
//    assertEquals("Value when received: $1,000.00", observer.values()[0])
//    observer.onComplete()
//    observer.assertNoErrors()
//}
