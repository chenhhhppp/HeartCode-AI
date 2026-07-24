<script setup lang="ts">
const currentYear = new Date().getFullYear()
</script>

<template>
  <div class="global-footer">
    <div class="footer-content">
      <p class="copyright">
        HeartCode 心码 AI 应用生成平台 by CHP © {{ currentYear }}
      </p>
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
  margin: 0;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
}

@media (max-width: 768px) {
  .footer-content {
    padding: 16px;
  }

  .copyright {
    font-size: 12px;
  }
}
</style>
