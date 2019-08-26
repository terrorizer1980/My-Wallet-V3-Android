package piuk.blockchain.android.injection

import android.content.Context
import com.blockchain.koin.KoinDaggerModule
import com.blockchain.swap.nabu.CurrentTier
import dagger.Module
import dagger.Provides
import piuk.blockchain.androidcore.data.bitcoincash.BchDataStore
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore
import piuk.blockchain.androidcore.data.rxjava.RxBus
import com.blockchain.swap.shapeshift.datastore.ShapeShiftDataStore
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState
import com.blockchain.preferences.CurrencyPrefs
import piuk.blockchain.androidcore.utils.PersistentPrefs
import javax.inject.Singleton

@Module
class ContextModule(private val appContext: Context) : KoinDaggerModule() {

    @Singleton
    @Provides
    fun appContext(): Context = appContext

    @Provides
    fun provideRxBus(): RxBus {
        return get(RxBus::class)
    }

    @Provides
    fun provideShapeShiftDataStore(): ShapeShiftDataStore {
        return get(ShapeShiftDataStore::class)
    }

    @Provides
    fun provideEthDataStore(): EthDataStore {
        return get(EthDataStore::class)
    }

    @Provides
    fun provideBchDataStore(): BchDataStore {
        return get(BchDataStore::class)
    }

    @Provides
    fun provideWalletOptionsState(): WalletOptionsState {
        return get(WalletOptionsState::class)
    }

    @Provides
    fun provideExchangeRateDataStore(): ExchangeRateDataStore {
        return get(ExchangeRateDataStore::class)
    }

    @Provides
    fun providePersistentPrefs(): PersistentPrefs {
        return get(PersistentPrefs::class)
    }

    @Provides
    fun provideFiatCurrencyPreference(): CurrencyPrefs {
        return get(CurrencyPrefs::class)
    }

    @Provides
    fun provideCurrentTier(): CurrentTier {
        return get(CurrentTier::class)
    }
}