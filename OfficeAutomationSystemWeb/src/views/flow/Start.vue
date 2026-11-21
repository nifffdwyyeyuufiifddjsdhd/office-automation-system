<template>
  <div class="flow-start">
    <div class="start-left">
      <div class="panel-title">选择流程</div>
      <div
        v-for="d in defs"
        :key="d.id"
        class="def-card"
        :class="{ active: currentDef?.id === d.id }"
        @click="selectDef(d)"
      >
        <el-tooltip :content="d.flow_name" placement="top" :show-after="300">
          <div class="def-name">{{ d.flow_name }}</div>
        </el-tooltip>
        <el-tooltip :content="d.description || '—'" placement="top" :show-after="300">
          <div class="def-desc">{{ d.description || "—" }}</div>
        </el-tooltip>
      </div>
      <el-empty v-if="!defs.length" description="暂无可用流程" :image-size="60" />
    </div>

    <div class="start-right">
      <div v-if="!currentDef" class="empty-tip">从左侧选择一个流程开始</div>
      <el-card v-else shadow="never">
        <template #header>{{ currentDef.flow_name }} · 申请表单</template>
        <el-form label-width="160px" style="max-width: 700px" class="flow-start-form">
          <el-form-item label="标题">
            <el-input v-model="formTitle" placeholder="默认用「我的 + 流程名」" />
            <span v-if="resubmitId" style="margin-left:12px;color:#e6a23c;font-size:12px">（重新提交已驳回的申请）</span>
          </el-form-item>
          <el-form-item label="紧急程度">
            <el-radio-group v-model="urgency">
              <el-radio :value="0">普通</el-radio>
              <el-radio :value="1">紧急</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            v-for="f in formFields"
            :key="f.field"
            :required="f.required"
          >
            <template #label>
              <el-tooltip :content="f.label" placement="top" :show-after="300">
                <span class="form-label-ellipsis">{{ f.label }}</span>
              </el-tooltip>
            </template>
            <el-input
              v-if="f.type === 'text'"
              v-model="formData[f.field]"
              :placeholder="'请输入' + f.label"
            />
            <el-input
              v-else-if="f.type === 'textarea'"
              v-model="formData[f.field]"
              type="textarea"
              :rows="3"
            />
            <el-input-number
              v-else-if="f.type === 'number'"
              v-model="formData[f.field]"
              :disabled="f.readonly"
              style="width: 100%"
            />
            <el-date-picker
              v-else-if="f.type === 'date'"
              v-model="formData[f.field]"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="附件">
            <div class="upload-wrap" @paste="onPaste" @mouseenter="onUploadHover" tabindex="0" ref="uploadWrapRef">
              <el-upload
                :auto-upload="false"
                :on-change="onFileChange"
                :on-remove="onFileRemove"
                :file-list="fileList"
                multiple
                :limit="10"
                drag
              >
                <div style="padding: 12px 0; color: #606266">
                  拖拽文件到这里，或 <span style="color: #409eff">点击上传</span>
                  ，也可以直接 <kbd style="background:#f4f4f5;padding:1px 6px;border-radius:3px;border:1px solid #dcdfe6;font-family:inherit">Ctrl+V</kbd> 粘贴截图
                </div>
                <div style="color: #909399; font-size: 12px; padding-bottom: 8px">
                  支持图片（JPG/PNG/GIF/WEBP/BMP/SVG）、PDF、Office（Word/Excel/PPT）、压缩包（ZIP/RAR/7Z）、TXT 等；单文件 ≤200MB，最多 10 个
                </div>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
            <el-button @click="openChart">浏览流程图</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 浏览流程图：不提交也能先看清要走几步、谁审批 -->
      <el-dialog v-model="chartVisible" title="流程图" width="420px" append-to-body>
        <div v-loading="chartLoading" class="chart-scroll">
          <FlowChart :nodes="chartNodes" compact end-text="审批通过，流程结束" />
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import type { UploadFile, UploadFiles } from "element-plus";
import { publishedDefinitions, startInstance, resubmitInstance, getInstanceDetail, getDefinitionDetail, uploadAttachment, type FlowDefinitionItem } from "@/api/flow";
import { useUserStore } from "@/store/user";
import FlowChart from "@/components/FlowChart.vue";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const defs = ref<FlowDefinitionItem[]>([]);
const resubmitId = ref<number | null>(null);
const originalStatus = ref<number>(0);
const currentDef = ref<FlowDefinitionItem | null>(null);
const formFields = ref<any[]>([]);
const formData = ref<Record<string, any>>({});
const formTitle = ref("");
const urgency = ref(0);
const submitting = ref(false);

