<script setup lang="ts">
import { useRouter } from 'vue-router'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const router = useRouter()

// 跳转到登录页
const goLogin = () => {
  emit('close')
  router.push('/user/login')
}

// 跳转到注册页
const goRegister = () => {
  emit('close')
  router.push('/user/register')
}
</script>

<template>
  <Transition name="modal-fade">
    <div v-if="props.visible" class="modal-overlay" @click.self="emit('close')">
      <!-- From Uiverse.io by ZstarPanda0210 -->
      <div class="card">
        <button type="button" class="dismiss" @click="emit('close')">×</button>
        <div class="header">
          <div class="image">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <g id="SVGRepo_iconCarrier">
                <path
                  stroke-linejoin="round"
                  stroke-linecap="round"
                  stroke-width="1.5"
                  stroke="#000000"
                  d="M20 7L9.00004 18L3.99994 13"
                ></path>
              </g>
            </svg>
          </div>
          <div class="content">
            <span class="title">未登录提示</span>
            <p class="message">您当前尚未登录，登录后才能生成应用。请先登录或注册账号后继续操作。</p>
          </div>
          <div class="actions">
            <button type="button" class="history" @click="goLogin">去登录</button>
            <button type="button" class="track" @click="goRegister">去注册</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
/* 遮罩层：全屏覆盖，置顶 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
}

/* From Uiverse.io by ZstarPanda0210 —— 卡片 */
.card {
  overflow: hidden;
  position: relative;
  text-align: left;
  border-radius: 0.5rem;
  max-width: 290px;
  width: 100%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  background-color: #fff;
}

.dismiss {
  position: absolute;
  right: 10px;
  top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background-color: #fff;
  color: black;
  border: 2px solid #d1d5db;
  font-size: 1rem;
  font-weight: 300;
  width: 30px;
  height: 30px;
  border-radius: 7px;
  transition: 0.3s ease;
  cursor: pointer;
  line-height: 1;
}

.dismiss:hover {
  background-color: #ee0d0d;
  border: 2px solid #ee0d0d;
  color: #fff;
}

.header {
  padding: 1.25rem 1rem 1rem 1rem;
}

.image {
  display: flex;
  margin-left: auto;
  margin-right: auto;
  background-color: #e2feee;
  flex-shrink: 0;
  justify-content: center;
  align-items: center;
  width: 3rem;
  height: 3rem;
  border-radius: 9999px;
  animation: animate 0.6s linear alternate-reverse infinite;
  transition: 0.6s ease;
}

.image svg {
  color: #0afa2a;
  width: 2rem;
  height: 2rem;
}

.content {
  margin-top: 0.75rem;
  text-align: center;
}

.title {
  color: #066e29;
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.5rem;
}

.message {
  margin-top: 0.5rem;
  color: #595b5f;
  font-size: 0.875rem;
  line-height: 1.25rem;
}

.actions {
  margin: 0.75rem 1rem;
}

.history {
  display: inline-flex;
  padding: 0.5rem 1rem;
  background-color: #1aa06d;
  color: #ffffff;
  font-size: 1rem;
  line-height: 1.5rem;
  font-weight: 500;
  justify-content: center;
  width: 100%;
  border-radius: 0.375rem;
  border: none;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.history:hover {
  background-color: #168058;
}

.track {
  display: inline-flex;
  margin-top: 0.75rem;
  padding: 0.5rem 1rem;
  color: #242525;
  font-size: 1rem;
  line-height: 1.5rem;
  font-weight: 500;
  justify-content: center;
  width: 100%;
  border-radius: 0.375rem;
  border: 1px solid #d1d5db;
  background-color: #fff;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.track:hover {
  background-color: #f9fafb;
}

@keyframes animate {
  from {
    transform: scale(1);
  }
  to {
    transform: scale(1.09);
  }
}

/* 弹出/关闭过渡 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .card,
.modal-fade-leave-active .card {
  transition: transform 0.25s ease;
}

.modal-fade-enter-from .card {
  transform: scale(0.9) translateY(10px);
}

.modal-fade-leave-to .card {
  transform: scale(0.95);
}
</style>
