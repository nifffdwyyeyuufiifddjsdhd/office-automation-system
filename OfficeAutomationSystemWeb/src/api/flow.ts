import request from "@/utils/request";

// 类型定义
export interface FlowDefinitionItem {
  id?: number;
  flow_name: string;
  code: string;
  flow_type?: number;
  need_payment?: number; // 1=审批通过后进入财务待付款池
  description?: string;
  icon?: string;
  form_config?: string; // JSON 字符串：表单字段数组
  version?: number;
  status?: number;
  statusText?: string;
  audit_status?: number;
  auditStatusText?: string;
  dept_id?: number | null;
  use_scope?: number; // 使用范围 0全员 1指定部门 2指定角色
  use_scope_ids?: string; // 使用范围的部门/角色 id，JSON 数组
  creator_id?: number;
  creatorName?: string;
  sort_order?: number;
  stepCount?: number;
  publish_time?: string;
  created_at?: string;
  // 被哪个新版本替代（status=3 时有效）
  supersededBy?: number;
}

// 保存结果：改已发布版本会生成新版本，这里要区分
export interface FlowDefinitionSaveResult {
  mode?: "new-version" | "in-place";
  definitionId?: number;
  version?: number;
  oldVersionId?: number;
  instanceCount?: number;
  published?: boolean;
  message?: string;
}

export interface FlowStepItem {
  id?: number;
  _key?: string; // 前端临时 key，用于保存时映射 stepId
  step_name: string;
  step_code?: string;
  step_type?: number; // 1审批 2抄送 3网关 4开始 5结束
  approver_type?: number; // 1指定用户 2指定角色 3部门主管 4职位匹配 5发起人自选
  // 存成 JSON 字符串
  step_form?: string;
  timeout_hours?: number;
  timeout_action?: number;
  sort_order?: number;
  sign_type?: number;
  sign_ratio?: number;
}

export interface FlowTransitionItem {
  step_key: string;
  target_step_key: string;
  condition_expr?: string;
  condition_name?: string;
  priority?: number;
  is_default?: number;
  transition_type?: number;
}

export interface FlowInstanceRow {
  id: number;
  instanceNo: string;
  title: string;
  status: number;
  statusText: string;
  currentStepName: string;
  urgency: number;
  submitTime: string;
  finishTime?: string;
  // 流转到当前节点的时间，待办按它倒序
  arrivedTime?: string;
  // 是否还没点开过，列表显示红点
  unread?: boolean;
  creatorName: string;
  auditorName: string;
  flowName: string;
  myAction?: number;
  myActionText?: string;
}

export interface TransferCandidate {
  id: number;
  username: string;
  realName: string;
  orgName?: string;
}

// 流程定义
export function listDefinitions() {
  return request.get<any, FlowDefinitionItem[]>("/flow/definitions");
}
export function publishedDefinitions() {
  return request.get<any, FlowDefinitionItem[]>("/flow/definitions/published");
}
export function getDefinitionDetail(id: number) {
  return request.get<any, {
    definition: FlowDefinitionItem;
    steps: FlowStepItem[];
    transitionsByStep: Record<string, FlowTransitionItem[]>;
  }>(`/flow/definitions/${id}`);
}
export function createDefinition(data: { definition: FlowDefinitionItem; steps: FlowStepItem[]; transitions: FlowTransitionItem[] }) {
  return request.post<any, number>("/flow/definitions", data);
}
export function updateDefinition(id: number, data: { definition: FlowDefinitionItem; steps: FlowStepItem[]; transitions: FlowTransitionItem[] }) {
  return request.put<any, FlowDefinitionSaveResult>(`/flow/definitions/${id}`, data);
}
export function publishDefinition(id: number) {
  return request.post<any, void>(`/flow/definitions/${id}/publish`);
}
export function disableDefinition(id: number) {
  return request.post<any, void>(`/flow/definitions/${id}/disable`);
}
export function deleteDefinition(id: number) {
  return request.delete<any, void>(`/flow/definitions/${id}`);
}
export function auditDefinition(id: number, pass: number) {
  return request.post<any, void>(`/flow/definitions/${id}/audit`, { pass });
}

