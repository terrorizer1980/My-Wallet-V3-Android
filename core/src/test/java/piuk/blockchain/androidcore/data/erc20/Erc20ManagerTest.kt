package piuk.blockchain.androidcore.data.erc20

import junit.framework.Assert
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

class Erc20ManagerTest {
    private lateinit var erc20Manager: Erc20Manager

    @Before
    fun setUp() {
        val ethDataManager: EthDataManager = mock()
        erc20Manager = Erc20Manager(ethDataManager)
    }

    @Test
    fun `raw transaction fields should be correct`() {
        val nonce = 10.toBigInteger()
        val to = "0xD1220A0cf47c7B9Be7A2E63A89F429762e7b9aDb"
        val contractAddress = "0x8E870D67F660D95d5be530380D0eC0bd388289E1"
        val gasPrice = 1.toBigInteger()
        val gasLimit = 5.toBigInteger()
        val amount = 7.toBigInteger()

        val rawTransaction =
            erc20Manager.createErc20Transaction(nonce, to, contractAddress, gasPrice, gasLimit, amount)

        Assert.assertEquals(nonce, rawTransaction!!.nonce)
        Assert.assertEquals(gasPrice, rawTransaction.gasPrice)
        Assert.assertEquals(gasLimit, rawTransaction.gasLimit)
        Assert.assertEquals("0x8E870D67F660D95d5be530380D0eC0bd388289E1", rawTransaction.to)
        Assert.assertEquals(0.toBigInteger(), rawTransaction.value)
        Assert.assertEquals(
            "a9059cbb000000000000000000000000d1220a0cf47c7b9be7a2e63a89f429762e7b" +
                    "9adb0000000000000000000000000000000000000000000000000000000000000007",
            rawTransaction.data)
    }
}