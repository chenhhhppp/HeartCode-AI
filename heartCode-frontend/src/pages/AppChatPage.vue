<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getAppVoById, deployApp, downloadAppCode } from '@/api/appController.ts'
import { listAppChatHistory } from '@/api/chatHistoryController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import { getDeployUrl, getStaticPreviewUrl } from '@/config/env'
import {
  enableEditMode as enableIframeEditMode,
  disableEditMode as disableIframeEditMode,
  generateEditorScript,
  isElementSelectedMessage,
  formatElementForPrompt,
  type SelectedElement,
} from '@/utils/visualEditor'
import {
  LeftOutlined,
  CloudUploadOutlined,
  UserOutlined,
  SendOutlined,
  CodeOutlined,
  ReloadOutlined,
  InfoCircleOutlined,
  HistoryOutlined,
  DownloadOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO | null>(null)
const appId = ref<string>()
const initPrompt = ref('')

// 消息列表
interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: Date
  isStreaming?: boolean // 是否正在流式生成中
  displayedContent?: string // 用于打字机效果的显示内容
  // 游标相关：最早消息的 createTime，用于向前加载
  createTime?: string
}

const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const inputDisabled = ref(false)
const isGenerating = ref(false)

// 历史加载相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false) // 是否还有更早的历史可以加载
const oldestCreateTime = ref<string | undefined>(undefined) // 当前已加载最早的 createTime

// 检查是否是应用所有者（后端 Long 以字符串序列化，统一转字符串比较）
const isOwner = computed(() => {
  if (!appInfo.value?.userId || !loginUserStore.loginUser?.id) {
    return false
  }
  return String(appInfo.value.userId) === String(loginUserStore.loginUser.id)
})

// 输入框是否禁用
const isInputDisabled = computed(() => !isOwner.value || inputDisabled.value || isGenerating.value)

// 禁用输入的原因（用于 tooltip）
const inputDisabledReason = computed(() => {
  if (!isOwner.value) {
    return '无法在别人的作品下对话哦~'
  }
  if (isGenerating.value) {
    return 'AI 正在生成中...'
  }
  return ''
})

// 进度条状态
const generationProgress = ref(0)
const progressStatus = ref<'active' | 'success' | 'exception'>('active')

// 预览 URL
const previewUrl = ref('')
const previewBlobUrl = ref('')
const showPreview = ref(false)
const previewLoading = ref(false) // 预览 HTML 加载中
const previewError = ref(false) // 预览加载失败
const previewFrame = ref<HTMLIFrameElement | null>(null) // 预览 iframe 引用
const deployLoading = ref(false)
const downloadLoading = ref(false)
const deployedUrl = ref('')

// 可视化编辑相关
const isEditMode = ref(false) // 是否处于编辑模式
const selectedElement = ref<SelectedElement | null>(null) // 当前选中的元素

// 代码文件 Tab 相关
interface CodeFile {
  path: string       // 文件相对路径，如 src/App.vue
  name: string       // 文件名，如 App.vue
  language: string   // 语言标识，如 vue、ts、css
  content: string    // 文件内容
}
const codeFiles = ref<CodeFile[]>([])
// 预览区域当前激活的 Tab：'preview' 或文件路径
const activeTab = ref<string>('preview')

// 应用详情模态框
const showAppDetailModal = ref(false)

// 消息容器引用（用于加载更多后保持滚动位置）
const messagesContainer = ref<HTMLElement | null>(null)

/**
 * 将后端 ChatHistory 转为前端 ChatMessage
 */
const chatHistoryToMessage = (record: API.ChatHistory): ChatMessage => {
  // 后端 messageType: user / ai / error
  const role: ChatMessage['role'] =
    record.messageType === 'user' ? 'user' : 'assistant'
  // 剥离可能的 JSON 外壳，得到纯 Markdown
  const content = extractAnswer(record.message || '')
  return {
    id: String(record.id ?? Date.now() + Math.random()),
    role,
    content,
    timestamp: new Date(record.createTime || Date.now()),
    displayedContent: content,
    createTime: record.createTime,
  }
}

/**
 * 从后端加载一页对话历史（游标查询）
 * @param reset 是否重置（首次加载），为 true 时清空当前消息
 */
