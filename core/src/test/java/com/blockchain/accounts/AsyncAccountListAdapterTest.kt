package com.blockchain.accounts

import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.payload.data.Account
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test

class AsyncAccountListAdapterTest {

    @Test
    fun `BtcAsyncAccountListAdapter account list`() {
        (BtcAccountListAdapter(mock {
            on { this.accounts } `it returns` listOf(
                Account().apply {
                    label = "Account 1"
                    xpub = "xpub 1"
                },
                Account().apply {
                    label = "Account 2"
                    xpub = "xpub 2"
                })
        }) as AccountList)
            .testAccountList() `should equal`
            listOf(
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "Account 1", xpub = "xpub 1"),
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "Account 2", xpub = "xpub 2")
            )
    }

    @Test
    fun `BtcAsyncAccountListAdapter account list - excludes archived`() {
        (BtcAccountListAdapter(mock {
            on { this.accounts } `it returns` listOf(
                Account().apply {
                    label = "Account 1"
                    xpub = "xpub 1"
                },
                Account().apply {
                    label = "Account 2"
                    xpub = "xpub 2"
                    isArchived = true
                })
        }) as AccountList)
            .testAccountList() `should equal`
            listOf(
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "Account 1", xpub = "xpub 1")
            )
    }

    @Test
    fun `BchAsyncAccountListAdapter account list`() {
        (BchAccountListAdapter(mock {
            on { this.getAccountMetadataList() } `it returns` listOf(
                GenericMetadataAccount().apply {
                    label = "The first bch account"
                    xpub = "xpub 1"
                },
                GenericMetadataAccount().apply {
                    label = "The second bch account"
                    xpub = "xpub 2"
                }
            )
        }) as AccountList)
            .testAccountList() `should equal`
            listOf(
                AccountReference.BitcoinLike(CryptoCurrency.BCH, "The first bch account", xpub = "xpub 1"),
                AccountReference.BitcoinLike(CryptoCurrency.BCH, "The second bch account", xpub = "xpub 2")
            )
    }

    @Test
    fun `BchAsyncAccountListAdapter account list - excludes archived`() {
        (BchAccountListAdapter(mock {
            on { this.getAccountMetadataList() } `it returns` listOf(
                GenericMetadataAccount().apply {
                    label = "The first bch account"
                    xpub = "xpub 1"
                },
                GenericMetadataAccount().apply {
                    label = "The second bch account"
                    xpub = "xpub 2"
                    isArchived = true
                }
            )
        }) as AccountList)
            .testAccountList() `should equal`
            listOf(
                AccountReference.BitcoinLike(CryptoCurrency.BCH, "The first bch account", xpub = "xpub 1")
            )
    }

    @Test
    fun `EtherAsyncAccountListAdapter account list`() {
        val accountLabel = "The default eth account"
        val accountAddr = "0x1Address"

        val ethereumAccount = mock<EthereumAccount> {
            on { label } `it returns` accountLabel
            on { address } `it returns` accountAddr
        }

        val wallet = mock<EthereumWallet> {
            on { account } `it returns` ethereumAccount
        }

        val accountReference = AccountReference.Ethereum(accountLabel, accountAddr)

        (EthAccountListAdapter(
            mock {
                on { getEthWallet() } `it returns` wallet
            }
        ) as AccountList)
            .testAccountList() `should equal`
            listOf(
                accountReference
            )
    }
}

private fun AccountList.testAccountList(): List<AccountReference> =
    accounts()
        .test()
        .assertComplete()
        .values()
        .single()
