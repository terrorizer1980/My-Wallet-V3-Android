package info.blockchain.wallet.contacts.data

import com.blockchain.serialization.JsonSerializableAccount
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.mikael.urlbuilder.UrlBuilder
import io.mikael.urlbuilder.util.UrlParameterMultimap
import java.io.IOException
import java.util.HashMap
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = Visibility.NONE,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE
)
class Contact : JsonSerializableAccount {

    @JsonProperty("id")
    var id: String = UUID.randomUUID().toString()

    @JsonProperty("name")
    var name: String? = null

    @JsonProperty("surname")
    var surname: String? = null

    @JsonProperty("company")
    var company: String? = null

    @JsonProperty("email")
    var email: String? = null

    @JsonProperty("xpub")
    var xpub: String? = null

    @JsonProperty("note")
    var note: String? = null

    @JsonProperty("mdid")
    var mdid: String? = null

    @JsonProperty("created")
    var created: Long = System.currentTimeMillis() / 1000

    @JsonProperty("invitationSent")
    var invitationSent: String? = null // I invited somebody

    @JsonProperty("invitationReceived")
    var invitationReceived: String? = null // Somebody invited me

    @JsonProperty("facilitatedTxList")
    var facilitatedTransactions: HashMap<String, FacilitatedTransaction> = HashMap()

    fun addFacilitatedTransaction(facilitatedTransaction: FacilitatedTransaction) {
        this.facilitatedTransactions[facilitatedTransaction.id] = facilitatedTransaction
    }

    fun deleteFacilitatedTransaction(fctxId: String) {
        facilitatedTransactions.remove(fctxId)
    }

    @Throws(IOException::class)
    fun fromJson(json: String): Contact {
        return ObjectMapper().readValue(json, Contact::class.java)
    }

    @Throws(JsonProcessingException::class)
    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

    private fun toQueryParameters(): UrlParameterMultimap {
        val queryParams = UrlParameterMultimap.newMultimap()
        if (id != null) queryParams.add("id", invitationSent)
        if (name != null) queryParams.add("name", name)
        if (surname != null) queryParams.add("surname", surname)

        return queryParams
    }

    fun fromQueryParameters(queryParams: Map<String, String>): Contact {
        return Contact().apply {
            invitationReceived = queryParams["id"]
            name = queryParams["name"]
            surname = queryParams["surname"]
        }
    }

    fun createURI(): String {
        val urlParameterMultimap = toQueryParameters()

        val urlBuilder = UrlBuilder.empty()
            .withScheme("https")
            .withHost("blockchain.info")
            .withPath("/invite")
            .withParameters(urlParameterMultimap)

        return urlBuilder.toUri().toString()
    }
}
