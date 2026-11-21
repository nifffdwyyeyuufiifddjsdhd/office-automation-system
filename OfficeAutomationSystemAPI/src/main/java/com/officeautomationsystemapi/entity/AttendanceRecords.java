package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("attendance_records")
public class AttendanceRecords {

    // 出勤记录ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long user_id;

    // 部门ID（冗余字段，便于部门统计）
    @TableField("dept_id")
    private Long dept_id;

    // 出勤日期
    @TableField("attendance_date")
    private LocalDate attendance_date;

    // 上班签到时间
    @TableField("check_in_time")
    private LocalDateTime check_in_time;

    // 下班签退时间
    @TableField("check_out_time")
    private LocalDateTime check_out_time;

    // 上班签到IP
    @TableField("check_in_ip")
    private String check_in_ip;

    // 下班签退IP
    @TableField("check_out_ip")
    private String check_out_ip;

    // 上班签到设备MAC地址
    @TableField("check_in_mac")
    private String check_in_mac;

    // 下班签退设备MAC地址
    @TableField("check_out_mac")
    private String check_out_mac;

    // 上班签到地理位置
    @TableField("check_in_location")
    private String check_in_location;

    // 下班签退地理位置
    @TableField("check_out_location")
    private String check_out_location;

    // 出勤状态：0正常 1迟到 2早退 3缺勤 4外出 5出差 6请假
    @TableField("status")
    private Integer status;

    // 工作时长（小时）
    @TableField("work_hours")
    private BigDecimal work_hours;

    // 加班时长（小时）
    @TableField("overtime_hours")
    private BigDecimal overtime_hours;

    // 是否补签：0正常 1补签
    @TableField("is_makeup")
    private Integer is_makeup;

    // 补签申请单ID（关联流程实例）
    @TableField("makeup_apply_id")
    private Long makeup_apply_id;

    // 备注
    @TableField("remark")
    private String remark;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime created_at;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updated_at;

    // 签到次数
    @TableField("check_in_count")
    private Integer check_in_count;

    // 签退次数
    @TableField("check_out_count")
    private Integer check_out_count;

    // 纬度
    @TableField("latitude")
    private BigDecimal latitude;

    // 经度
    @TableField("longitude")
    private BigDecimal longitude;

    // 打卡类型 normal/出差/外出
    @TableField("check_type")
    private String check_type;

    // 班次 day白班 night夜班 flex弹性
    @TableField("shift_type")
    private String shift_type;

    // 地理异常：0正常 1异地或定位失败待审核 2主管确认 3驳回记缺勤
    @TableField("geo_abnormal")
    private Integer geo_abnormal;

    // 员工姓名（联表补充字段，非数据库列）
    @TableField(exist = false)
    private String real_name;

    @TableLogic
    // 删除时间（软删除）
    @TableField("deleted_at")
    private LocalDateTime deleted_at;
}