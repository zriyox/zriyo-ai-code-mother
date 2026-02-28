<!--
3D 场景容器组件
使用原生 Three.js 实现
-->
<template>
  <div ref="containerRef" class="canvas-container">
    <!-- 3D 渲染容器 -->
    <div ref="canvasRef" class="canvas-3d" :style="{ height: computedHeight }"></div>

    <!-- 控制按钮 -->
    <div v-if="showControls" class="canvas-controls">
      <button class="control-btn" title="重置视角" @click="resetCamera">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74-2.74L3 12" />
          <path d="M3 3v9h9" />
        </svg>
      </button>
      <button class="control-btn" title="全屏" @click="toggleFullscreen">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 6h3m-3 0v3m0 0L8 21" />
        </svg>
      </button>
      <button class="control-btn" :title="isPlaying ? '暂停' : '播放'" @click="toggleAnimation">
        <svg v-if="!isPlaying" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
          <polygon points="5,3 19,12 5,21 12,19 19,5 5,3" />
        </svg>
        <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="4" width="4" height="16" />
          <rect x="14" y="4" width="4" height="16" />
        </svg>
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="canvas-loading">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'

interface Props {
  showControls?: boolean
  backgroundColor?: string
  height?: string | number
}

const props = withDefaults(defineProps<Props>(), {
  showControls: true,
  backgroundColor: '#1e1e2e',
  height: '400px',
})

const emit = defineEmits<{
  ready: [scene: THREE.Scene]
}>()

const containerRef = ref<HTMLElement>()
const canvasRef = ref<HTMLElement>()
const loading = ref(true)
const isPlaying = ref(true)

// Three.js 核心对象
let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let controls: OrbitControls
let animationId: number

// 计算高度
const computedHeight = computed(() => {
  return typeof props.height === 'number' ? `${props.height}px` : props.height
})

// 初始化场景
function initScene() {
  const width = canvasRef.value!.clientWidth
  const height = canvasRef.value!.clientHeight

  // 创建场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(props.backgroundColor)

  // 创建相机
  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000)
  camera.position.set(5, 5, 5)
  camera.lookAt(0, 0, 0)

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({
    antialias: true,
    alpha: true,
  })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  canvasRef.value!.appendChild(renderer.domElement)

  // 创建控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05

  // 添加环境光
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.5)
  scene.add(ambientLight)

  // 添加方向光
  const directionalLight = new THREE.DirectionalLight(0xffffff, 1)
  directionalLight.position.set(10, 10, 5)
  scene.add(directionalLight)

  // 添加网格辅助
  const gridHelper = new THREE.GridHelper(20, 20)
  scene.add(gridHelper)

  // 添加坐标轴辅助
  const axesHelper = new THREE.AxesHelper(5)
  scene.add(axesHelper)

  // 触发就绪事件
  emit('ready', scene)
  loading.value = false

  // 开始动画
  animate()
}

// 动画循环
function animate() {
  animationId = requestAnimationFrame(animate)

  if (isPlaying.value) {
    // 可以在这里添加自定义动画逻辑
  }

  controls.update()
  renderer.render(scene, camera)
}

// 重置相机
function resetCamera() {
  camera.position.set(5, 5, 5)
  camera.lookAt(0, 0, 0)
  controls.reset()
}

// 切换全屏
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    containerRef.value?.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 切换动画
function toggleAnimation() {
  isPlaying.value = !isPlaying.value
}

// 响应式调整
function handleResize() {
  if (!canvasRef.value) return

  const width = canvasRef.value.clientWidth
  const height = canvasRef.value.clientHeight

  camera.aspect = width / height
  camera.updateProjectionMatrix()

  renderer.setSize(width, height)
}

onMounted(() => {
  initScene()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', handleResize)
})

// 监听高度变化
watch(() => props.height, () => {
  if (canvasRef.value) {
    const height = typeof props.height === 'number' ? props.height : parseInt(props.height)
    canvasRef.value.style.height = `${height}px`
    handleResize()
  }
})
</script>

<style scoped>
.canvas-container {
  width: 100%;
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  background: linear-gradient(135deg, #1e1e2e 0%, #2d2d44 100%);
}

.canvas-3d {
  width: 100%;
}

.canvas-controls {
  position: absolute;
  bottom: 20px;
  right: 20px;
  display: flex;
  gap: 10px;
  z-index: 10;
}

.control-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.3s;
}

.control-btn:hover {
  background-color: rgba(255, 255, 255, 0.2);
  transform: scale(1.05);
}

.canvas-loading {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(0, 0, 0, 0.5);
  z-index: 20;
  border-radius: 12px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
