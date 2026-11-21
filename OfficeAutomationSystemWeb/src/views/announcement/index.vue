<script setup lang="ts">
import { ref, computed, onMounted, reactive, shallowRef, onBeforeUnmount, defineAsyncComponent } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Top } from '@element-plus/icons-vue'
import {
    pageAnnouncements, getAnnouncementDetail, addAnnouncement, updateAnnouncement,
    deleteAnnouncement, publishAnnouncement, offlineAnnouncement, batchPublishAnnouncements,
    toggleTop, getCategoryTree, addCategory, updateCategory, deleteCategory, toggleCategoryStatus,
    uploadAnnouncementImage,
    type AnnouncementItem, type AnnouncementCategory, type PageResult
} from '@/api/announcement'
import { hasPerm } from '@/utils/permission'
import { sanitizeHtml } from '@/utils/safeHtml'
import { ANNOUNCEMENT_TEMPLATES, ANNOUNCEMENT_THEMES, themeClass } from '@/constants/announcementContent'

// 权限
// 是否有管理权限，按钮权限由 v-permission 控制
const isManager = computed(() => hasPerm('announcement:add'))

// 查询与列表
const loading = ref(false)
const list = ref<AnnouncementItem[]>([])
const total = ref(0)
const categories = ref<AnnouncementCategory[]>([])
const selection = ref<AnnouncementItem[]>([])

const query = reactive({
    pageNum: 1,
    pageSize: 10,
    categoryId: null as number | null,
    keyword: '',
    status: null as number | null
})

async function loadCategories() {
    try {
        categories.value = await getCategoryTree()
    } catch { /* 类别加载失败不阻塞列表 */ }
}

async function load() {
    loading.value = true
    try {
        const res: PageResult<AnnouncementItem> = await pageAnnouncements(query)
        list.value = res.records
        total.value = res.total
    } catch { /* 错误提示已在拦截器处理 */ } finally {
        loading.value = false
    }
}

function reload() {
    query.pageNum = 1
    load()
}

function onSelectionChange(rows: AnnouncementItem[]) {
    selection.value = rows
}

