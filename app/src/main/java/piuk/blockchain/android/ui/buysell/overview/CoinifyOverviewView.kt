package piuk.blockchain.android.ui.buysell.overview

import androidx.annotation.StringRes
import piuk.blockchain.android.ui.buysell.details.models.AwaitingFundsModel
import piuk.blockchain.android.ui.buysell.details.models.BuySellDetailsModel
import piuk.blockchain.android.ui.buysell.details.models.RecurringTradeDisplayModel
import piuk.blockchain.androidcoreui.ui.base.View

interface CoinifyOverviewView : View {

    fun renderViewState(state: OverViewState)

    fun launchBuyPaymentSelectionFlow()

    fun launchCardBuyFlow()

    fun showAlertDialog(@StringRes message: Int)

    fun displayProgressDialog()

    fun dismissProgressDialog()

    fun launchDetailsPage(dataModel: BuySellDetailsModel)

    fun launchAwaitingTransferPage(dataModel: AwaitingFundsModel)

    fun launchRecurringTradeDetail(displayModel: RecurringTradeDisplayModel)

    fun onStartVerifyIdentification(redirectUrl: String, externalKycId: String)
}