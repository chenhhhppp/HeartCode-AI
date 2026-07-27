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
    <div class="auth-box">
      <!-- 游客登录提示框（样式参考 Uiverse.io by andrew-demchenk0） -->
      <div class="guest-tip">
        <div class="guest-tip__icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" viewBox="0 0 24 24" height="24" fill="none"><path fill-rule="evenodd" fill="#393a37" d="m12 1c-6.075 0-11 4.925-11 11s4.925 11 11 11 11-4.925 11-11-4.925-11-11-11zm4.768 9.14c.0878-.1004.1546-.21726.1966-.34383.0419-.12657.0581-.26026.0477-.39319-.0105-.13293-.0475-.26242-.1087-.38085-.0613-.11844-.1456-.22342-.2481-.30879-.1024-.08536-.2209-.14938-.3484-.18828s-.2616-.0519-.3942-.03823c-.1327.01366-.2612.05372-.3782.1178-.1169.06409-.2198.15091-.3027.25537l-4.3 5.159-2.225-2.226c-.1886-.1822-.4412-.283-.7034-.2807s-.51301.1075-.69842.2929-.29058.4362-.29285.6984c-.00228.2622.09851.5148.28067.7034l3 3c.0983.0982.2159.1748.3454.2251.1295.0502.2681.0729.4069.0665.1387-.0063.2747-.0414.3991-.1032.1244-.0617.2347-.1487.3236-.2554z" clip-rule="evenodd"></path></svg>
        </div>
        <div class="guest-tip__title">游客登录：账号：abcabc  密码：12345678</div>
      </div>

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

/* 提示框 + 登录卡片垂直容器 */
.auth-box {
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
}

/* 游客登录提示框（样式参考 Uiverse.io by andrew-demchenk0） */
.guest-tip {
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  padding: 12px;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: start;
  background: #84D65A;
  border-radius: 8px;
  box-shadow: 0px 0px 5px -3px #111;
}

.guest-tip__icon {
  width: 20px;
  height: 20px;
  transform: translateY(-2px);
  margin-right: 8px;
  flex-shrink: 0;
}

.guest-tip__icon path {
  fill: #393A37;
}

.guest-tip__title {
  font-weight: 500;
  font-size: 14px;
  color: #393A37;
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
