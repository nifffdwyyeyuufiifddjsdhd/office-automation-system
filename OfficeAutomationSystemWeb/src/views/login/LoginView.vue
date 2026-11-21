<template>
  <div class="login-page">
    <!-- 粒子背景（最底层） -->
    <vue-particles
      id="tsparticles"
      :options="particlesOptions"
      class="particles-bg"
    />

    <!-- 登录容器 -->
    <div class="login-container">
      <div class="glass-card">
        <!-- 顶部标题 -->
        <div class="login-header">
          <h1 class="login-title">OA 办公系统</h1>
          <p class="login-subtitle">Office Automation System</p>
        </div>

        <!-- Tab 切换 -->
        <el-tabs v-model="activeTab" class="login-tabs" stretch>
          <!-- 员工登录 -->
          <el-tab-pane label="员工登录" name="employee">
            <el-form
              ref="employeeFormRef"
              :model="employeeForm"
              :rules="employeeRules"
              size="large"
              @keyup.enter="handleLogin"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="employeeForm.username"
                  placeholder="员工工号 / 用户名"
                  :prefix-icon="User"
                  clearable
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="employeeForm.password"
                  type="password"
                  placeholder="登录密码"
                  :prefix-icon="Lock"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  class="login-btn"
                  :loading="loading"
                  @click="handleLogin"
                >
                  登 录
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- 邮箱登录 -->
          <el-tab-pane label="邮箱登录" name="email">
            <el-form
              ref="emailFormRef"
              :model="emailForm"
              :rules="emailRules"
              size="large"
              @keyup.enter="handleLogin"
            >
              <el-form-item prop="email">
                <el-input
                  v-model="emailForm.email"
                  placeholder="企业邮箱"
                  :prefix-icon="Message"
                  clearable
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="emailForm.password"
                  type="password"
                  placeholder="登录密码"
                  :prefix-icon="Lock"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  class="login-btn"
                  :loading="loading"
                  @click="handleLogin"
                >
                  登 录
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- 手机验证码登录 -->
          <el-tab-pane label="手机验证码" name="phone">
            <el-form
              ref="phoneFormRef"
              :model="phoneForm"
              :rules="phoneRules"
              size="large"
              @keyup.enter="handleLogin"
            >
              <el-form-item prop="phone">
                <el-input
                  v-model="phoneForm.phone"
                  placeholder="手机号"
                  :prefix-icon="Iphone"
                  maxlength="11"
                  clearable
                />
              </el-form-item>
              <el-form-item prop="code">
                <div class="code-row">
                  <el-input
                    v-model="phoneForm.code"
                    placeholder="验证码"
                    :prefix-icon="Message"
                    maxlength="6"
                  />
                  <el-button
                    class="code-btn"
                    :disabled="countdown > 0"
                    @click="sendCode"
                  >
                    {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  class="login-btn"
                  :loading="loading"
                  @click="handleLogin"
                >
                  登 录
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <!-- 底部 -->
        <div class="login-footer">
          <a href="javascript:;" @click="router.push('/forgot-password')">忘记密码？</a>
          <a href="javascript:;" @click="openBulletin">公开公告</a>
          <a href="javascript:;">联系管理员</a>
        </div>
      </div>
    </div>
  </div>

  <!-- 公开公告不用登录，对应 announcement:view-public -->
  <el-dialog v-model="bulletinVisible" title="公开公告" width="640px" class="bulletin-dialog">
    <div v-loading="bulletinLoading" class="bulletin-body">
      <div v-if="bulletinList.length === 0" class="bulletin-empty">暂无已发布的公开公告</div>
      <div v-for="item in bulletinList" :key="item.id" class="bulletin-item" @click="openBulletinDetail(item.id)">
        <div class="bulletin-title">
          <el-tag v-if="item.isTop === 1" size="small" type="danger" effect="plain">置顶</el-tag>
          <span>{{ item.title }}</span>
        </div>
        <div class="bulletin-meta">
          <span v-if="item.categoryName">{{ item.categoryName }}</span>
          <span>{{ item.publishTime || "" }}</span>
        </div>
        <div v-if="item.summary" class="bulletin-summary">{{ item.summary }}</div>
      </div>
    </div>
  </el-dialog>

  <el-dialog v-model="bulletinDetailVisible" :title="bulletinDetail.title" width="680px">
    <div v-loading="bulletinDetailLoading" class="bulletin-detail">
      <div class="bulletin-meta">
        <span v-if="bulletinDetail.categoryName">{{ bulletinDetail.categoryName }}</span>
        <span>{{ bulletinDetail.publishTime || "" }}</span>
        <span v-if="bulletinDetail.viewCount">浏览 {{ bulletinDetail.viewCount }}</span>
      </div>
      <div class="bulletin-content" :class="themeClass(bulletinDetail.theme)" v-html="sanitizeHtml(bulletinDetail.content || bulletinDetail.summary || '暂无内容')"></div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { sanitizeHtml } from '@/utils/safeHtml'
import { themeClass } from '@/constants/announcementContent'
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Message, Iphone } from '@element-plus/icons-vue'
import { particlesOptions } from '@/utils/particles'
import {login} from '@/api/auth'
import { getPublicAnnouncement, getPublicAnnouncements, type PublicAnnouncementBrief, type PublicAnnouncementDetail } from '@/api/announcement'
import {useUserStore} from '@/store/user'

