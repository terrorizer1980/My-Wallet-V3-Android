package com.blockchain.morph.ui.homebrew.exchange.error

import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialogFragment
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.blockchain.morph.ui.R
import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.LoggableEvent
import com.blockchain.ui.extensions.throttledClicks
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.parcel.Parcelize
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.utils.extensions.gone
import java.lang.IllegalStateException

class SwapErrorBottomDialog : BottomSheetDialogFragment() {

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
    private lateinit var ctaButton: Button
    private lateinit var dismissButton: Button

    var onCtaClick: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventLogger.logEvent(LoggableEvent.SwapErrorDialog)
        setStyle(STYLE_NORMAL,
            piuk.blockchain.androidcoreui.R.style.TransparentBottomSheetDialogTheme)
        content = arguments?.getParcelable(ARG_CONTENT)
            ?: throw IllegalStateException("No content provided")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        val view = themedInflater.inflate(R.layout.swap_error_bottom_dialog, container, false)
        init(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        clicksDisposable += ctaButton.throttledClicks()
            .subscribeBy(onNext = {
                onCtaClick()
                eventLogger.logEvent(LoggableEvent.SwapErrorDialogCtaClicked)
                dismiss()
            })
        clicksDisposable += dismissButton.throttledClicks()
            .subscribeBy(onNext = {
                eventLogger.logEvent(LoggableEvent.SwapErrorDialogDismissClicked)
                dismiss()
            })
    }

    override fun onPause() {
        clicksDisposable.clear()
        super.onPause()
    }

    private fun init(view: View) {
        view.findViewById<TextView>(R.id.dialog_title).apply {
            text = content.title
        }
        view.findViewById<ImageView>(R.id.dialog_icon).apply {
            setImageResource(content.icon)
        }
        view.findViewById<TextView>(R.id.dialog_body).apply {
            text = content.description
        }
        ctaButton = view.findViewById<Button>(R.id.button_cta).apply {
            if (content.ctaButtonText != 0) {
                setText(content.ctaButtonText)
            } else {
                gone()
            }
        }
        dismissButton = view.findViewById<Button>(R.id.button_dismiss).apply {
            if (content.dismissText != 0) {
                setText(content.dismissText)
            } else {
                gone()
            }
        }
    }

    companion object {

        private const val ARG_CONTENT = "arg_content"

        fun newInstance(content: Content): SwapErrorBottomDialog {
            val errorDialog = SwapErrorBottomDialog()
            errorDialog.arguments = Bundle().apply {
                putParcelable(ARG_CONTENT, content)
            }
            return errorDialog
        }
    }
}
