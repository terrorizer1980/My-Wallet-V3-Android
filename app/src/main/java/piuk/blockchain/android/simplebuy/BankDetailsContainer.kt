package piuk.blockchain.android.simplebuy

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.blockchain.swap.nabu.models.simplebuy.BankDetail
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.R
import piuk.blockchain.android.util.CopyableTextFormItem

class BankDetailsContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    fun initWithBankDetailsAndAmount(bankDetails: List<BankDetail>, amount: FiatValue) {
        if (childCount == 0) {
            bankDetails.forEach {
                addView(CopyableTextFormItem(it.title,
                    it.value,
                    it.isCopyable,
                    context),
                    LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT)
                )
            }
            addView(CopyableTextFormItem(context.getString(R.string.simple_buy_amount_to_send),
                amount.toStringWithSymbol(),
                false,
                context)
            )
        }
    }
}