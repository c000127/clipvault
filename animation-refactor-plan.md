# ClipVault Animation Refactor Plan

## 当前动画清单

### MainActivity.kt — NavHost 页面转场（6 组 × 4 = 24 处）

| 行号 | 路由 | API | 参数 | 问题 |
|------|------|-----|------|------|
| 82–105 | Home | `slideInHorizontally(it/3)` + `slideOutHorizontally(-it/3)` | `ClipVaultMotion.PageSlide` (spring<IntOffset>) | **无 Container Transform**。Home→Detail 应该用 `sharedBounds()` 容器变换，不是单纯的水平滑动 |
| 133–157 | Detail | `slideInHorizontally(it/2)` + `slideOutHorizontally(-it/3)` | 同上 | Detail 的 enter offset (it/2) 与 Home (it/3) 不一致，触觉脱节 |
| 166–190 | New | `slideInHorizontally(it/2)` + `slideOutHorizontally(-it/3)` | 同上 | **无 Container Transform**。FAB→NewItem 应该用 `sharedBounds()` |
| 201–225 | TagManager | `slideInHorizontally(it/3)` | 同上 | 方向性滑动可接受，但参数过于通用 |
| 234–258 | Settings | `slideInHorizontally(it/3)` | 同上 | 同上 |
| 272–296 | AiSettings | `slideInHorizontally(it/3)` | 同上 | 同上 |

### Theme.kt — ClipVaultMotion Token

| 行号 | Token | 类型 | 状态 |
|------|-------|------|------|
| 48–51 | `Instant/Quick/Standard/Deliberate` | const Int | ✅ 已引用 |
| 54–58 | `DefaultEasing/EmphasizedEasing/DecelerateEasing/AccelerateEasing/LinearEasing` | Easing | ⚠️ `DecelerateEasing`/`AccelerateEasing` 从未被引用 |
| 61–72 | `Snappy/Responsive/Bouncy` | `spring<Float>` | ⚠️ 从未被引用 |
| 74–77 | `ScaleIn` | `spring<Float>` | ✅ 被 EmptyState/ErrorState/DetailScreen 引用 |
| 79–82 | `GentleExpand` | `spring<Float>` | ⚠️ 从未被引用 |
| 85–88 | `PageSlide` | `spring<IntOffset>` | ✅ 被 NavHost + 组件使用 |
| 90–93 | `ExpandSpring` | `spring<IntSize>` | ✅ 被 HomeScreen/DetailScreen 使用 |
| 95–98 | `BoundsTransform` | `spring<Rect>` | ❌ **从未被引用！** `sharedBounds()` 不存在 |

### HomeScreen.kt — 列表/组件动画

| 行号 | 组件 | API | 参数 | 问题 |
|------|------|-----|------|------|
| 133–142 | Batch Actions Bar | `slideInVertically` + `slideOutVertically` | `PageSlide` (spring<IntOffset>) | ✅ 正确 |
| 316–323 | Clipboard Suggestion | `expandVertically` + `shrinkVertically` | `ExpandSpring` (spring<IntSize>) | ✅ 正确 |
| 437–440 | ClipCard sharedElement | `sharedElement("item_${item.id}")` | 字符串 key | ❌ **禁止字符串 key**。官方文档："create a key that is **not a string**" |

### DetailScreen.kt — 详情页动画

| 行号 | 组件 | API | 参数 | 问题 |
|------|------|-----|------|------|
| 240–248 | Edit Toggle AnimatedContent | `scaleIn(0.95f)` / `scaleOut(0.95f)` | `ScaleIn` (spring<Float>) | ✅ 正确 |
| 355–359 | Hero Section | `slideInVertically(40)` | `PageSlide` | ✅ 正确 |
| 439–444 | AI Results | `expandVertically` + `shrinkVertically` | `ExpandSpring` | ✅ 正确 |
| 206–209 | Scaffold sharedElement | `sharedElement("item_${viewModel.itemId}")` | 字符串 key | ❌ **禁止字符串 key** |

### EmptyState.kt / ErrorState.kt

| 文件 | 行号 | API | 参数 | 状态 |
|------|------|-----|------|------|
| EmptyState.kt | 41–44 | `scaleIn(0.9f)` | `ScaleIn` | ✅ 正确 |
| ErrorState.kt | 42–45 | `scaleIn(0.9f)` | `ScaleIn` | ✅ 正确 |

---

## 问题汇总

