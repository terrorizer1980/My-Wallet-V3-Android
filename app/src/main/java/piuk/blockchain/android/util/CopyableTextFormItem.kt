package piuk.blockchain.android.util

import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.copyable_text_form_item.view.*
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class CopyableTextFormItem @JvmOverloads constructor(
    private val title: String,
    private val value: String,
    private val isCopyable: Boolean,
    private val onCopy: (String) -> Unit = {},
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        initView()
    }

    private fun initView() {
        val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = mInflater.inflate(R.layout.copyable_text_form_item, this, true)

        view.title.text = title
        view.value.text = value
        view.ic_copy.visibleIf { isCopyable }
        view.ic_copy.setOnClickListener {
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("Copied Text", value)
            clipboard.primaryClip = clip
            val snackbar = Snackbar.make(
                parent as View,
                resources.getString(R.string.simple_buy_copied_to_clipboard, title),
                Snackbar.LENGTH_SHORT
            )

            snackbar.view.setBackgroundColor(ContextCompat.getColor(context, R.color.simple_buy_snackbar))
            snackbar.show()
            onCopy(title)
        }
    }
}