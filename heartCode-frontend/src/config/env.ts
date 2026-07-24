/**
 * 环境变量配置
 * 通过 Vite 的 import.meta.env 读取 .env.* 文件中的变量
 */

// 应用部署域名（部署后的作品访问地址前缀）
export const DEPLOY_DOMAIN = import.meta.env.VITE_DEPLOY_DOMAIN || 'http://localhost'

// API 基础地址
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'

// 静态资源地址（基于 API 地址派生）
export const STATIC_BASE_URL = `${API_BASE_URL}/static`

/**
 * 获取部署应用的完整 URL
 * @param deployKey 应用部署标识
 */
export const getDeployUrl = (deployKey: string) => {
  return `${DEPLOY_DOMAIN}/${deployKey}`
}

/**
 * 代码生成类型枚举
 */
export enum CodeGenTypeEnum {
  HTML = 'html',
  MULTI_FILE = 'multi_file',
  VUE_PROJECT = 'vue_project',
}

/**
 * 代码生成类型配置
 */
export const CODE_GEN_TYPE_CONFIG = {
  [CodeGenTypeEnum.HTML]: {
    label: '原生 HTML 模式',
    value: CodeGenTypeEnum.HTML,
  },
  [CodeGenTypeEnum.MULTI_FILE]: {
    label: '原生多文件模式',
    value: CodeGenTypeEnum.MULTI_FILE,
  },
  [CodeGenTypeEnum.VUE_PROJECT]: {
    label: 'Vue 项目模式',
    value: CodeGenTypeEnum.VUE_PROJECT,
  },
}

/**
 * 获取静态资源预览 URL
 * @param codeGenType 代码生成类型（html / multi_file / vue_project）
 * @param appId 应用 ID
 */
export const getStaticPreviewUrl = (codeGenType: string, appId: string) => {
  const baseUrl = `${STATIC_BASE_URL}/${codeGenType}_${appId}/`
  // 如果是 Vue 项目，浏览地址需要添加 dist 后缀
  if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
    return `${baseUrl}dist/index.html`
  }
  return baseUrl
}
