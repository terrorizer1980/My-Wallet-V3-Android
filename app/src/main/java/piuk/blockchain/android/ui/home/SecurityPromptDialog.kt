package piuk.blockchain.android.ui.home

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_security_centre.*

import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class SecurityPromptDialog : AppCompatDialogFragment() {

    var positiveButtonListener: (() -> Unit)? = null
    var negativeButtonListener: (() -> Unit)? = null

    val isChecked: Boolean
        get() = checkbox.isChecked

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_security_centre, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_positive.setOnClickListener { v -> positiveButtonListener?.invoke() }
        button_negative.setOnClickListener { v -> negativeButtonListener?.invoke() }

        arguments?.let {
            if (it.containsKey(KEY_TITLE)) {
                title.setText(it.getInt(KEY_TITLE))
            }

            if (it.containsKey(KEY_MESSAGE)) {
                message.text = it.getString(KEY_MESSAGE)
            }

            if (it.containsKey(KEY_ICON)) {
                icon.setImageResource(it.getInt(KEY_ICON))
            }

            if (it.containsKey(KEY_POSITIVE_BUTTON)) {
                button_positive.setText(it.getInt(KEY_POSITIVE_BUTTON))
            }

            button_negative.visibleIf(it.getBoolean(KEY_SHOW_NEGATIVE_BUTTON))
            checkbox.visibleIf(it.getBoolean(KEY_SHOW_CHECKBOX))
        }
    }

    companion object {

        val TAG = SecurityPromptDialog::class.java.simpleName!!

        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_ICON = "icon"
        private const val KEY_POSITIVE_BUTTON = "positive_button"
        private const val KEY_SHOW_CHECKBOX = "show_checkbox"
        private const val KEY_SHOW_NEGATIVE_BUTTON = "show_negative_button"

        fun newInstance(
            @StringRes title: Int,
            message: String,
            @DrawableRes icon: Int,
            @StringRes positiveButton: Int,
            showNegativeButton: Boolean = false,
            showCheckbox: Boolean = false
        ): SecurityPromptDialog {
            val fragment = SecurityPromptDialog()

            fragment.arguments = Bundle().apply {
                putInt(KEY_TITLE, title)
                putString(KEY_MESSAGE, message)
                putInt(KEY_ICON, icon)
                putInt(KEY_POSITIVE_BUTTON, positiveButton)
                putBoolean(KEY_SHOW_NEGATIVE_BUTTON, showNegativeButton)
                putBoolean(KEY_SHOW_CHECKBOX, showCheckbox)
            }
            return fragment
        }
    }
}
