<script setup lang="ts">
import { reactive, ref, onMounted, createVNode } from 'vue'
import { useRouter } from 'vue-router'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage, deleteApp } from '@/api/appController.ts'
import { message, Modal } from 'ant-design-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { getDeployUrl, CodeGenTypeEnum, CODE_GEN_TYPE_CONFIG } from '@/config/env'
import AppCard from '@/components/AppCard.vue'
import Pagination3D from '@/components/Pagination3D.vue'
import { Tag } from 'ant-design-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 创建应用表单
const createFormState = reactive<{
  initPrompt: string
  codeGenType: string
}>({
  initPrompt: '',
  codeGenType: CodeGenTypeEnum.MULTI_FILE,
})

const createLoading = ref(false)

// 创建应用
const handleCreateApp = async () => {
  if (!createFormState.initPrompt.trim()) {
    message.warning('请输入提示词')
    return
  }

  createLoading.value = true
  try {
    const res = await addApp({ initPrompt: createFormState.initPrompt, codeGenType: createFormState.codeGenType })
    if (res.data.code === 0 && res.data.data) {
      message.success('创建成功，正在跳转...')
      router.push({
        path: '/app/chat',
        query: { appId: String(res.data.data), initPrompt: createFormState.initPrompt },
      })
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    message.error('创建失败，请稍后重试')
  } finally {
    createLoading.value = false
  }
}

// 我的应用列表
const myAppList = ref<API.AppVO[]>([])
const myAppLoading = ref(false)
const myAppPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})
const myAppNameSearch = ref('')

const fetchMyApps = async () => {
  myAppLoading.value = true
  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppPagination.current,
      pageSize: myAppPagination.pageSize,
      appName: myAppNameSearch.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      myAppList.value = res.data.data.records || []
      myAppPagination.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    message.error('获取我的应用失败')
  } finally {
    myAppLoading.value = false
  }
}

// 精选应用列表
const goodAppList = ref<API.AppVO[]>([])
const goodAppLoading = ref(false)
const goodAppPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})
const goodAppNameSearch = ref('')

