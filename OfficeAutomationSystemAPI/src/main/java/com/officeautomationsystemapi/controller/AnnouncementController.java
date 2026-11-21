package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.service.AnnouncementService;
import com.officeautomationsystemapi.vo.AnnouncementCategoryVO;
import com.officeautomationsystemapi.vo.AnnouncementVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 公告模块：类别、公告、发布下线、置顶、阅读记录
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    // 上传根目录，与日志附件、头像共用一个配置
    @Value("${oa.upload-dir:./uploads}")
    private String uploadDir;

    // 正文插图只收常见图片，单张不超过 5MB
    private static final List<String> IMAGE_EXTS = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long IMAGE_MAX_SIZE = 5L * 1024 * 1024;

    // 上传正文插图到 uploads/announcement/年月，返回可写进正文的地址
    // 读取走 /api/public/announcement-image，img 标签带不上 token 所以免登录
    @PostMapping("/upload-image")
    @RequirePermission({"announcement:add", "announcement:edit"})
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件为空");
        }
        if (file.getSize() > IMAGE_MAX_SIZE) {
            return Result.error("图片不能超过5MB，请压缩后再上传");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase();
        if (!IMAGE_EXTS.contains(ext)) {
            return Result.error("仅支持 jpg/jpeg/png/gif/webp 图片");
        }
        // 后缀可以改，按文件头确认是不是真图片
        if (!imageMatchesExt(file, ext)) {
            return Result.error("文件内容不是有效的图片");
        }

        String monthDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Path.of(uploadDir, "announcement", monthDir);
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(fileName));
            }
        } catch (IOException e) {
            return Result.error("图片保存失败：" + e.getMessage());
        }

        String relative = "announcement/" + monthDir + "/" + fileName;
        Map<String, Object> result = new HashMap<>();
        result.put("url", "/api/public/announcement-image?file=" + relative);
        result.put("path", relative);
        result.put("fileName", original);
        return Result.success(result);
    }

    // 按文件头判断真实类型：jpg FFD8、png 89504E47、gif GIF8、webp RIFF
    private boolean imageMatchesExt(MultipartFile file, String ext) {
        byte[] head = new byte[12];
        int len;
        try (InputStream in = file.getInputStream()) {
            len = in.read(head);
        } catch (IOException e) {
            return false;
        }
        if (len < 4) {
            return false;
        }
        int b0 = head[0] & 0xFF;
        int b1 = head[1] & 0xFF;
        int b2 = head[2] & 0xFF;
        int b3 = head[3] & 0xFF;
        switch (ext) {
            case "jpg":
            case "jpeg":
                return b0 == 0xFF && b1 == 0xD8;
            case "png":
                return b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47;
            case "gif":
                return b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38;
            case "webp":
                return len >= 12 && b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
                        && (head[8] & 0xFF) == 0x57 && (head[9] & 0xFF) == 0x45
                        && (head[10] & 0xFF) == 0x42 && (head[11] & 0xFF) == 0x50;
            default:
                return false;
        }
    }

    // 类别树，按当前用户可见范围过滤
    @GetMapping("/categories/tree")
    public Result<List<AnnouncementCategoryVO>> categoryTree(HttpServletRequest request) {
        return Result.success(announcementService.listCategoryTree(RequestUser.id(request)));
    }

    @PostMapping("/categories")
    @RequirePermission("announcement:category:manage")
    public Result<Long> addCategory(@RequestBody AnnouncementCategoryVO vo) {
        return Result.success(announcementService.addCategory(vo));
    }

    @PutMapping("/categories")
    @RequirePermission("announcement:category:manage")
    public Result<Void> updateCategory(@RequestBody AnnouncementCategoryVO vo) {
        announcementService.updateCategory(vo);
        return Result.success();
    }

    @DeleteMapping("/categories/{id}")
    @RequirePermission("announcement:category:manage")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        announcementService.deleteCategory(id);
        return Result.success();
    }

    @PutMapping("/categories/{id}/status")
    @RequirePermission("announcement:category:manage")
    public Result<Void> toggleCategoryStatus(@PathVariable Long id, @RequestParam Integer status) {
        announcementService.toggleCategoryStatus(id, status);
        return Result.success();
    }

    // 公告列表，已按类别权限过滤
    @GetMapping("/page")
    @RequirePermission("announcement:list")
    public Result<Page<AnnouncementVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             HttpServletRequest request) {
        // 分页参数归整，页码最小 1，每页最多 500 条
        return Result.success(announcementService.pageAnnouncements(RequestUser.id(request),
                normalizePageNum(pageNum), normalizePageSize(pageSize), categoryId, keyword, status));
    }

    @GetMapping("/{id}")
    @RequirePermission("announcement:list")
    public Result<AnnouncementVO> detail(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(announcementService.getAnnouncementDetail(RequestUser.id(request), id));
    }

    @PostMapping
    @RequirePermission("announcement:add")
    public Result<Long> add(@RequestBody AnnouncementVO vo, HttpServletRequest request) {
        return Result.success(announcementService.addAnnouncement(RequestUser.id(request), vo));
    }

    @PutMapping
    @RequirePermission("announcement:edit")
    public Result<Void> update(@RequestBody AnnouncementVO vo) {
        announcementService.updateAnnouncement(null, vo);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("announcement:delete")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return Result.success();
    }

    @PutMapping("/{id}/publish")
    @RequirePermission("announcement:publish")
    public Result<Void> publish(@PathVariable Long id) {
        announcementService.publishAnnouncement(id);
        return Result.success();
    }

    @PutMapping("/{id}/offline")
    @RequirePermission("announcement:publish")
    public Result<Void> offline(@PathVariable Long id) {
        announcementService.cancelPublishAnnouncement(id);
        return Result.success();
    }

    @PostMapping("/batch-publish")
    @RequirePermission("announcement:publish")
    public Result<Void> batchPublish(@RequestBody List<Long> ids) {
        announcementService.batchPublish(ids);
        return Result.success();
    }

    @PutMapping("/{id}/top")
    @RequirePermission("announcement:top")
    public Result<Void> top(@PathVariable Long id, @RequestParam Integer isTop) {
        announcementService.toggleTop(id, isTop);
        return Result.success();
    }

    // 阅读记录，需要对应权限
    @GetMapping("/{id}/reads")
    @RequirePermission("announcement:reads")
    public Result<Page<Map<String, Object>>> reads(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(announcementService.pageReadUsers(id, normalizePageNum(pageNum), normalizePageSize(pageSize)));
    }

    // 页码小于 1 按 1 算
    private int normalizePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    // 每页条数小于 1 按 10 算，最大 500
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 500);
    }
}
