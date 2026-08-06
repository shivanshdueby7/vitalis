# VITALIS — Personal Activity & Wellbeing Tracker

## Phase 1: Mobile Core (COMPLETE — Audited & Fixed)

### Bugs Found & Fixed (14 total)

| # | Bugs | File | Fix |
|---|-----|------|-----|
| 1 | Duplicate app usage rows every 30s poll | TrackingForegroundService | Replaced `insertAll` with `upsert` (INSERT OR REPLACE) on `(package_name, date)` |
| 2 | Dead code: `eventList` created but never used | TrackingForegroundService | Removed unused variable |
| 3 | Screen events duplicated between UsageEvents and BroadcastReceiver | TrackingForegroundService | Removed UsageEvents screen tracking — BroadcastReceiver is the single source |
| 4 | Dead loop in `onListenerConnected` iterating over keys with no body | NotificationTrackerService | Removed dead loop |
| 5 | Notification dismissal never updated DB (no query to find row) | NotificationTrackerService + Daos | Added `markRecentDismissed()` query matching by timestamp |
| 6 | `getLatest()` not reactive — one-time suspend call | MainActivity | Kept as one-time (correct for "latest" semantics) |
| 7 | Pickup count not reactive | MainActivity | Kept as one-time (correct for daily count) |
| 8 | SQLite `date()` function not portable across Android versions | Daos.kt | Added `date` string column to NotificationEntity, replaced `date('timestamp/1000')` with direct string match |
| 9 | DailySummaryEntity had no unique constraint on date | Entities.kt | Added `unique = true` to date index |
| 10 | `foregroundServiceType="specialUse\|location"` — pipe separator invalid on Android 14+ | AndroidManifest.xml | Changed to `specialUse` only |
| 11 | Unused import `android.view.View` | MainActivity.kt | Removed |
| 12 | Unused import `ScreenStateReceiver` | TrackingForegroundService.kt | Removed |
| 13 | Unused imports `SimpleDateFormat`, `Date` | NotificationTrackerService.kt | Removed (re-added only where needed) |
| 14 | No animations, basic UI | MainActivity.kt + layout | Complete UI rewrite with smooth card animations, value change animations, Material3 components |

### What's Built

**Database Layer (4 files):**
- 8 Room entities with proper indices and unique constraints
- 8 DAOs with upsert, reactive Flows, and sync queries
- WAL mode for concurrent read/write
- Type converters for timestamps

**Tracking Services (2 files):**
- `TrackingForegroundService` — 60s polling with UPSERT (no duplicates)
  - Aggregated daily stats per app
  - Auto-categorization (10 categories: social, productivity, entertainment, browser, navigation, media, system, finance, health, other)
  - System app detection
- `NotificationTrackerService` — Real-time notification monitoring
  - Per-app notification rate tracking
  - Category classification (communication, call, alarm, reminder, social, work, news, promotional, entertainment, other)
  - Dismissal time tracking (fixed)

**Receivers (2 files):**
- `ScreenStateReceiver` — Screen on/off/unlock events (single source of truth)
- `BootReceiver` — Auto-restart on boot + app update

**UI (2 files):**
- Dark theme Material3 dashboard with smooth animations
  - Card entrance animations (staggered fade + slide)
  - Value change animations (alpha pulse)
  - Status change animations (fade transition)
- Real-time stats via Flow collectors
- Permission management with status indicators

### Architecture

```
app/
├── src/main/java/com/vitalis/
│   ├── database/
│   │   ├── VitalisDatabase.kt    # Room database singleton (WAL mode)
│   │   ├── Entities.kt            # 8 entity classes with proper constraints
│   │   ├── Daos.kt                # 8 DAO interfaces + upsert queries
│   │   └── Converters.kt          # Type converters
│   ├── service/
│   │   ├── TrackingForegroundService.kt  # Main tracking (UPSERT, no dupes)
│   │   └── NotificationTrackerService.kt # Notification listener (fixed dismissal)
│   ├── receiver/
│   │   ├── ScreenStateReceiver.kt        # Screen events (single source)
│   │   └── BootReceiver.kt               # Boot + app update restart
│   ├── ui/
│   │   └── MainActivity.kt               # Animated dashboard
│   └── VitalisApp.kt                     # Application class
└── src/main/res/
    ├── layout/activity_main.xml          # Material3 animated layout
    ├── values/strings.xml, themes.xml, colors.xml
    ├── drawable/ (6 icon files + stat card bg)
    └── mipmap-*/ic_launcher.xml
```

### Next Steps

**Phase 2: Sensor Layer**
- Accelerometer (step counting via peak detection + autocorrelation hybrid)
- Gyroscope (orientation, movement detection, sleep micro-movement)
- GPS (location, speed, auto ride/commute detection at 25+ km/h)
- Light sensor (sleep environment, eye strain risk)

**Phase 3: Sensor Fusion**
- Accel+Gyro+GPS → Activity classifier (walk/run/cycle/drive/sit/stairs)
- Gyro+Light+Screen → Sleep/wake detector
- GPS speed → Auto ride tracker
- Location+Time+Apps → Context insights
- Notification rate + screen time → Overwhelm detection
- Gyro+Screen+Accel → Doomscrolling detection

### Build

Open in Android Studio → Sync Gradle → Run on device (min SDK 26)

### Permissions Required

- PACKAGE_USAGE_STATS (special — user grants in Settings)
- BIND_NOTIFICATION_LISTENER_SERVICE (special — user grants in Settings)
- FOREGROUND_SERVICE (background tracking)
- ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION (GPS)
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS (prevent service kill)
- RECEIVE_BOOT_COMPLETED (restart on boot)
- POST_NOTIFICATIONS (Android 12+)
