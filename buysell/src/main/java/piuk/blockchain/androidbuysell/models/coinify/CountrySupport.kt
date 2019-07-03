package piuk.blockchain.androidbuysell.models.coinify

data class CountrySupport(val supported: Boolean, val states: Map<String, StateSupport> = mapOf()) {

    fun stateSupported(state: String?): Boolean =
        state?.let {
            return states[it]?.supported ?: false
        } ?: false
}

data class StateSupport(val supported: Boolean)