// 时间格式化
function formatTime(time?: string) {
    if (!time) return '-'
    const d = new Date(time)
    const p = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

// 详情
const detailVisible = ref(false)
const detail = ref<Partial<AnnouncementItem>>({})


async function openDetail(row: AnnouncementItem) {
    detail.value = await getAnnouncementDetail(row.id)   // 后端会自动标记已读
    detailVisible.value = true
    if (!row.is_read) load()                              // 刷新未读状态
}

// 新增 / 编辑
const formVisible = ref(false)
const formTitle = ref('新增公告')
const formLoading = ref(false)
const form = reactive({
    id: null as number | null,
    title: '',
    category_id: null as number | null,
    summary: '',
    content: '',
    theme: 'standard',
    is_top: 0,
    status: 0
})

// 主题说明跟在选择框后面，让人知道选这个主题长什么样
const themeDesc = computed(() => ANNOUNCEMENT_THEMES.find(t => t.key === form.theme)?.desc || '')

// 富文本编辑器（按需加载）
// 编辑器 JS 加样式约 800KB，列表页用不到
// 异步加载，第一次打开弹窗时才下载
const loadEditorKit = () => Promise.all([
    import('@wangeditor/editor-for-vue'),
    import('@wangeditor/editor/dist/css/style.css')
]).then(([mod]) => mod)
const Editor = defineAsyncComponent(() => loadEditorKit().then(mod => mod.Editor as any))
const Toolbar = defineAsyncComponent(() => loadEditorKit().then(mod => mod.Toolbar as any))

const editorRef = shallowRef()

// 工具栏只留后端存得下的功能：加粗、列表、表格、链接、图片
// 正文的 style、class、align 会被后端清掉，对应按钮去掉
const toolbarConfig = {
    excludeKeys: [
        'color', 'bgColor', 'fontSize', 'fontFamily', 'lineHeight',
        'indent', 'delIndent', 'justify', 'group-indent', 'group-justify',
        'emotion', 'video', 'insertVideo', 'uploadVideo', 'group-video',
        'todo', 'fullScreen'
    ]
}

const editorConfig = {
    placeholder: '请输入公告正文…',
    MENU_CONF: {
        // 插图走后端上传，返回的地址免登录可读
        uploadImage: {
            server: '/api/announcements/upload-image',
            fieldName: 'file',
            maxFileSize: 5 * 1024 * 1024,
            allowedFileTypes: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
            headers: { Authorization: 'Bearer ' + (sessionStorage.getItem('token') || '') },
            customInsert(res: any, insertFn: (url: string, alt: string, href: string) => void) {
                const url = res?.data?.url || res?.url
                if (url) insertFn(url, '', '')
                else ElMessage.error('图片上传失败，请重试')
            }
        }
    }
}
const handleCreated = (editor: any) => { editorRef.value = editor }

// 套用模板前先确认，正文已有内容时问一句
async function applyTemplate(key: string) {
    const tpl = ANNOUNCEMENT_TEMPLATES.find(t => t.key === key)
    if (!tpl) return
    if (form.content && form.content.trim()) {
        try {
            await ElMessageBox.confirm('套用模板会覆盖当前正文，确定继续吗？', '提示', { type: 'warning' })
        } catch {
            return
        }
    }
    const editor = editorRef.value
    if (!editor) {
        form.content = tpl.html
        return
    }
    // 用 setHtml 整体替换正文
    // dangerouslyInsertHtml 要光标在正文里，刚开弹窗会插入失败
    editor.setHtml(tpl.html || '')
    form.content = tpl.html
}
const destroyEditor = () => {
    const editor = editorRef.value
    if (!editor) return
    editor.destroy()
    editorRef.value = null
}
onBeforeUnmount(destroyEditor)

function openCreate() {
    formTitle.value = '新增公告'
    Object.assign(form, { id: null, title: '', category_id: null, summary: '', content: '', theme: 'standard', is_top: 0, status: 0 })
    formVisible.value = true
}

function openEdit(row: AnnouncementItem) {
    formTitle.value = '编辑公告'
    Object.assign(form, {
        id: row.id,
        title: row.title,
        category_id: row.category_id,
        summary: row.summary || '',
        content: row.content || '',
        theme: row.theme || 'standard',
        is_top: row.is_top,
        status: row.status
    })
    formVisible.value = true
}

async function submitForm() {
    if (!form.title.trim()) { ElMessage.warning('请输入公告标题'); return }
    if (!form.category_id) { ElMessage.warning('请选择公告分类'); return }
    formLoading.value = true
    try {
        const payload = {
            id: form.id ?? undefined,
            title: form.title,
            category_id: form.category_id as number,
            summary: form.summary,
            content: form.content,
            theme: form.theme,
            is_top: form.is_top,
            status: form.status
        }
        if (form.id) {
            await updateAnnouncement(payload)
            ElMessage.success('修改成功')
        } else {
            await addAnnouncement(payload)
            ElMessage.success('新增成功')
        }
        formVisible.value = false
        load()
    } catch { /* 错误提示已在拦截器处理 */ } finally {
        formLoading.value = false
    }
}

// 状态操作
async function publish(row: AnnouncementItem) {
    await publishAnnouncement(row.id)
    ElMessage.success('已发布')
    load()
}

async function offline(row: AnnouncementItem) {
    await offlineAnnouncement(row.id)
    ElMessage.success('已下线')
    load()
}

async function toggleTopRow(row: AnnouncementItem) {
    await toggleTop(row.id, row.is_top === 1 ? 0 : 1)
    load()
}

async function remove(row: AnnouncementItem) {
    // 点取消直接返回，不抛异常
    try {
        await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', { type: 'warning' })
    } catch {
        return
    }
    await deleteAnnouncement(row.id)
    ElMessage.success('已删除')
    load()
}

async function batchPublish() {
    if (!selection.value.length) { ElMessage.warning('请先勾选要发布的公告'); return }
    await batchPublishAnnouncements(selection.value.map(i => i.id))
    ElMessage.success(`已批量发布 ${selection.value.length} 条`)
    load()
}

// 类别管理
const categoryVisible = ref(false)
const categoryList = ref<AnnouncementCategory[]>([])
const categoryLoading = ref(false)
const newCategoryName = ref('')
const newCategoryParent = ref<number>(0)

async function openCategoryManager() {
    categoryVisible.value = true
    await refreshCategories()
}

async function refreshCategories() {
    categoryLoading.value = true
    try {
        categoryList.value = await getCategoryTree()
    } finally {
        categoryLoading.value = false
    }
}

async function createCategory() {
    if (!newCategoryName.value.trim()) { ElMessage.warning('请输入类别名称'); return }
    await addCategory({
        category_name: newCategoryName.value.trim(),
        parent_id: newCategoryParent.value || 0,
        sort_order: 0,
        status: 1,
        view_permission_type: 1
    })
    ElMessage.success('类别已新增')
    newCategoryName.value = ''
    newCategoryParent.value = 0
    await refreshCategories()
    loadCategories()
}

async function renameCategory(cat: AnnouncementCategory) {
    let value = ''
    try {
        const res = await ElMessageBox.prompt('修改后立即生效，已发布公告的分类名称同步更新', '重命名类别', {
            inputValue: cat.category_name,
            inputPlaceholder: '类别名称',
            inputValidator: (v: string) => (v && v.trim() ? true : '类别名称不能为空'),
            type: 'info'
        })
        value = res.value.trim()
    } catch {
        return
    }
    if (value === cat.category_name) return
    // 只提交要改的字段，不回写 children
    await updateCategory({
        id: cat.id,
        category_name: value,
        parent_id: cat.parent_id,
        sort_order: cat.sort_order ?? 0,
        status: cat.status,
        view_permission_type: cat.view_permission_type
    })
    ElMessage.success('已重命名')
    await refreshCategories()
    loadCategories()
}

async function toggleCategory(cat: AnnouncementCategory) {
    await toggleCategoryStatus(cat.id, cat.status === 1 ? 0 : 1)
    await refreshCategories()
}

async function removeCategory(cat: AnnouncementCategory) {
    try {
        await ElMessageBox.confirm(`确定删除类别「${cat.category_name}」吗？`, '删除确认', { type: 'warning' })
    } catch {
        return
    }
    await deleteCategory(cat.id)
    ElMessage.success('已删除')
    await refreshCategories()
    loadCategories()
}

// 初始化
onMounted(() => {
    loadCategories()
    load()
})
</script>

<template>
    <div class="announcement-page">
        <!-- 工具栏 -->
        <el-card shadow="never" class="toolbar-card">
            <div class="toolbar">
                <div class="toolbar-left">
                    <el-button type="primary" v-permission="'announcement:add'" @click="openCreate">
                        <el-icon style="margin-right:4px"><Plus /></el-icon>新增公告
                    </el-button>
                    <el-button v-permission="'announcement:publish'" @click="batchPublish">批量发布</el-button>
                    <el-button v-permission="'announcement:category:manage'" @click="openCategoryManager">类别管理</el-button>
                </div>
                <div class="toolbar-right">
                    <el-select
                        v-model="query.categoryId" clearable placeholder="全部分类"
                        style="width:140px" @change="reload">
                        <el-option v-for="c in categories" :key="c.id" :label="c.category_name" :value="c.id" />
                    </el-select>
                    <el-select
                        v-if="isManager" v-model="query.status" clearable placeholder="全部状态"
                        style="width:120px" @change="reload">
                        <el-option label="草稿" :value="0" />
                        <el-option label="已发布" :value="1" />
                        <el-option label="已下线" :value="2" />
                    </el-select>
                    <el-input
                        v-model="query.keyword" placeholder="搜索标题/摘要" clearable
                        style="width:220px" @keyup.enter="reload" @clear="reload" />
                    <el-button type="primary" plain @click="reload">
                        <el-icon style="margin-right:4px"><Search /></el-icon>搜索
                    </el-button>
                </div>
            </div>
        </el-card>

        <!-- 列表 -->
        <el-card shadow="never">
            <el-table
                :data="list" v-loading="loading"
                @selection-change="onSelectionChange"
                :row-class-name="({ row }: any) => (!row.is_read ? 'unread-row' : '')">
                <el-table-column v-if="isManager" type="selection" width="45" />
                <el-table-column width="40">
                    <template #default="{ row }">
                        <el-icon v-if="row.is_top === 1" color="#e6a23c" size="16"><Top /></el-icon>
                    </template>
                </el-table-column>
                <el-table-column label="标题" min-width="280">
                    <template #default="{ row }">
                        <div class="title-cell">
                            <span class="title-text" :class="{ 'title-unread': !row.is_read }" @click="openDetail(row)">
                                {{ row.title }}
                            </span>
                            <el-tag v-if="row.status === 0" size="small" type="info" effect="plain">草稿</el-tag>
                            <el-tag v-else-if="row.status === 2" size="small" type="danger" effect="plain">已下线</el-tag>
                            <el-tag v-if="!row.is_read" size="small" type="danger" class="unread-tag">未读</el-tag>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="分类" width="120">
                    <template #default="{ row }">
                        <el-tag size="small" effect="plain" type="primary">{{ row.category_name }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="author_name" label="发布人" width="100" />
                <el-table-column label="发布时间" width="150">
                    <template #default="{ row }">{{ formatTime(row.publish_time) }}</template>
                </el-table-column>
                <el-table-column label="阅读情况" width="140">
                    <template #default="{ row }">
                        <span class="read-count">{{ row.read_count || 0 }} 人已读</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="230" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openDetail(row)">查看</el-button>
                        <template v-if="isManager">
                            <el-button link type="primary" v-permission="'announcement:edit'" @click="openEdit(row)">编辑</el-button>
                            <el-button
                                v-if="row.status !== 1" link type="success"
                                v-permission="'announcement:publish'" @click="publish(row)">发布</el-button>
                            <el-button
                                v-else link type="warning"
                                v-permission="'announcement:publish'" @click="offline(row)">下线</el-button>
                            <el-button link type="warning" v-permission="'announcement:top'" @click="toggleTopRow(row)">
                                {{ row.is_top === 1 ? '取消置顶' : '置顶' }}
                            </el-button>
                            <el-button link type="danger" v-permission="'announcement:delete'" @click="remove(row)">删除</el-button>
                        </template>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-wrap">
                <el-pagination
                    v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                    :total="total" :page-sizes="[10, 20, 50]"
                    layout="total, sizes, prev, pager, next, jumper" background
                    @current-change="load" @size-change="reload" />
            </div>
        </el-card>

        <!-- 详情抽屉 -->
        <el-drawer v-model="detailVisible" size="600px" :title="detail.title">
            <div class="detail-meta">
                <el-tag size="small" effect="plain" type="primary">{{ detail.category_name }}</el-tag>
                <span class="meta-text">发布人：{{ detail.author_name }}</span>
                <span class="meta-text">发布时间：{{ formatTime(detail.publish_time) }}</span>
                <span class="meta-text">{{ detail.read_count || 0 }} 人已读</span>
            </div>
            <el-divider />
            <div :class="themeClass(detail.theme)" v-html="sanitizeHtml(detail.content || '')"></div>
        </el-drawer>

        <!-- 新增 / 编辑 -->
        <el-dialog v-model="formVisible" :title="formTitle" width="820px" destroy-on-close class="ann-form-dialog">
            <el-form :model="form" label-width="80px">
                <el-form-item label="标题" required>
                    <el-input v-model="form.title" maxlength="200" placeholder="请输入公告标题" />
                </el-form-item>
                <el-form-item label="分类" required>
                    <el-select v-model="form.category_id" placeholder="请选择分类" style="width:100%">
                        <el-option v-for="c in categories" :key="c.id" :label="c.category_name" :value="c.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="摘要">
                    <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" placeholder="公告摘要（可选）" />
                </el-form-item>
                <el-form-item label="模板">
                    <div class="tpl-row">
                        <el-dropdown trigger="click" @command="applyTemplate">
                            <el-button>套用模板</el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-for="t in ANNOUNCEMENT_TEMPLATES" :key="t.key" :command="t.key">
                                        <div class="tpl-item">
                                            <span class="tpl-name">{{ t.name }}</span>
                                            <span class="tpl-desc">{{ t.desc }}</span>
                                        </div>
                                    </el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                        <span class="tpl-tip">从 Word 或网页粘贴时只保留文字、加粗、列表和表格，字号颜色会被丢掉</span>
                    </div>
                </el-form-item>
                <el-form-item label="正文主题">
                    <div class="tpl-row">
                        <el-select v-model="form.theme" style="width:180px">
                            <el-option v-for="t in ANNOUNCEMENT_THEMES" :key="t.key" :label="t.name" :value="t.key" />
                        </el-select>
                        <span class="tpl-tip">{{ themeDesc }}</span>
                    </div>
                </el-form-item>
                <el-form-item label="内容">
                    <div class="editor-wrap ann-editor-preview">
                        <Toolbar style="border-bottom:1px solid #e4e7ed" :editor="editorRef" :defaultConfig="toolbarConfig" mode="default" />
                        <Editor
                            style="height:320px; overflow-y:auto"
                            v-model="form.content"
                            :defaultConfig="editorConfig"
                            mode="default"
                            @onCreated="handleCreated"
                        />
                    </div>
                </el-form-item>
                <el-form-item label="发布设置">
                    <el-checkbox v-model="form.is_top" :true-value="1" :false-value="0">置顶</el-checkbox>
                </el-form-item>
                <el-form-item label="保存为">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="0">草稿</el-radio>
                        <el-radio :value="1">直接发布</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button type="primary" :loading="formLoading" @click="submitForm">确定</el-button>
            </template>
        </el-dialog>

        <!-- 类别管理 -->
        <el-dialog v-model="categoryVisible" title="公告类别管理" width="520px">
            <div class="category-form">
                <el-input v-model="newCategoryName" placeholder="新类别名称" style="width:200px" />
                <el-select v-model="newCategoryParent" style="width:150px">
                    <el-option label="作为顶级类别" :value="0" />
                    <el-option v-for="c in categoryList" :key="c.id" :label="`作为「${c.category_name}」的子类别`" :value="c.id" />
                </el-select>
                <el-button type="primary" @click="createCategory">新增</el-button>
            </div>
            <el-divider />
            <div v-loading="categoryLoading">
                <div v-for="cat in categoryList" :key="cat.id" class="category-item">
                    <el-tag size="small" effect="plain">{{ cat.category_name }}</el-tag>
                    <span class="category-ops">
                        <el-button link type="primary" @click="renameCategory(cat)">重命名</el-button>
                        <el-button link type="warning" @click="toggleCategory(cat)">
                            {{ cat.status === 1 ? '停用' : '启用' }}
                        </el-button>
                        <el-button link type="danger" @click="removeCategory(cat)">删除</el-button>
                    </span>
                </div>
                <el-empty v-if="!categoryList.length" description="暂无类别，请先新增" :image-size="60" />
            </div>
        </el-dialog>
    </div>
</template>

<style>
/* 正文框固定高度，弹窗内容超出时自己滚动 */
.ann-form-dialog .el-dialog__body {
    max-height: 68vh;
    overflow-y: auto;
}
</style>

<style scoped>
.announcement-page {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.toolbar-card {
    border-radius: 8px;
}

.toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
}

.toolbar-left,
.toolbar-right {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

/* 表格 */
.title-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
}

.title-text {
    cursor: pointer;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.title-text:hover {
    color: #409eff;
}

.title-unread {
    font-weight: 600;
}

.unread-tag {
    flex-shrink: 0;
}

.read-count {
    color: #909399;
    font-size: 12px;
}

:deep(.unread-row) {
    background: #f7faff;
}

.pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
}

/* 详情 */
.detail-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.meta-text {
    color: #909399;
    font-size: 13px;
}

.editor-wrap {
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 10px rgba(31, 41, 55, 0.05);
}
.editor-wrap :deep(.w-e-toolbar) {
  background: #f6f9fc;
  padding: 4px 6px;
}
.editor-wrap :deep(.w-e-text-container) {
  line-height: 1.8;
  font-size: 14px;
}
.editor-wrap :deep(.w-e-text-container .w-e-text) {
  padding: 12px 16px;
}

/* 模板与主题选择 */
.tpl-row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
}

.tpl-tip {
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
}

.tpl-item {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 2px 0;
}

.tpl-name {
    font-size: 13px;
    color: #303133;
}

.tpl-desc {
    font-size: 12px;
    color: #909399;
}

/* 类别管理 */
.category-form {
    display: flex;
    gap: 8px;
    align-items: center;
}

.category-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 4px;
    border-bottom: 1px solid #f0f0f0;
}

.category-ops {
    margin-left: auto;
}
</style>
