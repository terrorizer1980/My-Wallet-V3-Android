package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.percentageDelta
import info.blockchain.balance.sum
import info.blockchain.balance.toFiat
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import piuk.blockchain.android.coincore.BalanceFilter
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class AssetMap(private val map: Map<CryptoCurrency, AssetModel>) : Map<CryptoCurrency, AssetModel> by map {
    override operator fun get(key: CryptoCurrency): AssetModel {
        return map.getOrElse(key) { throw IllegalArgumentException("$key is not a known CryptoCurrency") }
    }

    // TODO: This is horrendously inefficient. Fix it!
    fun copy(patchBalance: CryptoValue): AssetMap {
        val assets = toMutableMap()
        val value = get(patchBalance.currency).copy(cryptoBalance = patchBalance)
        assets[patchBalance.currency] = value
        return AssetMap(assets)
    }

    fun copy(patchAsset: AssetModel): AssetMap {
        val assets = toMutableMap()
        assets[patchAsset.currency] = patchAsset
        return AssetMap(assets)
    }
}

private fun mapOfAssets(vararg pairs: Pair<CryptoCurrency, AssetModel>) = AssetMap(mapOf(*pairs))

interface DashboardItem

interface BalanceModel : DashboardItem {
    val isLoading: Boolean
    val fiatBalance: FiatValue?
    val delta: Pair<FiatValue, Double>?

    operator fun get(currency: CryptoCurrency): AssetModel
}

data class DashboardState(
    val assets: AssetMap = mapOfAssets(
        CryptoCurrency.BTC to AssetModel(CryptoCurrency.BTC),
        CryptoCurrency.BCH to AssetModel(CryptoCurrency.BCH),
        CryptoCurrency.ETHER to AssetModel(CryptoCurrency.ETHER),
        CryptoCurrency.XLM to AssetModel(CryptoCurrency.XLM),
        CryptoCurrency.PAX to AssetModel(CryptoCurrency.PAX)
    ),
    val isRefreshing: Boolean = true,
    val showAssetSheetFor: CryptoCurrency? = null

) : MviState, BalanceModel {

    override val isLoading
        get() = isRefreshing

    override val fiatBalance: FiatValue? by unsafeLazy {
        assets.values
            .filter { !it.isLoading && it.fiatBalance != null }
            .map { it.fiatBalance!! }
            .sum()
    }

    private val fiatBalance24h: FiatValue? by unsafeLazy {
        assets.values
            .filter { !it.isLoading && it.fiatBalance24h != null }
            .map { it.fiatBalance24h!! }
            .sum()
    }

    override val delta: Pair<FiatValue, Double>? by unsafeLazy {
        val current = fiatBalance
        val old = fiatBalance24h

        if (current != null && old != null) {
            Pair(current - old, current.percentageDelta(old))
        } else {
            null
        }
    }

    override operator fun get(currency: CryptoCurrency): AssetModel =
        assets[currency]
}

data class AssetModel(
    val currency: CryptoCurrency,
    val cryptoBalance: CryptoValue? = null,
    val price: FiatValue? = null,
    val price24h: FiatValue? = null,
    val priceTrend: List<Float> = emptyList()
) : DashboardItem {
    val fiatBalance: FiatValue? by unsafeLazy {
        price?.let { cryptoBalance?.toFiat(it) ?: FiatValue.zero(it.currencyCode) }
    }

    val fiatBalance24h: FiatValue? by unsafeLazy {
        price24h?.let { cryptoBalance?.toFiat(it) ?: FiatValue.zero(it.currencyCode) }
    }

    val priceDelta: Double by unsafeLazy { price.percentageDelta(price24h) }

    val isLoading: Boolean by unsafeLazy {
        cryptoBalance == null || price == null || price24h == null
    }
}

class DashboardModel(
    initialState: DashboardState,
    mainScheduler: Scheduler,
    private val interactor: DashboardInteractor
) : MviModel<DashboardState, DashboardIntent>(initialState, mainScheduler) {

    override fun performAction(intent: DashboardIntent): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is RefreshAllIntent -> {
                interactor.refreshBalances(this, BalanceFilter.Total)
            }
            is BalanceUpdate -> {
                process(RefreshPrices(intent.cryptoCurrency))
                null
            }
            is RefreshPrices -> interactor.refreshPrices(this, intent.cryptoCurrency)
            is PriceUpdate -> interactor.refreshPriceHistory(this, intent.cryptoCurrency)
            is PriceHistoryUpdate,
            is ShowAssetDetails,
            is HideAssetDetails -> null
        }
    }
}
