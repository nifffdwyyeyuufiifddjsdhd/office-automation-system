<template>
    <div class="finance-page">
        <div class="page-head">
            <div class="head-left">
                <span class="head-bar"></span>
                <div>
                    <h2>费用类别</h2>
                    <p>维护差旅费、办公费、招待费等费用类别，财务统计按识别关键词自动归类</p>
                </div>
            </div>
            <el-button v-if="canAdd" type="primary" @click="openCreate">新增类别</el-button>
        </div>

        <el-card shadow="never" class="main-card">
            <el-table v-loading="loading" :data="rows" stripe empty-text="暂无费用类别">
                <el-table-column prop="categoryName" label="类别名称" width="140" />
                <el-table-column prop="categoryCode" label="类别编码" width="140" />
                <el-table-column label="识别关键词" min-width="280">
                    <template #default="{ row }">
                        <template v-if="row.keywords">
                            <el-tag v-for="word in row.keywords.split(',')" :key="word" class="kw-tag" size="small" effect="plain">
                                {{ word }}
                            </el-tag>
                        </template>
                        <span v-else class="kw-default">无关键词（作为兜底类别）</span>
                    </template>
                </el-table-column>
                <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
                <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
                            {{ row.status === 1 ? '启用' : '停用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
                <el-table-column v-if="canEdit || canDelete" label="操作" width="150" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="canEdit" link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button v-if="canDelete" link type="danger" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="dialogVisible" :title="editingId ? '编辑费用类别' : '新增费用类别'" width="520px">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
                <el-form-item label="类别名称" prop="categoryName">
                    <el-input v-model="form.categoryName" placeholder="如：差旅费" maxlength="50" />
                </el-form-item>
                <el-form-item label="类别编码" prop="categoryCode">
                    <el-input v-model="form.categoryCode" placeholder="不填则自动生成，如：TRAVEL" maxlength="50" />
                </el-form-item>
                <el-form-item label="识别关键词">
                    <el-input
                        v-model="form.keywords"
                        type="textarea"
                        :rows="3"
                        placeholder="逗号分隔，如：差旅,出差,交通,住宿；留空表示作为兜底类别"
                    />
                </el-form-item>
                <el-form-item label="排序号">
                    <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="1">启用</el-radio>
                        <el-radio :value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hasPerm } from '@/utils/permission'
import {
    createExpenseCategory,
    deleteExpenseCategory,
    getExpenseCategories,
    updateExpenseCategory,
    type ExpenseCategoryItem,
} from '@/api/finance'

// 三个按钮各自一个权限码，后端同名注解再拦一层
const canAdd = computed(() => hasPerm('finance:category:add'))
const canEdit = computed(() => hasPerm('finance:category:edit'))
const canDelete = computed(() => hasPerm('finance:category:delete'))

const loading = ref(false)
const rows = ref<ExpenseCategoryItem[]>([])

const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const form = reactive({
    categoryName: '',
    categoryCode: '',
    keywords: '',
    sortOrder: 0,
    status: 1,
    remark: '',
})

const rules = {
    categoryName: [{ required: true, message: '请输入类别名称', trigger: 'blur' }],
}

async function loadRows() {
    loading.value = true
    try {
        rows.value = await getExpenseCategories()
    } catch (e: any) {
        ElMessage.error('类别列表加载失败：' + (e?.message || '请稍后重试'))
    } finally {
        loading.value = false
    }
}

function resetForm() {
    form.categoryName = ''
    form.categoryCode = ''
    form.keywords = ''
    form.sortOrder = 0
    form.status = 1
    form.remark = ''
}

function openCreate() {
    editingId.value = null
    resetForm()
    dialogVisible.value = true
}

function openEdit(row: ExpenseCategoryItem) {
    editingId.value = row.id
    form.categoryName = row.categoryName
    form.categoryCode = row.categoryCode
    form.keywords = row.keywords || ''
    form.sortOrder = row.sortOrder ?? 0
    form.status = row.status ?? 1
    form.remark = row.remark || ''
    dialogVisible.value = true
}

async function submit() {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
    submitting.value = true
    try {
        if (editingId.value) {
            await updateExpenseCategory({ id: editingId.value, ...form })
            ElMessage.success('已保存')
        } else {
            await createExpenseCategory({ ...form })
            ElMessage.success('已新增')
        }
        dialogVisible.value = false
        loadRows()
    } catch (e: any) {
        ElMessage.error('保存失败：' + (e?.message || '请稍后重试'))
    } finally {
        submitting.value = false
    }
}

async function handleDelete(row: ExpenseCategoryItem) {
    try {
        await ElMessageBox.confirm(
            '删除「' + row.categoryName + '」后，未归类的报销单会落到兜底类别，确认删除？',
            '删除费用类别',
            { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
        )
    } catch {
        return
    }
    try {
        await deleteExpenseCategory(row.id)
        ElMessage.success('已删除')
        loadRows()
    } catch (e: any) {
        ElMessage.error('删除失败：' + (e?.message || '请稍后重试'))
    }
}

onMounted(loadRows)
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

.main-card {
    border-radius: 10px;
}

.kw-tag {
    margin: 2px 6px 2px 0;
}

.kw-default {
    font-size: 12px;
    color: #b0b6be;
}
</style>
