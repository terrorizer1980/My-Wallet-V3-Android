package piuk.blockchain.androidbuysell.models.coinify

data class CountrySupport(val supported: Boolean, val states: Map<String, StateSupport> = mapOf())
data class StateSupport(val supported: Boolean)