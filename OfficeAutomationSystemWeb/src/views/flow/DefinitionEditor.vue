<template>
  <div class="flow-editor">
    <el-page-header @back="$router.back()" content="流程定义编辑" />

    <el-collapse v-model="helpOpen" class="editor-help">
      <el-collapse-item name="help">
        <template #title>
          <span class="help-title">这个页面在配什么？点开看三分钟版说明</span>
        </template>
        <div class="help-line"><b>申请内容</b>：员工发起这条流程时要填的格子。比如请假就填开始日期、结束日期、事由，这几格就是「申请内容」。</div>
        <div class="help-line"><b>审批步骤</b>：这张单子要经过几个人点头、按什么顺序点头。一步就是一次「谁来审」，从上往下走。</div>
        <div class="help-line"><b>谁来审批</b>：这一步由哪种人审。可以指定某一个人、某个岗位（角色）、发起人的主管、某个职位，或者某个部门里谁先看到谁先办（待认领）。</div>
        <div class="help-line"><b>会签方式</b>：这一步有多个候选人时怎么算通过。或签＝任意一人点了通过就往下走；全部会签＝名单上每个人都得点；比例会签＝签够了设定百分比就往下走。</div>
        <div class="help-line"><b>什么情况走下一步</b>：只有需要分叉时才加。不加条件时，这一步审完自动按顺序走到下一步；加了条件就按条件挑路，比如「金额&gt;1000 走财务、否则直接结束」。</div>
        <div class="help-line"><b>右边流程图</b>：按左边填的内容实时画出来。图上每步可以上移、下移、删、点「编辑」跳到左边对应卡片，节点之间能插新步骤。</div>
      </el-collapse-item>
    </el-collapse>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header>基础信息</template>
      <el-form label-width="80px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="流程名称" required>
              <el-input v-model="def.flow_name" placeholder="如：请假申请" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="流程标识" required>
              <el-input v-model="def.code" placeholder="英文，如 leave，不可重复" :disabled="!!def.id" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="def.sort_order" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="适用范围">
          <el-radio-group v-model="def.use_scope">
            <el-radio :value="0">全员可用</el-radio>
            <el-radio :value="1" :disabled="!isAdmin">指定部门</el-radio>
            <el-radio :value="2" :disabled="!isAdmin">指定角色</el-radio>
          </el-radio-group>
          <el-select
            v-if="def.use_scope === 1"
            v-model="scopeIds"
            multiple
            filterable
            collapse-tags
            placeholder="选择部门（含下级部门）"
            style="margin-left: 12px; width: 320px"
          >
            <el-option v-for="o in orgOptions" :key="o.id" :label="o.label" :value="o.id" />
          </el-select>
          <el-select
            v-else-if="def.use_scope === 2"
            v-model="scopeIds"
            multiple
            filterable
            collapse-tags
            placeholder="选择可以使用该流程的角色"
            style="margin-left: 12px; width: 320px"
          >
            <el-option v-for="r in roleList" :key="r.id" :label="r.role_name" :value="r.id" />
          </el-select>
          <span v-if="def.use_scope !== 0 && !scopeIds.length" style="margin-left: 12px; color: #e6a23c; font-size: 12px">
            没选具体对象时视为全员可用
          </span>
          <span v-if="!isAdmin" style="margin-left: 12px; color: #909399; font-size: 12px">
            非管理员只能用自己部门可见的流程
          </span>
        </el-form-item>
        <!-- 勾上后审批通过的单子会进财务待付款池 -->
        <el-form-item label="财务付款">
          <el-switch v-model="def.need_payment" :active-value="1" :inactive-value="0" />
          <span class="scope-tip">审批通过后需要财务执行付款（报销、付款、采购等资金类流程打开）</span>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="def.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </el-card>

    <div class="editor-body">
      <div class="editor-left">
    <el-card shadow="never" style="margin-top: 12px">
      <template #header>
        <div class="card-header">
          <span>员工填写的申请内容</span>
          <el-button link type="primary" size="small" @click="addFormField">+ 添加字段</el-button>
        </div>
      </template>
      <el-table :data="formFields" size="small" border>
        <el-table-column label="给员工看的名称" min-width="200">
          <template #default="{ row }">
            <el-input v-model="row.label" placeholder="如 请假事由" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="填写方式" width="160">
          <template #default="{ row }">
            <el-select v-model="row.type" size="small">
              <el-option label="一行输入框" value="text" />
              <el-option label="多行输入框" value="textarea" />
              <el-option label="数字" value="number" />
              <el-option label="日期" value="date" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="必填" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.required" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="formFields.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="tip">员工提交申请时就会按这些内容填写。</div>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header>
        <div class="card-header">
          <span>审批步骤</span>
          <el-button link type="primary" size="small" @click="addStep">+ 添加步骤</el-button>
        </div>
      </template>

      <div v-if="!steps.length" class="empty-steps">还没有审批步骤，点右上角「+ 添加步骤」加一个。</div>
      <div
        v-for="(step, si) in steps"
        :key="step._key"
        :ref="(el) => setStepRef(el, si)"
        class="step-card"
        :class="{ 'is-active': activeStepIndex === si }"
      >
        <div class="step-head">
          <span class="step-no">步骤 {{ si + 1 }}</span>
          <el-input v-model="step.step_name" size="small" class="step-name" placeholder="步骤名称，如 部门主管审批" />
          <el-button link size="small" :disabled="si === 0" @click="moveStep(si, -1)">上移</el-button>
          <el-button link size="small" :disabled="si === steps.length - 1" @click="moveStep(si, 1)">下移</el-button>
          <el-button link type="danger" size="small" @click="removeStep(si)">删除步骤</el-button>
        </div>
        <el-form label-width="88px" style="margin-top: 6px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="谁来审批">
              <el-select v-model="step.approver_type" size="small">
                <el-option :value="1" label="指定某人" />
                <el-option :value="2" label="按岗位（角色）" />
                <el-option :value="3" label="员工自己的主管" />
                <el-option :value="4" label="按职位" />
                <el-option :value="5" label="员工自己选审批人" />
                <el-option :value="6" label="指定部门（谁先处理谁审批）" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6" v-if="step.approver_type === 1">
            <el-form-item label="审批人">
              <el-select v-model="step._userId" size="small" filterable placeholder="选择用户">
                <el-option v-for="u in userList" :key="u.id" :label="u.real_name + '（' + u.username + '）'" :value="String(u.id)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6" v-if="step.approver_type === 2">
            <el-form-item label="角色">
              <el-select v-model="step._roleId" size="small" placeholder="选择角色">
                <el-option v-for="r in roleList" :key="r.id" :label="r.role_name" :value="String(r.id)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6" v-if="step.approver_type === 4">
            <el-form-item label="职位">
              <el-select v-model="step._positionId" size="small" placeholder="选择职位">
                <el-option v-for="p in positionList" :key="p.id" :label="p.position_name" :value="String(p.id)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6" v-if="step.approver_type === 6">
            <el-form-item label="部门">
              <el-cascader
                v-model="step._orgIdPath"
                :options="orgTreeData"
                :props="{ checkStrictly: true, value: 'id', label: 'label', children: 'children' }"
                size="small"
                placeholder="选择部门"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="会签方式">
              <el-select v-model="step.sign_type" size="small">
                <el-option :value="1" label="或签（任意一人通过即流转）" />
                <el-option :value="2" label="全部会签（候选人都要签）" />
                <el-option :value="3" label="比例会签（签够比例即流转）" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6" v-if="Number(step.sign_type) === 3">
            <el-form-item label="通过比例">
              <el-input-number v-model="step.sign_ratio" :min="1" :max="100" size="small" />
              <span style="margin-left: 6px">%</span>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="trans-box">
          <div class="trans-title">
            <span>什么情况走下一步</span>
            <el-button link type="primary" size="small" @click="addTransition(step)">+ 添加条件</el-button>
          </div>
          <div v-if="!step._transitions.length" class="trans-empty">
            这一步不填条件也能用：审完自动按顺序走下一步。只有要分叉（金额大先走财务、天数多再加一级）时才加。
          </div>
          <div v-for="(tr, ti) in step._transitions" :key="ti" class="trans-row">
            <div class="trans-field">
              <span class="trans-label">走到哪一步</span>
              <el-select v-model="tr.target_step_key" size="small" class="trans-control">
                <el-option label="（流程结束）" value="__end__" />
                <el-option
                  v-for="(s, j) in steps"
                  :key="s._key"
                  :label="j === si ? '（下一步：' + s.step_name + '）' : s.step_name"
                  :value="s._key"
                  :disabled="j <= si"
                />
              </el-select>
            </div>
            <div class="trans-field trans-grow">
              <span class="trans-label">满足什么条件</span>
              <el-input v-model="tr.condition_expr" size="small" class="trans-control" placeholder="如 金额>1000，留空表示任何情况都走" />
            </div>
            <div class="trans-field">
              <span class="trans-label">这条路径叫什么</span>
              <el-input v-model="tr.condition_name" size="small" class="trans-control" placeholder="如 金额大走财务" />
            </div>
            <div class="trans-foot">
              <el-checkbox v-model="tr._default" size="small">上面都不满足时走这条</el-checkbox>
              <el-button link type="danger" size="small" @click="step._transitions.splice(ti, 1)">删除条件</el-button>
            </div>
          </div>
        </div>
        </el-form>
      </div>
    </el-card>
      </div>

      <aside class="editor-right">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>流程图</span>
              <span class="chart-hint">边编辑边更新</span>
            </div>
          </template>
          <div class="chart-toolbar">
            <el-button size="small" @click="addStep">+ 加一步</el-button>
            <span class="chart-toolbar-tip">图上 ↑↓ 挪顺序、编辑跳过去、删掉这一步</span>
          </div>
          <FlowChart
            :nodes="previewNodes"
            compact
            editable
            :active-index="activeStepIndex"
            @op="onChartOp"
          />
          <div class="tip">
            发起人在员工提交时才确定，模板阶段留空；分支按左边「什么情况走下一步」的条件画出来。
          </div>
        </el-card>
      </aside>
    </div>

    <div class="save-bar">
      <el-button @click="$router.back()">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存草稿</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import FlowChart from "@/components/FlowChart.vue";
