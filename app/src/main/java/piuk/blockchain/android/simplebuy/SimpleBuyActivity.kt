package piuk.blockchain.android.simplebuy

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.home.MainActivity

class SimpleBuyActivity : BlockchainActivity(), SimpleBuyNavigator {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_buy)
        setSupportActionBar(toolbar_general)
        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, SimpleBuyIntroFragment(), SimpleBuyIntroFragment::class.simpleName)
                .commitAllowingStateLoss()
    }

    override fun exitSimpleBuyFlow() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}