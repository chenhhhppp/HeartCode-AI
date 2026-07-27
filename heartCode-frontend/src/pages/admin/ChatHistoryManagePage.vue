<script setup lang="ts">
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'
import { reactive, ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 数据
const data = ref<API.ChatHistoryVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  // 后端默认按 createTime DESC，这里显式声明，确保语义清晰
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 消息类型枚举映射
const MessageTypeMap: Record<string, { text: string; color: string }> = {
  user: { text: '用户消息', color: 'blue' },
  ai: { text: 'AI 消息', color: 'green' },
  error: { text: '错误消息', color: 'red' },
  default: { text: '未知', color: 'default' },
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAllChatHistoryByPageForAdmin({
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
    title: '应用 ID',
    dataIndex: 'appId',
    key: 'appId',
    width: 100,
  },
  {
    title: '消息类型',
    dataIndex: 'messageType',
    key: 'messageType',
    width: 110,
  },
  {
    title: '消息内容',
    dataIndex: 'message',
    key: 'message',
    ellipsis: true,
  },
  {
    title: '发送用户',
    key: 'user',
    width: 140,
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
    width: 120,
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

// 查看对应应用的对话页
const viewApp = (appId?: number | string) => {
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
  <div id="chatHistoryManagePage" class="manage-container">
    <!-- 顶部标题 + 搜索卡片（琥珀渐变 + 点状纹理） -->
    <div class="manage-header">
      <div class="manage-header-title">对话管理</div>
      <a-form layout="inline" :model="searchParams" @finish="doSearch" class="manage-header-form">
        <a-form-item label="应用 ID">
          <a-input v-model:value="searchParams.appId" placeholder="输入应用 ID" />
        </a-form-item>
        <a-form-item label="消息类型">
          <a-select
            v-model:value="searchParams.messageType"
            placeholder="选择消息类型"
            style="width: 140px"
            allow-clear
          >
            <a-select-option value="user">用户消息</a-select-option>
            <a-select-option value="ai">AI 消息</a-select-option>
            <a-select-option value="error">错误消息</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="用户 ID">
          <a-input v-model:value="searchParams.userId" placeholder="输入用户 ID" />
        </a-form-item>
        <a-form-item label="消息内容">
          <a-input v-model:value="searchParams.message" placeholder="消息内容（模糊）" />
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
      :scroll="{ x: 1100 }"
    >
      <template #bodyCell="{ column, record }">
        <!-- 消息类型列自定义 -->
        <template v-if="column.key === 'messageType'">
          <a-tag
            v-if="record.messageType"
            :color="MessageTypeMap[record.messageType]?.color || MessageTypeMap.default.color"
          >
            {{ MessageTypeMap[record.messageType]?.text || record.messageType }}
          </a-tag>
          <span v-else class="text-gray">-</span>
        </template>

        <!-- 消息内容列自定义：截断显示 -->
        <template v-else-if="column.key === 'message'">
          <a-tooltip :title="record.message">
            <span class="text-gray">{{ (record.message || '').slice(0, 60) }}{{ record.message && record.message.length > 60 ? '...' : '' }}</span>
          </a-tooltip>
        </template>

        <!-- 发送用户列自定义 -->
        <template v-else-if="column.key === 'user'">
          <span class="text-gray">
            {{ record.user?.userName || record.userId || '-' }}
          </span>
        </template>

        <!-- 创建时间列自定义 -->
        <template v-else-if="column.key === 'createTime'">
          <span class="text-gray">{{ record.createTime || '-' }}</span>
        </template>

        <!-- 操作列自定义 -->
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="viewApp(record.appId)">
            查看对话
          </a-button>
        </template>

        <!-- 默认列 -->
        <template v-else>
          <span :class="{ 'text-gray': column.key === 'id' || column.key === 'appId' }">
            {{ record[column.dataIndex as keyof API.ChatHistoryVO] || '-' }}
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
