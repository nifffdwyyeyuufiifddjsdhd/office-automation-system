package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.FlowDefinitions;
import com.officeautomationsystemapi.entity.FlowInstances;
import com.officeautomationsystemapi.entity.FlowStepTransitions;
import com.officeautomationsystemapi.entity.FlowSteps;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.FlowDefinitionsRepository;
import com.officeautomationsystemapi.repository.FlowInstancesRepository;
import com.officeautomationsystemapi.repository.FlowStepTransitionsRepository;
import com.officeautomationsystemapi.repository.FlowStepsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.FlowScopeService;
import com.officeautomationsystemapi.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/flow")
public class FlowDefinitionController {

    @Autowired
    private FlowDefinitionsRepository definitionsRepository;
    @Autowired
    private FlowStepsRepository stepsRepository;
    @Autowired
    private FlowStepTransitionsRepository transitionsRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private FlowInstancesRepository instancesRepository;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private FlowScopeService flowScopeService;

    // 模板列表，管理后台用，带创建人姓名和步骤数
    @GetMapping("/definitions")
    @RequirePermission("flow:def:manage")
    public Result<List<Map<String, Object>>> list() {
        QueryWrapper<FlowDefinitions> qw = new QueryWrapper<>();
        // 同一模板的版本排在一起，版本号大的在前
        qw.orderByDesc("sort_order").orderByAsc("code").orderByDesc("version");
        List<FlowDefinitions> list = definitionsRepository.selectList(qw);

        // 先收集创建人 ID，一次查完
        List<Long> creatorIds = new ArrayList<>();
        for (FlowDefinitions d : list) {
            if (d.getCreator_id() != null && !creatorIds.contains(d.getCreator_id())) {
                creatorIds.add(d.getCreator_id());
            }
        }
        Map<Long, String> creatorNameMap = new HashMap<>();
        if (!creatorIds.isEmpty()) {
            List<Users> creators = usersRepository.selectBatchIds(creatorIds);
            for (Users u : creators) {
                creatorNameMap.put(u.getId(), u.getReal_name() != null ? u.getReal_name() : u.getUsername());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (FlowDefinitions d : list) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", d.getId());
            row.put("flow_name", d.getFlow_name());
            row.put("code", d.getCode());
            row.put("flow_type", d.getFlow_type());
            row.put("need_payment", d.getNeed_payment());
            row.put("description", d.getDescription());
            row.put("icon", d.getIcon());
            row.put("version", d.getVersion());
            row.put("supersededBy", d.getSuperseded_by());
            row.put("status", d.getStatus());
            row.put("statusText", statusText(d.getStatus()));
            row.put("audit_status", d.getAudit_status());
            row.put("auditStatusText", auditStatusText(d.getAudit_status()));
            row.put("publish_time", d.getPublish_time());
            row.put("created_at", d.getCreated_at());
            row.put("dept_id", d.getDept_id());
            // 适用范围 0 全员、1 指定部门、2 指定角色，列表要带上
            row.put("use_scope", d.getUse_scope());
            row.put("use_scope_ids", d.getUse_scope_ids());
            row.put("creator_id", d.getCreator_id());
            row.put("creatorName", creatorNameMap.getOrDefault(d.getCreator_id(), "未知"));
            QueryWrapper<FlowSteps> stepQw = new QueryWrapper<>();
            stepQw.eq("definition_id", d.getId());
            row.put("stepCount", stepsRepository.selectCount(stepQw));
            result.add(row);
        }
        return Result.success(result);
    }

    // 已发布且审核通过的模板，发起流程时用
    @GetMapping("/definitions/published")
    public Result<List<FlowDefinitions>> published(HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        QueryWrapper<FlowDefinitions> qw = new QueryWrapper<>();
        qw.eq("status", 1).eq("audit_status", 1).orderByDesc("sort_order");
        List<FlowDefinitions> all = definitionsRepository.selectList(qw);

        // 管理员看全部，其他人按使用范围过滤：全员、部门含下级、角色
        if (isAdmin(userId)) {
            return Result.success(all);
        }
        List<FlowDefinitions> filtered = new ArrayList<>();
        for (FlowDefinitions d : all) {
            if (flowScopeService.allowed(d, userId)) {
                filtered.add(d);
            }
        }
        return Result.success(filtered);
    }

    // 模板详情，带步骤和每步的跳转条件
    @GetMapping("/definitions/{id}")
    @RequirePermission({"flow:def:manage", "flow:start", "flow:all:view"})
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        FlowDefinitions definition = definitionsRepository.selectById(id);
        if (definition == null) {
            return Result.error("流程不存在");
        }
        QueryWrapper<FlowSteps> stepQw = new QueryWrapper<>();
        stepQw.eq("definition_id", id).orderByAsc("sort_order");
        List<FlowSteps> steps = stepsRepository.selectList(stepQw);

        Map<Long, List<FlowStepTransitions>> transitionMap = new HashMap<>();
        if (!steps.isEmpty()) {
            List<Long> stepIds = new ArrayList<>();
            for (FlowSteps step : steps) {
                stepIds.add(step.getId());
            }
            QueryWrapper<FlowStepTransitions> transitionQw = new QueryWrapper<>();
            transitionQw.in("step_id", stepIds).orderByAsc("step_id").orderByAsc("priority");
            for (FlowStepTransitions transition : transitionsRepository.selectList(transitionQw)) {
                transitionMap.computeIfAbsent(transition.getStep_id(), k -> new ArrayList<>()).add(transition);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("definition", definition);
        result.put("steps", steps);
        result.put("transitionsByStep", transitionMap);
        return Result.success(result);
    }

    // 新建模板存为草稿，同时写步骤和跳转
    @PostMapping("/definitions")
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("flow:def:manage")
    public Result<Long> save(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> defMap = (Map<String, Object>) body.get("definition");
        FlowDefinitions definition = new FlowDefinitions();
        definition.setFlow_name((String) defMap.get("flow_name"));
        definition.setCode((String) defMap.get("code"));
        definition.setFlow_type(defMap.get("flow_type") == null ? 1 : Integer.valueOf(defMap.get("flow_type").toString()));
        definition.setDescription((String) defMap.get("description"));
        definition.setIcon((String) defMap.get("icon"));
        definition.setForm_config((String) defMap.get("form_config"));
        // 勾了需要财务付款，审批通过后才进待付款池
        definition.setNeed_payment(defMap.get("need_payment") == null ? 0 : Integer.valueOf(defMap.get("need_payment").toString()));
        applyUseScope(definition, defMap);
        definition.setVersion(1);
        definition.setStatus(0);
        definition.setAudit_status(1);
        definition.setSort_order(defMap.get("sort_order") == null ? 0 : Integer.valueOf(defMap.get("sort_order").toString()));
        definition.setCreated_at(LocalDateTime.now());
        definition.setUpdated_at(LocalDateTime.now());

        Long currentUserId = RequestUser.id(request);
        definition.setCreator_id(currentUserId);
        // 管理员可指定部门，传空为全员；普通用户用自己的部门
        Object deptObj = defMap.get("dept_id");
        if (isAdmin(currentUserId)) {
            definition.setDept_id(deptObj == null || deptObj.toString().isEmpty() ? null : Long.valueOf(deptObj.toString()));
        } else {
            Users me = usersRepository.selectById(currentUserId);
            definition.setDept_id(me == null ? null : me.getOrg_id());
        }
        definitionsRepository.insert(definition);

        saveStepsAndTransitions(definition.getId(), (List<Map<String, Object>>) body.get("steps"),
                (List<Map<String, Object>>) body.get("transitions"));
        return Result.success(definition.getId());
    }

    // 已发布或有单据时不改原版本，生成新版本，旧单据仍绑原版本
    @PutMapping("/definitions/{id}")
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("flow:def:manage")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        FlowDefinitions definition = definitionsRepository.selectById(id);
        if (definition == null) {
            return Result.error("流程不存在");
        }
        Long currentUserId = RequestUser.id(request);
        if (!isAdmin(currentUserId) && !currentUserId.equals(definition.getCreator_id())) {
            return Result.error("只能修改自己创建的模板");
        }
        // 有单据在跑就不能改步骤，否则审批轨迹对不上
        long used = countInstances(id);
        Integer status = definition.getStatus();
        boolean published = status != null && status == 1;
        if (used > 0 || published) {
            return createNewVersion(definition, used, published, body);
        }

        Map<String, Object> defMap = (Map<String, Object>) body.get("definition");
        if (defMap.get("flow_name") != null) {
            definition.setFlow_name((String) defMap.get("flow_name"));
        }
        if (defMap.get("code") != null) {
            definition.setCode((String) defMap.get("code"));
        }
        if (defMap.get("description") != null) {
            definition.setDescription((String) defMap.get("description"));
        }
        if (defMap.get("icon") != null) {
            definition.setIcon((String) defMap.get("icon"));
        }
        if (defMap.get("form_config") != null) {
            definition.setForm_config((String) defMap.get("form_config"));
        }
        if (defMap.get("need_payment") != null) {
            definition.setNeed_payment(Integer.valueOf(defMap.get("need_payment").toString()));
        }
        applyUseScope(definition, defMap);
        if (defMap.get("sort_order") != null) {
            definition.setSort_order(Integer.valueOf(defMap.get("sort_order").toString()));
        }
        if (defMap.get("dept_id") != null) {
            Object deptObj = defMap.get("dept_id");
            definition.setDept_id(deptObj == null || deptObj.toString().isEmpty() ? null : Long.valueOf(deptObj.toString()));
        }
        definition.setUpdated_at(LocalDateTime.now());
        definitionsRepository.updateById(definition);

        // 旧步骤和跳转先删再重建
        QueryWrapper<FlowSteps> stepQw = new QueryWrapper<>();
        stepQw.eq("definition_id", id);
        List<FlowSteps> oldSteps = stepsRepository.selectList(stepQw);
        if (!oldSteps.isEmpty()) {
            List<Long> oldStepIds = new ArrayList<>();
            for (FlowSteps step : oldSteps) {
                oldStepIds.add(step.getId());
            }
            QueryWrapper<FlowStepTransitions> transitionQw = new QueryWrapper<>();
            transitionQw.in("step_id", oldStepIds);
            transitionsRepository.delete(transitionQw);
            stepsRepository.delete(stepQw);
        }
        saveStepsAndTransitions(id, (List<Map<String, Object>>) body.get("steps"),
                (List<Map<String, Object>>) body.get("transitions"));
        Map<String, Object> data = new HashMap<>();
        data.put("mode", "in-place");
        data.put("definitionId", id);
        data.put("version", definition.getVersion());
        data.put("message", "已保存");
        return Result.success(data);
    }

    // 使用范围 0 全员、1 指定部门、2 指定角色，ids 存 JSON 数组
    private void applyUseScope(FlowDefinitions definition, Map<String, Object> defMap) {
        if (defMap == null) {
            return;
        }
        if (defMap.get("use_scope") != null) {
            int scope = Integer.parseInt(defMap.get("use_scope").toString());
            definition.setUse_scope(scope == 1 || scope == 2 ? scope : 0);
        }
        if (defMap.get("use_scope_ids") != null) {
            String ids = defMap.get("use_scope_ids").toString();
            definition.setUse_scope_ids(ids.isEmpty() ? null : ids);
        }
    }

    // 生成新版本：复制旧表头和表单配置，步骤用这次提交的
    private Result<Map<String, Object>> createNewVersion(FlowDefinitions old, long used, boolean published,
                                                         Map<String, Object> body) {
        Map<String, Object> defMap = (Map<String, Object>) body.get("definition");
        int nextVersion = maxVersionOfCode(old.getCode()) + 1;

        FlowDefinitions version = new FlowDefinitions();
        version.setFlow_name(old.getFlow_name());
        version.setCode(old.getCode());
        version.setFlow_type(old.getFlow_type());
        version.setDescription(old.getDescription());
        version.setDept_id(old.getDept_id());
        version.setIcon(old.getIcon());
        version.setForm_config(old.getForm_config());
        version.setNeed_payment(old.getNeed_payment());
        version.setUse_scope(old.getUse_scope());
        version.setUse_scope_ids(old.getUse_scope_ids());
        version.setAudit_status(old.getAudit_status());
        version.setCreator_id(old.getCreator_id());
        version.setSort_order(old.getSort_order());
        version.setVersion(nextVersion);
        version.setStatus(0);
        version.setCreated_at(LocalDateTime.now());
        version.setUpdated_at(LocalDateTime.now());
        // 表头可以一起改，表单配置决定申请单长什么样，要同步过来
        if (defMap != null) {
            if (defMap.get("flow_name") != null) version.setFlow_name((String) defMap.get("flow_name"));
            if (defMap.get("description") != null) version.setDescription((String) defMap.get("description"));
            if (defMap.get("icon") != null) version.setIcon((String) defMap.get("icon"));
            if (defMap.get("form_config") != null) version.setForm_config((String) defMap.get("form_config"));
            if (defMap.get("need_payment") != null) {
                version.setNeed_payment(Integer.valueOf(defMap.get("need_payment").toString()));
            }
            applyUseScope(version, defMap);
            if (defMap.get("sort_order") != null) {
                version.setSort_order(Integer.valueOf(defMap.get("sort_order").toString()));
            }
            if (defMap.get("dept_id") != null) {
                Object deptObj = defMap.get("dept_id");
                version.setDept_id(deptObj == null || deptObj.toString().isEmpty() ? null : Long.valueOf(deptObj.toString()));
            }
        }
        definitionsRepository.insert(version);
        saveStepsAndTransitions(version.getId(), (List<Map<String, Object>>) body.get("steps"),
                (List<Map<String, Object>>) body.get("transitions"));

        Map<String, Object> data = new HashMap<>();
        data.put("mode", "new-version");
        data.put("definitionId", version.getId());
        data.put("version", nextVersion);
        data.put("oldVersionId", old.getId());
        data.put("instanceCount", used);

        // 老版本是发布状态就让新版本接手发布
        if (published) {
            String err = chainError(version.getId());
            if (err != null) {
                data.put("published", false);
                data.put("message", "已生成 v" + nextVersion + " 草稿，但" + err + "，修正后再发布");
                return Result.success(data);
            }
            publishVersion(version);
            data.put("published", true);
            data.put("message", "已生成 v" + nextVersion + " 并发布，之前 " + used + " 张单据继续按 v"
                    + old.getVersion() + " 流转");
        } else {
            data.put("published", false);
            data.put("message", "已生成 v" + nextVersion + " 草稿，确认后发布");
        }
        return Result.success(data);
    }

    // 发布：写状态和发布时间，同 code 的旧版本标为已被替代
    private void publishVersion(FlowDefinitions version) {
        LocalDateTime now = LocalDateTime.now();
        version.setStatus(1);
        version.setPublish_time(now);
        version.setUpdated_at(now);
        definitionsRepository.updateById(version);
        // 旧版本数据保留，只是不再出现在发起流程里
        definitionsRepository.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<FlowDefinitions>()
                .eq("code", version.getCode())
                .ne("id", version.getId())
                .eq("status", 1)
                .set("status", 3)
                .set("superseded_by", version.getId())
                .set("updated_at", now));
    }

