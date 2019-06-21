package piuk.blockchain.android.ui.thepit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import kotlinx.android.synthetic.main.activity_pit_kyc_promo_layout.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity

class PitPermissionsActivity : PitPermissionsView, BaseMvpActivity<PitPermissionsView, PitPermissionsPresenter>() {
    private val presenter: PitPermissionsPresenter by inject()

    override fun createPresenter(): PitPermissionsPresenter = presenter

    override fun getView(): PitPermissionsView = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pit_kyc_promo_layout)
        connect_now.setOnClickListener {
            presenter.connect()
        }
    }

    override fun onLinkSuccess(uuid: String) {
    }

    override fun onLinkFailed(reason: String) {
    }

    override fun showLoading() {
    }

    override fun hideLoading() {
    }

    companion object {
        fun start(ctx: Context) {
            ctx.startActivity(Intent(ctx, PitPermissionsActivity::class.java))
        }
    }
}