// 附件先上传拿 attachmentId，提交时带 attachmentIds
const fileList = ref<UploadFile[]>([]);
const uploadedIds = ref<number[]>([]);
const uploadWrapRef = ref<HTMLElement | null>(null);
function onUploadHover() {
  // 鼠标移到上传区就聚焦，Ctrl+V 不用先点
  uploadWrapRef.value?.focus();
}

// 表单里第一个「金额/费用」数字项，附件识别出金额就填它
const amountField = computed(() =>
  formFields.value.find((f: any) => f.type === "number" && /金额|费用/.test(f.label || ""))
);

// 附件识别出金额就填表，已填过的不覆盖
function applyAmountSuggestion(amt?: number | null, source?: string | null) {
  if (!amt || Number(amt) <= 0) return;
  const f = amountField.value;
  if (!f) return;
  const cur = formData.value[f.field];
  if (cur !== null && cur !== undefined && cur !== "" && Number(cur) > 0) return;
  formData.value[f.field] = Number(amt);
  ElMessage.success(
    "已从附件识别出" + f.label + " " + Number(amt).toFixed(2) + (source ? "（依据：" + source + "）" : "") + "，请核对"
  );
}

// 附件没读出金额时的处理：用户自己还没填就提醒他手填，已经填过的不打扰
function notifyAmountMissing(source?: string | null) {
  const f = amountField.value;
  if (!f) return;
  const cur = formData.value[f.field];
  if (cur !== null && cur !== undefined && cur !== "" && Number(cur) > 0) return;
  ElMessage.warning("没有从附件里提取到有效金额，请手动填写「" + f.label + "」" + (source ? "（" + source + "）" : ""));
}

// 上传成功后统一处理：有金额就自动填，没有就提示手填
function handleAmountAfterUpload(amt?: number | null, source?: string | null) {
  if (amt && Number(amt) > 0) applyAmountSuggestion(amt, source);
  else notifyAmountMissing(source);
}

async function onFileChange(file: UploadFile, _files: UploadFiles) {
  fileList.value = _files;
  // 自动上传
  try {
    const res = await uploadAttachment(file.raw!);
    uploadedIds.value.push(res.id);
    handleAmountAfterUpload(res.suggestAmount, res.amountSource);
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "附件上传失败");
    // 失败时从列表移除
    fileList.value = _files.filter((f) => f.uid !== file.uid);
  }
}

function onFileRemove(_file: UploadFile, files: UploadFiles) {
  fileList.value = files;
  // 删掉的附件对不上具体 id，只能整批重来
  // 未绑定的附件留在表里，提交时后端才绑定
}

// 粘贴上传从剪贴板取文件，走点击上传同一套逻辑
function onPaste(e: ClipboardEvent) {
  if (!e.clipboardData) return;
  const items = e.clipboardData.items;
  const files: File[] = [];
  for (const it of items) {
    if (it.kind === 'file') {
      const f = it.getAsFile();
      if (f) files.push(f);
    }
  }
  if (!files.length) return;
  e.preventDefault();
  if (fileList.value.length + files.length > 10) {
    ElMessage.warning('附件最多 10 个');
    return;
  }
  for (const f of files) {
    if (f.size > 200 * 1024 * 1024) {
      ElMessage.error(`文件 ${f.name || '粘贴的图片'} 超过 200MB`);
      continue;
    }
    const uid = Date.now() + Math.floor(Math.random() * 10000);
    const uploadFile = {
      uid,
      name: f.type.startsWith('image/') ? `粘贴截图_${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.${(f.type.split('/')[1] || 'png')}` : f.name,
      size: f.size,
      type: f.type,
      raw: f,
      status: 'uploading' as const,
    } as unknown as UploadFile;
    fileList.value = [...fileList.value, uploadFile];
    uploadAttachment(f)
      .then((res) => {
        uploadedIds.value.push(res.id);
        fileList.value = fileList.value.map((x) => x.uid === uid ? { ...x, status: 'success' as const } : x);
        handleAmountAfterUpload(res.suggestAmount, res.amountSource);
      })
      .catch((err) => {
        ElMessage.error(err?.response?.data?.message || '附件上传失败');
        fileList.value = fileList.value.filter((x) => x.uid !== uid);
      });
  }
}

