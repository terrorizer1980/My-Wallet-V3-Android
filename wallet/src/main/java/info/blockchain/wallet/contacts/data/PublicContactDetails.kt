package info.blockchain.wallet.contacts.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PublicContactDetails(var xpub: String? = null) {

    @JsonIgnore
    @Throws(IOException::class)
    fun fromJson(json: String) = ObjectMapper().readValue(json, PublicContactDetails::class.java)

    @JsonIgnore
    @Throws(JsonProcessingException::class)
    fun toJson(): String = ObjectMapper().writeValueAsString(this)
}
