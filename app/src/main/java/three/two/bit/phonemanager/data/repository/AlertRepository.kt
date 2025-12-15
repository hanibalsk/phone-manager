package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant
import three.two.bit.phonemanager.data.database.ProximityAlertDao
import three.two.bit.phonemanager.data.model.ProximityAlertEntity
import three.two.bit.phonemanager.data.model.toDomain
import three.two.bit.phonemanager.data.model.toEntity
import three.two.bit.phonemanager.domain.model.AlertDirection
import three.two.bit.phonemanager.domain.model.ProximityAlert
import three.two.bit.phonemanager.domain.model.ProximityState
import three.two.bit.phonemanager.network.NetworkException
import three.two.bit.phonemanager.network.NetworkManager
import three.two.bit.phonemanager.network.ProximityAlertApiService
import three.two.bit.phonemanager.network.models.CreateProximityAlertRequest
import three.two.bit.phonemanager.network.models.ProximityAlertDto
import three.two.bit.phonemanager.network.models.UpdateProximityAlertRequest
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E5.1: AlertRepository - Proximity alert management with local + remote sync
 *
 * AC E5.1.4: Server sync via POST /api/v1/proximity-alerts
 * AC E5.1.5: Alert management (CRUD operations)
 * AC E5.1.6: Sync on startup
 */
interface AlertRepository {
    /**
     * Observe all alerts for current device
     */
    fun observeAlerts(): Flow<List<ProximityAlert>>

    /**
     * Observe only active alerts
     */
    fun observeActiveAlerts(): Flow<List<ProximityAlert>>

    /**
     * Get a single alert by ID
     */
    suspend fun getAlert(alertId: String): ProximityAlert?

    /**
     * Create a new proximity alert
     * Creates locally and syncs to server if network available
     */
    suspend fun createAlert(
        targetDeviceId: String,
        radiusMeters: Int,
        direction: AlertDirection,
    ): Result<ProximityAlert>

    /**
     * Update an existing alert
     */
    suspend fun updateAlert(alert: ProximityAlert): Result<ProximityAlert>

    /**
     * Toggle alert active state
     */
    suspend fun toggleAlertActive(alertId: String, active: Boolean): Result<Unit>

    /**
     * Delete an alert
     */
    suspend fun deleteAlert(alertId: String): Result<Unit>

    /**
     * Sync alerts from server
     * AC E5.1.6: Called on app startup
     */
    suspend fun syncFromServer(): Result<Int>

    /**
     * Update last triggered time for an alert
     */
    suspend fun updateLastTriggered(alertId: String, triggeredAt: Instant)

