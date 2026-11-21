package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.RecruitmentCandidates;
import com.officeautomationsystemapi.entity.RecruitmentPositions;
import com.officeautomationsystemapi.repository.RecruitmentCandidatesRepository;
import com.officeautomationsystemapi.repository.RecruitmentPositionsRepository;
import com.officeautomationsystemapi.service.DataScopeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 招聘管理：人事发布岗位、维护候选人、推动面试，入职后走入职申请流程
@RestController
@RequestMapping("/api/recruitment")
public class RecruitmentController {

    @Autowired
    private RecruitmentPositionsRepository positionsRepository;

    @Autowired
    private RecruitmentCandidatesRepository candidatesRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataScopeService dataScopeService;

    // 岗位状态：0 已停招 1 在招 2 已结束
    private static final int POSITION_STOPPED = 0;
    private static final int POSITION_OPEN = 1;
    private static final int POSITION_FINISHED = 2;

    // 候选人状态：1 待筛选 2 待面试 3 面试通过 4 已入职 5 已淘汰
    private static final int CANDIDATE_SCREENING = 1;
    private static final int CANDIDATE_INTERVIEW = 2;
    private static final int CANDIDATE_PASSED = 3;
    private static final int CANDIDATE_HIRED = 4;
    private static final int CANDIDATE_REJECTED = 5;

