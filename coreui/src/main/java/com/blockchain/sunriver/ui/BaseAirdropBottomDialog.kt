package com.blockchain.sunriver.ui

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialogFragment
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.AnimationSet
import android.view.animation.CycleInterpolator
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.TextView
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.R
import piuk.blockchain.androidcoreui.utils.extensions.gone
import timber.log.Timber

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
                gone()
            }
        view.findViewById<TextView>(R.id.button_dismiss).apply {
            if (content.dismissText != 0) {
                setText(content.dismissText)
                setOnClickListener {
                    dismissButtonClick()
                }
            } else {
                gone()
            }
        }
        listOf(R.id.appCompatImageView_flames, R.id.appCompatImageView_no_flames)
            .forEach { id ->
                view.findViewById<View>(id)
                    .setOnClickListener {
                        rocketShipClick()
                    }
            }
        return view
    }

    abstract fun rocketShipClick()

    abstract fun ctaButtonClick()

    open fun dismissButtonClick() {
        dismiss()
    }

    override fun onResume() {
        super.onResume()
        compositeDisposable += animateShip.enabled
            .doOnError(Timber::e)
            .subscribeBy {
                if (it) {
                    view?.rockTheShip()
                        ?.animateFlames()
                }
            }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}

private fun View.rockTheShip(): View {
    findViewById<View>(R.id.appCompatImageView_no_flames)
        .startAnimation(rockAnimation())
    return this
}

private fun View.animateFlames() {
    val animation = AnimationSet(false)
        .apply {
            addAnimation(flameAnimation())
            addAnimation(rockAnimation())
        }
    findViewById<View>(R.id.appCompatImageView_flames)
        .startAnimation(animation)
}

private fun rockAnimation(): Animation {
    val degrees = 0.5f
    return RotateAnimation(-degrees, degrees, RELATIVE_TO_SELF, 0.375f, RELATIVE_TO_SELF, 0.575f)
        .apply {
            repeatCount = TranslateAnimation.INFINITE
            repeatMode = TranslateAnimation.RESTART
            interpolator = CycleInterpolator(1f)
            duration = 1000
        }
}

private fun flameAnimation(): Animation {
    val max = 1.05f
    val min = 0.9f
    return ScaleAnimation(max, min, max, min, RELATIVE_TO_SELF, 0.375f, RELATIVE_TO_SELF, 0.575f)
        .apply {
            repeatCount = TranslateAnimation.INFINITE
            repeatMode = TranslateAnimation.RESTART
            interpolator = CycleInterpolator(10f)
            duration = 1000
        }
}
