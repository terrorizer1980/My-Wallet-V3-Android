package piuk.blockchain.android.coincore

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.model.ActivitySummaryList
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan

class AssetTokensBaseTest {

    class TestAssetToken : AssetTokensBase(rxBus = spy()) {
        override fun custodialBalanceMaybe(): Maybe<CryptoValue> = Maybe.empty()
        override fun noncustodialBalance(): Single<CryptoValue> = Single.just(CryptoValue.ZeroBtc)
        override fun doFetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList> = Single.just(emptyList())
        override val asset: CryptoCurrency = CryptoCurrency.BTC
        override fun defaultAccount(): Single<CryptoAccount> = Single.error(Exception("Not implemented"))
        override fun receiveAddress(): Single<String> = Single.error(Exception("Not implemented"))
        override fun balance(account: CryptoAccount): Single<CryptoValue> = Single.error(Exception("Not implemented"))
        override fun exchangeRate(): Single<FiatValue> = Single.error(Exception("Not implemented"))
        override fun historicRate(epochWhen: Long): Single<FiatValue> = Single.error(Exception("Not implemented"))
        override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
            Single.error(Exception("Not implemented"))
    }

    private val subject: TestAssetToken = spy()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Test
    fun `activity fetch should return empty list on error`() {
        val itemAccount: ItemAccount = mock()
        whenever(subject.doFetchActivity(itemAccount)).doReturn(Single.error(Exception()))

        subject.fetchActivity(itemAccount)
            .test()
            .assertValueCount(1)
            .assertComplete()
            .assertNoErrors()
            .assertValue { it.isEmpty() }
    }
}