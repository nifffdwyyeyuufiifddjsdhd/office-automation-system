package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.officeautomationsystemapi.utilt.ExcelSafe;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.*;
import com.officeautomationsystemapi.repository.*;
import com.officeautomationsystemapi.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flow")
public class FlowInstanceController {

    // 流程实例、定义、步骤、流转条件、审批记录
    @Autowired private FlowInstancesRepository instancesRepository;
    @Autowired private FlowDefinitionsRepository definitionsRepository;
    @Autowired private FlowStepsRepository stepsRepository;
    @Autowired private FlowStepTransitionsRepository transitionsRepository;
    @Autowired private FlowApprovalsRepository approvalsRepository;

    // 人员、部门、角色、附件、通知、出勤记录
    @Autowired private UsersRepository usersRepository;
    @Autowired private OrganizationsRepository organizationsRepository;
    @Autowired private UserRolesRepository userRolesRepository;
    @Autowired private AttachmentsRepository attachmentsRepository;
    @Autowired private NotificationRecordsRepository notificationRecordsRepository;
    @Autowired private com.officeautomationsystemapi.service.NotificationBizService notificationBizService;
    @Autowired private AttendanceRecordsRepository attendanceRecordsRepository;
    @Autowired private PaymentRecordsRepository paymentRecordsRepository;
    // 判断管理员和权限码，流程可见性校验要用
    @Autowired private PermissionService permissionService;
    // 审批委托：代理人要能看到并处理委托人的待办
    @Autowired private com.officeautomationsystemapi.service.FlowDelegateService flowDelegateService;
    // 通知标题正文优先用模板，模板停用时用代码里的默认文案
    @Autowired private com.officeautomationsystemapi.service.NotificationTemplateService notificationTemplateService;

    // 附件上传的根目录
    @Value("${oa.upload-dir:./uploads}")
    private String uploadDir;

    private static final Logger log = LoggerFactory.getLogger(FlowInstanceController.class);

