package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import three.two.bit.phonemanager.data.database.GeofenceDao
import three.two.bit.phonemanager.data.model.GeofenceEntity
import three.two.bit.phonemanager.data.model.toDomain
import three.two.bit.phonemanager.data.model.toEntity
import three.two.bit.phonemanager.domain.model.Geofence
import three.two.bit.phonemanager.domain.model.TransitionType
import three.two.bit.phonemanager.geofence.GeofenceManager
import three.two.bit.phonemanager.network.GeofenceApiService
import three.two.bit.phonemanager.network.NetworkException
import three.two.bit.phonemanager.network.NetworkManager
import three.two.bit.phonemanager.network.models.CreateGeofenceRequest
import three.two.bit.phonemanager.network.models.GeofenceDto
import three.two.bit.phonemanager.network.models.GeofenceEventType
import three.two.bit.phonemanager.network.models.UpdateGeofenceRequest
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E6.1: GeofenceRepository - Geofence management with local + remote sync
 *
 * AC E6.1.4: Server sync via POST /api/v1/geofences
 * AC E6.1.5: Geofence management (CRUD operations)
 */
interface GeofenceRepository {
    /**
     * Observe all geofences for current device
     */
    fun observeGeofences(): Flow<List<Geofence>>

    /**
     * Observe only active geofences
     */
    fun observeActiveGeofences(): Flow<List<Geofence>>

    /**
     * Get a single geofence by ID
     */
    suspend fun getGeofence(geofenceId: String): Geofence?

    /**
     * Create a new geofence
     * Creates locally and syncs to server if network available
     */
    suspend fun createGeofence(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        transitionTypes: Set<TransitionType>,
    ): Result<Geofence>

    /**
     * Update an existing geofence
     */
    suspend fun updateGeofence(geofence: Geofence): Result<Geofence>

    /**
     * Toggle geofence active state
     */
    suspend fun toggleGeofenceActive(geofenceId: String, active: Boolean): Result<Unit>

    /**
     * Delete a geofence
     */
    suspend fun deleteGeofence(geofenceId: String): Result<Unit>

    /**
     * Sync geofences from server
     */
    suspend fun syncFromServer(): Result<Int>
}

