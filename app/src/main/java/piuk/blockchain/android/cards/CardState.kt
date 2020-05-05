package piuk.blockchain.android.cards

import com.blockchain.swap.nabu.datamanagers.BillingAddress
import com.blockchain.swap.nabu.datamanagers.Partner
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.CardStatus
import piuk.blockchain.android.ui.base.mvi.MviState

data class CardState(
    val fiatCurrency: String,
    val cardId: String? = null,
    val cardStatus: CardStatus? = null,
    val billingAddress: BillingAddress? = null,
    val addCard: Boolean = false,
    @Transient val authoriseEverypayCard: EverypayAuthOptions? = null,
    @Transient val cardRequestStatus: CardRequestStatus? = null
) : MviState

data class EverypayAuthOptions(val paymentLink: String, val exitLink: String)

sealed class CardRequestStatus {
    object Error : CardRequestStatus()
    object Loading : CardRequestStatus()
    class Success(val cardId: String, val cardLabel: String, val partner: Partner) : CardRequestStatus()
}