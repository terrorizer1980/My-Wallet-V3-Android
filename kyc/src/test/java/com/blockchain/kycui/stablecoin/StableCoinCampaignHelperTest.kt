package com.blockchain.kycui.stablecoin

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.junit.Rule
import org.junit.Test

class StableCoinCampaignHelperTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Test
    fun `is enabled returns false when feature flag is disabled`() {
        StableCoinCampaignHelper(
            mock {
                on { enabled } `it returns` Single.just(false)
            }
        ).isEnabled()
            .test()
            .apply {
                assertNoErrors()
                assertValueCount(1)
                assertValueAt(0, false)
            }
    }

    @Test
    fun `is enabled returns true when feature flag is enabled`() {
        StableCoinCampaignHelper(
            mock {
                on { enabled } `it returns` Single.just(true)
            }
        ).isEnabled()
            .test()
            .apply {
                assertNoErrors()
                assertValueCount(1)
                assertValueAt(0, true)
            }
    }
}