/**
 * 可视化编辑模块
 * 封装主页面与 iframe 预览页面之间的通信逻辑
 *
 * 通信机制：
 * - 主页面 → iframe：postMessage 控制编辑模式开关
 * - iframe → 主页面：postMessage 回传用户选中的元素信息
 *
 * @author CHP
 */

/** 消息类型常量 */
export const EDITOR_MESSAGE_TYPE = {
  /** 主页面 → iframe：启用编辑模式 */
  ENABLE_EDIT_MODE: 'ENABLE_EDIT_MODE',
  /** 主页面 → iframe：禁用编辑模式 */
  DISABLE_EDIT_MODE: 'DISABLE_EDIT_MODE',
  /** iframe → 主页面：选中了某个元素 */
  ELEMENT_SELECTED: 'ELEMENT_SELECTED',
} as const

/** 选中的元素信息 */
export interface SelectedElement {
  /** 标签名，如 div、button */
  tag: string
  /** id 属性 */
  id: string
  /** class 属性 */
  className: string
  /** 元素可见文本（截断前 100 字符） */
  text: string
  /** 元素的 outerHTML（截断前 500 字符） */
  outerHTML: string
  /** CSS 选择器路径 */
  cssSelector: string
}

/**
 * 生成注入到 iframe 预览页面中的编辑器脚本
 *
 * 该脚本负责：
 * 1. 监听主页面发来的启用/禁用编辑模式消息
 * 2. 编辑模式下，鼠标悬浮元素时显示蓝色高亮边框
 * 3. 点击元素时，固定橙色选中边框，并向主页面回传元素信息
 *
 * @returns 注入用的 <script> 标签字符串
 */
export function generateEditorScript(): string {
  // 注意：返回的是会被注入到 HTML 中执行的纯 JS，用普通字符串拼接
  // </script> 需转义为 <\/script>，避免被 HTML 解析器提前闭合
  return `<script>
    (function () {
      var editMode = false;
      var hoverOutline = null;
      var selectedOutline = null;

      /** 创建一个固定定位的边框浮层 */
      function createOutline(color, width) {
        var el = document.createElement('div');
        el.style.cssText =
          'position:fixed;pointer-events:none;z-index:999999;' +
          'border:' + width + 'px solid ' + color + ';' +
          'border-radius:2px;transition:all 0.05s ease;display:none;';
        return el;
      }

      /** 懒加载创建边框浮层（body 存在后才可 append） */
      function ensureOverlays() {
        if (!hoverOutline) {
          hoverOutline = createOutline('#1890ff', 2);
          document.body.appendChild(hoverOutline);
        }
        if (!selectedOutline) {
          selectedOutline = createOutline('#fa541c', 3);
          document.body.appendChild(selectedOutline);
        }
      }

      /** 将浮层定位到目标元素位置 */
      function positionOutline(outline, el) {
        var rect = el.getBoundingClientRect();
        outline.style.left = rect.left + 'px';
        outline.style.top = rect.top + 'px';
        outline.style.width = rect.width + 'px';
        outline.style.height = rect.height + 'px';
        outline.style.display = 'block';
      }

      /** 生成简化版 CSS 选择器路径 */
      function getCssSelector(el) {
        var path = [];
        var cur = el;
        while (cur && cur.nodeType === 1) {
          var selector = cur.tagName.toLowerCase();
          if (cur.id) {
            selector += '#' + cur.id;
            path.unshift(selector);
            break;
          }
          if (cur.className && typeof cur.className === 'string') {
            var classes = cur.className.trim().split(/\\s+/).slice(0, 2);
            if (classes.length) selector += '.' + classes.join('.');
          }
          path.unshift(selector);
          cur = cur.parentNode;
        }
        return path.join(' > ');
      }

      /** 收集元素信息 */
      function getElementInfo(el) {
        return {
          tag: el.tagName.toLowerCase(),
          id: el.id || '',
          className: typeof el.className === 'string' ? el.className : '',
          text: (el.innerText || '').substring(0, 100).trim(),
          outerHTML: el.outerHTML.substring(0, 500),
          cssSelector: getCssSelector(el),
        };
      }

      /** 鼠标移动：高亮当前元素 */
      function onMouseMove(e) {
        if (!editMode) return;
        var el = e.target;
        if (!el || el === hoverOutline || el === selectedOutline) return;
        ensureOverlays();
        positionOutline(hoverOutline, el);
      }

      /** 点击：选中元素并回传信息 */
      function onClick(e) {
        if (!editMode) return;
        e.preventDefault();
        e.stopPropagation();
        var el = e.target;
        if (!el || el === hoverOutline || el === selectedOutline) return;
        ensureOverlays();
        positionOutline(selectedOutline, el);
        // 向父窗口发送选中元素信息
        window.parent.postMessage(
          { type: 'ELEMENT_SELECTED', element: getElementInfo(el) },
          '*'
        );
      }

      function enableEditMode() {
        editMode = true;
        ensureOverlays();
        document.body.style.cursor = 'pointer';
      }

      function disableEditMode() {
        editMode = false;
        document.body.style.cursor = '';
        if (hoverOutline) hoverOutline.style.display = 'none';
        if (selectedOutline) selectedOutline.style.display = 'none';
      }

      // 监听主页面消息
      window.addEventListener('message', function (event) {
        var data = event.data;
        if (!data || !data.type) return;
        if (data.type === 'ENABLE_EDIT_MODE') {
          enableEditMode();
        } else if (data.type === 'DISABLE_EDIT_MODE') {
          disableEditMode();
        }
      });

      // 注册事件（捕获阶段，优先于目标元素自身处理）
      document.addEventListener('mousemove', onMouseMove, true);
      document.addEventListener('click', onClick, true);
    })();
  <\/script>`
}

