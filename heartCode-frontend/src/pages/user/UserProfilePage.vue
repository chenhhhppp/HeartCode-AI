<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { updateMyProfile, uploadAvatar } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import defaultLogo from '@/assets/logo.png'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserProfileUpdateRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

// 提交中状态
const submitting = ref(false)
// 头像上传中状态
const avatarUploading = ref(false)
// 隐藏的文件输入框引用
const fileInput = ref<HTMLInputElement | null>(null)

// 实际展示的头像：有头像用头像，没有用默认 logo
const avatarSrc = computed(() => formState.userAvatar || defaultLogo)

onMounted(() => {
  // 用当前登录用户信息回填表单
  const u = loginUserStore.loginUser
  formState.userName = u.userName ?? ''
  formState.userAvatar = u.userAvatar ?? ''
  formState.userProfile = u.userProfile ?? ''
})

// 触发文件选择
const handleAvatarClick = () => {
  fileInput.value?.click()
}

// 选择文件后上传头像
const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  // 前端简单校验图片类型
  if (!file.type.startsWith('image/')) {
    message.error('仅支持上传图片')
    target.value = ''
    return
  }
  avatarUploading.value = true
  try {
    const res = await uploadAvatar(file)
    if (res.data.code === 0 && res.data.data) {
      // 上传成功，更新表单中的头像链接（保存时一并提交）
      formState.userAvatar = res.data.data
      message.success('头像上传成功')
    } else {
      message.error('头像上传失败，' + res.data.message)
    }
  } catch (err) {
    message.error('头像上传失败，请稍后重试')
  } finally {
    avatarUploading.value = false
    // 清空 input，确保选择同一文件可再次触发 change
    target.value = ''
  }
}

const handleSubmit = async () => {
  if (!formState.userName?.trim()) {
    message.error('用户昵称不能为空')
    return
  }
  submitting.value = true
  try {
    const res = await updateMyProfile(formState)
    if (res.data.code === 0 && res.data.data) {
      // 同步更新全局登录态，使顶部头像/昵称立即刷新
      loginUserStore.setLoginUser(res.data.data)
      message.success('个人信息更新成功')
      router.push('/home')
    } else {
      message.error('更新失败，' + res.data.message)
    }
  } catch (e) {
    message.error('更新失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="profile-wrapper">
    <form class="profile-form" @submit.prevent="handleSubmit">
      <span class="form-title">个人信息</span>

      <!-- 头像区域：圆形 + 悬浮上传提示 -->
      <div class="avatar-area">
        <div class="avatar-wrapper" @click="handleAvatarClick">
          <img :src="avatarSrc" alt="头像" class="avatar-img" />
          <div class="avatar-overlay">
            <span v-if="avatarUploading">上传中...</span>
            <span v-else>点击上传图片</span>
          </div>
        </div>
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          class="file-input"
          @change="handleFileChange"
        />
      </div>

      <label class="form-label">昵称</label>
      <input
        v-model="formState.userName"
        class="form-input"
        type="text"
        placeholder="请输入昵称"
        required
      />

      <label class="form-label">个人简介</label>
      <textarea
        v-model="formState.userProfile"
        class="form-input form-textarea"
        placeholder="介绍一下自己吧~"
        rows="3"
      ></textarea>

      <button class="submit-btn" type="submit" :disabled="submitting">
        {{ submitting ? '保存中...' : '保存修改' }}
      </button>

      <button type="button" class="back-btn" @click="router.push('/home')">
        返回首页
      </button>
    </form>
  </div>
</template>

<style scoped>
.profile-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #e8e8e8;
}

/* From Uiverse.io by KhelVers —— 新拟态表单卡片 */
.profile-form {
  width: 26em;
  max-width: 100%;
  padding: 3em 4% 2em;
  border-radius: 16px;
  background: #e8e8e8;
  box-shadow:
    12px 12px 18px #bababa,
    -12px -12px 18px #ffffff;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

.form-title {
  text-align: center;
  font-size: 28px;
  letter-spacing: 3px;
  font-weight: 600;
  color: #555;
  text-shadow:
    -2px -2px 3px #ffffff,
    2px 2px 3px #bababa;
  margin-bottom: 18px;
}

/* 头像区域 */
.avatar-area {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  cursor: pointer;
  background: #f3f3f3;
  box-shadow:
    6px 6px 10px #bababa,
    -6px -6px 10px #ffffff;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.avatar-wrapper:hover {
  transform: translateY(-3px) scale(1.04);
  box-shadow:
    8px 8px 14px #a8a8a8,
    -8px -8px 14px #ffffff;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

/* 悬浮遮罩：点击上传图片 */
.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  opacity: 0;
  transition: opacity 0.25s ease;
  padding: 8px;
  box-sizing: border-box;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.file-input {
  display: none;
}

/* 标签 */
.form-label {
  font-size: 13px;
  letter-spacing: 1px;
  color: #666;
  margin-bottom: 6px;
  margin-top: 6px;
}

/* 新拟态输入框 */
.form-input {
  border: none;
  font-size: 14px;
  letter-spacing: 1px;
  border-radius: 8px;
  background: #f3f3f3;
  box-shadow:
    inset -2px -2px 4px #ffffff,
    inset 2px 2px 4px rgba(0, 0, 0, 0.356);
  padding: 12px 14px;
  margin-bottom: 16px;
  outline: none;
  width: 100%;
  box-sizing: border-box;
  font-family: inherit;
  color: #333;
  transition: all 0.15s ease;
  resize: vertical;
}

.form-input::placeholder {
  color: #999;
}

.form-input:focus {
  background: rgb(241, 241, 241);
  box-shadow:
    inset -2px -2px 4px #ffffff,
    inset 2px 2px 5px rgba(0, 0, 0, 0.4);
}

.form-textarea {
  min-height: 80px;
}

/* 提交按钮：渐变色 + 动画 */
.submit-btn {
  font-size: 16px;
  letter-spacing: 3px;
  color: white;
  font-weight: 700;
  background: linear-gradient(144deg, #af40ff, #5b42f3 50%, #00ddeb);
  border-radius: 8px;
  border: none;
  box-shadow:
    inset 1px 3px 3px #ffffffbd,
    inset -4px -4px 3px #00000046;
  background-size: 150% 150%;
  animation: gradient-shift 5s infinite;
  transition: all 0.3s ease;
  padding: 12px 16px;
  margin-top: 8px;
  cursor: pointer;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  background: linear-gradient(144deg, #9706ff, #2f0fff 50%, #18f0ff);
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.back-btn {
  background: transparent;
  color: #999;
  border: none;
  font-size: 14px;
  cursor: pointer;
  padding: 10px;
  margin-top: 8px;
  transition: color 0.2s;
}

.back-btn:hover {
  color: #666;
}

@keyframes gradient-shift {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

/* 响应式：小屏适配 */
@media (max-width: 480px) {
  .profile-form {
    width: 100%;
    padding: 2em 5%;
  }

  .form-title {
    font-size: 22px;
  }
}
</style>
