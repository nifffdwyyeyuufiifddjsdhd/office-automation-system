package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.utilt.ExcelSafe;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.ExpenseCategories;
import com.officeautomationsystemapi.entity.FlowDefinitions;
import com.officeautomationsystemapi.entity.FlowInstances;
import com.officeautomationsystemapi.entity.Organizations;
import com.officeautomationsystemapi.entity.PaymentRecords;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.ExpenseCategoriesRepository;
import com.officeautomationsystemapi.repository.FlowDefinitionsRepository;
import com.officeautomationsystemapi.repository.FlowInstancesRepository;
import com.officeautomationsystemapi.repository.OrganizationsRepository;
import com.officeautomationsystemapi.repository.PaymentRecordsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.ExpenseCategoryMatcher;
import com.officeautomationsystemapi.service.FlowFormAmountReader;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// 财务统计：按时间、部门、费用类别统计已付款，可导出 Excel
@RestController
@RequestMapping("/api/finance/stat")
public class FinanceStatController {

    @Autowired private PaymentRecordsRepository recordsRepository;
    @Autowired private FlowInstancesRepository instancesRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private OrganizationsRepository organizationsRepository;
    @Autowired private ExpenseCategoriesRepository categoriesRepository;
    @Autowired private FlowDefinitionsRepository definitionsRepository;
    @Autowired private ExpenseCategoryMatcher categoryMatcher;
    @Autowired private FlowFormAmountReader amountReader;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DAY_FILE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String[] PAY_METHODS = {"", "银行转账", "现金", "微信", "支付宝", "支票"};
    private static final String UNCATEGORIZED = "未分类";

    // 一条统计明细：付款记录、申请人、部门、费用类别
    private static class StatRow {
        PaymentRecords record;
        String applicantName;
        Long orgId;
        String orgName;
        String categoryName;
    }

