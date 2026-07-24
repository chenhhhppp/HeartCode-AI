<script setup lang="ts">
import { Modal, Avatar, Button, message } from 'ant-design-vue'
import { EditOutlined, DeleteOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { createVNode } from 'vue'

/**
 * 应用详情弹窗组件
 * 展示创建者、创建时间、应用描述，并对外暴露"修改/删除"操作
 * 删除确认对话框内置在组件内，调用方只需监听 deleted 事件做后续跳转
 */

interface Props {
  /** 控制显示（v-model:open） */
  open: boolean
  /** 应用数据 */
  app: API.AppVO | null
  /** 是否显示操作栏（修改/删除按钮） */
  showActions?: boolean
  /** 是否显示修改按钮 */
  canEdit?: boolean
  /** 是否显示删除按钮 */
  canDelete?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showActions: true,
  canEdit: true,
  canDelete: true,
})

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'edit', app: API.AppVO): void
  (e: 'deleted', app: API.AppVO): void
}>()

// 双向绑定 open
const handleOpenChange = (val: boolean) => emit('update:open', val)

// 点击修改
const handleEdit = () => {
  if (!props.app) return
  emit('edit', props.app)
}

// 点击删除（带确认对话框）
const handleDelete = () => {
  if (!props.app) return
  const app = props.app
  const appName = app.appName || '此应用'

  Modal.confirm({
    title: '确认删除',
    icon: createVNode(ExclamationCircleOutlined),
    content: `确定要删除应用 "${appName}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const { deleteApp } = await import('@/api/appController.ts')
        // 后端 Long 以字符串序列化（雪花算法 ID 精度保护），DeleteRequest 类型为 number
        // 实际发送字符串，这里 cast 为 any 避免类型告警
        const res = await deleteApp({ id: app.id as any })
        if (res.data.code === 0) {
          message.success('删除成功')
          emit('update:open', false)
          emit('deleted', app)
        } else {
          message.error('删除失败：' + res.data.message)
        }
      } catch (error: any) {
        const errMsg = error?.response?.data?.message || ''
        message.error('删除失败' + (errMsg ? '，' + errMsg : '，请稍后重试'))
      }
    },
  })
}
</script>

<template>
  <Modal
    :open="open"
    title="应用详情"
    :footer="null"
    width="500px"
    @update:open="handleOpenChange"
  >
    <div v-if="app" class="app-detail-content">
      <!-- 创建者信息 -->
      <div class="detail-section">
        <div class="detail-label">创建者</div>
        <div class="creator-info">
          <Avatar :size="40" :src="app.user?.userAvatar" style="background-color: #667eea">
            {{ app.user?.userName?.charAt(0) || '?' }}
          </Avatar>
          <span class="creator-name">{{ app.user?.userName || '未知用户' }}</span>
        </div>
      </div>

      <!-- 创建时间 -->
      <div class="detail-section">
        <div class="detail-label">创建时间</div>
        <div class="detail-value">{{ app.createTime || '-' }}</div>
      </div>

      <!-- 应用描述 -->
      <div v-if="app.initPrompt" class="detail-section">
        <div class="detail-label">应用描述</div>
        <div class="detail-value">{{ app.initPrompt }}</div>
      </div>

      <!-- 操作栏 -->
      <div v-if="showActions && (canEdit || canDelete)" class="detail-actions">
        <Button v-if="canEdit" @click="handleEdit">
          <EditOutlined />
          修改
        </Button>
        <Button v-if="canDelete" danger @click="handleDelete">
          <DeleteOutlined />
          删除
        </Button>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.app-detail-content {
  padding: 8px 0;
}

.detail-section {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-section:last-of-type {
  border-bottom: none;
}

.detail-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.creator-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.detail-actions {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
}

.detail-actions .ant-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
