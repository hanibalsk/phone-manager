package three.two.bit.phonemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.data.model.HealthStatus
import three.two.bit.phonemanager.domain.model.EnhancedServiceState
import three.two.bit.phonemanager.domain.model.ServiceStatus
import three.two.bit.phonemanager.ui.theme.PhoneManagerTheme
import java.time.Duration

/**
 * Story 1.3: ServiceStatusCard - Displays service health and status
 */
@Composable
fun ServiceStatusCard(serviceState: EnhancedServiceState, modifier: Modifier = Modifier, onCardClick: () -> Unit = {}) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (serviceState.status) {
                ServiceStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                ServiceStatus.GPS_ACQUIRING -> MaterialTheme.colorScheme.tertiaryContainer
                ServiceStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Status indicator dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when (serviceState.healthStatus) {
                                    HealthStatus.HEALTHY -> Color.Green
                                    HealthStatus.GPS_ACQUIRING -> Color.Yellow
                                    HealthStatus.GPS_UNAVAILABLE -> Color(0xFFFF9800) // Orange
                                    HealthStatus.NO_GPS_SIGNAL -> Color(0xFFFF9800)
                                    HealthStatus.ERROR -> Color.Red
                                },
                                shape = CircleShape,
                            ),
                    )

                    Text(
                        text = when (serviceState.status) {
                            ServiceStatus.STOPPED -> "Tracking Stopped"
                            ServiceStatus.STARTING -> "Starting..."
                            ServiceStatus.RUNNING -> "Tracking Active"
                            ServiceStatus.GPS_ACQUIRING -> "GPS Acquiring..."
                            ServiceStatus.STOPPING -> "Stopping..."
                            ServiceStatus.ERROR -> "Service Error"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when {
                        serviceState.errorMessage != null -> serviceState.errorMessage
                        serviceState.healthStatus == HealthStatus.GPS_UNAVAILABLE ->
                            "Location services are disabled"
                        serviceState.healthStatus == HealthStatus.GPS_ACQUIRING ->
                            "Waiting for GPS signal..."
                        serviceState.status == ServiceStatus.RUNNING ->
                            "Collecting locations every ${serviceState.currentInterval.toMinutes()} minutes"
                        else -> "Ready to start tracking"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Icon
            Icon(
                imageVector = when (serviceState.status) {
                    ServiceStatus.RUNNING -> Icons.Default.GpsFixed
                    ServiceStatus.GPS_ACQUIRING -> Icons.Default.GpsNotFixed
                    ServiceStatus.ERROR -> Icons.Default.Error
                    else -> Icons.Default.GpsOff
                },
                contentDescription = null,
                tint = when (serviceState.healthStatus) {
                    HealthStatus.HEALTHY -> MaterialTheme.colorScheme.primary
                    HealthStatus.ERROR -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ServiceStatusCardPreview() {
    PhoneManagerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ServiceStatusCard(
                serviceState = EnhancedServiceState(
                    isRunning = true,
                    status = ServiceStatus.RUNNING,
                    lastUpdate = java.time.Instant.now(),
                    locationCount = 42,
                    currentInterval = Duration.ofMinutes(5),
                    healthStatus = HealthStatus.HEALTHY,
                ),
            )

            ServiceStatusCard(
                serviceState = EnhancedServiceState(
                    isRunning = true,
                    status = ServiceStatus.GPS_ACQUIRING,
                    lastUpdate = null,
                    locationCount = 0,
                    currentInterval = Duration.ofMinutes(5),
                    healthStatus = HealthStatus.GPS_ACQUIRING,
                ),
            )

            ServiceStatusCard(
                serviceState = EnhancedServiceState(
                    isRunning = false,
                    status = ServiceStatus.ERROR,
                    lastUpdate = null,
                    locationCount = 0,
                    currentInterval = Duration.ofMinutes(5),
                    healthStatus = HealthStatus.ERROR,
                    errorMessage = "GPS signal lost",
                ),
            )
        }
    }
}
