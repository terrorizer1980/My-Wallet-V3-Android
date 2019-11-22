package info.blockchain.wallet.prices.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)

class PriceDatum {
    @JsonProperty("timestamp")
    val timestamp: Long
    @JsonProperty("price")
    val price: Double
    @JsonProperty("volume24h")
    val volume24h: Double

    // Jackson does not play nicely with Kotlin data classes and requires a default c_tor
    // But we want to be able to create this, so we'll have a second c_tor for that.
    constructor() {
        timestamp = 0
        price = 0.0
        volume24h = 0.0
    }

    constructor(
        timestamp: Long,
        price: Double,
        volume24h: Double = 0.0
    ) {
        this.timestamp = timestamp
        this.price = price
        this.volume24h = volume24h
    }
}
