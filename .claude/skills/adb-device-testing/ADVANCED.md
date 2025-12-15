# Advanced ADB Testing

## Location Spoofing Details

### Emulator Console Method (Alternative)
```bash
# Get auth token first
cat ~/.emulator_console_auth_token

# Connect via telnet
telnet localhost 5554

# Commands in telnet session:
auth <token>
geo fix <longitude> <latitude> [altitude]
```

### Physical Device Location Mocking
Physical devices require mock location apps:
1. Enable Developer Options > Allow mock locations
2. Install mock location app (e.g., Fake GPS)
3. Set app as mock location provider in developer options
4. Use the app to set coordinates

Alternatively via ADB (requires root):
```bash
adb shell appops set <package> android:mock_location allow
```

## Key Events Reference

| Key | Command |
|-----|---------|
| Back | `adb shell input keyevent KEYCODE_BACK` |
| Home | `adb shell input keyevent KEYCODE_HOME` |
| Menu | `adb shell input keyevent KEYCODE_MENU` |
| Enter | `adb shell input keyevent KEYCODE_ENTER` |
| Tab | `adb shell input keyevent KEYCODE_TAB` |
| Delete | `adb shell input keyevent KEYCODE_DEL` |
| Volume Up | `adb shell input keyevent KEYCODE_VOLUME_UP` |
| Volume Down | `adb shell input keyevent KEYCODE_VOLUME_DOWN` |
| Power | `adb shell input keyevent KEYCODE_POWER` |
| Camera | `adb shell input keyevent KEYCODE_CAMERA` |

## Multiple Devices

```bash
# List devices
adb devices -l

# Target specific device
adb -s <serial> shell ...
adb -s emulator-5554 shell input tap 100 200

# Get serial
adb get-serialno
```

## Performance Testing

```bash
# CPU info
adb shell dumpsys cpuinfo | head -20

# Memory info
adb shell dumpsys meminfo <package>

# Battery stats
adb shell dumpsys batterystats

# GPU rendering
adb shell dumpsys gfxinfo <package>
```

## Network Simulation

```bash
# Airplane mode
adb shell settings put global airplane_mode_on 1
adb shell am broadcast -a android.intent.action.AIRPLANE_MODE

# WiFi toggle
adb shell svc wifi disable
adb shell svc wifi enable

# Mobile data toggle
adb shell svc data disable
adb shell svc data enable
```

## Parsing uiautomator XML

Example element:
```xml
<node index="0" text="Login" resource-id="com.app:id/login_btn"
      class="android.widget.Button" bounds="[100,500][300,600]"/>
```

Extract coordinates:
- bounds="[x1,y1][x2,y2]" = [100,500][300,600]
- center_x = (100 + 300) / 2 = 200
- center_y = (500 + 600) / 2 = 550
- Tap: `adb shell input tap 200 550`

Search by resource-id:
```bash
grep 'resource-id="com.app:id/login_btn"' /tmp/ui.xml
```

Search by text:
```bash
grep 'text="Login"' /tmp/ui.xml
```

## Accessibility Testing

```bash
# Enable TalkBack
adb shell settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService

# Disable TalkBack
adb shell settings put secure enabled_accessibility_services ""

# Get accessibility focused node
adb shell dumpsys accessibility
```

## Automated Test Scripting

Example bash script for login flow:
```bash
#!/bin/bash
DEVICE="emulator-5554"
PKG="com.example.app"

# Setup
adb -s $DEVICE shell pm clear $PKG
adb -s $DEVICE shell am start -n $PKG/.MainActivity
sleep 2

# Screenshot initial
adb -s $DEVICE exec-out screencap -p > /tmp/01_initial.png

# Enter username (tap field first)
adb -s $DEVICE shell input tap 540 400
sleep 0.5
adb -s $DEVICE shell input text "testuser"

# Enter password
adb -s $DEVICE shell input tap 540 500
sleep 0.5
adb -s $DEVICE shell input text "password123"

# Tap login button
adb -s $DEVICE shell input tap 540 600
sleep 2

# Screenshot result
adb -s $DEVICE exec-out screencap -p > /tmp/02_after_login.png

echo "Test complete. Check /tmp/*.png"
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| No devices found | Check USB debugging, run `adb kill-server && adb start-server` |
| Permission denied | Accept USB debugging prompt on device |
| Screenshot black | Wait for app to render, increase sleep time |
| Wrong coordinates | Use uiautomator dump, recalculate from bounds |
| Location not updating | Verify emulator, check app location permissions |
| Text input issues | Use %s for spaces, special chars may not work |
