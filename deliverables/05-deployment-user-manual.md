# ClipVault 部署发布与用户操作手册

**文档编号：** CV-DUM-001  
**版本：** 1.0  
**日期：** 2026-05-25

---

## 1. 无 GUI 环境编译部署指南

### 1.1 环境要求

| 组件 | 版本 | 安装方式 |
|------|------|----------|
| OS | Debian 13 (headless) | — |
| JDK | OpenJDK 21 | `apt install openjdk-21-jdk` |
| Android SDK | API 37 | `android sdk install "platforms;android-37.0"` |
| Gradle | 9.4 (wrapper) | 项目自带 `gradlew` |
| ADB | 37.0.0 | `android sdk install platform-tools` |

### 1.2 环境初始化

```bash
# 1. 配置 ANDROID_HOME
export ANDROID_HOME=/root/Android/Sdk
echo 'export ANDROID_HOME=/root/Android/Sdk' >> ~/.bashrc

# 2. 接受 SDK 许可证
mkdir -p $ANDROID_HOME/licenses
echo -e "\n24333f8a63b6825ea9c5514f83c2829b004d1fee" > $ANDROID_HOME/licenses/android-sdk-license

# 3. 安装必要组件
android sdk install "platforms;android-37.0"
android sdk install "build-tools;37.0.0"
android sdk install platform-tools
```

### 1.3 编译

```bash
# 进入项目目录
cd /root/clipvault/clipvault

# 编译 Debug APK
./gradlew assembleDebug

# 编译 Release APK (需要签名配置)
./gradlew assembleRelease

# APK 输出路径
ls -la app/build/outputs/apk/debug/app-debug.apk
```

### 1.4 部署到设备

```bash
# 检查设备连接
adb devices

# 安装 APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.clipvault.app/.MainActivity

# 查看日志
adb logcat -s "ClipVault"

# 卸载应用
adb uninstall com.clipvault.app
```

### 1.5 常见编译问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| SDK location not found | 缺少 local.properties | 创建 `local.properties` 写入 `sdk.dir=/root/Android/Sdk` |
| License not accepted | SDK 许可证未接受 | 执行许可证写入命令 |
| Package not found | 缺少平台组件 | `android sdk install "platforms;android-37.0"` |
| Configuration cache error | Gradle 缓存问题 | `./gradlew --no-configuration-cache assembleDebug` |

---

## 2. 权限配置说明

### 2.1 AndroidManifest.xml 声明的权限

| 权限 | 用途 | 运行时申请 |
|------|------|-----------|
| READ_MEDIA_IMAGES | 读取图片 (Android 13+) | 是 |
| READ_MEDIA_VIDEO | 读取视频 (Android 13+) | 是 |
| READ_MEDIA_AUDIO | 读取音频 (Android 13+) | 是 |
| CAMERA | 拍照收藏 | 是 |
| INTERNET | AI API 调用 | 否 |
| ACCESS_NETWORK_STATE | 网络状态检测 | 否 |

### 2.2 运行时权限流程

```
用户从外部分享图片/视频到 ClipVault
    ↓
NewItemActivity.onCreate()
    ↓
检查 mimeType → 图片/视频/音频类型
    ↓
hasMediaPermission(mimeType)?
    ├── 是 → 直接复制文件
    └── 否 → requestMediaPermission()
                ↓
         ActivityResultContracts.RequestMultiplePermissions
                ├── 授权 → proceedWithCopy()
                └── 拒绝 → Toast "需要媒体访问权限才能附加文件"
```

### 2.3 网络安全配置

`res/xml/network_security_config.xml` 允许明文流量的内网地址：
- `localhost` (含子域名)
- `127.0.0.1`
- `10.0.0.0`
- `192.168.0.0`

用于支持本地部署的 AI 服务（如 Ollama、LM Studio）。

---

## 3. 数据导入导出操作手册

### 3.1 导出数据

**路径：** 首页 → 设置 → Export Data

**操作步骤：**
1. 点击 "Export Data"
2. 系统弹出 SAF 文件选择器
3. 选择保存位置，输入文件名（默认 `clipvault_export.json`）
4. 点击 "保存"
5. 等待导出完成，显示 "Exported X items, Y tags"

