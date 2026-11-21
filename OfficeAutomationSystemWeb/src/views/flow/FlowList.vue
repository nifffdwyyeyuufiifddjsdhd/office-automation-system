<template>
  <div class="flow-list">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-if="props.mode === 'todo'" v-model="filterDef" placeholder="流程类型" clearable size="small" style="width: 160px" @change="load">
        <el-option v-for="d in defs" :key="d.id" :label="d.flow_name" :value="d.id" />
      </el-select>
      <el-select v-if="props.mode === 'todo'" v-model="filterUrgency" placeholder="紧急程度" clearable size="small" style="width: 120px" @change="load">
        <el-option label="普通" :value="0" />
        <el-option label="紧急" :value="1" />
      </el-select>
      <el-input v-model="filterKeyword" placeholder="搜索标题" clearable size="small" style="width: 200px" @keyup.enter="load" @clear="load" @change="load" />
      <el-button v-if="props.mode === 'todo'" type="success" size="small" :disabled="selected.length === 0" @click="handleBatchApprove">
        批量通过（已选 {{ selected.length }} 条）
      </el-button>
    </div>

    <el-table
      :data="filteredList"
      v-loading="loading"
      size="small"
      border
      :row-key="(row: FlowInstanceRow) => row.id"
      :row-class-name="rowClassName"
      @row-click="goDetail"
      @selection-change="onSelectionChange"
      ref="tableRef"
    >
      <el-table-column v-if="props.mode === 'todo'" type="selection" width="40" />
      <el-table-column prop="instanceNo" label="编号" width="180" />
            <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          <span v-if="row.unread" class="unread-dot" title="有未查看的待办" />
          <el-popover
            :visible="popMap[row.id] === true"
            placement="top-start"
            :width="320"
            trigger="manual"
          >
            <template #reference>
              <span class="long-text" :class="{ fade: overflowMap[row.id] }" @mouseenter="showPop($event, row.id)">{{ row.title }}</span>
            </template>
            <div style="white-space:normal;word-break:break-all;line-height:1.6;">{{ row.title }}</div>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column prop="flowName" label="流程" width="140" />
      <el-table-column prop="creatorName" label="发起人" width="100" />
      <el-table-column prop="currentStepName" label="当前节点" width="120" />
      <el-table-column prop="auditorName" label="当前审批人" width="110" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" width="160">
        <template #default="{ row }">{{ row.submitTime?.replace('T', ' ')?.substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column v-if="props.mode === 'todo'" prop="arrivedTime" label="到达时间" width="160">
        <template #default="{ row }">{{ (row.arrivedTime || row.submitTime)?.replace('T', ' ')?.substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column v-if="showMine" label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click.stop="goDetail(row)">查看</el-button>
          <el-button v-if="row.status === 1 && hasPerm('flow:urge')" link type="warning" size="small" @click.stop="handleUrge(row)">催办</el-button>
          <el-button v-if="row.resubmitAvailable" link type="primary" size="small" @click.stop="handleResubmit(row)">重新提交</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { hasPerm } from "@/utils/permission";
import {
  todoInstancesV2,
  doneInstances,
  mineInstancesV2,
  urgeInstance,
  batchApproveInstances,
  publishedDefinitions,
  type FlowInstanceRow,
  type FlowDefinitionItem,
} from "@/api/flow";

const props = defineProps<{ mode: "todo" | "done" | "mine" }>();
const router = useRouter();
const route = useRoute();
const loading = ref(false);
const list = ref<FlowInstanceRow[]>([]);
const dayFilter = ref('all');

function flowDayKey(offset: number) {
  const d = new Date();
  d.setDate(d.getDate() - offset);
  return d.toISOString().slice(0, 10);
}
const filteredList = computed(() => {
  if (dayFilter.value === 'all') return list.value;
  const offset = dayFilter.value === 'today' ? 0 : dayFilter.value === 'yesterday' ? 1 : 2;
  const key = flowDayKey(offset);
  return list.value.filter(r => r.submitTime && r.submitTime.slice(0, 10) === key);
});
const selected = ref<FlowInstanceRow[]>([]);
const tableRef = ref<any>(null);
const defs = ref<FlowDefinitionItem[]>([]);
const highlightId = ref<number | null>(null);

// 长文本气泡，溢出才显示，点外部关闭
const popMap = ref<Record<number, boolean>>({});
const overflowMap = ref<Record<number, boolean>>({});
function showPop(e: MouseEvent, id: number) {
  const el = e.currentTarget as HTMLElement;
  const overflow = el.scrollWidth > el.clientWidth + 1;
  overflowMap.value = { ...overflowMap.value, [id]: overflow };
  popMap.value = overflow ? { [id]: true } : {};
}
function closeAllPop() {
  popMap.value = {};
}
if (typeof document !== "undefined") {
  document.addEventListener("click", closeAllPop);
}

