package com.blockchain.morph.exchange.mvi

import org.amshove.kluent.`should equal`
import org.junit.Test

class SetHasEnoughBalanceForFees {

    @Test
    fun `can set if the fees are not enough`() {
        given(
            initial("CAD")
        ).onLastStateAfter(
            EnoughFeesLimit(true)
        ) {
            hasEnoughEthFees `should equal` true
        }
    }

    @Test
    fun `can set if the fees are enough`() {
        given(
            initial("CAD")
        ).onLastStateAfter(
            EnoughFeesLimit(false)
        ) {
            hasEnoughEthFees `should equal` false
        }
    }
}