<!--
图表卡片组件
封装 ECharts Vue 组件
-->
<template>
  <div ref="chartRef" class="chart-card" :style="{ height: computedHeight }">
    <div v-if="loading" class="chart-loading">
      <div class="loading-spinner"></div>
    </div>
    <div v-else-if="isEmpty" class="chart-empty">
      <slot name="empty">
        <EmptyState text="暂无图表数据" />
      </slot>
    </div>
    <v-chart
      v-else
      :option="mergedOption"
      :theme="theme"
      :init-options="{ renderer: renderer }"
      :loading="chartLoading"
      @click="handleChartClick"
    />
    <!-- 图表工具栏 -->
    <div v-if="showToolbar" class="chart-toolbar">
      <button
        v-for="tool in toolbar"
        :key="tool.name"
        class="toolbar-btn"
        :title="tool.title"
        @click="tool.handler"
      >
        <component :is="tool.icon" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import {
  CanvasRenderer,
  SVGRenderer,
} from 'echarts/renderers'
import {
  LineChart,
  BarChart,
  PieChart,
  ScatterChart,
  RadarChart,
  GaugeChart,
  FunnelChart,
} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  ToolboxComponent,
  MarkLineComponent,
  MarkPointComponent,
} from 'echarts/components'
import type { EChartsCoreOption } from 'echarts/core'

// 类型别名
type EChartsOption = EChartsCoreOption
import EmptyState from '@/components/common/EmptyState.vue'

// 注册 ECharts 组件
use([
  CanvasRenderer,
  SVGRenderer,
  LineChart,
  BarChart,
  PieChart,
  ScatterChart,
  RadarChart,
  GaugeChart,
  FunnelChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  ToolboxComponent,
  MarkLineComponent,
  MarkPointComponent,
])

interface Props {
  option: EChartsOption
  height?: string | number
  loading?: boolean
  empty?: boolean
  theme?: string | object
  renderer?: 'canvas' | 'svg'
  autoResize?: boolean
  showToolbar?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  height: '400px',
  loading: false,
  empty: false,
  theme: '',
  renderer: 'canvas',
  autoResize: true,
  showToolbar: false,
})

const emit = defineEmits<{
  click: [params: any]
  ready: [instance: any]
}>()

const chartRef = ref<HTMLElement>()
const chartLoading = ref(false)
const isEmpty = computed(() => props.empty)

// 计算高度
const computedHeight = computed(() => {
  return typeof props.height === 'number' ? `${props.height}px` : props.height
})

// 合并默认配置
const mergedOption = computed<EChartsOption>(() => {
  const defaultOption: EChartsOption = {
    tooltip: {
      trigger: 'axis',
      confine: true,
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: 'transparent',
      textStyle: {
        color: '#fff',
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
  }

  return {
    ...defaultOption,
    ...props.option,
  }
})

// 工具栏配置
const toolbar = computed(() => [
  {
    name: 'refresh',
    title: '刷新',
    icon: 'Refresh',
    handler: () => {
      chartLoading.value = true
      setTimeout(() => {
        chartLoading.value = false
      }, 300)
    },
  },
  {
    name: 'download',
    title: '下载图片',
    icon: 'Download',
    handler: downloadChart,
  },
])

// 下载图表
function downloadChart() {
  const chart = chartRef.value?.querySelector('canvas')?.closest('.chart-card')
  if (chart) {
    // 实现下载逻辑
    console.log('Downloading chart...')
  }
}

function handleChartClick(params: any) {
  emit('click', params)
}

// 自动调整大小
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (props.autoResize && chartRef.value) {
    resizeObserver = new ResizeObserver(() => {
      // 图表会自动调整
    })
    resizeObserver.observe(chartRef.value)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.chart-card {
  position: relative;
  width: 100%;
  background-color: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.dark .chart-card {
  background-color: #1f2937;
}

.chart-loading,
.chart-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 200px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(0, 0, 0, 0.1);
  border-top-color: var(--primary-color, #3b82f6);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.chart-toolbar {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 8px;
}

.toolbar-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background-color: rgba(0, 0, 0, 0.05);
  color: #6b7280;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.2s;
}

.toolbar-btn:hover {
  background-color: rgba(0, 0, 0, 0.1);
  color: #374151;
}

.dark .toolbar-btn {
  background-color: rgba(255, 255, 255, 0.1);
  color: #9ca3af;
}

.dark .toolbar-btn:hover {
  background-color: rgba(255, 255, 255, 0.2);
  color: #f3f4f6;
}
</style>
