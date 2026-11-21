<template>
  <div class="report-page">
    <!-- 筛选栏 -->
    <div class="filter-card">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        size="default"
        style="width: 260px"
      />
      <el-select
        v-if="meta.canFilterDept"
        v-model="filterDept"
        placeholder="全部部门"
        clearable
        filterable
        style="width: 150px"
      >
        <el-option v-for="d in meta.depts" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <el-select
        v-if="meta.canFilterUser"
        v-model="filterUser"
        placeholder="全部人员"
        clearable
        filterable
        style="width: 150px"
      >
        <el-option v-for="u in meta.users" :key="u.id" :label="u.name" :value="u.id" />
      </el-select>
      <el-select
        v-model="filterProject"
        placeholder="全部项目"
        clearable
        filterable
        style="width: 160px"
      >
        <el-option v-for="p in meta.projects" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
      <span class="scope-tag" v-if="data">
        当前范围：<b>{{ data.scope_name }}</b>（{{ data.start_date }} ~ {{ data.end_date }}）
      </span>
    </div>

    <div v-loading="loading" class="report-body">
      <!-- 任务统计 -->
      <section class="panel">
        <div class="panel-head">
          <span class="dot dot-blue"></span>
          <span class="panel-title">任务统计</span>
          <span class="panel-sub">按文档 17.2 统计任务完成率、逾期率、工时偏差（含已归档）</span>
          <el-button class="export-btn" size="small" plain :loading="exporting==='task'"
            @click="doExport('task','任务统计')">导出 Excel</el-button>
        </div>
        <div class="kpi-row">
          <div class="kpi" v-for="k in taskKpis" :key="k.label">
            <div class="kpi-num" :class="k.color">{{ k.value }}</div>
            <div class="kpi-label">{{ k.label }}</div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-card">
            <h4>任务状态分布</h4>
            <BaseChart :option="taskPie" v-if="(task.status_dist||[]).length" />
            <div class="empty" v-else>暂无数据</div>
          </div>
          <div class="chart-card">
            <h4>人员完成排行 Top10</h4>
            <BaseChart :option="taskUserBar" v-if="(task.user_rank||[]).length" />
            <div class="empty" v-else>暂无数据</div>
          </div>
          <div class="chart-card chart-wide">
            <h4>任务创建 / 完成趋势</h4>
            <BaseChart :option="taskTrend" height="280px" v-if="(task.trend||[]).length" />
            <div class="empty" v-else>暂无数据</div>
          </div>
          <div class="chart-card chart-wide" v-if="task.scope!=='self' && (task.dept_rank||[]).length">
            <h4>各部门任务完成情况</h4>
            <BaseChart :option="taskDeptBar" height="260px" />
          </div>
        </div>
      </section>

      <!-- 出勤统计 -->
      <section class="panel">
        <div class="panel-head">
          <span class="dot dot-green"></span>
          <span class="panel-title">出勤统计</span>
          <span class="panel-sub">出勤率、迟到/早退/缺勤；出差、外出、请假单独统计不计缺勤；未签退异常单列</span>
          <el-button class="export-btn" size="small" plain :loading="exporting==='attendance'"
            @click="doExport('attendance','出勤统计')">导出 Excel</el-button>
        </div>
        <div class="kpi-row">
          <div class="kpi" v-for="k in attKpis" :key="k.label">
            <div class="kpi-num" :class="k.color">{{ k.value }}</div>
            <div class="kpi-label">{{ k.label }}</div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-card">
            <h4>出勤状态分布</h4>
            <BaseChart :option="attPie" v-if="(att.status_dist||[]).length" />
            <div class="empty" v-else>暂无数据</div>
          </div>
          <div class="chart-card" v-if="att.scope!=='self'">
            <h4>各部门出勤率（%）</h4>
            <BaseChart :option="attDeptBar" v-if="(att.dept_rank||[]).length" />
            <div class="empty" v-else>暂无数据</div>
          </div>
        </div>
      </section>

      <!-- 日志统计 -->
      <section class="panel">
        <div class="panel-head">
          <span class="dot dot-orange"></span>
          <span class="panel-title">工作日志统计</span>
          <span class="panel-sub">日志完成率、提交时段分布、字数、部门活跃度与评论活跃度</span>
          <el-button class="export-btn" size="small" plain :loading="exporting==='worklog'"
            @click="doExport('worklog','工作日志统计')">导出 Excel</el-button>
        </div>
        <div class="kpi-row">
          <div class="kpi" v-for="k in logKpis" :key="k.label">
            <div class="kpi-num" :class="k.color">{{ k.value }}</div>
            <div class="kpi-label">{{ k.label }}</div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-card">
            <h4>提交时段分布</h4>
            <BaseChart :option="logPie" v-if="hasLogTime" />
            <div class="empty" v-else>暂无数据</div>
          </div>
          <div class="chart-card" v-if="log.scope!=='self'">
            <h4>各部门日志撰写量</h4>
            <BaseChart :option="logDeptBar" v-if="(log.dept_rank||[]).length" />
            <div class="empty" v-else>暂无数据</div>
          </div>
          <div class="chart-card">
            <h4>评论活跃用户（发表评论数）</h4>
            <BaseChart :option="commentUserBar" v-if="(log.comment_user_rank||[]).length" />
            <div class="empty" v-else>暂无数据</div>
          </div>
          <div class="chart-card">
            <h4>评论最多的日志</h4>
            <BaseChart :option="commentLogBar" v-if="(log.comment_log_rank||[]).length" />
            <div class="empty" v-else>暂无数据</div>
          </div>
        </div>
      </section>

      <!-- 流程审批统计 -->
      <section class="panel">
        <div class="panel-head">
          <span class="dot dot-purple"></span>
          <span class="panel-title">流程审批效率</span>
          <span class="panel-sub">流程通过率、平均审批时长、各类型审批情况</span>
          <el-button class="export-btn" size="small" plain :loading="exporting==='flow'"
            @click="doExport('flow','流程审批统计')">导出 Excel</el-button>
        </div>
        <div class="kpi-row">
          <div class="kpi" v-for="k in flowKpis" :key="k.label">
            <div class="kpi-num" :class="k.color">{{ k.value }}</div>
            <div class="kpi-label">{{ k.label }}</div>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-card">
            <h4>流程状态分布</h4>
            <BaseChart :option="flowPie" v-if="(flow.status_dist||[]).length" />
            <div class="empty" v-else>暂无数据</div>
          </div>
          <div class="chart-card">
            <h4>各流程类型审批情况</h4>
            <BaseChart :option="flowTypeBar" v-if="(flow.type_dist||[]).length" />
            <div class="empty" v-else>暂无数据</div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import BaseChart from './BaseChart.vue'
