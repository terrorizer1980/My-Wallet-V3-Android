package piuk.blockchain.androidcoreui.ui.dlg

import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialogFragment
import android.text.method.LinkMovementMethod
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.LoggableEvent
import com.blockchain.ui.extensions.throttledClicks
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.error_bottom_dialog.*
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.R
import piuk.blockchain.androidcoreui.utils.extensions.gone
import java.lang.IllegalStateException

class ErrorBottomDialog : BottomSheetDialogFragment() {

    @Parcelize
    data class Content(
        val title: CharSequence,
        val description: CharSequence,
        @StringRes val ctaButtonText: Int = 0,
        @StringRes val dismissText: Int = 0,
        @DrawableRes val icon: Int
    ) : Parcelable

    private val eventLogger: EventLogger by inject()

    private val clicksDisposable = CompositeDisposable()

    private lateinit var content: Content

    var onCtaClick: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventLogger.logEvent(LoggableEvent.SwapErrorDialog)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialogTheme)

        content = arguments?.getParcelable(ARG_CONTENT) ?: throw IllegalStateException("No content provided")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        return themedInflater.inflate(R.layout.error_bottom_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        init()
    }

    override fun onResume() {
        super.onResume()
        clicksDisposable += button_cta.throttledClicks()
            .subscribeBy(onNext = {
                onCtaClick()
                eventLogger.logEvent(LoggableEvent.SwapErrorDialogCtaClicked)
                dismiss()
            })
        clicksDisposable += button_dismiss.throttledClicks()
            .subscribeBy(onNext = {
                eventLogger.logEvent(LoggableEvent.SwapErrorDialogDismissClicked)
                dismiss()
            })
    }

    override fun onPause() {
        clicksDisposable.clear()
        super.onPause()
    }

    private fun init() {

        with(content) {
            dialog_title.text = title
            dialog_icon.setImageResource(icon)

            dialog_body.apply {
                text = description
                movementMethod = LinkMovementMethod.getInstance()
            }

            button_cta.apply {
                if (ctaButtonText != 0) {
                    setText(ctaButtonText)
                } else {
                    gone()
                }
            }

            button_dismiss.apply {
                if (dismissText != 0) {
                    setText(dismissText)
                } else {
                    gone()
                }
            }
        }
    }

    companion object {

        private const val ARG_CONTENT = "arg_content"

        fun newInstance(content: Content): ErrorBottomDialog {
            val errorDialog = ErrorBottomDialog()
            errorDialog.arguments = Bundle().apply {
                putParcelable(ARG_CONTENT, content)
            }
            return errorDialog
        }
    }
}
