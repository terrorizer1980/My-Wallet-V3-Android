package piuk.blockchain.android.ui.thepit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import piuk.blockchain.android.R

class PitPermissionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pit_kyc_promo_layout)
    }

    companion object {
        fun start(ctx: Context) {
            ctx.startActivity(Intent(ctx, PitPermissionsActivity::class.java))
        }
    }
}
