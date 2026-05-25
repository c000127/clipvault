# ClipVault 测试计划与用例规范

**文档编号：** CV-TP-001  
**版本：** 1.0  
**日期：** 2026-05-25

---

## 1. 测试策略

### 1.1 测试范围

| 测试类型 | 范围 | 工具 |
|----------|------|------|
| 单元测试 | DAO、Repository、CryptoManager、AiService | JUnit 5 + Mockk |
| 集成测试 | Room 数据库、DataStore | AndroidX Test + Room Testing |
| UI 测试 | Compose Screen | Compose Testing |

### 1.2 测试环境
- JDK: OpenJDK 21
- Android SDK: API 37
- 测试框架: JUnit 5.10+, Mockk 1.13+, Turbine 1.0+

---

## 2. 测试用例

### 2.1 本地存储模块 (Room DAO)

#### TC-LS-001: ClipItem CRUD
```
前置条件: 空数据库
步骤:
1. insert(ClipItem(type="text", content="hello")) → 返回 id
2. getById(id) → 验证 content == "hello"
3. update(item.copy(note="updated")) → 验证 note == "updated"
4. delete(item) → 验证 getById 返回 null
预期结果: 全部通过
```

#### TC-LS-002: 全文搜索
```
前置条件: 插入 3 条记录 ("apple", "banana", "apple pie")
步骤:
1. search("apple") → 返回 2 条
2. search("banana") → 返回 1 条
3. search("xyz") → 返回 0 条
预期结果: 搜索结果正确
```

#### TC-LS-003: Tag 关联查询
```
前置条件: item(id=1), tag(id=10), item_tag(1, 10)
步骤:
1. getTagsByItemId(1) → 返回 [tag(id=10)]
2. getItemIdsByTagId(10) → 返回 [1]
3. delete(1, 10) → 验证关联已删除
预期结果: 关联操作正确
```

#### TC-LS-004: 分页查询
```
前置条件: 插入 50 条记录
步骤:
1. getAllPaged() → PagingSource 加载
2. 验证 pageSize=20 时首页 20 条
3. 验证按 createdAt DESC 排序
预期结果: 分页正确
```

### 2.2 标签树递归模块

#### TC-TT-001: CTE 递归查询
```
前置条件: root(1) → child(2) → grandchild(3)
步骤:
1. getTagTree(1) → 返回 [1, 2, 3]
2. getTagTree(2) → 返回 [2, 3]
3. getTagTree(3) → 返回 [3]
预期结果: 递归正确
```

#### TC-TT-002: 深度限制 50 层
```
前置条件: 构造 55 层嵌套 Tag 链
步骤:
1. getTagTree(root) → 最多返回 50 层
2. 验证 depth < 50 条件生效
预期结果: 无死循环，最多 50 层
```

#### TC-TT-003: 环形引用防护
```
前置条件: A(parent=null) → B(parent=A) → C(parent=B)
步骤:
1. moveTag(A, C) → 检测到 A 是 C 的祖先
2. 验证返回 Result.failure
3. 验证 parentId 未被修改
预期结果: 环形引用被阻止
```

#### TC-TT-004: 删除 Tag 重挂载
```
前置条件: A → B → C, item 关联 B
步骤:
1. deleteTagWithReparenting(B)
2. 验证 C.parentId == A
3. 验证 item_tags 中 B 的关联已删除
预期结果: 子节点上移一层
```

#### TC-TT-005: 按 Tag 过滤收藏
```
前置条件: tag(1) → tag(2), item(1) 关联 tag(2), item(2) 关联 tag(1)
步骤:
1. getItemsByTagWithChildren(1) → 返回 [item(1), item(2)]
2. getItemsByTagWithChildren(2) → 返回 [item(1)]
预期结果: 递归过滤正确
```

### 2.3 加密算法模块

#### TC-CR-001: 加密解密往返
```
步骤:
1. encrypt("test-api-key-12345") → 返回 Base64 字符串
2. decrypt(encrypted) → 返回 "test-api-key-12345"
预期结果: 明文一致
```

#### TC-CR-002: 空字符串处理
```
步骤:
1. encrypt("") → 返回 ""
2. decrypt("") → 返回 ""
预期结果: 无异常
```

#### TC-CR-003: 数据损坏处理
```
步骤:
1. decrypt("invalid-base64-data!!!") → 返回 ""
2. decrypt("dGVzdA==") → 返回 "" (长度不足)
预期结果: 无崩溃，返回空串
```