    private static final ObjectMapper OM = new ObjectMapper();
    // 流转链路最多走多少步，流程配成环时用来限制
    private static final int MAX_CHAIN_STEPS = 100;
    // 步骤没配超时小时数时，停留超过这么多小时就自动催办
    private static final int DEFAULT_URGE_HOURS = 48;
    // 流程附件允许的扩展名，只放行常见文档和图片
    private static final Set<String> ALLOWED_UPLOAD_EXTS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx", "xls", "xlsx", "zip", "rar", "txt");
    // 当天已发出的流程编号序号，软删除的实例查不到，靠它继续往后推
    private int lastSeqDayKey = 0;
    private int lastSeq = 0;
    // 条件表达式里 in 和 not in 的写法
    private static final Pattern IN_PATTERN =
            Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s+(not\\s+in|in)\\s*\\(([^)]+)\\)$");
    // 审批意见里的 @提醒，@ 后面跟 2 到 20 个中文、字母、数字或下划线
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("@([\\u4e00-\\u9fa5A-Za-z0-9_]{2,20})");
    // 条件表达式里普通比较的写法
    private static final Pattern COMPARE_PATTERN =
            Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*(>=|<=|==|!=|>|<)\\s*([\\d.\\w'-]+)$");
    // 条件表达式里的包含写法：field contains 值
    private static final Pattern CONTAINS_PATTERN =
            Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s+(not\\s+contains|contains)\\s+(.+)$");

    // 发一条站内通知，发不出去也不影响主流程
    private void notify(Long userId, String title, String content, String bizType, Long bizId, String jumpUrl) {
        if (userId == null) return;
        try {
            NotificationRecords n = new NotificationRecords();
            n.setUser_id(userId);
            n.setTitle(title);
            n.setContent(content);
            n.setType(2);
            n.setBiz_type(bizType);
            n.setBiz_id(bizId);
            n.setJump_url(jumpUrl);
            n.setIs_read(0);
            n.setPush_status(0);
            n.setCreated_at(LocalDateTime.now());
            n.setUpdated_at(LocalDateTime.now());
            notificationRecordsRepository.insert(n);
        } catch (Exception ignore) {
            // 通知失败只丢这一条，不影响流程本身
        }
    }

    // 给步骤的全部候选审批人发待办通知，已有未读待办的人不重复发
    private void notifyStepAuditors(FlowSteps step, Long instanceId, Long creatorId, String action, String title) {
        String instanceNo = null;
        try {
            FlowInstances target = instancesRepository.selectById(instanceId);
            instanceNo = target == null ? null : target.getInstance_no();
        } catch (Exception ignore) {
            // 拿不到流程编号就用默认文案
        }
        com.officeautomationsystemapi.service.NotificationTemplateService.Rendered message = renderNotice("flow_arrive",
                action + "：" + title, action + "：" + title + "，请及时处理",
                "title", title, "instanceNo", instanceNo, "stepName", step == null ? null : step.getStep_name());
        for (Long uid : resolveCandidates(step, creatorId, null)) {
            // 同一条流程同一个人只留一条通知，看过也不再重发
            Long exists = notificationRecordsRepository.selectCount(new QueryWrapper<NotificationRecords>()
                    .eq("user_id", uid).eq("biz_type", "flow_instance").eq("biz_id", instanceId));
            if (exists != null && exists > 0) continue;
            notify(uid, message.title(), message.content(), "flow_instance", instanceId, "/home/flow-todo");
        }
    }

    // 按模板渲染通知，kv 是变量名和值交替传入，值为空不参与替换
    private com.officeautomationsystemapi.service.NotificationTemplateService.Rendered renderNotice(
            String templateCode, String fallbackTitle, String fallbackContent, String... kv) {
        Map<String, String> vars = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i] != null && kv[i + 1] != null) {
                vars.put(kv[i], kv[i + 1]);
            }
        }
        return notificationTemplateService.render(templateCode, vars, fallbackTitle, fallbackContent);
    }

    // 通知发起人处理结果，result 是完成、驳回、退回这类结果词
    private void notifyFlowResult(FlowInstances inst, String result, Long approverId,
                                  String fallbackTitle, String fallbackContent) {
        String approver = null;
        try {
            Users user = approverId == null ? null : usersRepository.selectById(approverId);
            if (user != null) {
                approver = user.getReal_name() == null || user.getReal_name().isBlank()
                        ? user.getUsername() : user.getReal_name();
            }
        } catch (Exception ignore) {
            // 名字取不到就用默认文案
        }
        // 流程已出结果，之前挂在这条流程上的待办通知全部作废
        notificationBizService.clear("flow_instance", inst.getId());
        com.officeautomationsystemapi.service.NotificationTemplateService.Rendered message = renderNotice("flow_result",
                fallbackTitle, fallbackContent,
                "title", inst.getTitle(), "instanceNo", inst.getInstance_no(), "result", result, "approver", approver);
        notify(inst.getCreator_id(), message.title(), message.content(), "flow_instance", inst.getId(), "/home/flow-mine");
    }

    // 清掉这条流程的全部未读待办，有人处理后其他候选人的待办一起消失
    private void clearTodoNotifications(Long instanceId) {
        notificationRecordsRepository.update(null, new UpdateWrapper<NotificationRecords>()
                .eq("biz_type", "flow_instance").eq("biz_id", instanceId)
                .eq("jump_url", "/home/flow-todo").eq("is_read", 0)
                .set("is_read", 1).set("read_time", LocalDateTime.now()));
    }

    // 写一条审批留痕，会签状态为空时不写，目标用户只在转交和催办时有值
    private void addApproval(Long instanceId, Long stepId, Long approverId, int action,
                             String comment, Integer signStatus, Long targetUserId) {
        LocalDateTime now = LocalDateTime.now();
        FlowApprovals ap = new FlowApprovals();
        ap.setInstance_id(instanceId);
        ap.setStep_id(stepId);
        ap.setApprover_id(approverId);
        ap.setAction(action);
        ap.setComment(comment);
        ap.setApprove_time(now);
        if (signStatus != null) ap.setSign_status(signStatus);
        if (targetUserId != null) ap.setTarget_user_id(targetUserId);
        ap.setCreated_at(now);
        ap.setUpdated_at(now);
        approvalsRepository.insert(ap);
    }

    // 发起流程：校验表单约束、确定第一步审批人、落库并发待办
    @PostMapping("/instances/start")
    @RequirePermission("flow:start")
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> start(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Object definitionIdObj = body.get("definitionId");
        if (definitionIdObj == null) return Result.error("缺少流程定义");
        Long definitionId = Long.valueOf(definitionIdObj.toString());
        FlowDefinitions def = definitionsRepository.selectById(definitionId);
        if (def == null || def.getStatus() == null || def.getStatus() != 1) return Result.error("流程不存在或未发布");
        // 发布时可能限定了使用部门或角色，不在范围内不能发起
        String scopeErr = checkUseScope(def, userId);
        if (scopeErr != null) return Result.error(403, scopeErr);

        // 先校验表单跨字段约束，再按起止日期重算天数
        Map<String, Object> businessData = parseBusiness(body.get("businessData"));
        String constraintErr = validateConstraints(def, businessData);
        if (constraintErr != null) return Result.error(constraintErr);
        recalcDaysIfPresent(businessData);

        // 排序最靠前的审批节点就是第一步
        FlowSteps firstStep = firstApprovalStep(definitionId);
        if (firstStep == null) return Result.error("流程未配置审批步骤");

        // 池式步骤不锁定审批人，其他步骤锁定第一个候选人
        Object selectedApprover = body.get("selectedApproverId");
        Long selectedId = selectedApprover == null ? null : Long.valueOf(selectedApprover.toString());
        List<Long> firstCandidates = resolveCandidates(firstStep, userId, selectedId);
        if (firstCandidates.isEmpty()) return Result.error("无法确定第一步审核人，请检查流程审批人配置");
        Long auditorId = isPoolStep(firstStep) ? null : firstCandidates.get(0);

        // 后面每一步也过一遍，避免发起后卡在没人能审的节点
        String chainErr = validateChainNoSelf(firstStep.getId(), userId, businessData);
        if (chainErr != null) return Result.error(chainErr);

        String businessDataJson;
        try {
            businessDataJson = OM.writeValueAsString(businessData);
        } catch (Exception e) {
            return Result.error("表单数据格式错误");
        }

        String title = (String) body.get("title");
        if (title == null || title.trim().isEmpty()) {
            Users creator = usersRepository.selectById(userId);
            title = (creator == null ? "用户" : creator.getReal_name()) + "的" + def.getFlow_name();
        }

        LocalDateTime now = LocalDateTime.now();
        FlowInstances inst = new FlowInstances();
        inst.setDefinition_id(definitionId);
        inst.setInstance_no(generateInstanceNo());
        inst.setTitle(title);
        inst.setCreator_id(userId);
        inst.setCurrent_step_id(firstStep.getId());
        inst.setCurrent_step_name(firstStep.getStep_name());
        inst.setCurrent_auditor_id(auditorId);
        inst.setStatus(1);
        inst.setBusiness_data(businessDataJson);
        inst.setUrgency(body.get("urgency") == null ? 0 : Integer.valueOf(body.get("urgency").toString()));
        inst.setSubmit_time(now);
        inst.setCreated_at(now);
        inst.setUpdated_at(now);
        // 编号是唯一键，撞号时 insertInstance 会重新取号重试
        insertInstance(inst);

        // 发起人自己也留一条提交记录
        addApproval(inst.getId(), firstStep.getId(), userId, 1, "提交申请", 1, null);

        // 发起前已上传的附件在这里补上关联
        bindAttachments(inst.getId(), body.get("attachmentIds"), userId);

        notifyStepAuditors(firstStep, inst.getId(), userId, "新的待办流程", title);
        return Result.success(inst.getId());
    }

    // 审批通过：记一条通过记录，再找下一个节点，找不到说明流程走完了
    // 审核人是动态角色，不加权限码门槛，能不能批由 isCurrentAuditor 和 canUserApprove 逐单判定
    @PostMapping("/instances/{id}/approve")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> approve(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error("流程不存在");
        // 池式步骤允许候选人来处理
        if (!isCurrentAuditor(inst, userId) && !canUserApprove(inst, userId)) {
            return Result.error("当前不是你的待办");
        }
        String comment = (String) body.get("comment");
        if (comment == null || comment.trim().isEmpty()) comment = "同意";
        if (comment.length() > 500) return Result.error("审批意见不能超过500字");

        FlowSteps curStep = inst.getCurrent_step_id() == null ? null : stepsRepository.selectById(inst.getCurrent_step_id());
        if (curStep == null) return Result.error("当前审批步骤不存在");

        // 会签每个人都要表态，凑够人数才流转
        if (isCountersign(curStep)) return approveCountersign(inst, curStep, userId, comment);

        // 先按条件抢下这条待办，池式步骤两人同时点时只有一个人能更新到行
        if (claimCurrentStep(inst, userId) == 0) return Result.error("该审批已被处理，请刷新后重试");
        inst.setCurrent_auditor_id(userId);

        addApproval(id, inst.getCurrent_step_id(), userId, 1, comment, 2, null);
        notifyMentions(id, comment, userId, inst.getTitle());

        Map<String, Object> business = parseBusiness(inst.getBusiness_data());
        FlowSteps targetStep = nextApprovalStep(curStep.getId(), business, id, userId, inst.getCreator_id());
        if (targetStep == null) {
            completeInstance(inst);
            clearTodoNotifications(id);
            notifyFlowResult(inst, "已完成", userId, "流程已完成：" + inst.getTitle(), "您发起的流程已审批完成");
            return Result.success(null);
        }
        moveToStep(inst, targetStep);
        clearTodoNotifications(id);
        notifyStepAuditors(targetStep, id, inst.getCreator_id(), "新的待办流程", inst.getTitle());
        return Result.success(null);
    }

    // 流程结束，状态置为已通过，当前节点与审批人清空
    private void completeInstance(FlowInstances inst) {
        LocalDateTime now = LocalDateTime.now();
        instancesRepository.update(null, new UpdateWrapper<FlowInstances>()
                .eq("id", inst.getId())
                .set("status", 2)
                .set("current_step_name", "已完成")
                .set("finish_time", now)
                .set("current_auditor_id", null)
                .set("current_step_id", null)
                .set("updated_at", now));
        inst.setStatus(2);
        inst.setFinish_time(now);
        inst.setCurrent_step_name("已完成");
        inst.setCurrent_auditor_id(null);
        inst.setCurrent_step_id(null);
        markBusinessTripIfNeeded(inst);
    }

    // 会签审批：按人去重记录意见，够人数才流转，没凑够只留痕
    private Result<String> approveCountersign(FlowInstances inst, FlowSteps step, Long userId, String comment) {
        List<Long> candidates = resolveCandidates(step, inst.getCreator_id(), null);
        if (candidates.isEmpty()) return Result.error("该会签步骤没有可用的审批人，请联系管理员调整流程");
        if (!candidates.contains(userId)) return Result.error("当前不是你的待办");
        // 只算真正的审批票，提交、重新提交、抄送也写 action=1 但 sign_status=1，不算会签票
        Long signed = approvalsRepository.selectCount(new QueryWrapper<FlowApprovals>()
                .eq("instance_id", inst.getId()).eq("step_id", step.getId())
                .eq("approver_id", userId).eq("action", 1)
                .and(w -> w.isNull("sign_status").or().ne("sign_status", 1)));
        if (signed != null && signed > 0) return Result.error("您已经会签过这一步，等待其他审批人处理");

        addApproval(inst.getId(), step.getId(), userId, 1, comment, 2, null);
        notifyMentions(inst.getId(), comment, userId, inst.getTitle());
        // 我这一票已经投了，自己的红点先消掉，其他人的待办留着
        markTodoRead(inst.getId(), userId);

        int required = requiredSigners(step, candidates.size());
        long done = distinctSigners(inst.getId(), step.getId());
        if (done < required) {
            return Result.success("已记录您的会签意见（" + done + "/" + required + "），还需 "
                    + (required - done) + " 人审批", null);
        }

        Map<String, Object> business = parseBusiness(inst.getBusiness_data());
        FlowSteps targetStep = nextApprovalStep(step.getId(), business, inst.getId(), userId, inst.getCreator_id());
        if (targetStep == null) {
            completeInstance(inst);
            clearTodoNotifications(inst.getId());
            notifyFlowResult(inst, "已完成", userId, "流程已完成：" + inst.getTitle(), "您发起的流程已审批完成");
            return Result.success("会签已全部通过，流程结束", null);
        }
        moveToStep(inst, targetStep);
        clearTodoNotifications(inst.getId());
        notifyStepAuditors(targetStep, inst.getId(), inst.getCreator_id(), "新的待办流程", inst.getTitle());
        return Result.success("会签已全部通过（" + done + "/" + required + "），已流转到下一步", null);
    }

    // 会签要几个人：全部候选人，或按 sign_ratio 换算，向上取整至少 1 人
    private int requiredSigners(FlowSteps step, int total) {
        if (total <= 0) return 1;
        Integer sign = step.getSign_type();
        Integer ratio = step.getSign_ratio();
        if (sign != null && sign == 3 && ratio != null && ratio > 0 && ratio < 100) {
            return Math.max(1, (int) Math.ceil(total * ratio / 100.0));
        }
        return total;
    }

    // 这一步已经有多少个不同的人签过
    private long distinctSigners(Long instanceId, Long stepId) {
        // 同样排除提交记录，否则发起人那一行会被算成第 1 票
        List<FlowApprovals> list = approvalsRepository.selectList(new QueryWrapper<FlowApprovals>()
                .eq("instance_id", instanceId).eq("step_id", stepId).eq("action", 1)
                .and(w -> w.isNull("sign_status").or().ne("sign_status", 1)));
        return list.stream().map(FlowApprovals::getApprover_id).filter(Objects::nonNull).distinct().count();
    }

    // 会签步骤所有人看到同一个待办，签过的人就不再出现
    private boolean alreadySigned(FlowInstances inst, Long userId) {
        if (inst == null || inst.getCurrent_step_id() == null) return false;
        FlowSteps step = stepsRepository.selectById(inst.getCurrent_step_id());
        if (!isCountersign(step)) return false;
        Long c = approvalsRepository.selectCount(new QueryWrapper<FlowApprovals>()
                .eq("instance_id", inst.getId()).eq("step_id", step.getId())
                .eq("approver_id", userId).eq("action", 1)
                .and(w -> w.isNull("sign_status").or().ne("sign_status", 1)));
        return c != null && c > 0;
    }

    // 流转到指定步骤，池式步骤不写审批人
    private void moveToStep(FlowInstances inst, FlowSteps step) {
        Long auditor = isPoolStep(step) ? null : resolveApprover(step, inst.getCreator_id(), null);
        instancesRepository.update(null, new UpdateWrapper<FlowInstances>()
                .eq("id", inst.getId())
                .set("current_step_id", step.getId())
                .set("current_step_name", step.getStep_name())
                .set("current_auditor_id", auditor)
                .set("updated_at", LocalDateTime.now()));
        inst.setCurrent_step_id(step.getId());
        inst.setCurrent_step_name(step.getStep_name());
        inst.setCurrent_auditor_id(auditor);
    }

    // 出差流程通过后，给区间里还没有出勤记录的日子补一条出差
    private void markBusinessTripIfNeeded(FlowInstances inst) {
        try {
            FlowDefinitions def = definitionsRepository.selectById(inst.getDefinition_id());
            if (def == null || !"business_trip".equals(def.getCode())) return;
            Map<String, Object> business = parseBusiness(inst.getBusiness_data());
            Object startObj = business.get("start_date");
            Object endObj = business.get("end_date");
            if (startObj == null || endObj == null) return;
            // 前端可能传日期也可能传日期时间，统一截到天
            LocalDate start = LocalDate.parse(startObj.toString().substring(0, 10));
            LocalDate end = LocalDate.parse(endObj.toString().substring(0, 10));
            if (end.isBefore(start)) {
                LocalDate tmp = start;
                start = end;
                end = tmp;
            }
            Users creator = usersRepository.selectById(inst.getCreator_id());
            if (creator == null) return;
            String dest = business.get("destination") == null ? "" : business.get("destination").toString();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                AttendanceRecords rec = attendanceRecordsRepository.findByUserAndDate(inst.getCreator_id(), d);
                if (rec == null) {
                    rec = new AttendanceRecords();
                    rec.setUser_id(inst.getCreator_id());
                    rec.setDept_id(creator.getOrg_id());
                    rec.setAttendance_date(d);
                    rec.setStatus(5);
                    rec.setRemark("出差：" + dest + "（" + inst.getTitle() + "）");
                    rec.setCheck_type("出差");
                    rec.setCreated_at(LocalDateTime.now());
                    rec.setUpdated_at(LocalDateTime.now());
                    attendanceRecordsRepository.insert(rec);
                } else if (rec.getStatus() == null || rec.getStatus() != 5) {
                    rec.setStatus(5);
                    rec.setRemark("出差：" + dest + "（" + inst.getTitle() + "）");
                    rec.setCheck_type("出差");
                    rec.setUpdated_at(LocalDateTime.now());
                    attendanceRecordsRepository.updateById(rec);
                }
            }
        } catch (Exception ignore) {
            // 出勤同步失败不影响审批结果
        }
    }

    // 驳回：可以退回某个历史步骤，也可以不指定直接终止
    @PostMapping("/instances/{id}/reject")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error("流程不存在");
        if (!isCurrentAuditor(inst, userId) && !canUserApprove(inst, userId)) {
            return Result.error("当前不是你的待办");
        }
        // 已结束的流程再驳回会把状态改回审批中，这里必须卡住
        if (!Integer.valueOf(1).equals(inst.getStatus())) return Result.error("只有审批中的流程可以驳回");
        String comment = (String) body.get("comment");
        if (comment == null || comment.trim().length() < 5) {
            return Result.error("驳回原因必填且至少5个字");
        }
        if (comment.length() > 500) return Result.error("驳回原因不能超过500字");

        // 先把退回目标校验完，校验不过就不能留下驳回记录
        Object targetStepIdObj = body.get("targetStepId");
        FlowSteps targetStep = null;
        if (targetStepIdObj != null) {
            targetStep = stepsRepository.selectById(Long.valueOf(targetStepIdObj.toString()));
            if (targetStep == null) return Result.error("退回目标步骤不存在");
            // 只比顺序号不够，别的流程的步骤也能混进来，要确认是同一个流程定义下的节点
            if (!Objects.equals(targetStep.getDefinition_id(), inst.getDefinition_id())) {
                return Result.error("目标步骤不属于该流程");
            }
            FlowSteps curStep = inst.getCurrent_step_id() == null ? null : stepsRepository.selectById(inst.getCurrent_step_id());
            if (curStep != null && targetStep.getSort_order() != null && curStep.getSort_order() != null
                    && targetStep.getSort_order() >= curStep.getSort_order()) {
                return Result.error("只能退回到之前的步骤");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        // 更新前的节点留着当乐观锁条件，防止两人同时驳回把流程推乱
        Long originStepId = inst.getCurrent_step_id();
        addApproval(id, inst.getCurrent_step_id(), userId, 2, comment, 3, null);
        notifyMentions(id, comment, userId, inst.getTitle());

        // 退回开始节点（step_type=4 或顺序号最小）就是退回发起人
        boolean backToCreator = isStartStep(targetStep);
        // 两个分支共用同一个落库条件：流程还在审批中，且当前节点没被别人改过
        UpdateWrapper<FlowInstances> cas = new UpdateWrapper<FlowInstances>().eq("id", id).eq("status", 1);
        if (originStepId != null) cas.eq("current_step_id", originStepId);
        if (targetStep == null) {
            // 直接终止。updateById 会跳过 null 字段，这里用 UpdateWrapper 显式清空节点和审批人
            inst.setStatus(3);
            inst.setCurrent_step_name("已驳回");
            inst.setFinish_time(now);
            inst.setCurrent_auditor_id(null);
            inst.setCurrent_step_id(null);
            inst.setUpdated_at(now);
            cas.set("status", 3)
                    .set("current_step_name", "已驳回")
                    .set("finish_time", now)
                    .set("current_auditor_id", null)
                    .set("current_step_id", null)
                    .set("updated_at", now);
        } else {
            Long backAuditor = backToCreator ? inst.getCreator_id()
                    : (isPoolStep(targetStep) ? null : resolveApprover(targetStep, inst.getCreator_id(), null));
            inst.setCurrent_step_id(targetStep.getId());
            inst.setCurrent_step_name(targetStep.getStep_name());
            inst.setCurrent_auditor_id(backAuditor);
            inst.setStatus(1);
            inst.setFinish_time(null);
            inst.setUpdated_at(now);
            cas.set("current_step_id", targetStep.getId())
                    .set("current_step_name", targetStep.getStep_name())
                    .set("current_auditor_id", backAuditor)
                    .set("status", 1)
                    .set("finish_time", null)
                    .set("updated_at", now);
        }
        // 影响行数不是 1 说明流程已被别人推进，抛异常让整笔事务回滚
        if (instancesRepository.update(null, cas) != 1) {
            throw new RuntimeException("流程状态已变化，请刷新重试");
        }

        clearTodoNotifications(id);
        if (inst.getStatus() == 3) {
            notifyFlowResult(inst, "驳回", userId, "流程被驳回：" + inst.getTitle(), "您发起的流程被驳回：" + comment);
        } else if (backToCreator) {
            notifyFlowResult(inst, "退回", userId, "流程被退回：" + inst.getTitle(), "流程被退回到您这里：" + inst.getTitle());
        } else {
            notifyStepAuditors(targetStep, id, inst.getCreator_id(), "流程被退回", inst.getTitle());
        }
        return Result.success(null);
    }

    // 重新提交：退回到发起人后接着往下走，被驳回或撤回的 7 天内也能重新发起
    @PostMapping("/instances/{id}/resubmit")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> resubmit(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error("流程不存在");
        if (!Objects.equals(inst.getCreator_id(), userId)) return Result.error("只有发起人可以重新提交");
        // 能重提的只有两种：退回到发起人，或被驳回、撤回后 7 天内
        boolean backToCreator = Integer.valueOf(1).equals(inst.getStatus())
                && Objects.equals(inst.getCurrent_auditor_id(), userId);
        boolean rejectedOrWithdrawn = canResubmitWithin7Days(inst);
        if (!backToCreator && !rejectedOrWithdrawn) {
            return Result.error("当前状态不能重新提交（需要退回到发起人，或被驳回/撤回后7天内）");
        }
        Map<String, Object> business = parseBusiness(body.get("businessData"));
        FlowDefinitions def = definitionsRepository.selectById(inst.getDefinition_id());
        String constraintErr = validateConstraints(def, business);
        if (constraintErr != null) return Result.error(constraintErr);
        // 和发起时一样按起止日期重算天数
        recalcDaysIfPresent(business);
        try {
            inst.setBusiness_data(OM.writeValueAsString(business));
        } catch (Exception e) {
            return Result.error("表单数据格式错误");
        }

        LocalDateTime now = LocalDateTime.now();

        // 已驳回、已撤回的流程整条重走：回第一步、状态回审批中、清掉完成时间和审批耗时
        if (rejectedOrWithdrawn) {
            FlowSteps firstStep = firstApprovalStep(inst.getDefinition_id());
            if (firstStep == null) return Result.error("流程未配置审批步骤");
            Long firstAuditor = isPoolStep(firstStep) ? null : resolveApprover(firstStep, inst.getCreator_id(), null);
            if (!isPoolStep(firstStep) && firstAuditor == null) {
                return Result.error("第一步没有可用审批人，无法重新提交");
            }
            instancesRepository.update(null, new UpdateWrapper<FlowInstances>()
                    .eq("id", id)
                    .set("business_data", inst.getBusiness_data())
                    .set("status", 1)
                    .set("current_step_id", firstStep.getId())
                    .set("current_step_name", firstStep.getStep_name())
                    .set("current_auditor_id", firstAuditor)
                    .set("finish_time", null)
                    .set("approve_duration", null)
                    .set("updated_at", now));
            inst.setStatus(1);
            inst.setCurrent_step_id(firstStep.getId());
            inst.setCurrent_step_name(firstStep.getStep_name());
            inst.setCurrent_auditor_id(firstAuditor);
            inst.setFinish_time(null);
            inst.setApprove_duration(null);
            addApproval(id, firstStep.getId(), userId, 1, "重新提交申请", 1, null);
            // 重新提交时前端会带上新上传的附件
            bindAttachments(inst.getId(), body.get("attachmentIds"), userId);
            notifyStepAuditors(firstStep, id, inst.getCreator_id(), "新的待办流程", inst.getTitle());
            return Result.success(null);
        }

        FlowSteps curStep = inst.getCurrent_step_id() == null ? null : stepsRepository.selectById(inst.getCurrent_step_id());
        if (curStep == null) return Result.error("当前审批步骤不存在");

        // 退回发起人的情况只更新表单数据，节点流转在下面处理
        instancesRepository.update(null, new UpdateWrapper<FlowInstances>()
                .eq("id", id)
                .set("business_data", inst.getBusiness_data())
                .set("updated_at", now));

        addApproval(id, inst.getCurrent_step_id(), userId, 1, "修改后重新提交", 1, null);

        FlowSteps targetStep = nextApprovalStep(curStep.getId(), business, id, userId, inst.getCreator_id());
        if (targetStep == null) {
            completeInstance(inst);
        } else {
            moveToStep(inst, targetStep);
        }

        // 重新提交时前端会带上新上传的附件
        bindAttachments(inst.getId(), body.get("attachmentIds"), userId);

        if (targetStep == null) {
            notifyFlowResult(inst, "已完成", userId, "流程已完成：" + inst.getTitle(), "您发起的流程已审批完成");
        } else {
            notifyStepAuditors(targetStep, id, inst.getCreator_id(), "新的待办流程", inst.getTitle());
        }
        return Result.success(null);
    }

    // 驳回时可以退回的步骤：直接终止、退回发起人、当前节点之前的审批节点
    @GetMapping("/instances/{id}/returnable-steps")
    public Result<List<Map<String, Object>>> returnableSteps(@PathVariable Long id) {
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error("流程不存在");
        List<FlowSteps> steps = stepsRepository.selectList(new QueryWrapper<FlowSteps>()
                .eq("definition_id", inst.getDefinition_id()).orderByAsc("sort_order"));
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> terminate = new HashMap<>();
        terminate.put("stepId", null);
        terminate.put("stepName", "直接终止流程");
        result.add(terminate);
        FlowSteps cur = inst.getCurrent_step_id() == null ? null : stepsRepository.selectById(inst.getCurrent_step_id());
        // 顺序号最小的那一步就是起始节点，退回它等于退回发起人
        FlowSteps startStep = null;
        for (FlowSteps s : steps) {
            if (s.getSort_order() == null) continue;
            if (startStep == null || s.getSort_order() < startStep.getSort_order()) startStep = s;
        }
        if (startStep != null && !Objects.equals(startStep.getId(), inst.getCurrent_step_id())) {
            Map<String, Object> row = new HashMap<>();
            row.put("stepId", startStep.getId());
            row.put("stepName", "退回发起人（" + startStep.getStep_name() + "）");
            result.add(row);
        }
        for (FlowSteps s : steps) {
            if (Objects.equals(s.getId(), inst.getCurrent_step_id())) continue;
            // 起始节点上面已经按退回发起人加过了
            if (startStep != null && Objects.equals(s.getId(), startStep.getId())) continue;
            if (s.getStep_type() != null && s.getStep_type() != 1) continue;
            if (cur != null && s.getSort_order() != null && cur.getSort_order() != null
                    && s.getSort_order() >= cur.getSort_order()) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("stepId", s.getId());
            row.put("stepName", "退回：" + s.getStep_name());
            result.add(row);
        }
        return Result.success(result);
    }

    // 批量通过：逐条处理，处理不了的放进 skipped 返回
    @PostMapping("/instances/batch-approve")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> batchApprove(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        List<Long> ids = toIdList(body.get("ids"));
        if (ids.isEmpty()) return Result.error("请选择要批量通过的流程");
        String comment = (String) body.get("comment");
        if (comment == null || comment.trim().isEmpty()) comment = "同意";
        // flow_approvals.comment 是 1000 个字符，按码点截断，emoji 不会被截成半个
        String approvalComment = limitCodePoints(comment + "（批量审批）", 1000);

        int success = 0;
        List<String> skipped = new ArrayList<>();
        // 批量通过只处理同流程同步骤，混着勾选会把不同节点一起推走
        Long firstDefId = null;
        Long firstStepId = null;
        for (Long id : ids) {
            FlowInstances inst = instancesRepository.selectById(id);
            if (inst == null) {
                skipped.add(id + "：流程不存在");
                continue;
            }
            if (firstDefId == null) {
                firstDefId = inst.getDefinition_id();
                firstStepId = inst.getCurrent_step_id();
            } else if (!Objects.equals(firstDefId, inst.getDefinition_id())
                    || !Objects.equals(firstStepId, inst.getCurrent_step_id())) {
                skipped.add(inst.getInstance_no() + "：与首条不是同一流程的同一步骤，批量通过只支持同流程同步骤");
                continue;
            }
            if (!isCurrentAuditor(inst, userId) && !canUserApprove(inst, userId)) {
                skipped.add(inst.getInstance_no() + "：已被他人处理或不是您的待办");
                continue;
            }
            FlowSteps curStep = inst.getCurrent_step_id() == null ? null : stepsRepository.selectById(inst.getCurrent_step_id());
            if (curStep == null) {
                skipped.add(inst.getInstance_no() + "：当前步骤不存在");
                continue;
            }
            // 会签要按人逐个表态，批量通过会把节点直接推走，这里跳过
            if (isCountersign(curStep)) {
                skipped.add(inst.getInstance_no() + "：会签步骤需要单独提交意见，不支持批量通过");
                continue;
            }
            // 同样先抢待办，抢不到说明这条已被别人处理
            if (claimCurrentStep(inst, userId) == 0) {
                skipped.add(inst.getInstance_no() + "：该审批已被处理，请刷新后重试");
                continue;
            }
            inst.setCurrent_auditor_id(userId);
            addApproval(id, inst.getCurrent_step_id(), userId, 1, approvalComment, 2, null);

            Map<String, Object> business = parseBusiness(inst.getBusiness_data());
            FlowSteps targetStep = nextApprovalStep(curStep.getId(), business, id, userId, inst.getCreator_id());
            // 和单条审批一致：清掉当前节点的未读待办，把待办推给下一个节点的候选人
            clearTodoNotifications(id);
            if (targetStep == null) {
                completeInstance(inst);
                notifyFlowResult(inst, "已完成", userId, "流程已完成：" + inst.getTitle(), "您发起的流程已审批完成");
            } else {
                moveToStep(inst, targetStep);
                notifyStepAuditors(targetStep, id, inst.getCreator_id(), "新的待办流程", inst.getTitle());
                notify(inst.getCreator_id(), "流程审批进度：" + inst.getTitle(),
                        "您发起的流程已通过当前节点，进入：" + targetStep.getStep_name(),
                        "flow_instance", id, "/home/flow-mine");
            }
            success++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("skipped", skipped);
        return Result.success(result);
    }

    // 转交：把当前待办转给别人，同一步骤最多转 3 次
    @PostMapping("/instances/{id}/transfer")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> transfer(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error("流程不存在");
        // 只有审批中的流程才谈得上转交
        if (!Integer.valueOf(1).equals(inst.getStatus())) return Result.error("只有审批中的流程可以转交");
        if (!isCurrentAuditor(inst, userId) && !canUserApprove(inst, userId)) {
            return Result.error("当前不是你的待办");
        }
        String comment = (String) body.get("comment");
        if (comment == null || comment.trim().length() < 5) {
            return Result.error("转交原因必填且至少5个字");
        }
        if (comment.length() > 500) return Result.error("转交原因不能超过500字");
        Object targetIdObj = body.get("targetUserId");
        if (targetIdObj == null) return Result.error("请选择被转交人");
        Long targetUserId = Long.valueOf(targetIdObj.toString());
        Users target = usersRepository.selectById(targetUserId);
        if (target == null) return Result.error("被转交人不存在");
        if (target.getStatus() == null || target.getStatus() != 1) return Result.error("该账号已停用，不能接收转交的流程");
        if (targetUserId.equals(userId)) return Result.error("不能转交给自己");
        // 转给发起人等于绕开审批，必须挡掉
        if (Objects.equals(targetUserId, inst.getCreator_id())) return Result.error("不能转交给发起人");
        FlowSteps curStep = inst.getCurrent_step_id() == null ? null : stepsRepository.selectById(inst.getCurrent_step_id());
        if (curStep == null) return Result.error("当前审批步骤不存在");
        // 被转交人要么是这一步的候选审批人，要么本身有审批权限
        boolean isCandidate = resolveCandidates(curStep, inst.getCreator_id(), null).contains(targetUserId);
        boolean hasTodoPermission = permissionService.getUserPermissions(targetUserId).contains("flow:todo");
        if (!isCandidate && !hasTodoPermission) return Result.error("被转交人不是当前步骤的候选审批人");

        // 同一步骤的转交次数限制，查历史 action=3 的记录
        Long transferCount = approvalsRepository.selectCount(new QueryWrapper<FlowApprovals>()
                .eq("instance_id", id).eq("step_id", inst.getCurrent_step_id()).eq("action", 3));
        if (transferCount != null && transferCount >= 3) {
            return Result.error("该步骤已转交3次，需联系管理员介入");
        }

        // 先改审批人再留痕，条件带上当前审批人和审批中状态，同时转交只有一个能改到行
        UpdateWrapper<FlowInstances> cas = new UpdateWrapper<FlowInstances>()
                .eq("id", id)
                .eq("status", 1)
                .set("current_auditor_id", targetUserId)
                .set("updated_at", LocalDateTime.now());
        // 池式步骤没有锁定审批人，这时只能靠状态判断
        if (inst.getCurrent_auditor_id() != null) cas.eq("current_auditor_id", inst.getCurrent_auditor_id());
        if (instancesRepository.update(null, cas) != 1) {
            throw new RuntimeException("流程状态已变化，请刷新重试");
        }
        addApproval(id, inst.getCurrent_step_id(), userId, 3, comment, null, targetUserId);
        // 转交后原先的候选人不再是本单待办人，先清掉他们的未读
        clearTodoNotifications(id);
        notifyMentions(id, comment, userId, inst.getTitle());
        // 转交后通知新的审批人
        notify(targetUserId, "新的待办流程：" + inst.getTitle(),
                "流程【" + inst.getTitle() + "】已转交给您，请及时处理",
                "flow_instance", id, "/home/flow-todo");
        return Result.success(null);
    }

    // 转交候选人：给前端下拉用，只列这一步真正能接手的人
    @GetMapping("/instances/{id}/transfer-candidates")
    public Result<List<Map<String, Object>>> transferCandidates(@PathVariable Long id, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error("流程不存在");
        if (!canSeeInstance(userId, inst)) return Result.error(403, "无权查看该流程");
        FlowSteps curStep = inst.getCurrent_step_id() == null ? null : stepsRepository.selectById(inst.getCurrent_step_id());
        if (curStep == null) return Result.error("当前审批步骤不存在");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long candidateId : activeOnly(resolveCandidates(curStep, inst.getCreator_id(), null))) {
            // 转给自己或发起人都没有意义，直接不展示
            if (candidateId == null || candidateId.equals(userId)
                    || Objects.equals(candidateId, inst.getCreator_id())) continue;
            Users u = usersRepository.selectById(candidateId);
            if (u == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.getId());
            row.put("username", u.getUsername());
            row.put("realName", u.getReal_name());
            row.put("orgName", orgNameOf(u.getOrg_id()));
            rows.add(row);
        }
        return Result.success(rows);
    }

    // 撤回：只有发起人能撤，而且审批人还没处理过
    @PostMapping("/instances/{id}/withdraw")
    @RequirePermission("flow:withdraw")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> withdraw(@PathVariable Long id, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error("流程不存在");
        if (!Objects.equals(inst.getCreator_id(), userId)) return Result.error("只有发起人可以撤回");
        if (!Integer.valueOf(1).equals(inst.getStatus())) return Result.error("当前状态不能撤回");
        // 只有真正推进过流程的动作才算已处理，转交、催办、抄送只是留痕
        List<Long> ccStepIds = stepsRepository.selectList(new QueryWrapper<FlowSteps>()
                        .eq("definition_id", inst.getDefinition_id()).eq("step_type", 2))
                .stream().map(FlowSteps::getId).collect(Collectors.toList());
        QueryWrapper<FlowApprovals> handledQw = new QueryWrapper<FlowApprovals>()
                .eq("instance_id", id).ne("approver_id", inst.getCreator_id())
                .in("action", Arrays.asList(1, 2, 6));
        if (!ccStepIds.isEmpty()) handledQw.notIn("step_id", ccStepIds);
        Long handled = approvalsRepository.selectCount(handledQw);
        if (handled != null && handled > 0) {
            return Result.error("该流程已经被审批人处理，不能撤回");
        }

        LocalDateTime now = LocalDateTime.now();
        addApproval(id, inst.getCurrent_step_id(), userId, 4, "发起人撤回", null, null);
        // 撤回后节点和审批人必须清空，updateById 会跳过 null 字段，这里显式置空
        instancesRepository.update(null, new UpdateWrapper<FlowInstances>()
                .eq("id", id)
                .set("status", 4)
                .set("current_step_name", "已撤回")
                .set("finish_time", now)
                .set("current_auditor_id", null)
                .set("current_step_id", null)
                .set("updated_at", now));
        // 该流程的未读待办一起清掉，池式步骤的候选人也覆盖得到
        clearTodoNotifications(id);
        return Result.success(null);
    }

    // 删除已结束的流程，只有管理员能删，只标记删除
    @DeleteMapping("/instances/{id}")
    @RequirePermission("flow:def:manage")
    public Result<Void> delete(@PathVariable Long id) {
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error("流程不存在");
        if (inst.getStatus() == null || inst.getStatus() == 1) {
            return Result.error("只能删除已结束的流程（已通过/已驳回/已撤回）");
        }
        instancesRepository.deleteById(id);
        return Result.success(null);
    }

    // 催办：给当前审批人发提醒，池式步骤没有锁定审批人，给全部候选人各发一条
    @PostMapping("/instances/{id}/urge")
    @RequirePermission("flow:urge")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> urge(@PathVariable Long id, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error("流程不存在");
        if (!Objects.equals(inst.getCreator_id(), userId)) return Result.error("只有发起人可以催办");
        if (!Integer.valueOf(1).equals(inst.getStatus())) return Result.error("当前状态不能催办");
        FlowSteps curStep = inst.getCurrent_step_id() == null ? null : stepsRepository.selectById(inst.getCurrent_step_id());
        if (curStep == null) return Result.error("当前审批步骤不存在");

        // 同一流程 24 小时内只能催办一次
        Long urgedRecently = approvalsRepository.selectCount(new QueryWrapper<FlowApprovals>()
                .eq("instance_id", id).eq("action", 5)
                .ge("approve_time", LocalDateTime.now().minusHours(24)));
        if (urgedRecently != null && urgedRecently > 0) {
            return Result.error("同一流程每 24 小时只能催办一次，请等待审批人处理");
        }

        // 池式步骤认领前没有具体审批人，候选人都要收到提醒
        List<Long> targets = new ArrayList<>();
        if (inst.getCurrent_auditor_id() != null) {
            targets.add(inst.getCurrent_auditor_id());
        } else if (isPoolStep(curStep)) {
            targets.addAll(resolveCandidates(curStep, inst.getCreator_id(), null));
        }
        if (targets.isEmpty()) return Result.error("当前状态不能催办");

        // 催办只留一条痕迹，被催的人记在 target_user_id 里
        addApproval(id, inst.getCurrent_step_id(), userId, 5, "催办", null, inst.getCurrent_auditor_id());
        for (Long target : targets) {
            notify(target, "催办提醒：" + inst.getTitle(),
                    "发起人催办您审批的流程：" + inst.getTitle(),
                    "flow_instance", id, "/home/flow-todo");
        }
        return Result.success(null);
    }

    // 自动催办：审批中的流程在当前步骤停留超时就提醒审批人，步骤配了超时按配置，没配按 48 小时
    @Scheduled(cron = "0 0 * * * ?")
    public void autoUrgeTimeoutFlows() {
        int urged = urgeTimeoutFlows();
        if (urged > 0) {
            log.info("[流程自动催办] 本次自动催办 {} 条流程", urged);
        }
    }

    // 管理员手动跑一次超时催办，排查问题用
    @PostMapping("/instances/urge-timeout")
    @RequirePermission("flow:def:manage")
    public Result<Integer> urgeTimeoutNow() {
        return Result.success(urgeTimeoutFlows());
    }

    // 扫一遍审批中的流程，把停留超时的催一遍，返回催办条数
    private int urgeTimeoutFlows() {
        LocalDateTime now = LocalDateTime.now();
        List<FlowInstances> running = instancesRepository.selectList(new QueryWrapper<FlowInstances>()
                .eq("status", 1).isNotNull("current_step_id"));
        int urged = 0;
        for (FlowInstances inst : running) {
            FlowSteps step = stepsRepository.selectById(inst.getCurrent_step_id());
            if (step == null) continue;
            // 步骤没配超时小时数时按文档口径默认 48 小时
            int threshold = step.getTimeout_hours() != null && step.getTimeout_hours() > 0
                    ? step.getTimeout_hours() : DEFAULT_URGE_HOURS;

            // 进入当前步骤的时间取该步骤最后一条审批记录，没有就用流程提交时间
            FlowApprovals last = approvalsRepository.selectOne(new QueryWrapper<FlowApprovals>()
                    .eq("instance_id", inst.getId()).eq("step_id", inst.getCurrent_step_id())
                    .isNotNull("approve_time").orderByDesc("approve_time").last("limit 1"));
            LocalDateTime stepEnter = last != null && last.getApprove_time() != null
                    ? last.getApprove_time() : inst.getSubmit_time();
            if (stepEnter == null) continue;
            long hoursWaited = Duration.between(stepEnter, now).toHours();
            if (hoursWaited < threshold) continue;

            // 24 小时内催过的不重复打扰
            Long recent = approvalsRepository.selectCount(new QueryWrapper<FlowApprovals>()
                    .eq("instance_id", inst.getId()).eq("action", 5)
                    .ge("approve_time", now.minusHours(24)));
            if (recent != null && recent > 0) continue;

            // 锁定了审批人就催他，池式步骤催这一步的全部候选人
            List<Long> targets = new ArrayList<>();
            if (inst.getCurrent_auditor_id() != null) {
                targets.add(inst.getCurrent_auditor_id());
            } else if (isPoolStep(step)) {
                targets.addAll(activeOnly(resolveCandidates(step, inst.getCreator_id(), null)));
            }
            if (targets.isEmpty()) continue;

            addApproval(inst.getId(), inst.getCurrent_step_id(), inst.getCreator_id(), 5,
                    "系统自动催办：当前步骤已停留 " + hoursWaited + " 小时，超过 " + threshold + " 小时",
                    null, inst.getCurrent_auditor_id());
            for (Long target : targets) {
                notify(target, "催办提醒：" + inst.getTitle(),
                        "流程【" + inst.getTitle() + "】在您这里已停留 " + hoursWaited + " 小时，请及时处理",
                        "flow_instance", inst.getId(), "/home/flow-todo");
            }
            urged++;
        }
        return urged;
    }

    // 我作为候选人的池式步骤 id，用来拼待办查询的 or 条件
    private List<Long> myPoolStepIds(Long userId) {
        Set<Long> myRoles = userRolesRepository.selectList(new QueryWrapper<UserRoles>().eq("user_id", userId))
                .stream().map(UserRoles::getRole_id).collect(Collectors.toSet());
        Users me = usersRepository.selectById(userId);
        Long myOrgId = me == null ? null : me.getOrg_id();
        List<Long> result = new ArrayList<>();
        for (FlowSteps s : stepsRepository.selectList(new QueryWrapper<FlowSteps>())) {
            if (!isPoolStep(s)) continue;
            Map<String, Object> cfg = readCfg(s.getApprover_config());
            if (s.getApprover_type() == 2 && cfg.get("roleId") != null
                    && myRoles.contains(Long.valueOf(cfg.get("roleId").toString()))) {
                result.add(s.getId());
            } else if (s.getApprover_type() == 6 && cfg.get("orgId") != null && myOrgId != null
                    && orgSubtreeIds(Long.valueOf(cfg.get("orgId").toString())).contains(myOrgId)) {
                // 指定部门审批覆盖该部门及全部下级部门
                result.add(s.getId());
            }
        }
        return result;
    }

    // 待办条件：审批中、不是我发起的，并且要么锁给我，要么当前节点是我候选的池式步骤
    private QueryWrapper<FlowInstances> buildTodoWrapper(Long userId) {
        List<Long> poolIds = myPoolStepIds(userId);
        // 别人委托给我的待办：锁定的审核人是委托人，且当前时间在委托区间内
        List<Long> delegators = flowDelegateService.activeDelegators(userId);
        QueryWrapper<FlowInstances> qw = new QueryWrapper<>();
        qw.eq("status", 1).ne("creator_id", userId);
        qw.and(w -> {
            w.eq("current_auditor_id", userId);
            // 池式步骤要还没人认领、没被转交，才出现在全部候选人的待办里
            if (!poolIds.isEmpty()) {
                w.or(x -> x.isNull("current_auditor_id").in("current_step_id", poolIds));
            }
            // 委托人休假期间，他名下的待办也出现在代理人列表里
            if (!delegators.isEmpty()) {
                w.or(x -> x.in("current_auditor_id", delegators));
            }
        });
        return qw;
    }

    // 待办数量，铃铛角标用，只取自己的待办，不需要权限码
    @GetMapping("/instances/todo-count")
    public Result<Long> todoCount(HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        // 会签里我已经签过的单子不再算我的待办，不能直接用 count
        long count = instancesRepository.selectList(buildTodoWrapper(userId)).stream()
                .filter(i -> !alreadySigned(i, userId)).count();
        return Result.success(count);
    }

    // 我的待办列表
    @GetMapping("/instances/todo")
    public Result<List<Map<String, Object>>> todo(
            @RequestParam(required = false) Long definitionId,
            @RequestParam(required = false) Integer urgency,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        QueryWrapper<FlowInstances> qw = buildTodoWrapper(userId);
        if (definitionId != null) qw.eq("definition_id", definitionId);
        if (urgency != null) qw.eq("urgency", urgency);
        if (keyword != null && !keyword.trim().isEmpty()) qw.like("title", keyword.trim());
        // 审核人视角按到达时间倒序，updated_at 就是流转到当前节点的时间
        qw.orderByDesc("updated_at");
        List<FlowInstances> list = instancesRepository.selectList(qw);
        // 会签步骤已经签过的人不再看到这条待办
        list.removeIf(i -> alreadySigned(i, userId));
        List<Map<String, Object>> rows = assembleList(list);
        fillTodoRows(rows, list, userId);
        return Result.success(rows);
    }

    // 待办行补未读标记，还有未读通知说明他没点开过
    private void fillTodoRows(List<Map<String, Object>> rows, List<FlowInstances> list, Long userId) {
        if (rows == null || rows.isEmpty() || list == null || list.isEmpty()) return;
        List<Long> ids = new ArrayList<>();
        for (FlowInstances i : list) {
            if (i.getId() != null) ids.add(i.getId());
        }
        Set<Long> unread = new HashSet<>();
        for (NotificationRecords n : notificationRecordsRepository.selectList(new QueryWrapper<NotificationRecords>()
                .eq("user_id", userId).eq("biz_type", "flow_instance")
                .in("biz_id", ids).eq("jump_url", "/home/flow-todo").eq("is_read", 0))) {
            if (n.getBiz_id() != null) unread.add(n.getBiz_id());
        }
        for (Map<String, Object> row : rows) {
            Object idObj = row.get("id");
            Long id = idObj == null ? null : Long.valueOf(idObj.toString());
            row.put("unread", id != null && unread.contains(id));
        }
    }

    // 打开待办详情就算已读，列表上的红点随之消失
    private void markTodoRead(Long instanceId, Long userId) {
        try {
            notificationRecordsRepository.update(null, new UpdateWrapper<NotificationRecords>()
                    .eq("user_id", userId).eq("biz_type", "flow_instance").eq("biz_id", instanceId)
                    .eq("jump_url", "/home/flow-todo").eq("is_read", 0)
                    .set("is_read", 1).set("read_time", LocalDateTime.now()));
        } catch (Exception ignore) {
            // 标记已读失败不影响查看
        }
    }

    // 审批意见里的 @姓名 或 @用户名 会提醒被提到的人，同一个人只提醒一次
    private void notifyMentions(Long instanceId, String text, Long fromUserId, String title) {
        if (text == null || text.isEmpty() || text.indexOf('@') < 0) return;
        // @ 提醒只发给对该流程有查看权限的人，避免流程标题外泄
        FlowInstances mentionInst = instancesRepository.selectById(instanceId);
        if (mentionInst == null) return;
        Set<String> names = new LinkedHashSet<>();
        Matcher m = MENTION_PATTERN.matcher(text);
        while (m.find()) names.add(m.group(1));
        if (names.isEmpty()) return;
        Set<Long> notified = new HashSet<>();
        for (String name : names) {
            List<Users> users = usersRepository.selectList(new QueryWrapper<Users>()
                    .eq("status", 1).and(w -> w.eq("username", name).or().eq("real_name", name)));
            for (Users u : users) {
                if (u.getId() == null || u.getId().equals(fromUserId) || !notified.add(u.getId())) continue;
                if (!canSeeInstance(u.getId(), mentionInst)) continue;
                notify(u.getId(), "有人在审批意见里@了你：" + title,
                        "流程【" + title + "】的审批意见提到了你，请打开查看",
                        "flow_instance", instanceId, "/home/flow-detail?id=" + instanceId);
            }
        }
    }

    // 判断一条留痕是不是非审批动作：sign_status=1 是提交、重新提交、抄送，自动跳过也是系统代写
    private boolean isNotApproveRecord(FlowApprovals a) {
        if (a.getSign_status() != null && a.getSign_status() == 1) return true;
        String comment = a.getComment();
        return comment != null && comment.startsWith("该步骤无有效审批人");
    }

    // 我审批过的流程，带上我最后一次的处理动作
    @GetMapping("/instances/done")
    @RequirePermission("flow:done")
    public Result<List<Map<String, Object>>> done(HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        List<FlowApprovals> aps = approvalsRepository.selectList(new QueryWrapper<FlowApprovals>()
                .eq("approver_id", userId).in("action", Arrays.asList(1, 2, 3)).orderByDesc("approve_time"));
        // 提交申请、重新提交、抄送、自动跳过都写成 action=1，这些不能算已办
        aps.removeIf(this::isNotApproveRecord);
        if (aps.isEmpty()) return Result.success(new ArrayList<>());
        List<Long> instanceIds = new ArrayList<>();
        for (FlowApprovals a : aps) {
            if (!instanceIds.contains(a.getInstance_id())) instanceIds.add(a.getInstance_id());
        }
        Map<Long, FlowInstances> instMap = new HashMap<>();
        for (FlowInstances i : instancesRepository.selectBatchIds(instanceIds)) instMap.put(i.getId(), i);
        Map<Long, FlowApprovals> latest = new HashMap<>();
        for (FlowApprovals a : aps) {
            if (!instMap.containsKey(a.getInstance_id())) continue;
            latest.putIfAbsent(a.getInstance_id(), a);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, FlowApprovals> entry : latest.entrySet()) {
            FlowInstances i = instMap.get(entry.getKey());
            if (i == null) continue;
            Map<String, Object> row = assembleRow(i);
            row.put("myAction", entry.getValue().getAction());
            row.put("myActionText", actionText(entry.getValue().getAction()));
            result.add(row);
        }
        result.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.get("submitTime");
            LocalDateTime tb = (LocalDateTime) b.get("submitTime");
            if (ta == null || tb == null) return 0;
            return tb.compareTo(ta);
        });
        return Result.success(result);
    }

    // 我发起的流程
    @GetMapping("/instances/mine")
    @RequirePermission("flow:mine")
    public Result<List<Map<String, Object>>> mine(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        QueryWrapper<FlowInstances> qw = new QueryWrapper<>();
        qw.eq("creator_id", userId);
        if (status != null) qw.eq("status", status);
        if (keyword != null && !keyword.trim().isEmpty()) qw.like("title", keyword.trim());
        qw.orderByDesc("submit_time");
        return Result.success(assembleList(instancesRepository.selectList(qw)));
    }

    // 全部流程，管理员查看，支持筛选和分页
    @GetMapping("/instances/all")
    @RequirePermission({"flow:def:manage", "flow:all:view"})
    public Result<Map<String, Object>> all(
            @RequestParam(required = false) Long definitionId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        QueryWrapper<FlowInstances> qw = new QueryWrapper<>();
        if (definitionId != null) qw.eq("definition_id", definitionId);
        if (status != null) qw.eq("status", status);
        if (creatorId != null) qw.eq("creator_id", creatorId);
        if (keyword != null && !keyword.trim().isEmpty()) qw.like("title", keyword.trim());
        if (startTime != null && !startTime.isEmpty()) qw.ge("submit_time", startTime + " 00:00:00");
        if (endTime != null && !endTime.isEmpty()) qw.le("submit_time", endTime + " 23:59:59");
        qw.orderByDesc("submit_time");

        Page<FlowInstances> p = new Page<>(page, size);
        instancesRepository.selectPage(p, qw);

        Map<String, Object> result = new HashMap<>();
        result.put("records", assembleList(p.getRecords()));
        result.put("total", p.getTotal());
        result.put("current", p.getCurrent());
        result.put("size", p.getSize());
        return Result.success(result);
    }

    // 按表单主属性统计已完成流程的合计值
    @GetMapping("/instances/statistics")
    @RequirePermission({"flow:def:manage", "flow:all:view"})
    public Result<Map<String, Object>> statistics(
            @RequestParam Long definitionId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        FlowDefinitions def = definitionsRepository.selectById(definitionId);
        if (def == null) return Result.error("流程不存在");

        // 找出 form_config 里标了 isPrimary 的字段
        List<Map<String, Object>> primaryFields = new ArrayList<>();
        try {
            String cfgJson = def.getForm_config() == null ? "[]" : def.getForm_config();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> formCfg = OM.readValue(cfgJson, List.class);
            for (Map<String, Object> f : formCfg) {
                Object isPrimary = f.get("isPrimary");
                if (isPrimary != null && Boolean.parseBoolean(isPrimary.toString())) primaryFields.add(f);
            }
        } catch (Exception ignore) {
            // 表单配置不合法就当没有主属性
        }

        QueryWrapper<FlowInstances> qw = new QueryWrapper<>();
        qw.eq("definition_id", definitionId).eq("status", 2);
        if (startTime != null && !startTime.isEmpty()) qw.ge("submit_time", startTime + " 00:00:00");
        if (endTime != null && !endTime.isEmpty()) qw.le("submit_time", endTime + " 23:59:59");
        List<FlowInstances> list = instancesRepository.selectList(qw);

        Map<String, Double> sums = new HashMap<>();
        for (Map<String, Object> f : primaryFields) {
            sums.put((String) f.get("field"), 0.0);
        }
        for (FlowInstances inst : list) {
            Map<String, Object> biz = parseBusiness(inst.getBusiness_data());
            for (Map<String, Object> f : primaryFields) {
                String field = (String) f.get("field");
                Object v = biz.get(field);
                if (v == null) continue;
                try {
                    sums.merge(field, Double.parseDouble(v.toString()), Double::sum);
                } catch (NumberFormatException ignore) {
                    // 不是数字的字段跳过
                }
            }
        }

        Map<String, Object> fieldSummary = new HashMap<>();
        for (Map<String, Object> f : primaryFields) {
            String field = (String) f.get("field");
            Map<String, Object> row = new HashMap<>();
            row.put("label", f.get("label"));
            row.put("field", field);
            row.put("sum", sums.get(field));
            fieldSummary.put(field, row);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", list.size());
        result.put("fields", fieldSummary);
        return Result.success(result);
    }

    // 附件上传，文件落到 upload-dir/flow/年/月/日 下，先不关联业务
    @PostMapping("/instances/attachments/upload")
    @RequirePermission("flow:start")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        if (file.isEmpty()) return Result.error("文件为空");
        if (file.getSize() > 200L * 1024 * 1024) return Result.error("文件不能超过200MB");

        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        File dir = new File(uploadDir + File.separator + "flow" + File.separator + dateDir);
        if (!dir.exists()) dir.mkdirs();

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) ext = original.substring(original.lastIndexOf("."));
        // 扩展名只按白名单放行，原始文件名可以随便改，不能当安全依据
        String extName = ext.length() > 1 ? ext.substring(1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_UPLOAD_EXTS.contains(extName)) return Result.error("不支持的文件类型");
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        File dest = new File(dir, storedName);
        // 用 Files.copy 而不是 transferTo，避免相对路径下找不到目录
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }

        Attachments att = new Attachments();
        att.setFile_name(original);
        att.setFile_path(dest.getAbsolutePath());
        att.setFile_size(file.getSize());
        att.setFile_type(file.getContentType());
        att.setFile_ext(ext);
        att.setUploader_id(userId);
        att.setStorage_type(1);
        att.setCreated_at(LocalDateTime.now());
        att.setUpdated_at(LocalDateTime.now());
        // target_type 和 target_id 留空，发起流程时按 attachmentIds 绑定
        attachmentsRepository.insert(att);

        Map<String, Object> result = new HashMap<>();
        result.put("id", att.getId());
        result.put("fileName", att.getFile_name());
        result.put("fileSize", att.getFile_size());
        // 从报销单或清单里猜一个金额给前端做默认值，猜不到返回空，不阻碍上传
        com.officeautomationsystemapi.utilt.DocumentAmountReader.AmountHit hit =
                com.officeautomationsystemapi.utilt.DocumentAmountReader.read(dest, extName);
        if (hit != null) {
            result.put("suggestAmount", hit.getAmount());
            result.put("amountSource", hit.getSource());
        }
        return Result.success(result);
    }

    // 流程附件列表，看不到流程的人也拿不到附件清单
    @GetMapping("/instances/{id}/attachments")
    public Result<List<Map<String, Object>>> listAttachments(@PathVariable Long id, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error(404, "流程不存在");
        if (!canSeeInstance(userId, inst)) return Result.error(403, "无权查看该流程附件");
        return Result.success(attachmentRows(id));
    }

    // 附件下载，文件名按 UTF-8 放进响应头，下载次数加一
    @GetMapping("/instances/attachments/{aid}/download")
    public void download(@PathVariable Long aid, HttpServletResponse response, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Attachments a = attachmentsRepository.selectById(aid);
        if (a == null || a.getFile_path() == null) {
            response.setStatus(404);
            return;
        }
        // 只给上传人和能看到挂载流程的人下载，不能靠猜 id 拉走文件
        if (!canAccessAttachment(userId, a)) {
            writeForbidden(response, "无权下载该附件");
            return;
        }
        File f = new File(a.getFile_path());
        if (!f.exists()) {
            response.setStatus(404);
            return;
        }
        try (InputStream in = Files.newInputStream(f.toPath())) {
            response.setContentType(a.getFile_type() == null ? "application/octet-stream" : a.getFile_type());
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + URLEncoder.encode(a.getFile_name() == null ? "file" : a.getFile_name(), StandardCharsets.UTF_8) + "\"");
            StreamUtils.copy(in, response.getOutputStream());
            a.setDownload_count(a.getDownload_count() == null ? 1 : a.getDownload_count() + 1);
            a.setUpdated_at(LocalDateTime.now());
            attachmentsRepository.updateById(a);
        } catch (Exception ignore) {
            // 客户端取消下载时不再写错误页
        }
    }

    // 流程详情：基本信息、表单数据、附件、审批轨迹、当前用户能做的操作
    @GetMapping("/instances/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        FlowInstances inst = instancesRepository.selectById(id);
        if (inst == null) return Result.error("流程不存在");
        // 没归属关系的人不能靠遍历 id 读到别人的单据和付款信息
        if (!canSeeInstance(userId, inst)) return Result.error(403, "无权查看该流程");
        // 打开详情就算读过了，待办列表红点消失
        markTodoRead(id, userId);
        Map<String, Object> result = assembleRow(inst);
        result.put("definition", definitionsRepository.selectById(inst.getDefinition_id()));
        result.put("businessData", parseBusiness(inst.getBusiness_data()));

        boolean isMine = inst.getCreator_id() != null && inst.getCreator_id().equals(userId);
        boolean isCurrentAuditor = isCurrentAuditor(inst, userId) || canUserApprove(inst, userId);
        result.put("isMine", isMine);
        result.put("isCurrentAuditor", isCurrentAuditor);
        // 退回到发起人可以马上重提，被驳回或撤回的 7 天内也能重提
        result.put("canResubmit", isMine && ((isCurrentAuditor(inst, userId)
                && inst.getStatus() != null && inst.getStatus() == 1) || canResubmitWithin7Days(inst)));
        result.put("attachments", attachmentRows(id));
        // 会签进度：谁已签、还差几个，详情页用它显示进度条
        FlowSteps signStep = inst.getCurrent_step_id() == null ? null : stepsRepository.selectById(inst.getCurrent_step_id());
        if (signStep != null && isCountersign(signStep)) {
            List<Long> signCandidates = resolveCandidates(signStep, inst.getCreator_id(), null);
            // 排除提交记录 sign_status=1，否则发起人会被算成第一票
            List<Long> signedIds = approvalsRepository.selectList(new QueryWrapper<FlowApprovals>()
                            .eq("instance_id", id).eq("step_id", signStep.getId()).eq("action", 1)
                            .and(w -> w.isNull("sign_status").or().ne("sign_status", 1)))
                    .stream().map(FlowApprovals::getApprover_id).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, String> signNames = new HashMap<>();
            if (!signCandidates.isEmpty()) {
                for (Users u : usersRepository.selectBatchIds(signCandidates)) {
                    signNames.put(u.getId(), u.getReal_name() == null ? u.getUsername() : u.getReal_name());
                }
            }
            List<String> signedNames = new ArrayList<>();
            for (Long sid : signedIds) signedNames.add(signNames.getOrDefault(sid, "用户" + sid));
            result.put("countersign", true);
            result.put("signType", signStep.getSign_type());
            result.put("signApproved", signedIds.size());
            result.put("signRequired", requiredSigners(signStep, signCandidates.size()));
            result.put("signApprovedNames", signedNames);
            result.put("signPendingNames", signCandidates.stream().filter(x -> !signedIds.contains(x))
                    .map(x -> signNames.getOrDefault(x, "用户" + x)).collect(Collectors.toList()));
            result.put("signSignedByMe", signedIds.contains(userId));
        } else {
            result.put("countersign", false);
        }

        // 付款状态：审批通过后申请人要知道财务有没有付
        PaymentRecords payment = latestPayment(id);
        if (payment == null) {
            result.put("payStatus", 0);
            result.put("payStatusText", needsPayment(inst.getDefinition_id()) ? "待付款" : "无需付款");
        } else if (payment.getStatus() != null && payment.getStatus() == 2) {
            result.put("payStatus", 2);
            result.put("payStatusText", "已作废");
            result.put("payNo", payment.getPay_no());
            result.put("payAmount", payment.getAmount());
            result.put("payTime", payment.getPay_time());
            result.put("payeeName", payment.getPayee_name());
            result.put("categoryName", payment.getCategory_name());
            result.put("voidReason", payment.getVoid_reason());
        } else {
            result.put("payStatus", 1);
            result.put("payStatusText", "已付款");
            result.put("payNo", payment.getPay_no());
            result.put("payAmount", payment.getAmount());
            result.put("payTime", payment.getPay_time());
            result.put("payeeName", payment.getPayee_name());
            result.put("categoryName", payment.getCategory_name());
        }

        // 审批轨迹按时间正序，转交记录额外带上被转交人
        List<Map<String, Object>> track = new ArrayList<>();
        for (FlowApprovals a : approvalsRepository.selectList(new QueryWrapper<FlowApprovals>()
                .eq("instance_id", id).orderByAsc("approve_time"))) {
            Map<String, Object> row = new HashMap<>();
            Users approver = a.getApprover_id() == null ? null : usersRepository.selectById(a.getApprover_id());
            row.put("approverName", approver == null ? "系统" : approver.getReal_name());
            row.put("action", a.getAction());
            row.put("actionText", actionText(a.getAction()));
            row.put("comment", a.getComment());
            row.put("time", a.getApprove_time());
            if (a.getTarget_user_id() != null) {
                Users target = usersRepository.selectById(a.getTarget_user_id());
                row.put("targetName", target == null ? "" : target.getReal_name());
            }
            track.add(row);
        }
        result.put("track", track);
        return Result.success(result);
    }

    // 一条流程最近一次付款记录，作废后重付取到新的那条
    private PaymentRecords latestPayment(Long instanceId) {
        if (instanceId == null) return null;
        List<PaymentRecords> records = paymentRecordsRepository.selectList(new QueryWrapper<PaymentRecords>()
                .eq("instance_id", instanceId).orderByDesc("id").last("LIMIT 1"));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    // 这个流程模板是不是走财务付款
    private boolean needsPayment(Long definitionId) {
        if (definitionId == null) return false;
        FlowDefinitions def = definitionsRepository.selectById(definitionId);
        return def != null && def.getNeed_payment() != null && def.getNeed_payment() == 1;
    }

    // 能不能看这条流程：管理员、发起人、当前审批人、处理过的人都算有权
    private boolean canSeeInstance(Long userId, FlowInstances inst) {
        if (userId == null || inst == null || inst.getId() == null) return false;
        if (permissionService.isAdmin(userId)) return true;
        if (Objects.equals(inst.getCreator_id(), userId)) return true;
        if (Objects.equals(inst.getCurrent_auditor_id(), userId)) return true;
        // 代理人的待办也要能点开详情，否则列表有行却进不去
        if (flowDelegateService.canHandle(userId, inst.getCurrent_auditor_id())) return true;
        // 池式步骤没有锁定审批人，候选人也能看详情，否则点进去就是 403
        if (canUserApprove(inst, userId)) return true;
        // 审批轨迹里有我的留痕，说明我审过或抄送过
        Long count = approvalsRepository.selectCount(new QueryWrapper<FlowApprovals>()
                .eq("instance_id", inst.getId()).eq("approver_id", userId));
        return count != null && count > 0;
    }

    // 当前锁定的审批人是不是我
    private boolean isCurrentAuditor(FlowInstances inst, Long userId) {
        if (userId == null) return false;
        if (userId.equals(inst.getCurrent_auditor_id())) return true;
        // 委托人把审批权临时委托给了当前操作人
        return flowDelegateService.canHandle(userId, inst.getCurrent_auditor_id());
    }

    // 是不是池式步骤的候选人，候选人可以认领这条待办
    private boolean canUserApprove(FlowInstances inst, Long userId) {
        if (inst.getCurrent_step_id() == null || inst.getCreator_id() == null || userId == null) return false;
        // 已被别人认领或转交出去的单子只归当前审批人，其他候选人不能再抢
        if (inst.getCurrent_auditor_id() != null) return false;
        FlowSteps step = stepsRepository.selectById(inst.getCurrent_step_id());
        return isPoolStep(step) && resolveCandidates(step, inst.getCreator_id(), null).contains(userId);
    }

    // 校验 form_config 里配的跨字段约束，全部通过返回 null
    private String validateConstraints(FlowDefinitions def, Map<String, Object> business) {
        if (def == null || business == null || def.getForm_config() == null) return null;
        for (Map<String, Object> c : readConstraints(def.getForm_config())) {
            Object f1 = c.get("field1");
            Object f2 = c.get("field2");
            Object op = c.get("op");
            if (f1 == null || f2 == null || op == null) continue;
            String err = checkConstraint(c, f1.toString(), f2.toString(), op.toString().trim(), business);
            if (err != null) return err;
        }
        return null;
    }

    // 单条约束：按值的写法判断类型，日期按日期比、数字按数字比、其余按文本比
    private String checkConstraint(Map<String, Object> c, String field1, String field2, String op,
                                   Map<String, Object> business) {
        Object v1 = businessValue(business, field1);
        Object v2 = businessValue(business, field2);
        // 有一个没填就留给必填校验，这里不做比较
        if (v1 == null || v2 == null) return null;
        String label1 = constraintLabel(c, "label1", field1);
        String label2 = constraintLabel(c, "label2", field2);
        String s1 = v1.toString().trim();
        String s2 = v2.toString().trim();
        Boolean ok;
        // 任意一边像日期就按日期解析，另一边解析不出来说明格式填错了
        if (isDateValue(s1) || isDateValue(s2)) {
            LocalDate d1 = parseDateValue(s1);
            LocalDate d2 = parseDateValue(s2);
            if (d1 == null) return label1 + "格式不正确";
            if (d2 == null) return label2 + "格式不正确";
            ok = compareOrder(d1.compareTo(d2), op);
        } else if (isNumberValue(s1) || isNumberValue(s2)) {
            Double n1 = parseNumberValue(s1);
            Double n2 = parseNumberValue(s2);
            if (n1 == null) return label1 + "必须是数字";
            if (n2 == null) return label2 + "必须是数字";
            ok = compareOrder(Double.compare(n1, n2), op);
        } else {
            ok = compareOrder(s1.compareTo(s2), op);
        }
        // 只有不认识的比较符才跳过，认识的比较符一律给结论
        if (ok == null) return null;
        return ok ? null : "属性约束不满足：" + label1 + " " + op + " " + label2;
    }

    // 按比较符给结论，不认识的返回 null
    private Boolean compareOrder(int cmp, String op) {
        switch (op) {
            case "<": return cmp < 0;
            case "<=": return cmp <= 0;
            case ">": return cmp > 0;
            case ">=": return cmp >= 0;
            case "==": return cmp == 0;
            case "!=": return cmp != 0;
            default: return null;
        }
    }

    // 约束里的字段名可能是驼峰，business_data 里是下划线，两种都要取到
    private Object businessValue(Map<String, Object> business, String field) {
        if (business == null || field == null) return null;
        if (business.containsKey(field)) return business.get(field);
        String snake = toSnakeCase(field);
        if (!snake.equals(field) && business.containsKey(snake)) return business.get(snake);
        String camel = toCamelCase(field);
        if (!camel.equals(field) && business.containsKey(camel)) return business.get(camel);
        return null;
    }

    // 驼峰转下划线
    private String toSnakeCase(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    // 下划线转驼峰
    private String toCamelCase(String name) {
        if (name.indexOf('_') < 0) return name;
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '_') {
                upper = true;
                continue;
            }
            sb.append(upper ? Character.toUpperCase(ch) : ch);
            upper = false;
        }
        return sb.toString();
    }

    // 约束里的中文标签，没配就用字段名
    private String constraintLabel(Map<String, Object> c, String key, String field) {
        Object label = c == null ? null : c.get(key);
        if (label == null || label.toString().trim().isEmpty()) return field;
        return label.toString().trim();
    }

    // 形如 2026-01-01 的值当日期处理
    private boolean isDateValue(String text) {
        return text != null && text.matches("\\d{4}-\\d{2}-\\d{2}.*");
    }

    // 日期统一解析到天，日期和日期时间两种写法都认，解析不了返回 null
    private LocalDate parseDateValue(String text) {
        if (text == null || text.length() < 10) return null;
        try {
            return LocalDate.parse(text.replace('T', ' ').substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    // 数字按数字解析，解析不了返回 null
    private Double parseNumberValue(String text) {
        try {
            return Double.valueOf(text);
        } catch (Exception e) {
            return null;
        }
    }

    // 是不是数字写法
    private boolean isNumberValue(String text) {
        return text != null && text.matches("-?\\d+(\\.\\d+)?");
    }

    // 读约束配置，只有 form_config 是对象且带 constraints 数组时才生效
    private List<Map<String, Object>> readConstraints(String formConfig) {
        List<Map<String, Object>> constraints = new ArrayList<>();
        try {
            Object parsed = OM.readValue(formConfig, Object.class);
            if (parsed instanceof Map) {
                Object c = ((Map<?, ?>) parsed).get("constraints");
                if (c instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> raw = (List<Map<String, Object>>) c;
                    constraints.addAll(raw);
                }
            }
        } catch (Exception ignore) {
            // 解析不了就当没有约束
        }
        return constraints;
    }

    // 读审批人配置，解析失败按空对象处理
    private Map<String, Object> readCfg(String json) {
        try {
            return OM.readValue(json == null ? "{}" : json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // 解析步骤的全部候选审批人，发起人本人始终排除
    // 取某个角色下除发起人以外的全部用户，去重后追加到 ids
    private void collectRoleUsers(Long roleId, Long creatorId, List<Long> ids) {
        List<UserRoles> urs = userRolesRepository.selectList(
                new QueryWrapper<UserRoles>().eq("role_id", roleId));
        for (UserRoles ur : urs) {
            if (ur.getUser_id() == null || Objects.equals(ur.getUser_id(), creatorId)) continue;
            if (!ids.contains(ur.getUser_id())) ids.add(ur.getUser_id());
        }
    }

    private List<Long> resolveCandidates(FlowSteps step, Long creatorId, Long selectedId) {
        List<Long> ids = new ArrayList<>();
        if (step == null) return ids;
        int type = step.getApprover_type() == null ? 1 : step.getApprover_type();
        try {
            // 发起人自选，选的人不能是自己
            if (type == 5) {
                if (selectedId != null && !selectedId.equals(creatorId)) ids.add(selectedId);
                return ids;
            }
            Map<String, Object> cfg = readCfg(step.getApprover_config());
            // 指定用户
            if (type == 1) {
                Object uid = cfg.get("userId");
                if (uid != null && !Long.valueOf(uid.toString()).equals(creatorId)) {
                    ids.add(Long.valueOf(uid.toString()));
                }
                return ids;
            }
            // 指定角色，取该角色下的全部用户
            if (type == 2) {
                Object roleId = cfg.get("roleId");
                if (roleId == null) return ids;
                collectRoleUsers(Long.valueOf(roleId.toString()), creatorId, ids);
                // 这个角色只有发起人本人时，往上传给总经理
                if (ids.isEmpty()) collectRoleUsers(6L, creatorId, ids);
                // 还是没有就交给管理员，保证流程不会发起就卡死
                if (ids.isEmpty()) collectRoleUsers(1L, creatorId, ids);
                return activeOnly(ids);
            }
            // 部门主管，从发起人部门逐级往上找第一个不是发起人的主管
            if (type == 3) {
                Users creator = usersRepository.selectById(creatorId);
                if (creator == null || creator.getOrg_id() == null) return ids;
                Long orgId = creator.getOrg_id();
                for (int depth = 0; depth < 10; depth++) {
                    Organizations org = organizationsRepository.selectById(orgId);
                    if (org == null) break;
                    if (org.getLeader_id() != null && !org.getLeader_id().equals(creatorId)) {
                        ids.add(org.getLeader_id());
                        return ids;
                    }
                    if (org.getParent_id() == null || org.getParent_id() == 0) break;
                    orgId = org.getParent_id();
                }
                // 部门链上没有负责人，或者负责人就是发起人，退给全部部门主管角色
                if (ids.isEmpty()) {
                    List<UserRoles> managers = userRolesRepository.selectList(
                            new QueryWrapper<UserRoles>().eq("role_id", 2L));
                    for (UserRoles ur : managers) {
                        if (!Objects.equals(ur.getUser_id(), creatorId)) ids.add(ur.getUser_id());
                    }
                }
                // 连部门主管都没有就交给管理员，保证流程不会停在没人能处理的状态
                if (ids.isEmpty()) {
                    List<UserRoles> admins = userRolesRepository.selectList(
                            new QueryWrapper<UserRoles>().eq("role_id", 1L));
                    for (UserRoles ur : admins) {
                        if (!Objects.equals(ur.getUser_id(), creatorId)) ids.add(ur.getUser_id());
                    }
                }
                return activeOnly(ids);
            }
            // 指定职位，优先取发起人同部门的同职位用户
            if (type == 4) {
                Object posId = cfg.get("positionId");
                if (posId == null) return ids;
                Users creator = usersRepository.selectById(creatorId);
                QueryWrapper<Users> uQw = new QueryWrapper<Users>()
                        .eq("position_id", Long.valueOf(posId.toString())).eq("status", 1);
                if (creator != null && creator.getOrg_id() != null) uQw.eq("org_id", creator.getOrg_id());
                for (Users u : usersRepository.selectList(uQw)) {
                    if (!Objects.equals(u.getId(), creatorId)) ids.add(u.getId());
                }
                return ids;
            }
            // 指定部门先到先得，取该部门及全部下级部门的成员
            if (type == 6) {
                Object orgId = cfg.get("orgId");
                if (orgId == null) return ids;
                List<Long> orgIds = orgSubtreeIds(Long.valueOf(orgId.toString()));
                if (orgIds.isEmpty()) return ids;
                List<Users> members = usersRepository.selectList(new QueryWrapper<Users>()
                        .in("org_id", orgIds).eq("status", 1));
                for (Users u : members) {
                    if (!Objects.equals(u.getId(), creatorId)) ids.add(u.getId());
                }
                return activeOnly(ids);
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return ids;
    }

    // 过滤掉已停用的账号，避免流程落到没人处理的人身上
    private List<Long> activeOnly(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        List<Users> users = usersRepository.selectBatchIds(ids);
        Set<Long> active = new HashSet<>();
        for (Users u : users) {
            if (u.getStatus() != null && u.getStatus() == 1) active.add(u.getId());
        }
        List<Long> result = new ArrayList<>();
        for (Long id : ids) {
            if (active.contains(id) && !result.contains(id)) result.add(id);
        }
        return result;
    }

    // 部门及其全部下级部门：指定部门审批时，子部门的人也算
    // 使用范围 0 全员、1 指定部门含下级、2 指定角色；没配具体对象按全员处理
    private String checkUseScope(FlowDefinitions def, Long userId) {
        int scope = def.getUse_scope() == null ? 0 : def.getUse_scope();
        if (scope == 0) return null;
        List<Long> ids = parseScopeIds(def.getUse_scope_ids());
        if (ids.isEmpty()) return null;
        if (permissionService.isAdmin(userId)) return null;
        if (scope == 2) {
            List<Long> myRoles = new ArrayList<>();
            for (UserRoles ur : userRolesRepository.selectList(new QueryWrapper<UserRoles>().eq("user_id", userId))) {
                if (ur.getRole_id() != null) myRoles.add(ur.getRole_id());
            }
            for (Long id : ids) {
                if (myRoles.contains(id)) return null;
            }
            return "该流程只对指定角色开放，您不在使用范围内";
        }
        Users me = usersRepository.selectById(userId);
        Long myOrg = me == null ? null : me.getOrg_id();
        if (myOrg != null) {
            for (Long orgId : ids) {
                if (orgSubtreeIds(orgId).contains(myOrg)) return null;
            }
        }
        return "该流程只对指定部门开放，您不在使用范围内";
    }

    // 使用范围对象 id 存成 JSON 数组，解析失败按空处理
    private List<Long> parseScopeIds(String json) {
        List<Long> ids = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return ids;
        try {
            List<?> raw = OM.readValue(json, List.class);
            for (Object o : raw) {
                if (o != null) ids.add(Long.valueOf(o.toString()));
            }
        } catch (Exception ignore) {
            // 配置坏了就按全员放行，避免流程发不出去
        }
        return ids;
    }

    private List<Long> orgSubtreeIds(Long rootId) {
        List<Long> result = new ArrayList<>();
        if (rootId == null) return result;
        List<Organizations> all = organizationsRepository.selectList(new QueryWrapper<Organizations>());
        Map<Long, List<Long>> children = new HashMap<>();
        for (Organizations o : all) {
            if (o.getParent_id() == null) continue;
            children.computeIfAbsent(o.getParent_id(), k -> new ArrayList<>()).add(o.getId());
        }
        List<Long> queue = new ArrayList<>();
        queue.add(rootId);
        result.add(rootId);
        // 广度优先逐层展开，出现过的跳过，防止脏数据成环
        for (int i = 0; i < queue.size(); i++) {
            List<Long> kids = children.get(queue.get(i));
            if (kids == null) continue;
            for (Long kid : kids) {
                if (kid == null || result.contains(kid)) continue;
                result.add(kid);
                queue.add(kid);
            }
        }
        return result;
    }

    // 部门名，前端展示申请人或候选人所属部门用
    private String orgNameOf(Long orgId) {
        if (orgId == null) return null;
        Organizations org = organizationsRepository.selectById(orgId);
        return org == null ? null : org.getOrg_name();
    }

    // 取第一个候选审批人，用于单人锁定场景
    private Long resolveApprover(FlowSteps step, Long creatorId, Long selectedId) {
        List<Long> candidates = resolveCandidates(step, creatorId, selectedId);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    // 会签：sign_type 2 全员会签、3 按比例会签，需要多人分别表态
    private boolean isCountersign(FlowSteps step) {
        if (step == null) return false;
        Integer sign = step.getSign_type();
        return sign != null && (sign == 2 || sign == 3);
    }

    // 池式步骤：指定部门先到先得，或指定角色且是或签，这种步骤不锁定审批人
    private boolean isPoolStep(FlowSteps step) {
        if (step == null) return false;
        // 会签也算池式，不锁定单个审批人，全部候选人一起看到待办
        if (isCountersign(step)) return true;
        int type = step.getApprover_type() == null ? 1 : step.getApprover_type();
        if (type == 6) return true;
        if (type == 2) {
            Integer sign = step.getSign_type();
            return sign == null || sign == 1;
        }
        return false;
    }

    // 第一步：审批节点里排序最靠前的那个
    private FlowSteps firstApprovalStep(Long definitionId) {
        List<FlowSteps> steps = stepsRepository.selectList(new QueryWrapper<FlowSteps>()
                .eq("definition_id", definitionId).eq("step_type", 1).orderByAsc("sort_order"));
        return steps.isEmpty() ? null : steps.get(0);
    }

    // 顺序号最小的那一步就是起始节点，退回它等于退回发起人
    private boolean isStartStep(FlowSteps step) {
        if (step == null) return false;
        if (step.getStep_type() != null && step.getStep_type() == 4) return true;
        List<FlowSteps> steps = stepsRepository.selectList(new QueryWrapper<FlowSteps>()
                .eq("definition_id", step.getDefinition_id()).orderByAsc("sort_order"));
        FlowSteps first = null;
        for (FlowSteps s : steps) {
            if (s.getSort_order() == null) continue;
            if (first == null || s.getSort_order() < first.getSort_order()) first = s;
        }
        return first != null && Objects.equals(first.getId(), step.getId());
    }

    // 抢当前待办：流程还在审批中且当前节点没被推进时才更新得到行，返回受影响行数
    private int claimCurrentStep(FlowInstances inst, Long userId) {
        if (inst == null || inst.getCurrent_step_id() == null) return 0;
        return instancesRepository.update(null, new UpdateWrapper<FlowInstances>()
                .eq("id", inst.getId())
                .eq("status", 1)
                .eq("current_step_id", inst.getCurrent_step_id())
                .set("current_auditor_id", userId)
                .set("updated_at", LocalDateTime.now()));
    }

    // 按 Unicode 码点截断，emoji 这种占两个 char 的字符不会被截成半个
    private String limitCodePoints(String text, int max) {
        if (text == null) return null;
        if (text.codePointCount(0, text.length()) <= max) return text;
        return text.substring(0, text.offsetByCodePoints(0, max));
    }

    // 沿流转线找下一个需要人审的步骤，抄送自动留痕跳过，返回 null 表示流程结束
    private FlowSteps nextApprovalStep(Long fromStepId, Map<String, Object> business, Long instanceId,
                                       Long operatorId, Long creatorId) {
        FlowStepTransitions next = matchNextTransition(fromStepId, business);
        for (int guard = 0; next != null && guard < MAX_CHAIN_STEPS; guard++) {
            FlowSteps ts = stepsRepository.selectById(next.getTarget_step_id());
            if (ts == null) return null;
            // 抄送节点只记一条留痕，不挡流转
            if (ts.getStep_type() != null && ts.getStep_type() == 2) {
                Long ccAuditor = resolveApprover(ts, creatorId, null);
                addApproval(instanceId, ts.getId(), ccAuditor == null ? operatorId : ccAuditor, 1,
                        "抄送：" + ts.getStep_name(), 1, null);
                next = matchNextTransition(ts.getId(), business);
                continue;
            }
            // 池式步骤不锁定审批人，直接作为下一步
            if (isPoolStep(ts)) return ts;
            if (resolveApprover(ts, creatorId, null) != null) return ts;
            // 审批人没配或就是发起人本人，留痕后继续往后走
            addApproval(instanceId, ts.getId(), operatorId, 1,
                    "该步骤无有效审批人（审批人不能是发起人本人或未配置），自动跳过", 2, null);
            next = matchNextTransition(ts.getId(), business);
        }
        return null;
    }

    // 发起前整链检查，保证每一步都有审批人可审
    private String validateChainNoSelf(Long startStepId, Long creatorId, Map<String, Object> business) {
        FlowSteps cur = stepsRepository.selectById(startStepId);
        for (int guard = 0; cur != null && guard < MAX_CHAIN_STEPS; guard++) {
            // 抄送节点和发起人自选节点不参与校验
            boolean skipCheck = (cur.getStep_type() != null && cur.getStep_type() == 2)
                    || (cur.getApprover_type() != null && cur.getApprover_type() == 5);
            if (!skipCheck && resolveCandidates(cur, creatorId, null).isEmpty()) {
                return "流程步骤【" + cur.getStep_name() + "】无有效审批人（审批人不能是发起人本人或未配置），无法发起";
            }
            cur = chainNext(cur.getId(), business);
        }
        return null;
    }

    // 整链预演时取下一个非抄送步骤
    private FlowSteps chainNext(Long stepId, Map<String, Object> business) {
        FlowStepTransitions next = matchNextTransition(stepId, business);
        for (int guard = 0; next != null && guard < MAX_CHAIN_STEPS; guard++) {
            FlowSteps ts = stepsRepository.selectById(next.getTarget_step_id());
            if (ts == null) return null;
            if (ts.getStep_type() != null && ts.getStep_type() == 2) {
                next = matchNextTransition(ts.getId(), business);
                continue;
            }
            return ts;
        }
        return null;
    }

    // 按优先级找命中的流转条件，默认分支留到最后
    private FlowStepTransitions matchNextTransition(Long stepId, Map<String, Object> business) {
        List<FlowStepTransitions> list = transitionsRepository.selectList(new QueryWrapper<FlowStepTransitions>()
                .eq("step_id", stepId).eq("transition_type", 1).orderByAsc("priority"));
        FlowStepTransitions defaultT = null;
        for (FlowStepTransitions t : list) {
            if (t.getIs_default() != null && t.getIs_default() == 1) {
                if (defaultT == null) defaultT = t;
                continue;
            }
            if (t.getCondition_expr() == null || t.getCondition_expr().trim().isEmpty()) {
                if (defaultT == null) defaultT = t;
                continue;
            }
            if (evalCondition(t.getCondition_expr(), business)) return t;
        }
        return defaultT;
    }

    // 条件表达式先按 || 拆成多组，组内按 && 拆，全组为 true 才算命中
    private boolean evalCondition(String expr, Map<String, Object> business) {
        if (business == null || business.isEmpty()) return false;
        for (String orPart : expr.split("\\|\\|")) {
            boolean allAnd = true;
            for (String p : orPart.split("&&")) {
                if (!evalSingle(p.trim(), business)) {
                    allAnd = false;
                    break;
                }
            }
            if (allAnd) return true;
        }
        return false;
    }

    // 单个条件，支持 in、not in 和 >= <= == != > <
    private boolean evalSingle(String cond, Map<String, Object> business) {
        // 包含和不包含，按字符串子串比较
        Matcher cM = CONTAINS_PATTERN.matcher(cond);
        if (cM.matches()) {
            Object actual = business.get(cM.group(1));
            if (actual == null) return false;
            String needle = cM.group(3).trim().replaceAll("^['\"]|['\"]$", "");
            boolean hit = actual.toString().contains(needle);
            return "contains".equals(cM.group(2).replaceAll("\\s+", "")) ? hit : !hit;
        }

        Matcher inM = IN_PATTERN.matcher(cond);
        if (inM.matches()) {
            String field = inM.group(1);
            String op = inM.group(2).replaceAll("\\s+", "");
            String[] items = inM.group(3).split(",");
            Object actual = business.get(field);
            if (actual == null) return false;
            String a = actual.toString().trim();
            boolean hit = false;
            for (String it : items) {
                if (a.equals(it.trim().replaceAll("^['\\\"]|['\\\"]$", ""))) {
                    hit = true;
                    break;
                }
            }
            return "in".equals(op) ? hit : !hit;
        }

        Matcher m = COMPARE_PATTERN.matcher(cond);
        if (!m.matches()) return false;
        String field = m.group(1);
        String op = m.group(2);
        String val = m.group(3);
        Object actual = business.get(field);
        if (actual == null) return false;
        try {
            double a = Double.parseDouble(actual.toString());
            double b = Double.parseDouble(val);
            switch (op) {
                case ">": return a > b;
                case ">=": return a >= b;
                case "<": return a < b;
                case "<=": return a <= b;
                case "==": return a == b;
                case "!=": return a != b;
            }
        } catch (NumberFormatException e) {
            String a = actual.toString();
            if ("==".equals(op)) return a.equals(val);
            if ("!=".equals(op)) return !a.equals(val);
        }
        return false;
    }

    // 把业务数据统一转成 Map，字符串就当成 JSON 解析
    private Map<String, Object> parseBusiness(Object raw) {
        if (raw == null) return new HashMap<>();
        if (raw instanceof String) return parseBusiness((String) raw);
        try {
            return parseBusiness(OM.writeValueAsString(raw));
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // 解析库里存的业务数据 JSON，解析不出对象就返回空 Map
    private Map<String, Object> parseBusiness(String json) {
        if (json == null || json.trim().isEmpty()) return new HashMap<>();
        try {
            Object o = OM.readValue(json, Object.class);
            if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) o;
                return m;
            }
        } catch (Exception ignore) {
            // 脏数据按空表单处理
        }
        return new HashMap<>();
    }

    // 表单里有起止日期时按自然日重算天数，键名和 business_data 的 snake_case 一致
    private void recalcDaysIfPresent(Map<String, Object> businessData) {
        if (businessData == null) return;
        Object s = businessData.get("start_date");
        Object e = businessData.get("end_date");
        // 缺字段就直接返回，不往表单里塞对不上的键
        if (s == null || e == null) return;
        try {
            LocalDate sd = LocalDate.parse(s.toString().replace('T', ' ').substring(0, 10));
            LocalDate ed = LocalDate.parse(e.toString().replace('T', ' ').substring(0, 10));
            if (ed.isBefore(sd)) return;
            businessData.put("days", (int) (ChronoUnit.DAYS.between(sd, ed) + 1));
        } catch (Exception ignore) {
            // 日期格式不对就保留前端算好的天数
        }
    }

    // 插入流程实例：撞号时重新取号再试，最多试 3 次
    private void insertInstance(FlowInstances inst) {
        for (int attempt = 1; ; attempt++) {
            try {
                instancesRepository.insert(inst);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt >= 3) throw e;
                inst.setInstance_no(generateInstanceNo());
            }
        }
    }

    // 生成流程编号 FLOW 加日期加四位序号，先取当天最大序号再加一，加锁保证同一时刻只发一个号
    private synchronized String generateInstanceNo() {
        LocalDate today = LocalDate.now();
        int dayKey = today.getYear() * 10000 + today.getMonthValue() * 100 + today.getDayOfMonth();
        String prefix = "FLOW-" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        if (dayKey != lastSeqDayKey) {
            lastSeqDayKey = dayKey;
            lastSeq = 0;
        }
        int seq = lastSeq;
        // 序号补满 4 位，按字符串倒序就是按序号倒序
        FlowInstances last = instancesRepository.selectOne(new QueryWrapper<FlowInstances>()
                .likeRight("instance_no", prefix).orderByDesc("instance_no").last("limit 1"));
        if (last != null && last.getInstance_no() != null && last.getInstance_no().length() > prefix.length()) {
            try {
                seq = Math.max(seq, Integer.parseInt(last.getInstance_no().substring(prefix.length())));
            } catch (NumberFormatException ignore) {
                // 历史编号不是纯数字时按已发出的序号继续
            }
        }
        lastSeq = seq + 1;
        return prefix + String.format(Locale.ROOT, "%04d", lastSeq);
    }

    // 把发起前上传的附件挂到这条流程上，只改自己上传的那条
    private void bindAttachments(Long instanceId, Object attachmentIds, Long userId) {
        for (Long aid : toIdList(attachmentIds)) {
            // 条件里带 uploader_id，别人的附件 id 传进来也更新不到行
            int rows = attachmentsRepository.update(null, new UpdateWrapper<Attachments>()
                    .eq("id", aid)
                    .eq("uploader_id", userId)
                    .set("target_type", "flow_instance")
                    .set("target_id", instanceId)
                    .set("updated_at", LocalDateTime.now()));
            if (rows != 1) throw new RuntimeException("只能绑定自己上传的附件");
        }
    }

    // 前端传的 id 数组统一转成 Long，数字和字符串两种写法都认
    private List<Long> toIdList(Object raw) {
        List<Long> ids = new ArrayList<>();
        if (!(raw instanceof List)) return ids;
        for (Object v : (List<?>) raw) {
            if (v instanceof Number) {
                ids.add(((Number) v).longValue());
            } else if (v != null) {
                try {
                    ids.add(Long.valueOf(v.toString()));
                } catch (NumberFormatException ignore) {
                    // 非法 id 直接忽略
                }
            }
        }
        return ids;
    }

    // 附件能不能看：自己上传的放行，挂在流程上的按流程可见性判断
    private boolean canAccessAttachment(Long userId, Attachments a) {
        if (userId == null || a == null) return false;
        if (Objects.equals(a.getUploader_id(), userId)) return true;
        if (!"flow_instance".equals(a.getTarget_type()) || a.getTarget_id() == null) return false;
        FlowInstances inst = instancesRepository.selectById(a.getTarget_id());
        return canSeeInstance(userId, inst);
    }

    // 下载被拒时改状态码并写回统一结构，前端不会把错误 JSON 当成文件
    private void writeForbidden(HttpServletResponse response, String message) {
        try {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"" + message + "\"}");
        } catch (Exception ignore) {
            // 响应已提交时不再写错误体
        }
    }

    // 流程附件列表，下载地址由前端拼接口路径
    private List<Map<String, Object>> attachmentRows(Long instanceId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Attachments a : attachmentsRepository.selectList(new QueryWrapper<Attachments>()
                .eq("target_type", "flow_instance").eq("target_id", instanceId).orderByDesc("created_at"))) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", a.getId());
            row.put("fileName", a.getFile_name());
            row.put("fileSize", a.getFile_size());
            row.put("fileExt", a.getFile_ext());
            row.put("downloadUrl", "/api/flow/instances/attachments/" + a.getId() + "/download");
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> assembleList(List<FlowInstances> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (FlowInstances i : list) result.add(assembleRow(i));
        return result;
    }

    // 批量组装列表行，用户和流程定义一次查成 Map，导出时不再逐行查库
    private List<Map<String, Object>> assembleListBatch(List<FlowInstances> list) {
        Set<Long> userIds = new HashSet<>();
        Set<Long> definitionIds = new HashSet<>();
        for (FlowInstances i : list) {
            if (i.getCreator_id() != null) userIds.add(i.getCreator_id());
            if (i.getCurrent_auditor_id() != null) userIds.add(i.getCurrent_auditor_id());
            if (i.getDefinition_id() != null) definitionIds.add(i.getDefinition_id());
        }
        Map<Long, String> nameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Users u : usersRepository.selectBatchIds(userIds)) {
                nameMap.put(u.getId(), u.getReal_name() == null ? "" : u.getReal_name());
            }
        }
        Map<Long, String> flowNameMap = new HashMap<>();
        if (!definitionIds.isEmpty()) {
            for (FlowDefinitions d : definitionsRepository.selectBatchIds(definitionIds)) {
                flowNameMap.put(d.getId(), d.getFlow_name() == null ? "" : d.getFlow_name());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (FlowInstances i : list) result.add(assembleRow(i, nameMap, flowNameMap));
        return result;
    }

    // 列表行，前端依赖这里的键名
    private Map<String, Object> assembleRow(FlowInstances i) {
        return assembleRow(i, null, null);
    }

    // 列表行，预查好的 Map 传进来直接用，传 null 就单条查库
    private Map<String, Object> assembleRow(FlowInstances i, Map<Long, String> nameMap, Map<Long, String> flowNameMap) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", i.getId());
        row.put("instanceNo", i.getInstance_no());
        row.put("title", i.getTitle());
        row.put("status", i.getStatus());
        row.put("statusText", instanceStatusText(i.getStatus()));
        row.put("currentStepId", i.getCurrent_step_id());
        row.put("currentStepName", i.getCurrent_step_name());
        row.put("currentAuditorId", i.getCurrent_auditor_id());
        row.put("creatorId", i.getCreator_id());
        row.put("urgency", i.getUrgency());
        row.put("submitTime", i.getSubmit_time());
        row.put("finishTime", i.getFinish_time());
        // 到达当前节点的时间，待办列表按它倒序排
        row.put("arrivedTime", i.getUpdated_at());
        row.put("creatorName", nameMap == null ? userName(i.getCreator_id()) : mapText(nameMap, i.getCreator_id()));
        String auditorName = nameMap == null ? userName(i.getCurrent_auditor_id()) : mapText(nameMap, i.getCurrent_auditor_id());
        // 部门池等不锁人的步骤没有当前审批人，明确提示待认领
        if ((auditorName == null || auditorName.isBlank()) && Integer.valueOf(1).equals(i.getStatus())) {
            auditorName = "待认领";
        }
        row.put("auditorName", auditorName);
        if (flowNameMap != null) {
            row.put("flowName", mapText(flowNameMap, i.getDefinition_id()));
        } else {
            FlowDefinitions def = i.getDefinition_id() == null ? null : definitionsRepository.selectById(i.getDefinition_id());
            row.put("flowName", def == null ? "" : def.getFlow_name());
        }
        // 被驳回或撤回的流程 7 天内可以重提，和 resubmit 接口一致
        row.put("resubmitAvailable", canResubmitWithin7Days(i));
        return row;
    }

    // 从批量查好的 Map 里取文本，取不到给空串
    private String mapText(Map<Long, String> map, Long key) {
        if (key == null) return "";
        String v = map.get(key);
        return v == null ? "" : v;
    }

    // 驳回或撤回后 7 天内都允许重提，列表、详情、重提接口共用
    private boolean canResubmitWithin7Days(FlowInstances inst) {
        if (inst == null || inst.getStatus() == null || inst.getFinish_time() == null) return false;
        if (inst.getStatus() != 3 && inst.getStatus() != 4) return false;
        return Duration.between(inst.getFinish_time(), LocalDateTime.now()).toDays() <= 7;
    }

    // 查不到用户时返回空串
    private String userName(Long userId) {
        if (userId == null) return "";
        Users u = usersRepository.selectById(userId);
        return u == null || u.getReal_name() == null ? "" : u.getReal_name();
    }

    // 流程状态的中文文案
    private String instanceStatusText(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 0 -> "草稿";
            case 1 -> "审批中";
            case 2 -> "已通过";
            case 3 -> "已驳回";
            case 4 -> "已撤回";
            case 5 -> "已取消";
            default -> "未知";
        };
    }

    // 审批动作的中文文案
    private String actionText(Integer a) {
        if (a == null) return "";
        return switch (a) {
            case 1 -> "通过";
            case 2 -> "驳回";
            case 3 -> "转交";
            case 4 -> "撤回";
            case 5 -> "催办";
            case 6 -> "加签";
            default -> "";
        };
    }

    // 取导出单元格的文本，null 当空串
    private String cellText(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? "" : v.toString();
    }

    // 导出流程列表为 Excel，筛选条件和列表接口一致
    @GetMapping("/instances/export")
    @RequirePermission({"flow:def:manage", "flow:all:view"})
    public void exportInstances(@RequestParam(required = false) Long definitionId,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String startTime,
                                @RequestParam(required = false) String endTime,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        Long userId = RequestUser.id(request);
        QueryWrapper<FlowInstances> qw = new QueryWrapper<>();
        if (definitionId != null) qw.eq("definition_id", definitionId);
        if (status != null) qw.eq("status", status);
        // 只有管理员和持全公司查看权限的人能导出别人的单子，其他人只看自己发起的
        boolean globalViewer = permissionService.isAdmin(userId)
                || permissionService.getUserPermissions(userId).contains("flow:all:view");
        if (!globalViewer) qw.eq("creator_id", userId);
        if (keyword != null && !keyword.trim().isEmpty()) qw.like("title", keyword.trim());
        if (startTime != null && !startTime.isEmpty()) qw.ge("submit_time", startTime + " 00:00:00");
        if (endTime != null && !endTime.isEmpty()) qw.le("submit_time", endTime + " 23:59:59");
        qw.orderByDesc("submit_time");
        List<FlowInstances> list = instancesRepository.selectList(qw);
        // 导出前把用户和流程定义批量查好，避免每行查一次
        List<Map<String, Object>> rows = assembleListBatch(list);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("流程列表");
            String[] headers = {"编号", "标题", "流程", "发起人", "当前节点", "当前审批人", "状态", "紧急程度", "提交时间", "完成时间"};
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> rowData = rows.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(cellText(rowData, "instanceNo"));
                row.createCell(1).setCellValue(cellText(rowData, "title"));
                row.createCell(2).setCellValue(cellText(rowData, "flowName"));
                row.createCell(3).setCellValue(cellText(rowData, "creatorName"));
                row.createCell(4).setCellValue(cellText(rowData, "currentStepName"));
                row.createCell(5).setCellValue(cellText(rowData, "auditorName"));
                row.createCell(6).setCellValue(cellText(rowData, "statusText"));
                Object urgency = rowData.get("urgency");
                row.createCell(7).setCellValue(
                        urgency instanceof Number && ((Number) urgency).intValue() == 1 ? "紧急" : "普通");
                row.createCell(8).setCellValue(cellText(rowData, "submitTime"));
                row.createCell(9).setCellValue(cellText(rowData, "finishTime"));
            }
            int[] colWidths = {16, 30, 16, 12, 14, 14, 10, 10, 20, 20};
            for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i] * 256);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=flow_instances.xlsx");
            ExcelSafe.sanitize(wb);
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }
}
