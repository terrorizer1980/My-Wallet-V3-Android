package piuk.blockchain.android.data.coinswebsocket.strategy

import com.blockchain.network.websocket.ConnectionEvent
import com.blockchain.network.websocket.WebSocket
import com.google.gson.Gson
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.formatWithUnit
import info.blockchain.wallet.ethereum.Erc20TokenData
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.coinswebsocket.models.AddressParam
import piuk.blockchain.android.data.coinswebsocket.models.BtcBchResponse
import piuk.blockchain.android.data.coinswebsocket.models.Coin
import piuk.blockchain.android.data.coinswebsocket.models.CoinWebSocketInput
import piuk.blockchain.android.data.coinswebsocket.models.Entity
import piuk.blockchain.android.data.coinswebsocket.models.EthResponse
import piuk.blockchain.android.data.coinswebsocket.models.EthTransaction
import piuk.blockchain.android.data.coinswebsocket.models.SocketRequest
import piuk.blockchain.android.data.coinswebsocket.models.SocketResponse
import piuk.blockchain.android.data.coinswebsocket.models.TransactionState
import piuk.blockchain.android.data.coinswebsocket.service.MessagesSocketHandler
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.BTCDenomination
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.events.TransactionsUpdatedEvent
import piuk.blockchain.androidcore.data.events.WalletAndTransactionsUpdatedEvent
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.websockets.WebSocketReceiveEvent
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.rxjava.IgnorableDefaultObserver
import timber.log.Timber
import java.math.BigDecimal
import java.util.Locale

