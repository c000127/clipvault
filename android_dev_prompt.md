# Role
你是一个精通 Android 开发的专家级 Agent。你的任务是根据以下定义全自动实现该应用的源码。

## 工作信条（来自 SOUL.md）
- **Be genuinely helpful, not performatively helpful.** 跳过「Great question!」之类的填充话，直接干活。
- **Be resourceful before asking.** 遇到不明确的决策（API 用法、设计细节、库选择等），**优先搜索官方文档和最佳实践**，而不是停下来询问。你的任务是产出可编译的代码，不需要每一步都确认。
- **Have opinions.** 文档没有明确指定的地方（比如具体动画曲线参数、卡片边距、图标风格），做出合理的选择并注明理由。搜索验证你的选择。
- **Earn trust through competence.** 产出质量优先于速度。每段代码应该是有注释的、可维护的、遵循 Kotlin 惯用法的。
- **这是实现，不是规划。** 文档已经定义了完整的架构和数据模型。你的任务是把它们变成代码，不要重新设计。

## 搜索规则
- 使用 Android 官方文档（developer.android.com）、Material 3 指南（m3.material.io）、Android Source 等作为权威来源。
- 对于 Compose API 的具体用法，优先搜索 `@Composable` 函数的源码或官方示例。
- 所有搜索决策（如使用哪条 API 路径、什么参数）应当记录在代码注释中。
- 搜索后如果发现有更好的替代方案（比文档指定的更现代/更稳定），可以采纳，但需要在提交信息中说明原因。

---

# Tech Stack
- **语言：** Kotlin
- **UI：** Jetpack Compose（Material 3）
- **架构：** MVVM + Repository 模式
- **数据库：** Room（本地 SQLite）
- **DI：** Hilt
- **图片加载：** Coil 3.4.0（注意：Coil 3 迁移至 Kotlin Multiplatform，Maven group 改为 `io.coil-kt.coil3`，包名改为 `coil3.compose.*`，需显式添加 `coil-network-okhttp` 依赖）
- **网络（AI 调用）：** OkHttp + Gson（轻量，无额外 SDK 依赖）
- **导航：** Compose Navigation 2.9.8+（Type-Safe Navigation，使用 `@Serializable` Route 类 + KotlinX Serialization，替代字符串路由）
- **构建：** Gradle KTS + Version Catalog（libs.versions.toml）
- **构建环境：** AGP 9.2.1+ / Gradle 9.4+（注意：AGP 9.x 已内置 Kotlin 支持，无需再声明 `kotlin-android` 插件）
- **序列化：** KotlinX Serialization（用于 Type-Safe Navigation 路由序列化）+ Gson（用于 AI API 响应解析）

# Product Overview
一款超级剪贴板/记事本/收藏夹应用。

**核心价值：** 让用户以最低的操作成本，将任意来源的文字、截图、链接、媒体等碎片内容一键收藏到统一的知识库，并通过层级化 Tag 与 AI 能力高效管理和回顾。

**应用名：** ClipVault

# Target Users
- 跨应用收集碎片信息的知识工作者、学生、内容创作者
- 日常频繁复制粘贴、浏览收藏、整理信息的 Android 重度用户

# Core Features

## 1. 全能收藏
支持四种内容类型收藏与统一预览：
- `text` — 纯文本 / 富文本
- `image` — 截图、照片、网络图片（从外部 Share Sheet 传入时，复制到 App 私有目录 `files/clips/` 确保持久可用）
- `link` — URL 链接
- `media` — 音频、视频文件（同理复制到私有目录）

## 2. 快捷收藏入口
- **ACTION_PROCESS_TEXT**（Android 6.0+ 标准接口）：用户在任意应用长按选中文本 → 点击"更多选项"(⋯) → 从弹出菜单选择本应用，所选文本自动传入并打开新建收藏界面。
- **Share Sheet**：用户在系统相册、浏览器、文件管理器等处选中任意内容后通过「分享」入口发送到本应用。
- **应用内置导入**：新建收藏页内提供从相册选取、拍照、粘贴板导入、手动输入等入口。

## 3. 层级化 Tag（树形标签体系）
- 支持无限层级嵌套。
- Tag 节点：`id + name + parentId + createdAt`。
- 交互功能：创建、重命名、删除、调整父节点（改变层级）。
- 首页 Tag 筛选器：树形展开/折叠选择，支持多选 Tag 组合过滤。
- Tag 搜索：按名称搜索 Tag 后展示该 Tag 及其子节点下的所有收藏。

## 4. AI 辅助
- **部署方式：** 云端调用，纯 HTTP 请求，无本地模型。
- **提供商配置：** 用户自定义（支持多配置切换）：
  - Base URL（兼容 OpenAI Chat Completions API 格式）
  - API Key
  - Model Name
  - 系统提示词（System Prompt）可编辑
- **功能：**
  - AI 内容总结：用户手动触发，对收藏内容进行摘要总结（text 发全文；image 支持 vision 模型 base64 看图、不支持则降级为文字描述；link 可手动抓取页面内容后分析），结果追加到收藏的 `note` 字段。
  - Tag 推荐：每次总结时一并分析内容语义，推荐 3~5 个 Tag（支持已有 Tag 或建议新建 Tag），在 UI 上展示供用户一键采纳。
- **调用方式：** 后台异步（Coroutine + 协程），避免阻塞 UI，支持取消。

## 5. 基础编辑与管理
- 收藏列表：时间线流（Timeline Feed），按 `createdAt` 降序排列。
- 每条收藏展示：内容片段摘要 + 缩略图（媒体类型） + 层级 Tag 标签。
- 点击进入详情页：完整内容展示、编辑 `note` 备注、增删 Tag。
- 搜索：全文搜索（内容 + 备注 + Tag 名）。
- 长按多选：进入多选模式后支持 **批量删除**。
- 单条删除与分享。

