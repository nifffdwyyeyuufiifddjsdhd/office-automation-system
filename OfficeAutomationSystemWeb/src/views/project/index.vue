<template>
  <div class="project-page">
    <div class="page-header">
      <div class="head-left">
        <h2>项目管理</h2>
        <span class="page-desc">立项、成员、任务与结项的完整闭环；任务进度自动汇总到项目进度</span>
      </div>
      <div class="head-right">
        <el-button v-if="canAdd" type="primary" @click="openCreate">新建项目</el-button>
      </div>
    </div>

    <el-form :inline="true" class="filter-bar" @submit.prevent>
      <el-form-item label="关键词">
        <el-input v-model="filters.keyword" placeholder="项目名称 / 编号" clearable style="width: 200px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option v-for="(t, i) in STATUS_TEXT" :key="i" :label="t" :value="i" />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级">
        <el-select v-model="filters.priority" placeholder="全部优先级" clearable style="width: 120px">
          <el-option label="低" :value="1" />
          <el-option label="中" :value="2" />
          <el-option label="高" :value="3" />
          <el-option label="紧急" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="project_no" label="项目编号" width="150" />
      <el-table-column label="项目名称" min-width="150">
        <template #default="{ row }">
          <div class="fade-overflow" @click="openDetail(row)">{{ row.name }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="manager_name" label="项目经理" width="100" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status_text || statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="72" align="center">
        <template #default="{ row }">
          <el-tag :type="priorityType(row.priority)" size="small">{{ priorityText(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="计划周期" width="170">
        <template #default="{ row }">
          <div class="date-range">{{ row.plan_start_date || '—' }} ~ {{ row.plan_end_date || '—' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="110">
        <template #default="{ row }">
          <el-progress :percentage="row.progress || 0" :stroke-width="14" />
        </template>
      </el-table-column>
      <el-table-column label="任务" width="100" align="center">
        <template #default="{ row }">
          <span>{{ row.task_done || 0 }}/{{ row.task_total || 0 }}</span>
          <el-tag v-if="row.task_overdue" type="danger" size="small" effect="plain" style="margin-left: 4px">逾期{{ row.task_overdue }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="member_count" label="成员" width="64" align="center" />
      <el-table-column label="操作" width="225" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button v-if="canManage(row)" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canStart(row)" link type="success" size="small" @click="doStatus(row, 'start')">开始</el-button>
          <el-button v-if="canPause(row)" link type="warning" size="small" @click="doStatus(row, 'pause')">暂停</el-button>
          <el-button v-if="canResume(row)" link type="success" size="small" @click="doStatus(row, 'resume')">恢复</el-button>
          <el-button v-if="canComplete(row)" link type="success" size="small" @click="doStatus(row, 'complete')">结项</el-button>
          <el-button v-if="canCancel(row)" link type="info" size="small" @click="doStatus(row, 'cancel')">取消</el-button>
          <el-button v-if="canManage(row)" link type="danger" size="small" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      :total="total"
      :page-size="pageSize"
      layout="total, prev, pager, next"
      @current-change="loadData"
    />

    <!-- 立项 / 编辑 -->
    <el-dialog v-model="formVisible" :title="editMode ? '编辑项目' : '新建项目'" width="660px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" placeholder="请输入项目名称" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="项目目标、范围、交付物" />
        </el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="form.project_type" style="width: 100%">
            <el-option label="内部项目" :value="1" />
            <el-option label="客户项目" :value="2" />
            <el-option label="研发项目" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
            <el-option label="紧急" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目经理">
          <el-select v-model="form.manager_id" :disabled="!isAdmin" filterable style="width: 100%">
            <el-option v-for="u in memberOptions" :key="u.id" :label="u.real_name + '（' + (u.org_name || '无部门') + '）'" :value="u.id" />
          </el-select>
          <div v-if="!isAdmin" class="form-tip">非管理员立项时，项目经理默认为本人</div>
        </el-form-item>
        <el-form-item label="计划开始">
          <el-date-picker v-model="form.plan_start_date" type="date" value-format="YYYY-MM-DD" placeholder="选择开始日期" />
        </el-form-item>
        <el-form-item label="计划结束">
          <el-date-picker v-model="form.plan_end_date" type="date" value-format="YYYY-MM-DD" placeholder="选择结束日期" />
        </el-form-item>
        <el-form-item label="项目成员">
          <el-select v-model="form.member_ids" multiple filterable clearable placeholder="选择参与项目的同事" style="width: 100%">
            <el-option v-for="u in memberOptions" :key="u.id" :label="u.real_name + '（' + (u.org_name || '无部门') + '）'" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">{{ editMode ? '保存' : '立项' }}</el-button>
      </template>
    </el-dialog>

    <!-- 项目详情 -->
    <el-drawer v-model="detailVisible" :title="detail ? detail.name : '项目详情'" size="1000px">
      <div v-if="detail" v-loading="detailLoading">
        <div class="detail-head">
          <el-tag :type="statusType(detail.status)" size="small">{{ detail.status_text || statusText(detail.status) }}</el-tag>
          <span class="mono">{{ detail.project_no }}</span>
          <span class="detail-manager">项目经理：{{ detail.manager_name || '—' }}</span>
          <div class="detail-actions">
            <el-button v-if="canStart(detail)" size="small" type="success" @click="doStatus(detail, 'start')">开始项目</el-button>
            <el-button v-if="canPause(detail)" size="small" type="warning" @click="doStatus(detail, 'pause')">暂停</el-button>
            <el-button v-if="canResume(detail)" size="small" type="success" @click="doStatus(detail, 'resume')">恢复</el-button>
            <el-button v-if="canComplete(detail)" size="small" type="success" @click="doStatus(detail, 'complete')">结项</el-button>
          </div>
        </div>

        <div class="stat-cards">
          <div class="stat-card"><div class="stat-num">{{ detail.statistics.total || 0 }}</div><div class="stat-label">任务总数</div></div>
          <div class="stat-card"><div class="stat-num">{{ detail.statistics.done || 0 }}</div><div class="stat-label">已完成</div></div>
          <div class="stat-card"><div class="stat-num">{{ detail.statistics.running || 0 }}</div><div class="stat-label">进行中</div></div>
          <div class="stat-card"><div class="stat-num">{{ detail.statistics.completion_rate || 0 }}%</div><div class="stat-label">完成率</div></div>
          <div class="stat-card danger"><div class="stat-num">{{ detail.statistics.overdue || 0 }}</div><div class="stat-label">逾期任务</div></div>
          <div class="stat-card"><div class="stat-num">{{ detail.statistics.overdue_rate || 0 }}%</div><div class="stat-label">逾期率</div></div>
        </div>

        <el-tabs v-model="detailTab">
          <el-tab-pane label="项目概览" name="overview">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="项目编号">{{ detail.project_no }}</el-descriptions-item>
              <el-descriptions-item label="项目名称">{{ detail.name }}</el-descriptions-item>
              <el-descriptions-item label="项目经理">{{ detail.manager_name || '—' }}</el-descriptions-item>
              <el-descriptions-item label="创建人">{{ detail.creator_name || '—' }}</el-descriptions-item>
              <el-descriptions-item label="计划周期">{{ detail.plan_start_date || '—' }} ~ {{ detail.plan_end_date || '—' }}</el-descriptions-item>
              <el-descriptions-item label="实际周期">{{ detail.actual_start_date || '—' }} ~ {{ detail.actual_end_date || '—' }}</el-descriptions-item>
              <el-descriptions-item label="项目进度">{{ detail.progress || 0 }}%（按任务进度自动汇总）</el-descriptions-item>
              <el-descriptions-item label="工时偏差">{{ detail.statistics.avg_hour_diff }} 小时/任务</el-descriptions-item>
              <el-descriptions-item label="项目描述" :span="2">{{ detail.description || '—' }}</el-descriptions-item>
            </el-descriptions>
            <div class="assignee-rank">
              <div class="rank-title">成员任务负载</div>
              <el-table :data="detail.statistics.by_assignee || []" size="small" border>
                <el-table-column prop="real_name" label="成员" width="140" />
                <el-table-column prop="total" label="任务数" width="100" align="center" />
                <el-table-column prop="done" label="已完成" width="100" align="center" />
                <el-table-column label="完成率" align="center">
                  <template #default="{ row }">{{ row.total ? Math.round(row.done * 100 / row.total) : 0 }}%</template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="项目成员" name="members">
            <div class="tab-toolbar">
              <el-button v-if="detail.can_manage" size="small" type="primary" @click="openAddMember">添加成员</el-button>
            </div>
            <el-table :data="detail.members || []" size="small" border>
              <el-table-column prop="real_name" label="姓名" width="140" />
              <el-table-column prop="org_name" label="所属部门" width="140" />
              <el-table-column label="项目角色" width="120">
                <template #default="{ row }">{{ row.role === 1 ? '项目经理' : (row.role === 3 ? '关注人' : '项目成员') }}</template>
              </el-table-column>
              <el-table-column prop="join_time" label="加入时间" />
              <el-table-column label="任务中心" width="160" align="center">
                <template #default="{ row }">
                  <el-tag v-if="memberAccess[row.user_id]" size="small" type="success">已开通</el-tag>
                  <el-tag v-else size="small" type="info">未开通</el-tag>
                  <el-button v-if="canGrantTask && row.role !== 1" link size="small"
                    :type="memberAccess[row.user_id] ? 'danger' : 'primary'" style="margin-left:6px"
                    @click="toggleMemberAccess(row)">{{ memberAccess[row.user_id] ? '回收' : '开通' }}</el-button>
                </template>
              </el-table-column>
              <el-table-column v-if="detail.can_manage" label="操作" width="100" align="center">
                <template #default="{ row }">
                  <el-button v-if="row.role !== 1" link type="danger" size="small" @click="doRemoveMember(row)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="项目任务" name="tasks">
            <el-table :data="detail.tasks || []" size="small" border>
              <el-table-column prop="task_no" label="任务编号" width="170" />
              <el-table-column prop="title" label="任务名称" min-width="180" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="taskStatusType(row.status)">{{ taskStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="优先级" width="80" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="priorityType(row.priority)">{{ priorityText(row.priority) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="进度" width="120">
                <template #default="{ row }"><el-progress :percentage="row.progress || 0" :stroke-width="12" /></template>
              </el-table-column>
              <el-table-column prop="assignee_name" label="负责人" width="110" />
              <el-table-column label="截止" width="150">
                <template #default="{ row }">{{ formatDate(row.plan_end_time) }}</template>
              </el-table-column>
            </el-table>
            <div class="tab-tip">在「任务中心」新建任务时选择本项目，任务会自动出现在这里，进度也会汇总到项目进度。</div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <!-- 添加成员 -->
    <el-dialog v-model="memberVisible" title="添加项目成员" width="520px">
      <el-form label-width="90px">
        <el-form-item label="选择成员">
          <el-select v-model="memberPicked" multiple filterable clearable placeholder="可多选" style="width: 100%">
            <el-option v-for="u in addableOptions" :key="u.id" :label="u.real_name + '（' + (u.org_name || '无部门') + '）'" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMembers">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getProjects, getProjectDetail, createProject, updateProject, deleteProject,
  changeProjectStatus, addProjectMembers, removeProjectMember,
  getProjectMemberOptions, getProjectMemberTaskAccess
} from '@/api/project'
import { getUserExtraPermissions, grantUserExtraPermission, revokeUserExtraPermission } from '@/api/user'
import { useUserStore } from '@/store/user'
import { hasPerm } from '@/utils/permission'

const userStore = useUserStore()
const uid = Number(userStore.userId) || Number(sessionStorage.getItem('userId')) || 0
const roles = computed<string[]>(() => userStore.roles || [])
const isAdmin = computed(() => roles.value.includes('admin'))
const canAdd = computed(() => hasPerm('project:add'))

const STATUS_TEXT = ['未启动', '进行中', '已暂停', '已完成', '已取消']
const STATUS_TYPE = ['info', 'primary', 'warning', 'success', 'info']
const TASK_STATUS_TEXT = ['待分配', '待接受', '进行中', '待验收', '已完成', '已撤销', '已拒绝']
const TASK_STATUS_TYPE = ['info', 'warning', 'primary', 'warning', 'success', 'info', 'danger']

function statusText(s: number) { return STATUS_TEXT[s] || '未知' }
function statusType(s: number) { return STATUS_TYPE[s] || 'info' }
function taskStatusText(s: number) { return TASK_STATUS_TEXT[s] || '未知' }
function taskStatusType(s: number) { return TASK_STATUS_TYPE[s] || 'info' }
const PRIORITY_TEXT = ['', '低', '中', '高', '紧急']
const PRIORITY_TYPE = ['', 'info', 'primary', 'warning', 'danger']
function priorityText(p: number) { return PRIORITY_TEXT[p] || '中' }
function priorityType(p: number) { return PRIORITY_TYPE[p] || 'primary' }
function formatDate(d: any) { return d ? String(d).replace('T', ' ').slice(0, 16) : '—' }

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filters = ref<any>({ keyword: '', status: null, priority: null })

const memberOptions = ref<any[]>([])
const addableOptions = computed(() => {
  const used = new Set((detail.value?.members || []).map((m: any) => m.user_id))
  return memberOptions.value.filter((u: any) => !used.has(u.id))
})

const formVisible = ref(false)
const editMode = ref(false)
const submitting = ref(false)
const form = ref<any>({})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const detailTab = ref('overview')

const memberVisible = ref(false)
const memberPicked = ref<number[]>([])

function canManage(row: any) { return row && row.can_manage === true }
function canStart(row: any) { return canManage(row) && row.status === 0 }
function canPause(row: any) { return canManage(row) && row.status === 1 }
function canResume(row: any) { return canManage(row) && row.status === 2 }
function canComplete(row: any) { return canManage(row) && row.status === 1 }
function canCancel(row: any) { return canManage(row) && [0, 1, 2].includes(row.status) }

async function loadData() {
  loading.value = true
  try {
    const res: any = await getProjects({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: filters.value.keyword || undefined,
      status: filters.value.status,
      priority: filters.value.priority
    })
    list.value = res.records || []
    total.value = res.total || 0
  } catch {
    ElMessage.error('加载项目失败')
  } finally {
    loading.value = false
  }
}

async function loadMemberOptions() {
  try {
    const res: any = await getProjectMemberOptions()
    memberOptions.value = res || []
  } catch { /* 无权限时静默 */ }
}

function onSearch() { pageNum.value = 1; loadData() }
function onReset() {
  filters.value = { keyword: '', status: null, priority: null }
  pageNum.value = 1
  loadData()
}

function openCreate() {
  editMode.value = false
  form.value = {
    name: '', description: '', project_type: 1, priority: 2,
    manager_id: uid, plan_start_date: null, plan_end_date: null, member_ids: []
  }
  loadMemberOptions()
  formVisible.value = true
}

async function openEdit(row: any) {
  editMode.value = true
  editId.value = row.id
  loadMemberOptions()
  try {
    const d: any = await getProjectDetail(row.id)
    form.value = {
      name: d.name, description: d.description, project_type: d.project_type, priority: d.priority,
      manager_id: d.manager_id, plan_start_date: d.plan_start_date, plan_end_date: d.plan_end_date,
      member_ids: (d.members || []).filter((m: any) => m.role !== 1).map((m: any) => m.user_id)
    }
  } catch {
    form.value = { ...row, member_ids: [] }
  }
  formVisible.value = true
}

const editId = ref<number | null>(null)

async function submitForm() {
  if (!form.value.name || !String(form.value.name).trim()) {
    ElMessage.warning('请填写项目名称')
    return
  }
  if (form.value.plan_start_date && form.value.plan_end_date && form.value.plan_start_date > form.value.plan_end_date) {
    ElMessage.warning('计划结束日期不能早于开始日期')
    return
  }
  submitting.value = true
  try {
    if (editMode.value && editId.value) {
      await updateProject(editId.value, form.value)
      ElMessage.success('已保存')
    } else {
      await createProject(form.value)
      ElMessage.success('立项成功')
    }
    formVisible.value = false
    loadData()
    if (detailVisible.value && detail.value) await refreshDetail()
  } catch { /* 请求层已提示 */ } finally {
    submitting.value = false
  }
}

async function doStatus(row: any, action: string) {
  const labelMap: any = { start: '开始', pause: '暂停', resume: '恢复', complete: '结项', cancel: '取消' }
  try {
    await ElMessageBox.confirm('确定要' + labelMap[action] + '项目「' + row.name + '」吗？', '操作确认', { type: 'warning' })
  } catch { return }
  try {
    await changeProjectStatus(row.id, action)
    ElMessage.success(labelMap[action] + '成功')
    loadData()
    if (detailVisible.value && detail.value && detail.value.id === row.id) await refreshDetail()
  } catch { /* 请求层已提示 */ }
}

async function doDelete(row: any) {
  try {
    await ElMessageBox.confirm('删除后项目不可恢复（项目下的任务需先处理完），确定删除「' + row.name + '」吗？', '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await deleteProject(row.id)
    ElMessage.success('已删除')
    if (detailVisible.value) detailVisible.value = false
    loadData()
  } catch { /* 请求层已提示 */ }
}

async function openDetail(row: any) {
  detailVisible.value = true
  detailTab.value = 'overview'
  detail.value = { ...row, statistics: row.statistics || { by_assignee: [] }, members: [], tasks: [] }
  await refreshDetail()
  await loadMemberAccess()
}

// 只有持 task:grant 的项目经理和管理员能发放任务权限
const canGrantTask = computed(() => hasPerm('task:grant'))
const memberAccess = ref<Record<number, boolean>>({})

async function loadMemberAccess() {
  memberAccess.value = {}
  if (!canGrantTask.value || !detail.value?.id) return
  try {
    const rows = await getProjectMemberTaskAccess(detail.value.id)
    const map: Record<number, boolean> = {}
    for (const r of rows) map[r.user_id] = !!r.task_access
    memberAccess.value = map
  } catch {
    // 没有发放权限时不显示状态，也不打扰用户
  }
}

async function toggleMemberAccess(row: any) {
  if (!detail.value) return
  if (memberAccess.value[row.user_id]) {
    const info = await getUserExtraPermissions(row.user_id)
    const item = (info.items || []).find((it) => it.menuCode === 'task:access')
    if (item) await revokeUserExtraPermission(row.user_id, item.menuId)
  } else {
    await grantUserExtraPermission(row.user_id, 'task:access')
  }
  ElMessage.success('任务中心权限已更新')
  await loadMemberAccess()
}

async function refreshDetail() {
  if (!detail.value) return
  detailLoading.value = true
  try {
    const d: any = await getProjectDetail(detail.value.id)
    detail.value = d
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

function openAddMember() {
  memberPicked.value = []
  loadMemberOptions()
  memberVisible.value = true
}

async function submitMembers() {
  if (!memberPicked.value.length) {
    ElMessage.warning('请选择成员')
    return
  }
  submitting.value = true
  try {
    await addProjectMembers(detail.value.id, memberPicked.value)
    ElMessage.success('已添加成员')
    await loadMemberAccess()
    memberVisible.value = false
    await refreshDetail()
    loadData()
  } catch { /* 请求层已提示 */ } finally {
    submitting.value = false
  }
}

async function doRemoveMember(row: any) {
  try {
    await ElMessageBox.confirm('确定把「' + row.real_name + '」移出项目吗？', '移除确认', { type: 'warning' })
  } catch { return }
  try {
    await removeProjectMember(detail.value.id, row.user_id)
    ElMessage.success('已移除')
    await loadMemberAccess()
    await refreshDetail()
    loadData()
  } catch { /* 请求层已提示 */ }
}

onMounted(() => {
  loadData()
  loadMemberOptions()
})
</script>

<style scoped>
.project-page { padding: 4px 0 24px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.head-left { display: flex; align-items: baseline; gap: 12px; }
.page-header h2 { margin: 0; font-size: 20px; color: #1f2d3d; }
.page-desc { font-size: 12px; color: #909399; }
.filter-bar { margin-bottom: 6px; }
.fade-overflow { cursor: pointer; color: #1f6feb; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.date-range { font-size: 12px; color: #606266; }
.mono { font-family: Consolas, monospace; font-size: 12px; color: #909399; }
.form-tip { font-size: 12px; color: #909399; line-height: 1.6; }
.detail-head { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.detail-manager { font-size: 13px; color: #606266; }
.detail-actions { margin-left: auto; display: flex; gap: 8px; }
.stat-cards { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 10px; margin-bottom: 14px; }
.stat-card { background: #f7f9fc; border-radius: 8px; padding: 12px; text-align: center; }
.stat-card.danger { background: #fef1f0; }
.stat-num { font-size: 20px; font-weight: 600; color: #1f2d3d; }
.stat-card.danger .stat-num { color: #e6483d; }
.stat-label { font-size: 12px; color: #909399; margin-top: 2px; }
.assignee-rank { margin-top: 16px; }
.rank-title { font-size: 13px; font-weight: 600; color: #1f2d3d; margin-bottom: 8px; }
.tab-toolbar { margin-bottom: 10px; }
.tab-tip { margin-top: 10px; font-size: 12px; color: #909399; }
</style>
