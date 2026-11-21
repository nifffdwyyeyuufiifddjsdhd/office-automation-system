package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.entity.UserNotificationPreferences;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import com.officeautomationsystemapi.repository.UserNotificationPreferencesRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 站内通知接口：未读数、列表、已读、置顶、删除、偏好设置
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRecordsRepository repo;

    @Autowired
    private UserNotificationPreferencesRepository prefRepo;

    // 实体是下划线，前端要驼峰，逐项映射
    private Map<String, Object> toRow(NotificationRecords n) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", n.getId());
        row.put("title", n.getTitle());
        row.put("content", n.getContent());
        row.put("type", n.getType());
        row.put("bizType", n.getBiz_type());
        row.put("bizId", n.getBiz_id());
        row.put("jumpUrl", n.getJump_url());
        row.put("isRead", n.getIs_read());
        row.put("isTop", n.getIs_top() != null && n.getIs_top() == 1);
        row.put("createdAt", n.getCreated_at());
        row.put("readTime", n.getRead_time());
        return row;
    }

    // 同组定位条件：biz_type 和 biz_id 都有值才按业务合并
    // 缺一项就只锁这一条，不然会改到该用户全部未读
    private QueryWrapper<NotificationRecords> sameBusiness(NotificationRecords n, Long uid) {
        QueryWrapper<NotificationRecords> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", uid);
        if (n.getBiz_type() != null && n.getBiz_id() != null) {
            wrapper.eq("biz_type", n.getBiz_type());
            wrapper.eq("biz_id", n.getBiz_id());
        } else {
            wrapper.eq("id", n.getId());
        }
        return wrapper;
    }

    // 分组键和 sameBusiness 保持一致，缺业务字段的各自成组
    private String groupKey(NotificationRecords n) {
        if (n.getBiz_type() != null && n.getBiz_id() != null) {
            return n.getBiz_type() + "|" + n.getBiz_id();
        }
        return "id:" + n.getId();
    }

    // 不是本人的通知当作不存在
    private boolean isMine(NotificationRecords n, Long uid) {
        return uid != null && n != null && uid.equals(n.getUser_id());
    }

    // 未读数按记录条数统计，不按业务来源合并
    @GetMapping("/unread-count")
    @RequirePermission("notify:view")
    public Result<Map<String, Object>> unreadCount(HttpServletRequest request) {
        Long uid = RequestUser.id(request);
        if (uid == null) {
            return Result.success(Map.of("count", 0));
        }
        Long count = repo.selectCount(new QueryWrapper<NotificationRecords>()
                .eq("user_id", uid)
                .eq("is_read", 0));
        return Result.success(Map.of("count", count));
    }

    // 未读按业务来源合并，同组一条带 count，最多返回 20 组
    @GetMapping("/list")
    @RequirePermission("notify:view")
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        Long uid = RequestUser.id(request);
        if (uid == null) {
            return Result.success(List.of());
        }
        List<NotificationRecords> unread = repo.selectList(new QueryWrapper<NotificationRecords>()
                .eq("user_id", uid)
                .eq("is_read", 0)
                .orderByDesc("is_top")
                .orderByDesc("created_at")
                .last("LIMIT 50"));

        // LinkedHashMap 保留顺序，组内第一条是最新或置顶
        Map<String, List<NotificationRecords>> grouped = unread.stream()
                .collect(Collectors.groupingBy(
                        this::groupKey,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (List<NotificationRecords> group : grouped.values()) {
            Map<String, Object> row = toRow(group.get(0));
            row.put("count", group.size());
            result.add(row);
        }
        result.sort(Comparator.comparing(
                row -> (LocalDateTime) row.get("createdAt"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return Result.success(result.size() > 20 ? result.subList(0, 20) : result);
    }

    // 点开一条把同组未读都标已读
    @PostMapping("/{id}/read")
    @RequirePermission("notify:view")
    public Result<?> readOne(@PathVariable Long id, HttpServletRequest request) {
        Long uid = RequestUser.id(request);
        if (uid == null) {
            return Result.success(null);
        }
        NotificationRecords target = repo.selectById(id);
        if (!isMine(target, uid)) {
            return Result.success(null);
        }
        NotificationRecords update = new NotificationRecords();
        update.setIs_read(1);
        update.setRead_time(LocalDateTime.now());
        repo.update(update, sameBusiness(target, uid).eq("is_read", 0));
        return Result.success(null);
    }

    // 一次把当前用户的未读全标已读
    @PostMapping("/read-all")
    @RequirePermission("notify:view")
    public Result<?> readAll(HttpServletRequest request) {
        Long uid = RequestUser.id(request);
        if (uid == null) {
            return Result.success(null);
        }
        NotificationRecords update = new NotificationRecords();
        update.setIs_read(1);
        update.setRead_time(LocalDateTime.now());
        repo.update(update, new QueryWrapper<NotificationRecords>()
                .eq("user_id", uid)
                .eq("is_read", 0));
        return Result.success(null);
    }

    // 置顶作用于整组，top 不传按 1 算
    @PostMapping("/{id}/top")
    @RequirePermission("notify:view")
    public Result<?> toggleTop(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long uid = RequestUser.id(request);
        if (uid == null) {
            return Result.success(null);
        }
        NotificationRecords target = repo.selectById(id);
        if (!isMine(target, uid)) {
            return Result.success(null);
        }
        NotificationRecords update = new NotificationRecords();
        update.setIs_top(asInt(body.get("top"), 1));
        repo.update(update, sameBusiness(target, uid));
        return Result.success(null);
    }

    // 分页列表含已读记录，可按已读状态、类型、关键字筛选
    @GetMapping("/page")
    @RequirePermission("notify:view")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        // 页码和页容量先归整，防止传 0 或超大值
        pageNum = normalizePageNum(pageNum);
        pageSize = normalizePageSize(pageSize);
        Long uid = RequestUser.id(request);
        if (uid == null) {
            return Result.success(emptyPage(pageNum, pageSize));
        }

        QueryWrapper<NotificationRecords> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", uid);
        if (isRead != null) {
            wrapper.eq("is_read", isRead);
        }
        if (type != null) {
            wrapper.eq("type", type);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("title", kw).or().like("content", kw));
        }
        wrapper.orderByDesc("is_top").orderByDesc("created_at");

        Page<NotificationRecords> result = repo.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords().stream().map(this::toRow).collect(Collectors.toList()));
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success(data);
    }

    // 删除单条通知，只标记删除，只能删自己的
    @DeleteMapping("/{id}")
    @RequirePermission("notify:view")
    public Result<?> deleteOne(@PathVariable Long id, HttpServletRequest request) {
        Long uid = RequestUser.id(request);
        if (uid == null) {
            return Result.success(null);
        }
        if (!isMine(repo.selectById(id), uid)) {
            return Result.success(null);
        }
        repo.deleteById(id);
        return Result.success(null);
    }

    // 清空已读通知，未读保留
    @DeleteMapping("/read")
    @RequirePermission("notify:view")
    public Result<?> deleteRead(HttpServletRequest request) {
        Long uid = RequestUser.id(request);
        if (uid == null) {
            return Result.success(null);
        }
        repo.delete(new QueryWrapper<NotificationRecords>()
                .eq("user_id", uid)
                .eq("is_read", 1));
        return Result.success(null);
    }

    // 没配置过偏好时六项默认全开
    @GetMapping("/preferences")
    @RequirePermission("notify:view")
    public Result<Map<String, Object>> getPreferences(HttpServletRequest request) {
        Long uid = RequestUser.id(request);
        Map<String, Object> data = defaultPrefs();
        if (uid == null) {
            return Result.success(data);
        }
        // 偏好表没有唯一索引，历史可能多行，取最新一条
        UserNotificationPreferences pref = prefRepo.selectOne(
                new QueryWrapper<UserNotificationPreferences>().eq("user_id", uid)
                        .orderByDesc("id").last("LIMIT 1"));
        if (pref != null) {
            data.put("system", pref.getPref_system());
            data.put("flow", pref.getPref_flow());
            data.put("task", pref.getPref_task());
            data.put("attendance", pref.getPref_attendance());
            data.put("worklog", pref.getPref_worklog());
            data.put("sound", pref.getPref_sound());
        }
        return Result.success(data);
    }

    // 有记录就更新，没有就插入，缺的项按开启算
    @PutMapping("/preferences")
    @RequirePermission("notify:view")
    public Result<?> savePreferences(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long uid = RequestUser.id(request);
        if (uid == null) {
            return Result.success(null);
        }
        // 先查再决定插入还是更新，同样取最新一条
        UserNotificationPreferences pref = prefRepo.selectOne(
                new QueryWrapper<UserNotificationPreferences>().eq("user_id", uid)
                        .orderByDesc("id").last("LIMIT 1"));
        boolean isNew = pref == null;
        if (isNew) {
            pref = new UserNotificationPreferences();
            pref.setUser_id(uid);
            pref.setCreated_at(LocalDateTime.now());
        }
        pref.setPref_system(asInt(body.get("system"), 1));
        pref.setPref_flow(asInt(body.get("flow"), 1));
        pref.setPref_task(asInt(body.get("task"), 1));
        pref.setPref_attendance(asInt(body.get("attendance"), 1));
        pref.setPref_worklog(asInt(body.get("worklog"), 1));
        pref.setPref_sound(asInt(body.get("sound"), 1));
        pref.setUpdated_at(LocalDateTime.now());
        if (isNew) {
            prefRepo.insert(pref);
        } else {
            prefRepo.updateById(pref);
        }
        return Result.success(null);
    }

    // 页码小于 1 按第一页算
    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    // 页容量小于 1 按 10 算，最大 500
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 500);
    }

    // 没登录返回空分页，字段和正常分页一致
    private Map<String, Object> emptyPage(Integer pageNum, Integer pageSize) {
        Map<String, Object> empty = new HashMap<>();
        empty.put("records", List.of());
        empty.put("total", 0);
        empty.put("current", pageNum);
        empty.put("size", pageSize);
        return empty;
    }

    // 默认偏好，六项全开
    private Map<String, Object> defaultPrefs() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("system", 1);
        prefs.put("flow", 1);
        prefs.put("task", 1);
        prefs.put("attendance", 1);
        prefs.put("worklog", 1);
        prefs.put("sound", 1);
        return prefs;
    }

    // 布尔、数字、字符串都统一转成 0 或 1
    private Integer asInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean flag) {
            return flag ? 1 : 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
