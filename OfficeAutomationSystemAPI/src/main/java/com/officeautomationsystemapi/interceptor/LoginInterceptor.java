package com.officeautomationsystemapi.interceptor;

import com.alibaba.fastjson2.JSON;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.OnlineUserService;
import com.officeautomationsystemapi.service.PermissionService;
import com.officeautomationsystemapi.utilt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

// 校验请求头里的 token、账号状态和接口权限
@Component
public class LoginInterceptor implements HandlerInterceptor {

    //约定：前端请求时，必须带Authorization头
    private static final String AUTH_HEADER = "Authorization";
    //约定的token前缀：一般是”Bearer token“
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private OnlineUserService onlineUserService;
    @Autowired
    private UsersRepository usersRepository;        //查询数据库

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检不带 token，先放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        //从请求中获取token
        String token = request.getHeader(AUTH_HEADER);
        //没有Token/不是Bearer 开头就打回去
        if (token == null || !token.startsWith(BEARER_PREFIX)) {
            return writeError(response, 401, "未登录，请先登录");
        }

        //先把"Bearer\n"这七个字符切掉后，只保留后面的token
        token = token.substring(BEARER_PREFIX.length());

        Long userId;
        try {

            Claims claims = jwtUtil.parseToken(token);      //调用JWTUtil验证和解析
            userId = Long.valueOf(claims.getSubject());     //从载荷中取出userId
        } catch (Exception e) {
            //token被篡改、过期，就会抛出异常
            return writeError(response, 401, "token无效已过期");
        }

        // 账号删除或停用后，未过期的 token 也要失效
        Users user = usersRepository.selectById(userId);
        if (user == null || (user.getStatus() != null && user.getStatus() != 1)) {
            return writeError(response, 401, "账号已停用或删除，请重新登录");
        }

        //之后获取userId直接通过RequestUser.id(request),不用再访问数据库
        request.setAttribute("userId", userId);

        //handler表示当前请求的摇调用的controller方法
        if (handler instanceof HandlerMethod method) {
            //看看该方法有没有@RequirePermission注解
            RequirePermission required = method.getMethodAnnotation(RequirePermission.class);
            //假如方法上没有，看看这个类上面有没有
            if (required == null) {
                required = method.getBeanType().getAnnotation(RequirePermission.class);
            }

            //如果找到的了权限要求，而且当前用户不是超级管理员
            if (required != null && !permissionService.isAdmin(userId)) {
                //查看当前用户拥有的全部权限
                Set<String> permissions = permissionService.getUserPermissions(userId);
                //看看用户拥有的权限里，是否包含了注解里要求的任意一个权限
                boolean allowed = Arrays.stream(required.value()).anyMatch(permissions::contains);
                if (!allowed) {
                    return writeError(response, 403, "无权限访问");
                }
            }
        }

        //刷新用户的在线活跃时间
        onlineUserService.refreshActive(userId);

        //全部通过，放行
        return true;
    }

    // 写 JSON 到响应并返回 false，中断请求
    private boolean writeError(HttpServletResponse response, Integer code, String message) throws Exception {
        //设置HTTP状态码
        response.setStatus(code);
        //设置返回JSON格式
        response.setContentType("application/json;charset=UTF-8");
        //把Result对象转成JSON字符串，直接写到响应流里返回给前端
        response.getWriter().write(JSON.toJSONString(Result.error(code, message)));
        //中断请求
        return false;
    }
}
