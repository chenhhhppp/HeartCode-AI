<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  current: number
  pageSize: number
  total: number
  showTotal?: (total: number) => string
}

const props = withDefaults(defineProps<Props>(), {
  showTotal: (total: number) => `共 ${total} 条`,
})

const emit = defineEmits<{
  (e: 'update:current', page: number): void
  (e: 'update:pageSize', size: number): void
  (e: 'change', page: number, pageSize: number): void
}>()

const totalPages = computed(() => {
  return Math.max(1, Math.ceil(props.total / props.pageSize))
})

// 生成要显示的页码（最多显示 5 个数字页码，带省略号）
const pageList = computed<(number | string)[]>(() => {
  const total = totalPages.value
  const cur = props.current
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }
  const pages: (number | string)[] = [1]
  if (cur > 4) pages.push('...')
  const start = Math.max(2, cur - 1)
  const end = Math.min(total - 1, cur + 1)
  for (let i = start; i <= end; i++) pages.push(i)
  if (cur < total - 3) pages.push('...')
  pages.push(total)
  return pages
})

const go = (page: number) => {
  if (page < 1 || page > totalPages.value || page === props.current) return
  emit('update:current', page)
  emit('change', page, props.pageSize)
}

const prev = () => go(props.current - 1)
const next = () => go(props.current + 1)
</script>

<template>
  <div class="pagination-wrap">
    <span v-if="showTotal" class="pagination-total">{{ showTotal(total) }}</span>
    <div class="button-container">
      <!-- 上一页 -->
      <button
        class="page-btn"
        :disabled="current <= 1"
        @click="prev"
      >
        ❮
      </button>

      <!-- 数字页码 -->
      <button
        v-for="(p, idx) in pageList"
        :key="idx"
        class="page-btn"
        :class="{ active: p === current, ellipsis: p === '...' }"
        :disabled="p === '...'"
        @click="typeof p === 'number' && go(p)"
      >
        {{ p }}
      </button>

      <!-- 下一页 -->
      <button
        class="page-btn"
        :disabled="current >= totalPages"
        @click="next"
      >
        ❯
      </button>
    </div>
  </div>
</template>

<style scoped>
.pagination-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.pagination-total {
  font-size: 14px;
  color: #999;
  font-style: italic;
  font-family: 'Brush Script MT', cursive;
}

.button-container {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px;
}

/* From Uiverse.io by TCdesign-dev */
.page-btn {
  align-items: center;
  appearance: none;
  background-color: #fcfcfd;
  border-radius: 4px;
  border-width: 0;
  box-shadow:
    rgba(45, 35, 66, 0.2) 0 2px 4px,
    rgba(45, 35, 66, 0.15) 0 7px 13px -3px,
    #d6d6e7 0 -3px 0 inset;
  box-sizing: border-box;
  color: #36395a;
  cursor: pointer;
  display: inline-flex;
  height: 40px;
  justify-content: center;
  line-height: 1;
  list-style: none;
  overflow: hidden;
  padding-left: 12px;
  padding-right: 12px;
  min-width: 40px;
  position: relative;
  text-align: center;
  text-decoration: none;
  transition:
    box-shadow 0.15s,
    transform 0.15s;
  user-select: none;
  -webkit-user-select: none;
  touch-action: manipulation;
  white-space: nowrap;
  will-change: box-shadow, transform;
  font-size: 15px;
  font-weight: 500;
}

.page-btn:focus {
  box-shadow:
    #d6d6e7 0 0 0 1.5px inset,
    rgba(45, 35, 66, 0.4) 0 2px 4px,
    rgba(45, 35, 66, 0.3) 0 7px 13px -3px,
    #d6d6e7 0 -3px 0 inset;
}

.page-btn:hover:not(:disabled):not(.active) {
  box-shadow:
    rgba(45, 35, 66, 0.3) 0 4px 8px,
    rgba(45, 35, 66, 0.2) 0 7px 13px -3px,
    #d6d6e7 0 -3px 0 inset;
  transform: translateY(-2px);
}

.page-btn:active:not(:disabled) {
  box-shadow: #d6d6e7 0 3px 7px inset;
  transform: translateY(2px);
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.35;
}

/* 当前选中页 */
.page-btn.active {
  background-image: linear-gradient(145deg, #6a11cb, #2575fc);
  color: #fff;
  box-shadow:
    rgba(106, 17, 203, 0.4) 0 4px 8px,
    rgba(37, 117, 252, 0.3) 0 7px 13px -3px,
    rgba(106, 17, 203, 0.5) 0 -3px 0 inset;
}

/* 省略号 */
.page-btn.ellipsis {
  cursor: default;
  opacity: 0.5;
  box-shadow: none;
  background: transparent;
}
</style>
