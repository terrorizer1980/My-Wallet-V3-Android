package piuk.blockchain.android.ui.dashboard.assetdetails

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.blockchain.balance.currencyName
import com.blockchain.balance.setCoinIcon
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_dashboared_asset_details.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class AssetDetailSheet : SlidingModalBottomDialog() {

    val compositeDisposable = CompositeDisposable()

    interface Host {
        fun gotoSendFor(cryptoCurrency: CryptoCurrency)
        fun onSheetClosed()
    }

    private val host: Host by lazy {
        parentFragment as? Host ?: throw IllegalStateException("Host fragment is not a AssetDetailSheet.Host")
    }

    private val cryptoCurrency: CryptoCurrency by lazy {
        arguments?.getSerializable(ARG_CRYPTO_CURRENCY) as? CryptoCurrency
            ?: throw IllegalArgumentException("No cryptoCurrency specified")
    }

    private val assetSelect: AssetTokenLookup by inject()
    private val token: AssetTokens by lazy {
        assetSelect[cryptoCurrency]
    }

    override val layoutResource: Int
        get() = R.layout.dialog_dashboared_asset_details

    override fun initControls(view: View) {
        with(view) {
            asset_name.text = getString(cryptoCurrency.currencyName())
            balance_for_asset.text = getString(R.string.dashboard_balance_for_asset, cryptoCurrency.symbol)
            asset_icon.setCoinIcon(cryptoCurrency)

            compositeDisposable += token.totalBalance().zipWith(token.exchangeRate())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy({
                    println("error fetching balance $it")
                }) { (cryptoBalance, fiatPrice) ->
                    asset_balance_crypto.text = cryptoBalance.toStringWithSymbol()
                    asset_balance_fiat.text =
                        FiatValue.fromMajor(fiatPrice.currencyCode,
                            fiatPrice.toBigDecimal() * cryptoBalance.toBigDecimal()).toStringWithSymbol()
                }

            btn_send.setOnClickListener {
                dismiss()
                host.gotoSendFor(cryptoCurrency)
            }
        }
    }

    override fun onSheetHidden() {
        host.onSheetClosed()
        super.onSheetHidden()
    }

    override fun onCancel(dialog: DialogInterface?) {
        host.onSheetClosed()
        super.onCancel(dialog)
    }

    companion object {
        private const val ARG_CRYPTO_CURRENCY = "crypto"

        fun newInstance(cryptoCurrency: CryptoCurrency): AssetDetailSheet {
            return AssetDetailSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CRYPTO_CURRENCY, cryptoCurrency)
                }
            }
        }
    }
}