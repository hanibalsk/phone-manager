package three.two.bit.phonemanager.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Story E12.7: Admin Settings API Models
 *
 * API request/response models for admin settings management.
 */

// ============================================================================
// Request Models
// ============================================================================

/**
 * Request to update device settings.
 * AC E12.7.4: Modify Remote Settings
 */
@Serializable
data class UpdateDeviceSettingsRequest(
    val changes: Map<String, @Serializable(with = AnySerializer::class) Any>,
    @SerialName("notify_user") val notifyUser: Boolean = true,
)

/**
 * Request to lock/unlock settings.
 * AC E12.7.5: Lock/Unlock Settings
 */
@Serializable
data class LockSettingsRequest(
    @SerialName("setting_keys") val settingKeys: List<String>,
    val lock: Boolean,
)

/**
 * Request for bulk settings update.
 * AC E12.7.6: Bulk Settings Application
 */
@Serializable
data class BulkUpdateSettingsRequest(
    @SerialName("device_ids") val deviceIds: List<String>,
    val settings: Map<String, @Serializable(with = AnySerializer::class) Any>,
    val locks: List<String>? = null,
    @SerialName("notify_users") val notifyUsers: Boolean = true,
)

/**
 * Request to create/update a settings template.
 * AC E12.7.7: Settings Templates
 */
@Serializable
data class SaveTemplateRequest(
    val name: String,
    val description: String? = null,
    val settings: Map<String, @Serializable(with = AnySerializer::class) Any>,
    @SerialName("locked_settings") val lockedSettings: List<String>,
    @SerialName("is_shared") val isShared: Boolean = false,
)

// ============================================================================
// Response Models
// ============================================================================

/**
 * Response with device settings for admin.
 * AC E12.7.3: View Remote Settings
 */
@Serializable
data class AdminDeviceSettingsResponse(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("owner_email") val ownerEmail: String,
    @SerialName("is_online") val isOnline: Boolean,
    @SerialName("last_seen") val lastSeen: String? = null,
    val settings: Map<String, @Serializable(with = AnySerializer::class) Any>,
    val locks: Map<String, SettingLockResponse>,
    @SerialName("last_synced_at") val lastSyncedAt: String? = null,
    @SerialName("last_modified_by") val lastModifiedBy: String? = null,
)

/**
 * Response for settings update.
 * AC E12.7.4: Modify Remote Settings
 */
@Serializable
data class UpdateSettingsResponse(
    val success: Boolean,
    @SerialName("applied_settings") val appliedSettings: Map<String, @Serializable(with = AnySerializer::class) Any>? = null,
    @SerialName("push_sent") val pushSent: Boolean = false,
    val error: String? = null,
)

/**
 * Response for lock/unlock operation.
 * AC E12.7.5: Lock/Unlock Settings
 */
@Serializable
data class LockSettingsResponse(
    val success: Boolean,
    @SerialName("locked_count") val lockedCount: Int = 0,
    @SerialName("unlocked_count") val unlockedCount: Int = 0,
    val error: String? = null,
)

/**
 * Response for bulk update operation.
 * AC E12.7.6: Bulk Settings Application
 */
@Serializable
data class BulkUpdateResponse(
    val successful: List<BulkUpdateDeviceResult>,
    val failed: List<BulkUpdateDeviceResult>,
)

@Serializable
data class BulkUpdateDeviceResult(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String = "",
    val success: Boolean = true,
    val error: String? = null,
    @SerialName("applied_settings") val appliedSettings: Map<String, @Serializable(with = AnySerializer::class) Any>? = null,
)

/**
 * Response for settings history.
 * AC E12.7.8: Audit Trail
 */
@Serializable
data class SettingsHistoryResponse(
    val changes: List<SettingChangeResponse>,
    @SerialName("total_count") val totalCount: Int,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class SettingChangeResponse(
    val id: String,
    @SerialName("setting_key") val settingKey: String,
    @SerialName("old_value") val oldValue: @Serializable(with = AnySerializer::class) Any? = null,
    @SerialName("new_value") val newValue: @Serializable(with = AnySerializer::class) Any? = null,
    @SerialName("changed_by") val changedBy: String,
    @SerialName("changed_by_name") val changedByName: String,
    @SerialName("changed_at") val changedAt: String,
    @SerialName("change_type") val changeType: String,
)

/**
 * Response for settings templates list.
 * AC E12.7.7: Settings Templates
 */
@Serializable
data class TemplatesResponse(
    val templates: List<SettingsTemplateResponse>,
)

@Serializable
data class SettingsTemplateResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val settings: Map<String, @Serializable(with = AnySerializer::class) Any>,
    @SerialName("locked_settings") val lockedSettings: List<String>,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_by_name") val createdByName: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_shared") val isShared: Boolean = false,
)

@Serializable
data class SaveTemplateResponse(
    val success: Boolean,
    val template: SettingsTemplateResponse? = null,
    val error: String? = null,
)

/**
 * Member device info for list display.
 * AC E12.7.1: Device Settings List Screen
 */
@Serializable
data class MemberDeviceResponse(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("owner_email") val ownerEmail: String,
    @SerialName("is_online") val isOnline: Boolean,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("locked_settings_count") val lockedSettingsCount: Int = 0,
)

@Serializable
data class MemberDevicesResponse(
    val devices: List<MemberDeviceResponse>,
    @SerialName("total_count") val totalCount: Int,
)

/**
 * Response for setting lock information.
 * Used in AdminDeviceSettingsResponse.
 */
@Serializable
data class SettingLockResponse(
    @SerialName("is_locked") val isLocked: Boolean,
    @SerialName("locked_by") val lockedBy: String? = null,
    @SerialName("locked_at") val lockedAt: String? = null,
)

// ============================================================================
// Serializer for Any type
// ============================================================================

/**
 * Custom serializer for Any type to handle dynamic JSON values.
 */
object AnySerializer : kotlinx.serialization.KSerializer<Any> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("Any")

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Any) {
        val jsonEncoder = encoder as? kotlinx.serialization.json.JsonEncoder
            ?: throw kotlinx.serialization.SerializationException("This class can only be serialized by JSON")

        val jsonElement = when (value) {
            is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
            is Number -> kotlinx.serialization.json.JsonPrimitive(value)
            is String -> kotlinx.serialization.json.JsonPrimitive(value)
            else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
        }
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Any {
        val jsonDecoder = decoder as? kotlinx.serialization.json.JsonDecoder
            ?: throw kotlinx.serialization.SerializationException("This class can only be deserialized by JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" || element.content == "false" -> element.content.toBoolean()
                    element.content.contains('.') -> element.content.toDoubleOrNull() ?: element.content
                    else -> element.content.toLongOrNull() ?: element.content.toIntOrNull() ?: element.content
                }
            }
            else -> element.toString()
        }
    }
}
