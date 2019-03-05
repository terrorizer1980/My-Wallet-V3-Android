package piuk.blockchain.android.kyc

import android.content.Intent
import android.net.Uri
import com.blockchain.notifications.links.PendingLink
import io.reactivex.Maybe
import io.reactivex.Single

fun Uri.ignoreFragment(): Uri {
    val cleanedUri = this.toString().replace("/#", "")
    return Uri.parse(cleanedUri)
}

class KycDeepLinkHelper(
    private val linkHandler: PendingLink
) {

    fun getLink(intent: Intent): Single<KycLinkState> =
        linkHandler.getPendingLinks(intent)
            .map(this::mapUri)
            .switchIfEmpty(Maybe.just(KycLinkState.NoUri))
            .toSingle()
            .onErrorResumeNext { Single.just(KycLinkState.NoUri) }

    fun mapUri(uri: Uri): KycLinkState {
        val name = uri.ignoreFragment().getQueryParameter("deep_link_path")
        return when (name) {
            "verification" -> KycLinkState.Resubmit
            "email_verified" -> KycLinkState.EmailVerified
            "kyc" -> KycLinkState.General
            else -> KycLinkState.NoUri
        }
    }
}

enum class KycLinkState {
    /**
     * Deep link into the email confirmation part of KYC
     */
    EmailVerified,

    /**
     * General deep link into KYC
     */
    General,

    /**
     * Not a valid KYC deep link URI
     */
    NoUri,

    /**
     * Deep link into identity verification part of KYC
     */
    Resubmit
}
