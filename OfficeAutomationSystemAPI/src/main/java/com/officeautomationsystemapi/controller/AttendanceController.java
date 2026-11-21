package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.utilt.ExcelSafe;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.AttendanceAppeal;
import com.officeautomationsystemapi.entity.AttendanceMakeup;
import com.officeautomationsystemapi.entity.AttendanceRecords;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.AttendanceAppealRepository;
import com.officeautomationsystemapi.repository.AttendanceRecordsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.AttendanceService;
import com.officeautomationsystemapi.service.DataScopeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// 考勤接口：打卡、补签、申诉、外出、节假日、统计导出
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UsersRepository usersRepo;

    @Autowired
    private AttendanceAppealRepository appealRepo;

    @Autowired
    private AttendanceRecordsRepository recordsRepo;

    @Autowired
    private DataScopeService dataScopeService;

    // 允许改的考勤配置项，别的模块的键不能在这里改
    private static final Set<String> ATTENDANCE_CONFIG_KEYS = Set.of(
        "work_start", "work_end", "late_threshold", "early_leave_threshold", "max_re_sign", "re_sign_window_hours");

    // 当前登录用户 ID
    private Long getUserId(HttpServletRequest request) {
        return RequestUser.id(request);
    }

    // 取客户端 IP，有代理时取 X-Forwarded-For 第一段
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            return request.getRemoteAddr();
        }
        int comma = ip.indexOf(',');
        return comma < 0 ? ip : ip.substring(0, comma).trim();
    }

    // 是否管理员，按角色编码判断
    private boolean isAdmin(HttpServletRequest request) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON ur.role_id=r.id " +
                "WHERE ur.user_id=? AND ur.deleted_at IS NULL AND r.status=1 AND r.deleted_at IS NULL AND r.role_code='admin'",
                Integer.class, getUserId(request));
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 能看全公司出勤数据的人：管理员、总经理、人事主管
    private boolean isGlobalViewer(HttpServletRequest request) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON ur.role_id=r.id " +
                "WHERE ur.user_id=? AND ur.deleted_at IS NULL AND r.status=1 AND r.deleted_at IS NULL AND r.role_code IN ('admin','gm','hr_manager')",
                Integer.class, getUserId(request));
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 当前用户是否拥有指定角色编码
    private boolean hasRole(HttpServletRequest request, String roleCode) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON ur.role_id=r.id " +
                "WHERE ur.user_id=? AND ur.deleted_at IS NULL AND r.status=1 AND r.deleted_at IS NULL AND r.role_code=?",
                Integer.class, getUserId(request), roleCode);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 是否部门主管，负责至少一个部门
    private boolean isDeptManager(HttpServletRequest request) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM organizations WHERE leader_id=? AND deleted_at IS NULL",
                Integer.class, getUserId(request));
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 能审补签的人：管理员、部门主管、人事，环节在 Service 里判断
    private boolean canReviewMakeup(HttpServletRequest request) {
        return isAdmin(request) || isDeptManager(request) || hasRole(request, "hr") || hasRole(request, "hr_manager");
    }

    // 目标员工是否在操作者数据范围内
    private boolean inDataScope(HttpServletRequest request, Long targetUserId) {
        if (targetUserId == null) return false;
        List<Long> visible = dataScopeService.visibleUserIds(getUserId(request));
        return visible == null || visible.contains(targetUserId);
    }

    // 能审申诉的人：管理员、人事专员、人事主管，不含部门主管
    private boolean canReviewAppeal(HttpServletRequest request) {
        return isAdmin(request) || hasRole(request, "hr") || hasRole(request, "hr_manager");
    }

    // 能审异地签到的人：管理员、部门主管
    private boolean canReviewGeo(HttpServletRequest request) {
        return isAdmin(request) || isDeptManager(request);
    }

    // 当天考勤状态
    @GetMapping("/today")
    public Result<Map<String, Object>> today(HttpServletRequest request) {
        return Result.success(attendanceService.getTodayStatus(getUserId(request)));
    }

    // 上班签到，可带经纬度，force=true 时超范围也签
    @PostMapping("/check-in")
    @RequirePermission("attendance:check-in")
    public Result<AttendanceRecords> checkIn(
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestParam(required = false) Boolean force,
            HttpServletRequest request) {
        try {
            return Result.success(attendanceService.checkIn(getUserId(request), getClientIp(request), lat, lng, "normal", force));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 下班签退，可带经纬度
    @PostMapping("/check-out")
    @RequirePermission("attendance:check-out")
    public Result<AttendanceRecords> checkOut(
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestParam(required = false) Boolean force,
            HttpServletRequest request) {
        try {
            return Result.success(attendanceService.checkOut(getUserId(request), getClientIp(request), lat, lng, force));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 我的出勤记录，默认当月
    @GetMapping("/my")
    @RequirePermission("attendance:view-own")
    public Result<List<AttendanceRecords>> myRecords(
            @RequestParam(required = false) String month,
            HttpServletRequest request) {
        return Result.success(attendanceService.myRecords(getUserId(request), month));
    }

    // 外出打卡，登记事由和预计返回时间，不影响签到签退
    @PostMapping("/outing")
    @RequirePermission("attendance:out")
    public Result<?> outing(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            Users user = usersRepo.selectById(userId);
            Long deptId = user != null ? user.getOrg_id() : null;
            String reason = body.get("reason") == null ? "" : body.get("reason").toString();
            String expectedReturn = body.get("expected_return") == null ? "" : body.get("expected_return").toString();
            jdbc.update("INSERT INTO attendance_outing (user_id, dept_id, outing_date, start_time, reason, expected_return) VALUES (?,?,?,?,?,?)",
                userId, deptId, LocalDate.now(), LocalDateTime.now(), reason, expectedReturn);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 我的外出记录，默认当月
    @GetMapping("/outing/my")
    @RequirePermission("attendance:view-own")
    public Result<List<Map<String, Object>>> myOutings(
            @RequestParam(required = false) String month,
            HttpServletRequest request) {
        String targetMonth = (month == null || month.isEmpty()) ? LocalDate.now().toString().substring(0, 7) : month;
        return Result.success(jdbc.queryForList(
            "SELECT id, outing_date, DATE_FORMAT(start_time,'%H:%i') AS start_time, reason, expected_return FROM attendance_outing " +
            "WHERE user_id=? AND deleted_at IS NULL AND DATE_FORMAT(outing_date,'%Y-%m')=? ORDER BY outing_date DESC, id DESC",
            getUserId(request), targetMonth));
    }

    // 发起补签申请
    @PostMapping("/makeup")
    @RequirePermission("attendance:makeup:apply")
    public Result<AttendanceMakeup> applyMakeup(@RequestBody AttendanceMakeup body, HttpServletRequest request) {
        try {
            return Result.success(attendanceService.applyMakeup(getUserId(request), body));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 我的补签记录
    @GetMapping("/makeup/my")
    @RequirePermission("attendance:view-own")
    public Result<List<AttendanceMakeup>> myMakeups(HttpServletRequest request) {
        return Result.success(attendanceService.myMakeups(getUserId(request)));
    }

    // 待审批的补签，主管和人事能看
    @GetMapping("/makeup/pending")
    public Result<List<AttendanceMakeup>> pendingMakeups(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId,
            HttpServletRequest request) {
        if (!canReviewMakeup(request)) return Result.error(403, "无权限：仅部门主管、人事或管理员可审批补签");
        return Result.success(attendanceService.pendingMakeups(getUserId(request), status, deptId));
    }

    // 审批补签，result 为 1 通过、2 驳回
    @PostMapping("/makeup/{id}/approve")
    public Result<AttendanceMakeup> approveMakeup(
            @PathVariable Long id,
            @RequestParam Integer result,
            @RequestParam(required = false) String remark,
            HttpServletRequest request) {
        if (!canReviewMakeup(request)) return Result.error(403, "无权限：仅部门主管、人事或管理员可审批补签");
        try {
            return Result.success(attendanceService.approveMakeup(getUserId(request), id, result, remark));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 撤回自己的补签，只能撤待审批的
    @PostMapping("/makeup/{id}/withdraw")
    public Result<?> withdrawMakeup(@PathVariable Long id, HttpServletRequest request) {
        try {
            attendanceService.withdrawMakeup(getUserId(request), id);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 管理员删除补签记录
    @DeleteMapping("/makeup/{id}")
    public Result<?> deleteMakeup(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error(403, "无权限：仅系统管理员可操作");
        try {
            attendanceService.deleteMakeup(id);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 分页查出勤，全公司要 attendance:view-all，部门主管要 attendance:view-dept
    @GetMapping("/all")
    @RequirePermission({"attendance:view-all", "attendance:view-dept"})
    public Result<Map<String, Object>> allRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return Result.success(attendanceService.allRecords(getUserId(request), startDate, endDate, deptId, status, pageNum, pageSize));
    }

    // 出勤统计只给管理员、总经理、人事主管
    @GetMapping("/statistics")
    @RequirePermission("attendance:view-all")
    public Result<List<Map<String, Object>>> statistics(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String month,
            HttpServletRequest request) {
        if (!isGlobalViewer(request)) return Result.error(403, "无权限：仅管理员或总经理可查看");
        return Result.success(attendanceService.statistics(getUserId(request), deptId, month));
    }

    // 我的月度统计，不受部门范围限制
    @GetMapping("/my/statistics")
    @RequirePermission({"attendance:view-own", "attendance:view-all", "attendance:view-dept", "attendance:manage", "attendance:audit"})
    public Result<Map<String, Object>> myStatistics(
            @RequestParam(required = false) String month,
            HttpServletRequest request) {
        return Result.success(attendanceService.myStatistics(getUserId(request), month));
    }

    // 导出出勤明细 Excel，权限同列表查看
    @GetMapping("/export")
    @RequirePermission({"attendance:view-all", "attendance:view-dept"})
    public void export(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        // 导出范围由 allRecords 控制，全公司或本部门
        if (!isGlobalViewer(request) && !isDeptManager(request)) {
            response.setStatus(403);
            return;
        }
        Long userId = getUserId(request);
        // 导出不分页，一次取够
        Map<String, Object> result = attendanceService.allRecords(userId, startDate, endDate, deptId, status, 1, 10000);
        @SuppressWarnings("unchecked")
        List<AttendanceRecords> records = (List<AttendanceRecords>) result.get("records");

        String[] headers = {"日期", "姓名", "签到时间", "签退时间", "工时", "加班", "状态", "备注"};
        String[] statusText = {"正常", "迟到", "早退", "缺勤", "外出", "出差", "请假", "严重迟到", "未签退异常", "严重早退"};

        try (Workbook wb = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            Sheet sheet = wb.createSheet("出勤报表");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            int rowIndex = 1;
            for (AttendanceRecords record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(record.getAttendance_date() != null ? record.getAttendance_date().toString() : "");
                Users user = usersRepo.selectById(record.getUser_id());
                row.createCell(1).setCellValue(user != null ? user.getReal_name() : "");
                row.createCell(2).setCellValue(record.getCheck_in_time() != null ? record.getCheck_in_time().format(formatter) : "");
                row.createCell(3).setCellValue(record.getCheck_out_time() != null ? record.getCheck_out_time().format(formatter) : "");
                row.createCell(4).setCellValue(record.getWork_hours() != null ? record.getWork_hours().doubleValue() : 0);
                row.createCell(5).setCellValue(record.getOvertime_hours() != null ? record.getOvertime_hours().doubleValue() : 0);
                int statusValue = record.getStatus() != null ? record.getStatus() : 0;
                row.createCell(6).setCellValue(statusValue >= 0 && statusValue < statusText.length ? statusText[statusValue] : "未知");
                row.createCell(7).setCellValue(record.getRemark() != null ? record.getRemark() : "");
            }
            // 列宽按字符数设置，单位是 1/256 字符
            int[] widths = {14, 12, 20, 20, 10, 10, 12, 24};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String filename = URLEncoder.encode("出勤报表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + filename);
            ExcelSafe.sanitize(wb);
            wb.write(out);
            // 导出写一条操作日志，失败不影响导出
            try {
                String username = "";
                try {
                    username = jdbc.queryForObject("SELECT username FROM users WHERE id=? AND deleted_at IS NULL", String.class, userId);
                } catch (Exception ignored) {
                }
                jdbc.update("INSERT INTO operation_logs (user_id, username, module, action, method, url, ip_address, target_type, request_params, response_result, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                    userId, username, "attendance", "export", "GET", "/api/attendance/export", getClientIp(request), "attendance",
                    "startDate=" + (startDate == null ? "null" : startDate) + ";endDate=" + (endDate == null ? "null" : endDate) + ";deptId=" + (deptId == null ? "null" : String.valueOf(deptId)),
                    "导出" + records.size() + "条", 1);
            } catch (Exception ignored) {
            }
        }
    }

    // 考勤操作日志，管理员可查
    @GetMapping("/operation-logs")
    public Result<List<Map<String, Object>>> operationLogs(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error(403, "无权限：仅系统管理员可操作");
        return Result.success(jdbc.queryForList(
            "SELECT l.id, l.user_id, l.username, u.real_name, l.action, l.url, l.request_params, l.response_result, l.status, l.created_at FROM operation_logs l LEFT JOIN users u ON u.id=l.user_id WHERE l.module='attendance' AND l.deleted_at IS NULL ORDER BY l.created_at DESC LIMIT 100"));
    }

    // 提交出勤异常申诉
    @PostMapping("/appeal")
    @RequirePermission("attendance:appeal:apply")
    public Result<AttendanceAppeal> appeal(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object recordIdValue = body.get("record_id");
        if (recordIdValue == null) return Result.error("请选择要申诉的出勤记录");
        Long recordId = Long.valueOf(String.valueOf(recordIdValue));
        Object reasonValue = body.get("reason");
        String reason = reasonValue == null ? null : String.valueOf(reasonValue);
        if (reason == null || reason.trim().isEmpty()) return Result.error("请填写申诉理由");

        // 只能申诉当月的记录
        AttendanceRecords record = recordsRepo.selectById(recordId);
        if (record == null) return Result.error("出勤记录不存在");
        // 只能申诉自己的记录
        Long operatorId = getUserId(request);
        if (record.getUser_id() != null && !record.getUser_id().equals(operatorId)) {
            return Result.error("只能申诉自己的出勤记录");
        }
        LocalDate recordDate = record.getAttendance_date();
        LocalDate today = LocalDate.now();
        if (recordDate.isAfter(today)) return Result.error("不能申诉未来的出勤记录");
        if (recordDate.getYear() != today.getYear() || recordDate.getMonthValue() != today.getMonthValue()) {
            return Result.error("申诉仅限当月数据（" + today.toString().substring(0, 7) + "）");
        }

        AttendanceAppeal appeal = new AttendanceAppeal();
        appeal.setRecord_id(recordId);
        appeal.setUser_id(getUserId(request));
        appeal.setReason(reason);
        appeal.setStatus(0);
        appeal.setCreated_at(LocalDateTime.now());
        appeal.setUpdated_at(LocalDateTime.now());
        appealRepo.insert(appeal);
        // 绑定上传的证明材料
        Object attachments = body.get("attachment_ids");
        if (attachments instanceof List && !((List<?>) attachments).isEmpty()) {
            for (Object attachmentId : (List<?>) attachments) {
                try {
                    long id = Long.parseLong(String.valueOf(attachmentId));
                    // 只能绑定自己上传的附件
                    jdbc.update("UPDATE attachments SET target_type='attendance_appeal', target_id=? WHERE id=? AND uploader_id=? AND deleted_at IS NULL",
                        appeal.getId(), id, operatorId);
                } catch (Exception ignored) {
                }
            }
        }
        return Result.success(appeal);
    }

    // 补上申请人姓名、出勤状态和证明材料
    private List<Map<String, Object>> enrichAppeals(List<AttendanceAppeal> list) {
        String[] statusText = {"正常", "迟到", "早退", "缺勤", "外出", "出差", "请假", "严重迟到", "未签退异常", "严重早退"};
        List<Map<String, Object>> result = new ArrayList<>();
        for (AttendanceAppeal appeal : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", appeal.getId());
            item.put("record_id", appeal.getRecord_id());
            item.put("user_id", appeal.getUser_id());
            item.put("reason", appeal.getReason());
            item.put("status", appeal.getStatus());
            item.put("created_at", appeal.getCreated_at());
            item.put("approve_remark", appeal.getApprove_remark());
            item.put("approve_time", appeal.getApprove_time());

            Users user = usersRepo.selectById(appeal.getUser_id());
            item.put("real_name", user != null ? user.getReal_name() : "");
            AttendanceRecords record = recordsRepo.selectById(appeal.getRecord_id());
            if (record != null) {
                item.put("record_date", record.getAttendance_date());
                int statusValue = record.getStatus() != null ? record.getStatus() : 0;
                item.put("record_status_text", statusValue >= 0 && statusValue < statusText.length ? statusText[statusValue] : "未知");
            }

            List<Map<String, Object>> attachments = new ArrayList<>();
            try {
                List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT id, file_name, file_size, file_ext FROM attachments WHERE target_type='attendance_appeal' AND target_id=? AND deleted_at IS NULL ORDER BY created_at DESC", appeal.getId());
                for (Map<String, Object> row : rows) {
                    Map<String, Object> file = new HashMap<>();
                    file.put("id", row.get("id"));
                    file.put("fileName", row.get("file_name"));
                    file.put("fileSize", row.get("file_size"));
                    file.put("fileExt", row.get("file_ext"));
                    file.put("downloadUrl", "/api/flow/instances/attachments/" + row.get("id") + "/download");
                    attachments.add(file);
                }
            } catch (Exception ignored) {
            }
            item.put("attachments", attachments);
            result.add(item);
        }
        return result;
    }

    // 我的申诉记录
    @GetMapping("/appeal/my")
    @RequirePermission("attendance:view-own")
    public Result<List<Map<String, Object>>> myAppeals(HttpServletRequest request) {
        List<AttendanceAppeal> list = appealRepo.selectList(new QueryWrapper<AttendanceAppeal>()
            .eq("user_id", getUserId(request))
            .orderByDesc("created_at"));
        return Result.success(enrichAppeals(list));
    }

    // 待审申诉，人事专员和人事主管能看
    @GetMapping("/appeal/pending")
    @RequirePermission("attendance:audit")
    public Result<List<Map<String, Object>>> pendingAppeals(HttpServletRequest request) {
        if (!canReviewAppeal(request)) return Result.error(403, "无权限：仅管理员或人事可审核申诉");
        List<AttendanceAppeal> list = appealRepo.selectList(new QueryWrapper<AttendanceAppeal>()
            .eq("status", 0)
            .orderByDesc("created_at"));
        // 人事专员只看本部门申诉，管理员看全部
        List<AttendanceAppeal> scoped = new ArrayList<>();
        for (AttendanceAppeal appeal : list) {
            if (inDataScope(request, appeal.getUser_id())) {
                scoped.add(appeal);
            }
        }
        return Result.success(enrichAppeals(scoped));
    }

    // 审核申诉，1 通过 2 驳回，通过后状态改回正常
    @PostMapping("/appeal/{id}/approve")
    @RequirePermission("attendance:audit")
    public Result<AttendanceAppeal> approveAppeal(
            @PathVariable Long id,
            @RequestParam Integer result,
            @RequestParam(required = false) String remark,
            HttpServletRequest request) {
        if (!canReviewAppeal(request)) return Result.error(403, "无权限：仅管理员或人事可审核申诉");
        Long reviewerId = getUserId(request);
        AttendanceAppeal appeal = appealRepo.selectById(id);
        if (appeal == null) return Result.error("申诉不存在");
        if (appeal.getStatus() == null || appeal.getStatus() != 0) return Result.error("该申诉已处理");
        if (reviewerId != null && reviewerId.equals(appeal.getUser_id())) return Result.error("不能给自己审批");
        if (!inDataScope(request, appeal.getUser_id())) return Result.error(403, "无权审核该申诉：不在您的数据范围内");

        appeal.setStatus(result);
        appeal.setApprover_id(reviewerId);
        appeal.setApprove_time(LocalDateTime.now());
        appeal.setApprove_remark(remark);
        appeal.setUpdated_at(LocalDateTime.now());
        appealRepo.updateById(appeal);

        AttendanceRecords record = appeal.getRecord_id() == null ? null : recordsRepo.selectById(appeal.getRecord_id());
        // 申诉人和记录属主必须一致
        if (record != null && record.getUser_id() != null && !record.getUser_id().equals(appeal.getUser_id())) {
            return Result.error("申诉记录与申请人不一致，无法审核");
        }
        if (result == 1 && record != null) {
            record.setStatus(0);
            record.setRemark("申诉通过");
            record.setUpdated_at(LocalDateTime.now());
            recordsRepo.updateById(record);
        }

        // 通知申请人审核结果
        try {
            String dateText = record != null ? String.valueOf(record.getAttendance_date()) : "";
            String title = result == 1 ? "出勤申诉已通过" : "出勤申诉被驳回";
            String content = result == 1
                    ? "您 " + dateText + " 的出勤申诉已审批通过，出勤状态已修正为正常"
                    : "您 " + dateText + " 的出勤申诉被驳回" + (remark != null && !remark.isEmpty() ? "，意见：" + remark : "");
            jdbc.update("INSERT INTO notification_records (user_id, title, content, type, is_read, biz_type, biz_id, jump_url, created_at, updated_at) VALUES (?,?,?,2,0,'attendance_appeal',?,?,NOW(),NOW())",
                    appeal.getUser_id(), title, content, appeal.getId(), "/home/attendance");
        } catch (Exception ignored) {
        }
        return Result.success(appeal);
    }

    // 读考勤配置，需要 attendance:manage
    @RequirePermission("attendance:manage")
    @GetMapping("/config")
    public Result<?> getConfig(HttpServletRequest request) {
        // 只返回考勤自己的配置项
        return Result.success(jdbc.queryForList(
            "SELECT config_key, config_value, description FROM system_config WHERE config_key IN ('work_start','work_end','late_threshold','early_leave_threshold','max_re_sign','re_sign_window_hours')"));
    }

    // 批量保存考勤配置，需要 attendance:manage
    @RequirePermission("attendance:manage")
    @PutMapping("/config")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateConfig(@RequestBody Map<String, String> body, HttpServletRequest request) {
        // 键必须在考勤白名单内，防止改到别的模块
        List<String> invalid = new ArrayList<>();
        for (String key : body.keySet()) {
            if (!ATTENDANCE_CONFIG_KEYS.contains(key)) invalid.add(key);
        }
        if (!invalid.isEmpty()) {
            return Result.error("不支持的考勤配置项：" + String.join(", ", invalid));
        }
        body.forEach((key, value) -> jdbc.update("UPDATE system_config SET config_value=? WHERE config_key=?", value, key));
        return Result.success(null);
    }

    // 节假日列表，需要 attendance:manage
    @RequirePermission("attendance:manage")
    @GetMapping("/holidays")
    public Result<?> listHolidays(HttpServletRequest request) {
        return Result.success(jdbc.queryForList("SELECT id, holiday_date, holiday_name, holiday_type FROM holidays ORDER BY holiday_date DESC LIMIT 365"));
    }

    // 新增节假日，年份从日期里截取
    @RequirePermission("attendance:manage")
    @PostMapping("/holidays")
    public Result<?> addHoliday(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object date = body.get("holiday_date");
        String year = date != null && date.toString().length() >= 4 ? date.toString().substring(0, 4) : null;
        jdbc.update("INSERT INTO holidays (year, holiday_date, holiday_name, holiday_type) VALUES (?,?,?,?)",
            year, date, body.get("holiday_name"), body.get("holiday_type"));
        return Result.success(null);
    }

    // 删除节假日
    @RequirePermission("attendance:manage")
    @DeleteMapping("/holidays/{id}")
    public Result<?> delHoliday(@PathVariable Long id, HttpServletRequest request) {
        jdbc.update("DELETE FROM holidays WHERE id=?", id);
        return Result.success(null);
    }

    // 班次模板列表，需要 attendance:manage
    @RequirePermission("attendance:manage")
    @GetMapping("/shift-templates")
    public Result<?> listShiftTemplates(HttpServletRequest request) {
        return Result.success(jdbc.queryForList("SELECT * FROM shift_templates ORDER BY is_active DESC, id"));
    }

    // 校验班次时间：迟到线晚于上班，早退线早于下班
    private boolean validShiftTimes(Map<String, Object> body) {
        try {
            if (allDayValue(body) == 1) return true;
            LocalTime workStart = LocalTime.parse(body.get("work_start").toString());
            LocalTime workEnd = LocalTime.parse(body.get("work_end").toString());
            LocalTime lateThreshold = LocalTime.parse(body.get("late_threshold").toString());
            LocalTime earlyLeaveThreshold = LocalTime.parse(body.get("early_leave_threshold").toString());
            return lateThreshold.isAfter(workStart) && earlyLeaveThreshold.isBefore(workEnd) && workEnd.isAfter(workStart);
        } catch (Exception e) {
            return false;
        }
    }

    // is_all_day 统一成 1 或 0
    private int allDayValue(Map<String, Object> body) {
        Object value = body.get("is_all_day");
        return value != null && ("1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value))) ? 1 : 0;
    }

    // 新增或修改班次模板，带 id 视为修改
    @RequirePermission("attendance:manage")
    @PostMapping("/shift-templates")
    public Result<?> saveShiftTemplate(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!validShiftTimes(body)) return Result.error("时间配置不合理：迟到线须晚于上班时间，早退线须早于下班时间");
        if (body.get("id") != null) {
            jdbc.update("UPDATE shift_templates SET template_name=?, work_start=?, work_end=?, late_threshold=?, early_leave_threshold=?, shift_type=?, is_all_day=? WHERE id=?",
                body.get("template_name"), body.get("work_start"), body.get("work_end"),
                body.get("late_threshold"), body.get("early_leave_threshold"), body.get("shift_type"),
                allDayValue(body), body.get("id"));
        } else {
            jdbc.update("INSERT INTO shift_templates (template_name, work_start, work_end, late_threshold, early_leave_threshold, shift_type, is_all_day) VALUES (?,?,?,?,?,?,?)",
                body.get("template_name"), body.get("work_start"), body.get("work_end"),
                body.get("late_threshold"), body.get("early_leave_threshold"), body.get("shift_type"), allDayValue(body));
        }
        return Result.success(null);
    }

    // 启用某个模板，同时停用其它模板
    @RequirePermission("attendance:manage")
    @PutMapping("/shift-templates/{id}/activate")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> activateShiftTemplate(@PathVariable Long id, HttpServletRequest request) {
        jdbc.update("UPDATE shift_templates SET is_active=0");
        jdbc.update("UPDATE shift_templates SET is_active=1 WHERE id=?", id);
        return Result.success(null);
    }

    // 删除班次模板，已启用的不能删
    @RequirePermission("attendance:manage")
    @DeleteMapping("/shift-templates/{id}")
    public Result<?> delShiftTemplate(@PathVariable Long id, HttpServletRequest request) {
        jdbc.update("DELETE FROM shift_templates WHERE id=? AND is_active=0", id);
        return Result.success(null);
    }

    // 自动排班规则列表
    @RequirePermission("attendance:manage")
    @GetMapping("/shift-schedules")
    public Result<?> listSchedules(HttpServletRequest request) {
        return Result.success(jdbc.queryForList("SELECT * FROM shift_schedules ORDER BY id DESC"));
    }

    // 新增或修改排班规则，带 id 视为修改
    @RequirePermission("attendance:manage")
    @PostMapping("/shift-schedules")
    public Result<?> saveSchedule(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!validShiftTimes(body)) return Result.error("时间配置不合理：迟到线须晚于上班时间，早退线须早于下班时间");

        if (body.get("id") != null) {
            jdbc.update("UPDATE shift_schedules SET schedule_name=?, schedule_type=?, schedule_date=?, week_day=?, start_date=?, end_date=?, work_start=?, work_end=?, late_threshold=?, early_leave_threshold=?, lunar_month=?, lunar_day=?, yearly_month=?, yearly_day=? WHERE id=?",
                body.get("schedule_name"), body.get("schedule_type"), body.get("schedule_date"), body.get("week_day"),
                body.get("start_date"), body.get("end_date"), body.get("work_start"), body.get("work_end"),
                body.get("late_threshold"), body.get("early_leave_threshold"),
                body.get("lunar_month"), body.get("lunar_day"), body.get("yearly_month"), body.get("yearly_day"), body.get("id"));
        } else {
            jdbc.update("INSERT INTO shift_schedules (schedule_name, schedule_type, schedule_date, week_day, start_date, end_date, work_start, work_end, late_threshold, early_leave_threshold, lunar_month, lunar_day, yearly_month, yearly_day) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                body.get("schedule_name"), body.get("schedule_type"), body.get("schedule_date"), body.get("week_day"),
                body.get("start_date"), body.get("end_date"), body.get("work_start"), body.get("work_end"),
                body.get("late_threshold"), body.get("early_leave_threshold"),
                body.get("lunar_month"), body.get("lunar_day"), body.get("yearly_month"), body.get("yearly_day"));
        }
        return Result.success(null);
    }

    // 删除排班规则
    @RequirePermission("attendance:manage")
    @DeleteMapping("/shift-schedules/{id}")
    public Result<?> delSchedule(@PathVariable Long id, HttpServletRequest request) {
        jdbc.update("DELETE FROM shift_schedules WHERE id=?", id);
        return Result.success(null);
    }

    // 导出统计 Excel，权限同统计查看
    @GetMapping("/statistics/export")
    @RequirePermission("attendance:view-all")
    public void exportStatistics(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String month,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        if (!isGlobalViewer(request)) {
            response.setStatus(403);
            return;
        }
        Long userId = getUserId(request);
        List<Map<String, Object>> rows = attendanceService.statistics(userId, deptId, month);
        String[] headers = {"姓名", "应出勤天数", "实际出勤", "正常", "迟到", "早退", "缺勤", "出勤率"};

        try (Workbook wb = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            Sheet sheet = wb.createSheet("出勤统计");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Map<String, Object> row : rows) {
                Row sheetRow = sheet.createRow(rowIndex++);
                sheetRow.createCell(0).setCellValue(String.valueOf(row.getOrDefault("real_name", "")));
                sheetRow.createCell(1).setCellValue(((Number) row.getOrDefault("expected_days", 0)).intValue());
                sheetRow.createCell(2).setCellValue(((Number) row.getOrDefault("total_days", 0)).intValue());
                sheetRow.createCell(3).setCellValue(((Number) row.getOrDefault("normal_days", 0)).intValue());
                sheetRow.createCell(4).setCellValue(((Number) row.getOrDefault("late_days", 0)).intValue());
                sheetRow.createCell(5).setCellValue(((Number) row.getOrDefault("early_days", 0)).intValue());
                sheetRow.createCell(6).setCellValue(((Number) row.getOrDefault("absent_days", 0)).intValue());
                sheetRow.createCell(7).setCellValue(String.valueOf(row.getOrDefault("attendance_rate", 0)) + "%");
            }

            int[] widths = {12, 14, 12, 10, 10, 10, 10, 10};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String filename = URLEncoder.encode("出勤统计_" + (month == null ? "" : month) + ".xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + filename);
            ExcelSafe.sanitize(wb);
            wb.write(out);
            // 统计导出也记操作日志
            try {
                String username = "";
                try {
                    username = jdbc.queryForObject("SELECT username FROM users WHERE id=? AND deleted_at IS NULL", String.class, userId);
                } catch (Exception ignored) {
                }
                jdbc.update("INSERT INTO operation_logs (user_id, username, module, action, method, url, ip_address, target_type, request_params, response_result, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                    userId, username, "attendance", "export_statistics", "GET", "/api/attendance/statistics/export", getClientIp(request), "attendance",
                    "month=" + (month == null ? "null" : month) + ";deptId=" + (deptId == null ? "null" : String.valueOf(deptId)),
                    "导出" + rows.size() + "条", 1);
            } catch (Exception ignored) {
            }
        }
    }

    // 手动补跑结算，不传日期就结算昨天，定时任务 02:00 也会跑
    @RequirePermission("attendance:manage")
    @PostMapping("/settle")
    public Result<String> settle(
            @RequestParam(required = false) String date,
            HttpServletRequest request) {
        try {
            attendanceService.settleAttendance(date);
        } catch (Exception e) {
            return Result.error("结算失败：" + e.getMessage());
        }
        return Result.success("结算完成");
    }

    // 地理校验配置，签到前要拿，所有人可读
    @GetMapping("/geo-config")
    public Result<Map<String, Object>> geoConfig() {
        return Result.success(attendanceService.geoConfig());
    }

    // 保存地理校验配置，需要 attendance:manage
    @RequirePermission("attendance:manage")
    @PostMapping("/geo-config")
    public Result<String> saveGeoConfig(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        attendanceService.saveGeoConfig(body);
        return Result.success("保存成功");
    }

    // 异地带审列表，管理员看全部，部门主管看本部门
    @GetMapping("/geo-abnormal")
    public Result<List<Map<String, Object>>> geoAbnormal(HttpServletRequest request) {
        if (!canReviewGeo(request)) return Result.error(403, "无权限：仅管理员或部门主管可审核异地签到");
        return Result.success(attendanceService.geoAbnormalList(getUserId(request)));
    }

    // 确认异地签到正常
    @PostMapping("/geo-abnormal/{id}/approve")
    public Result<String> geoApprove(@PathVariable Long id, HttpServletRequest request) {
        if (!canReviewGeo(request)) return Result.error(403, "无权限：仅管理员或部门主管可审核异地签到");
        attendanceService.reviewGeoAbnormal(getUserId(request), id, 1, "异地签到已确认");
        return Result.success("已确认正常");
    }

    // 驳回异地签到，当天记缺勤
    @PostMapping("/geo-abnormal/{id}/reject")
    public Result<String> geoReject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        if (!canReviewGeo(request)) return Result.error(403, "无权限：仅管理员或部门主管可审核异地签到");
        String remark = body != null && body.get("remark") != null ? String.valueOf(body.get("remark")) : "异地签到已驳回";
        attendanceService.reviewGeoAbnormal(getUserId(request), id, 2, remark);
        return Result.success("已驳回并记缺勤");
    }
}