| # | 严重等级 | 问题 | 涉及文件 |
|---|---------|------|---------|
| 1 | 🔴 CRITICAL | **`sharedBounds()` 零使用** — Container Transform 是 M3 核心模式，完全缺失 | MainActivity, HomeScreen, DetailScreen |
| 2 | 🔴 CRITICAL | **`BoundsTransform` Token 孤儿** — 定义但从未引用，证明 sharedBounds 架构未接入 | Theme.kt |
| 3 | 🔴 CRITICAL | **Home→Detail 不是 Container Transform** — 只是水平滑动，不是 M3 空间变换 | MainActivity:133-157 |
| 4 | 🔴 CRITICAL | **New→NewItem 不是 Container Transform** — FAB→新建页应该用 sharedBounds | MainActivity:166-190 |
| 5 | 🟡 MEDIUM | **sharedElement key 使用字符串** — 违反官方规范："create a key that is **not a string**" | HomeScreen:438, DetailScreen:207 |
| 6 | 🟡 MEDIUM | **NavHost 转场用 slide 模式** — Jetsnack 用 `fadeIn`/`fadeOut` 做页面转场，因为 sharedElement 已经处理空间位移 | MainActivity |
| 7 | 🟡 MEDIUM | **`DecelerateEasing`/`AccelerateEasing` 未使用** | Theme.kt |
| 8 | 🟢 LOW | **4 个 token 未使用** — Snappy, Responsive, Bouncy, GentleExpand | Theme.kt |
| 9 | 🟢 LOW | **`slideInHorizontally` 的 offset 不一致** — Home: it/3, Detail: it/2, New: it/2 | MainActivity |

---

## M3 规范对照（来自 Android 官方文档 + Jetsnack）

### sharedBounds vs sharedElement

| 特征 | `sharedElement()` | `sharedBounds()` |
|------|------------------|-----------------|
| **内容要求** | 两端视觉**相同** | 两端视觉**不同**但共享空间 |
| **过渡可见性** | 仅目标端渲染 | **两端都可见** |
| **有 enter/exit 参数** | ❌ 无 | ✅ 有，类似 AnimatedContent |
| **主要场景** | Hero 过渡（图片缩放） | **Container Transform（M3 规范）** |
| **Text 支持** | ❌ 不推荐字体变化 | ✅ 推荐（支持 italic→bold，颜色变化） |

### Jetsnack Spring 规范

```kotlin
// 空间动画（bounds transform, position changes）
fun <T> spatialExpressiveSpring() = spring<T>(dampingRatio = 0.8f, stiffness = 380f)

// 非空间动画（fadeIn/Out, opacity）
fun <T> nonSpatialExpressiveSpring() = spring<T>(dampingRatio = 1f, stiffness = 1600f)
```

### 官方反模式警告

1. **Modifier 顺序是关键**："Put anything you don't want to be shared **before** sharedElement()"
2. **两端 Modifier 顺序必须一致**："Be consistent with the order of modifiers on matching items"
3. **不要用字符串做 key**："it is a good practice to create a key that is **not a string**, because strings can be error prone"
4. **无 View/Compose 互操作**："No interoperability between Views and Compose is supported"
5. **ContentScale 不做动画**：默认 snap 到目标值
6. **visible=false 时仍在 tree 中** — 应该在过渡完成后移除

---

## 逐场景转场映射表

| 场景 | 当前实现 | 目标实现 | API | 参数 |
|------|---------|---------|-----|------|
| Home→Detail 页面转场 | `slideInHorizontally(it/2)` | Container Transform | `sharedBounds()` | `boundsTransform = SpatialExpressiveSpring` + `OverlayClip(BentoAsymmetricCardShape)` |
| Detail→Home 返回 | `slideOutHorizontally(it/2)` | Container Transform 反向 | `sharedBounds()` | 同上 |
| 卡片内容→详情内容 | `sharedElement("item_${id}")` | `sharedBounds` + `sharedElement` 混合 | `sharedBounds(Bounds)` + `sharedElement(Content)` | `enter = fadeIn(NonSpatialExpressiveSpring)` |
| FAB 出现 | `slideInVertically + spring` | 保持 | `slideInVertically` + `PageSlide` | 无需改动 |
| 折叠区域展开 | `expandVertically(spring)` | 保持 | `expandVertically` + `ExpandSpring` | 无需改动 |
| 空/错误状态 | `scaleIn(0.9f)` | 保持 | `scaleIn` + `ScaleIn` | 无需改动 |
| Tag 管理/设置页 | `slideInHorizontally(it/3)` | 改为 fade | `fadeIn`/`fadeOut` + `NonSpatialExpressiveSpring` | 与 Jetsnack 模式一致 |

