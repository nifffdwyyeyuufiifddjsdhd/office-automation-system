<template>
    <div class="finance-page">
        <div class="page-head">
            <div class="head-left">
                <span class="head-bar"></span>
                <div>
                    <h2>付款管理</h2>
                    <p>对审批通过的报销、付款、采购流程执行付款并留痕</p>
                </div>
            </div>
        </div>

        <!-- 顶部四张统计卡 -->
        <div class="stat-grid">
            <div class="stat-card">
                <div class="stat-icon pending"><el-icon><Wallet /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-num">{{ summary.pendingCount }}</div>
                    <div class="stat-label">待付款（笔）</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon amount"><el-icon><Money /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-num">{{ money(summary.pendingAmount) }}</div>
                    <div class="stat-label">待付款金额（元）</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon done"><el-icon><CircleCheck /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-num">{{ summary.monthCount }}</div>
                    <div class="stat-label">本月已付款（笔）</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon paid"><el-icon><Coin /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-num">{{ money(summary.monthAmount) }}</div>
                    <div class="stat-label">本月付款金额（元）</div>
                </div>
            </div>
        </div>

        <el-card shadow="never" class="main-card">
            <el-tabs v-model="activeTab" @tab-change="onTabChange">
                <!-- 待付款 -->
                <el-tab-pane label="待付款" name="pending">
                    <div class="toolbar">
                        <el-input
                            v-model="pendingQuery.keyword"
                            placeholder="流程标题 / 流程编号"
                            clearable
                            class="w-240"
                            @keyup.enter="loadPending(1)"
                            @clear="loadPending(1)"
                        />
                        <el-button type="primary" @click="loadPending(1)">查询</el-button>
                        <el-button @click="reloadPending">刷新</el-button>
                        <span class="toolbar-tip">只有审批通过且标记了「需要财务付款」的流程才会出现在这里</span>
                    </div>
                    <el-table
                        v-loading="pendingLoading"
                        :data="pendingRows"
                        stripe
                        class="data-table"
                        empty-text="暂无待付款流程"
                    >
                        <el-table-column prop="instanceNo" label="流程编号" min-width="150" />
                        <el-table-column prop="title" label="流程标题" min-width="160">
                            <template #default="{ row }">
                                <el-link type="primary" :underline="false" @click="openFlow(row.instanceId)">
                                    {{ row.title }}
                                </el-link>
                            </template>
                        </el-table-column>
                        <el-table-column prop="flowName" label="流程名称" width="100" show-overflow-tooltip />
                        <el-table-column prop="creatorName" label="申请人" width="80" />
                        <el-table-column prop="orgName" label="部门" width="90" show-overflow-tooltip />
                        <el-table-column label="金额（元）" width="110" align="right">
                            <template #default="{ row }">
                                <span :class="{ 'amount-warn': row.amount === null }"
                                      :title="row.amount === null ? '流程表单里没有可识别的金额，付款时按发票手工填写' : ''">
                                    {{ row.amount === null ? '待补金额' : money(row.amount) }}
                                </span>
                            </template>
                        </el-table-column>
                        <el-table-column label="费用类别" width="90">
                            <template #default="{ row }">
                                <el-tag v-if="row.suggestCategoryName" size="small" effect="plain">
                                    {{ row.suggestCategoryName }}
                                </el-tag>
                                <span v-else class="text-muted">—</span>
                            </template>
                        </el-table-column>
                        <el-table-column prop="reason" label="事由" min-width="100" show-overflow-tooltip />
                        <el-table-column prop="finishTime" label="审批完成时间" width="115">
                            <template #default="{ row }">
                                <span :title="row.finishTime">{{ shortTime(row.finishTime) }}</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="130" fixed="right">
                            <template #default="{ row }">
                                <el-button
                                    v-if="canPay"
                                    type="primary"
                                    link
                                    @click="openPayDialog(row)"
                                >
                                    付款
                                </el-button>
                                <el-button type="primary" link @click="openFlow(row.instanceId)">查看</el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                    <div class="pager">
                        <el-pagination
                            background
                            layout="total, prev, pager, next, sizes"
                            :total="pendingTotal"
                            :current-page="pendingQuery.pageNum"
                            :page-size="pendingQuery.pageSize"
                            :page-sizes="[10, 20, 50]"
                            @current-change="(p: number) => { pendingQuery.pageNum = p; loadPending(); }"
                            @size-change="(s: number) => { pendingQuery.pageSize = s; loadPending(1); }"
                        />
                    </div>
                </el-tab-pane>

                <!-- 付款记录 -->
                <el-tab-pane label="付款记录" name="records">
                    <div class="toolbar">
                        <el-input
                            v-model="recordQuery.keyword"
                            placeholder="付款单号 / 标题 / 收款人"
                            clearable
                            class="w-220"
                            @keyup.enter="loadRecords(1)"
                        />
                        <el-select v-model="recordQuery.payMethod" placeholder="付款方式" clearable class="w-140">
                            <el-option
                                v-for="item in payMethods"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value"
                            />
                        </el-select>
                        <el-select v-model="recordQuery.status" placeholder="状态" clearable class="w-120">
                            <el-option label="已付款" :value="1" />
                            <el-option label="已作废" :value="2" />
                        </el-select>
                        <el-date-picker
                            v-model="recordDateRange"
                            type="daterange"
                            range-separator="至"
                            start-placeholder="付款开始日期"
                            end-placeholder="付款结束日期"
                            value-format="YYYY-MM-DD"
                            class="w-260"
                        />
                        <el-button type="primary" @click="loadRecords(1)">查询</el-button>
                        <el-button @click="resetRecords">重置</el-button>
                        <!-- 导出按钮要 finance:export 权限 -->
                        <el-button v-if="canExport" type="success" plain :loading="exporting" @click="handleExport">
                            导出 Excel
                        </el-button>
                    </div>
                    <el-table
                        v-loading="recordLoading"
                        :data="recordRows"
                        stripe
                        class="data-table"
                        empty-text="暂无付款记录"
                    >
                        <el-table-column prop="payNo" label="付款单号" width="150" show-overflow-tooltip />
                        <el-table-column prop="title" label="流程标题" width="155" show-overflow-tooltip>
                            <template #default="{ row }">
                                <el-link type="primary" :underline="false" @click="openFlow(row.instanceId)">
                                    {{ row.title }}
                                </el-link>
                            </template>
                        </el-table-column>
                        <el-table-column label="费用类别" width="96">
                            <template #default="{ row }">
                                <el-tag v-if="row.categoryName" size="small" effect="plain">
                                    {{ row.categoryName }}
                                </el-tag>
                                <span v-else class="text-muted">未分类</span>
                            </template>
                        </el-table-column>
                        <el-table-column prop="payeeName" label="收款人" width="80" show-overflow-tooltip />
                        <el-table-column label="金额（元）" width="105" align="right">
                            <template #default="{ row }">{{ money(row.amount) }}</template>
                        </el-table-column>
                        <el-table-column prop="payMethodText" label="付款方式" width="82" show-overflow-tooltip />
                        <el-table-column prop="payTime" label="付款时间" width="105">
                            <template #default="{ row }">
                                <span :title="row.payTime">{{ shortTime(row.payTime) }}</span>
                            </template>
                        </el-table-column>
                        <el-table-column prop="operatorName" label="付款人" width="80" show-overflow-tooltip />
                        <el-table-column label="状态" width="82">
                            <template #default="{ row }">
                                <el-tag :type="row.status === 2 ? 'info' : 'success'" effect="light">
                                    {{ row.statusText }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="remark" label="备注" width="92" show-overflow-tooltip />
                        <el-table-column label="操作" width="92" fixed="right">
                            <template #default="{ row }">
                                <el-button
                                    v-if="canVoid && row.status === 1"
                                    type="danger"
                                    link
                                    @click="openVoidDialog(row)"
                                >
                                    作废
                                </el-button>
                                <span v-else class="text-muted">—</span>
                            </template>
                        </el-table-column>
                    </el-table>
                    <div class="pager">
                        <el-pagination
                            background
                            layout="total, prev, pager, next, sizes"
                            :total="recordTotal"
                            :current-page="recordQuery.pageNum"
                            :page-size="recordQuery.pageSize"
                            :page-sizes="[10, 20, 50]"
                            @current-change="(p: number) => { recordQuery.pageNum = p; loadRecords(); }"
                            @size-change="(s: number) => { recordQuery.pageSize = s; loadRecords(1); }"
                        />
                    </div>
                </el-tab-pane>
            </el-tabs>
        </el-card>

        <!-- 付款弹窗 -->
        <el-dialog v-model="payVisible" title="执行付款" width="560px" :close-on-click-modal="false">
            <div class="pay-flow">
                <span class="pay-flow-label">流程</span>
                <span class="pay-flow-title">{{ currentPending?.title }}</span>
                <el-tag size="small" effect="plain">{{ currentPending?.flowName }}</el-tag>
                <span class="pay-flow-org">{{ currentPending?.orgName }} · {{ currentPending?.creatorName }}</span>
            </div>
            <el-form :model="payForm" label-width="96px" class="pay-form">
                <el-form-item label="付款金额">
                    <el-input v-model="payForm.amount" placeholder="请输入本次实际付款金额">
                        <template #append>元</template>
                    </el-input>
                    <div v-if="currentPending && currentPending.amount === null" class="form-tip">
                        该流程表单里没有可识别的金额字段，请按发票或附件核对后手工填写
                    </div>
                    <div v-else-if="currentPending" class="form-tip">
                        表单识别金额 {{ money(currentPending.amount) }} 元，可按实际付款修改
                    </div>
                </el-form-item>
                <el-form-item label="费用类别">
                    <el-select v-model="payForm.categoryId" class="w-full" placeholder="请选择费用类别" clearable>
                        <el-option
                            v-for="item in categoryOptions"
                            :key="item.id"
                            :label="item.categoryName"
                            :value="item.id"
                        />
                    </el-select>
                    <div class="form-tip">系统按关键词给了默认值，统计报表以这里确认的类别为准</div>
                </el-form-item>
                <el-form-item label="收款人">
                    <el-input v-model="payForm.payeeName" maxlength="64" placeholder="填写实际收款人姓名或单位" />
                </el-form-item>
                <el-form-item label="收款账号">
                    <el-input v-model="payForm.payeeAccount" maxlength="128" placeholder="可不填，银行卡号或账号" />
                </el-form-item>
                <el-form-item label="付款方式">
                    <el-select v-model="payForm.payMethod" class="w-full">
                        <el-option
                            v-for="item in payMethods"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="付款时间">
                    <el-date-picker
                        v-model="payForm.payTime"
                        type="datetime"
                        placeholder="默认当前时间"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        class="w-full"
                    />
                </el-form-item>
                <el-form-item label="备注">
                    <el-input
                        v-model="payForm.remark"
                        type="textarea"
                        :rows="2"
                        maxlength="500"
                        show-word-limit
                        placeholder="可填写票据号、付款说明等"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="payVisible = false">取消</el-button>
                <el-button type="primary" :loading="paySubmitting" @click="submitPay">确认付款</el-button>
            </template>
        </el-dialog>

        <!-- 作废弹窗 -->
        <el-dialog v-model="voidVisible" title="作废付款记录" width="480px" :close-on-click-modal="false">
            <div class="void-flow">
                作废后该流程会重新回到待付款列表，记录本身保留但标记为已作废。
            </div>
            <el-form label-width="90px">
                <el-form-item label="付款单号">
                    <span>{{ currentRecord?.payNo }}</span>
                </el-form-item>
                <el-form-item label="作废原因">
                    <el-input
                        v-model="voidReason"
                        type="textarea"
                        :rows="3"
                        maxlength="255"
                        show-word-limit
                        placeholder="请填写作废原因，申请人会收到通知"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="voidVisible = false">取消</el-button>
                <el-button type="danger" :loading="voidSubmitting" @click="submitVoid">确认作废</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hasPerm } from '@/utils/permission'
