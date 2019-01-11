package com.blockchain.kycui.veriffsplash

import android.support.annotation.StringRes
import com.blockchain.kyc.models.nabu.SupportedDocuments
import com.blockchain.kyc.services.nabu.VeriffApplicantAndToken
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface VeriffSplashView : View {

    val uiState: Observable<String>

    fun continueToVeriff(
        applicant: VeriffApplicantAndToken,
        supportedDocuments: List<SupportedDocuments>
    )

    fun showProgressDialog(cancelable: Boolean)

    fun dismissProgressDialog()

    fun continueToCompletion()

    fun showErrorToast(@StringRes message: Int)
}