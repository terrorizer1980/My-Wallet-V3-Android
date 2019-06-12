package com.blockchain.sunriver.ui

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialogFragment
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.airdrop_bottom_dialog.*
import piuk.blockchain.androidcoreui.R
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible

abstract class BaseAirdropBottomDialog(
    private val content: Content,
    @LayoutRes private val layout: Int = R.layout.airdrop_bottom_dialog
) : BottomSheetDialogFragment() {

    class Content(
        @StringRes val title: Int,
        @StringRes val description: Int,
        @StringRes val ctaButtonText: Int,
        @StringRes val dismissText: Int = 0,
        @DrawableRes val iconDrawable: Int? = null
    )

    protected val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        return themedInflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        dialog_title.setText(content.title)
        dialog_body.setText(content.description)
        button_cta.apply {
                if (content.ctaButtonText != 0) {
                    setText(content.ctaButtonText)
                    setOnClickListener { ctaButtonClick() }
                    visible()
                } else {
                    gone()
                }
            }

        button_dismiss.apply {
            if (content.dismissText != 0) {
                setText(content.dismissText)
                setOnClickListener { dismissButtonClick() }
                visible()
            } else {
                gone()
            }
        }

        xlm_icon.setOnClickListener { xlmLogoClick() }
        content.iconDrawable?.let {
            xlm_icon.setImageResource(it)
        }
    }

    abstract fun xlmLogoClick()

    abstract fun ctaButtonClick()

    open fun dismissButtonClick() {
        dismiss()
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}
