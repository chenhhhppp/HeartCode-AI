<template>
  <div class="home-page">
    <div class="welcome-section">
      <h1>欢迎使用 HeartCode 心码 AI 应用生成平台</h1>
      <p class="subtitle">智能、高效、创新</p>
    </div>

    <div class="features">
      <a-card title="智能生成" class="feature-card">
        <p>AI 驱动的应用生成引擎</p>
      </a-card>
      <a-card title="高效开发" class="feature-card">
        <p>快速构建现代化应用</p>
      </a-card>
      <a-card title="创新体验" class="feature-card">
        <p>全新的开发工作流</p>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home-page {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  min-height: 500px;
}

.welcome-section {
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  margin-bottom: 32px;
  color: #fff;
}

.welcome-section h1 {
  font-size: 32px;
  margin-bottom: 16px;
  font-weight: 600;
}

.subtitle {
  font-size: 18px;
  opacity: 0.9;
  margin: 0;
}

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.feature-card {
  text-align: center;
}

.feature-card p {
  color: rgba(0, 0, 0, 0.65);
  margin: 0;
}
</style>
