package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("holidays")
public class Holidays {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 年份
    @TableField("year")
    private Integer year;

    // 节假日日期
    @TableField("holiday_date")
    private LocalDate holiday_date;

    // 节假日名称
    @TableField("holiday_name")
    private String holiday_name;

    // 类型：1法定假日 2调休上班日 3周末
    @TableField("holiday_type")
    private Integer holiday_type;

    // 是否工作日：0否 1是（调休上班日为1）
    @TableField("is_workday")
    private Integer is_workday;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime created_at;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updated_at;

    @TableLogic
    // 删除时间（软删除）
    @TableField("deleted_at")
    private LocalDateTime deleted_at;
}