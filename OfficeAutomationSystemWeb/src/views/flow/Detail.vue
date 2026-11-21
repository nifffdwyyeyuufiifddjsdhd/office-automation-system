<template>
  <div class="flow-detail" v-loading="loading">
    <el-page-header @back="$router.back()" :content="detail?.title || '流程详情'" />

    <el-card v-if="detail" shadow="never" class="card-basic">
      <template #header>
        <div class="detail-head">
          <span>基本信息</span>
          <el-button size="small" @click="openChart">浏览流程图</el-button>
        </div>
      </template>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="流程编号">{{ detail.instanceNo }}</el-descriptions-item>
        <el-descriptions-item label="流程类型">{{ detail.flowName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detail.status)" size="small">{{ detail.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发起人">{{ detail.creatorName }}</el-descriptions-item>
        <el-descriptions-item label="当前节点">{{ detail.currentStepName || "—" }}</el-descriptions-item>
        <el-descriptions-item label="当前审批人">{{ detail.auditorName || "—" }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ fmt(detail.submitTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ fmt(detail.finishTime) || "—" }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="detail" shadow="never" class="card-form">
      <template #header>申请内容</template>
      <!-- 被退回到发起人时，允许修改表单 -->
      <el-form v-if="detail.canResubmit" label-width="120px">
        <el-form-item
          v-for="f in formFields"
          :key="f.field"
          :label="f.label"
          :required="f.required"
        >
          <el-input
            v-if="f.type === 'textarea'"
            v-model="editForm[f.field]"
            type="textarea"
            :rows="3"
          />
          <el-input
            v-else
            v-model="editForm[f.field]"
          />
        </el-form-item>
      </el-form>
      <el-descriptions v-else :column="2" border size="small">
        <el-descriptions-item
          v-for="f in formFields"
          :key="f.field"
          :label="f.label"
        >
          {{ detail.businessData?.[f.field] || "—" }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 付款状态：要财务付款的流程，通过后能看到付没付 -->
    <el-card v-if="detail && detail.payStatus !== undefined" shadow="never" class="card-pay">
      <template #header>
        <span>付款情况</span>
        <el-tag
          size="small"
          style="margin-left: 8px"
          :type="detail.payStatus === 1 ? 'success' : detail.payStatus === 2 ? 'info' : 'warning'"
        >
          {{ detail.payStatusText || "—" }}
        </el-tag>
      </template>
      <el-descriptions v-if="detail.payStatus === 1 || detail.payStatus === 2" :column="3" border size="small">
        <el-descriptions-item label="付款单号">{{ detail.payNo || "—" }}</el-descriptions-item>
        <el-descriptions-item label="付款金额">
          {{ detail.payAmount === null || detail.payAmount === undefined ? "—" : Number(detail.payAmount).toFixed(2) + " 元" }}
        </el-descriptions-item>
        <el-descriptions-item label="付款时间">{{ fmt(detail.payTime) || "—" }}</el-descriptions-item>
        <el-descriptions-item label="收款人">{{ detail.payeeName || "—" }}</el-descriptions-item>
        <el-descriptions-item label="费用类别">{{ detail.categoryName || "未分类" }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.payStatus === 2" label="作废原因">
          {{ detail.voidReason || "—" }}
        </el-descriptions-item>
      </el-descriptions>
      <el-alert v-else :type="detail.payStatusText === '待付款' ? 'warning' : 'info'" :closable="false">
        {{ detail.payStatusText === "待付款"
          ? "审批已通过，等待财务付款。财务付款后这里会显示付款单号和付款时间。"
          : "该流程不需要财务付款。" }}
      </el-alert>
    </el-card>

    <el-card v-if="detail" shadow="never" class="card-attach">
      <template #header>附件（{{ detail.attachments?.length || 0 }}）</template>
      <el-empty v-if="!detail.attachments?.length" description="无附件" :image-size="50" />
      <div v-else>
        <div
          v-for="a in detail.attachments"
          :key="a.id"
          style="padding: 6px 0; border-bottom: 1px solid #f0f0f0"
        >
          <el-link :href="a.downloadUrl" :underline="false" target="_blank">
            📎 {{ a.fileName }}
            <span style="color: #909399; font-size: 12px; margin-left: 8px">
              ({{ formatSize(a.fileSize) }})
            </span>
          </el-link>
        </div>
      </div>
    </el-card>

    <el-card v-if="detail" shadow="never" class="card-track">
      <template #header>审批轨迹</template>
      <el-timeline>
        <el-timeline-item
          v-for="(t, i) in detail.track"
          :key="i"
          :timestamp="fmt(t.time)"
          :type="trackType(t.action)"
        >
          <b>{{ t.approverName }}</b>
          <el-tag size="small" style="margin-left: 8px">{{ t.actionText }}</el-tag>
          <span v-if="t.targetName" style="margin-left: 8px; color: #909399">转交至：{{ t.targetName }}</span>
          <div v-if="t.comment" style="color: #606266; margin-top: 4px">{{ t.comment }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 审批操作区（当前审批人） -->
    <el-card v-if="detail?.isCurrentAuditor && detail?.status === 1" shadow="never" class="card-approve">
      <template #header>审批操作</template>
      <!-- 会签步骤显示已签人数，签过的人不再显示通过按钮 -->
      <el-alert v-if="detail?.countersign" :type="detail?.signSignedByMe ? 'success' : 'info'" :closable="false"
        style="margin-bottom: 12px">
        <template #title>
          会签进度 {{ detail.signApproved || 0 }}/{{ detail.signRequired || 0 }}
          <span v-if="detail?.signSignedByMe">（您已提交会签意见）</span>
        </template>
        <div>已签：{{ (detail.signApprovedNames || []).join('、') || '暂无' }}</div>
        <div v-if="(detail.signPendingNames || []).length">待签：{{ (detail.signPendingNames || []).join('、') }}</div>
      </el-alert>
      <el-form v-if="!detail?.signSignedByMe" label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="opinion" type="textarea" :rows="2" maxlength="500" show-word-limit
            placeholder="通过时可留空（默认填：同意）；驳回时必填，至少5个字；意见里用 @姓名 可以提醒到对方" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="handleApprove">{{ detail?.countersign ? '会签通过' : '通过' }}</el-button>
          <el-button type="danger" @click="openRejectDialog">驳回</el-button>
          <el-button @click="openTransfer">转交他人</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 发起人重新提交（被退回到发起人） -->
    <el-card v-if="detail?.canResubmit" shadow="never" class="card-resubmit">
      <template #header>被退回修改</template>
      <el-alert type="warning" :closable="false" style="margin-bottom: 12px">
        该流程被退回到您这里，请修改申请表单后重新提交，流程将从退回的下一步继续审批（不会从头走）。
      </el-alert>
      <el-button type="primary" @click="handleResubmit">修改完成，重新提交</el-button>
    </el-card>

    <!-- 发起人撤回 -->
    <el-card v-if="detail?.isMine && detail?.status === 1 && !detail.isCurrentAuditor" shadow="never" class="card-withdraw">
      <template #header>发起人操作</template>
      <el-button v-if="hasPerm('flow:withdraw')" type="warning" @click="handleWithdraw">撤回申请</el-button>
      <el-button v-if="hasPerm('flow:urge')" type="primary" plain @click="handleUrge" style="margin-left: 8px">催办</el-button>
    </el-card>

    <!-- 驳回对话框（选退回目标） -->
    <el-dialog v-model="rejectDialogVisible" title="驳回流程" width="480px">
      <el-form label-width="100px">
        <el-form-item label="退回方式" required>
          <el-select v-model="rejectTargetStepId" style="width: 100%">
            <el-option
              v-for="s in returnableList"
              :key="s.stepId === null ? 'end' : s.stepId"
              :label="s.stepName"
              :value="s.stepId as any"
            />
          </el-select>
          <div style="color: #909399; font-size: 12px; line-height: 1.5">
            选"直接终止流程"则流程结束；选"退回发起人/某步骤"则发起人修改后从该步骤继续审。
          </div>
        </el-form-item>
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectComment" type="textarea" :rows="3" maxlength="500" show-word-limit
            placeholder="必填，至少5个字，会通知被退回的人" />
          <div style="color: #f56c6c; font-size: 12px">{{ rejectComment.length }} / 500（至少5字）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 转交对话框 -->
    <el-dialog v-model="transferDialogVisible" title="转交审批" width="480px">
      <el-form label-width="100px">
        <el-form-item label="转交给" required>
          <el-select v-model="transferUserId" filterable placeholder="选择能接手这一步的同事" style="width: 100%">
            <el-option
              v-for="c in transferCandidates"
              :key="c.id"
              :label="c.realName + '（' + (c.orgName || '无部门') + '）'"
              :value="c.id"
            />
          </el-select>
          <div style="color: #909399; font-size: 12px; line-height: 1.5">
            这里只列出当前步骤的候选审批人；同一步骤最多转交 3 次。
          </div>
        </el-form-item>
        <el-form-item label="转交原因" required>
          <el-input v-model="transferComment" type="textarea" :rows="3" maxlength="500" show-word-limit
            placeholder="必填，至少5个字，会写入审批轨迹" />
          <div style="color: #f56c6c; font-size: 12px">{{ transferComment.length }} / 500（至少5字）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTransfer">确认转交</el-button>
      </template>
    </el-dialog>

    <!-- 流程图浏览：看清这条流程要走几步、现在卡在哪一步 -->
    <el-dialog v-model="chartVisible" title="流程图" width="470px">
      <div class="chart-scroll" v-loading="chartLoading">
        <FlowChart :nodes="chartNodes" :creator="detail?.creatorName || '发起人'" creator-role="发起人" end-text="审批通过，流程结束" />
      </div>
      <template #footer>
        <el-button @click="chartVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { hasPerm } from "@/utils/permission";
import FlowChart from "@/components/FlowChart.vue";
import {
  getInstanceDetail,
  getDefinitionDetail,
  approveInstance,
  rejectInstanceV2,
  transferInstance,
  withdrawInstance,
  resubmitInstance,
  returnableSteps,
  transferCandidates as listTransferCandidates,
  type TransferCandidate,
} from "@/api/flow";

const route = useRoute();

const loading = ref(false);
const detail = ref<any>(null);
const formFields = ref<any[]>([]);
const opinion = ref("");

// 驳回对话框
const rejectDialogVisible = ref(false);
const rejectComment = ref("");
const rejectTargetStepId = ref<number | null | undefined>(null);
const returnableList = ref<{ stepId: number | null; stepName: string }[]>([]);

// 转交对话框
const transferDialogVisible = ref(false);
const transferUserId = ref<number | null>(null);
const transferComment = ref("");
const transferCandidates = ref<TransferCandidate[]>([]);

// 被退回后修改表单
const editForm = reactive<Record<string, any>>({});

function fmt(t?: string) {
  if (!t) return "";
  return t.replace("T", " ").substring(0, 16);
}
function formatSize(bytes?: number) {
  if (!bytes) return "";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / 1024 / 1024).toFixed(1) + " MB";
}
function statusType(s: number) {
  if (s === 1) return "warning";
  if (s === 2) return "success";
  if (s === 3) return "danger";
  return "info";
}
function trackType(action: number) {
  if (action === 1) return "success";
  if (action === 2) return "danger";
  if (action === 3) return "warning";
  if (action === 4) return "info";
  return "primary";
}

// 浏览流程图
const chartVisible = ref(false);
const chartLoading = ref(false);
const chartNodes = ref<any[]>([]);

function approverText(s: any) {
  switch (Number(s.approver_type)) {
    case 1: return "指定审批人";
    case 2: return "按角色审批";
    case 3: return "发起人的直属主管";
    case 4: return "按职位审批";
    case 5: return "发起时自己选审批人";
    case 6: return "指定部门（先到先签）";
    default: return "";
  }
}

function signText(s: any) {
  const t = Number(s.sign_type);
  if (t === 2) return "会签：候选人都要签";
  if (t === 3) return "会签：签够 " + (Number(s.sign_ratio) || 50) + "% 即流转";
  return "或签：一人通过即流转";
}

// 节点状态按实例进度推断：已过算完成，当前高亮，驳回标红
async function openChart() {
  const defId = detail.value?.definition?.id;
  if (!defId) { ElMessage.warning("流程定义已删除，无法显示流程图"); return; }
  chartVisible.value = true;
  chartLoading.value = true;
  chartNodes.value = [];
  try {
    const def: any = await getDefinitionDetail(defId);
    const steps: any[] = def.steps || [];
    const st = Number(detail.value?.status);
    const currentId = detail.value?.currentStepId ?? detail.value?.current_step_id ?? null;
    const currentName = detail.value?.currentStepName;
    let passed = true;
    chartNodes.value = steps.map((s: any) => {
      const isCurrent = (currentId && s.id === currentId) || (!currentId && s.step_name === currentName);
      let status: string = "wait";
      if (st === 2) status = "done";
      else if (isCurrent) status = st === 3 ? "reject" : "current";
      else if (passed && st !== 3) status = "done";
      if (isCurrent) passed = false;
      return {
        name: s.step_name,
        role: approverText(s),
        extra: signText(s),
        status,
      };
    });
    if (!chartNodes.value.length) chartNodes.value = [{ name: "该流程没有配置审批步骤" }];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "流程图加载失败");
  } finally {
    chartLoading.value = false;
  }
}

async function load() {
  const id = Number(route.query.id);
  loading.value = true;
  try {
    detail.value = await getInstanceDetail(id);
    try {
      formFields.value = JSON.parse(detail.value.definition?.form_config || "[]");
    } catch {
      formFields.value = [];
    }
    // 初始化编辑表单
    Object.keys(editForm).forEach((k) => delete editForm[k]);
    for (const f of formFields.value) {
      editForm[f.field] = detail.value.businessData?.[f.field] ?? "";
    }
  } finally {
    loading.value = false;
  }
}

async function handleApprove() {
  try {
    await ElMessageBox.confirm("确认通过该流程？", "通过", { type: "success" });
  } catch {
    return;
  }
  try {
    await approveInstance(detail.value.id, opinion.value);
    await load();
    // 会签人数不够时流程停在本节点，提示只说记了一票
    if (detail.value?.countersign && detail.value?.signSignedByMe) {
      ElMessage.success("已记录您的会签意见，等待其他审批人处理");
    } else {
      ElMessage.success("已通过");
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}

async function openRejectDialog() {
  rejectComment.value = "";
  rejectTargetStepId.value = null;
  try {
    returnableList.value = await returnableSteps(detail.value.id);
  } catch {
    returnableList.value = [{ stepId: null, stepName: "直接终止流程" }];
  }
  rejectDialogVisible.value = true;
}

async function handleReject() {
  if (rejectComment.value.trim().length < 5) {
    ElMessage.warning("驳回原因至少5个字");
    return;
  }
  try {
    await rejectInstanceV2(detail.value.id, rejectComment.value.trim(), rejectTargetStepId.value);
    ElMessage.success("已驳回");
    rejectDialogVisible.value = false;
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}

// 打开转交弹窗时拉候选人列表
async function openTransfer() {
  transferUserId.value = null;
  transferComment.value = "";
  try {
    transferCandidates.value = await listTransferCandidates(detail.value.id);
  } catch (e: any) {
    transferCandidates.value = [];
    ElMessage.error(e?.response?.data?.message || "加载候选人失败");
    return;
  }
  transferDialogVisible.value = true;
}

async function handleTransfer() {
  if (!transferUserId.value) {
    ElMessage.warning("请选择被转交人");
    return;
  }
  if (transferComment.value.trim().length < 5) {
    ElMessage.warning("转交原因至少5个字");
    return;
  }
  try {
    await transferInstance(detail.value.id, transferUserId.value, transferComment.value.trim());
    ElMessage.success("已转交");
    transferDialogVisible.value = false;
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "转交失败");
  }
}

async function handleWithdraw() {
  try {
    await ElMessageBox.confirm("撤回后该流程将终止，确认撤回？", "撤回", { type: "warning" });
  } catch {
    return;
  }
  try {
    // 动态导入，免得循环依赖
    const { urgeInstance } = await import("@/api/flow");
    await withdrawInstance(detail.value.id);
    ElMessage.success("已撤回");
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}

async function handleUrge() {
  try {
    const { urgeInstance } = await import("@/api/flow");
    await urgeInstance(detail.value.id);
    ElMessage.success("已催办");
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "催办失败");
  }
}

async function handleResubmit() {
  try {
    await resubmitInstance(detail.value.id, { ...editForm });
    ElMessage.success("已重新提交");
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "提交失败");
  }
}

onMounted(load);
</script>

<style scoped>
/* 详情页左右两栏：左边是单据内容和要你操作的地方，右边是状态、付款和审批轨迹 */
.flow-detail {
  padding: 4px;
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.flow-detail > .el-card {
  margin-top: 0 !important;
}

/* 页头和基本信息横跨整宽 */
.flow-detail > .el-page-header,
.flow-detail > .card-basic {
  grid-column: 1 / -1;
}

.flow-detail > .card-form,
.flow-detail > .card-attach,
.flow-detail > .card-approve,
.flow-detail > .card-resubmit,
.flow-detail > .card-withdraw {
  grid-column: 1;
}

.flow-detail > .card-pay,
.flow-detail > .card-track {
  grid-column: 2;
}

/* 窄屏收成一栏，避免描述列表被挤成两三行一个字 */
@media (max-width: 1400px) {
  .flow-detail {
    grid-template-columns: minmax(0, 1fr);
  }
  .flow-detail > .card-pay,
  .flow-detail > .card-track,
  .flow-detail > .card-form,
  .flow-detail > .card-attach,
  .flow-detail > .card-approve,
  .flow-detail > .card-resubmit,
  .flow-detail > .card-withdraw,
  .flow-detail > .card-basic {
    grid-column: 1 / -1;
  }
}
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-scroll {
  max-height: 60vh;
  overflow: auto;
  padding: 4px 2px;
}
</style>

