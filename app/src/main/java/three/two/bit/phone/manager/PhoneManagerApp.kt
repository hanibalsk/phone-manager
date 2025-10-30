package three.two.bit.phone.manager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Phone Manager.
 * Configured with Hilt for dependency injection.
 *
 * @HiltAndroidApp triggers Hilt's code generation and sets up the application-level
 * dependency container.
 */
@HiltAndroidApp
class PhoneManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Future: Initialize Timber, WorkManager here
    }
}
