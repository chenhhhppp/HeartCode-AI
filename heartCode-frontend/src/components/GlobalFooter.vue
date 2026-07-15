<script setup lang="ts">
const copyright = 'HeartCode 心码 AI 零代码应用生成平台 by CHP'
const currentYear = new Date().getFullYear()
</script>

<template>
  <footer class="global-footer">
    <div class="footer-container">
      <p class="copyright">
        © {{ currentYear }} {{ copyright }}
      </p>
    </div>
  </footer>
</template>

<style scoped>
.global-footer {
  background-color: #ffffff;
  border-top: 1px solid #e8e8e8;
  padding: 16px 0;
}

.footer-container {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 24px;
  text-align: center;
}

.copyright {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .footer-container {
    padding: 0 16px;
  }

  .copyright {
    font-size: 12px;
  }
}
</style>