import { getReportOverview, getReportMeta, exportReport, type ReportMeta } from '@/api/report'

// 可见范围后端按角色判定，页面只看 meta 决定筛选器

const PALETTE = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#b37feb', '#36cfc9', '#909399']

const TASK_COLOR: Record<string, string> = {
  待分配: '#909399', 待接受: '#e6a23c', 进行中: '#409eff', 待验收: '#b37feb',
  已完成: '#67c23a', 已撤销: '#c0c4cc', 已拒绝: '#f56c6c'
}
const ATT_COLOR: Record<string, string> = {
  正常: '#67c23a', 迟到: '#e6a23c', 早退: '#f0a020', 缺勤: '#f56c6c',
  外出: '#409eff', 出差: '#36cfc9', 请假: '#909399', 严重迟到: '#f56c6c',
  未签退异常: '#b37feb', 严重早退: '#f56c6c'
}
const FLOW_COLOR: Record<string, string> = {
  草稿: '#909399', 审批中: '#409eff', 已通过: '#67c23a', 已驳回: '#f56c6c', 已撤销: '#b37feb'
}
const LOG_COLOR: Record<string, string> = { 上午: '#409eff', 下午: '#67c23a', 晚上: '#b37feb' }

const loading = ref(false)
const data = ref<any>(null)
const meta = ref<ReportMeta>({ depts: [], projects: [], users: [], canFilterUser: false, canFilterDept: false })

