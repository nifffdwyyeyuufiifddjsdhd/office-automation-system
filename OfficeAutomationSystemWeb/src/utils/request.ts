import axios from "axios";
import { ElMessage } from "element-plus";

// 接口前缀默认 /api，换域名改 .env 里的 VITE_API_BASE
const request=axios.create({
    baseURL:import.meta.env.VITE_API_BASE || "/api",
    timeout:10000
});

request.interceptors.request.use(config=>{
    const token =sessionStorage.getItem("token");
    if(token) config.headers.Authorization=`Bearer ${token}`;
    return config;
});

request.interceptors.response.use(
    res=>{
        // 文件下载（blob）直接返回，不走业务 code 判断
        if (res.data instanceof Blob) return res.data;
        const {code,message,data}=res.data
        if(code===200)return data;
        ElMessage.error(message)
        return Promise.reject(new Error(message))
    },
    err=>{
        const status = err.response && err.response.status
        // 后端带 code 和 message，优先显示后端的中文提示
        const serverMsg = err.response && err.response.data && err.response.data.message
        // 401 清登录态并跳回登录页
        if (status === 401) {
            sessionStorage.clear()
            const hash = window.location.hash
            if (!hash.includes('/login')) {
                window.location.hash = '#/login'
            }
            ElMessage.error(serverMsg || '登录已过期，请重新登录')
        } else if (status === 403) {
            ElMessage.error(serverMsg || '没有权限执行该操作')
        } else if (status === 429) {
            ElMessage.error(serverMsg || '操作过于频繁，请稍后再试')
        } else if (status === 500 && err.config && err.config.responseType === 'blob') {
            ElMessage.error('导出失败，请稍后重试');
        } else {
            ElMessage.error(serverMsg || err.message || "网络错误");
        }
        return Promise.reject(err);
    }
);

export default request;
