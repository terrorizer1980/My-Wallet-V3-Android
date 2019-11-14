package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycTierState
import com.blockchain.kyc.models.nabu.LimitsJson
import com.blockchain.kyc.models.nabu.TierJson
import com.blockchain.kyc.models.nabu.TiersJson
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.swap.nabu.NabuToken
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class AnnouncementQueriesTest {

    private val nabuToken: NabuToken = mock()
    private val settings: SettingsDataManager = mock()
    private val nabu: NabuDataManager = mock()
    private val tierService: TierService = mock()

    private val sampleLimits = LimitsJson("", 0.toBigDecimal(), 0.toBigDecimal())

    private lateinit var subject: AnnouncementQueries

    @Before
    fun setUp() {
        subject = AnnouncementQueries(
            nabuToken = nabuToken,
            settings = settings,
            nabu = nabu,
            tierService = tierService
        )
    }

    @Test
    fun `isTier1Or2Verified returns true for tier1 verified`() {

        whenever(tierService.tiers()).thenReturn(
            Single.just(
                TiersJson(
                    listOf(
                        TierJson(0,
                            "",
                            KycTierState.None,
                            sampleLimits
                        ),
                        TierJson(0,
                            "",
                            KycTierState.Verified,
                            sampleLimits
                        ),
                        TierJson(0,
                            "",
                            KycTierState.None,
                            sampleLimits
                        )
                    )
                )
            )
        )

        subject.isTier1Or2Verified()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `isTier1Or2Verified returns true for tier2 verified`() {
        whenever(tierService.tiers()).thenReturn(
            Single.just(
                TiersJson(
                    listOf(
                        TierJson(
                            0,
                            "",
                            KycTierState.None,
                            sampleLimits
                        ),
                        TierJson(
                            0,
                            "",
                            KycTierState.Verified,
                            sampleLimits
                        ),
                        TierJson(
                            0,
                            "",
                            KycTierState.Verified,
                            sampleLimits
                        )
                    )
                )
            )
        )

        subject.isTier1Or2Verified()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `isTier1Or2Verified returns false if not verified`() {
        whenever(tierService.tiers()).thenReturn(
            Single.just(
                TiersJson(
                    listOf(
                        TierJson(
                            0,
                            "",
                            KycTierState.None,
                            sampleLimits
                        ),
                        TierJson(
                            0,
                            "",
                            KycTierState.None,
                            sampleLimits
                        ),
                        TierJson(
                            0,
                            "",
                            KycTierState.None,
                            sampleLimits
                        )
                    )
                )
            )
        )

        subject.isTier1Or2Verified()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
