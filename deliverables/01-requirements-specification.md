# ClipVault 需求规格说明书 (SRS)

**文档编号：** CV-SRS-001  
**版本：** 1.0  
**日期：** 2026-05-25  
**状态：** 已批准

---

## 1. 引言

### 1.1 目的
本文档定义 ClipVault 应用的完整功能需求与非功能需求，作为设计、开发、测试和验收的基准。

### 1.2 范围
ClipVault 是一款 Android 原生应用，为知识工作者提供跨应用碎片信息收藏、层级化标签管理和 AI 辅助分析功能。

### 1.3 术语定义

| 术语 | 定义 |
|------|------|
| ClipItem | 一条收藏记录，支持 text/image/link/media 四种类型 |
| Tag | 树形标签节点，支持无限层级嵌套 |
| AiProvider | AI 服务商配置（Base URL / API Key / Model） |
| CTE | Common Table Expression，SQLite 递归查询 |
| SAF | Storage Access Framework，Android 存储访问框架 |

---

## 2. 系统概述

### 2.1 系统架构
```
UI Layer (Jetpack Compose + Material 3)
    ↕
ViewModel Layer (MVVM + Hilt)
    ↕
Data Layer (Repository → Room DAO + DataStore + OkHttp)
```

### 2.2 技术栈

| 组件 | 版本 |
|------|------|
| Kotlin | 2.3.21 (AGP 9.x 内置) |
| Jetpack Compose BOM | 2026.05.01 |
| Material 3 | 1.4.0 |
| Room | 2.8.4 |
| Hilt | 2.59.2 |
| Navigation Compose | 2.9.8 |
| Paging | 3.3.6 |
| Media3 | 1.10.1 |
| OkHttp | 5.3.2 |
| Coil | 3.4.0 |
| AGP | 9.2.1 |
| Gradle | 9.4 |
| Min SDK | 33 (Android 13) |
| Target SDK | 37 (Android 17) |

---

## 3. 功能需求

### FR-001 全能收藏
**优先级：** P0  
**描述：** 支持四种内容类型收藏与统一预览。

| 类型 | 存储方式 | 预览方式 |
|------|----------|----------|
| text | 内容字符串 | 全文显示，首页摘要前 100 字 |
| image | 私有目录 `files/clips/` | Coil AsyncImage |
| link | URL 字符串 | 域名 + 摘要 + 图标 |
| media | 私有目录 `files/clips/` | ExoPlayer 播放 |

**验收标准：**
- [ ] 四种类型均可创建、查看、编辑、删除
- [ ] 图片/媒体文件从外部 URI 复制到私有目录
- [ ] 首页卡片按类型差异化渲染

### FR-002 快捷收藏入口
**优先级：** P0  
**描述：** 通过系统级入口快速收藏内容。

| 入口 | Intent Action | 数据来源 |
|------|---------------|----------|
| 文本选择菜单 | ACTION_PROCESS_TEXT | 任意应用选中文本 |
| 系统分享菜单 | ACTION_SEND | 相册、浏览器、文件管理器 |
| 应用内置 | 手动输入 | 新建收藏页 |

**验收标准：**
- [ ] PROCESS_TEXT 正确提取 `EXTRA_PROCESS_TEXT`
- [ ] ACTION_SEND 根据 mimeType 分发到对应处理
- [ ] 外部 URI 在 `onCreate` 立即复制到私有目录
- [ ] `takePersistableUriPermission` 尝试获取持久权限

### FR-003 层级化标签
**优先级：** P0  
**描述：** 支持无限层级嵌套的树形标签体系。

**数据模型：**
```kotlin
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,  // null = 根节点
    val createdAt: Long = System.currentTimeMillis()
)
```

**操作：**
- 创建、重命名、删除、调整父节点
- 首页 Tag 筛选器：树形展开/折叠选择，多选组合过滤
- Tag 搜索：按名称搜索后展示该 Tag 及子节点下的所有收藏

**验收标准：**
- [ ] CTE 递归查询带 `depth < 50` 限制
- [ ] 环形引用防护：`moveTag` 校验新 parentId 不在子树中
- [ ] 删除 Tag 时子节点上移一层（`@Transaction`）

### FR-004 AI 辅助
**优先级：** P1  
**描述：** 云端调用 OpenAI Chat Completions API 兼容接口。

