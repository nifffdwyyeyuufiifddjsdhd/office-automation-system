<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Check, Checked } from '@element-plus/icons-vue'
import { uploadAttachment } from '@/api/flow'
import FlowChart from '@/components/FlowChart.vue'
import { hasPerm } from '@/utils/permission'
import { useUserStore } from '@/store/user'
import { orgTreeEnabled } from '@/api/user'
import {
  getTodayStatus, checkIn, checkOut, getMyRecords,
  applyMakeup, getMyMakeups, getPendingMakeups, approveMakeup, withdrawMakeup as withdrawMakeupApi, getAllRecords,
  outing, getMyOutings,
  getGeoConfig, saveGeoConfig, getGeoAbnormal, approveGeoAbnormal, rejectGeoAbnormal,
  getMyStatistics, getStatistics,
  applyAppeal, getMyAppeals, getPendingAppeals, approveAppeal,
  listShiftTemplates, saveShiftTemplate, deleteShiftTemplate, activateShiftTemplate,
  listShiftSchedules, saveShiftSchedule, deleteShiftSchedule,
  listHolidays, addHoliday, deleteHoliday,
  getAttendanceOperationLogs,
  type TodayStatus, type AttendanceRecord, type MakeupApply, type MyAttendanceStat
} from '@/api/attendance'

const userStore = useUserStore()
// 系统管理员后端豁免权限码，前端只认 admin
const isSysAdmin = computed(() => (userStore.roles || []).includes('admin'))
// 出勤配置（班次、节假日、定位）：管理员或持配置权限的角色
const canConfig = computed(() => isSysAdmin.value || hasPerm('attendance:manage'))
// 数据范围：attendance:view-all 看全公司，attendance:view-dept 看本部门
const canViewAll = computed(() => isSysAdmin.value || hasPerm('attendance:view-all') || hasPerm('attendance:view-dept'))
// 出勤统计：管理员、总经理、人事主管可看，部门主管只看本部门明细
const canViewStats = computed(() => isSysAdmin.value || hasPerm('attendance:view-all'))
// 角色：部门主管（补签初审 / 异地审核）、人事（补签终审 / 申诉审核）
const isManager = computed(() => (userStore.roles || []).includes('manager'))
// 人事专员与人事主管
const isHr = computed(() => { const roles = userStore.roles || []; return roles.includes('hr') || roles.includes('hr_manager') })
// 补签审批：管理员、部门主管、人事，列表内分初审终审
const canReviewMakeup = computed(() => isSysAdmin.value || isManager.value || isHr.value)
// 终审只有管理员和人事主管，人事专员只能初审
const canFinalMakeup = computed(() => isSysAdmin.value || (userStore.roles || []).includes('hr_manager'))
// 申诉审核：管理员或持 attendance:audit 的账号
const canReviewAppeal = computed(() => isSysAdmin.value || hasPerm('attendance:audit'))
// 异地签到审核：管理员和部门主管
const canReviewGeo = computed(() => isSysAdmin.value || isManager.value)

// Tab
const activeTab = ref('my')

// 当天打卡
const today = ref<TodayStatus>({
  canCheckIn: false, canCheckOut: false, isHoliday: false,
  workStart: '09:00', workEnd: '18:00',
  checkInTime: null, checkOutTime: null, status: null, workHours: null
})
const checkType = ref('normal')
const cfgForm = ref<any>({})

const shiftTemplates = ref<any[]>([])
const tplDialog = ref(false)
const tplForm = ref<any>({})

async function loadConfig() {
  try { shiftTemplates.value = await listShiftTemplates() } catch { /* 已由 request.ts 提示 */ }
}
function editTemplate(row: any) {
  tplForm.value = row ? { ...row } : { template_name: '', work_start: '09:00', work_end: '18:00', late_threshold: '09:30', early_leave_threshold: '17:30', shift_type: 'day', is_all_day: false }
  tplDialog.value = true
}
function validTimes(f: any): boolean {
  if (f.is_all_day) return true
  const ws = f.work_start, we = f.work_end, lt = f.late_threshold, et = f.early_leave_threshold
  if (!ws || !we || !lt || !et) { ElMessage.warning('请填全所有时间'); return false }
  if (lt <= ws) { ElMessage.warning('迟到线必须晚于上班时间'); return false }
  if (et >= we) { ElMessage.warning('早退线必须早于下班时间'); return false }
  if (we <= ws) { ElMessage.warning('下班时间必须晚于上班时间'); return false }
  return true
}
async function saveTemplate() {
  if (!validTimes(tplForm.value)) return
  if (tplForm.value.is_all_day) { tplForm.value.work_start = '00:00'; tplForm.value.work_end = '23:59'; tplForm.value.late_threshold = '23:59'; tplForm.value.early_leave_threshold = '00:00' }
  try {
    await saveShiftTemplate(tplForm.value)
    tplDialog.value = false
    ElMessage.success('模板已保存')
    loadConfig()
  } catch { /* 已由 request.ts 提示 */ }
}
async function activateTemplate(row: any) {
  try {
    await activateShiftTemplate(row.id)
    ElMessage.success('已切换为当前班次')
    loadConfig()
    loadToday()
  } catch { /* 已由 request.ts 提示 */ }
}
const schedules = ref<any[]>([])
const schedDialog = ref(false)
const schedForm = ref<any>({})

