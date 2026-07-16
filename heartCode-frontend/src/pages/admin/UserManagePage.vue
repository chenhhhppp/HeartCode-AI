<script setup lang="ts">
import { listUserVoByPage, deleteUser } from '@/api/userController'
import { reactive, ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { UserOutlined } from '@ant-design/icons-vue'

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
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败，' + res.data.message)
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<template>
  <div id="userManagePage" class="user-manage-container">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
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
    <a-divider />
    <!-- 表格 -->
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
            @click="doDelete(record.id!)"
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
</template>

<style scoped>
.user-manage-container {
  padding: 20px;
  background: #fff;
  min-height: 100%;
}

.text-gray {
  color: #999;
}
</style>
