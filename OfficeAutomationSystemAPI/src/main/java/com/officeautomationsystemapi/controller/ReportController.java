package com.officeautomationsystemapi.controller;

import com.alibaba.fastjson2.JSON;
import com.officeautomationsystemapi.utilt.ExcelSafe;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// 统计报表：算好任务、出勤、日志、流程四块的汇总给前端，并支持导出 Excel
// 数据范围按角色分：admin 看全公司，hr 的任务和出勤是全局、日志和流程只看本人
// 部门主管看自己负责的部门及下级，其他角色只看本人
// 工作日历读 holidays 表：holiday_type=1 是法定假日要剔除，2 是调休工作日要计入
// 周末默认不算工作日，是否计入由 system_params.attendance_weekend_count 控制
// status=8 是未签退异常，当天已到岗，算进实际出勤但单独列出来
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PermissionService permissionService;

    // 日期格式
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Excel 单元格最多 32767 个字符，超了会抛异常
    private static final int MAX_CELL_TEXT = 32767;

    // 是否全公司范围：管理员和总经理看全部，其余按部门或本人
    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        List<String> codes = permissionService.getUserRoleCodes(userId);
        // 公司整体运营报表只给管理员和总经理，人事主管只到出勤全局
        return codes.contains("admin") || codes.contains("gm");
    }

    // 是否人事，人事能看出勤和任务的全部统计
    private boolean isHr(Long userId) {
        if (userId == null) return false;
        List<String> codes = permissionService.getUserRoleCodes(userId);
        return codes.contains("hr") || codes.contains("hr_manager");
    }

    // 是否部门主管，至少负责一个部门
    private boolean isDeptManager(Long userId) {
        Integer c = jdbc.queryForObject(
            "SELECT COUNT(*) FROM organizations WHERE leader_id=? AND deleted_at IS NULL",
            Integer.class, userId);
        return c != null && c > 0;
    }

    // 主管负责的部门含下级里的全部用户
    private List<Long> managedDeptUserIds(Long managerId) {
        List<Long> leadOrgIds = jdbc.queryForList(
            "SELECT id FROM organizations WHERE leader_id=? AND deleted_at IS NULL", Long.class, managerId);
        return orgAndChildrenUserIds(leadOrgIds);
    }

    // 指定部门及全部下级部门里的用户 ID
    private List<Long> orgAndChildrenUserIds(List<Long> rootOrgIds) {
        if (rootOrgIds == null || rootOrgIds.isEmpty()) return new ArrayList<>();
        // 先建组织树父子关系，再从根部门往下收集
        List<Map<String, Object>> all = jdbc.queryForList(
            "SELECT id,parent_id FROM organizations WHERE deleted_at IS NULL");
        Map<Long, List<Long>> children = new HashMap<>();
        for (Map<String, Object> r : all) {
            Object pid = r.get("parent_id");
            if (pid == null) continue;
            children.computeIfAbsent(((Number) pid).longValue(), k -> new ArrayList<>())
                .add(((Number) r.get("id")).longValue());
        }
        Set<Long> scope = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>(rootOrgIds);
        while (!stack.isEmpty()) {
            Long o = stack.pop();
            if (!scope.add(o)) continue;
            List<Long> kids = children.get(o);
            if (kids != null) stack.addAll(kids);
        }
        if (scope.isEmpty()) return new ArrayList<>();
        String ph = scope.stream().map(x -> "?").collect(Collectors.joining(","));
        return jdbc.queryForList("SELECT id FROM users WHERE org_id IN (" + ph + ") AND deleted_at IS NULL",
            Long.class, scope.toArray());
    }

    // 部门名称，查不到时显示部门
    private String orgName(Long deptId) {
        try {
            Map<String, Object> org = jdbc.queryForMap("SELECT org_name FROM organizations WHERE id=?", deptId);
            return String.valueOf(org.get("org_name"));
        } catch (Exception e) {
            return "部门";
        }
    }

    // 单块数据范围：all 不过滤、dept 按部门人员、self 只看自己
    private static class Scope {
        final String scope;
        final List<Long> ids; // null 表示不过滤
        Scope(String scope, List<Long> ids) { this.scope = scope; this.ids = ids; }
        boolean isSelf() { return "self".equals(scope); }
    }

    // 四块各自的范围，外加给前端看的总范围说明
    private static class ScopeSet {
        Scope task, attendance, worklog, flow;
        String topScope, topName;
    }

    private Scope allScope() { return new Scope("all", null); }
    private Scope selfScope(long uid) { return new Scope("self", Collections.singletonList(uid)); }
    private Scope deptScope(List<Long> ids) { return new Scope("dept", ids); }

    // 按角色算出四块各自的数据范围
    private ScopeSet resolveScopes(long uid, Long deptId, Long userId) {
        boolean admin = isAdmin(uid);
        boolean hr = !admin && isHr(uid);
        boolean mgr = !admin && !hr && isDeptManager(uid);
        ScopeSet s = new ScopeSet();
        if (admin) {
            if (userId != null) {
                s.task = s.attendance = s.worklog = s.flow = selfScope(userId);
                s.topScope = "self";
                s.topName = "指定人员";
            } else if (deptId != null) {
                s.task = s.attendance = s.worklog = s.flow =
                    deptScope(orgAndChildrenUserIds(Collections.singletonList(deptId)));
                s.topScope = "dept";
                s.topName = orgName(deptId);
            } else {
                s.task = s.attendance = s.worklog = s.flow = allScope();
                s.topScope = "all";
                s.topName = "全公司";
            }
        } else if (hr) {
            Scope wide = deptId != null
                ? deptScope(orgAndChildrenUserIds(Collections.singletonList(deptId)))
                : allScope();
            s.task = s.attendance = wide;
            s.worklog = s.flow = selfScope(uid);
            s.topScope = "hr";
            s.topName = "人事（任务·出勤全局）";
        } else if (mgr) {
            s.task = s.attendance = s.worklog = s.flow = deptScope(managedDeptUserIds(uid));
            s.topScope = "dept";
            s.topName = "本部门";
        } else {
            s.task = s.attendance = s.worklog = s.flow = selfScope(uid);
            s.topScope = "self";
            s.topName = "个人";
        }
        return s;
    }

    // 是否 yyyy-MM-dd 格式的日期
    private boolean isDate(String s) {
        if (s == null || !s.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        try {
            LocalDate.parse(s, D);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 解析起止日期，默认最近 90 天
    private LocalDate[] parseRange(String startDate, String endDate) {
        LocalDate end = isDate(endDate) ? LocalDate.parse(endDate, D) : LocalDate.now();
        LocalDate start = isDate(startDate) ? LocalDate.parse(startDate, D) : end.minusDays(89);
        return new LocalDate[]{start, end};
    }

    // 开始晚于结束时直接报错，别让前端以为这段时间没数据
    private void checkRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) throw new RuntimeException("开始日期不能晚于结束日期");
    }

    // ID 列表拼成 IN 的内容，null 不过滤，空列表给 -1 查不到数据
    private String inIds(List<Long> ids) {
        if (ids == null) return null;
        if (ids.isEmpty()) return "-1";
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    // 单列 IN 过滤条件
    private String colIn(String col, Scope sc) {
        String in = inIds(sc.ids);
        return in == null ? "" : " AND " + col + " IN (" + in + ") ";
    }

    // date 列的时间条件
    private String dateRange(String col, String start, String end) {
        StringBuilder sb = new StringBuilder();
        if (isDate(start)) sb.append(" AND ").append(col).append(" >= '").append(start).append("'");
        if (isDate(end)) sb.append(" AND ").append(col).append(" <= '").append(end).append("'");
        return sb.toString();
    }

    // datetime 列的时间条件，结束日算到当天 23:59:59
    private String dateTimeRange(String col, String start, String end) {
        StringBuilder sb = new StringBuilder();
        if (isDate(start)) sb.append(" AND ").append(col).append(" >= '").append(start).append(" 00:00:00'");
        if (isDate(end)) sb.append(" AND ").append(col).append(" <= '").append(end).append(" 23:59:59'");
        return sb.toString();
    }

    // 工作日历：法定假日、调休工作日、周末是否计工作日
    private static class Cal {
        Set<LocalDate> statutory = new HashSet<>();
        Set<LocalDate> makeup = new HashSet<>();
        boolean weekendCount = false;
    }

    // 读系统参数，查不到用默认值
    private String sysParam(String key, String def) {
        try {
            List<Map<String, Object>> r = jdbc.queryForList(
                "SELECT param_value FROM system_params WHERE param_key=? AND deleted_at IS NULL", key);
            if (!r.isEmpty() && r.get(0).get("param_value") != null)
                return String.valueOf(r.get(0).get("param_value"));
        } catch (Exception ignored) {}
        return def;
    }

    // 库里的日期转成 LocalDate，兼容日期对象和字符串
    private LocalDate toLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate) return (LocalDate) o;
        if (o instanceof Date) return ((Date) o).toLocalDate();
        String str = String.valueOf(o);
        try {
            return LocalDate.parse(str.length() > 10 ? str.substring(0, 10) : str);
        } catch (Exception e) {
            return null;
        }
    }

    // 读区间内的节假日配置
    private Cal loadCal(LocalDate s, LocalDate e) {
        Cal cal = new Cal();
        cal.weekendCount = "1".equals(sysParam("attendance_weekend_count", "0"));
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT holiday_date, holiday_type FROM holidays WHERE deleted_at IS NULL AND holiday_date BETWEEN ? AND ?",
                Date.valueOf(s), Date.valueOf(e));
            for (Map<String, Object> r : rows) {
                LocalDate ld = toLocalDate(r.get("holiday_date"));
                if (ld == null) continue;
                Object t = r.get("holiday_type");
                int type = t == null ? 0 : ((Number) t).intValue();
                if (type == 2) cal.makeup.add(ld);
                else if (type == 1) cal.statutory.add(ld);
            }
        } catch (Exception ignored) {}
        return cal;
    }

    // 算区间里的工作日：调休算、周末默认不算、法定假日不算
    private long workdays(Cal cal, LocalDate s, LocalDate e) {
        long n = 0;
        LocalDate cur = s;
        while (!cur.isAfter(e)) {
            if (cal.makeup.contains(cur)) {
                n++;
            } else if (cur.getDayOfWeek().getValue() < 6 || cal.weekendCount) {
                if (!cal.statutory.contains(cur)) n++;
            }
            cur = cur.plusDays(1);
        }
        return n;
    }

    // 百分比，保留两位小数
    private double pct(long num, long den) {
        if (den <= 0) return 0;
        return Math.round(num * 10000.0 / den) / 100.0;
    }

    // 保留两位小数
    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // null 当 0 的 long
    private Long nl(Object o) { return o == null ? 0L : ((Number) o).longValue(); }

    // null 当 0 的 double
    private double nd(Object o) { return o == null ? 0 : ((Number) o).doubleValue(); }

    // 统一包装返回体
    private Map<String, Object> Result(Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", 200);
        m.put("message", "success");
        m.put("data", data);
        return m;
    }

    // 筛选下拉：部门、项目、可选人员
    @GetMapping("/meta")
    @RequirePermission("report:view")
    public Map<String, Object> meta(HttpServletRequest req) {
        Long uid = RequestUser.id(req);
        boolean admin = isAdmin(uid);
        boolean hr = isHr(uid);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("depts", jdbc.queryForList(
            "SELECT id, org_name AS name, parent_id FROM organizations WHERE deleted_at IS NULL ORDER BY id"));
        data.put("projects", jdbc.queryForList(
            "SELECT id, name FROM projects WHERE deleted_at IS NULL ORDER BY id"));
        // 只有管理员和主管能拿人员下拉，普通员工看不到全员名录
        data.put("users", admin || isDeptManager(uid)
            ? jdbc.queryForList("SELECT id, real_name AS name, org_id FROM users WHERE deleted_at IS NULL AND status=1 ORDER BY id")
            : new ArrayList<>());
        data.put("canFilterUser", admin);
        data.put("canFilterDept", admin || hr);
        return Result(data);
    }

    // 综合总览，一次返回四块
    @GetMapping("/overview")
    @RequirePermission("report:view")
    public Map<String, Object> overview(HttpServletRequest req,
                                        @RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) Long deptId,
                                        @RequestParam(required = false) Long userId,
                                        @RequestParam(required = false) Long projectId) {
        Long uid = RequestUser.id(req);
        LocalDate[] range = parseRange(startDate, endDate);
        LocalDate startD = range[0], endD = range[1];
        checkRange(startD, endD);
        String start = startD.format(D), endStr = endD.format(D);

        ScopeSet sc = resolveScopes(uid, deptId, userId);
        Cal cal = loadCal(startD, endD);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scope", sc.topScope);
        data.put("scope_name", sc.topName);
        data.put("start_date", start);
        data.put("end_date", endStr);
        data.put("task", taskSection(sc.task, start, endStr, projectId));
        data.put("attendance", attendanceSection(sc.attendance, start, endStr));
        data.put("worklog", worklogSection(sc.worklog, start, endStr, endD, cal));
        data.put("flow", flowSection(sc.flow, start, endStr));
        return Result(data);
    }

    // 任务板块：数量、状态分布、每日趋势、部门与人员排行
    private Map<String, Object> taskSection(Scope sc, String start, String end, Long projectId) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("scope", sc.scope);
        String in = inIds(sc.ids);
        String userScope = in == null ? "" : " AND (t.creator_id IN (" + in + ") OR t.assignee_id IN (" + in + ")) ";
        String proj = projectId != null ? " AND t.project_id = " + projectId + " " : "";
        String time = dateTimeRange("t.created_at", start, end);

        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT t.status, COUNT(*) c, " +
            "SUM(CASE WHEN t.is_overdue=1 THEN 1 ELSE 0 END) overdue, " +
            "COALESCE(SUM(t.plan_work_hours),0) plan_sum, " +
            "COALESCE(SUM(t.actual_work_hours),0) actual_sum, " +
            "SUM(CASE WHEN t.actual_work_hours IS NOT NULL AND t.plan_work_hours IS NOT NULL THEN 1 ELSE 0 END) diff_cnt, " +
            "COALESCE(SUM(CASE WHEN t.actual_work_hours IS NOT NULL AND t.plan_work_hours IS NOT NULL THEN t.plan_work_hours ELSE 0 END),0) plan_pair_sum, " +
            "COALESCE(SUM(CASE WHEN t.actual_work_hours IS NOT NULL AND t.plan_work_hours IS NOT NULL THEN t.actual_work_hours ELSE 0 END),0) actual_pair_sum " +
            "FROM tasks t WHERE t.deleted_at IS NULL " + time + userScope + proj + " GROUP BY t.status");

        long[] statusCount = new long[7];
        long total = 0, overdue = 0, diffCnt = 0;
        double planSum = 0, actualSum = 0, planPair = 0, actualPair = 0;
        for (Map<String, Object> r : rows) {
            int st = ((Number) r.get("status")).intValue();
            long c = nl(r.get("c"));
            if (st >= 0 && st < 7) statusCount[st] = c;
            total += c;
            overdue += nl(r.get("overdue"));
            planSum += nd(r.get("plan_sum"));
            actualSum += nd(r.get("actual_sum"));
            diffCnt += nl(r.get("diff_cnt"));
            planPair += nd(r.get("plan_pair_sum"));
            actualPair += nd(r.get("actual_pair_sum"));
        }
        long finished = statusCount[4];
        // 工时偏差只算计划和实际都填了的任务，分子分母口径才一致
        double avgDiff = diffCnt > 0 ? round2((actualPair - planPair) / diffCnt) : 0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("finished", finished);
        summary.put("completion_rate", pct(finished, total));
        summary.put("overdue", overdue);
        summary.put("overdue_rate", pct(overdue, total));
        summary.put("avg_plan_hours", total > 0 ? round2(planSum / total) : 0);
        summary.put("avg_actual_hours", total > 0 ? round2(actualSum / total) : 0);
        summary.put("avg_hour_diff", avgDiff);
        res.put("summary", summary);

        String[] statusNames = {"待分配", "待接受", "进行中", "待验收", "已完成", "已撤销", "已拒绝"};
        List<Map<String, Object>> statusDist = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            if (statusCount[i] > 0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", statusNames[i]);
                m.put("value", statusCount[i]);
                statusDist.add(m);
            }
        }
        res.put("status_dist", statusDist);

        // 趋势两条线各按自己的业务日期取数：新建看创建日期，完成看实际完成时间
        // 完成线按实际完成时间分天，跨月完成的任务才算对
        List<Map<String, Object>> createdRows = jdbc.queryForList(
            "SELECT DATE(t.created_at) d, COUNT(*) c FROM tasks t WHERE t.deleted_at IS NULL "
            + dateTimeRange("t.created_at", start, end) + userScope + proj + " GROUP BY DATE(t.created_at)");
        List<Map<String, Object>> finishedRows = jdbc.queryForList(
            "SELECT DATE(t.actual_end_time) d, COUNT(*) c FROM tasks t WHERE t.deleted_at IS NULL "
            + "AND t.status=4 AND t.actual_end_time IS NOT NULL "
            + dateTimeRange("t.actual_end_time", start, end) + userScope + proj
            + " GROUP BY DATE(t.actual_end_time)");
        // TreeMap 按日期升序，两条线出现过的日期都保留，缺的补 0
        Map<String, long[]> trendMap = new TreeMap<>();
        for (Map<String, Object> r : createdRows) {
            trendMap.computeIfAbsent(String.valueOf(r.get("d")), k -> new long[2])[0] = nl(r.get("c"));
        }
        for (Map<String, Object> r : finishedRows) {
            trendMap.computeIfAbsent(String.valueOf(r.get("d")), k -> new long[2])[1] = nl(r.get("c"));
        }
        List<Map<String, Object>> trendOut = new ArrayList<>();
        for (Map.Entry<String, long[]> e : trendMap.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", e.getKey());
            m.put("total", e.getValue()[0]);
            m.put("finished", e.getValue()[1]);
            trendOut.add(m);
        }
        res.put("trend", trendOut);

        // 部门排行只在全局或部门视图下给
        res.put("dept_rank", sc.isSelf() ? new ArrayList<>() : rankByDept(
            "SELECT o.org_name nm, COUNT(*) total, SUM(CASE WHEN t.status=4 THEN 1 ELSE 0 END) finished " +
            "FROM tasks t JOIN users u ON u.id=t.assignee_id " +
            "LEFT JOIN organizations o ON o.id=u.org_id " +
            "WHERE t.deleted_at IS NULL AND t.assignee_id IS NOT NULL " + time + userScope + proj +
            " GROUP BY o.id, o.org_name ORDER BY total DESC"));

        // 人员排行按负责人统计，取前十
        List<Map<String, Object>> userRank = jdbc.queryForList(
            "SELECT u.real_name nm, COUNT(*) total, SUM(CASE WHEN t.status=4 THEN 1 ELSE 0 END) finished " +
            "FROM tasks t JOIN users u ON u.id=t.assignee_id " +
            "WHERE t.deleted_at IS NULL AND t.assignee_id IS NOT NULL " + time + colIn("t.assignee_id", sc) + proj +
            " GROUP BY u.id, u.real_name ORDER BY total DESC LIMIT 10");
        res.put("user_rank", normalizeRank(userRank));
        return res;
    }

    // 部门排行，没有部门的归到未分配
    private List<Map<String, Object>> rankByDept(String sql) {
        List<Map<String, Object>> out = normalizeRank(jdbc.queryForList(sql));
        for (Map<String, Object> m : out) {
            if (m.get("name") == null) m.put("name", "未分配");
        }
        return out;
    }

    // 排行 SQL 结果整理成前端要的字段
    private List<Map<String, Object>> normalizeRank(List<Map<String, Object>> raw) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : raw) {
            Map<String, Object> m = new LinkedHashMap<>();
            long total = nl(r.get("total")), finished = nl(r.get("finished"));
            m.put("name", r.get("nm"));
            m.put("total", total);
            m.put("finished", finished);
            m.put("rate", pct(finished, total));
            out.add(m);
        }
        return out;
    }

    // 出勤板块：状态汇总、部门出勤率
    private Map<String, Object> attendanceSection(Scope sc, String start, String end) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("scope", sc.scope);
        String scope = colIn("a.user_id", sc);
        String time = dateRange("a.attendance_date", start, end);

        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT a.status, COUNT(*) c FROM attendance_records a " +
            "WHERE a.deleted_at IS NULL " + time + scope + " GROUP BY a.status");
        long[] byStatus = new long[10];
        long totalRecords = 0;
        for (Map<String, Object> r : rows) {
            int st = ((Number) r.get("status")).intValue();
            long c = nl(r.get("c"));
            if (st >= 0 && st < 10) byStatus[st] = c;
            totalRecords += c;
        }
        // 已到岗：正常、迟到、早退、严重迟到、未签退异常、严重早退
        long present = byStatus[0] + byStatus[1] + byStatus[2] + byStatus[7] + byStatus[8] + byStatus[9];
        long late = byStatus[1] + byStatus[7];
        long early = byStatus[2] + byStatus[9];
        long absent = byStatus[3];
        long abnormalSignout = byStatus[8];
        long outside = byStatus[4];
        long trip = byStatus[5];
        long leave = byStatus[6];
        // 应出勤人天 = 记录数扣除外出、出差、请假，这三种不用打卡
        long should = totalRecords - outside - trip - leave;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("should", should);
        summary.put("present", present);
        summary.put("late", late);
        summary.put("early", early);
        summary.put("absent", absent);
        summary.put("abnormal_signout", abnormalSignout);
        summary.put("outside", outside);
        summary.put("trip", trip);
        summary.put("leave", leave);
        summary.put("attendance_rate", pct(present, should));
        res.put("summary", summary);

        String[] names = {"正常", "迟到", "早退", "缺勤", "外出", "出差", "请假", "严重迟到", "未签退异常", "严重早退"};
        List<Map<String, Object>> dist = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (byStatus[i] > 0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", names[i]);
                m.put("value", byStatus[i]);
                dist.add(m);
            }
        }
        res.put("status_dist", dist);

        // 部门出勤率，分母同样扣掉外出、出差、请假
        List<Map<String, Object>> raw = jdbc.queryForList(
            "SELECT o.org_name nm, COUNT(*) total, " +
            "SUM(CASE WHEN a.status IN (0,1,2,7,8,9) THEN 1 ELSE 0 END) present, " +
            "SUM(CASE WHEN a.status IN (4,5,6) THEN 1 ELSE 0 END) special " +
            "FROM attendance_records a JOIN users u ON u.id=a.user_id " +
            "LEFT JOIN organizations o ON o.id=u.org_id " +
            "WHERE a.deleted_at IS NULL " + time + scope + " GROUP BY o.id, o.org_name ORDER BY total DESC");
        List<Map<String, Object>> dept = new ArrayList<>();
        for (Map<String, Object> r : raw) {
            Map<String, Object> m = new LinkedHashMap<>();
            long total = nl(r.get("total")), p = nl(r.get("present")), sp = nl(r.get("special"));
            long den = total - sp;
            m.put("name", r.get("nm") == null ? "未分配" : r.get("nm"));
            m.put("present", p);
            m.put("should", den);
            m.put("rate", pct(p, den));
            dept.add(m);
        }
        res.put("dept_rank", dept);
        return res;
    }

    // 日志板块：总量、提交时段、部门排名、评论活跃度
    private Map<String, Object> worklogSection(Scope sc, String start, String end, LocalDate endDate, Cal cal) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("scope", sc.scope);
        String scope = colIn("w.user_id", sc);
        String time = dateRange("w.log_date", start, end);

        Map<String, Object> agg = jdbc.queryForMap(
            "SELECT COUNT(*) total, COUNT(DISTINCT w.user_id) active_users, " +
            "COUNT(DISTINCT CONCAT(w.user_id,'-',w.log_date)) writing_days, " +
            "COALESCE(SUM(CHAR_LENGTH(w.content)),0) total_words " +
            "FROM worklogs w WHERE w.deleted_at IS NULL " + time + scope);
        long total = nl(agg.get("total"));
        long activeUsers = nl(agg.get("active_users"));
        long writingDays = nl(agg.get("writing_days"));
        long totalWords = nl(agg.get("total_words"));

        // 范围内在职员工数，日志完成率的分母
        long userCount;
        if (sc.ids == null) {
            userCount = nl(jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND status=1", Long.class));
        } else if (sc.ids.isEmpty()) {
            userCount = 0;
        } else {
            userCount = nl(jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND status=1 AND id IN (" + inIds(sc.ids) + ")",
                Long.class));
        }
        long wd = workdays(cal, LocalDate.parse(start, D), endDate);
        long shouldWrite = userCount * wd;

        // 范围内日志收到的评论数，软删的不算
        long commentCount = 0;
        try {
            commentCount = nl(jdbc.queryForObject(
                "SELECT COUNT(*) FROM worklog_comments wc JOIN worklogs w ON w.id=wc.worklog_id " +
                "WHERE wc.deleted_at IS NULL AND w.deleted_at IS NULL " +
                dateRange("w.log_date", start, end) + scope, Long.class));
        } catch (Exception ignored) {}

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("active_users", activeUsers);
        summary.put("writing_days", writingDays);
        summary.put("total_words", totalWords);
        summary.put("avg_words", total > 0 ? totalWords / total : 0);
        summary.put("completion_rate", pct(writingDays, shouldWrite));
        summary.put("workdays", wd);
        summary.put("comment_count", commentCount);
        res.put("summary", summary);

        // 提交时段：上午、下午、晚上
        List<Map<String, Object>> raw = jdbc.queryForList(
            "SELECT CASE WHEN HOUR(w.submit_time) < 12 THEN '上午' " +
            "WHEN HOUR(w.submit_time) < 18 THEN '下午' ELSE '晚上' END nm, COUNT(*) c " +
            "FROM worklogs w WHERE w.deleted_at IS NULL AND w.submit_time IS NOT NULL " +
            dateTimeRange("w.submit_time", start, end) + scope + " GROUP BY nm");
        Map<String, Long> byPeriod = new HashMap<>();
        for (Map<String, Object> r : raw) byPeriod.put(String.valueOf(r.get("nm")), nl(r.get("c")));
        List<Map<String, Object>> timeDist = new ArrayList<>();
        for (String p : new String[]{"上午", "下午", "晚上"}) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", p);
            m.put("value", byPeriod.getOrDefault(p, 0L));
            timeDist.add(m);
        }
        res.put("time_dist", timeDist);

        // 部门日志排名
        List<Map<String, Object>> deptRaw = jdbc.queryForList(
            "SELECT o.org_name nm, COUNT(*) total, COUNT(DISTINCT w.user_id) users, " +
            "COUNT(DISTINCT CONCAT(w.user_id,'-',w.log_date)) days " +
            "FROM worklogs w JOIN users u ON u.id=w.user_id " +
            "LEFT JOIN organizations o ON o.id=u.org_id " +
            "WHERE w.deleted_at IS NULL " + time + scope +
            " GROUP BY o.id, o.org_name ORDER BY total DESC");
        List<Map<String, Object>> dept = new ArrayList<>();
        for (Map<String, Object> r : deptRaw) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r.get("nm") == null ? "未分配" : r.get("nm"));
            m.put("count", nl(r.get("total")));
            m.put("users", nl(r.get("users")));
            m.put("days", nl(r.get("days")));
            dept.add(m);
        }
        res.put("dept_rank", dept);

        // 发表评论最多的人
        List<Map<String, Object>> cuRaw = jdbc.queryForList(
            "SELECT cu.real_name nm, COUNT(*) c FROM worklog_comments wc " +
            "JOIN worklogs w ON w.id=wc.worklog_id " +
            "JOIN users cu ON cu.id=wc.user_id " +
            "WHERE wc.deleted_at IS NULL AND w.deleted_at IS NULL " + dateRange("w.log_date", start, end) + scope +
            " GROUP BY cu.id, cu.real_name ORDER BY c DESC LIMIT 10");
        List<Map<String, Object>> commentUsers = new ArrayList<>();
        for (Map<String, Object> r : cuRaw) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r.get("nm"));
            m.put("value", nl(r.get("c")));
            commentUsers.add(m);
        }
        res.put("comment_user_rank", commentUsers);

        // 被评论最多的日志，标题太长截断
        List<Map<String, Object>> clRaw = jdbc.queryForList(
            "SELECT w.id wid, w.log_date d, w.title title, au.real_name author, COUNT(*) c " +
            "FROM worklog_comments wc " +
            "JOIN worklogs w ON w.id=wc.worklog_id " +
            "JOIN users au ON au.id=w.user_id " +
            "WHERE wc.deleted_at IS NULL AND w.deleted_at IS NULL " + dateRange("w.log_date", start, end) + scope +
            " GROUP BY w.id, w.log_date, w.title, au.real_name ORDER BY c DESC LIMIT 10");
        List<Map<String, Object>> commentLogs = new ArrayList<>();
        for (Map<String, Object> r : clRaw) {
            Map<String, Object> m = new LinkedHashMap<>();
            String title = r.get("title") == null ? "" : String.valueOf(r.get("title"));
            if (title.length() > 12) title = title.substring(0, 12) + "…";
            m.put("name", String.valueOf(r.get("d")) + " " + (r.get("author") == null ? "" : r.get("author")) + " " + title);
            m.put("value", nl(r.get("c")));
            commentLogs.add(m);
        }
        res.put("comment_log_rank", commentLogs);
        return res;
    }

    // 流程板块：状态汇总、平均审批时长、类型分布
    private Map<String, Object> flowSection(Scope sc, String start, String end) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("scope", sc.scope);
        String scope = colIn("i.creator_id", sc);
        String time = dateTimeRange("i.submit_time", start, end);

        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT i.status, COUNT(*) c FROM flow_instances i " +
            "WHERE i.deleted_at IS NULL AND i.submit_time IS NOT NULL " + time + scope + " GROUP BY i.status");
        long[] byStatus = new long[5];
        long total = 0;
        for (Map<String, Object> r : rows) {
            int st = ((Number) r.get("status")).intValue();
            long c = nl(r.get("c"));
            if (st >= 0 && st < 5) byStatus[st] = c;
            total += c;
        }
        long approving = byStatus[1], approved = byStatus[2], rejected = byStatus[3], revoked = byStatus[4];

        // 平均审批时长只算已通过、已驳回且有结束时间的单子
        Double avgMin = jdbc.queryForObject(
            "SELECT AVG(TIMESTAMPDIFF(MINUTE, i.submit_time, i.finish_time)) " +
            "FROM flow_instances i WHERE i.deleted_at IS NULL AND i.submit_time IS NOT NULL " +
            "AND i.finish_time IS NOT NULL AND i.status IN (2,3) " + scope +
            dateTimeRange("i.submit_time", start, end), Double.class);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("approving", approving);
        summary.put("approved", approved);
        summary.put("rejected", rejected);
        summary.put("revoked", revoked);
        summary.put("approval_rate", pct(approved, approved + rejected));
        summary.put("avg_approve_minutes", avgMin == null ? 0 : round2(avgMin));
        res.put("summary", summary);

        List<Map<String, Object>> dist = new ArrayList<>();
        String[] names = {"草稿", "审批中", "已通过", "已驳回", "已撤销"};
        for (int i = 0; i < 5; i++) {
            if (byStatus[i] > 0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", names[i]);
                m.put("value", byStatus[i]);
                dist.add(m);
            }
        }
        res.put("status_dist", dist);

        // 按流程类型统计数量和通过率
        List<Map<String, Object>> typeRaw = jdbc.queryForList(
            "SELECT d.flow_name nm, COUNT(*) total, SUM(CASE WHEN i.status=2 THEN 1 ELSE 0 END) approved " +
            "FROM flow_instances i LEFT JOIN flow_definitions d ON d.id=i.definition_id " +
            "WHERE i.deleted_at IS NULL AND i.submit_time IS NOT NULL " + time + scope +
            " GROUP BY i.definition_id, d.flow_name ORDER BY total DESC");
        List<Map<String, Object>> types = new ArrayList<>();
        for (Map<String, Object> r : typeRaw) {
            Map<String, Object> m = new LinkedHashMap<>();
            long t = nl(r.get("total")), ap = nl(r.get("approved"));
            m.put("name", r.get("nm") == null ? "未分类" : r.get("nm"));
            m.put("total", t);
            m.put("approved", ap);
            m.put("rate", pct(ap, t));
            types.add(m);
        }
        res.put("type_dist", types);
        return res;
    }

    // 导出 Excel，路径上的 section 决定导哪块
    @GetMapping("/export/{section}")
    @RequirePermission("report:view")
    public void export(@PathVariable("section") String section,
                       HttpServletRequest req, HttpServletResponse resp,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       @RequestParam(required = false) Long deptId,
                       @RequestParam(required = false) Long userId,
                       @RequestParam(required = false) Long projectId) {
        Long uid = RequestUser.id(req);
        LocalDate[] range = parseRange(startDate, endDate);
        LocalDate startD = range[0], endD = range[1];
        // 校验放在写响应头之前，日期反了返回 JSON 报错
        checkRange(startD, endD);
        String start = startD.format(D), endStr = endD.format(D);

        ScopeSet sc = resolveScopes(uid, deptId, userId);

        Scope target;
        String title;
        Map<String, Object> data;
        switch (section) {
            case "task":
                target = sc.task;
                title = "任务统计";
                data = taskSection(target, start, endStr, projectId);
                break;
            case "attendance":
                target = sc.attendance;
                title = "出勤统计";
                data = attendanceSection(target, start, endStr);
                break;
            case "worklog":
                target = sc.worklog;
                title = "工作日志统计";
                data = worklogSection(target, start, endStr, endD, loadCal(startD, endD));
                break;
            case "flow":
                target = sc.flow;
                title = "流程审批统计";
                data = flowSection(target, start, endStr);
                break;
            default:
                resp.setStatus(404);
                return;
        }
        data.put("start_date", start);
        data.put("end_date", endStr);

        List<Map<String, Object>> logDetail = "worklog".equals(section)
            ? worklogExportDetail(target, start, endStr) : new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook()) {
            int rowsWritten = fillWorkbook(wb, section, data, target, logDetail);
            String fileName = title + "_" + start + "_" + endStr + ".xlsx";
            logExport(uid, section, start, endStr, deptId, userId, projectId, rowsWritten);

            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resp.setCharacterEncoding("UTF-8");
            String enc = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            resp.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + enc);
            try (OutputStream os = resp.getOutputStream()) {
                ExcelSafe.sanitize(wb);
            wb.write(os);
                os.flush();
            }
        } catch (Exception e) {
            resp.setStatus(500);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    // 日志导出明细：用户名、部门、日期、标题、字数、评论数，最多 5000 条
    private List<Map<String, Object>> worklogExportDetail(Scope sc, String start, String end) {
        return jdbc.queryForList(
            "SELECT u.real_name uname, o.org_name dept, w.log_date d, w.title title, w.content content, " +
            "CHAR_LENGTH(w.content) words, " +
            "(SELECT COUNT(*) FROM worklog_comments wc WHERE wc.worklog_id=w.id AND wc.deleted_at IS NULL) comments " +
            "FROM worklogs w JOIN users u ON u.id=w.user_id " +
            "LEFT JOIN organizations o ON o.id=u.org_id " +
            "WHERE w.deleted_at IS NULL " + dateRange("w.log_date", start, end) + colIn("w.user_id", sc) +
            " ORDER BY w.log_date DESC, w.id DESC LIMIT 5000");
    }

    // 记一条导出日志：导出人、条件、条数
    private void logExport(Long uid, String section, String start, String end,
                           Long deptId, Long userId, Long projectId, int rows) {
        try {
            String username = jdbc.queryForObject("SELECT username FROM users WHERE id=?", String.class, uid);
            // 手拼 JSON 遇到引号和 null 会出错，先放 Map 再交给 fastjson2 序列化
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("start", start);
            params.put("end", end);
            params.put("deptId", deptId);
            params.put("userId", userId);
            params.put("projectId", projectId);
            jdbc.update(
                "INSERT INTO operation_logs(user_id,username,module,action,method,url,target_type," +
                "request_params,response_result,status,cost_time,created_at,updated_at) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                uid, username, "report", "export", "GET", "/api/report/export/" + section, section,
                JSON.toJSONString(params), "导出" + rows + "行", 1, 0);
        } catch (Exception ignored) {}
    }

    // 按板块把统计写进 Excel，返回写了多少行
    private int fillWorkbook(Workbook wb, String section, Map<String, Object> data,
                             Scope sc, List<Map<String, Object>> logDetail) {
        int[] rows = {0};
        switch (section) {
            case "task": {
                Map<String, Object> s = (Map<String, Object>) data.get("summary");
                sheet(wb, "任务汇总", new Object[][]{
                    {"统计范围", startEnd(data)},
                    {"任务总数", s.get("total")}, {"已完成", s.get("finished")},
                    {"完成率(%)", s.get("completion_rate")}, {"逾期数", s.get("overdue")},
                    {"逾期率(%)", s.get("overdue_rate")}, {"平均计划工时(h)", s.get("avg_plan_hours")},
                    {"平均实际工时(h)", s.get("avg_actual_hours")}, {"平均工时偏差(h)", s.get("avg_hour_diff")}
                }, null, null, rows);
                sheet(wb, "状态分布", null, new String[]{"状态", "数量"},
                    toRows((List<Map<String, Object>>) data.get("status_dist"), "name", "value"), rows);
                sheet(wb, "人员排行", null, new String[]{"姓名", "任务数", "已完成", "完成率(%)"},
                    rankRows((List<Map<String, Object>>) data.get("user_rank")), rows);
                if (!sc.isSelf())
                    sheet(wb, "部门排行", null, new String[]{"部门", "任务数", "已完成", "完成率(%)"},
                        rankRows((List<Map<String, Object>>) data.get("dept_rank")), rows);
                break;
            }
            case "attendance": {
                Map<String, Object> s = (Map<String, Object>) data.get("summary");
                sheet(wb, "出勤汇总", new Object[][]{
                    {"统计范围", startEnd(data)},
                    {"应出勤(人天)", s.get("should")}, {"实际出勤(人天)", s.get("present")},
                    {"出勤率(%)", s.get("attendance_rate")}, {"迟到", s.get("late")},
                    {"早退", s.get("early")}, {"缺勤", s.get("absent")},
                    {"未签退异常", s.get("abnormal_signout")},
                    {"外出", s.get("outside")}, {"出差", s.get("trip")}, {"请假", s.get("leave")}
                }, null, null, rows);
                sheet(wb, "状态分布", null, new String[]{"状态", "数量"},
                    toRows((List<Map<String, Object>>) data.get("status_dist"), "name", "value"), rows);
                if (!sc.isSelf())
                    sheet(wb, "部门出勤", null, new String[]{"部门", "实际出勤", "应出勤", "出勤率(%)"},
                        deptAttRows((List<Map<String, Object>>) data.get("dept_rank")), rows);
                break;
            }
            case "worklog": {
                Map<String, Object> s = (Map<String, Object>) data.get("summary");
                sheet(wb, "日志汇总", new Object[][]{
                    {"统计范围", startEnd(data)},
                    {"日志总数", s.get("total")}, {"撰写人数", s.get("active_users")},
                    {"撰写人天", s.get("writing_days")}, {"工作日数", s.get("workdays")},
                    {"日志完成率(%)", s.get("completion_rate")},
                    {"总字数", s.get("total_words")}, {"平均每篇(字)", s.get("avg_words")},
                    {"评论数", s.get("comment_count")}
                }, null, null, rows);
                if (!sc.isSelf())
                    sheet(wb, "部门撰写", null, new String[]{"部门", "日志数", "撰写人数", "撰写人天"},
                        deptLogRows((List<Map<String, Object>>) data.get("dept_rank")), rows);
                sheet(wb, "评论活跃人", null, new String[]{"用户", "评论数"},
                    toRows((List<Map<String, Object>>) data.get("comment_user_rank"), "name", "value"), rows);
                sheet(wb, "热门日志", null, new String[]{"日志", "评论数"},
                    toRows((List<Map<String, Object>>) data.get("comment_log_rank"), "name", "value"), rows);
                List<Object[]> detail = new ArrayList<>();
                for (Map<String, Object> r : logDetail) {
                    detail.add(new Object[]{
                        str(r.get("uname")), str(r.get("dept")), str(r.get("d")), str(r.get("title")),
                        str(r.get("content")), nl(r.get("words")), nl(r.get("comments"))});
                }
                sheet(wb, "日志明细", null,
                    new String[]{"用户名", "部门", "日志日期", "标题", "内容", "字数", "评论数"},
                    detail, rows);
                break;
            }
            default: {
                Map<String, Object> s = (Map<String, Object>) data.get("summary");
                sheet(wb, "流程汇总", new Object[][]{
                    {"统计范围", startEnd(data)},
                    {"流程总数", s.get("total")}, {"审批中", s.get("approving")},
                    {"已通过", s.get("approved")}, {"已驳回", s.get("rejected")},
                    {"已撤销", s.get("revoked")}, {"通过率(%)", s.get("approval_rate")},
                    {"平均审批时长(分钟)", s.get("avg_approve_minutes")}
                }, null, null, rows);
                sheet(wb, "类型分布", null, new String[]{"流程类型", "总数", "已通过", "通过率(%)"},
                    typeRows((List<Map<String, Object>>) data.get("type_dist")), rows);
                break;
            }
        }
        return rows[0];
    }

    // 汇总页里统计范围那一格的文字
    private String startEnd(Map<String, Object> data) {
        return data.get("scope") + " | " + data.get("start_date") + " ~ " + data.get("end_date");
    }

    // null 转空串
    private String str(Object o) { return o == null ? "" : String.valueOf(o); }

    // 两列数据（名称、数量）转 Excel 行
    private List<Object[]> toRows(List<Map<String, Object>> list, String k1, String k2) {
        List<Object[]> out = new ArrayList<>();
        if (list != null) for (Map<String, Object> r : list)
            out.add(new Object[]{str(r.get(k1)), nl(r.get(k2))});
        return out;
    }

    // 任务类排行转 Excel 行
    private List<Object[]> rankRows(List<Map<String, Object>> list) {
        List<Object[]> out = new ArrayList<>();
        if (list != null) for (Map<String, Object> r : list)
            out.add(new Object[]{str(r.get("name")), nl(r.get("total")), nl(r.get("finished")), nd(r.get("rate"))});
        return out;
    }

    // 流程类型分布转 Excel 行
    private List<Object[]> typeRows(List<Map<String, Object>> list) {
        List<Object[]> out = new ArrayList<>();
        if (list != null) for (Map<String, Object> r : list)
            out.add(new Object[]{str(r.get("name")), nl(r.get("total")), nl(r.get("approved")), nd(r.get("rate"))});
        return out;
    }

    // 部门出勤排行转 Excel 行
    private List<Object[]> deptAttRows(List<Map<String, Object>> list) {
        List<Object[]> out = new ArrayList<>();
        if (list != null) for (Map<String, Object> r : list)
            out.add(new Object[]{str(r.get("name")), nl(r.get("present")), nl(r.get("should")), nd(r.get("rate"))});
        return out;
    }

    // 部门日志排行转 Excel 行
    private List<Object[]> deptLogRows(List<Map<String, Object>> list) {
        List<Object[]> out = new ArrayList<>();
        if (list != null) for (Map<String, Object> r : list)
            out.add(new Object[]{str(r.get("name")), nl(r.get("count")), nl(r.get("users")), nl(r.get("days"))});
        return out;
    }

    // 建一个 sheet：先写键值汇总，空一行再写表头和明细
    private void sheet(Workbook wb, String name, Object[][] summary, String[] headers,
                       List<Object[]> rows, int[] counter) {
        Sheet s = wb.createSheet(name);
        int r = 0;
        CellStyle bold = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        bold.setFont(f);
        if (summary != null) {
            for (Object[] kv : summary) {
                Row row = s.createRow(r++);
                Cell k = row.createCell(0);
                k.setCellValue(str(kv[0]));
                k.setCellStyle(bold);
                setCell(row.createCell(1), kv[1]);
            }
            r++;
            counter[0] += summary.length;
        }
        if (headers != null) {
            CellStyle head = wb.createCellStyle();
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            head.setFont(f);
            Row hr = s.createRow(r++);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(head);
            }
            if (rows != null) {
                for (Object[] rr : rows) {
                    Row row = s.createRow(r++);
                    for (int i = 0; i < rr.length; i++) setCell(row.createCell(i), rr[i]);
                }
                counter[0] += rows.size();
            }
        }
        for (int i = 0; i < 8; i++) s.setColumnWidth(i, 18 * 256);
        // 第一列放名称或内容，放宽一点
        s.setColumnWidth(0, 20 * 256);
    }

    // 写单元格，数字按数字写，文本超上限就截断，否则整个导出会失败
    private void setCell(Cell c, Object v) {
        if (v == null) {
            c.setCellValue("");
            return;
        }
        if (v instanceof Number) {
            c.setCellValue(((Number) v).doubleValue());
            return;
        }
        String text = String.valueOf(v);
        // Excel 限字符数，substring 按 char 截会把 emoji 截成半个，这里按码点截
        if (text.codePointCount(0, text.length()) > MAX_CELL_TEXT) {
            text = text.substring(0, text.offsetByCodePoints(0, MAX_CELL_TEXT));
        }
        c.setCellValue(text);
    }
}
