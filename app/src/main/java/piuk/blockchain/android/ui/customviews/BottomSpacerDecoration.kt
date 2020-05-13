package piuk.blockchain.android.ui.customviews

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View

/**
 * For adding a spacer of a passed height (in pixels) to the bottom of a [RecyclerView].
 */
class BottomSpacerDecoration(private val height: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val childCount = parent.adapter?.itemCount
        // If not last position in list, don't offset
        if (childCount == null || parent.getChildLayoutPosition(view) != childCount - 1) {
            return
        }

        // Set bottom offset for last item in list
        outRect.set(0, 0, 0, height)
    }
}