const fetchGoodApps = async () => {
  goodAppLoading.value = true
  try {
    const res = await listGoodAppVoByPage({
      pageNum: goodAppPagination.current,
      pageSize: goodAppPagination.pageSize,
      appName: goodAppNameSearch.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      goodAppList.value = res.data.data.records || []
      goodAppPagination.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    message.error('获取精选应用失败')
  } finally {
    goodAppLoading.value = false
  }
}

// 分页 & 搜索
const handleMyAppPageChange = (page: number, pageSize: number) => {
  myAppPagination.current = page
  myAppPagination.pageSize = pageSize
  fetchMyApps()
}
const handleMyAppSearch = () => {
  myAppPagination.current = 1
  fetchMyApps()
}
const handleGoodAppPageChange = (page: number, pageSize: number) => {
  goodAppPagination.current = page
  goodAppPagination.pageSize = pageSize
  fetchGoodApps()
}
const handleGoodAppSearch = () => {
  goodAppPagination.current = 1
  fetchGoodApps()
}

// 卡片事件处理
const openAppView = (app: API.AppVO) => {
  if (!app.id) return
  router.push({
    path: '/app/chat',
    query: { appId: String(app.id) },
  })
}

const viewWork = (app: API.AppVO) => {
  if (!app.deployKey) return
  window.open(getDeployUrl(app.deployKey), '_blank')
}

const editMyApp = (app: API.AppVO) => {
  if (!app.id) return
  router.push({
    path: '/app/edit',
    query: { appId: String(app.id) },
  })
}

const deleteMyApp = (app: API.AppVO) => {
  if (!app.id) return
  Modal.confirm({
    title: '删除应用',
    icon: createVNode(ExclamationCircleOutlined),
    content: `确定要删除应用「${app.appName}」吗？该操作会同时删除该应用的对话历史，且不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteApp({ id: app.id })
        message.success('应用已删除')
        // 乐观更新：先从列表移除
        const removed = myAppList.value.filter(a => String(a.id) !== String(app.id))
        if (removed.length !== myAppList.value.length) {
          myAppList.value = removed
          myAppPagination.total = Math.max(0, myAppPagination.total - 1)
        }
        // 重新拉取以保持分页数据一致
        fetchMyApps()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    }
  })
}

onMounted(() => {
  loginUserStore.fetchLoginUser().catch(() => {})
  fetchMyApps().catch(() => {})
  fetchGoodApps()
})
</script>

<template>
  <div class="home-page">
    <!-- 网站标题和创建应用区域 -->
    <div class="header-section">
      <div class="title-container">
        <h1 class="main-title">HeartCode 心码 AI 应用生成平台</h1>
        <p class="subtitle">所想即所得，智能生成你的专属应用</p>
      </div>

      <div class="create-app-section">
        <div class="pb-ai-input-wrap">
          <input
            v-model="createFormState.initPrompt"
            type="text"
            class="pb-ai-input"
            placeholder="所想即所得，描述你想创建的应用..."
            @keyup.enter="handleCreateApp"
          />
          <button
            class="pb-ai-input-btn"
            :disabled="createLoading"
            @click="handleCreateApp"
          >
            <span>{{ createLoading ? '生成中' : '生成应用' }}</span>
            <span class="pb-ai-sparkle">✦</span>
          </button>
        </div>
        <!-- 生成模式选择 -->
        <div class="gen-type-row">
          <button
            v-for="item in Object.values(CODE_GEN_TYPE_CONFIG)"
            :key="item.value"
            class="gen-type-btn"
            :class="{ active: createFormState.codeGenType === item.value }"
            @click="createFormState.codeGenType = item.value"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 我的应用区域 -->
    <div class="section">
      <div class="section-header">
        <h2>我的应用</h2>
        <a-input-search
          v-model:value="myAppNameSearch"
          placeholder="搜索应用名称"
          style="width: 200px"
          @search="handleMyAppSearch"
        />
      </div>
      <a-spin :spinning="myAppLoading">
        <a-empty v-if="!myAppLoading && myAppList.length === 0" description="暂无应用，快来创建一个吧！" />
        <div v-else class="app-grid">
          <AppCard
            v-for="app in myAppList"
            :key="app.id"
            :app="app"
            default-cover-icon="🚀"
            :show-edit="loginUserStore.isAdmin"
            :show-delete="true"
            @click="openAppView"
            @view="openAppView"
            @view-work="viewWork"
            @edit="editMyApp"
            @delete="deleteMyApp"
          />
        </div>
      </a-spin>
      <Pagination3D
        v-if="myAppList.length > 0"
        v-model:current="myAppPagination.current"
        v-model:pageSize="myAppPagination.pageSize"
        :total="myAppPagination.total"
        :show-total="(total: number) => `共 ${total} 个应用`"
        @change="handleMyAppPageChange"
      />
    </div>

    <!-- 精选应用区域 -->
    <div class="section">
      <div class="section-header">
        <h2>精选应用</h2>
        <a-input-search
          v-model:value="goodAppNameSearch"
          placeholder="搜索应用名称"
          style="width: 200px"
          @search="handleGoodAppSearch"
        />
      </div>
      <a-spin :spinning="goodAppLoading">
        <a-empty v-if="!goodAppLoading && goodAppList.length === 0" description="暂无精选应用" />
        <div v-else class="app-grid">
          <AppCard
            v-for="app in goodAppList"
            :key="app.id"
            :app="app"
            default-cover-icon="⭐"
            :show-edit="false"
            :show-delete="false"
            @click="openAppView"
            @view="openAppView"
            @view-work="viewWork"
          >
            <template #tag>
              <Tag color="gold">精选</Tag>
            </template>
          </AppCard>
        </div>
      </a-spin>
      <Pagination3D
        v-if="goodAppList.length > 0"
        v-model:current="goodAppPagination.current"
        v-model:pageSize="goodAppPagination.pageSize"
        :total="goodAppPagination.total"
        :show-total="(total: number) => `共 ${total} 个应用`"
        @change="handleGoodAppPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: 100vh;
}

/* 头部区域（Uiverse.io 卡片+阴影风格，浅蓝玻璃拟态） */
.header-section {
  background-image: linear-gradient(120deg, rgba(161, 196, 253, 0.65) 0%, rgba(194, 233, 251, 0.65) 100%);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  padding: 48px 32px;
  margin-bottom: 32px;
  color: #2a2a72;
  text-align: center;
  transition: border-radius 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow:
    inset 0 -3em 3em rgba(0, 0, 0, 0.1),
    0 0 0 2px rgb(190, 190, 190),
    0.3em 0.3em 1em rgba(0, 0, 0, 0.3);
}

.title-container {
  margin-bottom: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.main-title {
  max-width: fit-content;
  background:
    linear-gradient(to bottom, #323232 0%, #3F3F3F 40%, #1C1C1C 150%),
    linear-gradient(to top, rgba(255, 255, 255, 0.40) 0%, rgba(0, 0, 0, 0.25) 200%);
  background-blend-mode: multiply;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
  font-size: 50px;
  font-family: MT;
  position: relative;
  font-style: italic;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.subtitle {
  max-width: fit-content;
  background:
    linear-gradient(to bottom, #323232 0%, #3F3F3F 40%, #1C1C1C 150%),
    linear-gradient(to top, rgba(255, 255, 255, 0.40) 0%, rgba(0, 0, 0, 0.25) 200%);
  background-blend-mode: multiply;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
  font-size: 28px;
  font-family: 'Brush Script MT', cursive;
  position: relative;
  font-style: italic;
  font-weight: 500;
  margin: 0;
}

.create-app-section {
  max-width: 720px;
  margin: 0 auto;
}

/* 生成模式选择行 */
.gen-type-row {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

/* Uiverse.io 3D 按钮风格 */
.gen-type-btn {
  font-size: 15px;
  padding: 8px 22px;
  border-radius: 0.7rem;
  background-image: linear-gradient(rgb(214, 202, 254), rgb(158, 129, 254));
  border: 2px solid rgb(50, 50, 50);
  border-bottom: 5px solid rgb(50, 50, 50);
  box-shadow: 0px 1px 6px 0px rgb(158, 129, 254);
  transform: translate(0, -3px);
  cursor: pointer;
  transition: 0.2s;
  transition-timing-function: linear;
}

.gen-type-btn:active {
  transform: translate(0, 0);
  border-bottom: 2px solid rgb(50, 50, 50);
}

/* 未选中按钮：扁平低饱和 */
.gen-type-btn:not(.active) {
  background-image: linear-gradient(rgb(240, 240, 240), rgb(225, 225, 225));
  box-shadow: none;
  transform: translate(0, 0);
  border-bottom: 2px solid rgb(50, 50, 50);
  opacity: 0.65;
}

.gen-type-btn:not(.active):hover {
  opacity: 0.85;
}

/* 生成应用输入框（Uiverse.io 紫色玻璃拟态风格，调整为白底黑字） */
.pb-ai-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px;
  border-radius: 999px;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(245, 240, 255, 0.9) 100%
  );
  backdrop-filter: blur(14px);
  box-shadow:
    0 0 0 4px rgba(125, 71, 255, 0.08),
    0 4px 20px rgba(98, 43, 255, 0.1),
    inset 0 0 6px rgba(255, 255, 255, 0.4);
  overflow: hidden;
  isolation: isolate;
}

/* Gloss */
.pb-ai-input-wrap::before {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: inherit;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.6),
    rgba(255, 255, 255, 0.2) 45%,
    rgba(255, 255, 255, 0)
  );
  pointer-events: none;
  z-index: 1;
}

/* Grain */
.pb-ai-input-wrap::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background-image: radial-gradient(
      circle at bottom center,
      rgba(166, 125, 255, 0.08) 0%,
      rgba(166, 125, 255, 0.02) 20%,
      transparent 60%
    );
  opacity: 0.5;
  pointer-events: none;
  z-index: 2;
}

.pb-ai-input {
  position: relative;
  z-index: 3;
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 6px 16px;
  color: #1f1f1f;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: -0.2px;
}

.pb-ai-input::placeholder {
  color: #999;
}

.pb-ai-input-btn {
  position: relative;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: none;
  outline: none;
  cursor: pointer;
  padding: 14px 22px;
  border-radius: 999px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.2px;
  white-space: nowrap;
  background: linear-gradient(180deg, #a67dff 0%, #7a45ff 45%, #5d24ff 100%);
  box-shadow:
    0 0 0 3px rgba(125, 71, 255, 0.1),
    0 5px 12px rgba(98, 43, 255, 0.2),
    inset 0 2px 8px rgba(255, 255, 255, 0.16);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.pb-ai-input-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow:
    0 0 0 4px rgba(125, 71, 255, 0.12),
    0 8px 16px rgba(98, 43, 255, 0.24),
    inset 0 2px 8px rgba(255, 255, 255, 0.2);
}

.pb-ai-input-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.pb-ai-input-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.pb-ai-sparkle {
  font-size: 14px;
  transform: translateY(-1px);
}

@media (max-width: 640px) {
  .pb-ai-input-btn span:first-child {
    display: none;
  }

  .pb-ai-input-btn {
    padding: 12px 14px;
  }
}

/* 通用区块样式（Uiverse.io 卡片+阴影风格） */
.section {
  background: white;
  border-radius: 10px;
  padding: 24px;
  margin-bottom: 24px;
  transition: border-radius 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow:
    inset 0 -3em 3em rgba(0, 0, 0, 0.1),
    0 0 0 2px rgb(190, 190, 190),
    0.3em 0.3em 1em rgba(0, 0, 0, 0.3);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #2a2a72;
}

/* 应用卡片网格 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
}
</style>
