package com.officeautomationsystemapi.vo;

import lombok.Data;

@Data
public class WorklogCategoryVO {
    private Long id;
    private String category_name;
    private String category_code;
    private String description;
    private Integer sort_order;
    private Integer status;
}