import {
    exportPaymentRecords,
    getExpenseCategories,
    getPaymentRecords,
    getPaymentSummary,
    getPendingPayments,
    payPayment,
    voidPayment,
    type ExpenseCategoryItem,
    type PaymentRecordItem,
    type PendingPaymentItem,
} from '@/api/finance'

const router = useRouter()

// 付款、导出、作废各自一个权限码，后端 @RequirePermission 也拦
const canPay = computed(() => hasPerm('finance:pay'))
const canExport = computed(() => hasPerm('finance:export'))
const canVoid = computed(() => hasPerm('finance:pay:void'))

const payMethods = [
    { label: '银行转账', value: 1 },
    { label: '现金', value: 2 },
    { label: '微信', value: 3 },
    { label: '支付宝', value: 4 },
    { label: '支票', value: 5 },
]

const activeTab = ref<'pending' | 'records'>('pending')
const summary = reactive({ pendingCount: 0, pendingAmount: 0, monthCount: 0, monthAmount: 0 })

// 待付款
const pendingLoading = ref(false)
const pendingRows = ref<PendingPaymentItem[]>([])
const pendingTotal = ref(0)
const pendingQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

async function loadPending(resetPage?: number) {
    if (resetPage) pendingQuery.pageNum = resetPage
    pendingLoading.value = true
    try {
        const res = await getPendingPayments({
            pageNum: pendingQuery.pageNum,
            pageSize: pendingQuery.pageSize,
            keyword: pendingQuery.keyword || undefined,
        })
        pendingRows.value = res.records || []
        pendingTotal.value = res.total || 0
    } catch (e: any) {
        ElMessage.error(e?.message || '加载待付款失败')
    } finally {
        pendingLoading.value = false
    }
}

