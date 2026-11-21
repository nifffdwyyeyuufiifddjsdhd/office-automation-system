<template>
    <div class="finance-page">
        <div class="page-head">
            <div class="head-left">
                <span class="head-bar"></span>
                <div>
                    <h2>财务统计</h2>
                    <p>按部门、时间段、费用类别统计已付款金额，可导出财务报表</p>
                </div>
            </div>
        </div>

        <!-- 筛选条件：时间段、部门、费用类别 -->
        <el-card shadow="never" class="filter-card">
            <div class="filter-bar">
                <el-date-picker
                    v-model="dateRange"
                    type="daterange"
                    value-format="YYYY-MM-DD"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    class="w-260"
                    unlink-panels
                />
                <el-select v-model="query.orgId" placeholder="全部部门" clearable class="w-180">
                    <el-option v-for="org in orgOptions" :key="org.id" :label="org.label" :value="org.id" />
                </el-select>
                <el-select v-model="query.categoryName" placeholder="全部费用类别" clearable class="w-180">
                    <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.categoryName" />
                </el-select>
                <el-button type="primary" @click="reloadAll">查询</el-button>
                <el-button @click="resetQuery">重置</el-button>
                <div class="filter-right">
                    <el-radio-group v-model="granularity" size="small" @change="loadTrend">
                        <el-radio-button label="month">按月</el-radio-button>
                        <el-radio-button label="day">按天</el-radio-button>
                    </el-radio-group>
                    <el-button v-if="canExport" :loading="exporting" @click="handleExport">导出报表</el-button>
                </div>
            </div>
        </el-card>

        <!-- 五张指标卡，待付款单也列出来，光看已付会误判 -->
        <div class="stat-grid">
            <div class="stat-card">
                <div class="stat-icon amount"><el-icon><Money /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-num">{{ money(overview.totalAmount) }}</div>
                    <div class="stat-label">付款总额（元）</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon count"><el-icon><Document /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-num">{{ overview.recordCount }}</div>
                    <div class="stat-label">付款笔数</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon avg"><el-icon><Coin /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-num">{{ money(overview.avgAmount) }}</div>
                    <div class="stat-label">平均单笔（元）</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon growth"><el-icon><TrendCharts /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-num" :class="growthClass">{{ growthText }}</div>
                    <div class="stat-label">较上一周期</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon pending"><el-icon><Clock /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-num">{{ money(overview.pendingAmount) }}</div>
                    <div class="stat-label">
                        待付款（{{ overview.pendingCount }} 笔）
                        <span v-if="overview.pendingUnknownCount > 0" class="stat-flag">{{ overview.pendingUnknownCount }} 笔金额待补</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 趋势图 + 类别占比 -->
        <div class="chart-grid">
            <el-card shadow="never" class="main-card">
                <template #header>
                    <span class="card-title">金额趋势（{{ granularity === 'month' ? '按月' : '按天' }}）</span>
                </template>
                <BaseChart :option="trendOption" height="300px" />
            </el-card>
            <el-card shadow="never" class="main-card">
                <template #header>
                    <span class="card-title">费用类别占比</span>
                </template>
                <el-table v-loading="loading" :data="categoryRows" size="small" empty-text="暂无数据" max-height="300">
                    <el-table-column prop="categoryName" label="费用类别" min-width="100" />
                    <el-table-column label="金额（元）" min-width="110" align="right">
                        <template #default="{ row }">{{ money(row.amount) }}</template>
                    </el-table-column>
                    <el-table-column prop="count" label="笔数" width="70" align="right" />
                    <el-table-column label="占比" min-width="140">
                        <template #default="{ row }">
                            <el-progress :percentage="Number(row.ratio)" :stroke-width="10" :format="() => row.ratio + '%'" />
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>
        </div>

        <!-- 部门汇总 -->
        <el-card shadow="never" class="main-card">
            <template #header>
                <span class="card-title">部门费用汇总</span>
            </template>
            <el-table v-loading="loading" :data="departmentRows" stripe empty-text="暂无数据">
                <el-table-column prop="orgName" label="部门" min-width="160" />
                <el-table-column label="付款金额（元）" min-width="140" align="right">
                    <template #default="{ row }">{{ money(row.amount) }}</template>
                </el-table-column>
                <el-table-column prop="count" label="笔数" width="90" align="right" />
                <el-table-column label="占比" min-width="180">
                    <template #default="{ row }">
                        <el-progress :percentage="Number(row.ratio)" :stroke-width="12" :format="() => row.ratio + '%'" />
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 付款明细 -->
        <el-card shadow="never" class="main-card">
            <template #header>
                <span class="card-title">付款明细</span>
            </template>
            <el-table v-loading="detailLoading" :data="detailRows" stripe empty-text="暂无付款记录">
                <el-table-column prop="payNo" label="付款单号" width="170" />
                <el-table-column prop="title" label="流程标题" min-width="200" show-overflow-tooltip />
                <el-table-column prop="applicantName" label="申请人" width="100" />
                <el-table-column prop="orgName" label="部门" width="130" show-overflow-tooltip />
                <el-table-column prop="categoryName" label="费用类别" width="100" />
                <el-table-column label="金额（元）" width="120" align="right">
                    <template #default="{ row }">{{ money(row.amount) }}</template>
                </el-table-column>
                <el-table-column prop="payMethodText" label="付款方式" width="100" />
                <el-table-column prop="payTime" label="付款时间" width="170" />
            </el-table>
            <div class="pager">
                <el-pagination
                    v-model:current-page="detailQuery.pageNum"
                    v-model:page-size="detailQuery.pageSize"
                    :page-sizes="[10, 20, 50]"
                    :total="detailTotal"
                    layout="total, sizes, prev, pager, next"
                    @size-change="() => loadDetail(1)"
                    @current-change="() => loadDetail()"
                />
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import BaseChart from '@/views/report/BaseChart.vue'
import { hasPerm } from '@/utils/permission'
import { orgTreeEnabled, type OrgNode } from '@/api/user'
import {
    exportStatReport,
    getExpenseCategories,
    getStatByCategory,
    getStatByDepartment,
    getStatDetail,
    getStatOverview,
    getStatTrend,
    type CategoryStatRow,
    type DepartmentStatRow,
    type ExpenseCategoryItem,
    type StatDetailItem,
    type StatOverview,
    type StatTrend,
} from '@/api/finance'

