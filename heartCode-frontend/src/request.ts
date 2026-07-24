import axios from 'axios'
import { message } from 'ant-design-vue'
import { API_BASE_URL } from '@/config/env'

// 创建axios实例
const myAxios = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    return config
  },
  function (error) {
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    // 未登录
    if (data.code === 40100) {
      // 公开页面白名单，不跳转到登录页
      const currentPath = window.location.pathname
      const PUBLIC_PATHS = ['/home', '/user/login', '/user/register', '/about']

      // 获取用户信息的请求不跳转
      const isGetLoginUser = response.request.responseURL?.includes('/user/get/login')

      // 如果在公开页面或正在获取用户信息，不跳转
      if (PUBLIC_PATHS.includes(currentPath) || isGetLoginUser) {
        return response
      }

      // 其他情况跳转到登录页
      if (!window.location.href.includes('/user/login')) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${encodeURIComponent(window.location.href)}`
      }
    }
    return response
  },
  function (error) {
    return Promise.reject(error)
  },
)

export default myAxios
