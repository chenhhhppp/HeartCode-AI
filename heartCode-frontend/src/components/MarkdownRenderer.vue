<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { Marked } from 'marked'
import hljs from 'highlight.js/lib/core'
// 按需注册常用语言（HTML/XML、CSS、JS、TS、JSON、Bash、Markdown 等）
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import markdown from 'highlight.js/lib/languages/markdown'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
// highlight.js 主题：IDE 浅色风格（绿标签 + 紫属性 + 蓝值 + 浅绿注释 + 黑文本）
import 'highlight.js/styles/idea.css'
import DOMPurify from 'dompurify'

// 注册常用语言（别名覆盖常见代码块标记）
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('html5', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('jsx', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('tsx', typescript)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('sh', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('md', markdown)
hljs.registerLanguage('python', python)
hljs.registerLanguage('py', python)
hljs.registerLanguage('sql', sql)

interface Props {
  content: string
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
})

const containerRef = ref<HTMLElement>()

// 创建自定义 marked 实例，配置代码块渲染（含复制按钮）
const markedInstance = new Marked()

// 自定义 renderer：渲染代码块时，加上语言标签 + 高亮
const renderer = {
  code(code: string, infostring: string | undefined): string {
    const language = (infostring || '').trim().split(/\s+/)[0] || ''
    let highlighted: string
    try {
      if (language && hljs.getLanguage(language)) {
        highlighted = hljs.highlight(code, { language }).value
      } else {
        highlighted = hljs.highlightAuto(code).value
      }
    } catch (e) {
      // 高亮失败时转义 HTML，避免 XSS
      highlighted = escapeHtml(code)
    }
    const langLabel = language || 'code'
    const langClass = language ? `language-${language}` : ''
    // 使用 data 属性记录原始代码，便于复制按钮读取
    return `<div class="md-code-block">
      <div class="md-code-header">
        <span class="md-code-lang">${escapeHtml(langLabel)}</span>
        <button type="button" class="md-code-copy" data-action="copy">
          <span class="md-copy-text">复制</span>
        </button>
      </div>
      <pre class="md-code-pre"><code class="hljs ${langClass}">${highlighted}</code></pre>
    </div>`
  },
}

markedInstance.use({ renderer: renderer as any })

// 转义 HTML
function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// 配置 DOMPurify：允许代码高亮所需的标签与 class
const purifyConfig = {
  ADD_ATTR: ['data-action', 'target'],
  ALLOWED_TAGS: DOMPurify.sanitize('').length
    ? undefined
    : [
        // 保留常用 markdown 输出标签
        'p', 'br', 'hr', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
        'strong', 'em', 'del', 's', 'u', 'sub', 'sup', 'mark',
        'blockquote', 'code', 'pre',
        'ul', 'ol', 'li', 'dl', 'dt', 'dd',
        'table', 'thead', 'tbody', 'tr', 'th', 'td',
        'a', 'span', 'div', 'img',
        'input', // 任务列表勾选
        'button',
      ],
}

// 解析 + 消毒后的 HTML
const renderedHtml = computed(() => {
  const raw = props.content || ''
  if (!raw.trim()) return ''
  let html: string
  try {
    html = markedInstance.parse(raw, { async: false }) as string
  } catch (e) {
    html = `<p>${escapeHtml(raw)}</p>`
  }
  return DOMPurify.sanitize(html, purifyConfig) as string
})

// 复制代码：事件委托
const handleCopy = async (e: MouseEvent) => {
  const target = e.target as HTMLElement
  const btn = target.closest('[data-action="copy"]') as HTMLButtonElement | null
  if (!btn) return
  const block = btn.closest('.md-code-block') as HTMLElement | null
  if (!block) return
  const codeEl = block.querySelector('code') as HTMLElement | null
  if (!codeEl) return
  // 优先取原始文本
  const text = codeEl.textContent || ''
  try {
    await navigator.clipboard.writeText(text)
    const textEl = btn.querySelector('.md-copy-text')
    if (textEl) {
      const old = textEl.textContent
      textEl.textContent = '已复制'
      btn.classList.add('copied')
      setTimeout(() => {
        textEl.textContent = old || '复制'
        btn.classList.remove('copied')
      }, 1500)
    }
  } catch (err) {
    // 降级方案
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    try {
      document.execCommand('copy')
    } catch {}
    document.body.removeChild(ta)
  }
}

// 链接：新窗口打开（事件委托）
const handleClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.tagName === 'A') {
    const a = target as HTMLAnchorElement
    a.target = '_blank'
    a.rel = 'noopener noreferrer'
  }
}

onMounted(() => {
  if (containerRef.value) {
    containerRef.value.addEventListener('click', handleCopy)
    containerRef.value.addEventListener('click', handleClick)
  }
})

// 暴露给父组件：获取纯文本长度（可选）
defineExpose({
  getElement: () => containerRef.value,
})
</script>

<template>
  <div ref="containerRef" class="markdown-body" v-html="renderedHtml"></div>
