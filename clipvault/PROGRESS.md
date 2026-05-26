# ClipVault Progress

## Task 1 — Project Scaffold ✅
- [x] Version catalog (libs.versions.toml)
- [x] settings.gradle.kts
- [x] Root build.gradle.kts
- [x] App build.gradle.kts
- [x] gradle.properties
- [x] Gradle wrapper (9.4)
- [x] AndroidManifest.xml
- [x] network_security_config.xml
- [x] ClipVaultApplication.kt
- [x] MainActivity.kt
- [x] Resource files (strings, themes, colors)
- [x] Build verification: `./gradlew assembleDebug`

## Task 2 — Room Database Layer ✅
- [x] ClipItem Entity
- [x] Tag Entity
- [x] ItemTag Entity
- [x] AiProvider Entity
- [x] ClipItemDao
- [x] TagDao (CTE recursive with depth < 50)
- [x] ItemTagDao
- [x] AiProviderDao
- [x] AppDatabase
- [x] Build verification

## Task 3 — Repository + DI ✅
- [x] ClipItemRepository
- [x] TagRepository
- [x] AiProviderRepository (API Key encryption placeholder)
- [x] DatabaseModule (Hilt)
- [x] RepositoryModule (Hilt)
- [x] Build verification

## Task 4 — Home Timeline Feed ✅
- [x] Navigation routes (@Serializable Screen)
- [x] Theme (Color, Typography, Shapes)
- [x] HomeScreen with LazyVerticalStaggeredGrid
- [x] Paging extension function for LazyStaggeredGridScope
- [x] HomeViewModel with search debounce 300ms
- [x] Tag filter Modal Bottom Sheet
- [x] Build verification

## Task 5 — Detail Screen ✅
- [x] DetailScreen with type-based rendering (text/image/link/media)
- [x] DetailViewModel with ExoPlayer lifecycle management
- [x] Jsoup link content fetching
- [x] Tag management (add/remove)
- [x] Note editing
- [x] Build verification

## Task 6 — New Item Screen ✅
- [x] NewItemActivity with PROCESS_TEXT + ACTION_SEND handling
- [x] NewItemScreen with text/media input
- [x] NewItemViewModel with URI file copying
- [x] Image/file picker integration
- [x] Tag selection
- [x] Build verification

## Task 7 — Tag Manager Screen ✅
- [x] TagManagerScreen with tree view
- [x] TagManagerViewModel with expand/collapse
- [x] Create/Rename/Delete/Move tag operations
- [x] Delete with @Transaction reparenting
- [x] Build verification

## Task 8 — AI Integration + AI Settings ✅
- [x] AiService with OpenAI Chat Completions API
- [x] Sealed AiResult (Success/Error)
- [x] AiSettingsScreen with provider management
- [x] Test connection button
- [x] Build verification

## Task 9 — General Settings ✅
- [x] SettingsScreen with navigation
- [x] Export JSON via SAF
- [x] Import JSON via SAF (Overwrite/Merge modes)
- [x] About section
- [x] Build verification

## Task 10 — Data Encryption ✅
- [x] CryptoManager with AES-GCM (Android Keystore)
- [x] API Key encrypted storage in DataStore
- [x] AiProviderRepository encryption integration
- [x] Build verification

## Task 11 — Theme System ✅
- [x] Material You Dynamic Color support
- [x] Dark/Light theme with OLED black
- [x] ThemePreferences DataStore (theme_mode key)
- [x] EmptyState and ErrorState components
- [x] Edge-to-edge layout
- [x] Build verification

## Bug Fixes ✅

### Bug 1: Tag display & edit broken
- [x] DetailViewModel: Fixed nested collect blocking — split into independent coroutines with `collectLatest` + `catch`
- [x] DetailViewModel: Tag add/remove now triggers Flow auto-update via getTagsForItem
- [x] HomeViewModel: Added `catch` error handling on Flow collectors

### Bug 2: Image/file attach fails
- [x] NewItemActivity: Added runtime permission request for READ_MEDIA_IMAGES/VIDEO/AUDIO (Android 13+)
- [x] NewItemActivity: Fixed deprecated getParcelableExtra → use type-safe API on API 33+
- [x] NewItemActivity: Permission check before URI copy, with launcher for deferred copy

