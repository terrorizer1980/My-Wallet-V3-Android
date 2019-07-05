package piuk.blockchain.android.ui.thepit

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class PitPermissionsPresenterTest {

    @get:Rule
    val initSchedulers = rxInit {
        computationTrampoline()
        mainTrampoline()
    }

    private lateinit var presenter: PitPermissionsPresenter
    private val settings: Settings = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val nabuToken: NabuToken = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val view = mock<PitPermissionsView>()

    @Before
    fun setUp() {
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(NabuOfflineTokenResponse("", "")))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(Settings()))

        presenter = PitPermissionsPresenter(nabuDataManager, nabuToken, settingsDataManager).also {
            it.initView(view)
        }
    }

    @Test
    fun `when mail is not verified, view should prompt mail for verification`() {
        whenever(settings.isEmailVerified).thenReturn(false)
        whenever(settings.email).thenReturn("test@test.com")

        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))

        presenter.onViewReady()
        presenter.tryToConnect.onNext(Unit)

        Mockito.verify(view).promptForEmailVerification("test@test.com")
    }

    @Test
    fun `when mail is  verified, view should not prompt mail for verification`() {
        whenever(settings.isEmailVerified).thenReturn(true)
        whenever(settings.email).thenReturn("test@test.com")

        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))

        presenter.onViewReady()
        presenter.tryToConnect.onNext(Unit)

        Mockito.verify(view, never()).promptForEmailVerification(any())
    }
}