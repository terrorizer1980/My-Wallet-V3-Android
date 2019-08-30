package piuk.blockchain.android.data.coinswebsocket.strategy

import com.blockchain.network.websocket.ConnectionEvent
import com.blockchain.network.websocket.WebSocket
import com.google.gson.Gson
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.formatWithUnit
import info.blockchain.wallet.ethereum.Erc20TokenData
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.coinswebsocket.models.AddressParam
import piuk.blockchain.android.data.coinswebsocket.models.Coin
import piuk.blockchain.android.data.coinswebsocket.models.CoinWebSocketInput
import piuk.blockchain.android.data.coinswebsocket.models.Entity
import piuk.blockchain.android.data.coinswebsocket.models.SocketRequest
import piuk.blockchain.android.data.coinswebsocket.models.SocketResponse
import piuk.blockchain.android.data.coinswebsocket.models.TransactionState
import piuk.blockchain.android.data.coinswebsocket.service.MessagesSocketHandler
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import timber.log.Timber
import java.math.BigDecimal
import java.util.Locale

class CoinsWebSocketStrategy(
    private val coinsWebSocket: WebSocket<String, String>,
    private val ethDataManager: EthDataManager,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    private val stringUtils: StringUtils,
    private val gson: Gson,
    private val erc20Account: Erc20Account
) {

    private var coinWebSocketInput: CoinWebSocketInput? = null
    private val compositeDisposable = CompositeDisposable()
    private var messagesSocketHandler: MessagesSocketHandler? = null

    fun updateInput(coinWebSocketInput: CoinWebSocketInput) {
        this.coinWebSocketInput = coinWebSocketInput
        subscribe(coinWebSocketInput)
    }

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

        compositeDisposable += coinsWebSocket.responses.subscribe {
            val socketResponse = gson.fromJson(it, SocketResponse::class.java)
            if (socketResponse.coin == null) return@subscribe
            when (socketResponse.coin) {
                Coin.ETH -> handleEthTransaction(socketResponse)
                Coin.BCH, Coin.BTC -> {
                } // todo handle transaction
            }
        }
    }

    private fun handleEthTransaction(socketResponse: SocketResponse) {
        if (socketResponse.isEthButNotReferredToPax()) {
            val transaction = socketResponse.transaction ?: return
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

        if (socketResponse.entity == Entity.TOKEN_ACCOUNT &&
            socketResponse.tokenTransfer != null &&
            socketResponse.tokenTransfer.to.equals(ethAddress(), true)
        ) {
            val tokenTransaction = socketResponse.tokenTransfer

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
                { messagesSocketHandler?.sendBroadcast(BalanceFragment.ACTION_INTENT) },
                { throwable -> Timber.e(throwable, "downloadEthTransactions failed") })
    }

    private fun updatePaxTransactions() {
        compositeDisposable += erc20Account.fetchAddressCompletable()
            .subscribe(
                { messagesSocketHandler?.sendBroadcast(BalanceFragment.ACTION_INTENT) },
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
        coinWebSocketInput = CoinWebSocketInput(ethAddress(), erc20Address(), erc20ContractAddress())
    }

    private fun erc20ContractAddress(): String =
        ethDataManager.getEthWallet()?.getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME)?.contractAddress ?: ""

    private fun erc20Address(): String =
        ethDataManager.getEthWallet()?.account?.address ?: swipeToReceiveHelper.getEthReceiveAddress()

    private fun ethAddress(): String =
        ethDataManager.getEthWallet()?.account?.address ?: swipeToReceiveHelper.getEthReceiveAddress()

    private fun subscribe(coinWebSocketInput: CoinWebSocketInput) {

        coinsWebSocket.send(gson.toJson(SocketRequest.SubscribeRequest(Entity.ACCOUNT,
            Coin.ETH,
            AddressParam.SimpleAddress(
                coinWebSocketInput.ethAddress
            ))))

        coinsWebSocket.send(gson.toJson(SocketRequest.SubscribeRequest(Entity.TOKEN_ACCOUNT,
            Coin.ETH,
            AddressParam.TokenedAddress(
                address = coinWebSocketInput.erc20Address,
                tokenAddress = coinWebSocketInput.erc20ContractAddress
            ))))
    }

    private fun ping() {
        coinsWebSocket.send(gson.toJson(SocketRequest.PingRequest))
    }

    private fun SocketResponse.isEthButNotReferredToPax() = entity == Entity.ACCOUNT &&
            !transaction?.to.equals(ethDataManager.getErc20TokenData(CryptoCurrency.PAX).contractAddress,
                true)
}