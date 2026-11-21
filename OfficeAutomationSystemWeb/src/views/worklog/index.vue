<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Download, View, Edit, Delete, ChatDotRound } from '@element-plus/icons-vue'
import { hasPerm, hasAnyPerm } from '@/utils/permission'
import { useUserStore } from '@/store/user'
import { orgTreeEnabled, pageUsers, listGroups, type OrgNode, type GroupItem } from '@/api/user'
import {
    myWorklogs, createWorklog, updateWorklog, deleteWorklog, myWorklogDetail, viewWorklogs, viewWorklogDetail,
    listComments, addComment, deleteComment, adminWorklogs, recycleWorklogs, adminDeleteWorklog,
    restoreWorklog, listCategories, listAllCategories, addCategory, updateCategory, deleteCategory,
    grantPage, addGrant, deleteGrant, myStats, deptStats, exportMine, exportAll,
    uploadWorklogAttachment, worklogAttachmentDownloadUrl, downloadWorklogAttachment, supplementGrant, supplementApply, supplementPending, supplementApprove,
    type WorklogItem, type WorklogComment, type WorklogCategory, type WorklogGrant, type WorklogStats, type PageResult
} from '@/api/worklog'

// 权限
const userStore = useUserStore()
// 日志范围：worklog:view-all 看全公司，worklog:view-dept 看本部门
const isAdminManager = computed(() => hasAnyPerm(['worklog:view-all', 'worklog:view-dept']))
// 只有能看全公司的角色才需要部门下拉
const canPickOrg = computed(() => hasPerm('worklog:view-all'))
const isWorklogAdmin = computed(() => (userStore.roles || []).includes('admin'))  // 仅系统管理员可做日志管理（删除/恢复/补写授权）
// 补写审批：管理员审全部，部门主管只审本部门和下级
const canApproveSupplement = computed(() => isWorklogAdmin.value || hasPerm('worklog:view-dept'))
const isCategoryManager = computed(() => hasPerm('worklog:category:manage'))
const isPermissionManager = computed(() => hasPerm('worklog:permission-config'))
const isExporter = computed(() => hasPerm('worklog:export'))

// 标签
const activeTab = ref('my')
const applyVisible = ref(false)
const applyForm = reactive({ range: null as string[] | null, reason: '' })
const pendingVisible = ref(false)
const pendingList = ref<any[]>([])
async function submitApply() {
  if (!applyForm.range || applyForm.range.length !== 2) return ElMessage.warning('请选择日期范围')
  try {
    await supplementApply({ start_date: applyForm.range[0] as string, end_date: applyForm.range[1] as string, reason: applyForm.reason })
    ElMessage.success('申请已提交')
    applyVisible.value = false
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '提交失败') }
}
async function openPending() {
  pendingVisible.value = true
  try { const r: any = await supplementPending(); pendingList.value = r.data || [] } catch {}
}
async function doApprove(row: any, pass: boolean) {
  try {
    await supplementApprove(row.id, pass)
    ElMessage.success(pass ? '已通过' : '已驳回')
    openPending()
  } catch (e: any) { ElMessage.error('操作失败') }
}
const supplementVisible = ref(false)
const supplementForm = reactive({ user_id: null as number | null, range: null as string[] | null, days: 7, reason: '' })
const allUsers = ref<any[]>([])
async function openSupplementGrant() {
  supplementVisible.value = true
  if (allUsers.value.length === 0) {
    try { const r: any = await pageUsers({ pageNum: 1, pageSize: 200 }); allUsers.value = r.records || [] } catch {}
  }
}
async function submitSupplement() {
  if (!supplementForm.user_id) return ElMessage.warning('请选择员工')
  if (!supplementForm.range || supplementForm.range.length !== 2) return ElMessage.warning('请选择日期范围')
  try {
    await supplementGrant({ user_id: supplementForm.user_id, start_date: supplementForm.range[0] as string, end_date: supplementForm.range[1] as string, days: supplementForm.days, reason: supplementForm.reason })
    ElMessage.success('授权成功')
    supplementVisible.value = false
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '授权失败') }
}