function reloadPending() {
    loadPending(1)
    loadSummary()
}

// 付款记录
const recordLoading = ref(false)
const recordRows = ref<PaymentRecordItem[]>([])
const recordTotal = ref(0)
const recordQuery = reactive<{
    pageNum: number
    pageSize: number
    keyword: string
    payMethod?: number
    status?: number
    startTime?: string
    endTime?: string
}>({ pageNum: 1, pageSize: 10, keyword: '', payMethod: undefined, status: undefined })
const recordDateRange = ref<[string, string] | null>(null)

async function loadRecords(resetPage?: number) {
    if (resetPage) recordQuery.pageNum = resetPage
    recordQuery.startTime = recordDateRange.value?.[0]
    recordQuery.endTime = recordDateRange.value?.[1]
    recordLoading.value = true
    try {
        const res = await getPaymentRecords({
            pageNum: recordQuery.pageNum,
            pageSize: recordQuery.pageSize,
            keyword: recordQuery.keyword || undefined,
            payMethod: recordQuery.payMethod || undefined,
            status: recordQuery.status || undefined,
            startTime: recordQuery.startTime || undefined,
            endTime: recordQuery.endTime || undefined,
        })
        recordRows.value = res.records || []
        recordTotal.value = res.total || 0
    } catch (e: any) {
        ElMessage.error(e?.message || '加载付款记录失败')
    } finally {
        recordLoading.value = false
    }
}

