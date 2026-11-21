<template>
    <div class="recruit-page">
        <div class="page-head">
            <div class="head-left">
                <span class="head-bar"></span>
                <div>
                    <h2>招聘岗位</h2>
                    <p>人事发布在招岗位，候选人按岗位投递，入职后走入职申请流程</p>
                </div>
            </div>
            <el-button v-if="canAdd" type="primary" @click="openCreate">发布岗位</el-button>
        </div>

        <el-card shadow="never" class="filter-card">
            <div class="filters">
                <el-input v-model="query.keyword" placeholder="岗位名称 / 工作地点" clearable style="width: 220px" @keyup.enter="search" />
                <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
                    <el-option label="在招" :value="1" />
                    <el-option label="已停招" :value="0" />
                    <el-option label="已结束" :value="2" />
                </el-select>
                <el-button type="primary" @click="search">查询</el-button>
                <el-button @click="resetQuery">重置</el-button>
                <span class="hint">共 {{ total }} 个岗位</span>
            </div>
        </el-card>

        <el-card shadow="never" class="main-card">
            <el-table v-loading="loading" :data="rows" stripe empty-text="暂无招聘岗位">
                <el-table-column prop="positionName" label="岗位名称" min-width="140" show-overflow-tooltip />
                <el-table-column prop="deptName" label="用人部门" width="100" show-overflow-tooltip />
                <el-table-column prop="positionLevelName" label="对应职位" width="100" show-overflow-tooltip />
                <el-table-column prop="headcount" label="招聘人数" width="84" align="center" />
                <el-table-column prop="education" label="学历" width="76" />
                <el-table-column label="经验" width="72" align="center">
                    <template #default="{ row }">
                        <span v-if="row.experienceYears">{{ row.experienceYears }} 年</span>
                        <span v-else class="muted">不限</span>
                    </template>
                </el-table-column>
                <el-table-column prop="salaryRange" label="薪资范围" width="110" show-overflow-tooltip />
                <el-table-column label="候选人" width="100" align="center">
                    <template #default="{ row }">
                        <span class="count-main">{{ row.candidateCount }}</span>
                        <span class="count-sub">已入职 {{ row.hiredCount }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusType(row.status)" size="small" effect="light">{{ row.statusText }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="deadline" label="截止日期" width="100" />
                <el-table-column v-if="canEdit || canDelete" label="操作" width="172" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="canEdit" link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button v-if="canEdit" link type="warning" @click="toggleStatus(row)">
                            {{ row.status === 1 ? "停招" : "重开" }}
                        </el-button>
                        <el-button v-if="canDelete" link type="danger" @click="handleDelete(row)">删除</el-button>
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

        <el-dialog v-model="dialogVisible" :title="editingId ? '编辑招聘岗位' : '发布招聘岗位'" width="640px">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
                <el-form-item label="岗位名称" prop="positionName">
                    <el-input v-model="form.positionName" placeholder="如：Java 后端开发工程师" maxlength="50" />
                </el-form-item>
                <el-form-item label="用人部门">
                    <el-select v-model="form.deptId" placeholder="请选择部门" clearable style="width: 100%">
                        <el-option v-for="org in orgOptions" :key="org.id" :label="org.label" :value="org.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="对应职位">
                    <el-input v-model="form.positionLevelName" placeholder="填写职位名称用于定级，如：开发工程师" maxlength="50" />
                </el-form-item>
                <el-form-item label="招聘人数" prop="headcount">
                    <el-input-number v-model="form.headcount" :min="1" :max="999" />
                </el-form-item>
                <el-form-item label="学历要求">
                    <el-select v-model="form.education" placeholder="不限" clearable style="width: 100%">
                        <el-option label="不限" value="不限" />
                        <el-option label="大专" value="大专" />
                        <el-option label="本科" value="本科" />
                        <el-option label="硕士及以上" value="硕士及以上" />
                    </el-select>
                </el-form-item>
                <el-form-item label="经验要求">
                    <el-input-number v-model="form.experienceYears" :min="0" :max="30" />
                    <span class="unit">年，0 表示不限</span>
                </el-form-item>
                <el-form-item label="薪资范围">
                    <el-input v-model="form.salaryRange" placeholder="如：8k-12k" maxlength="50" />
                </el-form-item>
                <el-form-item label="工作地点">
                    <el-input v-model="form.workPlace" placeholder="如：广州天河" maxlength="100" />
                </el-form-item>
                <el-form-item label="截止日期">
                    <el-date-picker v-model="form.deadline" type="date" value-format="YYYY-MM-DD" placeholder="不填表示长期有效" style="width: 100%" />
                </el-form-item>
                <el-form-item label="岗位职责">
                    <el-input v-model="form.duty" type="textarea" :rows="3" maxlength="1000" show-word-limit />
                </el-form-item>
                <el-form-item label="任职要求">
                    <el-input v-model="form.requirement" type="textarea" :rows="3" maxlength="1000" show-word-limit />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="1">在招</el-radio>
                        <el-radio :value="0">已停招</el-radio>
                        <el-radio :value="2">已结束</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { hasPerm } from "@/utils/permission"
import { orgTreeEnabled, type OrgNode } from "@/api/user"
import {
    createPosition, deletePosition, pagePositions, updatePosition, updatePositionStatus,
    type RecruitmentPositionItem,
} from "@/api/recruitment"

const canAdd = computed(() => hasPerm("recruitment:position:add"))
const canEdit = computed(() => hasPerm("recruitment:position:edit"))
const canDelete = computed(() => hasPerm("recruitment:position:delete"))

const loading = ref(false)
const submitting = ref(false)
const rows = ref<RecruitmentPositionItem[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: "", status: undefined as number | undefined })

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const orgOptions = ref<Array<{ id: number; label: string }>>([])
const form = reactive({
    positionName: "",
    deptId: null as number | null,
    positionLevelName: "",
    headcount: 1,
    education: "不限",
    experienceYears: 0,
    salaryRange: "",
    workPlace: "",
    deadline: null as string | null,
    duty: "",
    requirement: "",
    status: 1,
})

