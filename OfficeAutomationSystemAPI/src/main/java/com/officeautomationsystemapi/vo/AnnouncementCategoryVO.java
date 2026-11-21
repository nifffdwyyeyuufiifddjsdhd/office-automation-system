package com.officeautomationsystemapi.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AnnouncementCategoryVO {
    private Long id;
    private Long parent_id;
    private String category_name;
    private String category_code;
    private Integer sort_order;
    private Integer status;
    private Integer view_permission_type;   // 1全部可见 2指定角色可见
    private String icon;
    private List<Long> role_ids = new ArrayList<>();   // 已授权角色ID（view_permission_type=2 时有值）
    private List<AnnouncementCategoryVO> children = new ArrayList<>(); // 子类别
}
