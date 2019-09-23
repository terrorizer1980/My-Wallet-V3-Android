package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper

class RegisterFingerprintsAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val fingerprints: FingerprintHelper = mock()

    private lateinit var subject: RegisterFingerprintsAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[RegisterFingerprintsAnnouncement.DISMISS_KEY])
            .thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey)
            .thenReturn(RegisterFingerprintsAnnouncement.DISMISS_KEY)

        subject =
            RegisterFingerprintsAnnouncement(
                dismissRecorder = dismissRecorder,
                fingerprints = fingerprints
            )
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, when not already shown, and there is no fingerprint hardware`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(fingerprints.isHardwareDetected()).thenReturn(false)
        whenever(fingerprints.isFingerprintUnlockEnabled()).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, fingerprint hardware exists and fingerprints are registered`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(fingerprints.isHardwareDetected()).thenReturn(true)
        whenever(fingerprints.isFingerprintUnlockEnabled()).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, when not already shown, fingerprint hardware exists and fingerprints are not registered`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(fingerprints.isHardwareDetected()).thenReturn(true)
        whenever(fingerprints.isFingerprintUnlockEnabled()).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }
}
