# ClipVault Adaptive Refactoring Plan

## Architecture Audit Summary

### Current State
- **45 source files** across UI, Data, DI layers
- **All screens are single-column, phone-portrait-only** - zero adaptive layout support
- **No WindowSizeClass** usage anywhere in the codebase
- **No Material 3 Adaptive library** dependencies
- **Database version 3** with 5 entities (ClipItem, Tag, ItemTag, AiProvider, ContentAttachment)
- **100+ hardcoded dp values** across all screens
- **Navigation**: Single-Activity + NavHost, type-safe routes

### Key Adaptive Issues Per Screen

| Screen | Issues | Priority |
|--------|--------|----------|
| HomeScreen | `StaggeredGridCells.Fixed(2)` hardcoded, fillMaxWidth, no responsive columns | HIGH |
| DetailScreen | Single-column scroll, no side-by-side on tablet | HIGH |
| NewItemScreen | Vertical form, no floating panel on large screens | MEDIUM |
| TagManagerScreen | Single LazyColumn, no dual-pane tree+detail | MEDIUM |
| SettingsScreen | Vertical scroll, no responsive card grid | LOW |
| AiSettingsScreen | Single LazyColumn | LOW |

## Technical Strategy

### Dependencies to Add
```
implementation("androidx.compose.material3.adaptive:adaptive:1.1.0")
implementation("androidx.compose.material3.adaptive:adaptive-layout:1.1.0")
implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.1.0")
implementation("androidx.window:window:1.3.0")
```

### Layout Strategy
- **HomeScreen**: Adaptive column count via `WindowSizeClass` (2/3/4+ columns)
- **DetailScreen**: `NavigableListDetailPaneScaffold` on Medium+ screens
- **TagManagerScreen**: Dual-pane (tree left, detail right) on Expanded
- **All screens**: `AdaptiveTokens` for responsive spacing/dimensions

### Data Model Extensions
- New: `BehaviorLog` entity (event tracking)
- New: `UserInsight` entity (aggregated behavior patterns)
- Room Migration: version 3 → 4

## Implementation Phases

### Phase 2: Data Model Extension
- Add BehaviorLog + UserInsight entities
- Room Migration 3→4 with rollback safety
- New DAOs: BehaviorDao, InsightDao
- Update DatabaseModule

### Phase 3: Adaptive Infrastructure
- Add M3 Adaptive + WindowManager deps
- Create `DeviceFormFactor` enum
- Create `AdaptiveTokens` responsive dimension system
- Create `rememberDeviceFormFactor()` composable

### Phase 4: Adaptive Home
- Responsive column count (2/3/4/5)
- Responsive spacing via AdaptiveTokens
- Phone portrait must look identical to current

### Phase 5: Review

### Phase 6: Adaptive Detail + NewItem
- ListDetailPaneScaffold for detail on tablet
- Floating panel for NewItem on large screens

### Phase 7: Adaptive TagManager + Non-touch
- Dual-pane tag manager on large screens
- Keyboard shortcuts, focus indicators

### Phase 8: Behavior Tracking
- BehaviorTracker service
- InsightEngine aggregation
- Adaptive rules engine

### Phase 9: Lifecycle + Memory Decay
- LifecycleStage enum + gating
- Memory decay for tags, search, content
- Session memory restore

### Phase 10: Final Review
### Phase 11: Documentation
