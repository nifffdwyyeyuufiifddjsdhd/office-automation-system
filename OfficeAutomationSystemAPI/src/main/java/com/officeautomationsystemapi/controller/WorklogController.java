package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.Attachments;
import com.officeautomationsystemapi.repository.AttachmentsRepository;
import com.officeautomationsystemapi.service.WorklogService;
import com.officeautomationsystemapi.vo.WorklogCategoryVO;
import com.officeautomationsystemapi.vo.WorklogCommentVO;
import com.officeautomationsystemapi.vo.WorklogGrantVO;
import com.officeautomationsystemapi.vo.WorklogStatsVO;
import com.officeautomationsystemapi.vo.WorklogVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 工作日志接口：我的日志、查看、评论、后台、类别、授权、统计、导出、附件、补写
@RestController
@RequestMapping("/api/worklogs")
public class WorklogController {

    @Autowired
    private WorklogService worklogService;
    @Autowired
    private AttachmentsRepository attachmentsRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${oa.upload-dir:./uploads}")
    private String uploadDir;

    // 允许的附件后缀，只放文档、图片、压缩包和代码，不放可执行文件
    private static final Set<String> ALLOWED_EXTS = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "md",
            "jpg", "jpeg", "png", "gif", "bmp",
            "zip", "rar",
            "java", "js", "ts", "py");

    // 我的日志分页
    @GetMapping("/my")
    public Result<Page<WorklogVO>> myPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String logDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        return Result.success(worklogService.myPage(RequestUser.id(request), pageNum, pageSize, logDate, categoryId, keyword));
    }

    // 新增日志，直接就是已提交状态
    @PostMapping
    @RequirePermission("worklog:add")
    public Result<Long> create(@RequestBody WorklogVO vo, HttpServletRequest request) {
        return Result.success(worklogService.create(RequestUser.id(request), vo));
    }

    // 修改日志，只有当天的能改
    @PutMapping
    @RequirePermission("worklog:edit")
    public Result<Void> update(@RequestBody WorklogVO vo, HttpServletRequest request) {
        worklogService.update(RequestUser.id(request), vo);
        return Result.success(null);
    }

    // 删除自己的日志，只标记删除，管理员可在回收站恢复
    @DeleteMapping("/{id}")
    @RequirePermission("worklog:delete")
    public Result<Void> myDelete(@PathVariable Long id, HttpServletRequest request) {
        worklogService.myDelete(RequestUser.id(request), id);
        return Result.success(null);
    }

    // 我的日志详情
    @GetMapping("/my/{id}")
    public Result<WorklogVO> myDetail(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(worklogService.myDetail(RequestUser.id(request), id));
    }

    // 查看别人日志，只返回有权限看的人
    @GetMapping("/view")
    @RequirePermission({"worklog:view-auth", "worklog:view-all", "worklog:view-dept"})
    public Result<Page<WorklogVO>> viewPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long orgId,
            HttpServletRequest request) {
        return Result.success(worklogService.viewPage(RequestUser.id(request), pageNum, pageSize, keyword, categoryId, orgId));
    }

    // 日志详情，没权限报错，每看一次阅读数加一
    @GetMapping("/view/{id}")
    @RequirePermission({"worklog:view-auth", "worklog:view-all", "worklog:view-dept"})
    public Result<WorklogVO> viewDetail(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(worklogService.viewDetail(RequestUser.id(request), id));
    }

    // 某篇日志的评论列表
    @GetMapping("/{worklogId}/comments")
    public Result<List<WorklogCommentVO>> comments(@PathVariable Long worklogId, HttpServletRequest request) {
        return Result.success(worklogService.listComments(RequestUser.id(request), worklogId));
    }

    // 发表评论，parentId 为空表示顶层评论
    @PostMapping("/{worklogId}/comments")
    @RequirePermission("worklog:comment")
    public Result<Void> addComment(@PathVariable Long worklogId,
                                   @RequestBody Map<String, Object> body,
                                   HttpServletRequest request) {
        Long parentId = body.get("parentId") == null ? null : Long.valueOf(body.get("parentId").toString());
        worklogService.addComment(RequestUser.id(request), worklogId, (String) body.get("content"), parentId);
        return Result.success(null);
    }

    // 删除评论，日志作者、评论作者、管理员都能删
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        worklogService.deleteComment(RequestUser.id(request), commentId);
        return Result.success(null);
    }

    // 后台日志分页，可按状态、类别、部门、日期筛选
    @GetMapping("/admin/page")
    @RequirePermission({"worklog:view-all", "worklog:view-dept"})
    public Result<Page<WorklogVO>> adminPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        // 带上调用者，服务层按部门收范围
        return Result.success(worklogService.adminPage(RequestUser.id(request), pageNum, pageSize, keyword,
                status, categoryId, orgId, startDate, endDate));
    }

    // 回收站分页，只列已删除的日志
    @GetMapping("/admin/recycle")
    @RequirePermission({"worklog:view-all", "worklog:view-dept"})
    public Result<Page<WorklogVO>> recyclePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        return Result.success(worklogService.recyclePage(RequestUser.id(request), pageNum, pageSize, keyword));
    }

    // 彻底删除，不可恢复，所以用 worklog:manage
    @DeleteMapping("/admin/{id}")
    @RequirePermission("worklog:manage")
    public Result<Void> adminDelete(@PathVariable Long id) {
        worklogService.adminDelete(id);
        return Result.success(null);
    }

    // 从回收站恢复，和彻底删除共用 worklog:manage
    @PostMapping("/admin/restore/{id}")
    @RequirePermission("worklog:manage")
    public Result<Void> adminRestore(@PathVariable Long id) {
        worklogService.adminRestore(id);
        return Result.success(null);
    }

    // 启用中的类别，写日志时下拉选
    @GetMapping("/categories")
    public Result<List<WorklogCategoryVO>> categories() {
        return Result.success(worklogService.listCategories());
    }

    // 全部类别，含停用的，管理员维护用
    @GetMapping("/categories/all")
    @RequirePermission("worklog:category:manage")
    public Result<List<WorklogCategoryVO>> adminCategories() {
        return Result.success(worklogService.adminCategories());
    }

    // 新增类别
    @PostMapping("/categories")
    @RequirePermission("worklog:category:manage")
    public Result<Void> addCategory(@RequestBody WorklogCategoryVO vo, HttpServletRequest request) {
        worklogService.addCategory(RequestUser.id(request), vo);
        return Result.success(null);
    }

    // 修改类别
    @PutMapping("/categories")
    @RequirePermission("worklog:category:manage")
    public Result<Void> updateCategory(@RequestBody WorklogCategoryVO vo) {
        worklogService.updateCategory(vo);
        return Result.success(null);
    }

    // 删除类别，下面还有日志时不能删
    @DeleteMapping("/categories/{id}")
    @RequirePermission("worklog:category:manage")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        worklogService.deleteCategory(id);
        return Result.success(null);
    }

    // 查看授权分页
    @GetMapping("/permissions")
    @RequirePermission("worklog:permission-config")
    public Result<Page<WorklogGrantVO>> grantPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(worklogService.grantPage(pageNum, pageSize, keyword));
    }

    // 新增一条查看授权
    @PostMapping("/permissions")
    @RequirePermission("worklog:permission-config")
    public Result<Void> addGrant(@RequestBody WorklogGrantVO vo, HttpServletRequest request) {
        worklogService.addGrant(RequestUser.id(request), vo);
        return Result.success(null);
    }

    // 取消查看授权
    @DeleteMapping("/permissions/{id}")
    @RequirePermission("worklog:permission-config")
    public Result<Void> deleteGrant(@PathVariable Long id) {
        worklogService.deleteGrant(id);
        return Result.success(null);
    }

    // 我的日志统计
    @GetMapping("/stats/my")
    public Result<WorklogStatsVO> myStats(HttpServletRequest request) {
        return Result.success(worklogService.stats(RequestUser.id(request), null));
    }

    // 部门日志统计，只能看自己部门或自己管的部门
    @GetMapping("/stats/dept")
    public Result<WorklogStatsVO> deptStats(@RequestParam Long orgId, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        // 先校验部门在本人可见范围内
        if (!canViewDeptStats(userId, orgId)) return Result.error(403, "无权查看该部门统计");
        return Result.success(worklogService.stats(userId, orgId));
    }

    // 导出我自己的日志
    @GetMapping("/export/mine")
    public void exportMine(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           HttpServletRequest request, HttpServletResponse response) {
        worklogService.exportExcel(RequestUser.id(request), true, keyword, categoryId, null, startDate, endDate, response);
    }

    // 导出全部日志，管理员用，可带筛选条件
    @GetMapping("/export")
    @RequirePermission("worklog:export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Long orgId,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletRequest request, HttpServletResponse response) {
        worklogService.exportExcel(RequestUser.id(request), false, keyword, categoryId, orgId, startDate, endDate, response);
    }

    // 上传附件，单个不超过 20MB，只收白名单格式，先不绑定日志
    @PostMapping("/attachments/upload")
    public Result<Map<String, Object>> uploadAttachment(@RequestParam("file") MultipartFile file,
                                                        HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        if (file == null || file.isEmpty()) return Result.error("文件为空");
        if (file.getSize() > 20L * 1024 * 1024) return Result.error("文件大小超过20MB，请压缩后上传");

        String original = file.getOriginalFilename() == null ? "未命名" : file.getOriginalFilename();
        int dot = original.lastIndexOf(".");
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase();
        if (!ALLOWED_EXTS.contains(ext)) return Result.error("该文件格式不允许上传");
        // 后缀能改，按文件头确认类型，防止可执行文件伪装
        if (!typeMatchesExt(file, ext)) return Result.error("文件类型与扩展名不匹配");

        // 按年月日分目录，文件名用随机串避免重名
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        File dir = new File(uploadDir + File.separator + "worklog" + File.separator + dateDir);
        if (!dir.exists()) dir.mkdirs();
        File dest = new File(dir, UUID.randomUUID().toString().replace("-", "") + "." + ext);
        try {
            Files.copy(file.getInputStream(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        Attachments att = new Attachments();
        att.setFile_name(original);
        att.setFile_path(dest.getAbsolutePath());
        att.setFile_size(file.getSize());
        att.setFile_type(file.getContentType());
        att.setFile_ext("." + ext);
        att.setUploader_id(userId);
        att.setStorage_type(1);
        att.setCreated_at(now);
        att.setUpdated_at(now);
        // target_type 和 target_id 先留空，存日志时再绑
        try {
            attachmentsRepository.insert(att);
        } catch (Exception e) {
            // 写库失败就把刚存的文件删掉
            if (dest.exists()) dest.delete();
            return Result.error("附件保存失败，请稍后重试");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", att.getId());
        result.put("fileName", att.getFile_name());
        result.put("fileSize", att.getFile_size());
        result.put("fileExt", att.getFile_ext());
        return Result.success(result);
    }

    // 按文件头判断类型，纯文本和代码没有文件头，直接放行
    private boolean typeMatchesExt(MultipartFile file, String ext) {
        if ("txt".equals(ext) || "md".equals(ext) || "java".equals(ext)
                || "js".equals(ext) || "ts".equals(ext) || "py".equals(ext)) {
            return true;
        }
        byte[] head = new byte[8];
        int len;
        try (InputStream in = file.getInputStream()) {
            len = in.read(head);
        } catch (IOException e) {
            return false;
        }
        if (len < 2) return false;
        switch (ext) {
            case "pdf":
                return len >= 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
            case "docx":
            case "xlsx":
            case "pptx":
            case "zip":
                return head[0] == 'P' && head[1] == 'K';
            case "rar":
                return len >= 4 && head[0] == 'R' && head[1] == 'a' && head[2] == 'r';
            case "doc":
            case "xls":
            case "ppt":
                return len >= 4 && head[0] == (byte) 0xD0 && head[1] == (byte) 0xCF
                        && head[2] == (byte) 0x11 && head[3] == (byte) 0xE0;
            case "png":
                return len >= 4 && head[0] == (byte) 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
            case "jpg":
            case "jpeg":
                return len >= 3 && head[0] == (byte) 0xFF && head[1] == (byte) 0xD8 && head[2] == (byte) 0xFF;
            case "gif":
                return len >= 4 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8';
            case "bmp":
                return head[0] == 'B' && head[1] == 'M';
            default:
                return false;
        }
    }

    // 下载附件，要有这篇日志的查看权限
    @GetMapping("/attachments/{aid}/download")
    public void downloadAttachment(@PathVariable Long aid, HttpServletRequest request, HttpServletResponse response) {
        worklogService.downloadAttachment(RequestUser.id(request), aid, response);
    }

    // 补写超过 30 天的日志要申请，同时通知审批人
    @PostMapping("/supplement-apply")
    @Transactional
    @RequirePermission("worklog:makeup")
    public Result<?> applySupplement(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        Long userId = RequestUser.id(req);
        String startDate = requireText(body, "start_date", "请选择补写开始日期");
        String endDate = requireText(body, "end_date", "请选择补写结束日期");
        String reason = text(body, "reason");
        // 先插一条待审批记录，status 为 0
        jdbcTemplate.update(
                "INSERT INTO worklog_supplement_approvals(user_id,start_date,end_date,reason,status) VALUES(?,?,?,?,0)",
                userId, startDate, endDate, reason);
        // 审批人是申请人部门的主管，主管为空时转给管理员
        List<Long> reviewerIds = supplementReviewers(userId);
        if (reviewerIds.isEmpty()) throw new RuntimeException("您所在部门未配置主管，暂无法提交补写申请，请联系管理员");
        for (Long reviewerId : reviewerIds) {
            jdbcTemplate.update(
                    "INSERT INTO notification_records(user_id,title,content,biz_type,biz_id,jump_url,created_at) " +
                    "VALUES(?, '日志补写申请待审批', " +
                    "CONCAT('有员工申请补写 ', ?, ' 至 ', ?, ' 的日志，原因：', ?), " +
                    "'worklog_supplement', NULL, '/home/worklog', NOW())",
                    reviewerId, startDate, endDate, reason);
        }
        return Result.success("申请已提交，请等待审批");
    }

    // 待审补写申请，管理员看全部，部门主管看本部门及下级
    @GetMapping("/supplement-pending")
    public Result<?> pendingSupplement(HttpServletRequest req) {
        Long userId = RequestUser.id(req);
        if (isAdmin(userId)) {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    "SELECT a.*, u.real_name AS user_name FROM worklog_supplement_approvals a " +
                    "LEFT JOIN users u ON a.user_id=u.id WHERE a.status=0 ORDER BY a.created_at DESC");
            return Result.success(list);
        }
        Set<Long> scope = managedOrgIds(userId);
        if (scope.isEmpty()) return Result.error(403, "无权查看补写申请，仅管理员或部门主管可操作");
        String placeholders = String.join(",", java.util.Collections.nCopies(scope.size(), "?"));
        List<Object> args = new ArrayList<>(scope);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT a.*, u.real_name AS user_name FROM worklog_supplement_approvals a " +
                "JOIN users u ON a.user_id=u.id WHERE a.status=0 AND u.org_id IN (" + placeholders + ") " +
                "ORDER BY a.created_at DESC", args.toArray());
        return Result.success(list);
    }

    // 审批补写申请，通过后开放 7 天，不能批自己的
    @PostMapping("/supplement-approve/{id}")
    @Transactional
    public Result<?> approveSupplement(HttpServletRequest req, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object passValue = body.get("pass");
        if (passValue == null) throw new RuntimeException("请选择审批结果");
        boolean pass = Boolean.parseBoolean(passValue.toString());
        Long opUserId = RequestUser.id(req);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT user_id, status FROM worklog_supplement_approvals WHERE id=?", id);
        if (rows.isEmpty()) throw new RuntimeException("补写申请不存在");
        Map<String, Object> row = rows.get(0);
        if (row.get("status") == null || ((Number) row.get("status")).intValue() != 0) {
            throw new RuntimeException("该申请已审批，请勿重复操作");
        }
        Long applicantId = Long.valueOf(row.get("user_id").toString());
        // 通过等于开放历史日志写入，只能是管理员或本部门主管
        if (!canReviewSupplement(opUserId, applicantId)) {
            return Result.error(403, "无权审批该补写申请：申请人不在您管理的部门范围内");
        }
        // 不能审自己的申请
        if (applicantId.equals(opUserId)) return Result.error(403, "不能审批自己的补写申请");
        if (pass) {
            // 带 status=0 条件更新，同时点通过只有一个成功
            int updated = jdbcTemplate.update(
                    "UPDATE worklog_supplement_approvals SET status=1, approved_by=?, expire_at=DATE_ADD(NOW(), INTERVAL 7 DAY) WHERE id=? AND status=0",
                    opUserId, id);
            if (updated != 1) throw new RuntimeException("该申请已处理");
            jdbcTemplate.update(
                    "INSERT INTO notification_records(user_id,title,content,biz_type,biz_id,jump_url,created_at) " +
                    "VALUES(?, '日志补写申请已通过', '您的日志补写申请已通过，7天内可补写。', 'worklog_supplement', ?, '/home/worklog', NOW())",
                    applicantId, id);
        } else {
            // 驳回也带 status=0 条件，避免改回已通过的
            int updated = jdbcTemplate.update(
                    "UPDATE worklog_supplement_approvals SET status=2, approved_by=? WHERE id=? AND status=0", opUserId, id);
            if (updated != 1) throw new RuntimeException("该申请已处理");
            jdbcTemplate.update(
                    "INSERT INTO notification_records(user_id,title,content,biz_type,biz_id,jump_url,created_at) " +
                    "VALUES(?, '日志补写申请被驳回', '您的日志补写申请未通过，请联系主管了解原因。', 'worklog_supplement', ?, '/home/worklog', NOW())",
                    applicantId, id);
        }
        return Result.success(pass ? "已通过" : "已驳回");
    }

    // 管理员直接授权可补写的日期范围，默认 7 天有效
    @PostMapping("/supplement-grant")
    public Result<?> grantSupplement(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        Long userId = requireLong(body, "user_id", "请选择要授权的员工");
        String startDate = requireText(body, "start_date", "请选择开始日期");
        String endDate = requireText(body, "end_date", "请选择结束日期");
        String reason = text(body, "reason");
        int days = body.get("days") == null ? 7 : Integer.parseInt(body.get("days").toString());
        Long opUserId = RequestUser.id(req);
        // 直接授权绕过审批，只有管理员能操作
        if (!isAdmin(opUserId)) return Result.error(403, "仅管理员可授权补写");
        // 有未过期的授权就直接返回原记录
        List<Map<String, Object>> existed = jdbcTemplate.queryForList(
                "SELECT id, start_date, end_date, expire_at FROM worklog_supplement_approvals " +
                "WHERE user_id=? AND status=1 AND expire_at > NOW() ORDER BY id DESC LIMIT 1", userId);
        if (!existed.isEmpty()) {
            return Result.success("该员工已有未过期的补写授权，返回原记录", existed.get(0));
        }
        // 直接授权按已通过处理，status 为 1
        jdbcTemplate.update(
                "INSERT INTO worklog_supplement_approvals(user_id,start_date,end_date,reason,approved_by,status,expire_at) " +
                "VALUES(?,?,?,?,?,1,DATE_ADD(NOW(), INTERVAL ? DAY))",
                userId, startDate, endDate, reason, opUserId, days);
        return Result.success("授权成功，" + days + "天内可补写 " + startDate + " 至 " + endDate);
    }

    // 全部补写记录，含申请和授权，只给管理员看
    @GetMapping("/supplement-list")
    public Result<?> listSupplement(HttpServletRequest req) {
        if (!isAdmin(RequestUser.id(req))) return Result.error(403, "无权查看补写记录，仅管理员可操作");
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT a.*, u.real_name AS user_name, b.real_name AS approver_name " +
                "FROM worklog_supplement_approvals a LEFT JOIN users u ON a.user_id=u.id " +
                "LEFT JOIN users b ON a.approved_by=b.id ORDER BY a.created_at DESC");
        return Result.success(list);
    }

    // 我当前还能用的补写授权
    @GetMapping("/supplement-mine")
    public Result<?> mySupplement(HttpServletRequest req) {
        Long userId = RequestUser.id(req);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM worklog_supplement_approvals WHERE user_id=? AND status=1 AND expire_at > NOW() ORDER BY start_date",
                userId);
        return Result.success(list);
    }

    // 我负责的部门和全部下级，不是主管返回空集合
    private Set<Long> managedOrgIds(Long userId) {
        Set<Long> managed = new HashSet<>();
        if (userId == null) return managed;
        List<Map<String, Object>> orgs = jdbcTemplate.queryForList(
                "SELECT id, parent_id, leader_id FROM organizations WHERE deleted_at IS NULL");
        for (Map<String, Object> org : orgs) {
            if (org.get("leader_id") != null && userId.equals(((Number) org.get("leader_id")).longValue())) {
                managed.add(((Number) org.get("id")).longValue());
            }
        }
        if (managed.isEmpty()) return managed;
        // 逐层补下级部门，直到没有新增
        boolean grown = true;
        while (grown) {
            grown = false;
            for (Map<String, Object> org : orgs) {
                if (org.get("parent_id") == null) continue;
                long parentId = ((Number) org.get("parent_id")).longValue();
                if (managed.contains(parentId) && managed.add(((Number) org.get("id")).longValue())) {
                    grown = true;
                }
            }
        }
        return managed;
    }

    // 管理员不限，部门主管只能审自己部门的人
    private boolean canReviewSupplement(Long operatorId, Long applicantId) {
        if (operatorId == null || applicantId == null) return false;
        if (isAdmin(operatorId)) return true;
        Set<Long> scope = managedOrgIds(operatorId);
        if (scope.isEmpty()) return false;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT org_id FROM users WHERE id=? AND deleted_at IS NULL", applicantId);
        if (rows.isEmpty() || rows.get(0).get("org_id") == null) return false;
        return scope.contains(((Number) rows.get(0).get("org_id")).longValue());
    }

    // 审批人：上级各级主管和同部门主管，没有就转管理员
    private List<Long> supplementReviewers(Long applicantId) {
        Set<Long> reviewers = new HashSet<>();
        List<Map<String, Object>> orgs = jdbcTemplate.queryForList(
                "SELECT id, parent_id, leader_id FROM organizations WHERE deleted_at IS NULL");
        Map<Long, Map<String, Object>> orgById = new HashMap<>();
        for (Map<String, Object> org : orgs) {
            orgById.put(((Number) org.get("id")).longValue(), org);
        }
        List<Map<String, Object>> applicant = jdbcTemplate.queryForList(
                "SELECT org_id FROM users WHERE id=? AND deleted_at IS NULL", applicantId);
        Object orgIdValue = applicant.isEmpty() ? null : applicant.get(0).get("org_id");
        Long orgId = orgIdValue == null ? null : ((Number) orgIdValue).longValue();
        // 逐级向上找主管，最多 20 层
        for (int depth = 0; orgId != null && depth < 20; depth++) {
            Map<String, Object> org = orgById.get(orgId);
            if (org == null) break;
            if (org.get("leader_id") != null) reviewers.add(((Number) org.get("leader_id")).longValue());
            Object parent = org.get("parent_id");
            orgId = parent == null ? null : ((Number) parent).longValue();
        }
        // 部门里挂了主管角色的用户也算审批人
        if (orgIdValue != null) {
            reviewers.addAll(jdbcTemplate.queryForList(
                    "SELECT DISTINCT ur.user_id FROM user_roles ur JOIN roles r ON ur.role_id=r.id " +
                    "JOIN users u ON u.id=ur.user_id " +
                    "WHERE r.role_code='manager' AND r.status=1 AND r.deleted_at IS NULL AND ur.deleted_at IS NULL " +
                    "AND u.org_id=? AND u.deleted_at IS NULL AND u.status=1",
                    Long.class, ((Number) orgIdValue).longValue()));
        }
        // 没有主管时加上管理员
        reviewers.addAll(jdbcTemplate.queryForList(
                "SELECT DISTINCT ur.user_id FROM user_roles ur JOIN roles r ON ur.role_id=r.id " +
                "WHERE r.role_code='admin' AND r.status=1 AND r.deleted_at IS NULL AND ur.deleted_at IS NULL",
                Long.class));
        // 自己的申请自己不能批
        reviewers.remove(applicantId);
        return new ArrayList<>(reviewers);
    }

    // 判断是否系统管理员
    private boolean isAdmin(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON ur.role_id=r.id " +
                "WHERE ur.user_id=? AND r.role_code='admin' AND ur.deleted_at IS NULL AND r.status=1 AND r.deleted_at IS NULL",
                Integer.class, userId);
        return count != null && count > 0;
    }

    // 管理员看全部，其他人看自己部门或自己管的部门
    private boolean canViewDeptStats(Long userId, Long orgId) {
        if (userId == null || orgId == null) return false;
        if (isAdmin(userId)) return true;
        // 本人所在部门可以直接看
        List<Map<String, Object>> mine = jdbcTemplate.queryForList("SELECT org_id FROM users WHERE id=?", userId);
        if (!mine.isEmpty() && mine.get(0).get("org_id") != null
                && orgId.equals(((Number) mine.get(0).get("org_id")).longValue())) {
            return true;
        }
        // 自己管的部门和下级算管理范围
        List<Map<String, Object>> orgs = jdbcTemplate.queryForList(
                "SELECT id, parent_id, leader_id FROM organizations WHERE deleted_at IS NULL");
        Set<Long> allowed = new HashSet<>();
        for (Map<String, Object> org : orgs) {
            if (org.get("leader_id") != null && userId.equals(((Number) org.get("leader_id")).longValue())) {
                allowed.add(((Number) org.get("id")).longValue());
            }
        }
        // 逐层补下级部门，直到没有新增
        boolean grown = true;
        while (grown) {
            grown = false;
            for (Map<String, Object> org : orgs) {
                if (org.get("parent_id") == null) continue;
                long parentId = ((Number) org.get("parent_id")).longValue();
                if (allowed.contains(parentId) && allowed.add(((Number) org.get("id")).longValue())) {
                    grown = true;
                }
            }
        }
        return allowed.contains(orgId);
    }

    // 取请求体里的必填文本，缺失报错
    private String requireText(Map<String, Object> body, String key, String message) {
        Object value = body.get(key);
        if (value == null || value.toString().trim().isEmpty()) throw new RuntimeException(message);
        return value.toString().trim();
    }

    // 取请求体里的可选文本，缺失返回空串
    private String text(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : value.toString();
    }

    // 取请求体里的必填数字
    private Long requireLong(Map<String, Object> body, String key, String message) {
        Object value = body.get(key);
        if (value == null || value.toString().trim().isEmpty()) throw new RuntimeException(message);
        return Long.valueOf(value.toString().trim());
    }
}
