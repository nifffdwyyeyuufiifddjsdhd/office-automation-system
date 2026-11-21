package com.officeautomationsystemapi.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrgNodeVO {
    private Long id;
    private String label;
    private List<OrgNodeVO> children = new ArrayList<>();
}