    // 同一个 code 当前最大版本号
    private int maxVersionOfCode(String code) {
        QueryWrapper<FlowDefinitions> qw = new QueryWrapper<>();
        qw.eq("code", code);
        List<FlowDefinitions> versions = definitionsRepository.selectList(qw);
        int max = 1;
        for (FlowDefinitions d : versions) {
            if (d.getVersion() != null && d.getVersion() > max) {
                max = d.getVersion();
            }
        }
        return max;
    }

    // 校验流转线是否完整，通过返回 null
    private String chainError(Long definitionId) {
        List<FlowSteps> allSteps = stepsRepository.selectList(new QueryWrapper<FlowSteps>()
                .eq("definition_id", definitionId).orderByAsc("sort_order"));
        if (allSteps.isEmpty()) {
            return "至少需要一个审批步骤才能发布";
        }
        List<Long> stepIdList = new ArrayList<>();
        for (FlowSteps s : allSteps) {
            stepIdList.add(s.getId());
        }
        Set<Long> stepWithNext = new HashSet<>();
        QueryWrapper<FlowStepTransitions> transQw = new QueryWrapper<>();
        transQw.in("step_id", stepIdList);
        for (FlowStepTransitions t : transitionsRepository.selectList(transQw)) {
            stepWithNext.add(t.getStep_id());
        }
        for (int i = 0; i < allSteps.size() - 1; i++) {
            FlowSteps s = allSteps.get(i);
            if (!stepWithNext.contains(s.getId())) {
                return "第" + (i + 1) + "步「" + s.getStep_name() + "」没有配置流转线，发布后流程会卡在这一步";
            }
        }
        return null;
    }