# UI and Architecture

## 设计系统（Design System）

### 设计语言：Material You 3.0 (Material 3 Expressive)
遵循 Google 最新设计体系，核心理念为 **"form follows feeling"**——情感化、个性化、富有生命力的交互界面。

### 色彩系统（Color System）
- **Dynamic Color（动态取色）**：通过 `DynamicColors.applyToActivitiesIfAvailable()` 自动提取用户壁纸主色调生成整套配色方案（Primary / Secondary / Tertiary / Neutral / Error 色系）。
- **自定义主题回退**：提供一套精心设计的默认色调方案（推荐暖色系或蓝紫色系作为品牌色），在设备不支持动态取色或用户关闭时使用。
- **自适应对比度**：使用 Material 3 的 `contrastLevel` 参数（-1.0 ~ 1.0），默认跟随系统无障碍设置中的对比度偏好。确保所有 semantic color token 组合（如 primary/onPrimary）在 Standard / Medium / High 三个对比度级别下满足 WCAG AA 标准。

### 排版系统（Typography System）
- **Material 3 默认排版缩放**：使用 Compose Material 3 内置的 `Typography`（Display / Headline / Title / Body / Label 五级）。
- **动态字体权重**：标题区域使用粗体大字号与宽敞字距，正文区域采用轻量可读字体。
- **可访问性**：遵循系统字体缩放设置（`fontScale`），保证无障碍阅读。

### 形状与动效（Shape & Motion）
- **形状系统**：
  - 卡片与容器优先采用 Material 3 的 `RoundedCornerShape` 系列（大圆角 16dp~28dp 营造亲和感）。
  - 关键操作控件（FAB、按钮）使用 `FullRoundedShape`（药丸形）。
  - 模态组件（Bottom Sheet、Dialog）使用顶部大圆角+底部直角的非对称设计。
- **物理动效**：
  - 使用 `spring()` 弹簧动画曲线实现自然弹性感：列表滑动惯性、开关切换回弹、卡片按压反馈。
  - 页面转场使用 `SharedTransitionScope`（Compose 的共享元素过渡）实现流畅的列表→详情过渡。
  - 动效遵循 Material Motion 的四大原则：连续性（Continuity）、空间性（Spatial）、层次性（Hierarchy）、反馈性（Feedback）。

### 布局原则（Layout Principles）
- **拇指区优化（Thumb-Zone Layout）**：
  - 主要导航操作、FAB（悬浮操作按钮）、搜索栏置于屏幕 **下半部分**（bottom third），单手操作可达。
  - 标题/大字数置于顶部作为视觉留白区域。
- **Edge-to-Edge 沉浸式**：
  - 内容渲染在系统状态栏和导航栏下方，`WindowCompat.setDecorFitsSystemWindows(window, false)`。
  - 系统导航栏与状态栏背景自适应半透明（配合动态颜色）。
- **模块化卡片（Modular Cards）**：
  - 首页时间线卡片采用非对称 Bento Grid 灵感布局——不同类型收藏（文字/图片/链接）渲染为不同高宽比例的卡片，形成视觉层次感。
  - 卡片交互：点击进入详情 / 长按进入多选模式。

### 组件风格（Component Style）
- **Floating Toolbar**：上下文操作栏使用浮动工具栏形态（非传统 TopAppBar），随内容滚动而优雅显隐。
- **Bottom Sheet**：Tag 筛选器、AI 结果展示等使用 Modal Bottom Sheet 呈现，保留空间上下文。
- **Glassmorphism 点缀**：在 AI 分析结果展示、空状态页面等场景使用微妙的毛玻璃效果（背景模糊 + 噪声纹理叠加），传递现代感。
- **跨组件动效**：Pull-to-refresh 使用弹簧阻尼效果，Swipe-to-dismiss（滑动删除）提供手势预览反馈。

### 深色模式（Dark Mode）
- **Dark-First 设计**：深色模式不是「浅色反转」，而是独立的色彩 token 体系，利用 Material 3 的 `darkColorScheme()` 单独定义。
- OLED 优化：纯黑（#000000）在深色背景中使用时减少发光区域，节省电量。
- 用户可手动切换深色/浅色/跟随系统。

### 设计 Tokens 映射
```kotlin
// Compose Material 3 原生支持以下 tokens
ColorScheme(
    primary = /* 动态颜色 */,
    onPrimary = /* 动态颜色 */,
    surface = /* 动态颜色 */,
    surfaceVariant = /* 动态颜色 */,
    background = /* 动态颜色 */,
    // ... 其余由 Material 3 自动推导
)

Typography(
    displayLarge = /* 系统默认 */,
    headlineMedium = /* 系统默认 */,
    titleLarge = /* 系统默认 */,
    bodyLarge = /* 系统默认 */,
    bodyMedium = /* 系统默认 */,
    labelSmall = /* 系统默认 */,
    // ... 其余由 Material 3 Typography 默认值填充
)

Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Compose BOM 2026.05.01 映射版本：
// - Compose UI/Foundation/Animation: 1.11.2
// - Material 3: 1.4.0（注意：Material 3 1.4.0 不再隐式包含 material-icons-core，需显式添加）
// - Compose Navigation: 2.9.8
```

---

## 页面结构

