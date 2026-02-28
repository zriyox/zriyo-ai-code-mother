<!--
SVG 图标组件
使用 Iconify 或本地 SVG 图标
-->
<template>
  <svg
    v-if="isLocal"
    class="svg-icon"
    :class="className"
    :style="styleObject"
    aria-hidden="true"
    v-on="$attrs"
  >
    <use :xlink:href="`#icon-${name}`" />
  </svg>
  <span
    v-else
    class="iconify"
    :class="className"
    :style="{ ...styleObject, fontSize: size + 'px' }"
    v-on="$attrs"
  ></span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  name: string
  size?: number | string
  color?: string
  className?: string
  local?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 16,
  color: '',
  className: '',
  local: false,
})

const isLocal = computed(() => props.local)

const styleObject = computed(() => {
  const style: Record<string, string> = {}
  if (props.color) {
    style.color = props.color
  }
  if (typeof props.size === 'number') {
    style.fontSize = `${props.size}px`
  }
  return style
})
</script>

<style scoped>
.svg-icon {
  width: 1em;
  height: 1em;
  fill: currentColor;
  vertical-align: middle;
}
</style>
