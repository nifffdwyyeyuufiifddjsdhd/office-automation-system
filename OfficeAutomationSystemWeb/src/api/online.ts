import request from "@/utils/request";

export interface OnlineUserItem {
  userId: number;
  username: string;
  loginTime: string;
  lastActiveTime: string;
  ip: string;
  browser: string;
  os: string;
  device: string;
  deptId: number | null;
  // 基础名单里后端会补部门名，明细里没有
  orgName?: string | null;
}

export function getOnlineCount() {
  return request.get<any, { count: number }>("/online/count");
}

export function getOnlineList(params?: { deptId?: number; keyword?: string }) {
  return request.get<any, OnlineUserItem[]>("/online/list", { params });
}

// 员工可看的基础名单：姓名、部门、时间，没有 IP 和设备
export function getOnlineDirectory(params?: { deptId?: number; keyword?: string }) {
  return request.get<any, OnlineUserItem[]>("/online/directory", { params });
}

export interface OnlineOperationItem {
  module?: string | null;
  action?: string | null;
  method?: string | null;
  url?: string | null;
  status?: number | null;
  ip?: string | null;
  createdAt?: string | null;
}

export interface OnlineLoginItem {
  loginTime?: string | null;
  ip?: string | null;
  result?: number | null;
  failReason?: string | null;
}

export interface OnlineUserDetail {
  userId: number;
  username: string;
  realName?: string | null;
  orgName?: string | null;
  phone?: string | null;
  email?: string | null;
  status?: number | null;
  online?: boolean;
  lastLoginTime?: string | null;
  lastLoginIp?: string | null;
  lastLogoutTime?: string | null;
  ip?: string | null;
  browser?: string | null;
  os?: string | null;
  device?: string | null;
  recentOperations?: OnlineOperationItem[];
  recentLogins?: OnlineLoginItem[];
}

// 在线用户详情：资料 + 最近 10 条操作记录 + 最近 5 次登录
export function getOnlineDetail(userId: number) {
  return request.get<any, OnlineUserDetail>("/online/" + userId);
}