</template>

<style scoped>
.markdown-body {
  font-size: 15px;
  line-height: 1.75;
  color: #1a1a1a;
  word-break: break-word;
}

/* 标题 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  margin: 1.2em 0 0.6em;
  font-weight: 600;
  line-height: 1.35;
}
.markdown-body :deep(h1) { font-size: 1.6em; padding-bottom: 0.3em; border-bottom: 1px solid #eee; }
.markdown-body :deep(h2) { font-size: 1.4em; padding-bottom: 0.3em; border-bottom: 1px solid #eee; }
.markdown-body :deep(h3) { font-size: 1.2em; }
.markdown-body :deep(h4) { font-size: 1.05em; }
.markdown-body :deep(h5) { font-size: 0.95em; }
.markdown-body :deep(h6) { font-size: 0.9em; color: #666; }
.markdown-body :deep(h1:first-child),
.markdown-body :deep(h2:first-child),
.markdown-body :deep(h3:first-child) { margin-top: 0.2em; }

/* 段落与换行 */
.markdown-body :deep(p) { margin: 0.6em 0; }

/* 强调 */
.markdown-body :deep(strong) { font-weight: 600; }
.markdown-body :deep(em) { font-style: italic; }
.markdown-body :deep(del) { color: #999; }
.markdown-body :deep(mark) { background: #fff3a0; padding: 0 2px; }

/* 链接 */
.markdown-body :deep(a) { color: #3a6df0; text-decoration: none; }
.markdown-body :deep(a:hover) { text-decoration: underline; }

/* 列表 */
.markdown-body :deep(ul),
.markdown-body :deep(ol) { margin: 0.5em 0; padding-left: 1.6em; }
.markdown-body :deep(li) { margin: 0.25em 0; }
.markdown-body :deep(li > ul),
.markdown-body :deep(li > ol) { margin: 0.25em 0; }

/* 引用 */
.markdown-body :deep(blockquote) {
  margin: 0.8em 0;
  padding: 0.4em 1em;
  border-left: 4px solid #667eea;
  background: #f6f8fa;
  color: #555;
}
.markdown-body :deep(blockquote p) { margin: 0.3em 0; }

/* 行内代码样式见下方 VSCode 风格定义 */

/* 表格 */
.markdown-body :deep(table) {
  border-collapse: collapse;
  margin: 1em 0;
  width: 100%;
  font-size: 0.92em;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #d0d7de;
  padding: 6px 13px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: #f6f8fa;
  font-weight: 600;
}
.markdown-body :deep(tr:nth-child(2n) td) { background: #fafbfc; }

/* 分隔线 */
.markdown-body :deep(hr) {
  margin: 1.2em 0;
  border: 0;
  border-top: 1px solid #e0e0e0;
}

/* 图片 */
.markdown-body :deep(img) { max-width: 100%; border-radius: 6px; }

/* 代码块容器 - 浅色 IDE 风格（白色圆角面板 + 轻微外阴影） */
.markdown-body :deep(.md-code-block) {
  margin: 1em 0;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e6e8eb;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
}

/* 代码块顶部 - 仿 IDE 文件标签栏 */
.markdown-body :deep(.md-code-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 14px;
  background: #fafbfc;
  border-bottom: 1px solid #e6e8eb;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.markdown-body :deep(.md-code-lang) {
  font-size: 12px;
  color: #57606a;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* 语言标签前的小圆点（仿 IDE 文件标签） */
.markdown-body :deep(.md-code-lang)::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1a7f37;
}

/* 复制按钮 - 浅色风格 */
.markdown-body :deep(.md-code-copy) {
  border: 1px solid #d0d7de;
  background: #ffffff;
  color: #57606a;
  cursor: pointer;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.15s;
  font-family: inherit;
}

.markdown-body :deep(.md-code-copy:hover) {
  background: #f3f4f6;
  border-color: #57606a;
  color: #1f2328;
}

.markdown-body :deep(.md-code-copy.copied) {
  background: #dafbe1;
  border-color: #1a7f37;
  color: #1a7f37;
}

/* 代码块主体 - 白底 + 左对齐 + 五色高亮 */
.markdown-body :deep(.md-code-pre) {
  margin: 0;
  padding: 16px 20px;
  background: #ffffff;
  overflow-x: auto;
  font-family: 'SFMono-Regular', 'Consolas', 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-size: 13.5px;
  line-height: 1.65;
  tab-size: 2;
  text-align: left;
}

.markdown-body :deep(.md-code-pre code) {
  background: transparent;
  padding: 0;
  color: #1f2328;
  white-space: pre;
}

/* 行内代码 - 浅色风格 */
.markdown-body :deep(:not(pre) > code) {
  padding: 2px 6px;
  background: #f6f8fa;
  border: 1px solid #d0d7de;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.88em;
  color: #0550ae;
}

/* 任务列表 */
.markdown-body :deep(input[type="checkbox"]) {
  margin-right: 0.4em;
  vertical-align: middle;
}
</style>