    // 发布模板：先校验步骤和流转线，再标记同 code 的旧版本
    @PostMapping("/definitions/{id}/publish")
    @RequirePermission("flow:def:manage")
    public Result<Void> publish(@PathVariable Long id) {
        FlowDefinitions definition = definitionsRepository.selectById(id);
        if (definition == null) {
            return Result.error("流程不存在");
        }
        String chainErr = chainError(id);
        if (chainErr != null) {
            return Result.error(chainErr);
        }
        Integer auditStatus = definition.getAudit_status();
        if (auditStatus != null && auditStatus == 2) {
            return Result.error("模板已被拒绝，请修改后重新发布");
        }
        publishVersion(definition);
        return Result.success(null);
    }

    // 审核模板，只有管理员能操作
    @PostMapping("/definitions/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        if (!isAdmin(userId)) {
            return Result.error("只有管理员可以审核模板");
        }
        FlowDefinitions definition = definitionsRepository.selectById(id);
        if (definition == null) {
            return Result.error("流程不存在");
        }
        int pass = body.get("pass") == null ? 1 : Integer.parseInt(body.get("pass").toString());
        definition.setAudit_status(pass);
        // 拒绝时把模板一起停用，通过的保持原状态
        if (pass == 2) {
            definition.setStatus(2);
        }
        definition.setUpdated_at(LocalDateTime.now());
        definitionsRepository.updateById(definition);
        return Result.success(null);
    }

    // 停用模板
    @PostMapping("/definitions/{id}/disable")
    @RequirePermission("flow:def:manage")
    public Result<Void> disable(@PathVariable Long id, HttpServletRequest request) {
        FlowDefinitions definition = definitionsRepository.selectById(id);
        if (definition == null) {
            return Result.error("流程不存在");
        }
        Long currentUserId = RequestUser.id(request);
        if (!isAdmin(currentUserId) && !currentUserId.equals(definition.getCreator_id())) {
            return Result.error("只能停用自己创建的模板");
        }
        definition.setStatus(2);
        definition.setUpdated_at(LocalDateTime.now());
        definitionsRepository.updateById(definition);
        return Result.success(null);
    }

    // 删除模板，步骤和跳转一起标记删除
    @DeleteMapping("/definitions/{id}")
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("flow:def:manage")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        FlowDefinitions definition = definitionsRepository.selectById(id);
        if (definition == null) {
            return Result.error("流程不存在");
        }
        Long currentUserId = RequestUser.id(request);
        if (!isAdmin(currentUserId) && !currentUserId.equals(definition.getCreator_id())) {
            return Result.error("只能删除自己创建的模板");
        }
        // 有单据引用时不能删，只能停用
        long used = countInstances(id);
        if (used > 0) {
            return Result.error("该流程已有 " + used + " 张单据，不能删除，请改用停用");
        }
        QueryWrapper<FlowSteps> stepQw = new QueryWrapper<>();
        stepQw.eq("definition_id", id);
        List<FlowSteps> steps = stepsRepository.selectList(stepQw);
        if (!steps.isEmpty()) {
            List<Long> stepIds = new ArrayList<>();
            for (FlowSteps step : steps) {
                stepIds.add(step.getId());
            }
            QueryWrapper<FlowStepTransitions> transitionQw = new QueryWrapper<>();
            transitionQw.in("step_id", stepIds);
            transitionsRepository.delete(transitionQw);
            stepsRepository.delete(stepQw);
        }
        definitionsRepository.deleteById(id);
        return Result.success(null);
    }

    // 统计这个模板下还有多少单据
    private long countInstances(Long definitionId) {
        QueryWrapper<FlowInstances> qw = new QueryWrapper<>();
        qw.eq("definition_id", definitionId);
        Long count = instancesRepository.selectCount(qw);
        return count == null ? 0L : count;
    }

    // 保存步骤和跳转，前端临时 key 换成入库的步骤 ID
    private void saveStepsAndTransitions(Long definitionId,
                                         List<Map<String, Object>> steps,
                                         List<Map<String, Object>> transitions) {
        if (steps == null) {
            return;
        }
        Map<String, Long> stepIdMap = new HashMap<>();
        for (Map<String, Object> stepMap : steps) {
            FlowSteps step = new FlowSteps();
            step.setDefinition_id(definitionId);
            step.setStep_name((String) stepMap.get("step_name"));
            step.setStep_code((String) stepMap.get("step_code"));
            step.setStep_type(stepMap.get("step_type") == null ? 1 : Integer.valueOf(stepMap.get("step_type").toString()));
            step.setApprover_type(stepMap.get("approver_type") == null ? 1 : Integer.valueOf(stepMap.get("approver_type").toString()));
            step.setApprover_config((String) stepMap.get("approver_config"));
            step.setStep_form((String) stepMap.get("step_form"));
            step.setTimeout_hours(stepMap.get("timeout_hours") == null ? 0 : Integer.valueOf(stepMap.get("timeout_hours").toString()));
            step.setTimeout_action(stepMap.get("timeout_action") == null ? 0 : Integer.valueOf(stepMap.get("timeout_action").toString()));
            step.setSort_order(stepMap.get("sort_order") == null ? 0 : Integer.valueOf(stepMap.get("sort_order").toString()));
            step.setSign_type(stepMap.get("sign_type") == null ? 1 : Integer.valueOf(stepMap.get("sign_type").toString()));
            step.setSign_ratio(stepMap.get("sign_ratio") == null ? null : Integer.valueOf(stepMap.get("sign_ratio").toString()));
            step.setCreated_at(LocalDateTime.now());
            step.setUpdated_at(LocalDateTime.now());
            stepsRepository.insert(step);
            if (stepMap.get("_key") != null) {
                stepIdMap.put(stepMap.get("_key").toString(), step.getId());
            }
        }
        if (transitions == null) {
            return;
        }
        for (Map<String, Object> transitionMap : transitions) {
            Long stepId = stepIdMap.get(String.valueOf(transitionMap.get("step_key")));
            Long targetStepId = stepIdMap.get(String.valueOf(transitionMap.get("target_step_key")));
            if (stepId == null || targetStepId == null) {
                continue;
            }
            FlowStepTransitions transition = new FlowStepTransitions();
            transition.setStep_id(stepId);
            transition.setTarget_step_id(targetStepId);
            transition.setCondition_expr((String) transitionMap.get("condition_expr"));
            transition.setCondition_name((String) transitionMap.get("condition_name"));
            transition.setPriority(transitionMap.get("priority") == null ? 0 : Integer.valueOf(transitionMap.get("priority").toString()));
            transition.setIs_default(transitionMap.get("is_default") == null ? 0 : Integer.valueOf(transitionMap.get("is_default").toString()));
            transition.setTransition_type(transitionMap.get("transition_type") == null ? 1 : Integer.valueOf(transitionMap.get("transition_type").toString()));
            transition.setCreated_at(LocalDateTime.now());
            transition.setUpdated_at(LocalDateTime.now());
            transitionsRepository.insert(transition);
        }
    }

    // 是否管理员，按角色编码判断
    private boolean isAdmin(Long userId) {
        return userId != null && permissionService.isAdmin(userId);
    }

    // 审核状态文案
    private String auditStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 0:
                return "待审核";
            case 1:
                return "已通过";
            case 2:
                return "已拒绝";
            default:
                return "未知";
        }
    }

    // 模板状态文案
    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 0:
                return "草稿";
            case 1:
                return "已发布";
            case 2:
                return "已停用";
            case 3:
                return "已被新版本替代";
            default:
                return "未知";
        }
    }
}