function resetRecords() {
    recordQuery.keyword = ''
    recordQuery.payMethod = undefined
    recordQuery.status = undefined
    recordDateRange.value = null
    loadRecords(1)
}

// 统计
async function loadSummary() {
    try {
        const res = await getPaymentSummary()
        summary.pendingCount = res.pendingCount || 0
        summary.pendingAmount = res.pendingAmount || 0
        summary.monthCount = res.monthCount || 0
        summary.monthAmount = res.monthAmount || 0
    } catch (e: any) {
        // 统计失败给个提示，不然顶部全 0 像真没数据
        ElMessage.warning(e?.message || '付款统计加载失败')
    }
}

// 付款
const payVisible = ref(false)
const paySubmitting = ref(false)
const currentPending = ref<PendingPaymentItem | null>(null)
// 费用类别选项，付款时选一次，报表才不会出现未分类
const categoryOptions = ref<ExpenseCategoryItem[]>([])

async function loadCategories() {
    try {
        // 后端只返回启用的类别，这里再过滤一次
        const res = await getExpenseCategories()
        categoryOptions.value = (res || []).filter((item) => item.status === 1)
    } catch (e: any) {
        // 类别加载失败不挡付款，用户可以留空走后端默认
        ElMessage.warning(e?.message || '费用类别加载失败，可先留空由后端按关键词匹配')
    }
}

