package piuk.blockchain.android.ui.buysell.coinify.launcher

import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.NabuCoinifyAccountCreator
import com.blockchain.kycui.settings.KycStatusHelper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherPresenter
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherView

class BuySellLauncherPresenterTest : RxTest() {

    private lateinit var subject: BuySellLauncherPresenter

    private val view: BuySellLauncherView = mock()
    private var kycStatusHelper: KycStatusHelper = mock()
    private val nabuCoinifyAccountCreator: NabuCoinifyAccountCreator = mock()

    @Before
    fun setup() {
        subject = BuySellLauncherPresenter(kycStatusHelper, nabuCoinifyAccountCreator)
        subject.initView(view)
    }

    @Test
    fun `onViewReady user has completed KYC`() {
        // Assemble
        whenever(kycStatusHelper.getKyc2TierStatus()).thenReturn(Single.just(Kyc2TierState.Tier2Approved))
        whenever(nabuCoinifyAccountCreator.createCoinifyAccountIfNeeded()).thenReturn(Completable.complete())
        // Act
        subject.onViewReady()
        // Assert
        verify(nabuCoinifyAccountCreator).createCoinifyAccountIfNeeded()
        verify(view).displayProgressDialog()
        verify(view).onStartCoinifyOverview()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady user has not completed KYC`() {
        // Assemble
        whenever(kycStatusHelper.getKyc2TierStatus()).thenReturn(Single.just(Kyc2TierState.Locked))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayProgressDialog()
        verify(view).onStartCoinifySignUp()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady has coinify user KYC not approved`() {
        // Assemble
        whenever(kycStatusHelper.getKyc2TierStatus()).thenReturn(Single.just(Kyc2TierState.Tier2InReview))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayProgressDialog()
        verify(view).showPendingVerificationView()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady network exception`() {
        // Arrange
        whenever(kycStatusHelper.getKyc2TierStatus()).thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
        verify(view).finishPage()
    }
}