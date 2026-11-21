import request from "@/utils/request"

// 待付款流程：审批过了、模板要付款、还没生成付款记录
export interface PendingPaymentItem {
    instanceId: number
    instanceNo: string
    title: string
    flowName: string
    creatorName: string
    orgName: string
    amount: number | null
    reason: string
    attachmentId: number | null
    // 后端按关键词猜的费用类别，付款时可以改
    suggestCategoryId: number | null
    suggestCategoryName: string
    submitTime: string
    finishTime: string
}

// 付款记录
export interface PaymentRecordItem {
    id: number
    payNo: string
    instanceId: number
    instanceNo: string
    title: string
    flowName: string
    applicantName: string
    categoryId: number | null
    categoryName: string
    payeeName: string
    payeeAccount: string
    amount: number | null
    payMethod: number
    payMethodText: string
    payTime: string
    status: number
    statusText: string
    operatorName: string
    remark: string
    voidReason: string
    voidTime: string
}

export interface PageResult<T> {
    records: T[]
    total: number
}

export interface PaymentSummary {
    pendingCount: number
    pendingAmount: number
    monthCount: number
    monthAmount: number
}

// 待付款列表
export function getPendingPayments(params: { pageNum: number; pageSize: number; keyword?: string }) {
    return request.get<any, PageResult<PendingPaymentItem>>("/finance/payments/pending", { params })
}

// 已付款记录
export function getPaymentRecords(params: {
    pageNum: number
    pageSize: number
    keyword?: string
    payMethod?: number
    status?: number
    startTime?: string
    endTime?: string
}) {
    return request.get<any, PageResult<PaymentRecordItem>>("/finance/payments/records", { params })
}

// 顶部统计
export function getPaymentSummary() {
    return request.get<any, PaymentSummary>("/finance/payments/summary")
}

// 执行付款
export function payPayment(data: {
    instanceId: number
    amount: number
    payeeName: string
    payeeAccount?: string
    payMethod: number
    payTime?: string
    remark?: string
    // 财务选的费用类别，不传后端按关键词猜
    categoryId?: number | null
}) {
    return request.post<any, number>("/finance/payments/pay", data)
}

// 作废付款
export function voidPayment(id: number, reason: string) {
    return request.post<any, null>(`/finance/payments/${id}/void`, { reason })
}

// 导出付款记录
export function exportPaymentRecords(params: {
    keyword?: string
    payMethod?: number
    status?: number
    startTime?: string
    endTime?: string
}) {
    return request.get<any, Blob>("/finance/payments/export", { params, responseType: "blob" })
}

// 财务统计

// 统计概览：金额、笔数、平均值、最高类别、环比
export interface StatOverview {
    totalAmount: number
    recordCount: number
    avgAmount: number
    maxAmount: number
    categoryCount: number
    orgCount: number
    topCategoryName: string
    topCategoryAmount: number
    prevAmount: number
    growthRate: number | null
    // 已审批通过但还没付款的金额和笔数
    pendingAmount: number
    pendingCount: number
    // 金额识别不出来的笔数，按 0 计，要人工补
    pendingUnknownCount: number
    startDate: string
    endDate: string
}

export interface CategoryStatRow {
    categoryName: string
    amount: number
    count: number
    ratio: number
}

export interface DepartmentStatRow {
    orgName: string
    amount: number
    count: number
    ratio: number
}

export interface StatTrend {
    granularity: "month" | "day"
    labels: string[]
    amounts: number[]
    counts: number[]
}

export interface StatDetailItem {
    id: number
    payNo: string
    instanceNo: string
    title: string
    flowName: string
    applicantName: string
    orgName: string
    categoryName: string
    amount: number
    payMethodText: string
    payTime: string
}

export interface StatQuery {
    startDate?: string
    endDate?: string
    orgId?: number | null
    categoryName?: string
}

// 统计概览
export function getStatOverview(params: StatQuery) {
    return request.get<any, StatOverview>("/finance/stat/overview", { params })
}

// 按费用类别汇总
export function getStatByCategory(params: StatQuery) {
    return request.get<any, CategoryStatRow[]>("/finance/stat/by-category", { params })
}

// 按部门汇总
export function getStatByDepartment(params: StatQuery) {
    return request.get<any, DepartmentStatRow[]>("/finance/stat/by-department", { params })
}

// 金额趋势
export function getStatTrend(params: StatQuery & { granularity?: "month" | "day" }) {
    return request.get<any, StatTrend>("/finance/stat/trend", { params })
}

// 统计明细
export function getStatDetail(params: StatQuery & { pageNum: number; pageSize: number }) {
    return request.get<any, { records: StatDetailItem[]; total: number }>("/finance/stat/detail", { params })
}

// 导出财务报表
export function exportStatReport(params: StatQuery) {
    return request.get<any, Blob>("/finance/stat/export", { params, responseType: "blob" })
}

// 费用类别维护

export interface ExpenseCategoryItem {
    id: number
    categoryName: string
    categoryCode: string
    keywords: string
    sortOrder: number
    status: number
    remark: string
    createdAt: string
}

// 类别列表（统计页的筛选下拉也用它）
export function getExpenseCategories() {
    return request.get<any, ExpenseCategoryItem[]>("/finance/categories")
}

// 新增类别
export function createExpenseCategory(data: {
    categoryName: string
    categoryCode?: string
    keywords?: string
    sortOrder?: number
    status?: number
    remark?: string
}) {
    return request.post<any, number>("/finance/categories", data)
}

// 修改类别
export function updateExpenseCategory(data: {
    id: number
    categoryName: string
    categoryCode?: string
    keywords?: string
    sortOrder?: number
    status?: number
    remark?: string
}) {
    return request.put<any, null>("/finance/categories", data)
}

// 删除类别
export function deleteExpenseCategory(id: number) {
    return request.delete<any, null>(`/finance/categories/${id}`)
}
