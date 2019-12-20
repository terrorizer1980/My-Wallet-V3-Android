package piuk.blockchain.android.ui.campaign

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.view.View
import com.blockchain.logging.CrashLogger
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.AirdropStatusList
import com.blockchain.swap.nabu.models.nabu.CampaignTransactionState
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_stx_airdrop_complete.view.*
import kotlinx.android.synthetic.main.dialog_stx_campaign_complete.view.cta_button
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.blockstackCampaignName
import java.lang.IllegalStateException
import java.text.DateFormat

class BlockstackAirdropCompleteSheet : PromoBottomSheet() {

    private val nabuToken: NabuToken by inject()
    private val nabu: NabuDataManager by inject()
    private val crashLogger: CrashLogger by inject()

    private val disposables = CompositeDisposable()

    override val layoutResource: Int = R.layout.dialog_stx_airdrop_complete

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }

        disposables += nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getAirdropCampaignStatus(token) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { renderUiElements(view, it) },
                onError = {
                    crashLogger.logException(it)
                    dismiss()
                }
            )
    }

    @SuppressLint("SetTextI18n")
    private fun renderUiElements(view: View, airdropList: AirdropStatusList) {
        val stxDrop = airdropList[blockstackCampaignName] ?: throw IllegalStateException("No STX airdrop found")

        // First up, check we're all valid and have actually received the airdrop;
        // bail out with an exception if we have not

        // For a valid completed airdrop, we are expecting to find a campaign transaction with a
        // "FinishedWithdrawal" state (yeah, it's nuts. But so many of these endpoints are...)
        val tx = stxDrop.txResponseList
            .firstOrNull {
                it.transactionState == CampaignTransactionState.FinishedWithdrawal
            } ?: throw IllegalStateException("No STX airdrop found in completed state")

        val date = DateFormat.getDateInstance(DateFormat.SHORT).format(tx.withdrawalAt)
        val fiat = FiatValue.fromMinor(tx.fiatCurrency, tx.fiatValue)

        val cryptoCurrency = CryptoCurrency.fromSymbol(tx.withdrawalCurrency)
            ?: throw IllegalStateException("Unknown crypto currency: ${tx.withdrawalCurrency}")

        val crypto = CryptoValue.fromMinor(cryptoCurrency, tx.withdrawalQuantity.toBigDecimal())

        view.date_value.text = date
        view.amount_value.text = "${crypto.formatOrSymbolForZero()} (${fiat.toStringWithSymbol()})"
    }

    override fun onCancel(dialog: DialogInterface) {
        disposables.clear()
        super.onCancel(dialog)
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    private fun onCtaClick() = dismiss()
}