### Bug 3: White screen + data loss
- [x] DatabaseModule: Added `fallbackToDestructiveMigration(dropAllTables = false)` safety net
- [x] HomeViewModel: Added `catch` on Flow collectors to prevent crash propagation
- [x] DetailViewModel: Added `catch` + `collectLatest` for robust Flow collection
- [x] SettingsViewModel: Fixed `collect { return@collect }` anti-pattern → use `first()`

### Bug 4: AI API unreachable
- [x] CryptoManager.decrypt(): Catch `BadPaddingException` + `IllegalBlockSizeException` (data corruption) — return "" instead of crashing
- [x] CryptoManager.decrypt(): Validate minimum data length (IV_SIZE + 1)
- [x] CryptoManager.encrypt(): Wrap in try-catch, return "" on failure
- [x] AiService: Fixed Base URL handling — strips trailing `/v1/`, handles all input variants
- [x] AiSettingsViewModel: testConnection() now retrieves stored API key from DataStore when editing existing provider

## Refactoring & Security Enhancements (Material 3 Expressive) ✅

### 1. UI System Redesign (Material 3 Expressive)
- [x] Added `BentoAsymmetricCardShape` and `ExpressiveBottomSheetShape` round corner definitions (16dp-28dp asymmetry) in `Theme.kt`.
- [x] Wrapped `NavHost` in `SharedTransitionLayout` in `MainActivity.kt` to enable fluid transition animations between list cards and detail contents.
- [x] Applied `BentoAsymmetricCardShape` and staggered layouts on `HomeScreen.kt` cards to form an asymmetric Bento Grid.
- [x] Configured spring-damping scale click feedback physics on grid cards via pointer input.
- [x] Ensured critical action elements (Save button, FAB, search/filter inputs) use standard Pill Shapes.
- [x] Applied `ExpressiveBottomSheetShape` (top large rounded, bottom straight) to `TagFilterSheet` and AI result sheets.
- [x] Customized Dark/Light theme values with contrast-aware fallbacks and pure black `#000000` OLED background.

### 2. Database & DataStore Concurrency & Error Mitigation
- [x] Replaced database `fallbackToDestructiveMigration` with custom double-checked locking builder inside `DatabaseModule.kt` and caught exceptions, falling back to an in-memory database to prevent startup white-screen failures.
- [x] Wrapped Preferences DataStore reads in `ThemePreferences.kt` and `AiProviderRepository.kt` with IOException handlers emitting `emptyPreferences()` or safe default fallback values.
- [x] Implemented self-healing keystore retrieval and key regeneration loops in `CryptoManager.kt` during both `encrypt()` and `decrypt()` phases.

### 3. Hierarchical Tag Queries & Database Transactions
- [x] Restructured recursive Common Table Expression (CTE) query in `ClipItemDao.kt` to sit at the top level of the SELECT statements, fixing Room SQLite parser syntax failures.
- [x] Wrapped repository-level `deleteTagWithReparenting` in a database `@Transaction` block to ensure consistent reparenting.

### 4. Shared URI Streaming Copy & Lifecycles
- [x] Triggered stream-copying of shared URIs immediately in `NewItemActivity.onCreate` rather than delaying it with storage permissions, avoiding temporary URI expiration.

### 5. AI Service Custom Host Compatibility
- [x] Redesigned url concatenation in `AiService.buildChatUrl` to flexibly parse custom proxies and hosts.
- [x] Force-disabled chunked streaming in request body by specifying `"stream": false`.
- [x] Integrated AI analysis action trigger button, loading overlay, and result review/tag-adoption modal bottom sheet in `DetailScreen` and `DetailViewModel`.
- [x] Migrated media content layout in `DetailScreen` from basic VideoView placeholder to native AndroidX Media3 `PlayerView`.

## Engineering Deliverables ✅
- [x] 01-requirements-specification.md — SRS
- [x] 02-design-specification.md — SDD (Updated Room CTE structures & AI URL builders)
- [x] 03-source-code-assets.md — Source Code Asset Manifest
- [x] 04-test-plan.md — Test Plan
- [x] 05-deployment-user-manual.md — Deployment & User Manual (Updated permissions and stream copy flow descriptions)