| 页面 | 说明 | 路由（Type-Safe） |
|---|---|---|
| **首页/收藏列表** | Timeline Feed，时间倒序，支持搜索 + Tag 筛选 | `Screen.Home` |
| **收藏详情** | 完整内容展示，编辑备注，Tag 增删，AI 总结触发 | `Screen.Detail(id: Long)` |
| **新建收藏** | 粘贴/手动输入/导入，选择 Tag，保存 | `Screen.New(text: String? = null)` |
| **PROCESS_TEXT 快捷入口** | ACTION_PROCESS_TEXT 接收文本后打开此页面+预填内容 | 同 `Screen.New`，通过外部 Intent 启动 |
| **Tag 管理** | 树形展示所有 Tag，调整层级、编辑、删除 | `Screen.TagManager` |
| **AI 设置** | 多提供商配置管理（Base URL / API Key / Model / System Prompt） | `Screen.AiSettings` |
| **通用设置** | 关设置、数据导出/导入（JSON）、关于 | `Screen.Settings` |

## 导航图（Type-Safe Routes）
```kotlin
// 使用 @Serializable 定义所有路由，编译期类型安全
@Serializable sealed interface Screen {
    @Serializable data object Home : Screen
    @Serializable data class Detail(val id: Long) : Screen
    @Serializable data class New(val text: String? = null) : Screen
    @Serializable data object TagManager : Screen
    @Serializable data object AiSettings : Screen
    @Serializable data object Settings : Screen
}
```

**导航流程：**
```
Home ──点击收藏──→ Screen.Detail(id)
  ├───点击"+" ───→ Screen.New()
  ├───Tag 筛选 ──→ (Modal Bottom Sheet, 非路由)
  └───Tag 管理 ──→ Screen.TagManager

Screen.New ←── PROCESS_TEXT / ACTION_SEND (外部 Intent 传入参数)

Screen.Settings ──→ Screen.AiSettings
```

## 数据模型（Room Entity）

```kotlin
@Entity(tableName = "items")
data class ClipItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,           // "text" | "image" | "link" | "media"
    val content: String,        // 文本内容 / 本地文件路径 / URL / 图片路径
    val note: String = "",      // 用户备注（AI 总结也追加至此）
    val thumbnailPath: String = "",  // 缩略图本地路径
    val fetchedContent: String = "", // Jsoup 抓取的页面纯文本，link 类型专用
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceApp: String = ""  // 来源应用包名（可选记录）
)

@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.NO_ACTION  // 应用层控制删除逻辑，见 Task 7
        )
    ],
    indices = [Index("parentId")]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,  // null = 根节点
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "item_tags",
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ClipItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("tagId")]
)
data class ItemTag(
    val itemId: Long,
    val tagId: Long
)

@Entity(tableName = "ai_providers")
data class AiProvider(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                       // 用户自定义名称，如 "My GPT"
    val baseUrl: String,                    // 如 https://api.openai.com/v1（末尾不带 /chat/completions）
    val apiKey: String = "",             // Room 中不存实际密钥，存储空字符串或占位符。实际 API Key 存储在 DataStore 中，key 格式 "api_key_{providerId}"
    val modelName: String,                  // 如 gpt-4o-mini（传递 exact ID）
    val supportsVision: Boolean = false,    // 模型是否支持图片理解（对应 OpenClaw 的 input:["text","image"]）
    val maxTokens: Int = 4096,              // 最大输出 token 数
    val temperature: Float = 0.7f,           // 生成温度
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val isActive: Boolean = false            // 当前激活的配置
)
```

## 架构分层
```
UI Layer (Compose Screens / ViewModels)
    ↕
Domain Layer (Use Cases — 可选，按需)
    ↕
Data Layer (Repository → Room DAO + 外部服务)
```

## 关键流程说明

### ACTION_PROCESS_TEXT 完整流程
1. 用户在任意应用选中文本 → 点击"⋯" → 选择本应用。
2. 系统通过 `<intent-filter>` 匹配到 `PROCESS_TEXT` 声明，启动 `NewItemActivity`（或作为 Compose 的入口）。
3. Activity 从 Intent 中提取 `EXTRA_PROCESS_TEXT`（所选文本）和 `EXTRA_PROCESS_TEXT_READONLY`。
4. 文本预填入内容，打开新建收藏页面，用户确认后添加 Tag 并保存。

### AI 调用流程
1. 用户在收藏详情页点击「AI 分析」。
2. ViewModel 发起协程，在 `Dispatchers.IO` 上向用户配置的 AI 提供商发送请求。
3. 请求体构建兼容 OpenAI Chat Completions API 格式（包含系统提示词 + 收藏内容）。
4. 响应解析出总结文本和推荐 Tag。
5. 总结追加到 `note` 字段（待用户确认后写库），推荐 Tag 在 UI 上展示供一键采纳。

### Tag 层级搜索流程
1. 用户选择某个 Tag（如"工作"），搜索时返回该 Tag 及其所有子节点（如"工作/项目A"）下的收藏。
2. 查询通过递归 SQL（或 Room 的 `@RawQuery`）或应用层递归实现。

# Android Platform Requirements

| 项目 | 值 |
|---|---|
| **Min SDK** | 33 (Android 13) |
| **Target SDK** | 37 (Android 17) |
| **Compile SDK** | 37 |
| **语言** | Kotlin 100% |
| **构建系统** | Gradle KTS + Version Catalog |
| **最低 Gradle** | 9.4+ |
| **最低 AGP** | 9.2.1+（注意：AGP 9.x 内置 Kotlin 支持，无需声明 `kotlin-android` 插件） |

## Android 17 (API 37) 兼容性注意事项
- **ACTION_PROCESS_TEXT**：Android 17 将文本选择工具栏从 App 进程迁移至系统进程，`ACTION_PROCESS_TEXT` 功能不变。本项目使用标准 Compose TextField / Android TextView，兼容性无问题。
- **分享入口（ACTION_SEND）**：无重大行为变更。
- **权限模型**：Min SDK 33 以上使用 Granular Media Permissions，无需 `WRITE_EXTERNAL_STORAGE`。
- **明文流量限制**：API 37 默认阻止 HTTP 明文流量（`usesCleartextTraffic` 已废弃）。AI 设置页中 Base URL 应校验是否为 HTTPS；如需支持本地 HTTP 部署（如 localhost），需在 `network_security_config.xml` 中显式允许 `127.0.0.1` / `10.0.0.0/8` 等内网地址。
- **大屏强制适配**：在 sw≥600dp 设备上系统将忽略方向锁定和宽高比约束。所有页面布局必须在平板竖屏/横屏下正常显示，避免硬编码宽度。

