package com.blockchain.sunriver.ui

import android.os.Bundle
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
import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.LoggableEvent
import com.blockchain.nabu.StartKyc
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.R
import timber.log.Timber

class AirdropBottomDialog : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.TransparentBottomSheetDialogTheme)
    }

    private val startKyc: StartKyc by inject()

    private val eventLogger: EventLogger by inject()

    private val animateShip: FeatureFlag by inject("ff_animate_stellar_ship")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        val view = themedInflater.inflate(R.layout.airdrop_bottom_dialog, container, false)
        view.findViewById<View>(R.id.button_get_free_xlm)
            .setOnClickListener {
                eventLogger.logEvent(LoggableEvent.SunRiverBottomDialogClicked)
                startKycAndDismiss()
            }
        listOf(R.id.appCompatImageView_flames, R.id.appCompatImageView_no_flames)
            .forEach { id ->
                view.findViewById<View>(id)
                    .setOnClickListener {
                        eventLogger.logEvent(LoggableEvent.SunRiverBottomDialogClicked)
                        eventLogger.logEvent(LoggableEvent.SunRiverBottomDialogClickedRocket)
                        startKycAndDismiss()
                    }
            }
        return view
    }

    private fun startKycAndDismiss() {
        dismiss()
        startKyc.startKycActivity(activity!!)
    }

    override fun onStart() {
        super.onStart()
        eventLogger.logEvent(LoggableEvent.SunRiverBottomDialog)
    }

    private val compositeDisposable = CompositeDisposable()

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