const loadHistoryPage = async (reset = false) => {
  if (!appId.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: String(appId.value),
      pageSize: 10,
    }
    // 向前加载更多时，传入当前最早的 createTime
    if (!reset && oldestCreateTime.value) {
      params.lastCreateTime = oldestCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records ?? []
      console.log(`[加载历史] appId=${appId.value}, 返回记录数=${records.length}`)
      // 后端按 createTime DESC 返回，需要反转为升序展示
      const newMessages = records.map(chatHistoryToMessage).reverse()
      // 是否还有更早的历史：返回的条数等于 pageSize 时大概率还有
      hasMoreHistory.value = records.length >= 10

      if (reset) {
        messages.value = newMessages
        if (newMessages.length > 0) {
          // 首次加载时，oldestCreateTime 是最新页的最早一条
          oldestCreateTime.value = newMessages[0].createTime
          // 首次加载后滚动到底部，显示最新消息
          await nextTick()
          scrollToBottom()
        }
      } else {
        // 向前加载：把更早的消息插入到最前面
        // 记录当前滚动位置（相对于底部的距离）
        const container = messagesContainer.value
        const prevScrollHeight = container?.scrollHeight ?? 0
        // 插入到最前面
        messages.value = [...newMessages, ...messages.value]
        // 更新游标为更早的 createTime
        if (newMessages.length > 0) {
          oldestCreateTime.value = newMessages[0].createTime
        }
        // 保持滚动位置（视觉上不跳动）
        await nextTick()
        if (container) {
          const newScrollHeight = container.scrollHeight
          container.scrollTop = newScrollHeight - prevScrollHeight
        }
      }
    }
  } catch (e) {
    console.error('加载对话历史失败:', e)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 点击"加载更多"
const handleLoadMore = () => {
  loadHistoryPage(false)
}

// 滚动到底部，显示最新消息
const scrollToBottom = async () => {
  await nextTick()
  const container = messagesContainer.value
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

/**
 * 从可能的 JSON 外壳中提取 answer 字段
 * 后端 LangChain4j 流式模式下，AI 可能直接输出 {"answer": "..."} 格式
 * 此时需要剥离外壳，只展示真实的 Markdown 内容
 */
const extractAnswer = (raw: string): string => {
  if (!raw) return ''
  let content = raw
  // 快速判断：必须是 { 开头 } 结尾才尝试 JSON 解析
  const trimmed = raw.trim()
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    try {
      const parsed = JSON.parse(trimmed)
      if (parsed && typeof parsed.answer === 'string') {
        content = parsed.answer
      } else if (parsed && typeof parsed.data === 'string') {
        content = parsed.data
      }
    } catch {
      // 不是合法 JSON，按原始内容处理
    }
  }
  // 兜底：如果内容里混入了字面 \n / \r\n / \t（反斜杠+n 两字符），转回真正的换行/制表符
  // 某些 AI 流式返回会把换行符编码成字面字符串，导致 marked 无法识别代码块边界
  if (content.includes('\\n') || content.includes('\\t') || content.includes('\\r')) {
    content = content
      .replace(/\\r\\n/g, '\n')
      .replace(/\\r/g, '\n')
      .replace(/\\n/g, '\n')
      .replace(/\\t/g, '\t')
  }
  return content
}

/**
 * 从 AI 响应文本中解析出所有代码文件
 * 匹配格式：
 * [工具调用] 写入文件 src/App.vue
 * ```vue
 * <content>
 * ```
 */
const parseCodeFiles = (text: string, codeGenType?: string): CodeFile[] => {
  const files: CodeFile[] = []
  const addOrUpdate = (path: string, language: string, content: string) => {
    const name = path.split('/').pop() || path
    const existing = files.find((f) => f.path === path)
    if (existing) {
      existing.content = content
    } else {
      files.push({ path, name, language, content })
    }
  }

  // 1. VUE_PROJECT 模式：匹配 [工具调用] 写入文件 <path> 后紧跟的代码块
  const toolRegex = /\[工具调用\] 写入文件\s+(\S+)\s*\n```(\w*)\n([\s\S]*?)```/g
  let match: RegExpExecArray | null
  while ((match = toolRegex.exec(text)) !== null) {
    addOrUpdate(match[1], match[2] || 'text', match[3].trim())
  }

  // 2. 如果没匹配到工具调用格式，尝试解析标准 Markdown 代码块（HTML / MULTI_FILE 模式）
  if (files.length === 0) {
    const blockRegex = /```(\w+)\n([\s\S]*?)```/g
    const langFileMap: Record<string, { path: string; lang: string }> = {
      html: { path: 'index.html', lang: 'html' },
      css: { path: 'style.css', lang: 'css' },
      javascript: { path: 'script.js', lang: 'javascript' },
      js: { path: 'script.js', lang: 'javascript' },
    }
    while ((match = blockRegex.exec(text)) !== null) {
      const lang = match[1].toLowerCase()
      const content = match[2].trim()
      const mapping = langFileMap[lang]
      if (mapping && content) {
        addOrUpdate(mapping.path, mapping.lang, content)
      }
    }
  }

  return files
}

/**
 * 根据后缀名推断语言标识（用于语法高亮）
 */
const getLanguageBySuffix = (fileName: string): string => {
  const suffix = fileName.split('.').pop()?.toLowerCase() || ''
  const langMap: Record<string, string> = {
    vue: 'vue', js: 'javascript', ts: 'typescript',
    jsx: 'jsx', tsx: 'tsx', css: 'css', scss: 'scss',
    html: 'html', json: 'json', md: 'markdown',
  }
  return langMap[suffix] || 'text'
}

// 获取应用信息 + 加载历史
const fetchAppInfo = async () => {
  const id = route.query.appId as string
  const prompt = route.query.initPrompt as string

  // 严格校验 id，拦截 undefined / null / "null" / "undefined" 等无效字符串
  if (!id || id === 'null' || id === 'undefined') {
    message.error('应用ID不能为空')
    router.push('/home')
    return
  }

  appId.value = id
  initPrompt.value = prompt || ''

  try {
    // 1. 先获取应用信息
    const res = await getAppVoById({ id })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      // 构建预览 URL（生成网站浏览地址，与部署地址不同）
      const codeGenType = res.data.data.codeGenType || 'multi_file'
      // Vue 项目已在 getStaticPreviewUrl 中拼接 dist/index.html，其他类型需补 index.html
      const rawUrl = getStaticPreviewUrl(codeGenType, String(id))
      previewUrl.value = codeGenType === 'vue_project' ? rawUrl : `${rawUrl}index.html`

      // 部署地址
      if (res.data.data.deployKey) {
        deployedUrl.value = getDeployUrl(res.data.data.deployKey)
      }

      // 2. 加载最近一页对话历史（10 条）
      await loadHistoryPage(true)
      const hasHistory = messages.value.length > 0

      // 2.1 从历史 AI 消息中解析代码文件，填充 Tab 列表
      if (hasHistory) {
        const allAiContent = messages.value
          .filter((m) => m.role === 'assistant')
          .map((m) => m.content || '')
          .join('\n')
        const parsedFiles = parseCodeFiles(allAiContent, appInfo.value?.codeGenType)
        if (parsedFiles.length > 0) {
          codeFiles.value = parsedFiles
        }
      }

      // 3. 已有对话历史或已部署的应用，自动展示预览网站
      //    不再依赖 messages.length >= 2，只要有任何历史记录或已部署即展示预览
      if (hasHistory || res.data.data.deployKey) {
        showPreview.value = true
        // 显式加载预览 HTML，不依赖 watch 确保可靠触发
        loadPreviewHtml()
      }

      // 4. 仅当：是 owner + 没有历史 + 有 initPrompt 时，才自动发送初始消息（新建应用场景）
      if (isOwner.value && !hasHistory && initPrompt.value) {
        await sendMessage(initPrompt.value)
      }
    }
  } catch (error) {
    message.error('获取应用信息失败')
  }
}

