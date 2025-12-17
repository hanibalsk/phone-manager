package three.two.bit.phonemanager.ui.enrollment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import three.two.bit.phonemanager.R

/**
 * Story E13.10: Android Enrollment Flow - Success Screen
 *
 * AC E13.10.6: Enrollment success screen showing:
 * - Success message with checkmark
 * - Organization name
 * - Policy summary (locked settings count)
 * - IT contact info
 * - "Get Started" button
 */
@Composable
fun EnrollmentSuccessScreen(onGetStarted: () -> Unit, viewModel: EnrollmentViewModel = hiltViewModel()) {
    val organizationInfo by viewModel.organizationInfo.collectAsState()
    val devicePolicy by viewModel.devicePolicy.collectAsState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Success checkmark icon (AC E13.10.6)
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.enrollment_success_content_desc),
                modifier = Modifier.size(96.dp),
                tint = Color(0xFF4CAF50), // Green success color
            )

            // Success message (AC E13.10.6)
            Text(
                text = stringResource(R.string.enrollment_success_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.enrollment_success_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Organization info card (AC E13.10.6)
            organizationInfo?.let { org ->
                OrganizationCard(
                    organizationName = org.name,
                    contactEmail = org.contactEmail,
                    supportPhone = org.supportPhone,
                )
            }

            // Policy summary card (AC E13.10.6)
            devicePolicy?.let { policy ->
                val lockedCount = policy.lockedCount()
                if (lockedCount > 0) {
                    PolicySummaryCard(lockedSettingsCount = lockedCount)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Get Started button (AC E13.10.6)
            Button(
                onClick = {
                    viewModel.resetState()
                    onGetStarted()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.enrollment_get_started))
            }

            // Info text
            Text(
                text = stringResource(R.string.enrollment_success_info),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Organization information card.
 * AC E13.10.6: Display organization name and IT contact info.
 */
@Composable
private fun OrganizationCard(organizationName: String, contactEmail: String?, supportPhone: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Organization name
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.enrollment_organization_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = organizationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Contact info
            if (contactEmail != null || supportPhone != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )

                Text(
                    text = stringResource(R.string.enrollment_it_support_contact_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )

                contactEmail?.let { email ->
                    ContactInfoRow(
                        icon = Icons.Default.Email,
                        text = email,
                    )
                }

                supportPhone?.let { phone ->
                    ContactInfoRow(
                        icon = Icons.Default.Phone,
                        text = phone,
                    )
                }
            }
        }
    }
}

/**
 * Contact information row.
 */
@Composable
private fun ContactInfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * Policy summary card.
 * AC E13.10.6: Show locked settings count.
 */
@Composable
private fun PolicySummaryCard(lockedSettingsCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.enrollment_device_policy_applied_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.enrollment_locked_settings_managed,
                        lockedSettingsCount,
                        lockedSettingsCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
