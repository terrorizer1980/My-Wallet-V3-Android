package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.amshove.kluent.mock
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

const val FIAT_CURRENCY = "USD"

val TEST_ASSETS = listOf(
    CryptoCurrency.BTC,
    CryptoCurrency.ETHER,
    CryptoCurrency.XLM
)

val initialBtcState = AssetModel(
    currency = CryptoCurrency.BTC,
    cryptoBalance = CryptoValue.zero(CryptoCurrency.BTC),
    price = FiatValue.fromMajor(FIAT_CURRENCY, 300.toBigDecimal()),
    price24h = FiatValue.fromMajor(FIAT_CURRENCY, 400.toBigDecimal()),
    priceTrend = emptyList()
)

val initialEthState = AssetModel(
    currency = CryptoCurrency.ETHER,
    cryptoBalance = CryptoValue.zero(CryptoCurrency.ETHER),
    price = FiatValue.fromMajor(FIAT_CURRENCY, 200.toBigDecimal()),
    price24h = FiatValue.fromMajor(FIAT_CURRENCY, 100.toBigDecimal()),
    priceTrend = emptyList()
)

val initialXlmState = AssetModel(
    currency = CryptoCurrency.XLM,
    cryptoBalance = CryptoValue.zero(CryptoCurrency.XLM),
    price = FiatValue.fromMajor(FIAT_CURRENCY, 100.toBigDecimal()),
    price24h = FiatValue.fromMajor(FIAT_CURRENCY, 75.toBigDecimal()),
    priceTrend = emptyList()
)

val testAnnouncementCard_1 = StandardAnnouncementCard(
    name = "test_1",
    dismissRule = DismissRule.CardOneTime,
    dismissEntry = mock()
)

val testAnnouncementCard_2 = StandardAnnouncementCard(
    name = "test_2",
    dismissRule = DismissRule.CardOneTime,
    dismissEntry = mock()
)

val initialState = DashboardState(
    assets = mapOfAssets(
            CryptoCurrency.BTC to initialBtcState,
            CryptoCurrency.ETHER to initialEthState,
            CryptoCurrency.XLM to initialXlmState
        ),
    showAssetSheetFor = null,
    announcement = null
)
