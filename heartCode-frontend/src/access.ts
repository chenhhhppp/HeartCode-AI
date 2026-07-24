import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import router from '@/router'

// 不需要登录的页面白名单
const WHITE_LIST = ['/home', '/user/login', '/user/register', '/about']

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser

  // 首页和公开页面不需要等待登录用户信息
  if (WHITE_LIST.includes(to.path)) {
    next()
    return
  }

  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }

  const toUrl = to.fullPath
  // 管理员页面需要检查权限
  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('没有权限')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }

  // 其他需要登录的页面（如创建应用、对话等）
  if (!loginUser || !loginUser.id) {
    message.warning('请先登录')
    next(`/user/login?redirect=${to.fullPath}`)
    return
  }

  next()
})
