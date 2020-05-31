package covid.trace.morocco.base

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import covid.trace.morocco.LocaleHelper
import io.github.inflationx.viewpump.ViewPumpContextWrapper



open class BaseFragmentActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.getInstance().setLocale(this)
        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            ViewPumpContextWrapper.wrap(
                LocaleHelper.getInstance().setLocale(newBase)
            )
        )
    }
}