import { ElMessage } from "element-plus";
import {
  getDefinitionDetail,
  createDefinition,
  updateDefinition,
  type FlowDefinitionItem,
  type FlowStepItem,
  type FlowTransitionItem,
} from "@/api/flow";
import {
  listRoles,
  pageUsers,
  orgTree,
  listPositions,
  type RoleOption,
  type UserItem,
  type OrgNode,
  type PositionItem,
} from "@/api/user";

interface FormField {
  field: string;
  label: string;
  type: string;
  required: boolean;
}

const route = useRoute();
const router = useRouter();
const saving = ref(false);

// 顶部说明面板，默认收起，别一进来就被一大段字压住
const helpOpen = ref<string[]>([]);

// 右边流程图点了某一步时，左边对应卡片高亮并滚过去
const activeStepIndex = ref(-1);
const stepEls = new Map<number, HTMLElement>();

function setStepRef(el: any, index: number) {
  const node = el as HTMLElement | null;
  if (node) stepEls.set(index, node);
  else stepEls.delete(index);
}

function focusStep(index: number) {
  activeStepIndex.value = index;
  nextTick(() => {
    stepEls.get(index)?.scrollIntoView({ block: "center", behavior: "smooth" });
  });
}

function moveStep(index: number, delta: number) {
  const to = index + delta;
  if (to < 0 || to >= steps.value.length) return;
  const list = steps.value.slice();
  const moved = list.splice(index, 1)[0];
  list.splice(to, 0, moved);
  steps.value = list;
  activeStepIndex.value = to;
}

