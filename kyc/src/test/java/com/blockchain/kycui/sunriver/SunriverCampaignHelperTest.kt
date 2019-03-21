package com.blockchain.kycui.sunriver

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.CampaignData
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.RegisterCampaignRequest
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import info.blockchain.balance.AccountReference
import io.reactivex.Completable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.junit.Rule
import org.junit.Test

class SunriverCampaignHelperTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Test
    fun `get card type none as campaign disabled`() {
        SunriverCampaignHelper(
            mock {
                on { enabled } `it returns` Single.just(false)
            },
            mock(),
            mock(),
            mock(),
            mock()
        ).getCampaignCardType()
            .test()
            .values()
            .first()
            .apply {
                this `should equal` SunriverCardType.None
            }
    }

    @Test
    fun `get card type complete`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        SunriverCampaignHelper(
            mock {
                on { enabled } `it returns` Single.just(true)
            },
            mock {
                on { getCampaignList(offlineToken) } `it returns` Single.just(listOf("SUNRIVER"))
            },
            givenToken(offlineToken),
            mock {
                on { getUserState() } `it returns` Single.just<UserState>(UserState.Active)
                on { getKycStatus() } `it returns` Single.just<KycState>(KycState.Verified)
            },
            mock()
        ).getCampaignCardType()
            .test()
            .values()
            .first()
            .apply {
                this `should equal` SunriverCardType.Complete
            }
    }

    @Test
    fun `get card type join wait list`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        SunriverCampaignHelper(
            mock {
                on { enabled } `it returns` Single.just(true)
            },
            mock {
                on { getCampaignList(offlineToken) } `it returns` Single.just(emptyList())
            },
            givenToken(offlineToken),
            mock {
                on { getUserState() } `it returns` Single.just<UserState>(UserState.Active)
                on { getKycStatus() } `it returns` Single.just<KycState>(KycState.None)
            },
            mock()
        ).getCampaignCardType()
            .test()
            .values()
            .first()
            .apply {
                this `should equal` SunriverCardType.JoinWaitList
            }
    }

    @Test
    fun `get card type finish sign up`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        SunriverCampaignHelper(
            mock {
                on { enabled } `it returns` Single.just(true)
            },
            mock {
                on { getCampaignList(offlineToken) } `it returns` Single.just(listOf("SUNRIVER"))
            },
            givenToken(offlineToken),
            mock {
                on { getUserState() } `it returns` Single.just<UserState>(UserState.Created)
                on { getKycStatus() } `it returns` Single.just<KycState>(KycState.None)
            },
            mock()
        ).getCampaignCardType()
            .test()
            .values()
            .first()
            .apply {
                this `should equal` SunriverCardType.FinishSignUp
            }
    }

    @Test
    fun `register as user already has an account`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        val accountRef = AccountReference.Xlm("", "GABCDEFHI")
        val campaignData = CampaignData("name", false)
        val nabuDataManager = mock<NabuDataManager> {
            on { registerCampaign(any(), any(), any()) } `it returns` Completable.complete()
        }
        SunriverCampaignHelper(
            mock(),
            nabuDataManager,
            givenToken(offlineToken),
            mock(),
            mock()
        ).registerCampaignAndSignUpIfNeeded(accountRef, campaignData)
            .test()
            .assertNoErrors()
            .assertComplete()
        verify(nabuDataManager).registerCampaign(
            offlineToken,
            RegisterCampaignRequest.registerSunriver(
                accountRef.accountId,
                campaignData.newUser
            ),
            campaignData.campaignName
        )
        verifyNoMoreInteractions(nabuDataManager)
    }

    @Test
    fun `register as user has no account`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        val accountRef = AccountReference.Xlm("", "GABCDEFHIJ")
        val campaignData = CampaignData("name", false)
        val nabuDataManager = mock<NabuDataManager> {
            on { registerCampaign(any(), any(), any()) } `it returns` Completable.complete()
            on { requestJwt() } `it returns` Single.just("jwt")
            on { getAuthToken("jwt") } `it returns` Single.just(offlineToken)
        }
        SunriverCampaignHelper(
            mock(),
            nabuDataManager,
            givenToken(offlineToken),
            mock(),
            mock()
        ).registerCampaignAndSignUpIfNeeded(accountRef, campaignData)
            .test()
            .assertNoErrors()
            .assertComplete()
        verify(nabuDataManager).registerCampaign(
            offlineToken,
            RegisterCampaignRequest.registerSunriver(
                accountRef.accountId,
                campaignData.newUser
            ),
            campaignData.campaignName
        )
        verifyNoMoreInteractions(nabuDataManager)
    }

    @Test
    fun `register sunriver campaign`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        val accountRef = AccountReference.Xlm("", "GABCDEFHJIK")
        val nabuDataManager = mock<NabuDataManager> {
            on { registerCampaign(any(), any(), any()) } `it returns` Completable.complete()
        }
        SunriverCampaignHelper(
            mock(),
            nabuDataManager,
            givenToken(offlineToken),
            mock(),
            mock {
                on { defaultAccount() } `it returns` Single.just(accountRef)
            }
        ).registerSunRiverCampaign()
            .test()
            .assertNoErrors()
            .assertComplete()
        verify(nabuDataManager).registerCampaign(
            offlineToken,
            RegisterCampaignRequest.registerSunriver(
                "GABCDEFHJIK",
                false
            ),
            "SUNRIVER"
        )
        verifyNoMoreInteractions(nabuDataManager)
    }

    @Test
    fun `user is in sunriver campaign`() {
        givenUserInCampaigns(listOf("SUNRIVER"))
            .userIsInSunRiverCampaign()
            .test()
            .values()
            .single() `should be` true
    }

    @Test
    fun `user is not in any campaign`() {
        givenUserInCampaigns(emptyList())
            .userIsInSunRiverCampaign()
            .test()
            .values()
            .single() `should be` false
    }

    @Test
    fun `user is in other campaign`() {
        givenUserInCampaigns(listOf("CAMPAIGN2"))
            .userIsInSunRiverCampaign()
            .test()
            .values()
            .single() `should be` false
    }

    @Test
    fun `user is in multiple campaigns`() {
        givenUserInCampaigns(listOf("CAMPAIGN2", "SUNRIVER"))
            .userIsInSunRiverCampaign()
            .test()
            .values()
            .single() `should be` true
    }
}

private fun givenUserInCampaigns(campaigns: List<String>): SunriverCampaignHelper {
    val offlineToken = NabuOfflineTokenResponse("userId", "token")
    return SunriverCampaignHelper(
        mock(),
        mock {
            on { getCampaignList(offlineToken) } `it returns` Single.just(campaigns)
        },
        givenToken(offlineToken),
        mock(),
        mock()
    )
}

private fun givenToken(offlineToken: NabuOfflineTokenResponse): NabuToken =
    mock {
        on { fetchNabuToken() } `it returns` Single.just(offlineToken)
    }
