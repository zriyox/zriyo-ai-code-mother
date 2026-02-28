<!--
项目管理页面
-->
<template>
  <div class="projects-page">
    <div class="page-header">
      <h1 class="page-title">项目管理</h1>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        新建项目
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="项目类型">
          <el-select v-model="filterForm.type" placeholder="全部" clearable>
            <el-option label="Vue" value="vue" />
            <el-option label="React" value="react" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable>
            <el-option label="进行中" value="active" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索项目名称"
            prefix-icon="Search"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 项目列表 -->
    <el-card class="projects-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="projects"
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="name" label="项目名称" min-width="200">
          <template #default="{ row }">
            <div class="project-name">
              <el-icon class="project-icon" :color="row.color">
                <component :is="row.icon" />
              </el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="250" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'vue' ? 'success' : 'warning'" size="small">
              {{ row.type.toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'active' ? 'success' : 'info'"
              size="small"
            >
              {{ row.status === 'active' ? '进行中' : '已完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="handleView(row)">
              查看
            </el-button>
            <el-button link size="small" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click.stop="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 创建项目对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="新建项目"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
      >
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目类型" prop="type">
          <el-radio-group v-model="createForm.type">
            <el-radio-button label="vue">Vue</el-radio-button>
            <el-radio-button label="react">React</el-radio-button>
            <el-radio-button label="other">其他</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入项目描述"
          />
        </el-form-item>
        <el-form-item label="初始化" prop="template">
          <el-select v-model="createForm.template" placeholder="选择模板">
            <el-option label="空白项目" value="blank" />
            <el-option label="管理后台" value="admin" />
            <el-option label="移动端 H5" value="h5" />
            <el-option label="数据大屏" value="dashboard" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, FolderOpened, Grid } from '@element-plus/icons-vue'

const router = useRouter()

// 筛选表单
const filterForm = reactive({
  type: '',
  status: '',
  keyword: '',
})

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

// 数据
const loading = ref(false)
const projects = ref([
  {
    id: 1,
    name: '电商平台前端',
    description: '一个功能完整的电商前端项目',
    type: 'vue',
    status: 'active',
    icon: 'ShoppingCart',
    color: '#3b82f6',
    createdAt: '2024-01-20 14:30:00',
  },
  {
    id: 2,
    name: '管理系统',
    description: '企业级后台管理系统',
    type: 'react',
    status: 'completed',
    icon: 'Monitor',
    color: '#22c55e',
    createdAt: '2024-01-19 10:15:00',
  },
  {
    id: 3,
    name: '数据可视化',
    description: '大数据可视化展示平台',
    type: 'vue',
    status: 'active',
    icon: 'TrendCharts',
    color: '#f59e0b',
    createdAt: '2024-01-18 16:45:00',
  },
])

// 创建对话框
const showCreateDialog = ref(false)
const createFormRef = ref<FormInstance>()
const submitting = ref(false)

const createForm = reactive({
  name: '',
  type: 'vue',
  description: '',
  template: 'blank',
})

const createRules: FormRules = {
  name: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { min: 2, max: 50, message: '项目名称长度为 2-50 个字符', trigger: 'blur' },
  ],
  type: [
    { required: true, message: '请选择项目类型', trigger: 'change' },
  ],
}

// 方法
function handleSearch() {
  console.log('Search:', filterForm)
  // TODO: 实现搜索
}

function handleReset() {
  filterForm.type = ''
  filterForm.status = ''
  filterForm.keyword = ''
}

function handleRowClick(row: any) {
  router.push(`/projects/${row.id}`)
}

function handleView(row: any) {
  router.push(`/projects/${row.id}`)
}

function handleEdit(row: any) {
  ElMessage.info('编辑功能开发中...')
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确定要删除项目"${row.name}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    ElMessage.success('删除成功')
    // TODO: 调用删除 API
  })
}

function handlePageChange(page: number) {
  pagination.page = page
  // TODO: 加载数据
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  // TODO: 加载数据
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true

  try {
    // TODO: 调用创建 API
    await new Promise((resolve) => setTimeout(resolve, 1000))

    ElMessage.success('项目创建成功')
    showCreateDialog.value = false

    // 重置表单
    createFormRef.value?.resetFields()
  } catch (error) {
    ElMessage.error('创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  pagination.total = 3
})
</script>

<style scoped>
.projects-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.filter-card {
  border: none;
}

.filter-form {
  margin-bottom: 0;
}

.projects-card {
  flex: 1;
  border: none;
}

.project-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.project-icon {
  font-size: 18px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .filter-form {
    flex-direction: column;
  }

  .filter-form .el-form-item {
    width: 100% !important;
    margin-right: 0 !important;
  }
}
</style>
