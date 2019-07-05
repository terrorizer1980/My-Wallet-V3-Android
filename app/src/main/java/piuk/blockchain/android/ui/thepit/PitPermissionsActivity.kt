package piuk.blockchain.android.ui.thepit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import kotlinx.android.synthetic.main.activity_pit_kyc_promo_layout.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.confirm.ConfirmPaymentDialog
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.dlg.ErrorBottomDialog

class PitPermissionsActivity : PitPermissionsView, BaseMvpActivity<PitPermissionsView, PitPermissionsPresenter>() {
    private val presenter: PitPermissionsPresenter by inject()

    override fun createPresenter(): PitPermissionsPresenter = presenter

    override fun getView(): PitPermissionsView = this

    private var loadingDialog: PitStateBottomDialog? = null
    private var errorDialog: PitStateBottomDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pit_kyc_promo_layout)
        connect_now.setOnClickListener {
            presenter.connect()
        }
        loadingDialog = PitStateBottomDialog.newInstance(
            PitStateBottomDialog.StateContent(ErrorBottomDialog.Content(getString(R.string.pit_loading_dialog_title),
                getString(R.string.pit_loading_dialog_description), 0, 0, 0), true
            ))
    }

    override fun onLinkSuccess(uuid: String) {
    }

    override fun onLinkFailed(reason: String) {
    }

    override fun showLoading() {
        loadingDialog?.show(supportFragmentManager, "LoadingBottomDialog")
    }

    override fun hideLoading() {
        loadingDialog?.dismiss()
    }

    companion object {
        fun start(ctx: Context) {
            ctx.startActivity(Intent(ctx, PitPermissionsActivity::class.java))
        }
    }
}
