package com.blockchain.ui.dialog

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.FragmentManager
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.notifications.analytics.Analytics
import kotlinx.android.synthetic.main.dialog_single_button_sheet.*
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.R

open class SingleButtonBottomSheet : BottomSheetDialogFragment() {

    data class Content(
        @DrawableRes val iconId: Int,
        @StringRes val titleText: Int,
        @StringRes val bodyText: Int,
        @StringRes val btnText: Int,
        val onBtnClick: () -> Unit
    )

    protected val analytics: Analytics by inject()
    protected open val layout = R.layout.dialog_single_button_sheet

    private var content: Content? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        return themedInflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!init()) dismiss()
    }

    protected fun setContent(content: Content) {
        this.content = content
    }

    private fun init(): Boolean {
        val c = content ?: return false

        with(c) {
            icon.setImageResource(iconId)
            txt_title.setText(titleText)
            txt_body.setText(bodyText)
            button.setText(btnText)
            button.setOnClickListener { onBtnClick.invoke() }
        }
        return true
    }
}

class TourBottomDialog : SingleButtonBottomSheet() {

    companion object {
        fun showDialog(fm: FragmentManager, content: Content) {
            TourBottomDialog().apply {
                setContent(content)
                show(fm, "TOUR_SHEET")
            }
        }
    }
}