/**
 * 向 iframe 发送启用编辑模式消息
 * @param iframe 预览 iframe 元素
 */
export function enableEditMode(iframe: HTMLIFrameElement | null) {
  if (!iframe || !iframe.contentWindow) return
  iframe.contentWindow.postMessage(
    { type: EDITOR_MESSAGE_TYPE.ENABLE_EDIT_MODE },
    '*',
  )
}

/**
 * 向 iframe 发送禁用编辑模式消息
 * @param iframe 预览 iframe 元素
 */
export function disableEditMode(iframe: HTMLIFrameElement | null) {
  if (!iframe || !iframe.contentWindow) return
  iframe.contentWindow.postMessage(
    { type: EDITOR_MESSAGE_TYPE.DISABLE_EDIT_MODE },
    '*',
  )
}

/**
 * 判断 postMessage 事件是否为选中元素消息
 * @param event message 事件对象
 */
export function isElementSelectedMessage(
  event: MessageEvent,
): event is MessageEvent & { data: { type: typeof EDITOR_MESSAGE_TYPE.ELEMENT_SELECTED; element: SelectedElement } } {
  return (
    event.data != null &&
    typeof event.data === 'object' &&
    event.data.type === EDITOR_MESSAGE_TYPE.ELEMENT_SELECTED
  )
}

/**
 * 将选中元素格式化为追加到提示词的描述文本
 * 生成结构化的上下文信息，让 AI 能准确识别和定位目标元素
 *
 * @param el 选中的元素信息
 * @returns 拼接到用户输入后的上下文描述
 */
export function formatElementForPrompt(el: SelectedElement): string {
  const lines: string[] = []
  lines.push('【目标元素信息】用户希望针对以下页面元素进行修改：')

  // 1. 元素定位信息（tag + id + class）
  let tagDesc = '<' + el.tag
  if (el.id) tagDesc += ' id="' + el.id + '"'
  if (el.className) tagDesc += ' class="' + el.className + '"'
  tagDesc += '>'
  lines.push('元素：' + tagDesc)

  // 2. CSS 选择器路径（精确定位）
  if (el.cssSelector) {
    lines.push('CSS 选择器：' + el.cssSelector)
  }

  // 3. 元素文本内容
  if (el.text) {
    lines.push('文本内容：' + el.text)
  }

  // 4. 元素的 HTML 结构（让 AI 看到完整结构，便于精确修改）
  if (el.outerHTML) {
    lines.push('元素 HTML：')
    lines.push(el.outerHTML)
  }

  lines.push('请根据以上信息，仅修改该目标元素及相关样式，保持页面其他部分不变。')
  return lines.join('\n')
}
