package piuk.blockchain.android.ui.campaign

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.blockchain.logging.CrashLogger
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.AirdropStatus
import com.blockchain.swap.nabu.models.nabu.AirdropStatusList
import com.blockchain.swap.nabu.models.nabu.CampaignState
import com.blockchain.swap.nabu.models.nabu.CampaignTransactionState
import com.blockchain.swap.nabu.models.nabu.UserCampaignState
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_airdrop_status.view.*
import kotlinx.android.synthetic.main.dialog_stx_campaign_complete.view.cta_button
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.campaign.sunriverCampaignName
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import java.lang.IllegalStateException
import java.text.DateFormat
import java.util.Date

class AirdropStatusSheet : PromoBottomSheet() {

    private val nabuToken: NabuToken by inject()
    private val nabu: NabuDataManager by inject()
    private val crashLogger: CrashLogger by inject()

    private val disposables = CompositeDisposable()

    override val layoutResource: Int = R.layout.dialog_airdrop_status

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

        view.title.setText(R.string.airdrop_sheet_stx_title)
        view.body.setText(R.string.airdrop_sheet_stx_body)
        view.icon_crypto.setImageResource(R.drawable.ic_logo_stx)

        renderStatus(view, stxDrop)
        renderDate(view, stxDrop)
        renderAmount(view, stxDrop)
    }

    private fun renderStatus(view: View, status: AirdropStatus) {
        when (status.airdropStatus()) {
            AirdropUserState.UNKNOWN ->
                setStatusView(
                    view,
                    R.string.airdrop_status_unknown,
                    R.color.black,
                    R.drawable.bkgd_status_unknown
                )
            AirdropUserState.EXPIRED ->
                setStatusView(
                    view,
                    R.string.airdrop_status_expired,
                    R.color.grey_600,
                    R.drawable.bkgd_status_expired
                )
            AirdropUserState.PENDING ->
                setStatusView(
                    view,
                    R.string.airdrop_status_pending,
                    R.color.blue_600,
                    R.drawable.bkgd_status_pending
                )
            AirdropUserState.RECEIVED ->
                setStatusView(
                    view,
                    R.string.airdrop_status_received,
                    R.color.green_600,
                    R.drawable.bkgd_status_received
                )
        }
    }

    private fun setStatusView(
        view: View,
        @StringRes message: Int,
        @ColorRes textColour: Int,
        @DrawableRes background: Int
    ) {
        with(view.status_value) {
            setText(message)
            setTextColor(ContextCompat.getColor(context, textColour))
            setBackground(ContextCompat.getDrawable(context, background))
        }
    }

    private fun renderDate(view: View, status: AirdropStatus) {
        val date = status.airdropDate()
        date?.let {
            val formatted = DateFormat.getDateInstance(DateFormat.SHORT).format(status.airdropDate())
            view.date_value.text = formatted
        }
    }

    @SuppressLint("SetTextI18n")
    private fun renderAmount(view: View, status: AirdropStatus) {
        val tx = status.txResponseList
            .firstOrNull {
                it.transactionState == CampaignTransactionState.FinishedWithdrawal
            }

        tx?.let {
            val fiat = FiatValue.fromMinor(tx.fiatCurrency, tx.fiatValue)

            val cryptoCurrency = CryptoCurrency.fromSymbol(tx.withdrawalCurrency)
                ?: throw IllegalStateException("Unknown crypto currency: ${tx.withdrawalCurrency}")

            val crypto = CryptoValue.fromMinor(cryptoCurrency, tx.withdrawalQuantity.toBigDecimal())

            view.amount_value.text =
                "${crypto.formatOrSymbolForZero()} (${fiat.toStringWithSymbol()})"
        }

        with(view) {
            amount_label.goneIf(tx == null)
            amount_value.goneIf(tx == null)
            divider_amount.goneIf(tx == null)
        }
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

private fun AirdropStatus.airdropDate(): Date? {
    // The backend response is... weird. For an STX airdrop, we only have a valid date when the airdrop is received.
    // Any other time - say when accessed via the airdrop center - iOS and web default to 29/02/2020. This is clearly
    // both nuts and meaningless, so I'm not doing that. At least if we display the current date it reflects the
    // date when the displayed status is valid. Pointless but, um, so it goes...

    return if (txResponseList.isNullOrEmpty()) {
        when (campaignName) {
            blockstackCampaignName -> if (userState == UserCampaignState.RewardReceived) updatedAt else Date()
            sunriverCampaignName -> updatedAt
            else -> null
        }
    } else {
        txResponseList.maxBy { it.withdrawalAt }!!.withdrawalAt
    }
}

enum class AirdropUserState {
    UNKNOWN,
    EXPIRED,
    PENDING,
    RECEIVED
}

fun AirdropStatus.airdropStatus() =
    if (campaignState == CampaignState.Ended) {
        when (userState) {
            UserCampaignState.RewardReceived -> AirdropUserState.RECEIVED
            UserCampaignState.TaskFinished -> AirdropUserState.PENDING
            else -> AirdropUserState.EXPIRED
        }
    } else {
        AirdropUserState.UNKNOWN
    }
