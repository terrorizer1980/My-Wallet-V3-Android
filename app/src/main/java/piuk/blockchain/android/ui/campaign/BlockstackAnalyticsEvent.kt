package piuk.blockchain.android.ui.campaign

import com.blockchain.notifications.analytics.AnalyticsEvent

sealed class BlockstackAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object IntroSheetShown : BlockstackAnalyticsEvent(event = INTRO_SHOWN)
    object IntroSheetActioned : BlockstackAnalyticsEvent(event = INTRO_ACTIONED)
    object IntroSheetDismissed : BlockstackAnalyticsEvent(event = INTRO_DISMISSED)
    object CompletionSheetShown : BlockstackAnalyticsEvent(event = COMPLETION_SHOWN)
    object CompletionSheetActioned : BlockstackAnalyticsEvent(event = COMPLETION_ACTIONED)
    object CompletionSheetDismissed : BlockstackAnalyticsEvent(event = COMPLETION_DISMISSED)

    companion object {
        private const val INTRO_SHOWN = "blockstack_intro_offered"
        private const val INTRO_ACTIONED = "blockstack_intro_started"
        private const val INTRO_DISMISSED = "blockstack_intro_dismissed"
        private const val COMPLETION_SHOWN = "blockstack_completion_offered"
        private const val COMPLETION_ACTIONED = "blockstack_completion_started"
        private const val COMPLETION_DISMISSED = "blockstack_completion_dismissed"
    }
}
