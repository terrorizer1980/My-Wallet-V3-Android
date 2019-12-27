package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HideAssetDetailsTest {

    val subject = ClearBottomSheet

    @Test
    fun `clearing empty asset sheet no effect`() {
        val initialState = DashboardState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            showAssetSheetFor = null,
            showPromoSheet = null,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)
        assertEquals(result, initialState)
    }

    @Test
    fun `clearing asset sheet, clears the asset and leaves other fields unchanged`() {

        val initialState = DashboardState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            showAssetSheetFor = CryptoCurrency.ETHER,
            showPromoSheet = null,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result.assets, initialState.assets)
        assertNull(result.showAssetSheetFor)
        assertNull(result.showPromoSheet)
        assertEquals(result.announcement, initialState.announcement)
    }

    @Test
    fun `clearing promo sheet, clears the sheet and leaves other fields unchanged`() {

        val initialState = DashboardState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            showAssetSheetFor = null,
            showPromoSheet = PromoSheet.PROMO_STX_CAMPAIGN_INTO,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result.assets, initialState.assets)
        assertNull(result.showAssetSheetFor)
        assertNull(result.showPromoSheet)
        assertEquals(result.announcement, initialState.announcement)
    }
}
