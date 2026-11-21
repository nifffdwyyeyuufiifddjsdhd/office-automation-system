import {createWebHashHistory,createRouter,type RouteRecordRaw}from "vue-router";
import { useUserStore } from '@/store/user';

declare module 'vue-router'
{
    interface RouteMeta
    {
        requiresAuth?:boolean;
        roles?:string[];
        guest?:boolean;
    }
}

// 页面路由
const frontRoutes: RouteRecordRaw[]=[
    {
        path:'/',
        redirect:'/home',
    },
    {
        path:'/login',
        name:'Login',
        component:()=>import('@/views/login/LoginView.vue'),
        meta:{guest:true}
    },
    {
        path:"/change-password",
        name:"ChangePassword",
        component:()=>import('@/views/ChangePassword.vue'),
        meta:{requiresAuth:true}
    },
    {
        path:"/forgot-password",
        name:"ForgotPassword",
        component:()=>import('@/views/ForgotPassword.vue'),
        meta:{guest:true}
    },
    {
        path:'/home',
        name:'Home',
        component:()=>import('@/views/frontend/Home.vue'),
        meta:{requiresAuth:true},
        children:[
            {
                path:'',
                name:'Workbench',
                component:()=>import('@/views/frontend/Workbench.vue'),
                meta:{requiresAuth:true,title:'首页'}
            },
            {
                path:'profile',
                name:'Profile',
                component:()=>import('@/views/profile/index.vue'),
                meta:{requiresAuth:true,title:'个人中心'}
            },
            {
                path:'announcement',
                name:'Announcement',
                component:()=>import('@/views/announcement/index.vue'),
                meta:{requiresAuth:true,title:'公告通知'}
            },
            {
                // 访客账号登录后只看对外公告，不暴露任何内部模块
                path:'public-announcement',
                name:'PublicAnnouncement',
                component:()=>import('@/views/announcement/public.vue'),
                meta:{requiresAuth:true,title:'公开公告'}
            },
            {
                path:'user',
                name:'UserManage',
                component:()=>import('@/views/user/index.vue'),
                meta:{requiresAuth:true,title:'用户管理'}
            },
            {
                path:'worklog',
                name:'Worklog',
                component:()=>import('@/views/worklog/index.vue'),
                meta:{requiresAuth:true,title:'工作日志'}
            },
            {
                path:'flow-definition',
                name:'FlowDefinition',
                component:()=>import('@/views/flow/DefinitionList.vue'),
                meta:{requiresAuth:true,title:'流程定义'}
            },
            {
                path:'flow-definition-editor',
                name:'FlowDefinitionEditor',
                component:()=>import('@/views/flow/DefinitionEditor.vue'),
                meta:{requiresAuth:true,title:'流程定义编辑'}
            },
            {
                path:'flow-start',
                name:'FlowStart',
                component:()=>import('@/views/flow/Start.vue'),
                meta:{requiresAuth:true,title:'发起流程'}
            },
            {
                path:'flow-todo',
                name:'FlowTodo',
                component:()=>import('@/views/flow/Todo.vue'),
                meta:{requiresAuth:true,title:'待办流程'}
            },
            {
                path:'flow-delegate',
                name:'FlowDelegate',
                component:()=>import('@/views/flow/Delegates.vue'),
                meta:{requiresAuth:true,title:'审批委托'}
            },
            {
                path:'flow-done',
                name:'FlowDone',
                component:()=>import('@/views/flow/Done.vue'),
                meta:{requiresAuth:true,title:'已办流程'}
            },
            {
                path:'flow-mine',
                name:'FlowMine',
                component:()=>import('@/views/flow/Mine.vue'),
                meta:{requiresAuth:true,title:'我发起的'}
            },
            {
                path:'flow-detail',
                name:'FlowDetail',
                component:()=>import('@/views/flow/Detail.vue'),
                meta:{requiresAuth:true,title:'流程详情'}
            },
            {
                path:'flow-all',
                name:'FlowAll',
                component:()=>import('@/views/flow/AllFlows.vue'),
                meta:{requiresAuth:true,title:'所有流程'}
            },
            {
                path:'attendance',
                name:'Attendance',
                component:()=>import('@/views/attendance/index.vue'),
                meta:{requiresAuth:true,title:'出勤管理'}
            },
            {
                path:'online',
                name:'OnlineUsers',
                component:()=>import('@/views/system/online.vue'),
                meta:{requiresAuth:true,title:'在线用户'}
            },
            {
                path:'project',
                name:'ProjectManage',
                component:()=>import('@/views/project/index.vue'),
                meta:{requiresAuth:true,title:'项目管理'}
            },
            {
                path:'task',
                name:'TaskCenter',
                component:()=>import('@/views/task/index.vue'),
                meta:{requiresAuth:true,title:'任务中心'}
            },
            {
                path:'report',
                name:'Report',
                component:()=>import('@/views/report/index.vue'),
                meta:{requiresAuth:true,title:'统计报表'}
            },
            {
                path:'finance/payment',
                name:'FinancePayment',
                component:()=>import('@/views/finance/index.vue'),
                meta:{requiresAuth:true,title:'付款管理'}
            },
            {
                path:'finance/stat',
                name:'FinanceStat',
                component:()=>import('@/views/finance/stat/index.vue'),
                meta:{requiresAuth:true,title:'财务统计'}
            },
            {
                path:'finance/category',
                name:'FinanceCategory',
                component:()=>import('@/views/finance/category/index.vue'),
                meta:{requiresAuth:true,title:'费用类别'}
            },
            {
                path:'recruitment/position',
                name:'RecruitmentPosition',
                component:()=>import('@/views/recruitment/position/index.vue'),
                meta:{requiresAuth:true,title:'招聘岗位'}
            },
            {
                path:'recruitment/candidate',
                name:'RecruitmentCandidate',
                component:()=>import('@/views/recruitment/candidate/index.vue'),
                meta:{requiresAuth:true,title:'候选人管理'}
            },
            {
                path:'system/login-log',
                name:'LoginLog',
                component:()=>import('@/views/system/login-log/index.vue'),
                meta:{requiresAuth:true,title:'登录日志'}
            },
            {
                path:'system/role',
                name:'SystemRole',
                component:()=>import('@/views/system/role/index.vue'),
                meta:{requiresAuth:true,title:'权限管理'}
            },
            {
                path:'system/manage',
                name:'SystemManage',
                component:()=>import('@/views/system/manage/index.vue'),
                meta:{requiresAuth:true,title:'系统管理'}
            }
        ]
    },
    {
        path:'/:pathMatch(.*)*',
        name:'not-found',
        component:()=>import('@/views/404.vue'),
    }
];