const rules = {
    positionName: [{ required: true, message: "请输入岗位名称", trigger: "blur" }],
}

function statusType(status: number) {
    if (status === 1) return "success"
    if (status === 2) return "info"
    return "warning"
}

async function loadRows() {
    loading.value = true
    try {
        const data = await pagePositions({
            pageNum: query.pageNum,
            pageSize: query.pageSize,
            keyword: query.keyword || undefined,
            status: query.status,
        })
        rows.value = data.records
        total.value = data.total
    } finally {
        loading.value = false
    }
}

function search() {
    query.pageNum = 1
    loadRows()
}

function resetQuery() {
    query.keyword = ""
    query.status = undefined
    search()
}

function resetForm() {
    form.positionName = ""
    form.deptId = null
    form.positionLevelName = ""
    form.headcount = 1
    form.education = "不限"
    form.experienceYears = 0
    form.salaryRange = ""
    form.workPlace = ""
    form.deadline = null
    form.duty = ""
    form.requirement = ""
    form.status = 1
}

function openCreate() {
    editingId.value = null
    resetForm()
    dialogVisible.value = true
}

function openEdit(row: RecruitmentPositionItem) {
    editingId.value = row.id
    form.positionName = row.positionName
    form.deptId = row.deptId
    form.positionLevelName = row.positionLevelName || ""
    form.headcount = row.headcount || 1
    form.education = row.education || "不限"
    form.experienceYears = row.experienceYears || 0
    form.salaryRange = row.salaryRange || ""
    form.workPlace = row.workPlace || ""
    form.deadline = row.deadline
    form.duty = row.duty || ""
    form.requirement = row.requirement || ""
    form.status = row.status
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
            positionName: form.positionName,
            deptId: form.deptId,
            headcount: form.headcount,
            education: form.education,
            experienceYears: form.experienceYears,
            salaryRange: form.salaryRange,
            workPlace: form.workPlace,
            deadline: form.deadline,
            duty: form.duty,
            requirement: form.requirement,
            status: form.status,
        }
        if (editingId.value) {
            await updatePosition(editingId.value, payload)
            ElMessage.success("已保存")
        } else {
            await createPosition(payload)
            ElMessage.success("已发布")
        }
        dialogVisible.value = false
        loadRows()
    } finally {
        submitting.value = false
    }
}

async function toggleStatus(row: RecruitmentPositionItem) {
    const next = row.status === 1 ? 0 : 1
    await updatePositionStatus(row.id, next)
    ElMessage.success(next === 1 ? "已重新开始招聘" : "已停招")
    loadRows()
}

async function handleDelete(row: RecruitmentPositionItem) {
    try {
        await ElMessageBox.confirm("确定删除岗位「" + row.positionName + "」吗？还有候选人时无法删除。", "提示", { type: "warning" })
    } catch {
        return
    }
    await deletePosition(row.id)
    ElMessage.success("已删除")
    loadRows()
}

// 部门下拉只要登录就能读，不需要 org:manage
async function loadOrgs() {
    try {
        const tree = await orgTreeEnabled()
        const flat: Array<{ id: number; label: string }> = []
        const walk = (nodes: OrgNode[], prefix: string) => {
            for (const node of nodes) {
                flat.push({ id: node.id, label: prefix + node.label })
                if (node.children && node.children.length) walk(node.children, prefix + node.label + " / ")
            }
        }
        walk(tree || [], "")
        orgOptions.value = flat
    } catch {
        orgOptions.value = []
    }
}

onMounted(() => {
    loadOrgs()
    loadRows()
})
</script>

<style scoped>
.recruit-page { padding: 16px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.head-left { display: flex; align-items: center; gap: 10px; }
.head-bar { width: 4px; height: 34px; background: #409eff; border-radius: 2px; }
.page-head h2 { margin: 0; font-size: 18px; }
.page-head p { margin: 2px 0 0; font-size: 12px; color: #909399; }
.filter-card { margin-bottom: 12px; }
.filters { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.hint { margin-left: auto; font-size: 12px; color: #909399; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.count-main { font-weight: 600; margin-right: 6px; }
.count-sub { font-size: 12px; color: #909399; }
.muted { color: #c0c4cc; }
.unit { margin-left: 8px; font-size: 12px; color: #909399; }
</style>