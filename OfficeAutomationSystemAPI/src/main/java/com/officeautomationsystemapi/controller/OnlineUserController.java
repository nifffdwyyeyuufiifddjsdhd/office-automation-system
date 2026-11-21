package com.officeautomationsystemapi.controller;

import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.service.OnlineUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 在线用户接口，右上角人数和在线列表
@RestController
@RequestMapping("/api/online")
public class OnlineUserController {

    @Autowired
    private OnlineUserService onlineUserService;

    // 在线人数，登录即可看，不限权限
    @GetMapping("/count")
    public Result<Map<String, Object>> count() {
        Map<String, Object> data = new HashMap<>();
        data.put("count", onlineUserService.count());
        return Result.success(data);
    }

    // 在线明细含 IP、浏览器和部门，需要 system:online 权限，管理员放行
    @GetMapping("/list")
    @RequirePermission("system:online")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long deptId,
                                                  @RequestParam(required = false) String keyword) {
        return Result.success(onlineUserService.list(deptId, keyword));
    }

    // 用户详情和最近操作记录
    @GetMapping("/{userId}")
    @RequirePermission("system:online")
    public Result<Map<String, Object>> detail(@PathVariable Long userId) {
        Map<String, Object> data = onlineUserService.detail(userId);
        if (data == null) {
            return Result.error("用户不存在");
        }
        return Result.success(data);
    }

    // 基础在线名单，只返回姓名、部门和时间，不含 IP 和设备
    @GetMapping("/directory")
    @RequirePermission("online:view")
    public Result<List<Map<String, Object>>> directory(@RequestParam(required = false) Long deptId,
                                                       @RequestParam(required = false) String keyword) {
        return Result.success(onlineUserService.directory(deptId, keyword));
    }
}
