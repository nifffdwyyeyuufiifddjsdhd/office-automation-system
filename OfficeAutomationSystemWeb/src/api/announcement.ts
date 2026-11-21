import request from "@/utils/request";

// 公告
export interface AnnouncementItem {
    id: number;
    title: string;
    category_id: number;
    category_name?: string;
    summary?: string;
    cover_image?: string;
    theme?: string;          // 正文主题 standard/notice/academic/finance
    content?: string;
    author_id?: number;
    author_name?: string;
    status: number;          // 0草稿 1已发布 2已下线
    is_top: number;          // 0否 1是
    publish_time?: string;
    view_count?: number;
    read_count?: number;
    is_read?: boolean;
    allow_comment?: number;
}

export interface PageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
}

export function pageAnnouncements(params: {
    pageNum: number;
    pageSize: number;
    categoryId?: number | null;
    keyword?: string;
    status?: number | null;
}) {
    return request.get<any, PageResult<AnnouncementItem>>("/announcements/page", { params });
}

export function getAnnouncementDetail(id: number) {
    return request.get<any, AnnouncementItem>(`/announcements/${id}`);
}

export function addAnnouncement(data: Partial<AnnouncementItem>) {
    return request.post<any, number>("/announcements", data);
}

export function updateAnnouncement(data: Partial<AnnouncementItem>) {
    return request.put<any, void>("/announcements", data);
}

export function deleteAnnouncement(id: number) {
    return request.delete<any, void>(`/announcements/${id}`);
}

export function publishAnnouncement(id: number) {
    return request.put<any, void>(`/announcements/${id}/publish`);
}

export function offlineAnnouncement(id: number) {
    return request.put<any, void>(`/announcements/${id}/offline`);
}

export function batchPublishAnnouncements(ids: number[]) {
    return request.post<any, void>("/announcements/batch-publish", ids);
}

export function toggleTop(id: number, isTop: number) {
    return request.put<any, void>(`/announcements/${id}/top`, null, { params: { isTop } });
}

// 正文插图上传，返回能直接写进正文的地址
export function uploadAnnouncementImage(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, { url: string; path: string; fileName: string }>('/announcements/upload-image', formData)
}

// 类别
export interface AnnouncementCategory {
    id: number;
    parent_id: number;
    category_name: string;
    category_code?: string;
    sort_order?: number;
    status: number;
    view_permission_type: number;  // 1全部可见 2指定角色可见
    icon?: string;
    role_ids?: number[];
    children?: AnnouncementCategory[];
}

export function getCategoryTree() {
    return request.get<any, AnnouncementCategory[]>("/announcements/categories/tree");
}

export function addCategory(data: Partial<AnnouncementCategory>) {
    return request.post<any, number>("/announcements/categories", data);
}

export function updateCategory(data: Partial<AnnouncementCategory> & { id: number }) {
    return request.put<any, void>("/announcements/categories", data);
}

export function deleteCategory(id: number) {
    return request.delete<any, void>(`/announcements/categories/${id}`);
}

export function toggleCategoryStatus(id: number, status: number) {
    return request.put<any, void>(`/announcements/categories/${id}/status`, null, { params: { status } });
}

// 公开公告，登录页免登录可看（announcement:view-public）
export interface PublicAnnouncementBrief {
    id: number;
    title: string;
    summary?: string;
    coverImage?: string;
    categoryName?: string;
    isTop?: number;
    publishTime?: string;
}

export interface PublicAnnouncementDetail {
    id: number;
    title: string;
    content?: string;
    theme?: string;
    summary?: string;
    coverImage?: string;
    categoryName?: string;
    publishTime?: string;
    viewCount?: number;
}

// 已发布公告列表，不需要登录态
export function getPublicAnnouncements(limit = 10) {
    return request.get<any, PublicAnnouncementBrief[]>("/public/announcements", { params: { limit } });
}

// 公开公告详情，看一次浏览量加一
export function getPublicAnnouncement(id: number) {
    return request.get<any, PublicAnnouncementDetail>("/public/announcements/" + id);
}
