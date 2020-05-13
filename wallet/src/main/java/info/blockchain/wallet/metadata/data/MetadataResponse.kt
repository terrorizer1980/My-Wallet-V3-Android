package info.blockchain.wallet.metadata.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
data class MetadataResponse(
    var version: Int = 0,
    var payload: String = "",
    var signature: String = "",
    @get:JsonProperty("prev_magic_hash")
    @set:JsonProperty("prev_magic_hash")
    var prevMagicHash: String? = null,
    @get:JsonProperty("type_id")
    @set:JsonProperty("type_id")
    var typeId: Int = 0,
    @get:JsonProperty("created_at")
    @set:JsonProperty("created_at")
    var createdAt: Long = 0,
    @get:JsonProperty("updated_at")
    @set:JsonProperty("updated_at")
    var updatedAt: Long = 0,
    var address: String = ""
) {
    @JsonIgnore
    @Throws(JsonProcessingException::class)
    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }
}