    /**
     * Update proximity state for an alert
     */
    suspend fun updateProximityState(alertId: String, state: ProximityState)
}

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val proximityAlertDao: ProximityAlertDao,
    private val alertApiService: ProximityAlertApiService,
    private val networkManager: NetworkManager,
    private val secureStorage: SecureStorage,
) : AlertRepository {

    private val ownerDeviceId: String
        get() = secureStorage.getDeviceId()

    override fun observeAlerts(): Flow<List<ProximityAlert>> =
        proximityAlertDao.observeAlertsByOwner(ownerDeviceId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeActiveAlerts(): Flow<List<ProximityAlert>> =
        proximityAlertDao.observeActiveAlerts(ownerDeviceId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getAlert(alertId: String): ProximityAlert? = proximityAlertDao.getById(alertId)?.toDomain()

    override suspend fun createAlert(
        targetDeviceId: String,
        radiusMeters: Int,
        direction: AlertDirection,
    ): Result<ProximityAlert> {
        val now = Clock.System.now()
        val alertId = UUID.randomUUID().toString()

        // Create domain model
        val alert = ProximityAlert(
            id = alertId,
            ownerDeviceId = ownerDeviceId,
            targetDeviceId = targetDeviceId,
            radiusMeters = radiusMeters,
            direction = direction,
            active = true,
            lastState = ProximityState.OUTSIDE,
            createdAt = now,
            updatedAt = now,
            lastTriggeredAt = null,
        )

        // Save locally first
        proximityAlertDao.insert(alert.toEntity())
        Timber.d("Alert created locally: $alertId")

        // Sync to server if network available
        if (networkManager.isNetworkAvailable()) {
            val request = CreateProximityAlertRequest(
                sourceDeviceId = ownerDeviceId,
                targetDeviceId = targetDeviceId,
                radiusMeters = radiusMeters,
                isActive = true,
                metadata = mapOf("direction" to direction.name),
            )

            alertApiService.createAlert(request).fold(
                onSuccess = { dto ->
                    // Update local with server ID if different
                    if (dto.alertId != alertId) {
                        proximityAlertDao.delete(alert.toEntity())
                        val serverAlert = dto.toDomain(direction)
                        proximityAlertDao.insert(serverAlert.toEntity())
                        Timber.i("Alert synced to server: ${dto.alertId}")
                        return Result.success(serverAlert)
                    }
                    Timber.i("Alert synced to server: ${dto.alertId}")
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to sync alert to server, will retry later")
                },
            )
        } else {
            Timber.d("Network unavailable, alert saved locally for later sync")
        }

        return Result.success(alert)
    }

    override suspend fun updateAlert(alert: ProximityAlert): Result<ProximityAlert> {
        val updatedAlert = alert.copy(updatedAt = Clock.System.now())

        // Update locally
        proximityAlertDao.update(updatedAlert.toEntity())
        Timber.d("Alert updated locally: ${alert.id}")

        // Sync to server if network available
        if (networkManager.isNetworkAvailable()) {
            val request = UpdateProximityAlertRequest(
                radiusMeters = alert.radiusMeters,
                isActive = alert.active,
                metadata = mapOf("direction" to alert.direction.name),
            )

            alertApiService.updateAlert(alert.id, request).fold(
                onSuccess = {
                    Timber.i("Alert update synced to server: ${alert.id}")
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to sync alert update to server")
                },
            )
        }

        return Result.success(updatedAlert)
    }

    override suspend fun toggleAlertActive(alertId: String, active: Boolean): Result<Unit> {
        val existing = proximityAlertDao.getById(alertId)
            ?: return Result.failure(IllegalArgumentException("Alert not found: $alertId"))

        val updatedEntity = existing.copy(
            active = active,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        proximityAlertDao.update(updatedEntity)
        Timber.d("Alert ${if (active) "activated" else "deactivated"}: $alertId")

        // Sync to server
        if (networkManager.isNetworkAvailable()) {
            alertApiService.updateAlert(alertId, UpdateProximityAlertRequest(isActive = active))
                .onFailure { error ->
                    Timber.w(error, "Failed to sync alert toggle to server")
                }
        }

        return Result.success(Unit)
    }

    override suspend fun deleteAlert(alertId: String): Result<Unit> {
        val existing = proximityAlertDao.getById(alertId)
            ?: return Result.failure(IllegalArgumentException("Alert not found: $alertId"))

        // Delete locally
        proximityAlertDao.delete(existing)
        Timber.d("Alert deleted locally: $alertId")

        // Delete from server
        if (networkManager.isNetworkAvailable()) {
            alertApiService.deleteAlert(alertId).fold(
                onSuccess = {
                    Timber.i("Alert deleted from server: $alertId")
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to delete alert from server")
                },
            )
        }

        return Result.success(Unit)
    }

    /**
     * AC E5.1.6: Sync alerts from server on startup
     *
     * Strategy: Replace local alerts with server data (server is source of truth)
     */
    override suspend fun syncFromServer(): Result<Int> {
        if (!networkManager.isNetworkAvailable()) {
            return Result.failure(NetworkException("No network connection available"))
        }

        return alertApiService.listAlerts(ownerDeviceId, includeInactive = true).map { response ->
            Timber.d("Fetched ${response.total} alerts from server")

            // Clear local and replace with server data
            proximityAlertDao.deleteAllByOwner(ownerDeviceId)

            response.alerts.forEach { dto ->
                val entity = dto.toEntity()
                proximityAlertDao.insert(entity)
            }

            Timber.i("Synced ${response.total} alerts from server")
            response.total
        }
    }

    override suspend fun updateLastTriggered(alertId: String, triggeredAt: Instant) {
        val existing = proximityAlertDao.getById(alertId) ?: return
        val updated = existing.copy(
            lastTriggeredAt = triggeredAt.toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        proximityAlertDao.update(updated)
    }

    override suspend fun updateProximityState(alertId: String, state: ProximityState) {
        val existing = proximityAlertDao.getById(alertId) ?: return
        val updated = existing.copy(
            lastState = state.name,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        proximityAlertDao.update(updated)
    }
}

/**
 * Convert server DTO to domain model
 */
private fun ProximityAlertDto.toDomain(direction: AlertDirection = AlertDirection.BOTH): ProximityAlert {
    // Extract direction from metadata if available
    val alertDirection = metadata?.get("direction")?.let {
        try {
            AlertDirection.valueOf(it)
        } catch (e: IllegalArgumentException) {
            direction
        }
    } ?: direction

    return ProximityAlert(
        id = alertId,
        ownerDeviceId = sourceDeviceId,
        targetDeviceId = targetDeviceId,
        targetDisplayName = name,
        radiusMeters = radiusMeters,
        direction = alertDirection,
        active = isActive,
        lastState = ProximityState.OUTSIDE, // Default state
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
        lastTriggeredAt = null,
    )
}

/**
 * Convert server DTO to Room entity
 */
private fun ProximityAlertDto.toEntity(): ProximityAlertEntity {
    val direction = metadata?.get("direction") ?: AlertDirection.BOTH.name

    return ProximityAlertEntity(
        id = alertId,
        ownerDeviceId = sourceDeviceId,
        targetDeviceId = targetDeviceId,
        radiusMeters = radiusMeters,
        direction = direction,
        active = isActive,
        lastState = ProximityState.OUTSIDE.name,
        createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
        updatedAt = Instant.parse(updatedAt).toEpochMilliseconds(),
        lastTriggeredAt = null,
    )
}