class CoinsWebSocketStrategy(
    private val coinsWebSocket: WebSocket<String, String>,
    private val ethDataManager: EthDataManager,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    private val stringUtils: StringUtils,
    private val gson: Gson,
    private val rxBus: RxBus,
    private val currencyFormatManager: CurrencyFormatManager,
    private val erc20Account: Erc20Account,
    private val payloadDataManager: PayloadDataManager,
    private val bchDataManager: BchDataManager
) {

    private var coinWebSocketInput: CoinWebSocketInput? = null
    private val compositeDisposable = CompositeDisposable()
    private var messagesSocketHandler: MessagesSocketHandler? = null

    fun setMessagesHandler(messagesSocketHandler: MessagesSocketHandler) {
        this.messagesSocketHandler = messagesSocketHandler
    }

    fun open() {
        initInput()
        subscribeToEvents()
        coinsWebSocket.open()
    }

    private fun subscribeToEvents() {
        compositeDisposable += coinsWebSocket.connectionEvents.subscribe {
            when (it) {
                is ConnectionEvent.Connected -> run {
                    ping()
                    subscribe(coinWebSocketInput ?: return@run)
                }
            }
        }

        compositeDisposable += coinsWebSocket.responses.subscribe { response ->
            val socketResponse = gson.fromJson(response, SocketResponse::class.java)
            if (socketResponse.coin == null) return@subscribe
            when (socketResponse.coin) {
                Coin.ETH -> handleEthTransaction(response)
                Coin.BTC -> handleBtcTransaction(response)
                Coin.BCH -> handleBchTransaction(response)
            }
        }
    }

    private fun handleBtcTransaction(response: String) {
        val btcResponse = gson.fromJson(response, BtcBchResponse::class.java)
        val transaction = btcResponse.transaction ?: return

        var value = 0.toBigDecimal()
        var totalValue = 0.toBigDecimal()
        var inAddr: String? = null

        transaction.inputs.forEach { input ->
            input.prevOut?.let { output ->
                if (output.value != null) {
                    value = output.value
                }
                if (output.xpub != null) {
                    totalValue -= value
                } else if (output.addr != null) {
                    if (payloadDataManager.wallet?.containsLegacyAddress(output.addr) == true) {
                        totalValue -= value
                    } else if (inAddr == null) {
                        inAddr = output.addr
                    }
                }
            }
        }

        transaction.outputs.forEach { output ->
            output.value?.let {
                value = output.value
            }
            if (output.addr != null && transaction.hash != null) {
                rxBus.emitEvent(WebSocketReceiveEvent::class.java, WebSocketReceiveEvent(
                    output.addr,
                    transaction.hash
                ))
            }
            if (output.xpub != null) {
                totalValue += value
            } else if (output.addr != null && payloadDataManager.wallet?.containsLegacyAddress(output.addr) == true) {
                totalValue += value
            }
        }
        updateBtcBalancesAndTransactions()
    }

    private fun handleBchTransaction(response: String) {
        val bchResponse = gson.fromJson(response, BtcBchResponse::class.java)
        val transaction = bchResponse.transaction ?: return

        var value = 0.toBigDecimal()
        var totalValue = 0.toBigDecimal()
        var inAddr: String? = null

        transaction.inputs.forEach { input ->
            input.prevOut?.let { output ->
                if (output.value != null) {
                    value = output.value
                }
                if (output.xpub != null) {
                    totalValue -= value
                } else if (output.addr != null) {
                    if (bchDataManager.getLegacyAddressStringList().contains(output.addr)) {
                        totalValue -= value
                    } else if (inAddr == null) {
                        inAddr = output.addr
                    }
                }
            }
        }

        transaction.outputs.forEach { output ->
            output.value?.let {
                value = output.value
            }
            if (output.addr != null && transaction.hash != null) {
                rxBus.emitEvent(WebSocketReceiveEvent::class.java, WebSocketReceiveEvent(
                    output.addr,
                    transaction.hash
                ))
            }
            if (output.xpub != null) {
                totalValue += value
            } else if (output.addr != null && bchDataManager.getLegacyAddressStringList().contains(output.addr)) {
                totalValue += value
            }
        }
        updateBchBalancesAndTransactions()

        val title = stringUtils.getString(R.string.app_name)

        if (totalValue > BigDecimal.ZERO) {

            val marquee = stringUtils.getString(R.string.received_bitcoin_cash) +
                    " ${currencyFormatManager.getFormattedBchValueWithUnit(totalValue,
                        BTCDenomination.SATOSHI)}"

            var text = marquee
            text += " ${stringUtils.getString(R.string.from).toLowerCase(Locale.US)} $inAddr"
            messagesSocketHandler?.triggerNotification(
                title, marquee, text
            )
        }
    }

    private fun updateBtcBalancesAndTransactions() {
        payloadDataManager.updateAllBalances()
            .andThen(payloadDataManager.updateAllTransactions())
            .doOnComplete { rxBus.emitEvent(ActionEvent::class.java, WalletAndTransactionsUpdatedEvent()) }
            .subscribe(IgnorableDefaultObserver<Any>())
    }

    private fun updateBchBalancesAndTransactions() {
        bchDataManager.updateAllBalances()
            .andThen(bchDataManager.getWalletTransactions(50, 0))
            .doOnComplete { rxBus.emitEvent(ActionEvent::class.java, WalletAndTransactionsUpdatedEvent()) }
            .subscribe(IgnorableDefaultObserver<List<TransactionSummary>>())
    }

    private fun handleEthTransaction(response: String) {
        val ethResponse = gson.fromJson(response, EthResponse::class.java)

        if (ethResponse.transaction != null && ethResponse.isEthButNotReferredToPax()) {
            val transaction: EthTransaction = ethResponse.transaction
            if (transaction.state == TransactionState.CONFIRMED &&
                transaction.to.equals(ethAddress(), true)
            ) {
                val title = stringUtils.getString(R.string.app_name)
                val marquee = stringUtils.getString(R.string.received_ethereum) + " " +
                        Convert.fromWei(BigDecimal(transaction.value), Convert.Unit.ETHER) + " ETH"
                val text =
                    marquee + " " + stringUtils.getString(R.string.from).toLowerCase(Locale.US) + " " + transaction.from

                messagesSocketHandler?.triggerNotification(
                    title, marquee, text
                )
            }
            updateEthTransactions()
        }

        if (ethResponse.entity == Entity.TokenAccount &&
            ethResponse.tokenTransfer != null &&
            ethResponse.tokenTransfer.to.equals(ethAddress(), true)
        ) {
            val tokenTransaction = ethResponse.tokenTransfer

            val title = stringUtils.getString(R.string.app_name)
            val marquee = stringUtils.getString(R.string.received_usd_pax) + " " +
                    CryptoValue.usdPaxFromMinor(tokenTransaction.value).formatWithUnit()
            val text =
                marquee + " " + stringUtils.getString(R.string.from).toLowerCase(Locale.US) +
                        " " + tokenTransaction.from

            messagesSocketHandler?.triggerNotification(
                title, marquee, text
            )
            updatePaxTransactions()
        }
    }

    private fun updateEthTransactions() {
        compositeDisposable += downloadEthTransactions()
            .subscribe(
                { messagesSocketHandler?.sendBroadcast(TransactionsUpdatedEvent()) },
                { throwable -> Timber.e(throwable, "downloadEthTransactions failed") })
    }

    fun subscribeToXpubBtc(xpub: String) {
        val updatedList = (coinWebSocketInput?.xPubsBtc?.toMutableList() ?: mutableListOf()) + xpub
        coinWebSocketInput = coinWebSocketInput?.copy(xPubsBtc = updatedList)

        subscribeToXpub(Coin.BTC, xpub)
    }

    fun subscribeToExtraBtcAddress(address: String) {
        val updatedList = (coinWebSocketInput?.receiveBtcAddresses?.toMutableList()
            ?: mutableListOf()) + address
        coinWebSocketInput = coinWebSocketInput?.copy(receiveBtcAddresses = updatedList)
        coinsWebSocket.send(gson.toJson(SocketRequest.SubscribeRequest(Entity.Account,
            Coin.BTC,
            AddressParam.SimpleAddress(
                address = address
            ))))
    }

    private fun updatePaxTransactions() {
        compositeDisposable += erc20Account.fetchAddressCompletable()
            .subscribe(
                { messagesSocketHandler?.sendBroadcast(TransactionsUpdatedEvent()) },
                { throwable -> Timber.e(throwable, "downloadPaxTransactions failed") })
    }

    fun close() {
        coinsWebSocket.close()
        compositeDisposable.clear()
    }

    private fun downloadEthTransactions(): Observable<CombinedEthModel> {
        return ethDataManager.fetchEthAddress()
            .applySchedulers()
    }

    private fun initInput() {
        coinWebSocketInput = CoinWebSocketInput(ethAddress(),
            erc20Address(),
            erc20ContractAddress(),
            btcReceiveAddresses(),
            bchReceiveAddresses(),
            xPubsBtc(),
            xPubsBch())
    }

    private fun xPubsBch(): List<String> {
        val nbAccounts: Int
        return if (payloadDataManager.wallet?.isUpgraded == true) {
            nbAccounts = bchDataManager.getActiveXpubs().size
            val xpubs = mutableListOf<String>()
            for (i in 0 until nbAccounts) {
                val activeXpubs = bchDataManager.getActiveXpubs()
                if (activeXpubs[i] != null && activeXpubs[i].isNotEmpty()) {
                    xpubs.add(bchDataManager.getActiveXpubs()[i])
                }
            }
            xpubs
        } else {
            emptyList()
        }
    }

    private fun xPubsBtc(): List<String> {
        val nbAccounts: Int
        if (payloadDataManager.wallet?.isUpgraded == true) {
            nbAccounts = try {
                payloadDataManager.totalAccounts()
            } catch (e: IndexOutOfBoundsException) {
                0
            }

            val xpubs = mutableListOf<String>()
            for (i in 0 until nbAccounts) {
                val xPub = payloadDataManager.wallet!!.hdWallets[0].accounts[i].xpub
                if (xPub != null && xPub.isNotEmpty()) {
                    xpubs.add(xPub)
                }
            }
            return xpubs
        } else {
            return emptyList()
        }
    }

    private fun btcReceiveAddresses(): List<String> {
        when {
            payloadDataManager.wallet != null -> {
                val nbLegacy = payloadDataManager.wallet?.legacyAddressList?.size ?: 0
                val addresses = mutableListOf<String>()
                for (i in 0 until nbLegacy) {
                    val address = payloadDataManager.wallet?.legacyAddressList?.get(i)?.address
                    if (address.isNullOrEmpty().not()) {
                        addresses.add(address!!)
                    }
                }
                return addresses
            }
            swipeToReceiveHelper.getBitcoinReceiveAddresses().isNotEmpty() -> {
                val addresses = mutableListOf<String>()
                val receiveAddresses =
                    swipeToReceiveHelper.getBitcoinReceiveAddresses()
                receiveAddresses.forEach { address ->
                    addresses.add(address)
                }
                return addresses
            }
            else -> return emptyList()
        }
    }

    private fun bchReceiveAddresses(): List<String> {
        when {
            payloadDataManager.wallet != null -> {
                val nbLegacy = bchDataManager.getLegacyAddressStringList().size
                val addrs = mutableListOf<String>()
                for (i in 0 until nbLegacy) {
                    val address = bchDataManager.getLegacyAddressStringList().get(i)
                    if (address.isNotEmpty()) {
                        addrs.add(address)
                    }
                }

                return addrs
            }
            swipeToReceiveHelper.getBitcoinCashReceiveAddresses().isNotEmpty() -> {
                val addrs = mutableListOf<String>()
                val receiveAddresses =
                    swipeToReceiveHelper.getBitcoinCashReceiveAddresses()
                receiveAddresses.forEach { address ->
                    addrs.add(address)
                }
                return addrs
            }
            else -> return emptyList()
        }
    }

    private fun erc20ContractAddress(): String =
        ethDataManager.getEthWallet()?.getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME)?.contractAddress
            ?: ""

    private fun erc20Address(): String =
        ethDataManager.getEthWallet()?.account?.address ?: swipeToReceiveHelper.getEthReceiveAddress()

    private fun ethAddress(): String =
        ethDataManager.getEthWallet()?.account?.address ?: swipeToReceiveHelper.getEthReceiveAddress()

    private fun subscribe(coinWebSocketInput: CoinWebSocketInput) {

        coinWebSocketInput.receiveBtcAddresses.forEach { address ->
            coinsWebSocket.send(gson.toJson(SocketRequest.SubscribeRequest(Entity.Account,
                Coin.BTC,
                AddressParam.SimpleAddress(
                    address = address
                ))))
        }

        coinWebSocketInput.receiveBhcAddresses.forEach { address ->
            coinsWebSocket.send(gson.toJson(SocketRequest.SubscribeRequest(Entity.Account,
                Coin.BTC,
                AddressParam.SimpleAddress(
                    address = address
                ))))
        }

        coinWebSocketInput.xPubsBtc.forEach { xPub ->
            subscribeToXpub(Coin.BTC, xPub)
        }

        coinWebSocketInput.xPubsBch.forEach { xPub ->
            subscribeToXpub(Coin.BCH, xPub)
        }

        coinsWebSocket.send(gson.toJson(SocketRequest.SubscribeRequest(Entity.Account,
            Coin.ETH,
            AddressParam.SimpleAddress(
                coinWebSocketInput.ethAddress
            ))))

        coinsWebSocket.send(gson.toJson(SocketRequest.SubscribeRequest(Entity.TokenAccount,
            Coin.ETH,
            AddressParam.TokenedAddress(
                address = coinWebSocketInput.erc20Address,
                tokenAddress = coinWebSocketInput.erc20ContractAddress
            ))))
    }

    private fun subscribeToXpub(coin: Coin, xpub: String) {

        coinsWebSocket.send(gson.toJson(SocketRequest.SubscribeRequest(Entity.Xpub,
            coin,
            AddressParam.SimpleAddress(
                address = xpub
            ))))
    }

    private fun ping() {
        coinsWebSocket.send(gson.toJson(SocketRequest.PingRequest))
    }

    private fun EthResponse.isEthButNotReferredToPax() = entity == Entity.Account &&
            !transaction?.to.equals(ethDataManager.getErc20TokenData(CryptoCurrency.PAX).contractAddress,
                true)

    private fun PayloadDataManager.totalAccounts(): Int =
        wallet?.hdWallets?.get(0)?.accounts?.size ?: 0
}