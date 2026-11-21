package com.officeautomationsystemapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.utilt.ExcelSafe;
import com.officeautomationsystemapi.entity.Attachments;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.entity.OperationLogs;
import com.officeautomationsystemapi.entity.Organizations;
import com.officeautomationsystemapi.entity.Roles;
import com.officeautomationsystemapi.entity.UserGroupMembers;
import com.officeautomationsystemapi.entity.UserGroups;
import com.officeautomationsystemapi.entity.UserRoles;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.entity.WorklogCategories;
import com.officeautomationsystemapi.entity.WorklogComments;
import com.officeautomationsystemapi.entity.WorklogViewPermissions;
import com.officeautomationsystemapi.entity.Worklogs;
import com.officeautomationsystemapi.repository.AttachmentsRepository;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import com.officeautomationsystemapi.repository.OperationLogsRepository;
import com.officeautomationsystemapi.repository.OrganizationsRepository;
import com.officeautomationsystemapi.repository.RolesRepository;
import com.officeautomationsystemapi.repository.UserGroupMembersRepository;
import com.officeautomationsystemapi.repository.UserGroupsRepository;
import com.officeautomationsystemapi.repository.UserRolesRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.repository.WorklogCategoriesRepository;
import com.officeautomationsystemapi.repository.WorklogCommentsRepository;
import com.officeautomationsystemapi.repository.WorklogViewPermissionsRepository;
import com.officeautomationsystemapi.repository.WorklogsRepository;
import com.officeautomationsystemapi.service.DataScopeService;
import com.officeautomationsystemapi.service.PermissionService;
import com.officeautomationsystemapi.service.WorklogService;
import com.officeautomationsystemapi.vo.WorklogCategoryVO;
import com.officeautomationsystemapi.vo.WorklogCommentVO;
import com.officeautomationsystemapi.vo.WorklogGrantVO;
import com.officeautomationsystemapi.vo.WorklogStatsVO;
import com.officeautomationsystemapi.vo.WorklogVO;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 工作日志：可见范围、增删改、评论、后台管理、统计、导出
@Service
public class WorklogServiceImpl implements WorklogService {

    @Autowired
    private WorklogsRepository worklogsRepository;
    @Autowired
    private WorklogCategoriesRepository worklogCategoriesRepository;
    @Autowired
    private WorklogCommentsRepository worklogCommentsRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private WorklogViewPermissionsRepository worklogViewPermissionsRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private OrganizationsRepository organizationsRepository;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private UserGroupsRepository userGroupsRepository;
    @Autowired
    private UserGroupMembersRepository userGroupMembersRepository;
    @Autowired
    private AttachmentsRepository attachmentsRepository;
    @Autowired
    private NotificationRecordsRepository notificationRecordsRepository;
    @Autowired
    private OperationLogsRepository operationLogsRepository;
    @Autowired
    private UserRolesRepository userRolesRepository;
    @Autowired
    private RolesRepository rolesRepository;
    @Autowired
    private DataScopeService dataScopeService;

    // 评论里 @某人的写法
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([^@\\s，。,.!！?？<>()【】\\[\\]]+)");

    // 取用户的部门 ID
    private Long getUserOrgId(Long userId) {
        Users u = usersRepository.selectById(userId);
        return u == null ? null : u.getOrg_id();
    }

    // 递归收集某个部门和它的全部子部门 ID
    private Set<Long> getAllChildOrgIds(Long orgId) {
        Set<Long> ids = new LinkedHashSet<>();
        if (orgId == null) return ids;
        ids.add(orgId);
        collectChildren(orgId, organizationsRepository.selectList(null), ids);
        return ids;
    }

    private void collectChildren(Long parentId, List<Organizations> all, Set<Long> ids) {
        for (Organizations o : all) {
            if (o.getParent_id() != null && o.getParent_id().equals(parentId) && ids.add(o.getId())) {
                collectChildren(o.getId(), all, ids);
            }
        }
    }

    // 查某部门及其子部门下的所有用户 ID
    private List<Long> userIdsByOrg(Long orgId) {
        Set<Long> orgIds = getAllChildOrgIds(orgId);
        if (orgIds.isEmpty()) return new ArrayList<>();
        return usersRepository.selectList(new QueryWrapper<Users>().in("org_id", orgIds))
                .stream().map(Users::getId).collect(Collectors.toList());
    }

    // 当前用户能看到哪些人的日志，返回 null 表示能看全部
    private Set<Long> getViewableUserIds(Long userId) {
        List<String> roles = permissionService.getUserRoleCodes(userId);
        // 管理员和总经理能看全公司
        if (permissionService.isAdmin(userId) || roles.contains("gm")) return null;

        Set<Long> ids = new HashSet<>();
        ids.add(userId);
        Long orgId = getUserOrgId(userId);

        // 部门主管能看本部门及子部门的人
        if (roles.contains("manager") && orgId != null) {
            for (Users u : usersRepository.selectList(new QueryWrapper<Users>().in("org_id", getAllChildOrgIds(orgId)))) {
                ids.add(u.getId());
            }
        }

        // 三种查看授权取并集：按人、按群组、按部门
        for (WorklogViewPermissions p : worklogViewPermissionsRepository.selectList(new QueryWrapper<>())) {
            if (p.getPermission_type() == null) continue;
            boolean matched;
            if (p.getPermission_type() == 1) {
                matched = p.getViewer_id() != null && p.getViewer_id().equals(userId);
            } else if (p.getPermission_type() == 2) {
                matched = inGroup(userId, p.getGroup_id());
            } else if (p.getPermission_type() == 3) {
                matched = orgId != null && p.getDept_id() != null
                        && getAllChildOrgIds(p.getDept_id()).contains(orgId);
            } else {
                matched = false;
            }
            if (matched && p.getTarget_id() != null) ids.add(p.getTarget_id());
        }
        return ids;
    }

