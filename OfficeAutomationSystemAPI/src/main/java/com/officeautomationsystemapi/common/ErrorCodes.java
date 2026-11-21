package com.officeautomationsystemapi.common;

// 业务错误码，四位数字，前端按码判断
// 号段：1001 用户账号，2001 流程审批，3001 出勤，4001 任务项目
// 业务失败回 200 加错误码；401 未登录，403 无权限，429 限流
public final class ErrorCodes {

    private ErrorCodes() {
    }

    // 用户名或密码错误
    public static final int USER_BAD_CREDENTIALS = 1001;
    // 账号已停用或删除
    public static final int USER_DISABLED = 1002;
    // 账号被锁定
    public static final int USER_LOCKED = 1003;
    // 账号不存在
    public static final int USER_NOT_FOUND = 1004;

    // 当前不是你的待办
    public static final int FLOW_NOT_YOUR_TODO = 2001;
    // 流程状态不允许该操作
    public static final int FLOW_ILLEGAL_STATE = 2002;
    // 流程没有可用审批人
    public static final int FLOW_NO_APPROVER = 2003;

    // 出勤状态不允许该操作
    public static final int ATTENDANCE_ILLEGAL_STATE = 3001;
    // 补签次数已达上限
    public static final int ATTENDANCE_MAKEUP_LIMIT = 3002;

    // 任务状态不允许该操作
    public static final int TASK_ILLEGAL_STATE = 4001;
    // 任务已归档只读
    public static final int TASK_ARCHIVED = 4002;
}
