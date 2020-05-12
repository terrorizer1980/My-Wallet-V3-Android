package piuk.blockchain.android.coincore

import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.coincore.bch.BchTokens
import piuk.blockchain.android.coincore.pax.PaxTokens
import piuk.blockchain.android.coincore.btc.BtcTokens
import piuk.blockchain.android.coincore.eth.EthTokens
import piuk.blockchain.android.coincore.impl.AssetActivityRepo
import piuk.blockchain.android.coincore.stx.StxTokens
import piuk.blockchain.android.coincore.xlm.XlmTokens

val coincoreModule = applicationContext {

    context("Payload") {

        bean {
            StxTokens(
                rxBus = get(),
                payloadManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                custodialWalletManager = get(),
                crashLogger = get(),
                labels = get()
            )
        }

        bean {
            BtcTokens(
                payloadManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                payloadDataManager = get(),
                rxBus = get(),
                custodialWalletManager = get(),
                crashLogger = get(),
                labels = get()
            )
        }

        bean {
            BchTokens(
                bchDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                rxBus = get(),
                crashLogger = get(),
                stringUtils = get(),
                custodialWalletManager = get(),
                environmentSettings = get(),
                labels = get()
            )
        }

        bean {
            XlmTokens(
                rxBus = get(),
                xlmDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialWalletManager = get(),
                crashLogger = get(),
                labels = get()
            )
        }

        bean {
            EthTokens(
                ethDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                rxBus = get(),
                crashLogger = get(),
                stringUtils = get(),
                custodialWalletManager = get(),
                labels = get()
            )
        }

        bean {
            PaxTokens(
                rxBus = get(),
                paxAccount = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                custodialWalletManager = get(),
                stringUtils = get(),
                crashLogger = get(),
                labels = get()
            )
        }

        bean {
            Coincore(
                btcTokens = get(),
                bchTokens = get(),
                ethTokens = get(),
                xlmTokens = get(),
                paxTokens = get(),
                stxTokens = get(),
                defaultLabels = get()
            )
        }

        bean {
            AssetActivityRepo()
        }
    }
}
