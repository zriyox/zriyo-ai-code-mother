<!--
项目详情页面
-->
<template>
  <div class="project-detail-page">
    <div class="page-header">
      <div class="header-left">
        <el-button circle @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="project-info">
          <h1 class="page-title">{{ project.name }}</h1>
          <el-tag :type="project.type === 'vue' ? 'success' : 'warning'">
            {{ project.type.toUpperCase() }}
          </el-tag>
        </div>
      </div>
      <div class="header-right">
        <el-button @click="openInEditor">
          <el-icon><Edit /></el-icon>
          打开编辑器
        </el-button>
        <el-button type="primary" @click="runProject">
          <el-icon><VideoPlay /></el-icon>
          运行
        </el-button>
      </div>
    </div>

    <!-- 选项卡 -->
    <el-tabs v-model="activeTab" class="project-tabs">
      <!-- 文件浏览器 -->
      <el-tab-pane label="文件" name="files">
        <div class="files-container">
          <div class="files-sidebar">
            <el-tree
              :data="fileTree"
              :props="{ children: 'children', label: 'name' }"
              node-key="path"
              @node-click="handleFileClick"
            />
          </div>
          <div class="files-main">
            <el-card class="file-editor-card" shadow="never">
              <template #header>
                <div class="editor-header">
                  <span>{{ currentFile?.name || '选择文件查看内容' }}</span>
                  <el-button-group v-if="currentFile">
                    <el-button size="small" @click="copyFile">
                      <el-icon><DocumentCopy /></el-icon>
                      复制
                    </el-button>
                    <el-button size="small" @click="renameFile">
                      <el-icon><Edit /></el-icon>
                      重命名
                    </el-button>
                  </el-button-group>
                </div>
              </template>
              <div v-if="currentFile" class="code-editor">
                <pre><code>{{ fileContent }}</code></pre>
              </div>
              <empty-state v-else text="请选择文件" />
            </el-card>
          </div>
        </div>
      </el-tab-pane>

      <!-- 代码生成 -->
      <el-tab-pane label="AI 生成" name="ai">
        <div class="ai-generate-container">
          <el-card class="ai-prompt-card" shadow="never">
            <template #header>
              <span>代码生成</span>
            </template>
            <el-input
              v-model="prompt"
              type="textarea"
              :rows="4"
              placeholder="描述你想要生成的功能，例如：创建一个用户列表页面..."
              @keydown.ctrl.enter="handleGenerate"
            />
            <div class="prompt-actions">
              <el-button type="primary" :loading="generating" @click="handleGenerate">
                <el-icon><MagicStick /></el-icon>
                生成代码
              </el-button>
              <el-select v-model="llmProvider" placeholder="选择 LLM">
                <el-option label="OpenAI" value="openai" />
                <el-option label="Claude" value="claude" />
                <el-option label="DeepSeek" value="deepseek" />
                <el-option label="通义千问" value="qwen" />
              </el-select>
            </div>
          </el-card>

          <!-- 生成结果 -->
          <el-card v-if="generatedCode" class="ai-result-card" shadow="never">
            <template #header>
              <div class="result-header">
                <span>生成结果</span>
                <div class="result-actions">
                  <el-button size="small" @click="copyCode">复制</el-button>
                  <el-button size="small" type="primary" @click="applyCode">
                    应用到项目
                  </el-button>
                </div>
              </div>
            </template>
            <div class="code-editor">
              <pre><code>{{ generatedCode }}</code></pre>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 预览 -->
      <el-tab-pane label="预览" name="preview">
        <div class="preview-container">
          <div class="preview-toolbar">
            <span>设备:</span>
            <el-radio-group v-model="previewDevice">
              <el-radio-button label="desktop">桌面</el-radio-button>
              <el-radio-button label="tablet">平板</el-radio-button>
              <el-radio-button label="mobile">手机</el-radio-button>
            </el-radio-group>
          </div>
          <div class="preview-iframe" :class="['preview-' + previewDevice]">
            <iframe
              :src="previewUrl"
              frameborder="0"
              sandbox="allow-scripts allow-same-origin allow-forms"
            ></iframe>
          </div>
        </div>
      </el-tab-pane>

      <!-- 设置 -->
      <el-tab-pane label="设置" name="settings">
        <div class="settings-container">
          <el-card shadow="never">
            <template #header>
              <span>项目设置</span>
            </template>
            <el-form label-width="120px">
              <el-form-item label="项目名称">
                <el-input v-model="project.name" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="project.description" type="textarea" />
              </el-form-item>
              <el-form-item label="构建命令">
                <el-input v-model="project.buildCommand" placeholder="npm run build" />
              </el-form-item>
              <el-form-item label="启动命令">
                <el-input v-model="project.devCommand" placeholder="npm run dev" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveProjectSettings">保存</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Edit,
  VideoPlay,
  DocumentCopy,
  MagicStick,
} from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const activeTab = ref('files')
const currentFile = ref<any>(null)
const fileContent = ref('')
const prompt = ref('')
const generating = ref(false)
const generatedCode = ref('')
const llmProvider = ref('deepseek')
const previewDevice = ref('desktop')

