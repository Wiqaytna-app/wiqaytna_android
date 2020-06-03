package covid.trace.morocco.base

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import covid.trace.morocco.LocaleHelper
import io.github.inflationx.viewpump.ViewPumpContextWrapper



open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.setLocale(this)
        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            ViewPumpContextWrapper.wrap(
                LocaleHelper.setLocale(newBase)!!
            )
        )
    }
}
