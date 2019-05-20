package com.blockchain.accounts

import com.blockchain.wallet.toAccountReference
import info.blockchain.balance.AccountReference
import io.reactivex.Single
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.R
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class BtcAccountListAdapter(private val payloadDataManager: PayloadDataManager) : AccountList {

    override fun defaultAccountReference() =
        payloadDataManager.defaultAccount.toAccountReference()
}

internal class BchAccountListAdapter(private val bchPayloadDataManager: BchDataManager) : AccountList {

    override fun defaultAccountReference() =
        with(bchPayloadDataManager) {
            getAccountMetadataList()[getDefaultAccountPosition()].toAccountReference()
        }
}

internal class EthAccountListAdapter(private val ethDataManager: EthDataManager) : AccountList {

    override fun defaultAccountReference() =
        (ethDataManager.getEthWallet() ?: throw Exception("No ether wallet found"))
            .account.toAccountReference()
}

internal class BtcAsyncAccountListAdapter(private val payloadDataManager: PayloadDataManager) :
    AsyncAccountList {

    override fun accounts(): Single<List<AccountReference>> =
        Single.just(payloadDataManager.accounts
            .filter { !it.isArchived }
            .map { it.toAccountReference() })
}

internal class BchAsyncAccountListAdapter(private val bchPayloadDataManager: BchDataManager) :
    AsyncAccountList {

    override fun accounts(): Single<List<AccountReference>> =
        Single.just(bchPayloadDataManager.getAccountMetadataList()
            .filter { !it.isArchived }
            .map { it.toAccountReference() })
}

internal class EthAsyncAccountListAdapter(private val ethAccountListAdapter: EthAccountListAdapter) :
    AsyncAccountList {

    override fun accounts(): Single<List<AccountReference>> =
        Single.just(listOf(ethAccountListAdapter.defaultAccountReference()))
}

internal class PaxAsyncAccountList(private val ethDataManager: EthDataManager, private val stringUtils: StringUtils) :
    AsyncAccountList {
    override fun accounts(): Single<List<AccountReference>> =
        Single.just(listOf(AccountReference.Pax(stringUtils.getString(R.string.pax_default_account_label),
            ethDataManager.getEthWallet()?.account?.address ?: throw Exception("No ether wallet found"), "")))
}
