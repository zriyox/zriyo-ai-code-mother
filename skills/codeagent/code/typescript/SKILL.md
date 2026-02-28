---
name: typescript
description: "TypeScript 使用 Skill 文档"
metadata:
  short-description: "TypeScript 使用 Skill 文档"
---

# TypeScript 使用 Skill 文档

> 官方文档: https://www.typescriptlang.org/docs/

## 概述

TypeScript 是 JavaScript 的超集，为其添加了静态类型定义。TypeScript 通过类型注解提供编译时类型检查，使代码更加健壮。

## 基础类型

### 原始类型

```typescript
// 字符串
let name: string = '张三'
const template: string = `Hello ${name}`

// 数字
let age: number = 25
const height: number = 1.75

// 布尔值
let isActive: boolean = true
const isDone: boolean = false

// 数组
let numbers: number[] = [1, 2, 3, 4, 5]
let strings: string[] = ['a', 'b', 'c']
const matrix: number[][] = [[1, 2], [3, 4]]

// 元组
let tuple: [string, number] = ['hello', 42]

// 枚举
enum Direction {
  Up,
  Down,
  Left,
  Right,
}
```

### 特殊类型

```typescript
// any: 任意类型（避免使用）
let anything: any = 'anything'

// unknown: 类型安全的 any
let value: unknown = 'safe'

// void: 无返回值
function log(message: string): void {
  console.log(message)
}

// never: 永不返回
function error(message: string): never {
  throw new Error(message)
}

// null 和 undefined
let nothing: null = null
let notDefined: undefined = undefined
```

## 接口 (Interfaces)

### 基础接口

```typescript
interface User {
  id: number | string
  username: string
  nickname?: string // 可选属性
  readonly email: string // 只读属性
}

const user: User = {
  id: 1,
  username: 'zhangsan',
  email: 'zhangsan@example.com',
}
```

### 函数类型接口

```typescript
interface SearchFunc {
  (source: string, subString: string): boolean
}

const search: SearchFunc = (source, subString) => {
  return source.includes(subString)
}
```

### 可索引签名

```typescript
interface StringArray {
  [index: number]: string
  [key: string]: string | number
}
```

### 类型扩展

```typescript
interface Animal {
  name: string
}

interface Bear extends Animal {
  honey: boolean
}
```

## 类型别名 (Type Aliases)

```typescript
// 基本别名
type ID = number | string

// 联合类型
type Status = 'pending' | 'success' | 'error'

// 函数类型
type EventHandler = (event: Event) => void

// 泛型别名
type Container<T> = { value: T }
```

## 函数

### 函数声明

```typescript
function add(a: number, b: number): number {
  return a + b
}

// 可选参数
function greet(name: string, greeting?: string): string {
  return `${greeting || 'Hello'}, ${name}!`
}

// 剩余参数
function sum(...numbers: number[]): number {
  return numbers.reduce((acc, val) => acc + val, 0)
}
```

### 箭头函数

```typescript
const multiply = (a: number, b: number): number => a * b

// 类型推断
const divide = (a: number, b: number) => a / b
```

## 泛型 (Generics)

### 泛型函数

```typescript
function identity<T>(arg: T): T {
  return arg
}

// 显式指定类型
const output = identity<string>('hello')

// 类型推断
const num = identity(42)
```

### 泛型接口

```typescript
interface Box<T> {
  value: T
}

const stringBox: Box<string> = { value: 'hello' }
const numberBox: Box<number> = { value: 42 }
```

### 泛型约束

```typescript
interface Lengthwise {
  length: number
}

function logLength<T extends Lengthwise>(arg: T): void {
  console.log(arg.length)
}
```

## 类型断言

```typescript
// 尖括号语法
let value: any = 'Hello World'
let strLength: number = (<string>value).length

// as 语法
let someValue: unknown = 'Hello'
let strLen: number = (someValue as string).length

// const 断言
const canvas = document.getElementById('canvas') as HTMLCanvasElement
```

## 类型守卫

### typeof 类型守卫

```typescript
function printAll(strs: string | string[]) {
  if (typeof strs === 'object') {
    for (const s of strs) {
      console.log(s)
    }
  } else {
    console.log(strs)
  }
}
```

### instanceof 类型守卫

```typescript
function logValue(value: Date | string[]) {
  if (value instanceof Date) {
    console.log(value.toISOString())
  } else {
    value.forEach(s => console.log(s))
  }
}
```

### 自定义类型守卫

```typescript
function isString(value: unknown): value is string {
  return typeof value === 'string'
}

function process(value: unknown) {
  if (isString(value)) {
    console.log(value.toUpperCase())
  }
}
```

## 类型收窄

### 可辨识联合