## 所需权限

```xml
<!-- 读/写外部存储（Android 13+ 使用 Granular Media Permissions） -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

<!-- 相机（拍照收藏） -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- 网络（AI API 调用） -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Intent Filter 声明

```xml
<!-- ACTION_PROCESS_TEXT + ACTION_SEND 合并为单个 Activity，包含多个 intent-filter -->
<activity
    android:name=".ui.newitem.NewItemActivity"
    android:exported="true"
    android:theme="@style/Theme.ClipVault.Transparent">
    <!-- ACTION_PROCESS_TEXT 入口：文本选择菜单快捷收藏 -->
    <intent-filter>
        <action android:name="android.intent.action.PROCESS_TEXT" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
    <!-- ACTION_SEND 入口：系统分享菜单 -->
    <intent-filter>
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
        <data android:mimeType="image/*" />
        <data android:mimeType="audio/*" />
        <data android:mimeType="video/*" />
    </intent-filter>
</activity>
```

# Implementation Tasks

按实现顺序排列，每个任务产出可编译的增量代码。

## Task 1 — 项目脚手架

按以下步骤创建完整的 Android 项目结构和所有 Gradle 配置文件。所有文件创建完后，`./gradlew assembleDebug` 应一次通过（仅依赖下载耗时）。

### 1. 创建文件树

```
clipvault/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/clipvault/app/
│           │   ├── ClipVaultApplication.kt
│           │   ├── MainActivity.kt
│           │   ├── data/
│           │   │   ├── local/
│           │   │   └── repository/
│           │   ├── di/
│           │   ├── domain/
│           │   └── ui/
│           │       ├── navigation/
│           │       ├── theme/
│           │       ├── home/
│           │       ├── detail/
│           │       ├── newitem/
│           │       ├── tagmanager/
│           │       ├── settings/
│           │       └── aisettings/
│           └── res/
│               ├── values/
│               │   ├── strings.xml
│               │   ├── themes.xml
│               │   └── colors.xml
│               └── xml/
│                   └── network_security_config.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── libs.versions.toml
├── gradlew
├── gradlew.bat
└── PROGRESS.md
```

### 2. 创建 `gradle/libs.versions.toml`

包名：`com.clipvault.app`。AGP 9.2.1 内置 Kotlin 2.3.21，以下所有 Kotlin 插件版本必须与之一致。

```toml
[versions]
agp = "9.2.1"
kotlin = "2.3.21"
ksp = "2.3.8"
composeBom = "2026.05.01"
navigationCompose = "2.9.8"
room = "2.8.4"
# ⚠️ Room 2.8.4 是纯 Android 维护版。Room 3.0 是 KMP 重构版（命名空间 androidx.room3），不要升级。
hilt = "2.59.2"
hiltNavigationCompose = "1.4.0-beta01"
datastorePreferences = "1.2.1"
pagingRuntime = "3.3.6"
pagingCompose = "3.3.6"
media3Bom = "1.10.1"
okhttpBom = "5.3.2"
gson = "2.13.1"
kotlinxSerializationJson = "1.11.0"
coil = "3.4.0"
jsoup = "1.22.2"
desugarJdkLibsNio = "2.1.4"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons-core = { group = "androidx.compose.material", name = "material-icons-core" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }
activity-compose = { group = "androidx.activity", name = "activity-compose" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-paging = { group = "androidx.room", name = "room-paging", version.ref = "room" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-android-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
paging-runtime = { group = "androidx.paging", name = "paging-runtime-ktx", version.ref = "pagingRuntime" }
paging-compose = { group = "androidx.paging", name = "paging-compose", version.ref = "pagingCompose" }
media3-bom = { group = "androidx.media3", name = "media3-bom", version.ref = "media3Bom" }
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer" }
media3-ui-compose = { group = "androidx.media3", name = "media3-ui-compose" }
media3-session = { group = "androidx.media3", name = "media3-session" }
okhttp-bom = { group = "com.squareup.okhttp3", name = "okhttp-bom", version.ref = "okhttpBom" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor" }
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
jsoup = { group = "org.jsoup", name = "jsoup", version.ref = "jsoup" }
desugar-jdk-libs-nio = { group = "com.android.tools", name = "desugar_jdk_libs_nio", version.ref = "desugarJdkLibsNio" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
# ⚠️ 不要声明 kotlin-android 插件，AGP 9.x 已内置 Kotlin 支持
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### 3. 创建 `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ClipVault"
include(":app")
```

### 4. 创建根目录 `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.compose.compiler) apply false
}
```

### 5. 创建 `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    // ⚠️ 不要声明 kotlin("android")，AGP 9.x 内置 Kotlin 2.3.21
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.clipvault.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.clipvault.app"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.datastore.preferences)
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    val media3Bom = platform(libs.media3.bom)
    implementation(media3Bom)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui.compose)
    implementation(libs.media3.session)

    val okhttpBom = platform(libs.okhttp.bom)
    implementation(okhttpBom)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.jsoup)
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)
}
```

### 6. 创建 `gradle.properties`

```properties
android.useAndroidX=true
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.configuration-cache=true
android.builder.sdkDownload=true
android.nonTransitiveRClass=true
```

### 7. 生成 Gradle Wrapper

```bash
# 进入 clipvault/ 目录，系统需安装 Gradle 9.4+
gradle wrapper --gradle-version=9.4
```

如 VPS 未安装 Gradle：
```bash
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh
sdk install gradle 9.4
# 然后 cd clipvault/ && gradle wrapper --gradle-version=9.4
```

生成 `gradle/wrapper/gradle-wrapper.properties`：
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

生成后 `gradlew` / `gradlew.bat` / `gradle-wrapper.jar` 即就位，后续用 `./gradlew` 代替 `gradle`。

### 8. 创建 `app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">127.0.0.1</domain>
        <domain includeSubdomains="true">10.0.0.0</domain>
        <domain includeSubdomains="true">192.168.0.0</domain>
    </domain-config>
