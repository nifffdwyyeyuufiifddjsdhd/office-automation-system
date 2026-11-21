package com.officeautomationsystemapi.controller;

import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.service.OnlineUserService;
import com.officeautomationsystemapi.service.PermissionService;
import com.officeautomationsystemapi.service.UserService;
import com.officeautomationsystemapi.service.impl.ForgetPasswordService;
import com.officeautomationsystemapi.utilt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

// 认证接口：登录、改密、找回密码、退出
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private ForgetPasswordService forgetPasswordService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private OnlineUserService onlineUserService;

    // 登录成功才发 token，账号删除或停用则拒绝
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body,
                                             HttpServletRequest request) {
        if (body == null) {
            return Result.error(1001, "用户名或密码错误");
        }
        Users user;
        try {
            user = userService.login(body.get("username"), body.get("password"), request.getRemoteAddr());
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? "用户名或密码错误" : e.getMessage();
            return Result.error(1001, message);
        }
        if (user == null) {
            return Result.error(1001, "用户名或密码错误");
        }
        if (user.getDeleted_at() != null || (user.getStatus() != null && user.getStatus() != 1)) {
            return Result.error(1001, "账号已停用或删除，请联系管理员");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        onlineUserService.recordLogin(user.getId(), request.getRemoteAddr(), request.getHeader("User-Agent"));

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);                       //token凭证
        data.put("userId", user.getId());
        data.put("mustChangePassword", user.getMust_change_password() != null && user.getMust_change_password() == 1);
        data.put("username", user.getUsername());
        data.put("realName", user.getReal_name());
        data.put("role", permissionService.getUserRoleCodes(user.getId()));             //查角色
        data.put("permissions", permissionService.getUserPermissions(user.getId()));    //查权限

        //返回Result返回
        return Result.success(data);
    }

    // 当前用户的角色和权限，前端进页面时拉一次
    @GetMapping("/me")
    public Result<Map<String, Object>> me(HttpServletRequest request) {     //获取当前用户信息
        Long userId = RequestUser.id(request);          //解析当前用户ID
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("role", permissionService.getUserRoleCodes(userId));
        data.put("permissions", permissionService.getUserPermissions(userId));
        return Result.success(data);
    }

    // 登录用户修改自己的密码
    @PostMapping("/change-password")
    @RequirePermission("user:change-pwd")     //自定义注解，只有user:change-pwd权限的，才允许调用否则方法在执行前会被拦住
    public Result<Void> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        userService.changePassword(RequestUser.id(request), body.get("oldPassword"), body.get("newPassword"));
        return Result.success(null);
    }

    // 找回密码第一步，向绑定邮箱发验证码
    @PostMapping("/forgot-password/send-code")
    public Result<Void> sendResetCode(@RequestBody Map<String, String> body) {
        forgetPasswordService.sendCode(body.get("username"), body.get("email"));
        return Result.success(null);
    }

    // 找回密码第二步，校验验证码后重置密码
    @PostMapping("/forgot-password/reset")
    public Result<Void> resetPassword(@RequestBody Map<String, String> body) {
        forgetPasswordService.resetPassword(body.get("username"), body.get("email"),
                body.get("code"), body.get("newPassword"));
        return Result.success(null);
    }

    // 退出登录，记最后在线时间并清掉在线状态
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        userService.logout(userId);
        onlineUserService.logout(userId);
        return Result.success(null);
    }
}
