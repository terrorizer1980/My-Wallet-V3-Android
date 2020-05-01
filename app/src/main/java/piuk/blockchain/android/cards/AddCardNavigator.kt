package piuk.blockchain.android.cards

import com.blockchain.swap.nabu.datamanagers.Partner

interface AddCardNavigator {
    fun navigateToBillingDetails()
    fun navigateToCardVerification()
    fun exitWithSuccess(cardId: String, cardLabel: String, partner: Partner)
    fun exitWithError()
}