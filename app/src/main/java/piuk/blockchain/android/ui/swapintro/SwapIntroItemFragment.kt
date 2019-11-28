package piuk.blockchain.android.ui.swapintro

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_swap_intro_item_layout.*
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class SwapIntroItemFragment : Fragment() {

    private val item: SwapIntroModel by unsafeLazy {
        arguments?.getParcelable(ARG_ITEM) as SwapIntroModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_swap_intro_item_layout)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        icon.setImageResource(item.resourceIcon)
        title.text = item.title
        description.text = item.description
    }

    companion object {
        private const val ARG_ITEM = "arg_item"

        fun newInstance(item: SwapIntroModel): SwapIntroItemFragment {
            return SwapIntroItemFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ITEM, item)
                }
            }
        }
    }
}