// 流程图上的操作：上移、下移、跳到左边编辑、删除、在某一步前面插入
function onChartOp(action: string, index: number) {
  if (action === "up") return moveStep(index, -1);
  if (action === "down") return moveStep(index, 1);
  if (action === "edit") return focusStep(index);
  if (action === "remove") return removeStep(index);
  if (action === "insert") {
    const at = Math.min(index, steps.value.length);
    addStep();
    // addStep 把新步骤放在末尾，插在中间时手动挪到目标位置
    const list = steps.value.slice();
    const added = list.pop();
    if (!added) return;
    list.splice(at, 0, added);
    steps.value = list;
    focusStep(at);
  }
}

// 下拉数据源
const userList = ref<UserItem[]>([]);
const roleList = ref<RoleOption[]>([]);
const positionList = ref<PositionItem[]>([]);
const orgTreeData = ref<OrgNode[]>([]);

const def = reactive<FlowDefinitionItem>({
  flow_name: "",
  code: "",
  description: "",
  sort_order: 0,
  flow_type: 1,
  need_payment: 0,
  use_scope: 0,
});

// 选中的部门/角色 id，提交时转成 use_scope_ids
const scopeIds = ref<number[]>([]);

// 部门树拍平成下拉选项，用缩进体现层级
const orgOptions = computed(() => {
  const out: { id: number; label: string }[] = [];
  const walk = (list: any[], depth: number) => {
    for (const n of list || []) {
      out.push({ id: n.id, label: "　".repeat(depth) + (n.label || n.name || "") });
      if (n.children?.length) walk(n.children, depth + 1);
    }
  };
  walk(orgTreeData.value, 0);
  return out;
});

