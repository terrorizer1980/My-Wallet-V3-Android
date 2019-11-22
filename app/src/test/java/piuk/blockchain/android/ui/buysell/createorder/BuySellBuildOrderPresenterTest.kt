package piuk.blockchain.android.ui.buysell.createorder

import com.blockchain.android.testutils.rxInit
import com.blockchain.swap.nabu.models.nabu.Address
import com.blockchain.swap.nabu.models.nabu.NabuUser
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.buysell.createorder.models.OrderType
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.BankLimits
import piuk.blockchain.androidbuysell.models.coinify.CardLimits
import piuk.blockchain.androidbuysell.models.coinify.CountrySupport
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.Level
import piuk.blockchain.androidbuysell.models.coinify.LimitInAmounts
import piuk.blockchain.androidbuysell.models.coinify.LimitValues
import piuk.blockchain.androidbuysell.models.coinify.Limits
import piuk.blockchain.androidbuysell.models.coinify.Medium
import piuk.blockchain.androidbuysell.models.coinify.MinimumInAmounts
import piuk.blockchain.androidbuysell.models.coinify.PaymentMethod
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.models.coinify.Trader

class BuySellBuildOrderPresenterTest {

    private val view = mock<BuySellBuildOrderView> {
        on { orderType } `it returns` OrderType.Buy
    }

    private fun traderLevel(): Level =
        Level(1, "Test", "USD", 1.0,
            Limits(CardLimits(LimitValues(1.0, 100.0)), BankLimits(LimitValues(10.0, 1000.0),
                LimitValues(10.0, 1000.0))))

    private val userWithState: NabuUser = mock {
        on { address } `it returns` Address("", "", "", "XX", "", "YY")
    }

    private val userWithOutState: NabuUser = mock {
        on { address } `it returns` Address("", "", "", "", "", "EE")
    }

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Test
    fun `when card and bank transfer available, min should be the lower of the two`() {

        val presenter =
            BuySellBuildOrderPresenter(
                coinifyDataManager = mock {
                    on { getTrader(any()) } `it returns` Single.just(Trader(1, "USD", "",
                        mock(), traderLevel()))
                    on { getKycReviews(any()) } `it returns` Single.just(listOf(KycResponse(1,
                        ReviewState.Completed,
                        "",
                        "",
                        "",
                        "",
                        "")))
                    on { getQuote(any(), any(), any(), any()) } `it returns` Single.just(mock())
                    on { getPaymentMethods(any(), anyOrNull(), anyOrNull()) } `it returns` Observable.just(listOf(
                        cardPaymentMethod(), transferPaymentMethod()
                    ))
                    on { getSupportedCountries() } `it returns` Single.just(mapOf("US" to CountrySupport(false,
                        mapOf())))
                },
                sendDataManager = mock(),
                payloadDataManager = mock(),
                exchangeService = mock {
                    on { getExchangeMetaData() } `it returns` Observable.just(ExchangeData().apply {
                        coinify = CoinifyData(1, "testToken")
                    })
                },
                currencyFormatManager = mock {
                    on { getFormattedFiatValueWithSymbol(any(), any(), any()) } `it returns` ""
                },
                feeDataManager = mock {
                    on { btcFeeOptions } `it returns` Observable.just(mock())
                },
                dynamicFeeCache = mock(),
                exchangeRateDataManager = mock(),
                nabuDataManager = mock {
                    on { getUser(any()) } `it returns` Single.just(userWithOutState)
                },
                nabuToken = mock {
                    on { fetchNabuToken() } `it returns` Single.just(NabuOfflineTokenResponse(
                        "",
                        ""))
                },
                stringUtils = mock(),
                coinSelectionRemoteConfig = mock())

        presenter.initView(view)
        presenter.onViewReady()
        presenter.onMinClicked()

        verify(view).requestSendFocus()
        verify(view, atLeastOnce()).isCountrySupported(false)
        verify(view).updateSendAmount("1.0")
    }