    // 岗位列表：按部门、状态、名称筛选，带上候选人数量
    @GetMapping("/positions")
    @RequirePermission("recruitment:position:list")
    public Result<Map<String, Object>> positionList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId,
            HttpServletRequest request) {
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        // 人事主管只看本部门及下级的岗位，全公司范围的角色不受限
        List<Long> depts = dataScopeService.visibleOrgIds(RequestUser.id(request));
        if (depts != null && depts.isEmpty()) {
            return Result.success(emptyPage(page, size));
        }
        QueryWrapper<RecruitmentPositions> wrapper = new QueryWrapper<>();
        if (depts != null) {
            wrapper.in("dept_id", depts);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("position_name", kw).or().like("work_place", kw));
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (deptId != null) {
            wrapper.eq("dept_id", deptId);
        }
        wrapper.orderByDesc("status").orderByDesc("id");
        Page<RecruitmentPositions> result = positionsRepository.selectPage(new Page<>(page, size), wrapper);

        // 岗位的候选人数量一次取出，避免逐行查
        Map<Long, int[]> counters = candidateCounters();
        List<Map<String, Object>> records = new ArrayList<>();
        for (RecruitmentPositions p : result.getRecords()) {
            Map<String, Object> row = toPositionRow(p);
            int[] counter = counters.get(p.getId());
            row.put("candidateCount", counter == null ? 0 : counter[0]);
            row.put("hiredCount", counter == null ? 0 : counter[1]);
            records.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("pageNum", page);
        data.put("pageSize", size);
        return Result.success(data);
    }

    // 岗位下拉：在招的，或已停招但还有候选人的
    @GetMapping("/positions/options")
    @RequirePermission({"recruitment:position:list", "recruitment:candidate:list"})
    public Result<List<Map<String, Object>>> positionOptions(HttpServletRequest request) {
        QueryWrapper<RecruitmentPositions> wrapper = new QueryWrapper<>();
        wrapper.ne("status", POSITION_FINISHED).orderByDesc("status").orderByDesc("id");
        List<Long> depts = dataScopeService.visibleOrgIds(RequestUser.id(request));
        if (depts != null) {
            if (depts.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
            wrapper.in("dept_id", depts);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RecruitmentPositions p : positionsRepository.selectList(wrapper)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("positionName", p.getPosition_name());
            row.put("deptName", orgName(p.getDept_id()));
            row.put("status", p.getStatus());
            rows.add(row);
        }
        return Result.success(rows);
    }

    // 发布岗位：名称和招聘人数必填，状态默认在招，记发布时间
    @PostMapping("/positions")
    @RequirePermission("recruitment:position:add")
    public Result<Long> createPosition(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String name = text(body, "positionName", "position_name");
        if (name == null) {
            return Result.error("招聘岗位名称不能为空");
        }
        RecruitmentPositions entity = new RecruitmentPositions();
        entity.setPosition_name(name);
        Long deptIdValue = toLong(first(body, "deptId", "dept_id"));
        if (deptIdValue == null) {
            // 不指定部门时只有全公司范围的能发，其余要选自己范围内的部门
            if (!dataScopeService.canSeeAll(RequestUser.id(request))) {
                return Result.error("请选择用人部门");
            }
        } else if (!dataScopeService.canOperateOrg(RequestUser.id(request), deptIdValue)) {
            return Result.error(403, "无权在该部门下发布岗位：不在您的数据范围内");
        }
        entity.setDept_id(deptIdValue);
        entity.setPosition_id(toLong(first(body, "positionId", "position_id")));
        Integer headcount = toInt(first(body, "headcount", "headcount"));
        if (headcount != null && headcount < 1) {
            return Result.error("招聘人数至少为 1");
        }
        entity.setHeadcount(headcount == null ? 1 : headcount);
        entity.setEducation(text(body, "education", "education"));
        entity.setExperience_years(toInt(first(body, "experienceYears", "experience_years")));
        entity.setSalary_range(text(body, "salaryRange", "salary_range"));
        entity.setWork_place(text(body, "workPlace", "work_place"));
        entity.setDuty(text(body, "duty", "duty"));
        entity.setRequirement(text(body, "requirement", "requirement"));
        Integer status = toInt(first(body, "status", "status"));
        entity.setStatus(status == null ? POSITION_OPEN : status);
        entity.setPublish_time(entity.getStatus() == POSITION_OPEN ? LocalDateTime.now() : null);
        entity.setDeadline(toDate(first(body, "deadline", "deadline")));
        entity.setCreator_id(RequestUser.id(request));
        entity.setRemark(text(body, "remark", "remark"));
        entity.setCreated_at(LocalDateTime.now());
        entity.setUpdated_at(LocalDateTime.now());
        positionsRepository.insert(entity);
        return Result.success(entity.getId());
    }

    // 没传的字段保持原值，停招改在招时补发布时间
    @PutMapping("/positions/{id}")
    @RequirePermission("recruitment:position:edit")
    public Result<Void> updatePosition(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        RecruitmentPositions db = positionsRepository.selectById(id);
        if (db == null) {
            return Result.error("岗位不存在或已删除");
        }
        if (!dataScopeService.canOperateOrg(RequestUser.id(request), db.getDept_id())) {
            return Result.error(403, "无权修改该岗位：不在您的数据范围内");
        }
        String name = text(body, "positionName", "position_name");
        if (name != null) {
            db.setPosition_name(name);
        }
        Long deptId = toLong(first(body, "deptId", "dept_id"));
        if (deptId != null) {
            db.setDept_id(deptId);
        }
        Long positionId = toLong(first(body, "positionId", "position_id"));
        if (positionId != null) {
            db.setPosition_id(positionId);
        }
        Integer headcount = toInt(first(body, "headcount", "headcount"));
        if (headcount != null) {
            if (headcount < 1) {
                return Result.error("招聘人数至少为 1");
            }
            db.setHeadcount(headcount);
        }
        String education = text(body, "education", "education");
        if (education != null) {
            db.setEducation(education);
        }
        Integer experienceYears = toInt(first(body, "experienceYears", "experience_years"));
        if (experienceYears != null) {
            db.setExperience_years(experienceYears);
        }
        String salaryRange = text(body, "salaryRange", "salary_range");
        if (salaryRange != null) {
            db.setSalary_range(salaryRange);
        }
        String workPlace = text(body, "workPlace", "work_place");
        if (workPlace != null) {
            db.setWork_place(workPlace);
        }
        String duty = text(body, "duty", "duty");
        if (duty != null) {
            db.setDuty(duty);
        }
        String requirement = text(body, "requirement", "requirement");
        if (requirement != null) {
            db.setRequirement(requirement);
        }
        LocalDate deadline = toDate(first(body, "deadline", "deadline"));
        if (deadline != null) {
            db.setDeadline(deadline);
        }
        String remark = text(body, "remark", "remark");
        if (remark != null) {
            db.setRemark(remark);
        }
        Integer status = toInt(first(body, "status", "status"));
        if (status != null) {
            if (status == POSITION_OPEN && db.getPublish_time() == null) {
                db.setPublish_time(LocalDateTime.now());
            }
            db.setStatus(status);
        }
        db.setUpdated_at(LocalDateTime.now());
        positionsRepository.updateById(db);
        return Result.success(null);
    }

    // 只改岗位状态：停招、在招、结束，页面开关走这里
    @PutMapping("/positions/{id}/status")
    @RequirePermission("recruitment:position:edit")
    public Result<Void> updatePositionStatus(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        RecruitmentPositions db = positionsRepository.selectById(id);
        if (db == null) {
            return Result.error("岗位不存在或已删除");
        }
        if (!dataScopeService.canOperateOrg(RequestUser.id(request), db.getDept_id())) {
            return Result.error(403, "无权修改该岗位：不在您的数据范围内");
        }
        Integer status = toInt(first(body, "status", "status"));
        if (status == null || (status != POSITION_STOPPED && status != POSITION_OPEN && status != POSITION_FINISHED)) {
            return Result.error("岗位状态只能是 0已停招、1在招、2已结束");
        }
        if (status == POSITION_OPEN && db.getPublish_time() == null) {
            db.setPublish_time(LocalDateTime.now());
        }
        db.setStatus(status);
        db.setUpdated_at(LocalDateTime.now());
        positionsRepository.updateById(db);
        return Result.success(null);
    }

    // 还有候选人挂着就不能删岗位
    @DeleteMapping("/positions/{id}")
    @RequirePermission("recruitment:position:delete")
    public Result<Void> deletePosition(@PathVariable Long id, HttpServletRequest request) {
        RecruitmentPositions target = positionsRepository.selectById(id);
        if (target == null) {
            return Result.error("岗位不存在或已删除");
        }
        if (!dataScopeService.canOperateOrg(RequestUser.id(request), target.getDept_id())) {
            return Result.error(403, "无权删除该岗位：不在您的数据范围内");
        }
        QueryWrapper<RecruitmentCandidates> wrapper = new QueryWrapper<>();
        wrapper.eq("position_id", id);
        long count = candidatesRepository.selectCount(wrapper);
        if (count > 0) {
            return Result.error("该岗位还有 " + count + " 位候选人，请先处理或删除候选人");
        }
        positionsRepository.deleteById(id);
        return Result.success(null);
    }

    // 候选人列表：按岗位、状态、姓名或手机号筛选
    @GetMapping("/candidates")
    @RequirePermission("recruitment:candidate:list")
    public Result<Map<String, Object>> candidateList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long positionId,
            HttpServletRequest request) {
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        // 候选人跟着岗位走，只看自己范围内的
        List<Long> positionIds = visiblePositionIds(RequestUser.id(request));
        if (positionIds != null && positionIds.isEmpty()) {
            return Result.success(emptyPage(page, size));
        }
        QueryWrapper<RecruitmentCandidates> wrapper = new QueryWrapper<>();
        if (positionIds != null) {
            wrapper.in("position_id", positionIds);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("candidate_name", kw).or().like("phone", kw).or().like("email", kw));
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (positionId != null) {
            wrapper.eq("position_id", positionId);
        }
        wrapper.orderByAsc("status").orderByDesc("id");
        Page<RecruitmentCandidates> result = candidatesRepository.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (RecruitmentCandidates c : result.getRecords()) {
            records.add(toCandidateRow(c));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("pageNum", page);
        data.put("pageSize", size);
        return Result.success(data);
    }

    // 录入候选人：岗位要在招，手机号和邮箱只做基本校验
    @PostMapping("/candidates")
    @RequirePermission("recruitment:candidate:add")
    public Result<Long> createCandidate(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String name = text(body, "candidateName", "candidate_name");
        if (name == null) {
            return Result.error("候选人姓名不能为空");
        }
        Long positionId = toLong(first(body, "positionId", "position_id"));
        if (positionId == null) {
            return Result.error("请选择应聘岗位");
        }
        RecruitmentPositions position = positionsRepository.selectById(positionId);
        if (position == null) {
            return Result.error("应聘岗位不存在或已删除");
        }
        if (!dataScopeService.canOperateOrg(RequestUser.id(request), position.getDept_id())) {
            return Result.error(403, "无权操作该岗位的候选人：不在您的数据范围内");
        }
        if (position.getStatus() != null && position.getStatus() == POSITION_FINISHED) {
            return Result.error("该岗位已结束招聘，不能再录入候选人");
        }
        RecruitmentCandidates entity = new RecruitmentCandidates();
        entity.setPosition_id(positionId);
        entity.setCandidate_name(name);
        entity.setGender(genderOf(first(body, "gender", "gender")));
        entity.setPhone(text(body, "phone", "phone"));
        entity.setEmail(text(body, "email", "email"));
        entity.setEducation(text(body, "education", "education"));
        entity.setExperience_years(toInt(first(body, "experienceYears", "experience_years")));
        entity.setSource(text(body, "source", "source"));
        entity.setResume_path(text(body, "resumePath", "resume_path"));
        entity.setStatus(CANDIDATE_SCREENING);
        entity.setExpected_salary(toDecimal(first(body, "expectedSalary", "expected_salary")));
        entity.setCreator_id(RequestUser.id(request));
        entity.setRemark(text(body, "remark", "remark"));
        entity.setCreated_at(LocalDateTime.now());
        entity.setUpdated_at(LocalDateTime.now());
        candidatesRepository.insert(entity);
        return Result.success(entity.getId());
    }

    // 改候选人资料，状态不在这里改
    @PutMapping("/candidates/{id}")
    @RequirePermission("recruitment:candidate:edit")
    public Result<Void> updateCandidate(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        RecruitmentCandidates db = candidatesRepository.selectById(id);
        if (db == null) {
            return Result.error("候选人不存在或已删除");
        }
        if (!candidateInScope(request, db)) {
            return Result.error(403, "无权修改该候选人：不在您的数据范围内");
        }
        String name = text(body, "candidateName", "candidate_name");
        if (name != null) {
            db.setCandidate_name(name);
        }
        Long positionId = toLong(first(body, "positionId", "position_id"));
        if (positionId != null && !positionId.equals(db.getPosition_id())) {
            if (positionsRepository.selectById(positionId) == null) {
                return Result.error("应聘岗位不存在或已删除");
            }
            db.setPosition_id(positionId);
        }
        Integer gender = genderOf(first(body, "gender", "gender"));
        if (gender != null) {
            db.setGender(gender);
        }
        String phone = text(body, "phone", "phone");
        if (phone != null) {
            db.setPhone(phone);
        }
        String email = text(body, "email", "email");
        if (email != null) {
            db.setEmail(email);
        }
        String education = text(body, "education", "education");
        if (education != null) {
            db.setEducation(education);
        }
        Integer experienceYears = toInt(first(body, "experienceYears", "experience_years"));
        if (experienceYears != null) {
            db.setExperience_years(experienceYears);
        }
        String source = text(body, "source", "source");
        if (source != null) {
            db.setSource(source);
        }
        String resumePath = text(body, "resumePath", "resume_path");
        if (resumePath != null) {
            db.setResume_path(resumePath);
        }
        BigDecimal salary = toDecimal(first(body, "expectedSalary", "expected_salary"));
        if (salary != null) {
            db.setExpected_salary(salary);
        }
        String remark = text(body, "remark", "remark");
        if (remark != null) {
            db.setRemark(remark);
        }
        db.setUpdated_at(LocalDateTime.now());
        candidatesRepository.updateById(db);
        return Result.success(null);
    }

    // 状态在待筛选、待面试、面试通过、已入职之间推进，任何阶段可淘汰，淘汰后回待筛选
    @PutMapping("/candidates/{id}/status")
    @RequirePermission("recruitment:candidate:status")
    public Result<Void> updateCandidateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        RecruitmentCandidates db = candidatesRepository.selectById(id);
        if (db == null) {
            return Result.error("候选人不存在或已删除");
        }
        if (!candidateInScope(request, db)) {
            return Result.error(403, "无权修改该候选人：不在您的数据范围内");
        }
        Integer target = toInt(first(body, "status", "status"));
        if (target == null) {
            return Result.error("缺少目标状态");
        }
        int current = db.getStatus() == null ? CANDIDATE_SCREENING : db.getStatus();
        if (!canTransfer(current, target)) {
            return Result.error("不能从「" + candidateStatusText(current) + "」直接变成「" + candidateStatusText(target) + "」");
        }
        // 安排面试必须给时间，否则没法提醒
        LocalDateTime interviewTime = toDateTime(first(body, "interviewTime", "interview_time"));
        if (target == CANDIDATE_INTERVIEW && interviewTime == null && db.getInterview_time() == null) {
            return Result.error("安排面试请先填写面试时间");
        }
        // 入职前要确认岗位还有名额
        if (target == CANDIDATE_HIRED) {
            RecruitmentPositions position = positionsRepository.selectById(db.getPosition_id());
            if (position == null) {
                return Result.error("应聘岗位不存在或已删除，无法办理入职");
            }
            if (position.getStatus() != null && position.getStatus() != POSITION_OPEN) {
                return Result.error("该岗位不在招聘中，无法标记入职");
            }
            if (position.getHeadcount() != null && position.getHeadcount() > 0) {
                QueryWrapper<RecruitmentCandidates> wrapper = new QueryWrapper<>();
                wrapper.eq("position_id", position.getId()).eq("status", CANDIDATE_HIRED);
                long hired = candidatesRepository.selectCount(wrapper);
                if (hired >= position.getHeadcount()) {
                    return Result.error("该岗位计划招聘 " + position.getHeadcount() + " 人，已入职 " + hired + " 人，无法再标记入职");
                }
            }
        }
        db.setStatus(target);
        if (interviewTime != null) {
            db.setInterview_time(interviewTime);
        }
        String interviewer = text(body, "interviewer", "interviewer");
        if (interviewer != null) {
            db.setInterviewer(interviewer);
        }
        String evaluation = text(body, "evaluation", "evaluation");
        if (evaluation != null) {
            db.setEvaluation(evaluation);
        }
        db.setUpdated_at(LocalDateTime.now());
        candidatesRepository.updateById(db);
        return Result.success(null);
    }

    // 已入职的候选人不能删，记录要留着
    @DeleteMapping("/candidates/{id}")
    @RequirePermission("recruitment:candidate:delete")
    public Result<Void> deleteCandidate(@PathVariable Long id, HttpServletRequest request) {
        RecruitmentCandidates db = candidatesRepository.selectById(id);
        if (db == null) {
            return Result.error("候选人不存在或已删除");
        }
        if (!candidateInScope(request, db)) {
            return Result.error(403, "无权删除该候选人：不在您的数据范围内");
        }
        if (db.getStatus() != null && db.getStatus() == CANDIDATE_HIRED) {
            return Result.error("该候选人已入职，不能删除");
        }
        candidatesRepository.deleteById(id);
        return Result.success(null);
    }

    // 招聘概览：岗位数和各阶段候选人数量
    @GetMapping("/overview")
    @RequirePermission({"recruitment:position:list", "recruitment:candidate:list"})
    public Result<Map<String, Object>> overview(HttpServletRequest request) {
        // 概览也要按数据范围算，和列表对得上
        List<Long> depts = dataScopeService.visibleOrgIds(RequestUser.id(request));
        QueryWrapper<RecruitmentPositions> openWrapper = new QueryWrapper<>();
        openWrapper.eq("status", POSITION_OPEN);
        if (depts != null) {
            if (depts.isEmpty()) {
                openWrapper.apply("1 = 0");
            } else {
                openWrapper.in("dept_id", depts);
            }
        }
        List<RecruitmentPositions> openList = positionsRepository.selectList(openWrapper);
        int headcount = 0;
        for (RecruitmentPositions position : openList) {
            headcount += position.getHeadcount() == null ? 0 : position.getHeadcount();
        }
        List<Long> positionIds = visiblePositionIds(RequestUser.id(request));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("openPositions", openList.size());
        data.put("headcount", headcount);
        data.put("screening", candidateCount(CANDIDATE_SCREENING, positionIds));
        data.put("interview", candidateCount(CANDIDATE_INTERVIEW, positionIds));
        data.put("passed", candidateCount(CANDIDATE_PASSED, positionIds));
        data.put("hired", candidateCount(CANDIDATE_HIRED, positionIds));
        data.put("rejected", candidateCount(CANDIDATE_REJECTED, positionIds));
        return Result.success(data);
    }

    // 可见岗位 id，null 表示不限，空集合表示一个都看不到
    private List<Long> visiblePositionIds(Long userId) {
        List<Long> depts = dataScopeService.visibleOrgIds(userId);
        if (depts == null) {
            return null;
        }
        List<Long> ids = new ArrayList<>();
        if (depts.isEmpty()) {
            return ids;
        }
        QueryWrapper<RecruitmentPositions> wrapper = new QueryWrapper<>();
        wrapper.select("id").in("dept_id", depts);
        for (RecruitmentPositions position : positionsRepository.selectList(wrapper)) {
            ids.add(position.getId());
        }
        return ids;
    }

    // 候选人所属岗位是否在当前用户的数据范围内
    private boolean candidateInScope(HttpServletRequest request, RecruitmentCandidates candidate) {
        RecruitmentPositions position = candidate.getPosition_id() == null
                ? null : positionsRepository.selectById(candidate.getPosition_id());
        return position != null && dataScopeService.canOperateOrg(RequestUser.id(request), position.getDept_id());
    }

    // 数据范围为空时直接返回空页，省一次查询
    private Map<String, Object> emptyPage(int page, int size) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", new ArrayList<>());
        data.put("total", 0);
        data.put("pageNum", page);
        data.put("pageSize", size);
        return data;
    }

    // 某状态下的候选人数量
    private Integer candidateCount(int status, List<Long> positionIds) {
        if (positionIds != null && positionIds.isEmpty()) {
            return 0;
        }
        QueryWrapper<RecruitmentCandidates> wrapper = new QueryWrapper<>();
        wrapper.eq("status", status);
        if (positionIds != null) {
            wrapper.in("position_id", positionIds);
        }
        return Math.toIntExact(candidatesRepository.selectCount(wrapper));
    }

    // 已入职是终点，任何阶段可淘汰，淘汰后可回待筛选
    private boolean canTransfer(int current, int target) {
        if (current == target) {
            return false;
        }
        if (current == CANDIDATE_HIRED) {
            return false;
        }
        if (target == CANDIDATE_REJECTED) {
            return current != CANDIDATE_HIRED;
        }
        if (current == CANDIDATE_REJECTED) {
            return target == CANDIDATE_SCREENING;
        }
        if (current == CANDIDATE_SCREENING) {
            return target == CANDIDATE_INTERVIEW;
        }
        if (current == CANDIDATE_INTERVIEW) {
            return target == CANDIDATE_PASSED;
        }
        return current == CANDIDATE_PASSED && target == CANDIDATE_HIRED;
    }

    // 状态文案，报错时给中文，前端不用再翻译
    private String candidateStatusText(int status) {
        switch (status) {
            case CANDIDATE_SCREENING:
                return "待筛选";
            case CANDIDATE_INTERVIEW:
                return "待面试";
            case CANDIDATE_PASSED:
                return "面试通过";
            case CANDIDATE_HIRED:
                return "已入职";
            case CANDIDATE_REJECTED:
                return "已淘汰";
            default:
                return "未知状态";
        }
    }

    // 每个岗位的候选人数与已入职人数，键是岗位 ID
    private Map<Long, int[]> candidateCounters() {
        Map<Long, int[]> counters = new HashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT position_id, COUNT(*) AS total, SUM(status = 4) AS hired FROM recruitment_candidates " +
                        "WHERE deleted_at IS NULL GROUP BY position_id");
        for (Map<String, Object> row : rows) {
            Long positionId = toLong(row.get("position_id"));
            if (positionId == null) {
                continue;
            }
            counters.put(positionId, new int[]{toInt(row.get("total")), toInt(row.get("hired"))});
        }
        return counters;
    }

    // 岗位转前端字段，顺带查出部门名称
    private Map<String, Object> toPositionRow(RecruitmentPositions p) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", p.getId());
        row.put("positionName", p.getPosition_name());
        row.put("deptId", p.getDept_id());
        row.put("deptName", orgName(p.getDept_id()));
        row.put("positionId", p.getPosition_id());
        row.put("positionLevelName", positionName(p.getPosition_id()));
        row.put("headcount", p.getHeadcount());
        row.put("education", p.getEducation());
        row.put("experienceYears", p.getExperience_years());
        row.put("salaryRange", p.getSalary_range());
        row.put("workPlace", p.getWork_place());
        row.put("duty", p.getDuty());
        row.put("requirement", p.getRequirement());
        row.put("status", p.getStatus());
        row.put("statusText", positionStatusText(p.getStatus()));
        row.put("publishTime", p.getPublish_time());
        row.put("deadline", p.getDeadline());
        row.put("remark", p.getRemark());
        row.put("creatorId", p.getCreator_id());
        row.put("createdAt", p.getCreated_at());
        return row;
    }

    // 候选人转前端字段
    private Map<String, Object> toCandidateRow(RecruitmentCandidates c) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", c.getId());
        row.put("positionId", c.getPosition_id());
        RecruitmentPositions position = c.getPosition_id() == null ? null : positionsRepository.selectById(c.getPosition_id());
        row.put("positionName", position == null ? null : position.getPosition_name());
        row.put("deptName", position == null ? null : orgName(position.getDept_id()));
        row.put("candidateName", c.getCandidate_name());
        row.put("gender", c.getGender());
        row.put("phone", c.getPhone());
        row.put("email", c.getEmail());
        row.put("education", c.getEducation());
        row.put("experienceYears", c.getExperience_years());
        row.put("source", c.getSource());
        row.put("resumePath", c.getResume_path());
        row.put("status", c.getStatus());
        row.put("statusText", candidateStatusText(c.getStatus() == null ? CANDIDATE_SCREENING : c.getStatus()));
        row.put("interviewTime", c.getInterview_time());
        row.put("interviewer", c.getInterviewer());
        row.put("evaluation", c.getEvaluation());
        row.put("expectedSalary", c.getExpected_salary());
        row.put("remark", c.getRemark());
        row.put("creatorId", c.getCreator_id());
        row.put("createdAt", c.getCreated_at());
        return row;
    }

    // 岗位状态文案
    private String positionStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case POSITION_STOPPED:
                return "已停招";
            case POSITION_OPEN:
                return "在招";
            case POSITION_FINISHED:
                return "已结束";
            default:
                return "未知";
        }
    }

    // 部门名称，查不到给空
    private String orgName(Long orgId) {
        if (orgId == null) {
            return null;
        }
        try {
            return jdbc.queryForObject("SELECT org_name FROM organizations WHERE id = ? AND deleted_at IS NULL", String.class, orgId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // 职位名称，用于显示岗位对应职级
    private String positionName(Long positionId) {
        if (positionId == null) {
            return null;
        }
        try {
            return jdbc.queryForObject("SELECT position_name FROM positions WHERE id = ? AND deleted_at IS NULL", String.class, positionId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // 取第一个非空的字段值，兼容前端小驼峰与库里下划线
    private Object first(Map<String, Object> body, String camelKey, String snakeKey) {
        if (body == null) {
            return null;
        }
        Object value = body.get(camelKey);
        return value == null ? body.get(snakeKey) : value;
    }

    // 取字符串字段，空串按没传处理
    private String text(Map<String, Object> body, String camelKey, String snakeKey) {
        Object value = first(body, camelKey, snakeKey);
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    // 取整数字段
    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 取主键
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 取金额，非法值按没传处理
    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 取性别，只接受 0、1、2
    private Integer genderOf(Object value) {
        Integer gender = toInt(value);
        if (gender == null) {
            return null;
        }
        if (gender != 0 && gender != 1 && gender != 2) {
            return 0;
        }
        return gender;
    }

    // 取日期，前端可能传 yyyy-MM-dd 或带时间的 ISO 串
    private LocalDate toDate(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            if (s.length() >= 10) {
                return LocalDate.parse(s.substring(0, 10));
            }
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    // 取时间，前端 el-date-picker 会带 T 和毫秒
    private LocalDateTime toDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            String normalized = s.replace(' ', 'T');
            if (normalized.length() == 16) {
                normalized = normalized + ":00";
            }
            if (normalized.length() > 19) {
                normalized = normalized.substring(0, 19);
            }
            return LocalDateTime.parse(normalized);
        } catch (Exception e) {
            return null;
        }
    }
}
