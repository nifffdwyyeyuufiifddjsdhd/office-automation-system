package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.LoginLogs;
import com.officeautomationsystemapi.repository.LoginLogsRepository;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.service.DataScopeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 登录日志，人事主管查员工登录和失败记录
@RestController
@RequestMapping("/api/system/login-logs")
public class LoginLogController {

    @Autowired
    private LoginLogsRepository loginLogsRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataScopeService dataScopeService;

    // 分页查登录日志，可按用户名、状态、时间筛选
    @GetMapping
    @RequirePermission("login:stat")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
        // 人事主管只看本部门及下级，全公司数据范围的不受限
        List<Long> visible = dataScopeService.visibleUserIds(RequestUser.id(request));
        if (visible != null && visible.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("records", new ArrayList<>());
            empty.put("total", 0);
            empty.put("pageNum", page);
            empty.put("pageSize", size);
            return Result.success(empty);
        }
        QueryWrapper<LoginLogs> wrapper = new QueryWrapper<>();
        if (visible != null) {
            wrapper.in("user_id", visible);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("username", kw).or().like("ip_address", kw).or().like("location", kw));
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        LocalDate start = parseDate(startDate);
        if (start != null) {
            wrapper.ge("login_time", start.atStartOfDay());
        }
        LocalDate end = parseDate(endDate);
        if (end != null) {
            // 结束日期按当天 23:59:59 算，否则当天记录查不到
            wrapper.le("login_time", end.atTime(23, 59, 59));
        }
        wrapper.orderByDesc("login_time").orderByDesc("id");
        Page<LoginLogs> result = loginLogsRepository.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (LoginLogs log : result.getRecords()) {
            records.add(toRow(log));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("pageNum", page);
        data.put("pageSize", size);
        return Result.success(data);
    }

    // 登录统计：今日成败次数、7 天趋势、失败原因、登录最多的账号
    @GetMapping("/overview")
    @RequirePermission("login:stat")
    public Result<Map<String, Object>> overview(HttpServletRequest request) {
        // 统计和列表用同一套数据范围
        String scope = scopeSql(dataScopeService.visibleUserIds(RequestUser.id(request)));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayTotal", countOf("SELECT COUNT(*) FROM login_logs WHERE DATE(login_time) = CURDATE() AND deleted_at IS NULL" + scope));
        data.put("todaySuccess", countOf("SELECT COUNT(*) FROM login_logs WHERE DATE(login_time) = CURDATE() AND status = 1 AND deleted_at IS NULL" + scope));
        data.put("todayFail", countOf("SELECT COUNT(*) FROM login_logs WHERE DATE(login_time) = CURDATE() AND status = 0 AND deleted_at IS NULL" + scope));
        data.put("todayUsers", countOf("SELECT COUNT(DISTINCT username) FROM login_logs WHERE DATE(login_time) = CURDATE() AND status = 1 AND deleted_at IS NULL" + scope));
        data.put("trend", trend(scope));
        data.put("failReasons", failReasons(scope));
        data.put("topUsers", topUsers(scope));
        return Result.success(data);
    }

    // 可见用户拼成 SQL 片段，null 不限，空集合查不到
    private String scopeSql(List<Long> visibleUsers) {
        if (visibleUsers == null) {
            return "";
        }
        if (visibleUsers.isEmpty()) {
            return " AND 1 = 0";
        }
        StringBuilder sb = new StringBuilder(" AND user_id IN (");
        for (int i = 0; i < visibleUsers.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(visibleUsers.get(i));
        }
        return sb.append(")").toString();
    }

    // 近 7 天每天成败次数，没记录的日子补 0
    private List<Map<String, Object>> trend(String scope) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT DATE(login_time) AS d, SUM(status = 1) AS success_count, SUM(status = 0) AS fail_count " +
                        "FROM login_logs WHERE login_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND deleted_at IS NULL " + scope +
                        " GROUP BY DATE(login_time)");
        Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byDate.put(String.valueOf(row.get("d")), row);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            Map<String, Object> row = byDate.get(day.toString());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.toString());
            item.put("successCount", row == null ? 0 : toInt(row.get("success_count")));
            item.put("failCount", row == null ? 0 : toInt(row.get("fail_count")));
            result.add(item);
        }
        return result;
    }

    // 失败原因分布，取前 5 条
    private List<Map<String, Object>> failReasons(String scope) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT COALESCE(fail_reason, '未记录原因') AS reason, COUNT(*) AS amount FROM login_logs " +
                        "WHERE status = 0 AND deleted_at IS NULL " + scope + " GROUP BY reason ORDER BY amount DESC LIMIT 5");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reason", row.get("reason"));
            item.put("amount", toInt(row.get("amount")));
            result.add(item);
        }
        return result;
    }

    // 近 30 天登录最多的账号
    private List<Map<String, Object>> topUsers(String scope) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT username, COUNT(*) AS amount, MAX(login_time) AS last_time FROM login_logs " +
                        "WHERE status = 1 AND login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) AND deleted_at IS NULL " + scope +
                        " GROUP BY username ORDER BY amount DESC LIMIT 5");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", row.get("username"));
            item.put("amount", toInt(row.get("amount")));
            item.put("lastTime", row.get("last_time"));
            result.add(item);
        }
        return result;
    }

    // 单值计数，COUNT 返回的 Long 转成 int
    private Integer countOf(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value.intValue();
    }

    // 日志转前端字段
    private Map<String, Object> toRow(LoginLogs log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", log.getId());
        row.put("userId", log.getUser_id());
        row.put("username", log.getUsername());
        row.put("loginTime", log.getLogin_time());
        row.put("ipAddress", log.getIp_address());
        row.put("location", log.getLocation());
        row.put("device", log.getDevice());
        row.put("browser", log.getBrowser());
        row.put("os", log.getOs());
        row.put("status", log.getStatus());
        row.put("failReason", log.getFail_reason());
        row.put("logoutTime", log.getLogout_time());
        return row;
    }

    // 日期参数可能带时间，只取前 10 位
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String s = value.trim();
        try {
            return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
        } catch (Exception e) {
            return null;
        }
    }

    // 数值转换，无记录时 SUM 返回 null
    private Integer toInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return new java.math.BigDecimal(String.valueOf(value)).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
