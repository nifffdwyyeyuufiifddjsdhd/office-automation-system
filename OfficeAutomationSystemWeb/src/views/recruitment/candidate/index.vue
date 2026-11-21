<template>
    <div class="recruit-page">
        <div class="page-head">
            <div class="head-left">
                <span class="head-bar"></span>
                <div>
                    <h2>候选人管理</h2>
                    <p>按岗位维护候选人：待筛选 → 待面试 → 面试通过 → 已入职，入职后走入职申请流程</p>
                </div>
            </div>
            <el-button v-if="canAdd" type="primary" @click="openCreate">录入候选人</el-button>
        </div>

        <div class="stat-grid">
            <el-card shadow="never" class="stat-card">
                <div class="stat-icon screening"><el-icon><User /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-value">{{ overview.screening }}</div>
                    <div class="stat-label">待筛选</div>
                </div>
            </el-card>
            <el-card shadow="never" class="stat-card">
                <div class="stat-icon interview"><el-icon><Calendar /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-value">{{ overview.interview }}</div>
                    <div class="stat-label">待面试</div>
                </div>
            </el-card>
            <el-card shadow="never" class="stat-card">
                <div class="stat-icon passed"><el-icon><CircleCheck /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-value">{{ overview.passed }}</div>
                    <div class="stat-label">面试通过</div>
                </div>
            </el-card>
            <el-card shadow="never" class="stat-card">
                <div class="stat-icon hired"><el-icon><Star /></el-icon></div>
                <div class="stat-body">
                    <div class="stat-value">{{ overview.hired }}</div>
                    <div class="stat-label">已入职</div>
                </div>
            </el-card>
        </div>

        <el-card shadow="never" class="filter-card">
            <div class="filters">
                <el-input v-model="query.keyword" placeholder="姓名 / 手机 / 邮箱" clearable style="width: 200px" @keyup.enter="search" />
                <el-select v-model="query.positionId" placeholder="全部岗位" clearable filterable style="width: 190px">
                    <el-option v-for="item in positions" :key="item.id" :label="item.positionName" :value="item.id" />
                </el-select>
                <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
                    <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
                <el-button type="primary" @click="search">查询</el-button>
                <el-button @click="resetQuery">重置</el-button>
                <span class="hint">共 {{ total }} 位候选人</span>
            </div>
        </el-card>

        <el-card shadow="never" class="main-card">
            <el-table v-loading="loading" :data="rows" stripe empty-text="暂无候选人">
                <el-table-column prop="candidateName" label="姓名" width="90" />
                <el-table-column label="性别" width="62" align="center">
                    <template #default="{ row }">{{ row.gender === 2 ? "女" : "男" }}</template>
                </el-table-column>
                <el-table-column prop="phone" label="联系电话" width="120" />
                <el-table-column prop="positionName" label="应聘岗位" min-width="120" show-overflow-tooltip />
                <el-table-column prop="education" label="学历" width="104" show-overflow-tooltip />
                <el-table-column label="经验" width="72" align="center">
                    <template #default="{ row }">
                        <span v-if="row.experienceYears">{{ row.experienceYears }} 年</span>
                        <span v-else class="muted">应届</span>
                    </template>
                </el-table-column>
                <el-table-column prop="source" label="来源" width="96" show-overflow-tooltip />
                <el-table-column label="期望薪资" width="96" align="right">
                    <template #default="{ row }">
                        <span v-if="row.expectedSalary">{{ money(row.expectedSalary) }}</span>
                        <span v-else class="muted">-</span>
                    </template>
                </el-table-column>
                <el-table-column label="面试时间" width="130">
                    <template #default="{ row }">
                        <span v-if="row.interviewTime">{{ formatTime(row.interviewTime) }}</span>
                        <span v-else class="muted">未安排</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="86" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusType(row.status)" size="small" effect="light">{{ row.statusText }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="180" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="canStatus && row.status === 1" link type="primary" @click="openInterview(row)">安排面试</el-button>
                        <el-button v-if="canStatus && row.status === 2" link type="success" @click="passInterview(row)">面试通过</el-button>
                        <el-button v-if="canStatus && row.status === 3" link type="primary" @click="hire(row)">办理入职</el-button>
                        <el-button v-if="canStatus && row.status === 5" link type="warning" @click="restore(row)">恢复</el-button>
                        <el-dropdown v-if="row.status !== 4 && moreItems(row).length > 0" trigger="click" @command="(cmd: string) => handleMore(cmd, row)">
                            <el-button link type="primary">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-for="item in moreItems(row)" :key="item.cmd" :command="item.cmd">{{ item.label }}</el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
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

        <el-dialog v-model="dialogVisible" :title="editingId ? '编辑候选人' : '录入候选人'" width="620px">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
                <el-form-item label="姓名" prop="candidateName">
                    <el-input v-model="form.candidateName" maxlength="30" />
                </el-form-item>
                <el-form-item label="性别">
                    <el-radio-group v-model="form.gender">
                        <el-radio :value="1">男</el-radio>
                        <el-radio :value="2">女</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                    <el-input v-model="form.phone" maxlength="20" placeholder="11 位手机号" />
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model="form.email" maxlength="60" />
                </el-form-item>
                <el-form-item label="应聘岗位" prop="positionId">
                    <el-select v-model="form.positionId" placeholder="请选择岗位" filterable style="width: 100%">
                        <el-option v-for="item in positions" :key="item.id" :label="item.positionName" :value="item.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="学历">
                    <el-select v-model="form.education" placeholder="不限" clearable style="width: 100%">
                        <el-option label="不限" value="不限" />
                        <el-option label="大专" value="大专" />
                        <el-option label="本科" value="本科" />
                        <el-option label="硕士及以上" value="硕士及以上" />
                    </el-select>
                </el-form-item>
                <el-form-item label="工作年限">
                    <el-input-number v-model="form.experienceYears" :min="0" :max="40" />
                    <span class="unit">年</span>
                </el-form-item>
                <el-form-item label="简历来源">
                    <el-input v-model="form.source" placeholder="如：BOSS直聘、内推、校招" maxlength="50" />
                </el-form-item>
                <el-form-item label="期望薪资">
                    <el-input-number v-model="form.expectedSalary" :min="0" :max="1000000" :step="500" />
                    <span class="unit">元 / 月</span>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="interviewVisible" title="安排面试" width="480px">
            <el-form :model="interviewForm" label-width="90px">
                <el-form-item label="候选人">
                    <span>{{ interviewForm.candidateName }}</span>
                </el-form-item>
                <el-form-item label="面试时间" required>
                    <el-date-picker v-model="interviewForm.interviewTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择面试时间" style="width: 100%" />
                </el-form-item>
                <el-form-item label="面试官">
                    <el-input v-model="interviewForm.interviewer" placeholder="如：王主管" maxlength="30" />
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="interviewForm.evaluation" type="textarea" :rows="2" maxlength="300" show-word-limit />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="interviewVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="submitInterview">确定安排</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { User, Calendar, CircleCheck, Star, ArrowDown } from "@element-plus/icons-vue"
import { hasPerm } from "@/utils/permission"
import {
    createCandidate, deleteCandidate, getRecruitmentOverview, pageCandidates, positionOptions,
    updateCandidate, updateCandidateStatus,
    type RecruitmentCandidateItem, type RecruitmentOverview, type RecruitmentPositionItem,
} from "@/api/recruitment"