    // 概览：总额、笔数、平均值、最大单笔、最高类别、环比
    @GetMapping("/overview")
    @RequirePermission("finance:stat")
    public Result<Map<String, Object>> overview(@RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate,
                                                @RequestParam(required = false) Long orgId,
                                                @RequestParam(required = false) String categoryName) {
        LocalDate[] range = resolveRange(startDate, endDate);
        List<StatRow> rows = buildRows(range, orgId, categoryName);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        Set<Long> orgIds = new HashSet<>();
        for (StatRow row : rows) {
            BigDecimal amount = amountOf(row.record);
            total = total.add(amount);
            if (amount.compareTo(max) > 0) max = amount;
            byCategory.merge(row.categoryName, amount, BigDecimal::add);
            if (row.orgId != null) orgIds.add(row.orgId);
        }
        int count = rows.size();
        BigDecimal avg = count == 0 ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        String topCategory = "";
        BigDecimal topAmount = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : byCategory.entrySet()) {
            if (entry.getValue().compareTo(topAmount) > 0) {
                topAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }

        // 环比取同样长度的上一区间，上期为 0 不算百分比
        long days = range[0].until(range[1]).getDays() + 1;
        LocalDate prevStart = range[0].minusDays(days);
        LocalDate prevEnd = range[0].minusDays(1);
        BigDecimal prevTotal = BigDecimal.ZERO;
        for (StatRow row : buildRows(new LocalDate[]{prevStart, prevEnd}, orgId, categoryName)) {
            prevTotal = prevTotal.add(amountOf(row.record));
        }
        BigDecimal growthRate = null;
        if (prevTotal.compareTo(BigDecimal.ZERO) > 0) {
            growthRate = total.subtract(prevTotal)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(prevTotal, 1, RoundingMode.HALF_UP);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalAmount", total.setScale(2, RoundingMode.HALF_UP));
        data.put("recordCount", count);
        data.put("avgAmount", avg);
        data.put("maxAmount", max.setScale(2, RoundingMode.HALF_UP));
        data.put("categoryCount", byCategory.size());
        data.put("orgCount", orgIds.size());
        data.put("topCategoryName", topCategory);
        data.put("topCategoryAmount", topAmount.setScale(2, RoundingMode.HALF_UP));
        data.put("prevAmount", prevTotal.setScale(2, RoundingMode.HALF_UP));
        data.put("growthRate", growthRate);
        // 审批通过但没付款的单据单列，报表还要看欠款
        // 待付款不看区间，跨月的欠款也要算
        List<FlowInstances> pending = pendingInstances();
        Map<Long, FlowDefinitions> pendingDefs = definitionsOf(pending);
        BigDecimal pendingAmount = BigDecimal.ZERO;
        int pendingUnknownCount = 0;
        for (FlowInstances instance : pending) {
            FlowDefinitions def = pendingDefs.get(instance.getDefinition_id());
            BigDecimal amount = amountReader.extract(instance.getBusiness_data(),
                    def == null ? null : def.getForm_config());
            if (amount != null) {
                pendingAmount = pendingAmount.add(amount);
            } else {
                pendingUnknownCount++;
            }
        }
        data.put("pendingCount", pending.size());
        data.put("pendingAmount", pendingAmount.setScale(2, RoundingMode.HALF_UP));
        // 金额识别不出来的按 0 算，单独报笔数，别让欠款偏低
        data.put("pendingUnknownCount", pendingUnknownCount);
        data.put("startDate", range[0].toString());
        data.put("endDate", range[1].toString());
        return Result.success(data);
    }

    // 按费用类别汇总，金额从高到低
    @GetMapping("/by-category")
    @RequirePermission("finance:stat")
    public Result<List<Map<String, Object>>> byCategory(@RequestParam(required = false) String startDate,
                                                        @RequestParam(required = false) String endDate,
                                                        @RequestParam(required = false) Long orgId) {
        LocalDate[] range = resolveRange(startDate, endDate);
        List<StatRow> rows = buildRows(range, orgId, null);
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> amountMap = new LinkedHashMap<>();
        Map<String, Integer> countMap = new LinkedHashMap<>();
        for (StatRow row : rows) {
            BigDecimal amount = amountOf(row.record);
            total = total.add(amount);
            amountMap.merge(row.categoryName, amount, BigDecimal::add);
            countMap.merge(row.categoryName, 1, Integer::sum);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : amountMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categoryName", entry.getKey());
            item.put("amount", entry.getValue().setScale(2, RoundingMode.HALF_UP));
            item.put("count", countMap.getOrDefault(entry.getKey(), 0));
            item.put("ratio", ratio(entry.getValue(), total));
            list.add(item);
        }
        list.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        return Result.success(list);
    }

    // 按部门汇总，取申请人所属部门
    @GetMapping("/by-department")
    @RequirePermission("finance:stat")
    public Result<List<Map<String, Object>>> byDepartment(@RequestParam(required = false) String startDate,
                                                          @RequestParam(required = false) String endDate,
                                                          @RequestParam(required = false) String categoryName) {
        LocalDate[] range = resolveRange(startDate, endDate);
        List<StatRow> rows = buildRows(range, null, categoryName);
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> amountMap = new LinkedHashMap<>();
        Map<String, Integer> countMap = new LinkedHashMap<>();
        for (StatRow row : rows) {
            BigDecimal amount = amountOf(row.record);
            total = total.add(amount);
            String orgName = row.orgName == null || row.orgName.isBlank() ? "未分配部门" : row.orgName;
            amountMap.merge(orgName, amount, BigDecimal::add);
            countMap.merge(orgName, 1, Integer::sum);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : amountMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orgName", entry.getKey());
            item.put("amount", entry.getValue().setScale(2, RoundingMode.HALF_UP));
            item.put("count", countMap.getOrDefault(entry.getKey(), 0));
            item.put("ratio", ratio(entry.getValue(), total));
            list.add(item);
        }
        list.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        return Result.success(list);
    }

    // 趋势：按月或按天出金额，空区间补 0，前端直接画线
    @GetMapping("/trend")
    @RequirePermission("finance:stat")
    public Result<Map<String, Object>> trend(@RequestParam(required = false) String startDate,
                                             @RequestParam(required = false) String endDate,
                                             @RequestParam(defaultValue = "month") String granularity,
                                             @RequestParam(required = false) Long orgId,
                                             @RequestParam(required = false) String categoryName) {
        LocalDate[] range = resolveRange(startDate, endDate);
        List<StatRow> rows = buildRows(range, orgId, categoryName);
        boolean byMonth = !"day".equalsIgnoreCase(granularity);
        // 按天统计时区间过长会按月汇总，减少数据点
        if (!byMonth && range[0].until(range[1]).getDays() > 92) {
            byMonth = true;
        }

        List<String> labels = new ArrayList<>();
        if (byMonth) {
            YearMonth cursor = YearMonth.from(range[0]);
            YearMonth last = YearMonth.from(range[1]);
            while (!cursor.isAfter(last)) {
                labels.add(cursor.format(MONTH_KEY));
                cursor = cursor.plusMonths(1);
            }
        } else {
            LocalDate cursor = range[0];
            while (!cursor.isAfter(range[1])) {
                labels.add(cursor.toString());
                cursor = cursor.plusDays(1);
            }
        }

        Map<String, BigDecimal> amountMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();
        for (StatRow row : rows) {
            LocalDateTime payTime = row.record.getPay_time();
            if (payTime == null) continue;
            String key = byMonth ? YearMonth.from(payTime).format(MONTH_KEY) : payTime.toLocalDate().toString();
            amountMap.merge(key, amountOf(row.record), BigDecimal::add);
            countMap.merge(key, 1, Integer::sum);
        }

        List<BigDecimal> amounts = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (String label : labels) {
            amounts.add(amountMap.getOrDefault(label, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
            counts.add(countMap.getOrDefault(label, 0));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("granularity", byMonth ? "month" : "day");
        data.put("labels", labels);
        data.put("amounts", amounts);
        data.put("counts", counts);
        return Result.success(data);
    }

    // 明细列表，前端表格用，筛选和汇总一致
    @GetMapping("/detail")
    @RequirePermission("finance:stat")
    public Result<Map<String, Object>> detail(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String startDate,
                                              @RequestParam(required = false) String endDate,
                                              @RequestParam(required = false) Long orgId,
                                              @RequestParam(required = false) String categoryName) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        LocalDate[] range = resolveRange(startDate, endDate);
        List<StatRow> rows = buildRows(range, orgId, categoryName);
        int total = rows.size();
        int from = Math.min((num - 1) * size, total);
        int to = Math.min(from + size, total);

        List<Map<String, Object>> records = new ArrayList<>();
        for (StatRow row : rows.subList(from, to)) {
            PaymentRecords r = row.record;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("payNo", r.getPay_no());
            item.put("instanceNo", r.getInstance_no());
            item.put("title", r.getTitle());
            item.put("flowName", r.getFlow_name());
            item.put("applicantName", row.applicantName);
            item.put("orgName", row.orgName);
            item.put("categoryName", row.categoryName);
            item.put("amount", amountOf(r));
            item.put("payMethodText", payMethodText(r.getPay_method()));
            item.put("payTime", r.getPay_time() == null ? "" : r.getPay_time().format(DATE_TIME));
            records.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("startDate", range[0].toString());
        data.put("endDate", range[1].toString());
        return Result.success(data);
    }

    // 导出报表：按类别、按部门、明细各一张
    @GetMapping("/export")
    @RequirePermission("finance:export")
    public void export(@RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       @RequestParam(required = false) Long orgId,
                       @RequestParam(required = false) String categoryName,
                       HttpServletResponse response) {
        LocalDate[] range = resolveRange(startDate, endDate);
        List<StatRow> rows = buildRows(range, orgId, categoryName);
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> catAmount = new LinkedHashMap<>();
        Map<String, Integer> catCount = new LinkedHashMap<>();
        Map<String, BigDecimal> orgAmount = new LinkedHashMap<>();
        Map<String, Integer> orgCount = new LinkedHashMap<>();
        for (StatRow row : rows) {
            BigDecimal amount = amountOf(row.record);
            total = total.add(amount);
            catAmount.merge(row.categoryName, amount, BigDecimal::add);
            catCount.merge(row.categoryName, 1, Integer::sum);
            String orgName = row.orgName == null || row.orgName.isBlank() ? "未分配部门" : row.orgName;
            orgAmount.merge(orgName, amount, BigDecimal::add);
            orgCount.merge(orgName, 1, Integer::sum);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Sheet catSheet = wb.createSheet("费用类别汇总");
            writeHeader(wb, catSheet, headerStyle, new String[]{"费用类别", "付款金额", "笔数", "占比(%)"});
            int catRow = 1;
            for (Map.Entry<String, BigDecimal> entry : catAmount.entrySet()) {
                Row row = catSheet.createRow(catRow++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString());
                row.createCell(2).setCellValue(catCount.getOrDefault(entry.getKey(), 0));
                row.createCell(3).setCellValue(ratio(entry.getValue(), total).toPlainString());
            }
            Row catTotal = catSheet.createRow(catRow);
            catTotal.createCell(0).setCellValue("合计");
            catTotal.createCell(1).setCellValue(total.setScale(2, RoundingMode.HALF_UP).toPlainString());
            catTotal.createCell(2).setCellValue(rows.size());
            setWidths(catSheet, new int[]{18, 16, 10, 12});

            Sheet orgSheet = wb.createSheet("部门汇总");
            writeHeader(wb, orgSheet, headerStyle, new String[]{"部门", "付款金额", "笔数", "占比(%)"});
            int orgRow = 1;
            for (Map.Entry<String, BigDecimal> entry : orgAmount.entrySet()) {
                Row row = orgSheet.createRow(orgRow++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString());
                row.createCell(2).setCellValue(orgCount.getOrDefault(entry.getKey(), 0));
                row.createCell(3).setCellValue(ratio(entry.getValue(), total).toPlainString());
            }
            setWidths(orgSheet, new int[]{24, 16, 10, 12});

            Sheet detailSheet = wb.createSheet("付款明细");
            writeHeader(wb, detailSheet, headerStyle, new String[]{"付款单号", "流程编号", "流程标题", "流程名称",
                    "申请人", "部门", "费用类别", "付款金额", "付款方式", "付款时间"});
            for (int i = 0; i < rows.size(); i++) {
                StatRow statRow = rows.get(i);
                PaymentRecords r = statRow.record;
                Row row = detailSheet.createRow(i + 1);
                row.createCell(0).setCellValue(nvl(r.getPay_no()));
                row.createCell(1).setCellValue(nvl(r.getInstance_no()));
                row.createCell(2).setCellValue(nvl(r.getTitle()));
                row.createCell(3).setCellValue(nvl(r.getFlow_name()));
                row.createCell(4).setCellValue(nvl(statRow.applicantName));
                row.createCell(5).setCellValue(nvl(statRow.orgName));
                row.createCell(6).setCellValue(nvl(statRow.categoryName));
                row.createCell(7).setCellValue(amountOf(r).toPlainString());
                row.createCell(8).setCellValue(payMethodText(r.getPay_method()));
                row.createCell(9).setCellValue(r.getPay_time() == null ? "" : r.getPay_time().format(DATE_TIME));
            }
            setWidths(detailSheet, new int[]{20, 22, 30, 18, 12, 18, 12, 14, 12, 20});

            // 第四个 sheet：审批通过还没付的单据，列清楚欠款
            List<FlowInstances> pendingList = pendingInstances();
            Map<Long, FlowDefinitions> pendingDefMap = definitionsOf(pendingList);
            Set<Long> pendingUserIds = pendingList.stream().map(FlowInstances::getCreator_id)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, Users> pendingUserMap = new HashMap<>();
            if (!pendingUserIds.isEmpty()) {
                for (Users user : usersRepository.selectBatchIds(pendingUserIds)) pendingUserMap.put(user.getId(), user);
            }
            Sheet pendingSheet = wb.createSheet("待付款单据");
            writeHeader(wb, pendingSheet, headerStyle, new String[]{"流程编号", "流程标题", "流程名称",
                    "申请人", "待付款金额", "审批完成时间"});
            BigDecimal pendingTotal = BigDecimal.ZERO;
            int pendingUnknown = 0;
            for (int i = 0; i < pendingList.size(); i++) {
                FlowInstances instance = pendingList.get(i);
                FlowDefinitions def = pendingDefMap.get(instance.getDefinition_id());
                Users applicant = instance.getCreator_id() == null ? null : pendingUserMap.get(instance.getCreator_id());
                BigDecimal amount = amountReader.extract(instance.getBusiness_data(),
                        def == null ? null : def.getForm_config());
                if (amount != null) {
                    pendingTotal = pendingTotal.add(amount);
                } else {
                    pendingUnknown++;
                }
                Row row = pendingSheet.createRow(i + 1);
                row.createCell(0).setCellValue(nvl(instance.getInstance_no()));
                row.createCell(1).setCellValue(nvl(instance.getTitle()));
                row.createCell(2).setCellValue(def == null ? "" : nvl(def.getFlow_name()));
                row.createCell(3).setCellValue(applicant == null ? "" : nvl(applicant.getReal_name()));
                row.createCell(4).setCellValue(amount == null ? "待补" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
                row.createCell(5).setCellValue(instance.getFinish_time() == null ? ""
                        : instance.getFinish_time().format(DATE_TIME));
            }
            Row pendingSum = pendingSheet.createRow(pendingList.size() + 1);
            pendingSum.createCell(0).setCellValue("合计");
            pendingSum.createCell(1).setCellValue("待付款 " + pendingList.size() + " 笔"
                    + (pendingUnknown > 0 ? "，其中 " + pendingUnknown + " 笔金额待补" : ""));
            pendingSum.createCell(4).setCellValue(pendingTotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
            setWidths(pendingSheet, new int[]{22, 34, 18, 12, 14, 20});

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=finance_stat_"
                    + LocalDate.now().format(DAY_FILE) + ".xlsx");
            ExcelSafe.sanitize(wb);
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    // 只算有效付款 status=1，按付款时间取区间，部门看申请人
    private List<StatRow> buildRows(LocalDate[] range, Long orgId, String categoryName) {
        QueryWrapper<PaymentRecords> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.ge("pay_time", range[0].atStartOfDay());
        wrapper.le("pay_time", range[1].atTime(23, 59, 59));
        wrapper.orderByDesc("pay_time").orderByDesc("id");
        List<PaymentRecords> records = recordsRepository.selectList(wrapper);

        List<ExpenseCategories> categories = categoryMatcher.enabled();
        Set<Long> instanceIds = records.stream().map(PaymentRecords::getInstance_id)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = records.stream().map(PaymentRecords::getApplicant_id)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, FlowInstances> instanceMap = new HashMap<>();
        if (!instanceIds.isEmpty()) {
            for (FlowInstances instance : instancesRepository.selectBatchIds(instanceIds)) {
                instanceMap.put(instance.getId(), instance);
            }
        }
        Map<Long, Users> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Users user : usersRepository.selectBatchIds(userIds)) userMap.put(user.getId(), user);
        }
        Set<Long> orgIds = userMap.values().stream().map(Users::getOrg_id)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Organizations> orgMap = new HashMap<>();
        if (!orgIds.isEmpty()) {
            for (Organizations org : organizationsRepository.selectBatchIds(orgIds)) orgMap.put(org.getId(), org);
        }

        List<StatRow> rows = new ArrayList<>();
        for (PaymentRecords record : records) {
            StatRow row = new StatRow();
            row.record = record;
            Users applicant = record.getApplicant_id() == null ? null : userMap.get(record.getApplicant_id());
            row.applicantName = applicant == null ? "" : nvl(applicant.getReal_name());
            row.orgId = applicant == null ? null : applicant.getOrg_id();
            Organizations org = row.orgId == null ? null : orgMap.get(row.orgId);
            row.orgName = org == null ? "" : nvl(org.getOrg_name());

            FlowInstances instance = record.getInstance_id() == null ? null : instanceMap.get(record.getInstance_id());
            StringBuilder text = new StringBuilder();
            text.append(nvl(record.getTitle())).append(" ")
                    .append(nvl(record.getFlow_name())).append(" ")
                    .append(instance == null ? "" : nvl(instance.getTitle())).append(" ")
                    .append(instance == null ? "" : nvl(instance.getBusiness_data()));
            // 单据确认过类别就用单据的，旧数据才按关键词猜
            if (record.getCategory_name() != null && !record.getCategory_name().isBlank()) {
                row.categoryName = record.getCategory_name();
            } else {
                ExpenseCategories guessed = categoryMatcher.guess(categories, text.toString());
                row.categoryName = guessed == null ? UNCATEGORIZED : categoryMatcher.nameOf(guessed);
            }

            if (orgId != null && !orgId.equals(row.orgId)) continue;
            if (categoryName != null && !categoryName.isBlank() && !categoryName.equals(row.categoryName)) continue;
            rows.add(row);
        }
        return rows;
    }

    // 审批通过还没付款的单据，和付款管理的待付款池一致
    // 不看日期，跨月的欠款也要报
    private List<FlowInstances> pendingInstances() {
        QueryWrapper<FlowInstances> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 2);
        wrapper.inSql("definition_id", "SELECT id FROM flow_definitions WHERE need_payment = 1 AND deleted_at IS NULL");
        wrapper.notInSql("id", "SELECT instance_id FROM payment_records WHERE status = 1 AND deleted_at IS NULL");
        wrapper.orderByDesc("finish_time").orderByDesc("id");
        return instancesRepository.selectList(wrapper);
    }

    // 一次取出这些单据的流程定义，金额字段配置在里面
    private Map<Long, FlowDefinitions> definitionsOf(List<FlowInstances> instances) {
        Set<Long> ids = instances.stream().map(FlowInstances::getDefinition_id)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, FlowDefinitions> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        for (FlowDefinitions def : definitionsRepository.selectBatchIds(ids)) map.put(def.getId(), def);
        return map;
    }

    // 都不传取本月，只传结束日期从当月 1 号起
    private LocalDate[] resolveRange(String startDate, String endDate) {
        LocalDate end = parseDate(endDate);
        LocalDate start = parseDate(startDate);
        if (end == null) end = LocalDate.now();
        if (start == null) start = end.withDayOfMonth(1);
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        return new LocalDate[]{start, end};
    }

    private LocalDate parseDate(String text) {
        if (text == null) return null;
        String value = text.trim();
        if (value.isEmpty()) return null;
        try {
            return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value, DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal amountOf(PaymentRecords record) {
        return record.getAmount() == null ? BigDecimal.ZERO : record.getAmount();
    }

    private BigDecimal ratio(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }

    private void writeHeader(Workbook wb, Sheet sheet, CellStyle style, String[] headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void setWidths(Sheet sheet, int[] widths) {
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
    }

    private String payMethodText(Integer method) {
        if (method == null || method < 1 || method > PAY_METHODS.length - 1) return "";
        return PAY_METHODS[method];
    }

    private String nvl(String text) {
        return text == null ? "" : text;
    }
}
