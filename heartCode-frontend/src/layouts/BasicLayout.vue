<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Layout } from 'ant-design-vue'
import GlobalHeader from '../components/GlobalHeader.vue'
import GlobalFooter from '../components/GlobalFooter.vue'

const ALayout = Layout
const ALayoutContent = Layout.Content
const ALayoutFooter = Layout.Footer

const router = useRouter()

// 当前路由路径，用于高亮菜单
const selectedKeys = computed(() => {
  const path = router.currentRoute.value.path
  return [path === '/' ? '/home' : path]
})

// 返回顶部按钮显示控制（滚动超过 300px 时显示）
const showBackToTop = ref(false)

const handleScroll = () => {
  showBackToTop.value = window.scrollY > 300
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <ALayout class="basic-layout">
    <GlobalHeader :selected-keys="selectedKeys" />
    <ALayoutContent class="content">
      <RouterView />
    </ALayoutContent>
    <ALayoutFooter class="footer">
      <GlobalFooter />
    </ALayoutFooter>
    <!-- 返回顶部按钮（样式参考 Uiverse.io by vinodjangid07） -->
    <Transition name="backtop-fade">
      <button v-if="showBackToTop" class="back-to-top" aria-label="返回顶部" @click="scrollToTop">
        <svg height="1.2em" class="back-to-top-arrow" viewBox="0 0 512 512">
          <path
            d="M233.4 105.4c12.5-12.5 32.8-12.5 45.3 0l192 192c12.5 12.5 12.5 32.8 0 45.3s-32.8 12.5-45.3 0L256 173.3 86.6 342.6c-12.5 12.5-32.8 12.5-45.3 0s-12.5-32.8 0-45.3l192-192z"
          ></path>
        </svg>
        <p class="back-to-top-text">Back to Top</p>
      </button>
    </Transition>
  </ALayout>
</template>

<style scoped>
/* 全局背景容器（Uiverse.io by SachinKumar666，蓝白斜纹+旋转光晕风格） */
.basic-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  background-color: #2a2a72;
  overflow: hidden;
  z-index: 1;
}

/* 斜向网格 + 蓝色线性渐变 */
.basic-layout::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
      45deg,
      transparent 0,
      transparent 35px,
      rgba(255, 255, 255, 0.15) 35px,
      rgba(255, 255, 255, 0.15) 70px
    ),
    repeating-linear-gradient(
      -45deg,
      transparent 0,
      transparent 35px,
      rgba(255, 255, 255, 0.15) 35px,
      rgba(255, 255, 255, 0.15) 70px
    ),
    linear-gradient(90deg, #009ffd, #2a2a72);
  z-index: -1;
}

/* 旋转的放射状光晕 + 暗角 */
.basic-layout::after {
  content: '';
  position: absolute;
  inset: -50%;
  background: radial-gradient(
      circle at 50% 50%,
      transparent 20%,
      rgba(0, 0, 0, 0.2) 70%,
      rgba(0, 0, 0, 0.5) 100%
    ),
    repeating-conic-gradient(
      from 0deg,
      rgba(255, 255, 255, 0.15) 0deg 30deg,
      transparent 30deg 60deg
    );
  mix-blend-mode: overlay;
  z-index: -1;
  animation: bg-rotate 20s linear infinite;
  pointer-events: none;
}

@keyframes bg-rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .basic-layout::after {
    animation: none;
  }
}

@media (prefers-contrast: more) {
  .basic-layout {
    background-color: #1a1a4a;
  }
  .basic-layout::before {
    opacity: 0.9;
  }
}

.content {
  flex: 1;
  padding: 24px;
  position: relative;
  z-index: 1;
}

.footer {
  padding: 0;
  background: #fff;
  position: relative;
  z-index: 1;
}

/* 返回顶部按钮（样式参考 Uiverse.io by vinodjangid07） */
.back-to-top {
  width: 45px;
  height: 45px;
  background: linear-gradient(#44ea76, #39fad7);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  position: fixed;
  right: 32px;
  bottom: 32px;
  border: none;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.back-to-top:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.35);
}

.back-to-top-arrow path {
  fill: white;
}

.back-to-top-text {
  font-size: 0.7em;
  width: 100px;
  position: absolute;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  bottom: -18px;
  opacity: 0;
  transition-duration: 0.7s;
  margin: 0;
  white-space: nowrap;
}

.back-to-top:hover .back-to-top-text {
  opacity: 1;
  transition-duration: 0.7s;
}

.back-to-top:hover .back-to-top-arrow {
  animation: slide-in-bottom 0.7s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}

@keyframes slide-in-bottom {
  0% {
    transform: translateY(10px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 显示/隐藏过渡 */
.backtop-fade-enter-active,
.backtop-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.backtop-fade-enter-from,
.backtop-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
