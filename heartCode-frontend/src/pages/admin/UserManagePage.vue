<script setup lang="ts">
import { listUserVoByPage, deleteUser } from '@/api/userController'
import { reactive, ref, onMounted, computed, createVNode } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ExclamationCircleOutlined, UserOutlined } from '@ant-design/icons-vue'

// 数据
const data = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 用户角色枚举
const UserRoleMap: Record<string, { text: string; color: string }> = {
  admin: { text: '管理员', color: 'green' },
  user: { text: '普通用户', color: 'blue' },
  default: { text: '未知', color: 'default' },
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage({
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
    title: 'id',
    dataIndex: 'id',
    key: 'id',
    width: 120,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    key: 'userAccount',
    width: 150,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    key: 'userName',
    width: 150,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    key: 'userAvatar',
    width: 100,
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    key: 'userProfile',
    width: 200,
    ellipsis: true,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    key: 'userRole',
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
    width: 100,
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
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 删除数据
const doDelete = (id: string | number, userName?: string) => {
  if (!id) {
    return
  }

  const displayName = userName || `ID: ${id}`
  Modal.confirm({
    title: '确认删除',
    icon: createVNode(ExclamationCircleOutlined),
    content: `确定要删除用户 "${displayName}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        // 直接发送原始 id（字符串），避免 Number 转换导致雪花算法 ID 精度丢失
        const res = await deleteUser({ id })
        if (res.data.code === 0) {
          message.success('删除成功')
          // 刷新数据
          fetchData()
        } else {
          message.error('删除失败，' + res.data.message)
        }
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<template>
  <div id="userManagePage" class="manage-container">
    <!-- 顶部标题 + 搜索卡片（琥珀渐变 + 点状纹理） -->
    <div class="manage-header">
      <div class="manage-header-title">用户管理</div>
      <a-form layout="inline" :model="searchParams" @finish="doSearch" class="manage-header-form">
        <a-form-item label="账号">
          <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="searchParams.userName" placeholder="输入用户名" />
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
    >
      <!-- 头像列自定义 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'userAvatar'">
          <a-avatar
            :size="48"
            :src="record.userAvatar"
            :style="{ backgroundColor: '#1890ff' }"
          >
            <template #icon>
              <UserOutlined />
            </template>
          </a-avatar>
        </template>

        <!-- 用户角色列自定义 -->
        <template v-else-if="column.key === 'userRole'">
          <a-tag
            v-if="record.userRole"
            :color="UserRoleMap[record.userRole]?.color || UserRoleMap.default.color"
          >
            {{ UserRoleMap[record.userRole]?.text || record.userRole }}
          </a-tag>
          <span v-else class="text-gray">-</span>
        </template>

        <!-- 创建时间列自定义 -->
        <template v-else-if="column.key === 'createTime'">
          <span class="text-gray">{{ record.createTime || '-' }}</span>
        </template>

        <!-- 操作列自定义 -->
        <template v-else-if="column.key === 'action'">
          <a-button
            danger
            type="link"
            size="small"
            @click="doDelete(record.id!, record.userName)"
          >
            删除
          </a-button>
        </template>

        <!-- 简介列自定义 -->
        <template v-else-if="column.key === 'userProfile'">
          <span class="text-gray">{{ record.userProfile || '-' }}</span>
        </template>

        <!-- 默认列 -->
        <template v-else>
          <span :class="{ 'text-gray': column.key === 'id' }">
            {{ record[column.dataIndex] || '-' }}
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
