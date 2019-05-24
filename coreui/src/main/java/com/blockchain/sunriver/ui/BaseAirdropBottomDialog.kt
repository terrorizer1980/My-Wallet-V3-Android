package com.blockchain.sunriver.ui

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialogFragment
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
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
        @StringRes val dismissText: Int = 0
    )

    protected val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.TransparentBottomSheetDialogTheme)
    }

    private val animateShip: FeatureFlag by inject("ff_animate_stellar_ship")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        val view = themedInflater.inflate(layout, container, false)
        view.findViewById<TextView>(R.id.dialog_title).setText(content.title)
        view.findViewById<TextView>(R.id.dialog_body).setText(content.description)
        view.findViewById<Button>(R.id.button_cta)
            .apply {
                if (content.ctaButtonText != 0) {
                    setText(content.ctaButtonText)
                    setOnClickListener {
                        ctaButtonClick()
                    }
                    visible()
                } else {
                    gone()
                }
            }
        view.findViewById<TextView>(R.id.button_dismiss).apply {
            if (content.dismissText != 0) {
                setText(content.dismissText)
                setOnClickListener {
                    dismissButtonClick()
                }
                visible()
            } else {
                gone()
            }
        }
        listOf(R.id.xlm_icon)
            .forEach { id ->
                view.findViewById<View>(id)
                    .setOnClickListener {
                        xlmLogoClick()
                    }
            }
        return view
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
