package piuk.blockchain.android.ui.swapintro

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SwapIntroModel(val title: String, val description: String, val resourceIcon: Int) :
    Parcelable