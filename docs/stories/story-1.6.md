# Story 1.6: System Integration & Auto-Start on Boot

**Status:** Ready for Implementation
**Epic:** 1 - Background Location Tracking Service
**Priority:** MVP - Critical Path
**Complexity:** Medium
**Estimated Effort:** 3-5 days

## Story

As a user,
I want the location tracking service to start automatically when my phone boots,
so that location tracking continues without manual intervention after restarts.

## Acceptance Criteria

1. **Boot Receiver:**
   - [ ] Broadcast receiver responds to BOOT_COMPLETED
   - [ ] Service starts automatically on boot if tracking was enabled
   - [ ] Permission state verified before starting service
   - [ ] Handle ACTION_MY_PACKAGE_REPLACED for app updates

2. **Service Reliability:**
   - [ ] Service survives device restart
   - [ ] Service survives app update
   - [ ] Pending uploads resume after boot
   - [ ] No data loss during restart

3. **Battery Optimization:**
   - [ ] User prompted to disable battery optimization (if needed)
   - [ ] Handle Doze mode restrictions
   - [ ] Detect aggressive OEM battery optimization
   - [ ] Provide manufacturer-specific instructions

4. **System Integration:**
   - [ ] Handle low battery gracefully
   - [ ] Handle airplane mode transitions
   - [ ] Handle device admin changes
   - [ ] Service state persists correctly

5. **Testing:**
   - [ ] Test device reboot multiple times
   - [ ] Test after app update
   - [ ] Test on multiple OEM devices
   - [ ] Test battery saver mode enabled

## Tasks / Subtasks

### Task 1: Add Permissions
```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

### Task 2: Create Boot Receiver
```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                handleAppUpdated(context)
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val config = getTrackingConfig(context)
            if (config.isTrackingEnabled && hasPermissions(context)) {
                startLocationService(context)
                scheduleUploadWork(context)
            }
        }
    }
}
```

### Task 3: Declare Receiver in Manifest
```xml
<receiver
    android:name=".receiver.BootReceiver"
    android:enabled="true"
    android:exported="true"
    android:directBootAware="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
    </intent-filter>
</receiver>
```

### Task 4: Battery Optimization Helper
```kotlin
class BatteryOptimizationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService<PowerManager>()
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun requestBatteryOptimizationExemption(activity: Activity) {
        if (!isIgnoringBatteryOptimizations()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            activity.startActivity(intent)
        }
    }

    fun getManufacturerSpecificInstructions(): String {
        return when (Build.MANUFACTURER.lowercase()) {
            "xiaomi" -> "Go to Settings > Battery & Performance > App Battery Saver > Phone Manager > No restrictions"
            "samsung" -> "Go to Settings > Apps > Phone Manager > Battery > Unrestricted"
            "huawei" -> "Go to Settings > Battery > App Launch > Phone Manager > Manage manually"
            "oppo" -> "Go to Settings > Battery > App Battery Management > Phone Manager"
            else -> "Disable battery optimization for Phone Manager in system settings"
        }
    }
}
```

### Task 5: Tracking State Persistence
```kotlin
suspend fun setTrackingEnabled(enabled: Boolean) {
    dataStore.edit { prefs ->
        prefs[Keys.TRACKING_ENABLED] = enabled
    }
}
```

### Task 6: Testing
- Test device reboot scenarios
- Test OEM-specific behaviors
- Test app updates
- Test battery optimization states

## Definition of Done

- [ ] All acceptance criteria met
- [ ] Service starts on boot reliably
- [ ] Tested on multiple OEM devices
- [ ] Battery optimization handled
- [ ] Documentation complete

## Dependencies

**Blocks:** None (MVP complete after this)

**Blocked By:**
- Story 1.2 (service) ✅
- Story 1.5 (upload scheduler) ✅

## References

- [Boot Completed](https://developer.android.com/reference/android/content/Intent#ACTION_BOOT_COMPLETED)
- [Battery Optimization](https://developer.android.com/training/monitoring-device-state/doze-standby)

---

**Epic:** [Epic 1](../epics/epic-1-location-tracking.md)
**Previous:** [Story 1.5](./story-1.5.md) | **Next:** [Story 1.7](./story-1.7.md)