const payForm = reactive({
    amount: '',
    categoryId: null as number | null,
    payeeName: '',
    payeeAccount: '',
    payMethod: 1,
    payTime: '',
    remark: '',
})

function openPayDialog(row: PendingPaymentItem) {
    currentPending.value = row
    payForm.amount = row.amount === null ? '' : String(row.amount)
    payForm.categoryId = row.suggestCategoryId ?? null
    // 收款人不预填申请人，付供应商时容易记错人
    payForm.payeeName = ''
    payForm.payeeAccount = ''
    payForm.payMethod = 1
    payForm.payTime = nowText()
    payForm.remark = ''
    payVisible.value = true
}

async function submitPay() {
    if (!currentPending.value) return
    const amountText = String(payForm.amount).trim()
    // 金额两位小数、上限一个亿，免得 1.005 这种值算错分
    if (!/^\d+(\.\d{1,2})?$/.test(amountText)) {
        ElMessage.warning('付款金额最多保留两位小数')
        return
    }
    const amount = Number(amountText)
    if (Number.isNaN(amount) || amount <= 0) {
        ElMessage.warning('请填写有效的付款金额')
        return
    }
    if (amount > 100000000) {
        ElMessage.warning('单笔付款金额不能超过一亿元')
        return
    }
    if (!payForm.payeeName.trim()) {
        ElMessage.warning('请填写收款人')
        return
    }
    paySubmitting.value = true
    try {
        await payPayment({
            instanceId: currentPending.value.instanceId,
            amount,
            payeeName: payForm.payeeName.trim(),
            payeeAccount: payForm.payeeAccount.trim() || undefined,
            payMethod: payForm.payMethod,
            payTime: payForm.payTime || undefined,
            remark: payForm.remark.trim() || undefined,
            categoryId: payForm.categoryId ?? undefined,
        })
        ElMessage.success('付款已完成')
        payVisible.value = false
        await Promise.all([loadPending(1), loadSummary()])
        if (activeTab.value === 'records') loadRecords(1)
    } catch (e: any) {
        ElMessage.error(e?.message || '付款失败')
    } finally {
        paySubmitting.value = false
    }
}

// 作废
const voidVisible = ref(false)
const voidSubmitting = ref(false)
const currentRecord = ref<PaymentRecordItem | null>(null)
const voidReason = ref('')

function openVoidDialog(row: PaymentRecordItem) {
    currentRecord.value = row
    voidReason.value = ''
    voidVisible.value = true
}

async function submitVoid() {
    if (!currentRecord.value) return
    if (!voidReason.value.trim()) {
        ElMessage.warning('请填写作废原因')
        return
    }
    // 点取消就不作废，直接返回
    const confirmed = await ElMessageBox.confirm('作废后该流程会回到待付款列表，确认作废这条付款记录吗？', '确认作废', {
        type: 'warning',
        confirmButtonText: '确认作废',
        cancelButtonText: '取消',
    }).then(() => true).catch(() => false)
    if (!confirmed) return
    voidSubmitting.value = true
    try {
        await voidPayment(currentRecord.value.id, voidReason.value.trim())
        ElMessage.success('已作废')
        voidVisible.value = false
        await Promise.all([loadRecords(), loadPending(1), loadSummary()])
    } catch (e: any) {
        ElMessage.error(e?.message || '作废失败')
    } finally {
        voidSubmitting.value = false
    }
}

