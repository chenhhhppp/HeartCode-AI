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
import UnloginPromptModal from '@/components/UnloginPromptModal.vue'
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

// 快捷提示词应用列表（来源 applist.txt）
const quickAppList = [
  {
    name: '星座指南',
    prompt: '开发一个星座指南应用。顶部提供十二星座选择器按钮（白羊、金牛、双子、巨蟹、狮子、处女、天秤、天蝎、射手、摩羯、水瓶、双鱼），点击显示下拉列表切换星座。主区域展示选中星座的介绍，配色采用神秘深邃的星空风格，背景为蓝紫渐变，点缀金色和星光元素。',
  },
  {
    name: '日历表',
    prompt: '开发一个日历表应用。主体为一个完整的月历视图，顶部显示当前年月，附带下拉箭头，点击出现下拉列表可以切换月份，支持点击"今天"快速回到当日。每个日期格内高亮显示当天日期。配色采用清爽简洁风格，白色卡片背景配蓝色主题色。整体响应式布局。',
  },
  {
    name: '任务清单列表',
    prompt: '开发一个任务清单应用。顶部为一个输入框和"添加任务"按钮，输入后回车或点击即可新增任务。任务列表每项包含左侧复选框（勾选后划线表示完成）、任务标题、截止日期。每项右侧有编辑和删除操作按钮。顶部上方提供筛选 Tab："全部""待办""已完成"，以及一个搜索框支持按关键词过滤。底部显示任务统计：总数、已完成数。支持拖拽排序调整任务顺序。所有数据使用 localStorage 持久化存储，页面刷新后不丢失。整体采用清爽现代风格，白色背景配蓝色主题。',
  },
  {
    name: '相册',
    prompt: '开发一个相册应用。页面顶部为一个工具栏，包含"全部照片""收藏""相册分类"三个筛选 Tab。主区域以瀑布流网格展示照片缩略图，鼠标悬浮时照片轻微放大并显示标题。点击照片弹出全屏大图预览，支持切换上一张/下一张，点击空白处关闭。大图底部显示照片标题和拍摄描述。每张照片右上角有收藏星标按钮，可切换收藏状态。整体采用暗色优雅风格，深灰背景让照片更突出。',
  },
  {
    name: '番茄钟',
    prompt: '开发一个番茄钟应用。页面正中央是一个大号的环形进度倒计时器，外圈为 SVG 圆环进度条，随时间流逝动态减少，中心显示剩余时间（分:秒）。下方提供"开始专注""暂停""重置"三个控制按钮。页面右侧统计面板展示今日完成的番茄数、累计专注时长，以及最近几次专注记录列表。配色采用温暖的红色系主题，专注时倒计时环为红色，休息时切换为绿色。所有计时数据使用 localStorage 本地持久化保存。',
  },
  {
    name: '个人名片',
    prompt: '开发一个个人名片应用。页面中央展示一张精致的垂直卡片，顶部为圆形头像，下方依次展示姓名（大号字体）、职位/称号、个人简介（两三句话）、邮箱、电话、所在城市。鼠标悬浮时按钮高亮。整体采用深色渐变背景配合毛玻璃卡片效果，卡片有悬浮阴影和呼吸动画。',
  },
  {
    name: '密码生成器',
    prompt: '开发一个密码生成器应用。页面中央为一张操作卡片，顶部用大号等宽字体展示生成的密码，旁边有一键复制按钮。下方提供自定义选项：密码长度滑块（4-32位）、是否包含大写字母、小写字母、数字、特殊符号的勾选框。底部一个"生成密码"按钮，点击后生成新密码并有刷新动画。密码强度指示器实时显示当前密码的强弱（弱/中/强，红/黄/绿）。复制成功后显示 Toast 提示。整体采用现代简约风格，深色背景配霓虹蓝绿色主题。',
  },
  {
    name: '单位换算器',
    prompt: '开发一个单位换算器应用。顶部提供换算类别选择Tab（长度、重量、温度、面积），切换不同类别时下方自动加载对应的单位列表。主区域分左右两列输入：左侧选择输入单位并填写数值，右侧自动显示换算结果和目标单位。中间有一个左右互换的箭头按钮。数值输入时实时计算换算结果。长度类包含：米、千米、厘米、毫米、英里、英尺、英寸。重量类包含：千克、克、吨、磅、盎司。温度类包含：摄氏度、华氏度、开尔文。配色采用清爽的蓝白色主题，数字使用等宽字体便于阅读。',
  },
  {
    name: 'BMI 计算器',
    prompt: '开发一个BMI计算器应用。页面左侧为输入区：身高滑块（100-220cm）和体重滑块（30-150kg），滑块旁实时显示当前数值，也可手动输入。右侧为结果展示区：大号数字显示BMI值，下方用彩色仪表盘弧形图（绿-黄-红渐变）指示BMI所处的健康区间，区间分为偏瘦（<18.5）、正常（18.5-24）、超重（24-28）、肥胖（>28）。仪表盘上有指针随BMI值动态移动。底部给出对应的健康建议文字。整体采用清新健康风格，浅绿白色主题。',
  },
  {
    name: '饮水提醒',
    prompt: '开发一个饮水提醒应用。页面中央展示一个大号环形进度条，显示今日饮水目标完成百分比，圆环中央显示已饮水量/目标量（单位ml）。圆环下方有一排快捷加水按钮：+100ml、+200ml、+300ml、+500ml，点击后进度环平滑动画增长。右侧展示今日饮水记录时间轴，列出每次饮水的时间和容量。底部显示健康小贴士文字轮播。目标饮水量默认2000ml，可在设置中调整。所有数据使用localStorage本地持久化存储，每天自动重置。整体采用清新的蓝色水波纹主题，背景有微妙的水波动画效果。',
  },
  {
    name: '掷骰子',
    prompt: '开发一个掷骰子应用。页面中央展示一个大号的3D骰子，使用CSS 3D变换实现立体效果，六个面分别用点阵表示1到6点。点击"投掷"按钮后，骰子进行翻滚旋转动画（随机多轴旋转约1.5秒），动画结束后停在随机点数上。骰子下方显示最近10次的投掷结果历史记录，以小骰子图标排列展示。右下角统计区显示总投掷次数、最高点数出现次数。骰子悬浮在页面中央时有微妙的上下浮动动画，投掷时伴有震动反馈视觉效果。整体采用深色木质桌面背景风格，骰子为象牙白色。',
  },
  {
    name: '抽奖转盘',
    prompt: '开发一个抽奖转盘应用。页面中央为一个SVG绘制的圆形转盘，等分为多个扇形区域，每个扇形交替填充不同颜色，内填写奖项名称。转盘顶部有一个固定红色三角指针。点击"开始抽奖"按钮后，转盘旋转动画（先加速后减速，总共约3秒），最终随机停在某个奖项上，指针所指即为中奖结果。中奖结果用弹出动画展示奖项名称。左侧提供奖项编辑面板，可添加、删除、修改奖项名称，转盘扇形随之动态更新。整体采用喜庆热闹风格，红金配色，背景有五彩纸屑装饰元素。',
  },
  {
    name: '每日一言',
    prompt: '开发一个每日一言应用。页面中央展示一张精致的卡片，卡片内居中显示一句励志语录（大号衬线字体），下方显示出处作者。卡片背景每天自动更换一张精美渐变色。右上角有一个刷新按钮，点击随机切换一条语录，切换时有淡入淡出动画。卡片下方显示今日日期。底部提供"收藏"按钮，收藏的语录在左侧抽屉面板中列表展示。语录数据用一个本地JSON文件提供，包含至少30条经典名言。整体采用优雅文艺风格，米白色卡片配深棕文字，营造书卷气息。',
  },
  {
    name: '便签墙',
    prompt: '开发一个便签墙应用。页面主体为一个可拖拽的画布区域，上面贴有多个彩色便签纸（黄、粉、蓝、绿四色随机）。每张便签可自由拖拽移动位置，鼠标按住拖动即可。双击便签进入编辑模式可修改文字内容，便签右上角有删除按钮。顶部工具栏有"新建便签"按钮，点击后在画布中央添加一张新便签。便签有轻微随机旋转角度，营造真实贴纸效果。便签内容、位置、颜色使用localStorage持久化存储，刷新页面不丢失。整体背景为软木板纹理风格，便签纸有阴影增加立体感。',
  },
  {
    name: '天气卡片',
    prompt: '开发一个天气卡片应用。页面中央展示一张当前天气大卡片：顶部显示城市名称和当前日期，中央用大号SVG图标展示天气状况（晴、多云、雨、雪，每种对应不同动画效果），旁边用超大号字体显示当前温度。卡片下半部分展示四个天气维度：湿度、风速、体感温度、空气质量，用图标+数值排列。卡片下方横向排列未来5天的天气预报小卡片，每张含星期、天气图标、最高/最低温度。鼠标悬浮小卡片有上浮效果。整体根据天气状况动态切换背景配色，晴天为蓝白渐变，阴天为灰蓝渐变，雨天为深蓝渐变。',
  },
  {
    name: '答案之书',
    prompt: '开发一个答案之书应用。页面中央展示一本闭合的精装书本图形，封面带有烫金标题"答案之书"。上方有一个输入框，提示用户在心中默念一个问题并输入，然后点击"翻开答案"按钮。点击后书本执行翻页动画（CSS 3D 翻转约1.5秒），翻开后页面中央展示一句随机答案文字（如"是的""再想想""时机未到""放手去做"等简短回答），答案以优雅的衬线字体居中展示，配合淡入动画。底部有"再问一次"按钮可重新翻书获取新答案。左侧抽屉展示历史提问与对应答案记录列表。答案数据用一个本地JSON文件提供，包含至少50条不同风格的简短答案。整体采用神秘古典风格，深棕色羊皮纸质感背景，书本封面为深红或墨绿色，营造仪式感。',
  },
]