// 当前用户是否 admin，取自 userStore
import { useUserStore } from "@/store/user";
const userStore = useUserStore();
const isAdmin = computed(() => (userStore.roles || []).includes("admin"));

const formFields = ref<FormField[]>([]);

// 步骤是前端结构，_userId/_roleId/_positionId/_transitions 只在编辑期用
const steps = ref<any[]>([]);

function addFormField() {
  formFields.value.push({ field: "field_" + Date.now().toString(36), label: "", type: "text", required: false });
}

function addStep() {
  steps.value.push({
    _key: "step_" + Date.now() + "_" + steps.value.length,
    step_name: "",
    step_code: "",
    approver_type: 1,
    _userId: "",
    _roleId: "",
    _positionId: "",
    _orgIdPath: [] as number[],
    sort_order: steps.value.length + 1,
    step_type: 1,
    sign_type: 1,
    sign_ratio: 100,
    _transitions: [],
  });
}

function removeStep(i: number) {
  steps.value.splice(i, 1);
  if (activeStepIndex.value >= steps.value.length) activeStepIndex.value = steps.value.length - 1;
}

// 审批人说法，流程图和详情页共用
function approverLabel(step: any): string {
  switch (Number(step.approver_type)) {
    case 1: {
      const u = userList.value.find((x) => String(x.id) === String(step._userId));
      return u ? "指定：" + u.real_name : "指定某人（未选）";
    }
    case 2: {
      const r = roleList.value.find((x) => String(x.id) === String(step._roleId));
      return r ? "角色：" + r.role_name : "按角色（未选）";
    }
    case 3:
      return "发起人的直属主管";
    case 4: {
      const p = positionList.value.find((x) => String(x.id) === String(step._positionId));
      return p ? "职位：" + p.position_name : "按职位（未选）";
    }
    case 5:
      return "由发起人自己选审批人";
    case 6:
      return "指定部门（先到先签）";
    default:
      return "";
  }
}

function signLabel(step: any): string {
  const t = Number(step.sign_type);
  if (t === 2) return "会签：候选人都要签";
  if (t === 3) return "会签：签够 " + (Number(step.sign_ratio) || 50) + "% 即流转";
  return "或签：一人通过即流转";
}

// 编辑期的步骤是前端结构，映射成流程图节点
const previewNodes = computed(() =>
  steps.value.map((s: any) => ({
    name: s.step_name || "未命名步骤",
    role: approverLabel(s),
    extra: signLabel(s),
    branches: (s._transitions || []).map((t: any) => {
      const target = steps.value.find((x: any) => x._key === t.target_step_key);
      return {
        to: !target ? "流程结束" : target.step_name || "未命名步骤",
        condition: t.condition_name || t.condition_expr || "",
        isDefault: !!t._default,
      };
    }),
  }))
);

function addTransition(step: any) {
  step._transitions.push({
    target_step_key: "__end__",
    condition_expr: "",
    condition_name: "",
    priority: 0,
    _default: false,
  });
}

