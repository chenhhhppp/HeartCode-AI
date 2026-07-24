/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 应用部署域名 */
  readonly VITE_DEPLOY_DOMAIN?: string
  /** API 基础地址 */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
