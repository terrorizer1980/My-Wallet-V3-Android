package piuk.blockchain.android.ui.buysell.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.status.KycStatusActivity
import com.blockchain.nabu.StartKycForBuySell
import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.LoggableEvent
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.buysell.overview.CoinifyOverviewActivity
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.toast

/**
 * This activity checks the user's current buy sell account status and redirects to specified signup or overview components.
 */
class BuySellLauncherActivity : BaseMvpActivity<BuySellLauncherView, BuySellLauncherPresenter>(),
    BuySellLauncherView {

    private var progressDialog: MaterialProgressDialog? = null
    private val startBuySellKyc: StartKycForBuySell by inject()
    private val presenter: BuySellLauncherPresenter by inject()

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buysell_launcher)
        get<EventLogger>().logEvent(LoggableEvent.BuyBitcoin)

        onViewReady()
    }

    override fun onStartCoinifySignUp() {
        startBuySellKyc.startKycActivity(this)
        finishPage()
    }

    override fun onStartCoinifyOverview() {
        CoinifyOverviewActivity.start(this)
        finishPage()
    }

    override fun showPendingVerificationView() {
        KycStatusActivity.start(this, CampaignType.BuySell)
        finishPage()
    }

    override fun finishPage() {
        finish()
    }

    override fun displayProgressDialog() {
        if (!isFinishing) {
            progressDialog = MaterialProgressDialog(this).apply {
                setMessage(getString(R.string.please_wait))
                setCancelable(false)
                show()
            }
        }
    }

    override fun dismissProgressDialog() {
        if (progressDialog?.isShowing == true) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    override fun showErrorToast(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun createPresenter() = presenter

    override fun getView() = this

    companion object {

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, BuySellLauncherActivity::class.java)
            context.startActivity(intent)
        }
    }
}