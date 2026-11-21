import request from "@/utils/request";

// 当天打卡
export interface TodayStatus {
  canCheckIn: boolean;
  canCheckOut: boolean;
  isHoliday: boolean;
  workStart: string;
  workEnd: string;
  checkInTime: string | null;
  checkOutTime: string | null;
  status: number | null;
  workHours: number | string | null;
  holidayName?: string | null;
  shiftName?: string | null;
  [key: string]: any;
}

export interface AttendanceRecord {
  id: number;
  user_id?: number;
  user_name?: string;
  dept_name?: string;
  attendance_date: string;
  check_in_time: string | null;
  check_out_time: string | null;
  work_hours: number | string | null;
  // 0 正常 1 迟到 2 早退 3 迟到且早退
  is_makeup: number;
  makeup_apply_id: number | null;
  late_minutes?: number;
  early_minutes?: number;
  remark?: string | null;
  [key: string]: any;
}

export interface MakeupApply {
  id?: number;
  user_id?: number;
  user_name?: string;
  dept_name?: string;
  attendance_date: string;
  type: number;                 // 1 补签到 2 补签退 3 补签到+签退
  reason: string;
  check_in_time?: string | null;
  check_out_time?: string | null;
  status?: number;              // 0 待审批 1 已通过 2 已驳回 3 已撤回
  current_stage?: number;       // 1 主管初审 2 人事终审
  approve_remark?: string | null;
  first_approve_remark?: string | null;
  apply_time?: string;
  [key: string]: any;
}

export const getTodayStatus = () =>
  request.get<any, TodayStatus>("/attendance/today");

export const checkIn = (lat?: number | null, lng?: number | null, force?: boolean) =>
  request.post<any, AttendanceRecord>("/attendance/check-in", null, { params: { lat, lng, force } });

export const checkOut = (lat?: number | null, lng?: number | null, force?: boolean) =>
  request.post<any, AttendanceRecord>("/attendance/check-out", null, { params: { lat, lng, force } });

// 定位校验：管理员配置，异常打卡走审批
// geo_check_enable、geo_lat、geo_lng、geo_radius 字符串数字都收
export interface GeoConfig {
  enable?: string | number | boolean;
  lat?: string | number;
  lng?: string | number;
  radius?: string | number;
  weekendCount?: string | number;
  [key: string]: any;
}

export const getGeoConfig = () =>
  request.get<any, GeoConfig>("/attendance/geo-config");

export const saveGeoConfig = (data: GeoConfig) =>
  request.post<any, void>("/attendance/geo-config", data);

export const getGeoAbnormal = () =>
  request.get<any, any[]>("/attendance/geo-abnormal");

export const approveGeoAbnormal = (id: number) =>
  request.post<any, void>("/attendance/geo-abnormal/" + id + "/approve");

export const rejectGeoAbnormal = (id: number, remark?: string) =>
  request.post<any, void>("/attendance/geo-abnormal/" + id + "/reject", null, { params: { remark } });

// 外出打卡
export const outing = (data: { reason: string; expected_return?: string }) =>
  request.post<any, any>("/attendance/outing", data);

export const getMyOutings = (month?: string) =>
  request.get<any, any[]>("/attendance/outing/my", { params: { month } });

// 我的出勤记录 / 补签
export const getMyRecords = (month?: string) =>
  request.get<any, AttendanceRecord[]>("/attendance/my", { params: { month } });

export const applyMakeup = (data: MakeupApply) =>
  request.post<any, MakeupApply>("/attendance/makeup", data);

export const getMyMakeups = () =>
  request.get<any, MakeupApply[]>("/attendance/makeup/my");

export const getPendingMakeups = (status?: number, deptId?: number) =>
  request.get<any, MakeupApply[]>("/attendance/makeup/pending", { params: { status, deptId } });

export const approveMakeup = (id: number, result: number, remark?: string) =>
  request.post<any, MakeupApply>("/attendance/makeup/" + id + "/approve", null, { params: { result, remark } });

