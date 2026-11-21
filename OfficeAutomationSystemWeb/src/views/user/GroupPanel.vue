<template>
  <div class="group-panel">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="group-usage-tip"
      title="群组用来跨部门把人圈在一起，目前用于工作日志的查看授权（日志 - 查看权限 - 按群组，授权后组内成员能看到对方的日志）。钉钉/企业微信这类第三方账号只在这里存一个对应关系，不存密码。"
    />
    <div class="group-body">
    <!-- 左：群组列表 -->
    <div class="group-left">
      <div class="group-left-header">
        <span>群组列表</span>
        <el-button v-permission="'group:manage'" link type="primary" size="small" @click="openGroupDialog(null)">新增</el-button>
      </div>
      <div
        v-for="g in groups"
        :key="g.id"
        class="group-item"
        :class="{ active: currentGroupId === g.id }"
        @click="selectGroup(g)"
      >
        <div class="group-item-name">{{ g.group_name }}</div>
        <div class="group-item-meta">
          成员 {{ g.memberCount || 0 }} 人
          <span v-if="g.group_code"> · {{ g.group_code }}</span>
        </div>
        <span class="group-item-ops">
          <el-button v-permission="'group:manage'" link type="primary" size="small" @click.stop="openGroupDialog(g)">编辑</el-button>
          <el-button v-permission="'group:manage'" link type="danger" size="small" @click.stop="handleDeleteGroup(g)">删除</el-button>
        </span>
      </div>
      <el-empty v-if="!groups.length" description="暂无群组" :image-size="60" />
    </div>

    <!-- 右：成员管理 -->
    <div class="group-right">
      <div class="group-right-header">
        <span>{{ currentGroup ? `${currentGroup.group_name} · 成员` : "请选择左侧群组" }}</span>
        <el-button
          v-if="currentGroup"
          v-permission="'group:manage'"
          type="primary"
          size="small"
          @click="memberDialogVisible = true"
        >添加成员</el-button>
      </div>
      <el-table :data="members" size="small" border v-loading="membersLoading">
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="orgName" label="部门" min-width="120" />
        <el-table-column prop="joinTime" label="加入时间" min-width="160">
          <template #default="{ row }">{{ row.joinTime?.replace('T', ' ')?.substring(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'group:manage'" link type="danger" size="small" @click="handleRemoveMember(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    </div>

    <!-- 群组新增/编辑弹窗 -->
    <el-dialog v-model="groupDialogVisible" :title="groupIsEdit ? '编辑群组' : '新增群组'" width="480px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="群组名称" required>
          <el-input v-model="groupForm.group_name" placeholder="如：前端组、项目A组" />
        </el-form-item>
        <el-form-item label="群组编码">
          <el-input v-model="groupForm.group_code" placeholder="如：fe_team" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="groupForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="groupSaving" @click="handleSaveGroup">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加成员弹窗 -->
    <el-dialog v-model="memberDialogVisible" title="添加成员" width="560px" destroy-on-close>
      <el-input v-model="memberKeyword" placeholder="搜索姓名 / 账号" clearable style="margin-bottom: 10px" />
      <el-table
        :data="filteredUsers"
        size="small"
        border
        @selection-change="onSelectionChange"
        max-height="300"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="real_name" label="姓名" min-width="100" />
        <el-table-column prop="username" label="账号" min-width="120" />
      </el-table>
      <template #footer>
        <el-button @click="memberDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addingMember" @click="handleAddMembers">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  listGroups,
  createGroup,
  updateGroup,
  deleteGroup,
  listGroupMembers,
  addGroupMembers,
  removeGroupMember,
  pageUsers,
  type GroupItem,
  type GroupMemberItem,
  type UserItem,
} from "@/api/user";

const groups = ref<GroupItem[]>([]);
const currentGroupId = ref<number | null>(null);
const currentGroup = ref<GroupItem | null>(null);
const members = ref<GroupMemberItem[]>([]);
const membersLoading = ref(false);

const groupDialogVisible = ref(false);
const groupIsEdit = ref(false);
const groupSaving = ref(false);
const groupForm = reactive<GroupItem>({ group_name: "", group_code: "", description: "" });

const memberDialogVisible = ref(false);
const addingMember = ref(false);
const memberKeyword = ref("");
const allUsers = ref<UserItem[]>([]);
const selectedUserIds = ref<number[]>([]);

