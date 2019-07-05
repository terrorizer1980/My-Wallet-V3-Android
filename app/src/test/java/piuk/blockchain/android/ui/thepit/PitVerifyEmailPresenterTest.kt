package piuk.blockchain.android.ui.thepit

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater

class PitVerifyEmailPresenterTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    private lateinit var presenter: PitVerifyEmailPresenter
    private val emailSyncUpdater: EmailSyncUpdater = mock()
    private val view: PitVerifyEmailView = mock()

    @Before
    fun setUp() {
        presenter = PitVerifyEmailPresenter(emailSyncUpdater).also {
            it.initView(view)
        }
    }

    @Test
    fun `when update mail fails, event should be propagated to the view`() {
        whenever(emailSyncUpdater.updateEmailAndSync(any())).thenReturn(Single.error(Throwable()))

        presenter.onViewReady()
        presenter.resendMail.onNext("")

        verify(view).mailResentFailed()
    }

    @Test
    fun `when update mail succeeds , event should be propagated to the view`() {
        whenever(emailSyncUpdater.updateEmailAndSync(any())).thenReturn(Single.just(Email("test@test",
            true)))

        presenter.onViewReady()
        presenter.resendMail.onNext("")

        verify(view, never()).mailResentFailed()
        verify(view).mailResentSuccessfully()
    }
}