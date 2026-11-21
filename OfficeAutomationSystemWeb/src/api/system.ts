import request from "@/utils/request";
import type { PageResult } from "./announcement";

export interface SystemParamItem {
    id: number;
    paramKey: string;
    paramValue?: string;
    paramName?: string;
    paramGroup?: string;
    paramType?: number;
    sortOrder?: number;
    status: number;
    remark?: string;
    updatedAt?: string;
}

export interface MenuItemRow {
    id: number;
    parentId: number;
    parentName?: string;
    menuName: string;
    menuCode?: string;
    menuType: number;
    path?: string;
    component?: string;
    icon?: string;
    isNav: number;
    sortOrder?: number;
    status: number;
    visible: number;
}

export interface NavImageItem {
    id: number;
    title?: string;
    imageUrl: string;
    linkUrl?: string;
    position?: string;
    sortOrder?: number;
    status: number;
    startTime?: string;
    endTime?: string;
    clickCount?: number;
    remark?: string;
}

export interface OperationLogItem {
    id: number;
    username?: string;
    module?: string;
    action?: string;
    method?: string;
    url?: string;
    ipAddress?: string;
    targetType?: string;
    targetId?: number;
    requestParams?: string;
    status?: number;
    errorMsg?: string;
    costTime?: number;
    createdAt?: string;
}

export interface ExceptionLogItem {
    id: number;
    username?: string;
    url?: string;
    method?: string;
    ipAddress?: string;
    browser?: string;
    os?: string;
    exceptionType?: string;
    errorMsg?: string;
    stackTrace?: string;
    requestParams?: string;
    status?: number;
    resolved?: number;
    resolvedTime?: string;
    resolveNote?: string;
    createdAt?: string;
}

export interface NotificationTemplateItem {
    id: number;
    templateCode: string;
    templateName: string;
    bizType?: string;
    titleTemplate: string;
    contentTemplate?: string;
    channels?: string;
    status: number;
    remark?: string;
    updatedAt?: string;
}

export interface PushLogItem {
    id: number;
    userId?: number;
    userName?: string;
    title?: string;
    bizType?: string;
    bizId?: number;
    isRead?: number;
    pushStatus?: number;
    pushStatusText?: string;
    pushTime?: string;
    pushChannel?: string;
    pushFailReason?: string;
    createdAt?: string;
}

// 系统参数
export function listSystemParams(params: { group?: string; keyword?: string }) {
    return request.get<any, SystemParamItem[]>("/system/params", { params });
}
export function createSystemParam(data: Partial<SystemParamItem>) {
    return request.post<any, { id: number }>("/system/params", data);
}
export function updateSystemParam(id: number, data: Partial<SystemParamItem>) {
    return request.put<any, null>("/system/params/" + id, data);
}
export function deleteSystemParam(id: number) {
    return request.delete<any, null>("/system/params/" + id);
}

// 菜单与权限码
export function listMenus(params: { keyword?: string }) {
    return request.get<any, MenuItemRow[]>("/system/menus", { params });
}
export function createMenu(data: Partial<MenuItemRow>) {
    return request.post<any, { id: number }>("/system/menus", data);
}
export function updateMenu(id: number, data: Partial<MenuItemRow>) {
    return request.put<any, null>("/system/menus/" + id, data);
}
export function deleteMenu(id: number) {
    return request.delete<any, null>("/system/menus/" + id);
}

// 导航图片
export function listNavImages(params: { position?: string }) {
    return request.get<any, NavImageItem[]>("/system/nav-images", { params });
}
export function createNavImage(data: Partial<NavImageItem>) {
    return request.post<any, { id: number }>("/system/nav-images", data);
}
export function updateNavImage(id: number, data: Partial<NavImageItem>) {
    return request.put<any, null>("/system/nav-images/" + id, data);
}
export function deleteNavImage(id: number) {
    return request.delete<any, null>("/system/nav-images/" + id);
}

// 操作日志
export function pageOperationLogs(params: {
    pageNum: number;
    pageSize: number;
    keyword?: string;
    module?: string;
    status?: number | null;
    startDate?: string;
    endDate?: string;
}) {
    return request.get<any, PageResult<OperationLogItem>>("/system/operation-logs", { params });
}

// 异常日志
export function pageExceptionLogs(params: {
    pageNum: number;
    pageSize: number;
    keyword?: string;
    resolved?: number | null;
    startDate?: string;
    endDate?: string;
}) {
    return request.get<any, PageResult<ExceptionLogItem>>("/system/exception-logs", { params });
}
export function getExceptionLog(id: number) {
    return request.get<any, ExceptionLogItem>("/system/exception-logs/" + id);
}
export function createExceptionLog(data: Partial<ExceptionLogItem>) {
    return request.post<any, { id: number }>("/system/exception-logs", data);
}
export function resolveExceptionLog(id: number, resolveNote: string) {
    return request.put<any, null>("/system/exception-logs/" + id + "/resolve", { resolveNote });
}

// 通知模板与推送记录
export function listNotificationTemplates(params: { keyword?: string; bizType?: string }) {
    return request.get<any, NotificationTemplateItem[]>("/notifications/templates", { params });
}
export function createNotificationTemplate(data: Partial<NotificationTemplateItem>) {
    return request.post<any, { id: number }>("/notifications/templates", data);
}
export function updateNotificationTemplate(id: number, data: Partial<NotificationTemplateItem>) {
    return request.put<any, null>("/notifications/templates/" + id, data);
}
export function deleteNotificationTemplate(id: number) {
    return request.delete<any, null>("/notifications/templates/" + id);
}
export function pagePushLogs(params: {
    pageNum: number;
    pageSize: number;
    keyword?: string;
    bizType?: string;
    pushStatus?: number | null;
    startDate?: string;
    endDate?: string;
}) {
    return request.get<any, PageResult<PushLogItem>>("/notifications/logs", { params });
}
export function resendPushLog(id: number) {
    return request.post<any, null>("/notifications/logs/" + id + "/resend");
}
