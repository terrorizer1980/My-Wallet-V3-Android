package piuk.blockchain.android.coincore.impl

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan

class AssetTokensBaseTest {

    internal class TestAssetToken : AssetTokensBase(rxBus = spy()) {
        override val asset: CryptoCurrency = CryptoCurrency.BTC
        override fun init(): Completable =
            Completable.complete()
        override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
            Maybe.empty()
        override fun noncustodialBalance(): Single<CryptoValue> =
            Single.just(CryptoValue.ZeroBtc)
        override fun doFetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList> =
            Single.just(emptyList())
        override fun defaultAccount(): Single<CryptoSingleAccount> =
            Single.error(Exception("Not implemented"))
        override fun accounts(filter: Set<AssetFilter>): Single<CryptoAccountGroup> =
            Single.error(Exception("Not implemented"))
        override fun defaultAccountRef(): Single<AccountReference> =
            Single.error(Exception("Not implemented"))
        override fun receiveAddress(): Single<String> =
            Single.error(Exception("Not implemented"))
        override fun balance(account: AccountReference): Single<CryptoValue> =
            Single.error(Exception("Not implemented"))
        override fun exchangeRate(): Single<FiatValue> =
            Single.error(Exception("Not implemented"))
        override fun historicRate(epochWhen: Long): Single<FiatValue> =
            Single.error(Exception("Not implemented"))
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