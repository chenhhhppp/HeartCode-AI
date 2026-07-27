<script setup lang="ts">
import { listAppVoByPageByAdmin, deleteAppByAdmin, updateAppByAdmin } from '@/api/appController'
import { reactive, ref, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { createVNode } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 数据
const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 代码生成类型枚举
const CodeGenTypeMap: Record<string, { text: string; color: string }> = {
  html: { text: 'HTML模式', color: 'blue' },
  multi_file: { text: '多文件模式', color: 'green' },
  vue_project: { text: 'Vue项目模式', color: 'purple' },
  default: { text: '未知', color: 'default' },
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    message.error('获取数据失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
    key: 'appName',
    width: 150,
    ellipsis: true,
  },
  {
    title: '封面',
    dataIndex: 'cover',
    key: 'cover',
    width: 100,
  },
  {
    title: '初始化提示词',
    dataIndex: 'initPrompt',
    key: 'initPrompt',
    width: 200,
    ellipsis: true,
  },
  {
    title: '代码类型',
    dataIndex: 'codeGenType',
    key: 'codeGenType',
    width: 120,
  },
  {
    title: '部署标识',
    dataIndex: 'deployKey',
    key: 'deployKey',
    width: 100,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    key: 'priority',
    width: 80,
  },
  {
    title: '创建人',
    key: 'user',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right',
  },
]

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化处理
const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 删除按钮点击
const onDeleteClick = (record: API.AppVO) => {
  doDelete(record.id!, record.appName || '')
}

// 删除数据
const doDelete = async (id: string | number, appName: string) => {
  // 严格校验 id，拦截 undefined / null / "null" / "undefined" 等无效值
  if (id === undefined || id === null || id === '' || id === 'null' || id === 'undefined') {
    message.error('应用ID无效，无法删除（可能是后端返回数据异常）')
    return
  }

  Modal.confirm({
    title: '确认删除',
    icon: createVNode(ExclamationCircleOutlined),
    content: `确定要删除应用 "${appName}" 吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        // 直接发送原始 id（字符串），避免 Number 转换导致雪花算法 ID 精度丢失
        const res = await deleteAppByAdmin({ id })
        if (res.data.code === 0) {
          message.success('删除成功')
          // 乐观更新：立即从列表中移除，避免等待刷新
          const removed = data.value.filter(app => String(app.id) !== String(id))
          if (removed.length !== data.value.length) {
            data.value = removed
            total.value = Math.max(0, total.value - 1)
          }
          // 后台同步最新数据
          fetchData()
        } else {
          message.error('删除失败，' + res.data.message)
        }
      } catch (error: any) {
        const errMsg = error?.response?.data?.message || error?.message || ''
        message.error('删除失败' + (errMsg ? '，' + errMsg : ''))
        console.error('删除应用失败:', error)
      }
    },
  })
}

// 设置为精选
const doSetGood = async (id: string, isGood: boolean) => {
  try {
    const res = await updateAppByAdmin({
      id,
      priority: isGood ? 0 : 99,
    })
    if (res.data.code === 0) {
      message.success(isGood ? '已取消精选' : '已设为精选')
      fetchData()
    } else {
      message.error('操作失败，' + res.data.message)
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 编辑应用
const editApp = (appId: string) => {
  if (!appId || appId === 'null' || appId === 'undefined') {
    message.error('应用ID无效')
    return
  }
  router.push({
    path: '/app/edit',
    query: { appId },
  })
}

// 查看应用详情（现在通过游标查询自动加载历史，不再需要 view=1 标识）
const viewApp = (appId: string | number) => {
  if (!appId || appId === 'null' || appId === 'undefined') {
    message.error('应用ID无效')
    return
  }
  router.push({
    path: '/app/chat',
    query: { appId: String(appId) },
  })
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<template>
  <div id="appManagePage" class="manage-container">
    <!-- 顶部标题 + 搜索卡片（琥珀渐变 + 点状纹理） -->
    <div class="manage-header">
      <div class="manage-header-title">应用管理</div>
      <a-form layout="inline" :model="searchParams" @finish="doSearch" class="manage-header-form">
        <a-form-item label="应用名称">
          <a-input v-model:value="searchParams.appName" placeholder="输入应用名称" />
        </a-form-item>
        <a-form-item label="代码类型">
          <a-select
            v-model:value="searchParams.codeGenType"
            placeholder="选择代码类型"
            style="width: 120px"
            allow-clear
          >
            <a-select-option value="html">HTML模式</a-select-option>
            <a-select-option value="multi_file">多文件模式</a-select-option>
            <a-select-option value="vue_project">Vue项目模式</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="部署标识">
          <a-input v-model:value="searchParams.deployKey" placeholder="输入部署标识" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>
    </div>
    <!-- 表格卡片 -->
    <div class="manage-table-card">
    <a-table
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="doTableChange"
      :scroll="{ x: 1400 }"
    >
      <!-- 封面列自定义 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cover'">
          <a-image
            v-if="record.cover"
            :src="record.cover"
            :width="60"
            :height="40"
            :style="{ objectFit: 'cover' }"
          />
          <span v-else class="text-gray">-</span>
        </template>

        <!-- 初始化提示词列自定义 -->
        <template v-else-if="column.key === 'initPrompt'">
          <a-tooltip :title="record.initPrompt">
            <span class="text-gray">{{ record.initPrompt?.slice(0, 30) || '-' }}{{ record.initPrompt && record.initPrompt.length > 30 ? '...' : '' }}</span>
          </a-tooltip>
        </template>

        <!-- 代码类型列自定义 -->
        <template v-else-if="column.key === 'codeGenType'">
          <a-tag
            v-if="record.codeGenType"
            :color="CodeGenTypeMap[record.codeGenType]?.color || CodeGenTypeMap.default.color"
          >
            {{ CodeGenTypeMap[record.codeGenType]?.text || record.codeGenType }}
          </a-tag>
          <span v-else class="text-gray">-</span>
        </template>

        <!-- 优先级列自定义 -->
        <template v-else-if="column.key === 'priority'">
          <a-tag v-if="record.priority === 99" color="gold">精选</a-tag>
          <span v-else class="text-gray">{{ record.priority || 0 }}</span>
        </template>

        <!-- 创建人列自定义 -->
        <template v-else-if="column.key === 'user'">
          <span class="text-gray">{{ record.user?.userName || '-' }}</span>
        </template>

        <!-- 创建时间列自定义 -->
        <template v-else-if="column.key === 'createTime'">
          <span class="text-gray">{{ record.createTime || '-' }}</span>
        </template>

        <!-- 操作列自定义 -->
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="editApp(record.id!)">
              编辑
            </a-button>
            <a-button type="link" size="small" @click="viewApp(record.id!)">
              查看
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="doSetGood(record.id!, record.priority === 99)"
            >
              {{ record.priority === 99 ? '取消精选' : '设为精选' }}
            </a-button>
            <a-button danger type="link" size="small" @click="onDeleteClick(record)">
              删除
            </a-button>
          </a-space>
        </template>

        <!-- 默认列 -->
        <template v-else>
          <span :class="{ 'text-gray': column.key === 'id' || column.key === 'deployKey' }">
            {{ record[column.dataIndex as keyof API.AppVO] || '-' }}
          </span>
        </template>
      </template>
    </a-table>
    </div>
  </div>
</template>

<style scoped>
/* 页面容器：浅灰背景，让卡片更突出 */
.manage-container {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100%;
}

/* 顶部标题 + 搜索卡片：参考 UiH 琥珀渐变 + 点状纹理 + 圆角阴影 */
.manage-header {
  position: relative;
  overflow: hidden;
  padding: 24px 28px;
  border-radius: 24px;
  background: linear-gradient(135deg, #fdc56b 0%, #ffbb4e 100%);
  box-shadow: 0 4px 16px rgba(255, 187, 78, 0.35);
}

/* 点状纹理叠加层 */
.manage-header::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(0, 0, 0, 0.08) 1.5px, transparent 1.5px);
  background-size: 18px 18px;
  pointer-events: none;
}

.manage-header-title {
  position: relative;
  font-size: 22px;
  font-weight: 700;
  color: #5a3d1b;
  margin-bottom: 16px;
  letter-spacing: 1px;
}

.manage-header-form {
  position: relative;
}

/* 表格卡片：透明，融入页面背景（原白底大框已隐藏） */
.manage-table-card {
  margin-top: 20px;
  padding: 0;
  background: transparent;
  border-radius: 0;
  box-shadow: none;
}

/* antd 表格内部背景透明，避免残留白色大框 */
.manage-table-card :deep(.ant-table),
.manage-table-card :deep(.ant-table-wrapper),
.manage-table-card :deep(.ant-table-container) {
  background: transparent !important;
}

.text-gray {
  color: #999;
}
</style>