// 通用
function formatTime(time?: string) {
    if (!time) return '-'
    const d = new Date(time)
    const p = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const statusText = (s: number) => (s === 0 ? '草稿' : s === 1 ? '已提交' : s === 2 ? '已撤回' : '-')

// 我的日志
const myLoading = ref(false)
const myList = ref<WorklogItem[]>([])
const myTotal = ref(0)
const stats = ref<WorklogStats>({ myMonthCount: 0, myMonthDays: 0, myViewCount: 0, myCommentCount: 0, myCompletionRate: 0, timeDist: [], wordStats: { totalWords: 0, avgWords: 0 }, commentTop: [], deptRank: [] })
const categories = ref<WorklogCategory[]>([])
const myQuery = reactive({ pageNum: 1, pageSize: 10, logDate: '', categoryId: null as number | null, keyword: '' })
const myDayFilter = ref('all')
function onMyDayFilter(v: string) {
  if (v === 'all') { myQuery.logDate = '' }
  else {
    const d = new Date()
    d.setDate(d.getDate() - (v === 'today' ? 0 : v === 'yesterday' ? 1 : 2))
    myQuery.logDate = d.toISOString().slice(0, 10)
  }
  myQuery.pageNum = 1
  reloadMy()
}

async function loadCategories() {
    try { categories.value = await listCategories() } catch { /* 忽略 */ }
}

async function loadMyStats() {
    try { stats.value = await myStats() } catch { /* 忽略 */ }
}

async function loadMy() {
    myLoading.value = true
    try {
        const res = await myWorklogs(myQuery)
        myList.value = res.records
        myTotal.value = res.total
    } catch { /* 拦截器已提示 */ } finally { myLoading.value = false }
}

function reloadMy() {
    myQuery.pageNum = 1
    loadMy()
    loadMyStats()
}

// 新增 / 编辑
const formVisible = ref(false)
const formTitle = ref('撰写日志')
const form = reactive({
    id: null as number | null,
    log_date: '',
    category_id: null as number | null,
    title: '',
    content: '',
    work_summary: '',
    next_plan: ''
})

// 附件：每篇一个，不超过 20MB，格式走白名单
const formAttachment = ref<{ id: number; fileName: string; fileSize: number } | null>(null)
const uploading = ref(false)
const attInputRef = ref<HTMLInputElement | null>(null)

function formatSize(bytes?: number) {
    if (!bytes) return ''
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function distPercent(count: number) {
    const total = (stats.value.timeDist || []).reduce((s, d) => s + (d.count || 0), 0)
    return total > 0 ? Math.max((count / total) * 100, 2) : 0
}

function rankPercent(count: number) {
    const max = Math.max(...(stats.value.deptRank || []).map(r => r.count), 1)
    return max > 0 ? Math.max((count / max) * 100, 4) : 0
}

async function handleAttachmentFile(file: File) {
    if (formAttachment.value) return ElMessage.warning('每篇日志最多1个附件，请先删除当前附件')
    if (file.size > 20 * 1024 * 1024) return ElMessage.warning('文件大小超过20MB，请压缩后上传')
    uploading.value = true
    try {
        const res = await uploadWorklogAttachment(file)
        formAttachment.value = { id: res.id, fileName: res.fileName, fileSize: res.fileSize }
        ElMessage.success('附件上传成功')
    } catch { /* 拦截器已提示 */ } finally { uploading.value = false }
}

function onAttPick(e: Event) {
    const f = (e.target as HTMLInputElement).files?.[0]
    if (f) handleAttachmentFile(f)
    ;(e.target as HTMLInputElement).value = ''
}

function onAttDrop(e: DragEvent) {
    e.preventDefault()
    const f = e.dataTransfer?.files?.[0]
    if (f) handleAttachmentFile(f)
}

// 鼠标悬停就聚焦文件框，粘贴不用先点
function onAttHover() {
    if (!formAttachment.value && !uploading.value) attInputRef.value?.focus()
}

function removeFormAttachment() {
    formAttachment.value = null
}

async function downloadAtt(row: Partial<WorklogItem>) {
    if (!row.attachment_id) return
    try {
        const blob = await downloadWorklogAttachment(row.attachment_id)
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = row.attachment_file_name || '附件'
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(url)
    } catch (e: any) {
        ElMessage.error('下载失败：' + (e?.response?.data?.message || '请稍后重试'))
    }
}

function openCreate() {
    formTitle.value = '撰写日志'
    Object.assign(form, { id: null, log_date: new Date().toISOString().slice(0, 10), category_id: null, title: '', content: '', work_summary: '', next_plan: '' })
    formAttachment.value = null
    formVisible.value = true
}

function openEdit(row: WorklogItem) {
    formTitle.value = '编辑日志'
    Object.assign(form, { id: row.id, log_date: row.log_date, category_id: row.category_id, title: row.title, content: row.content || '', work_summary: row.work_summary || '', next_plan: row.next_plan || '' })
    formAttachment.value = row.attachment_id ? { id: row.attachment_id, fileName: row.attachment_file_name || '附件', fileSize: row.attachment_file_size || 0 } : null
    formVisible.value = true
}

async function saveForm() {
    if (!form.title.trim()) return ElMessage.warning('请填写日志标题')
    if (!form.content.trim()) return ElMessage.warning('请填写日志内容')
    const payload = { ...form, id: form.id ?? undefined, attachment_id: formAttachment.value?.id ?? null } as Partial<WorklogItem>
    if (form.id) {
        await updateWorklog(payload)
        ElMessage.success('已保存')
    } else {
        await createWorklog(payload)
        ElMessage.success('已提交')
    }
    formVisible.value = false
    reloadMy()
}

async function removeMy(row: WorklogItem) {
    await ElMessageBox.confirm('确定删除这篇日志？当天删除可恢复，历史删除需管理员恢复。', '提示', { type: 'warning' })
    await deleteWorklog(row.id)
    ElMessage.success('已删除')
    reloadMy()
}

// 我的日志详情
const myDetailVisible = ref(false)
const myDetail = ref<Partial<WorklogItem>>({})
async function openMyDetail(row: WorklogItem) {
    myDetail.value = await myWorklogDetail(row.id)
    myDetailVisible.value = true
}

// 导出我的日志
async function doExportMine() {
    const blob = await exportMine({ keyword: myQuery.keyword, categoryId: myQuery.categoryId })
    downloadBlob(blob, '我的工作日志.xlsx')
}

function downloadBlob(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
}

// 日志查看
const viewLoading = ref(false)
const viewList = ref<WorklogItem[]>([])
const viewTotal = ref(0)
const orgTreeData = ref<OrgNode[]>([])
const viewQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '', categoryId: null as number | null, orgId: null as number | null })

async function loadOrgs() {
    try { orgTreeData.value = await orgTreeEnabled() } catch { /* 忽略 */ }
}

async function loadView() {
    viewLoading.value = true
    try {
        const res = await viewWorklogs(viewQuery)
        viewList.value = res.records
        viewTotal.value = res.total
    } catch { /* 拦截器已提示 */ } finally { viewLoading.value = false }
}

function reloadView() {
    viewQuery.pageNum = 1
    loadView()
}

// 查看详情 + 评论
const detailVisible = ref(false)
const detail = ref<Partial<WorklogItem>>({})
const comments = ref<WorklogComment[]>([])
const commentText = ref('')
const commenting = ref(false)

async function openDetail(row: WorklogItem) {
    detail.value = await viewWorklogDetail(row.id)
    comments.value = await listComments(row.id)
    detailVisible.value = true
}

