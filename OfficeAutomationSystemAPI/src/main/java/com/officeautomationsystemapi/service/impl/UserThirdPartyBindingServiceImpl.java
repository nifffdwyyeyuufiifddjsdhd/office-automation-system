package com.officeautomationsystemapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.entity.UserThirdPartyBindings;
import com.officeautomationsystemapi.repository.UserThirdPartyBindingsRepository;
import com.officeautomationsystemapi.service.UserThirdPartyBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 第三方账号绑定：只存账号标识，不存任何第三方密码或令牌
@Service
public class UserThirdPartyBindingServiceImpl implements UserThirdPartyBindingService {

    private static final String WECOM = "wecom";
    private static final String DINGTALK = "dingtalk";

    @Autowired
    private UserThirdPartyBindingsRepository bindingsRepository;

    @Override
    public List<Map<String, Object>> listMine(Long userId) {
        Map<String, UserThirdPartyBindings> owned = new LinkedHashMap<>();
        List<UserThirdPartyBindings> rows = bindingsRepository.selectList(new QueryWrapper<UserThirdPartyBindings>()
                .eq("user_id", userId)
                .eq("status", 1));
        for (UserThirdPartyBindings row : rows) {
            owned.put(row.getPlatform(), row);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(row(WECOM, owned.get(WECOM)));
        result.add(row(DINGTALK, owned.get(DINGTALK)));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> bind(Long userId, String platform, String externalId, String externalName) {
        String p = normalizePlatform(platform);
        String external = externalId == null ? null : externalId.trim();
        if (external == null || external.isEmpty()) {
            throw new RuntimeException("请填写" + platformName(p) + "账号标识");
        }
        if (external.length() > 64) {
            throw new RuntimeException("账号标识不能超过 64 个字符");
        }
        String name = externalName == null ? null : externalName.trim();
        if (name != null && name.isEmpty()) {
            name = null;
        }
        if (name != null && name.length() > 50) {
            throw new RuntimeException("显示名不能超过 50 个字符");
        }
        // 同一个第三方账号不能挂在两个人名下，否则消息推错人
        Long taken = bindingsRepository.selectCount(new QueryWrapper<UserThirdPartyBindings>()
                .eq("platform", p)
                .eq("external_id", external)
                .eq("status", 1)
                .ne("user_id", userId));
        if (taken != null && taken > 0) {
            throw new RuntimeException("该" + platformName(p) + "账号已被其他同事绑定，请核对后重试");
        }

        LocalDateTime now = LocalDateTime.now();
        UserThirdPartyBindings exist = bindingsRepository.selectOne(new QueryWrapper<UserThirdPartyBindings>()
                .eq("user_id", userId)
                .eq("platform", p)
                .last("LIMIT 1"));
        if (exist == null || Integer.valueOf(0).equals(exist.getStatus()) && exist.getDeleted_at() != null) {
            UserThirdPartyBindings row = new UserThirdPartyBindings();
            row.setUser_id(userId);
            row.setPlatform(p);
            row.setExternal_id(external);
            row.setExternal_name(name);
            row.setStatus(1);
            row.setBind_time(now);
            row.setCreated_at(now);
            row.setUpdated_at(now);
            bindingsRepository.insert(row);
            return row(p, row);
        }
        exist.setExternal_id(external);
        exist.setExternal_name(name);
        exist.setStatus(1);
        exist.setBind_time(now);
        exist.setUpdated_at(now);
        bindingsRepository.updateById(exist);
        return row(p, exist);
    }

    @Override
    @Transactional
    public void unbind(Long userId, String platform) {
        String p = normalizePlatform(platform);
        UserThirdPartyBindings exist = bindingsRepository.selectOne(new QueryWrapper<UserThirdPartyBindings>()
                .eq("user_id", userId)
                .eq("platform", p)
                .last("LIMIT 1"));
        if (exist == null) {
            throw new RuntimeException("还没有绑定" + platformName(p) + "账号");
        }
        // 逻辑删除而不是改状态，历史绑定记录留档
        bindingsRepository.deleteById(exist.getId());
    }

    // 平台编码只认这两个，其他值直接拒绝，避免脏数据进库
    private String normalizePlatform(String platform) {
        if (WECOM.equals(platform)) return WECOM;
        if (DINGTALK.equals(platform)) return DINGTALK;
        throw new RuntimeException("只能绑定企业微信或钉钉");
    }

    private String platformName(String platform) {
        return WECOM.equals(platform) ? "企业微信" : "钉钉";
    }

    private Map<String, Object> row(String platform, UserThirdPartyBindings binding) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("platform", platform);
        item.put("platformName", platformName(platform));
        item.put("bound", binding != null && Integer.valueOf(1).equals(binding.getStatus()));
        item.put("externalId", binding == null ? null : binding.getExternal_id());
        item.put("externalName", binding == null ? null : binding.getExternal_name());
        item.put("bindTime", binding == null ? null : binding.getBind_time());
        return item;
    }
}
