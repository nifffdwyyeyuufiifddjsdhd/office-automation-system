
import { hasPerm } from '@/utils/permission';

export type MenuLocation = 'sidebar' | 'header' | 'both';

export interface MenuItem {
    title: string;
    path?: string;               // 叶子节点才有
    icon?: string;               // 图标名（字符串，组件里动态映射）
    children?: MenuItem[];       // 有 children 时渲染为子菜单
    group?: boolean;
    location?: MenuLocation;     // 显示位置，默认 'sidebar'
    component?: string;
    perm?: string;               // 该菜单项所需权限标识；不填=所有登录用户可见
}

// 流程子项，多角色共用，perm 自动过滤
const flowChildren: MenuItem[] = [
    { title: '发起流程', path: '/home/flow-start' },
    { title: '待办流程', path: '/home/flow-todo' },
    { title: '已办流程', path: '/home/flow-done' },
    { title: '审批委托', path: '/home/flow-delegate' },
    { title: '我发起的', path: '/home/flow-mine' },
    { title: '所有流程', path: '/home/flow-all', perm: 'flow:all:view' },
    { title: '流程定义', path: '/home/flow-definition', perm: 'flow:def:manage' },
];

// 招聘子项，人事和 admin 共用，perm 自动过滤
const recruitmentPositionMenu: MenuItem = { title: '招聘岗位', path: '/home/recruitment/position', perm: 'recruitment:position:list' };
const recruitmentChildren: MenuItem[] = [
    recruitmentPositionMenu,
    { title: '候选人管理', path: '/home/recruitment/candidate', perm: 'recruitment:candidate:list' },
];

// 系统管理员
export const adminMenus: MenuItem[] = [
    { title: '首页', path: '/home', icon: 'HomeFilled' },
    { title: '用户管理', path: '/home/user', icon: 'User', perm: 'user:list' },
    { title: '公告通知', path: '/home/announcement', icon: 'Bell', perm: 'announcement:list' },
    { title: '工作日志', path: '/home/worklog', icon: 'Notebook' },
    { title: '流程管理', icon: 'Connection', children: flowChildren },
    { title: '出勤管理', path: '/home/attendance', icon: 'Clock' },
    { title: '在线用户', path: '/home/online', icon: 'Monitor' },
    { title: '任务中心', path: '/home/task', icon: 'List', perm: 'task:access' },
    { title: '项目管理', path: '/home/project', icon: 'Folder', perm: 'project:list' },
    { title: '统计报表', path: '/home/report', icon: 'DataAnalysis' },
    { title: '招聘管理', icon: 'Suitcase', children: recruitmentChildren },
    { title: '登录日志', path: '/home/system/login-log', icon: 'Tickets', perm: 'login:stat' },
    { title: '权限管理', path: '/home/system/role', icon: 'Lock', perm: 'role:manage' },
    { title: '系统管理', path: '/home/system/manage', icon: 'Setting', perm: 'system:param' },
    {
        title: '财务管理',
        icon: 'Money',
        children: [
            { title: '付款管理', path: '/home/finance/payment', icon: 'Wallet', perm: 'finance:pay' },
            { title: '财务统计', path: '/home/finance/stat', icon: 'DataAnalysis', perm: 'finance:stat' },
            { title: '费用类别', path: '/home/finance/category', icon: 'Files', perm: 'finance:category:list' },
        ],
    },
];

// 部门经理
export const managerMenus: MenuItem[] = [
    { title: '首页', path: '/home', icon: 'HomeFilled' },
    { title: '公告通知', path: '/home/announcement', icon: 'Bell', perm: 'announcement:list' },
    { title: '工作日志', path: '/home/worklog', icon: 'Notebook' },
    { title: '流程管理', icon: 'Connection', children: flowChildren },
    { title: '出勤管理', path: '/home/attendance', icon: 'Clock' },
    { title: '任务中心', path: '/home/task', icon: 'List', perm: 'task:access' },
];

// 普通员工
// 统计报表只给管理员和总经理（TABLE 1 / TABLE 2）
const reportMenu: MenuItem = { title: '统计报表', path: '/home/report', icon: 'DataAnalysis', perm: 'report:view' };

