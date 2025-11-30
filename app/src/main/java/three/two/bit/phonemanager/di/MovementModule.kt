package three.two.bit.phonemanager.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.movement.ActivityRecognitionManager
import three.two.bit.phonemanager.movement.AndroidAutoDetector
import three.two.bit.phonemanager.movement.BluetoothCarDetector
import three.two.bit.phonemanager.movement.TransportationModeManager
import javax.inject.Singleton

/**
 * Hilt module for movement detection dependencies.
 *
 * Provides detection components for:
 * - Activity Recognition (walking, running, cycling, vehicle)
 * - Bluetooth car detection
 * - Android Auto detection
 * - Aggregated transportation mode management
 */
@Module
@InstallIn(SingletonComponent::class)
object MovementModule {

    @Provides
    @Singleton
    fun provideActivityRecognitionManager(
        @ApplicationContext context: Context,
    ): ActivityRecognitionManager = ActivityRecognitionManager(context)

    @Provides
    @Singleton
    fun provideBluetoothCarDetector(
        @ApplicationContext context: Context,
    ): BluetoothCarDetector = BluetoothCarDetector(context)

    @Provides
    @Singleton
    fun provideAndroidAutoDetector(
        @ApplicationContext context: Context,
    ): AndroidAutoDetector = AndroidAutoDetector(context)

    @Provides
    @Singleton
    fun provideTransportationModeManager(
        @ApplicationContext context: Context,
        activityRecognitionManager: ActivityRecognitionManager,
        bluetoothCarDetector: BluetoothCarDetector,
        androidAutoDetector: AndroidAutoDetector,
        preferencesRepository: PreferencesRepository,
    ): TransportationModeManager = TransportationModeManager(
        context = context,
        activityRecognitionManager = activityRecognitionManager,
        bluetoothCarDetector = bluetoothCarDetector,
        androidAutoDetector = androidAutoDetector,
        preferencesRepository = preferencesRepository,
    )
}
