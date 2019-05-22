package com.blockchain.data

import com.blockchain.android.testutils.rxInit
import com.blockchain.datamanagers.BalanceCalculator
import com.blockchain.datamanagers.BalanceCalculatorImpl
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel

class BalanceCalculatorTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
    }

    private val ethDataManager: EthDataManager = mock()

    private lateinit var balanceCalculator: BalanceCalculator

    @Before
    fun setUp() {
        balanceCalculator = BalanceCalculatorImpl(ethDataManager)
    }

    @Test
    fun `should use the local balance, when local model available`() {
        val combinedModel = CombinedEthModel(responseWithJustBalance())
        whenever(ethDataManager.getEthResponseModel()).thenReturn(combinedModel)

        val subscriber = balanceCalculator.balance(CryptoCurrency.ETHER).test()

        subscriber.assertNoErrors()
        subscriber.assertComplete()
        subscriber.assertValue(CryptoValue(CryptoCurrency.ETHER, 100.toBigInteger()))
    }

    private fun responseWithJustBalance(): EthAddressResponseMap =
        EthAddressResponseMap().apply {
            setEthAddressResponseMap("", EthAddressResponse().apply {
                balance = 100.toBigInteger()
            })
        }
}