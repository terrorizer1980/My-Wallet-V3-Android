package piuk.blockchain.android.ui.settings.preferences

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.CardStatus
import kotlinx.android.synthetic.main.preference_cards_layout.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.icon
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class CardPreference(
    private val card: PaymentMethod,
    context: Context
) : Preference(context, null, R.attr.preferenceStyle, 0) {
    private var typeface: Typeface? = null

    init {
        widgetLayoutResource = R.layout.preference_cards_layout

        loadFont(context, CustomFont.MONTSERRAT_REGULAR) {
            typeface = it
            this.title = title // Forces setting fonts when Title is set via XML
        }
        title = (card as? PaymentMethod.Card)?.uiLabel() ?: context.getString(R.string.add_card_title)
        icon = getContext().getDrawable((card as? PaymentMethod.Card)?.cardType?.icon()
            ?: R.drawable.ic_payment_card)
    }

    override fun setTitle(titleResId: Int) {
        title = context.getString(titleResId)
    }

    override fun setTitle(title: CharSequence?) {
        typeface?.let {
            super.setTitle(title?.applyFont(typeface))
        } ?: super.setTitle(title)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val titleView = holder.findViewById(android.R.id.title) as? TextView
        val endDigits = holder.itemView.end_digits
        val expDate = holder.itemView.exp_date
        val expired = holder.itemView.expired
        val addCard = holder.itemView.add_card
        (card as? PaymentMethod.Card)?.let {
            endDigits.text = card.dottedEndDigits()
            expDate.text =
                context.getString(R.string.card_expiry_date, card.expireDate.formatted())
            expired.visibleIf { it.status == CardStatus.EXPIRED }
            endDigits.visibleIf { it.status != CardStatus.EXPIRED }
            expDate.visibleIf { it.status != CardStatus.EXPIRED }
            addCard.gone()
        } ?: kotlin.run {
            endDigits.gone()
            expDate.gone()
            expired.gone()
            expDate.gone()
            addCard.visible()
        }
        titleView?.ellipsize = TextUtils.TruncateAt.END
        titleView?.setSingleLine(true)

        holder.isDividerAllowedAbove = true
    }
}

private fun Date.formatted() =
    SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(this)