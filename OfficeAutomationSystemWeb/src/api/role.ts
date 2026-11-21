import request from "@/utils/request";

export interface RoleItem {
    id: number;
    role_code: string;
    role_name: string;
    description?: string;
    data_scope: number;
    dataScopeText?: string;
    role_type?: number;
    sort_order?: number;
    status: number;
    is_system: number;
    user_count?: number;
    perm_count?: number;
    created_at?: string;
}

export interface RoleDetail extends RoleItem {
    menuIds: number[];
}

export interface RoleMenuNode {
    id: number;
    parentId: number;
    label: string;
    menuCode?: string;
    menuType: number;
    children: RoleMenuNode[];
}

export interface RoleForm {
    id?: number;
    role_code?: string;
    role_name?: string;
    description?: string;
    data_scope?: number;
    sort_order?: number;
    status?: number;
}

// 角色列表
export function listRoles(params: { keyword?: string; status?: number | null }) {
    return request.get<any, RoleItem[]>("/roles", { params });
}

// 下拉可选角色（管理员、人事、入职办理共用）
export function roleOptions() {
    return request.get<any, { id: number; role_code: string; role_name: string }[]>("/roles/options");
}

export function getRoleDetail(id: number) {
    return request.get<any, RoleDetail>("/roles/" + id);
}

export function createRole(data: RoleForm) {
    return request.post<any, { id: number }>("/roles", data);
}

export function updateRole(id: number, data: RoleForm) {
    return request.put<any, null>("/roles/" + id, data);
}

export function deleteRole(id: number) {
    return request.delete<any, null>("/roles/" + id);
}

// 全量保存角色的菜单与按钮权限
export function saveRolePermissions(id: number, menuIds: number[]) {
    return request.put<any, { count: number }>("/roles/" + id + "/permissions", { menuIds });
}

// 权限勾选树
export function roleMenuTree() {
    return request.get<any, RoleMenuNode[]>("/roles/menus/tree");
}

// 角色成员
export function listRoleUsers(id: number) {
    return request.get<any, { id: number; username: string; real_name: string; org_name?: string; status: number }[]>("/roles/" + id + "/users");
}

// 用户当前角色
export function getUserRoles(userId: number) {
    return request.get<any, { roleIds: number[]; roles: RoleItem[] }>("/roles/user/" + userId);
}

// 覆盖式保存用户角色
export function saveUserRoles(userId: number, roleIds: number[]) {
    return request.put<any, { count: number }>("/roles/user/" + userId, { roleIds });
}