async function loadDefs() {
  defs.value = await publishedDefinitions();
}

// 标题默认带上当前用户的真实姓名，用户可以改
function defaultTitle(d: FlowDefinitionItem) {
  const who = userStore.realName || userStore.username || "我";
  return who + "的" + (d.flow_name || "申请");
}

function selectDef(d: FlowDefinitionItem) {
  currentDef.value = d;
  formTitle.value = defaultTitle(d);
  urgency.value = 0;
  formData.value = {};
  try {
    formFields.value = JSON.parse(d.form_config || "[]");
  } catch {
    formFields.value = [];
  }
  formFields.value.forEach((f) => {
    formData.value[f.field] = f.type === "number" ? null : "";
  });
}

// 表单有 startDate/endDate/days 时，选完日期自动填天数
watch(
  () => [formData.value.startDate, formData.value.endDate],
  ([s, e]) => {
    const hasDaysField = formFields.value.some((f) => f.field === "days");
    if (!hasDaysField) return;
    if (!s || !e) {
      formData.value.days = null;
      return;
    }
    const sd = new Date(s);
    const ed = new Date(e);
    if (sd > ed) {
      formData.value.days = null;
      ElMessage.warning("结束日期不能早于开始日期");
      return;
    }
    // 自然日：结束-开始+1（1月1日到1月1日 = 1天，1月1日到1月2日 = 2天）
    const diff = Math.round((ed.getTime() - sd.getTime()) / 86400000) + 1;
    formData.value.days = diff;
  }
);

async function handleSubmit() {
  if (!currentDef.value) return;
  for (const f of formFields.value) {
    if (f.required && (formData.value[f.field] === "" || formData.value[f.field] == null)) {
      ElMessage.warning(`请填写${f.label}`);
      return;
    }
  }
  submitting.value = true;
  try {
    if (resubmitId.value && originalStatus.value === 1) {
      // 审批人退回给发起人，走 resubmit
      await resubmitInstance(resubmitId.value, formData.value);
      ElMessage.success("已重新提交");
    } else {
      // 被驳回或全新发起，直接 start 新流程
      await startInstance({
        definitionId: currentDef.value.id!,
        title: formTitle.value || undefined,
        businessData: formData.value,
        urgency: urgency.value,
        attachmentIds: uploadedIds.value,
      } as any);
      ElMessage.success(resubmitId.value ? "已重新提交" : "已提交");
    }
    router.push("/home/flow-mine");
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "提交失败");
  } finally {
    submitting.value = false;
  }
}

// 浏览流程图
const chartVisible = ref(false);
const chartLoading = ref(false);
const chartNodes = ref<any[]>([]);

// 审批人类型翻译成一句话；具体是谁由后端在执行时解析
function approverText(s: any) {
  switch (Number(s.approver_type)) {
    case 1:
      return "指定审批人";
    case 2:
      return "按角色审批";
    case 3:
      return "发起人的直属主管";
    case 4:
      return "按职位审批";
    case 5:
      return "发起时自己选审批人";
    case 6:
      return "指定部门（先到先签）";
    default:
      return "";
  }
}

function signText(s: any) {
  const t = Number(s.sign_type);
  if (t === 2) return "会签：候选人都要签";
  if (t === 3) return "会签：签够 " + (Number(s.sign_ratio) || 50) + "% 即流转";
  return "或签：一人通过即流转";
}

