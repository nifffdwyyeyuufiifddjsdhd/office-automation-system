<template>
  <div class="flow-def-list">
    <div class="page-toolbar">
      <el-button v-permission="'flow:def:manage'" type="primary" @click="goEditor(null)">新建流程</el-button>
      <div class="toolbar-right">
        <el-switch v-model="onlyCurrent" size="small" />
        <span class="toolbar-tip">只看当前发布版本</span>
      </div>
    </div>
    <el-table :data="rows" v-loading="loading" size="small" border>
      <el-table-column prop="flow_name" label="流程名称" min-width="160" />
      <el-table-column prop="code" label="编码" width="120" />
      <el-table-column prop="stepCount" label="步骤数" width="80" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.audit_status === 1" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="row.audit_status === 2" type="danger" size="small">已拒绝</el-tag>
          <el-tag v-else type="info" size="small">待审核</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="creatorName" label="创建者" width="100" align="center" />
      <el-table-column label="版本" width="90" align="center">
        <template #default="{ row }">
          <span>v{{ row.version || 1 }}</span>
          <el-tooltip v-if="row.supersededBy" :content="`已被 #${row.supersededBy} 替代`" placement="top">
            <span class="muted">（旧）</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="适用范围" width="130" align="center">
        <template #default="{ row }">
          <el-tag v-if="!row.use_scope || row.use_scope === 0" type="success" size="small">全员</el-tag>
          <el-tooltip
            v-else
            :content="`已指定 ${scopeCount(row)} 个${row.use_scope === 2 ? '角色' : '部门'}`"
            placement="top"
          >
            <el-tag type="primary" size="small">{{ row.use_scope === 2 ? '指定角色' : '指定部门' }}</el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="240" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'flow:def:manage'" link type="primary" size="small" @click="goEditor(row.id)">编辑</el-button>
          <el-button
            v-if="row.status === 0"
            v-permission="'flow:def:manage'"
            link type="success"
            size="small"
            @click="handlePublish(row)"
          >发布</el-button>
          <el-button
            v-if="row.status === 1"
            v-permission="'flow:def:manage'"
            link type="warning"
            size="small"
            @click="handleDisable(row)"
          >停用</el-button>
          <el-button
            v-if="row.audit_status === 0"
            v-permission="'flow:def:manage'"
            link type="success"
            size="small"
            @click="handleAudit(row, 1)"
          >通过</el-button>
          <el-button
            v-if="row.audit_status === 0"
            v-permission="'flow:def:manage'"
            link type="danger"
            size="small"
            @click="handleAudit(row, 2)"
          >拒绝</el-button>
          <el-button v-permission="'flow:def:manage'" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { listDefinitions, publishDefinition, disableDefinition, deleteDefinition, auditDefinition, type FlowDefinitionItem } from "@/api/flow";
import { orgTree, type OrgNode } from "@/api/user";

const router = useRouter();
const loading = ref(false);
const list = ref<FlowDefinitionItem[]>([]);
// 默认只显示当前生效的版本，历史版本另外查
const onlyCurrent = ref(true);
const rows = computed(() => (onlyCurrent.value ? list.value.filter((r) => r.status !== 3) : list.value));
const orgMap = ref<Record<number, string>>({});

function scopeCount(row: any) {
  try {
    const ids = row.use_scope_ids ? JSON.parse(row.use_scope_ids) : [];
    return Array.isArray(ids) ? ids.length : 0;
  } catch {
    return 0;
  }
}

function orgName(id: number) {
  return orgMap.value[id] || `部门#${id}`;
}

function flattenOrgTree(nodes: OrgNode[], map: Record<number, string>) {
  for (const n of nodes) {
    map[n.id] = n.label;
    if (n.children) flattenOrgTree(n.children, map);
  }
}

function statusType(s: number) {
  if (s === 1) return "success";
  if (s === 2) return "info";
  // 3=已被新版本替代，按不生效处理
  if (s === 3) return "info";
  return "warning";
}

async function load() {
  loading.value = true;
  try {
    list.value = await listDefinitions();
  } finally {
    loading.value = false;
  }
}

function goEditor(id: number | null) {
  router.push({ path: "/home/flow-definition-editor", query: id ? { id } : {} });
}

async function handlePublish(row: FlowDefinitionItem) {
  try {
    await publishDefinition(row.id!);
    ElMessage.success("已发布");
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "发布失败");
  }
}

async function handleDisable(row: FlowDefinitionItem) {
  try {
    await disableDefinition(row.id!);
    ElMessage.success("已停用");
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}

async function handleAudit(row: FlowDefinitionItem, pass: number) {
  try {
    if (pass === 2) {
      await ElMessageBox.confirm(`确定拒绝流程「${row.flow_name}」吗？拒绝后模板会被停用。`, "审核拒绝", { type: "warning" });
    }
    await auditDefinition(row.id!, pass);
    ElMessage.success(pass === 1 ? "已通过" : "已拒绝");
    load();
  } catch (e: any) {
    if (e === "cancel") return;
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}

async function handleDelete(row: FlowDefinitionItem) {
  try {
    await ElMessageBox.confirm(`确定删除流程「${row.flow_name}」吗？已发起的实例不受影响。`, "删除流程", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await deleteDefinition(row.id!);
    ElMessage.success("已删除");
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "删除失败");
  }
}

onMounted(async () => {
  try {
    const tree = await orgTree();
    flattenOrgTree(tree || [], orgMap.value);
  } catch {}
  load();
});
</script>

<style scoped>
.flow-def-list {
  padding: 4px;
}
.page-toolbar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.toolbar-tip {
  font-size: 12px;
  color: #909399;
}
.muted {
  color: #909399;
  font-size: 12px;
}
</style>
