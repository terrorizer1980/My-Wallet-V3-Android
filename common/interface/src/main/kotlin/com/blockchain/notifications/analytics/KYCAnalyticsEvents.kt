package com.blockchain.notifications.analytics

sealed class KYCAnalyticsEvents(override val event: String, override val params: Map<String, String> = mapOf()) :
    AnalyticsEvent {
    object VerifyEmailButtonClicked : KYCAnalyticsEvents("kyc_verify_email_button_click")
    object CountrySelected : KYCAnalyticsEvents("kyc_country_selected")
    class PersonalDetailsSet(data: String) : KYCAnalyticsEvents("kyc_personal_detail_set", mapOf("field_name" to data))
    object AddressChanged : KYCAnalyticsEvents("kyc_address_detail_set")
    object VerifyIdentityStart : KYCAnalyticsEvents("kyc_verify_id_start_button_click")
    object VeriffInfoSubmitted : KYCAnalyticsEvents("kyc_veriff_info_submitted")
    object Tier1Clicked : KYCAnalyticsEvents("kyc_unlock_silver_click")
    object Tier2Clicked : KYCAnalyticsEvents("kyc_unlock_gold_click")
    object PhoneNumberUpdateButtonClicked : KYCAnalyticsEvents("kyc_phone_update_button_click")
    object EmailUpdateButtonClicked : KYCAnalyticsEvents("kyc_email_update_button_click")
    /*object PersonalFormFieldChange : KYCAnalyticsEvents("personal_form")
    object PersonalFormPhoneNumberChange : KYCAnalyticsEvents("mobile")
    object OnboardingStart : KYCAnalyticsEvents("kyc_onboarding_start")
    object SelectTier : KYCAnalyticsEvents("select_tier")
    class StepChange(step: Int) : KYCAnalyticsEvents("kyc_onboarding_step_to_$step")
    object SendVerificationEmail : KYCAnalyticsEvents("kyc_verify_email_button_click")
    object SendSmsCode : KYCAnalyticsEvents("send_sms_code")
    object PhoneVerificationSuccess : KYCAnalyticsEvents("verify_phone_number_success")
    object PhoneVerificationFailure : KYCAnalyticsEvents("verify_phone_number_failure")*/
}