</network-security-config>
```

### 9. 创建 `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".ClipVaultApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:networkSecurityConfig="@xml/network_security_config"
        android:supportsRtl="true"
        android:theme="@style/Theme.ClipVault">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.ClipVault">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".ui.newitem.NewItemActivity"
            android:exported="true"
            android:theme="@style/Theme.ClipVault.Transparent">
            <intent-filter>
                <action android:name="android.intent.action.PROCESS_TEXT" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
                <data android:mimeType="image/*" />
                <data android:mimeType="audio/*" />
                <data android:mimeType="video/*" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

### 10. 创建包结构 + Application 类

在每个目录下创建 `.gitkeep` 文件以确保空目录被版本控制追踪。

创建 `ClipVaultApplication.kt`：
```kotlin
package com.clipvault.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClipVaultApplication : Application()
```

创建 `MainActivity.kt`（空 Compose 宿主，后续 Task 填充导航）：
```kotlin
package com.clipvault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 导航入口将在后续 Task 中注入
        }
    }
}
```

创建 `res/values/strings.xml`、`res/values/themes.xml`、`res/values/colors.xml`（最小占位文件）。

### 11. 验证编译

```bash
cd clipvault/
./gradlew assembleDebug
```

首次编译会下载所有依赖，耗时约 5-15 分钟。编译成功后即完成 Task 1。

### 版本依赖索引（供参考）

| 组件 | 版本 |
|---|---|
| AGP | 9.2.1 |
| Kotlin（内置） | 2.3.21 |
| KSP | 2.3.8 |
| Gradle | 9.4 |
| Compose BOM | 2026.05.01 |
| Material 3 | 1.4.0 |
| Navigation Compose | 2.9.8 |
| Room | 2.8.4 |
| Hilt | 2.59.2 |
| DataStore Preferences | 1.2.1 |
| Paging | 3.3.6 |
| Media3 BOM | 1.10.1 |
| OkHttp BOM | 5.3.2 |
| Coil | 3.4.0 |
| KotlinX Serialization | 1.11.0 |
| Jsoup | 1.22.2 |
| Desugar JDK Libs NIO | 2.1.4 |
| Gson | 2.13.1 |
| Compose Compiler | 2.3.21 |

## Task 2 — Room 数据库层
- [ ] 创建 Entity：`ClipItem`、`Tag`、`ItemTag`、`AiProvider`（按上述数据模型）。
- [ ] 创建 DAO：
  - `ClipItemDao`：CRUD + 全文搜索 + 按 Tag 过滤（含子节点递归）。
  - `TagDao`：CRUD + 查找子节点 + 调整 parentId。【agent 自行搜索】Tag 递归查询优先使用 SQLite CTE（Common Table Expression）实现，效率最高；如果 Room 对 CTE 支持有限，退而采用应用层递归。
    - **CTE 递归查询必须添加深度计数器，上限 50 层**，防止循环引用导致死循环：
      ```sql
      WITH RECURSIVE tag_tree(id, name, parentId, depth) AS (
        SELECT id, name, parentId, 0 FROM tags WHERE id = :rootTagId
        UNION ALL
        SELECT t.id, t.name, t.parentId, tt.depth + 1
        FROM tags t INNER JOIN tag_tree tt ON t.parentId = tt.id
        WHERE tt.depth < 50
      )
      SELECT * FROM tag_tree
      ```
    - **环形引用防护**：在 `updateParentId()` 写入前校验新 parentId 不在当前节点的子树中。
  - `ItemTagDao`：关联增删 + 按 itemId 查 Tag + 按 tagId 查 Item。
  - `AiProviderDao`：CRUD + 激活切换。
- [ ] 创建 `AppDatabase`（@Database，版本 1，包含所有 Entity + TypeConverter）。
- [ ] 全文搜索实现：先使用 `LIKE '%' || :query || '%'` 实现简易搜索（MVP 阶段可接受），在数据量增长后升级为 Room FTS4。搜索范围：ClipItem.content + ClipItem.note + 关联的 Tag.name。结果按 createdAt 降序排列。

## Task 3 — Repository + DI
- [ ] 创建 `ClipItemRepository`（包裹 DAO 操作，增删改查 + 搜索 + Tag 关联）。
- [ ] 创建 `TagRepository`（包裹 Tag DAO + 层级递归查询）。
- [ ] 创建 `AiProviderRepository`（API Key 使用 DataStore + Android Keystore AES-GCM 加密存储，见 Task 10）。
- [ ] 创建 Hilt Module：`DatabaseModule`（提供数据库 + 所有 DAO）、`RepositoryModule`（绑定接口与实现）。注意：AGP 9.x 内置 Kotlin 支持，项目级 `build.gradle.kts` 中不应声明 `kotlin("android")` 插件。
- [ ] 可选：如果 TAG 递归查询较复杂，引入 Use Case 层（`GetItemsByTagHierarchyUseCase`）。