export const staffMenus: MenuItem[] = [
    { title: '首页', path: '/home', icon: 'HomeFilled' },
    { title: '公告通知', path: '/home/announcement', icon: 'Bell', perm: 'announcement:list' },
    { title: '工作日志', path: '/home/worklog', icon: 'Notebook' },
    { title: '流程管理', icon: 'Connection', children: flowChildren },
    { title: '出勤管理', path: '/home/attendance', icon: 'Clock' },
    // 在线用户：TABLE 9 要求员工能看名单，后端只给脱敏字段
    { title: '在线用户', path: '/home/online', icon: 'Monitor', perm: 'online:view' },
    { title: '任务中心', path: '/home/task', icon: 'List', perm: 'task:access' },
];

// 人事专员 hr：日常办公加用户管理菜单
export const hrMenus: MenuItem[] = staffMenus.flatMap((m) =>
    m.path === '/home'
        ? [
            m,
            { title: '用户管理', path: '/home/user', icon: 'User', perm: 'user:list' },
            // 人事专员只维护招聘岗位，候选人与登录日志归人事主管
            { title: '招聘管理', icon: 'Suitcase', children: [recruitmentPositionMenu] },
        ]
        : [m]
);

// 人事主管：按 TABLE 4，用户组织、全公司出勤、招聘、登录日志
// 任务中心、报表、在线用户只给管理员和总经理，不放这里
export const hrManagerMenus: MenuItem[] = [
    { title: '首页', path: '/home', icon: 'HomeFilled' },
    { title: '用户管理', path: '/home/user', icon: 'User', perm: 'user:list' },
    { title: '公告通知', path: '/home/announcement', icon: 'Bell', perm: 'announcement:list' },
    { title: '工作日志', path: '/home/worklog', icon: 'Notebook' },
    { title: '流程管理', icon: 'Connection', children: flowChildren },
    { title: '出勤管理', path: '/home/attendance', icon: 'Clock' },
    { title: '招聘管理', icon: 'Suitcase', children: recruitmentChildren },
    { title: '登录日志', path: '/home/system/login-log', icon: 'Tickets', perm: 'login:stat' },
];

// 总经理 gm：全局数据查看加日常办公，没有系统配置
export const gmMenus: MenuItem[] = [
    { title: '首页', path: '/home', icon: 'HomeFilled' },
    { title: '公告通知', path: '/home/announcement', icon: 'Bell', perm: 'announcement:list' },
    { title: '工作日志', path: '/home/worklog', icon: 'Notebook' },
    { title: '流程管理', icon: 'Connection', children: flowChildren },
    { title: '出勤管理', path: '/home/attendance', icon: 'Clock' },
    { title: '任务中心', path: '/home/task', icon: 'List', perm: 'task:access' },
    reportMenu,
    // 金额只给财务和总经理看，这里只读统计，付款导出归财务
    {
        title: '财务管理',
        icon: 'Money',
        children: [
            { title: '财务统计', path: '/home/finance/stat', icon: 'DataAnalysis', perm: 'finance:stat' },
        ],
    },
];

// 财务专员 finance：日常办公加付款，没有用户管理
export const financeMenus: MenuItem[] = [
    { title: '首页', path: '/home', icon: 'HomeFilled' },
    { title: '公告通知', path: '/home/announcement', icon: 'Bell', perm: 'announcement:list' },
    { title: '工作日志', path: '/home/worklog', icon: 'Notebook' },
    { title: '流程管理', icon: 'Connection', children: flowChildren },
    { title: '出勤管理', path: '/home/attendance', icon: 'Clock' },
    // 财务整组都挂权限码，没权限码连目录都看不到
    {
        title: '财务管理',
        icon: 'Money',
        children: [
            { title: '付款管理', path: '/home/finance/payment', icon: 'Wallet', perm: 'finance:pay' },
            { title: '财务统计', path: '/home/finance/stat', icon: 'DataAnalysis', perm: 'finance:stat' },
            { title: '费用类别', path: '/home/finance/category', icon: 'Files', perm: 'finance:category:list' },
        ],
    },
    { title: '任务中心', path: '/home/task', icon: 'List', perm: 'task:access' },
];

