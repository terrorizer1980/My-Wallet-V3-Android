package piuk.blockchain.android.ui.launcher

import com.blockchain.logging.CrashLogger
import com.blockchain.swap.shapeshift.ShapeShiftDataManager
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class Prerequisites(
    private val metadataManager: MetadataManager,
    private val settingsDataManager: SettingsDataManager,
    private val shapeShiftDataManager: ShapeShiftDataManager,
    private val crashLogger: CrashLogger,
    private val dynamicFeeCache: DynamicFeeCache,
    private val feeDataManager: FeeDataManager,
    private val simpleBuySync: SimpleBuySyncFactory,
    private val rxBus: RxBus
) {

    fun initMetadataAndRelatedPrerequisites(): Completable =
        metadataManager.attemptMetadataSetup()
            .andThen(shapeShiftCompletable())
            .andThen(feesCompletable())
            .andThen(Completable.defer {
                simpleBuySync.performSync()
            })
            .doOnComplete {
                rxBus.emitEvent(MetadataEvent::class.java, MetadataEvent.SETUP_COMPLETE)
            }.subscribeOn(Schedulers.io())

    fun initSettings(guid: String, sharedKey: String): Observable<Settings> =
        settingsDataManager.initSettings(
            guid,
            sharedKey
        )

    private fun shapeShiftCompletable(): Completable {
        return shapeShiftDataManager.initShapeshiftTradeData()
            .onErrorComplete()
            .doOnError { throwable ->
                crashLogger.logException(throwable, "Failed to load shape shift trades")
            }
    }

    private fun feesCompletable(): Completable =
        feeDataManager.btcFeeOptions
            .doOnNext { dynamicFeeCache.btcFeeOptions = it }
            .ignoreElements()
            .andThen(feeDataManager.ethFeeOptions
                .doOnNext { dynamicFeeCache.ethFeeOptions = it }
                .ignoreElements()
            )
            .andThen(feeDataManager.bchFeeOptions
                .doOnNext { dynamicFeeCache.bchFeeOptions = it }
                .ignoreElements()

            )
            .subscribeOn(Schedulers.io())

    fun decryptAndSetupMetadata(secondPassword: String) = metadataManager.decryptAndSetupMetadata(
        secondPassword
    )
}