```typescript
interface Circle {
  kind: 'circle'
  radius: number
}

interface Square {
  kind: 'square'
  sideLength: number
}

type Shape = Circle | Square

function getArea(shape: Shape): number {
  if (shape.kind === 'circle') {
    return Math.PI * shape.radius * shape.radius
  }
  return shape.sideLength * shape.sideLength
}
```

### 等值收窄

```typescript
function example(x: string | number, y: string | boolean) {
  if (x === y) {
    // x 和 y 都是 string
    x.toUpperCase()
    y.toUpperCase()
  }
}
```

## 类 (Classes)

### 类定义

```typescript
class Person {
  // 属性声明
  name: string
  age: number

  // 构造函数
  constructor(name: string, age: number) {
    this.name = name
    this.age = age
  }

  // 方法
  greet(): string {
    return `Hello, I'm ${this.name}`
  }

  // 存取器
  get fullName(): string {
    return `${this.name} (${this.age} years old)`
  }
}
```

### 修饰符

```typescript
class Employee {
  // public: 公共属性（默认）
  public name: string

  // private: 私有属性
  private salary: number

  // protected: 受保护属性
  protected department: string

  // readonly: 只读属性
  readonly id: number
}
```

### 抽象类

```typescript
abstract class Animal {
  abstract makeSound(): void

  move(): void {
    console.log('Moving...')
  }
}

class Dog extends Animal {
  makeSound(): void {
    console.log('Woof!')
  }
}
```

## 模块

### 导出

```typescript
// 默认导出
export default class Calculator {}

// 命名导出
export const PI = 3.14

// 类型导出
export interface User {
  id: number
}
```

### 导入

```typescript
// 默认导入
import Calculator from './calculator'

// 命名导入
import { PI } from './constants'

// 类型导入
import type { User } from './types'

// 命空间导入
import * as utils from './utils'
```

## 类型推断

```typescript
// 变量推断
let count = 0 // 推断为 number
count = 'string' // 错误：Type 'string' is not assignable to type 'number'

// 最佳通用类型
let arr = [0, 1, null] // 推断为 (number | null)[]
```

## 实用工具类型

```typescript
// Partial<T>: 所有属性变为可选
type PartialUser = Partial<User>

// Required<T>: 所有属性变为必需
type RequiredUser = Required<PartialUser>

// Readonly<T>: 所有属性变为只读
type ReadonlyUser = Readonly<User>

// Record<K, T>: 构造对象类型
type UserMap = Record<string, User>

// Pick<T, K>: 选择部分属性
type UserName = Pick<User, 'username'>

// Omit<T, K>: 排除部分属性
type UserWithoutEmail = Omit<User, 'email'>

// Exclude<T, U>: 排除联合类型中的某些类型
type T = Exclude<'a' | 'b', 'a'> // 'b'

// Extract<T, U>: 提取联合类型中的某些类型
type T = Extract<'a' | 'b', 'a'> // 'a'

// ReturnType<T>: 获取函数返回值类型
type Return = ReturnType<typeof fetch>

// Parameters<T>: 获取函数参数类型
type Params = Parameters<typeof console.log>
```

## Vue 3 + TypeScript

### Props 类型定义

```vue
<script setup lang="ts">
interface Props {
  title: string
  count?: number
  items: string[]
}

const props = withDefaults(defineProps<Props>(), {
  count: 0,
  items: () => [],
})
</script>
```

### Emits 类型定义

```vue
<script setup lang="ts">
const emit = defineEmits<{
  change: [value: string]
  submit: [event: Event]
}>()
</script>
```

### Ref 类型定义

```vue
<script setup lang="ts">
import { ref } from 'vue'

// 简单类型
const count = ref(0)

// 复杂类型
interface User {
  name: string
  age: number
}
const user = ref<User>({ name: 'Alice', age: 30 })

// 泛型 ref
const list = ref<string[]>([])
</script>
```

### Reactive 类型定义

```vue
<script setup lang="ts">
import { reactive } from 'vue'

interface State {
  count: number
  user: User | null
}

const state = reactive<State>({
  count: 0,
  user: null,
})
</script>
```

## 配置文件

### tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,

    /* Bundler mode */
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",

    /* Linting */
    "strict": true,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "noFallthroughCasesInSwitch": true,

    /* Path mapping */
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  }
}
```

## 最佳实践

1. **开启严格模式**：使用 `strict: true` 获得完整的类型检查
2. **避免 any**：优先使用 `unknown` 代替 `any`
3. **使用接口**：为对象定义清晰的接口
4. **类型推断**：让 TypeScript 自动推断类型，减少冗余
5. **泛型约束**：使用泛型约束确保类型安全
6. **工具类型**：充分利用 Partial、Pick、Omit 等工具类型
