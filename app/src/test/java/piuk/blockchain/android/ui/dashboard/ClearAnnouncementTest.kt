package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClearAnnouncementTest {

    val subject = ClearAnnouncement

    @Test
    fun `clearing null announcement has no effect`() {
        val result = subject.reduce(initialState)
        assertEquals(result, initialState)
    }

    @Test
    fun `clearing an announcement, clears the announcement and leaves other fields unchanged`() {

        val initialState = DashboardState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            showAssetSheetFor = CryptoCurrency.ETHER,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result.assets, initialState.assets)
        assertEquals(result.showAssetSheetFor, initialState.showAssetSheetFor)
        assertNull(result.announcement)
    }
}