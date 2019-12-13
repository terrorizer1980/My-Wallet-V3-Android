package piuk.blockchain.android.simplebuy

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume

class SimpleBuyActivity : BlockchainActivity(), SimpleBuyNavigator {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_buy)
        setSupportActionBar(toolbar_general)
        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction()
                .add(R.id.content_frame,
                    SimpleBuyIntroFragment())
                .commitAllowingStateLoss()
    }

    override fun exitSimpleBuyFlow() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun goToBuyCryptoScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SimpleBuyCryptoFragment(), SimpleBuyCryptoFragment::class.simpleName)
            .addToBackStack(SimpleBuyIntroFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment is SimpleBuyScreen && fragment.onBackPressed() && pop()) {
                return
            }
        }
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressed()
    }

    private fun pop(): Boolean {
        val backStackEntryCount = supportFragmentManager.backStackEntryCount
        if (backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
            return true
        }
        return false
    }
}