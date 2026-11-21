import request from "@/utils/request"

// 登录日志，人事主管查异常账号，字段对应 LoginLogController
export interface LoginLogItem {
    id: number
    userId: number | null
    username: string
    loginTime: string
    ipAddress: string | null
    location: string | null
    device: string | null
    browser: string | null
    os: string | null
    status: number
    failReason: string | null
    logoutTime: string | null
}

export interface LoginTrendItem {
    date: string
    successCount: number
    failCount: number
}

export interface LoginOverview {
    todayTotal: number
    todaySuccess: number
    todayFail: number
    todayUsers: number
    trend: LoginTrendItem[]
    failReasons: Array<{ reason: string; amount: number }>
    topUsers: Array<{ username: string; amount: number; lastTime: string }>
}

export interface LoginLogPage {
    records: LoginLogItem[]
    total: number
    pageNum: number
    pageSize: number
}

export function pageLoginLogs(params: Record<string, unknown>) {
    return request.get<any, LoginLogPage>("/system/login-logs", { params })
}

export function getLoginOverview() {
    return request.get<any, LoginOverview>("/system/login-logs/overview")
}