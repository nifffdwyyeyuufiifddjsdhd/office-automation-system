import request from "@/utils/request";
import type { PageResult } from "./announcement";
export type { PageResult };

// 类型
export interface WorklogItem {
    id: number;
    user_id: number;
    username?: string;
    real_name?: string;
    org_id?: number;
    org_name?: string;
    category_id?: number;
    category_name?: string;
    log_date: string;
    title: string;
    content?: string;
    work_summary?: string;
    next_plan?: string;
    status: number;
    is_supplement: number;
    submit_time?: string;
    view_count: number;
    comment_count: number;
    created_at?: string;
    mine?: boolean;
    // 附件（需求文档 13.3.6）
    attachment_id?: number | null;
    attachment_file_name?: string;
    attachment_file_size?: number;
    attachment_download_url?: string;
}

export interface WorklogComment {
    id: number;
    worklog_id: number;
    user_id: number;
    username?: string;
    real_name?: string;
    avatar?: string;
    parent_id: number;
    content: string;
    comment_time: string;
    deleted?: boolean;
}

export interface WorklogCategory {
    id: number;
    category_name: string;
    category_code?: string;
    description?: string;
    sort_order: number;
    status: number;
}

export interface WorklogGrant {
    id: number;
    target_id: number;
    target_name?: string;
    viewer_id: number;
    viewer_name?: string;
    group_id?: number;
    group_name?: string;
    dept_id?: number;
    dept_name?: string;
    permission_type: number;
    grant_time: string;
    granter_name?: string;
}

export interface WorklogStats {
    myMonthCount: number;
    myMonthDays: number;
    myViewCount: number;
    myCommentCount: number;
    myCompletionRate: number;
    timeDist: Array<{ period: string; count: number }>;
    wordStats: { totalWords: number; avgWords: number };
    commentTop: Array<{ id: number; title: string; comment_count: number }>;
    deptRank: Array<{ user_id: number; real_name: string; count: number }>;
}

// 我的日志
export function myWorklogs(params: { pageNum: number; pageSize: number; logDate?: string; categoryId?: number | null; keyword?: string }) {
    return request.get<any, PageResult<WorklogItem>>("/worklogs/my", { params });
}

export function createWorklog(data: Partial<WorklogItem>) {
    return request.post<any, number>("/worklogs", data);
}

export function updateWorklog(data: Partial<WorklogItem>) {
    return request.put<any, void>("/worklogs", data);
}

export function deleteWorklog(id: number) {
    return request.delete<any, void>(`/worklogs/${id}`);
}

export function myWorklogDetail(id: number) {
    return request.get<any, WorklogItem>(`/worklogs/my/${id}`);
}

// 日志查看
export function viewWorklogs(params: { pageNum: number; pageSize: number; keyword?: string; categoryId?: number | null; orgId?: number | null }) {
    return request.get<any, PageResult<WorklogItem>>("/worklogs/view", { params });
}

export function viewWorklogDetail(id: number) {
    return request.get<any, WorklogItem>(`/worklogs/view/${id}`);
}

export function listComments(worklogId: number) {
    return request.get<any, WorklogComment[]>(`/worklogs/${worklogId}/comments`);
}

export function addComment(worklogId: number, content: string, parentId?: number) {
    return request.post<any, void>(`/worklogs/${worklogId}/comments`, { content, parentId });
}

export function deleteComment(commentId: number) {
    return request.delete<any, void>(`/worklogs/comments/${commentId}`);
}

// 日志管理（管理员）
export function adminWorklogs(params: { pageNum: number; pageSize: number; keyword?: string; status?: number | null; categoryId?: number | null; orgId?: number | null; startDate?: string; endDate?: string }) {
    return request.get<any, PageResult<WorklogItem>>("/worklogs/admin/page", { params });
}

export function recycleWorklogs(params: { pageNum: number; pageSize: number; keyword?: string }) {
    return request.get<any, PageResult<WorklogItem>>("/worklogs/admin/recycle", { params });
}

export function adminDeleteWorklog(id: number) {
    return request.delete<any, void>(`/worklogs/admin/${id}`);
}

export function restoreWorklog(id: number) {
    return request.post<any, void>(`/worklogs/admin/restore/${id}`);
}

// 类别
export function listCategories() {
    return request.get<any, WorklogCategory[]>("/worklogs/categories");
}

export function listAllCategories() {
    return request.get<any, WorklogCategory[]>("/worklogs/categories/all");
}

export function addCategory(data: Partial<WorklogCategory>) {
    return request.post<any, void>("/worklogs/categories", data);
}

export function updateCategory(data: Partial<WorklogCategory>) {
    return request.put<any, void>("/worklogs/categories", data);
}

export function deleteCategory(id: number) {
    return request.delete<any, void>(`/worklogs/categories/${id}`);
}

// 查看权限（管理员）
export function grantPage(params: { pageNum: number; pageSize: number; keyword?: string }) {
    return request.get<any, PageResult<WorklogGrant>>("/worklogs/permissions", { params });
}

export function addGrant(data: Partial<WorklogGrant>) {
    return request.post<any, void>("/worklogs/permissions", data);
}

export function deleteGrant(id: number) {
    return request.delete<any, void>(`/worklogs/permissions/${id}`);
}

// 统计
export function myStats() {
    return request.get<any, WorklogStats>("/worklogs/stats/my");
}

export function deptStats(orgId: number) {
    return request.get<any, WorklogStats>("/worklogs/stats/dept", { params: { orgId } });
}

// 附件（需求文档 13.3.6）
export function uploadWorklogAttachment(file: File) {
    const fd = new FormData();
    fd.append("file", file);
    return request.post<any, { id: number; fileName: string; fileSize: number; fileExt: string }>("/worklogs/attachments/upload", fd);
}

export function worklogAttachmentDownloadUrl(aid: number) {
    return `/api/worklogs/attachments/${aid}/download`;
}

export function downloadWorklogAttachment(aid: number) {
    return request.get<any, Blob>(`/worklogs/attachments/${aid}/download`, { responseType: "blob" });
}

// 导出
export function exportMine(params?: { keyword?: string; categoryId?: number | null; startDate?: string; endDate?: string }) {
    return request.get<any, Blob>("/worklogs/export/mine", { params, responseType: "blob" });
}

export function exportAll(params?: { keyword?: string; categoryId?: number | null; orgId?: number | null; startDate?: string; endDate?: string }) {
    return request.get<any, Blob>("/worklogs/export", { params, responseType: "blob" });
}

// 超 30 天补写授权，需求 13.3.4 第 4、5 条
export function supplementGrant(data: { user_id: number; start_date: string; end_date: string; reason?: string; days?: number }) {
    return request.post("/worklogs/supplement-grant", data);
}
export function supplementList() {
    return request.get("/worklogs/supplement-list");
}
export function supplementMine() {
    return request.get("/worklogs/supplement-mine");
}

export function supplementApply(data: { start_date: string; end_date: string; reason?: string }) {
    return request.post("/worklogs/supplement-apply", data);
}
export function supplementPending() {
    return request.get("/worklogs/supplement-pending");
}
export function supplementApprove(id: number, pass: boolean) {
    return request.post(`/worklogs/supplement-approve/${id}`, { pass });
}
