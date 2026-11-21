package com.officeautomationsystemapi.service;

import com.officeautomationsystemapi.entity.FlowDelegates;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 审批委托：委托人在时间段内把待办交给代理人处理
public interface FlowDelegateService {

    // 当前生效的委托人ID列表，代理人的待办查询要用它做 IN 条件
    List<Long> activeDelegators(Long agentId);

    // 判断待办的审核人是否把这段时间委托给了当前操作人
    boolean canHandle(Long agentId, Long auditorId);

    // 我的委托 + 委托给我的，两段一起返回
    Map<String, Object> list(Long userId);

    // 新建委托
    FlowDelegates create(Long ownerId, Long agentId, LocalDateTime startTime, LocalDateTime endTime, String reason);

    // 取消委托，只有委托人自己或管理员能取消
    void cancel(Long operatorId, Long id);

    // 可选的代理人列表（在职用户）
    List<Map<String, Object>> agentOptions();
}