const project = ref({
  id: 1,
  name: '电商平台前端',
  description: '一个功能完整的电商前端项目',
  type: 'vue',
  buildCommand: 'npm run build',
  devCommand: 'npm run dev',
})

const fileTree = ref([
  {
    name: 'src',
    path: '/src',
    children: [
      { name: 'App.vue', path: '/src/App.vue' },
      { name: 'main.ts', path: '/src/main.ts' },
      {
        name: 'views',
        path: '/src/views',
        children: [
          { name: 'Home.vue', path: '/src/views/Home.vue' },
          { name: 'About.vue', path: '/src/views/About.vue' },
        ],
      },
    ],
  },
  {
    name: 'package.json',
    path: '/package.json',
  },
])

const previewUrl = ref('/preview.html')

function goBack() {
  router.back()
}

function openInEditor() {
  ElMessage.info('打开编辑器功能开发中')
}

function runProject() {
  ElMessage.success('项目启动中...')
}

function handleFileClick(file: any) {
  if (file.children) return
  currentFile.value = file
  // TODO: 加载文件内容
  const className = file.name.replace('.vue', '')
  const scriptClose = '<' + '/script>'
  fileContent.value = [
    '<template>',
    '  <div class="' + className + '">',
    '    <h1>Hello World</h1>',
    '  </div>',
    '</template>',
    '',
    scriptClose + ' setup lang="ts">',
    'import { ref } from \'vue\'',
    '',
    'const message = ref(\'Hello from ' + className + '\')',
    scriptClose,
    '',
    '<style scoped>',
    '.hello {',
    '  color: #42b883;',
    '}',
    '</style>',
  ].join('\n')
}

function copyFile() {
  navigator.clipboard.writeText(fileContent.value)
  ElMessage.success('已复制到剪贴板')
}

function renameFile() {
  ElMessage.info('重命名功能开发中')
}

async function handleGenerate() {
  if (!prompt.value.trim()) {
    ElMessage.warning('请输入生成需求')
    return
  }

  generating.value = true

  try {
    // TODO: 调用 Python AI 服务生成代码
    await new Promise((resolve) => setTimeout(resolve, 2000))

    const scriptClose = '<' + '/script>'
    generatedCode.value = [
      '<template>',
      '  <div class="generated-page">',
      '    <h1>' + prompt.value + '</h1>',
      '    <p>这是由 AI 生成的代码</p>',
      '  </div>',
      '</template>',
      '',
      scriptClose + ' setup lang="ts">',
      'import { ref } from \'vue\'',
      '',
      'const message = ref(\'Hello World\')',
      scriptClose,
      '',
      '<style scoped>',
      '.generated-page {',
      '  padding: 20px;',
      '}',
      '',
      '.generated-page h1 {',
      '  color: #42b883;',
      '}',
      '</style>',
    ].join('\n')

    ElMessage.success('代码生成成功')
  } catch (error) {
    ElMessage.error('代码生成失败')
  } finally {
    generating.value = false
  }
}

function copyCode() {
  navigator.clipboard.writeText(generatedCode.value)
  ElMessage.success('已复制到剪贴板')
}

function applyCode() {
  ElMessage.success('代码已应用到项目')
}

function saveProjectSettings() {
  ElMessage.success('设置已保存')
}

onMounted(() => {
  const projectId = route.params.id
  // TODO: 加载项目详情
})
</script>

<style scoped>
.project-detail-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.project-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.files-container {
  display: flex;
  height: 600px;
}

.files-sidebar {
  width: 250px;
  border-right: 1px solid #f3f4f6;
  padding-right: 16px;
  overflow-y: auto;
}

.files-main {
  flex: 1;
  padding-left: 16px;
}

.code-editor {
  background-color: #1e1e1e;
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
  max-height: 500px;
}

.code-editor pre {
  margin: 0;
}

.code-editor code {
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  color: #d4d4d4;
}

.ai-generate-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ai-prompt-card {
  margin-bottom: 0;
}

.prompt-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.preview-container {
  display: flex;
  flex-direction: column;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.preview-iframe {
  width: 100%;
  height: 500px;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.preview-iframe iframe {
  width: 100%;
  height: 100%;
}

.preview-desktop .preview-iframe iframe {
  width: 100%;
}

.preview-tablet .preview-iframe {
  width: 768px;
  margin: 0 auto;
}

.preview-mobile .preview-iframe {
  width: 375px;
  margin: 0 auto;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .files-container {
    flex-direction: column;
  }

  .files-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #f3f4f6;
    padding-bottom: 12px;
    padding-right: 0;
  }

  .files-main {
    padding-left: 0;
    padding-top: 12px;
  }
}
</style>
