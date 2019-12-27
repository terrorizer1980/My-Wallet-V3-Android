package com.blockchain.accounts

import com.blockchain.sunriver.XlmDataManager
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.AccountReference
import io.reactivex.Maybe
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test

class XlmAsyncAccountListAdapterTest {

    @Test
    fun `XlmAsyncAccountListAdapter account list`() {
        val accountReference = AccountReference.Xlm("Xlm Account", "GABC")

        val xlmDataMan: XlmDataManager = mock {
            on { maybeDefaultAccount() } `it returns` Maybe.just(accountReference)
        }

        XlmAsyncAccountListAdapter(xlmDataMan)
            .accounts()
            .test()
            .values()
            .single() `should equal` listOf(accountReference)
    }

    @Test
    fun `XlmAsyncAccountListAdapter account list - empty`() {
        val xlmDataMan: XlmDataManager = mock {
            on { maybeDefaultAccount() } `it returns` Maybe.empty()
        }

        XlmAsyncAccountListAdapter(xlmDataMan)
            .accounts()
                .test()
                .values()
                .single() `should equal` emptyList()
    }
}
