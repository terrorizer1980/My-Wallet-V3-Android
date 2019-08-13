package com.blockchain.kyc.address

import com.blockchain.android.testutils.rxInit
import com.blockchain.getBlankNabuUser
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.Tiers
import com.blockchain.kycui.address.EligibilityForFreeEthAdapter
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.validOfflineToken
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EligibilityForFreeEthAdapterTest {

    private lateinit var eligibilityForFreeEthAdapter: EligibilityForFreeEthAdapter
    private val nabuToken: NabuToken = mock()
    private val nabuDataManager: NabuDataManager = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
    }

    @Before
    fun setUp() {
        eligibilityForFreeEthAdapter = EligibilityForFreeEthAdapter(nabuToken, nabuDataManager)
    }

    @Test
    fun `should not be eligiable, if tier is lower than 2 and no tag contained`() {
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))

        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(getBlankNabuUser()
                .copy(
                    tiers = Tiers(1, 1, 2))
                .copy(tags = mapOf())
            ))

        val testEligibleObserver = eligibilityForFreeEthAdapter.isEligible().test()

        testEligibleObserver.values().single().apply {
            this `should equal to` false
        }
    }

    @Test
    fun `should not be eligiable, if tier is lower than 2 and tag contained`() {
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))

        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(getBlankNabuUser()
                .copy(tiers = Tiers(1, 1, 2))
                .copy(tags = mapOf("POWER_PAX" to mapOf("some key" to "some key")))
            ))

        val testEligibleObserver = eligibilityForFreeEthAdapter.isEligible().test()

        testEligibleObserver.values().single().apply {
            this `should equal to` false
        }
    }

    @Test
    fun `should not be eligiable, if tier is  2 and tag contained`() {
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))

        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(getBlankNabuUser()
                .copy(tiers = Tiers(2, 2, 2))
                .copy(tags = mapOf("POWER_PAX" to mapOf("some key" to "some key")))
            ))

        val testEligibleObserver = eligibilityForFreeEthAdapter.isEligible().test()

        testEligibleObserver.values().single().apply {
            this `should equal to` false
        }
    }

    @Test
    fun `should be eligiable, if tier is 2 and no tag contained`() {
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))

        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(getBlankNabuUser()
                .copy(tiers = Tiers(2, 2, 2))
                .copy(tags = mapOf())
            ))

        val testEligibleObserver = eligibilityForFreeEthAdapter.isEligible().test()

        testEligibleObserver.values().single().apply {
            this `should equal to` true
        }
    }
}