package com.blockchain.koin.modules

import com.blockchain.swap.nabu.StartBuySell
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherPresenter
import piuk.blockchain.android.ui.buysell.launcher.BuySellStarter

val appBuySellModule = applicationContext {

    factory { BuySellStarter() as StartBuySell }

    context("Payload") {

        factory {
            BuySellLauncherPresenter(get(), get())
        }
    }
}