// 流程实例
export function startInstance(data: {
  definitionId: number;
  title?: string;
  businessData: Record<string, any>;
  urgency?: number;
  selectedApproverId?: number;
}) {
  return request.post<any, number>("/flow/instances/start", data);
}
export function todoInstances() {
  return request.get<any, FlowInstanceRow[]>("/flow/instances/todo");
}
export function doneInstances() {
  return request.get<any, FlowInstanceRow[]>("/flow/instances/done");
}
export function mineInstances() {
  return request.get<any, FlowInstanceRow[]>("/flow/instances/mine");
}
export function allInstances() {
  return request.get<any, FlowInstanceRow[]>("/flow/instances/all");
}
export function transferCandidates(instanceId: number) {
  return request.get<any, TransferCandidate[]>(`/flow/instances/${instanceId}/transfer-candidates`);
}
export function getInstanceDetail(id: number) {
  return request.get<any, FlowInstanceRow & {
    definition: FlowDefinitionItem;
    businessData: Record<string, any>;
    track: { approverName: string; action: number; actionText: string; comment: string; time: string; targetName?: string }[];
    attachments?: { id: number; fileName: string; fileSize: number; fileExt: string; downloadUrl?: string }[];
    // 付款状态：0 待付款/无需付款、1 已付款、2 已作废
    payStatus?: number;
    payStatusText?: string;
    payNo?: string;
    payAmount?: number;
    payTime?: string;
    payeeName?: string;
    categoryName?: string;
    voidReason?: string;
    // 会签进度：要几人签、已签了谁、还需不需要我签
    countersign?: boolean;
    signType?: number;
    signApproved?: number;
    signRequired?: number;
    signApprovedNames?: string[];
    signPendingNames?: string[];
    signSignedByMe?: boolean;
  }>(`/flow/instances/${id}`);
}
export function approveInstance(id: number, comment: string) {
  return request.post<any, void>(`/flow/instances/${id}/approve`, { comment });
}
export function rejectInstance(id: number, comment: string) {
  return request.post<any, void>(`/flow/instances/${id}/reject`, { comment });
}
export function transferInstance(id: number, targetUserId: number, comment: string) {
  return request.post<any, void>(`/flow/instances/${id}/transfer`, { targetUserId, comment });
}
export function withdrawInstance(id: number) {
  return request.post<any, void>(`/flow/instances/${id}/withdraw`);
}
export function urgeInstance(id: number) {
  return request.post<any, void>(`/flow/instances/${id}/urge`);
}

// 断点续审、批量审批、可退回步骤
export function rejectInstanceV2(id: number, comment: string, targetStepId?: number | null) {
  return request.post<any, void>(`/flow/instances/${id}/reject`, { comment, targetStepId });
}
export function returnableSteps(id: number) {
  return request.get<any, { stepId: number | null; stepName: string }[]>(`/flow/instances/${id}/returnable-steps`);
}
export function resubmitInstance(id: number, businessData: Record<string, any>) {
  return request.post<any, void>(`/flow/instances/${id}/resubmit`, { businessData });
}
export function batchApproveInstances(ids: number[], comment?: string) {
  return request.post<any, { success: number; skipped: string[] }>(`/flow/instances/batch-approve`, { ids, comment });
}

// 删除、待办数、筛选、统计、附件
export function deleteInstance(id: number) {
  return request.delete<any, void>(`/flow/instances/${id}`);
}
export function todoCount() {
  return request.get<any, number>(`/flow/instances/todo-count`);
}
export function todoInstancesV2(params?: { definitionId?: number; urgency?: number; keyword?: string }) {
  return request.get<any, FlowInstanceRow[]>(`/flow/instances/todo`, { params });
}
export function mineInstancesV2(params?: { status?: number; keyword?: string }) {
  return request.get<any, FlowInstanceRow[]>(`/flow/instances/mine`, { params });
}
export function allInstancesV2(params?: {
  definitionId?: number; status?: number; creatorId?: number; keyword?: string;
  startTime?: string; endTime?: string; page?: number; size?: number;
}) {
  return request.get<any, { records: FlowInstanceRow[]; total: number; current: number; size: number }>(
    `/flow/instances/all`, { params }
  );
}
export function instanceStatistics(definitionId: number, startTime?: string, endTime?: string) {
  return request.get<any, {
    total: number;
    fields: Record<string, { label: string; field: string; sum: number }>;
  }>(`/flow/instances/statistics`, { params: { definitionId, startTime, endTime } });
}
export function uploadAttachment(file: File) {
  const form = new FormData();
  form.append("file", file);
  return request.post<any, { id: number; fileName: string; fileSize: number; suggestAmount?: number | null; amountSource?: string | null }>(
    `/flow/instances/attachments/upload`, form,
    { headers: { "Content-Type": "multipart/form-data" } }
  );
}
export function listAttachments(id: number) {
  return request.get<any, { id: number; fileName: string; fileSize: number; fileExt: string; downloadUrl: string }[]>(
    `/flow/instances/${id}/attachments`
  );
}

// 审批委托：这段时间的待办由代理人代批
export interface FlowDelegateItem {
  id: number;
  delegatorId: number;
  agentId: number;
  delegatorName: string;
  agentName: string;
  delegatorOrg?: string;
  agentOrg?: string;
  startTime: string;
  endTime: string;
  reason?: string;
  status: number;
  active: boolean;
  statusText: string;
  mine: boolean;
}

export interface FlowDelegateList {
  mine: FlowDelegateItem[];
  toMe: FlowDelegateItem[];
}

export interface FlowDelegatePayload {
  agentId: number;
  startTime: string;
  endTime: string;
  reason?: string;
}

export function listFlowDelegates() {
  return request.get<any, FlowDelegateList>("/flow/delegates");
}
export function flowDelegateAgentOptions() {
  return request.get<any, { id: number; username: string; realName: string; orgName?: string }[]>(
    "/flow/delegates/agent-options"
  );
}
export function createFlowDelegate(data: FlowDelegatePayload) {
  return request.post<any, { id: number; startTime: string; endTime: string }>("/flow/delegates", data);
}
export function cancelFlowDelegate(id: number) {
  return request.delete<any, void>(`/flow/delegates/${id}`);
}