@Singleton
class GeofenceRepositoryImpl @Inject constructor(
    private val geofenceDao: GeofenceDao,
    private val geofenceApiService: GeofenceApiService,
    private val networkManager: NetworkManager,
    private val secureStorage: SecureStorage,
    private val geofenceManager: GeofenceManager,
) : GeofenceRepository {

    private val deviceId: String
        get() = secureStorage.getDeviceId()

    override fun observeGeofences(): Flow<List<Geofence>> =
        geofenceDao.observeGeofencesByDevice(deviceId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeActiveGeofences(): Flow<List<Geofence>> =
        geofenceDao.observeActiveGeofences(deviceId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getGeofence(geofenceId: String): Geofence? = geofenceDao.getById(geofenceId)?.toDomain()

    override suspend fun createGeofence(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        transitionTypes: Set<TransitionType>,
    ): Result<Geofence> {
        val now = Clock.System.now()
        val geofenceId = UUID.randomUUID().toString()

        // Create domain model
        val geofence = Geofence(
            id = geofenceId,
            deviceId = deviceId,
            name = name,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            transitionTypes = transitionTypes,
            webhookId = null,
            active = true,
            createdAt = now,
            updatedAt = now,
        )

        // Save locally first
        geofenceDao.insert(geofence.toEntity())
        Timber.d("Geofence created locally: $geofenceId")

        // Register with Android Geofencing API if permissions available
        if (geofenceManager.hasRequiredPermissions()) {
            geofenceManager.addGeofence(geofence).fold(
                onSuccess = {
                    Timber.i("Geofence registered with Android API: $geofenceId")
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to register geofence with Android API")
                },
            )
        } else {
            Timber.d("Background location permission not granted, skipping Android Geofencing registration")
        }

        // Sync to server if network available
        if (networkManager.isNetworkAvailable()) {
            val request = CreateGeofenceRequest(
                deviceId = deviceId,
                name = name,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters.toDouble(),
                eventTypes = transitionTypes.map { it.toEventType() },
                active = true,
            )

            geofenceApiService.createGeofence(request).fold(
                onSuccess = { dto ->
                    // Update local with server ID if different
                    if (dto.geofenceId != geofenceId) {
                        geofenceDao.delete(geofence.toEntity())
                        val serverGeofence = dto.toDomain()
                        geofenceDao.insert(serverGeofence.toEntity())
                        Timber.i("Geofence synced to server: ${dto.geofenceId}")
                        return Result.success(serverGeofence)
                    }
                    Timber.i("Geofence synced to server: ${dto.geofenceId}")
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to sync geofence to server, will retry later")
                },
            )
        } else {
            Timber.d("Network unavailable, geofence saved locally for later sync")
        }

        return Result.success(geofence)
    }

    override suspend fun updateGeofence(geofence: Geofence): Result<Geofence> {
        val updatedGeofence = geofence.copy(updatedAt = Clock.System.now())

        // Update locally
        geofenceDao.update(updatedGeofence.toEntity())
        Timber.d("Geofence updated locally: ${geofence.id}")

        // Sync to server if network available
        if (networkManager.isNetworkAvailable()) {
            val request = UpdateGeofenceRequest(
                name = geofence.name,
                latitude = geofence.latitude,
                longitude = geofence.longitude,
                radiusMeters = geofence.radiusMeters.toDouble(),
                eventTypes = geofence.transitionTypes.map { it.toEventType() },
                active = geofence.active,
            )

            geofenceApiService.updateGeofence(geofence.id, request).fold(
                onSuccess = {
                    Timber.i("Geofence update synced to server: ${geofence.id}")
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to sync geofence update to server")
                },
            )
        }

        return Result.success(updatedGeofence)
    }

    override suspend fun toggleGeofenceActive(geofenceId: String, active: Boolean): Result<Unit> {
        val existing = geofenceDao.getById(geofenceId)
            ?: return Result.failure(IllegalArgumentException("Geofence not found: $geofenceId"))

        val updatedEntity = existing.copy(
            active = active,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        geofenceDao.update(updatedEntity)
        Timber.d("Geofence ${if (active) "activated" else "deactivated"}: $geofenceId")

        // Update Android Geofencing API registration
        if (geofenceManager.hasRequiredPermissions()) {
            if (active) {
                // Register with Android API when activating
                geofenceManager.addGeofence(updatedEntity.toDomain()).fold(
                    onSuccess = {
                        Timber.i("Geofence registered with Android API: $geofenceId")
                    },
                    onFailure = { error ->
                        Timber.w(error, "Failed to register geofence with Android API")
                    },
                )
            } else {
                // Remove from Android API when deactivating
                geofenceManager.removeGeofence(geofenceId).fold(
                    onSuccess = {
                        Timber.i("Geofence removed from Android API: $geofenceId")
                    },
                    onFailure = { error ->
                        Timber.w(error, "Failed to remove geofence from Android API")
                    },
                )
            }
        }

        // Sync to server
        if (networkManager.isNetworkAvailable()) {
            geofenceApiService.updateGeofence(geofenceId, UpdateGeofenceRequest(active = active))
                .onFailure { error ->
                    Timber.w(error, "Failed to sync geofence toggle to server")
                }
        }

        return Result.success(Unit)
    }

    override suspend fun deleteGeofence(geofenceId: String): Result<Unit> {
        val existing = geofenceDao.getById(geofenceId)
            ?: return Result.failure(IllegalArgumentException("Geofence not found: $geofenceId"))

        // Delete locally
        geofenceDao.delete(existing)
        Timber.d("Geofence deleted locally: $geofenceId")

        // Remove from Android Geofencing API
        if (geofenceManager.hasRequiredPermissions()) {
            geofenceManager.removeGeofence(geofenceId).fold(
                onSuccess = {
                    Timber.i("Geofence removed from Android API: $geofenceId")
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to remove geofence from Android API")
                },
            )
        }

        // Delete from server
        if (networkManager.isNetworkAvailable()) {
            geofenceApiService.deleteGeofence(geofenceId).fold(
                onSuccess = {
                    Timber.i("Geofence deleted from server: $geofenceId")
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to delete geofence from server")
                },
            )
        }

        return Result.success(Unit)
    }

    /**
     * Sync geofences from server
     *
     * Strategy: Replace local geofences with server data (server is source of truth)
     * Also re-registers active geofences with Android Geofencing API
     */
    override suspend fun syncFromServer(): Result<Int> {
        if (!networkManager.isNetworkAvailable()) {
            return Result.failure(NetworkException("No network connection available"))
        }

        return geofenceApiService.listGeofences(deviceId, includeInactive = true).map { response ->
            Timber.d("Fetched ${response.total} geofences from server")

            // Remove all existing geofences from Android API
            if (geofenceManager.hasRequiredPermissions()) {
                geofenceManager.removeAllGeofences().fold(
                    onSuccess = {
                        Timber.d("Removed all geofences from Android API before sync")
                    },
                    onFailure = { error ->
                        Timber.w(error, "Failed to remove geofences from Android API before sync")
                    },
                )
            }

            // Clear local and replace with server data
            geofenceDao.deleteAllByDevice(deviceId)

            val activeGeofences = mutableListOf<Geofence>()
            response.geofences.forEach { dto ->
                val entity = dto.toEntity()
                geofenceDao.insert(entity)

                // Collect active geofences for Android API registration
                if (dto.active) {
                    activeGeofences.add(dto.toDomain())
                }
            }

            // Register active geofences with Android API
            if (geofenceManager.hasRequiredPermissions() && activeGeofences.isNotEmpty()) {
                geofenceManager.addGeofences(activeGeofences).fold(
                    onSuccess = {
                        Timber.i("Registered ${activeGeofences.size} active geofences with Android API")
                    },
                    onFailure = { error ->
                        Timber.w(error, "Failed to register geofences with Android API after sync")
                    },
                )
            }

            Timber.i("Synced ${response.total} geofences from server")
            response.total
        }
    }
}

/**
 * Convert TransitionType to GeofenceEventType
 */
private fun TransitionType.toEventType(): GeofenceEventType = when (this) {
    TransitionType.ENTER -> GeofenceEventType.ENTER
    TransitionType.EXIT -> GeofenceEventType.EXIT
    TransitionType.DWELL -> GeofenceEventType.DWELL
}

/**
 * Convert GeofenceEventType to TransitionType
 */
private fun GeofenceEventType.toTransitionType(): TransitionType = when (this) {
    GeofenceEventType.ENTER -> TransitionType.ENTER
    GeofenceEventType.EXIT -> TransitionType.EXIT
    GeofenceEventType.DWELL -> TransitionType.DWELL
}

/**
 * Convert server DTO to domain model
 */
private fun GeofenceDto.toDomain(): Geofence = Geofence(
    id = geofenceId,
    deviceId = deviceId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters.toInt(),
    transitionTypes = eventTypes.map { it.toTransitionType() }.toSet(),
    webhookId = metadata?.get("webhookId"),
    active = active,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

/**
 * Convert server DTO to Room entity
 */
private fun GeofenceDto.toEntity(): GeofenceEntity = GeofenceEntity(
    id = geofenceId,
    deviceId = deviceId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters.toInt(),
    transitionTypes = eventTypes.joinToString(",") { it.name },
    webhookId = metadata?.get("webhookId"),
    active = active,
    createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
    updatedAt = Instant.parse(updatedAt).toEpochMilliseconds(),
)
