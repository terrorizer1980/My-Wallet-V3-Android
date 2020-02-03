package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.models.nabu.KycTierState
import com.blockchain.swap.nabu.models.nabu.LimitsJson
import com.blockchain.swap.nabu.models.nabu.TierJson
import com.blockchain.swap.nabu.models.nabu.TiersJson
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.BuyOrderStatus
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.OrderStatus
import com.blockchain.swap.nabu.service.TierService
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.simplebuy.SimpleBuyOrder
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuyUtils
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class AnnouncementQueriesTest {

    private val nabuToken: NabuToken = mock()
    private val settings: SettingsDataManager = mock()
    private val nabu: NabuDataManager = mock()
    private val tierService: TierService = mock()

    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val sbUtils: SimpleBuyUtils = mock()

    private val sampleLimits = LimitsJson("", 0.toBigDecimal(), 0.toBigDecimal())

    private lateinit var subject: AnnouncementQueries

    @Before
    fun setUp() {
        subject = AnnouncementQueries(
            nabuToken = nabuToken,
            settings = settings,
            nabu = nabu,
            tierService = tierService,
            simpleBuyPrefs = simpleBuyPrefs,
            sbUtils = sbUtils,
            custodialWalletManager = custodialWalletManager
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

    @Test
    fun `isSimpleBuyTransactionPending - no prefs state is available, should return false`() {
        whenever(sbUtils.inflateSimpleBuyState(simpleBuyPrefs)).thenReturn(null)

        subject.isSimpleBuyTransactionPending()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `isSimpleBuyTransactionPending - prefs state is available, order state not CONFIRMED, should return false`() {
        val state: SimpleBuyState = mock()
        val order: SimpleBuyOrder = mock()

        whenever(state.order).thenReturn(order)
        whenever(order.orderState).thenReturn(OrderState.INITIALISED)

        whenever(sbUtils.inflateSimpleBuyState(simpleBuyPrefs)).doReturn(state)

        subject.isSimpleBuyTransactionPending()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `isSimpleBuyTransactionPending - has prefs state, is CONFIRMED but backend has no record, return false`() {
        val state: SimpleBuyState = mock()
        val order: SimpleBuyOrder = mock()

        whenever(state.id).thenReturn(BUY_ORDER_ID)
        whenever(state.order).thenReturn(order)
        whenever(order.orderState).thenReturn(OrderState.FINISHED)
        whenever(sbUtils.inflateSimpleBuyState(simpleBuyPrefs)).thenReturn(state)

        val remoteOrder: BuyOrderStatus = mock()
        whenever(remoteOrder.status).thenReturn(OrderStatus.UNKNOWN_ORDER)
        whenever(custodialWalletManager.getBuyOrderStatus(any())).thenReturn(Single.just(remoteOrder))

        subject.isSimpleBuyTransactionPending()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `isSimpleBuyTransactionPending - has prefs state, CONFIRMED, backend COMPLETE then wipe state & ret false`() {
        val state: SimpleBuyState = mock()
        val order: SimpleBuyOrder = mock()

        whenever(state.id).thenReturn(BUY_ORDER_ID)
        whenever(state.order).thenReturn(order)
        whenever(order.orderState).thenReturn(OrderState.FINISHED)
        whenever(sbUtils.inflateSimpleBuyState(simpleBuyPrefs)).thenReturn(state)

        val remoteOrder: BuyOrderStatus = mock()
        whenever(remoteOrder.status).thenReturn(OrderStatus.COMPLETE)
        whenever(custodialWalletManager.getBuyOrderStatus(any())).thenReturn(Single.just(remoteOrder))

        subject.isSimpleBuyTransactionPending()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()

        verify(simpleBuyPrefs).clearState()
    }

    @Test
    fun `isSimpleBuyTransactionPending - has prefs state, CONFIRMED, backend AWAITING FUNDS should return true`() {
        val state: SimpleBuyState = mock()
        val order: SimpleBuyOrder = mock()

        whenever(state.id).thenReturn(BUY_ORDER_ID)
        whenever(state.order).thenReturn(order)
        whenever(order.orderState).thenReturn(OrderState.FINISHED)
        whenever(sbUtils.inflateSimpleBuyState(simpleBuyPrefs)).thenReturn(state)

        val remoteOrder: BuyOrderStatus = mock()
        whenever(remoteOrder.status).thenReturn(OrderStatus.AWAITING_FUNDS)
        whenever(custodialWalletManager.getBuyOrderStatus(any())).thenReturn(Single.just(remoteOrder))

        subject.isSimpleBuyTransactionPending()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `isSimpleBuyKycInProgress - no local simple buy state exists, return false`() {
        whenever(sbUtils.inflateSimpleBuyState(simpleBuyPrefs)).thenReturn(null)

        subject.isSimpleBuyKycInProgress()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `isSimpleBuyKycInProgress - local simple buy state exists but has finished kyc, return false`() {
        val state: SimpleBuyState = mock()
        whenever(state.kycStartedButNotCompleted).thenReturn(false)
        whenever(sbUtils.inflateSimpleBuyState(simpleBuyPrefs)).thenReturn(state)

        subject.isSimpleBuyKycInProgress()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `isSimpleBuyKycInProgress - local simple buy state exists and has finished kyc, return true`() {
        val state: SimpleBuyState = mock()
        whenever(state.kycStartedButNotCompleted).thenReturn(true)
        whenever(sbUtils.inflateSimpleBuyState(simpleBuyPrefs)).thenReturn(state)

        subject.isSimpleBuyKycInProgress()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    companion object {
        private const val BUY_ORDER_ID = "1234567890"
    }
}