// 发送消息
const sendMessage = async (msg?: string) => {
  // 非所有者不允许对话（自动发送仅发生在自己新建应用时，此时 isOwner 已就绪）
  if (appInfo.value && !isOwner.value) {
    message.warning('无法在别人的作品下对话哦~')
    return
  }

  const content = msg || inputMessage.value.trim()
  if (!content) {
    return
  }

  // 编辑模式：将选中元素信息附加到实际发送给后端的内容中
  // 用户消息气泡只展示原始输入，元素上下文静默附加
  let sendContent = content
  const hasSelectedElement = !!selectedElement.value
  if (hasSelectedElement) {
    const elementContext = formatElementForPrompt(selectedElement.value!)
    sendContent = content + '\n\n' + elementContext
  }

  // 添加用户消息（界面展示原始输入，不含元素上下文）
  const userMessage: ChatMessage = {
    id: Date.now().toString(),
    role: 'user',
    content,
    timestamp: new Date(),
    displayedContent: content, // 用户消息直接显示全部内容
  }
  messages.value.push(userMessage)
  // 用户消息追加后滚动到底部
  await scrollToBottom()

  // 清空输入框
  inputMessage.value = ''
  inputDisabled.value = true
  isGenerating.value = true

  // 发送后清除选中元素并退出编辑模式
  if (hasSelectedElement) {
    exitEditMode()
  }

  // 重置进度条
  generationProgress.value = 0
  progressStatus.value = 'active'

  // 创建 AI 消息占位
  const aiMessage: ChatMessage = {
    id: (Date.now() + 1).toString(),
    role: 'assistant',
    content: '',
    timestamp: new Date(),
    isStreaming: true,
    displayedContent: '', // 打字机效果显示内容
  }
  messages.value.push(aiMessage)
  // AI 占位消息追加后滚动到底部
  await scrollToBottom()
  // 保存引用以便后续更新
  const currentAiMessage = messages.value[messages.value.length - 1]

  // 使用 fetch 进行 SSE 连接（使用相对路径，通过 Vite 代理）
  try {
    const url = `/api/app/chat/gen/code?appId=${appId.value}&message=${encodeURIComponent(sendContent)}`

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
      headers: {
        'Accept': 'text/event-stream',
      },
    })

    if (!response.ok) {
      throw new Error(`网络响应失败: ${response.status} ${response.statusText}`)
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()

    if (!reader) {
      throw new Error('无法读取响应流')
    }

    let fullContent = ''
    let buffer = '' // 缓冲区，用于处理不完整的数据块
    let chunkCount = 0 // 用于计算进度
    let currentEventType = '' // 跟踪当前 SSE 事件类型

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk

      // 按行分割，但保留最后一个可能不完整的行
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmedLine = line.trim()

        if (!trimmedLine) continue // 跳过空行

        console.log('SSE 收到行:', trimmedLine)

        // 处理事件行: event: done
        if (trimmedLine.startsWith('event:')) {
          const eventName = trimmedLine.slice(6).trim()
          currentEventType = eventName
          console.log('事件类型:', eventName)
          if (eventName === 'done') {
            // 收到完成事件
            console.log('SSE 完成，最终内容长度:', fullContent.length)

            inputDisabled.value = false
            isGenerating.value = false
            generationProgress.value = 100
            progressStatus.value = 'success'

            if (currentAiMessage) {
              currentAiMessage.isStreaming = false
              // 确保内容被正确设置
              if (fullContent && fullContent.trim()) {
                // 剥离可能的 JSON 外壳 {"answer": "..."}，得到纯 Markdown
                const cleanContent = extractAnswer(fullContent)
                currentAiMessage.content = cleanContent
                currentAiMessage.displayedContent = cleanContent
                // 从 AI 响应中解析代码文件，更新 Tab 列表
                const parsedFiles = parseCodeFiles(cleanContent, appInfo.value?.codeGenType)
                if (parsedFiles.length > 0) {
                  codeFiles.value = parsedFiles
                  // 保持预览 Tab 为默认，不自动切换到代码视图
                  // 用户可手动点击文件 Tab 查看代码
                }
                // 调试日志：检查代码块标记是否完整
                const fenceCount = (cleanContent.match(/```/g) || []).length
                console.log('[SSE完成] 原始长度:', fullContent.length, '清洗后长度:', cleanContent.length, '代码块标记数:', fenceCount, '是否为偶数(完整):', fenceCount % 2 === 0)
                console.log('[SSE完成] 内容前200字符:', JSON.stringify(cleanContent.slice(0, 200)))
              } else {
                // 如果内容仍然为空，设置默认内容
                const defaultContent = '✅ 应用代码已生成完成！'
                currentAiMessage.content = defaultContent
                currentAiMessage.displayedContent = defaultContent
                console.log('AI内容为空，设置默认内容')
              }
            }
            showPreview.value = true
            // 强制重新加载预览（watch 仅在 false→true 时触发，重复设 true 不生效）
            loadPreviewHtml()
            message.success('代码生成完成')
            // 生成完成后滚动到底部
            await scrollToBottom()
          }
          // 重置事件类型标记（已消费）
          if (currentEventType === 'done' || currentEventType === 'business-error') {
            currentEventType = ''
          }
        }
        // 处理数据行: data: {"data": "chunk"}
        else if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.slice(5).trim()

          // 跳过空数据
          if (!data) continue

          // 处理 business-error 事件的数据行（后端限流等错误）
          if (currentEventType === 'business-error') {
            currentEventType = ''
            try {
              const errorData = JSON.parse(data)
              console.error('SSE业务错误事件:', errorData)
              // 显示具体的错误信息
              const errorMessage = errorData.message || '生成过程中出现错误'
              currentAiMessage.content = `❌ ${errorMessage}`
              currentAiMessage.displayedContent = `❌ ${errorMessage}`
              currentAiMessage.isStreaming = false
              message.error(errorMessage)
              isGenerating.value = false
              progressStatus.value = 'exception'
            } catch (parseError) {
              console.error('解析错误事件失败:', parseError, '原始数据:', data)
            }
            continue
          }

          try {
            const parsed = JSON.parse(data)
            // 后端返回格式是 {"data": "chunk"}（与后端 AppController#L94 对齐）
            const chunkText = parsed.data ?? parsed.d
            if (chunkText !== undefined) {
              console.log('收到代码片段:', chunkText)
              fullContent += chunkText
              chunkCount++

              // 更新进度条（模拟进度，基于接收的块数）
              // 假设大约需要 50 个块来完成，但设置上限为 95%
              const estimatedProgress = Math.min(95, Math.floor(chunkCount * 2))
              generationProgress.value = estimatedProgress

              if (currentAiMessage) {
                currentAiMessage.content = fullContent
                currentAiMessage.displayedContent = fullContent
                console.log('实时更新AI消息，当前长度:', fullContent.length)
                // 流式输出过程中始终滚动到底部，确保最新内容可见
                scrollToBottom()
              }
            }
          } catch (e) {
            console.error('解析 SSE 数据失败:', e, data)
          }
        }
      }
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    message.error(`发送失败：${error instanceof Error ? error.message : '请稍后重试'}`)
    inputDisabled.value = false
    isGenerating.value = false
    progressStatus.value = 'exception'
    if (currentAiMessage) {
      currentAiMessage.isStreaming = false
    }
  }
}

// 部署应用
const handleDeploy = async () => {
  if (!appId.value) {
    message.error('应用ID不能为空')
    return
  }

  deployLoading.value = true
  try {
    const res = await deployApp({ appId: appId.value })
    if (res.data.code === 0 && res.data.data) {
      deployedUrl.value = res.data.data
      message.success('部署成功！')
      // 打开新窗口访问
      window.open(res.data.data, '_blank')
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    message.error('部署失败，请稍后重试')
  } finally {
    deployLoading.value = false
  }
}

// 下载应用代码
const handleDownload = async () => {
  if (!appId.value) {
    message.error('应用ID不能为空')
    return
  }
  downloadLoading.value = true
  try {
    const res = await downloadAppCode(
      { appId: appId.value },
      { responseType: 'blob' },
    )
    // 从响应头 Content-Disposition 中解析文件名
    const disposition = res.headers['content-disposition'] || ''
    let fileName = `${appId.value}.zip`
    const match = disposition.match(/filename="?([^"]+)"?/)
    if (match) {
      fileName = match[1]
    }
    // 创建下载链接
    const blob = new Blob([res.data], { type: 'application/zip' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('代码下载已开始')
  } catch (error) {
    message.error('下载失败，请稍后重试')
  } finally {
    downloadLoading.value = false
  }
}

// 打开应用详情模态框
const openAppDetailModal = () => {
  showAppDetailModal.value = true
}

// 详情弹窗：修改
const handleDetailEdit = (app: API.AppVO) => {
  if (!app?.id) {
    message.error('应用ID不能为空')
    return
  }
  showAppDetailModal.value = false
  router.push({
    path: '/app/edit',
    query: { appId: String(app.id) },
  })
}

// 详情弹窗：删除后返回首页
const handleDetailDeleted = () => {
  router.push('/home')
}

// 加载预览 HTML（通过 Blob URL 强制浏览器以 HTML 渲染）
const loadPreviewHtml = async (retryCount = 0) => {
  if (!previewUrl.value) return

  // 首次尝试时设置加载状态
  if (retryCount === 0) {
    previewLoading.value = true
    previewError.value = false
  }

  try {
    const response = await fetch(previewUrl.value, {
      credentials: 'include',
    })
    if (response.ok) {
      let html = await response.text()

      // 获取原始 URL 的基础路径（支持相对路径）
      const urlObj = new URL(previewUrl.value, window.location.origin)
      const baseUrl = `${urlObj.protocol}//${urlObj.host}${urlObj.pathname.split('/').slice(0, -1).join('/')}/`

      // 在 HTML 中插入 base 标签，使相对路径资源能正确加载
      const baseTag = `<base href="${baseUrl}">`

      if (html.includes('<head>')) {
        html = html.replace('<head>', `<head>${baseTag}`)
      } else if (html.includes('<html>')) {
        html = html.replace('<html>', `<html><head>${baseTag}</head>`)
      } else {
        html = baseTag + html
      }

      // 注入可视化编辑脚本，使其能响应主页面发来的编辑模式指令
      const editorScript = generateEditorScript()
      if (html.includes('</body>')) {
        html = html.replace('</body>', `${editorScript}</body>`)
      } else {
        html = html + editorScript
      }

      const blob = new Blob([html], { type: 'text/html' })
      // 释放之前的 blob URL
      if (previewBlobUrl.value) {
        URL.revokeObjectURL(previewBlobUrl.value)
      }
      previewBlobUrl.value = URL.createObjectURL(blob)
      previewLoading.value = false
      previewError.value = false
    } else {
      // 如果返回 404，可能是后端还在构建中，延迟重试
      if (response.status === 404 && retryCount < 150) {
        console.log(`预览文件未就绪，2秒后重试 (${retryCount + 1}/150)`)
        setTimeout(() => loadPreviewHtml(retryCount + 1), 2000)
      } else {
        console.error('加载预览失败，状态码:', response.status)
        previewLoading.value = false
        previewError.value = true
      }
    }
  } catch (error) {
    console.error('加载预览失败:', error)
    // 网络错误也重试
    if (retryCount < 150) {
      setTimeout(() => loadPreviewHtml(retryCount + 1), 2000)
    } else {
      previewLoading.value = false
      previewError.value = true
    }
  }
}

// 监听 showPreview 变化，加载预览内容（SSE done 事件触发时使用）
watch(showPreview, (newVal) => {
  if (newVal && !previewLoading.value && !previewBlobUrl.value) {
    loadPreviewHtml()
  }
})

// ============ 可视化编辑模式相关 ============

/**
 * 切换编辑模式开关
 * 仅在有预览且是应用所有者时可用
 */
const toggleEditMode = () => {
  if (!showPreview.value || !previewBlobUrl.value) {
    message.warning('请先生成或加载预览后再进入编辑模式')
    return
  }
  isEditMode.value = !isEditMode.value
  if (isEditMode.value) {
    // 确保切到预览 Tab，使 iframe 被渲染（否则 previewFrame 为 null）
    activeTab.value = 'preview'
    // 等 iframe 渲染 + 内容加载后再发消息
    nextTick(() => {
      setTimeout(() => {
        enableIframeEditMode(previewFrame.value)
      }, 200)
    })
    message.info('已进入编辑模式，点击预览中的元素以选中')
  } else {
    if (previewFrame.value) {
      disableIframeEditMode(previewFrame.value)
    }
    selectedElement.value = null
    message.info('已退出编辑模式')
  }
}

/** 退出编辑模式（发送消息后调用） */
const exitEditMode = () => {
  isEditMode.value = false
  selectedElement.value = null
  if (previewFrame.value) {
    disableIframeEditMode(previewFrame.value)
  }
}

/** 手动清除选中的元素（Alert 关闭时） */
const clearSelectedElement = () => {
  selectedElement.value = null
}

/**
 * 监听 iframe 发来的 postMessage，接收选中的元素信息
 */
const handleMessage = (event: MessageEvent) => {
  if (isElementSelectedMessage(event)) {
    selectedElement.value = event.data.element
    message.success('已选中元素，可在输入框中描述修改需求')
  }
}

// 回车发送
const handleKeyPress = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 返回首页
const goBack = () => {
  router.push('/home')
}

onMounted(() => {
  // 注册 iframe 消息监听，接收选中元素信息
  window.addEventListener('message', handleMessage)
  fetchAppInfo()
})

onUnmounted(() => {
  window.removeEventListener('message', handleMessage)
})
</script>

<template>
  <div class="app-chat-page">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <div class="top-bar-left">
        <a-button type="text" @click="goBack">
          <LeftOutlined />
        </a-button>
        <h1 class="app-title">{{ appInfo?.appName || '应用对话' }}</h1>
      </div>
      <div class="top-bar-right">
        <a-tag v-if="deployedUrl" color="success">
          已部署: <a :href="deployedUrl" target="_blank">{{ deployedUrl }}</a>
        </a-tag>
        <a-button @click="openAppDetailModal">
          <InfoCircleOutlined />
          应用详情
        </a-button>
        <a-button
          :loading="downloadLoading"
          :disabled="isGenerating || !showPreview"
          @click="handleDownload"
        >
          <DownloadOutlined />
          下载代码
        </a-button>
        <a-button
          type="primary"
          :loading="deployLoading"
          :disabled="isGenerating || !showPreview"
          @click="handleDeploy"
        >
          <CloudUploadOutlined />
          部署应用
        </a-button>
      </div>
    </div>

    <!-- 核心内容区域 -->
    <div class="content-area">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <div ref="messagesContainer" class="messages-container">
          <!-- 加载更多历史消息 -->
          <div v-if="messages.length > 0 && hasMoreHistory" class="load-more">
            <a-button size="small" :loading="loadingHistory" @click="handleLoadMore">
              <HistoryOutlined />
              加载更多历史消息
            </a-button>
          </div>

          <div v-if="messages.length === 0 && !loadingHistory" class="empty-messages">
            <p>开始与 AI 对话来生成你的应用吧！</p>
            <p class="tip">提示：你可以描述你想要的功能、界面风格等</p>
          </div>

          <!-- 消息列表 -->
          <template v-if="messages.length > 0">
            <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
              <div class="message-avatar">
                <!-- 用户消息：显示当前登录用户头像 -->
                <img
                  v-if="msg.role === 'user' && loginUserStore.loginUser?.userAvatar"
                  :src="loginUserStore.loginUser.userAvatar"
                  alt="用户头像"
                  class="user-avatar-img"
                  @error="(e) => (e.target as HTMLImageElement).style.display = 'none'"
                />
                <UserOutlined v-else-if="msg.role === 'user'" />
                <!-- AI 消息：显示 AI 头像 -->
                <img v-else :src="aiAvatar" alt="AI" class="ai-avatar-img" />
              </div>
              <div class="message-content">
                <div class="message-bubble">
                  <!-- 用户消息，直接显示 -->
                  <div v-if="msg.role === 'user'" class="message-text">
                    {{ msg.content }}
                  </div>

                  <!-- AI 消息且正在生成中但没有内容，显示加载动画 -->
                  <div v-else-if="msg.role === 'assistant' && msg.isStreaming && !msg.content" class="loading-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>

                  <!-- AI 消息，使用 Markdown 渲染 -->
                  <div v-else-if="msg.role === 'assistant'" class="parsed-content">
                    <MarkdownRenderer :content="msg.displayedContent || msg.content || ''" />
                    <!-- 显示打字机效果的游标 -->
                    <span v-if="msg.isStreaming" class="typing-cursor">|</span>
                  </div>
                </div>

                <!-- AI 生成进度条（Uiverse.io by ilkhoeri） -->
                <div v-if="msg.role === 'assistant' && msg.isStreaming" class="generation-progress">
                  <div class="uivo-progress" role="progressbar" :aria-valuenow="generationProgress" aria-valuemin="0" aria-valuemax="100">
                    <div class="uivo-progress-bar">
                      <span class="uivo-progress-fill" :style="{ width: generationProgress + '%' }"></span>
                    </div>
                    <div class="uivo-progress-info">
                      <span class="uivo-progress-label">
                        {{ progressStatus === 'exception' ? '生成失败' : (generationProgress >= 100 ? '代码生成完成' : 'AI 正在生成代码') }}
                      </span>
                      <span class="uivo-progress-percent">{{ generationProgress }}%</span>
                    </div>
                  </div>
                </div>

                <div class="message-time">{{ new Date(msg.timestamp).toLocaleTimeString() }}</div>
              </div>
            </div>
          </template>
        </div>
        <div class="input-area">
          <!-- 用 span 包裹，确保 disabled 时 tooltip 仍可触发 -->
          <a-tooltip :title="!isOwner ? inputDisabledReason : ''" placement="top">
            <span class="input-tooltip-wrap">
              <a-textarea
                v-model:value="inputMessage"
                :placeholder="isOwner ? (isEditMode ? '描述你想对该元素的修改...' : '输入你的需求，让 AI 帮你生成代码...') : '无法在别人的作品下对话哦~'"
                :disabled="isInputDisabled"
                :auto-size="{ minRows: 1, maxRows: 6 }"
                @keypress="handleKeyPress"
                class="message-input"
              />
            </span>
          </a-tooltip>
          <a-button
            type="primary"
            :disabled="!inputMessage.trim() || isInputDisabled"
            :loading="isGenerating"
            @click="sendMessage()"
            class="send-button"
          >
            <SendOutlined />
            发送
          </a-button>
        </div>
      </div>

      <!-- 右侧预览区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>应用预览</h3>
          <a-space>
            <!-- 编辑/退出编辑按钮 -->
            <a-button
              v-if="isOwner && showPreview"
              size="small"
              :type="isEditMode ? 'primary' : 'default'"
              :danger="isEditMode"
              @click="toggleEditMode"
            >
              <EditOutlined />
              {{ isEditMode ? '退出编辑' : '编辑' }}
            </a-button>
            <a-button v-if="showPreview" size="small" @click="loadPreviewHtml()">
              <ReloadOutlined />
              刷新
            </a-button>
          </a-space>
        </div>
        <!-- 选中元素信息显示区域（始终占位，编辑模式下高亮） -->
        <div v-if="isEditMode" class="selected-element-bar">
          <div v-if="selectedElement" class="selected-element-info">
            <div class="selected-element-label">
              <span class="info-prefix">已选中：</span>
              <code class="element-tag">
                &lt;{{ selectedElement.tag }}{{ selectedElement.id ? ' id="' + selectedElement.id + '"' : '' }}{{ selectedElement.className ? ' class="' + selectedElement.className + '"' : '' }}&gt;
              </code>
              <a-button type="link" size="small" class="clear-btn" @click="clearSelectedElement">
                移除
              </a-button>
            </div>
            <div v-if="selectedElement.text || selectedElement.cssSelector" class="selected-element-detail">
              <span v-if="selectedElement.text" class="element-text">{{ selectedElement.text }}</span>
              <span v-if="selectedElement.cssSelector" class="element-selector">{{ selectedElement.cssSelector }}</span>
            </div>
          </div>
          <div v-else class="selected-element-placeholder">
            <InfoCircleOutlined />
            <span>点击预览中的任意元素以选中它</span>
          </div>
        </div>
        <!-- 代码文件 Tab 栏（有代码文件时显示） -->
        <div v-if="codeFiles.length > 0" class="code-tabs-bar">
          <div
            class="code-tab"
            :class="{ active: activeTab === 'preview' }"
            @click="activeTab = 'preview'"
          >
            <span class="text">预览</span>
          </div>
          <div
            v-for="file in codeFiles"
            :key="file.path"
            class="code-tab"
            :class="{ active: activeTab === file.path }"
            :title="file.path"
            @click="activeTab = file.path"
          >
            <span class="text">{{ file.name }}</span>
          </div>
        </div>
        <div class="preview-content">
          <!-- 代码生成中：打字机加载动画 -->
          <div v-if="isGenerating" class="preview-loading">
            <div class="typewriter">
              <div class="slide"><i></i></div>
              <div class="paper"></div>
              <div class="keyboard"></div>
            </div>
            <p class="loading-text">AI 正在努力生成应用...</p>
            <p class="loading-subtext">通常需要 10-30 秒，请耐心等待</p>
          </div>
          <!-- 预览加载中（历史应用加载预览 HTML） -->
          <div v-else-if="previewLoading" class="preview-loading">
            <a-spin size="large" />
            <p class="loading-text">正在加载应用预览...</p>
          </div>
          <!-- 预览加载失败 -->
          <div v-else-if="previewError" class="preview-placeholder">
            <CodeOutlined class="placeholder-icon" />
            <p>预览加载失败</p>
            <a-button size="small" type="primary" @click="loadPreviewHtml()">
              <ReloadOutlined />
              重新加载
            </a-button>
          </div>
          <!-- 无内容且非生成中：占位提示 -->
          <div v-else-if="!showPreview" class="preview-placeholder">
            <CodeOutlined class="placeholder-icon" />
            <p>代码生成后将在此处预览</p>
          </div>
          <!-- 代码视图（选中某个文件 Tab 时） -->
          <div
            v-else-if="activeTab !== 'preview'"
            class="code-view"
          >
            <pre><code>{{ codeFiles.find(f => f.path === activeTab)?.content }}</code></pre>
          </div>
          <!-- 展示预览结果 -->
          <iframe
            v-else
            ref="previewFrame"
            :src="previewBlobUrl"
            class="preview-frame"
            :class="{ 'edit-mode-active': isEditMode }"
            sandbox="allow-scripts allow-same-origin"
            scrolling="auto"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情模态框 -->
    <AppDetailModal
      v-model:open="showAppDetailModal"
      :app="appInfo"
      :can-edit="isOwner || loginUserStore.isAdmin"
      :can-delete="isOwner || loginUserStore.isAdmin"
      @edit="handleDetailEdit"
      @deleted="handleDetailDeleted"
    />
  </div>
</template>

<style scoped>
.app-chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部栏 */
.top-bar {
  height: 64px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 核心内容区域 */
.content-area {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-right: 1px solid #e8e8e8;
  min-width: 320px;
  max-width: 420px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-messages {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-messages .tip {
  font-size: 14px;
  margin-top: 8px;
  color: #bbb;
}

/* 加载更多按钮 */
.load-more {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 8px;
}

.message {
  display: flex;
  gap: 12px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.2);
}

.message.user .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message.assistant .message-avatar {
  background: transparent;
  color: white;
  overflow: hidden;
}

.ai-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

/* 用户真实头像：与 AI 头像尺寸保持一致 */
.user-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.message-content {
  max-width: 80%;
}

.message.user .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  word-break: break-word;
  overflow-x: auto;
  max-width: 100%;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message.user .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-bubble {
  background: #f5f7fa;
  color: #1a1a1a;
  border-bottom-left-radius: 4px;
}

.message-text {
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
}

/* 解析内容样式（Markdown 渲染由 MarkdownRenderer 组件接管） */
.parsed-content {
  margin: 0;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 6px;
  opacity: 0.8;
}

/* 生成中动画 */
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

/* 打字机效果游标 */
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 1.2em;
  background: #667eea;
  margin-left: 2px;
  animation: blink 1s infinite;
  vertical-align: text-bottom;
}

@keyframes blink {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}

/* AI 生成进度条（Uiverse.io by ilkhoeri 风格） */
.generation-progress {
  margin-top: 12px;
  animation: slideIn 0.3s ease-out;
}

.uivo-progress {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.uivo-progress-bar {
  position: relative;
  width: 100%;
  height: 8px;
  background: rgba(106, 17, 203, 0.12);
  border-radius: 999px;
  overflow: hidden;
}

.uivo-progress-fill {
  display: block;
  height: 100%;
  width: 0%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6a11cb 0%, #2575fc 100%);
  position: relative;
  transition: width 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: 0 0 8px rgba(106, 17, 203, 0.45);
}

/* 进度条上扫描的高光条 */
.uivo-progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.55) 50%,
    transparent 100%
  );
  transform: translateX(-100%);
  animation: uivo-progress-scan 1.6s linear infinite;
}

@keyframes uivo-progress-scan {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.uivo-progress-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.uivo-progress-label {
  color: #555;
  font-weight: 500;
}

.uivo-progress-percent {
  color: #6a11cb;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 输入区域 */
.input-area {
  padding: 16px 20px;
  border-top: 1px solid #e8e8e8;
  display: flex;
  gap: 12px;
  align-items: flex-end;
  background: #fafafa;
}

.input-tooltip-wrap {
  flex: 1;
  display: block;
  min-width: 0;
}

.message-input {
  width: 100%;
}

.send-button {
  height: auto;
  min-height: 32px;
}

/* 可视化编辑 - 选中元素信息栏 */
.selected-element-bar {
  flex-shrink: 0;
  background: #fffbe6;
  border-bottom: 1px solid #ffe58f;
  padding: 8px 16px;
  font-size: 12px;
  transition: background 0.2s;
}

.selected-element-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.selected-element-label {
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-prefix {
  color: #d48806;
  font-weight: 500;
  flex-shrink: 0;
}

.element-tag {
  background: #e6f7ff;
  padding: 1px 6px;
  border-radius: 3px;
  color: #1890ff;
  font-family: 'Consolas', 'Monaco', monospace;
  word-break: break-all;
}

.clear-btn {
  margin-left: auto;
  padding: 0 4px;
  height: auto;
  font-size: 12px;
}

.selected-element-detail {
  padding-left: 48px;
  color: #888;
}

.element-text {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.element-selector {
  display: block;
  color: #999;
  font-family: 'Consolas', 'Monaco', monospace;
  word-break: break-all;
}

.selected-element-placeholder {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #d48806;
}

/* 编辑模式激活时，iframe 增加边框提示 */
.preview-frame.edit-mode-active {
  outline: 2px solid #fa541c;
  outline-offset: -2px;
}

/* 代码文件 Tab 栏 - 参考 UiF.html 深色按压按钮风格，IDEA 风格横向滚动 */
.code-tabs-bar {
  display: flex;
  align-items: center;
  gap: 2px;
  background-color: black;
  padding: 4px;
  flex-shrink: 0;
  height: 56px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: thin;
  scrollbar-color: #555 transparent;
}

.code-tabs-bar::-webkit-scrollbar {
  height: 4px;
}

.code-tabs-bar::-webkit-scrollbar-track {
  background: transparent;
}

.code-tabs-bar::-webkit-scrollbar-thumb {
  background: #555;
  border-radius: 2px;
}

.code-tabs-bar::-webkit-scrollbar-thumb:hover {
  background: #777;
}

.code-tab {
  flex-shrink: 0;
  min-width: 80px;
  max-width: 140px;
  height: 44px;
  background: linear-gradient(to bottom, #333333, rgb(36, 35, 35));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  transition: all 0.1s linear;
  border-top: 1px solid #4e4d4d;
  background-color: #333333;
  position: relative;
  cursor: pointer;
  box-shadow: 0px 8px 5px 1px rgba(0, 0, 0, 0.2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  user-select: none;
}

.code-tab:first-child {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}

.code-tab:last-child {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}

.code-tab:hover {
  filter: brightness(1.1);
}

.code-tab.active {
  box-shadow: 0px 8px 5px 1px rgba(0, 0, 0, 0);
  background: linear-gradient(to bottom, #1d1d1d, #1d1d1d);
  border-top: none;
}

.code-tab::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 103%;
  height: 100%;
  border-radius: 10px;
  background: linear-gradient(
    to bottom,
    transparent 10%,
    transparent,
    transparent 90%
  );
  transition: all 0.1s linear;
  z-index: 0;
  pointer-events: none;
}

.code-tab.active::before {
  background: linear-gradient(
    to bottom,
    transparent 10%,
    #cae2fd63,
    transparent 90%
  );
}

.code-tab .text {
  position: relative;
  z-index: 1;
  color: #8a8a8a;
  font-size: 13px;
  font-weight: 800;
  text-transform: uppercase;
  transition: all 0.1s linear;
  text-shadow:
    -1px -1px 1px rgba(224, 224, 224, 0.1),
    0px 2px 3px rgba(0, 0, 0, 0.3);
}

.code-tab.active .text {
  color: rgb(202, 226, 253);
  text-shadow: 0px 0px 12px #cae2fd;
}

/* 代码视图 */
.code-view {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: auto;
  background: #1e1e1e;
  margin: 0;
}

.code-view pre {
  margin: 0;
  padding: 16px;
  min-width: max-content;
}

.code-view code {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #d4d4d4;
  white-space: pre;
}

/* 代码视图自定义滚动条（与代码 Tab 栏样式保持一致） */
.code-view::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.code-view::-webkit-scrollbar-track {
  background: #1e1e1e;
}

.code-view::-webkit-scrollbar-thumb {
  background: #4e4d4d;
  border-radius: 5px;
}

.code-view::-webkit-scrollbar-thumb:hover {
  background: #6a6a6a;
}

/* 右侧预览区域 */
.preview-section {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  overflow: hidden;
}

.preview-header {
  height: 48px;
  flex-shrink: 0;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
}

.preview-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #666;
}

.preview-content {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.generating-hint {
  margin-top: 8px;
  font-size: 14px;
  color: #667eea;
}

/* === 生成中加载动画（Uiverse.io by Nawsome） === */
.preview-loading {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf3 100%);
  padding: 20px;
}

.loading-text {
  margin-top: 32px;
  font-size: 16px;
  font-weight: 600;
  color: #2a2a72;
}

.loading-subtext {
  margin-top: 8px;
  font-size: 13px;
  color: #999;
}

/* 打字机加载动画（Uiverse.io by dilmurod_3869） */
.typewriter {
  --blue: #5c86ff;
  --blue-dark: #1e0325;
  --key: #fff;
  --paper: #eef0fd;
  --text: #d3d4ec;
  --tool: #fbc56c;
  --duration: 3s;
  position: relative;
  -webkit-animation: bounce05 var(--duration) linear infinite;
  animation: bounce05 var(--duration) linear infinite;
}

.typewriter .slide {
  width: 92px;
  height: 20px;
  border-radius: 3px;
  margin-left: 14px;
  transform: translateX(14px);
  background: linear-gradient(var(--blue), var(--blue-dark));
  -webkit-animation: slide05 var(--duration) ease infinite;
  animation: slide05 var(--duration) ease infinite;
}

.typewriter .slide:before,
.typewriter .slide:after,
.typewriter .slide i:before {
  content: "";
  position: absolute;
  background: var(--tool);
}

.typewriter .slide:before {
  width: 2px;
  height: 8px;
  top: 6px;
  left: 100%;
}

.typewriter .slide:after {
  left: 94px;
  top: 3px;
  height: 14px;
  width: 6px;
  border-radius: 3px;
}

.typewriter .slide i {
  display: block;
  position: absolute;
  right: 100%;
  width: 6px;
  height: 4px;
  top: 4px;
  background: var(--tool);
}

.typewriter .slide i:before {
  right: 100%;
  top: -2px;
  width: 4px;
  border-radius: 2px;
  height: 14px;
}

.typewriter .paper {
  position: absolute;
  left: 24px;
  top: -26px;
  width: 40px;
  height: 46px;
  border-radius: 5px;
  background: var(--paper);
  transform: translateY(46px);
  -webkit-animation: paper05 var(--duration) linear infinite;
  animation: paper05 var(--duration) linear infinite;
}

.typewriter .paper:before {
  content: "";
  position: absolute;
  left: 6px;
  right: 6px;
  top: 7px;
  border-radius: 2px;
  height: 4px;
  transform: scaleY(0.8);
  background: var(--text);
  box-shadow:
    0 12px 0 var(--text),
    0 24px 0 var(--text),
    0 36px 0 var(--text);
}

.typewriter .keyboard {
  width: 120px;
  height: 56px;
  margin-top: -10px;
  z-index: 1;
  position: relative;
}

.typewriter .keyboard:before,
.typewriter .keyboard:after {
  content: "";
  position: absolute;
}

.typewriter .keyboard:before {
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 7px;
  background: linear-gradient(135deg, var(--blue), var(--blue-dark));
  transform: perspective(10px) rotateX(2deg);
  transform-origin: 50% 100%;
}

.typewriter .keyboard:after {
  left: 2px;
  top: 25px;
  width: 11px;
  height: 4px;
  border-radius: 2px;
  box-shadow:
    15px 0 0 var(--key),
    30px 0 0 var(--key),
    45px 0 0 var(--key),
    60px 0 0 var(--key),
    75px 0 0 var(--key),
    90px 0 0 var(--key),
    22px 10px 0 var(--key),
    37px 10px 0 var(--key),
    52px 10px 0 var(--key),
    60px 10px 0 var(--key),
    68px 10px 0 var(--key),
    83px 10px 0 var(--key);
  -webkit-animation: keyboard05 var(--duration) linear infinite;
  animation: keyboard05 var(--duration) linear infinite;
}

@keyframes bounce05 {
  85%, 92%, 100% { transform: translateY(0); }
  89% { transform: translateY(-4px); }
  95% { transform: translateY(2px); }
}

@keyframes slide05 {
  5% { transform: translateX(14px); }
  15%, 30% { transform: translateX(6px); }
  40%, 55% { transform: translateX(0); }
  65%, 70% { transform: translateX(-4px); }
  80%, 89% { transform: translateX(-12px); }
  100% { transform: translateX(14px); }
}

@keyframes paper05 {
  5% { transform: translateY(46px); }
  20%, 30% { transform: translateY(34px); }
  40%, 55% { transform: translateY(22px); }
  65%, 70% { transform: translateY(10px); }
  80%, 85% { transform: translateY(0); }
  92%, 100% { transform: translateY(46px); }
}

@keyframes keyboard05 {
  5%, 12%, 21%, 30%, 39%, 48%, 57%, 66%, 75%, 84% {
    box-shadow:
      15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key),
      60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key),
      22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key),
      60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key);
  }
  9% {
    box-shadow:
      15px 2px 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key),
      60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key),
      22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key),
      60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key);
  }
  18% {
    box-shadow:
      15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key),
      60px 2px 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key),
      22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key),
      60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key);
  }
  27% {
    box-shadow:
      15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key),
      60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key),
      22px 12px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key),
      60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key);
  }
  36% {
    box-shadow:
      15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key),
      60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key),
      22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 12px 0 var(--key),
      60px 12px 0 var(--key), 68px 12px 0 var(--key), 83px 10px 0 var(--key);
  }
  45% {
    box-shadow:
      15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key),
      60px 0 0 var(--key), 75px 0 0 var(--key), 90px 2px 0 var(--key),
      22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key),
      60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key);
  }
  54% {
    box-shadow:
      15px 0 0 var(--key), 30px 2px 0 var(--key), 45px 0 0 var(--key),
      60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key),
      22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key),
      60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key);
  }
  63% {
    box-shadow:
      15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key),
      60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key),
      22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key),
      60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 12px 0 var(--key);
  }
  72% {
    box-shadow:
      15px 0 0 var(--key), 30px 0 0 var(--key), 45px 2px 0 var(--key),
      60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key),
      22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key),
      60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key);
  }
  81% {
    box-shadow:
      15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key),
      60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key),
      22px 10px 0 var(--key), 37px 12px 0 var(--key), 52px 10px 0 var(--key),
      60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key);
  }
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: white;
}

/* 应用详情模态框样式已迁移至 AppDetailModal 组件 */
</style>
