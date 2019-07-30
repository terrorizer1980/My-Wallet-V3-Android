package com.blockchain.android.testutils

import org.amshove.kluent.`should equal`
import org.junit.Test

class DaggerLazyImplTest {

    @Test
    fun `get returns object`() {
        val any = Any()
        DaggerLazyImpl(any).get() `should equal` any
    }
}