# ClipVault Debugging Guide

## Viewing Logcat for Critical Components

Connect device via USB and run this comprehensive logcat filter:
```bash
adb logcat -s DatabaseModule AppDB CryptoManager AiProviderRepo SettingsVM HomeScreen MainActivity ClipVault NewItemVM -v time
```

Or grep the full stream:
```bash
adb logcat | grep -E 'DatabaseModule|AppDB|CryptoManager|AiProviderRepo|SettingsVM|HomeScreen|MainActivity|ClipVault|NewItemVM'
```

## Database & Migration Diagnostics

If you encounter database anomalies or want to inspect local files, run:

1. **Check SQLite DB files existence:**
   ```bash
   adb shell ls -la /data/data/com.clipvault.app/databases/
   ```

2. **Pull SQLite DB to inspect locally (requires root or debug build):**
   ```bash
   adb backup -f backup.ab com.clipvault.app
   # Or using run-as:
   adb shell "run-as com.clipvault.app cp /data/data/com.clipvault.app/databases/clipvault.db /sdcard/"
   adb pull /sdcard/clipvault.db .
   ```

## Common Issues & Troubleshooting

- **White screen on Settings**: Check `SettingsVM` and `CryptoManager` logs. If Keystore fails to initialize, check the on-demand `ensureInitialized` logs inside `CryptoManager`.
- **Database migration failures**: Look for `DatabaseModule` and `AppDB` logs. If migration fails, pre-existing data is preserved and the app will boot safely with `AppDatabase.migrationFailed = true`. A warning banner will appear in the settings page.
- **AI analysis fails**: Check `AiProviderRepo` logs for getApiKey/encryption issues.
- **Add button crashes**: Check `NewItemVM` logs. Ensure there are no database instantiation or injection errors.
