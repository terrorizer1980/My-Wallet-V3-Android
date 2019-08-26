package piuk.blockchain.android.ui.onboarding

import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentEmailPromptBinding
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import java.lang.NullPointerException

class EmailPromptFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private var binding: FragmentEmailPromptBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_email_prompt, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.buttonEnable?.setOnClickListener { listener?.onVerifyEmailClicked() }
        binding?.buttonLater?.setOnClickListener { listener?.onVerifyLaterClicked() }
        binding?.textviewEmail?.text = arguments?.emailAddress
        binding?.buttonLater.goneIf(!arguments.showDismiss)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    internal interface OnFragmentInteractionListener {
        fun onVerifyEmailClicked()
        fun onVerifyLaterClicked()
    }

    companion object {

        private const val ARGUMENT_EMAIL = "email"
        private const val ARGUMENT_SHOW_DISMISS = "show_dismiss"

        fun newInstance(email: String, showDismissBtn: Boolean): EmailPromptFragment {
            val fragment = EmailPromptFragment()

            fragment.arguments = Bundle().apply {
                emailAddress = email
                showDismiss = showDismissBtn
            }
            return fragment
        }

        private var Bundle?.emailAddress: String
            get() = this?.getString(ARGUMENT_EMAIL) ?: ""
            set(v) = this?.putString(ARGUMENT_EMAIL, v) ?: throw NullPointerException()

        private var Bundle?.showDismiss: Boolean
            get() = this?.getBoolean(ARGUMENT_SHOW_DISMISS, true) ?: true
            set(v) = this?.putBoolean(ARGUMENT_SHOW_DISMISS, v) ?: throw NullPointerException()
    }
}
