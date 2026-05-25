# ClipVault Debugging Guide

## Viewing Logcat for Settings White-Screen Issues

Connect device via USB and run:
```
adb logcat -s SettingsVM AiProviderRepo DetailVM -v time
```

## Filtering by Process
```
adb logcat | grep -E 'SettingsVM|AiProviderRepo|DetailVM'
```

## Common Issues
- **White screen on Settings**: Check SettingsVM init logs. If you see `init: start` but not `init: themeMode subscribed`, the ThemePreferences DataStore is blocked.
- **AI analysis fails**: Check AiProviderRepo logs for getApiKey errors.