    // 用户是否在某个群组里
    private boolean inGroup(Long userId, Long groupId) {
        if (groupId == null) return false;
        Long count = userGroupMembersRepository.selectCount(new QueryWrapper<UserGroupMembers>()
                .eq("group_id", groupId).eq("user_id", userId));
        return count != null && count > 0;
    }

    // 这篇日志对当前用户是否可见
    private boolean canView(Long userId, Worklogs w) {
        Set<Long> ids = getViewableUserIds(userId);
        return ids == null || ids.contains(w.getUser_id());
    }

    // 组装日志 VO，带上撰写人、部门、类别和附件信息
    private WorklogVO toVO(Worklogs w, Long currentUserId) {
        WorklogVO vo = new WorklogVO();
        BeanUtils.copyProperties(w, vo);
        vo.setMine(w.getUser_id() != null && w.getUser_id().equals(currentUserId));
        Users u = usersRepository.selectById(w.getUser_id());
        if (u != null) {
            vo.setUsername(u.getUsername());
            vo.setReal_name(u.getReal_name());
            vo.setOrg_id(u.getOrg_id());
            Organizations o = u.getOrg_id() == null ? null : organizationsRepository.selectById(u.getOrg_id());
            if (o != null) vo.setOrg_name(o.getOrg_name());
        }
        if (w.getCategory_id() != null) {
            WorklogCategories c = worklogCategoriesRepository.selectById(w.getCategory_id());
            if (c != null) vo.setCategory_name(c.getCategory_name());
        }
        if (w.getAttachment_id() != null) {
            Attachments a = attachmentsRepository.selectById(w.getAttachment_id());
            if (a != null) {
                vo.setAttachment_id(a.getId());
                vo.setAttachment_file_name(a.getFile_name());
                vo.setAttachment_file_size(a.getFile_size());
                vo.setAttachment_download_url("/api/worklogs/attachments/" + a.getId() + "/download");
            }
        }
        return vo;
    }