## Task 4 — 首页 Timeline Feed
- [ ] `HomeScreen.kt`：使用 **LazyVerticalStaggeredGrid**（`columns = StaggeredGridCells.Fixed(2)`）实现非对称瀑布流布局。不同类型卡片高度自适应：
  - text：紧凑高度（摘要前 100 字 + "…" + Tag chips）。
  - image：Coil 3 AsyncImage + thumbnailPath，按图片宽高比显示（`aspectRatio` modifier）。注意 Coil 3 包名 `coil3.compose.AsyncImage`，需 `coil-network-okhttp` 依赖。
  - link：固定比例卡片（URL 域名 + 内容摘要 + 图标）。
  - media：紧凑高度（文件图标 + 文件名）。
  - 尾部：层级 Tag 标签（Chip 展示完整层级路径，如「工作/项目A」）。
- [ ] 如果 Paging 3 与 StaggeredGrid 集成复杂度过高，可降级为 LazyColumn 单列卡片列表，并在代码注释中说明原因。
  - **注意：** `paging-compose` 未内置 `LazyStaggeredGridScope` 的 `items()` 扩展。需自行编写扩展函数：
    ```kotlin
    fun <T : Any> LazyStaggeredGridScope.myItems(
        items: LazyPagingItems<T>,
        itemContent: @Composable LazyStaggeredGridItemScope.(value: T?) -> Unit
    ) {
        items(count = items.itemCount) { index ->
            itemContent(items[index])
        }
    }
    ```
- [ ] `HomeViewModel`：加载 Item（强制 Paging 3 分页），监听搜索查询 + Tag 筛选。
- [ ] 搜索框：顶部搜索栏，实时搜索（debounce 300ms）。
- [ ] Tag 筛选器：点击展开 Tag 树 Modal，多选。

## Task 5 — 收藏详情页
- [ ] 定义路由：`Screen.Detail(id: Long)`（`@Serializable` data class）。
- [ ] `DetailScreen.kt`：根据 `type` 展示内容：
  - text：全文展示。
  - image：Coil 3 全屏预览图片（`coil3.compose.AsyncImage`）。
  - link：可点击打开浏览器。
  - media：使用 **AndroidX Media3** + `media3-ui-compose` 的 `PlayerSurface` composable。
    - ExoPlayer 实例由 DetailViewModel 持有（@HiltViewModel 注入 Context 创建）。
    - 生命周期绑定：`LifecycleEventEffect` 监听 ON_PAUSE→pause(), ON_RESUME→play()。
    - 资源释放：`DetailViewModel.onCleared()` + `DisposableEffect` 双重保障调用 `player.release()`。
    - 音频类型：播放/暂停 + 进度条 + 时长。
    - 视频类型：PlayerSurface + 控制栏。
    - 解码失败时友好提示。
  - 备注 `note` 可编辑区域。
- [ ] 链接抓取按钮：link 类型详情页显示「抓取页面内容」按钮，使用 Jsoup 抓取纯文本（见"AI 链接内容抓取"章节）。
- [ ] `DetailViewModel`：加载单条 Item + 关联 Tag。
- [ ] Tag 编辑：显示已有层级 Tag，点击添加/移除。
- [ ] AI 按钮：「AI 分析」，触发后显示加载状态。

## Task 6 — 新建收藏页（含 PROCESS_TEXT + Share Sheet 入口）
- [ ] `NewItemScreen.kt`：
  - 文本输入区（预填外部传入文本）。
  - 图片/媒体：相册选取、拍照、粘贴板粘贴。外部传入的 content URI 统一复制到 `files/clips/` 私有目录，存储本地路径而非 URI。
    - 文件命名：`{timestamp}_{UUID.randomUUID()}.{ext}`（ext 从 ContentResolver 推断）。
    - 复制操作在 `Dispatchers.IO` 中执行，超过 10MB 显示进度 Toast 或 Snackbar。
    - 在 `Activity.onCreate()` 中**立即启动复制操作**——content:// URI 权限可能随 Activity 栈变化过期。使用 `contentResolver.takePersistableUriPermission()` 尝试获取持久权限。
    - 存储空间不足时捕获 `IOException`，提示用户清理。
    - 复制失败不阻塞收藏创建，content 字段存储原始 URI 字符串降级，UI 标注 "⚠ 文件可能无法持久访问"。
  - Tag 选择器（可展开树形选择 / 新建 Tag 输入）。
  - 保存按钮。
- [ ] 解析外部 Intent：
  - `PROCESS_TEXT` → `intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)` 预填。
  - `ACTION_SEND` → 根据 `mimeType` 提取 text / image / media URI 预填。
- [ ] `NewItemActivity` 作为容器 + 透明主题（短跳转体验）。
- [ ] 保存成功后弹出 Toast + 自动返回。

## Task 7 — Tag 管理页
- [ ] `TagManagerScreen.kt`：树形展开列表（类似文件树）。
- [ ] 每个 Tag 行：名称 + 子节点数 + 操作按钮（新建子 Tag / 重命名 / 删除 / 移动父节点）。
- [ ] 移动父节点：弹出选择器选择新父 Tag（或设为根节点）。
- [ ] 删除：确认对话框。**Tag 删除策略（应用层事务，非数据库级 CASCADE）：**
    1. 开启 Room `@Transaction`。
    2. 查询被删节点的所有直接子节点。
    3. 将这些子节点的 parentId 更新为被删节点的 parentId（上移一层）。
    4. 删除目标 Tag。
    5. 删除该 Tag 关联的所有 ItemTag 记录。
    Tag Entity 的 parentId ForeignKey 使用 `onDelete = NO_ACTION`，由应用层控制删除逻辑。