export const withdrawMakeup = (id: number) =>
  request.post<any, void>("/attendance/makeup/" + id + "/withdraw");

// 管理员查看 / 统计
export const getAllRecords = (params: any) =>
  request.get<any, { records: AttendanceRecord[]; total: number }>("/attendance/all", { params });

export interface MyAttendanceStat {
  userId: number;
  realName?: string;
  month: string;
  expectedDays: number;
  totalDays: number;
  normalDays: number;
  lateDays: number;
  earlyDays: number;
  absentDays: number;
  otherDays: number;
  abnormalDays: number;
  attendanceRate: number;
}

export const getMyStatistics = (month?: string) =>
  request.get<any, MyAttendanceStat>("/attendance/my/statistics", { params: { month } });

export const getStatistics = (deptId?: number | null, month?: string) =>
  request.get<any, any[]>("/attendance/statistics", { params: { deptId, month } });

// 出勤申诉
export const applyAppeal = (data: { record_id: number | null; reason: string; attachment_ids?: number[] }) =>
  request.post<any, any>("/attendance/appeal", data);

export const getMyAppeals = () =>
  request.get<any, any[]>("/attendance/appeal/my");

export const getPendingAppeals = (params?: { deptId?: number; status?: number }) =>
  request.get<any, any[]>("/attendance/appeal/pending", { params });

export const approveAppeal = (id: number, result: number, remark?: string) =>
  request.post<any, any>("/attendance/appeal/" + id + "/approve", null, { params: { result, remark } });

// 出勤通用配置
export const getAttendanceConfig = () =>
  request.get<any, Record<string, string>>("/attendance/config");

export const saveAttendanceConfig = (data: Record<string, string>) =>
  request.put<any, void>("/attendance/config", data);

// 班次模板（出勤配置页）
export interface ShiftTemplate {
  id?: number;
  template_name: string;
  work_start: string;
  work_end: string;
  late_threshold: string;
  early_leave_threshold: string;
  shift_type?: string;
  is_all_day?: boolean | number;
  status?: number;
  [key: string]: any;
}

export const listShiftTemplates = () =>
  request.get<any, ShiftTemplate[]>("/attendance/shift-templates");

export const saveShiftTemplate = (data: Partial<ShiftTemplate>) =>
  request.post<any, any>("/attendance/shift-templates", data);

export const deleteShiftTemplate = (id: number) =>
  request.delete<any, void>("/attendance/shift-templates/" + id);

export const activateShiftTemplate = (id: number) =>
  request.put<any, void>("/attendance/shift-templates/" + id + "/activate");

// 自动排班规则
export interface ShiftSchedule {
  id?: number;
  schedule_name: string;
  schedule_type: string;
  work_start: string;
  work_end: string;
  late_threshold: string;
  early_leave_threshold: string;
  trigger_date?: string | null;
  status?: number;
  [key: string]: any;
}

export const listShiftSchedules = () =>
  request.get<any, ShiftSchedule[]>("/attendance/shift-schedules");

export const saveShiftSchedule = (data: Partial<ShiftSchedule>) =>
  request.post<any, any>("/attendance/shift-schedules", data);

export const deleteShiftSchedule = (id: number) =>
  request.delete<any, void>("/attendance/shift-schedules/" + id);

// 节假日维护
export interface HolidayItem {
  id?: number;
  holiday_date: string;
  holiday_name: string;
  holiday_type: number;   // 1 法定假日 2 调休工作日
  remark?: string;
}

export const listHolidays = () =>
  request.get<any, HolidayItem[]>("/attendance/holidays");

export const addHoliday = (data: Partial<HolidayItem>) =>
  request.post<any, any>("/attendance/holidays", data);

export const deleteHoliday = (id: number) =>
  request.delete<any, void>("/attendance/holidays/" + id);

// 出勤操作日志（管理员）
export const getAttendanceOperationLogs = () =>
  request.get<any, any[]>("/attendance/operation-logs");