    @Test
    fun `test availability when user country is supported`() {
        val presenter =
            BuySellBuildOrderPresenter(
                coinifyDataManager = mock {
                    on { getTrader(any()) } `it returns` Single.just(Trader(1, "USD", "",
                        mock(), traderLevel()))
                    on { getKycReviews(any()) } `it returns` Single.just(listOf(KycResponse(1,
                        ReviewState.Completed,
                        "",
                        "",
                        "",
                        "",
                        "")))
                    on { getQuote(any(), any(), any(), any()) } `it returns` Single.just(mock())
                    on { getPaymentMethods(any(), anyOrNull(), anyOrNull()) } `it returns` Observable.just(listOf(
                        cardPaymentMethod(), transferPaymentMethod()
                    ))
                    on { getSupportedCountries() } `it returns` Single.just(mapOf("EE"
                            to CountrySupport(true, mapOf())))
                },
                sendDataManager = mock(),
                payloadDataManager = mock(),
                exchangeService = mock {
                    on { getExchangeMetaData() } `it returns` Observable.just(ExchangeData().apply {
                        coinify = CoinifyData(1, "testToken")
                    })
                },
                currencyFormatManager = mock {
                    on { getFormattedFiatValueWithSymbol(any(), any(), any()) } `it returns` ""
                },
                feeDataManager = mock {
                    on { btcFeeOptions } `it returns` Observable.just(mock())
                },
                dynamicFeeCache = mock(),
                exchangeRateDataManager = mock(),
                nabuDataManager = mock {
                    on { getUser(any()) } `it returns` Single.just(userWithOutState)
                },
                nabuToken = mock {
                    on { fetchNabuToken() } `it returns` Single.just(NabuOfflineTokenResponse(
                        "",
                        ""))
                },
                stringUtils = mock(),
                coinSelectionRemoteConfig = mock())

        presenter.initView(view)
        presenter.onViewReady()

        verify(view, atLeastOnce()).isCountrySupported(true)
    }

    @Test
    fun `test availability when user country is not supported`() {
        val presenter =
            BuySellBuildOrderPresenter(
                coinifyDataManager = mock {
                    on { getTrader(any()) } `it returns` Single.just(Trader(1, "USD", "",
                        mock(), traderLevel()))
                    on { getKycReviews(any()) } `it returns` Single.just(listOf(KycResponse(1,
                        ReviewState.Completed,
                        "",
                        "",
                        "",
                        "",
                        "")))
                    on { getQuote(any(), any(), any(), any()) } `it returns` Single.just(mock())
                    on { getPaymentMethods(any(), anyOrNull(), anyOrNull()) } `it returns` Observable.just(listOf(
                        cardPaymentMethod(), transferPaymentMethod()
                    ))
                    on { getSupportedCountries() } `it returns` Single.just(mapOf("FF"
                            to CountrySupport(true, mapOf())))
                },
                sendDataManager = mock(),
                payloadDataManager = mock(),
                exchangeService = mock {
                    on { getExchangeMetaData() } `it returns` Observable.just(ExchangeData().apply {
                        coinify = CoinifyData(1, "testToken")
                    })
                },
                currencyFormatManager = mock {
                    on { getFormattedFiatValueWithSymbol(any(), any(), any()) } `it returns` ""
                },
                feeDataManager = mock {
                    on { btcFeeOptions } `it returns` Observable.just(mock())
                },
                dynamicFeeCache = mock(),
                exchangeRateDataManager = mock(),
                nabuDataManager = mock {
                    on { getUser(any()) } `it returns` Single.just(userWithOutState)
                },
                nabuToken = mock {
                    on { fetchNabuToken() } `it returns` Single.just(NabuOfflineTokenResponse(
                        "",
                        ""))
                },
                stringUtils = mock(),
                coinSelectionRemoteConfig = mock())

        presenter.initView(view)
        presenter.onViewReady()
        verify(view, atLeastOnce()).isCountrySupported(false)
    }

    private fun transferPaymentMethod(): PaymentMethod =
        PaymentMethod(
            inMedium = Medium.Card,
            outMedium = Medium.Blockchain,
            name = "",
            inCurrencies = listOf("USD"),
            inCurrency = null,
            outCurrencies = listOf(),
            outCurrency = "",
            minimumInAmounts = MinimumInAmounts(1.00, 1.00, 1.00, 1.00, 1.00),
            limitInAmounts = LimitInAmounts(10.00, 10.00, 10.00, 10.00, 10.00),
            limitOutAmounts = null,
            inFixedFees = mock(), inPercentageFee = 1.0, outFixedFees = mock(),
            outPercentageFee = 1.0, canTrade = true, cannotTradeReasons = null
        )

    private fun cardPaymentMethod(): PaymentMethod =
        PaymentMethod(
            inMedium = Medium.Bank,
            outMedium = Medium.Blockchain,
            name = "",
            inCurrencies = listOf("USD"),
            inCurrency = null,
            outCurrencies = listOf(),
            outCurrency = "",
            minimumInAmounts = MinimumInAmounts(10.00, 10.00, 10.00, 10.00, 10.00),
            limitInAmounts = LimitInAmounts(100.00, 100.00, 100.00, 100.00, 100.00),
            limitOutAmounts = null,
            inFixedFees = mock(), inPercentageFee = 1.0, outFixedFees = mock(),
            outPercentageFee = 1.0, canTrade = true, cannotTradeReasons = null
        )
}