async function openChart() {
  if (!currentDef.value?.id) return;
  chartVisible.value = true;
  chartLoading.value = true;
  chartNodes.value = [];
  try {
    const detail: any = await getDefinitionDetail(currentDef.value.id);
    const steps: any[] = detail.steps || [];
    const transitions: any[] = detail.transitions || [];
    chartNodes.value = steps.map((s: any) => {
      const key = String(s._key ?? s.id);
      const bs = transitions
        .filter((t: any) => String(t.step_key) === key)
        .map((t: any) => {
          const target = steps.find((x: any) => String(x._key ?? x.id) === String(t.target_step_key));
          return {
            to: target ? target.step_name : "流程结束",
            condition: t.condition_name || t.condition_expr || "",
            isDefault: Number(t.is_default) === 1,
          };
        });
      return { name: s.step_name, role: approverText(s), extra: signText(s), branches: bs };
    });
    if (!chartNodes.value.length) chartNodes.value = [{ name: "该流程还没有配置审批步骤" }];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "流程图加载失败");
  } finally {
    chartLoading.value = false;
  }
}

onMounted(async () => {
  await loadDefs();
  const rid = route.query.resubmitId;
  if (rid) {
    resubmitId.value = Number(rid);
    try {
      const detail = await getInstanceDetail(resubmitId.value);
      // 选回原流程
      const def = defs.value.find((d) => d.id === detail.definition.id) || detail.definition;
      selectDef(def as FlowDefinitionItem);
      // 回填
      formTitle.value = detail.title || "";
      urgency.value = detail.urgency || 0;
      originalStatus.value = detail.status || 0;
      Object.assign(formData.value, detail.businessData || {});
      // 回填原附件
      if (detail.attachments && detail.attachments.length) {
        uploadedIds.value = detail.attachments.map((a: any) => a.id);
        fileList.value = detail.attachments.map((a: any) => ({
          name: a.fileName,
          size: a.fileSize,
          uid: a.id,
          status: 'success' as const,
        }) as unknown as UploadFile);
      }
    } catch (e: any) {
      ElMessage.error("加载原申请失败：" + (e?.response?.data?.message || ""));
    }
  }
});
</script>

<style scoped>
.flow-start {
  display: flex;
  gap: 16px;
  height: 100%;
}
.chart-scroll {
  max-height: 60vh;
  overflow-y: auto;
}
.upload-wrap {
  outline: none;
}
.upload-wrap:focus {
  outline: 2px dashed #c0c4cc;
  outline-offset: 4px;
  border-radius: 4px;
}
.start-left {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid #ebeef5;
  padding-right: 12px;
  overflow: auto;
}
.panel-title {
  font-weight: 600;
  margin-bottom: 10px;
}
.def-card {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 6px;
  border: 1px solid #ebeef5;
}
.def-card:hover {
  background: #f5f7fa;
}
.def-card.active {
  background: #ecf5ff;
  border-color: #409eff;
}
.def-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  position: relative;
  padding-right: 30px;
}
.def-name::after {
  content: "";
  position: absolute;
  right: 0; top: 0; bottom: 0;
  width: 30px;
  background: linear-gradient(to right, transparent, #fff);
  pointer-events: none;
}
.def-card.active .def-name::after {
  background: linear-gradient(to right, transparent, #ecf5ff);
}
.def-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  position: relative;
  padding-right: 30px;
}
.def-desc::after {
  content: "";
  position: absolute;
  right: 0; top: 0; bottom: 0;
  width: 30px;
  background: linear-gradient(to right, transparent, #fff);
  pointer-events: none;
}
.def-card.active .def-desc::after {
  background: linear-gradient(to right, transparent, #ecf5ff);
}
.flow-start-form :deep(.el-form-item__label) {
  width: 160px !important;
  max-width: 160px;
}
.form-label-ellipsis {
  display: inline-block;
  max-width: 160px;
  white-space: nowrap;
  overflow: hidden;
  vertical-align: bottom;

}
.start-right {
  flex: 1;
  min-width: 0;
}
.empty-tip {
  color: #909399;
  text-align: center;
  margin-top: 80px;
}
</style>
