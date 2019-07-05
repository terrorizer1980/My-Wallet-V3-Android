package piuk.blockchain.androidbuysell.models.coinify

data class CountrySupport(val supported: Boolean, val states: Map<String, StateSupport>?) {

    fun stateSupported(state: String?): Boolean {
        val countryStates = states ?: return false
        state?.let {
            return countryStates[it]?.supported ?: false
        } ?: return false
    }
}

data class StateSupport(val supported: Boolean)
