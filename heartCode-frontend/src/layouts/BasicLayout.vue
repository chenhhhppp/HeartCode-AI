<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Layout } from 'ant-design-vue'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const ALayout = Layout
const ALayoutContent = Layout.Content
const ALayoutFooter = Layout.Footer

const router = useRouter()
const route = useRoute()

// 菜单配置
const menuItems = [
  {
    key: 'home',
    label: '首页',
    path: '/'
  },
  {
    key: 'about',
    label: '关于',
    path: '/about'
  }
]

// 当前选中的菜单项
const selectedKeys = computed(() => {
  return [route.name as string || 'home']
})

// 处理菜单点击
const handleMenuClick = ({ key }: { key: string }) => {
  const menuItem = menuItems.find(item => item.key === key)
  if (menuItem) {
    router.push(menuItem.path)
  }
}
</script>

<template>
  <ALayout class="basic-layout">
    <GlobalHeader
      :menu-items="menuItems"
      :selected-keys="selectedKeys"
      @menu-click="handleMenuClick"
    />
    <ALayoutContent class="layout-content">
      <RouterView />
    </ALayoutContent>
    <ALayoutFooter class="layout-footer">
      <GlobalFooter />
    </ALayoutFooter>
  </ALayout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-content {
  flex: 1;
  padding: 24px;
  background-color: #f5f5f5;
}

.layout-footer {
  padding: 0;
  height: auto;
}

@media (max-width: 768px) {
  .layout-content {
    padding: 16px;
  }
}
</style>
