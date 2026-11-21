package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.officeautomationsystemapi.utilt.ExcelSafe;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.ExpenseCategories;
import com.officeautomationsystemapi.entity.FlowDefinitions;
import com.officeautomationsystemapi.entity.FlowInstances;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.entity.Organizations;
import com.officeautomationsystemapi.entity.PaymentRecords;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.FlowDefinitionsRepository;
import com.officeautomationsystemapi.repository.FlowInstancesRepository;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import com.officeautomationsystemapi.repository.OrganizationsRepository;
import com.officeautomationsystemapi.repository.PaymentRecordsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.ExpenseCategoryMatcher;
import com.officeautomationsystemapi.service.FlowFormAmountReader;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 付款管理：财务对审批通过的报销、付款、采购流程动款留痕
@RestController
@RequestMapping("/api/finance/payments")
public class PaymentController {

    @Autowired private PaymentRecordsRepository recordsRepository;
    @Autowired private FlowInstancesRepository instancesRepository;
    @Autowired private FlowDefinitionsRepository definitionsRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private OrganizationsRepository organizationsRepository;
    @Autowired private NotificationRecordsRepository notificationRecordsRepository;
    @Autowired private ExpenseCategoryMatcher categoryMatcher;
    @Autowired private FlowFormAmountReader amountReader;