const userStore=useUserStore();
const router = useRouter()
const activeTab = ref('employee')
// 公开公告（免登录）
const bulletinVisible = ref(false)
const bulletinLoading = ref(false)
const bulletinList = ref<PublicAnnouncementBrief[]>([])
const bulletinDetailVisible = ref(false)
const bulletinDetailLoading = ref(false)
const bulletinDetail = ref<Partial<PublicAnnouncementDetail>>({})

async function openBulletin() {
  bulletinVisible.value = true
  bulletinLoading.value = true
  try {
    bulletinList.value = await getPublicAnnouncements(10)
  } catch {
    bulletinList.value = []
  } finally {
    bulletinLoading.value = false
  }
}

async function openBulletinDetail(id: number) {
  bulletinDetailVisible.value = true
  bulletinDetailLoading.value = true
  bulletinDetail.value = {}
  try {
    bulletinDetail.value = await getPublicAnnouncement(id)
  } catch {
    bulletinDetail.value = {}
  } finally {
    bulletinDetailLoading.value = false
  }
}


// 员工登录
const employeeFormRef = ref<FormInstance>()
const employeeForm = reactive({
  username: '',
  password: '',
})
const employeeRules: FormRules = {
  username: [{ required: true, message: '请输入员工工号 / 用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

// 邮箱登录
const emailFormRef = ref<FormInstance>()
const emailForm = reactive({
  email: '',
  password: '',
})
const emailRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

// 手机验证码登录
const phoneFormRef = ref<FormInstance>()
const phoneForm = reactive({
  phone: '',
  code: '',
})
const phoneRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为 6 位', trigger: 'blur' },
  ],
}

// 验证码倒计时
const countdown = ref(0)
let timer: number | null = null

const sendCode = () => {
  if (!phoneForm.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  // TODO: 调后端发送验证码接口
  ElMessage.success('验证码已发送，请注意查收')
  countdown.value = 60
  timer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0 && timer) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// 登录
const loading = ref(false)

const handleLogin = async () => {
  let formRef: FormInstance | undefined
  if (activeTab.value === 'employee') formRef = employeeFormRef.value
  else if (activeTab.value === 'email') formRef = emailFormRef.value
  else formRef = phoneFormRef.value

  if (!formRef) return

  await formRef.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const data=await login({
        username:employeeForm.username,
        password:employeeForm.password
      })
      userStore.setLogin(data);
      if(data.mustChangePassword){
        router.push('/change-password')
      }else{
        router.push('/');
      }
    } catch (err: any) {
      ElMessage.error(err?.message || '登录失败，请重试')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
/* 页面根容器 */
.login-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
  /* 有背景图就用背景图，没有用渐变 */
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 50%, #43e97b 100%);
  background: url('@/assets/image/【哲风壁纸】二次元-动漫.png') center/cover no-repeat fixed;
 
}

/* 粒子背景层 */
.particles-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

/* 布局容器：把玻璃框推到中间偏右 */
.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px;
}

/* 玻璃拟态卡片 */
.glass-card {
  width: 420px;
  max-width: 100%;
  padding: 36px 34px 24px;
  border-radius: 20px;

  /* 玻璃核心三件套 */
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(18px) saturate(200%) brightness(115%);
  -webkit-backdrop-filter: blur(28px) saturate(200%) brightness(115%);
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 0 8px 40px rgba(31, 38, 135, 0.28),
            inset 0 1px 0 rgba(255, 255, 255, 0.65),
            inset 0 -1px 0 rgba(255, 255, 255, 0.15);

   border: 1px solid rgba(255, 255, 255, 0.55);


  /* 居中偏右：向右偏移 28% 自身宽度 */
  transform: translateX(80%);
  transition: transform 0.3s ease;
}

/* 小屏回到正中，不溢出 */
@media (max-width: 900px) {
  .glass-card {
    transform: translateX(0);
  }
}

/* 标题 */
.login-header {
  text-align: center;
  margin-bottom: 18px;
}

.login-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #1a2b4c;
  letter-spacing: 2px;
}

