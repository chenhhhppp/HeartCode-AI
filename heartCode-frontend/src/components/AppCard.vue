<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardMeta, Avatar, Tag, Tooltip } from 'ant-design-vue'
import {
  EyeOutlined,
  ExportOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'

/**
 * 应用卡片组件
 * 统一渲染单张应用卡片：封面 + 元信息（头像/标题/作者/标签/时间）+ 操作按钮
 * 风格参考 Uiverse.io by JohnnyCSilva：大圆角 + 柔和双向阴影 + 浅色信息区
 */

interface Props {
  /** 应用数据 */
  app: API.AppVO
  /** 默认封面 emoji（无封面时显示） */
  defaultCoverIcon?: string
  /** 是否显示"编辑"按钮（默认 false） */
  showEdit?: boolean
  /** 是否显示"删除"按钮（默认 false） */
  showDelete?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  defaultCoverIcon: '🚀',
  showEdit: false,
  showDelete: false,
})

const emit = defineEmits<{
  (e: 'click', app: API.AppVO): void
  (e: 'view', app: API.AppVO): void
  (e: 'viewWork', app: API.AppVO): void
  (e: 'edit', app: API.AppVO): void
  (e: 'delete', app: API.AppVO): void
}>()

// 状态标签（默认根据 deployKey 显示"已部署/未部署"；父级可通过 slot 覆盖）
const deployedText = computed(() => (props.app.deployKey ? '已部署' : '未部署'))
const deployedColor = computed(() => (props.app.deployKey ? 'success' : 'default'))

const handleClick = () => emit('click', props.app)
const handleView = (e: Event) => {
  e.stopPropagation()
  emit('view', props.app)
}
const handleViewWork = (e: Event) => {
  e.stopPropagation()
  emit('viewWork', props.app)
}
const handleEdit = (e: Event) => {
  e.stopPropagation()
  emit('edit', props.app)
}
const handleDelete = (e: Event) => {
  e.stopPropagation()
  emit('delete', props.app)
}
</script>

<template>
  <Card class="app-card" hoverable @click="handleClick">
    <template #cover>
      <div class="app-cover">
        <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
        <div v-else class="default-cover">
          <span class="app-icon">{{ defaultCoverIcon }}</span>
        </div>
      </div>
    </template>

    <template #actions>
      <Tooltip title="查看对话" @click.stop="handleView">
        <EyeOutlined />
      </Tooltip>
      <Tooltip v-if="app.deployKey" title="查看作品" @click.stop="handleViewWork">
        <ExportOutlined />
      </Tooltip>
      <Tooltip v-if="showEdit && !app.deployKey" title="编辑" @click.stop="handleEdit">
        <EditOutlined />
      </Tooltip>
      <Tooltip v-if="showDelete" title="删除" @click.stop="handleDelete">
        <DeleteOutlined />
      </Tooltip>
    </template>

    <CardMeta>
      <template #title>{{ app.appName }}</template>
      <template #description>
        <div class="card-author">
          <span class="author-text">by {{ app.user?.userName || '未知用户' }}</span>
        </div>
        <div class="app-meta">
          <slot name="tag">
            <Tag :color="deployedColor">{{ deployedText }}</Tag>
          </slot>
          <span class="time">{{ app.createTime?.slice(0, 10) }}</span>
        </div>
      </template>
      <template #avatar>
        <Avatar :size="44" :src="app.user?.userAvatar" />
      </template>
    </CardMeta>
  </Card>
</template>

<style scoped>
/* 卡片本体：大圆角 + 柔和双向阴影（neumorphism 风格） */
.app-card {
  border-radius: 30px;
  overflow: hidden;
  border: none;
  background: #ffffff;
  transition: box-shadow 0.2s ease-in-out, transform 0.2s ease-in-out;
  box-shadow:
    15px 15px 30px rgba(190, 190, 190, 0.6),
    -15px -15px 30px rgba(255, 255, 255, 0.9);
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow:
    0 10px 25px rgba(0, 0, 0, 0.12),
    0 6px 10px rgba(0, 0, 0, 0.06);
}

/* 封面区域 */
.app-cover {
  height: 160px;
  overflow: hidden;
  position: relative;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.app-card:hover .app-cover img {
  transform: scale(1.05);
}

.default-cover {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-icon {
  font-size: 56px;
  filter: drop-shadow(0 4px 8px rgba(42, 42, 114, 0.15));
}

/* 卡片元信息：左侧头像 + 右侧标题/描述 */
.app-card :deep(.ant-card-body) {
  padding: 20px;
}

.app-card :deep(.ant-card-meta) {
  display: flex;
  align-items: center;
}

.app-card :deep(.ant-card-meta-avatar) {
  padding-right: 14px;
}

.app-card :deep(.ant-card-meta-title) {
  margin-bottom: 4px;
  font-size: 16px;
  font-weight: 600;
  color: #2a2a72;
  line-height: 1.3;
}

.app-card :deep(.ant-card-meta-description) {
  font-size: 13px;
  color: #888;
}

/* 作者信息 */
.card-author {
  margin-bottom: 6px;
}

.author-text {
  color: #666;
  font-weight: 500;
}

/* 状态标签 + 时间 */
.app-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.app-meta .time {
  font-size: 12px;
  color: #999;
}

/* 底部操作栏：浅色圆角背景 */
.app-card :deep(.ant-card-actions) {
  background: rgba(245, 247, 250, 0.8);
  border-top: 1px solid rgba(0, 0, 0, 0.04);
}

.app-card :deep(.ant-card-actions > li) {
  margin: 8px 0;
}

.app-card :deep(.ant-card-actions .anticon) {
  font-size: 18px;
  color: #6b7280;
  transition: color 0.2s ease, transform 0.2s ease;
}

.app-card :deep(.ant-card-actions .anticon:hover) {
  color: #3b82f6;
  transform: scale(1.15);
}
</style>
