import request from "@/utils/request";

export interface NotificationItem {
  id: number;
  title: string;
  content: string;
  bizType: string;
  bizId: number;
  jumpUrl: string;
  isRead: number;
  isTop: boolean;
  createdAt: string;
  count: number;
}

export function getUnreadCount() {
  return request.get<any, { count: number }>("/notifications/unread-count");
}

export function getNotificationList() {
  return request.get<any, NotificationItem[]>("/notifications/list");
}

export function markRead(id: number) {
  return request.post<any, void>(`/notifications/${id}/read`);
}

export function markAllRead() {
  return request.post<any, void>("/notifications/read-all");
}

export function toggleTop(id: number, top: boolean) {
  return request.post<any, void>(`/notifications/${id}/top`, { top: top ? 1 : 0 });
}

// 个人中心：系统通知全量列表
export interface NotificationPageItem {
  id: number;
  title: string;
  content: string;
  type: number;
  bizType: string;
  bizId: number;
  jumpUrl: string;
  isRead: number;
  isTop: boolean;
  createdAt: string;
  readTime?: string | null;
}

export interface NotificationPageResult {
  records: NotificationPageItem[];
  total: number;
  current: number;
  size: number;
}

export function pageNotifications(params: {
  pageNum: number;
  pageSize: number;
  isRead?: number | null;
  type?: number | null;
  keyword?: string;
}) {
  return request.get<any, NotificationPageResult>("/notifications/page", { params });
}

export function deleteNotification(id: number) {
  return request.delete<any, void>(`/notifications/${id}`);
}

export function deleteReadNotifications() {
  return request.delete<any, void>("/notifications/read");
}

// 通知偏好
export interface NotificationPreferences {
  system: number;
  flow: number;
  task: number;
  attendance: number;
  worklog: number;
  sound: number;
}

export function getNotificationPreferences() {
  return request.get<any, NotificationPreferences>("/notifications/preferences");
}

export function saveNotificationPreferences(data: NotificationPreferences) {
  return request.put<any, void>("/notifications/preferences", data);
}