**导出文件格式：**
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
      "thumbnailPath": "",
      "fetchedContent": "",
      "createdAt": 1716624000000,
      "updatedAt": 1716624000000,
      "sourceApp": ""
    }
  ],
  "tags": [
    { "id": 1, "name": "工作", "parentId": null, "createdAt": 1716624000000 },
    { "id": 2, "name": "项目A", "parentId": 1, "createdAt": 1716624000000 }
  ],
  "itemTags": [
    { "itemId": 1, "tagId": 2 }
  ]
}
```

### 3.2 导入数据

**路径：** 首页 → 设置 → Import Data

**操作步骤：**
1. 点击 "Import Data"
2. 系统弹出 SAF 文件选择器
3. 选择 JSON 文件
4. 弹出导入模式选择对话框

**导入模式：**

| 模式 | 行为 | 适用场景 |
|------|------|----------|
| 覆盖 (Overwrite) | 清空所有现有数据，导入新数据 | 恢复备份 |
| 合并 (Merge) | 保留现有数据，新数据追加，Tag 去重 | 多设备同步 |

**覆盖模式详细流程：**
1. 查询并删除所有现有 ClipItem
2. 查询并删除所有现有 Tag
3. 按原始 ID 批量插入 Tag
4. 按原始 ID 批量插入 ClipItem
5. 重建所有 ItemTag 关联
6. 全部操作在 `@Transaction` 中执行

**合并模式详细流程：**
1. 遍历导入的 Tag 列表
2. 按 `name + parentId` 查找已有 Tag
3. 已有 → 复用 ID；新建 → autoGenerate 新 ID
4. 建立 `oldTagId → newTagId` 映射表
5. 遍历导入的 ClipItem，autoGenerate 新 ID
6. 建立 `oldItemId → newItemId` 映射表
7. 根据映射表重建 ItemTag 关联

### 3.3 注意事项

- 导出文件包含所有收藏数据，请妥善保管
- 覆盖模式不可逆，建议先导出当前数据再操作
- 合并模式下，原始 ID 不会被保留
- 大文件导入可能需要几秒钟，请勿中途退出

---

## 4. AI 配置操作手册

### 4.1 添加 AI 提供商

**路径：** 首页 → 设置 → AI Settings → +

**必填字段：**

| 字段 | 示例 | 说明 |
|------|------|------|
| Name | My GPT | 自定义名称 |
| Base URL | https://api.openai.com/v1 | 支持末尾 `/v1` 或不带 |
| API Key | sk-xxx | 密文输入，存储时 AES-GCM 加密 |
| Model Name | gpt-4o-mini | 传递 exact model ID |

**可选字段：**

| 字段 | 默认值 | 说明 |
|------|--------|------|
| Supports Vision | 关 | 是否支持图片分析 |
| Max Tokens | 4096 | 最大输出 token |
| Temperature | 0.7 | 生成温度 |
| System Prompt | (见附录) | 可编辑 |

### 4.2 测试连接

1. 填写 Base URL、API Key、Model Name
2. 点击 "Test Connection"
3. 系统发送 `"Say 'Hello'"` 请求
4. 显示绿色 ✅ "Connection successful!" 或红色 ❌ 错误信息

### 4.3 激活配置

- 每个 Provider 卡片有 "Activate" 按钮
- 同时只有一个 Provider 处于激活状态
- 激活后所有 AI 调用使用此配置

### 4.4 AI 分析操作

**路径：** 收藏详情页 → AI 分析按钮

**流程：**
1. 读取当前激活的 AI 配置
2. 根据收藏类型构建请求：
   - text → 全文发送
   - image + vision → base64 图片发送
   - image - vision → 降级为文件名描述
   - link + 已抓取 → URL + 页面内容
   - link - 未抓取 → 仅 URL
3. 发送请求，显示加载状态
4. 解析响应：`{"summary": "...", "suggested_tags": [...]}`
5. 展示结果：总结文本 + 推荐 Tag（Chip 形式）
6. 用户确认后保存到 `note` 字段

---

## 5. 主题设置

**路径：** 首页 → 设置 → Theme

**选项：**
- **Follow System** — 跟随系统深色/浅色设置（默认）
- **Light** — 始终浅色模式
- **Dark** — 始终深色模式（OLED 纯黑背景）

**Dynamic Color：** 在支持的设备上自动提取壁纸主色调生成配色方案。

---

## 6. 快捷入口

### 6.1 ACTION_PROCESS_TEXT
1. 在任意应用长按选中文本
2. 点击 "⋯" 更多选项
3. 选择 "ClipVault"
4. 自动打开新建收藏页，文本已预填

### 6.2 ACTION_SEND (系统分享)
1. 在相册/浏览器/文件管理器选中内容
2. 点击分享按钮
3. 选择 "ClipVault"
4. 自动打开新建收藏页，内容已预填
