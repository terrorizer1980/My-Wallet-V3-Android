package com.blockchain.kyc.dev

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.blockchain.kycui.navhost.KycNavHostActivity
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.sunriver.ui.AirdropBottomDialog
import com.blockchain.sunriver.ui.SunriverCampaignSignupBottomDialog
import com.blockchain.veriff.VeriffApplicantAndToken
import com.blockchain.veriff.VeriffLauncher
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc_dev_main)
    }

    fun launchKycForAirdrop(view: View) {
        KycNavHostActivity.start(this, CampaignType.Sunriver)
    }

    fun launchKycForSwap(view: View) {
        KycNavHostActivity.start(this, CampaignType.Swap)
    }

    fun launchKycForResubmission(view: View) {
        KycNavHostActivity.start(this, CampaignType.Resubmission)
    }

    fun launchVeriff(view: View) {
        VeriffLauncher()
            .launchVeriff(
                activity = this,
                applicant = VeriffApplicantAndToken("", "Token1234"),
                requestCode = 1234
            )
    }

    fun airdropDialog(view: View) {
        AirdropBottomDialog().show(supportFragmentManager, "BOTTOM_DIALOG")
    }

    fun sunriverDialog(view: View) {
        val dialog = SunriverCampaignSignupBottomDialog()
        compositeDisposable += dialog
            .shouldShow()
            .doOnError(Timber::e)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { show ->
                if (show) {
                    dialog.show(supportFragmentManager, "BOTTOM_DIALOG")
                } else {
                    Toast.makeText(this, "User is in campaign", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}
