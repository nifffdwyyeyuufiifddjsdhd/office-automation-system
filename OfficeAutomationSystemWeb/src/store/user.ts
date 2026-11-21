import { defineStore } from "pinia";
import { getMyAuth } from "@/api/auth";

// 多角色时按权限优先级取主角色，菜单不被降级
const ROLE_PRIORITY = ['admin', 'gm', 'manager', 'project_manager', 'hr_manager', 'hr', 'finance', 'employee']
function pickRole(roles: string[]): string {
    return ROLE_PRIORITY.find(r => roles.includes(r)) || (roles[0] || 'staff')
}

// 安全读取 sessionStorage 中的 JSON 数组
function readJsonArray(key: string): string[] {
    try {
        const raw = sessionStorage.getItem(key)
        if (!raw) return []
        const parsed = JSON.parse(raw)
        return Array.isArray(parsed) ? parsed : []
    } catch {
        return []
    }
}

export const useUserStore = defineStore('user', {
    state: () => ({
        token: sessionStorage.getItem('token') || '',
        userId: Number(sessionStorage.getItem('userId')) || 0,
        mustChangePassword: sessionStorage.getItem('mustChangePassword') === 'true',
        username: sessionStorage.getItem('username') || '',
        realName: sessionStorage.getItem('realName') || '',
        role: sessionStorage.getItem('role') || '',
        roles: readJsonArray('roles'),          // 角色编码列表（来自后端 login.role）
        permissions: readJsonArray('permissions') // 权限标识列表（来自后端 login.permissions）
    }),
    getters: {
        isLoggedIn: (state) => !!state.token
    },
    actions: {
        setLogin(data: any) {
            this.token = data.token
            this.userId = data.userId != null ? Number(data.userId) : 0
            this.mustChangePassword = data.mustChangePassword
            this.username = data.username || ''
            this.realName = data.realName || ''
            this.roles = Array.isArray(data.role) ? data.role : []
            this.permissions = Array.isArray(data.permissions) ? data.permissions : []
            this.role = pickRole(this.roles)
            sessionStorage.setItem('token', data.token)
            sessionStorage.setItem('userId', String(this.userId))
            sessionStorage.setItem('mustChangePassword', String(data.mustChangePassword))
            sessionStorage.setItem('username', this.username)
            sessionStorage.setItem('realName', this.realName)
            sessionStorage.setItem('role', this.role)
            sessionStorage.setItem('roles', JSON.stringify(this.roles))
            sessionStorage.setItem('permissions', JSON.stringify(this.permissions))
        },
        // 从后端重拉角色与权限，新发的权限不用重登就生效
        async refreshAuth() {
            if (!this.token) return
            const data = await getMyAuth()
            if (!data) return
            this.roles = Array.isArray(data.role) ? data.role : []
            this.permissions = Array.isArray(data.permissions) ? data.permissions : []
            this.role = pickRole(this.roles)
            sessionStorage.setItem('role', this.role)
            sessionStorage.setItem('roles', JSON.stringify(this.roles))
            sessionStorage.setItem('permissions', JSON.stringify(this.permissions))
        },
        clearLogin() {
            this.token = ''
            this.userId = 0
            this.mustChangePassword = false
            this.username = ''
            this.realName = ''
            this.role = ''
            this.roles = []
            this.permissions = []
            sessionStorage.removeItem('token')
            sessionStorage.removeItem('userId')
            sessionStorage.removeItem('mustChangePassword')
            sessionStorage.removeItem('username')
            sessionStorage.removeItem('realName')
            sessionStorage.removeItem('role')
            sessionStorage.removeItem('roles')
            sessionStorage.removeItem('permissions')
        }
    }
})