async function sendComment() {
    if (!commentText.value.trim()) return ElMessage.warning('请输入评论内容')
    commenting.value = true
    try {
        await addComment(detail.value.id!, commentText.value.trim())
        commentText.value = ''
        ElMessage.success('评论成功')
        comments.value = await listComments(detail.value.id!)
        detail.value.comment_count = (detail.value.comment_count || 0) + 1
    } finally { commenting.value = false }
}

async function removeComment(c: WorklogComment) {
    await ElMessageBox.confirm('确定删除这条评论？', '提示', { type: 'warning' })
    await deleteComment(c.id)
    ElMessage.success('已删除')
    comments.value = await listComments(detail.value.id!)
    detail.value.comment_count = Math.max((detail.value.comment_count || 0) - 1, 0)
}

// 日志管理（管理员），mgrView 切换列表与回收站
const mgrView = ref('list')
const mgrLoading = ref(false)
const mgrList = ref<WorklogItem[]>([])
const mgrTotal = ref(0)
const mgrQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: null as number | null, categoryId: null as number | null, orgId: null as number | null, startDate: '', endDate: '' })

async function loadMgr() {
    mgrLoading.value = true
    try {
        const res = mgrView.value === 'list'
            ? await adminWorklogs(mgrQuery)
            : await recycleWorklogs({ pageNum: mgrQuery.pageNum, pageSize: mgrQuery.pageSize, keyword: mgrQuery.keyword })
        mgrList.value = res.records
        mgrTotal.value = res.total
    } catch { /* 拦截器已提示 */ } finally { mgrLoading.value = false }
}

function reloadMgr() {
    mgrQuery.pageNum = 1
    loadMgr()
}

async function removeAdmin(row: WorklogItem) {
    await ElMessageBox.confirm('确定删除该日志？（软删除，可到回收站恢复）', '提示', { type: 'warning' })
    await adminDeleteWorklog(row.id)
    ElMessage.success('已删除')
    reloadMgr()
}

async function removeForever(row: WorklogItem) {
    await ElMessageBox.confirm('确定永久删除该日志？此操作不可恢复！', '警告', { type: 'error' })
    await adminDeleteWorklog(row.id)
    ElMessage.success('已永久删除')
    reloadMgr()
}

async function doRestore(row: WorklogItem) {
    await restoreWorklog(row.id)
    ElMessage.success('已恢复')
    reloadMgr()
}

async function doExportAll() {
    const blob = await exportAll({ keyword: mgrQuery.keyword, categoryId: mgrQuery.categoryId, orgId: mgrQuery.orgId, startDate: mgrQuery.startDate, endDate: mgrQuery.endDate })
    downloadBlob(blob, '工作日志导出.xlsx')
}

// 类别管理（管理员）
const catLoading = ref(false)
const catList = ref<WorklogCategory[]>([])
async function loadCats() {
    catLoading.value = true
    try { catList.value = await listAllCategories() } finally { catLoading.value = false }
}

const catFormVisible = ref(false)
const catForm = reactive({ id: null as number | null, category_name: '', category_code: '', description: '', sort_order: 0, status: 1 })

function openCatCreate() {
    Object.assign(catForm, { id: null, category_name: '', category_code: '', description: '', sort_order: 0, status: 1 })
    catFormVisible.value = true
}

function openCatEdit(row: WorklogCategory) {
    Object.assign(catForm, { id: row.id, category_name: row.category_name, category_code: row.category_code || '', description: row.description || '', sort_order: row.sort_order, status: row.status })
    catFormVisible.value = true
}

async function saveCat() {
    if (!catForm.category_name.trim()) return ElMessage.warning('请填写类别名称')
    const catPayload = { ...catForm, id: catForm.id ?? undefined } as Partial<WorklogCategory>
    if (catForm.id) {
        await updateCategory(catPayload)
    } else {
        await addCategory(catPayload)
    }
    ElMessage.success('已保存')
    catFormVisible.value = false
    loadCats()
    loadCategories()
}

async function removeCat(row: WorklogCategory) {
    try {
        await ElMessageBox.confirm(`确定删除类别「${row.category_name}」？`, '提示', { type: 'warning' })
    } catch { return }
    await deleteCategory(row.id)
    ElMessage.success('已删除')
    loadCats()
    loadCategories()
}

// 查看权限（管理员）
const grantLoading = ref(false)
const grantList = ref<WorklogGrant[]>([])
const grantTotal = ref(0)
const grantQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const users = ref<Array<{ id: number; real_name: string; username: string }>>([])

async function loadUsers() {
    try {
        const res = await pageUsers({ pageNum: 1, pageSize: 200 })
        users.value = res.records.map(u => ({ id: u.id, real_name: u.real_name, username: u.username }))
    } catch { /* 忽略 */ }
}

async function loadGrants() {
    grantLoading.value = true
    try {
        const res = await grantPage(grantQuery)
        grantList.value = res.records
        grantTotal.value = res.total
    } catch { /* 拦截器已提示 */ } finally { grantLoading.value = false }
}

const grantFormVisible = ref(false)
const grantForm = reactive({
    target_id: null as number | null,
    permission_type: 1,
    viewer_id: null as number | null,
    group_id: null as number | null,
    dept_id: null as number | null
})

// 群组列表只在授权弹窗里用，进弹窗时再拉
const groupOptions = ref<GroupItem[]>([])

async function loadGroups() {
    try {
        groupOptions.value = await listGroups()
    } catch {
        groupOptions.value = []
    }
}

function openGrantCreate() {
    Object.assign(grantForm, { target_id: null, permission_type: 1, viewer_id: null, group_id: null, dept_id: null })
    grantFormVisible.value = true
    loadGroups()
}

async function saveGrant() {
    if (!grantForm.target_id) return ElMessage.warning('请选择被查看人')
    if (grantForm.permission_type === 1 && !grantForm.viewer_id) return ElMessage.warning('请选择查看人')
    if (grantForm.permission_type === 2 && !grantForm.group_id) return ElMessage.warning('请选择用户群组')
    if (grantForm.permission_type === 3 && !grantForm.dept_id) return ElMessage.warning('请选择部门')
    await addGrant({ ...grantForm } as Partial<WorklogGrant>)
    ElMessage.success('授权成功')
    grantFormVisible.value = false
    loadGrants()
}

