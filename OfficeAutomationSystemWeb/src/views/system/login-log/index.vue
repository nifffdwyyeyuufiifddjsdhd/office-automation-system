<template>
    <div class="log-page">
        <div class="page-head">
            <div class="head-left">
                <span class="head-bar"></span>
                <div>
                    <h2>登录日志统计</h2>
                    <p>查看员工登录情况与失败原因，用于发现异常账号与密码试用行为</p>
                </div>
            </div>
            <el-button @click="refreshAll">刷新</el-button>
        </div>

        <div class="stat-grid">
            <el-card shadow="never" class="stat-card">
                <div class="stat-icon total"><el-icon><Connection /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-value">{{ overview.todayTotal }}</div>
                    <div class="stat-label">今日登录次数</div>
                </div>
            </el-card>
            <el-card shadow="never" class="stat-card">
                <div class="stat-icon success"><el-icon><CircleCheck /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-value">{{ overview.todaySuccess }}</div>
                    <div class="stat-label">今日成功</div>
                </div>
            </el-card>
            <el-card shadow="never" class="stat-card">
                <div class="stat-icon fail"><el-icon><CircleClose /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-value">{{ overview.todayFail }}</div>
                    <div class="stat-label">今日失败</div>
                </div>
            </el-card>
            <el-card shadow="never" class="stat-card">
                <div class="stat-icon users"><el-icon><User /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-value">{{ overview.todayUsers }}</div>
                    <div class="stat-label">今日登录人数</div>
                </div>
            </el-card>
        </div>

        <div class="chart-grid">
            <el-card shadow="never">
                <template #header><span class="card-title">近 7 天登录趋势</span></template>
                <div v-if="trendTotal === 0" class="empty-line">近 7 天暂无登录记录</div>
                <div v-else class="trend">
                    <div v-for="item in overview.trend" :key="item.date" class="trend-col">
                        <div class="trend-bars">
                            <div class="bar success" :style="{ height: barHeight(item.successCount) }" :title="'成功 ' + item.successCount"></div>
                            <div class="bar fail" :style="{ height: barHeight(item.failCount) }" :title="'失败 ' + item.failCount"></div>
                        </div>
                        <div class="trend-label">{{ item.date.slice(5) }}</div>
                    </div>
                </div>
                <div class="legend">
                    <span><i class="dot success"></i>成功</span>
                    <span><i class="dot fail"></i>失败</span>
                </div>
            </el-card>
            <el-card shadow="never">
                <template #header><span class="card-title">失败原因分布（前 5）</span></template>
                <div v-if="overview.failReasons.length === 0" class="empty-line">暂无失败记录</div>
                <div v-for="item in overview.failReasons" :key="item.reason" class="reason-row">
                    <span class="reason-name" :title="item.reason">{{ item.reason }}</span>
                    <el-progress :percentage="reasonPercent(item.amount)" :stroke-width="8" :show-text="false" status="exception" />
                    <span class="reason-amount">{{ item.amount }}</span>
                </div>
            </el-card>
            <el-card shadow="never">
                <template #header><span class="card-title">近 30 天高频账号</span></template>
                <div v-if="overview.topUsers.length === 0" class="empty-line">暂无数据</div>
                <div v-for="(item, index) in overview.topUsers" :key="item.username" class="top-row">
                    <span class="top-rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
                    <span class="top-name">{{ item.username }}</span>
                    <span class="top-amount">{{ item.amount }} 次</span>
                    <span class="top-time">{{ item.lastTime ? item.lastTime.slice(5, 16) : "-" }}</span>
                </div>
            </el-card>
        </div>

        <el-card shadow="never" class="filter-card">
            <div class="filters">
                <el-input v-model="query.keyword" placeholder="账号 / IP / 登录地点" clearable style="width: 200px" @keyup.enter="search" />
                <el-select v-model="query.status" placeholder="全部结果" clearable style="width: 120px">
                    <el-option label="成功" :value="1" />
                    <el-option label="失败" :value="0" />
                </el-select>
                <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
                <el-button type="primary" @click="search">查询</el-button>
                <el-button @click="resetQuery">重置</el-button>
                <span class="hint">共 {{ total }} 条记录</span>
            </div>
        </el-card>

        <el-card shadow="never" class="main-card">
            <el-table v-loading="loading" :data="rows" stripe empty-text="暂无登录日志">
                <el-table-column prop="username" label="账号" width="110" />
                <el-table-column label="登录时间" width="150">
                    <template #default="{ row }">{{ formatTime(row.loginTime) }}</template>
                </el-table-column>
                <el-table-column prop="ipAddress" label="IP 地址" width="130" />
                <el-table-column label="登录地点" width="130" show-overflow-tooltip>
                    <template #default="{ row }">{{ text(row.location) }}</template>
                </el-table-column>
                <el-table-column label="设备" width="100" show-overflow-tooltip>
                    <template #default="{ row }">{{ text(row.device) }}</template>
                </el-table-column>
                <el-table-column label="浏览器" width="110" show-overflow-tooltip>
                    <template #default="{ row }">{{ text(row.browser) }}</template>
                </el-table-column>
                <el-table-column label="系统" width="100" show-overflow-tooltip>
                    <template #default="{ row }">{{ text(row.os) }}</template>
                </el-table-column>
                <el-table-column label="结果" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="light">
                            {{ row.status === 1 ? "成功" : "失败" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="失败原因" min-width="140" show-overflow-tooltip>
                    <template #default="{ row }">{{ text(row.failReason) }}</template>
                </el-table-column>
                <el-table-column label="退出时间" width="150">
                    <template #default="{ row }">
                        <span v-if="row.logoutTime">{{ formatTime(row.logoutTime) }}</span>
                        <span v-else class="muted">-</span>
                    </template>
                </el-table-column>
            </el-table>
            <div class="pager">
                <el-pagination
                    v-model:current-page="query.pageNum"
                    v-model:page-size="query.pageSize"
                    :total="total"
                    :page-sizes="[10, 20, 50]"
                    layout="total, sizes, prev, pager, next"
                    @current-change="loadRows"
                    @size-change="search"
                />
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue"
import { Connection, CircleCheck, CircleClose, User } from "@element-plus/icons-vue"
import { getLoginOverview, pageLoginLogs, type LoginLogItem, type LoginOverview } from "@/api/loginLog"

const loading = ref(false)
const rows = ref<LoginLogItem[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({ pageNum: 1, pageSize: 20, keyword: "", status: undefined as number | undefined })

const overview = reactive<LoginOverview>({
    todayTotal: 0, todaySuccess: 0, todayFail: 0, todayUsers: 0,
    trend: [], failReasons: [], topUsers: [],
})

// 趋势图的纵向最大高度按 100% 归一，柱子用百分比高度
const trendMax = computed(() => {
    let max = 0
    for (const item of overview.trend) {
        max = Math.max(max, item.successCount, item.failCount)
    }
    return max
})

const trendTotal = computed(() => {
    return overview.trend.reduce((sum, item) => sum + item.successCount + item.failCount, 0)
})

function barHeight(value: number) {
    if (!trendMax.value || !value) return "2px"
    const percent = Math.round((value / trendMax.value) * 100)
    return Math.max(percent, 4) + "%"
}

function reasonPercent(amount: number) {
    const totalFail = overview.failReasons.reduce((sum, item) => sum + item.amount, 0)
    if (!totalFail) return 0
    return Math.round((amount / totalFail) * 100)
}

function formatTime(value: string | null | undefined) {
    if (!value) return "-"
    return value.length >= 19 ? value.slice(0, 19).replace("T", " ") : value
}

// 采不到设备或地点时显示占位符，免得整列空白
function text(value: string | null | undefined) {
    const content = (value ?? "").trim()
    return content === "" ? "-" : content
}

async function loadRows() {
    loading.value = true
    try {
        const data = await pageLoginLogs({
            pageNum: query.pageNum,
            pageSize: query.pageSize,
            keyword: query.keyword || undefined,
            status: query.status,
            startDate: dateRange.value ? dateRange.value[0] : undefined,
            endDate: dateRange.value ? dateRange.value[1] : undefined,
        })
        rows.value = data.records
        total.value = data.total
    } finally {
        loading.value = false
    }
}

async function loadOverview() {
    const data = await getLoginOverview()
    Object.assign(overview, data)
}

function refreshAll() {
    loadOverview()
    loadRows()
}

function search() {
    query.pageNum = 1
    loadRows()
}

function resetQuery() {
    query.keyword = ""
    query.status = undefined
    dateRange.value = null
    search()
}

onMounted(() => {
    refreshAll()
})
</script>

<style scoped>
.log-page { padding: 16px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.head-left { display: flex; align-items: center; gap: 10px; }
.head-bar { width: 4px; height: 34px; background: #409eff; border-radius: 2px; }
.page-head h2 { margin: 0; font-size: 18px; }
.page-head p { margin: 2px 0 0; font-size: 12px; color: #909399; }
.stat-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 12px; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; gap: 12px; padding: 16px; }
.stat-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; }
.stat-icon.total { background: linear-gradient(135deg, #409eff, #79bbff); }
.stat-icon.success { background: linear-gradient(135deg, #67c23a, #95d475); }
.stat-icon.fail { background: linear-gradient(135deg, #f56c6c, #f89898); }
.stat-icon.users { background: linear-gradient(135deg, #a855f7, #c084fc); }
.stat-value { font-size: 22px; font-weight: 600; line-height: 1.2; }
.stat-label { font-size: 12px; color: #909399; }
.chart-grid { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 12px; margin-bottom: 12px; }
.card-title { font-size: 14px; font-weight: 600; }
.trend { display: flex; align-items: flex-end; justify-content: space-between; height: 160px; padding: 0 4px; }
.trend-col { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; }
.trend-bars { flex: 1; display: flex; align-items: flex-end; gap: 4px; }
.bar { width: 12px; border-radius: 3px 3px 0 0; min-height: 2px; transition: height .3s; }
.bar.success { background: #67c23a; }
.bar.fail { background: #f56c6c; }
.trend-label { font-size: 12px; color: #909399; margin-top: 6px; }
.legend { display: flex; gap: 14px; justify-content: center; margin-top: 6px; font-size: 12px; color: #909399; }
.legend .dot { display: inline-block; width: 8px; height: 8px; border-radius: 2px; margin-right: 4px; }
.legend .dot.success { background: #67c23a; }
.legend .dot.fail { background: #f56c6c; }
.reason-row { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.reason-name { width: 96px; font-size: 12px; color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.reason-row :deep(.el-progress) { flex: 1; }
.reason-amount { width: 34px; text-align: right; font-size: 12px; color: #909399; }
.top-row { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; font-size: 13px; }
.top-rank { width: 20px; height: 20px; line-height: 20px; text-align: center; border-radius: 4px; background: #f2f3f5; color: #909399; font-size: 12px; }
.top-rank.rank-1 { background: #f56c6c; color: #fff; }
.top-rank.rank-2 { background: #e6a23c; color: #fff; }
.top-rank.rank-3 { background: #409eff; color: #fff; }
.top-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.top-amount { color: #303133; }
.top-time { width: 78px; text-align: right; font-size: 12px; color: #c0c4cc; }
.empty-line { text-align: center; color: #909399; font-size: 13px; padding: 24px 0; }
.filter-card { margin-bottom: 12px; }
.filters { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.hint { margin-left: auto; font-size: 12px; color: #909399; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.muted { color: #c0c4cc; }
</style>