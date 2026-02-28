<!--
设置页面
-->
<template>
  <div class="settings-page">
    <h1 class="page-title">系统设置</h1>

    <el-row :gutter="20">
      <el-col :xs="24" :md="16">
        <!-- 通用设置 -->
        <el-card class="settings-card" shadow="never">
          <template #header>
            <span>通用设置</span>
          </template>
          <el-form label-width="120px">
            <el-form-item label="系统名称">
              <el-input v-model="settings.appName" />
            </el-form-item>
            <el-form-item label="语言">
              <el-radio-group v-model="settings.language">
                <el-radio-button label="zh">中文</el-radio-button>
                <el-radio-button label="en">English</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="主题">
              <el-radio-group v-model="settings.theme">
                <el-radio-button label="light">浅色</el-radio-button>
                <el-radio-button label="dark">深色</el-radio-button>
                <el-radio-button label="auto">跟随系统</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- API 设置 -->
        <el-card class="settings-card" shadow="never">
          <template #header>
            <span>API 配置</span>
          </template>
          <el-form label-width="120px">
            <el-form-item label="Java API">
              <el-input v-model="settings.javaApi" placeholder="http://localhost:8080" />
            </el-form-item>
            <el-form-item label="Python API">
              <el-input v-model="settings.pythonApi" placeholder="http://localhost:8000" />
            </el-form-item>
            <el-form-item>
              <el-button @click="testConnection">测试连接</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <!-- 快捷操作 -->
        <el-card class="settings-card" shadow="never">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <el-button class="action-btn" @click="clearCache">
              <el-icon><Delete /></el-icon>
              清除缓存
            </el-button>
            <el-button class="action-btn" @click="exportData">
              <el-icon><Download /></el-icon>
              导出数据
            </el-button>
            <el-button type="danger" class="action-btn" @click="resetSettings">
              <el-icon><RefreshLeft /></el-icon>
              重置设置
            </el-button>
          </div>
        </el-card>

        <!-- 系统信息 -->
        <el-card class="settings-card" shadow="never">
          <template #header>
            <span>系统信息</span>
          </template>
          <div class="system-info">
            <p><span class="label">版本:</span> {{ version }}</p>
            <p><span class="label">构建时间:</span> {{ buildTime }}</p>
            <p><span class="label">环境:</span> {{ environment }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Download, RefreshLeft } from '@element-plus/icons-vue'
import { pingPython } from '@/api/python'

const settings = ref({
  appName: 'AI Generated App',
  language: 'zh',
  theme: 'light',
  javaApi: 'http://localhost:8080',
  pythonApi: 'http://localhost:8000',
})

const version = __APP_VERSION__
const buildTime = new Date(__BUILD_TIME__).toLocaleString('zh-CN')
const environment = import.meta.env.MODE

function saveSettings() {
  localStorage.setItem('app-settings', JSON.stringify(settings.value))
  ElMessage.success('设置已保存')
}

function clearCache() {
  localStorage.clear()
  sessionStorage.clear()
  ElMessage.success('缓存已清除')
  setTimeout(() => {
    location.reload()
  }, 500)
}

function exportData() {
  // TODO: 实现数据导出
  ElMessage.info('导出功能开发中')
}

function resetSettings() {
  localStorage.removeItem('app-settings')
  ElMessage.success('设置已重置')
  setTimeout(() => {
    location.reload()
  }, 500)
}

async function testConnection() {
  try {
    const result = await pingPython()
    if (result.pong) {
      ElMessage.success('Python 服务连接正常')
    }
  } catch (error) {
    ElMessage.error('连接失败，请检查 API 配置')
  }
}

onMounted(() => {
  const saved = localStorage.getItem('app-settings')
  if (saved) {
    try {
      Object.assign(settings.value, JSON.parse(saved))
    } catch (e) {
      console.error('Failed to parse settings:', e)
    }
  }
})
</script>

<style scoped>
.settings-page {
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

.settings-card {
  margin-bottom: 0;
}

.settings-card + .settings-card {
  margin-top: 20px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  width: 100%;
  display: flex;
  justify-content: flex-start;
  gap: 8px;
}

.system-info p {
  margin: 8px 0;
  font-size: 14px;
  color: #6b7280;
}

.dark .system-info p {
  color: #9ca3af;
}

.system-info .label {
  color: #374151;
  font-weight: 500;
}

.dark .system-info .label {
  color: #e5e7eb;
}
</style>
