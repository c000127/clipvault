# ClipVault 概要与详细设计说明书 (SDD)

**文档编号：** CV-SDD-001  
**版本：** 1.0  
**日期：** 2026-05-25

---

## 1. 系统架构设计

### 1.1 分层架构
```
┌─────────────────────────────────────────────┐
│           UI Layer (Compose Screens)         │
│  HomeScreen │ DetailScreen │ NewItemScreen   │
│  TagManager │ AiSettings   │ Settings        │
├─────────────────────────────────────────────┤
│         ViewModel Layer (MVVM + Hilt)        │
│  HomeVM │ DetailVM │ NewItemVM │ TagMgrVM    │
│  AiSettingsVM │ SettingsVM                   │
├─────────────────────────────────────────────┤
│           Data Layer (Repository)            │
│  ClipItemRepo │ TagRepo │ AiProviderRepo     │
├──────────────┬──────────────┬───────────────┤
│  Room (DAO)  │  DataStore   │  OkHttp       │
│  AppDatabase │  API Keys    │  AiService    │
│  CryptoMgr   │  Theme Prefs │  Jsoup        │
└──────────────┴──────────────┴───────────────┘
```

### 1.2 依赖注入配置
```
@HiltAndroidApp
ClipVaultApplication
    └── @AndroidEntryPoint MainActivity
        └── NavHost
            ├── composable<Screen.Home>     → HomeScreen
            ├── composable<Screen.Detail>   → DetailScreen
            ├── composable<Screen.New>      → NewItemScreen
            ├── composable<Screen.TagManager> → TagManagerScreen
            ├── composable<Screen.Settings> → SettingsScreen
            └── composable<Screen.AiSettings> → AiSettingsScreen

@Module DatabaseModule
    ├── provideDatabase() → AppDatabase (Singleton)
    ├── provideClipItemDao()
    ├── provideTagDao()
    ├── provideItemTagDao()
    └── provideAiProviderDao()

@Module RepositoryModule (abstract, constructor injection handles bindings)
```

### 1.3 导航路由
```kotlin
@Serializable sealed interface Screen {
    @Serializable data object Home : Screen
    @Serializable data class Detail(val id: Long) : Screen
    @Serializable data class New(val text: String? = null) : Screen
    @Serializable data object TagManager : Screen
    @Serializable data object AiSettings : Screen
    @Serializable data object Settings : Screen
}
```

---

## 2. 数据库设计

### 2.1 ER 图
```
ClipItem (1) ──── (N) ItemTag (N) ──── (1) Tag
                                           │
                                      (self-referencing)
                                           │
                                     Tag.parentId → Tag.id
```

### 2.2 表结构

#### items 表
| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTO_INCREMENT | 主键 |
| type | TEXT | NOT NULL | "text"/"image"/"link"/"media" |
| content | TEXT | NOT NULL | 文本内容/本地路径/URL |
| note | TEXT | DEFAULT "" | 用户备注（AI 总结追加至此） |
| thumbnailPath | TEXT | DEFAULT "" | 缩略图本地路径 |
| fetchedContent | TEXT | DEFAULT "" | Jsoup 抓取的页面纯文本 |
| createdAt | INTEGER | NOT NULL | 创建时间戳 |
| updatedAt | INTEGER | NOT NULL | 更新时间戳 |
| sourceApp | TEXT | DEFAULT "" | 来源应用包名 |

#### tags 表
| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTO_INCREMENT | 主键 |
| name | TEXT | NOT NULL | 标签名称 |
| parentId | INTEGER | FK → tags.id, ON DELETE NO_ACTION | 父节点 ID，null 为根节点 |
| createdAt | INTEGER | NOT NULL | 创建时间戳 |

索引：`CREATE INDEX index_tags_parentId ON tags(parentId)`

#### item_tags 表
| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| itemId | INTEGER | FK → items.id, ON DELETE CASCADE | 收藏 ID |
| tagId | INTEGER | FK → tags.id, ON DELETE CASCADE | 标签 ID |

复合主键：`(itemId, tagId)`  
索引：`CREATE INDEX index_item_tags_itemId ON item_tags(itemId)`  
索引：`CREATE INDEX index_item_tags_tagId ON item_tags(tagId)`

#### ai_providers 表
| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTO_INCREMENT | 主键 |
| name | TEXT | NOT NULL | 用户自定义名称 |
| baseUrl | TEXT | NOT NULL | API 基础 URL |
| apiKey | TEXT | DEFAULT "" | 占位，实际存 DataStore |
| modelName | TEXT | NOT NULL | 模型 ID |
| supportsVision | INTEGER | DEFAULT 0 | 是否支持图片理解 |
| maxTokens | INTEGER | DEFAULT 4096 | 最大输出 token |
| temperature | REAL | DEFAULT 0.7 | 生成温度 |
| systemPrompt | TEXT | DEFAULT (系统提示词) | 系统提示词 |
| isActive | INTEGER | DEFAULT 0 | 当前激活配置 |

