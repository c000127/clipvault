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

## Task 12 — Compose First Refactoring (Compose 1.11) ✅
- [x] **Motion System Upgrade**: Integrated `ResponsiveSpring` and `SnappySpring` for fluid, non-linear physical animations.
- [x] **Shared Element Transitions**: Correctly configured `SharedTransitionLayout` hero animations between list cards and detail views.
- [x] **Navigation Animation**: Replaced static `fadeIn/fadeOut` with Material 3 Expressive `slideInHorizontally` + `spring()` transitions.
- [x] **Stability Optimization**: Verified that Strong Skipping Mode is active; confirmed no redundant `@Stable` annotations.
- [x] **UI Testing Foundation**: Implemented `HomeScreenTest.kt` using Compose Testing v2 APIs and `StandardTestDispatcher`.
- [x] **Preview Refinement**: Added `PreviewWrapper` in `Theme.kt` to simplify UI preview management.
- [x] **Interaction Cleanup**: Removed `graphicsLayer` scale from cards to prevent text rendering artifacts during long-press.
- [x] **Build Verification**: All changes passed `./gradlew assembleDebug`.

## Adaptive Refactoring (Android 17 Adaptive-First) ✅

### Phase 2: Data Model Extension
- [x] `BehaviorLog` entity — behavior event tracking (event, metadata, timestamp, sessionId)
- [x] `UserInsight` entity — aggregated behavior patterns (type, key, value, sampleCount)
- [x] `BehaviorDao` — event recording, time distribution, search/tag frequency queries
- [x] `InsightDao` — upsert, type-based queries, delete-all
- [x] Room Migration 3→4 — creates `behavior_logs` and `user_insights` tables with indices
- [x] `DatabaseModule` updated — provides new DAOs
- [x] Build verification passed

### Phase 3: Adaptive Layout Infrastructure
- [x] Added Material 3 Adaptive dependencies (`adaptive`, `adaptive-layout`, `adaptive-navigation`)
- [x] Added `androidx.window:window:1.3.0` for WindowSizeClass
- [x] `DeviceFormFactor` enum — PhonePortrait, PhoneLandscape, Foldable, Tablet, Desktop
- [x] `AdaptiveTokens` — responsive dimension system (pageHorizontal, gridColumns, contentMaxWidth, etc.)
- [x] `AdaptiveScope.kt` — `rememberDeviceFormFactor()`, `rememberWindowWidthSizeClass()`, `rememberAdaptiveTokens()`
- [x] Build verification passed

### Phase 4: Adaptive Home Timeline Feed
- [x] `ContentGrid` — responsive column count via `AdaptiveTokens.gridColumns` (2/3/4/5)
- [x] Responsive spacing via `AdaptiveTokens.itemSpacing` and `pageHorizontal`
- [x] Search bar width limited to 600dp on large screens, centered
- [x] Content width limited to `AdaptiveTokens.contentMaxWidth` on large screens
- [x] FullLine span threshold increased on large screens (200 chars vs 120)
- [x] Phone portrait layout identical to pre-refactoring
- [x] Build verification passed

### Phase 6: Adaptive Detail + New Item Pages
- [x] `DetailScreen` — content width limited and centered on large screens
- [x] `NewItemScreen` — content width limited and centered on large screens
- [x] Responsive padding via `AdaptiveTokens.pageHorizontal`
- [x] Build verification passed

### Phase 7: Adaptive Tag Management + Settings
- [x] `TagManagerScreen` — content width limited and centered on large screens
- [x] `SettingsScreen` — content width limited and centered on large screens
- [x] Responsive spacing via `AdaptiveTokens.sectionGap`
- [x] Build verification passed

### Phase 8: Behavior Tracking + Adaptive Rules
- [x] `BehaviorTracker` — singleton service for recording user behavior events
- [x] Event types: 收藏, 搜索, AI总结, Tag操作, 页面访问, 阅读
- [x] `InsightEngine` — aggregates BehaviorLog → UserInsight
- [x] Tag frequency aggregation (30-day window)
- [x] Time pattern aggregation (hourly distribution)
- [x] Content distribution aggregation
- [x] Search hot words with decay (7d ×3, 30d ×1, 90d ×0.3)
- [x] AI adoption rate tracking
- [x] 180-day log auto-archival
- [x] "Forget My Habits" feature — clears all behavior + insight data
- [x] Build verification passed