async function removeGrant(row: WorklogGrant) {
    await ElMessageBox.confirm('确定撤销该授权？', '提示', { type: 'warning' })
    await deleteGrant(row.id)
    ElMessage.success('已撤销')
    loadGrants()
}

// 初始化
// 弹窗打开时全局粘贴即可上传附件（无需先点击上传框）
function onGlobalPaste(e: ClipboardEvent) {
    if (!formVisible.value) return
    const f = e.clipboardData?.files?.[0]
    if (f) handleAttachmentFile(f)
}

onMounted(() => {
    window.addEventListener('paste', onGlobalPaste)
    loadCategories()
    loadMy()
    loadMyStats()
    if (isAdminManager.value) loadView()
    // 部门树和用户列表只有按部门筛选与授权时用，普通员工不加载
    if (canPickOrg.value || isPermissionManager.value) loadOrgs()
    if (isPermissionManager.value) loadUsers()
    if (isWorklogAdmin.value) loadMgr()
    if (isCategoryManager.value) loadCats()
    if (isPermissionManager.value) loadGrants()
})
</script>

<template>
  <div class="worklog-page">
    <!-- 标签切换 -->
    <el-tabs v-model="activeTab" @tab-change="(name: any) => { if (name==='view') loadView(); if (name==='admin') loadMgr(); if (name==='category') loadCats(); if (name==='grant') loadGrants(); }">
      <!-- 我的日志 -->
      <el-tab-pane label="我的日志" name="my">
        <!-- 统计卡片 -->
        <el-row :gutter="12" style="margin-bottom:14px;">
          <el-col :span="6"><div class="stat-card"><div class="stat-num">{{ stats.myMonthCount }}</div><div class="stat-label">本月撰写（篇）</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="stat-num">{{ stats.myMonthDays }}</div><div class="stat-label">本月撰写（天）</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="stat-num">{{ stats.myCompletionRate }}%</div><div class="stat-label">本月完成率（工作日口径）</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="stat-num">{{ stats.myViewCount }}</div><div class="stat-label">累计被查看</div></div></el-col>
        </el-row>
        <el-row :gutter="12" style="margin-bottom:14px;">
          <el-col :span="4"><div class="stat-card"><div class="stat-num">{{ stats.myCommentCount }}</div><div class="stat-label">累计被评论</div></div></el-col>
          <el-col :span="4"><div class="stat-card"><div class="stat-num">{{ stats.wordStats.totalWords }}</div><div class="stat-label">本月总字数</div></div></el-col>
          <el-col :span="4"><div class="stat-card"><div class="stat-num">{{ stats.wordStats.avgWords }}</div><div class="stat-label">平均每篇字数</div></div></el-col>
          <el-col :span="12"><div class="stat-card stat-chart">
            <div class="stat-label" style="margin-top:0;">提交时段分布（本月）</div>
            <div class="dist-chart">
              <div v-for="d in stats.timeDist" :key="d.period" class="dist-item">
                <span class="dist-name">{{ d.period }}</span>
                <div class="dist-bar-wrap"><div class="dist-bar" :style="{ width: distPercent(d.count) + '%' }"></div></div>
                <span class="dist-count">{{ d.count }}篇</span>
              </div>
            </div>
          </div></el-col>
        </el-row>
        <!-- 部门排名 + 评论活跃度（经理/管理员可见） -->
        <el-row v-if="isAdminManager" :gutter="12" style="margin-bottom:14px;">
          <el-col :span="12"><div class="stat-card stat-chart">
            <div class="stat-label" style="margin-top:0;">本月部门撰写排名</div>
            <div class="rank-list">
              <div v-for="(r, i) in stats.deptRank.slice(0, 8)" :key="r.user_id" class="rank-item">
                <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
                <span class="rank-name">{{ r.real_name }}</span>
                <div class="dist-bar-wrap"><div class="dist-bar" :style="{ width: rankPercent(r.count) + '%' }"></div></div>
                <span class="rank-count">{{ r.count }}篇</span>
              </div>
              <el-empty v-if="stats.deptRank.length === 0" description="本月暂无部门日志数据" :image-size="50" />
            </div>
          </div></el-col>
          <el-col :span="12"><div class="stat-card stat-chart">
            <div class="stat-label" style="margin-top:0;">本月评论最多日志 TOP5</div>
            <div class="rank-list">
              <div v-for="(r, i) in stats.commentTop" :key="r.id" class="rank-item">
                <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
                <span class="rank-name rank-title" :title="r.title">{{ r.title }}</span>
                <span class="rank-count">{{ r.comment_count }}条</span>
              </div>
              <el-empty v-if="stats.commentTop.length === 0" description="本月暂无评论数据" :image-size="50" />
            </div>
          </div></el-col>
        </el-row>

        <!-- 工具栏 -->
        <div class="toolbar">
          <el-radio-group v-model="myDayFilter" size="small" @change="onMyDayFilter">
            <el-radio-button value="today">今天</el-radio-button>
            <el-radio-button value="yesterday">昨天</el-radio-button>
            <el-radio-button value="dayBefore">前天</el-radio-button>
            <el-radio-button value="all">全部</el-radio-button>
          </el-radio-group>
          <el-date-picker v-model="myQuery.logDate" type="date" value-format="YYYY-MM-DD" placeholder="按日期筛选" style="width:150px;" clearable @change="reloadMy" />
          <el-select v-model="myQuery.categoryId" placeholder="按类别筛选" style="width:140px;" clearable @change="reloadMy">
            <el-option v-for="c in categories" :key="c.id" :label="c.category_name" :value="c.id" />
          </el-select>
          <el-input v-model="myQuery.keyword" placeholder="标题 / 内容关键词" clearable style="width:200px;" @keyup.enter="reloadMy" @clear="reloadMy" />
          <el-button type="primary" :icon="Search" @click="reloadMy">查询</el-button>
          <div style="flex:1;"></div>
          <el-button v-if="hasPerm('worklog:makeup')" type="warning" @click="applyVisible = true">补写申请</el-button>
          <el-button :icon="Download" @click="doExportMine">导出我的日志</el-button>
          <el-button v-if="hasPerm('worklog:add')" type="primary" :icon="Plus" @click="openCreate">撰写日志</el-button>
        </div>

        <el-table v-loading="myLoading" :data="myList" border stripe>
          <el-table-column label="日期" width="130">
            <template #default="{ row }">
              <div style="display:flex;align-items:center;gap:4px;">
                <span>{{ row.log_date }}</span>
                <el-tag v-if="row.is_supplement === 1" size="small" type="warning">补写</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="category_name" label="类别" width="100">
            <template #default="{ row }"><el-tag size="small" type="info">{{ row.category_name || '-' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column label="补写" width="70" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.is_supplement === 1" size="small" type="warning">补写</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ statusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="view_count" label="查看" width="70" align="center" />
          <el-table-column prop="comment_count" label="评论" width="70" align="center" />
          <el-table-column label="附件" width="90" align="center">
            <template #default="{ row }">
              <el-tooltip v-if="row.attachment_id" :content="row.attachment_file_name || '附件'" placement="top">
                <el-link type="primary" :underline="false" @click="downloadAtt(row)">📎 附件</el-link>
              </el-tooltip>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-button size="small" link :icon="View" @click="openMyDetail(row)">查看</el-button>
              <el-button v-if="row.log_date === new Date().toISOString().slice(0,10) && hasPerm('worklog:edit')" size="small" link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="hasPerm('worklog:delete')" size="small" link type="danger" :icon="Delete" @click="removeMy(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination style="margin-top:12px;justify-content:flex-end;" layout="total, prev, pager, next, sizes" :total="myTotal"
          v-model:current-page="myQuery.pageNum" v-model:page-size="myQuery.pageSize" :page-sizes="[10,20,50]" @change="loadMy" />

        <!-- 我的日志详情 -->
        <el-dialog v-model="myDetailVisible" title="日志详情" width="640px">
          <div class="detail-block">
            <div class="detail-title">{{ myDetail.title }}</div>
            <div class="detail-meta">{{ myDetail.log_date }} · {{ myDetail.category_name || '-' }} · {{ statusText(myDetail.status || 1) }} · 被查看 {{ myDetail.view_count }} 次</div>
            <div class="detail-content">{{ myDetail.content }}</div>
            <template v-if="myDetail.work_summary">
              <div class="detail-sub-title">工作总结</div>
              <div class="detail-content">{{ myDetail.work_summary }}</div>
            </template>
            <template v-if="myDetail.next_plan">
              <div class="detail-sub-title">明日计划</div>
              <div class="detail-content">{{ myDetail.next_plan }}</div>
            </template>
            <template v-if="myDetail.attachment_id">
              <div class="detail-sub-title">附件</div>
              <div class="att-line">
                <span class="att-name">📎 {{ myDetail.attachment_file_name }}</span>
                <span class="att-size">{{ formatSize(myDetail.attachment_file_size) }}</span>
                <el-button size="small" type="primary" link @click="downloadAtt(myDetail)">下载</el-button>
              </div>
            </template>
          </div>
        </el-dialog>

        <!-- 新增 / 编辑 -->
        <el-dialog v-model="formVisible" :title="formTitle" width="720px">
          <el-form :model="form" label-width="80px">
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="日志日期">
                  <el-date-picker v-model="form.log_date" type="date" value-format="YYYY-MM-DD" :disabled-date="(d: Date) => d.getTime() > Date.now() || d.getTime() < Date.now() - 30*24*3600*1000" placeholder="今天或近30天" style="width:100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="日志类别">
                  <el-select v-model="form.category_id" placeholder="选择类别" clearable style="width:100%;">
                    <el-option v-for="c in categories" :key="c.id" :label="c.category_name" :value="c.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="日志标题">
              <el-input v-model="form.title" maxlength="200" placeholder="如：9月28日-工作日志模块开发" />
            </el-form-item>
            <el-form-item label="日志内容">
              <el-input v-model="form.content" type="textarea" :rows="6" maxlength="5000" placeholder="今天完成了什么工作？" />
            </el-form-item>
            <el-form-item label="工作总结">
              <el-input v-model="form.work_summary" type="textarea" :rows="2" maxlength="1000" placeholder="今天工作的总结（可选）" />
            </el-form-item>
            <el-form-item label="明日计划">
              <el-input v-model="form.next_plan" type="textarea" :rows="2" maxlength="1000" placeholder="明天计划做什么（可选）" />
            </el-form-item>
            <el-form-item label="附件">
              <div class="att-upload" @dragenter.prevent @dragover.prevent @drop="onAttDrop" @mouseenter="onAttHover" @click="attInputRef?.click()">
                <input ref="attInputRef" type="file" class="att-input" @change="onAttPick" />
                <template v-if="formAttachment">
                  <div class="att-file">
                    <span class="att-name">📎 {{ formAttachment.fileName }}</span>
                    <span class="att-size">{{ formatSize(formAttachment.fileSize) }}</span>
                    <el-button size="small" type="danger" link @click.stop="removeFormAttachment">移除</el-button>
                  </div>
                </template>
                <template v-else>
                  <div class="att-hint">
                    <span v-if="uploading">上传中…</span>
                    <span v-else>点击选择 / 拖拽文件到此处 / 直接 Ctrl+V 粘贴图片或文件（≤20MB）</span>
                  </div>
                </template>
              </div>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="formVisible = false">取消</el-button>
            <el-button type="primary" :loading="commenting" @click="saveForm">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 日志查看（仅经理可见，普通员工只看自己的日志） -->
      <el-tab-pane v-if="isAdminManager" label="日志查看" name="view">
        <div class="toolbar">
          <el-cascader v-if="canPickOrg" v-model="viewQuery.orgId" :options="orgTreeData" :props="{ value: 'id', label: 'label', emitPath: false, checkStrictly: true }" placeholder="按部门筛选" style="width:180px;" clearable @change="reloadView" />
          <el-select v-model="viewQuery.categoryId" placeholder="按类别筛选" style="width:140px;" clearable @change="reloadView">
            <el-option v-for="c in categories" :key="c.id" :label="c.category_name" :value="c.id" />
          </el-select>
          <el-input v-model="viewQuery.keyword" placeholder="标题 / 内容关键词" clearable style="width:200px;" @keyup.enter="reloadView" @clear="reloadView" />
          <el-button type="primary" :icon="Search" @click="reloadView">查询</el-button>
          <div style="flex:1;"></div>
          <el-button v-if="!isWorklogAdmin && canApproveSupplement" type="primary" @click="openPending">补写审批</el-button>
          <el-button :icon="Refresh" @click="reloadView">刷新</el-button>
        </div>
        <el-table v-loading="viewLoading" :data="viewList" border stripe>
          <el-table-column prop="real_name" label="作者" width="100" />
          <el-table-column prop="org_name" label="部门" width="130" show-overflow-tooltip />
          <el-table-column prop="log_date" label="日期" width="110" />
          <el-table-column label="补写" width="70" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.is_supplement === 1" size="small" type="warning">补写</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="view_count" label="查看" width="70" align="center" />
          <el-table-column prop="comment_count" label="评论" width="70" align="center" />
          <el-table-column label="附件" width="90" align="center">
            <template #default="{ row }">
              <el-tooltip v-if="row.attachment_id" :content="row.attachment_file_name || '附件'" placement="top">
                <el-link type="primary" :underline="false" @click="downloadAtt(row)">📎 附件</el-link>
              </el-tooltip>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="View" @click="openDetail(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination style="margin-top:12px;justify-content:flex-end;" layout="total, prev, pager, next, sizes" :total="viewTotal"
          v-model:current-page="viewQuery.pageNum" v-model:page-size="viewQuery.pageSize" :page-sizes="[10,20,50]" @change="loadView" />
      </el-tab-pane>

      <!-- 日志管理（管理员） -->
      <el-tab-pane v-if="isWorklogAdmin" label="日志管理" name="admin">
        <el-radio-group v-model="mgrView" style="margin-bottom:12px;" @change="reloadMgr">
          <el-radio-button value="list">全部日志</el-radio-button>
          <el-radio-button value="recycle">回收站</el-radio-button>
        </el-radio-group>

        <div class="toolbar">
          <template v-if="mgrView === 'list'">
            <el-cascader v-model="mgrQuery.orgId" :options="orgTreeData" :props="{ value: 'id', label: 'label', emitPath: false, checkStrictly: true }" placeholder="按部门筛选" style="width:170px;" clearable />
            <el-select v-model="mgrQuery.status" placeholder="按状态筛选" style="width:120px;" clearable>
              <el-option label="草稿" :value="0" />
              <el-option label="已提交" :value="1" />
              <el-option label="已撤回" :value="2" />
            </el-select>
            <el-select v-model="mgrQuery.categoryId" placeholder="类别" style="width:110px;" clearable>
              <el-option v-for="c in categories" :key="c.id" :label="c.category_name" :value="c.id" />
            </el-select>
            <el-date-picker v-model="mgrQuery.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width:130px;" clearable />
            <el-date-picker v-model="mgrQuery.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width:130px;" clearable />
            <el-input v-model="mgrQuery.keyword" placeholder="标题 / 内容关键词" clearable style="width:170px;" @keyup.enter="reloadMgr" />
            <el-button type="primary" :icon="Search" @click="reloadMgr">查询</el-button>
            <div style="flex:1;"></div>
            <el-button v-if="isExporter" :icon="Download" @click="doExportAll">导出</el-button>
            <el-button v-if="isWorklogAdmin" type="primary" @click="openPending">补写审批</el-button>
            <el-button v-if="isWorklogAdmin" type="success" @click="openSupplementGrant">补写授权</el-button>
          </template>
          <template v-else>
            <el-input v-model="mgrQuery.keyword" placeholder="标题关键词" clearable style="width:220px;" @keyup.enter="reloadMgr" />
            <el-button type="primary" :icon="Search" @click="reloadMgr">查询</el-button>
          </template>
        </div>

        <el-table v-loading="mgrLoading" :data="mgrList" border stripe>
          <el-table-column prop="real_name" label="作者" width="100" />
          <el-table-column prop="org_name" label="部门" width="130" show-overflow-tooltip />
          <el-table-column prop="log_date" label="日期" width="110" />
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <template v-if="mgrView === 'list'">
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ statusText(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="view_count" label="查看" width="70" align="center" />
            <el-table-column prop="comment_count" label="评论" width="70" align="center" />
            <el-table-column label="附件" width="90" align="center">
              <template #default="{ row }">
                <el-tooltip v-if="row.attachment_id" :content="row.attachment_file_name || '附件'" placement="top">
                  <el-link type="primary" :underline="false" @click="downloadAtt(row)">📎 附件</el-link>
                </el-tooltip>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="danger" :icon="Delete" @click="removeAdmin(row)">删除</el-button>
              </template>
            </el-table-column>
          </template>
          <template v-else>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="doRestore(row)">恢复</el-button>
                <el-button size="small" type="danger" @click="removeForever(row)">永久删除</el-button>
              </template>
            </el-table-column>
          </template>
        </el-table>
        <el-pagination style="margin-top:12px;justify-content:flex-end;" layout="total, prev, pager, next, sizes" :total="mgrTotal"
          v-model:current-page="mgrQuery.pageNum" v-model:page-size="mgrQuery.pageSize" :page-sizes="[10,20,50]" @change="loadMgr" />
      </el-tab-pane>

      <!-- 类别管理（管理员） -->
      <el-tab-pane v-if="isCategoryManager" label="类别管理" name="category">
        <div class="toolbar">
          <div style="flex:1;"></div>
          <el-button type="primary" :icon="Plus" @click="openCatCreate">新增类别</el-button>
        </div>
        <el-table v-loading="catLoading" :data="catList" border stripe>
          <el-table-column prop="category_name" label="类别名称" min-width="140" />
          <el-table-column prop="category_code" label="编码" width="120" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="sort_order" label="排序" width="80" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" :icon="Edit" style="width:64px" @click="openCatEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" :icon="Delete" style="width:64px" @click="removeCat(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog v-model="catFormVisible" :title="catForm.id ? '编辑类别' : '新增类别'" width="480px">
          <el-form :model="catForm" label-width="80px">
            <el-form-item label="类别名称"><el-input v-model="catForm.category_name" maxlength="100" /></el-form-item>
            <el-form-item label="类别编码"><el-input v-model="catForm.category_code" maxlength="50" placeholder="如 DAILY / WEEKLY" /></el-form-item>
            <el-form-item label="描述"><el-input v-model="catForm.description" type="textarea" :rows="2" maxlength="500" /></el-form-item>
            <el-form-item label="排序号"><el-input-number v-model="catForm.sort_order" :min="0" /></el-form-item>
            <el-form-item label="状态">
              <el-switch v-model="catForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="catFormVisible = false">取消</el-button>
            <el-button type="primary" @click="saveCat">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 查看权限（管理员） -->
      <el-tab-pane v-if="isPermissionManager" label="查看权限" name="grant">
        <div class="toolbar">
          <el-input v-model="grantQuery.keyword" placeholder="被查看人 / 查看人姓名" clearable style="width:220px;" @keyup.enter="loadGrants" />
          <el-button type="primary" :icon="Search" @click="loadGrants">查询</el-button>
          <div style="flex:1;"></div>
          <el-button type="primary" :icon="Plus" @click="openGrantCreate">新增授权</el-button>
        </div>
        <el-table v-loading="grantLoading" :data="grantList" border stripe>
          <el-table-column prop="target_name" label="被查看人" min-width="110" />
          <el-table-column label="授权方式" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.permission_type === 1 ? 'primary' : row.permission_type === 2 ? 'success' : 'warning'">
                {{ row.permission_type === 1 ? '按用户' : row.permission_type === 2 ? '按群组' : '按部门' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="查看人 / 范围" min-width="140">
            <template #default="{ row }">
              {{ row.viewer_name || row.group_name || row.dept_name || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="granter_name" label="授权人" width="100" />
          <el-table-column prop="grant_time" label="授权时间" width="160">
            <template #default="{ row }">{{ formatTime(row.grant_time) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" :icon="Delete" @click="removeGrant(row)">撤销</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination style="margin-top:12px;justify-content:flex-end;" layout="total, prev, pager, next" :total="grantTotal"
          v-model:current-page="grantQuery.pageNum" v-model:page-size="grantQuery.pageSize" @change="loadGrants" />

        <el-dialog v-model="grantFormVisible" title="新增授权" width="520px">
          <el-form :model="grantForm" label-width="100px">
            <el-form-item label="被查看人">
              <el-select v-model="grantForm.target_id" filterable placeholder="谁的日志可以被查看" style="width:100%;">
                <el-option v-for="u in users" :key="u.id" :label="`${u.real_name}（${u.username}）`" :value="u.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="授权方式">
              <el-radio-group v-model="grantForm.permission_type">
                <el-radio :value="1">按用户</el-radio>
                <el-radio :value="2">按群组</el-radio>
                <el-radio :value="3">按部门</el-radio>
              </el-radio-group>
              <div style="font-size:12px;color:#909399;margin-top:4px;">群组在「用户管理 - 用户群组」里维护</div>
            </el-form-item>
            <el-form-item v-if="grantForm.permission_type === 1" label="查看人">
              <el-select v-model="grantForm.viewer_id" filterable placeholder="谁可以查看该日志" style="width:100%;">
                <el-option v-for="u in users" :key="u.id" :label="`${u.real_name}（${u.username}）`" :value="u.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-else-if="grantForm.permission_type === 2" label="用户群组">
              <el-select v-model="grantForm.group_id" filterable placeholder="该群组成员可查看" style="width:100%;">
                <el-option v-for="g in groupOptions" :key="g.id" :label="g.group_name" :value="g.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-else label="授权部门">
              <el-cascader v-model="grantForm.dept_id" :options="orgTreeData" :props="{ value: 'id', label: 'label', emitPath: false, checkStrictly: true }" placeholder="该部门（含子部门）员工可查看" style="width:100%;" clearable />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="grantFormVisible = false">取消</el-button>
            <el-button type="primary" @click="saveGrant">授权</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>

    <!-- 日志查看详情（含评论） -->
    <el-dialog v-model="detailVisible" title="日志详情" width="720px">
      <div class="detail-block">
        <div class="detail-title">{{ detail.title }}</div>
        <div class="detail-meta">{{ detail.real_name }} · {{ detail.org_name }} · {{ detail.log_date }} · {{ statusText(detail.status || 1) }}</div>
        <div class="detail-content">{{ detail.content }}</div>
        <template v-if="detail.work_summary">
          <div class="detail-sub-title">工作总结</div>
          <div class="detail-content">{{ detail.work_summary }}</div>
        </template>
        <template v-if="detail.next_plan">
          <div class="detail-sub-title">明日计划</div>
          <div class="detail-content">{{ detail.next_plan }}</div>
        </template>
        <template v-if="detail.attachment_id">
          <div class="detail-sub-title">附件</div>
          <div class="att-line">
            <span class="att-name">📎 {{ detail.attachment_file_name }}</span>
            <span class="att-size">{{ formatSize(detail.attachment_file_size) }}</span>
            <el-button size="small" type="primary" link @click="downloadAtt(detail)">下载</el-button>
          </div>
        </template>

        <div class="detail-sub-title" style="margin-top:18px;">评论（{{ detail.comment_count || 0 }}）</div>
        <div class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-head">
              <span class="comment-name">{{ c.deleted ? '已删除用户' : (c.real_name || c.username) }}</span>
              <span class="comment-time">{{ formatTime(c.comment_time) }}</span>
              <el-button v-if="!c.deleted" size="small" type="danger" link @click="removeComment(c)">删除</el-button>
            </div>
            <div class="comment-content">{{ c.deleted ? '该评论已删除' : c.content }}</div>
          </div>
          <el-empty v-if="comments.length === 0" description="暂无评论" :image-size="60" />
        </div>
        <div class="comment-input">
          <el-input v-model="commentText" type="textarea" :rows="2" maxlength="1000" placeholder="发表评论…" />
          <el-button v-if="hasPerm('worklog:comment')" type="primary" :loading="commenting" :icon="ChatDotRound" style="margin-top:8px;" @click="sendComment">发表评论</el-button>
        </div>
      </div>
    </el-dialog>
  </div>

    <el-dialog v-model="supplementVisible" title="超30天补写授权" width="480px">
      <el-form :model="supplementForm" label-width="90px">
        <el-form-item label="选择员工">
          <el-select v-model="supplementForm.user_id" placeholder="搜索员工" filterable style="width:100%;">
            <el-option v-for="u in allUsers" :key="u.id" :label="u.real_name + (u.username ? '（' + u.username + '）' : '')" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="补写日期范围">
          <el-date-picker v-model="supplementForm.range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" style="width:100%;" :disabled-date="(d: Date) => d.getTime() > Date.now()" />
        </el-form-item>
        <el-form-item label="有效天数">
          <el-input-number v-model="supplementForm.days" :min="1" :max="30" />
          <span style="margin-left:8px;color:#999;font-size:12px;">默认7天，到期自动失效</span>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="supplementForm.reason" type="textarea" :rows="2" placeholder="如：出差/病假" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supplementVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSupplement">确认授权</el-button>
      </template>
    </el-dialog>


    <!-- 员工补写申请弹窗 -->
    <el-dialog v-model="applyVisible" title="超30天补写申请" width="480px">
      <el-alert title="超过30天未写的日志不能直接补写，需提交申请经主管审批后开放7天权限。" type="info" :closable="false" style="margin-bottom:12px;" />
      <el-form :model="applyForm" label-width="90px">
        <el-form-item label="补写日期范围">
          <el-date-picker v-model="applyForm.range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" style="width:100%;" :disabled-date="(d: Date) => d.getTime() > Date.now()" />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model="applyForm.reason" type="textarea" :rows="3" placeholder="如：出差/病假/系统原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 主管补写审批弹窗 -->
    <el-dialog v-model="pendingVisible" title="补写申请审批" width="640px">
      <el-table :data="pendingList" size="small">
        <el-table-column prop="user_name" label="申请人" width="100" />
        <el-table-column label="日期范围">
          <template #default="{ row }">{{ row.start_date }} ~ {{ row.end_date }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" show-overflow-tooltip />
        <el-table-column prop="created_at" label="申请时间" width="150" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="doApprove(row, true)">通过</el-button>
            <el-button size="small" type="danger" @click="doApprove(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

</template>

<style scoped>
.worklog-page {
  padding: 4px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(31, 41, 55, 0.04);
}
.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: #409eff;
}
.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.detail-block {
  padding: 0 4px;
}
.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2d3d;
  margin-bottom: 6px;
}
.detail-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 14px;
}
.detail-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 6px;
}
.detail-sub-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
  margin: 10px 0 8px;
}
.comment-list {
  max-height: 260px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px 12px;
}
.comment-item {
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.comment-name {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
}
.comment-time {
  font-size: 12px;
  color: #c0c4cc;
}
.comment-content {
  font-size: 13px;
  color: #303133;
  line-height: 1.7;
  margin-top: 4px;
  white-space: pre-wrap;
}
.comment-input {
  margin-top: 12px;
}
/* 统计图表用 CSS 条形，不引图表库 */
.stat-chart {
  padding: 14px 18px;
}
.dist-chart {
  margin-top: 8px;
}
.dist-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.dist-name {
  width: 40px;
  font-size: 12px;
  color: #606266;
  flex-shrink: 0;
}
.dist-bar-wrap {
  flex: 1;
  height: 10px;
  background: #f0f2f5;
  border-radius: 5px;
  overflow: hidden;
}
.dist-bar {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #79bbff);
  border-radius: 5px;
  transition: width 0.3s;
}
.dist-count {
  width: 44px;
  font-size: 12px;
  color: #909399;
  text-align: right;
  flex-shrink: 0;
}
.rank-list {
  margin-top: 8px;
}
.rank-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.rank-no {
  width: 20px;
  height: 20px;
  line-height: 20px;
  text-align: center;
  border-radius: 50%;
  background: #f0f2f5;
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}
.rank-no.top {
  background: #409eff;
  color: #fff;
}
.rank-name {
  font-size: 13px;
  color: #303133;
  flex-shrink: 0;
  max-width: 120px;
}
.rank-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rank-count {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}
/* 附件上传与展示 */
.att-upload {
  width: 100%;
  min-height: 56px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  background: #fafbfc;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  position: relative;
}
.att-upload:hover {
  border-color: #409eff;
  background: #f0f7ff;
}
.att-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
}
.att-hint {
  font-size: 12px;
  color: #909399;
  padding: 10px 14px;
  text-align: center;
}
.att-file {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
}
.att-name {
  font-size: 13px;
  color: #303133;
  word-break: break-all;
}
.att-size {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}
.att-line {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px 14px;
}
</style>
