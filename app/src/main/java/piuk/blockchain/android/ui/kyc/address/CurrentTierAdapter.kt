package piuk.blockchain.android.ui.kyc.address
import com.blockchain.swap.nabu.CurrentTier
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import io.reactivex.Single

internal class CurrentTierAdapter(
    private val nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager
) : CurrentTier {

    override fun usersCurrentTier(): Single<Int> =
        nabuToken.fetchNabuToken()
            .flatMap(nabuDataManager::getUser)
            .map { it.tiers?.current ?: 0 }
}
