package piuk.blockchain.android.cards

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import kotlinx.android.synthetic.main.fragment_add_new_card.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher

class AddNewCardFragment : MviFragment<CardModel, CardIntent, CardState>(), AddCardFlowFragment {

    override val model: CardModel by inject()

    override val navigator: AddCardNavigator
        get() = (activity as? AddCardNavigator)
            ?: throw IllegalStateException("Parent must implement AddCardNavigator")

    override val cardDetailsPersistence: CardDetailsPersistence
        get() = (activity as? CardDetailsPersistence)
            ?: throw IllegalStateException("Parent must implement CardDetailsPersistence")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_add_new_card)

    private val textWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            btn_next.isEnabled = card_name.isValid && card_number.isValid && cvv.isValid && expiry_date.isValid
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        card_name.addTextChangedListener(textWatcher)
        card_number.addTextChangedListener(textWatcher)
        cvv.addTextChangedListener(textWatcher)
        expiry_date.addTextChangedListener(textWatcher)
        btn_next.apply {
            isEnabled = false
            setOnClickListener {
                cardDetailsPersistence.setCardData(CardData(
                    fullName = card_name.text.toString(),
                    number = card_number.text.toString().replace(" ", ""),
                    month = expiry_date.month.toInt(),
                    year = expiry_date.year.toInt(),
                    cvv = cvv.text.toString()
                ))
                navigator.navigateToBillingDetails()
                analytics.logEvent(SimpleBuyAnalytics.CARD_INFO_SET)
            }
        }
        card_number.displayCardTypeIcon(false)
        activity.setupToolbar(R.string.add_card_title)
        analytics.logEvent(SimpleBuyAnalytics.ADD_CARD)
    }

    override fun render(newState: CardState) {}

    override fun onBackPressed(): Boolean = true
}