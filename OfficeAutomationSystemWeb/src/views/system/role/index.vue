<template>
    <div class="role-page">
        <div class="page-head">
            <div class="head-left">
                <span class="head-bar"></span>
                <div>
                    <h2>权限管理</h2>
                    <p>维护角色、勾选菜单与按钮权限、分配成员。角色权限改动后成员需重新登录生效</p>
                </div>
            </div>
            <el-button type="primary" @click="openCreate">新建角色</el-button>
        </div>

        <el-card shadow="never" class="filter-card">
            <div class="filters">
                <el-input v-model="query.keyword" placeholder="角色名称 / 角色编码" clearable style="width: 220px" @keyup.enter="loadRoles" />
                <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
                    <el-option label="启用" :value="1" />
                    <el-option label="停用" :value="0" />
                </el-select>
                <el-button type="primary" @click="loadRoles">查询</el-button>
                <el-button @click="resetQuery">重置</el-button>
            </div>
        </el-card>

        <el-card shadow="never">
            <el-table v-loading="loading" :data="roles" border stripe>
                <el-table-column prop="role_name" label="角色名称" min-width="140" />
                <el-table-column prop="role_code" label="角色编码" min-width="140">
                    <template #default="{ row }">
                        <span class="code-text">{{ row.role_code }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="角色类型" width="110">
                    <template #default="{ row }">
                        <el-tag :type="row.is_system === 1 ? 'danger' : 'info'" effect="plain" size="small">
                            {{ row.is_system === 1 ? "系统内置" : row.role_type === 3 ? "动态角色" : "业务角色" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="dataScopeText" label="数据范围" width="140" />
                <el-table-column label="成员" width="90" align="center">
                    <template #default="{ row }">{{ row.user_count ?? 0 }}</template>
                </el-table-column>
                <el-table-column label="权限项" width="90" align="center">
                    <template #default="{ row }">{{ row.perm_count ?? 0 }}</template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? "启用" : "停用" }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
                <el-table-column label="操作" width="250" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openPermission(row)">配置权限</el-button>
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="primary" @click="openMembers(row)">成员</el-button>
                        <el-button link type="danger" :disabled="row.is_system === 1" @click="removeRole(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="formVisible" :title="form.id ? '编辑角色' : '新建角色'" width="560px">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
                <el-form-item label="角色名称" prop="role_name">
                    <el-input v-model="form.role_name" placeholder="如：项目助理" maxlength="50" />
                </el-form-item>
                <el-form-item label="角色编码" prop="role_code">
                    <el-input v-model="form.role_code" :disabled="!!form.id" placeholder="小写英文与下划线，如 project_assistant" maxlength="50" />
                </el-form-item>
                <el-form-item label="数据范围" prop="data_scope">
                    <el-select v-model="form.data_scope" style="width: 100%">
                        <el-option v-for="item in dataScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                    <div class="form-tip">数据范围决定该角色在用户、日志、出勤等列表里能看到多少数据</div>
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="form.sort_order" :min="0" :max="999" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
                </el-form-item>
                <el-form-item label="说明">
                    <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" show-word-limit />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
            </template>
        </el-dialog>

        <el-drawer v-model="permVisible" :title="'配置权限 · ' + (currentRole?.role_name || '')" size="520px">
            <div class="perm-head">
                <span class="perm-tip">勾选后保存即可生效；勾选父级会连带勾选它下面的菜单与按钮</span>
                <div class="perm-actions">
                    <el-input
                        v-model="permKeyword"
                        placeholder="搜索权限名称 / 权限码"
                        size="small"
                        clearable
                        class="perm-search"
                    />
                    <el-button size="small" @click="toggleAll(true)">全选</el-button>
                    <el-button size="small" @click="toggleAll(false)">清空</el-button>
                </div>
            </div>
            <el-tree
                ref="treeRef"
                :data="menuTree"
                node-key="id"
                show-checkbox
                default-expand-all
                :filter-node-method="filterPermNode"
                :props="{ label: 'label', children: 'children' }"
                class="perm-tree"
            >
                <template #default="{ data }">
                    <span class="perm-node">
                        <span>{{ data.label }}</span>
                        <span v-if="data.menuCode" class="perm-code">{{ data.menuCode }}</span>
                        <el-tag v-if="data.menuType === 3" size="small" type="info" effect="plain">按钮</el-tag>
                    </span>
                </template>
            </el-tree>
            <template #footer>
                <el-button @click="permVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitPermissions">保存权限</el-button>
            </template>
        </el-drawer>

        <el-dialog v-model="memberVisible" :title="'角色成员 · ' + (currentRole?.role_name || '')" width="760px">
            <div class="member-add">
                <el-select v-model="addUserId" placeholder="选择要加入该角色的用户（可搜索）" filterable clearable style="width: 320px">
                    <el-option v-for="u in candidateUsers" :key="u.id" :label="u.real_name + '（' + u.username + '）'" :value="u.id" />
                </el-select>
                <el-button type="primary" :disabled="!addUserId" @click="addMember">加入角色</el-button>
                <span class="member-tip">一个用户可以同时拥有多个角色，权限取并集</span>
            </div>
            <el-table v-loading="memberLoading" :data="members" border max-height="420">
                <el-table-column prop="real_name" label="姓名" width="120" />
                <el-table-column prop="username" label="账号" width="140" />
                <el-table-column prop="org_name" label="所属部门" min-width="130" />
                <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? "启用" : "停用" }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="100">
                    <template #default="{ row }">
                        <el-button link type="danger" @click="removeMember(row)">移出</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
    createRole,
    deleteRole,
    getRoleDetail,
    getUserRoles,
    listRoleUsers,
    listRoles,
    roleMenuTree,
    saveRolePermissions,
    saveUserRoles,
    updateRole,
    type RoleItem,
    type RoleMenuNode,
} from "@/api/role";
import { pageUsers, type UserItem } from "@/api/user";

// 数据范围口径与后端 DataScopeService 保持一致
const dataScopeOptions = [
    { value: 1, label: "全公司" },
    { value: 2, label: "本部门" },
    { value: 3, label: "本部门及下级" },
    { value: 4, label: "仅本人" },
    { value: 5, label: "本人负责的项目" },
];

const loading = ref(false);
const saving = ref(false);
const roles = ref<RoleItem[]>([]);
const query = reactive<{ keyword: string; status: number | null }>({ keyword: "", status: null });

const formVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<{ id?: number; role_name: string; role_code: string; description: string; data_scope: number; sort_order: number; status: number }>({
    role_name: "",
    role_code: "",
    description: "",
    data_scope: 2,
    sort_order: 10,
    status: 1,
});

const rules: FormRules = {
    role_name: [{ required: true, message: "请填写角色名称", trigger: "blur" }],
    role_code: [
        { required: true, message: "请填写角色编码", trigger: "blur" },
        { pattern: /^[a-z][a-z0-9_]{1,49}$/, message: "只能用小写字母、数字、下划线，且以字母开头", trigger: "blur" },
    ],
    data_scope: [{ required: true, message: "请选择数据范围", trigger: "change" }],
};

const permVisible = ref(false);
const currentRole = ref<RoleItem | null>(null);
const menuTree = ref<RoleMenuNode[]>([]);
const treeRef = ref<any>();
const checkedIds = ref<number[]>([]);
const permKeyword = ref("");

// 权限树按名称或权限码过滤，父节点要留着才能看到命中项
function filterPermNode(value: string, data: any) {
    if (!value) return true;
    const kw = value.trim().toLowerCase();
    const label = String(data?.label || "").toLowerCase();
    const code = String(data?.menuCode || "").toLowerCase();
    return label.includes(kw) || code.includes(kw);
}

watch(permKeyword, (v) => {
    treeRef.value?.filter(v);
});

const memberVisible = ref(false);
const memberLoading = ref(false);
const members = ref<{ id: number; username: string; real_name: string; org_name?: string; status: number }[]>([]);
const addUserId = ref<number | null>(null);
const allUsers = ref<UserItem[]>([]);

const candidateUsers = computed(() => allUsers.value.filter((u) => !members.value.some((m) => m.id === u.id)));

async function loadRoles() {
    loading.value = true;
    try {
        roles.value = await listRoles({ keyword: query.keyword || undefined, status: query.status });
    } finally {
        loading.value = false;
    }
}

function resetQuery() {
    query.keyword = "";
    query.status = null;
    loadRoles();
}

function openCreate() {
    Object.assign(form, { id: undefined, role_name: "", role_code: "", description: "", data_scope: 2, sort_order: 10, status: 1 });
    formVisible.value = true;
}

async function openEdit(row: RoleItem) {
    const detail = await getRoleDetail(row.id);
    Object.assign(form, {
        id: detail.id,
        role_name: detail.role_name,
        role_code: detail.role_code,
        description: detail.description || "",
        data_scope: detail.data_scope,
        sort_order: detail.sort_order ?? 10,
        status: detail.status,
    });
    formVisible.value = true;
}

async function submitForm() {
    if (!formRef.value) return;
    await formRef.value.validate(async (ok) => {
        if (!ok) return;
        saving.value = true;
        try {
            if (form.id) {
                await updateRole(form.id, { ...form });
                ElMessage.success("角色已更新");
            } else {
                await createRole({ ...form });
                ElMessage.success("角色已创建，接着给它配置权限吧");
            }
            formVisible.value = false;
            loadRoles();
        } finally {
            saving.value = false;
        }
    });
}

async function removeRole(row: RoleItem) {
    await ElMessageBox.confirm("确定删除角色「" + row.role_name + "」吗？该角色下不能还有成员。", "删除角色", { type: "warning" });
    await deleteRole(row.id);
    ElMessage.success("已删除");
    loadRoles();
}

async function openPermission(row: RoleItem) {
    currentRole.value = row;
    saving.value = true;
    try {
        if (menuTree.value.length === 0) menuTree.value = await roleMenuTree();
        const detail = await getRoleDetail(row.id);
        checkedIds.value = detail.menuIds || [];
        permVisible.value = true;
        // 树渲染完再回填勾选，免得父级半选被全选覆盖
        setTimeout(() => {
            treeRef.value?.setCheckedKeys(checkedIds.value, false);
        }, 60);
    } finally {
        saving.value = false;
    }
}

function toggleAll(checked: boolean) {
    if (checked) {
        const all: number[] = [];
        const walk = (nodes: RoleMenuNode[]) => nodes.forEach((n) => { all.push(n.id); if (n.children?.length) walk(n.children); });
        walk(menuTree.value);
        treeRef.value?.setCheckedKeys(all, false);
    } else {
        treeRef.value?.setCheckedKeys([], false);
    }
}

async function submitPermissions() {
    if (!currentRole.value) return;
    const checked = treeRef.value?.getCheckedKeys() || [];
    const half = treeRef.value?.getHalfCheckedKeys() || [];
    const menuIds = Array.from(new Set([...checked, ...half])) as number[];
    saving.value = true;
    try {
        const res = await saveRolePermissions(currentRole.value.id, menuIds);
        ElMessage.success("已保存 " + (res?.count ?? menuIds.length) + " 项权限，成员重新登录后生效");
        permVisible.value = false;
        loadRoles();
    } finally {
        saving.value = false;
    }
}

async function openMembers(row: RoleItem) {
    currentRole.value = row;
    memberVisible.value = true;
    addUserId.value = null;
    await Promise.all([loadMembers(row.id), loadAllUsers()]);
}

async function loadMembers(roleId: number) {
    memberLoading.value = true;
    try {
        members.value = await listRoleUsers(roleId);
    } finally {
        memberLoading.value = false;
    }
}

async function loadAllUsers() {
    if (allUsers.value.length > 0) return;
    const res = await pageUsers({ pageNum: 1, pageSize: 200 });
    allUsers.value = res.records || [];
}

async function addMember() {
    if (!currentRole.value || !addUserId.value) return;
    const exist = await getUserRoles(addUserId.value);
    const ids = Array.from(new Set([...(exist.roleIds || []), currentRole.value.id]));
    await saveUserRoles(addUserId.value, ids);
    ElMessage.success("已加入该角色");
    addUserId.value = null;
    await loadMembers(currentRole.value.id);
    loadRoles();
}

async function removeMember(row: { id: number }) {
    if (!currentRole.value) return;
    const exist = await getUserRoles(row.id);
    const ids = (exist.roleIds || []).filter((id) => id !== currentRole.value!.id);
    if (ids.length === 0) {
        ElMessage.warning("该用户只有这一个角色，不能直接移出；请先在用户管理里给他分配其他角色");
        return;
    }
    await saveUserRoles(row.id, ids);
    ElMessage.success("已移出该角色");
    await loadMembers(currentRole.value.id);
    loadRoles();
}

onMounted(loadRoles);
</script>

<style scoped>
.role-page { padding: 4px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.head-left { display: flex; align-items: center; gap: 10px; }
.head-bar { width: 4px; height: 34px; border-radius: 2px; background: linear-gradient(180deg, #3b82f6, #6366f1); }
.page-head h2 { margin: 0; font-size: 20px; color: #1f2d3d; }
.page-head p { margin: 4px 0 0; font-size: 13px; color: #8a94a6; }
.filter-card { margin-bottom: 14px; }
.filters { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.code-text { font-family: Consolas, Monaco, monospace; color: #475569; }
.form-tip { font-size: 12px; color: #909399; line-height: 1.5; margin-top: 4px; }
.perm-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 10px; }
.perm-tip { font-size: 12px; color: #909399; flex: 1 1 260px; line-height: 1.5; }
.perm-actions { display: flex; gap: 8px; align-items: center; flex-shrink: 0; }
.perm-search { width: 190px; }
.perm-tree { border: 1px solid #ebeef5; border-radius: 6px; padding: 8px; }
.perm-node { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; }
.perm-code { font-size: 11px; color: #a0a7b4; font-family: Consolas, Monaco, monospace; }
.member-add { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.member-tip { font-size: 12px; color: #909399; }
</style>
