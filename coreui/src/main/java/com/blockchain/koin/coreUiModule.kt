@file:Suppress("USELESS_CAST")

package com.blockchain.koin

import com.blockchain.CrashLoggerImpl
import com.blockchain.SwapDiagnosticsImpl
import com.blockchain.logging.CrashLogger
import com.blockchain.logging.EventLogger
import com.blockchain.logging.SwapDiagnostics
import com.blockchain.remoteconfig.ABTestExperiment
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.RemoteConfiguration
import com.blockchain.transactions.ResourceSendFundsResultLocalizer
import com.blockchain.transactions.SendFundsResultLocalizer
import com.blockchain.ui.chooser.AccountChooserPresenter
import com.crashlytics.android.answers.Answers
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcoreui.BuildConfig
import piuk.blockchain.androidcoreui.utils.OverlayDetection
import piuk.blockchain.androidcoreui.utils.logging.AnswersEventLogger

val coreUiModule = applicationContext {

    context("Payload") {

        factory {
            AccountChooserPresenter(get(), get())
        }
    }

    bean {
        val config = FirebaseRemoteConfigSettings.Builder()
            .setDeveloperModeEnabled(BuildConfig.DEBUG)
            .build()
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettings(config)
        }
    }

    factory { RemoteConfiguration(get()) }
        .bind(RemoteConfig::class)
        .bind(ABTestExperiment::class)

    factory { ResourceSendFundsResultLocalizer(get()) as SendFundsResultLocalizer }

    factory { Answers.getInstance() }

    factory { AnswersEventLogger(get()) as EventLogger }

    bean {
        OverlayDetection(get())
    }

    bean {
        CrashLoggerImpl(BuildConfig.DEBUG)
    }.bind(CrashLogger::class)

    bean {
        SwapDiagnosticsImpl(crashLogger = get())
    }.bind(SwapDiagnostics::class)
}
