package piuk.blockchain.androidbuysell.services

import info.blockchain.wallet.metadata.Metadata
import info.blockchain.wallet.metadata.MetadataInteractor
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.ReplaySubject
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.androidbuysell.api.CoinifyWalletService
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.TradeData
import piuk.blockchain.androidbuysell.models.WebViewLoginDetails
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.websockets.WebSocketReceiveEvent
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.toKotlinObject
import timber.log.Timber
import java.util.ArrayList

/**
 * Created by justin on 5/1/17.
 */

class ExchangeService(
    private val payloadManager: PayloadManager,
    private val metadataInteractor: MetadataInteractor,
    private val metadataManager: MetadataManager,
    private val rxBus: RxBus
) : CoinifyWalletService {

    override fun getCoinifyData(): Maybe<CoinifyData> =
        getExchangeMetaData()
            .firstElement()
            .flatMap { it.coinify?.let { coinifyData -> Maybe.just(coinifyData) } ?: Maybe.empty() }

    private var metadataSubject: ReplaySubject<Metadata> = ReplaySubject.create(1)

    private var didStartLoad: Boolean = false

    fun getWebViewLoginDetails(): Observable<WebViewLoginDetails> = Observable.zip(
        getExchangeData().flatMapSingle { buyMetadata ->
            metadataInteractor.loadRemoteMetadata(buyMetadata).defaultIfEmpty("").toSingle()
        }.applySchedulers(),
        getExchangeData().flatMapSingle { buyMetadata ->
            metadataInteractor.fetchMagic(buyMetadata.address).map {
                val magicHash = it
                Hex.toHexString(magicHash)
            }
        }.applySchedulers(),
        BiFunction { externalJson, magicHash ->
            val walletJson = payloadManager.payload!!.toJson()
            val password = payloadManager.tempPassword
            WebViewLoginDetails(walletJson, password, externalJson, magicHash)
        }
    )

    private fun getExchangeData(): Observable<Metadata> {
        if (!didStartLoad) {
            reloadExchangeData()
            didStartLoad = true
        }
        return metadataSubject
            .doOnError { Timber.e(it) }
    }

    /**
     * The reason that metadataInteractor is injected here and not metadata manager,
     * is due to code legacy that will be removed soon due to coinify elimination.
     */

    private fun getPendingTradeAddresses(): Observable<String> = getExchangeData()
        .flatMap { metadata ->
            metadataInteractor.loadRemoteMetadata(metadata).defaultIfEmpty("").toObservable().applySchedulers()
        }
        .flatMapIterable { exchangeData ->
            if (exchangeData.isEmpty()) {
                emptyList<TradeData>()
            } else {
                val data = exchangeData.toKotlinObject<ExchangeData>()
                val trades = ArrayList<TradeData>()
                when {
                    data.coinify != null -> trades.addAll(data.coinify?.trades ?: arrayListOf())
                }
                trades
            }
        }
        .filter { tradeData -> tradeData.isBuy && !tradeData.isConfirmed }
        .map { tradeData ->
            payloadManager.getReceiveAddressAtArbitraryPosition(
                payloadManager.payload!!.hdWallets[0].getAccount(tradeData.accountIndex),
                tradeData.receiveIndex
            )!!
        }
        .distinct()

    fun getExchangeMetaData(): Observable<ExchangeData> =
        getExchangeData()
            .flatMap { metadata ->
                metadataInteractor.loadRemoteMetadata(metadata).defaultIfEmpty("").toObservable().applySchedulers()
            }
            .map { exchangeData ->
                if (exchangeData.isEmpty()) {
                    ExchangeData()
                } else {
                    exchangeData.toKotlinObject()
                }
            }

    fun wipe() {
        metadataSubject = ReplaySubject.create(1)
        didStartLoad = false
    }

    fun watchPendingTrades(): Observable<String> {
        val receiveEvents = rxBus.register(WebSocketReceiveEvent::class.java)

        return getPendingTradeAddresses()
            .doOnNext { Timber.d("watchPendingTrades: watching receive address: %s", it) }
            .flatMap { address ->
                receiveEvents
                    .filter { event -> event.address == address }
                    .map { it.hash }
            }
    }

    fun reloadExchangeData() {
        val exchangeDataStream =
            Observable.defer {
                Observable.just(metadataManager.buildMetadata(MetadataManager.METADATA_TYPE_EXCHANGE))
            }
        exchangeDataStream.subscribeWith(metadataSubject)
    }
}