// 点击快捷按钮，将提示词填入输入框
const fillPrompt = (prompt: string) => {
  createFormState.initPrompt = prompt
}

// 未登录弹窗显示控制
const unloginModalVisible = ref(false)

// 是否已登录（userName 存在且不是默认占位值）
const isLoggedIn = () => {
  const u = loginUserStore.loginUser
  return !!u && !!u.id
}

// 创建应用
const handleCreateApp = async () => {
  // 未登录：弹出提示卡片，不继续后续流程
  if (!isLoggedIn()) {
    unloginModalVisible.value = true
    return
  }

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
        <!-- 输入框样式参考 Uiverse.io by mahiatlinux -->
        <div class="pb-ai-input-wrap">
          <textarea
            v-model="createFormState.initPrompt"
            class="pb-ai-input"
            placeholder="所想即所得，描述你想创建的应用..."
            rows="4"
            @keydown.enter.exact.prevent="handleCreateApp"
          ></textarea>
          <button
            class="pb-ai-clear-btn"
            type="button"
            title="一键清空"
            @click="createFormState.initPrompt = ''"
          >
            清空
          </button>
          <button
            class="pb-ai-input-btn"
            :disabled="createLoading"
            @click="handleCreateApp"
          >
            <svg
              v-if="!createLoading"
              viewBox="0 0 16 16"
              fill="currentColor"
              height="16"
              width="16"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M11.251.068a.5.5 0 0 1 .227.58L9.677 6.5H13a.5.5 0 0 1 .364.843l-8 8.5a.5.5 0 0 1-.842-.49L6.323 9.5H3a.5.5 0 0 1-.364-.843l8-8.5a.5.5 0 0 1 .615-.09z"
              ></path>
            </svg>
            {{ createLoading ? '生成中' : '生成应用' }}
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
        <!-- 快捷提示词按钮 -->
        <div class="quick-prompt-row">
          <button
            v-for="app in quickAppList"
            :key="app.name"
            class="quick-prompt-btn"
            @click="fillPrompt(app.prompt)"
          >
            <span class="quick-prompt-btn-top">{{ app.name }}</span>
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

    <!-- 未登录弹窗（点击生成应用时触发） -->
    <UnloginPromptModal
      :visible="unloginModalVisible"
      @close="unloginModalVisible = false"
    />
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

/* 快捷提示词按钮行 */
.quick-prompt-row {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 10px;
  margin-top: 14px;
}

/* From Uiverse.io by Voxybuns —— 3D 立体按压按钮 */
.quick-prompt-btn {
  --qp-radius: 0.75em;
  --qp-color: #e8e8e8;
  --qp-outline: #000000;
  font-size: 13px;
  font-weight: bold;
  border: none;
  cursor: pointer;
  border-radius: var(--qp-radius);
  background: var(--qp-outline);
}

.quick-prompt-btn-top {
  display: block;
  box-sizing: border-box;
  border: 2px solid var(--qp-outline);
  border-radius: var(--qp-radius);
  padding: 0.4em 0.6em;
  background: var(--qp-color);
  color: var(--qp-outline);
  transform: translateY(-0.2em);
  transition: transform 0.1s ease;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quick-prompt-btn:hover .quick-prompt-btn-top {
  transform: translateY(-0.33em);
}

.quick-prompt-btn:active .quick-prompt-btn-top {
  transform: translateY(0);
}

/* 右侧操作按钮组（清空 + 生成），纵向排列撑满输入框高度 */
.pb-ai-input-wrap {
  position: relative;
  display: flex;
  align-items: stretch;
  gap: 8px;
  width: 100%;
  max-width: 1080px;
  padding: 8px;
  border-radius: 1rem;
  background: #f3f4f6;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  transition: all 0.15s ease-in-out;
}

/* 悬浮放大 + 加深阴影（UiK 风格） */
.pb-ai-input-wrap:hover {
  transform: scale(1.01);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

/* 输入框 */
.pb-ai-input {
  position: relative;
  z-index: 1;
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 12px 16px;
  color: #374151;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: -0.2px;
  resize: vertical;
  line-height: 1.6;
  font-family: inherit;
  min-height: 140px;
  /* 自动换行 */
  white-space: pre-wrap;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.pb-ai-input::placeholder {
  color: #9ca3af;
}

/* 一键清空按钮（与生成按钮纵向排列，撑满高度） */
.pb-ai-clear-btn {
  width: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 0.5rem;
  background: #e5e7eb;
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.pb-ai-clear-btn:hover {
  background: #ef4444;
  color: #fff;
}

/* 生成按钮（样式参考 Uiverse.io by milegelu） */
.pb-ai-input-btn {
  --bezier: cubic-bezier(0.22, 0.61, 0.36, 1);
  --edge-light: hsla(0, 0%, 50%, 0.8);
  --text-light: rgba(255, 255, 255, 0.4);
  --back-color: 240, 40%;

  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5em;
  padding: 0.7em 1.4em;
  border-radius: 0.5em;
  min-height: 2.4em;
  flex-shrink: 0;
  white-space: nowrap;

  font-size: 15px;
  letter-spacing: 0.05em;
  line-height: 1;
  font-weight: bold;

  cursor: pointer;
  border: 0;

  background: linear-gradient(
    140deg,
    hsla(var(--back-color), 50%, 1) min(2em, 20%),
    hsla(var(--back-color), 50%, 0.6) min(8em, 100%)
  );
  color: hsla(0, 0%, 90%);
  box-shadow: inset 0.4px 1px 4px var(--edge-light);

  transition: all 0.1s var(--bezier);
}

.pb-ai-input-btn:hover:not(:disabled) {
  --edge-light: hsla(0, 0%, 50%, 1);
  text-shadow: 0px 0px 10px var(--text-light);
  box-shadow: inset 0.4px 1px 4px var(--edge-light),
    2px 4px 8px hsla(0, 0%, 0%, 0.295);
  transform: scale(1.05);
}

.pb-ai-input-btn:active:not(:disabled) {
  --text-light: rgba(255, 255, 255, 1);

  background: linear-gradient(
    140deg,
    hsla(var(--back-color), 50%, 1) min(2em, 20%),
    hsla(var(--back-color), 50%, 0.6) min(8em, 100%)
  );
  box-shadow: inset 0.4px 1px 8px var(--edge-light),
    0px 0px 8px hsla(var(--back-color), 50%, 0.6);
  text-shadow: 0px 0px 20px var(--text-light);
  color: hsla(0, 0%, 100%, 1);
  letter-spacing: 0.1em;
  transform: scale(1);
}

.pb-ai-input-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .pb-ai-input-btn {
    padding: 10px 16px;
    font-size: 14px;
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
