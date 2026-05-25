# ClipVault 规范化源代码资产清单

**文档编号：** CV-SCA-001  
**版本：** 1.0  
**日期：** 2026-05-25

---

## 1. 构建配置文件

### 1.1 Version Catalog (`gradle/libs.versions.toml`)
状态：完整  
依赖项总数：26 个库 + 5 个插件

| 依赖 | 版本 | 用途 |
|------|------|------|
| compose-bom | 2026.05.01 | Compose UI 统一版本管理 |
| room | 2.8.4 | 本地数据库 (非 3.0 KMP 版) |
| hilt | 2.59.2 | 依赖注入 |
| navigation-compose | 2.9.8 | Type-Safe 导航 |
| media3 | 1.10.1 | ExoPlayer 媒体播放 |
| okhttp-bom | 5.3.2 | HTTP 客户端 |
| coil | 3.4.0 | 图片加载 (coil3 包名) |
| jsoup | 1.22.2 | HTML 解析 |

### 1.2 App Build Config (`app/build.gradle.kts`)
状态：完整  
关键配置：
- `namespace = "com.clipvault.app"`
- `compileSdk = 37`, `minSdk = 33`, `targetSdk = 37`
- `isCoreLibraryDesugaringEnabled = true`
- KSP `room.schemaLocation` 导出到 `$projectDir/schemas`
- 未声明 `kotlin("android")` 插件（AGP 9.x 内置）

---

## 2. 源代码文件清单

### 2.1 Application 层 (2 文件)

| 文件 | 行数 | 职责 | 注释状态 |
|------|------|------|----------|
| ClipVaultApplication.kt | 10 | @HiltAndroidApp 入口 | 完整 |
| MainActivity.kt | 82 | NavHost + enableEdgeToEdge | 完整 |

### 2.2 Data Layer (16 文件)
 
| 文件 | 行数 | 职责 | 注释状态 |
|------|------|------|----------|
| AppDatabase.kt | 130 | @Database 5 entities + MIGRATION_1_2 | 完整 |
| CryptoManager.kt | 168 | AES-256-GCM 加密 | 完整 |
| ClipItemDao.kt | 136 | CRUD + 搜索 + Tag 过滤 | 完整 |
| TagDao.kt | 101 | CRUD + CTE 递归 + 环形检测 | 完整 |
| ItemTagDao.kt | 58 | 关联增删查询 | 完整 |
| AiProviderDao.kt | 76 | CRUD + 激活切换 | 完整 |
| ContentAttachmentDao.kt | 30 | CRUD for attachments | 完整 |
| ClipItem.kt | 21 | items Entity with attachments | 完整 |
| ContentAttachment.kt | 28 | content_attachments Entity | 完整 |
| Tag.kt | 21 | tags Entity (自引用 FK) | 完整 |
| ItemTag.kt | 20 | item_tags 关联 Entity | 完整 |
| AiProvider.kt | 33 | ai_providers Entity | 完整 |
| AiService.kt | 258 | OpenAI API 封装 | 完整 |
| ClipItemRepository.kt | 110 | 收藏仓库 (with attachments) | 完整 |
| TagRepository.kt | 109 | 标签仓库 (with getTagPath) | 完整 |
| AiProviderRepository.kt | 106 | Provider 仓库 + 加密 Key | 完整 |

### 2.3 DI Layer (2 文件)

| 文件 | 行数 | 职责 | 注释状态 |
|------|------|------|----------|
| DatabaseModule.kt | 50 | Room + DAO 提供 | 完整 |
| RepositoryModule.kt | 12 | Repository 绑定占位 | 完整 |

### 2.4 UI Layer (22 文件)

| 文件 | 行数 | 职责 | 注释状态 |
|------|------|------|----------|
| Screen.kt | 12 | @Serializable 路由 | 完整 |
| Color.kt | 14 | 回退色彩定义 | 完整 |
| Type.kt | 48 | Typography | 完整 |
| Theme.kt | 65 | Material You 主题 | 完整 |
| ThemePreferences.kt | 36 | DataStore 主题偏好 | 完整 |
| EmptyState.kt | 38 | 空状态组件 | 完整 |
| ErrorState.kt | 44 | 错误状态组件 | 完整 |
| HomeScreen.kt | 230 | 首页瀑布流 | 完整 |
| HomeViewModel.kt | 97 | 搜索 + Tag 过滤 | 完整 |
| PagingExt.kt | 15 | pagingItems 扩展 | 完整 |
| TagFilterSheet.kt | 107 | Tag 筛选 Bottom Sheet | 完整 |
| DetailScreen.kt | 331 | 详情页 | 完整 |
| DetailViewModel.kt | 173 | 详情逻辑 | 完整 |
| NewItemActivity.kt | 155 | Intent 处理 + 权限 | 完整 |
| NewItemScreen.kt | 185 | 新建页 UI | 完整 |
| NewItemViewModel.kt | 178 | 新建逻辑 | 完整 |
| TagManagerScreen.kt | 310 | 标签管理页 | 完整 |
| TagManagerViewModel.kt | 155 | 标签管理逻辑 | 完整 |
| AiSettingsScreen.kt | 280 | AI 设置页 | 完整 |
| AiSettingsViewModel.kt | 148 | AI 设置逻辑 | 完整 |
| SettingsScreen.kt | 230 | 通用设置页 | 完整 |
| SettingsViewModel.kt | 174 | 导出/导入逻辑 | 完整 |

---

## 3. 已修复的 Bug 清单

| Bug | 根因 | 修复文件 | 修复方式 |
|-----|------|----------|----------|
| 标签不刷新 | 嵌套 collect 阻塞 Flow | DetailViewModel.kt | 拆分独立协程 + collectLatest + catch |
| 图片附加失败 | 无动态权限请求 | NewItemActivity.kt | RequestMultiplePermissions launcher |
| 白屏数据丢失 | 无 fallbackToDestructiveMigration | DatabaseModule.kt | 添加 dropAllTables=false |
| AI 无法连接 | decrypt 抛异常 / URL 拼接 / Key 未读取 | CryptoManager.kt, AiService.kt, AiSettingsViewModel.kt | catch 全异常 / buildChatUrl / getApiKey |

---

## 4. 测试代码状态

当前项目无单元测试文件。需要补充的测试模块见 `04-test-plan.md`。

---

## 5. 资源文件清单

| 文件 | 用途 |
|------|------|
| res/values/strings.xml | 应用名称 |
| res/values/themes.xml | XML 主题占位 |
| res/values/colors.xml | 基础颜色 |
| res/values/ic_launcher_background.xml | 启动图标背景色 |
| res/xml/network_security_config.xml | 内网明文流量白名单 |
| res/drawable/ic_launcher_foreground.xml | 启动图标前景 |
| res/mipmap-anydpi-v26/ic_launcher.xml | 自适应图标 |
