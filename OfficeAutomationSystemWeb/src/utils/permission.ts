import { useUserStore } from '@/store/user'

// 判断当前用户是否拥有某个权限标识
export function hasPerm(perm: string): boolean {
  const store = useUserStore()
  // admin 全部放行，其余查 permissions
  if (store.roles?.includes('admin')) return true
  return store.permissions?.includes(perm) ?? false
}

// 是否拥有任意一个权限，编辑或删除任一即可显示
export function hasAnyPerm(perms: string[]): boolean {
  return perms.some(p => hasPerm(p))
}
