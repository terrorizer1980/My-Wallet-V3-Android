package com.blockchain.kyc.services.nabu

import io.reactivex.Completable

/**
 * Interface definition for creating a Coinify account for a Nabu User.
 */
interface NabuCoinifyAccountCreator {

    /**
     * Creates a Coinify account for the nabu user, if no such Coinify user is associated to the Nabu user.
     */
    fun createCoinifyAccountIfNeeded(): Completable
}