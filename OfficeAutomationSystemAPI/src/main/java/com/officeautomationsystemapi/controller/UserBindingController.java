package com.officeautomationsystemapi.controller;

import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.service.UserThirdPartyBindingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// 个人中心，企业微信和钉钉账号绑定
@RestController
@RequestMapping("/api/profile")
public class UserBindingController {

    @Autowired
    private UserThirdPartyBindingService bindingService;

    // 我绑定的平台，企业微信和钉钉各一行
    @GetMapping("/bindings")
    @RequirePermission("user:profile:edit")
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        return Result.success(bindingService.listMine(RequestUser.id(request)));
    }

    // 绑定或改绑
    @PostMapping("/bindings")
    @RequirePermission("user:profile:edit")
    public Result<Map<String, Object>> bind(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object platformValue = body.get("platform");
        String platform = platformValue == null ? null : String.valueOf(platformValue);
        String externalId = body.get("externalId") == null ? null : String.valueOf(body.get("externalId"));
        String externalName = body.get("externalName") == null ? null : String.valueOf(body.get("externalName"));
        return Result.success(bindingService.bind(RequestUser.id(request), platform, externalId, externalName));
    }

    // 解绑
    @DeleteMapping("/bindings/{platform}")
    @RequirePermission("user:profile:edit")
    public Result<Void> unbind(@PathVariable String platform, HttpServletRequest request) {
        bindingService.unbind(RequestUser.id(request), platform);
        return Result.success(null);
    }
}
