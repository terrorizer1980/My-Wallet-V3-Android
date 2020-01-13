package piuk.blockchain.android.simplebuy

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
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
            .addToBackStack(SimpleBuyCryptoFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun goToCheckOutScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SimpleBuyCheckoutFragment(), SimpleBuyCheckoutFragment::class.simpleName)
            .addToBackStack(SimpleBuyCheckoutFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun goToKycVerificationScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SimpleBuyPendingKycFragment(), SimpleBuyPendingKycFragment::class.simpleName)
            .addToBackStack(SimpleBuyPendingKycFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun goToBankDetailsScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SimpleBuyBankDetailsFragment(), SimpleBuyBankDetailsFragment::class.simpleName)
            .addToBackStack(SimpleBuyBankDetailsFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun startKyc() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, KYC_STARTED)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == KYC_STARTED && resultCode == RESULT_KYC_SIMPLE_BUY_COMPLETE) {
            goToKycVerificationScreen()
        }
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

    companion object {
        const val KYC_STARTED = 6788
        const val RESULT_KYC_SIMPLE_BUY_COMPLETE = 7854
    }
}