// 导出要 finance:export，统计接口要 finance:stat
const canExport = computed(() => hasPerm('finance:export'))

const dateRange = ref<[string, string] | null>(null)
const query = reactive<{ orgId: number | null; categoryName: string }>({ orgId: null, categoryName: '' })
const granularity = ref<'month' | 'day'>('month')

const loading = ref(false)
const detailLoading = ref(false)
const exporting = ref(false)

const overview = reactive<StatOverview>({
    totalAmount: 0, recordCount: 0, avgAmount: 0, maxAmount: 0, categoryCount: 0, orgCount: 0,
    topCategoryName: '', topCategoryAmount: 0, prevAmount: 0, growthRate: null,
    pendingAmount: 0, pendingCount: 0, pendingUnknownCount: 0, startDate: '', endDate: '',
})
const trend = ref<StatTrend>({ granularity: 'month', labels: [], amounts: [], counts: [] })
const categoryRows = ref<CategoryStatRow[]>([])
const departmentRows = ref<DepartmentStatRow[]>([])
const categories = ref<ExpenseCategoryItem[]>([])
const orgOptions = ref<OrgNode[]>([])
const detailRows = ref<StatDetailItem[]>([])
const detailTotal = ref(0)
const detailQuery = reactive({ pageNum: 1, pageSize: 10 })

// 后端接口统一的筛选参数
function baseParams() {
    return {
        startDate: dateRange.value?.[0],
        endDate: dateRange.value?.[1],
        orgId: query.orgId || undefined,
        categoryName: query.categoryName || undefined,
    }
}

async function loadOverview() {
    try {
        Object.assign(overview, await getStatOverview(baseParams()))
    } catch (e: any) {
        ElMessage.error('统计概览加载失败：' + (e?.message || '请稍后重试'))
    }
}

async function loadTrend() {
    try {
        trend.value = await getStatTrend({ ...baseParams(), granularity: granularity.value })
    } catch (e: any) {
        ElMessage.error('趋势数据加载失败：' + (e?.message || '请稍后重试'))
    }
}

async function loadAggregates() {
    loading.value = true
    try {
        const [catList, depList] = await Promise.all([
            getStatByCategory(baseParams()),
            getStatByDepartment({ startDate: dateRange.value?.[0], endDate: dateRange.value?.[1], categoryName: query.categoryName || undefined }),
        ])
        categoryRows.value = catList
        departmentRows.value = depList
    } catch (e: any) {
        ElMessage.error('汇总数据加载失败：' + (e?.message || '请稍后重试'))
    } finally {
        loading.value = false
    }
}

async function loadDetail(resetPage?: number) {
    if (resetPage) detailQuery.pageNum = resetPage
    detailLoading.value = true
    try {
        const data = await getStatDetail({ ...baseParams(), pageNum: detailQuery.pageNum, pageSize: detailQuery.pageSize })
        detailRows.value = data.records
        detailTotal.value = data.total
    } catch (e: any) {
        ElMessage.error('明细加载失败：' + (e?.message || '请稍后重试'))
    } finally {
        detailLoading.value = false
    }
}

function reloadAll() {
    loadOverview()
    loadTrend()
    loadAggregates()
    loadDetail(1)
}