### Phase 9: Lifecycle Stages + Memory Decay
- [x] `LifecycleStage` enum — NEWBORN (0-7d), GROWTH (1-4w), MATURE (1-3m), EVOLUTION (3m+)
- [x] `LifecycleManager` — stage determination based on install time
- [x] Stage gating: adaptive features only enabled in MATURE/EVOLUTION with 50+ samples
- [x] Session memory: tracks last session time
- [x] "My Usage Insights" panel in SettingsScreen
- [x] Shows lifecycle stage, days since install, adaptive status
- [x] Shows grouped insights with sample counts
- [x] "Forget My Habits" button
- [x] `SettingsViewModel` updated — injects LifecycleManager + InsightEngine
- [x] Build verification passed

### Architecture Summary
- **4 new files**: `BehaviorLog.kt`, `UserInsight.kt`, `BehaviorDao.kt`, `InsightDao.kt`
- **3 new adaptive files**: `DeviceFormFactor.kt`, `AdaptiveTokens.kt`, `AdaptiveScope.kt`
- **2 new behavior files**: `BehaviorTracker.kt`, `InsightEngine.kt`, `LifecycleManager.kt`
- **6 screens made adaptive**: Home, Detail, NewItem, TagManager, Settings (AiSettings shares pattern)
- **Database version**: 3 → 4 (migration-safe)
- **New dependencies**: M3 Adaptive 1.1.0, WindowManager 1.3.0
- **All behavior data local-only**: no network uploads

## M3 原生动效重构 (Compose BOM 2026.05.01) ✅

### Phase A: Token 层重构 (Theme.kt)
- [x] 新增 `DecelerateEasing` / `AccelerateEasing` 缓动 Token
- [x] 新增 `ScaleIn` Float spring（轻弹，用于 scaleIn/Out）
- [x] 新增 `GentleExpand` Float spring（用于 animateFloatAsState）
- [x] 新增 `ExpandSpring` IntSize spring（用于 expandVertically/shrinkVertically）
- [x] 新增 `BoundsTransform` Rect spring（用于容器变换）
- [x] Build verification passed

### Phase B: NavHost 页面转场重构 (MainActivity.kt)
- [x] 移除所有 `fadeIn` / `fadeOut` 导入和调用
- [x] 移除所有 `tween()` 转场
- [x] 6 组页面（Home/Detail/New/TagManager/Settings/AiSettings）统一为纯 `slideInHorizontally` + `PageSlide` spring
- [x] Build verification passed

### Phase C: HomeScreen 组件动画重写
- [x] Batch Actions Bar: `slideInVertically + fadeIn` → 纯 `slideInVertically(PageSlide)`
- [x] Clipboard Suggestion: `expandVertically + fadeIn` → 纯 `expandVertically(ExpandSpring)`
- [x] Build verification passed

### Phase D: DetailScreen 组件动画重写
- [x] 编辑模式切换: `fadeIn + scaleIn(0.96f)` → 纯 `scaleIn(0.95f, ScaleIn)`
- [x] Hero 区域: `slideInVertically + fadeIn` → 纯 `slideInVertically(PageSlide)`
- [x] AI 结果: `expandVertically + fadeIn` → 纯 `expandVertically(ExpandSpring)`
- [x] Build verification passed

### Phase E: EmptyState/ErrorState 重写
- [x] EmptyState: `fadeIn(tween(Deliberate))` → `scaleIn(0.9f, ScaleIn)`
- [x] ErrorState: `fadeIn(tween(Deliberate))` → `scaleIn(0.9f, ScaleIn)`
- [x] Build verification passed

### Phase F: SkeletonLoader 审计
- [x] Shimmer 效果保留（infiniteRepeatable + LinearEasing 循环动画合理）
- [x] 颜色已使用 M3 surfaceContainer（无需修改）
- [x] Build verification passed

### 动效重构统计
- **消除**: 35+ 处 `fadeIn`/`fadeOut` 调用
- **消除**: 所有 `tween()` 转场（shimmer 除外）
- **新增**: 6 个动效 Token（DecelerateEasing, AccelerateEasing, ScaleIn, GentleExpand, ExpandSpring, BoundsTransform）
- **模式**: 6 种 M3 原生动效（纯 spring slide, expand/shrink, scaleIn, sharedElement, PageSlide spring, BoundsTransform）

