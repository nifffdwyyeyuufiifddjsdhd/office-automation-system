package com.officeautomationsystemapi.service;

import java.util.List;
import java.util.Map;

// 个人中心第三方账号绑定：企业微信、钉钉
public interface UserThirdPartyBindingService {

    // 我绑定过的平台，没绑的也返回一条空记录
    List<Map<String, Object>> listMine(Long userId);

    // 绑定或改绑，同一平台只保留一条有效绑定
    Map<String, Object> bind(Long userId, String platform, String externalId, String externalName);

    // 解绑，解绑后可以重新绑别的账号
    void unbind(Long userId, String platform);
}