async function handleSave() {
  if (!def.flow_name.trim()) {
    ElMessage.warning("请填写流程名称");
    return;
  }
  if (!def.code.trim()) {
    ElMessage.warning("请填写流程标识（英文）");
    return;
  }
  if (!steps.value.length) {
    ElMessage.warning("至少添加一个审批步骤");
    return;
  }
  for (const s of steps.value) {
    if (!s.step_name.trim()) {
      ElMessage.warning("每个步骤都要填写名称");
      return;
    }
    // 校验审批人配置：选了需要具体对象的类型但没选对象
    if (s.approver_type === 1 && !s._userId) {
      ElMessage.warning(`步骤「${s.step_name}」选了"指定用户"，但没选具体审批人`);
      return;
    }
    if (s.approver_type === 2 && !s._roleId) {
      ElMessage.warning(`步骤「${s.step_name}」选了"指定角色"，但没选具体角色`);
      return;
    }
    if (s.approver_type === 4 && !s._positionId) {
      ElMessage.warning(`步骤「${s.step_name}」选了"职位匹配"，但没选具体职位`);
      return;
    }
    if (s.approver_type === 6 && (!s._orgIdPath || !s._orgIdPath.length)) {
      ElMessage.warning(`步骤「${s.step_name}」选了"指定部门"，但没选具体部门`);
      return;
    }
    if (Number(s.sign_type) === 3 && (!s.sign_ratio || Number(s.sign_ratio) < 1 || Number(s.sign_ratio) > 100)) {
      ElMessage.warning(`步骤「${s.step_name}」选了"比例会签"，通过比例要在 1~100 之间`);
      return;
    }
  }

  // 组装 form_config
  def.form_config = JSON.stringify(formFields.value);

  // 使用范围存 id 数组，同时回写 dept_id 给老列表用
  const outDef: any = { ...def };
  outDef.use_scope = def.use_scope || 0;
  outDef.use_scope_ids = scopeIds.value.length ? JSON.stringify(scopeIds.value) : null;
  outDef.dept_id = def.use_scope === 1 && scopeIds.value.length ? scopeIds.value[0] : null;
  if (def.use_scope !== 0 && !scopeIds.value.length) {
    ElMessage.warning("选了指定部门/角色但没勾选具体对象，将按全员可用处理");
  }

  // 组装 steps + transitions
  const outSteps: FlowStepItem[] = steps.value.map((s, i) => {
    let config = "";
    if (s.approver_type === 1 && s._userId) config = JSON.stringify({ userId: Number(s._userId) });
    if (s.approver_type === 2 && s._roleId) config = JSON.stringify({ roleId: Number(s._roleId) });
    if (s.approver_type === 4 && s._positionId) config = JSON.stringify({ positionId: Number(s._positionId) });
    if (s.approver_type === 6 && s._orgIdPath?.length) {
      // cascader 路径是 [父id, ..., 部门id]，取最后一个
      config = JSON.stringify({ orgId: Number(s._orgIdPath[s._orgIdPath.length - 1]) });
    }
    return {
      _key: s._key,
      step_name: s.step_name,
      step_code: s.step_code || "",
      step_type: 1,
      approver_type: s.approver_type,
      approver_config: config,
      sort_order: i + 1,
      sign_type: Number(s.sign_type) || 1,
      sign_ratio: Number(s.sign_type) === 3 ? Number(s.sign_ratio) || 100 : 100,
    };
  });

  const outTransitions: FlowTransitionItem[] = [];
  steps.value.forEach((s) => {
    for (const t of s._transitions || []) {
      if (t.target_step_key === "__end__") continue; // 结束不需要 transition
      outTransitions.push({
        step_key: s._key,
        target_step_key: t.target_step_key,
        condition_expr: t.condition_expr || "",
        condition_name: t.condition_name || "",
        priority: t.priority || 0,
        is_default: t._default ? 1 : 0,
        transition_type: 1,
      });
    }
  });

  saving.value = true;
  try {
    if (def.id) {
      const res = await updateDefinition(def.id, { definition: outDef, steps: outSteps, transitions: outTransitions });
      if (res?.mode === "new-version") {
        // 已发布且有在途单据的流程，后端会另存新版本
        if (res.published) {
          ElMessage.success(res.message || `已生成 v${res.version} 并发布`);
        } else {
          ElMessage.warning(`已生成 v${res.version} 草稿，去列表页点【发布】即可生效`);
        }
      } else {
        ElMessage.success(res?.message || "已保存");
      }
    } else {
      await createDefinition({ definition: outDef, steps: outSteps, transitions: outTransitions });
      ElMessage.success("已创建草稿，去列表页点【发布】即可生效");
    }
    router.back();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  // 并行拉下拉数据源
  try { roleList.value = await listRoles(); } catch { roleList.value = []; }
  try { positionList.value = await listPositions(); } catch { positionList.value = []; }
  try { orgTreeData.value = await orgTree(); } catch { orgTreeData.value = []; }
  try {
    const p = await pageUsers({ pageNum: 1, pageSize: 200, status: 1 });
    userList.value = p.records || [];
  } catch { userList.value = []; }

  const id = route.query.id;
  if (!id) return;
  try {
    const data = await getDefinitionDetail(Number(id));
    Object.assign(def, data.definition);
    // 回填使用范围，优先 use_scope_ids，老数据看 dept_id
    const d: any = data.definition as any;
    let ids: number[] = [];
    try {
      const parsed = d.use_scope_ids ? JSON.parse(d.use_scope_ids) : [];
      ids = (parsed || []).map((v: any) => Number(v)).filter((v: number) => !!v);
    } catch {
      ids = [];
    }
    if (!d.use_scope && d.dept_id) {
      def.use_scope = 1;
      ids = [Number(d.dept_id)];
    } else {
      def.use_scope = Number(d.use_scope || 0);
    }
    scopeIds.value = ids;
    // 还原 form_config
    try {
      formFields.value = JSON.parse(data.definition.form_config || "[]");
    } catch {
      formFields.value = [];
    }
    // 还原 steps
    const tsByStep = data.transitionsByStep || {};
    steps.value = data.steps.map((s: any) => {
      let userId = "", roleId = "", positionId = "";
      let orgIdPath: number[] = [];
      try {
        const cfg = JSON.parse(s.approver_config || "{}");
        userId = cfg.userId || "";
        roleId = cfg.roleId || "";
        positionId = cfg.positionId || "";
        if (cfg.orgId) {
          // 从 orgTreeData 里找节点路径
          orgIdPath = findOrgPath(orgTreeData.value, Number(cfg.orgId)) || [];
        }
      } catch {}
      const ts = (tsByStep[s.id] || []).map((t: any) => ({
        target_step_key: String(t.target_step_id),
        condition_expr: t.condition_expr || "",
        condition_name: t.condition_name || "",
        priority: t.priority || 0,
        _default: t.is_default === 1,
      }));
      return {
        ...s,
        _key: String(s.id),
        _userId: String(userId),
        _roleId: String(roleId),
        _positionId: String(positionId),
        _orgIdPath: orgIdPath,
        _transitions: ts,
      };
    });
    // 步骤 id 到 _key 的映射，_key 就是数据库 id
  } catch (e: any) {
    ElMessage.error("加载流程失败");
  }
});