## Task 8 — AI 集成 + AI 设置页
- [ ] `AiService.kt`：封装 OpenAI Chat Completions API 兼容 HTTP 调用。
  - 请求：`POST {baseUrl}/chat/completions`（OpenAI 兼容格式，末尾无 `/v1` 则默认补全）。
  - 标准 Body：`{ model, messages: [system, user], temperature, max_tokens }`。
  - 图片内容（vision 模式）：
    ```json
    {
      "messages": [{"role": "user", "content": [
        {"type": "text", "text": "分析这张图片"},
        {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,..."}}
      ]}]
    }
    ```
    仅在 `supportsVision=true` 时启用。图片 base64 控制在 1MB 以内。
  - 解析响应 JSON，提取 `choices[0].message.content`。
  - 预期返回格式：`{"summary": "...", "suggested_tags": ["tag1", "tag2"]}`。
    如果 AI 返回的不是 JSON（失忆/跑偏），降级处理：将全文回复作为 summary，不解析 tags。
  - **AiService 异常处理规范：**
    - HTTP 401/403 → "API Key 无效或已过期"
    - HTTP 429 → "请求频率过高"，可选指数退避重试（最多 3 次）
    - HTTP 500+ → "AI 服务暂时不可用"
    - 网络超时 → OkHttp 连接 15s / 读取 60s / 写入 15s
    - `choices` 为空或 null / `message.content` 为 null → "AI 未返回有效内容"
    - 响应是 SSE 格式（`data: `开头）→ 解析最后一个 data 块
    - 请求体中显式设置 `"stream": false`（非请求头），防止流式响应
    - image base64 超过 1MB → 降低 JPEG quality 至 50，再超则降分辨率至 1024px
    - text content 超过 32000 字符 → 截断并附加 "[内容已截断]"
    - 所有异常统一封装为 sealed class `AiResult`
- [ ] `AiSettingsScreen.kt`：多提供商配置管理（参考 OpenClaw / Continue.dev / Claude Code 的成熟模式）。
  - **配置列表**：卡片式展示已有 AI 配置，显示名称、模型名、当前激活状态。支持左滑删除。
  - **新建/编辑表单**：
    - 名称（必填，如 "My GPT"）
    - Base URL（必填，如 `https://api.openai.com/v1`，支持末尾 `/v1` 或不带）
    - API Key（必填，密文输入框，显示为 ••••••）
    - Model Name（必填，如 `gpt-4o-mini`）
    - 开关：「此模型支持图片分析」（supportsVision）
    - 高级选项（可折叠）：Max Tokens（默认 4096）、Temperature（默认 0.7）、System Prompt（多行编辑，默认值见附录）
  - **测试连接按钮**：点击后发送一个简短请求（"Say 'Hello'"），验证 Base URL + API Key + Model 是否联通，返回绿色 ✅ / 红色 ❌ 结果。
  - **激活开关**：每页顶部或列表中设置当前激活的配置，激活后所有 AI 调用使用此配置。
- [ ] AI 调用整合到 `DetailViewModel`：
  - 用户手动点击「AI 分析」（所有类型均需手动触发，无自动分析）。
  - 读取当前激活的 AI 配置 → 根据 `type` + `supportsVision` 构建请求体 → 异步调用。
  - 请求场景适配：
    - `type=text`：content 全文作为 text 消息发送。
    - `type=image` & `supportsVision=true`：将缩略图压缩为 base64（1MB 上限）以 image_url 格式发送 + 备注文字。
    - `type=image` & `supportsVision=false`：降级，发送 "[图片收藏] 文件名: xxx, 备注: xxx"。
    - `type=link` & 已抓取内容：发送 URL + 抓取的文章内容 + 备注。
    - `type=link` & 未抓取：发送 URL + 备注。
  - 解析响应：提取 summary 和 suggested_tags。
  - 展示 Modal Bottom Sheet：总结文本（可编辑）+ 推荐 Tag（Chip 形式，一键采纳）+ 确认保存 / 取消。
  - 总结保存到 `note` 字段；采纳的 Tag 自动关联到当前 Item。

## Task 9 — 通用设置页
- [ ] `SettingsScreen.kt`：
  - 数据导出（JSON）：所有 `ClipItem` + `Tag` + `ItemTag` 导出为 JSON 文件，用 SAF（Storage Access Framework）让用户选择保存位置。
  - 数据导入（JSON）：从 SAF 选择 JSON 文件。
    - **覆盖模式**：清空所有现有数据，批量插入导入数据（`@Transaction` 确保原子性）。
    - **合并模式**：忽略导入的原始 ID，使用 autoGenerate 生成新 ID；Tag 按 name+parentId 去重复用；ItemTag 根据新 ID 重建关联。
    - 导入前展示预览：Item 数量、Tag 数量、覆盖影响提醒。
  - 关于：版本号、开源许可等。

## Task 10 — 数据加密与安全
- [ ] API Key 加密存储：
  - **架构**：Room 的 AiProvider 表中 `apiKey` 字段存储空字符串。实际 API Key 存储在 Preferences DataStore 中，key 格式 `"api_key_{providerId}"`。AiProviderRepository 读取时从 DataStore 解密拼装完整对象。
  - **方案 A（推荐评估）**：使用 `androidx.datastore:datastore-tink:1.3.0-alpha07` 的 `AeadSerializer`——Google 官方维护，自动处理 IV 生成和密文格式。注意当前为 alpha 版本。
  - **方案 B（手写）**：创建 `CryptoManager` 工具类：
    - 密钥别名：`"clipvault_aes_key"`，Android Keystore 生成 AES-256。
    - 参数：`PURPOSE_ENCRYPT|DECRYPT` + `BLOCK_MODE_GCM` + `ENCRYPTION_PADDING_NONE`。如设备支持 StrongBox，设 `setIsStrongBoxBacked(true)`。
    - 加密：`Cipher.getInstance("AES/GCM/NoPadding")` → 自动生成 12 字节 IV → `Base64(IV[12] + CipherText + AuthTag[16])`。
    - 解密：Base64 解码 → 提取前 12 字节为 IV → `GCMParameterSpec(128, iv)` → 解密。
    - 密钥丢失：捕获 `KeyPermanentlyInvalidatedException` / `UnrecoverableKeyException` → 删除旧密钥 → 清空加密 API Key → 提示用户重新配置。
    - 所有操作在 `Dispatchers.IO` 执行。
