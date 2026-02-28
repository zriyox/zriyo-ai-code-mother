<!--
空状态组件
-->
<template>
  <div class="empty-state">
    <slot name="icon">
      <svg class="empty-icon" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="32" cy="32" r="30" stroke="currentColor" stroke-width="2" stroke-dasharray="4 4" opacity="0.3"/>
        <path d="M32 20V32M32 44V44" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.5"/>
      </svg>
    </slot>
    <p class="empty-text">
      <slot name="text">{{ text }}</slot>
    </p>
    <p v-if="description" class="empty-description">{{ description }}</p>
    <slot name="action"></slot>
  </div>
</template>

<script setup lang="ts">
interface Props {
  text?: string
  description?: string
}

withDefaults(defineProps<Props>(), {
  text: '暂无数据',
  description: '',
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  width: 64px;
  height: 64px;
  color: var(--text-color-placeholder, #9ca3af);
  margin-bottom: 20px;
}

.empty-text {
  font-size: 16px;
  color: var(--text-color-secondary, #6b7280);
  margin-bottom: 8px;
}

.empty-description {
  font-size: 14px;
  color: var(--text-color-placeholder, #9ca3af);
  max-width: 400px;
}
</style>