function resetQuery() {
    dateRange.value = null
    query.orgId = null
    query.categoryName = ''
    granularity.value = 'month'
    reloadAll()
}

async function loadOptions() {
    try {
        const [catList, tree] = await Promise.all([getExpenseCategories(), orgTreeEnabled()])
        categories.value = catList
        orgOptions.value = flatten(tree)
    } catch {
        // 下拉数据失败不影响主统计，忽略即可
    }
}

// 部门树拍平成选项，子部门名字前加缩进
function flatten(nodes: OrgNode[], depth = 0): OrgNode[] {
    const out: OrgNode[] = []
    for (const node of nodes || []) {
        out.push({ ...node, label: (depth > 0 ? '　'.repeat(depth) : '') + node.label })
        if (node.children && node.children.length) out.push(...flatten(node.children, depth + 1))
    }
    return out
}

const growthText = computed(() => {
    if (overview.growthRate === null || overview.growthRate === undefined) return '—'
    const value = Number(overview.growthRate)
    return (value > 0 ? '+' : '') + value.toFixed(1) + '%'
})

const growthClass = computed(() => {
    if (overview.growthRate === null || overview.growthRate === undefined) return ''
    return Number(overview.growthRate) >= 0 ? 'up' : 'down'
})

const trendOption = computed(() => ({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 24, top: 30, bottom: 46 },
    xAxis: {
        type: 'category',
        data: trend.value.labels,
        axisLabel: { interval: 0, rotate: trend.value.labels.length > 10 ? 40 : 0 },
    },
    yAxis: { type: 'value', name: '元' },
    series: [
        {
            name: '付款金额',
            type: 'line',
            smooth: true,
            showSymbol: true,
            areaStyle: { opacity: 0.15 },
            itemStyle: { color: '#2f7cf6' },
            data: trend.value.amounts,
        },
    ],
}))

async function handleExport() {
    exporting.value = true
    try {
        const blob = await exportStatReport(baseParams())
        const url = URL.createObjectURL(new Blob([blob]))
        const a = document.createElement('a')
        a.href = url
        a.download = '财务报表_' + todayText() + '.xlsx'
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

function money(value: number | null | undefined) {
    if (value === null || value === undefined) return '0.00'
    return Number(value).toFixed(2)
}

function pad(n: number) {
    return n < 10 ? '0' + n : String(n)
}

function todayText() {
    const d = new Date()
    return '' + d.getFullYear() + pad(d.getMonth() + 1) + pad(d.getDate())
}

onMounted(() => {
    loadOptions()
    reloadAll()
})
</script>

<style scoped>
.finance-page {
    padding: 4px 2px 24px;
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
    gap: 12px;
}

.head-bar {
    width: 4px;
    height: 38px;
    border-radius: 2px;
    background: linear-gradient(180deg, #2f7cf6, #6aa9ff);
}

.page-head h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
}

.page-head p {
    margin: 4px 0 0;
    font-size: 13px;
    color: #8c939d;
}

.filter-card {
    margin-bottom: 16px;
    border-radius: 10px;
}

.filter-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.filter-right {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 12px;
}

.w-260 {
    width: 260px;
}

.w-180 {
    width: 180px;
}

.stat-grid {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 16px;
    margin-bottom: 16px;
}

.stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 20px;
    background: #fff;
    border-radius: 10px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #fff;
}

.stat-icon.amount { background: linear-gradient(135deg, #2f7cf6, #6aa9ff); }
.stat-icon.count { background: linear-gradient(135deg, #13c2c2, #5cdbd3); }
.stat-icon.avg { background: linear-gradient(135deg, #fa8c16, #ffc069); }
.stat-icon.growth { background: linear-gradient(135deg, #722ed1, #b37feb); }
.stat-icon.pending { background: linear-gradient(135deg, #e6483d, #ff9c96); }

.stat-num {
    font-size: 22px;
    font-weight: 600;
    line-height: 1.2;
}

.stat-num.up { color: #f5222d; }
.stat-num.down { color: #52c41a; }

.stat-label {
    margin-top: 4px;
    font-size: 12px;
    color: #8c939d;
}

/* 金额识别不出来的待付款单据，单独标出来 */
.stat-flag {
    display: inline-block;
    margin-left: 6px;
    padding: 0 6px;
    border-radius: 8px;
    background: #fff1f0;
    border: 1px solid #ffccc7;
    color: #cf1322;
    font-size: 11px;
    line-height: 16px;
}

.chart-grid {
    display: grid;
    grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
    gap: 16px;
    margin-bottom: 16px;
}

.main-card {
    border-radius: 10px;
    margin-bottom: 16px;
}

.card-title {
    font-size: 15px;
    font-weight: 600;
}

.pager {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
}

@media (max-width: 1280px) {
    .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .chart-grid { grid-template-columns: minmax(0, 1fr); }
}
</style>