- [ ] 数据库文件是否加密（使用 Room 的 SQLCipher 集成）——默认不做，按需启用。

## Task 11 — 主题系统集成
- [ ] 实现 Material You 3.0 主题系统：`DynamicColors.applyToActivitiesIfAvailable()` + 自定义回退 `ColorScheme`。
- [ ] 定义独立的 `lightColorScheme()` 与 `darkColorScheme()`（Dark-First 设计，OLED 纯黑背景）。
- [ ] 配置 `Typography`（Material 3 默认缩放）与 `Shapes`（大圆角体系）。
- [ ] Edge-to-Edge 沉浸布局：`WindowCompat.setDecorFitsSystemWindows(window, false)` + 系统栏半透明适配。
- [ ] 用户可手动切换深色/浅色/跟随系统。偏好使用 Preferences DataStore 存储，key `"theme_mode"`（枚举值 LIGHT/DARK/FOLLOW_SYSTEM），默认 FOLLOW_SYSTEM。在 Theme composable 启动时读取。
- [ ] 骨架屏加载占位（Shimmer Effect，可选）。
- [ ] 空状态页面（展示毛玻璃风格插图 + "还没有收藏，去其他应用复制文本试试"）。
- [ ] 错误状态处理：加载失败、AI 调用失败（展示友好错误信息 + 重试按钮）。

## 非功能需求
- **性能：** Timeline LazyColumn + Coil 图片缓存 + 【agent 自行搜索】强制使用 Paging 3 实现分页加载。
- **离线优先：** 所有数据本地存储，无需网络即可正常使用全功能。
- **AI 容错：** AI 调用失败不影响核心收藏功能，显示友好错误提示。
- **压缩：** 图片超过 5MB 自动压缩后再存缩略图。缩略图生成：`BitmapFactory.Options inSampleSize` 采样，目标尺寸不超过 2048px（长边），JPEG quality 85。原图保留，缩略图单独存储。
  - AI vision 分析：从缩略图进一步压缩到 base64 不超过 1MB，如仍超过则降低 quality 至 50，再超过则降低分辨率至 1024px。
  - GIF/动画 WebP：不生成缩略图，使用原文件。AI 分析时降级为文件名描述。
  - 压缩异常（OOM/格式不支持）：使用空缩略图路径，UI 显示通用文件图标，不阻塞保存流程。
- **协程安全：** 所有 IO 操作在 `Dispatchers.IO`，Room 查询使用 `Flow`。

## AI 链接内容抓取
link 类型的 AI 总结采用**手动触发**模式：
- 链接收藏详情页增加「抓取页面内容」按钮（独立于 AI 分析）。
- **Jsoup 请求配置：**
  - userAgent: `"Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"`
  - timeout: 15000ms
  - maxBodySize: 2MB（防止 OOM）
  - followRedirects: true（最多 20 次）
  - ignoreHttpErrors: true（手动处理状态码）
- **内容提取：** 优先提取 `<article>`、`<main>` 或 `[role="main"]` 元素纯文本；不足 50 字符则回退到 `body.text()`。
- **存储：** 保存到 `ClipItem.fetchedContent` 字段。
- **异常处理：**
  - `SocketTimeoutException` → "页面加载超时"
  - `HttpStatusException` (403/404/5xx) → "无法访问该页面 (HTTP {code})"
  - `UnsupportedMimeTypeException` → "该链接不是网页"
  - `IOException` → "网络错误"
  - 提取文本 < 50 字符 → "该页面可能需要 JavaScript 渲染，AI 分析将仅基于 URL"
- 所有 Jsoup 操作在 `Dispatchers.IO` 上执行。
- 抓取失败不影响 AI 分析功能——降级为仅传 URL + 备注。

# 附录：默认 AI System Prompt（可编辑）

```
You are an AI assistant for a personal knowledge collection app called ClipVault.
Given the user's saved content, please:
1. Write a concise summary (2-3 sentences in the user's language).
2. Suggest 3-5 relevant tags for categorization. Tags should be hierarchical if appropriate (e.g., "Work/ProjectA").
Return in JSON format:
{"summary": "...", "suggested_tags": ["tag1", "Work/ProjectB", "tag3"]}
```

# 附录：数据导出 JSON Schema

```json
{
  "version": 1,
  "exportedAt": "2026-05-25T10:00:00Z",
  "items": [
    {
      "id": 1,
      "type": "text",
      "content": "示例文本",
      "note": "",
      "fetchedContent": "",
      "thumbnailPath": "",
      "createdAt": 1716624000000,
      "updatedAt": 1716624000000,
      "sourceApp": ""
    }
  ],
  "tags": [
    {
      "id": 1,
      "name": "工作",
      "parentId": null,
      "createdAt": 1716624000000
    },
    {
      "id": 2,
      "name": "项目A",
      "parentId": 1,
      "createdAt": 1716624000000
    }
  ],
  "itemTags": [
    { "itemId": 1, "tagId": 2 }
  ]
}
```

说明：
- tags 使用 flat list（非嵌套），parentId 引用同一数组内的 Tag id。
- 合并导入时忽略所有 id，由 autoGenerate 重新分配。
- itemTags 中的 itemId/tagId 引用导出时的原始 id，导入时需建立 oldId → newId 映射表重建关联。
- version 字段用于未来数据库 schema 升级时的兼容性判断。