---

## sharedBounds 架构设计

### Key 类型设计

```kotlin
// ClipVaultSharedElementKey.kt
data class ClipVaultSharedElementKey(
    val clipId: Long,
    val type: ClipVaultSharedElementType
)

enum class ClipVaultSharedElementType {
    Bounds,      // 容器变换（整个卡片→详情页）
    Content,     // 内容文字过渡
    Title,       // 标题文字过渡
    Image,       // 缩略图过渡
}

object TagManagerSharedElementKey
object SettingsSharedElementKey
```

### 哪些元素做 sharedBounds，哪些做 sharedElement

| 元素 | 类型 | 理由 |
|------|------|------|
| ClipCard 外层容器 | `sharedBounds(Bounds)` | 视觉不同：卡片小 → 详情全屏，Container Transform |
| ClipCard 内容文字 | `sharedElement(Content)` | 视觉相同：文字内容不变，只位置/大小变化 |
| ClipCard 标题 | `sharedBounds(Title)` | 视觉可能不同：列表显示截断，详情显示完整 |
| ClipCard 缩略图 | `sharedBounds(Image)` | 视觉不同：卡片比例 → 详情比例 |

### boundsTransform Spring 参数

```kotlin
// Theme.kt 新增
val SpatialExpressiveSpring = spring<Rect>(
    dampingRatio = 0.8f,   // 接近 LowBouncy 但不是完全无阻尼
    stiffness = 380f        // 接近 StiffnessMediumLow
)

val NonSpatialExpressiveSpring = spring<Float>(
    dampingRatio = 1f,     // NoBouncy
    stiffness = 1600f       // 比 High 还高的刚度
)
```

---

## 需要修改的文件清单

| 文件 | 改动内容 | 依赖 |
|------|---------|------|
| `ui/theme/Theme.kt` | 新增 `SpatialExpressiveSpring`, `NonSpatialExpressiveSpring` tokens | 无 |
| `ui/theme/ClipVaultSharedElementKey.kt` | **新建** Key data class + enum | 无 |
| `MainActivity.kt` | NavHost 转场从 slide 改为 fade + sharedBounds 模式 | Theme.kt, ClipVaultSharedElementKey.kt |
| `ui/home/HomeScreen.kt` | ClipCard 添加 `sharedBounds()` 包裹层，key 改为 data class | ClipVaultSharedElementKey.kt |
| `ui/detail/DetailScreen.kt` | Scaffold 添加 `sharedBounds()` 包裹层，key 改为 data class | ClipVaultSharedElementKey.kt, HomeScreen.kt |
| `PROGRESS.md` | 更新进度文档 | 以上所有 |

---

## 执行顺序

| Phase | 文件 | 改动 | 依赖 | 验证 |
|-------|------|------|------|------|
| **P1** | 新建 `ClipVaultSharedElementKey.kt` | Key data class + enum | 无 | 编译通过 |
| **P2** | `Theme.kt` | 新增 SpatialExpressiveSpring, NonSpatialExpressiveSpring | P1 | 编译通过 |
| **P3** | `MainActivity.kt` | NavHost 转场：Home/Detail/New 改为 fade；TagManager/Settings/AiSettings 保持 slide | P2 | 编译通过 |
| **P4** | `HomeScreen.kt` | ClipCard: sharedElement → sharedBounds + sharedElement 混合；key 改 data class | P3 | 编译通过 |
| **P5** | `DetailScreen.kt` | Scaffold: sharedElement → sharedBounds + sharedElement 混合；key 改 data class | P4 | 编译通过 |
| **P6** | `PROGRESS.md` | 更新进度 | P1–P5 | 文件存在 |
| **P7** | 全量验证 | `./gradlew assembleDebug` + 无残留字符串 key | P1–P6 | BUILD SUCCESSFUL |

### 每个 Phase 的验证标准

```
□ ./gradlew assembleDebug 通过
□ 无残留的字符串 key (grep "item_" 无结果)
□ sharedBounds() 被实际引用
□ BoundsTransform token 被引用
□ 代码注释标注 [动效] 前缀
□ Modifier 顺序正确（sharedBounds 在 size/layout 之后，combinedClickable 之前）
```
