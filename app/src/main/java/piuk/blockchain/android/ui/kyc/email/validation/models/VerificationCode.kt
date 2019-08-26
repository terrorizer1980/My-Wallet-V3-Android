package piuk.blockchain.android.ui.kyc.email.validation.models

class VerificationCode(verificationCode: String) {
    val code = verificationCode.toUpperCase()
    val isValid = verificationCode.length >= 5
}