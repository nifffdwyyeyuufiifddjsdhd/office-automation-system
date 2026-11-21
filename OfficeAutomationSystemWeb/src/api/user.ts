import request from "@/utils/request";
import type { PageResult } from "./announcement";

export interface UserItem {
    id: number;
    username: string;
    real_name: string;
    gender: number;
    phone?: string;
    email?: string;
    org_id?: number;
    position_id?: number;
    title_id?: number;
    entry_date?: string;
    status: number;
    last_login_time?: string;
    lock_time?: string;
    created_at?: string;
    roleIds?: number[];
    roleNames?: string[];
    password?: string; // 仅新增/重置密码时使用
    emergency_contact?: string; // 紧急联系人
    emergency_phone?: string; // 紧急联系人电话
    bank_account?: string; // 银行卡号
    bank_name?: string; // 开户行
    salary?: number | null; // 月薪，无薪资查看权限时后端返回 null
}

export interface RoleOption {
    id: number;
    role_name: string;
    role_code: string;
}

export interface OrgNode {
    id: number;
    label: string;
    children: OrgNode[];
}

export function pageUsers(params: { pageNum: number; pageSize: number; keyword?: string; status?: number | null; orgId?: number | null }) {
    return request.get<any, PageResult<UserItem>>("/users/page", { params });
}

// 登录就能用的精简用户选项，任务指派、评论@用
export function listUserOptions() {
    return request.get<any, UserItem[]>("/users/options");
}

export function orgTree() {
    return request.get<any, OrgNode[]>("/users/org-tree");
}

// 只读部门树，登录即可用，按部门筛选时用
export function orgTreeEnabled() {
    return request.get<any, OrgNode[]>("/orgs/tree-enabled");
}

export interface ImportResult {
    success: number;
    errors: string[];
}

// 批量导入用户（Excel）
export function importUsers(file: File) {
    const formData = new FormData();
    formData.append("file", file);
    // 不手动设置 Content-Type，由浏览器自动生成带 boundary 的 multipart 头
    return request.post<any, ImportResult>("/users/import", formData);
}

// 导出用户列表（.xlsx），返回 Blob 由浏览器下载
export function exportUsers(params: { keyword?: string; status?: number | null; orgId?: number | null }) {
    return request.get<any, Blob>("/users/export", { params, responseType: "blob" });
}


// 组织机构维护
export interface OrgForm {
  id?: number;
  parent_id?: number | null;
  org_name: string;
  org_code?: string;
  org_type?: number;
  sort_order?: number;
  status?: number;
}

// 部门管理树（含停用节点）
export function orgTreeAdmin() {
  return request.get<any, OrgNode[]>("/orgs/tree");
}

export function createOrg(data: OrgForm) {
  return request.post<any, number>("/orgs", data);
}

export function updateOrg(data: OrgForm) {
  return request.put<any, void>("/orgs", data);
}

export function deleteOrg(id: number) {
  return request.delete<any, void>(`/orgs/${id}`);
}


// 职位管理
export interface PositionItem {
  id?: number;
  position_name: string;
  position_code?: string;
  level?: number;
  sort_order?: number;
  status?: number;
  remark?: string;
}

export function listPositions() {
  return request.get<any, PositionItem[]>("/positions");
}
export function createPosition(data: PositionItem) {
  return request.post<any, number>("/positions", data);
}
export function updatePosition(data: PositionItem) {
  return request.put<any, void>("/positions", data);
}
export function deletePosition(id: number) {
  return request.delete<any, void>(`/positions/${id}`);
}

// 职称管理
export interface TitleItem {
  id?: number;
  title_name: string;
  code?: string;
  title_level?: number;
  sort_order?: number;
  status?: number;
  remark?: string;
}

export function listTitles() {
  return request.get<any, TitleItem[]>("/titles");
}
export function createTitle(data: TitleItem) {
  return request.post<any, number>("/titles", data);
}
export function updateTitle(data: TitleItem) {
  return request.put<any, void>("/titles", data);
}
export function deleteTitle(id: number) {
  return request.delete<any, void>(`/titles/${id}`);
}


