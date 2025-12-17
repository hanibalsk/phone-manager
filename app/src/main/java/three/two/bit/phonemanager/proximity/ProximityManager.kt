package three.two.bit.phonemanager.proximity

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.first
import three.two.bit.phonemanager.data.repository.AlertRepository
import three.two.bit.phonemanager.domain.model.Device
import three.two.bit.phonemanager.domain.model.ProximityAlert
import three.two.bit.phonemanager.notification.ProximityNotificationManager
import three.two.bit.phonemanager.util.ProximityCalculator
import three.two.bit.phonemanager.util.ProximityCheckResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Story E5.2: Proximity Manager
 *
 * Coordinates proximity alert checking during location polling cycles.
 * AC E5.2.2: Calculation during poll cycle
 * AC E5.2.3: State tracking
 * AC E5.2.4: Trigger on state transition only
 * AC E5.2.5: Local notification on trigger
 * AC E5.2.6: Update lastTriggeredAt
 */
@Singleton
class ProximityManager @Inject constructor(
    private val alertRepository: AlertRepository,
    private val notificationManager: ProximityNotificationManager,
) {
    /**
     * Check all active proximity alerts against current and group member locations (AC E5.2.2)
     *
     * Called during each polling cycle from MapViewModel
     *
     * @param myLocation Current device location
     * @param groupMembers List of group members with their locations
     */
    suspend fun checkProximityAlerts(myLocation: LatLng, groupMembers: List<Device>) {
        // Get all active alerts
        val activeAlerts = alertRepository.observeActiveAlerts().first()

        if (activeAlerts.isEmpty()) {
            return
        }

        Timber.d("Checking ${activeAlerts.size} proximity alerts against ${groupMembers.size} group members")

        // Create lookup map for group members by device ID
        val membersByDeviceId = groupMembers.associateBy { it.deviceId }

        for (alert in activeAlerts) {
            checkSingleAlert(alert, myLocation, membersByDeviceId)
        }
    }

    /**
     * Check a single proximity alert (AC E5.2.1, E5.2.4)
     */
    private suspend fun checkSingleAlert(
        alert: ProximityAlert,
        myLocation: LatLng,
        membersByDeviceId: Map<String, Device>,
    ) {
        // Find target device
        val targetDevice = membersByDeviceId[alert.targetDeviceId]
        if (targetDevice == null) {
            Timber.d("Target device ${alert.targetDeviceId} not found in group members")
            return
        }

        // Get target location
        val deviceLocation = targetDevice.lastLocation
        if (deviceLocation == null) {
            Timber.d("Target device ${alert.targetDeviceId} has no location")
            return
        }

        val targetLocation = LatLng(deviceLocation.latitude, deviceLocation.longitude)

        // Calculate proximity and check for trigger (AC E5.2.1, E5.2.4)
        val result = ProximityCalculator.checkProximity(myLocation, targetLocation, alert)

        Timber.d(
            "Proximity check for alert ${alert.id}: " +
                "distance=${result.distance}m, state=${result.newState}, triggered=${result.triggered}",
        )

        // AC E5.2.3: Update state if changed
        if (alert.lastState != result.newState) {
            updateAlertState(alert, result)
        }

        // AC E5.2.4, E5.2.5, E5.2.6: Handle trigger if needed
        if (result.triggered) {
            handleAlertTriggered(alert, targetDevice, result)
        }
    }

    /**
     * Update alert state in database (AC E5.2.3)
     */
    private suspend fun updateAlertState(alert: ProximityAlert, result: ProximityCheckResult) {
        alertRepository.updateProximityState(alert.id, result.newState)
        Timber.d("Updated proximity state for alert ${alert.id}: ${alert.lastState} -> ${result.newState}")
    }

    /**
     * Handle alert trigger - show notification and update timestamp (AC E5.2.5, E5.2.6)
     */
    private suspend fun handleAlertTriggered(
        alert: ProximityAlert,
        targetDevice: Device,
        result: ProximityCheckResult,
    ) {
        val targetName = alert.targetDisplayName ?: targetDevice.displayName

        // AC E5.2.5: Show notification
        notificationManager.showProximityAlert(alert, targetName, result.distance)

        // AC E5.2.6: Update lastTriggeredAt
        val triggeredAt = Clock.System.now()
        alertRepository.updateLastTriggered(alert.id, triggeredAt)

        Timber.i(
            "Proximity alert triggered: ${alert.id}, target=$targetName, " +
                "distance=${result.distance}m, direction=${alert.direction}",
        )
    }
}
