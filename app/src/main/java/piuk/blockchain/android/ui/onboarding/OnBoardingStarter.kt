package piuk.blockchain.android.ui.onboarding

import android.content.Context
import com.blockchain.activities.StartOnboarding

class OnBoardingStarter : StartOnboarding {
    override fun startOnBoarding(context: Any, emailOnly: Boolean, hasOptionForDismiss: Boolean) {
        (context as? Context)?.let {
            OnboardingActivity.launch(it, emailOnly, hasOptionForDismiss)
        }
    }
}