**功能：**
- AI 内容总结：手动触发，结果追加到 `note` 字段
- Tag 推荐：分析内容语义，推荐 3~5 个 Tag
- 多提供商配置：支持切换不同 AI 服务商

**请求格式：**
```json
{
  "model": "gpt-4o-mini",
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "..."}
  ],
  "temperature": 0.7,
  "max_tokens": 4096,
  "stream": false
}
```

**异常处理：**

| HTTP 状态码 | 处理方式 |
|-------------|----------|
| 401/403 | "API Key 无效或已过期" |
| 429 | "请求频率过高" |
| 500+ | "AI 服务暂时不可用" |
| 超时 | OkHttp 连接 15s / 读取 60s / 写入 15s |
| choices 为空 | "AI 未返回有效内容" |
| SSE 格式 | 解析最后一个 data 块 |

**验收标准：**
- [ ] 请求体显式设置 `"stream": false`
- [ ] text content 超过 32000 字符截断
- [ ] image base64 超过 1MB 压缩至 50 quality / 1024px
- [ ] 响应解析 JSON → `{"summary": "...", "suggested_tags": [...]}`
- [ ] 非 JSON 响应降级处理

### FR-005 基础编辑与管理
**优先级：** P0  
**描述：** 收藏的 CRUD、搜索、批量操作。

**功能列表：**
- 时间线流（Timeline Feed），按 `createdAt` 降序
- 全文搜索：content + note + Tag.name，debounce 300ms
- 长按多选：批量删除
- 单条删除与分享
- 备注编辑（`note` 字段）
- 链接内容抓取（Jsoup）

**验收标准：**
- [ ] Paging 3 分页加载
- [ ] LazyVerticalStaggeredGrid 瀑布流布局
- [ ] 搜索使用 `LIKE '%' || :query || '%'`

### FR-006 数据导出/导入
**优先级：** P1  
**描述：** 通过 SAF 导出/导入 JSON 格式数据。

**JSON Schema：**
```json
{
  "version": 1,
  "exportedAt": "2026-05-25T10:00:00Z",
  "items": [{ "id": 1, "type": "text", "content": "...", ... }],
  "tags": [{ "id": 1, "name": "工作", "parentId": null, ... }],
  "itemTags": [{ "itemId": 1, "tagId": 2 }]
}
```

**导入模式：**
- 覆盖模式：清空现有数据，批量插入
- 合并模式：忽略原始 ID，autoGenerate 重新分配，Tag 按 name+parentId 去重

**验收标准：**
- [ ] 导出包含所有 ClipItem + Tag + ItemTag
- [ ] 覆盖模式使用 `@Transaction` 确保原子性
- [ ] 合并模式建立 oldId → newId 映射表

---

## 4. 非功能需求

### NFR-001 性能
- Timeline LazyScroll + Coil 图片缓存 + Paging 3 分页
- 图片超过 5MB 自动压缩（`inSampleSize`，目标 2048px，JPEG quality 85）
- 缩略图单独存储

### NFR-002 离线优先
- 所有数据本地存储，无需网络即可正常使用全功能
- AI 调用失败不影响核心收藏功能

### NFR-003 安全
- API Key 使用 Android Keystore AES-256-GCM 加密
- 密钥别名：`clipvault_aes_key`
- 格式：`Base64(IV[12] + CipherText + AuthTag[16])`
- DataStore 存储加密后的密文

### NFR-004 可访问性
- 遵循系统字体缩放设置（`fontScale`）
- Dynamic Color 动态取色
- 深色模式独立色彩 token 体系

### NFR-005 兼容性
- Min SDK 33 (Android 13)
- Target SDK 37 (Android 17)
- `usesCleartextTraffic` 已废弃，内网地址通过 `network_security_config.xml` 显式允许

---

## 5. 验收指标汇总

| 指标 | 目标值 |
|------|--------|
| 编译成功率 | `./gradlew assembleDebug` 零错误 |
| 支持内容类型 | 4 种 (text/image/link/media) |
| 标签层级深度 | 无限（CTE 限制 50 层） |
| 搜索响应时间 | < 500ms (本地数据库) |
| AI 调用超时 | 连接 15s / 读取 60s |
| 加密算法 | AES-256-GCM |
| 数据导入模式 | 2 种 (覆盖/合并) |
