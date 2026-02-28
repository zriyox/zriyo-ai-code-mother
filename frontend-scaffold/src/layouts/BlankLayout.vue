<!--
空白布局组件
用于登录页、404页等不需要侧边栏的页面
-->
<template>
  <div class="blank-layout">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// 空白布局，不需要额外逻辑
</script>

<style scoped>
.blank-layout {
  width: 100%;
  height: 100vh;
  background-color: #f3f4f6;
}

.dark .blank-layout {
  background-color: #111827;
}
</style>
