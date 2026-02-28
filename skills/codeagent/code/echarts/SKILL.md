---
name: echarts
description: "ECharts 图表配置 Skill 文档"
metadata:
  short-description: "ECharts 图表配置 Skill 文档"
---

# ECharts 图表配置 Skill 文档

> 官方文档: https://echarts.apache.org/zh/option.html

## 概述

Apache ECharts 是一款基于 JavaScript 的数据可视化图表库，提供直观、生动、可交互、可个性化定制的数据可视化图表。

## 安装

```bash
```

## 基础配置

### 折线图 (Line Chart)

```vue
<template>
  <v-chart :option="lineOption" style="height: 400px" />
</template>

<script setup>
import { ref } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
} from 'echarts/components'

use([
  CanvasRenderer,
  LineChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
])

const lineOption = ref({
  title: {
    text: '折线图示例',
  },
  tooltip: {
    trigger: 'axis',
  },
  legend: {
    data: ['销量', '产量'],
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  },
  yAxis: {
    type: 'value',
  },
  series: [
    {
      name: '销量',
      type: 'line',
      data: [120, 200, 150, 80, 70, 110, 130],
      smooth: true,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0)' },
          ],
        },
      },
    },
    {
      name: '产量',
      type: 'line',
      data: [220, 180, 200, 240, 180, 160, 200],
      smooth: true,
    },
  ],
})
</script>
```

### 柱状图 (Bar Chart)

```javascript
const barOption = {
  title: {
    text: '柱状图示例',
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow',
    },
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
  },
  yAxis: {
    type: 'value',
  },
  series: [
    {
      name: '直接访问',
      type: 'bar',
      data: [320, 332, 301, 334, 390, 330, 320],
      itemStyle: {
        color: '#3b82f6',
      },
    },
    {
      name: '邮件营销',
      type: 'bar',
      data: [120, 132, 101, 134, 90, 230, 210],
      itemStyle: {
        color: '#22c55e',
      },
    },
  ],
}
```

### 饼图 (Pie Chart)

```javascript
const pieOption = {
  title: {
    text: '饼图示例',
    left: 'center',
  },
  tooltip: {
    trigger: 'item',
    formatter: '{a} <br/>{b}: {c} ({d}%)',
  },
  legend: {
    bottom: '5%',
    left: 'center',
  },
  series: [
    {
      name: '访问来源',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2,
      },
      label: {
        show: false,
        position: 'center',
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 20,
          fontWeight: 'bold',
        },
      },
      labelLine: {
        show: false,
      },
      data: [
        { value: 1048, name: '搜索引擎', itemStyle: { color: '#3b82f6' } },
        { value: 735, name: '直接访问', itemStyle: { color: '#22c55e' } },
        { value: 580, name: '邮件营销', itemStyle: { color: '#f59e0b' } },
        { value: 484, name: '联盟广告', itemStyle: { color: '#ef4444' } },
        { value: 300, name: '视频广告', itemStyle: { color: '#8b5cf6' } },
      ],
    },
  ],
}
```

### 散点图 (Scatter Chart)

```javascript
const scatterOption = {
  title: {
    text: '散点图示例',
  },
  tooltip: {
    trigger: 'item',
    formatter: function (params) {
      return `${params.seriesName}<br/>X: ${params.value[0]}, Y: ${params.value[1]}`
    },
  },
  xAxis: {
    scale: true,
  },
  yAxis: {
    scale: true,
  },
  series: [
    {
      name: '数据1',
      type: 'scatter',
      symbolSize: 10,
      data: [
        [161.2, 51.6],
        [167.5, 59.0],
        [159.5, 49.2],
        [157.0, 63.0],
        [155.8, 53.6],
      ],
    },
  ],
}
```

## 配置项详解

### 标题 (title)

```javascript
title: {
  text: '主标题',
  subtext: '副标题',
  left: 'center', // 'left', 'center', 'right'
  top: 'top',
  textStyle: {
    color: '#333',
    fontSize: 18,
    fontWeight: 'bold',
  },
}
```

### 提示框 (tooltip)

```javascript
tooltip: {
  trigger: 'item', // 'item', 'axis', 'none'
  axisPointer: {
    type: 'shadow', // 'line', 'shadow', 'cross', 'none'
  },
  formatter: (params) => {
    // 自定义提示内容
    return `${params.name}: ${params.value}`
  },
  backgroundColor: 'rgba(0, 0, 0, 0.8)',
  borderColor: 'transparent',
  textStyle: {
    color: '#fff',
  },
}
```