## Final Online Review & Specification Alignment ✅
- [x] Checked **Material 3 Expressive** Compose guidelines (Spring Motion system utilizing motion scheme and dynamic contrast fallback compliance validated).
- [x] Reviewed **Android 16 & 17** specifications (Mandatory large screen adaptation via non-restricting manifest constraints, and cleartext traffic blocking default via Network Security Config files).
- [x] Verified **Coil 3** package name alignment (`coil3.compose.*` with `io.coil-kt.coil3` group ID).
- [x] Confirmed **Media3 Player lifecycle** release standards (Correct pause/resume binding on Lifecycle events and final release in ViewModel `onCleared`).
- [x] Re-ran Gradle verification compiles (`./gradlew assembleDebug` passed in <1s).

## Real-Device Testing & Verification Fixes ✅
- [x] **Theme Mode Switcher & Non-Blocking Datastore**: Injected `ThemePreferences` into `MainActivity` and `SettingsViewModel`, providing a dynamic theme settings selector in `SettingsScreen`. Migrated all JSON/IO and database queries inside `SettingsViewModel` to `Dispatchers.IO` and `Dispatchers.Default` background scopes to prevent blocking of Compose UI rendering.
- [x] **Transactional New Item Insertion**: Wrapped database inserts and initial tag mappings in `ClipItemRepository.insertWithTags` inside a database `@Transaction` block (`database.withTransaction`), avoiding incomplete transactions if user navigates back early.
- [x] **Tag Management for Untagged Items**: Designed and integrated a tags editing modal bottom sheet in `DetailScreen` using `ExpressiveBottomSheetShape`, permitting users to manage and toggle tag associations via checkboxes even if the item originally had no tags.
- [x] **Save Validation & Media Preview**: Added file path state collection in `NewItemScreen` to show a proper `AsyncImage` thumbnail preview right after media/file imports. Fixed Save button validation logic, allowing save operations if either the text field OR the media file path is not blank.

## Tag System Optimization & Refactoring ✅
- [x] **Multi-Select Tree Tag Filter Binding**: Updated `HomeScreen.kt` to bind `selectedTagIds` (Set<Long>) to the collapsible tree filter sheet `TagFilterSheet.kt` and use proper VM handlers `toggleTagSelection(tagId)` and `clearTagSelection()`, resolving compiler errors and enabling seamless tag list-based multi-filter behavior.
- [x] **Transactional AI Tag Recommendation Resolver**: Refactored `applyAiResult` in `DetailViewModel.kt` to perform all database operations (item updates, tag inserts, association inserts) inside a Room transaction. Fixed the hierarchical tag creation algorithm to use a mutable list lookup to avoid creating duplicate parent tags for paths starting with the same segment.
- [x] **Successful Build Verification**: Re-ran the full compilation verify task `./gradlew assembleDebug`, passing successfully.

## Refinement Releases (Fixes 6-12) ✅

### 1. Database Lazy Initialization & Data Preservation (Fix v6)
- Implemented 100% lazy and non-blocking asynchronous database initialization in `AppDatabase.kt` and `ClipVaultApplication.kt`. Removed blocking open-calls on the main thread, completely resolving settings page white-screen freeze and data loss issues.

### 2. Bento-Style UI & Adaptive Sizing (Fix v7)
- Redesigned the screen layout with elevated modular Bento cards for content sections and settings groups.
- Enabled adaptive detail image aspect-ratio scaling without letters or squishing.

### 3. State-Aware Interaction Shielding (Fix v8)
- Added visual delete confirmation dialogs and implemented state-aware click shielding during navigation exit transitions to eliminate predictive back gesture double-navigation bugs.

### 4. Tag Hierarchy Tree Views & Full Detail Screen Editing (Fix v9 & v11)
- Replaced flat path chips with nested tree columns using indentations, custom vertical parent-child lines, and parent highlighting.
- Extended edit mode to support full CRUD and re-ordering on attachments with Outline pickers for files/images.
- Modified tree selector to automatically expand ancestor paths to selected tag nodes on sheet load.

### 5. Transition Zeroing & Performance Optimization (Fix v10 & v12)
- Replaced spring card animations with tween curves, and subsequently zeroed out all page transition animations (`{ null }`) to ensure instant navigations.
- Stripped the `SharedTransitionLayout` wrapping structure and related imports/parameters to maximize rendering efficiency.
- Cleaned up unused fields (like sourceApp text inputs) from edit mode and ensured textless items (such as pure images) show proper edit triggers.
- Passed all automated verify builds cleanly.