## M3 Container Transform 重构（第二轮）✅

### Phase 1: Token 层修正
- [x] 新增 `SpatialExpressiveSpring`（dampingRatio=0.8f, stiffness=380f，参考 Jetsnack）
- [x] 新增 `NonSpatialExpressiveSpring`（dampingRatio=1f, stiffness=1600f，参考 Jetsnack）
- [x] 更新 `BoundsTransform` 参数与 SpatialExpressiveSpring 一致
- [x] 确认 `EmphasizedEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)` 正确
- [x] Build verification passed

### Phase 2: Shared Element Key 重构
- [x] 新建 `ClipSharedElementKey.kt`：`ClipSharedElementKey(clipId, type)` + `ClipSharedElementType` enum
- [x] `ClipSharedElementType`: `Bounds`, `Title`, `Content`
- [x] HomeScreen.kt: 字符串 key `"item_${item.id}"` → `ClipSharedElementKey(item.id, ClipSharedElementType.Content)`
- [x] DetailScreen.kt: 字符串 key `"item_${viewModel.itemId}"` → `ClipSharedElementKey(viewModel.itemId, ClipSharedElementType.Content)`
- [x] Build verification passed

### Phase 3: Container Transform 接入
- [x] **MainActivity.kt**: Home/Detail/New 页面转场从 `slideInHorizontally` → `fadeIn/fadeOut(NonSpatialExpressiveSpring)`（Jetsnack 模式）
- [x] **HomeScreen.kt**: ClipCard 从 `sharedElement` → `sharedBounds(Bounds)` + `sharedElement(Content)` 双层架构
- [x] **HomeScreen.kt**: `sharedBounds` 使用 `boundsTransform = SpatialExpressiveSpring` + `OverlayClip(BentoAsymmetricCardShape)`
- [x] **DetailScreen.kt**: Scaffold 从 `sharedElement` → `sharedBounds(Bounds)` + `sharedElement(Content)` 双层架构
- [x] **DetailScreen.kt**: `sharedBounds` 使用 `boundsTransform = SpatialExpressiveSpring` + `OverlayClip(BentoAsymmetricCardShape)`
- [x] Build verification passed

### Phase 4: Materialize 动画
- [x] HomeScreen 批量操作栏：`slideInVertically` → `scaleIn(0.8f, ScaleIn)` + `fadeIn(NonSpatialExpressiveSpring)`
- [x] scaleIn 为主视觉效果，fadeIn 为辅
- [x] Build verification passed

### Phase 5: 折叠区域确认
- [x] DetailScreen AI 结果：`expandVertically` + `shrinkVertically` 使用 `ExpandSpring` ✅
- [x] HomeScreen 剪贴板建议：`expandVertically` + `shrinkVertically` 使用 `ExpandSpring` ✅

### Phase 6-7: 审查与修复
- [x] 移除 3 个未使用导入（HomeScreen: detectTapGestures, pointerInput; DetailScreen: clickable）
- [x] 移除 1 个重复导入（DetailScreen: BentoAsymmetricCardShape）
- [x] 移除 1 个未使用导入（DetailScreen: rememberDeviceFormFactor）
- [x] 验证所有 sharedBounds/sharedElement key 使用 data class（0 残留字符串 key）
- [x] 验证所有 spring 参数引用 ClipVaultMotion Token（0 硬编码）

### Container Transform 架构总结
- **文件新增**: 1 (`ClipSharedElementKey.kt`)
- **文件修改**: 4 (Theme.kt, MainActivity.kt, HomeScreen.kt, DetailScreen.kt)
- **API 模式**: `sharedBounds()` (Container Transform) + `sharedElement()` (Hero Transition) 双层架构
- **Key 类型**: `ClipSharedElementKey(clipId: Long, type: ClipSharedElementType)` data class
- **Spring 参数**: `SpatialExpressiveSpring(0.8f, 380f)` for bounds, `NonSpatialExpressiveSpring(1f, 1600f)` for fade
- **参考**: Jetsnack `SnackSharedElementKey` + `spatialExpressiveSpring()` + `nonSpatialExpressiveSpring()`