#### TC-CR-004: 密钥丢失恢复
```
步骤:
1. encrypt("data") → 正常加密
2. deleteKey() → 删除并重新生成密钥
3. decrypt(encrypted) → 返回 "" (旧密文无法解密)
4. encrypt("new-data") → 正常加密
5. decrypt(newEncrypted) → 返回 "new-data"
预期结果: 旧数据丢失，新数据正常
```

#### TC-CR-005: IV 唯一性
```
步骤:
1. encrypt("same-text") 两次
2. 验证两次结果不同 (IV 不同)
预期结果: 同明文不同密文
```

### 2.4 网络模块 (AiService)

#### TC-NW-001: Base URL 拼接
```
测试数据:
  "https://api.openai.com" → "https://api.openai.com/v1/chat/completions"
  "https://api.openai.com/" → "https://api.openai.com/v1/chat/completions"
  "https://api.openai.com/v1" → "https://api.openai.com/v1/chat/completions"
  "https://api.openai.com/v1/" → "https://api.openai.com/v1/chat/completions"
  "https://api.openai.com/v1/chat/completions" → 不变
预期结果: 全部正确拼接
```

#### TC-NW-002: 请求体构建
```
步骤:
1. provider(modelName="gpt-4o", temperature=0.7, maxTokens=4096)
2. buildRequestBody(provider, messages)
3. 验证 JSON 包含 "stream": false
4. 验证 model/messages/temperature/max_tokens 正确
预期结果: 请求体格式正确
```

#### TC-NW-003: 成功响应解析
```
步骤:
1. parseResponse('{"choices":[{"message":{"content":"{\"summary\":\"ok\",\"suggested_tags\":[\"a\"]}"}}]}')
2. 验证返回 AiResult.Success(summary="ok", suggestedTags=["a"])
预期结果: 解析正确
```

#### TC-NW-004: 非 JSON 响应降级
```
步骤:
1. parseResponse('{"choices":[{"message":{"content":"This is plain text"}}]}')
2. 验证返回 AiResult.Success(summary="This is plain text", suggestedTags=[])
预期结果: 降级处理正确
```

#### TC-NW-005: SSE 格式解析
```
步骤:
1. parseResponse("data: {...}\ndata: [DONE]")
2. 验证解析最后一个 data 块
预期结果: SSE 正确处理
```

#### TC-NW-006: HTTP 错误处理
```
测试数据:
  401 → "API Key 无效或已过期"
  429 → "请求频率过高"
  500 → "AI 服务暂时不可用"
预期结果: 错误消息正确
```

#### TC-NW-007: Vision 模式图片压缩
```
步骤:
1. 准备 2MB 图片文件
2. encodeImageBase64(path)
3. 验证 base64 长度 < 1MB 对应的字符数
预期结果: 压缩成功
```

### 2.5 Repository 层

#### TC-RP-001: API Key 加密存储
```
步骤:
1. saveApiKey(1, "sk-12345")
2. getApiKey(1) → 返回 "sk-12345"
3. 验证 DataStore 中存储的是加密后的密文
预期结果: 读写一致，存储已加密
```

#### TC-RP-002: Provider 激活切换
```
步骤:
1. insert(providerA), insert(providerB)
2. setActiveProvider(providerA.id)
3. getActiveProviderOnce() → providerA
4. setActiveProvider(providerB.id)
5. getActiveProviderOnce() → providerB
6. 验证 providerA.isActive == false
预期结果: 同时只有一个激活
```

#### TC-RP-003: 设置覆盖导入
```
步骤:
1. 插入原始数据 (3 items, 2 tags)
2. importOverwrite(ExportData(items=5, tags=3))
3. 验证数据库中只有导入的 5 items, 3 tags
预期结果: 原始数据被清除
```

#### TC-RP-004: 设置合并导入
```
步骤:
1. 插入 tag(name="Work", parentId=null)
2. importMerge(ExportData(tags=[Tag(name="Work")], items=[...]))
3. 验证 tag 未重复创建
4. 验证新 item 关联到已有 tag
预期结果: Tag 去重，关联重建
```

---

## 3. 测试执行标准

| 指标 | 目标 |
|------|------|
| 代码覆盖率 | ≥ 70% (DAO/Repository/CryptoManager) |
| 用例通过率 | 100% |
| 关键路径覆盖 | CRUD + 加密 + 递归查询 100% |
