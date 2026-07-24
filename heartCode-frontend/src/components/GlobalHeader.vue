<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { Layout, message } from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'
import { LogoutOutlined, HomeOutlined, AppstoreOutlined, TeamOutlined, MessageOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'

const ALayoutHeader = Layout.Header

const router = useRouter()

// 菜单配置项（保留原有功能，仅渲染方式改变）
const originItems: MenuProps['items'] = [
  {
    key: '/home',
    icon: () => h(HomeOutlined),
    label: '首页',
    title: '首页',
  },
  {
    key: '/admin/userManage',
    icon: () => h(TeamOutlined),
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    icon: () => h(AppstoreOutlined),
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: '/admin/chatHistoryManage',
    icon: () => h(MessageOutlined),
    label: '对话管理',
    title: '对话管理',
  },
]

// 引入 Store
const loginUserStore = useLoginUserStore()

// 过滤菜单项（权限校验保留）
const filterMenus = (menus = originItems) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      return loginUserStore.isAdmin
    }
    return true
  })
}

// 展示在菜单的数组（用普通数组方便自定义渲染）
const menuItems = computed(() => filterMenus() || [])

const selectedKeys = defineModel<string[]>('selectedKeys', { default: ['/home'] })

// 处理菜单点击
const handleMenuClick = (key: string) => {
  router.push(key)
}

// 跳转到个人信息页面
const goToProfile = () => {
  router.push('/user/profile')
}

// 用户注销
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败,' + res.data.message)
  }
}
</script>

<template>
  <ALayoutHeader class="header">
    <div class="header-content">
      <!-- 左侧：Logo + 标题 + 菜单 -->
      <div class="header-left">
        <RouterLink to="/home" class="logo-link">
          <img src="@/assets/logo.png" alt="Logo" class="logo" />
          <span class="site-title">HeartCode</span>
        </RouterLink>
        <!-- Uiverse.io 风格菜单按钮 -->
        <nav class="header-nav">
          <button
            v-for="item in menuItems"
            :key="item.key as string"
            class="nav-btn"
            :class="{ active: selectedKeys.includes(item.key as string) }"
            @click="handleMenuClick(item.key as string)"
          >
            <span class="nav-btn-bg"></span>
            <component :is="item.icon" class="nav-btn-icon" />
            {{ item.label }}
          </button>
        </nav>
      </div>

      <!-- 右侧：用户头像和昵称 / 登录按钮 -->
      <div class="header-right">
        <div v-if="loginUserStore.loginUser.id">
          <a-dropdown>
            <a-space class="user-info">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              {{ loginUserStore.loginUser.userName ?? '无名' }}
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="goToProfile">
                  <UserOutlined />
                  个人信息
                </a-menu-item>
                <a-menu-item @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else>
          <button class="nav-btn login-btn" @click="router.push('/user/login')">
            <span class="nav-btn-bg"></span>
            登录
          </button>
        </div>
      </div>
    </div>
  </ALayoutHeader>
</template>

<style scoped>
/* From Uiverse.io by adamgiebl - 导航栏按钮风格 */
.nav-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0.5em 1.4em;
  font-size: 15px;
  background: transparent;
  cursor: pointer;
  user-select: none;
  overflow: hidden;
  color: royalblue;
  z-index: 1;
  font-family: inherit;
  font-weight: 500;
  border: none;
  border-radius: 0;
  transition: color 0.3s;
}

.nav-btn-bg {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  z-index: -1;
  border: 2px solid transparent;
  transition: border-color 0.3s;
}

.nav-btn::before {
  content: '';
  display: block;
  position: absolute;
  width: 8%;
  height: 500%;
  background: rgba(255, 255, 255, 0.4);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-60deg);
  transition: all 0.3s;
  z-index: -1;
}

.nav-btn:hover::before,
.nav-btn.active::before {
  transform: translate(-50%, -50%) rotate(-90deg);
  width: 100%;
  background: royalblue;
}

.nav-btn:hover,
.nav-btn.active {
  color: white;
}

.nav-btn:hover .nav-btn-bg,
.nav-btn.active .nav-btn-bg {
  border-color: royalblue;
}

.nav-btn:active::before {
  background: #2751cd;
}

.nav-btn-icon {
  font-size: 16px;
}

/* 登录按钮稍微突出 */
.login-btn {
  color: #ff4757;
}

.login-btn::before {
  background: rgba(255, 255, 255, 0.4);
}

.login-btn:hover::before {
  background: #ff4757;
}

.login-btn:hover .nav-btn-bg {
  border-color: #ff4757;
}

.login-btn:hover {
  color: white;
}

/* Header 容器 */
.header {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 0;
  z-index: 10;
  position: relative;
}

.header-content {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
  flex: 1;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  white-space: nowrap;
}

.logo {
  width: 32px;
  height: 32px;
}

.site-title {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #3a3a72, #009ffd);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.4);
}

/* 响应式 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .header-left {
    gap: 16px;
  }

  .site-title {
    font-size: 16px;
  }

  .header-nav {
    display: none;
  }
}
</style>