// 在部门树里找节点路径，找不到返回 null
function findOrgPath(tree: OrgNode[], targetId: number, trail: number[] = []): number[] | null {
  for (const node of tree) {
    const path = [...trail, node.id];
    if (node.id === targetId) return path;
    if (node.children && node.children.length) {
      const r = findOrgPath(node.children, targetId, path);
      if (r) return r;
    }
  }
  return null;
}
</script>

<style scoped>
.flow-editor {
  padding: 4px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 左边编辑、右边实时看流程图，步骤区不再铺满整行 */
.editor-body {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.editor-left {
  flex: 1;
  min-width: 0;
}

.editor-right {
  flex: 0 0 320px;
  width: 320px;
  position: sticky;
  top: 12px;
  margin-top: 12px;
}

.chart-card :deep(.el-card__body) {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.chart-hint {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
}

/* 每个审批步骤一张卡片，序号和名称放标题行 */
.step-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px 4px;
  margin-bottom: 10px;
  background: #fcfdff;
}

.step-card.is-active {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.step-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.step-no {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: #1677ff;
  background: #e8f2ff;
  border-radius: 10px;
  padding: 1px 8px;
}

.step-name {
  flex: 0 1 260px;
}

.empty-steps {
  padding: 18px 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

/* 条件区：每个条件一行字段块，窄容器里自动折行，不再用固定列宽的表格 */
.trans-box {
  margin: 10px 0 8px 0;
  border-left: 3px solid #e4e7ed;
  padding-left: 12px;
  min-width: 0;
}
.trans-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.trans-empty {
  font-size: 12px;
  color: #909399;
  background: #fafbfc;
  border: 1px dashed #e4e7ed;
  border-radius: 6px;
  padding: 8px 10px;
  line-height: 1.6;
}
.trans-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  padding: 10px;
  margin-bottom: 8px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}
.trans-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 0 1 190px;
  min-width: 150px;
}
.trans-field.trans-grow {
  flex: 1 1 240px;
}
.trans-label {
  font-size: 12px;
  color: #909399;
  line-height: 1.2;
}
.trans-control {
  width: 100%;
}
.trans-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex: 1 1 100%;
  padding-top: 2px;
  border-top: 1px dashed #ebeef5;
}

/* 顶部说明 */
.editor-help {
  margin-top: 10px;
  border-top: none;
}
.help-title {
  font-size: 13px;
  color: #606266;
}
.help-line {
  font-size: 12px;
  color: #606266;
  line-height: 1.9;
}
.help-line b {
  color: #303133;
}

/* 流程图工具条 */
.chart-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.chart-toolbar-tip {
  font-size: 11px;
  color: #a8abb2;
  text-align: right;
  line-height: 1.4;
}
.tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
.scope-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}
.save-bar {
  text-align: right;
  padding: 16px 0;
}

/* 屏幕窄的时候流程图挪到下面，避免左右都挤成一条 */
@media (max-width: 1280px) {
  .editor-body {
    flex-direction: column;
  }
  .editor-right {
    flex: 1 1 auto;
    width: 100%;
    position: static;
  }
}
</style>
