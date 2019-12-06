package com.blockchain.swap.nabu

import org.junit.Test

class DisabledFeatureFlagTest {

    @Test
    fun `enabled returns false`() {
        DisabledFeatureFlag()
            .enabled
            .test()
            .assertValue(false)
    }
}