import type { Directive } from 'vue'
import { hasPerm } from '@/utils/permission'

export const permission: Directive = {
  mounted(el, binding) {
    const perm = binding.value as string
    if (!perm) return
    if (!hasPerm(perm)) {
      el.parentNode?.removeChild(el) // 无权限直接移除该元素（按钮/区块）
    }
  }
}
