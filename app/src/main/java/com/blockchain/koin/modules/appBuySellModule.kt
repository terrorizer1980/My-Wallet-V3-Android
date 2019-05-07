package com.blockchain.koin.modules

import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherPresenter

val appBuySellModule = applicationContext {

    context("Payload") {

        factory {
            BuySellLauncherPresenter(get(), get())
        }
    }
}
