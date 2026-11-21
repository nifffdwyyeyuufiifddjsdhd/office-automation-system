import request from "@/utils/request";

export interface LoginResult {
    token:string;
    mustChangePassword:boolean;
    username:string;
    realName:string;
    role:string[];          // 角色编码列表
    permissions:string[];   // 权限标识列表
}

// 当前用户的角色与权限，进页面拉一次，后台改权不用重登
export function getMyAuth(){
    return request.get<any,{userId:number;role:string[];permissions:string[]}>("/auth/me");
}

// 登录，返回 token 和是否必须改密码
export function login(data:{username:string;password:string}){
    return request.post<any,LoginResult>("/auth/login",data);
}


// 修改密码
export function changePassword(data:{oldPassword:string;newPassword:string}){
    return request.post<any,LoginResult>("/auth/change-password",data);
}

// 发送邮箱验证码，找回密码第一步
export function sendResetCode(data:{username:string;email:string}){
    return request.post<any,any>("/auth/forgot-password/send-code",data);
}

// 验证码加新密码重置密码
export function resetPassword(data:{username:string;email:string;code:string;newPassword:string}){
    return request.post<any,any>("/auth/forgot-password/reset",data);
}

