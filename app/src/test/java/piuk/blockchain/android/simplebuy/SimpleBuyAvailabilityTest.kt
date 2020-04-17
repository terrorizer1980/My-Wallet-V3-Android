package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class SimpleBuyAvailabilityTest {

    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val simpleBuyFlag: FeatureFlag = mock()

    private lateinit var simpleBuyAvailability: SimpleBuyAvailability

    @Before
    fun setup() {
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(true))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(true)

        simpleBuyAvailability = SimpleBuyAvailability(
            simpleBuyPrefs, simpleBuyFlag
        )
    }

    @Test
    fun `should work in the happy scenario`() {
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, true)
    }

    @Test
    fun `should not be available when feature flag is off`() {
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(false))
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when user request fails and no local state exists`() {
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }
}