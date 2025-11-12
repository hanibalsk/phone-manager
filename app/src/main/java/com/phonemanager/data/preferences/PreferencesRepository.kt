package com.phonemanager.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
 * Story 1.1: PreferencesRepository - Manages app preferences using DataStore
 */
interface PreferencesRepository {
    val isTrackingEnabled: Flow<Boolean>
    suspend fun setTrackingEnabled(enabled: Boolean)
    val trackingInterval: Flow<Int>
    suspend fun setTrackingInterval(minutes: Int)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object PreferencesKeys {
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        val TRACKING_INTERVAL = intPreferencesKey("tracking_interval_minutes")
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
            preferences[PreferencesKeys.TRACKING_INTERVAL] ?: 5 // default 5 minutes
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading tracking interval preference")
                emit(5)
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
}
