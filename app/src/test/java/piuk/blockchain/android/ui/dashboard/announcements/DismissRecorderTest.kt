package piuk.blockchain.android.ui.dashboard.announcements

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import piuk.blockchain.androidcore.utils.PersistentPrefs

class DismissRecorderTest {

    private val prefs: PersistentPrefs = mock()
    private lateinit var subject: DismissRecorder

    @Before
    fun setup() {
        subject = DismissRecorder(prefs)
    }

    @Test
    fun `entry is undismissed by default`() {
        whenever(prefs.getValue(DISMISS_KEY, false)).thenReturn(false)

        val entry = subject[DISMISS_KEY]

        assertFalse(entry.isDismissed)
    }

    @Test
    fun `dismiss forever write to persistent prefs`() {

        val entry = subject[DISMISS_KEY]
        entry.dismiss(DismissRule.DismissForever)

        assertTrue(entry.isDismissed)

        val captorValue = argumentCaptor<Boolean>()
        val captorString = argumentCaptor<String>()
        verify(prefs).setValue(captorString.capture(), captorValue.capture())

        assertEquals(captorString.firstValue, DISMISS_KEY)
        assertEquals(captorValue.firstValue, true)
    }

    @Test
    fun `dismiss for session doesn't write to persistent prefs`() {

        val entry = subject[DISMISS_KEY]
        entry.dismiss(DismissRule.DismissForSession)

        assertTrue(entry.isDismissed)
        verify(prefs, never()).setValue(anyString(), anyBoolean())
    }

    @Test
    fun `forever dismissed reads returns true if dismissed`() {
        whenever(prefs.getValue(DISMISS_KEY, false)).thenReturn(true)

        val entry = subject[DISMISS_KEY]

        assertTrue(entry.isDismissed)
    }

    @Test
    fun `forever dismissed reads returns false if not dismissed`() {
        whenever(prefs.getValue(DISMISS_KEY, false)).thenReturn(false)

        val entry = subject[DISMISS_KEY]

        assertFalse(entry.isDismissed)
    }

    @Test
    fun `undismiss clears all dismiss flags`() {
        val announcementList: AnnouncementList = mock()
        whenever(announcementList.dismissKeys()).thenReturn(listOf(DISMISS_KEY, DISMISS_KEY_2))

        subject[DISMISS_KEY].dismiss(DismissRule.DismissForSession)
        subject[DISMISS_KEY_2].dismiss(DismissRule.DismissForSession)

        assertEquals(subject.dismissed.size, 2)

        subject.undismissAll(announcementList)

        val captorString = argumentCaptor<String>()
        verify(prefs, times(2)).removeValue(captorString.capture())

        assertEquals(captorString.firstValue, DISMISS_KEY)
        assertEquals(captorString.secondValue, DISMISS_KEY_2)

        assertTrue(subject.dismissed.isEmpty())
    }

    companion object {
        private const val DISMISS_KEY = "wibble"
        private const val DISMISS_KEY_2 = "wobble"
    }
}