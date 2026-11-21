import { createApp } from 'vue'
import App from './App.vue'
import "@/assets/main.css"
import "@/assets/announcement-body.css"
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 粒子背景
import Particles from "@tsparticles/vue3"
import {loadSlim} from "@tsparticles/slim"
import {createPinia} from "pinia"
import router from '@/router'
import { permission } from '@/directives/permission'
import 'element-plus/dist/index.css'

const app=createApp(App);
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局注册权限指令 v-permission
app.directive('permission', permission)

// 全局注册所有图标
for(const [key,component] of Object.entries(ElementPlusIconsVue)){
    app.component(key,component)
}

app.use(Particles,{
    init:async(engine)=>{
        await loadSlim(engine);
    }
})
app.mount('#app')