// 用户群组
export interface GroupItem {
  id?: number;
  group_name: string;
  group_code?: string;
  description?: string;
  status?: number;
  memberCount?: number;
  created_at?: string;
}

export interface GroupMemberItem {
  userId: number;
  realName: string;
  username: string;
  orgName: string;
  joinTime: string;
}

export function listGroups() {
  return request.get<any, GroupItem[]>("/groups");
}
export function createGroup(data: GroupItem) {
  return request.post<any, number>("/groups", data);
}
export function updateGroup(data: GroupItem) {
  return request.put<any, void>("/groups", data);
}
export function deleteGroup(id: number) {
  return request.delete<any, void>(`/groups/${id}`);
}
export function listGroupMembers(id: number) {
  return request.get<any, GroupMemberItem[]>(`/groups/${id}/members`);
}
export function addGroupMembers(id: number, userIds: number[]) {
  return request.post<any, void>(`/groups/${id}/members`, { userIds });
}
export function removeGroupMember(id: number, userId: number) {
  return request.delete<any, void>(`/groups/${id}/members/${userId}`);
}

// 退出登录：通知后端记录退出时间
export function logout() {
    return request.post<any, void>("/auth/logout");
}

export function getUserDetail(id: number) {
    return request.get<any, UserItem>(`/users/${id}`);
}

export function listRoles() {
    return request.get<any, RoleOption[]>("/users/roles");
}

export function addUser(data: Partial<UserItem>) {
    return request.post<any, number>("/users", data);
}

export function updateUser(data: Partial<UserItem>) {
    return request.put<any, void>("/users", data);
}

export function deleteUser(id: number) {
    return request.delete<any, void>(`/users/${id}`);
}

export function resetUserPassword(id: number, newPassword: string) {
    return request.put<any, void>(`/users/${id}/reset-password`, { newPassword });
}

export function changeUserStatus(id: number, status: number) {
    return request.put<any, void>(`/users/${id}/status`, null, { params: { status } });
}

export function unlockUser(id: number) {
    return request.post<any, void>(`/users/unlock/${id}`);
}

// 入职办理 / 离职交接
export interface OnboardResult {
    id: number;
    username: string;
    realName: string;
    initPassword: string;
}

// 入职开通账号：初始密码留空由后端生成
export function onboardUser(data: Partial<UserItem>) {
    return request.post<any, OnboardResult>("/users/onboard", data);
}

export interface HandoverFlowBrief {
    id: number;
    instanceNo: string;
    title: string;
    currentStepName?: string;
    submitTime?: string;
}

// 交接清单里的任务摘要，只给交接判断够用的几列
export interface HandoverTaskBrief {
    id: number;
    taskNo: string;
    title: string;
    status: number;
    projectId?: number | null;
}

export interface HandoverPreview {
    userId: number;
    username: string;
    realName: string;
    status: number;
    orgName?: string;
    todoCount: number;
    runningCount: number;
    doneCount: number;
    taskCount: number;
    projectCount: number;
    todoList: HandoverFlowBrief[];
    runningList: HandoverFlowBrief[];
    taskList: HandoverTaskBrief[];
    // 选“自动转直属上级”时会交给谁，没找到主管时为 null
    autoReceiverId?: number | null;
    autoReceiverName?: string | null;
}

// 离职交接前先看清要转多少流程
export function handoverPreview(id: number) {
    return request.get<any, HandoverPreview>(`/users/${id}/handover-preview`);
}

// 处理方式：transfer 手动指定接收人 / auto 自动转直属上级 / keep 只停用账号保留待办
export type HandoverMode = "transfer" | "auto" | "keep";

export interface HandoverPayload {
    mode?: HandoverMode;
    receiverId?: number | null;
    reason?: string;
    effectiveDate?: string;
    transferRunning?: boolean;
}

