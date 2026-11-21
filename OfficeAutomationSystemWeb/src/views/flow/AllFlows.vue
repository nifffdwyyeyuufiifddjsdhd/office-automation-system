<template>
  <div class="all-flows">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="filterDef" placeholder="流程类型" clearable size="small" style="width: 160px" @change="load">
        <el-option v-for="d in defs" :key="d.id" :label="d.flow_name" :value="d.id" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态" clearable size="small" style="width: 120px" @change="load">
        <el-option label="审批中" :value="1" />
        <el-option label="已通过" :value="2" />
        <el-option label="已驳回" :value="3" />
        <el-option label="已撤回" :value="4" />
      </el-select>
      <el-input v-model="filterKeyword" placeholder="搜索标题" clearable size="small" style="width: 200px" @keyup.enter="load" @clear="load" />
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        size="small"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="load"
      />
      <el-button type="primary" size="small" @click="load">查询</el-button>
      <el-button type="success" size="small" :loading="exporting" @click="handleExport">导出Excel</el-button>
    </div>

    <!-- 主属性统计卡片（选了流程类型才显示） -->
    <el-card v-if="stats && Object.keys(stats.fields || {}).length" shadow="never" class="stat-card">
      <template #header>
        「{{ currentDefName }}」已完成流程主属性统计（共 {{ stats.total }} 条）
      </template>
      <div class="stat-grid">
        <div v-for="(f, k) in stats.fields" :key="k" class="stat-item">
          <div class="stat-label">{{ f.label }}</div>
          <div class="stat-value">{{ f.sum }}</div>
        </div>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-table :data="list" v-loading="loading" size="small" border @row-click="goDetail">
      <el-table-column prop="instanceNo" label="编号" width="180" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="flowName" label="流程" width="140" />
      <el-table-column prop="creatorName" label="发起人" width="100" />
      <el-table-column prop="currentStepName" label="当前节点" width="120" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" width="160">
        <template #default="{ row }">{{ row.submitTime?.replace('T', ' ')?.substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            size="small"
            @click.stop="goDetail(row)"
          >查看</el-button>
          <el-button
            v-if="row.status !== 1 && canManage"
            link
            type="danger"
            size="small"
            @click.stop="handleDelete(row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      style="margin-top: 12px; justify-content: flex-end"
      layout="total, prev, pager, next, sizes"
      @current-change="load"
      @size-change="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/utils/request";
import { hasPerm } from "@/utils/permission";

// 只有流程定义管理员能删流程，gm 只有 flow:all:view
const canManage = computed(() => hasPerm("flow:def:manage"));
import {
  allInstancesV2,
  deleteInstance,
  instanceStatistics,
  publishedDefinitions,
  type FlowInstanceRow,
  type FlowDefinitionItem,
} from "@/api/flow";

const router = useRouter();
const loading = ref(false);
const exporting = ref(false);
const list = ref<FlowInstanceRow[]>([]);
const defs = ref<FlowDefinitionItem[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(20);

const filterDef = ref<number | undefined>(undefined);
const filterStatus = ref<number | undefined>(undefined);
const filterKeyword = ref("");
const dateRange = ref<[string, string] | null>(null);

const stats = ref<any>(null);
const currentDefName = computed(() => {
  const d = defs.value.find((x) => x.id === filterDef.value);
  return d?.flow_name || "";
});

function statusType(s: number) {
  if (s === 1) return "warning";
  if (s === 2) return "success";
  if (s === 3) return "danger";
  return "info";
}

async function load() {
  loading.value = true;
  try {
    const res = await allInstancesV2({
      definitionId: filterDef.value,
      status: filterStatus.value,
      keyword: filterKeyword.value || undefined,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1],
      page: page.value,
      size: size.value,
    });
    list.value = res.records;
    total.value = res.total;

    // 有选流程类型时拉统计
    if (filterDef.value) {
      try {
        stats.value = await instanceStatistics(
          filterDef.value,
          dateRange.value?.[0],
          dateRange.value?.[1]
        );
      } catch {
        stats.value = null;
      }
    } else {
      stats.value = null;
    }
  } finally {
    loading.value = false;
  }
}

function goDetail(row: FlowInstanceRow) {
  router.push({ path: "/home/flow-detail", query: { id: row.id } });
}

async function handleDelete(row: FlowInstanceRow) {
  try {
    await ElMessageBox.confirm(
      `删除「${row.title}」后将不可恢复，确认删除？`,
      "删除流程",
      { type: "warning" }
    );
  } catch {
    return;
  }
  try {
    await deleteInstance(row.id);
    ElMessage.success("已删除");
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "删除失败");
  }
}

async function handleExport() {
  exporting.value = true;
  try {
    const res = await request.get<any, Blob>("/flow/instances/export", {
      responseType: "blob",
      params: {
        definitionId: filterDef.value,
        status: filterStatus.value,
        keyword: filterKeyword.value || undefined,
        startTime: dateRange.value?.[0],
        endTime: dateRange.value?.[1],
      },
    });
    const blob = new Blob([res], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "流程列表.xlsx";
    a.click();
    window.URL.revokeObjectURL(url);
    ElMessage.success("导出成功");
  } catch (e: any) {
    ElMessage.error("导出失败");
  } finally {
    exporting.value = false;
  }
}

onMounted(async () => {
  defs.value = await publishedDefinitions();
  load();
});
</script>

<style scoped>
.all-flows {
  padding: 4px;
}
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.stat-card {
  margin-bottom: 12px;
}
.stat-grid {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}
.stat-item {
  min-width: 120px;
}
.stat-label {
  font-size: 12px;
  color: #909399;
}
.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #1677ff;
  margin-top: 4px;
}
</style>
