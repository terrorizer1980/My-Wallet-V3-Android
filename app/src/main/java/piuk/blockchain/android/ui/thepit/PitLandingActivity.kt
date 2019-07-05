package piuk.blockchain.android.ui.thepit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import com.blockchain.ui.urllinks.URL_THE_PIT_LANDING_LEARN_MORE
import kotlinx.android.synthetic.main.activity_pit_landing.*
import org.koin.android.ext.android.get
import piuk.blockchain.android.R
import piuk.blockchain.android.util.StringUtils

class PitLandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pit_landing)

        setupLinks()

        btn_signup.setOnClickListener { handleSignupClick() }
    }

    private fun setupLinks() {

        val linksMap = mapOf<String, Uri>(
            "pit_info" to Uri.parse(URL_THE_PIT_LANDING_LEARN_MORE)
        )

        val stringUtils: StringUtils = get()
        val linkText = stringUtils.getStringWithMappedLinks(R.string.pit_landing_learn_more, linksMap)

        link_learn_more.text = linkText
        link_learn_more.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun handleSignupClick() {
        PitPermissionsActivity.start(this)
        finish()
    }

    companion object {
        fun start(ctx: Context) {
            ctx.startActivity(Intent(ctx, PitLandingActivity::class.java))
        }
    }
}
