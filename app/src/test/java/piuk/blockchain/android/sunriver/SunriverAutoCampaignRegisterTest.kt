package piuk.blockchain.android.sunriver

import com.blockchain.kyc.datamanagers.nabu.NabuDataUserProvider
import com.blockchain.kyc.models.nabu.CampaignData
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.Tiers
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.sunriver.XlmDataManager
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import info.blockchain.balance.AccountReference
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.junit.Test

class SunriverAutoCampaignRegisterTest {

    @Test
    fun `a tier 0 user does not register for campaign`() {
        val xlmDataManager = mock<XlmDataManager>()
        val sunriverCampaignHelper = mock<SunriverCampaignHelper>()
        SunriverAutoCampaignRegister(givenTier0User(), sunriverCampaignHelper, xlmDataManager)
            .autoRegisterForCampaign()
            .test()
        verifyZeroInteractions(xlmDataManager)
        verifyZeroInteractions(sunriverCampaignHelper)
    }

    @Test
    fun `a tier 1 user does not register for campaign`() {
        val xlmDataManager = mock<XlmDataManager>()
        val sunriverCampaignHelper = mock<SunriverCampaignHelper>()
        SunriverAutoCampaignRegister(givenTier1User(), sunriverCampaignHelper, xlmDataManager)
            .autoRegisterForCampaign()
            .test()
        verifyZeroInteractions(xlmDataManager)
        verifyZeroInteractions(sunriverCampaignHelper)
    }

    @Test
    fun `a tier 2 user does register for campaign`() {
        val accountRef = AccountReference.Xlm("", "GABCD")
        val xlmDataManager = mock<XlmDataManager> {
            on { maybeDefaultAccount() } `it returns` Maybe.just(accountRef)
        }
        val sunriverCampaignHelper = mock<SunriverCampaignHelper> {
            on { registerCampaignAndSignUpIfNeeded(any(), any()) } `it returns` Completable.complete()
        }
        SunriverAutoCampaignRegister(givenTier2User(), sunriverCampaignHelper, xlmDataManager)
            .autoRegisterForCampaign()
            .test()
        verify(sunriverCampaignHelper).registerCampaignAndSignUpIfNeeded(accountRef, CampaignData("SUNRIVER", true))
        verifyNoMoreInteractions(sunriverCampaignHelper)
    }

    @Test
    fun `a tier 2 user which has a campaign address does not register for campaign again`() {
        val xlmDataManager = mock<XlmDataManager>()
        val sunriverCampaignHelper = mock<SunriverCampaignHelper>()
        SunriverAutoCampaignRegister(
            givenTier2UserWithSunriverCampaignAddress(),
            sunriverCampaignHelper,
            xlmDataManager
        ).autoRegisterForCampaign()
            .test()
        verifyZeroInteractions(xlmDataManager)
        verifyZeroInteractions(sunriverCampaignHelper)
    }

    @Test
    fun `a tier 2 user with sunriver tag but no campaign address does register for campaign`() {
        val accountRef = AccountReference.Xlm("", "GABCD")
        val xlmDataManager = mock<XlmDataManager> {
            on { maybeDefaultAccount() } `it returns` Maybe.just(accountRef)
        }
        val sunriverCampaignHelper = mock<SunriverCampaignHelper> {
            on { registerCampaignAndSignUpIfNeeded(any(), any()) } `it returns` Completable.complete()
        }
        SunriverAutoCampaignRegister(
            givenTier2UserWithBadSunriverCampaignAddress(),
            sunriverCampaignHelper,
            xlmDataManager
        ).autoRegisterForCampaign()
            .test()
        verify(sunriverCampaignHelper).registerCampaignAndSignUpIfNeeded(accountRef, CampaignData("SUNRIVER", true))
        verifyNoMoreInteractions(sunriverCampaignHelper)
    }
}

private fun NabuUser.withProvider() =
    mock<NabuDataUserProvider> {
        on { getUser() } `it returns` Single.just<NabuUser>(this@withProvider)
    }

private fun givenTier0User() = givenUsersTiers(0).withProvider()

private fun givenTier1User() = givenUsersTiers(1).withProvider()

private fun givenTier2User() = givenUsersTiers(2).withProvider()

private fun givenTier2UserWithSunriverCampaignAddress() =
    givenUsersTiers(2).withSunriverCampaignAddress().withProvider()

private fun givenTier2UserWithBadSunriverCampaignAddress() =
    givenUsersTiers(2).withBadSunriverCampaignAddress().withProvider()

private fun NabuUser.withSunriverCampaignAddress(): NabuUser =
    copy(
        tags = mapOf("SUNRIVER" to mapOf("x-campaign-address" to "GABCD"))
    )

private fun NabuUser.withBadSunriverCampaignAddress(): NabuUser =
    copy(
        tags = mapOf("SUNRIVER" to mapOf("x-campaign-address-2" to "GABCD"))
    )

private fun givenUsersTiers(
    current: Int
) = emptyNabuUser()
    .copy(
        tiers = Tiers(
            current = current,
            selected = 0,
            next = 0
        )
    )

private fun emptyNabuUser() =
    NabuUser(
        firstName = null,
        lastName = null,
        email = null,
        emailVerified = false,
        dob = null,
        mobile = null,
        mobileVerified = false,
        address = null,
        state = UserState.None,
        kycState = KycState.None,
        insertedAt = null
    )