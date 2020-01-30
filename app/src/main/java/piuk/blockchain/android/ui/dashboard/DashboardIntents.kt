package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.androidcore.data.charts.PriceSeries
import java.math.BigInteger

sealed class DashboardIntent : MviIntent<DashboardState>

object RefreshAllIntent : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(assets = oldState.assets.reset())
    }
}

class BalanceUpdate(
    val cryptoCurrency: CryptoCurrency,
    private val newBalance: CryptoValue
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {

        require(cryptoCurrency == newBalance.currency) { throw IllegalStateException("CryptoCurrency mismatch") }

        val oldAsset = oldState[cryptoCurrency]
        val newAsset = oldAsset.copy(cryptoBalance = newBalance, hasBalanceError = false)
        val newAssets = oldState.assets.copy(patchAsset = newAsset)

        return oldState.copy(assets = newAssets)
    }
}

class BalanceUpdateError(
    val cryptoCurrency: CryptoCurrency
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val oldAsset = oldState[cryptoCurrency]
        val newAsset = oldAsset.copy(
            cryptoBalance = CryptoValue(cryptoCurrency, BigInteger.ZERO),
            hasBalanceError = true
        )
        val newAssets = oldState.assets.copy(patchAsset = newAsset)

        return oldState.copy(assets = newAssets)
    }
}

class CheckForCustodialBalanceIntent(
    val cryptoCurrency: CryptoCurrency
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val oldAsset = oldState[cryptoCurrency]
        val newAsset = oldAsset.copy(
            hasCustodialBalance = false
        )
        val newAssets = oldState.assets.copy(patchAsset = newAsset)
        return oldState.copy(assets = newAssets)
    }
}

class UpdateHasCustodialBalanceIntent(
    val cryptoCurrency: CryptoCurrency,
    private val hasCustodial: Boolean
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val oldAsset = oldState[cryptoCurrency]
        val newAsset = oldAsset.copy(
            hasCustodialBalance = hasCustodial
        )
        val newAssets = oldState.assets.copy(patchAsset = newAsset)
        return oldState.copy(assets = newAssets)
    }
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
        val oldAsset = oldState.assets[cryptoCurrency]
        val newAsset = updateAsset(oldAsset, latestPrice, oldPrice)

        return oldState.copy(assets = oldState.assets.copy(patchAsset = newAsset))
    }

    private fun updateAsset(
        old: AssetState,
        latestPrice: FiatValue,
        oldPrice: FiatValue
    ): AssetState {
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
        val oldAsset = oldState.assets[cryptoCurrency]
        val newAsset = updateAsset(oldAsset, historicPrices)

        return oldState.copy(assets = oldState.assets.copy(patchAsset = newAsset))
    }

    private fun updateAsset(
        old: AssetState,
        historicPrices: PriceSeries
    ): AssetState {
        val trend = historicPrices.filter { it.price != null }.map { it.price!!.toFloat() }

        return old.copy(priceTrend = trend)
    }
}

class ShowAnnouncement(private val card: AnnouncementCard) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(announcement = card)
    }
}

object ClearAnnouncement : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(announcement = null)
    }
}

class ShowAssetDetails(
    private val cryptoCurrency: CryptoCurrency
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        when {
            oldState.showAssetSheetFor != null -> oldState
            oldState.shouldShowCustodialIntro(cryptoCurrency) ->
                oldState.copy(
                    showPromoSheet = PromoSheet.PROMO_CUSTODY_INTRO,
                    pendingAssetSheetFor = cryptoCurrency,
                    showAssetSheetFor = null,
                    custodyIntroSeen = true
                )
            else -> oldState.copy(
                showAssetSheetFor = cryptoCurrency,
                pendingAssetSheetFor = null,
                showPromoSheet = null
            )
        }
}

class ShowPromoSheet(
    private val promoSheet: PromoSheet
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        // Custody sheet isn't displayed via this intent, so filter it out
        if (promoSheet == PromoSheet.PROMO_CUSTODY_INTRO) {
            oldState
        } else {
            oldState.copy(
                showPromoSheet = promoSheet,
                showAssetSheetFor = null
            )
        }
}

object ClearBottomSheet : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            showPromoSheet = null,
            showAssetSheetFor = oldState.pendingAssetSheetFor,
            pendingAssetSheetFor = null
        )
}
