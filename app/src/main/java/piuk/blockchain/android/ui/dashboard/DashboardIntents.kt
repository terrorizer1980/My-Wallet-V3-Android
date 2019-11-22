package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.androidcore.data.charts.PriceSeries
import timber.log.Timber

sealed class DashboardIntent : MviIntent<DashboardState>

object RefreshAllIntent : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        Timber.d("***> RefreshAllIntent.reduce()")
        return DashboardState(isRefreshing = true)
    }
}

class BalanceUpdate(
    val cryptoCurrency: CryptoCurrency,
    private val newBalance: CryptoValue
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        Timber.d("***> BalanceUpdate.reduce()")

        return DashboardState(
            assets = oldState.assets.copy(patchAsset = updateAsset(newBalance)),
            isRefreshing = false
        )
    }

    private fun updateAsset(balance: CryptoValue) =
        AssetModel(cryptoBalance = balance, currency = balance.currency)
}

class RefreshPrices(
    val cryptoCurrency: CryptoCurrency
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState = oldState
}

class PriceUpdate(
    val cryptoCurrency: CryptoCurrency,
    private val latestPrice: FiatValue,
    private val oldPrice: FiatValue
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        Timber.d("***> PriceUpdate.reduce()")

        val oldAsset = oldState.assets[cryptoCurrency]
        val newAsset = updateAsset(oldAsset, latestPrice, oldPrice)

        return oldState.copy(
            assets = oldState.assets.copy(patchAsset = newAsset),
            isRefreshing = false
        )
    }

    private fun updateAsset(
        old: AssetModel,
        latestPrice: FiatValue,
        oldPrice: FiatValue
    ): AssetModel {
        return old.copy(
            price = latestPrice,
            price24h = oldPrice
        )
    }
}

class PriceHistoryUpdate(
    val cryptoCurrency: CryptoCurrency,
    private val historicPrices: PriceSeries
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        Timber.d("***> PriceHistoryUpdate.reduce()")

        val oldAsset = oldState.assets[cryptoCurrency]
        val newAsset = updateAsset(oldAsset, historicPrices)

            return oldState.copy(
                assets = oldState.assets.copy(patchAsset = newAsset),
                isRefreshing = false
            )
        }

    private fun updateAsset(
        old: AssetModel,
        historicPrices: PriceSeries
    ): AssetModel {
        val trend = historicPrices.filter { it.price != null }.map { it.price!!.toFloat() }

        return old.copy(priceTrend = trend)
    }
}

class ShowAssetDetails(
    private val cryptoCurrency: CryptoCurrency
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(showAssetSheetFor = cryptoCurrency)
    }
}

object HideAssetDetails : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(showAssetSheetFor = null)
    }
}