const dateRange = ref<[string, string]>(['', ''])
const filterDept = ref<number | null>(null)
const filterUser = ref<number | null>(null)
const filterProject = ref<number | null>(null)
const exporting = ref('')

const fmt = (d: Date) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const isSelf = computed(() => data.value?.scope === 'self')

// 拿不到元数据多半是没权限，要提示，不然只剩空下拉
const metaFailed = ref(false)

async function loadMeta() {
  try {
    meta.value = await getReportMeta()
  } catch (e: any) {
    metaFailed.value = true
    ElMessage.warning(e?.message || '统计报表加载失败，可能没有查看权限')
  }
}

async function loadData() {
  if (!dateRange.value || !dateRange.value[0]) {
    ElMessage.warning('请选择统计时间范围')
    return
  }
  loading.value = true
  try {
    const params: any = {
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    }
    if (filterDept.value) params.deptId = filterDept.value
    if (filterUser.value) params.userId = filterUser.value
    if (filterProject.value) params.projectId = filterProject.value
    data.value = await getReportOverview(params)
  } catch (e) {
    ElMessage.error('统计数据加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterDept.value = null
  filterUser.value = null
  filterProject.value = null
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 89)
  dateRange.value = [fmt(start), fmt(end)]
  loadData()
}

function buildParams() {
  const params: any = {
    startDate: dateRange.value[0],
    endDate: dateRange.value[1]
  }
  if (filterDept.value) params.deptId = filterDept.value
  if (filterUser.value) params.userId = filterUser.value
  if (filterProject.value) params.projectId = filterProject.value
  return params
}

async function doExport(section: string, label: string) {
  if (!dateRange.value || !dateRange.value[0]) {
    ElMessage.warning('请选择统计时间范围')
    return
  }
  exporting.value = section
  try {
    const blob = await exportReport(section, buildParams())
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${label}_${dateRange.value[0]}_${dateRange.value[1]}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = ''
  }
}

const task = computed(() => data.value?.task || {})
const att = computed(() => data.value?.attendance || {})
const log = computed(() => data.value?.worklog || {})
const flow = computed(() => data.value?.flow || {})

const withUnit = (v: any, unit: string) => (v ?? 0) + unit
const taskKpis = computed(() => {
  const s = task.value.summary || {}
  return [
    { label: '任务总数', value: s.total ?? 0, color: 'c-blue' },
    { label: '已完成', value: s.finished ?? 0, color: 'c-green' },
    { label: '完成率', value: withUnit(s.completion_rate, '%'), color: 'c-blue' },
    { label: '逾期数', value: s.overdue ?? 0, color: 'c-red' },
    { label: '逾期率', value: withUnit(s.overdue_rate, '%'), color: 'c-orange' },
    { label: '平均工时偏差', value: withUnit(s.avg_hour_diff, 'h'), color: 'c-purple' }
  ]
})
const attKpis = computed(() => {
  const s = att.value.summary || {}
  return [
    { label: '应出勤', value: s.should ?? 0, color: 'c-blue' },
    { label: '实际出勤', value: s.present ?? 0, color: 'c-green' },
    { label: '出勤率', value: withUnit(s.attendance_rate, '%'), color: 'c-green' },
    { label: '迟到', value: s.late ?? 0, color: 'c-orange' },
    { label: '早退', value: s.early ?? 0, color: 'c-orange' },
    { label: '缺勤', value: s.absent ?? 0, color: 'c-red' },
    { label: '未签退异常', value: s.abnormal_signout ?? 0, color: 'c-purple' }
  ]
})
const logKpis = computed(() => {
  const s = log.value.summary || {}
  return [
    { label: '日志总数', value: s.total ?? 0, color: 'c-blue' },
    { label: '撰写人数', value: s.active_users ?? 0, color: 'c-green' },
    { label: '撰写人天', value: s.writing_days ?? 0, color: 'c-purple' },
    { label: '日志完成率', value: withUnit(s.completion_rate, '%'), color: 'c-blue' },
    { label: '总字数', value: s.total_words ?? 0, color: 'c-dark' },
    { label: '平均每篇', value: withUnit(s.avg_words, ' 字'), color: 'c-orange' },
    { label: '评论数', value: s.comment_count ?? 0, color: 'c-blue' }
  ]
})
const flowKpis = computed(() => {
  const s = flow.value.summary || {}
  const mins = s.avg_approve_minutes ?? 0
  const avgText = mins >= 60 ? (mins / 60).toFixed(1) + ' 小时' : mins + ' 分钟'
  return [
    { label: '流程总数', value: s.total ?? 0, color: 'c-blue' },
    { label: '审批中', value: s.approving ?? 0, color: 'c-orange' },
    { label: '已通过', value: s.approved ?? 0, color: 'c-green' },
    { label: '已驳回', value: s.rejected ?? 0, color: 'c-red' },
    { label: '通过率', value: withUnit(s.approval_rate, '%'), color: 'c-green' },
    { label: '平均审批时长', value: avgText, color: 'c-purple' }
  ]
})

// ECharts option 构造
function pieOption(list: any[], colorMap: Record<string, string>) {
  return {
    color: (list || []).map((x) => colorMap[x.name] || '#409eff'),
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll', textStyle: { color: '#606266' } },
    series: [{
      type: 'pie',
      radius: ['42%', '66%'],
      center: ['50%', '44%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: list || []
    }]
  }
}

function hBarOption(names: string[], series: { name: string; data: number[]; color: string }[]): any {
  return {
    color: series.map((s) => s.color),
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, textStyle: { color: '#606266' } },
    grid: { left: 8, right: 36, top: 34, bottom: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    yAxis: {
      type: 'category',
      data: names,
      inverse: true,
      axisLabel: { color: '#606266' },
      axisLine: { lineStyle: { color: '#dcdfe6' } }
    },
    series: series.map((s) => ({
      name: s.name,
      type: 'bar',
      data: s.data,
      barMaxWidth: 16,
      itemStyle: { borderRadius: [0, 4, 4, 0] }
    }))
  }
}

const taskPie = computed(() => pieOption(task.value.status_dist, TASK_COLOR))
const taskTrend = computed(() => {
  const rows = task.value.trend || []
  return {
    color: ['#409eff', '#67c23a'],
    tooltip: { trigger: 'axis' },
    legend: { top: 0, data: ['新建', '完成'], textStyle: { color: '#606266' } },
    grid: { left: 8, right: 24, top: 36, bottom: 30, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: rows.map((r: any) => (r.date || '').substring(5)),
      axisLabel: { color: '#909399' }
    },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    series: [
      {
        name: '新建', type: 'line', smooth: true, data: rows.map((r: any) => r.total),
        areaStyle: { opacity: 0.12 }, symbolSize: 6
      },
      {
        name: '完成', type: 'line', smooth: true, data: rows.map((r: any) => r.finished),
        areaStyle: { opacity: 0.12 }, symbolSize: 6
      }
    ]
  }
})
const taskUserBar = computed(() => {
  const rows = task.value.user_rank || []
  return hBarOption(
    rows.map((r: any) => r.name),
    [
      { name: '任务数', data: rows.map((r: any) => r.total), color: '#409eff' },
      { name: '已完成', data: rows.map((r: any) => r.finished), color: '#67c23a' }
    ]
  )
})
const taskDeptBar = computed(() => {
  const rows = task.value.dept_rank || []
  return hBarOption(
    rows.map((r: any) => r.name),
    [
      { name: '任务数', data: rows.map((r: any) => r.total), color: '#409eff' },
      { name: '已完成', data: rows.map((r: any) => r.finished), color: '#67c23a' }
    ]
  )
})

const attPie = computed(() => pieOption(att.value.status_dist, ATT_COLOR))
const attDeptBar = computed(() => {
  const rows = att.value.dept_rank || []
  const opt = hBarOption(
    rows.map((r: any) => r.name),
    [{ name: '出勤率', data: rows.map((r: any) => r.rate), color: '#67c23a' }]
  )
  opt.xAxis.max = 100
  opt.series[0].label = { show: true, position: 'right', formatter: '{c}%', color: '#606266' }
  return opt
})

const logPie = computed(() => pieOption(log.value.time_dist, LOG_COLOR))
const hasLogTime = computed(() => (log.value.time_dist || []).some((x: any) => x.value > 0))
const logDeptBar = computed(() => {
  const rows = log.value.dept_rank || []
  return hBarOption(
    rows.map((r: any) => r.name),
    [{ name: '日志数', data: rows.map((r: any) => r.count), color: '#e6a23c' }]
  )
})
const commentUserBar = computed(() => {
  const rows = log.value.comment_user_rank || []
  return hBarOption(
    rows.map((r: any) => r.name),
    [{ name: '评论数', data: rows.map((r: any) => r.value), color: '#409eff' }]
  )
})
const commentLogBar = computed(() => {
  const rows = log.value.comment_log_rank || []
  return hBarOption(
    rows.map((r: any) => r.name),
    [{ name: '评论数', data: rows.map((r: any) => r.value), color: '#b37feb' }]
  )
})

const flowPie = computed(() => pieOption(flow.value.status_dist, FLOW_COLOR))
const flowTypeBar = computed(() => {
  const rows = flow.value.type_dist || []
  return hBarOption(
    rows.map((r: any) => r.name),
    [
      { name: '总数', data: rows.map((r: any) => r.total), color: '#409eff' },
      { name: '已通过', data: rows.map((r: any) => r.approved), color: '#67c23a' }
    ]
  )
})

onMounted(() => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 89)
  dateRange.value = [fmt(start), fmt(end)]
  loadMeta()
  loadData()
})
</script>

