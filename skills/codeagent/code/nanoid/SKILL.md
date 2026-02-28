---
name: nanoid
description: "NanoID 唯一 ID 生成 Skill 文档"
metadata:
  short-description: "NanoID 唯一 ID 生成 Skill 文档"
---

# NanoID 唯一 ID 生成 Skill 文档

## 概述

NanoID 是一个小巧、安全、URL 友好的唯一字符串 ID 生成器，适用于各种场景（如数据库 ID、会话 ID 等）。

## 安装

```bash
```

## 基础用法

### 生成默认 ID

```typescript
import { nanoid } from 'nanoid'

// 默认：21 字符 URL 友好字符串
const id = nanoid() // 'V1StGXR8_Z5jdHi6B-myS'

// 自定义长度
const id = nanoid(10) // 'V1StGXR8_Z'

// 可自定义字符集
const id = nanoid(10, '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ')
```

## 自定义配置

### 自定义字符集

```typescript
import { customAlphabet } from 'nanoid'

// 创建自定义生成器
const nanoid = customAlphabet('0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ')

const id = nanoid() // '7K9F8Q5N2G3'
```

### 异步生成

```typescript
import { nanoid } from 'nanoid'

async function generateId() {
  const id = await nanoid()
  return id
}
```

## 常用场景

### 数据库 ID

```typescript
import { nanoid } from 'nanoid'

interface User {
  id: string
  name: string
}

const user: User = {
  id: nanoid(),
  name: '张三',
}
```

### 短 ID 生成

```typescript
import { customAlphabet } from 'nanoid'

// 10 位数字 ID（适用于验证码）
const nanoid10 = customAlphabet('0123456789', 10)
const code = nanoid10() // '5123948230'

// UUID v4 兼容格式
const uuid = () => nanoid().replace(/-/g, '').slice(0, 32)
```

### 文件名安全 ID

```typescript
import { nanoid } from 'nanoid'

// 生成文件名安全 ID
const fileId = nanoid(12)
const filename = `image_${fileId}.png`
```

## 安全性

- **唯一性**：极低的碰撞概率
- **不可预测**：使用加密强度高的随机数生成器
- **URL 友好**：默认使用 URL 友好字符集

## 性能

```typescript
import { nanoid } from 'nanoid'

// 性能测试（生成 10000 个 ID）
console.time('nanoid')
for (let i = 0; i < 10000; i++) {
  nanoid()
}
console.timeEnd('nanoid')
// 通常小于 50ms
```

## 替代方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| NanoID | 体积小、快速、安全 | 无 UUID 兼容性 |
| UUID v4 | 标准化、兼容性好 | 较长、无序、性能较差 |
| Math.random() | 简单 | 不安全、可能重复 |

## 最佳实践

1. **默认使用 nanoid()**：21 字符长度足够安全
2. **短场景使用 10-15 字符**：如临时 Token、验证码
3. **避免缩短到 8 字符以下**：碰撞概率显著增加
