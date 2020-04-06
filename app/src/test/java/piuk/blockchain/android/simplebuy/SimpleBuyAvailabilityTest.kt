package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager

class SimpleBuyAvailabilityTest {

    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val simpleBuyFlag: FeatureFlag = mock()
    private val buyDataManager: BuyDataManager = mock()

    private lateinit var simpleBuyAvailability: SimpleBuyAvailability

    @Before
    fun setup() {
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(true))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(true)

        whenever(buyDataManager.isCoinifyAllowed)
            .thenReturn(Single.just(false))

        simpleBuyAvailability = SimpleBuyAvailability(
            simpleBuyPrefs, simpleBuyFlag, buyDataManager
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
    fun `should not  be available when is coinify tagged and no local state`() {
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Single.just(true))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when user request fails and no local state exists`() {
        whenever(buyDataManager.isCoinifyAllowed)
            .thenReturn(Single.error(Throwable()))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)

        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }
}