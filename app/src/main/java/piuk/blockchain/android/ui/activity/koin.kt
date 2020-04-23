package piuk.blockchain.android.ui.activity

import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.coincore.impl.TransactionNoteUpdater
import piuk.blockchain.android.ui.activity.detail.*

val activitiesModule = applicationContext {

    context("Payload") {

        factory {
            ActivitiesModel(
                initialState = ActivitiesState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get()
            )
        }

        factory {
            ActivitiesInteractor(
                coincore = get()
            )
        }

        factory {
            ActivityDetailsModel(
                initialState = ActivityDetailState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get()
            )
        }

        factory {
            ActivityDetailsInteractor(
                coincore = get()
            )
        }

        factory {
            TransactionInOutMapper(
                transactionHelper = get(),
                payloadDataManager = get(),
                stringUtils = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                xlmDataManager = get(),
                environmentSettings = get()
            )
        }

        factory {
            TransactionHelper(
                payloadDataManager = get(),
                bchDataManager = get()
            )
        }

        bean {
            TransactionNoteUpdater(
                exchangeService = get(),
                shapeShiftDataManager = get(),
                coinifyDataManager = get(),
                stringUtils = get()
            )
        }
    }
}
