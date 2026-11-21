package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.ExceptionLogs;
import com.officeautomationsystemapi.entity.Menus;
import com.officeautomationsystemapi.entity.NavImages;
import com.officeautomationsystemapi.entity.OperationLogs;
import com.officeautomationsystemapi.entity.SystemParams;
import com.officeautomationsystemapi.repository.ExceptionLogsRepository;
import com.officeautomationsystemapi.repository.MenusRepository;
import com.officeautomationsystemapi.repository.NavImagesRepository;
import com.officeautomationsystemapi.repository.OperationLogsRepository;
import com.officeautomationsystemapi.repository.SystemParamsRepository;
import com.officeautomationsystemapi.service.SensitiveDataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 系统管理：参数、菜单权限、导航图片、操作日志、异常日志
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private SensitiveDataService sensitiveDataService;

    @Autowired
    private SystemParamsRepository paramsRepository;

    @Autowired
    private MenusRepository menusRepository;

    @Autowired
    private NavImagesRepository navImagesRepository;

    @Autowired
    private OperationLogsRepository operationLogsRepository;

    @Autowired
    private ExceptionLogsRepository exceptionLogsRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // 这些字段不能原样写进日志
    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i)(\"?(?:password|passwd|pwd|token|access_token|refresh_token|secret|authorization|captcha)\"?\\s*[:=]\\s*\"?)([^\",}\\s]+)");

    // 系统参数

    @GetMapping("/params")
    @RequirePermission("system:param")
    public Result<List<Map<String, Object>>> listParams(
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String keyword) {
        QueryWrapper<SystemParams> wrapper = new QueryWrapper<>();
        if (group != null && !group.trim().isEmpty()) {
            wrapper.eq("param_group", group.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("param_key", kw).or().like("param_name", kw).or().like("remark", kw));
        }
        wrapper.orderByAsc("param_group").orderByAsc("sort_order").orderByAsc("id");
        List<SystemParams> list = paramsRepository.selectList(wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SystemParams p : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("paramKey", p.getParam_key());
            row.put("paramValue", p.getParam_value());
            row.put("paramName", p.getParam_name());
            row.put("paramGroup", p.getParam_group());
            row.put("paramType", p.getParam_type());
            row.put("sortOrder", p.getSort_order());
            row.put("status", p.getStatus());
            row.put("remark", p.getRemark());
            row.put("updatedAt", p.getUpdated_at());
            rows.add(row);
        }
        return Result.success(rows);
    }

    @PostMapping("/params")
    @RequirePermission("system:param")
    public Result<Map<String, Object>> createParam(@RequestBody SystemParams body) {
        String key = body.getParam_key() == null ? "" : body.getParam_key().trim();
        if (key.isEmpty()) {
            return Result.error("参数键不能为空");
        }
        Long exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_params WHERE param_key = ? AND deleted_at IS NULL", Long.class, key);
        if (exists != null && exists > 0) {
            return Result.error("参数键 " + key + " 已存在");
        }
        body.setParam_key(key);
        body.setParam_type(body.getParam_type() == null ? 1 : body.getParam_type());
        body.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        body.setSort_order(body.getSort_order() == null ? 0 : body.getSort_order());
        body.setCreated_at(LocalDateTime.now());
        body.setUpdated_at(LocalDateTime.now());
        paramsRepository.insert(body);
        Map<String, Object> data = new HashMap<>();
        data.put("id", body.getId());
        return Result.success(data);
    }

    @PutMapping("/params/{id}")
    @RequirePermission("system:param")
    public Result<?> updateParam(@PathVariable Long id, @RequestBody SystemParams body) {
        SystemParams param = paramsRepository.selectById(id);
        if (param == null) {
            return Result.error(404, "参数不存在或已删除");
        }
        if (body.getParam_value() != null) {
            param.setParam_value(body.getParam_value());
        }
        if (body.getParam_name() != null) {
            param.setParam_name(body.getParam_name());
        }
        if (body.getParam_group() != null) {
            param.setParam_group(body.getParam_group());
        }
        if (body.getParam_type() != null) {
            param.setParam_type(body.getParam_type());
        }
        if (body.getSort_order() != null) {
            param.setSort_order(body.getSort_order());
        }
        if (body.getStatus() != null) {
            param.setStatus(body.getStatus());
        }
        if (body.getRemark() != null) {
            param.setRemark(body.getRemark());
        }
        param.setUpdated_at(LocalDateTime.now());
        paramsRepository.updateById(param);
        return Result.success(null);
    }

    @DeleteMapping("/params/{id}")
    @RequirePermission("system:param")
    public Result<?> deleteParam(@PathVariable Long id) {
        if (paramsRepository.selectById(id) == null) {
            return Result.error(404, "参数不存在或已删除");
        }
        paramsRepository.deleteById(id);
        return Result.success(null);
    }

    // 菜单与权限码

    // 菜单按树形返回，带父级名称
    @GetMapping("/menus")
    @RequirePermission("system:menu")
    public Result<List<Map<String, Object>>> listMenus(@RequestParam(required = false) String keyword) {
        List<Menus> all = menusRepository.selectList(new QueryWrapper<Menus>()
                .orderByAsc("sort_order").orderByAsc("id"));
        Map<Long, String> names = new HashMap<>();
        for (Menus m : all) {
            names.put(m.getId(), m.getMenu_name());
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Menus m : all) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim();
                boolean hit = (m.getMenu_name() != null && m.getMenu_name().contains(kw))
                        || (m.getMenu_code() != null && m.getMenu_code().contains(kw))
                        || (m.getPath() != null && m.getPath().contains(kw));
                if (!hit) {
                    continue;
                }
            }
            rows.add(menuRow(m, names));
        }
        return Result.success(rows);
    }

    @PostMapping("/menus")
    @RequirePermission("system:menu")
    public Result<Map<String, Object>> createMenu(@RequestBody Menus body) {
        if (body.getMenu_name() == null || body.getMenu_name().trim().isEmpty()) {
            return Result.error("菜单名称不能为空");
        }
        if (body.getMenu_code() != null && !body.getMenu_code().trim().isEmpty()) {
            Long exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM menus WHERE menu_code = ? AND deleted_at IS NULL", Long.class, body.getMenu_code().trim());
            if (exists != null && exists > 0) {
                return Result.error("权限标识 " + body.getMenu_code() + " 已存在");
            }
        }
        body.setParent_id(body.getParent_id() == null ? 0L : body.getParent_id());
        body.setMenu_type(body.getMenu_type() == null ? 3 : body.getMenu_type());
        body.setIs_nav(body.getIs_nav() == null ? 0 : body.getIs_nav());
        body.setSort_order(body.getSort_order() == null ? 0 : body.getSort_order());
        body.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        body.setVisible(body.getVisible() == null ? 1 : body.getVisible());
        body.setCreated_at(LocalDateTime.now());
        body.setUpdated_at(LocalDateTime.now());
        menusRepository.insert(body);
        Map<String, Object> data = new HashMap<>();
        data.put("id", body.getId());
        return Result.success(data);
    }

    @PutMapping("/menus/{id}")
    @RequirePermission("system:menu")
    public Result<?> updateMenu(@PathVariable Long id, @RequestBody Menus body) {
        Menus menu = menusRepository.selectById(id);
        if (menu == null) {
            return Result.error(404, "菜单不存在或已删除");
        }
        if (body.getParent_id() != null) {
            if (body.getParent_id().equals(id)) {
                return Result.error("上级菜单不能是自己");
            }
            menu.setParent_id(body.getParent_id());
        }
        if (body.getMenu_name() != null) {
            menu.setMenu_name(body.getMenu_name());
        }
        if (body.getMenu_code() != null && !body.getMenu_code().equals(menu.getMenu_code())) {
            Long exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM menus WHERE menu_code = ? AND id <> ? AND deleted_at IS NULL",
                    Long.class, body.getMenu_code().trim(), id);
            if (exists != null && exists > 0) {
                return Result.error("权限标识 " + body.getMenu_code() + " 已存在");
            }
            menu.setMenu_code(body.getMenu_code().trim());
        }
        if (body.getMenu_type() != null) {
            menu.setMenu_type(body.getMenu_type());
        }
        if (body.getPath() != null) {
            menu.setPath(body.getPath());
        }
        if (body.getComponent() != null) {
            menu.setComponent(body.getComponent());
        }
        if (body.getIcon() != null) {
            menu.setIcon(body.getIcon());
        }
        if (body.getIs_nav() != null) {
            menu.setIs_nav(body.getIs_nav());
        }
        if (body.getSort_order() != null) {
            menu.setSort_order(body.getSort_order());
        }
        if (body.getStatus() != null) {
            menu.setStatus(body.getStatus());
        }
        if (body.getVisible() != null) {
            menu.setVisible(body.getVisible());
        }
        menu.setUpdated_at(LocalDateTime.now());
        menusRepository.updateById(menu);
        return Result.success(null);
    }

    // 有子菜单的不能删
    @DeleteMapping("/menus/{id}")
    @RequirePermission("system:menu")
    public Result<?> deleteMenu(@PathVariable Long id) {
        if (menusRepository.selectById(id) == null) {
            return Result.error(404, "菜单不存在或已删除");
        }
        Long children = jdbc.queryForObject(
                "SELECT COUNT(*) FROM menus WHERE parent_id = ? AND deleted_at IS NULL", Long.class, id);
        if (children != null && children > 0) {
            return Result.error("该菜单下还有 " + children + " 个子菜单，请先删除子菜单");
        }
        jdbc.update("DELETE FROM role_permissions WHERE menu_id = ?", id);
        menusRepository.deleteById(id);
        return Result.success(null);
    }

    // 导航图片

    @GetMapping("/nav-images")
    @RequirePermission("system:menu")
    public Result<List<Map<String, Object>>> listNavImages(@RequestParam(required = false) String position) {
        QueryWrapper<NavImages> wrapper = new QueryWrapper<>();
        if (position != null && !position.trim().isEmpty()) {
            wrapper.eq("position", position.trim());
        }
        wrapper.orderByAsc("sort_order").orderByDesc("id");
        List<NavImages> list = navImagesRepository.selectList(wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (NavImages n : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", n.getId());
            row.put("title", n.getTitle());
            row.put("imageUrl", n.getImage_url());
            row.put("linkUrl", n.getLink_url());
            row.put("position", n.getPosition());
            row.put("sortOrder", n.getSort_order());
            row.put("status", n.getStatus());
            row.put("startTime", n.getStart_time());
            row.put("endTime", n.getEnd_time());
            row.put("clickCount", n.getClick_count());
            row.put("remark", n.getRemark());
            rows.add(row);
        }
        return Result.success(rows);
    }

    @PostMapping("/nav-images")
    @RequirePermission("system:menu")
    public Result<Map<String, Object>> createNavImage(@RequestBody NavImages body, HttpServletRequest request) {
        if (body.getImage_url() == null || body.getImage_url().trim().isEmpty()) {
            return Result.error("图片地址不能为空");
        }
        body.setPosition(body.getPosition() == null || body.getPosition().trim().isEmpty() ? "banner" : body.getPosition().trim());
        body.setSort_order(body.getSort_order() == null ? 0 : body.getSort_order());
        body.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        body.setClick_count(0);
        body.setUploader_id(RequestUser.id(request));
        body.setCreated_at(LocalDateTime.now());
        body.setUpdated_at(LocalDateTime.now());
        navImagesRepository.insert(body);
        Map<String, Object> data = new HashMap<>();
        data.put("id", body.getId());
        return Result.success(data);
    }

    @PutMapping("/nav-images/{id}")
    @RequirePermission("system:menu")
    public Result<?> updateNavImage(@PathVariable Long id, @RequestBody NavImages body) {
        NavImages image = navImagesRepository.selectById(id);
        if (image == null) {
            return Result.error(404, "导航图片不存在或已删除");
        }
        if (body.getTitle() != null) {
            image.setTitle(body.getTitle());
        }
        if (body.getImage_url() != null) {
            image.setImage_url(body.getImage_url());
        }
        if (body.getLink_url() != null) {
            image.setLink_url(body.getLink_url());
        }
        if (body.getPosition() != null) {
            image.setPosition(body.getPosition());
        }
        if (body.getSort_order() != null) {
            image.setSort_order(body.getSort_order());
        }
        if (body.getStatus() != null) {
            image.setStatus(body.getStatus());
        }
        if (body.getStart_time() != null) {
            image.setStart_time(body.getStart_time());
        }
        if (body.getEnd_time() != null) {
            image.setEnd_time(body.getEnd_time());
        }
        if (body.getRemark() != null) {
            image.setRemark(body.getRemark());
        }
        image.setUpdated_at(LocalDateTime.now());
        navImagesRepository.updateById(image);
        return Result.success(null);
    }

    @DeleteMapping("/nav-images/{id}")
    @RequirePermission("system:menu")
    public Result<?> deleteNavImage(@PathVariable Long id) {
        if (navImagesRepository.selectById(id) == null) {
            return Result.error(404, "导航图片不存在或已删除");
        }
        navImagesRepository.deleteById(id);
        return Result.success(null);
    }

    // 操作日志

    @GetMapping("/operation-logs")
    @RequirePermission("log:operation:view")
    public Result<Map<String, Object>> operationLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        pageNum = normalizePageNum(pageNum);
        pageSize = normalizePageSize(pageSize);
        QueryWrapper<OperationLogs> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("username", kw).or().like("url", kw).or().like("target_type", kw));
        }
        if (module != null && !module.trim().isEmpty()) {
            wrapper.eq("module", module.trim());
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);
        if (start != null) {
            wrapper.ge("created_at", start);
        }
        if (end != null) {
            wrapper.le("created_at", end);
        }
        wrapper.orderByDesc("id");
        Page<OperationLogs> page = operationLogsRepository.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OperationLogs log : page.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", log.getId());
            row.put("username", log.getUsername());
            row.put("module", log.getModule());
            row.put("action", log.getAction());
            row.put("method", log.getMethod());
            row.put("url", log.getUrl());
            row.put("ipAddress", log.getIp_address());
            row.put("targetType", log.getTarget_type());
            row.put("targetId", log.getTarget_id());
            row.put("requestParams", sensitiveDataService.maskText(mask(log.getRequest_params())));
            row.put("status", log.getStatus());
            row.put("errorMsg", log.getError_msg());
            row.put("costTime", log.getCost_time());
            row.put("createdAt", log.getCreated_at());
            rows.add(row);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", rows);
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return Result.success(data);
    }

    // 异常日志

    @GetMapping("/exception-logs")
    @RequirePermission("log:exception:view")
    public Result<Map<String, Object>> exceptionLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer resolved,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        pageNum = normalizePageNum(pageNum);
        pageSize = normalizePageSize(pageSize);
        QueryWrapper<ExceptionLogs> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("error_msg", kw).or().like("exception_type", kw)
                    .or().like("url", kw).or().like("username", kw));
        }
        if (resolved != null) {
            wrapper.eq("resolved", resolved);
        }
        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);
        if (start != null) {
            wrapper.ge("created_at", start);
        }
        if (end != null) {
            wrapper.le("created_at", end);
        }
        wrapper.orderByDesc("id");
        Page<ExceptionLogs> page = exceptionLogsRepository.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExceptionLogs log : page.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", log.getId());
            row.put("username", log.getUsername());
            row.put("url", log.getUrl());
            row.put("method", log.getMethod());
            row.put("ipAddress", log.getIp_address());
            row.put("exceptionType", log.getException_type());
            row.put("errorMsg", log.getError_msg());
            row.put("requestParams", mask(log.getRequest_params()));
            row.put("status", log.getStatus());
            row.put("resolved", log.getResolved());
            row.put("resolvedTime", log.getResolved_time());
            row.put("resolveNote", log.getResolve_note());
            row.put("createdAt", log.getCreated_at());
            rows.add(row);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", rows);
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return Result.success(data);
    }

    @GetMapping("/exception-logs/{id}")
    @RequirePermission("log:exception:view")
    public Result<Map<String, Object>> exceptionDetail(@PathVariable Long id) {
        ExceptionLogs log = exceptionLogsRepository.selectById(id);
        if (log == null) {
            return Result.error(404, "异常记录不存在或已删除");
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", log.getId());
        row.put("username", log.getUsername());
        row.put("url", log.getUrl());
        row.put("method", log.getMethod());
        row.put("ipAddress", log.getIp_address());
        row.put("browser", log.getBrowser());
        row.put("os", log.getOs());
        row.put("exceptionType", log.getException_type());
        row.put("errorMsg", log.getError_msg());
        row.put("stackTrace", log.getStack_trace());
        row.put("requestParams", mask(log.getRequest_params()));
        row.put("resolved", log.getResolved());
        row.put("resolvedTime", log.getResolved_time());
        row.put("resolveNote", log.getResolve_note());
        row.put("createdAt", log.getCreated_at());
        return Result.success(row);
    }

    // 手工补录异常，自动抓不到的报错由管理员登记
    @PostMapping("/exception-logs")
    @RequirePermission("log:exception:view")
    public Result<Map<String, Object>> createException(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String errorMsg = text(body.get("errorMsg"));
        if (errorMsg.isEmpty()) {
            return Result.error("异常信息不能为空");
        }
        ExceptionLogs log = new ExceptionLogs();
        log.setUser_id(RequestUser.id(request));
        log.setUsername(text(body.get("username")).isEmpty() ? "手工登记" : text(body.get("username")));
        log.setUrl(text(body.get("url")));
        log.setMethod(text(body.get("method")).isEmpty() ? "手工" : text(body.get("method")));
        log.setIp_address(text(body.get("ipAddress")));
        log.setException_type(text(body.get("exceptionType")).isEmpty() ? "手工登记" : text(body.get("exceptionType")));
        log.setError_msg(errorMsg);
        log.setStack_trace(text(body.get("stackTrace")));
        log.setRequest_params(mask(text(body.get("requestParams"))));
        log.setStatus(0);
        log.setResolved(0);
        log.setCreated_at(LocalDateTime.now());
        log.setUpdated_at(LocalDateTime.now());
        exceptionLogsRepository.insert(log);
        Map<String, Object> data = new HashMap<>();
        data.put("id", log.getId());
        return Result.success(data);
    }

    // 标记已解决，记录处理说明和处理人
    @PutMapping("/exception-logs/{id}/resolve")
    @RequirePermission("log:exception:view")
    public Result<?> resolveException(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        ExceptionLogs log = exceptionLogsRepository.selectById(id);
        if (log == null) {
            return Result.error(404, "异常记录不存在或已删除");
        }
        log.setResolved(1);
        log.setStatus(1);
        log.setResolved_by(RequestUser.id(request));
        log.setResolved_time(LocalDateTime.now());
        log.setResolve_note(text(body.get("resolveNote")));
        log.setUpdated_at(LocalDateTime.now());
        exceptionLogsRepository.updateById(log);
        return Result.success(null);
    }

    // 菜单转前端字段

    private Map<String, Object> menuRow(Menus m, Map<Long, String> names) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", m.getId());
        row.put("parentId", m.getParent_id());
        row.put("parentName", m.getParent_id() == null ? "" : names.getOrDefault(m.getParent_id(), ""));
        row.put("menuName", m.getMenu_name());
        row.put("menuCode", m.getMenu_code());
        row.put("menuType", m.getMenu_type());
        row.put("path", m.getPath());
        row.put("component", m.getComponent());
        row.put("icon", m.getIcon());
        row.put("isNav", m.getIs_nav());
        row.put("sortOrder", m.getSort_order());
        row.put("status", m.getStatus());
        row.put("visible", m.getVisible());
        return row;
    }

    // 密码、令牌这类参数在日志里打码
    private String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher matcher = SENSITIVE.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1) + "***"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private LocalDateTime parseStart(String date) {
        LocalDate d = parseDate(date);
        return d == null ? null : d.atStartOfDay();
    }

    private LocalDateTime parseEnd(String date) {
        LocalDate d = parseDate(date);
        return d == null ? null : d.atTime(23, 59, 59);
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 200);
    }
}
