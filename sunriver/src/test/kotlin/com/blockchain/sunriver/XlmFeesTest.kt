package com.blockchain.sunriver

import com.blockchain.testutils.rxInit
import com.blockchain.testutils.stroops
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.junit.Rule
import org.junit.Test

class XlmFeesTest {

    @get:Rule
    val initRx = rxInit {
        ioTrampoline()
    }

    @Test
    fun `fee fetched from Horizon`() {
        val horizonProxy: HorizonProxy = mock()
        val feesServices = XlmFeesService(horizonProxy)
        val networkFees = 15000.stroops()
        whenever(horizonProxy.fees()).thenReturn(networkFees)

        val testObserver = feesServices.perOperationFee.test()
        testObserver.values().first() `should equal` 15000.stroops()
    }

    @Test
    fun `fee falls back to 100 stroops`() {
        val horizonProxy: HorizonProxy = mock()
        val feesServices = XlmFeesService(horizonProxy)
        whenever(horizonProxy.fees()).thenReturn(null)

        val testObserver = feesServices.perOperationFee.test()
        testObserver.values().first() `should equal` 100.stroops()
    }
}