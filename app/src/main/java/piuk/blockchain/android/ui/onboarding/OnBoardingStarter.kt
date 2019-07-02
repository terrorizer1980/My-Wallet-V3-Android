package piuk.blockchain.android.ui.onboarding

import android.content.Context
import android.content.Intent
import com.blockchain.activities.StartOnboarding

class OnBoardingStarter : StartOnboarding {
    override fun startOnBoarding(context: Any, emailOnly: Boolean, hasOptionForDismiss: Boolean) {
        (context as?Context)?.let {
            Intent(context, OnboardingActivity::class.java).apply {
                putExtra(OnboardingActivity.EXTRAS_EMAIL_ONLY, emailOnly)
                putExtra(OnboardingActivity.EXTRAS_OPTION_TO_DISMISS, hasOptionForDismiss)
            }.run { context.startActivity(this) }
        }
    }
}