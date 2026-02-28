<!--
工作台页面
-->
<template>
  <div class="dashboard-page">
    <h1 class="page-title">工作台</h1>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="stat in stats" :key="stat.title">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" :style="{ backgroundColor: stat.color }">
              <el-icon :size="24" :color="stat.iconColor">
                <component :is="stat.icon" />
              </el-icon>
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ stat.value }}</p>
              <p class="stat-title">{{ stat.title }}</p>
            </div>
          </div>
          <div class="stat-footer">
            <span :class="stat.trend > 0 ? 'trend-up' : 'trend-down'">
              {{ stat.trend > 0 ? '+' : '' }}{{ stat.trend }}%
            </span>
            <span class="trend-label">较上周</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="16">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>数据趋势</span>
              <el-button-group>
                <el-button
                  v-for="period in periods"
                  :key="period.value"
                  :type="selectedPeriod === period.value ? 'primary' : ''"
                  size="small"
                  @click="selectedPeriod = period.value"
                >
                  {{ period.label }}
                </el-button>
              </el-button-group>
            </div>
          </template>
          <chart-card :option="lineChartOption" height="300px" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>项目分布</span>
          </template>
          <chart-card :option="pieChartOption" height="300px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 3D 展示区域 -->
    <el-row :gutter="20" class="3d-row">
      <el-col :span="24">
        <el-card class="3d-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>3D 模型预览</span>
              <el-button size="small" @click="refresh3D">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <scene-3d :show-default="false" height="400px" @ready="onSceneReady" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近项目 -->
    <el-row :gutter="20" class="projects-row">
      <el-col :span="24">
        <el-card class="projects-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近项目</span>
              <el-button type="primary" size="small" @click="createProject">
                <el-icon><Plus /></el-icon>
                新建项目
              </el-button>
            </div>
          </template>
          <el-table :data="recentProjects" style="width: 100%">
            <el-table-column prop="name" label="项目名称" />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ row.status === 'active' ? '进行中' : '已完成' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" width="180" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button type="primary" link size="small">查看</el-button>
                <el-button link size="small">编辑</el-button>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  TrendCharts,
  Refresh,
  Plus,
} from '@element-plus/icons-vue'
import ChartCard from '@/components/charts/ChartCard.vue'
import type { EChartsCoreOption } from 'echarts/core'

// 类型别名
type EChartsOption = EChartsCoreOption

const router = useRouter()

// 统计数据
const stats = ref([
  {
    title: '总项目数',
    value: '128',
    icon: 'FolderOpened',
    color: 'rgba(59, 130, 246, 0.1)',
    iconColor: '#3b82f6',
    trend: 12,
  },
  {
    title: 'AI 生成代码',
    value: '3,456',
    icon: 'Document',
    color: 'rgba(34, 197, 94, 0.1)',
    iconColor: '#22c55e',
    trend: 8,
  },
  {
    title: '活跃用户',
    value: '89',
    icon: 'User',
    color: 'rgba(245, 158, 11, 0.1)',
    iconColor: '#f59e0b',
    trend: -3,
  },
  {
    title: 'API 调用',
    value: '12.5K',
    icon: 'TrendCharts',
    color: 'rgba(239, 68, 68, 0.1)',
    iconColor: '#ef4444',
    trend: 24,
  },
])

// 时间段选择
const periods = [
  { label: '今日', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
]
const selectedPeriod = ref('week')

// 折线图配置
const lineChartOption = ref<EChartsOption>({
  tooltip: {
    trigger: 'axis',
  },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  },
  yAxis: {
    type: 'value',
  },
  series: [
    {
      name: '代码生成',
      type: 'line',
      data: [120, 200, 150, 80, 70, 110, 130],
      smooth: true,
      itemStyle: { color: '#3b82f6' },
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
      name: 'API 调用',
      type: 'line',
      data: [220, 180, 200, 240, 180, 160, 200],
      smooth: true,
      itemStyle: { color: '#22c55e' },
    },
  ],
})

// 饼图配置
const pieChartOption = ref<EChartsOption>({
  tooltip: {
    trigger: 'item',
  },
  legend: {
    bottom: '5%',
    left: 'center',
  },
  series: [
    {
      name: '项目类型',
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
        { value: 45, name: 'Vue 项目', itemStyle: { color: '#3b82f6' } },
        { value: 30, name: 'React 项目', itemStyle: { color: '#22c55e' } },
        { value: 25, name: '其他', itemStyle: { color: '#f59e0b' } },
      ],
    },
  ],
})

// 最近项目
const recentProjects = ref([
  { name: '电商平台前端', type: 'Vue', status: 'active', updatedAt: '2024-01-20 14:30' },
  { name: '管理系统', type: 'React', status: 'completed', updatedAt: '2024-01-19 10:15' },
  { name: '数据可视化', type: 'Vue', status: 'active', updatedAt: '2024-01-18 16:45' },
  { name: '移动端 H5', type: 'Vue', status: 'completed', updatedAt: '2024-01-17 09:20' },
])

// 方法
function createProject() {
  router.push('/projects/new')
}

function refresh3D() {
  console.log('Refreshing 3D scene...')
}

function onSceneReady(scene: any) {
  console.log('3D scene ready:', scene)
}

onMounted(() => {
  // 加载数据
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.dark .page-title {
  color: #f3f4f6;
}

.stats-row {
  margin-bottom: 0;
}

.stat-card {
  height: 100%;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.dark .stat-value {
  color: #f3f4f6;
}

.stat-title {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.stat-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.dark .stat-footer {
  border-top-color: #374151;
}

.trend-up {
  color: #22c55e;
  font-size: 12px;
  font-weight: 500;
}

.trend-down {
  color: #ef4444;
  font-size: 12px;
  font-weight: 500;
}

.trend-label {
  font-size: 12px;
  color: #9ca3af;
}

.charts-row,
.projects-row,
.3d-row {
  margin-bottom: 0;
}

.chart-card,
.projects-card,
.3d-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 768px) {
  .page-title {
    font-size: 20px;
  }

  .stat-value {
    font-size: 20px;
  }
}
</style>
