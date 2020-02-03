package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.google.gson.Gson

class SimpleBuyUtils(private val gson: Gson) {

    fun inflateSimpleBuyState(prefs: SimpleBuyPrefs): SimpleBuyState? =
        prefs.simpleBuyState()?.let {
            gson.fromJson(it, SimpleBuyState::class.java)
        }
}
