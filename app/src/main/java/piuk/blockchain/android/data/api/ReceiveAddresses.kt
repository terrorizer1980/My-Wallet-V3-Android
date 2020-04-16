package piuk.blockchain.android.data.api

import com.google.gson.Gson

data class ReceiveAddresses(private val coin: String, private val addresses: List<String>) {
    fun toJson() = Gson().toJson(this)
}