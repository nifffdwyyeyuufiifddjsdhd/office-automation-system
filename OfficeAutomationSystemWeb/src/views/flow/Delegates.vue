<template>
  <div class="delegates">
    <div class="page-head">
      <div>
        <h3>审批委托</h3>
        <p class="muted">
          出差、休假期间可以把审批待办委托给同事，委托人自己的待办在委托时间段内会同步出现在代理人那里，代理人批完会记录实际操作人。
        </p>
      </div>
      <el-button type="primary" @click="openCreate">新建委托</el-button>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane :label="`我委托的（${list.mine.length}）`" name="mine">
        <el-table :data="list.mine" v-loading="loading" border size="small">
          <el-table-column label="代理人" width="200">
            <template #default="{ row }">
              {{ row.agentName }}
              <span class="muted">（{{ row.agentOrg || '—' }}）</span>
            </template>
          </el-table-column>
          <el-table-column label="委托时间段" min-width="240">
            <template #default="{ row }">{{ rangeText(row) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="委托原因" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row)" size="small">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button v-if="row.active" link type="danger" size="small" @click="cancel(row)">取消委托</el-button>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="`委托给我的（${list.toMe.length}）`" name="toMe">
        <el-table :data="list.toMe" v-loading="loading" border size="small">
          <el-table-column label="委托人" width="200">
            <template #default="{ row }">
              {{ row.delegatorName }}
              <span class="muted">（{{ row.delegatorOrg || '—' }}）</span>
            </template>
          </el-table-column>
          <el-table-column label="委托时间段" min-width="240">
            <template #default="{ row }">{{ rangeText(row) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="委托原因" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row)" size="small">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default>
              <el-button link type="primary" size="small" @click="goTodo">去待办处理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="createVisible" title="新建审批委托" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="代理人" required>
          <el-select v-model="form.agentId" filterable placeholder="选择接手的同事" style="width:100%">
            <el-option
              v-for="u in agents"
              :key="u.id"
              :label="`${u.realName}（${u.orgName || '未分配部门'}）`"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="委托时间段" required>
          <el-date-picker
            v-model="form.range"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="委托原因">
          <el-input v-model="form.reason" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="如：外出培训、年假" />
        </el-form-item>
      </el-form>
      <div class="muted" style="line-height:1.7">
        单次委托最长 90 天；委托期间你本人仍然能处理自己的待办。代理人审批后，流程轨迹里记录的是他本人。
      </div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确认委托</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  listFlowDelegates,
  flowDelegateAgentOptions,
  createFlowDelegate,
  cancelFlowDelegate,
  type FlowDelegateItem,
  type FlowDelegateList,
} from "@/api/flow";

const router = useRouter();
const tab = ref<"mine" | "toMe">("mine");
const loading = ref(false);
const list = ref<FlowDelegateList>({ mine: [], toMe: [] });

const createVisible = ref(false);
const submitting = ref(false);
const agents = ref<{ id: number; username: string; realName: string; orgName?: string }[]>([]);
const form = reactive<{ agentId: number | null; range: [string, string] | null; reason: string }>({
  agentId: null,
  range: null,
  reason: "",
});

function rangeText(row: FlowDelegateItem) {
  const fmt = (v: string) => (v || "").replace("T", " ").substring(0, 16);
  return `${fmt(row.startTime)} ~ ${fmt(row.endTime)}`;
}

function statusType(row: FlowDelegateItem) {
  if (row.active) return "success";
  if (row.status === 0) return "info";
  if (new Date(row.endTime) < new Date()) return "info";
  return "warning";
}

async function load() {
  loading.value = true;
  try {
    list.value = await listFlowDelegates();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "读取委托列表失败");
  } finally {
    loading.value = false;
  }
}

async function openCreate() {
  form.agentId = null;
  form.range = null;
  form.reason = "";
  createVisible.value = true;
  try {
    agents.value = await flowDelegateAgentOptions();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "读取同事列表失败");
  }
}

async function submitCreate() {
  if (!form.agentId) {
    ElMessage.warning("请选择代理人");
    return;
  }
  if (!form.range || form.range.length !== 2) {
    ElMessage.warning("请选择委托时间段");
    return;
  }
  submitting.value = true;
  try {
    const res = await createFlowDelegate({
      agentId: form.agentId,
      startTime: form.range[0],
      endTime: form.range[1],
      reason: form.reason || undefined,
    });
    createVisible.value = false;
    ElMessage.success(`委托已生效，编号 ${res.id}`);
    tab.value = "mine";
    await load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "新建委托失败");
  } finally {
    submitting.value = false;
  }
}

async function cancel(row: FlowDelegateItem) {
  try {
    await ElMessageBox.confirm(`确认取消对「${row.agentName}」的委托吗？取消后对方立刻看不到你的待办。`, "取消委托", {
      type: "warning",
      confirmButtonText: "取消委托",
      cancelButtonText: "再想想",
    });
  } catch {
    return;
  }
  try {
    await cancelFlowDelegate(row.id);
    ElMessage.success("已取消");
    await load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "取消失败");
  }
}

function goTodo() {
  router.push("/home/flow-todo");
}

onMounted(load);
</script>

<style scoped>
.delegates { padding: 4px; }
.page-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 8px; }
.page-head h3 { margin: 0 0 4px; font-size: 17px; }
.page-head .muted { margin: 0; }
.muted { color: #909399; font-size: 12px; }
</style>
