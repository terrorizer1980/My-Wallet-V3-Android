package com.blockchain.swap.common.exchange.mvi

import com.blockchain.swap.nabu.service.Fix
import com.blockchain.swap.nabu.service.isBase
import com.blockchain.swap.nabu.service.isCounter
import com.blockchain.swap.nabu.service.isCrypto
import com.blockchain.swap.nabu.service.isFiat
import org.amshove.kluent.`should be`
import org.junit.Test

class FixTest {

    @Test
    fun `base fiat`() {
        Fix.BASE_FIAT.apply {
            isBase `should be` true
            isFiat `should be` true
            isCounter `should be` false
            isCrypto `should be` false
        }
    }

    @Test
    fun `base crypto`() {
        Fix.BASE_CRYPTO.apply {
            isBase `should be` true
            isFiat `should be` false
            isCounter `should be` false
            isCrypto `should be` true
        }
    }

    @Test
    fun `counter fiat`() {
        Fix.COUNTER_FIAT.apply {
            isBase `should be` false
            isFiat `should be` true
            isCounter `should be` true
            isCrypto `should be` false
        }
    }

    @Test
    fun `counter crypto`() {
        Fix.COUNTER_CRYPTO.apply {
            isBase `should be` false
            isFiat `should be` false
            isCounter `should be` true
            isCrypto `should be` true
        }
    }
}
