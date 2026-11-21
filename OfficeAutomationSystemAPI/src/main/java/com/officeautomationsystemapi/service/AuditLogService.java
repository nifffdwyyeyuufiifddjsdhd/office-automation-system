package com.officeautomationsystemapi.service;

import com.officeautomationsystemapi.entity.OperationLogs;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.OperationLogsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

// 操作审计入口：控制器可补业务字段，拦截器记所有写操作
// 一次请求只会落一条：谁先写谁打标记，后写的看到标记就跳过
@Service
public class AuditLogService {

    public static final String RECORDED_ATTR = "AUDIT_LOG_RECORDED";

    private static final int MAX_TEXT = 2000;

    @Autowired
    private OperationLogsRepository operationLogsRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private SensitiveDataService sensitiveDataService;

    public void record(Long userId, String module, String action, String method, String url,
                       String ip, String userAgent, String requestParams, String responseResult,
                       Integer status, String errorMsg, Integer costTime) {
        record(userId, module, action, method, url, ip, userAgent, requestParams, responseResult,
                status, errorMsg, costTime, null, null);
    }

    public void record(Long userId, String module, String action, String method, String url,
                       String ip, String userAgent, String requestParams, String responseResult,
                       Integer status, String errorMsg, Integer costTime,
                       String targetType, Long targetId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            OperationLogs log = new OperationLogs();
            log.setUser_id(userId);
            log.setUsername(usernameOf(userId));
            log.setModule(cut(module, 50));
            log.setAction(cut(action, 50));
            log.setMethod(method);
            log.setUrl(cut(url, 255));
            log.setIp_address(cut(ip, 64));
            log.setUser_agent(cut(userAgent, 255));
            log.setTarget_type(targetType);
            log.setTarget_id(targetId);
            log.setRequest_params(cut(mask(requestParams), MAX_TEXT));
            log.setResponse_result(cut(responseResult, MAX_TEXT));
            log.setStatus(status == null ? 1 : status);
            log.setError_msg(cut(mask(errorMsg), 500));
            log.setCost_time(costTime);
            log.setCreated_at(now);
            log.setUpdated_at(now);
            operationLogsRepository.insert(log);
            markRecorded();
        } catch (Exception ignore) {
            // 审计失败不能影响业务
        }
    }

    // 给当前请求打标记，拦截器据此避免同一请求重复记录
    private void markRecorded() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            servletAttributes.getRequest().setAttribute(RECORDED_ATTR, Boolean.TRUE);
        }
    }

    private String usernameOf(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            Users user = usersRepository.selectById(userId);
            return user == null ? null : user.getUsername();
        } catch (Exception e) {
            return null;
        }
    }

    // 参数里可能带手机号等敏感信息，落库前先脱敏
    private String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        try {
            return sensitiveDataService.maskText(text);
        } catch (Exception e) {
            return text;
        }
    }

    private String cut(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}
