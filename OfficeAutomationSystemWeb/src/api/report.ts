import request from "@/utils/request";

export interface ReportQuery {
  startDate?: string;
  endDate?: string;
  deptId?: number | null;
  userId?: number | null;
  projectId?: number | null;
}

export interface NameValue {
  name: string;
  value: number;
}

export interface OptionItem {
  id: number;
  name: string;
  parent_id?: number;
  org_id?: number;
}

export interface ReportMeta {
  depts: OptionItem[];
  projects: OptionItem[];
  users: OptionItem[];
  canFilterUser: boolean;
  canFilterDept: boolean;
}

// 综合报表总览（四大板块聚合）
export const getReportOverview = (params: ReportQuery) =>
  request.get<any, any>("/report/overview", { params });

// 筛选下拉数据
export const getReportMeta = () =>
  request.get<any, ReportMeta>("/report/meta");

// 板块导出 Excel：section = task / attendance / worklog / flow
export const exportReport = (section: string, params: ReportQuery) =>
  request.get<any, Blob>(`/report/export/${section}`, {
    params,
    responseType: "blob",
  });