<style scoped>
.report-page {
  height: 100%;
  overflow-y: auto;
  background: #f5f7fa;
  padding: 16px;
  box-sizing: border-box;
}

.filter-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.scope-tag {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}
.scope-tag b {
  color: #409eff;
  font-weight: 600;
}

.report-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px 18px;
}
.panel-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
.dot-blue { background: #409eff; }
.dot-green { background: #67c23a; }
.dot-orange { background: #e6a23c; }
.dot-purple { background: #b37feb; }
.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}
.panel-sub {
  font-size: 12px;
  color: #a8abb2;
}
.export-btn {
  margin-left: auto;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.kpi {
  background: #f8f9fb;
  border: 1px solid #eef0f4;
  border-radius: 8px;
  padding: 14px 12px;
  text-align: center;
  min-width: 0;
}
.kpi-num {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
  word-break: break-all;
}
.kpi-label {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
.c-blue { color: #409eff; }
.c-green { color: #67c23a; }
.c-orange { color: #e6a23c; }
.c-red { color: #f56c6c; }
.c-purple { color: #b37feb; }
.c-dark { color: #303133; }

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 14px;
}
.chart-card {
  border: 1px solid #eef0f4;
  border-radius: 8px;
  padding: 12px 14px;
  background: #fff;
  min-width: 0;
}
.chart-wide {
  grid-column: 1 / -1;
}
.chart-card h4 {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.empty {
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 13px;
}

@media (max-width: 768px) {
  .chart-grid { grid-template-columns: 1fr; }
  .chart-wide { grid-column: auto; }
}
</style>
