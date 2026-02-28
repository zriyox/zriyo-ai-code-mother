/**
 * 自定义指令
 */

import type { App, Directive, DirectiveBinding } from 'vue'

// 权限指令
const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const permissions = ['*'] // TODO: 从 store 获取权限

    if (value && !permissions.includes(value)) {
      el.parentNode?.removeChild(el)
    }
  },
}

// 复制指令
const copy: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    el.addEventListener('click', () => {
      const text = binding.value
      navigator.clipboard.writeText(text).then(() => {
        // 显示成功提示
        console.log('Copied:', text)
      })
    })
  },
}

// 加载指令
const loading: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const isLoading = binding.value
    if (isLoading) {
      el.classList.add('is-loading')
      ;(el as any).disabled = true
    } else {
      el.classList.remove('is-loading')
      ;(el as any).disabled = false
    }
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    const isLoading = binding.value
    if (isLoading) {
      el.classList.add('is-loading')
      ;(el as any).disabled = true
    } else {
      el.classList.remove('is-loading')
      ;(el as any).disabled = false
    }
  },
}

// 防抖指令
const debounce: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    let timer: number | null = null
    const delay = binding.arg ? Number(binding.arg) : 300

    el.addEventListener('click', () => {
      if (timer) {
        clearTimeout(timer)
      }
      timer = window.setTimeout(() => {
        binding.value()
      }, delay)
    })
  },
}

// 节流指令
const throttle: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    let lastTime = 0
    const delay = binding.arg ? Number(binding.arg) : 300

    el.addEventListener('click', () => {
      const now = Date.now()
      if (now - lastTime >= delay) {
        binding.value()
        lastTime = now
      }
    })
  },
}

// 无限滚动指令
const infiniteScroll: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const callback = binding.value
    const options = binding.arg || {}

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            callback()
          }
        })
      },
      {
        rootMargin: '100px',
        ...options,
      }
    )

    observer.observe(el)

    // 保存 observer 以便销毁
    ;(el as any)._infiniteScrollObserver = observer
  },
  unmounted(el: HTMLElement) {
    const observer = (el as any)._infiniteScrollObserver
    if (observer) {
      observer.disconnect()
    }
  },
}

// 长按指令
const longPress: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    let timer: number | null = null

    const start = () => {
      timer = window.setTimeout(() => {
        binding.value()
      }, 500)
    }

    const cancel = () => {
      if (timer) {
        clearTimeout(timer)
      }
    }

    el.addEventListener('mousedown', start)
    el.addEventListener('touchstart', start)
    el.addEventListener('mouseup', cancel)
    el.addEventListener('mouseleave', cancel)
    el.addEventListener('touchend', cancel)
    el.addEventListener('touchcancel', cancel)
  },
}

// 设置所有指令
export function setupDirectives(app: App) {
  app.directive('permission', permission)
  app.directive('copy', copy)
  app.directive('loading', loading)
  app.directive('debounce', debounce)
  app.directive('throttle', throttle)
  app.directive('infinite-scroll', infiniteScroll)
  app.directive('long-press', longPress)
}

export {
  permission,
  copy,
  loading,
  debounce,
  throttle,
  infiniteScroll,
  longPress,
}