const router=createRouter({
    history:createWebHashHistory(import.meta.env.BASE_URL),
    routes:[
        ...frontRoutes,
    ],
});

const WHITE_LIST=['/login','/register','/forgot-password'];

// 权限可能被在线改，每次进页面问一次后端，15 秒内不重复
let lastAuthRefresh = 0;
async function refreshAuthIfNeeded(){
    if(Date.now() - lastAuthRefresh < 15000) return;
    lastAuthRefresh = Date.now();
    try{
        await useUserStore().refreshAuth();
    }catch{
        // 接口失败就当权限没变，不挡导航
    }
}

router.beforeEach(async (to,from,next)=>{
    const token =sessionStorage.getItem('token');
    const mustChange=sessionStorage.getItem('mustChangePassword')==='true';

    // 已登录还去登录页
    if(to.path==='/login' && token){
        return next('/home');
    }

    // 要登录但没有 token
    if(to.meta.requiresAuth && !token){
        return next({path:'/login',query:{redirect:to.fullPath}});
    }

    if(token && mustChange && to.path !=='/change-password'){
        return next('/change-password');
    }

    if(token){
        await refreshAuthIfNeeded();
        // 访客只有公开公告，进首页会 403，直接送公告页
        if(sessionStorage.getItem('role')==='guest' && to.path==='/home'){
            return next('/home/public-announcement');
        }
    }

    // 在线用户：admin 看明细，有 online:view 看脱敏名单，都没有回首页
    if(to.path==='/home/online'){
        let roles:string[]=[];
        let perms:string[]=[];
        try{ roles=JSON.parse(sessionStorage.getItem('roles')||'[]'); }catch{}
        try{ perms=JSON.parse(sessionStorage.getItem('permissions')||'[]'); }catch{}
        if(!roles.includes('admin') && !perms.includes('system:online') && !perms.includes('online:view')){
            return next('/home');
        }
    }

    next();
});
export default router;