const filterDef = ref<number | undefined>(undefined);
const filterUrgency = ref<number | undefined>(undefined);
const filterKeyword = ref("");

function statusType(s: number) {
  if (s === 1) return "warning";
  if (s === 2) return "success";
  if (s === 3) return "danger";
  return "info";
}

async function load() {
  loading.value = true;
  try {
    if (props.mode === "todo") {
      list.value = await todoInstancesV2({
        definitionId: filterDef.value,
        urgency: filterUrgency.value,
        keyword: filterKeyword.value || undefined,
      });
    } else if (props.mode === "done") {
      list.value = await doneInstances();
    } else {
      list.value = await mineInstancesV2({ keyword: filterKeyword.value || undefined });
    }
  } finally {
    loading.value = false;
  }
}

function goDetail(row: FlowInstanceRow) {
  router.push({ path: "/home/flow-detail", query: { id: row.id, mode: props.mode } });
}

function onSelectionChange(rows: FlowInstanceRow[]) {
  selected.value = rows;
}

async function handleBatchApprove() {
  if (selected.value.length === 0) return;
  try {
    await ElMessageBox.confirm(
      `将批量通过 ${selected.value.length} 条待办，审批意见默认为"同意"。确认？`,
      "批量通过",
      { type: "warning" }
    );
  } catch {
    return;
  }
  try {
    const ids = selected.value.map((r) => r.id);
    const res = await batchApproveInstances(ids);
    let msg = `成功通过 ${res.success} 条`;
    if (res.skipped && res.skipped.length > 0) {
      msg += `；跳过 ${res.skipped.length} 条（${res.skipped[0]}${res.skipped.length > 1 ? " 等" : ""}）`;
    }
    ElMessage.success(msg);
    if (tableRef.value) tableRef.value.clearSelection();
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "批量通过失败");
  }
}

function handleResubmit(row: FlowInstanceRow) {
  router.push({ path: "/home/flow-start", query: { resubmitId: row.id } });
}

async function handleUrge(row: FlowInstanceRow) {
  try {
    await ElMessageBox.confirm(`催办「${row.title}」？`, "催办", {
      type: "info",
      confirmButtonText: "发送催办",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await urgeInstance(row.id);
    ElMessage.success("已催办");
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "催办失败");
  }
}

const showMine = props.mode === "mine";

function rowClassName({ row }: { row: FlowInstanceRow }) {
  if (highlightId.value && row.id === highlightId.value) return 'row-highlight';
  return '';
}

async function doHighlight() {
  if (!highlightId.value) return;
  await nextTick();
  await nextTick();
  // 重新渲染让 rowClassName 生效
  const hId = highlightId.value;
  highlightId.value = null;
  await nextTick();
  highlightId.value = hId;
  await nextTick();
  // 找到对应行并滚动
  const rowEl = document.querySelector('.el-table__row.row-highlight') as HTMLElement | null;
  if (rowEl) {
    rowEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
    setTimeout(() => rowEl.classList.remove('row-highlight'), 1500);
  }
}

onMounted(async () => {
  const hq = route.query.highlightId;
  if (hq) highlightId.value = Number(hq);
  if (props.mode === "todo" || props.mode === "mine") {
    defs.value = await publishedDefinitions();
  }
  await load();
  if (highlightId.value) doHighlight();
});
</script>

<style scoped>
.flow-list {
  padding: 4px;
}
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.flow-list :deep(.el-table__row) {
  cursor: pointer;
}
.long-text {
  display: inline-block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  vertical-align: bottom;
  cursor: default;
}
.unread-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 6px;
  border-radius: 50%;
  background: #f56c6c;
  vertical-align: middle;
}
.long-text.fade {
  -webkit-mask-image: linear-gradient(to right, #000 calc(100% - 20px), transparent 100%);
  mask-image: linear-gradient(to right, #000 calc(100% - 20px), transparent 100%);
}
:deep(.el-table__row.row-highlight) {
  animation: rowPulse 1.5s ease-out;
  background-color: #fff7e6 !important;
}
@keyframes rowPulse {
  0% { transform: scale(1); background-color: #fff7e6; }
  30% { transform: scale(1.01); background-color: #ffe58f; }
  100% { transform: scale(1); background-color: #fff7e6; }
}
</style>