.login-subtitle {
  margin: 6px 0 0;
  font-size: 12px;
  color: #405a7a;
  letter-spacing: 1px;
  text-transform: uppercase;
}

/* Tabs 样式覆盖 */
.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 22px;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(255, 255, 255, 0.35);
}

.login-tabs :deep(.el-tabs__item) {
  color: #2b3d5c;
  font-weight: 500;
  font-size: 14px;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: #1677ff;
  font-weight: 700;
}

.login-tabs :deep(.el-tabs__active-bar) {
  background-color: #1677ff;
  height: 3px;
  border-radius: 3px;
}

/* 输入框 / 按钮 */
:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.65);
  box-shadow: none !important;
  border-radius: 10px;
  transition: background-color 0.2s;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  background-color: rgba(255, 255, 255, 0.9);
}

:deep(.el-input__inner) {
  color: #1a2b4c;
}

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.35);
  transition: transform 0.15s, box-shadow 0.15s;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.45);
}

/* 验证码行 */
.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.code-btn {
  width: 130px;
  height: 40px;
  border-radius: 10px;
  background-color: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(22, 119, 255, 0.4);
  color: #1677ff;
  font-size: 13px;
  white-space: nowrap;
}

.code-btn:hover:not(:disabled) {
  background-color: rgba(22, 119, 255, 0.1);
}

/* 底部 */
.bulletin-body { max-height: 460px; overflow: auto; }
.bulletin-empty { text-align: center; color: #909399; font-size: 13px; padding: 24px 0; }
.bulletin-item { padding: 12px 14px; border-radius: 8px; cursor: pointer; transition: background .2s; border: 1px solid #eef1f6; margin-bottom: 10px; }
.bulletin-item:hover { background: #f5f8ff; border-color: #d6e2ff; }
.bulletin-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: #1f2d3d; }
.bulletin-meta { display: flex; gap: 12px; font-size: 12px; color: #98a1b3; margin-top: 6px; }
.bulletin-summary { font-size: 13px; color: #5b6472; margin-top: 6px; line-height: 1.6; }
.bulletin-detail { min-height: 80px; }
/* 正文排版见 .ann-body，这里只留上间距 */
.bulletin-content { margin-top: 12px; }
.login-footer {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #405a7a;
}

.login-footer a {
  color: #1677ff;
  text-decoration: none;
}

.login-footer a:hover {
  text-decoration: underline;
}
</style>