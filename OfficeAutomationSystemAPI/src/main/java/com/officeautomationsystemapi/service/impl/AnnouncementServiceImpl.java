package com.officeautomationsystemapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.entity.AnnouncementCategories;
import com.officeautomationsystemapi.entity.AnnouncementCategoryRoles;
import com.officeautomationsystemapi.entity.AnnouncementReads;
import com.officeautomationsystemapi.entity.Announcements;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.entity.Roles;
import com.officeautomationsystemapi.entity.UserRoles;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.AnnouncementCategoriesRepository;
import com.officeautomationsystemapi.repository.AnnouncementCategoryRolesRepository;
import com.officeautomationsystemapi.repository.AnnouncementReadsRepository;
import com.officeautomationsystemapi.repository.AnnouncementsRepository;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import com.officeautomationsystemapi.repository.RolesRepository;
import com.officeautomationsystemapi.repository.UserRolesRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.AnnouncementService;
import com.officeautomationsystemapi.service.PermissionService;
import com.officeautomationsystemapi.vo.AnnouncementCategoryVO;
import com.officeautomationsystemapi.vo.AnnouncementVO;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// 公告业务：类别可见性控制、发布下线、置顶和阅读统计
@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementsRepository announcementsRepository;

    @Autowired
    private AnnouncementCategoriesRepository categoriesRepository;

    @Autowired
    private AnnouncementReadsRepository readsRepository;

    @Autowired
    private AnnouncementCategoryRolesRepository categoryRolesRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private UserRolesRepository userRolesRepository;

    @Autowired
    private NotificationRecordsRepository notificationRecordsRepository;

    @Autowired
    private PermissionService permissionService;

    // 能看的类别：管理员看全部启用类别，其他人看公开和授权的
    private Set<Long> getVisibleCategoryIds(Long userId) {
        QueryWrapper<AnnouncementCategories> enabledWrapper = new QueryWrapper<>();
        enabledWrapper.eq("status", 1);
        List<AnnouncementCategories> enabled = categoriesRepository.selectList(enabledWrapper);

        if (permissionService.isAdmin(userId)) {
            return enabled.stream().map(AnnouncementCategories::getId).collect(Collectors.toSet());
        }

        // 公开类别：view_permission_type 为 1
        Set<Long> visible = enabled.stream()
                .filter(c -> Objects.equals(c.getView_permission_type(), 1))
                .map(AnnouncementCategories::getId)
                .collect(Collectors.toSet());

        List<String> roleCodes = permissionService.getUserRoleCodes(userId);
        if (roleCodes.isEmpty()) {
            return visible;
        }
        QueryWrapper<Roles> roleWrapper = new QueryWrapper<>();
        roleWrapper.in("role_code", roleCodes);
        List<Long> roleIds = rolesRepository.selectList(roleWrapper).stream()
                .map(Roles::getId)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return visible;
        }

        // 角色被授权的类别，用 Set 方便后面判断包含关系
        QueryWrapper<AnnouncementCategoryRoles> categoryRoleWrapper = new QueryWrapper<>();
        categoryRoleWrapper.in("role_id", roleIds);
        Set<Long> authorizedIds = categoryRolesRepository.selectList(categoryRoleWrapper).stream()
                .map(AnnouncementCategoryRoles::getCategory_id)
                .collect(Collectors.toSet());
        if (authorizedIds.isEmpty()) {
            return visible;
        }

        enabled.stream()
                .filter(c -> Objects.equals(c.getView_permission_type(), 2) && authorizedIds.contains(c.getId()))
                .forEach(c -> visible.add(c.getId()));
        return visible;
    }

    // 类别树只挂能看的节点，父节点不可见就把子节点提为根
    @Override
    public List<AnnouncementCategoryVO> listCategoryTree(Long userId) {
        Set<Long> visible = getVisibleCategoryIds(userId);
        QueryWrapper<AnnouncementCategories> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort_order");

        // 用 LinkedHashMap 保留 sort_order 的查询顺序，否则前端拿到的同级顺序是随机的
        Map<Long, AnnouncementCategoryVO> voMap = new LinkedHashMap<>();
        for (AnnouncementCategories category : categoriesRepository.selectList(wrapper)) {
            if (!visible.contains(category.getId())) {
                continue;
            }
            AnnouncementCategoryVO vo = new AnnouncementCategoryVO();
            BeanUtils.copyProperties(category, vo);
            voMap.put(category.getId(), vo);
        }

        List<AnnouncementCategoryVO> roots = new ArrayList<>();
        for (AnnouncementCategoryVO vo : voMap.values()) {
            Long parentId = vo.getParent_id();
            if (parentId == null || parentId == 0) {
                roots.add(vo);
                continue;
            }
            AnnouncementCategoryVO parent = voMap.get(parentId);
            if (parent == null) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        return roots;
    }

    // 新增类别，限定角色可见时同步写入角色关联
    @Override
    @Transactional
    public Long addCategory(AnnouncementCategoryVO vo) {
        AnnouncementCategories category = new AnnouncementCategories();
        BeanUtils.copyProperties(vo, category);
        category.setId(null);
        category.setCreated_at(LocalDateTime.now());
        category.setUpdated_at(LocalDateTime.now());
        categoriesRepository.insert(category);

        if (Objects.equals(vo.getView_permission_type(), 2)) {
            saveCategoryRoles(category.getId(), vo.getRole_ids());
        }
        return category.getId();
    }

    // 更新类别并重设角色关联，置为公开时清空关联
    @Override
    @Transactional
    public void updateCategory(AnnouncementCategoryVO vo) {
        AnnouncementCategories category = categoriesRepository.selectById(vo.getId());
        if (category == null) {
            throw new RuntimeException("类别不存在");
        }
        BeanUtils.copyProperties(vo, category, "id", "created_at");
        category.setUpdated_at(LocalDateTime.now());
        categoriesRepository.updateById(category);

        List<Long> roleIds = Objects.equals(vo.getView_permission_type(), 2)
                ? vo.getRole_ids() : Collections.emptyList();
        saveCategoryRoles(category.getId(), roleIds);
    }

    // 删除前先挡掉有子类别和类别下还有公告的情况
    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        QueryWrapper<AnnouncementCategories> childWrapper = new QueryWrapper<>();
        childWrapper.eq("parent_id", categoryId);
        if (categoriesRepository.selectCount(childWrapper) > 0) {
            throw new RuntimeException("该类别下有子类别，无法删除");
        }

        QueryWrapper<Announcements> announcementWrapper = new QueryWrapper<>();
        announcementWrapper.eq("category_id", categoryId);
        if (announcementsRepository.selectCount(announcementWrapper) > 0) {
            throw new RuntimeException("该类别下存在公告，无法删除");
        }

        categoriesRepository.deleteById(categoryId);
        QueryWrapper<AnnouncementCategoryRoles> roleWrapper = new QueryWrapper<>();
        roleWrapper.eq("category_id", categoryId);
        categoryRolesRepository.delete(roleWrapper);
    }

    // 先清旧关联再逐条写新角色，传空表示不再按角色限定
    private void saveCategoryRoles(Long categoryId, List<Long> roleIds) {
        QueryWrapper<AnnouncementCategoryRoles> wrapper = new QueryWrapper<>();
        wrapper.eq("category_id", categoryId);
        categoryRolesRepository.delete(wrapper);
        if (roleIds == null) {
            return;
        }
        // 去空去重，避免同一类别重复插关联行
        for (Long roleId : distinctIds(roleIds)) {
            AnnouncementCategoryRoles relation = new AnnouncementCategoryRoles();
            relation.setCategory_id(categoryId);
            relation.setRole_id(roleId);
            relation.setCreated_at(LocalDateTime.now());
            relation.setUpdated_at(LocalDateTime.now());
            categoryRolesRepository.insert(relation);
        }
    }

    // 只改启用状态
    @Override
    public void toggleCategoryStatus(Long categoryId, Integer status) {
        AnnouncementCategories category = categoriesRepository.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("类别不存在");
        }
        category.setStatus(status);
        category.setUpdated_at(LocalDateTime.now());
        categoriesRepository.updateById(category);
    }

    // 公告分页：管理员可按状态筛，普通用户只看已发布且可见的
    @Override
    public Page<AnnouncementVO> pageAnnouncements(Long userId, Integer pageNum, Integer pageSize,
                                                  Long categoryId, String keyword, Integer status) {
        boolean isAdmin = permissionService.isAdmin(userId);
        Set<Long> visible = getVisibleCategoryIds(userId);
        if (!isAdmin && visible.isEmpty()) {
            return emptyPage(pageNum, pageSize);
        }

        QueryWrapper<Announcements> wrapper = new QueryWrapper<>();
        // 普通用户限定在可见类别内，空集合不能直接拼进 in 条件
        if (!isAdmin || status != null) {
            if (!visible.isEmpty()) {
                wrapper.in("category_id", visible);
            }
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("title", keyword).or().like("summary", keyword));
        }
        if (isAdmin) {
            if (status != null) {
                wrapper.eq("status", status);
            }
        } else {
            wrapper.eq("status", 1);
        }
        wrapper.orderByDesc("is_top").orderByDesc("publish_time");

        Page<Announcements> page = announcementsRepository.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Announcements> records = page.getRecords();
        List<Long> announcementIds = records.stream().map(Announcements::getId).collect(Collectors.toList());

        // 类别名、作者名、阅读状态和阅读数按当前页批量查
        Map<Long, String> categoryNames = loadCategoryNames(records.stream()
                .map(Announcements::getCategory_id).collect(Collectors.toList()));
        Map<Long, String> authorNames = loadUserNames(records.stream()
                .map(Announcements::getAuthor_id).collect(Collectors.toList()));
        Set<Long> readIds = loadReadIds(userId, announcementIds);
        Map<Long, Long> readCounts = loadReadCounts(announcementIds);

        Page<AnnouncementVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(records.stream().map(a -> {
            AnnouncementVO vo = new AnnouncementVO();
            BeanUtils.copyProperties(a, vo);
            vo.setCategory_name(categoryNames.get(a.getCategory_id()));
            vo.setAuthor_name(authorNames.get(a.getAuthor_id()));
            vo.setRead_count(readCounts.getOrDefault(a.getId(), 0L));
            vo.setIs_read(readIds.contains(a.getId()));
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    // 公告详情：校验可见性，首次打开记阅读并给浏览数加一
    @Override
    @Transactional
    public AnnouncementVO getAnnouncementDetail(Long userId, Long announcementId) {
        Announcements announcement = announcementsRepository.selectById(announcementId);
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }
        boolean isAdmin = permissionService.isAdmin(userId);
        // status 可能为 null，先比包装类型再拆箱，避免空指针
        if (!Integer.valueOf(1).equals(announcement.getStatus()) && !isAdmin) {
            throw new RuntimeException("公告不存在或未发布");
        }
        if (!isAdmin && !getVisibleCategoryIds(userId).contains(announcement.getCategory_id())) {
            throw new RuntimeException("公告不存在");
        }

        QueryWrapper<AnnouncementReads> readWrapper = new QueryWrapper<>();
        readWrapper.eq("announcement_id", announcementId).eq("user_id", userId);
        if (readsRepository.selectCount(readWrapper) == 0) {
            LocalDateTime now = LocalDateTime.now();
            AnnouncementReads read = new AnnouncementReads();
            read.setAnnouncement_id(announcementId);
            read.setUser_id(userId);
            read.setRead_time(now);
            read.setCreated_at(now);
            read.setUpdated_at(now);
            try {
                readsRepository.insert(read);

                // 浏览数用 SQL 自增，避免并发覆盖，只更新这两列
                announcementsRepository.update(null, new UpdateWrapper<Announcements>()
                        .eq("id", announcementId)
                        .setSql("view_count = COALESCE(view_count, 0) + 1")
                        .set("updated_at", now));
                announcement.setView_count(announcement.getView_count() == null ? 1 : announcement.getView_count() + 1);
                announcement.setUpdated_at(now);
            } catch (DuplicateKeyException e) {
                // 并发首次打开时唯一索引会拦住重复行，按已读处理
                // 表上还没加唯一索引，这条分支暂时不会走到
            }
        }

        AnnouncementVO vo = new AnnouncementVO();
        BeanUtils.copyProperties(announcement, vo);
        // 类别或作者缺失时跳过名称查询，List.of 不接受 null 元素
        Long categoryId = announcement.getCategory_id();
        if (categoryId != null) {
            vo.setCategory_name(loadCategoryNames(List.of(categoryId)).get(categoryId));
        }
        Long authorId = announcement.getAuthor_id();
        if (authorId != null) {
            vo.setAuthor_name(loadUserNames(List.of(authorId)).get(authorId));
        }
        vo.setRead_count(readsRepository.selectCount(new QueryWrapper<AnnouncementReads>()
                .eq("announcement_id", announcementId)));
        vo.setIs_read(true);
        return vo;
    }

    // 新增公告：作者取当前登录人，正文先过滤，未给状态时按草稿存
    @Override
    public Long addAnnouncement(Long userId, AnnouncementVO vo) {
        Announcements announcement = new Announcements();
        BeanUtils.copyProperties(vo, announcement, "id", "read_count", "category_name", "author_name", "is_read");
        announcement.setAuthor_id(userId);
        announcement.setStatus(vo.getStatus() == null ? 0 : vo.getStatus());
        announcement.setContent(sanitizeContent(vo.getContent()));
        announcement.setView_count(0);
        announcement.setCreated_at(LocalDateTime.now());
        announcement.setUpdated_at(LocalDateTime.now());
        announcementsRepository.insert(announcement);
        return announcement.getId();
    }

    // 更新公告：作者、浏览数和阅读数不允许被前端覆盖
    @Override
    public void updateAnnouncement(Long userId, AnnouncementVO vo) {
        Announcements announcement = announcementsRepository.selectById(vo.getId());
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }

        // updateById 跳过 null 字段，清空正文写不进库，改用 UpdateWrapper
        UpdateWrapper<Announcements> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", announcement.getId());
        wrapper.set("content", sanitizeContent(vo.getContent()));
        wrapper.set("summary", vo.getSummary());
        wrapper.set("cover_image", vo.getCover_image());
        wrapper.set("updated_at", LocalDateTime.now());

        // 其余字段仍按实体回写，null 值跳过，前端没传的列不会被误清
        Announcements update = new Announcements();
        BeanUtils.copyProperties(vo, update, "id", "created_at", "author_id", "view_count", "content",
                "summary", "cover_image", "read_count", "category_name", "author_name", "is_read");
        announcementsRepository.update(update, wrapper);
    }

    // 富文本只留结构化标签，内联 style 和 class 一律清掉
    // 正文外观由前端主题控制，不让公告里带字号颜色
    private static final Safelist CONTENT_SAFELIST = new Safelist()
            .addTags("p", "br", "div", "span", "h1", "h2", "h3", "h4", "h5", "h6",
                    "strong", "b", "em", "i", "u", "s", "del", "sub", "sup",
                    "blockquote", "pre", "code", "hr", "ul", "ol", "li",
                    "a", "img", "figure", "figcaption",
                    "table", "thead", "tbody", "tfoot", "tr", "th", "td")
            .addAttributes("a", "href", "title")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addAttributes("th", "colspan", "rowspan")
            .addAttributes("td", "colspan", "rowspan")
            // 只放正常协议，javascript: 之类会被丢掉
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https", "data");

    // 插图存的是 /api/public/... 相对地址，Jsoup 校验会删掉 src
    // 清洗前补假域名，清洗完换回相对地址
    private static final String TEMP_IMAGE_HOST = "https://oa.local";

    private String sanitizeContent(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        String prepared = html
                .replace("src=\"/api/public/announcement-image", "src=\"" + TEMP_IMAGE_HOST + "/api/public/announcement-image")
                .replace("src='/api/public/announcement-image", "src='" + TEMP_IMAGE_HOST + "/api/public/announcement-image");
        String cleaned = Jsoup.clean(prepared, CONTENT_SAFELIST);
        return cleaned.replace(TEMP_IMAGE_HOST, "");
    }

    // 逻辑删除，统计数据保留
    @Override
    public void deleteAnnouncement(Long announcementId) {
        announcementsRepository.deleteById(announcementId);
    }

    // 发布：置为已发布并记发布时间，再给能看这个类别的人发通知
    @Override
    @Transactional
    public void publishAnnouncement(Long announcementId) {
        Announcements announcement = announcementsRepository.selectById(announcementId);
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }
        // 已发布的公告重复点发布会再发一轮通知，这里直接挡掉
        if (Integer.valueOf(1).equals(announcement.getStatus())) {
            throw new RuntimeException("该公告已发布，请勿重复操作");
        }
        announcement.setStatus(1);
        announcement.setPublish_time(LocalDateTime.now());
        announcement.setUpdated_at(LocalDateTime.now());
        announcementsRepository.updateById(announcement);
        sendAnnouncementNotifications(announcement);
    }

    // 通知对象是类别授权角色下的用户，先查角色再走 user_roles 关联表查人
    private void sendAnnouncementNotifications(Announcements announcement) {
        QueryWrapper<AnnouncementCategoryRoles> roleWrapper = new QueryWrapper<>();
        roleWrapper.eq("category_id", announcement.getCategory_id());
        List<Long> roleIds = categoryRolesRepository.selectList(roleWrapper).stream()
                .map(AnnouncementCategoryRoles::getRole_id)
                .distinct()
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return;
        }

        QueryWrapper<UserRoles> relationWrapper = new QueryWrapper<>();
        relationWrapper.in("role_id", roleIds);
        List<Long> userIds = userRolesRepository.selectList(relationWrapper).stream()
                .map(UserRoles::getUser_id)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Users user : usersRepository.selectBatchIds(userIds)) {
            NotificationRecords record = new NotificationRecords();
            record.setUser_id(user.getId());
            record.setTitle("新公告：" + announcement.getTitle());
            record.setContent(announcement.getTitle());
            record.setType(1);
            record.setBiz_type("announcement");
            record.setBiz_id(announcement.getId());
            record.setJump_url("/home/notice");
            record.setIs_read(0);
            record.setCreated_at(now);
            notificationRecordsRepository.insert(record);
        }
    }

    // 下线：置为已下线并记录下线时间
    @Override
    public void cancelPublishAnnouncement(Long announcementId) {
        Announcements announcement = announcementsRepository.selectById(announcementId);
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }
        announcement.setStatus(2);
        announcement.setOffline_time(LocalDateTime.now());
        announcement.setUpdated_at(LocalDateTime.now());
        announcementsRepository.updateById(announcement);
    }

    // 批量发布，逐条走单条发布的逻辑
    @Override
    @Transactional
    public void batchPublish(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            // 已发布的直接跳过，否则一条重复发布就会让整批事务回滚
            Announcements announcement = announcementsRepository.selectById(id);
            if (announcement != null && Integer.valueOf(1).equals(announcement.getStatus())) {
                continue;
            }
            publishAnnouncement(id);
        }
    }

    // 置顶开关
    @Override
    public void toggleTop(Long announcementId, Integer isTop) {
        Announcements announcement = announcementsRepository.selectById(announcementId);
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }
        announcement.setIs_top(isTop);
        announcement.setUpdated_at(LocalDateTime.now());
        announcementsRepository.updateById(announcement);
    }

    // 阅读记录分页，最近读的排前面
    @Override
    public Page<Map<String, Object>> pageReadUsers(Long announcementId, Integer pageNum, Integer pageSize) {
        QueryWrapper<AnnouncementReads> wrapper = new QueryWrapper<>();
        wrapper.eq("announcement_id", announcementId).orderByDesc("read_time");
        Page<AnnouncementReads> page = readsRepository.selectPage(new Page<>(pageNum, pageSize), wrapper);

        Map<Long, String> userNames = loadUserNames(page.getRecords().stream()
                .map(AnnouncementReads::getUser_id).collect(Collectors.toList()));

        Page<Map<String, Object>> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(read -> {
            Map<String, Object> row = new HashMap<>();
            row.put("userId", read.getUser_id());
            row.put("userName", userNames.getOrDefault(read.getUser_id(), "已离职用户"));
            row.put("readTime", read.getRead_time());
            return row;
        }).collect(Collectors.toList()));
        return voPage;
    }

    // 批量取类别名，只查当前页用到的 ID
    private Map<Long, String> loadCategoryNames(Collection<Long> categoryIds) {
        List<Long> ids = distinctIds(categoryIds);
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        // 逐条 put，类别名为 null 时不会像 toMap 那样抛空指针
        for (AnnouncementCategories category : categoriesRepository.selectBatchIds(ids)) {
            names.put(category.getId(), category.getCategory_name());
        }
        return names;
    }

    // 批量取用户名，优先真实姓名，没有则用登录名
    private Map<Long, String> loadUserNames(Collection<Long> userIds) {
        List<Long> ids = distinctIds(userIds);
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        for (Users user : usersRepository.selectBatchIds(ids)) {
            names.put(user.getId(), user.getReal_name() == null ? user.getUsername() : user.getReal_name());
        }
        return names;
    }

    // 去掉空 ID 并去重，避免 in 条件里出现 null
    private List<Long> distinctIds(Collection<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    // 当前用户在这批公告里读过哪些
    private Set<Long> loadReadIds(Long userId, List<Long> announcementIds) {
        if (announcementIds.isEmpty()) {
            return Collections.emptySet();
        }
        QueryWrapper<AnnouncementReads> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).in("announcement_id", announcementIds);
        return readsRepository.selectList(wrapper).stream()
                .map(AnnouncementReads::getAnnouncement_id)
                .collect(Collectors.toCollection(HashSet::new));
    }

    // 每条公告有多少人读过
    private Map<Long, Long> loadReadCounts(List<Long> announcementIds) {
        if (announcementIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<AnnouncementReads> wrapper = new QueryWrapper<>();
        wrapper.in("announcement_id", announcementIds);
        return readsRepository.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(AnnouncementReads::getAnnouncement_id, Collectors.counting()));
    }

    // 没有可见类别时返回空分页，字段和正常分页保持一致
    private Page<AnnouncementVO> emptyPage(Integer pageNum, Integer pageSize) {
        Page<AnnouncementVO> page = new Page<>(pageNum, pageSize, 0);
        page.setRecords(new ArrayList<>());
        return page;
    }
}