    private static final ObjectMapper OM = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    // 付款方式字典，下标就是数据库里存的编号
    private static final String[] PAY_METHODS = {"", "银行转账", "现金", "微信", "支付宝", "支票"};
    // 待付款池的筛选条件，列表和导出共用
    // 审批通过、模板要求付款、且没有有效付款记录
    private QueryWrapper<FlowInstances> pendingWrapper(String keyword) {
        QueryWrapper<FlowInstances> qw = new QueryWrapper<>();
        qw.eq("status", 2);
        qw.inSql("definition_id", "SELECT id FROM flow_definitions WHERE need_payment = 1 AND deleted_at IS NULL");
        qw.notInSql("id", "SELECT instance_id FROM payment_records WHERE status = 1 AND deleted_at IS NULL");
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            qw.and(w -> w.like("title", k).or().like("instance_no", k));
        }
        qw.orderByDesc("finish_time").orderByDesc("id");
        return qw;
    }

    // 付款记录的筛选条件，列表和导出共用
    private QueryWrapper<PaymentRecords> recordWrapper(String keyword, Integer payMethod, Integer status,
                                                       String startTime, String endTime) {
        QueryWrapper<PaymentRecords> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            qw.and(w -> w.like("title", k).or().like("payee_name", k)
                    .or().like("pay_no", k).or().like("instance_no", k));
        }
        if (payMethod != null) qw.eq("pay_method", payMethod);
        if (status != null) qw.eq("status", status);
        if (startTime != null && !startTime.isEmpty()) qw.ge("pay_time", startTime + " 00:00:00");
        if (endTime != null && !endTime.isEmpty()) qw.le("pay_time", endTime + " 23:59:59");
        qw.orderByDesc("pay_time").orderByDesc("id");
        return qw;
    }

    // 待付款列表，带出流程名称、申请人、部门和大致的金额
    @GetMapping("/pending")
    @RequirePermission("finance:pay")
    public Result<Map<String, Object>> pending(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String keyword) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<FlowInstances> page = instancesRepository.selectPage(new Page<>(num, size), pendingWrapper(keyword));

        // 流程定义和申请人一次查出，避免逐行回表
        Set<Long> defIds = page.getRecords().stream().map(FlowInstances::getDefinition_id)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> creatorIds = page.getRecords().stream().map(FlowInstances::getCreator_id)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, FlowDefinitions> defMap = new HashMap<>();
        if (!defIds.isEmpty()) {
            for (FlowDefinitions d : definitionsRepository.selectBatchIds(defIds)) defMap.put(d.getId(), d);
        }
        Map<Long, Users> userMap = new HashMap<>();
        if (!creatorIds.isEmpty()) {
            for (Users u : usersRepository.selectBatchIds(creatorIds)) userMap.put(u.getId(), u);
        }
        Map<Long, String> orgNameMap = orgNames(userMap.values());
        // 类别只查一次，逐行按关键词给默认值
        List<ExpenseCategories> categoryList = categoryMatcher.enabled();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (FlowInstances inst : page.getRecords()) {
            FlowDefinitions def = defMap.get(inst.getDefinition_id());
            Users creator = userMap.get(inst.getCreator_id());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("instanceId", inst.getId());
            row.put("instanceNo", inst.getInstance_no());
            row.put("title", inst.getTitle());
            row.put("flowName", def == null ? "" : def.getFlow_name());
            row.put("creatorName", displayName(creator));
            row.put("orgName", creator == null ? "" : orgNameMap.getOrDefault(creator.getOrg_id(), ""));
            row.put("amount", amountReader.extract(inst.getBusiness_data(),
                    def == null ? null : def.getForm_config()));
            row.put("reason", extractReason(inst.getBusiness_data()));
            // 默认类别跟着单据走，付款弹窗里财务可以改
            ExpenseCategories suggest = categoryMatcher.guess(categoryList,
                    nvl(inst.getTitle()) + " " + (def == null ? "" : nvl(def.getFlow_name()))
                            + " " + nvl(inst.getBusiness_data()));
            row.put("suggestCategoryId", suggest == null ? null : suggest.getId());
            row.put("suggestCategoryName", categoryMatcher.nameOf(suggest));
            row.put("attachmentId", inst.getAttachment_id());
            row.put("submitTime", format(inst.getSubmit_time()));
            row.put("finishTime", format(inst.getFinish_time()));
            rows.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", rows);
        data.put("total", page.getTotal());
        return Result.success(data);
    }

    // 已付款记录，可按关键词、付款方式、状态、付款时间筛选
    @GetMapping("/records")
    @RequirePermission("finance:pay")
    public Result<Map<String, Object>> records(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer payMethod,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String startTime,
                                               @RequestParam(required = false) String endTime) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<PaymentRecords> page = recordsRepository.selectPage(new Page<>(num, size),
                recordWrapper(keyword, payMethod, status, startTime, endTime));

        Set<Long> userIds = new HashSet<>();
        for (PaymentRecords r : page.getRecords()) {
            if (r.getApplicant_id() != null) userIds.add(r.getApplicant_id());
            if (r.getOperator_id() != null) userIds.add(r.getOperator_id());
        }
        Map<Long, Users> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Users u : usersRepository.selectBatchIds(userIds)) userMap.put(u.getId(), u);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (PaymentRecords r : page.getRecords()) {
            rows.add(recordRow(r, userMap));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", rows);
        data.put("total", page.getTotal());
        return Result.success(data);
    }

    // 页面四张卡片：待付款笔数和金额、本月已付款笔数和金额
    @GetMapping("/summary")
    @RequirePermission("finance:pay")
    public Result<Map<String, Object>> summary() {
        List<FlowInstances> pending = instancesRepository.selectList(pendingWrapper(null));
        // 金额按表单配置取，动态字段名 field_xxx 也能取对
        Map<Long, FlowDefinitions> defMap = new HashMap<>();
        Set<Long> defIds = pending.stream().map(FlowInstances::getDefinition_id)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!defIds.isEmpty()) {
            for (FlowDefinitions def : definitionsRepository.selectBatchIds(defIds)) defMap.put(def.getId(), def);
        }
        BigDecimal pendingAmount = BigDecimal.ZERO;
        for (FlowInstances inst : pending) {
            FlowDefinitions def = defMap.get(inst.getDefinition_id());
            BigDecimal amount = amountReader.extract(inst.getBusiness_data(),
                    def == null ? null : def.getForm_config());
            if (amount != null) pendingAmount = pendingAmount.add(amount);
        }

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        QueryWrapper<PaymentRecords> monthWrapper = new QueryWrapper<>();
        monthWrapper.eq("status", 1).ge("pay_time", monthStart);
        List<PaymentRecords> monthPaid = recordsRepository.selectList(monthWrapper);
        BigDecimal monthAmount = monthPaid.stream().map(PaymentRecords::getAmount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingCount", pending.size());
        data.put("pendingAmount", pendingAmount.setScale(2, RoundingMode.HALF_UP));
        data.put("monthCount", monthPaid.size());
        data.put("monthAmount", monthAmount.setScale(2, RoundingMode.HALF_UP));
        return Result.success(data);
    }

    // 执行付款：金额、收款人、方式、时间落库，并通知申请人
    @PostMapping("/pay")
    @RequirePermission("finance:pay")
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> pay(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Long instanceId = toLong(body.get("instanceId"));
        if (instanceId == null) return Result.error("缺少要付款的流程");

        FlowInstances inst = instancesRepository.selectById(instanceId);
        if (inst == null) return Result.error("流程不存在");
        // 只有审批走完 status=2 才能付款，驳回和审批中的都不能付
        if (inst.getStatus() == null || inst.getStatus() != 2) return Result.error("只有审批通过的流程才能付款");

        FlowDefinitions def = inst.getDefinition_id() == null
                ? null : definitionsRepository.selectById(inst.getDefinition_id());
        if (def == null || def.getNeed_payment() == null || def.getNeed_payment() != 1) {
            return Result.error("该流程模板没有标记需要财务付款");
        }
        Long paid = recordsRepository.selectCount(new QueryWrapper<PaymentRecords>()
                .eq("instance_id", instanceId).eq("status", 1));
        if (paid != null && paid > 0) return Result.error("该流程已经付过款，不能重复付款");

        // 前端没传金额时按表单配置取，避免财务手抄
        BigDecimal amount = toDecimal(body.get("amount"));
        if (amount == null) amount = amountReader.extract(inst.getBusiness_data(), def.getForm_config());
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return Result.error("付款金额必须大于 0");
        if (amount.compareTo(new BigDecimal("99999999.99")) > 0) return Result.error("付款金额超出上限");
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        String payeeName = text(body.get("payeeName"));
        if (payeeName == null) return Result.error("请填写收款人");
        if (payeeName.length() > 64) return Result.error("收款人不能超过 64 个字");
        String payeeAccount = text(body.get("payeeAccount"));
        if (payeeAccount != null && payeeAccount.length() > 128) return Result.error("收款账号不能超过 128 个字");

        Integer payMethod = toInt(body.get("payMethod"));
        if (payMethod == null || payMethod < 1 || payMethod > PAY_METHODS.length - 1) {
            return Result.error("请选择付款方式");
        }

        LocalDateTime payTime = parseTime(text(body.get("payTime")));
        if (payTime == null) payTime = LocalDateTime.now();
        // 允许补录今天的付款，不接受明显未来的时间
        if (payTime.isAfter(LocalDateTime.now().plusDays(1))) return Result.error("付款时间不能晚于明天");

        String remark = text(body.get("remark"));
        if (remark != null && remark.length() > 500) return Result.error("备注不能超过 500 个字");

        // 类别以财务确认的为准，没传按关键词给默认值
        Long categoryId = toLong(body.get("categoryId"));
        ExpenseCategories category = categoryMatcher.byId(categoryId);
        if (categoryId != null && category == null) return Result.error("费用类别不存在或已停用");
        if (category == null) {
            category = categoryMatcher.guess(categoryMatcher.enabled(),
                    nvl(inst.getTitle()) + " " + nvl(def.getFlow_name()) + " " + nvl(inst.getBusiness_data()));
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentRecords rec = new PaymentRecords();
        rec.setInstance_id(instanceId);
        rec.setInstance_no(inst.getInstance_no());
        rec.setDefinition_id(def.getId());
        rec.setFlow_name(def.getFlow_name());
        rec.setTitle(inst.getTitle());
        rec.setApplicant_id(inst.getCreator_id());
        rec.setCategory_id(category == null ? null : category.getId());
        rec.setCategory_name(category == null ? null : category.getCategory_name());
        rec.setAmount(amount);
        rec.setPayee_name(payeeName);
        rec.setPayee_account(payeeAccount);
        rec.setPay_method(payMethod);
        rec.setPay_time(payTime);
        rec.setStatus(1);
        rec.setOperator_id(userId);
        rec.setRemark(remark);
        rec.setCreated_at(now);
        rec.setUpdated_at(now);
        insertWithPayNo(rec, now);

        notify(inst.getCreator_id(), "付款已完成：" + inst.getTitle(),
                "付款单号 " + rec.getPay_no() + "，金额 ¥" + amount.toPlainString()
                        + "，方式 " + payMethodText(payMethod) + "，付款时间 " + payTime.format(DATE_TIME),
                "/home/flow-detail?id=" + instanceId);
        return Result.success(rec.getId());
    }

    // 作废付款：记录保留并标为作废，流程回到待付款列表
    @PostMapping("/{id}/void")
    @RequirePermission("finance:pay:void")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> voidPay(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        PaymentRecords rec = recordsRepository.selectById(id);
        if (rec == null) return Result.error("付款记录不存在");
        if (rec.getStatus() == null || rec.getStatus() != 1) return Result.error("只有已付款的记录可以作废");

        String reason = text(body.get("reason"));
        if (reason == null) return Result.error("请填写作废原因");
        if (reason.length() > 255) return Result.error("作废原因不能超过 255 个字");

        rec.setStatus(2);
        rec.setVoid_reason(reason);
        rec.setVoid_by(userId);
        rec.setVoid_time(LocalDateTime.now());
        rec.setUpdated_at(LocalDateTime.now());
        recordsRepository.updateById(rec);

        notify(rec.getApplicant_id(), "付款已作废：" + rec.getTitle(),
                "付款单号 " + rec.getPay_no() + " 已作废，原因：" + reason + "，财务会重新处理这笔付款",
                "/home/flow-detail?id=" + rec.getInstance_id());
        return Result.success(null);
    }

    // 导出付款记录，筛选条件跟列表一致
    @GetMapping("/export")
    @RequirePermission("finance:export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer payMethod,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       HttpServletResponse response) {
        List<PaymentRecords> list = recordsRepository.selectList(
                recordWrapper(keyword, payMethod, status, startTime, endTime));

        Set<Long> userIds = new HashSet<>();
        for (PaymentRecords r : list) {
            if (r.getApplicant_id() != null) userIds.add(r.getApplicant_id());
            if (r.getOperator_id() != null) userIds.add(r.getOperator_id());
        }
        Map<Long, Users> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Users u : usersRepository.selectBatchIds(userIds)) userMap.put(u.getId(), u);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("付款记录");
            String[] headers = {"付款单号", "流程编号", "流程标题", "流程名称", "申请人", "费用类别", "收款人", "收款账号",
                    "付款金额", "付款方式", "付款时间", "付款人", "状态", "备注", "作废原因"};
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
            for (int i = 0; i < list.size(); i++) {
                PaymentRecords r = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(nvl(r.getPay_no()));
                row.createCell(1).setCellValue(nvl(r.getInstance_no()));
                row.createCell(2).setCellValue(nvl(r.getTitle()));
                row.createCell(3).setCellValue(nvl(r.getFlow_name()));
                row.createCell(4).setCellValue(displayName(userMap.get(r.getApplicant_id())));
                row.createCell(5).setCellValue(nvl(r.getCategory_name()));
                row.createCell(6).setCellValue(nvl(r.getPayee_name()));
                row.createCell(7).setCellValue(nvl(r.getPayee_account()));
                row.createCell(8).setCellValue(r.getAmount() == null ? "" : r.getAmount().toPlainString());
                row.createCell(9).setCellValue(payMethodText(r.getPay_method()));
                row.createCell(10).setCellValue(format(r.getPay_time()));
                row.createCell(11).setCellValue(displayName(userMap.get(r.getOperator_id())));
                row.createCell(12).setCellValue(r.getStatus() != null && r.getStatus() == 2 ? "已作废" : "已付款");
                row.createCell(13).setCellValue(nvl(r.getRemark()));
                row.createCell(14).setCellValue(nvl(r.getVoid_reason()));
            }
            int[] colWidths = {20, 22, 30, 16, 12, 12, 12, 24, 12, 12, 20, 12, 10, 24, 20};
            for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i] * 256);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=payment_records.xlsx");
            ExcelSafe.sanitize(wb);
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    // 付款记录转成前端要的字段
    private Map<String, Object> recordRow(PaymentRecords r, Map<Long, Users> userMap) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", r.getId());
        row.put("payNo", r.getPay_no());
        row.put("instanceId", r.getInstance_id());
        row.put("instanceNo", r.getInstance_no());
        row.put("title", r.getTitle());
        row.put("flowName", r.getFlow_name());
        row.put("applicantName", displayName(userMap.get(r.getApplicant_id())));
        row.put("categoryId", r.getCategory_id());
        row.put("categoryName", nvl(r.getCategory_name()));
        row.put("payeeName", r.getPayee_name());
        row.put("payeeAccount", r.getPayee_account());
        row.put("amount", r.getAmount());
        row.put("payMethod", r.getPay_method());
        row.put("payMethodText", payMethodText(r.getPay_method()));
        row.put("payTime", format(r.getPay_time()));
        row.put("status", r.getStatus());
        row.put("statusText", r.getStatus() != null && r.getStatus() == 2 ? "已作废" : "已付款");
        row.put("operatorName", displayName(userMap.get(r.getOperator_id())));
        row.put("remark", r.getRemark());
        row.put("voidReason", r.getVoid_reason());
        row.put("voidTime", format(r.getVoid_time()));
        return row;
    }

    // 部门名称，按用户身上的 org_id 批量查
    private Map<Long, String> orgNames(Collection<Users> users) {
        Set<Long> orgIds = users.stream().map(Users::getOrg_id).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> map = new HashMap<>();
        if (orgIds.isEmpty()) return map;
        for (Organizations org : organizationsRepository.selectBatchIds(orgIds)) {
            map.put(org.getId(), org.getOrg_name());
        }
        return map;
    }

    // 显示名优先用真实姓名，没有就用账号
    private String displayName(Users user) {
        if (user == null) return "";
        if (user.getReal_name() != null && !user.getReal_name().trim().isEmpty()) return user.getReal_name();
        return user.getUsername() == null ? "" : user.getUsername();
    }

    // 付款单号按天自增，撞号重新取号再插
    private void insertWithPayNo(PaymentRecords rec, LocalDateTime now) {
        String prefix = "PAY-" + now.format(DAY) + "-";
        for (int i = 0; i < 5; i++) {
            Long count = recordsRepository.selectCount(new QueryWrapper<PaymentRecords>().likeRight("pay_no", prefix));
            rec.setPay_no(prefix + String.format("%04d", (count == null ? 0 : count) + 1 + i));
            try {
                recordsRepository.insert(rec);
                return;
            } catch (DuplicateKeyException e) {
                // 单号被抢先用了，换下一个号再来
                if (i == 4) throw e;
            }
        }
    }

    // 通知发不出去不影响付款本身
    private void notify(Long userId, String title, String content, String jumpUrl) {
        if (userId == null) return;
        try {
            NotificationRecords n = new NotificationRecords();
            n.setUser_id(userId);
            n.setTitle(title);
            n.setContent(content);
            n.setType(2);
            n.setBiz_type("payment");
            n.setJump_url(jumpUrl);
            n.setIs_read(0);
            n.setPush_status(0);
            n.setCreated_at(LocalDateTime.now());
            n.setUpdated_at(LocalDateTime.now());
            notificationRecordsRepository.insert(n);
        } catch (Exception ignore) {
            // 通知失败只丢这一条
        }
    }

    // 列表里给财务看的一句话事由，取表单里最长的一段文字
    private String extractReason(String businessData) {
        Map<String, Object> data = parseBusiness(businessData);
        String best = "";
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof String text)) continue;
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            boolean reasonKey = key.contains("reason") || key.contains("desc") || key.contains("remark")
                    || entry.getKey().contains("事由") || entry.getKey().contains("说明") || entry.getKey().contains("用途");
            if (reasonKey) return cut(text, 60);
            if (text.length() > best.length()) best = text;
        }
        return cut(best, 60);
    }

    private Map<String, Object> parseBusiness(String businessData) {
        if (businessData == null || businessData.trim().isEmpty()) return new HashMap<>();
        try {
            Map<String, Object> map = OM.readValue(businessData, new TypeReference<Map<String, Object>>() {});
            return map == null ? new HashMap<>() : map;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String payMethodText(Integer method) {
        if (method == null || method < 1 || method > PAY_METHODS.length - 1) return "";
        return PAY_METHODS[method];
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME);
    }

    private String cut(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private String nvl(String text) {
        return text == null ? "" : text;
    }

    private String text(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof Number number) return new BigDecimal(number.toString());
            String text = value.toString().trim().replace(",", "").replace("¥", "").replace("￥", "");
            if (text.isEmpty()) return null;
            return new BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseTime(String text) {
        if (text == null) return null;
        try {
            return LocalDateTime.parse(text.length() > 19 ? text.substring(0, 19) : text, DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}
