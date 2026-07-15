<script setup lang="ts">
import { computed } from 'vue'
import { Menu, Button, Avatar, Space, Dropdown } from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'

interface MenuItem {
  key: string
  label: string
  path: string
}

interface Props {
  menuItems: MenuItem[]
  selectedKeys: string[]
}

interface Emits {
  (e: 'menuClick', info: { key: string }): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 网站标题
const siteTitle = 'HeartCode 心码 AI'

// 当前用户状态（模拟登录前状态）
const isLoggedIn = computed(() => false)

// 用户信息（登录后显示）
const userInfo = computed(() => ({
  nickname: '用户昵称',
  avatar: ''
}))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = info => {
  emit('menuClick', { key: info.key as string })
}

// 处理登录
const handleLogin = () => {
  console.log('跳转到登录页面')
  // TODO: 实现登录逻辑
}

// 用户下拉菜单
const userMenuItems = [
  {
    key: 'profile',
    label: '个人中心'
  },
  {
    key: 'settings',
    label: '设置'
  },
  {
    key: 'logout',
    label: '退出登录'
  }
]

// 处理用户菜单点击
const handleUserMenuClick: MenuProps['onClick'] = ({ key }) => {
  console.log('用户菜单点击:', key)
  if (key === 'logout') {
    // TODO: 实现退出登录逻辑
  }
}
</script>

<template>
  <header class="global-header">
    <div class="header-container">
      <!-- 左侧：Logo 和网站标题 -->
      <div class="header-left">
        <img class="logo" src="@/assets/logo.png" alt="HeartCode Logo" />
        <h1 class="site-title">{{ siteTitle }}</h1>
      </div>

      <!-- 中间：导航菜单 -->
      <div class="header-center">
        <Menu
          :selected-keys="selectedKeys"
          mode="horizontal"
          :style="{ flex: 1, border: 'none' }"
          @click="handleMenuClick"
        >
          <Menu.Item v-for="item in menuItems" :key="item.key">
            {{ item.label }}
          </Menu.Item>
        </Menu>
      </div>

      <!-- 右侧：用户信息或登录按钮 -->
      <div class="header-right">
        <Space v-if="isLoggedIn" size="middle">
          <Dropdown>
            <Space class="user-info">
              <Avatar :size="32" :src="userInfo.avatar || undefined">
                {{ userInfo.nickname?.charAt(0) }}
              </Avatar>
              <span class="user-nickname">{{ userInfo.nickname }}</span>
            </Space>
            <template #overlay>
              <Menu @click="handleUserMenuClick">
                <Menu.Item v-for="menu in userMenuItems" :key="menu.key">
                  {{ menu.label }}
                </Menu.Item>
              </Menu>
            </template>
          </Dropdown>
        </Space>
        <Button v-else type="primary" @click="handleLogin">登录</Button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.global-header {
  background-color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-container {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
}

.logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.site-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-center {
  flex: 1;
  max-width: 600px;
  margin: 0 24px;
}

.header-right {
  min-width: 120px;
  display: flex;
  justify-content: flex-end;
}

.user-info {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.user-nickname {
  font-size: 14px;
  color: #333;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-container {
    padding: 0 16px;
    height: 56px;
  }

  .header-left {
    min-width: auto;
  }

  .site-title {
    display: none;
  }

  .header-center {
    margin: 0 12px;
  }

  .header-right {
    min-width: auto;
  }

  .user-nickname {
    display: none;
  }
}
</style>
