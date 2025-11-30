package three.two.bit.phonemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.material.icons.rounded.Webhook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import three.two.bit.phonemanager.ui.theme.PhoneManagerTheme

/**
 * QuickActionCard - A modern card-style navigation button with icon and title.
 * Used for dashboard navigation in the HomeScreen.
 */
@Composable
fun QuickActionCard(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickActionCardPreview() {
    PhoneManagerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            QuickActionCard(
                icon = Icons.Rounded.Groups,
                title = "Group",
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickActionCardGridPreview() {
    PhoneManagerTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                QuickActionCard(
                    icon = Icons.Rounded.Groups,
                    title = "Group",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    icon = Icons.Rounded.Map,
                    title = "Map",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                QuickActionCard(
                    icon = Icons.Rounded.History,
                    title = "History",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Alerts",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                QuickActionCard(
                    icon = Icons.Rounded.ShareLocation,
                    title = "Geofences",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    icon = Icons.Rounded.Webhook,
                    title = "Webhooks",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
