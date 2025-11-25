package com.phonemanager.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 1.1/1.4: PreferencesRepository - Manages app preferences using DataStore
 *
 * Story 1.4 additions: Service state persistence for boot restoration
 */
interface PreferencesRepository {
    val isTrackingEnabled: Flow<Boolean>
    suspend fun setTrackingEnabled(enabled: Boolean)
    val trackingInterval: Flow<Int>
    suspend fun setTrackingInterval(minutes: Int)

    // Story 1.4: Service state persistence
    val serviceRunningState: Flow<Boolean>
    suspend fun setServiceRunningState(isRunning: Boolean)
    val lastLocationUpdateTime: Flow<Long?>
    suspend fun setLastLocationUpdateTime(timestamp: Long)
    suspend fun clearLastLocationUpdateTime()
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) :
    PreferencesRepository {

    companion object {
        /** Default tracking interval in minutes */
        const val DEFAULT_TRACKING_INTERVAL_MINUTES = 5
    }

    private object PreferencesKeys {
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        val TRACKING_INTERVAL = intPreferencesKey("tracking_interval_minutes")

        // Story 1.4: Service state persistence keys
        val SERVICE_RUNNING = booleanPreferencesKey("service_running")
        val LAST_LOCATION_UPDATE = longPreferencesKey("last_location_update")
    }

    override val isTrackingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRACKING_ENABLED] ?: false
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading tracking enabled preference")
                emit(false)
            } else {
                throw exception
            }
        }

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACKING_ENABLED] = enabled
        }
        Timber.d("Tracking enabled set to: $enabled")
    }

    override val trackingInterval: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRACKING_INTERVAL] ?: DEFAULT_TRACKING_INTERVAL_MINUTES
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading tracking interval preference")
                emit(DEFAULT_TRACKING_INTERVAL_MINUTES)
            } else {
                throw exception
            }
        }

    override suspend fun setTrackingInterval(minutes: Int) {
        require(minutes in 1..60) { "Interval must be between 1 and 60 minutes" }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACKING_INTERVAL] = minutes
        }
        Timber.d("Tracking interval set to: $minutes minutes")
    }

    // Story 1.4: Service state persistence implementation

    override val serviceRunningState: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SERVICE_RUNNING] ?: false
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading service running state preference")
                emit(false)
            } else {
                throw exception
            }
        }

    override suspend fun setServiceRunningState(isRunning: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVICE_RUNNING] = isRunning
        }
        Timber.d("Service running state persisted: $isRunning")
    }

    override val lastLocationUpdateTime: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_LOCATION_UPDATE]
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading last location update time preference")
                emit(null)
            } else {
                throw exception
            }
        }

    override suspend fun setLastLocationUpdateTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_LOCATION_UPDATE] = timestamp
        }
        Timber.d("Last location update time persisted: $timestamp")
    }

    override suspend fun clearLastLocationUpdateTime() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.LAST_LOCATION_UPDATE)
        }
        Timber.d("Last location update time cleared")
    }
}
