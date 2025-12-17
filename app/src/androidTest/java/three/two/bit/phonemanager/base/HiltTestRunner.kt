package three.two.bit.phonemanager.base

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner that uses HiltTestApplication for dependency injection.
 *
 * This runner must be specified in build.gradle.kts:
 * testInstrumentationRunner = "three.two.bit.phonemanager.base.HiltTestRunner"
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
