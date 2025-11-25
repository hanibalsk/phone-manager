package three.two.bit.phonemanager.data.model

/**
 * Epic 0.2/1.3: ServiceHealth - Service health status data model
 * Used for real-time service monitoring in Epic 1.3
 */
data class ServiceHealth(
    val isRunning: Boolean,
    val lastLocationUpdate: Long? = null,
    val locationCount: Int = 0,
    val healthStatus: HealthStatus = HealthStatus.HEALTHY,
    val errorMessage: String? = null,
)

enum class HealthStatus {
    HEALTHY, // Service running, locations being collected
    GPS_UNAVAILABLE, // Location services disabled
    GPS_ACQUIRING, // Service running but no GPS fix yet
    NO_GPS_SIGNAL, // Service running but no GPS fix
    ERROR, // Service error state
}