// 项目经理：任务中心、项目管理、项目流程审批
const projectMenu: MenuItem = { title: '项目管理', path: '/home/project', icon: 'Folder', perm: 'project:list' };

export const pmMenus: MenuItem[] = [
    { title: '首页', path: '/home', icon: 'HomeFilled' },
    projectMenu,
    { title: '公告通知', path: '/home/announcement', icon: 'Bell', perm: 'announcement:list' },
    { title: '工作日志', path: '/home/worklog', icon: 'Notebook' },
    { title: '流程管理', icon: 'Connection', children: flowChildren },
    { title: '出勤管理', path: '/home/attendance', icon: 'Clock' },
    { title: '任务中心', path: '/home/task', icon: 'List', perm: 'task:access' },
];

// 访客：只有对外公告一个入口
export const guestMenus: MenuItem[] = [
    { title: '公开公告', path: '/home/public-announcement', icon: 'Bell', perm: 'announcement:view-public' },
];

// 按角色取菜单
export function getMenusByRole(role: string): MenuItem[] {
    if (role === 'admin') return adminMenus;
    if (role === 'manager') return managerMenus;
    if (role === 'hr') return hrMenus;
    if (role === 'hr_manager') return hrManagerMenus;
    if (role === 'gm') return gmMenus;
    if (role === 'finance') return financeMenus;
    if (role === 'project_manager') return pmMenus;
    if (role === 'guest') return guestMenus;
    return staffMenus;
}

// 多角色合并：一个人可能有多个角色，菜单取并集
function mergeMenus(list: MenuItem[]): MenuItem[] {
    const byKey = new Map<string, MenuItem>();
    const order: string[] = [];
    for (const m of list) {
        const key = m.path || m.title;
        const exist = byKey.get(key);
        if (!exist) {
            byKey.set(key, { ...m, children: m.children ? mergeMenus(m.children) : undefined });
            order.push(key);
            continue;
        }
        // 同一菜单被多个角色列出时合并子节点，权限码取严的
        const mergedChildren = m.children && m.children.length
            ? mergeMenus([...(exist.children || []), ...m.children])
            : exist.children;
        byKey.set(key, { ...exist, perm: exist.perm || m.perm, children: mergedChildren });
    }
    return order.map((k) => byKey.get(k)!);
}

export function getMenusByRoles(roles: string[]): MenuItem[] {
    const list = roles && roles.length ? roles : ['employee'];
    return mergeMenus(list.flatMap((r) => getMenusByRole(r)));
}

// 递归过滤权限
function filterByPerm(menus: MenuItem[]): MenuItem[] {
    const out: MenuItem[] = [];
    for (const m of menus) {
        // 自己没权限且没有可见子节点就整个隐藏
        const selfOk = !m.perm || hasPerm(m.perm);
        const childrenOk = m.children ? filterByPerm(m.children) : undefined;
        if (!selfOk) {
            // 父菜单自己没权限，但有一个子项可见就保留
            if (m.children && childrenOk && childrenOk.length > 0) {
                out.push({ ...m, children: childrenOk });
            }
            continue;
        }
        out.push({ ...m, children: childrenOk });
    }
    return out;
}

// 侧边栏菜单（location 为 sidebar 或 both）
export function getSidebarMenus(role: string): MenuItem[] {
    return filterByPerm(getMenusByRole(role)).filter(
        (m) => m.location !== 'header'
    );
}

// 多角色侧边栏取并集，不能只看主角色
export function getSidebarMenusByRoles(roles: string[]): MenuItem[] {
    return filterByPerm(getMenusByRoles(roles)).filter(
        (m) => m.location !== 'header'
    );
}

// 白色头部组件（location 为 header 或 both）
export function getHeaderMenus(role: string): MenuItem[] {
    return filterByPerm(getMenusByRole(role)).filter(
        (m) => m.location !== 'sidebar'
    );
}
