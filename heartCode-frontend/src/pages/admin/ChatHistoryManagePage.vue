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
  <div id="chatHistoryManagePage" class="chat-history-manage-container">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
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
    <a-divider />
    <!-- 表格 -->
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
</template>

<style scoped>
.chat-history-manage-container {
  padding: 20px;
  background: #fff;
  min-height: 100%;
}

.text-gray {
  color: #999;
}
</style>