const filteredUsers = computed(() => {
  const kw = memberKeyword.value.trim().toLowerCase();
  if (!kw) return allUsers.value;
  return allUsers.value.filter(
    (u) => u.real_name?.toLowerCase().includes(kw) || u.username?.toLowerCase().includes(kw)
  );
});

async function loadGroups() {
  groups.value = await listGroups();
}

async function selectGroup(g: GroupItem) {
  currentGroupId.value = g.id!;
  currentGroup.value = g;
  await loadMembers();
}

async function loadMembers() {
  if (!currentGroupId.value) return;
  membersLoading.value = true;
  try {
    members.value = await listGroupMembers(currentGroupId.value);
  } finally {
    membersLoading.value = false;
  }
}

function openGroupDialog(g: GroupItem | null) {
  groupIsEdit.value = !!g;
  if (g) {
    Object.assign(groupForm, g);
  } else {
    groupForm.id = undefined;
    groupForm.group_name = "";
    groupForm.group_code = "";
    groupForm.description = "";
  }
  groupDialogVisible.value = true;
}

async function handleSaveGroup() {
  if (!groupForm.group_name.trim()) {
    ElMessage.warning("请输入群组名称");
    return;
  }
  groupSaving.value = true;
  try {
    if (groupIsEdit.value) {
      await updateGroup(groupForm);
      ElMessage.success("已更新");
    } else {
      await createGroup(groupForm);
      ElMessage.success("已创建");
    }
    groupDialogVisible.value = false;
    loadGroups();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "保存失败");
  } finally {
    groupSaving.value = false;
  }
}

async function handleDeleteGroup(g: GroupItem) {
  try {
    await ElMessageBox.confirm(
      `删除群组「${g.group_name}」将同时清除其成员关系，确定删除吗？`,
      "删除群组",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" }
    );
  } catch {
    return;
  }
  try {
    await deleteGroup(g.id!);
    ElMessage.success("已删除");
    if (currentGroupId.value === g.id) {
      currentGroupId.value = null;
      currentGroup.value = null;
      members.value = [];
    }
    loadGroups();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "删除失败");
  }
}

async function openMemberDialog() {
  memberKeyword.value = "";
  selectedUserIds.value = [];
  memberDialogVisible.value = true;
  // 拉全量用户供选择（单页最多200条）
  const res = await pageUsers({ pageNum: 1, pageSize: 200 });
  allUsers.value = res.records || [];
}

function onSelectionChange(rows: UserItem[]) {
  selectedUserIds.value = rows.map((r) => r.id!);
}

async function handleAddMembers() {
  if (!selectedUserIds.value.length) {
    ElMessage.warning("请至少选择一名用户");
    return;
  }
  addingMember.value = true;
  try {
    await addGroupMembers(currentGroupId.value!, selectedUserIds.value);
    ElMessage.success("已添加");
    memberDialogVisible.value = false;
    loadMembers();
    loadGroups();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "添加失败");
  } finally {
    addingMember.value = false;
  }
}

async function handleRemoveMember(row: GroupMemberItem) {
  try {
    await ElMessageBox.confirm(`将「${row.realName}」移出群组？`, "移除成员", {
      type: "warning",
      confirmButtonText: "移除",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await removeGroupMember(currentGroupId.value!, row.userId);
    ElMessage.success("已移除");
    loadMembers();
    loadGroups();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "移除失败");
  }
}

onMounted(loadGroups);
</script>

<style scoped>
.group-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
}
/* 上面一行是用途说明，下面这行才是左右两栏 */
.group-body {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}
.group-usage-tip {
  flex-shrink: 0;
}
.group-left {
  width: 260px;
  flex-shrink: 0;
  border-right: 1px solid #ebeef5;
  padding-right: 12px;
  overflow: auto;
}
.group-left-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 10px;
}
.group-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  border: 1px solid transparent;
}
.group-item:hover {
  background: #f5f7fa;
}
.group-item.active {
  background: #ecf5ff;
  border-color: #d9ecff;
}
.group-item-name {
  font-size: 14px;
  color: #303133;
}
.group-item-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.group-item-ops {
  float: right;
  margin-top: -20px;
}
.group-right {
  flex: 1;
  min-width: 0;
}
.group-right-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 10px;
}
</style>
