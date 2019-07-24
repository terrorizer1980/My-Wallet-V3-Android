package piuk.blockchain.android.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class SettingsActivity : BaseAuthActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupToolbar()
    }

    override fun enforceFlagSecure(): Boolean = true

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.action_settings)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    companion object {

        fun start(context: Context, extras: Bundle?) {
            val starter = Intent(context, SettingsActivity::class.java)
            if (extras != null) starter.putExtras(extras)
            context.startActivity(starter)
        }
    }
}
