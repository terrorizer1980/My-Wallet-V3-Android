package com.blockchain.koin.modules

import android.content.Context
import com.blockchain.balance.TotalBalance
import com.blockchain.balance.plus
import com.blockchain.kycui.settings.KycStatusHelper
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.ui.CurrentContextAccess
import com.blockchain.ui.chooser.AccountListing
import com.blockchain.ui.password.SecondPasswordHandler
import info.blockchain.wallet.util.PrivateKeyFactory
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.kyc.KycDeepLinkHelper
import piuk.blockchain.android.sunriver.SunRiverCampaignAccountProviderAdapter
import piuk.blockchain.android.sunriver.SunriverDeepLinkHelper
import piuk.blockchain.android.ui.account.SecondPasswordHandlerDialog
import piuk.blockchain.android.ui.chooser.WalletAccountHelperAccountListingAdapter
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.send.SendPresenterXSendView
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.external.PerCurrencySendPresenter
import piuk.blockchain.android.ui.send.strategy.BitcoinCashSendStrategy
import piuk.blockchain.android.ui.send.strategy.BitcoinSendStrategy
import piuk.blockchain.android.ui.send.strategy.Erc20SendStrategy
import piuk.blockchain.android.ui.send.strategy.EtherSendStrategy
import piuk.blockchain.android.ui.send.strategy.SendStrategy
import piuk.blockchain.android.ui.send.strategy.XlmSendStrategy
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.android.util.PrngHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.erc20.Erc20Manager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcoreui.utils.DateUtil
import java.util.Locale

val applicationModule = applicationContext {

    factory { OSUtil(get()) }

    factory { StringUtils(get()) }

    factory { get<Context>().resources }

    factory { Locale.getDefault() }

    bean { CurrentContextAccess() }

    context("Payload") {

        factory {
            EthDataManager(get(), get(), get(), get(), get(), get(), get(), get())
        }

        factory {
            Erc20Manager(ethDataManager = get(), erc20DataStore = get(), environmentSettings = get())
        }

        factory {
            BchDataManager(get(), get(), get(), get(), get(), get(), get())
        }

        factory {
            BuyDataManager(get(), get(), get(), get(), get())
        }

        factory {
            SwipeToReceiveHelper(get(), get(), get(), get(), get(), get(), get())
        }

        factory {
            WalletAccountHelper(
                payloadManager = get(),
                stringUtils = get(),
                currencyState = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                xlmDataManager = get(),
                environmentSettings = get(),
                exchangeRates = get(),
                erc20DataManager = get())
        }

        factory { WalletAccountHelperAccountListingAdapter(get()) }
            .bind(AccountListing::class)

        factory {
            SecondPasswordHandlerDialog(get(), get()) as SecondPasswordHandler
        }

        factory { KycStatusHelper(get(), get(), get(), get()) }

        factory { TransactionListDataManager(get(), get(), get(), get(), get(), get(), get()) }

        factory("spendable") { get<TransactionListDataManager>() as TotalBalance }

        factory("all") {
            if (BuildConfig.SHOW_LOCKBOX_BALANCE) {
                get<TotalBalance>("lockbox") + get("spendable")
            } else {
                get("spendable")
            }
        }

        factory {
            SendPresenterXSendView(
                PerCurrencySendPresenter(
                    btcStrategy = get("BTCStrategy"),
                    bchStrategy = get("BCHStrategy"),
                    etherStrategy = get("EtherStrategy"),
                    xlmStrategy = get("XLMStrategy"),
                    erc20Strategy = get("erc20Strategy"),
                    prefs = get(),
                    exchangeRates = get(),
                    stringUtils = get(),
                    envSettings = get(),
                    exchangeRateFactory = get()
                )
            )
        }

        factory<SendStrategy<SendView>>("BTCStrategy") {
            BitcoinSendStrategy(
                walletAccountHelper = get(),
                payloadDataManager = get(),
                currencyState = get(),
                prefs = get(),
                exchangeRateFactory = get(),
                stringUtils = get(),
                sendDataManager = get(),
                dynamicFeeCache = get(),
                feeDataManager = get(),
                privateKeyFactory = get(),
                environmentSettings = get(),
                currencyFormatter = get(),
                exchangeRates = get()
            )
        }

        factory<SendStrategy<SendView>>("BCHStrategy") {
            BitcoinCashSendStrategy(
                walletAccountHelper = get(),
                payloadDataManager = get(),
                prefs = get(),
                stringUtils = get(),
                sendDataManager = get(),
                dynamicFeeCache = get(),
                feeDataManager = get(),
                privateKeyFactory = get(),
                environmentSettings = get(),
                bchDataManager = get(),
                currencyFormatter = get(),
                exchangeRates = get(),
                environmentConfig = get(),
                currencyState = get()
            )
        }

        factory<SendStrategy<SendView>>("EtherStrategy") {
            EtherSendStrategy(
                walletAccountHelper = get(),
                payloadDataManager = get(),
                ethDataManager = get(),
                stringUtils = get(),
                sendDataManager = get(),
                dynamicFeeCache = get(),
                feeDataManager = get(),
                privateKeyFactory = get(),
                environmentSettings = get(),
                currencyFormatter = get(),
                exchangeRates = get(),
                environmentConfig = get(),
                currencyState = get()
            )
        }

        factory<SendStrategy<SendView>>("XLMStrategy") {
            XlmSendStrategy(
                currencyState = get(),
                xlmDataManager = get(),
                xlmFeesFetcher = get(),
                xlmTransactionSender = get(),
                fiatExchangeRates = get(),
                sendFundsResultLocalizer = get()
            )
        }

        factory<SendStrategy<SendView>>("erc20Strategy") {
            Erc20SendStrategy(
                currencyState = get()
            )
        }

        factory { SunriverDeepLinkHelper(get()) }

        factory { KycDeepLinkHelper(get()) }

        factory { DeepLinkProcessor(get(), get(), get()) }

        factory { DeepLinkPersistence(get()) }

        factory { SunRiverCampaignAccountProviderAdapter(get()) as SunriverCampaignHelper.XlmAccountProvider }
    }

    factory { DateUtil(get()) }

    bean { PrngHelper(get(), get()) as PrngFixer }

    factory { PrivateKeyFactory() }

    bean { DynamicFeeCache() }
}