### 2.3 CTE 递归查询

```sql
-- 获取 Tag 及其所有子节点（深度限制 50 层）
WITH RECURSIVE tag_tree(id, name, parentId, createdAt, depth) AS (
    SELECT id, name, parentId, createdAt, 0 FROM tags WHERE id = :rootTagId
    UNION ALL
    SELECT t.id, t.name, t.parentId, t.createdAt, tt.depth + 1
    FROM tags t INNER JOIN tag_tree tt ON t.parentId = tt.id
    WHERE tt.depth < 50
)
SELECT id, name, parentId, createdAt FROM tag_tree

-- 按 Tag 过滤收藏（含子节点）
WITH RECURSIVE tag_tree(id, depth) AS (
    SELECT :tagId, 0
    UNION ALL
    SELECT t.id, tt.depth + 1
    FROM tags t INNER JOIN tag_tree tt ON t.parentId = tt.id
    WHERE tt.depth < 50
)
SELECT DISTINCT i.* FROM items i
INNER JOIN item_tags it ON i.id = it.itemId
WHERE it.tagId IN (SELECT id FROM tag_tree)
ORDER BY i.createdAt DESC

```

---

## 3. 数据加密存储方案

### 3.1 API Key 加密流程
```
用户输入 API Key
    ↓
AiProviderRepository.saveApiKey(providerId, apiKey)
    ↓
CryptoManager.encrypt(apiKey)
    ├── KeyGenerator.getInstance("AES", "AndroidKeyStore")
    ├── KeyGenParameterSpec: AES-256, GCM, NoPadding
    ├── Cipher.getInstance("AES/GCM/NoPadding")
    ├── cipher.init(ENCRYPT_MODE, key)
    ├── iv = cipher.iv (12 bytes)
    ├── ciphertext = cipher.doFinal(plaintext)
    └── return Base64(iv + ciphertext)
    ↓
DataStore<Preferences>.edit { prefs["api_key_$id"] = encrypted }
```

### 3.2 API Key 解密流程
```
DataStore.data.first()["api_key_$id"]
    ↓
CryptoManager.decrypt(encryptedBase64)
    ├── combined = Base64.decode(encryptedBase64)
    ├── iv = combined[0..12]
    ├── ciphertext = combined[12..]
    ├── cipher.init(DECRYPT_MODE, key, GCMParameterSpec(128, iv))
    └── return String(cipher.doFinal(ciphertext))
    ↓
AiProvider.copy(apiKey = decryptedKey)
```

### 3.3 密钥丢失恢复
```
catch (KeyPermanentlyInvalidatedException) → handleKeyLoss()
catch (BadPaddingException) → return "" (数据损坏)
catch (IllegalBlockSizeException) → return "" (数据损坏)
catch ("Key not found") → handleKeyLoss()

handleKeyLoss():
    keyStore.deleteEntry(KEY_ALIAS)
    generateKey()
```

---

## 4. 接口协议

### 4.1 AI Chat Completions API

**请求：**
```
POST {baseUrl}/v1/chat/completions
Authorization: Bearer {apiKey}
Content-Type: application/json

{
    "model": "{modelName}",
    "messages": [
        {"role": "system", "content": "{systemPrompt}"},
        {"role": "user", "content": "{content}"}
    ],
    "temperature": 0.7,
    "max_tokens": 4096,
    "stream": false
}
```

**Base URL 拼接规则：**
```kotlin
fun buildChatUrl(baseUrl: String): String {
    val url = baseUrl.trim().trimEnd('/')
    return when {
        url.endsWith("/chat/completions") -> url
        url.contains("/chat/completions/") -> url
        url.endsWith("/v1") -> "$url/chat/completions"
        url.contains("/v1/") -> {
            val v1Index = url.indexOf("/v1/")
            val basePart = url.substring(0, v1Index + 3)
            val remainingPart = url.substring(v1Index + 4).trim('/')
            if (remainingPart.contains("chat/completions")) {
                url
            } else if (remainingPart.isEmpty()) {
                "$basePart/chat/completions"
            } else {
                "$basePart/$remainingPart/chat/completions"
            }
        }
        url.endsWith("/completions") || url.endsWith("/generate") -> url
        else -> "$url/v1/chat/completions"
    }
}

```

**Vision 模式请求体：**
```json
{
    "messages": [{
        "role": "user",
        "content": [
            {"type": "text", "text": "分析这张图片"},
            {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,..."}}
        ]
    }]
}
```

