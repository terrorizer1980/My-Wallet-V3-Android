package com.blockchain.swap.nabu.nabu

import com.blockchain.serialization.JsonSerializable
import com.blockchain.swap.nabu.models.nabu.LimitsJson
import com.blockchain.swap.nabu.models.nabu.TierJson
import com.blockchain.swap.nabu.models.nabu.TiersJson
import com.blockchain.testutils.`should be assignable from`
import org.junit.Test

class NabuTiersJsonClassesTest {

    @Test
    fun `ensure TiersJson is JsonSerializable for proguard`() {
        JsonSerializable::class `should be assignable from` TiersJson::class
    }

    @Test
    fun `ensure TierJson is JsonSerializable for proguard`() {
        JsonSerializable::class `should be assignable from` TierJson::class
    }

    @Test
    fun `ensure LimitsJson is JsonSerializable for proguard`() {
        JsonSerializable::class `should be assignable from` LimitsJson::class
    }
}
