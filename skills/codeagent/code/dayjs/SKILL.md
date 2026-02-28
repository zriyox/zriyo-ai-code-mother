---
name: dayjs
description: "Day.js 日期处理 Skill 文档"
metadata:
  short-description: "Day.js 日期处理 Skill 文档"
---

# Day.js 日期处理 Skill 文档

> 官方文档: https://day.js.org/docs/en/

## 概述

Day.js 是一个轻量级（仅 2KB）的 JavaScript 日期处理库，提供与 Moment.js 兼容的 API，具有现代化设计、不可变性和链式调用等特点。

## 安装

```bash
```

## 基础用法

### 解析日期

```javascript
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn' // 中文语言包

// 当前时间
const now = dayjs()

// 解析字符串
dayjs('2024-01-20')
dayjs('2024-01-20 15:30:00')

// 解析时间戳
dayjs(1705746800000)

// 指定格式解析
dayjs('20-01-2024', 'DD-MM-YYYY')
```

### 格式化日期

```javascript
// 默认格式
dayjs().format() // '2024-01-20T12:00:00+08:00'

// 自定义格式
dayjs().format('YYYY-MM-DD HH:mm:ss') // '2024-01-20 12:00:00'
dayjs().format('YYYY年MM月DD日') // '2024年01月20日'

// 常用格式
dayjs().format('YYYY-MM-DD') // '2024-01-20'
dayjs().format('HH:mm:ss') // '12:00:00'
dayjs().format('ddd') // '周五'
dayjs().format('MMMM') // '一月'
```

## 日期操作

### 增减时间

```javascript
// 添加
dayjs().add(1, 'day') // 加一天
dayjs().add(1, 'week') // 加一周
dayjs().add(1, 'month') // 加一个月
dayjs().add(1, 'year') // 加一年

// 减少
dayjs().subtract(1, 'day') // 减一天
dayjs().subtract(7, 'week') // 减一周

// 链式调用
dayjs().add(1, 'day').subtract(1, 'month')
```

### 开始/结束时间

```javascript
// 本月开始
dayjs().startOf('month')
dayjs().startOf('week')
dayjs().startOf('day')

// 本月结束
dayjs().endOf('month')
dayjs().endOf('week')
dayjs().endOf('day')
```

## 获取信息

### 日期部分

```javascript
const date = dayjs('2024-01-20')

date.year() // 2024
date.month() // 0 (0-11, 0 = 一月)
date.date() // 20 (1-31)
date.day() // 6 (0-6, 0 = 周日)

date.hour() // 12 (0-23)
date.minute() // 30 (0-59)
date.second() // 0 (0-59)

date.daysInMonth() // 31 (当月天数)
```

### 比较

```javascript
// 比较之前
dayjs().isBefore(dayjs('2024-12-31'))

// 比较之后
dayjs().isAfter(dayjs('2024-01-01'))

// 是否相同
dayjs().isSame(dayjs('2024-01-20'), 'day') // 比较天
dayjs().isSame(dayjs('2024-01-20'), 'month') // 比较月
```

## 差值计算

```javascript
// 时间差
dayjs().diff('2024-01-20') // 相差毫秒数
dayjs().diff('2024-01-20', 'day') // 相差天数
dayjs().diff('2024-01-20', 'week') // 相差周数
dayjs().diff('2024-01-20', 'month') // 相差月数

// 毫秒差
dayjs().diff(date, 'millisecond')

// Unix 时间戳
dayjs().valueOf() // 1705746800000
dayjs().unix() // 1705746800
```

## 插件使用

### UTC 插件

```javascript
import utc from 'dayjs/plugin/utc'
import dayjs from 'dayjs'

dayjs.extend(utc)

// UTC 时间
dayjs().utc().format()
dayjs().utcOffset('+08:00').format()
```

### 相对时间插件

```javascript
import relativeTime from 'dayjs/plugin/relativeTime'
import dayjs from 'dayjs'

dayjs.extend(relativeTime)

dayjs().to(dayjs('2024-01-20')) // '3 天前'
dayjs().from(dayjs('2024-01-20')) // '3 天后'
```

### 时区插件

```javascript
import timezone from 'dayjs/plugin/timezone'
import dayjs from 'dayjs'

dayjs.extend(timezone)

dayjs().tz('Asia/Shanghai').format()
```

### 持续时间插件

```javascript
import duration from 'dayjs/plugin/duration'
import dayjs from 'dayjs'

dayjs.extend(duration)

const duration = dayjs.duration(2, 'days')
duration.asHours() // 48
duration.asMinutes() // 2880
```

### 更新语言

```javascript
import 'dayjs/locale/zh-cn'
import updateLocale from 'dayjs/plugin/updateLocale'
import dayjs from 'dayjs'

dayjs.extend(updateLocale)

dayjs.locale('zh-cn')

dayjs().format('MMMM') // '一月'
```

## Vue 3 集成

### Composable

```typescript
// composables/useDayjs.ts
import { computed } from 'vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export function useDayjs(date?: string | Date) {
  const currentDate = computed(() => dayjs(date))

  const formatted = computed(() => currentDate.value.format('YYYY-MM-DD HH:mm:ss'))

  const relative = computed(() => currentDate.value.fromNow())

  return {
    currentDate,
    formatted,
    relative,
  }
}

// 使用
const { formatted, relative } = useDayjs('2024-01-20')
```

## 最佳实践

1. **不可变性**：Day.js 对象是不可变的，每次操作返回新对象
2. **链式调用**：充分利用链式调用简化代码
3. **本地化**：使用语言包支持本地化日期显示
4. **插件按需加载**：只引入需要的插件减小打包体积
5. **时区处理**：使用 UTC 插件处理跨时区问题
