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

class EmailPromptFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private var binding: FragmentEmailPromptBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_email_prompt, container,
            false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.buttonEnable?.setOnClickListener {
            if (listener != null)
                listener?.onVerifyEmailClicked()
        }

        binding?.buttonLater?.setOnClickListener {
            if (listener != null)
                listener?.onVerifyLaterClicked()
        }

        if (arguments != null) {
            val email = arguments?.getString(ARGUMENT_EMAIL)
            binding?.textviewEmail?.text = email
            val showDismsissOption = arguments?.getBoolean(ARGUMENT_SHOW_DISMISS,
                true) ?: true
            binding?.buttonLater.goneIf(showDismsissOption.not())
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context!!.toString() +
                    " must implement OnFragmentInteractionListener")
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

        fun newInstance(email: String, showDismiss: Boolean): EmailPromptFragment {
            val fragment = EmailPromptFragment()
            val args = Bundle()
            args.putString(ARGUMENT_EMAIL, email)
            args.putBoolean(ARGUMENT_SHOW_DISMISS, showDismiss)
            fragment.arguments = args
            return fragment
        }
    }
}
