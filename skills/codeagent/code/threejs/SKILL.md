---
name: threejs
description: "Three.js 3D 场景开发 Skill 文档"
metadata:
  short-description: "Three.js 3D 场景开发 Skill 文档"
---

# Three.js 3D 场景开发 Skill 文档

> 官方文档: https://threejs.org/docs/index.html

## 概述

Three.js 是一个基于 WebGL 的 JavaScript 3D 库，提供了简单易用的 API 来创建和显示 3D 图形。

## 安装

```bash
```

## 核心概念

### 场景 (Scene)

场景是所有 3D 对象的容器。

```javascript
import * as THREE from 'three'

const scene = new THREE.Scene()
scene.background = new THREE.Color(0x1e1e2e)
```

### 相机 (Camera)

相机定义了观察场景的视角。

```javascript
// 透视相机
const camera = new THREE.PerspectiveCamera(
  75, // 视野角度 (FOV)
  width / height, // 宽高比
  0.1, // 近裁剪面
  1000 // 远裁剪面
)
camera.position.set(5, 5, 5)
camera.lookAt(0, 0, 0)
```

### 渲染器 (Renderer)

渲染器负责将场景和相机渲染到画布上。

```javascript
const renderer = new THREE.WebGLRenderer({
  antialias: true, // 抗锯齿
  alpha: true, // 透明背景
})
renderer.setSize(width, height)
renderer.setPixelRatio(window.devicePixelRatio)
document.body.appendChild(renderer.domElement)
```

### 几何体 (Geometry)

几何体定义了 3D 对象的形状。

```javascript
// 立方体
const boxGeometry = new THREE.BoxGeometry(1, 1, 1)

// 球体
const sphereGeometry = new THREE.SphereGeometry(1, 32, 32)

// 圆柱体
const cylinderGeometry = new THREE.CylinderGeometry(1, 1, 2, 32)

// 圆环
const torusGeometry = new THREE.TorusGeometry(1, 0.3, 16, 100)

// 平面
const planeGeometry = new THREE.PlaneGeometry(10, 10)
```

### 材质 (Material)

材质定义了 3D 对象的外观。

```javascript
// 基础材质
const basicMaterial = new THREE.MeshBasicMaterial({ color: 0x3b82f6 })

// 标准材质（受光照影响）
const standardMaterial = new THREE.MeshStandardMaterial({
  color: 0x3b82f6,
  metalness: 0.3,
  roughness: 0.4,
})

// 物理材质（更真实）
const physicalMaterial = new THREE.MeshPhysicalMaterial({
  color: 0x3b82f6,
  metalness: 0.5,
  roughness: 0.1,
  clearcoat: 1.0,
  clearcoatRoughness: 0.1,
})

// 法线材质（显示法线方向）
const normalMaterial = new THREE.MeshNormalMaterial()
```

### 网格 (Mesh)

网格是几何体和材质的组合。

```javascript
const mesh = new THREE.Mesh(geometry, material)
mesh.position.set(0, 0, 0)
mesh.rotation.set(0, 0, 0)
mesh.scale.set(1, 1, 1)
scene.add(mesh)
```

## 光照

### 环境光 (Ambient Light)

```javascript
const ambientLight = new THREE.AmbientLight(0xffffff, 0.5)
scene.add(ambientLight)
```

### 方向光 (Directional Light)

```javascript
const directionalLight = new THREE.DirectionalLight(0xffffff, 1)
directionalLight.position.set(10, 10, 5)
scene.add(directionalLight)
```

### 点光源 (Point Light)

```javascript
const pointLight = new THREE.PointLight(0xffffff, 100)
pointLight.position.set(5, 5, 5)
scene.add(pointLight)
```

### 聚光灯 (Spot Light)

```javascript
const spotLight = new THREE.SpotLight(0xffffff, 100)
spotLight.position.set(5, 5, 5)
spotLight.angle = Math.PI / 6
spotLight.penumbra = 0.5
scene.add(spotLight)
```

## 控制器

### 轨道控制器 (OrbitControls)

```javascript
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'

const controls = new OrbitControls(camera, renderer.domElement)
controls.enableDamping = true
controls.dampingFactor = 0.05
controls.minDistance = 5
controls.maxDistance = 50
```

## 动画

```javascript
function animate() {
  requestAnimationFrame(animate)

  // 更新控制器
  controls.update()

  // 渲染场景
  renderer.render(scene, camera)
}

animate()
```

## 响应式调整

```javascript
function handleResize() {
  const width = window.innerWidth
  const height = window.innerHeight

  camera.aspect = width / height
  camera.updateProjectionMatrix()

  renderer.setSize(width, height)
}

window.addEventListener('resize', handleResize)
```

## 辅助工具

### 网格辅助 (GridHelper)

```javascript
const gridHelper = new THREE.GridHelper(20, 20)
scene.add(gridHelper)
```

### 坐标轴辅助 (AxesHelper)

```javascript
const axesHelper = new THREE.AxesHelper(5)
scene.add(axesHelper)
```

## 加载模型

### GLTF 加载器

```javascript
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js'

const loader = new GLTFLoader()
loader.load(
  'model.gltf',
  (gltf) => {
    scene.add(gltf.scene)
  },
  (progress) => {
    console.log((progress.loaded / progress.total) * 100 + '% loaded')
  },
  (error) => {
    console.error('An error happened', error)
  }
)
```

## Vue 3 集成示例

```vue
<template>
  <div ref="containerRef" class="canvas-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'

const props = defineProps({
  backgroundColor: {
    type: String,
    default: '#1e1e2e',
  },
  showControls: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits<{
  ready: [scene: THREE.Scene]
}>()

const containerRef = ref<HTMLElement>()

let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let controls: OrbitControls
let animationId: number

function initScene() {
  const width = containerRef.value!.clientWidth
  const height = containerRef.value!.clientHeight

  // 创建场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(props.backgroundColor)

  // 创建相机
  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000)
  camera.position.set(5, 5, 5)
  camera.lookAt(0, 0, 0)

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  containerRef.value!.appendChild(renderer.domElement)

  // 创建控制器
  if (props.showControls) {
    controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.dampingFactor = 0.05
  }

  // 添加光照
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.5)
  scene.add(ambientLight)

  const directionalLight = new THREE.DirectionalLight(0xffffff, 1)
  directionalLight.position.set(10, 10, 5)
  scene.add(directionalLight)

  // 添加辅助工具
  scene.add(new THREE.GridHelper(20, 20))
  scene.add(new THREE.AxesHelper(5))

  // 触发就绪事件
  emit('ready', scene)

  // 开始动画
  animate()
}

function animate() {
  animationId = requestAnimationFrame(animate)
  controls?.update()
  renderer.render(scene, camera)
}

function handleResize() {
  if (!containerRef.value) return

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  camera.aspect = width / height
  camera.updateProjectionMatrix()

  renderer.setSize(width, height)
}

onMounted(() => {
  initScene()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.canvas-container {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>
```

## 最佳实践

1. **资源释放**：在组件卸载时释放几何体、材质等资源
2. **性能优化**：使用 InstancedMesh 处理大量相同对象
3. **阴影优化**：仅对需要的对象启用阴影
4. **纹理压缩**：使用压缩纹理减少内存占用
5. **防抖处理**：resize 事件使用防抖避免频繁重绘
