package com.blockchain.koin

import androidx.appcompat.app.AppCompatActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.koin.test.dryRun
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication

@Config(sdk = [23], application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class KoinGraphTest : AutoCloseKoinTest() {

    @Test
    fun `test module configuration`() {
        dryRun(defaultParameters = { anActivity().toInjectionParameters() })
    }

    private fun anActivity() = Robolectric.buildActivity(AppCompatActivity::class.java).get()
}
