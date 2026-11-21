package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.Announcements;
import com.officeautomationsystemapi.repository.AnnouncementsRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 公开接口，免登录，供 guest 和门户页看公告
// 只返回 status=1 已发布未删除的公告，作者 ID 等内部字段不返回
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private AnnouncementsRepository announcementsRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // 上传根目录，公告插图在 uploads/announcement 下
    @Value("${oa.upload-dir:./uploads}")
    private String uploadDir;

    // 正文插图读取，img 带不上 token，放在免登录的 public 下
    // 只读 uploads/announcement 内的文件，标准化路径防止 ../ 穿越
    @GetMapping("/announcement-image")
    public void announcementImage(@RequestParam("file") String file, HttpServletResponse response) {
        if (file == null || file.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        Path imageRoot = root.resolve("announcement").normalize();
        Path target;
        try {
            target = root.resolve(file).normalize();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!target.startsWith(imageRoot) || !Files.isRegularFile(target)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String lower = target.getFileName().toString().toLowerCase();
        String contentType = "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (lower.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (lower.endsWith(".webp")) {
            contentType = "image/webp";
        }
        try {
            response.setContentType(contentType);
            // 文件名是随机串，内容不变，允许浏览器缓存一天
            response.setHeader("Cache-Control", "public, max-age=86400");
            Files.copy(target, response.getOutputStream());
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // 公开公告列表，默认最新 10 条，最多 50 条
    @GetMapping("/announcements")
    public Result<List<Map<String, Object>>> announcements(@RequestParam(defaultValue = "10") Integer limit) {
        int size = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        List<Announcements> list = announcementsRepository.selectList(new QueryWrapper<Announcements>()
                .eq("status", 1)
                .orderByDesc("is_top")
                .orderByDesc("publish_time")
                .orderByDesc("id")
                .last("LIMIT " + size));
        Map<Long, String> categories = categoryNames(list);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Announcements a : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("title", a.getTitle());
            row.put("summary", a.getSummary());
            row.put("coverImage", a.getCover_image());
            row.put("categoryName", categories.getOrDefault(a.getCategory_id(), ""));
            row.put("isTop", a.getIs_top());
            row.put("publishTime", a.getPublish_time());
            rows.add(row);
        }
        return Result.success(rows);
    }

    // 公告详情，只有已发布的能看，顺带加一次浏览量
    @GetMapping("/announcements/{id}")
    public Result<Map<String, Object>> announcementDetail(@PathVariable Long id) {
        Announcements a = announcementsRepository.selectById(id);
        if (a == null || a.getStatus() == null || a.getStatus() != 1) {
            return Result.error(404, "公告不存在或未公开发布");
        }
        jdbc.update("UPDATE announcements SET view_count = COALESCE(view_count, 0) + 1 WHERE id = ?", id);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", a.getId());
        row.put("title", a.getTitle());
        row.put("content", a.getContent());
        // 正文主题，前端按它选排版样式
        row.put("theme", a.getTheme() == null ? "standard" : a.getTheme());
        row.put("summary", a.getSummary());
        row.put("coverImage", a.getCover_image());
        row.put("categoryName", categoryNames(List.of(a)).getOrDefault(a.getCategory_id(), ""));
        row.put("publishTime", a.getPublish_time());
        row.put("viewCount", (a.getView_count() == null ? 0 : a.getView_count()) + 1);
        return Result.success(row);
    }

    // 类别名批量查
    private Map<Long, String> categoryNames(List<Announcements> list) {
        Map<Long, String> map = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (Announcements a : list) {
            if (a.getCategory_id() != null && !ids.contains(a.getCategory_id())) {
                ids.add(a.getCategory_id());
            }
        }
        if (ids.isEmpty()) {
            return map;
        }
        StringBuilder sql = new StringBuilder("SELECT id, category_name FROM announcement_categories WHERE id IN (");
        for (int i = 0; i < ids.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(")");
        for (Map<String, Object> row : jdbc.queryForList(sql.toString(), ids.toArray())) {
            Long id = ((Number) row.get("id")).longValue();
            Object name = row.get("category_name");
            map.put(id, name == null ? "" : name.toString());
        }
        return map;
    }
}