async function loadSchedules() {
  try { schedules.value = await listShiftSchedules() } catch { /* 已由 request.ts 提示 */ }
}
function editSchedule(row: any) {
  schedForm.value = row ? { ...row } : { schedule_name: '', schedule_type: 'once', work_start: '09:00', work_end: '18:00', late_threshold: '09:30', early_leave_threshold: '17:30' }
  schedDialog.value = true
}
async function saveSchedule() {
  if (!validTimes(schedForm.value)) return
  try {
    await saveShiftSchedule(schedForm.value)
    schedDialog.value = false
    ElMessage.success('规则已保存')
    loadSchedules()
  } catch { /* 已由 request.ts 提示 */ }
}
async function delSchedule(row: any) {
  try {
    await ElMessageBox.confirm('确定删除这条排班规则吗？', '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await deleteShiftSchedule(row.id)
    ElMessage.success('已删除')
    loadSchedules()
  } catch { /* 已由 request.ts 提示 */ }
}
async function delTemplate(row: any) {
  try {
    await ElMessageBox.confirm('确定删除这个班次模板吗？', '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await deleteShiftTemplate(row.id)
    ElMessage.success('已删除')
    loadConfig()
  } catch { /* 已由 request.ts 提示 */ }
}

async function loadToday() {
  try { today.value = await getTodayStatus() } catch { /* 忽略 */ }
}

function getPosition(): Promise<{ lat: number | null; lng: number | null }> {
  return new Promise(resolve => {
    if (!navigator.geolocation) return resolve({ lat: null, lng: null })
    navigator.geolocation.getCurrentPosition(
      pos => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      () => resolve({ lat: null, lng: null }),
      { timeout: 3000 }
    )
  })
}

// 打卡：先取定位再调接口，后端返回 NEED_FORCE 时确认后强打
async function punch(api: (lat: number | null, lng: number | null, force: boolean) => Promise<any>) {
  try {
    const { lat, lng } = await getPosition()
    await api(lat, lng, false)
  } catch (e: any) {
    const msg = e?.message || ''
    if (msg.startsWith('NEED_FORCE:')) {
      try {
        await ElMessageBox.confirm(msg.replace('NEED_FORCE:', ''), '位置校验', {
          confirmButtonText: '强制打卡', cancelButtonText: '取消', type: 'warning'
        })
        const { lat, lng } = await getPosition()
        await api(lat, lng, true)
        return
      } catch (cancel: any) {
        throw new Error('已取消打卡')
      }
    }
    throw e
  }
}

async function doCheckIn() {
  try {
    await punch((lat, lng, force) => checkIn(lat, lng, force))
    ElMessage.success('签到成功')
    loadToday()
    loadMyRecords()
  } catch (e: any) { ElMessage.error(e?.message || '签到失败') }
}

async function doCheckOut() {
  try {
    await punch((lat, lng, force) => checkOut(lat, lng, force))
    ElMessage.success('签退成功')
    loadToday()
    loadMyRecords()
  } catch (e: any) { ElMessage.error(e?.message || '签退失败') }
}

// 管理员：地理位置校验配置（13.1.10）
const geoForm = ref<any>({ enable: false, lat: '', lng: '', radius: 500, weekendCount: false })
const geoAbnormalList = ref<any[]>([])

async function loadGeoConfig() {
  try {
    const cfg = await getGeoConfig()
    geoForm.value = { enable: cfg.enable === '1', lat: cfg.lat, lng: cfg.lng, radius: Number(cfg.radius) || 500, weekendCount: cfg.weekendCount === '1' }
  } catch { /* 忽略 */ }
}
async function submitGeoConfig() {
  try {
    await saveGeoConfig({
      enable: geoForm.value.enable ? '1' : '0',
      lat: geoForm.value.lat,
      lng: geoForm.value.lng,
      radius: String(geoForm.value.radius),
      weekendCount: geoForm.value.weekendCount ? '1' : '0'
    })
    ElMessage.success('地理位置配置已保存')
  } catch (e: any) { ElMessage.error(e?.message || '保存失败') }
}
async function loadGeoAbnormal() {
  try { geoAbnormalList.value = await getGeoAbnormal() } catch { /* 忽略 */ }
}
const opLogDialog = ref(false)
const opLogs = ref<any[]>([])
const opLogLoading = ref(false)
async function loadOperationLogs() {
  opLogLoading.value = true
  try {
    opLogs.value = await getAttendanceOperationLogs()
  } catch { /* 已由 request.ts 提示 */ }
  opLogLoading.value = false
}
async function geoApprove(row: any) {
  try {
    await approveGeoAbnormal(row.id)
    ElMessage.success('已确认正常')
    loadGeoAbnormal()
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
}
async function geoReject(row: any) {
  try {
    await ElMessageBox.confirm('驳回后该记录将标记为缺勤，确定吗？', '驳回确认', { type: 'warning' })
  } catch { return }
  try {
    await rejectGeoAbnormal(row.id)
    ElMessage.success('已驳回并记缺勤')
    loadGeoAbnormal()
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
}

// 外出打卡
const outingDialog = ref(false)
const outingForm = ref({ reason: '', expected_return: '' })
const myOutings = ref<any[]>([])

async function loadMyOutings() {
  try { myOutings.value = await getMyOutings(month.value) } catch { /* 忽略 */ }
}

function openOutingDialog() {
  outingForm.value = { reason: '', expected_return: '' }
  outingDialog.value = true
}

async function submitOuting() {
  if (!outingForm.value.reason.trim()) { ElMessage.warning('请填写外出事由'); return }
  try {
    await outing({ reason: outingForm.value.reason.trim(), expected_return: outingForm.value.expected_return.trim() || undefined })
    ElMessage.success('外出打卡成功')
    outingDialog.value = false
    loadMyOutings()
  } catch (e: any) { ElMessage.error(e?.message || '外出打卡失败') }
}

// 我的记录
const myRecords = ref<AttendanceRecord[]>([])
const dayFilter = ref('today')
const makeupDayFilter = ref('all')

function dayKey(offset: number) {
  const d = new Date()
  d.setDate(d.getDate() - offset)
  return d.toISOString().slice(0, 10)
}
const filteredRecords = computed(() => {
  if (dayFilter.value === 'all') return myRecords.value
  const offset = dayFilter.value === 'today' ? 0 : dayFilter.value === 'yesterday' ? 1 : 2
  return myRecords.value.filter(r => r.attendance_date === dayKey(offset))
})
const filteredMakeups = computed(() => {
  if (makeupDayFilter.value === 'all') return myMakeups.value
  const offset = makeupDayFilter.value === 'today' ? 0 : makeupDayFilter.value === 'yesterday' ? 1 : 2
  return myMakeups.value.filter(r => r.attendance_date === dayKey(offset))
})
// 重新签到/重新签退：签到/签退后2小时内可重新操作（13.1.9）
const recheckInWindow = computed(() => {
  const t = today.value?.checkInTime
  return !!t && new Date(t).getTime() + 2 * 3600 * 1000 > Date.now()
})
const recheckOutWindow = computed(() => {
  const t = today.value?.checkOutTime
  return !!t && new Date(t).getTime() + 2 * 3600 * 1000 > Date.now()
})
// 我的申诉日期筛选（今天/昨天/前天/全部）
const appealDayFilter = ref('all')
const filteredAppeals = computed(() => {
  if (appealDayFilter.value === 'all') return myAppeals.value
  const offset = appealDayFilter.value === 'today' ? 0 : appealDayFilter.value === 'yesterday' ? 1 : 2
  return myAppeals.value.filter(r => r.record_date === dayKey(offset))
})
const loading = ref(false)
const month = ref(new Date().toISOString().slice(0, 7))

async function loadMyRecords() {
  loading.value = true
  try { myRecords.value = await getMyRecords(month.value) } catch { /* 忽略 */ }
  finally { loading.value = false }
}

// 补签申请
const makeupDialog = ref(false)
const makeupForm = ref<MakeupApply>({ attendance_date: '', type: 1, reason: '' })
const myMakeups = ref<MakeupApply[]>([])

async function loadMyMakeups() {
  try { myMakeups.value = await getMyMakeups() } catch { /* 忽略 */ }
}

function openMakeupDialog(existing?: MakeupApply) {
  if (existing) {
    makeupForm.value = { ...existing }
  } else {
    makeupForm.value = { attendance_date: '', type: 1, reason: '' }
  }
  makeupDialog.value = true
}

async function submitMakeup() {
  if (!makeupForm.value.attendance_date || !makeupForm.value.reason) {
    ElMessage.warning('请填写日期和原因')
    return
  }
  try {
    const payload: any = { ...makeupForm.value }
    delete payload.id            // 重新提交不带旧ID，免得主键冲突
    delete payload.status        // 状态由后端重新置为待审批
    delete payload.created_at
    delete payload.approve_remark
    delete payload.approve_time
    delete payload.real_name
    if (!payload.check_in_time) payload.check_in_time = null
    if (!payload.check_out_time) payload.check_out_time = null
    await applyMakeup(payload)
    ElMessage.success('补签申请已提交')
    makeupDialog.value = false
    loadMyMakeups()
  } catch (e: any) { ElMessage.error(e?.message || '提交失败') }
}

// 管理员：全部出勤
const allRecords = ref<AttendanceRecord[]>([])
const allTotal = ref(0)
const allQuery = ref({ pageNum: 1, pageSize: 20, startDate: '', endDate: '', deptId: null as number | null, status: null as number | null })
const quickDate = ref('today')

function onQuickDate(v: string) {
  const today = new Date()
  const fmt = (d: Date) => {
    const p = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}`
  }
  if (v === 'today') {
    allQuery.value.startDate = fmt(today)
    allQuery.value.endDate = fmt(today)
  } else if (v === 'yesterday') {
    const y = new Date(today); y.setDate(y.getDate()-1)
    allQuery.value.startDate = fmt(y)
    allQuery.value.endDate = fmt(y)
  } else if (v === 'dayBeforeBefore') {
    const d = new Date(today); d.setDate(d.getDate()-2)
    allQuery.value.startDate = fmt(d)
    allQuery.value.endDate = fmt(d)
  } else {
    allQuery.value.startDate = ''
    allQuery.value.endDate = ''
  }
  allQuery.value.pageNum = 1
  loadAllRecords()
}
const allLoading = ref(false)

function exportExcel() {
  const params = new URLSearchParams()
  if (allQuery.value.startDate) params.set('startDate', allQuery.value.startDate)
  if (allQuery.value.endDate) params.set('endDate', allQuery.value.endDate)
  if (allQuery.value.deptId) params.set('deptId', String(allQuery.value.deptId))
  if (allQuery.value.status !== null) params.set('status', String(allQuery.value.status))
  const token = sessionStorage.getItem('token') || ''
  // 用 fetch 下载文件
  fetch('/api/attendance/export?' + params.toString(), { headers: { Authorization: 'Bearer ' + token } })
    .then(async r => {
      if (!r.ok) throw new Error('导出失败')
      // 后端出错会返回 JSON，别当 Excel 存下来
      const ct = r.headers.get('content-type') || ''
      if (!ct.includes('spreadsheet')) {
        const j = await r.json().catch(() => null)
        throw new Error(j?.message || '导出失败')
      }
      return r.blob()
    })
    .then(blob => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = '出勤报表.xlsx'
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    })
    .catch(e => ElMessage.error(e.message || '导出失败'))
}

async function loadAllRecords() {
  allLoading.value = true
  try {
    const res = await getAllRecords(allQuery.value)
    allRecords.value = res.records
    allTotal.value = res.total
  } catch { /* 忽略 */ }
  finally { allLoading.value = false }
}

// 管理员：补签审批
const pendingMakeups = ref<MakeupApply[]>([])
const pendingLoading = ref(false)

async function loadPendingMakeups() {
  pendingLoading.value = true
  try { pendingMakeups.value = await getPendingMakeups(0) } catch { /* 忽略 */ }
  finally { pendingLoading.value = false }
}

async function doApprove(item: MakeupApply, result: number) {
  try {
    await ElMessageBox.prompt(result === 1 ? '审批意见（选填）' : '驳回原因（选填）', result === 1 ? '通过补签' : '驳回补签', {
      inputPlaceholder: '可填写审批意见',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }).then(async ({ value }) => {
      await approveMakeup(item.id!, result, value || '')
      const okMsg = result === 1
        ? (item.current_stage === 1 && !canFinalMakeup.value ? '已通过初审，转人事终审' : '已通过')
        : '已驳回'
      ElMessage.success(okMsg)
      loadPendingMakeups()
    })
  } catch { /* 取消 */ }
}

// 撤回自己的补签申请
async function withdrawMakeup(row: MakeupApply) {
  try {
    await ElMessageBox.confirm('确定撤回这条补签申请吗？', '撤回申请', { type: 'warning' })
    await withdrawMakeupApi(Number(row.id))
    ElMessage.success('已撤回')
    loadMyMakeups()
  } catch (e: any) {
    ElMessage.error(e?.message || '撤回失败，请重试')
  }
}


// 我的统计
const myStatMonth = ref(new Date().toISOString().slice(0, 7))
const myStat = ref<MyAttendanceStat | null>(null)
const myStatLoading = ref(false)
// 明细按状态拆开，能看出迟到几次
const myStatRows = computed(() => {
  const m = myStat.value
  if (!m) return []
  return [
    { name: '正常出勤', days: m.normalDays, tip: '按时上下班的天数' },
    { name: '迟到', days: m.lateDays, tip: '签到晚于上班时间' },
    { name: '早退', days: m.earlyDays, tip: '签退早于下班时间' },
    { name: '缺勤', days: m.absentDays, tip: '当天没有任何打卡记录' },
    { name: '其他', days: m.otherDays, tip: '出差、请假、外出打卡等不计入正常/异常的天数' }
  ]
})
async function loadMyStat() {
  myStatLoading.value = true
  try {
    myStat.value = await getMyStatistics(myStatMonth.value)
  } catch {
    myStat.value = null
  } finally {
    myStatLoading.value = false
  }
}

// 出勤统计（管理员）
const orgs = ref<any[]>([])
const statsMonth = ref(new Date().toISOString().slice(0, 7))
const statsDeptId = ref(0 as number | null)
const statRows = ref<any[]>([])
const statLoading = ref(false)

async function loadOrgs() {
  try {
    const tree = await orgTreeEnabled()
    const flat: any[] = []
    const walk = (nodes: any[]) => nodes.forEach((n: any) => {
      flat.push({ id: n.id, name: n.label })
      if (n.children?.length) walk(n.children)
    })
    walk(tree || [])
    orgs.value = flat
  } catch { /* 忽略 */ }
}
async function loadStats() {
  statLoading.value = true
  try {
    statRows.value = await getStatistics(statsDeptId.value, statsMonth.value || undefined)
  } catch { /* 忽略 */ }
  finally { statLoading.value = false }
}
function exportStats() {
  const params = new URLSearchParams()
  if (statsMonth.value) params.set('month', statsMonth.value)
  if (statsDeptId.value) params.set('deptId', String(statsDeptId.value))
  const token = sessionStorage.getItem('token') || ''
  // 导出要二进制流并校验 content-type，直接走 fetch
  fetch('/api/attendance/statistics/export?' + params.toString(), { headers: { Authorization: 'Bearer ' + token } })
    .then(async r => {
      if (!r.ok) throw new Error('导出失败')
      const ct = r.headers.get('content-type') || ''
      if (!ct.includes('spreadsheet')) {
        const j = await r.json().catch(() => null)
        throw new Error(j?.message || '导出失败')
      }
      return r.blob()
    })
    .then(blob => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = '出勤统计.xlsx'
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    })
    .catch(e => ElMessage.error(e.message || '导出失败'))
}

// 异常申诉
const appealDialog = ref(false)
const appealForm = ref({ record_id: null as number | null, reason: '', attachment_ids: [] as number[] })
const appealFileList = ref<any[]>([])
const appealUploadRef = ref<any>(null)
const myAppeals = ref<any[]>([])
const pendingAppeals = ref<any[]>([])
const appealLoading = ref(false)

function sameMonth(dateStr?: string) {
  if (!dateStr) return false
  const now = new Date()
  const m = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  return dateStr.slice(0, 7) === m
}

const appealStatusMap = (s: number) => ({
  0: { text: '待审核', type: 'warning' as const },
  1: { text: '已通过', type: 'success' as const },
  2: { text: '已驳回', type: 'danger' as const }
})[s] || { text: '-', type: 'info' as const }

function openAppeal(row: any) {
  if (!row.attendance_date) { ElMessage.warning('该记录无出勤日期'); return }
  const now = new Date()
  const ym = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  const d = String(row.attendance_date).slice(0, 10)
  if (d > now.toISOString().slice(0, 10)) { ElMessage.warning('不能申诉未来的出勤记录'); return }
  if (d.slice(0, 7) !== ym) { ElMessage.warning(`申诉仅限当月数据（${ym}）`); return }
  appealForm.value = { record_id: row.id, reason: '', attachment_ids: [] }
  appealFileList.value = []
  appealDialog.value = true
}
async function onAppealFileChange(file: any, files: any[]) {
  appealFileList.value = files
  try {
    const res = await uploadAttachment(file.raw)
    appealForm.value.attachment_ids.push(res.id)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '附件上传失败')
    appealFileList.value = files.filter((f: any) => f.uid !== file.uid)
  }
}
function onAppealFileRemove(_file: any, files: any[]) {
  appealFileList.value = files
}
function onAppealPaste(e: ClipboardEvent) {
  if (!e.clipboardData) return
  const files: File[] = []
  for (const it of e.clipboardData.items) {
    if (it.kind === 'file') { const f = it.getAsFile(); if (f) files.push(f) }
  }
  if (!files.length) return
  e.preventDefault()
  if (appealFileList.value.length + files.length > 10) { ElMessage.warning('附件最多 10 个'); return }
  for (const f of files) {
    if (f.size > 200 * 1024 * 1024) { ElMessage.error(`文件 ${f.name || '粘贴的图片'} 超过 200MB`); continue }
    const uid = Date.now() + Math.floor(Math.random() * 10000)
    const uf = { uid, name: f.type.startsWith('image/') ? `粘贴截图_${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.${(f.type.split('/')[1] || 'png')}` : f.name, size: f.size, type: f.type, raw: f, status: 'uploading' } as any
    appealFileList.value = [...appealFileList.value, uf]
    uploadAttachment(f).then((res) => {
      appealForm.value.attachment_ids.push(res.id)
      appealFileList.value = appealFileList.value.map((x: any) => x.uid === uid ? { ...x, status: 'success' } : x)
    }).catch((err) => {
      ElMessage.error(err?.response?.data?.message || '附件上传失败')
      appealFileList.value = appealFileList.value.filter((x: any) => x.uid !== uid)
    })
  }
}
function onAppealHover() { appealUploadRef.value?.focus() }
async function submitAppeal() {
  if (!appealForm.value.reason) { ElMessage.warning('请填写申诉理由'); return }
  try {
    await applyAppeal(appealForm.value)
    ElMessage.success('申诉已提交')
    appealDialog.value = false
    loadMyAppeals()
  } catch { ElMessage.error('提交失败') }
}
async function loadMyAppeals() {
  try {
    myAppeals.value = await getMyAppeals()
  } catch { /* 忽略 */ }
}
async function loadPendingAppeals() {
  appealLoading.value = true
  try {
    pendingAppeals.value = await getPendingAppeals()
  } catch { /* 忽略 */ }
  finally { appealLoading.value = false }
}
async function doAppealApprove(row: any, result: number) {
  try {
    const { value } = await ElMessageBox.prompt(result === 1 ? '审批意见（选填）' : '驳回说明（选填）', result === 1 ? '通过申诉' : '驳回申诉', {
      inputPlaceholder: '可填写审批意见',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await approveAppeal(row.id, result, value || '')
    ElMessage.success(result === 1 ? '已通过' : '已驳回')
    loadPendingAppeals()
  } catch { /* 取消 */ }
}

// 节假日维护（管理员）
const holidays = ref<any[]>([])
const holidayDialog = ref(false)
const holidayForm = ref({ holiday_date: '', holiday_name: '', holiday_type: 1 })

async function loadHolidays() {
  try {
    holidays.value = await listHolidays()
  } catch { /* 忽略 */ }
}
function openHoliday() {
  holidayForm.value = { holiday_date: '', holiday_name: '', holiday_type: 1 }
  holidayDialog.value = true
}
async function saveHoliday() {
  if (!holidayForm.value.holiday_date || !holidayForm.value.holiday_name) { ElMessage.warning('请填写日期和名称'); return }
  try {
    await addHoliday(holidayForm.value)
    ElMessage.success('已添加')
    holidayDialog.value = false
    loadHolidays()
  } catch { ElMessage.error('保存失败') }
}
async function delHoliday(row: any) {
  try {
    await ElMessageBox.confirm('确定删除这条节假日记录吗？', '删除确认', { type: 'warning' })
    await deleteHoliday(row.id)
    ElMessage.success('已删除')
    loadHolidays()
  } catch { /* 取消 */ }
}
// 工具
function fmtTime(t?: string | null) {
  if (!t) return '--:--'
  const d = new Date(t)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

function statusTag(s: number | null) {
  if (s === null || s === undefined) return { text: '未签到', type: 'info' as const }
  const map: Record<number, { text: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
    0: { text: '正常', type: 'success' },
    1: { text: '迟到', type: 'warning' },
    2: { text: '早退', type: 'warning' },
    3: { text: '缺勤', type: 'danger' },
    4: { text: '外出', type: 'info' },
    5: { text: '出差', type: 'info' },
    6: { text: '请假', type: 'info' },
    7: { text: '严重迟到', type: 'danger' },
    8: { text: '未签退异常', type: 'danger' },
    9: { text: '严重早退', type: 'danger' }
  }
  return map[s] || { text: '未知', type: 'info' as const }
}

function statusTextTag(text?: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    正常: 'success', 迟到: 'warning', 早退: 'warning', 缺勤: 'danger',
    严重迟到: 'danger', 未签退异常: 'danger', 严重早退: 'danger'
  }
  return { text: text || '未知', type: map[text || ''] || 'info' as const }
}

const makeupStatusMap = (s: number) => ({
  0: { text: '待审批', type: 'warning' as const },
  1: { text: '已通过', type: 'success' as const },
  2: { text: '已驳回', type: 'danger' as const }
})[s] || { text: '-', type: 'info' as const }

// 流程图：补签走主管初审加人事终审，申诉走部门主管
const chartVisible = ref(false)
const chartTitle = ref('')
const chartCreator = ref('')
const chartNodes = ref<any[]>([])

function openChart(title: string, creator: string, nodes: any[]) {
  chartTitle.value = title
  chartCreator.value = creator
  chartNodes.value = nodes
  chartVisible.value = true
}

function openMakeupChart(row: MakeupApply) {
  const stage = Number((row as any).current_stage || 1)
  let first = 'wait'
  let second = 'wait'
  if (row.status === 1) { first = 'done'; second = 'done' }
  else if (row.status === 2) {
    // 看 current_stage 判断驳回落哪步，2 表示人事驳回
    if (stage >= 2) { first = 'done'; second = 'reject' } else { first = 'reject' }
  } else if (stage >= 2) { first = 'done'; second = 'current' } else { first = 'current' }

  openChart('补签申请审批流程', row.attendance_date, [
    { name: '部门主管初审', role: '申请人所在部门主管', status: first },
    { name: '人事终审', role: '人事专员 / 人事主管', status: second },
  ])
}

function openAppealChart(row: any) {
  let s = 'current'
  if (row.status === 1) s = 'done'
  else if (row.status === 2) s = 'reject'
  openChart('考勤异常申诉流程', row.record_date || '', [
    { name: '部门主管审核', role: '申请人所在部门主管', status: s },
  ])
}

function tabChanged(t: string) {
  if (t === 'all' && canViewAll.value) loadAllRecords()
  if (t === 'makeup-approve' && canReviewMakeup.value) loadPendingMakeups()
  if (t === 'makeup') loadMyMakeups()
  if (t === 'stats' && canViewStats.value) { loadOrgs(); loadStats() }
  if (t === 'appeal') loadMyAppeals()
  if (t === 'my-stat') loadMyStat()
  if (t === 'appeal-approve' && canReviewAppeal.value) loadPendingAppeals()
  if (t === 'config' && canConfig.value) { loadConfig(); loadSchedules(); loadHolidays(); loadGeoConfig() }
  if (t === 'geo' && canReviewGeo.value) loadGeoAbnormal()
}

onMounted(() => {
  loadToday()
  loadMyRecords()
})
</script>

<template>
  <div class="attendance-page">
    <el-tabs v-model="activeTab" @tab-change="tabChanged">
      <!-- 我的打卡 -->
      <el-tab-pane label="我的打卡" name="my">
        <el-alert v-if="today.isHoliday" type="info" :closable="false" title="今天是节假日，无需打卡" style="margin-bottom:12px" />
        <!-- 打卡卡片 -->
        <el-card class="clock-card" shadow="never">
          <div class="clock-inner">
            <div class="clock-left">
              <el-icon :size="40" color="#409EFF"><Clock /></el-icon>
              <div class="clock-info">
                <div class="work-time">工作时间 {{ today.workStart }} - {{ today.workEnd }}</div>
                <div class="today-date">{{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}</div>
              </div>
            </div>
            <div class="clock-center">
              <div class="time-block">
                <div class="time-label">上班签到</div>
                <div class="time-value" :class="{ late: today.status === 1 }">{{ fmtTime(today.checkInTime) }}</div>
              </div>
              <div class="time-divider"></div>
              <div class="time-block">
                <div class="time-label">下班签退</div>
                <div class="time-value" :class="{ early: today.status === 2 }">{{ fmtTime(today.checkOutTime) }}</div>
              </div>
            </div>
            <div class="clock-right">
              <el-tag v-if="today.status !== null" :type="statusTag(today.status).type" size="large">
                {{ statusTag(today.status).text }}
              </el-tag>
              <template v-if="!today.checkInTime">
                <el-tooltip :disabled="today.canCheckIn" content="当前不在上班签到时间窗口内" placement="top">
                  <el-button type="primary" size="large" :disabled="!today.canCheckIn" @click="doCheckIn">
                    <el-icon><Check /></el-icon>&nbsp;上班签到
                  </el-button>
                </el-tooltip>
              </template>
              <template v-else-if="!today.checkOutTime">
                <el-tooltip :disabled="today.canCheckOut" content="当前不在下班签退时间窗口内" placement="top">
                  <el-button type="success" size="large" :disabled="!today.canCheckOut" @click="doCheckOut">
                    <el-icon><Checked /></el-icon>&nbsp;下班签退
                  </el-button>
                </el-tooltip>
              </template>
              <el-tag v-else type="success" size="large">今日已完成</el-tag>
              <el-button type="warning" plain size="large" @click="openOutingDialog" style="margin-left:10px">外出打卡</el-button>
              <div v-if="today.checkInTime || today.checkOutTime" style="margin-top:8px;display:flex;gap:6px;justify-content:center;flex-wrap:wrap;">
                <el-button v-if="today.checkInTime && recheckInWindow" type="primary" link size="small" @click="doCheckIn">重新签到</el-button>
                <el-button v-if="today.checkOutTime && recheckOutWindow" type="success" link size="small" @click="doCheckOut">重新签退</el-button>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 我的记录 -->
        <el-card shadow="never">
          <template #header>
            <div class="records-header">
              <span>我的出勤记录</span>
              <div style="display:flex;gap:8px;">
                <el-date-picker v-model="month" type="month" placeholder="选择月份" format="YYYY-MM" value-format="YYYY-MM" @change="loadMyRecords(); loadMyOutings()" style="width:130px" />
                <el-button type="primary" plain size="small" @click="openMakeupDialog">申请补签</el-button>
              </div>
            </div>
          </template>
          <el-radio-group v-model="dayFilter" size="small" style="margin-bottom:12px">
            <el-radio-button value="today">今天</el-radio-button>
            <el-radio-button value="yesterday">昨天</el-radio-button>
            <el-radio-button value="dayBefore">前天</el-radio-button>
            <el-radio-button value="all">全部</el-radio-button>
          </el-radio-group>
          <el-table :data="filteredRecords" v-loading="loading" stripe>
            <el-table-column prop="attendance_date" label="日期" width="110" />
            <el-table-column label="签到" width="80">
              <template #default="{ row }">{{ fmtTime(row.check_in_time) }}</template>
            </el-table-column>
            <el-table-column label="签退" width="80">
              <template #default="{ row }">{{ fmtTime(row.check_out_time) }}</template>
            </el-table-column>
            <el-table-column label="工时" width="70">
              <template #default="{ row }">{{ row.work_hours ? row.work_hours + 'h' : '-' }}</template>
            </el-table-column>
            <el-table-column label="加班" width="70">
              <template #default="{ row }">{{ row.overtime_hours ? row.overtime_hours + 'h' : '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="补签" width="60">
              <template #default="{ row }">
                <el-tag v-if="row.is_makeup" type="info" size="small">补签</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
            <el-table-column label="操作" width="70" fixed="right">
              <template #default="{ row }">
                <el-button v-if="[1,2,3,7,8,9].includes(row.status) && sameMonth(row.attendance_date)" type="warning" link size="small" @click="openAppeal(row)">申诉</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 我的外出记录 -->
        <el-card shadow="never" style="margin-top:12px">
          <template #header>
            <div class="records-header">
              <span>我的外出记录</span>
              <el-button type="warning" plain size="small" @click="openOutingDialog">外出打卡</el-button>
            </div>
          </template>
          <el-table :data="myOutings" stripe size="small">
            <el-table-column prop="outing_date" label="日期" width="110" />
            <el-table-column prop="start_time" label="外出时间" width="90" />
            <el-table-column prop="reason" label="外出事由" show-overflow-tooltip />
            <el-table-column prop="expected_return" label="预计返回" width="120" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 我的补签申请 -->
      <el-tab-pane label="我的补签" name="makeup">
        <el-card shadow="never">
          <template #header>
            <div class="records-header">
              <span>补签申请记录</span>
              <el-button type="primary" plain size="small" @click="openMakeupDialog">发起补签</el-button>
            </div>
          </template>
          <el-radio-group v-model="makeupDayFilter" size="small" style="margin-bottom:12px">
            <el-radio-button value="today">今天</el-radio-button>
            <el-radio-button value="yesterday">昨天</el-radio-button>
            <el-radio-button value="dayBefore">前天</el-radio-button>
            <el-radio-button value="all">全部</el-radio-button>
          </el-radio-group>
          <el-table :data="filteredMakeups" stripe>
            <el-table-column prop="attendance_date" label="补签日期" width="110" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                {{ row.type === 1 ? '补签到' : row.type === 2 ? '补签退' : '签+退' }}
              </template>
            </el-table-column>
            <el-table-column label="申请时间" width="150">
              <template #default="{ row }">{{ row.created_at?.slice(0, 16).replace('T', ' ') }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" :type="row.current_stage === 2 ? 'warning' : 'primary'" size="small">
                  {{ row.current_stage === 2 ? '人事终审中' : '主管初审中' }}
                </el-tag>
                <el-tag v-else :type="makeupStatusMap(row.status).type" size="small">{{ makeupStatusMap(row.status).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审批意见" show-overflow-tooltip>
              <template #default="{ row }">{{ row.approve_remark || row.first_approve_remark || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openMakeupChart(row)">浏览</el-button>
                <el-button v-if="row.status === 2" type="primary" link size="small" @click="openMakeupDialog(row)">重新提交</el-button>
                <el-button v-if="row.status === 0" type="danger" link size="small" @click="withdrawMakeup(row)">撤回</el-button>
                <span v-if="row.status === 1" style="color:#c0c4cc">--</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 我的申诉 -->
      <el-tab-pane label="我的申诉" name="appeal">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
              <span>我的异常申诉记录</span>
              <el-radio-group v-model="appealDayFilter" size="small">
                <el-radio-button value="today">今天</el-radio-button>
                <el-radio-button value="yesterday">昨天</el-radio-button>
                <el-radio-button value="dayBefore">前天</el-radio-button>
                <el-radio-button value="all">全部</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <el-table :data="filteredAppeals" stripe>
            <el-table-column prop="record_date" label="申诉日期" width="110" />
            <el-table-column prop="record_status_text" label="原状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusTextTag(row.record_status_text).type" size="small">{{ row.record_status_text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="申诉理由" show-overflow-tooltip />
            <el-table-column label="申请时间" width="150">
              <template #default="{ row }">{{ row.created_at?.slice(0, 16).replace('T', ' ') }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="appealStatusMap(row.status).type" size="small">{{ appealStatusMap(row.status).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approve_remark" label="审批意见" show-overflow-tooltip />
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openAppealChart(row)">浏览</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 我的统计 -->
      <el-tab-pane label="我的统计" name="my-stat">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
              <span>我的出勤统计</span>
              <el-date-picker v-model="myStatMonth" type="month" value-format="YYYY-MM" placeholder="选择月份" size="small" style="width:140px" @change="loadMyStat" />
            </div>
          </template>
          <div class="stat-cards" v-loading="myStatLoading">
            <div class="stat-card"><div class="stat-label">应出勤（天）</div><div class="stat-value">{{ myStat?.expectedDays ?? 0 }}</div></div>
            <div class="stat-card"><div class="stat-label">有记录（天）</div><div class="stat-value">{{ myStat?.totalDays ?? 0 }}</div></div>
            <div class="stat-card"><div class="stat-label">出勤率</div><div class="stat-value">{{ myStat?.attendanceRate ?? 0 }}%</div></div>
            <div class="stat-card"><div class="stat-label">异常（次）</div><div class="stat-value warn">{{ myStat?.abnormalDays ?? 0 }}</div></div>
          </div>
          <el-table :data="myStatRows" stripe style="margin-top:14px">
            <el-table-column prop="name" label="项目" width="160" />
            <el-table-column prop="days" label="天数" width="120" />
            <el-table-column prop="tip" label="说明" show-overflow-tooltip />
          </el-table>
          <div class="stat-tip">统计口径：出勤率 = 当月有出勤记录的天数 ÷ 应出勤天数（工作日扣除节假日）；异常次数为迟到 + 早退 + 缺勤，同一天多项会重复计数。</div>
        </el-card>
      </el-tab-pane>

      <!-- 全部出勤（管理员） -->
      <el-tab-pane v-if="canViewAll" label="全部出勤" name="all">
        <el-card shadow="never">
          <template #header>
            <div class="records-header">
              <span>全员出勤记录</span>
              <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
                <el-radio-group v-model="quickDate" size="small" @change="onQuickDate">
                  <el-radio-button value="today">今天</el-radio-button>
                  <el-radio-button value="yesterday">昨天</el-radio-button>
                  <el-radio-button value="dayBeforeBefore">前天</el-radio-button>
                  <el-radio-button value="all">全部</el-radio-button>
                </el-radio-group>
                <el-date-picker v-model="allQuery.startDate" type="date" placeholder="开始日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:130px" />
                <el-date-picker v-model="allQuery.endDate" type="date" placeholder="结束日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:130px" />
                <el-select v-model="allQuery.status" placeholder="状态" clearable style="width:110px">
                  <el-option label="正常" :value="0" />
                  <el-option label="迟到" :value="1" />
                  <el-option label="早退" :value="2" />
                  <el-option label="缺勤" :value="3" />
                  <el-option label="严重迟到" :value="7" />
                  <el-option label="未签退异常" :value="8" />
                  <el-option label="严重早退" :value="9" />
                </el-select>
                <el-button type="primary" @click="loadAllRecords">查询</el-button>
                <el-button type="success" @click="exportExcel">导出Excel</el-button>
              </div>
            </div>
          </template>
          <el-table :data="allRecords" v-loading="allLoading" stripe>
            <el-table-column prop="real_name" label="姓名" width="90" />
            <el-table-column prop="attendance_date" label="日期" width="110" />
            <el-table-column label="签到" width="80">
              <template #default="{ row }">{{ fmtTime(row.check_in_time) }}</template>
            </el-table-column>
            <el-table-column label="签退" width="80">
              <template #default="{ row }">{{ fmtTime(row.check_out_time) }}</template>
            </el-table-column>
            <el-table-column label="工时" width="70">
              <template #default="{ row }">{{ row.work_hours ? row.work_hours + 'h' : '-' }}</template>
            </el-table-column>
            <el-table-column label="加班" width="70">
              <template #default="{ row }">{{ row.overtime_hours ? row.overtime_hours + 'h' : '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
          </el-table>
          <el-pagination
            v-model:current-page="allQuery.pageNum"
            v-model:page-size="allQuery.pageSize"
            :total="allTotal"
            layout="total, sizes, prev, next"
            style="margin-top:12px;justify-content:flex-end"
            @current-change="loadAllRecords"
          />
        </el-card>
      </el-tab-pane>

      <!-- 出勤统计（管理员） -->
      <el-tab-pane v-if="canViewStats" label="出勤统计" name="stats">
        <el-card shadow="never">
          <template #header>
            <div class="records-header">
              <span>出勤统计（应出勤 / 实际出勤 / 出勤率）</span>
              <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
                <el-select v-model="statsDeptId" placeholder="全部部门" style="width:140px" @change="loadStats">
                  <el-option label="全部部门" :value="0" />
                  <el-option v-for="o in orgs" :key="o.id" :label="o.name" :value="o.id" />
                </el-select>
                <el-date-picker v-model="statsMonth" type="month" placeholder="选择月份" format="YYYY-MM" value-format="YYYY-MM" @change="loadStats" style="width:130px" />
                <el-button type="success" @click="exportStats">导出Excel</el-button>
              </div>
            </div>
          </template>
          <el-table :data="statRows" v-loading="statLoading" stripe>
            <el-table-column prop="real_name" label="姓名" width="110" />
            <el-table-column prop="expected_days" label="应出勤" width="80" align="center" />
            <el-table-column prop="total_days" label="实际出勤" width="80" align="center" />
            <el-table-column prop="normal_days" label="正常" width="70" align="center" />
            <el-table-column prop="late_days" label="迟到" width="70" align="center" />
            <el-table-column prop="early_days" label="早退" width="70" align="center" />
            <el-table-column prop="absent_days" label="缺勤" width="70" align="center" />
            <el-table-column label="出勤率" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.attendance_rate >= 90 ? 'success' : row.attendance_rate >= 75 ? 'warning' : 'danger'" size="small">{{ row.attendance_rate }}%</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div style="color:#909399;font-size:12px;margin-top:8px">
            说明：应出勤=当月工作日数（扣除周末与法定假日，调休工作日计入）；实际出勤=有打卡记录的日期数；出勤率=实际出勤÷应出勤×100%。
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 补签审批（管理员） -->
      <el-tab-pane v-if="canReviewMakeup" label="补签审批" name="makeup-approve">
        <el-card shadow="never">
          <template #header><span>待审批补签申请</span></template>
          <el-table :data="pendingMakeups" v-loading="pendingLoading" stripe>
            <el-table-column prop="real_name" label="申请人" width="100" />
            <el-table-column prop="attendance_date" label="补签日期" width="110" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                {{ row.type === 1 ? '补签到' : row.type === 2 ? '补签退' : '签+退' }}
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" show-overflow-tooltip />
            <el-table-column label="申请时间" width="150">
              <template #default="{ row }">{{ row.created_at?.slice(0, 16).replace('T', ' ') }}</template>
            </el-table-column>
            <el-table-column label="审批阶段" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.current_stage === 2 ? 'warning' : 'primary'">
                  {{ row.current_stage === 2 ? '人事终审' : '主管初审' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="doApprove(row, 1)">
                  {{ row.current_stage === 2 || canFinalMakeup ? '通过' : '通过送人事' }}
                </el-button>
                <el-button type="danger" size="small" @click="doApprove(row, 2)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 申诉审核（管理员 / 人事 / 部门主管） -->
      <el-tab-pane v-if="canReviewAppeal" label="申诉审核" name="appeal-approve">
        <el-card shadow="never">
          <template #header><span>待审核异常申诉</span></template>
          <el-table :data="pendingAppeals" v-loading="appealLoading" stripe>
            <el-table-column prop="real_name" label="申请人" width="100" />
            <el-table-column prop="record_date" label="申诉日期" width="110" />
            <el-table-column prop="record_status_text" label="原状态" width="90" />
            <el-table-column prop="reason" label="申诉理由" show-overflow-tooltip />
            <el-table-column label="申请时间" width="150">
              <template #default="{ row }">{{ row.created_at?.slice(0, 16).replace('T', ' ') }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="doAppealApprove(row, 1)">通过</el-button>
                <el-button type="danger" size="small" @click="doAppealApprove(row, 2)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 管理员：出勤配置 -->
      <el-tab-pane v-if="canConfig" label="出勤配置" name="config">
        <div style="display:flex;gap:16px;align-items:flex-start">
          <!-- 左：班次模板 -->
          <el-card shadow="never" style="width:55%;flex-shrink:0">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span>班次模板</span>
                <el-button type="primary" size="small" @click="editTemplate(null)">新建模板</el-button>
              </div>
            </template>
            <el-table :data="shiftTemplates" stripe size="small" max-height="500">
              <el-table-column label="模板名称" width="150">
                <template #default="{ row }">
                  <span>{{ row.template_name }}</span>
                  <el-tag v-if="row.is_all_day" type="warning" size="small" style="margin-left:4px">全天</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="工作时间" width="110">
                <template #default="{ row }">{{ row.work_start?.slice(0,5) }} - {{ row.work_end?.slice(0,5) }}</template>
              </el-table-column>
              <el-table-column label="类型" width="60">
                <template #default="{ row }">{{ row.shift_type === 'night' ? '夜班' : row.shift_type === 'flex' ? '弹性' : '白班' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="60" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.is_active" type="success" size="small">使用中</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="185" align="center">
                <template #default="{ row }">
                  <div style="white-space:nowrap">
                    <el-button v-if="!row.is_active" type="primary" link size="small" @click="activateTemplate(row)">启用</el-button>
                    <el-button size="small" link @click="editTemplate(row)">编辑</el-button>
                    <el-button v-if="!row.is_active" type="danger" link size="small" @click="delTemplate(row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- 右：自动排班规则 -->
          <el-card shadow="never" style="width:45%;flex-shrink:0">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span>自动排班规则</span>
                <el-button type="primary" size="small" @click="editSchedule(null)">新建规则</el-button>
              </div>
            </template>
            <el-table :data="schedules" stripe size="small" max-height="500">
              <el-table-column prop="schedule_name" label="规则" width="90" show-overflow-tooltip />
              <el-table-column label="触发条件" width="105">
                <template #default="{ row }">
                  <span v-if="row.schedule_type==='once'">{{ row.schedule_date }}</span>
                  <span v-else-if="row.schedule_type==='weekly'">每周{{ ['日','一','二','三','四','五','六'][row.week_day] }}</span>
                  <span v-else-if="row.schedule_type==='yearly'">每年{{ row.yearly_month }}月{{ row.yearly_day }}日</span>
                <span v-else-if="row.schedule_type==='lunar'">农历{{ row.lunar_month }}月{{ row.lunar_day }}</span>
                <span v-else>{{ row.start_date?.slice(5) }}~{{ row.end_date?.slice(5) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="时间" width="75">
                <template #default="{ row }">{{ row.work_start?.slice(0,5) }}-{{ row.work_end?.slice(0,5) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center">
                <template #default="{ row }">
                  <div style="white-space:nowrap">
                    <el-button size="small" link @click="editSchedule(row)">编辑</el-button>
                    <el-button type="danger" size="small" link @click="delSchedule(row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <!-- 节假日维护 -->
        <el-card shadow="never" style="margin-top:16px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>节假日维护（法定假日不计应出勤，调休工作日正常考勤）</span>
              <el-button type="primary" size="small" @click="openHoliday">新增节假日</el-button>
            </div>
          </template>
          <el-table :data="holidays" stripe size="small" max-height="300">
            <el-table-column prop="holiday_date" label="日期" width="120" />
            <el-table-column prop="holiday_name" label="名称" width="160" show-overflow-tooltip />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="row.holiday_type === 1 ? 'danger' : 'success'" size="small">{{ row.holiday_type === 1 ? '法定假日' : '调休工作日' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="说明" min-width="200">
              <template #default="{ row }">
                <span style="color:#909399;font-size:12px">{{ row.holiday_type === 1 ? '当天无需打卡' : '当天需要正常打卡（调休上班）' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-button type="danger" link size="small" @click="delHoliday(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 地理位置校验（13.1.10） -->
        <el-card shadow="never" style="margin-top:16px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>地理位置校验</span>
              <span>
                <el-button type="primary" size="small" @click="submitGeoConfig">保存配置</el-button>
                <el-button size="small" @click="loadOperationLogs(); opLogDialog = true">出勤操作日志</el-button>
              </span>
            </div>
          </template>
          <div style="display:flex;gap:16px;flex-wrap:wrap;align-items:center">
            <el-switch v-model="geoForm.enable" active-text="开启签到位置校验" inactive-text="关闭" style="margin-right:8px" />
            <span style="color:#909399;font-size:13px">公司纬度</span>
            <el-input v-model="geoForm.lat" style="width:130px" placeholder="如 23.1320" />
            <span style="color:#909399;font-size:13px">公司经度</span>
            <el-input v-model="geoForm.lng" style="width:130px" placeholder="如 113.3220" />
            <span style="color:#909399;font-size:13px">允许半径(米)</span>
            <el-input-number v-model="geoForm.radius" :min="100" :max="100000" :step="100" style="width:150px" />
            <el-switch v-model="geoForm.weekendCount" active-text="周末计入出勤" inactive-text="周末不计出勤" style="margin-left:16px" />
          </div>
          <div style="color:#909399;font-size:12px;margin-top:8px">
            开启后，员工签到/签退需在公司坐标半径内；超出范围或无法定位时需本人确认强制打卡，强制打卡的记录进入下方"异地签到审核"待管理员处理。未配置公司坐标时不校验。
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 管理员：异地签到审核 -->
      <el-tab-pane v-if="canReviewGeo" label="异地签到审核" name="geo">
        <el-card shadow="never">
          <template #header><span>异地签到审核（超出范围/无法定位强制打卡的记录）</span></template>
          <el-table :data="geoAbnormalList" stripe size="small" empty-text="暂无待审核的异地签到记录">
            <el-table-column prop="real_name" label="姓名" width="100" />
            <el-table-column prop="attendance_date" label="日期" width="110" />
            <el-table-column label="签到时间" width="150">
              <template #default="{ row }">{{ row.check_in_time ? String(row.check_in_time).replace('T', '  ').slice(0, 19) : '-' }}</template>
            </el-table-column>
            <el-table-column label="定位" min-width="200">
              <template #default="{ row }">
                <span v-if="row.latitude">{{ row.latitude }}, {{ row.longitude }}</span>
                <span v-else style="color:#E6A23C">未获取到定位</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="180" align="center">
              <template #default="{ row }">
                <div style="white-space:nowrap">
                  <el-button type="success" size="small" link @click="geoApprove(row)">确认正常</el-button>
                  <el-button type="danger" size="small" link @click="geoReject(row)">驳回记缺勤</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-dialog v-model="tplDialog" :title="tplForm.id ? '编辑模板' : '新建模板'" width="450px">
        <el-form :model="tplForm" label-width="100px">
          <el-form-item label="模板名称"><el-input v-model="tplForm.template_name" /></el-form-item>
          <el-form-item label="上班时间"><el-time-picker v-model="tplForm.work_start" format="HH:mm" value-format="HH:mm" :disabled="tplForm.is_all_day" /></el-form-item>
          <el-form-item label="下班时间"><el-time-picker v-model="tplForm.work_end" format="HH:mm" value-format="HH:mm" :disabled="tplForm.is_all_day" /></el-form-item>
          <el-form-item label="迟到线"><el-time-picker v-model="tplForm.late_threshold" format="HH:mm" value-format="HH:mm" :disabled="tplForm.is_all_day" /></el-form-item>
          <el-form-item label="早退线"><el-time-picker v-model="tplForm.early_leave_threshold" format="HH:mm" value-format="HH:mm" :disabled="tplForm.is_all_day" /></el-form-item>
          <el-form-item label="全天可打卡">
            <el-switch v-model="tplForm.is_all_day" />
            <span style="margin-left:8px;color:#909399;font-size:12px">勾选后任何时间都可签到/签退，不判迟到早退</span>
          </el-form-item>
          <el-form-item label="班次类型">
            <el-select v-model="tplForm.shift_type">
              <el-option label="白班" value="day" />
              <el-option label="夜班" value="night" />
              <el-option label="弹性" value="flex" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="tplDialog = false">取消</el-button>
          <el-button type="primary" @click="saveTemplate">保存</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="schedDialog" :title="schedForm.id ? '编辑规则' : '新建规则'" width="500px">
        <el-form :model="schedForm" label-width="100px">
          <el-form-item label="规则名称"><el-input v-model="schedForm.schedule_name" placeholder="如：中秋当天、每周三早下班" /></el-form-item>
          <el-form-item label="触发类型">
            <el-select v-model="schedForm.schedule_type" @change="schedForm.week_day=null;schedForm.schedule_date=null;schedForm.start_date=null;schedForm.end_date=null">
              <el-option label="指定日期" value="once" />
              <el-option label="每周重复" value="weekly" />
              <el-option label="日期范围" value="range" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="schedForm.schedule_type==='once'" label="日期">
            <el-date-picker v-model="schedForm.schedule_date" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item v-if="schedForm.schedule_type==='weekly'" label="星期几">
            <el-select v-model="schedForm.week_day">
              <el-option v-for="(d,i) in ['日','一','二','三','四','五','六']" :key="i" :label="'周'+d" :value="i" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="schedForm.schedule_type==='range'" label="日期范围">
            <el-date-picker v-model="schedForm.start_date" type="date" value-format="YYYY-MM-DD" /> ~
            <el-date-picker v-model="schedForm.end_date" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item v-if="schedForm.schedule_type==='yearly'" label="每年日期">
            <el-select v-model="schedForm.yearly_month" style="width:90px"><el-option v-for="n in 12" :key="n" :label="n+'月'" :value="n" /></el-select>
            <el-select v-model="schedForm.yearly_day" style="width:80px;margin-left:8px"><el-option v-for="n in 31" :key="n" :label="n+'日'" :value="n" /></el-select>
          </el-form-item>
          <el-form-item v-if="schedForm.schedule_type==='lunar'" label="农历日期">
            <el-select v-model="schedForm.lunar_month" style="width:90px">
              <el-option v-for="n in 12" :key="n" :label="n+'月'" :value="n" />
            </el-select>
            <el-select v-model="schedForm.lunar_day" style="width:80px;margin-left:8px">
              <el-option v-for="n in 30" :key="n" :label="n+'日'" :value="n" />
            </el-select>
            <div style="margin-left:8px;color:#909399;font-size:12px">如中秋=农历8月15，春节=农历1月1</div>
          </el-form-item>
          <el-form-item label="上班时间"><el-time-picker v-model="schedForm.work_start" format="HH:mm" value-format="HH:mm" /></el-form-item>
          <el-form-item label="下班时间"><el-time-picker v-model="schedForm.work_end" format="HH:mm" value-format="HH:mm" /></el-form-item>
          <el-form-item label="迟到线"><el-time-picker v-model="schedForm.late_threshold" format="HH:mm" value-format="HH:mm" /></el-form-item>
          <el-form-item label="早退线"><el-time-picker v-model="schedForm.early_leave_threshold" format="HH:mm" value-format="HH:mm" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="schedDialog = false">取消</el-button>
          <el-button type="primary" @click="saveSchedule">保存</el-button>
        </template>
      </el-dialog>
    </el-tabs>

    <!-- 补签弹窗 -->
    <el-dialog v-model="makeupDialog" title="发起补签申请" width="450px">
      <el-form :model="makeupForm" label-width="80px">
        <el-form-item label="补签日期">
          <el-date-picker v-model="makeupForm.attendance_date" type="date" placeholder="选择日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" :disabled-date="(d: Date) => d.getTime() > Date.now() || d.getTime() < Date.now() - 30*24*60*60*1000" />
        </el-form-item>
        <el-form-item label="补签类型">
          <el-radio-group v-model="makeupForm.type">
            <el-radio :value="1">补签到</el-radio>
            <el-radio :value="2">补签退</el-radio>
            <el-radio :value="3">两者都补</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="申请签到时间" v-if="makeupForm.type === 1 || makeupForm.type === 3">
          <el-date-picker v-model="makeupForm.check_in_time" type="datetime" placeholder="选择签到日期时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm" style="width:100%" :disabled-date="(d: Date) => d.getTime() > Date.now()" />
        </el-form-item>
        <el-form-item label="申请签退时间" v-if="makeupForm.type === 2 || makeupForm.type === 3">
          <el-date-picker v-model="makeupForm.check_out_time" type="datetime" placeholder="选择签退日期时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm" style="width:100%" :disabled-date="(d: Date) => d.getTime() > Date.now()" />
        </el-form-item>
        <el-form-item label="补签原因">
          <el-input v-model="makeupForm.reason" type="textarea" :rows="3" placeholder="请说明漏签原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="makeupDialog = false">取消</el-button>
        <el-button type="primary" @click="submitMakeup">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 异常申诉弹窗 -->
    <el-dialog v-model="appealDialog" title="发起异常申诉" width="520px">
      <el-form label-width="90px">
        <el-form-item label="申诉理由">
          <el-input v-model="appealForm.reason" type="textarea" :rows="4" placeholder="请说明异常原因（如堵车、临时会议等）" />
        </el-form-item>
        <el-form-item label="证明材料">
          <div class="upload-wrap" @paste="onAppealPaste" @mouseenter="onAppealHover" tabindex="0" ref="appealUploadRef" style="width:100%;border:1px dashed #dcdfe6;border-radius:6px;padding:12px;cursor:pointer;outline:none">
            <el-upload multiple :auto-upload="false" :on-change="onAppealFileChange" :on-remove="onAppealFileRemove" :file-list="appealFileList" drag style="width:100%">
              <div style="font-size:13px;color:#909399;padding:4px 0">点击 / 拖拽选择附件，或直接 Ctrl+V 粘贴图片（最多 10 个，单个≤200MB）</div>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>

    <!-- 节假日弹窗 -->
    <el-dialog v-model="holidayDialog" title="新增节假日" width="450px">
      <el-form :model="holidayForm" label-width="90px">
        <el-form-item label="日期">
          <el-date-picker v-model="holidayForm.holiday_date" type="date" placeholder="选择日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="holidayForm.holiday_name" placeholder="如：国庆节、中秋调休上班" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="holidayForm.holiday_type">
            <el-radio :value="1">法定假日（无需打卡）</el-radio>
            <el-radio :value="2">调休工作日（正常打卡）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="holidayDialog = false">取消</el-button>
        <el-button type="primary" @click="saveHoliday">保存</el-button>
      </template>
    </el-dialog>

    <!-- 出勤操作日志弹窗（管理员查看重新签到/签退、导出记录） -->
    <el-dialog v-model="opLogDialog" title="出勤操作日志" width="760px">
      <el-table :data="opLogs" v-loading="opLogLoading" stripe size="small" max-height="480">
        <el-table-column label="时间" width="165">
          <template #default="{ row }">{{ row.created_at ? String(row.created_at).replace('T', '  ').slice(0, 19) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="100">
          <template #default="{ row }">{{ row.real_name || row.username || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.action === 'export' ? 'primary' : 'warning'">{{ row.action === 'export' ? '导出报表' : row.action === 'recheck_in' ? '重新签到' : row.action === 'recheck_out' ? '重新签退' : row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="request_params" label="原时间/参数" min-width="200" show-overflow-tooltip />
        <el-table-column prop="response_result" label="结果" min-width="120" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <!-- 外出打卡弹窗 -->
    <el-dialog v-model="outingDialog" title="外出打卡" width="420">
      <el-form label-width="90px">
        <el-form-item label="外出事由">
          <el-input v-model="outingForm.reason" placeholder="如：外出办事 / 拜访客户 / 外出采购" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="预计返回">
          <el-input v-model="outingForm.expected_return" placeholder="如：12:30 或 下午3点（选填）" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outingDialog = false">取消</el-button>
        <el-button type="primary" @click="submitOuting">提交</el-button>
      </template>
    </el-dialog>

    <!-- 流程图浏览弹窗：补签 / 申诉 -->
    <el-dialog v-model="chartVisible" :title="chartTitle" width="460px">
      <div class="chart-scroll">
        <FlowChart :nodes="chartNodes" :creator="chartCreator" creator-role="申请人" end-text="流程结束" />
      </div>
      <template #footer>
        <el-button @click="chartVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.attendance-page { padding: 16px; }
.chart-scroll { max-height: 60vh; overflow: auto; padding: 4px 2px; }
.clock-card { margin-bottom: 16px; }
.clock-inner { display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.clock-left { display: flex; align-items: center; gap: 12px; }
.work-time { font-size: 14px; color: #606266; }
.today-date { font-size: 13px; color: #909399; margin-top: 4px; }
.clock-center { display: flex; align-items: center; gap: 32px; }
.time-block { text-align: center; }
.time-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.time-value { font-size: 32px; font-weight: 600; font-variant-numeric: tabular-nums; color: #303133; }
.time-value.late { color: #E6A23C; }
.time-value.early { color: #F56C6C; }
.time-divider { width: 1px; height: 50px; background: #EBEEF5; }
.clock-right { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.records-header { display: flex; justify-content: space-between; align-items: center; }
.stat-cards { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.stat-card { border: 1px solid #EBEEF5; border-radius: 8px; padding: 14px 16px; }
.stat-label { font-size: 13px; color: #909399; }
.stat-value { font-size: 24px; font-weight: 600; color: #303133; margin-top: 6px; font-variant-numeric: tabular-nums; }
.stat-value.warn { color: #E6A23C; }
.stat-tip { margin-top: 12px; font-size: 12px; color: #909399; line-height: 1.7; }
</style>