const canAdd = computed(() => hasPerm("recruitment:candidate:add"))
const canEdit = computed(() => hasPerm("recruitment:candidate:edit"))
const canStatus = computed(() => hasPerm("recruitment:candidate:status"))
const canDelete = computed(() => hasPerm("recruitment:candidate:delete"))

const statusOptions = [
    { value: 1, label: "待筛选" },
    { value: 2, label: "待面试" },
    { value: 3, label: "面试通过" },
    { value: 4, label: "已入职" },
    { value: 5, label: "已淘汰" },
]

const loading = ref(false)
const submitting = ref(false)
const rows = ref<RecruitmentCandidateItem[]>([])
const positions = ref<RecruitmentPositionItem[]>([])
const total = ref(0)
const overview = reactive<RecruitmentOverview>({
    openPositions: 0, headcount: 0, screening: 0, interview: 0, passed: 0, hired: 0, rejected: 0,
})
const query = reactive({
    pageNum: 1, pageSize: 10, keyword: "",
    positionId: undefined as number | undefined,
    status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const form = reactive({
    candidateName: "", gender: 1, phone: "", email: "",
    positionId: null as number | null, education: "", experienceYears: 0,
    source: "", expectedSalary: 0, remark: "",
})
const rules = {
    candidateName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
    phone: [{ pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" }],
    positionId: [{ required: true, message: "请选择应聘岗位", trigger: "change" }],
}

const interviewVisible = ref(false)
const interviewForm = reactive({
    id: 0, candidateName: "",
    interviewTime: "" as string | null, interviewer: "", evaluation: "",
})

function money(value: number | null | undefined) {
    if (value === null || value === undefined) return "-"
    return Number(value).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function statusType(status: number) {
    if (status === 1) return "info"
    if (status === 2) return "warning"
    if (status === 3) return "primary"
    if (status === 4) return "success"
    return "danger"
}

async function loadRows() {
    loading.value = true
    try {
        const data = await pageCandidates({
            pageNum: query.pageNum,
            pageSize: query.pageSize,
            keyword: query.keyword || undefined,
            positionId: query.positionId,
            status: query.status,
        })
        rows.value = data.records
        total.value = data.total
    } finally {
        loading.value = false
    }
}

async function loadOverview() {
    const data = await getRecruitmentOverview()
    Object.assign(overview, data)
}

async function loadPositions() {
    positions.value = await positionOptions()
}

function search() {
    query.pageNum = 1
    loadRows()
}

function resetQuery() {
    query.keyword = ""
    query.positionId = undefined
    query.status = undefined
    search()
}

async function refreshAll() {
    await Promise.all([loadRows(), loadOverview()])
}

function resetForm() {
    form.candidateName = ""
    form.gender = 1
    form.phone = ""
    form.email = ""
    form.positionId = null
    form.education = ""
    form.experienceYears = 0
    form.source = ""
    form.expectedSalary = 0
    form.remark = ""
}

function openCreate() {
    editingId.value = null
    resetForm()
    dialogVisible.value = true
}

function openEdit(row: RecruitmentCandidateItem) {
    editingId.value = row.id
    form.candidateName = row.candidateName
    form.gender = row.gender || 1
    form.phone = row.phone || ""
    form.email = row.email || ""
    form.positionId = row.positionId
    form.education = row.education || ""
    form.experienceYears = row.experienceYears || 0
    form.source = row.source || ""
    form.expectedSalary = row.expectedSalary || 0
    form.remark = row.remark || ""
    dialogVisible.value = true
}

async function submit() {
    if (formRef.value) {
        const valid = await formRef.value.validate().catch(() => false)
        if (!valid) return
    }
    submitting.value = true
    try {
        const payload = {
            candidateName: form.candidateName,
            gender: form.gender,
            phone: form.phone || null,
            email: form.email || null,
            positionId: form.positionId,
            education: form.education || null,
            experienceYears: form.experienceYears,
            source: form.source || null,
            expectedSalary: form.expectedSalary || null,
            remark: form.remark || null,
        }
        if (editingId.value) {
            await updateCandidate(editingId.value, payload)
            ElMessage.success("已保存")
        } else {
            await createCandidate(payload)
            ElMessage.success("已录入")
        }
        dialogVisible.value = false
        refreshAll()
    } finally {
        submitting.value = false
    }
}

function openInterview(row: RecruitmentCandidateItem) {
    interviewForm.id = row.id
    interviewForm.candidateName = row.candidateName
    interviewForm.interviewTime = row.interviewTime
    interviewForm.interviewer = row.interviewer || ""
    interviewForm.evaluation = row.evaluation || ""
    interviewVisible.value = true
}

async function submitInterview() {
    if (!interviewForm.interviewTime) {
        ElMessage.warning("请选择面试时间")
        return
    }
    submitting.value = true
    try {
        await updateCandidateStatus(interviewForm.id, {
            status: 2,
            interviewTime: interviewForm.interviewTime,
            interviewer: interviewForm.interviewer || null,
            evaluation: interviewForm.evaluation || null,
        })
        ElMessage.success("已安排面试")
        interviewVisible.value = false
        refreshAll()
    } finally {
        submitting.value = false
    }
}

async function changeStatus(row: RecruitmentCandidateItem, status: number, tip: string, payload?: Record<string, unknown>) {
    try {
        await ElMessageBox.confirm("确定将「" + row.candidateName + "」" + tip + "吗？", "提示", { type: "warning" })
    } catch {
        return
    }
    await updateCandidateStatus(row.id, { status, ...(payload || {}) })
    ElMessage.success("操作成功")
    refreshAll()
}

// 时间列只显示月-日 时:分，完整值放 title
function formatTime(value: string) {
    const text = value.replace("T", " ")
    return text.length >= 16 ? text.slice(5, 16) : text
}

// 次要操作收进更多下拉，状态列才不被挤掉
function moreItems(row: RecruitmentCandidateItem) {
    const items: { cmd: string, label: string }[] = []
    if (canEdit.value && row.status !== 4) items.push({ cmd: "edit", label: "编辑" })
    if (canStatus.value && (row.status === 1 || row.status === 2 || row.status === 3)) items.push({ cmd: "reject", label: "淘汰" })
    if (canDelete.value && row.status !== 4) items.push({ cmd: "delete", label: "删除" })
    return items
}

function handleMore(cmd: string, row: RecruitmentCandidateItem) {
    if (cmd === "edit") openEdit(row)
    else if (cmd === "reject") reject(row)
    else if (cmd === "delete") handleDelete(row)
}

function passInterview(row: RecruitmentCandidateItem) {
    changeStatus(row, 3, "标记为面试通过")
}

function hire(row: RecruitmentCandidateItem) {
    changeStatus(row, 4, "办理入职（岗位需在招且未招满）")
}

function reject(row: RecruitmentCandidateItem) {
    changeStatus(row, 5, "淘汰")
}

function restore(row: RecruitmentCandidateItem) {
    changeStatus(row, 1, "恢复为待筛选")
}

async function handleDelete(row: RecruitmentCandidateItem) {
    try {
        await ElMessageBox.confirm("确定删除候选人「" + row.candidateName + "」吗？", "提示", { type: "warning" })
    } catch {
        return
    }
    await deleteCandidate(row.id)
    ElMessage.success("已删除")
    refreshAll()
}

onMounted(() => {
    loadPositions()
    refreshAll()
})
</script>

<style scoped>
.recruit-page { padding: 16px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.head-left { display: flex; align-items: center; gap: 10px; }
.head-bar { width: 4px; height: 34px; background: #409eff; border-radius: 2px; }
.page-head h2 { margin: 0; font-size: 18px; }
.page-head p { margin: 2px 0 0; font-size: 12px; color: #909399; }
.stat-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 12px; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; gap: 12px; padding: 16px; }
.stat-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; }
.stat-icon.screening { background: linear-gradient(135deg, #909399, #c0c4cc); }
.stat-icon.interview { background: linear-gradient(135deg, #e6a23c, #f7c873); }
.stat-icon.passed { background: linear-gradient(135deg, #409eff, #79bbff); }
.stat-icon.hired { background: linear-gradient(135deg, #67c23a, #95d475); }
.stat-value { font-size: 22px; font-weight: 600; line-height: 1.2; }
.stat-label { font-size: 12px; color: #909399; }
.filter-card { margin-bottom: 12px; }
.filters { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.hint { margin-left: auto; font-size: 12px; color: #909399; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.muted { color: #c0c4cc; }
.unit { margin-left: 8px; font-size: 12px; color: #909399; }
</style>