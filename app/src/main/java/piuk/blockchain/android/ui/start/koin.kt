package piuk.blockchain.android.ui.start

import org.koin.dsl.module.applicationContext

val startupUiModule = applicationContext {

    context("Payload") {
        factory {
            LandingPresenter(
                environmentSettings = get(),
                prefs = get(),
                rootUtil = get()
            )
        }

        factory {
            LoginPresenter(
                payloadDataManager = get(),
                appUtil = get(),
                analytics = get(),
                prefs = get()
            )
        }

        factory {
            ManualPairingPresenter(
                appUtil = get(),
                authDataManager = get(),
                payloadDataManager = get(),
                prefs = get(),
                analytics = get()
            )
        }

        factory {
            PasswordRequiredPresenter(
                appUtil = get(),
                prefs = get(),
                authDataManager = get(),
                payloadDataManager = get(),
                buyDataManager = get(),
                coinifyDataManager = get()
            )
        }
    }
}