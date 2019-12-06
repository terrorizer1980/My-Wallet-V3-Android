package piuk.blockchain.android.coincore

import org.koin.dsl.module.applicationContext

val coincoreModule = applicationContext {

    context("Payload") {

        bean {
            STXTokens(
                payloadManager = get(),
                currencyPrefs = get()
            )
        }

        bean {
            BTCTokens(
                payloadManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                payloadDataManager = get()
            )
        }

        bean {
            BCHTokens(
                bchDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get()
            )
        }

        bean {
            XLMTokens(
                xlmDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get()
            )
        }

        bean {
            ETHTokens(
                ethDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get()
            )
        }

        bean {
            PAXTokens(
                erc20Account = get(),
                exchangeRates = get(),
                currencyPrefs = get()
            )
        }

        bean {
            AssetTokenLookup(
                btcTokens = get(),
                bchTokens = get(),
                ethTokens = get(),
                xlmTokens = get(),
                paxTokens = get(),
                stxTokens = get()
            )
        }
    }
}
