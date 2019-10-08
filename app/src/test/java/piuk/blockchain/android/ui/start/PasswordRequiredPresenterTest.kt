package piuk.blockchain.android.ui.start

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.payload.data.Wallet

import org.json.JSONObject
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import io.reactivex.Completable
import io.reactivex.Observable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.amshove.kluent.any
import org.amshove.kluent.mock
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.util.DialogButtonCallback
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import retrofit2.Response
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.android.R

class PasswordRequiredPresenterTest : RxTest() {

    private lateinit var subject: PasswordRequiredPresenter
    private val view: PasswordRequiredView = mock()
    private val appUtil: AppUtil = mock()
    private val prefs: PersistentPrefs = mock()
    private val authDataManager: AuthDataManager = mock()
    private val buyDataManager: BuyDataManager = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val wallet: Wallet = mock()

    @Before
    fun setUp() {
        whenever(prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, "")).thenReturn(GUID)
        whenever(view.password).thenReturn("1234567890")

        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(payloadDataManager.wallet!!.sharedKey).thenReturn("shared_key")
        whenever(payloadDataManager.wallet!!.guid).thenReturn(GUID)

        subject = PasswordRequiredPresenter(
            appUtil,
            prefs,
            authDataManager,
            payloadDataManager,
            buyDataManager,
            coinifyDataManager
        )

