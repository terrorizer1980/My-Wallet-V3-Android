package piuk.blockchain.android.ui.swapintro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class SwapIntroPagerAdapter(fm: FragmentManager, private val items: List<SwapIntroModel>) :
    FragmentStatePagerAdapter(fm) {
    override fun getItem(position: Int): Fragment =
        SwapIntroItemFragment.newInstance(items[position])

    override fun getCount(): Int =
        items.size
}