    // 分页结果里的日志逐条转成 VO，保留分页信息
    private Page<WorklogVO> toVoPage(Page<Worklogs> p, Long currentUserId) {
        Page<WorklogVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(x -> toVO(x, currentUserId)).collect(Collectors.toList()));
        return result;
    }

    // 组装评论 VO，离职的人名字后面标注一下
    private WorklogCommentVO toCommentVO(WorklogComments c) {
        WorklogCommentVO vo = new WorklogCommentVO();
        BeanUtils.copyProperties(c, vo);
        vo.setDeleted(false);
        Users u = usersRepository.selectById(c.getUser_id());
        if (u != null) {
            vo.setUsername(u.getUsername());
            String name = displayName(u);
            if (u.getStatus() != null && u.getStatus() == 0) name = name + "（已离职）";
            vo.setReal_name(name);
            vo.setAvatar(u.getAvatar());
        }
        return vo;
    }

    // 优先显示真实姓名，没有就用登录名
    private String displayName(Users u) {
        return u.getReal_name() != null ? u.getReal_name() : u.getUsername();
    }

    // 我的日志分页
    @Override
    public Page<WorklogVO> myPage(Long userId, int pageNum, int pageSize, String logDate, Long categoryId, String keyword) {
        QueryWrapper<Worklogs> w = new QueryWrapper<>();
        w.eq("user_id", userId);
        if (logDate != null && !logDate.isEmpty()) w.eq("log_date", logDate);
        if (categoryId != null) w.eq("category_id", categoryId);
        if (keyword != null && !keyword.isEmpty()) {
            w.and(x -> x.like("title", keyword).or().like("content", keyword));
        }
        w.orderByDesc("log_date").orderByDesc("created_at");
        return toVoPage(worklogsRepository.selectPage(new Page<>(pageNum, pageSize), w), userId);
    }

    // 新增日志，写历史日期算补写，超过30天要先有审批通过的授权
    @Override
    @Transactional
    public Long create(Long userId, WorklogVO vo) {
        LocalDate today = LocalDate.now();
        if (vo.getLog_date() == null) vo.setLog_date(today);
        if (vo.getLog_date().isAfter(today)) {
            throw new RuntimeException("不能撰写未来日期的日志");
        }
        boolean supplement = vo.getLog_date().isBefore(today);
        if (supplement && vo.getLog_date().isBefore(today.minusDays(30))) {
            Integer approved = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM worklog_supplement_approvals " +
                    "WHERE user_id=? AND status=1 AND expire_at > NOW() " +
                    "AND start_date <= ? AND end_date >= ?",
                    Integer.class, userId, vo.getLog_date(), vo.getLog_date());
            if (approved == null || approved == 0) {
                throw new RuntimeException("超过30天的日志需发起『日志补写申请』流程，审批通过后方可补写");
            }
        }
        if (vo.getTitle() == null || vo.getTitle().trim().isEmpty()) {
            throw new RuntimeException("日志标题不能为空");
        }
        if (vo.getContent() == null || vo.getContent().trim().isEmpty()) {
            throw new RuntimeException("日志内容不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        Worklogs w = new Worklogs();
        w.setUser_id(userId);
        w.setLog_date(vo.getLog_date());
        w.setCategory_id(vo.getCategory_id());
        w.setTitle(vo.getTitle());
        w.setContent(vo.getContent());
        w.setWork_summary(vo.getWork_summary());
        w.setNext_plan(vo.getNext_plan());
        w.setStatus(1);
        w.setIs_supplement(supplement ? 1 : 0);
        w.setSubmit_time(now);
        w.setView_count(0);
        w.setComment_count(0);
        w.setAttachment_id(vo.getAttachment_id());
        w.setCreated_at(now);
        w.setUpdated_at(now);
        worklogsRepository.insert(w);

        // 附件绑到刚存的这篇日志上，只能绑自己上传的附件
        if (vo.getAttachment_id() != null) {
            attachmentsRepository.update(null, new UpdateWrapper<Attachments>()
                    .eq("id", vo.getAttachment_id()).eq("uploader_id", userId)
                    .set("target_type", "worklog").set("target_id", w.getId()));
        }
        // 日志写完就把当天发给这个人的日志提醒删掉
        try {
            jdbcTemplate.update(
                    "DELETE FROM notification_records WHERE user_id=? AND biz_type='worklog_remind' AND DATE(created_at)=CURDATE()",
                    userId);
        } catch (Exception ignore) {
        }
        return w.getId();
    }

    // 修改日志，只能改自己当天写的
    @Override
    @Transactional
    public void update(Long userId, WorklogVO vo) {
        if (vo.getId() == null) throw new RuntimeException("日志不存在");
        Worklogs w = worklogsRepository.selectById(vo.getId());
        if (w == null) throw new RuntimeException("日志不存在");
        if (!userId.equals(w.getUser_id())) throw new RuntimeException("只能修改自己的日志");
        if (!LocalDate.now().equals(w.getLog_date())) {
            throw new RuntimeException("非当天的日志不可修改，只能删除");
        }
        w.setCategory_id(vo.getCategory_id());
        w.setTitle(vo.getTitle());
        w.setContent(vo.getContent());
        w.setWork_summary(vo.getWork_summary());
        w.setNext_plan(vo.getNext_plan());
        // 换了附件就把新附件绑过来
        if (vo.getAttachment_id() != null && !vo.getAttachment_id().equals(w.getAttachment_id())) {
            attachmentsRepository.update(null, new UpdateWrapper<Attachments>()
                    .eq("id", vo.getAttachment_id()).eq("uploader_id", userId)
                    .set("target_type", "worklog").set("target_id", w.getId()));
        }
        w.setAttachment_id(vo.getAttachment_id());
        w.setUpdated_at(LocalDateTime.now());
        worklogsRepository.updateById(w);
    }

    // 删除自己的日志，走软删除
    @Override
    @Transactional
    public void myDelete(Long userId, Long id) {
        Worklogs w = worklogsRepository.selectById(id);
        if (w == null) throw new RuntimeException("日志不存在");
        if (!userId.equals(w.getUser_id())) throw new RuntimeException("只能删除自己的日志");
        worklogsRepository.deleteById(id);
    }

    // 我的日志详情
    @Override
    public WorklogVO myDetail(Long userId, Long id) {
        Worklogs w = worklogsRepository.selectById(id);
        if (w == null || !userId.equals(w.getUser_id())) throw new RuntimeException("日志不存在");
        return toVO(w, userId);
    }

    // 只看能看到的日志
    @Override
    public Page<WorklogVO> viewPage(Long userId, int pageNum, int pageSize, String keyword, Long categoryId, Long orgId) {
        Set<Long> viewable = getViewableUserIds(userId);
        if (viewable != null && viewable.isEmpty()) return new Page<>(pageNum, pageSize, 0);

        QueryWrapper<Worklogs> w = new QueryWrapper<>();
        if (viewable != null) w.in("user_id", viewable);
        if (categoryId != null) w.eq("category_id", categoryId);
        if (keyword != null && !keyword.isEmpty()) {
            w.and(x -> x.like("title", keyword).or().like("content", keyword));
        }
        if (orgId != null) {
            List<Long> userIds = userIdsByOrg(orgId);
            if (userIds.isEmpty()) return new Page<>(pageNum, pageSize, 0);
            w.in("user_id", userIds);
        }
        w.orderByDesc("log_date").orderByDesc("created_at");
        return toVoPage(worklogsRepository.selectPage(new Page<>(pageNum, pageSize), w), userId);
    }

    // 查看日志详情，看一次阅读数加一
    @Override
    @Transactional
    public WorklogVO viewDetail(Long userId, Long id) {
        Worklogs w = worklogsRepository.selectById(id);
        if (w == null) throw new RuntimeException("日志不存在或已删除");
        if (!canView(userId, w)) throw new RuntimeException("无权查看该日志");
        // 在数据库里直接加一，避免两个人同时看时把数字覆盖掉
        jdbcTemplate.update("UPDATE worklogs SET view_count = COALESCE(view_count, 0) + 1 WHERE id = ?", id);
        w.setView_count((w.getView_count() == null ? 0 : w.getView_count()) + 1);
        return toVO(w, userId);
    }

    // 评论列表
    @Override
    public List<WorklogCommentVO> listComments(Long userId, Long worklogId) {
        Worklogs w = worklogsRepository.selectById(worklogId);
        if (w == null) throw new RuntimeException("日志不存在");
        if (!canView(userId, w)) throw new RuntimeException("无权查看该日志");
        return worklogCommentsRepository.selectList(new QueryWrapper<WorklogComments>()
                        .eq("worklog_id", worklogId).orderByAsc("comment_time"))
                .stream().map(this::toCommentVO).collect(Collectors.toList());
    }

    // 发表评论，顺带处理 @提醒、回复提醒和评论数
    @Override
    @Transactional
    public void addComment(Long userId, Long worklogId, String content, Long parentId) {
        Worklogs w = worklogsRepository.selectById(worklogId);
        if (w == null) throw new RuntimeException("日志不存在");
        if (!canView(userId, w)) throw new RuntimeException("无权查看该日志，不能评论");
        if (content == null || content.trim().isEmpty()) throw new RuntimeException("评论内容不能为空");
        if (content.length() > 1000) throw new RuntimeException("评论内容不能超过1000字");

        // 解析评论里的 @姓名，能看日志的同名人都通知，Set 去重
        Set<Long> mentionedIds = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            List<Users> targets = usersRepository.selectList(new QueryWrapper<Users>().eq("real_name", name));
            boolean anyViewable = false;
            for (Users target : targets) {
                if (!canView(target.getId(), w)) continue;
                mentionedIds.add(target.getId());
                anyViewable = true;
            }
            // 同名的都看不了这篇日志才报错，避免一人没权限发不出去
            if (!targets.isEmpty() && !anyViewable) {
                throw new RuntimeException("用户「" + name + "」无权限查看此日志，无法@");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        WorklogComments c = new WorklogComments();
        c.setWorklog_id(worklogId);
        c.setUser_id(userId);
        c.setParent_id(parentId == null ? 0L : parentId);
        c.setContent(content);
        c.setComment_time(now);
        c.setCreated_at(now);
        c.setUpdated_at(now);
        worklogCommentsRepository.insert(c);
        jdbcTemplate.update("UPDATE worklogs SET comment_count = COALESCE(comment_count, 0) + 1 WHERE id = ?", worklogId);

        Users me = usersRepository.selectById(userId);
        String meName = (me != null && me.getReal_name() != null) ? me.getReal_name() : "有人";
        String logTitle = (w.getTitle() != null && !w.getTitle().isEmpty()) ? w.getTitle() : "未命名日志";
        String brief = truncateText(content, 60);
        // 通知日志作者，评论者自己就不用收了
        if (!userId.equals(w.getUser_id())) {
            insertNotification(w.getUser_id(), "日志新评论",
                    meName + " 在你的日志《" + logTitle + "》中评论：" + brief,
                    "worklog_comment", worklogId, "/home/worklog");
        }
        // 通知被回复的人，评论者和日志作者已经在上面处理过
        if (parentId != null && parentId > 0) {
            WorklogComments parent = worklogCommentsRepository.selectById(parentId);
            if (parent != null && !parent.getUser_id().equals(userId) && !parent.getUser_id().equals(w.getUser_id())) {
                insertNotification(parent.getUser_id(), "评论被回复",
                        meName + " 回复了你的评论：" + brief,
                        "worklog_comment", worklogId, "/home/worklog");
            }
        }
        // 通知被 @ 的人
        for (Long uid : mentionedIds) {
            if (uid.equals(userId)) continue;
            insertNotification(uid, "日志@提醒",
                    meName + " 在日志《" + logTitle + "》中@了你：" + brief,
                    "worklog_comment", worklogId, "/home/worklog");
        }
    }

    // 把评论内容压成一行，超过长度就截断
    private String truncateText(String s, int n) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > n ? t.substring(0, n) + "..." : t;
    }

    // 删除评论，日志作者、评论作者、管理员都有权限
    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        WorklogComments c = worklogCommentsRepository.selectById(commentId);
        if (c == null) throw new RuntimeException("评论不存在");
        Worklogs w = worklogsRepository.selectById(c.getWorklog_id());
        boolean isLogAuthor = w != null && userId.equals(w.getUser_id());
        boolean isCommentAuthor = userId.equals(c.getUser_id());
        if (!isLogAuthor && !isCommentAuthor && !permissionService.isAdmin(userId)) {
            throw new RuntimeException("无权删除该评论");
        }
        worklogCommentsRepository.deleteById(commentId);
        if (w != null) {
            jdbcTemplate.update(
                    "UPDATE worklogs SET comment_count = GREATEST(COALESCE(comment_count, 0) - 1, 0) WHERE id = ?",
                    w.getId());
        }
    }

    // 后台日志分页，先按调用者数据范围过滤，再叠加部门等条件
    @Override
    public Page<WorklogVO> adminPage(Long viewerId, int pageNum, int pageSize, String keyword, Integer status,
                                     Long categoryId, Long orgId, String startDate, String endDate) {
        QueryWrapper<Worklogs> w = new QueryWrapper<>();
        if (status != null) w.eq("status", status);
        if (categoryId != null) w.eq("category_id", categoryId);
        if (startDate != null && !startDate.isEmpty()) w.ge("log_date", startDate);
        if (endDate != null && !endDate.isEmpty()) w.le("log_date", endDate);
        if (keyword != null && !keyword.isEmpty()) {
            w.and(x -> x.like("title", keyword).or().like("content", keyword));
        }
        List<Long> visibleUsers = dataScopeService.visibleUserIds(viewerId);
        if (visibleUsers != null && visibleUsers.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }
        List<Long> deptUsers = orgId == null ? null : userIdsByOrg(orgId);
        if (deptUsers != null && deptUsers.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }
        if (visibleUsers == null) {
            if (deptUsers != null) w.in("user_id", deptUsers);
        } else if (deptUsers == null) {
            w.in("user_id", visibleUsers);
        } else {
            // 部门筛选和数据范围取交集，两个条件都不能放过
            List<Long> effective = deptUsers.stream().filter(visibleUsers::contains).collect(Collectors.toList());
            if (effective.isEmpty()) return new Page<>(pageNum, pageSize, 0);
            w.in("user_id", effective);
        }
        w.orderByDesc("log_date").orderByDesc("created_at");
        return toVoPage(worklogsRepository.selectPage(new Page<>(pageNum, pageSize), w), 0L);
    }

    // 回收站分页，只查已删除的日志，同样按数据范围过滤
    @Override
    public Page<WorklogVO> recyclePage(Long viewerId, int pageNum, int pageSize, String keyword) {
        List<Long> visibleUsers = dataScopeService.visibleUserIds(viewerId);
        if (visibleUsers != null && visibleUsers.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }
        IPage<Worklogs> page = worklogsRepository.selectRecyclePage(new Page<>(pageNum, pageSize), keyword, visibleUsers);
        Page<Worklogs> p = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        p.setRecords(page.getRecords());
        return toVoPage(p, 0L);
    }

    // 回收站里彻底删除，这些记录已经软删过，直接物理删掉
    @Override
    @Transactional
    public void adminDelete(Long id) {
        jdbcTemplate.update("DELETE FROM worklogs WHERE id = ?", id);
    }

    // 从回收站恢复，逻辑删除会自动过滤，所以直接走 SQL
    @Override
    @Transactional
    public void adminRestore(Long id) {
        worklogsRepository.restoreById(id);
    }

    // 启用中的类别
    @Override
    public List<WorklogCategoryVO> listCategories() {
        return worklogCategoriesRepository.selectList(new QueryWrapper<WorklogCategories>()
                        .eq("status", 1).orderByAsc("sort_order"))
                .stream().map(this::toCategoryVO).collect(Collectors.toList());
    }

    // 全部类别，含停用的
    @Override
    public List<WorklogCategoryVO> adminCategories() {
        return worklogCategoriesRepository.selectList(new QueryWrapper<WorklogCategories>()
                        .orderByAsc("sort_order"))
                .stream().map(this::toCategoryVO).collect(Collectors.toList());
    }

    private WorklogCategoryVO toCategoryVO(WorklogCategories c) {
        WorklogCategoryVO vo = new WorklogCategoryVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }

    // 新增类别
    @Override
    @Transactional
    public void addCategory(Long userId, WorklogCategoryVO vo) {
        if (vo.getCategory_name() == null || vo.getCategory_name().trim().isEmpty()) {
            throw new RuntimeException("类别名称不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        WorklogCategories c = new WorklogCategories();
        c.setCategory_name(vo.getCategory_name());
        c.setCategory_code(vo.getCategory_code());
        c.setDescription(vo.getDescription());
        c.setSort_order(vo.getSort_order() == null ? 0 : vo.getSort_order());
        c.setStatus(vo.getStatus() == null ? 1 : vo.getStatus());
        c.setCreator_id(userId);
        c.setCreated_at(now);
        c.setUpdated_at(now);
        worklogCategoriesRepository.insert(c);
    }

    // 修改类别
    @Override
    @Transactional
    public void updateCategory(WorklogCategoryVO vo) {
        WorklogCategories c = worklogCategoriesRepository.selectById(vo.getId());
        if (c == null) throw new RuntimeException("类别不存在");
        c.setCategory_name(vo.getCategory_name());
        c.setCategory_code(vo.getCategory_code());
        c.setDescription(vo.getDescription());
        c.setSort_order(vo.getSort_order() == null ? 0 : vo.getSort_order());
        c.setStatus(vo.getStatus() == null ? 1 : vo.getStatus());
        c.setUpdated_at(LocalDateTime.now());
        worklogCategoriesRepository.updateById(c);
    }

    // 删除类别，还有日志在用的类别不能删
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Long cnt = worklogsRepository.selectCount(new QueryWrapper<Worklogs>().eq("category_id", id));
        if (cnt != null && cnt > 0) {
            throw new RuntimeException("该类别下已有日志，不能删除");
        }
        worklogCategoriesRepository.deleteById(id);
    }

    // 查看授权分页，关键词按被查看人和查看人的姓名过滤
    @Override
    public Page<WorklogGrantVO> grantPage(int pageNum, int pageSize, String keyword) {
        QueryWrapper<WorklogViewPermissions> q = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            List<Long> uids = usersRepository.selectList(new QueryWrapper<Users>()
                            .like("real_name", keyword).or().like("username", keyword))
                    .stream().map(Users::getId).collect(Collectors.toList());
            if (uids.isEmpty()) return new Page<>(pageNum, pageSize, 0);
            q.and(x -> x.in("target_id", uids).or().in("viewer_id", uids));
        }
        q.orderByDesc("grant_time");
        Page<WorklogViewPermissions> p = worklogViewPermissionsRepository.selectPage(new Page<>(pageNum, pageSize), q);
        Page<WorklogGrantVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toGrantVO).collect(Collectors.toList()));
        return result;
    }

    // 组装授权 VO，把被查看人、查看人、群组、部门、授权人的名字都补上
    private WorklogGrantVO toGrantVO(WorklogViewPermissions p) {
        WorklogGrantVO vo = new WorklogGrantVO();
        BeanUtils.copyProperties(p, vo);
        Users t = usersRepository.selectById(p.getTarget_id());
        if (t != null) vo.setTarget_name(displayName(t));
        if (p.getViewer_id() != null && p.getViewer_id() != 0) {
            Users v = usersRepository.selectById(p.getViewer_id());
            if (v != null) vo.setViewer_name(displayName(v));
        }
        if (p.getGroup_id() != null) {
            UserGroups g = userGroupsRepository.selectById(p.getGroup_id());
            if (g != null) vo.setGroup_name(g.getGroup_name());
        }
        if (p.getDept_id() != null) {
            Organizations o = organizationsRepository.selectById(p.getDept_id());
            if (o != null) vo.setDept_name(o.getOrg_name());
        }
        Users g = usersRepository.selectById(p.getGranter_id());
        if (g != null) vo.setGranter_name(displayName(g));
        return vo;
    }

    // 新增查看授权，按人、按群组、按部门三选一
    @Override
    @Transactional
    public void addGrant(Long granterId, WorklogGrantVO vo) {
        if (vo.getTarget_id() == null) throw new RuntimeException("请选择被查看人");
        if (vo.getPermission_type() == null) throw new RuntimeException("请选择授权方式");
        WorklogViewPermissions p = new WorklogViewPermissions();
        p.setTarget_id(vo.getTarget_id());
        p.setPermission_type(vo.getPermission_type());
        if (vo.getPermission_type() == 1) {
            if (vo.getViewer_id() == null) throw new RuntimeException("请选择查看人");
            p.setViewer_id(vo.getViewer_id());
        } else if (vo.getPermission_type() == 2) {
            if (vo.getGroup_id() == null) throw new RuntimeException("请选择用户群组");
            p.setViewer_id(0L);
            p.setGroup_id(vo.getGroup_id());
        } else if (vo.getPermission_type() == 3) {
            if (vo.getDept_id() == null) throw new RuntimeException("请选择部门");
            p.setViewer_id(0L);
            p.setDept_id(vo.getDept_id());
        } else {
            throw new RuntimeException("不支持的授权方式");
        }
        LocalDateTime now = LocalDateTime.now();
        p.setGrant_time(now);
        p.setGranter_id(granterId);
        p.setCreated_at(now);
        p.setUpdated_at(now);
        worklogViewPermissionsRepository.insert(p);
    }

    // 取消查看授权
    @Override
    @Transactional
    public void deleteGrant(Long id) {
        worklogViewPermissionsRepository.deleteById(id);
    }

    // 日志统计：条数、完成率、提交时段、字数、热门日志、排名
    @Override
    public WorklogStatsVO stats(Long userId, Long orgId) {
        WorklogStatsVO vo = new WorklogStatsVO();
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        vo.setMyMonthCount(worklogsRepository.countByUserMonth(userId, month));
        vo.setMyMonthDays(worklogsRepository.countDaysByUserMonth(userId, month));
        vo.setMyViewCount(toInt(worklogsRepository.sumViewCount(userId)));
        vo.setMyCommentCount(toInt(worklogsRepository.sumCommentCount(userId)));

        // 完成率：本月写日志天数除以工作日数，只算周一到周五
        YearMonth yearMonth = YearMonth.now();
        int workdays = 0;
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            DayOfWeek dow = yearMonth.atDay(day).getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) workdays++;
        }
        long wroteDays = vo.getMyMonthDays() == null ? 0 : vo.getMyMonthDays();
        vo.setMyCompletionRate(workdays > 0 ? (int) Math.round(wroteDays * 100.0 / workdays) : 0);

        // 本月我的日志，用来算提交时段和字数
        List<Worklogs> monthLogs = worklogsRepository.selectList(new QueryWrapper<Worklogs>()
                .eq("user_id", userId).likeRight("log_date", month).isNotNull("submit_time"));
        int morning = 0, afternoon = 0, evening = 0;
        long totalWords = 0;
        for (Worklogs l : monthLogs) {
            if (l.getSubmit_time() != null) {
                int hour = l.getSubmit_time().getHour();
                if (hour < 12) morning++;
                else if (hour < 18) afternoon++;
                else evening++;
            }
            totalWords += stripHtml(l.getContent()).length();
        }
        List<Map<String, Object>> timeDist = new ArrayList<>();
        timeDist.add(buildDist("上午", morning));
        timeDist.add(buildDist("下午", afternoon));
        timeDist.add(buildDist("晚上", evening));
        vo.setTimeDist(timeDist);

        Map<String, Object> wordStats = new HashMap<>();
        wordStats.put("totalWords", totalWords);
        wordStats.put("avgWords", monthLogs.isEmpty() ? 0 : (int) Math.round(totalWords * 1.0 / monthLogs.size()));
        vo.setWordStats(wordStats);

        // 评论最多的前五篇日志，只在自己能看的范围里找
        Set<Long> viewable = getViewableUserIds(userId);
        List<Map<String, Object>> commentTop = new ArrayList<>();
        // viewable 为 null 是管理员不过滤，空集合必须返回空列表
        if (viewable == null || !viewable.isEmpty()) {
            QueryWrapper<Worklogs> topQuery = new QueryWrapper<>();
            if (viewable != null) topQuery.in("user_id", viewable);
            topQuery.gt("comment_count", 0).likeRight("log_date", month)
                    .orderByDesc("comment_count").last("LIMIT 5");
            for (Worklogs l : worklogsRepository.selectList(topQuery)) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", l.getId());
                item.put("title", l.getTitle() == null ? "(无标题)" : l.getTitle());
                item.put("comment_count", l.getComment_count());
                commentTop.add(item);
            }
        }
        vo.setCommentTop(commentTop);

        // 部门排名：统计传入部门及其子部门里每个人写了多少篇
        List<Map<String, Object>> deptRank = new ArrayList<>();
        if (orgId != null) {
            List<Users> deptUsers = usersRepository.selectList(new QueryWrapper<Users>()
                    .in("org_id", getAllChildOrgIds(orgId)));
            if (!deptUsers.isEmpty()) {
                Map<Long, Users> userMap = deptUsers.stream().collect(Collectors.toMap(Users::getId, x -> x));
                for (Map<String, Object> row : worklogsRepository.countByUserIds(new ArrayList<>(userMap.keySet()))) {
                    Long uid = ((Number) row.get("user_id")).longValue();
                    Users u = userMap.get(uid);
                    Map<String, Object> item = new HashMap<>();
                    item.put("user_id", uid);
                    item.put("real_name", u == null ? "-" : displayName(u));
                    item.put("count", row.get("cnt"));
                    deptRank.add(item);
                }
            }
        }
        vo.setDeptRank(deptRank);
        return vo;
    }

    // 统计里空值统一转成 0
    private int toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private Map<String, Object> buildDist(String period, int count) {
        Map<String, Object> m = new HashMap<>();
        m.put("period", period);
        m.put("count", count);
        return m;
    }

    // 导出 Excel，员工导出自己的，管理员导出全部且最多5000条
    @Override
    public void exportExcel(Long userId, boolean mineOnly, String keyword, Long categoryId, Long orgId,
                            String startDate, String endDate, HttpServletResponse response) {
        QueryWrapper<Worklogs> w = new QueryWrapper<>();
        if (mineOnly) {
            w.eq("user_id", userId);
        } else {
            // 导出和列表口径保持一致：部门筛选叠在数据范围之上
            List<Long> visibleUsers = dataScopeService.visibleUserIds(userId);
            List<Long> deptUsers = orgId == null ? null : userIdsByOrg(orgId);
            if (visibleUsers != null && visibleUsers.isEmpty()) {
                w.in("user_id", java.util.Collections.singletonList(-1L));
            } else if (visibleUsers == null) {
                if (deptUsers != null) w.in("user_id", deptUsers);
            } else if (deptUsers == null) {
                w.in("user_id", visibleUsers);
            } else {
                List<Long> effective = deptUsers.stream().filter(visibleUsers::contains).collect(Collectors.toList());
                w.in("user_id", effective.isEmpty() ? java.util.Collections.singletonList(-1L) : effective);
            }
        }
        if (categoryId != null) w.eq("category_id", categoryId);
        if (startDate != null && !startDate.isEmpty()) w.ge("log_date", startDate);
        if (endDate != null && !endDate.isEmpty()) w.le("log_date", endDate);
        if (keyword != null && !keyword.isEmpty()) {
            w.and(x -> x.like("title", keyword).or().like("content", keyword));
        }
        w.orderByDesc("log_date");
        if (!mineOnly) w.last("LIMIT 5000");
        List<Worklogs> list = worklogsRepository.selectList(w);

        try (Workbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet("工作日志");
            String[] headers = {"用户名", "部门", "日志日期", "日志标题", "日志内容", "评论数", "状态"};
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) head.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (Worklogs log : list) {
                Users u = usersRepository.selectById(log.getUser_id());
                Organizations o = u == null || u.getOrg_id() == null ? null : organizationsRepository.selectById(u.getOrg_id());
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(u == null ? "-" : displayName(u));
                row.createCell(1).setCellValue(o == null ? "-" : o.getOrg_name());
                row.createCell(2).setCellValue(log.getLog_date() == null ? "" : log.getLog_date().toString());
                row.createCell(3).setCellValue(log.getTitle() == null ? "" : log.getTitle());
                row.createCell(4).setCellValue(stripHtml(log.getContent()));
                row.createCell(5).setCellValue(log.getComment_count() == null ? 0 : log.getComment_count());
                row.createCell(6).setCellValue(log.getStatus() == null ? "-" : statusText(log.getStatus()));
            }
            for (int i = 0; i < headers.length; i++) sheet.setColumnWidth(i, i == 4 ? 60 * 256 : 18 * 256);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode("工作日志导出.xlsx", StandardCharsets.UTF_8));
            ExcelSafe.sanitize(book);
            book.write(response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    // 下载附件，有查看权限的人才能下，下载数加一并记日志
    @Override
    public void downloadAttachment(Long userId, Long aid, HttpServletResponse response) {
        Attachments a = attachmentsRepository.selectById(aid);
        if (a == null || !"worklog".equals(a.getTarget_type()) || a.getTarget_id() == null) {
            throw new RuntimeException("附件不存在");
        }
        Worklogs w = worklogsRepository.selectById(a.getTarget_id());
        if (w == null) throw new RuntimeException("日志不存在或已删除");
        if (!canView(userId, w)) throw new RuntimeException("无权下载该日志的附件");
        if (a.getFile_path() == null || a.getFile_path().isBlank()) {
            throw new RuntimeException("附件文件路径为空，请联系管理员");
        }
        File f = new File(a.getFile_path());
        if (!f.isFile() || !f.canRead()) throw new RuntimeException("文件已丢失或无法读取，请联系管理员");
        // 先更新下载次数并记日志，再写文件，计数失败还没输出字节
        a.setDownload_count((a.getDownload_count() == null ? 0 : a.getDownload_count()) + 1);
        a.setUpdated_at(LocalDateTime.now());
        attachmentsRepository.updateById(a);
        saveDownloadLog(userId, aid);
        try {
            response.setContentType(a.getFile_type() == null ? "application/octet-stream" : a.getFile_type());
            String fileName = a.getFile_name() == null ? "附件" : a.getFile_name();
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
            try (InputStream in = new FileInputStream(f)) {
                StreamUtils.copy(in, response.getOutputStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("下载失败：" + e.getMessage());
        }
    }

    // 记一条附件下载操作日志，写失败也不影响下载
    private void saveDownloadLog(Long userId, Long attachmentId) {
        try {
            Users u = usersRepository.selectById(userId);
            OperationLogs log = new OperationLogs();
            log.setUser_id(userId);
            log.setUsername(u != null ? u.getUsername() : String.valueOf(userId));
            log.setModule("工作日志");
            log.setAction("下载附件");
            log.setMethod("GET");
            log.setUrl("/api/worklogs/attachments/" + attachmentId + "/download");
            log.setTarget_type("worklog_attachment");
            log.setTarget_id(attachmentId);
            log.setStatus(1);
            log.setCreated_at(LocalDateTime.now());
            log.setUpdated_at(LocalDateTime.now());
            operationLogsRepository.insert(log);
        } catch (Exception ignored) {
        }
    }

    // 工作日下午提醒今天还没写日志的人，每人每天只提醒一次
    @Override
    public void sendUnwrittenReminders() {
        LocalDate today = LocalDate.now();
        List<Users> users = usersRepository.selectList(new QueryWrapper<Users>().eq("status", 1));
        int sent = 0;
        for (Users u : users) {
            Long cnt = worklogsRepository.selectCount(new QueryWrapper<Worklogs>()
                    .eq("user_id", u.getId()).eq("log_date", today));
            if (cnt != null && cnt > 0) continue;
            if (hasRemindToday(u.getId())) continue;
            insertNotification(u.getId(), "日志提醒", "您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。",
                    "worklog_remind", null, "/home/worklog");
            sent++;
        }
        System.out.println("[工作日志提醒] 已向 " + sent + " 名未写日志员工发送站内提醒");
    }

    // 今天是否已经发过日志提醒
    private boolean hasRemindToday(Long userId) {
        Integer already = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_records WHERE user_id=? AND biz_type='worklog_remind' AND DATE(created_at)=CURDATE()",
                Integer.class, userId);
        return already != null && already > 0;
    }

    // 周一给部门主管发上周本部门的日志完成率
    @Override
    public void sendWeeklySummary() {
        LocalDate lastMon = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastSun = lastMon.plusDays(6);
        List<Users> users = usersRepository.selectList(new QueryWrapper<Users>().eq("status", 1));
        Map<Long, List<Users>> byOrg = new HashMap<>();
        for (Users u : users) {
            if (u.getOrg_id() == null) continue;
            byOrg.computeIfAbsent(u.getOrg_id(), k -> new ArrayList<>()).add(u);
        }
        // 部门主管按角色编码查，不写死角色主键
        List<Long> managerRoleIds = rolesRepository.selectList(new QueryWrapper<Roles>().eq("role_code", "manager"))
                .stream().map(Roles::getId).collect(Collectors.toList());
        if (managerRoleIds.isEmpty()) {
            System.out.println("[工作日志周报] 未找到部门主管角色，跳过发送");
            return;
        }
        // 一个人可能挂多个主管角色，先去重，避免收到多份周报
        Set<Long> managerIds = new LinkedHashSet<>();
        for (UserRoles ur : userRolesRepository.selectList(new QueryWrapper<UserRoles>().in("role_id", managerRoleIds))) {
            managerIds.add(ur.getUser_id());
        }
        int sent = 0;
        for (Long managerId : managerIds) {
            Users m = usersRepository.selectById(managerId);
            if (m == null || m.getOrg_id() == null) continue;
            List<Users> orgUsers = byOrg.getOrDefault(m.getOrg_id(), new ArrayList<>());
            int wrote = 0;
            for (Users u : orgUsers) {
                Long cnt = worklogsRepository.selectCount(new QueryWrapper<Worklogs>()
                        .eq("user_id", u.getId()).ge("log_date", lastMon).le("log_date", lastSun));
                if (cnt != null && cnt > 0) wrote++;
            }
            int rate = orgUsers.isEmpty() ? 0 : (int) Math.round(wrote * 100.0 / orgUsers.size());
            insertNotification(m.getId(), "日志周报",
                    "上周（" + lastMon + " 至 " + lastSun + "）您部门日志完成率：" + rate
                            + "%（" + wrote + "/" + orgUsers.size() + " 人已写），请督促未写员工及时补写。",
                    "worklog_weekly", null, "/home/worklog");
            sent++;
        }
        System.out.println("[工作日志周报] 已向 " + sent + " 名部门主管发送上周汇总");
    }

    // 定时物理清理超过180天的已删除日志
    @Override
    public void physicalCleanupDeleted() {
        try {
            int n = jdbcTemplate.update(
                    "DELETE FROM worklogs WHERE deleted_at IS NOT NULL AND deleted_at < DATE_SUB(NOW(), INTERVAL 180 DAY)");
            if (n > 0) System.out.println("[工作日志清理] 物理删除 " + n + " 条超过180天的已删除日志");
        } catch (Exception e) {
            System.out.println("[工作日志清理] 异常：" + e.getMessage());
        }
    }

    // 写一条站内通知，失败只打印不抛出
    private void insertNotification(Long userId, String title, String content, String bizType, Long bizId, String jumpUrl) {
        try {
            NotificationRecords n = new NotificationRecords();
            n.setUser_id(userId);
            n.setTitle(title);
            n.setContent(content);
            n.setType(5);
            n.setIs_read(0);
            n.setBiz_type(bizType);
            n.setBiz_id(bizId);
            n.setJump_url(jumpUrl);
            n.setCreated_at(LocalDateTime.now());
            n.setUpdated_at(LocalDateTime.now());
            notificationRecordsRepository.insert(n);
        } catch (Exception e) {
            System.out.println("[工作日志提醒] 通知写入失败 userId=" + userId + "：" + e.getMessage());
        }
    }

    // 去掉富文本里的标签，留纯文字
    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").trim();
    }

    // 日志状态转中文
    private String statusText(Integer status) {
        if (status == null) return "-";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已提交";
            case 2: return "已撤回";
            default: return "-";
        }
    }
}
