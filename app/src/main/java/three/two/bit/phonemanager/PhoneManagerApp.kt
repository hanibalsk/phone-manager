package three.two.bit.phonemanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import three.two.bit.phonemanager.security.SecureStorage
import three.two.bit.phonemanager.worker.SettingsSyncWorker
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PhoneManagerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var secureStorage: SecureStorage

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("PhoneManagerApp initialized")

        // Schedule periodic settings sync if user is logged in
        scheduleWorkersIfLoggedIn()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Schedule background workers if user is authenticated.
     * This ensures periodic sync continues after app restarts.
     */
    private fun scheduleWorkersIfLoggedIn() {
        if (secureStorage.getAccessToken() != null) {
            Timber.i("User is logged in, scheduling periodic workers")
            SettingsSyncWorker.schedule(this)
        } else {
            Timber.d("User not logged in, skipping worker scheduling")
        }
    }
}