        subject.attachView(view)
    }

    @Test
    fun onContinueClickedNoPassword() {
        // Arrange
        whenever(prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, "")).thenReturn("")
        whenever(view.password).thenReturn("")

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).showToast(anyInt(), anyString())
    }

    @Test
    fun onContinueClickedCorrectPassword() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.just("1234567890"))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.complete())

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).goToPinPage()
        verify(prefs).setValue(PersistentPrefs.KEY_WALLET_GUID, GUID)
        verify(prefs).setValue(PersistentPrefs.KEY_EMAIL_VERIFIED, true)
        verify(appUtil).sharedKey = anyString()
    }

    @Test
    fun onContinueClickedCorrectPasswordTwoFa() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody = TWO_FA_RESPONSE.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.just("1234567890"))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.complete())

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).dismissProgressDialog()
        verify(view).showTwoFactorCodeNeededDialog(any(), any(), any(), any())
    }

    @Test
    fun onContinueClickedPairingFailure() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.error(Throwable()))
        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.just("1234567890"))

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).showToast(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    @Ignore("This has never actually worked, but refactoring has highlighted the failure")
    @Test
    fun onContinueClickedCreateFailure() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.error(Throwable()))

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).showToast(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun onContinueClickedDecryptionFailure() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.just("1234567890"))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.error(DecryptionException()))

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).showToast(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun onContinueClickedHDWalletExceptionFailure() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.just("1234567890"))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.error(HDWalletException()))

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).showToast(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun onContinueClickedFatalErrorClearData() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.just("1234567890"))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.error(RuntimeException()))
        // Act
        subject.onContinueClicked()
        // Assert

        verify(view).showToast(anyInt(), anyString())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity::class.java)
    }

    @Test
    fun onContinueClickedFatalError() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString()))
            .thenReturn(Observable.error(Throwable()))

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).showToast(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity::class.java)
    }

    @Test
    fun onContinueClickedEncryptedPayloadFailure() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        whenever(authDataManager.getEncryptedPayload(any(), any()))
            .thenReturn(Observable.error(Throwable()))
        // Act
        subject.onContinueClicked()
        // Assert

        verify(view).showToast(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity::class.java)
    }

    @Test
    fun onContinueClickedWaitingForAuthRequired() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody = KEY_AUTH_REQUIRED_JSON.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.just(PasswordRequiredPresenter.KEY_AUTH_REQUIRED))
        whenever(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1))

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).showToast(anyInt(), any())
        verify(view).resetPasswordField()
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity::class.java)
    }

    @Test
    fun onContinueClickedWaitingForAuthSuccess() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody = KEY_AUTH_REQUIRED_JSON.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.just("{}"))
        whenever(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.complete())

        // Act
        subject.onContinueClicked()

        // Assert
        verify(payloadDataManager).initializeFromPayload(any(), any())
    }

    /**
     * [AuthDataManager.createCheckEmailTimer] throws an error. Should show error toast.
     */
    @Test
    fun onContinueClickedWaitingForAuthEmailTimerError() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")

        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody =
            KEY_AUTH_REQUIRED_JSON.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.just("{}"))
        whenever(authDataManager.createCheckEmailTimer())
            .thenReturn(Observable.error(Throwable()))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.complete())

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).showToast(anyInt(), anyString())
        verify(view).resetPasswordField()
    }

    @Test
    fun onContinueClickedWaitingForAuthFailure() {
        // Arrange
        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody = KEY_AUTH_REQUIRED_JSON.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.error(Throwable()))

        // Act
        subject.onContinueClicked()

        // Assert
        verify(view).showToast(anyInt(), anyString())
        verify(view).resetPasswordField()
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity::class.java)
    }

    @Test
    fun onContinueClickedWaitingForAuthCountdownComplete() {
        // Arrange
        whenever(view.password).thenReturn("1234567890")
        whenever(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"))
        val responseBody =
            KEY_AUTH_REQUIRED_JSON.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(0))
        whenever(authDataManager.startPollingAuthStatus(anyString(), anyString()))
            .thenReturn(Observable.just("1234567890"))
        // Act
        subject.onContinueClicked()
        // Assert

        verify(view, times(2)).showToast(any(), any())
        verify(view, times(2)).resetPasswordField()
        verify(appUtil, times(2)).clearCredentialsAndRestart(LauncherActivity::class.java)
    }

    @Test
    fun submitTwoFactorCodeNull() {
        // Arrange
        val responseObject = JSONObject()
        val sessionId = "SESSION_ID"
        val password = "PASSWORD"

        // Act
        subject.submitTwoFactorCode(responseObject, sessionId, password, null)

        // Assert
        verify(view).showToast(R.string.two_factor_null_error, ToastCustom.TYPE_ERROR)
    }

    @Test
    fun submitTwoFactorCodeFailed() {
        // Arrange
        val responseObject = JSONObject()
        val sessionId = "SESSION_ID"
        val password = "PASSWORD"
        val code = "123456"
        whenever(authDataManager.submitTwoFactorCode(sessionId, GUID, code))
            .thenReturn(Observable.error(Throwable()))

        // Act
        subject.submitTwoFactorCode(responseObject, sessionId, password, code)

        // Assert
        verify(view).showProgressDialog(R.string.please_wait, null)
        verify(view, atLeastOnce()).dismissProgressDialog()
        verify(view).showToast(R.string.two_factor_incorrect_error, ToastCustom.TYPE_ERROR)
        verify(authDataManager).submitTwoFactorCode(sessionId, GUID, code)
    }

    @Test
    fun submitTwoFactorCodeSuccess() {
        // Arrange
        val responseObject = JSONObject()
        val sessionId = "SESSION_ID"
        val password = "PASSWORD"
        val code = "123456"

        whenever(authDataManager.submitTwoFactorCode(sessionId, GUID, code))
            .thenReturn(
                Observable.just(TWO_FA_RESPONSE.toResponseBody("application/json".toMediaTypeOrNull()))
            )
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.complete())

        // Act
        subject.submitTwoFactorCode(responseObject, sessionId, password, code)

        // Assert
        verify(view).showProgressDialog(R.string.please_wait, null)
        verify(view, atLeastOnce()).dismissProgressDialog()
        verify(view).goToPinPage()
        verify(authDataManager).submitTwoFactorCode(sessionId, GUID, code)
        verify(payloadDataManager).initializeFromPayload(any(), any())
    }

    @Test
    fun onForgetWalletClickedShowWarningAndDismiss() {
        // Arrange
        whenever(view.showForgetWalletWarning(any()))
            .doAnswer { invocation -> (invocation.arguments[0] as DialogButtonCallback).onNegativeClicked() }

        // Act
        subject.onForgetWalletClicked()

        // Assert
        verify(view).showForgetWalletWarning(any())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun onForgetWalletClickedShowWarningAndContinue() {
        // Arrange
        whenever(view.showForgetWalletWarning(any()))
            .doAnswer { invocation -> (invocation.arguments[0] as DialogButtonCallback).onPositiveClicked() }

        // Act
        subject.onForgetWalletClicked()

        // Assert
        verify(view).showForgetWalletWarning(any())
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity::class.java)
    }

    companion object {

        private const val GUID = "1234567890"

        private const val KEY_AUTH_REQUIRED_JSON = "{\n" +
            "  \"authorization_required\": true\n" +
            "}"

        private const val TWO_FA_RESPONSE = "{\n" +
            "  \"auth_type\": 5\n" +
            "}"
    }
}