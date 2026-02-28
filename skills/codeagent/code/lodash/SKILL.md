---
name: lodash
description: "Lodash-es 工具函数 Skill 文档"
metadata:
  short-description: "Lodash-es 工具函数 Skill 文档"
---

# Lodash-es 工具函数 Skill 文档

## 概述

Lodash 是一个一致性、模块化、高性能的 JavaScript 实用工具库。`lodash-es` 是 Lodash 的 ES 模块化版本，支持按需引入。

## 安装

```bash
```

## 常用工具函数

### 数组操作

```typescript
import {
  chunk,
  compact,
  concat,
  difference,
  uniq,
  sortBy,
  groupBy,
  flatten,
  flattenDeep,
  zip,
} from 'lodash-es'

// 分块 - 将数组拆分成指定大小的块
chunk([1, 2, 3, 4, 5], 2) // [[1, 2], [3, 4], [5]]

// 去除假值 - 移除数组中的假值
compact([0, 1, false, 2, '', 3]) // [1, 2, 3]

// 合并数组
concat([1], [2], [3]) // [1, 2, 3]

// 差集 - 获取第一个数组中不包含在其他数组中的元素
difference([2, 1], [2, 3]) // [1]

// 去重 - 移除重复元素
uniq([1, 1, 2, 2, 3]) // [1, 2, 3]

// 排序
sortBy([{ name: 'fred', age: 48 }, { name: 'barney', age: 36 }], ['age'])
// [{ name: 'barney', age: 36 }, { name: 'fred', age: 48 }]

// 分组
groupBy(['one', 'two', 'three'], (item) => item.length)
// { '3': ['one', 'two'], '5': ['three'] }

// 扁平
flatten([1, [2, [3, [4]]]]) // [1, 2, 3, [4]]
flattenDeep([1, [2, [3, [4]]]]) // [1, 2, 3, 4]

// 配对
zip(['a', 'b'], [1, 2], [true, false]) // [['a', 1, true], ['b', 2, false]]
```

### 对象操作

```typescript
import {
  merge,
  cloneDeep,
  omit,
  pick,
  get,
  set,
  has,
  keys,
  values,
  fromPairs,
  toPairs,
} from 'lodash-es'

// 深度合并对象
merge({ a: 1 }, { b: 2 }) // { a: 1, b: 2 }
merge({}, { a: { b: { c: 3 } } }, { a: { d: 4 } })
// { a: { b: { c: 3, d: 4 } } }

// 深度克隆
cloneDeep({ a: 1, b: { c: 2 } })

// 省略属性
omit({ a: 1, b: 2, c: 3 }, ['a', 'c']) // { b: 2 }

// 选取属性
pick({ a: 1, b: 2, c: 3 }, ['a', 'c']) // { a: 1, c: 3 }

// 获取属性值
get({ a: { b: 2 } }, 'a.b') // 2

// 设置属性值
set({}, 'a.b', 3) // { a: { b: 3 } }

// 检查属性
has({ a: 1 }, 'a') // true

// 获取所有键
keys({ a: 1, b: 2 }) // ['a', 'b']

// 获取所有值
values({ a: 1, b: 2 }) // [1, 2]

// 转换
fromPairs([['a', 1], ['b', 2]]) // { a: 1, b: 2 }
toPairs({ a: 1, b: 2 }) // [['a', 1], ['b', 2]]
```

### 字符串操作

```typescript
import {
  camelCase,
  kebabCase,
  snakeCase,
  startCase,
  upperFirst,
  lowerFirst,
  trim,
  truncate,
} from 'lodash-es'

// 驼峰命名
camelCase('foo-bar') // 'fooBar'

// 短横线命名
kebabCase('fooBar') // 'foo-bar'

// 蛇形命名
snakeCase('fooBar') // 'foo_bar'

// 首字母大写
startCase('foo bar') // 'Foo Bar'

// 首字母大写
upperFirst('fred') // 'Fred'
lowerFirst('FRED') // 'fRED'

// 去除首尾空格
trim('  abc  ') // 'abc'

// 截断字符串
truncate('hi-diddly-ho', 5) // 'hi...'
```

### 数学和逻辑

```typescript
import {
  debounce,
  throttle,
  delay,
  random,
  cloneDeep,
  isEqual,
  isEmpty,
  isNull,
  isUndefined,
  isArray,
  isObject,
  isString,
} from 'lodash-es'

// 防抖 - 延迟执行
debounce(() => {
  console.log('Debounced')
}, 300)

// 节流 - 限制执行频率
throttle(() => {
  console.log('Throttled')
}, 300)

// 延迟
delay(1000).then(() => console.log('Delayed 1s'))

// 随机数
random(0, 5) // 0-5 之间的随机数

// 深度比较相等
isEqual({ a: 1 }, { a: 1 }) // true

// 检查为空
isEmpty({}) // true
isEmpty([]) // true
isEmpty(null) // true
isEmpty(undefined) // true
isEmpty('') // true
isEmpty(0) // true

// 类型检查
isNull(null) // true
isUndefined(undefined) // true
isArray([1, 2, 3]) // true
isObject({}) // true
isString('hello') // true
```

## Vue 3 集成

### 自动导入

```typescript
// vite.config.ts
import AutoImport from 'unplugin-auto-import/vite'

export default defineConfig({
  plugins: [
    AutoImport({
      imports: [
        'vue',
        'vue-router',
        'pinia',
        '@vueuse/core',
        // 自动导入 lodash-es 常用函数
        {
          lodash: [
            'debounce',
            'throttle',
            'cloneDeep',
            'merge',
            'isEmpty',
            'isEqual',
            'uniq',
            'sortBy',
            'groupBy',
            'pick',
            'omit',
            'get',
            'set',
          ],
        },
      ],
      dts: 'types/auto-imports.d.ts',
    }),
  ],
})
```

### 使用示例

```vue
<script setup>
import { debounce, throttle } from 'lodash-es'

// 防抖搜索
const search = debounce((query: string) => {
  console.log('Searching:', query)
}, 300)

// 节流滚动
const handleScroll = throttle(() => {
  console.log('Scrolling...')
}, 100)

// 生命周期
onUnmounted(() => {
  // 取消防抖和节流
})
</script>
```

## 性能优化建议

1. **按需引入**：只导入需要使用的函数
2. **Tree Shaking**：ES 模块支持更好的 Tree Shaking
3. **防抖节流**：合理使用 debounce 和 throttle 优化性能
4. **避免重复导入**：项目中统一使用 lodash-es
