<script setup lang="ts">
import { GithubOutlined } from '@ant-design/icons-vue'

const currentYear = new Date().getFullYear()
const githubUrl = 'https://github.com/chenhhhppp'
</script>

<template>
  <div class="global-footer">
    <div class="footer-content">
      <p class="copyright">
        HeartCode 心码 AI 应用生成平台 by CHP © {{ currentYear }}
      </p>
      <a
        class="github-link"
        :href="githubUrl"
        target="_blank"
        rel="noopener noreferrer"
      >
        <GithubOutlined class="github-icon" />
        <span>Github: {{ githubUrl }}</span>
      </a>
    </div>
  </div>
</template>

<style scoped>
.global-footer {
  width: 100%;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.footer-content {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px;
  text-align: center;
}

.copyright {
  margin: 0 0 8px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
}

.github-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  text-decoration: none;
  transition: color 0.2s ease;
}

.github-link:hover {
  color: #1890ff;
}

.github-icon {
  font-size: 15px;
}

@media (max-width: 768px) {
  .footer-content {
    padding: 16px;
  }

  .copyright,
  .github-link {
    font-size: 12px;
  }
}
</style>
