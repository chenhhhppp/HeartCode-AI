<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getAppVoById, updateApp, updateAppByAdmin } from '@/api/appController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { storeToRefs } from 'pinia'
import { LeftOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const { isAdmin } = storeToRefs(loginUserStore)

// 应用完整信息（只读展示用）
const appInfo = ref<API.AppVO | null>(null)

// 表单状态
const formState = reactive<{
  id: string
  appName: string
  cover?: string
  priority?: number
}>({
  id: '',
  appName: '',
  cover: '',
  priority: 0,
})

const loading = ref(false)
const submitLoading = ref(false)

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.query.appId as string
  // 严格校验 id，拦截 undefined / null / "null" / "undefined" 等无效字符串
  if (!id || id === 'null' || id === 'undefined') {
    message.error('应用ID不能为空')
    router.push('/home')
    return
  }

  loading.value = true
  try {
    const res = await getAppVoById({ id })
    if (res.data.code === 0 && res.data.data) {
      const app = res.data.data
      appInfo.value = app

      // 权限检查：普通用户只能编辑自己的应用
      if (!isAdmin.value && String(app.user?.id ?? '') !== String(loginUserStore.loginUser?.id ?? '')) {
        message.error('没有权限编辑此应用')
        router.push('/home')
        return
      }

      formState.id = String(app.id ?? '')
      formState.appName = app.appName || ''
      formState.cover = app.cover || ''
      formState.priority = app.priority ?? 0
    } else {
      message.error('获取应用信息失败：' + res.data.message)
    }
  } catch (error) {
    message.error('获取应用信息失败')
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formState.appName.trim()) {
    message.warning('请输入应用名称')
    return
  }

  submitLoading.value = true
  try {
    let res

    if (isAdmin.value) {
      // 管理员可以编辑更多信息
      res = await updateAppByAdmin({
        id: formState.id,
        appName: formState.appName,
        cover: formState.cover || undefined,
        priority: formState.priority,
      })
    } else {
      // 普通用户只能编辑应用名称
      res = await updateApp({
        id: formState.id,
        appName: formState.appName,
      })
    }

    if (res.data.code === 0) {
      message.success('更新成功')
      // 优先返回上一页（通常是对话详情页），否则跳回对话页
      if (window.history.length > 1) {
        router.back()
      } else {
        router.push({ path: '/app/chat', query: { appId: formState.id } })
      }
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } catch (error: any) {
    const errMsg = error?.response?.data?.message || error?.message || ''
    message.error('更新失败' + (errMsg ? '，' + errMsg : '，请稍后重试'))
  } finally {
    submitLoading.value = false
  }
}

// 取消
const handleCancel = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}

// 返回首页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}

onMounted(() => {
  fetchAppInfo()
})
</script>

<template>
  <div class="app-edit-page">
    <a-spin :spinning="loading">
      <div class="edit-container">
        <!-- 顶部返回 + 标题 -->
        <div class="edit-header">
          <a-button type="text" class="back-btn" @click="goBack">
            <LeftOutlined />
            返回
          </a-button>
          <h1>{{ isAdmin ? '编辑应用' : '编辑我的应用' }}</h1>
          <p class="tip">{{ isAdmin ? '管理员可以编辑应用的所有信息' : '只能编辑应用名称' }}</p>
        </div>

        <!-- 只读信息展示区 -->
        <div v-if="appInfo" class="info-section">
          <h2 class="section-title">基本信息</h2>
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="应用 ID">
              {{ appInfo.id || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">
              {{ appInfo.createTime || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="创建者">
              {{ appInfo.user?.userName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="代码类型">
              <a-tag v-if="appInfo.codeGenType === 'html'" color="blue">HTML 模式</a-tag>
              <a-tag v-else-if="appInfo.codeGenType === 'multi_file'" color="green">多文件模式</a-tag>
              <a-tag v-else-if="appInfo.codeGenType === 'vue_project'" color="purple">Vue 项目模式</a-tag>
              <span v-else>-</span>
            </a-descriptions-item>
            <a-descriptions-item label="初始化提示词" :span="2">
              <div class="init-prompt">{{ appInfo.initPrompt || '-' }}</div>
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <!-- 可编辑信息区 -->
        <div class="info-section">
          <h2 class="section-title">可编辑信息</h2>
          <a-form :model="formState" layout="vertical">
            <!-- 应用名称（所有用户都可以编辑） -->
            <a-form-item label="应用名称" required>
              <a-input
                v-model:value="formState.appName"
                placeholder="请输入应用名称"
                :maxlength="50"
                show-count
              />
            </a-form-item>

            <!-- 管理员专属字段 -->
            <template v-if="isAdmin">
              <a-form-item label="应用封面">
                <a-input
                  v-model:value="formState.cover"
                  placeholder="请输入封面图片URL"
                />
                <div class="preview-cover" v-if="formState.cover">
                  <img :src="formState.cover" alt="封面预览" />
                </div>
              </a-form-item>

              <a-form-item label="优先级">
                <a-input-number
                  v-model:value="formState.priority"
                  :min="0"
                  :max="99"
                  style="width: 200px"
                />
                <span class="form-tip">优先级为 99 的应用将显示在精选列表中</span>
              </a-form-item>
            </template>

            <!-- 操作按钮 -->
            <a-form-item>
              <a-space>
                <a-button type="primary" :loading="submitLoading" @click="handleSubmit">
                  保存
                </a-button>
                <a-button @click="handleCancel">取消</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<style scoped>
.app-edit-page {
  max-width: 900px;
  margin: 24px auto;
  padding: 0 24px;
}

.edit-container {
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.edit-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}

.back-btn {
  position: absolute;
  left: 0;
  top: 0;
  padding-left: 0;
}

.edit-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.tip {
  margin: 0;
  color: #999;
  font-size: 14px;
}

.info-section {
  margin-bottom: 32px;
}

.info-section:last-child {
  margin-bottom: 0;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  padding-left: 8px;
  border-left: 3px solid #667eea;
}

.init-prompt {
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 120px;
  overflow-y: auto;
  line-height: 1.6;
}

.preview-cover {
  margin-top: 12px;
  width: 200px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

.preview-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-tip {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}
</style>
