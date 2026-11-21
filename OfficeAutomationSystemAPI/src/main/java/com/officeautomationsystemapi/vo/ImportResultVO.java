package com.officeautomationsystemapi.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResultVO {
    private Integer success = 0;
    private List<String> errors = new ArrayList<>();
}
