package three.two.bit.phonemanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import three.two.bit.phonemanager.data.repository.AlertRepository
import three.two.bit.phonemanager.data.repository.AlertRepositoryImpl
import three.two.bit.phonemanager.data.repository.DeviceRepository
import three.two.bit.phonemanager.data.repository.DeviceRepositoryImpl
import three.two.bit.phonemanager.data.repository.GeofenceRepository
import three.two.bit.phonemanager.data.repository.GeofenceRepositoryImpl
import three.two.bit.phonemanager.data.repository.LocationRepository
import three.two.bit.phonemanager.data.repository.LocationRepositoryImpl
import three.two.bit.phonemanager.data.repository.MovementEventRepository
import three.two.bit.phonemanager.data.repository.MovementEventRepositoryImpl
import three.two.bit.phonemanager.data.repository.TripRepository
import three.two.bit.phonemanager.data.repository.TripRepositoryImpl
import three.two.bit.phonemanager.data.repository.AdminSettingsRepository
import three.two.bit.phonemanager.data.repository.AdminSettingsRepositoryImpl
import three.two.bit.phonemanager.data.repository.SettingsSyncRepository
import three.two.bit.phonemanager.data.repository.SettingsSyncRepositoryImpl
import three.two.bit.phonemanager.data.repository.UnlockRequestRepository
import three.two.bit.phonemanager.data.repository.UnlockRequestRepositoryImpl
import three.two.bit.phonemanager.data.repository.WebhookRepository
import three.two.bit.phonemanager.data.repository.WebhookRepositoryImpl
import javax.inject.Singleton

/**
 * Epic 0.2/1: RepositoryModule - Binds repository interfaces to implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    /**
     * Story E5.1: Alert Repository binding
     */
    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    /**
     * Story E6.1: Geofence Repository binding
     */
    @Binds
    @Singleton
    abstract fun bindGeofenceRepository(impl: GeofenceRepositoryImpl): GeofenceRepository

    /**
     * Story E6.3: Webhook Repository binding
     */
    @Binds
    @Singleton
    abstract fun bindWebhookRepository(impl: WebhookRepositoryImpl): WebhookRepository

    /**
     * Story E8.3: Trip Repository binding
     */
    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository

    /**
     * Story E8.3: Movement Event Repository binding
     */
    @Binds
    @Singleton
    abstract fun bindMovementEventRepository(impl: MovementEventRepositoryImpl): MovementEventRepository

    /**
     * Story E12.6: Settings Sync Repository binding
     */
    @Binds
    @Singleton
    abstract fun bindSettingsSyncRepository(impl: SettingsSyncRepositoryImpl): SettingsSyncRepository

    /**
     * Story E12.7: Admin Settings Repository binding
     */
    @Binds
    @Singleton
    abstract fun bindAdminSettingsRepository(impl: AdminSettingsRepositoryImpl): AdminSettingsRepository

    /**
     * Story E12.8: Unlock Request Repository binding
     */
    @Binds
    @Singleton
    abstract fun bindUnlockRequestRepository(impl: UnlockRequestRepositoryImpl): UnlockRequestRepository
}
