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

## Engineering Deliverables ✅
- [x] 01-requirements-specification.md — SRS (功能/非功能需求 + 验收指标)
- [x] 02-design-specification.md — SDD (架构/数据库/加密/接口/模块设计)
- [x] 03-source-code-assets.md — 源代码资产清单 (40 文件 + Bug 修复记录)
- [x] 04-test-plan.md — 测试计划 (25 测试用例: 存储/递归/加密/网络)
- [x] 05-deployment-user-manual.md — 部署与用户手册 (编译/权限/导入导出/AI配置)
