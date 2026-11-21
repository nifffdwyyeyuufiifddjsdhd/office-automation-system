package com.officeautomationsystemapi.exception;

import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.ExceptionLogs;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.ExceptionLogsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.SensitiveDataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

// 把没被捕获的异常转成统一 Result，同时记日志
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final int MAX_MESSAGE_LENGTH = 200;
    private static final int MAX_STACK_LENGTH = 8000;           //防止超出数据库TEXT字段限制引发二次异常

    @Autowired
    private ExceptionLogsRepository exceptionLogsRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private SensitiveDataService sensitiveDataService;

    // 业务代码抛的运行时异常，消息原样返回
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e, HttpServletResponse response, HttpServletRequest request) {
        log.warn("业务异常: {}", e.getMessage(), e);
        markDownloadFailed(response);
        // 消息不像业务提示时才记一条异常日志
        if (!looksLikeBusinessHint(e.getMessage())) {
            saveException(e, request);
        }
        // 数据库报错含表名和 SQL 片段，不回给前端
        if (looksLikeDatabaseError(e.getMessage())) {
            return Result.error(badInput(e.getMessage()) ? 400 : 500,
                    badInput(e.getMessage()) ? "参数格式不正确：日期或数字格式有误" : "数据查询失败，请稍后重试");
        }
        return Result.error(shortMessage(e.getMessage()));
    }

    // 唯一索引冲突多是编码、账号重复，提示改一下就行
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("唯一字段冲突: {}", e.getMessage());
        return Result.error(400, "数据已存在，请检查唯一字段");
    }

    // 缺必填参数，提示缺的是哪个
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParameter(MissingServletRequestParameterException e, HttpServletResponse response) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        markDownloadFailed(response);
        return Result.error(400, "缺少必需的参数：" + e.getParameterName());
    }

    // 路径里的变量没给全
    @ExceptionHandler(MissingPathVariableException.class)
    public Result<Void> handleMissingPathVariable(MissingPathVariableException e, HttpServletResponse response) {
        log.warn("缺少路径参数: {}", e.getVariableName());
        markDownloadFailed(response);
        return Result.error(400, "缺少必需的路径参数：" + e.getVariableName());
    }

    // 参数类型不对，例如 id 传了文字，按 400 返回
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletResponse response) {
        log.warn("参数类型不正确: {}={}", e.getName(), e.getValue());
        markDownloadFailed(response);
        return Result.error(400, "参数格式不正确：" + e.getName());
    }

    // 请求体不是合法 JSON，或字段类型不匹配
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e, HttpServletResponse response) {
        log.warn("请求体解析失败: {}", e.getMessage());
        markDownloadFailed(response);
        return Result.error(400, "请求体格式不正确，请检查提交的 JSON");
    }

    // 静态资源和没匹配的路由抛 NoResourceFoundException，按 404 返回
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e, HttpServletResponse response) {
        log.debug("资源不存在: {}", e.getMessage());
        response.setStatus(404);
        return Result.error(404, "请求的资源不存在");
    }

    // 其它异常不暴露细节，统一提示服务器错误
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletResponse response, HttpServletRequest request) {
        log.error("未捕获异常", e);
        markDownloadFailed(response);
        saveException(e, request);
        return Result.error("服务器内部错误，请稍后重试");
    }

    // 数据库异常的判断串，命中就换成安全提示
    private boolean looksLikeDatabaseError(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("### Error querying database") || message.contains("### SQL:")
                || message.contains("SQLException") || message.contains("BadSqlGrammarException")
                || message.contains("DataIntegrityViolationException");
    }

    // 这类报错是调用方传了非法值，按 400 提示
    private boolean badInput(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("Incorrect DATE value") || message.contains("Incorrect integer value")
                || message.contains("Incorrect decimal value") || message.contains("Incorrect datetime value")
                || message.contains("Data truncation");
    }

    // 没有中文或消息很长，就不是业务提示
    private boolean looksLikeBusinessHint(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (message.length() > 100) {
            return false;
        }
        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) >= 0x4e00 && message.charAt(i) <= 0x9fa5) {
                return true;
            }
        }
        return false;
    }

    // 写入异常日志表
    private void saveException(Throwable e, HttpServletRequest request) {
        if (request == null) {
            return;
        }
        try {
            ExceptionLogs entry = new ExceptionLogs();
            Long userId = RequestUser.id(request);
            entry.setUser_id(userId);
            entry.setUsername(usernameOf(userId));
            String query = request.getQueryString();
            String uri = request.getRequestURI();
            entry.setUrl(query == null || query.isEmpty() ? uri : uri + "?" + query);
            entry.setMethod(request.getMethod());
            entry.setIp_address(clientIp(request));
            String userAgent = request.getHeader("User-Agent");
            entry.setBrowser(cut(userAgent, 255));
            entry.setOs(cut(userAgent, 255));
            entry.setScreen_resolution(cut(request.getHeader("X-Screen-Resolution"), 64));
            entry.setException_type(e.getClass().getSimpleName());
            String message = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
            entry.setError_msg(cut(sensitiveDataService.maskText(message), 500));
            entry.setStack_trace(cut(stackOf(e), MAX_STACK_LENGTH));
            entry.setRequest_params(cut(sensitiveDataService.maskText(query), 2000));
            entry.setStatus(0);
            entry.setResolved(0);
            entry.setCreated_at(LocalDateTime.now());
            entry.setUpdated_at(LocalDateTime.now());
            exceptionLogsRepository.insert(entry);
        } catch (Exception ignore) {
            // 记异常时再抛会盖掉原始错误，这里忽略
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

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private String stackOf(Throwable e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private String cut(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    // 下载接口出错时状态改 500，别让前端把错误 JSON 存成文件
    private void markDownloadFailed(HttpServletResponse response) {
        if (response.getHeader("Content-Disposition") != null) {
            response.setStatus(500);
        }
    }

    // 消息为空给通用提示，过长按码点截断，避免切开 emoji
    private String shortMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "操作失败";
        }
        if (message.codePointCount(0, message.length()) <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, message.offsetByCodePoints(0, MAX_MESSAGE_LENGTH));
    }
}