// 导出
const exporting = ref(false)

async function handleExport() {
    exporting.value = true
    try {
        const blob = await exportPaymentRecords({
            keyword: recordQuery.keyword || undefined,
            payMethod: recordQuery.payMethod || undefined,
            status: recordQuery.status || undefined,
            startTime: recordDateRange.value?.[0],
            endTime: recordDateRange.value?.[1],
        })
        const url = URL.createObjectURL(new Blob([blob]))
        const a = document.createElement('a')
        a.href = url
        a.download = `付款记录_${todayText()}.xlsx`
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(url)
    } catch (e: any) {
        ElMessage.error('导出失败：' + (e?.message || '请稍后重试'))
    } finally {
        exporting.value = false
    }
}

// 通用
function onTabChange(name: string | number) {
    if (name === 'pending') loadPending()
    else loadRecords()
}

function openFlow(instanceId: number) {
    router.push({ path: '/home/flow-detail', query: { id: String(instanceId) } })
}

function money(value: number | null | undefined) {
    if (value === null || value === undefined) return '0.00'
    return Number(value).toFixed(2)
}

// 时间列只显示月-日 时:分，完整值放 title
function shortTime(value: string | null | undefined) {
    if (!value) return ''
    return value.length >= 16 ? value.slice(5, 16) : value
}

function pad(n: number) {
    return n < 10 ? '0' + n : String(n)
}

function nowText() {
    const d = new Date()
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function todayText() {
    const d = new Date()
    return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}`
}

onMounted(() => {
    loadSummary()
    loadPending(1)
    loadCategories()
})
</script>

<style scoped>
.finance-page {
    padding: 4px 2px 20px;
}
.page-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
}
.head-left {
    display: flex;
    align-items: center;
    gap: 10px;
}
.head-bar {
    width: 4px;
    height: 22px;
    border-radius: 2px;
    background: #409eff;
}
.page-head h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: #1f2d3d;
}
.page-head p {
    margin: 4px 0 0;
    font-size: 13px;
    color: #8a94a6;
}
.stat-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    margin-bottom: 16px;
}
.stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 10px;
    padding: 18px 20px;
}
.stat-icon {
    width: 46px;
    height: 46px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    color: #fff;
}
.stat-icon.pending {
    background: #5b8ff9;
}
.stat-icon.amount {
    background: #36cfc9;
}
.stat-icon.done {
    background: #f6a623;
}
.stat-icon.paid {
    background: #9254de;
}
.stat-num {
    font-size: 22px;
    font-weight: 600;
    color: #1f2d3d;
    line-height: 1.2;
}
.stat-label {
    margin-top: 4px;
    font-size: 13px;
    color: #8a94a6;
}
.main-card {
    border-radius: 10px;
}
.toolbar {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    margin-bottom: 12px;
}
.toolbar-tip {
    font-size: 12px;
    color: #a8b0bd;
}
.w-240 {
    width: 240px;
}
.w-220 {
    width: 220px;
}
.w-140 {
    width: 140px;
}
.w-120 {
    width: 120px;
}
.w-260 {
    width: 260px;
}
.w-full {
    width: 100%;
}
.amount-warn {
    color: #e6a23c;
}
.text-muted {
    color: #c0c4cc;
}
.pager {
    display: flex;
    justify-content: flex-end;
    margin-top: 14px;
}
.pay-flow {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    padding: 10px 12px;
    margin-bottom: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    font-size: 13px;
    color: #1f2d3d;
}
.pay-flow-label {
    color: #8a94a6;
}
.pay-flow-title {
    font-weight: 600;
}
.pay-flow-org {
    margin-left: auto;
    color: #8a94a6;
}
.form-tip {
    margin-top: 4px;
    font-size: 12px;
    line-height: 1.5;
    color: #a8b0bd;
}
.void-flow {
    margin-bottom: 14px;
    font-size: 13px;
    line-height: 1.6;
    color: #8a94a6;
}
</style>