export interface HandoverResult {
    handoverId: number;
    handleMode?: HandoverMode;
    todoCount: number;
    runningCount: number;
    doneCount: number;
    taskCount: number;
    projectCount: number;
    pendingTodo?: number;
    pendingTask?: number;
    receiverName: string | null;
    itemSummary: string;
}

export function userHandover(id: number, data: HandoverPayload) {
    return request.post<any, HandoverResult>(`/users/${id}/handover`, data);
}

export interface HandoverRecord {
    id: number;
    user_id: number;
    receiver_id: number | null;
    handle_mode?: HandoverMode;
    operator_id: number;
    reason?: string;
    effective_date?: string;
    todo_count: number;
    running_count: number;
    task_count: number;
    project_count: number;
    done_count: number;
    item_summary?: string;
    user_name?: string;
    receiver_name?: string | null;
    operator_name?: string;
    org_name?: string;
    created_at?: string;
}

export function pageHandovers(params: { pageNum: number; pageSize: number; leaverId?: number | null }) {
    return request.get<any, PageResult<HandoverRecord>>("/users/handovers", { params });
}


// 用户级附加权限：任务中心访问 task:access、发放任务权限 task:grant
export interface ExtraPermissionItem {
    menuId: number;
    menuName: string;
    menuCode: string;
    source: number; // 1 管理员授予 2 派发任务自动开通
    sourceText: string;
    reason: string;
    grantedBy: number | null;
    grantedByName: string;
    createdAt: string;
}

export interface ExtraPermissionInfo {
    items: ExtraPermissionItem[];
    taskAccess: boolean;
    canGrantPermission: boolean;
}

export function getUserExtraPermissions(id: number) {
    return request.get<any, ExtraPermissionInfo>(`/users/${id}/extra-permissions`);
}

export function grantUserExtraPermission(id: number, menuCode: string, reason?: string) {
    return request.post<any, { menuId: number; menuCode: string; created: boolean }>(`/users/${id}/extra-permissions`, { menu_code: menuCode, reason });
}

export function revokeUserExtraPermission(id: number, menuId: number) {
    return request.delete<any, boolean>(`/users/${id}/extra-permissions/${menuId}`);
}
// 个人中心
export interface ProfileItem {
    id: number;
    username: string;
    real_name: string;
    gender: number;
    phone?: string;
    email?: string;
    avatar?: string;
    org_id?: number;
    org_name?: string;
    position_id?: number;
    position_name?: string;
    title_id?: number;
    title_name?: string;
    entry_date?: string;
    status?: number;
    last_login_time?: string;
    roleNames?: string[];
}

export function getMyProfile() {
    return request.get<any, ProfileItem>("/users/me");
}

export function updateMyProfile(data: Partial<ProfileItem>) {
    return request.put<any, void>("/users/me", data);
}

// 头像上传（multipart）
export function uploadAvatar(file: File) {
    const form = new FormData();
    form.append("file", file);
    return request.post<any, { avatar: string }>("/users/me/avatar", form, {
        headers: { "Content-Type": "multipart/form-data" },
    });
}

// 拉取头像转成 objectURL，没头像直接返回 null，不发请求
export async function fetchAvatarUrl(avatarPath?: string | null): Promise<string | null> {
    if (!avatarPath) return null;
    try {
        const blob = await request.get<any, Blob>("/users/me/avatar", { responseType: "blob" });
        if (!blob || blob.size === 0) return null;
        return URL.createObjectURL(blob);
    } catch {
        return null;
    }
}

// 个人中心：企业微信 / 钉钉绑定
export interface ThirdPartyBindingItem {
    platform: "wecom" | "dingtalk";
    platformName: string;
    bound: boolean;
    externalId?: string | null;
    externalName?: string | null;
    bindTime?: string | null;
}

export function getMyBindings() {
    return request.get<any, ThirdPartyBindingItem[]>("/profile/bindings");
}

export function bindThirdParty(data: { platform: string; externalId: string; externalName?: string }) {
    return request.post<any, ThirdPartyBindingItem>("/profile/bindings", data);
}

export function unbindThirdParty(platform: string) {
    return request.delete<any, void>("/profile/bindings/" + platform);
}
