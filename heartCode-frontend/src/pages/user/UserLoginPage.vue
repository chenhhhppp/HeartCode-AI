<script setup lang="ts">
import { reactive } from 'vue'
import { userLogin } from '@/api/userController.ts'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { message } from 'ant-design-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const handleSubmit = async (values: any) => {
  const res = await userLogin(values)
  // 登录成功，把登录态保存到全局状态中
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    // 跳转到首页
    router.push('/home')
  } else {
    message.error('登录失败，' + res.data.message)
  }
}
</script>

<template>
  <div class="auth-wrapper">
    <div class="auth-card">
      <h2 class="auth-title">心码 AI 应用生成 - 用户登录</h2>
      <p class="auth-desc">所想即所得，应用自动生成</p>

      <form class="auth-form" @submit.prevent="handleSubmit(formState)">
        <input
          v-model="formState.userAccount"
          class="auth-input"
          type="text"
          placeholder="请输入账号"
          required
        />
        <input
          v-model="formState.userPassword"
          class="auth-input"
          type="password"
          placeholder="请输入密码"
          minlength="8"
          required
        />

        <div class="auth-options">
          <span class="auth-tip-text">没有账号?</span>
          <RouterLink class="auth-link" to="/user/register">立即注册</RouterLink>
        </div>

        <button class="auth-submit-btn" type="submit">登录</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: #1f2937;
  border-radius: 12px;
  box-shadow:
    0 10px 25px rgba(0, 0, 0, 0.3),
    0 20px 40px rgba(0, 0, 0, 0.15);
  padding: 32px;
}

.auth-title {
  font-size: 24px;
  font-weight: 700;
  color: #e5e7eb;
  margin: 0 0 8px;
}

.auth-desc {
  color: #9ca3af;
  font-size: 14px;
  margin: 0 0 24px;
}

.auth-form {
  display: flex;
  flex-direction: column;
}

.auth-input {
  background: #374151;
  color: #e5e7eb;
  border: 0;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 16px;
  font-size: 14px;
  outline: none;
  transition: all 0.15s ease;
  width: 100%;
  box-sizing: border-box;
}

.auth-input::placeholder {
  color: #9ca3af;
}

.auth-input:focus {
  background: #4b5563;
  box-shadow: 0 0 0 2px #3b82f6;
}

.auth-options {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.auth-tip-text {
  color: #e5e7eb;
  font-size: 14px;
}

.auth-link {
  color: #60a5fa;
  font-size: 14px;
  text-decoration: none;
}

.auth-link:hover {
  text-decoration: underline;
}

.auth-submit-btn {
  background: linear-gradient(to right, #6366f1, #3b82f6);
  color: #fff;
  font-weight: 700;
  padding: 12px 16px;
  border: 0;
  border-radius: 8px;
  margin-top: 8px;
  cursor: pointer;
  font-size: 15px;
  transition: all 0.15s ease;
}

.auth-submit-btn:hover {
  background: linear-gradient(to right, #4f46e5, #2563eb);
}

.auth-submit-btn:active {
  transform: scale(0.98);
}
</style>
