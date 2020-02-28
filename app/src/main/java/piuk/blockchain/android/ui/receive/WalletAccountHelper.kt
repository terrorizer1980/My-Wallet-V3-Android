package piuk.blockchain.android.ui.receive

import com.blockchain.logging.CrashLogger
import com.blockchain.sunriver.XlmDataManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import org.bitcoinj.core.Address
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import timber.log.Timber
import java.math.BigInteger
import java.util.Collections

class WalletAccountHelper(
    private val payloadManager: PayloadManager,
    private val stringUtils: StringUtils,
    private val ethDataManager: EthDataManager,
    private val paxAccount: Erc20Account,
    private val bchDataManager: BchDataManager,
    private val xlmDataManager: XlmDataManager,
    private val environmentSettings: EnvironmentConfig,
    private val crashLogger: CrashLogger
) {
    /**
     * Returns a list of [ItemAccount] objects containing both HD accounts and [LegacyAddress]
     * objects, eg from importing accounts.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    @Deprecated("XLM needs singles - this needs to go")
    fun getAccountItems(cryptoCurrency: CryptoCurrency): List<ItemAccount> =
        try {
            when (cryptoCurrency) {
                CryptoCurrency.BTC -> allBtcAccountItems()
                CryptoCurrency.BCH -> allBchAccountItems()
                CryptoCurrency.ETHER -> getEthAccount()
                CryptoCurrency.XLM -> throw IllegalArgumentException("XLM is not supported here")
                CryptoCurrency.PAX -> getPaxAccount()
                CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            }
        } catch (t: Throwable) {
            crashLogger.logException(t)
            emptyList()
        }

    /**
     * Returns a list of [ItemAccount] objects containing both HD accounts and [LegacyAddress]
     * objects, eg from importing accounts.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun accountItems(cryptoCurrency: CryptoCurrency): Single<List<ItemAccount>> =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> Single.just(allBtcAccountItems())
            CryptoCurrency.BCH -> Single.just(allBchAccountItems())
            CryptoCurrency.ETHER -> Single.just(getEthAccount())
            CryptoCurrency.XLM -> getXlmAccount()
            CryptoCurrency.PAX -> Single.just(getPaxAccount())
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        }

    private fun allBtcAccountItems() = getHdAccounts() + getLegacyBtcAddresses()

    private fun allBchAccountItems() = getHdBchAccounts() + getLegacyBchAddresses()

    /**
     * Returns a list of [ItemAccount] objects containing only HD accounts.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getHdAccounts(): List<ItemAccount> {
        val list = payloadManager.payload?.hdWallets?.get(0)?.accounts
            ?: Collections.emptyList<Account>()
        // Skip archived account
        return list.filterNot { it.isArchived }
            .map {
                ItemAccount(
                    label = it.label,
                    balance = CryptoValue.fromMinor(CryptoCurrency.BTC, payloadManager.getAddressBalance(it.xpub)),
                    accountObject = it,
                    address = it.xpub,
                    type = ItemAccount.TYPE.SINGLE_ACCOUNT
                )
            }
    }

    /**
     * Returns a list of [ItemAccount] objects containing only HD accounts.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getHdBchAccounts(): List<ItemAccount> = bchDataManager.getActiveAccounts()
        // Skip archived account
        .filterNot { it.isArchived }
        .map {
            ItemAccount(
                label = it.label,
                balance = CryptoValue.fromMinor(CryptoCurrency.BCH, payloadManager.getAddressBalance(it.xpub)),
                accountObject = it,
                address = it.xpub,
                type = ItemAccount.TYPE.SINGLE_ACCOUNT
            )
        }

    /**
     * Returns a list of [ItemAccount] objects containing only [LegacyAddress] objects.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getLegacyBtcAddresses(): List<ItemAccount> {
        val list = payloadManager.payload?.legacyAddressList
            ?: Collections.emptyList<LegacyAddress>()
        // Skip archived address
        return list.filterNot { it.tag == LegacyAddress.ARCHIVED_ADDRESS }
            .map {
                ItemAccount(
                    label = makeLabel(it),
                    balance = CryptoValue.fromMinor(CryptoCurrency.BTC, getAddressAbsoluteBalance(it)),
                    tag = checkTag(it),
                    accountObject = it,
                    address = it.address
                )
            }
    }

    /**
     * Returns a list of [ItemAccount] objects containing only [LegacyAddress] objects which also
     * have a BCH balance.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getLegacyBchAddresses(): List<ItemAccount> {
        val list = payloadManager.payload?.legacyAddressList ?: emptyList()

        // Skip archived address
        return list.filterNot { it.tag == LegacyAddress.ARCHIVED_ADDRESS }
            .filterNot {
                bchDataManager.getAddressBalance(it.address).compareTo(BigInteger.ZERO) == 0
            }.map {
                val cashAddress = Address.fromBase58(
                    environmentSettings.bitcoinCashNetworkParameters,
                    it.address
                ).toCashAddress().removeBchUri()

                ItemAccount(
                    label = makeLabel(it),
                    balance = CryptoValue.fromMinor(CryptoCurrency.BCH, getBchAddressAbsoluteBalance(it)),
                    tag = checkTag(it),
                    accountObject = it,
                    address = cashAddress
                )
            }
    }

    // If address has no label, we'll display address
    private fun makeLabel(address: LegacyAddress): String {
        var labelOrAddress: String? = address.label
        if (labelOrAddress == null || labelOrAddress.trim { it <= ' ' }.isEmpty()) {
            labelOrAddress = address.address
        }
        return labelOrAddress ?: ""
    }

    // Watch-only tag - we'll ask for xpriv scan when spending from
    private fun checkTag(address: LegacyAddress): String =
        if(address.isWatchOnly) {
            stringUtils.getString(R.string.watch_only)
        } else {
            ""
        }

    /**
     * Returns a list of [ItemAccount] objects containing only [LegacyAddress] objects,
     * specifically from the list of address book entries.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getAddressBookEntries() = payloadManager.payload?.addressBook?.map {
        val address = it.address ?: ""
        ItemAccount(
            if (it.label.isNullOrEmpty()) address else it.label,
            null,
            stringUtils.getString(R.string.address_book_label),
            null,
            address
        )
    } ?: emptyList()

    fun getDefaultOrFirstFundedAccount(cryptoCurrency: CryptoCurrency): ItemAccount? =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> getDefaultOrFirstFundedBtcAccount()
            CryptoCurrency.BCH -> getDefaultOrFirstFundedBchAccount()
            CryptoCurrency.ETHER -> getDefaultEthAccount()
            CryptoCurrency.XLM -> throw IllegalArgumentException("XLM is not supported here")
            CryptoCurrency.PAX -> getDefaultPaxAccount()
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        }

    fun getEthAccount() =
        getDefaultEthAccount()?.let { listOf(it) } ?: emptyList()

    fun getPaxAccount() =
        getDefaultPaxAccount()?.let { listOf(it) } ?: emptyList()

    fun getXlmAccount(): Single<List<ItemAccount>> =
        getDefaultXlmAccountItem().map { listOf(it) }

    /**
     * Returns the balance of an [Account] in Satoshis (BTC)
     */
    private fun getAccountBtcAbsoluteBalance(account: Account) =
        payloadManager.getAddressBalance(account.xpub)

    private fun getAccountBchAbsoluteBalance(account: GenericMetadataAccount) =
        bchDataManager.getAddressBalance(account.xpub)

    private fun getAddressAbsoluteBalance(legacyAddress: LegacyAddress) =
        payloadManager.getAddressBalance(legacyAddress.address)

    /**
     * Returns the balance of a [LegacyAddress] in Satoshis
     */
    private fun getBchAddressAbsoluteBalance(legacyAddress: LegacyAddress) =
        bchDataManager.getAddressBalance(legacyAddress.address)

    private fun getDefaultOrFirstFundedBtcAccount(): ItemAccount? {
        val hdWallet = payloadManager.payload?.hdWallets?.get(0)

        var account: Account = hdWallet?.accounts?.get(hdWallet.defaultAccountIdx) ?: return null

        if (getAccountBtcAbsoluteBalance(account) <= BigInteger.ZERO)
            for (funded in hdWallet.accounts) {
                if (!funded.isArchived && getAccountBtcAbsoluteBalance(funded) > BigInteger.ZERO) {
                    account = funded
                    break
                }
            }

        return ItemAccount(
            label = account.label,
            balance = CryptoValue.fromMinor(CryptoCurrency.BTC, payloadManager.getAddressBalance(account.xpub)),
            accountObject = account,
            address = account.xpub
        )
    }

    private fun getDefaultOrFirstFundedBchAccount(): ItemAccount? {
        var account = bchDataManager.getDefaultGenericMetadataAccount() ?: return null

        if (getAccountBchAbsoluteBalance(account) <= BigInteger.ZERO)
            for (funded in bchDataManager.getActiveAccounts()) {
                if (getAccountBchAbsoluteBalance(funded) > BigInteger.ZERO) {
                    account = funded
                    break
                }
            }

        return ItemAccount(
            label = account.label,
            balance = CryptoValue.fromMinor(CryptoCurrency.BCH, getAccountBchAbsoluteBalance(account)),
            accountObject = account,
            address = account.xpub
        )
    }

    private fun getDefaultEthAccount(): ItemAccount? {
        val ethModel = ethDataManager.getEthResponseModel()
        val ethAccount = ethDataManager.getEthWallet()?.account
        val balance = CryptoValue.etherFromWei(ethModel?.getTotalBalance() ?: BigInteger.ZERO)

        return if (ethAccount == null) {
            Timber.e("Invalid ETHER account: no account")
            null
        } else {
            ItemAccount(
                label = ethAccount.label ?: "",
                balance = balance,
                accountObject = ethAccount,
                address = ethAccount.address
            )
        }
    }

    private fun getDefaultPaxAccount(): ItemAccount? {
        val erc20DataModel = paxAccount.getErc20Model()
        val ethAccount = ethDataManager.getEthWallet()?.account
        val balance = erc20DataModel?.totalBalance ?: CryptoValue.ZeroPax

        return if (ethAccount == null) {
            Timber.e("Invalid pax account: no account")
            null
        } else {
            ItemAccount(
                label = ethAccount.label,
                balance = balance,
                accountObject = ethAccount,
                address = ethAccount.address
            )
        }
    }

    private fun getDefaultXlmAccountItem() =
        xlmDataManager.defaultAccount()
            .zipWith(xlmDataManager.getBalance())
            .map { (account, balance) ->
                ItemAccount(
                    label = account.label,
                    balance = balance,
                    accountObject = null,
                    address = account.accountId
                )
            }

    /**
     * Returns a list of [ItemAccount] objects containing both HD accounts and [LegacyAddress]
     * objects, eg from importing accounts.
     */
    fun getAccountItemsForOverview(cryptoCurrency: CryptoCurrency): Single<List<ItemAccount>> =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> Single.just(getBtcOverviewList())
            CryptoCurrency.BCH -> Single.just(getBchOverviewList())
            CryptoCurrency.ETHER -> Single.just(getEthOverviewList())
            CryptoCurrency.XLM -> getDefaultXlmAccountItem().map { listOf(it) }
            CryptoCurrency.PAX -> Single.just(getPaxOverviewList())
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        }

    private fun getEthOverviewList(): List<ItemAccount> = getEthAccount()

    private fun getPaxOverviewList(): List<ItemAccount> = getPaxAccount()

    private fun getBchOverviewList(): MutableList<ItemAccount> {
        return mutableListOf<ItemAccount>().apply {

            val legacyAddresses = getLegacyBchAddresses()
            val accounts = getHdBchAccounts()

            // Create "All Accounts" if necessary
            if (accounts.size > 1 || legacyAddresses.isNotEmpty()) {
                add(getBchWalletAccountItem())
            }
            addAll(accounts)

            // Create consolidated "Imported Addresses"
            if (legacyAddresses.isNotEmpty()) {
                add(getBchImportedAddressesAccountItem())
            }
        }
    }

    private fun getBtcOverviewList(): List<ItemAccount> {
        return mutableListOf<ItemAccount>().apply {

            val legacyAddresses = getLegacyBtcAddresses()
            val accounts = getHdAccounts()

            // Create "All Accounts" if necessary
            if (accounts.size > 1 || legacyAddresses.isNotEmpty()) {
                add(getBtcWalletAccountItem())
            }
            addAll(accounts)

            // Create consolidated "Imported Addresses"
            if (legacyAddresses.isNotEmpty()) {
                add(getBtcImportedAddressesAccountItem())
            }
        }.toList()
    }

    private fun getBtcWalletAccountItem(): ItemAccount {
        val bigIntBalance = payloadManager.walletBalance

        return ItemAccount(
            label = stringUtils.getString(R.string.all_accounts),
            balance = CryptoValue.fromMinor(CryptoCurrency.BTC, bigIntBalance),
            type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
        )
    }

    private fun getBchWalletAccountItem(): ItemAccount {
        val bigIntBalance = bchDataManager.getWalletBalance()

        return ItemAccount(
            label = stringUtils.getString(R.string.bch_all_accounts),
            balance = CryptoValue.fromMinor(CryptoCurrency.BCH, bigIntBalance),
            type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
        )
    }

    private fun getBtcImportedAddressesAccountItem(): ItemAccount {
        val bigIntBalance = payloadManager.importedAddressesBalance

        return ItemAccount(
            label = stringUtils.getString(R.string.imported_addresses),
            balance = CryptoValue.fromMinor(CryptoCurrency.BTC, bigIntBalance),
            type = ItemAccount.TYPE.ALL_LEGACY
        )
    }

    private fun getBchImportedAddressesAccountItem(): ItemAccount {
        val bigIntBalance = bchDataManager.getImportedAddressBalance()

        return ItemAccount(
            label = stringUtils.getString(R.string.bch_imported_addresses),
            balance = CryptoValue.fromMinor(CryptoCurrency.BCH, bigIntBalance),
            type = ItemAccount.TYPE.ALL_LEGACY
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // Extension functions
    // /////////////////////////////////////////////////////////////////////////

    private fun String.removeBchUri(): String = this.replace("bitcoincash:", "")

    fun hasMultipleEntries(cryptoCurrency: CryptoCurrency) =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> allBtcAccountItems().size + getAddressBookEntries().size
            CryptoCurrency.ETHER -> getEthAccount().size
            CryptoCurrency.BCH -> allBchAccountItems().size
            CryptoCurrency.XLM -> 1 // TODO("AND-1511") Ideally we're getting real account count here, even if one
            CryptoCurrency.PAX -> getEthAccount().size
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        } > 1
}