### 图例 (legend)

```javascript
legend: {
  data: ['系列1', '系列2'],
  orient: 'horizontal', // 'horizontal', 'vertical'
  left: 'center',
  top: 'top',
  itemWidth: 25,
  itemHeight: 14,
  textStyle: {
    color: '#333',
  },
  selected: {
    '系列1': true,
    '系列2': false,
  },
}
```

### 网格 (grid)

```javascript
grid: {
  left: '3%',
  right: '4%',
  top: '10%',
  bottom: '3%',
  containLabel: true,
  backgroundColor: '#f5f5f5',
  show: true,
  borderWidth: 1,
  borderColor: '#ccc',
}
```

### X 轴 (xAxis)

```javascript
xAxis: {
  type: 'category', // 'value', 'category', 'time', 'log'
  data: ['A', 'B', 'C', 'D'],
  axisLine: {
    lineStyle: {
      color: '#333',
    },
  },
  axisLabel: {
    color: '#666',
    rotate: 0,
  },
  splitLine: {
    show: false,
  },
}
```

### Y 轴 (yAxis)

```javascript
yAxis: {
  type: 'value',
  name: '数值',
  axisLabel: {
    formatter: '{value}',
  },
  splitLine: {
    lineStyle: {
      type: 'dashed',
    },
  },
}
```

### 系列列 (series)

```javascript
series: [
  {
    name: '系列名称',
    type: 'line', // 'line', 'bar', 'pie', 'scatter'
    data: [120, 200, 150, 80, 70, 110, 130],
    smooth: true,
    symbol: 'circle', // 'circle', 'rect', 'triangle', 'diamond'
    symbolSize: 6,
    itemStyle: {
      color: '#3b82f6',
    },
    lineStyle: {
      width: 2,
      type: 'solid', // 'solid', 'dashed', 'dotted'
    },
    areaStyle: {
      color: 'rgba(59, 130, 246, 0.3)',
    },
    label: {
      show: true,
      position: 'top',
    },
    emphasis: {
      focus: 'series',
    },
  },
]
```

## 交互配置

### 数据缩放 (dataZoom)

```javascript
dataZoom: [
  {
    type: 'slider',
    show: true,
    xAxisIndex: [0],
    start: 0,
    end: 100,
  },
  {
    type: 'inside',
    xAxisIndex: [0],
    start: 0,
    end: 100,
  },
]
```

### 工具栏 (toolbox)

```javascript
toolbox: {
  show: true,
  feature: {
    dataZoom: { yAxisIndex: 'none' },
    dataView: { readOnly: false },
    magicType: { type: ['line', 'bar'] },
    restore: {},
    saveAsImage: {},
  },
}
```

## 响应式配置

```javascript
const chartOption = {
  baseOption: {
    // 基础配置
  },
  media: [
    {
      query: {
        maxWidth: 768,
      },
      option: {
        legend: {
          right: 10,
          top: 'bottom',
        },
        series: [
          {
            id: 'bar',
            label: {
              show: false,
            },
          },
        ],
      },
    },
  ],
}
```

## 最佳实践

1. **按需引入**：只引入需要的图表类型和组件
2. **响应式**：使用 ResizeObserver 或 window.resize 监听尺寸变化
3. **主题定制**：通过主题对象统一样式风格
4. **性能优化**：大量数据时使用 dataZoom 进行数据抽样
5. **可访问性**：提供 ARIA 标签和键盘导航支持

## Vue 3 集成示例

```vue
<template>
  <v-chart
    ref="chartRef"
    :option="chartOption"
    :theme="theme"
    :init-options="{ renderer: 'canvas' }"
    :loading="loading"
    :loading-options="{ text: '加载中...', color: '#409eff' }"
    style="height: 400px"
    @click="handleChartClick"
    @ready="onChartReady"
  />
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'

use([CanvasRenderer, LineChart])

const chartRef = ref()
const loading = ref(false)
const theme = ref('default')

const chartOption = ref({
  // 配置
})

function handleChartClick(params) {
  console.log('图表点击', params)
}

function onChartReady(instance) {
  console.log('图表就绪', instance)
}

// 自动调整大小
let resizeObserver

onMounted(() => {
  resizeObserver = new ResizeObserver(() => {
    chartRef.value?.resize()
  })
  resizeObserver.observe(chartRef.value.$el)
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})
</script>
```