**响应解析：**
```json
{
    "choices": [{
        "message": {
            "content": "{\"summary\": \"...\", \"suggested_tags\": [\"tag1\", \"tag2\"]}"
        }
    }]
}
```

### 4.2 Jsoup 链接抓取配置
```kotlin
Jsoup.connect(url)
    .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ...")
    .timeout(15000)
    .maxBodySize(2 * 1024 * 1024)  // 2MB
    .followRedirects(true)
    .ignoreHttpErrors(true)
    .get()
```

内容提取优先级：`<article>` / `<main>` / `[role=main]` → `body.text()` → 降级提示

---

## 5. 模块划分

### 5.1 源代码目录结构
```
com.clipvault.app/
├── ClipVaultApplication.kt          // @HiltAndroidApp
├── MainActivity.kt                   // @AndroidEntryPoint, NavHost
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           // @Database, 4 entities
│   │   ├── CryptoManager.kt         // AES-256-GCM 加密
│   │   ├── dao/
│   │   │   ├── ClipItemDao.kt       // CRUD + 搜索 + Tag 过滤
│   │   │   ├── TagDao.kt            // CRUD + CTE 递归 + 环形检测
│   │   │   ├── ItemTagDao.kt        // 关联增删 + 按 ID 查询
│   │   │   └── AiProviderDao.kt     // CRUD + 激活切换
│   │   └── entity/
│   │       ├── ClipItem.kt          // items 表
│   │       ├── Tag.kt               // tags 表 (自引用 FK)
│   │       ├── ItemTag.kt           // item_tags 关联表
│   │       └── AiProvider.kt        // ai_providers 表
│   ├── remote/
│   │   └── AiService.kt             // OkHttp + OpenAI API
│   └── repository/
│       ├── ClipItemRepository.kt    // 收藏 CRUD + 搜索 + Tag 关联
│       ├── TagRepository.kt         // 标签 CRUD + 树操作 + 删除重挂载
│       └── AiProviderRepository.kt  // Provider CRUD + DataStore 加密 Key
├── di/
│   ├── DatabaseModule.kt            // Room + DAO 提供
│   └── RepositoryModule.kt          // Repository 绑定
└── ui/
    ├── navigation/
    │   └── Screen.kt                // @Serializable 路由定义
    ├── theme/
    │   ├── Color.kt                 // 自定义回退色
    │   ├── Type.kt                  // Typography
    │   ├── Theme.kt                 // Material You + Dynamic Color
    │   └── ThemePreferences.kt      // DataStore 主题偏好
    ├── components/
    │   ├── EmptyState.kt            // 空状态组件
    │   └── ErrorState.kt            // 错误状态组件
    ├── home/
    │   ├── HomeScreen.kt            // LazyVerticalStaggeredGrid
    │   ├── HomeViewModel.kt         // 搜索 + Tag 过滤 + Paging
    │   ├── PagingExt.kt             // pagingItems 扩展函数
    │   └── TagFilterSheet.kt        // Modal Bottom Sheet
    ├── detail/
    │   ├── DetailScreen.kt          // 类型分支渲染
    │   └── DetailViewModel.kt       // ExoPlayer + Jsoup + 标签管理
    ├── newitem/
    │   ├── NewItemActivity.kt       // PROCESS_TEXT + ACTION_SEND + 权限
    │   ├── NewItemScreen.kt         // 输入 + 媒体选取 + Tag 选择
    │   └── NewItemViewModel.kt      // URI 复制 + 保存逻辑
    ├── tagmanager/
    │   ├── TagManagerScreen.kt      // 树形展开列表
    │   └── TagManagerViewModel.kt   // CRUD + 移动 + 删除重挂载
    ├── aisettings/
    │   ├── AiSettingsScreen.kt      // Provider 列表 + 表单
    │   └── AiSettingsViewModel.kt   // CRUD + 测试连接
    └── settings/
        ├── SettingsScreen.kt        // 导出/导入 + 关于
        └── SettingsViewModel.kt     // SAF JSON 操作
```

### 5.2 模块职责矩阵

| 模块 | 职责 | 关键类 |
|------|------|--------|
| 数据层 | 持久化存储 | AppDatabase, DAO, Entity |
| 加密层 | API Key 保护 | CryptoManager |
| 网络层 | AI API 调用 | AiService |
| 仓库层 | 数据访问抽象 | ClipItemRepository, TagRepository, AiProviderRepository |
| DI 层 | 依赖注入 | DatabaseModule, RepositoryModule |
| UI 层 | 界面渲染 | Screen, ViewModel, Composable |
| 主题层 | 视觉系统 | Theme, ThemePreferences |
