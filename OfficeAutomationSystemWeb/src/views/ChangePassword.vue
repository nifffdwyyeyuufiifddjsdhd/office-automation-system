<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { pinyin } from 'pinyin-pro'
import { changePassword } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { particlesOptions } from '@/utils/particles'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// 实时强度提示（第④条需求：不提交、页面实时提示）
const strengthText = computed(() => {
  const p = form.newPassword
  if (!p) return ''
  const lenOk = p.length >= 8 && p.length <= 20
  let kinds = 0
  if (/[a-z]/.test(p)) kinds++
  if (/[A-Z]/.test(p)) kinds++
  if (/\d/.test(p)) kinds++
  if (/[^a-zA-Z0-9]/.test(p)) kinds++
  if (!lenOk) return '密码长度需 8-20 位'
  if (kinds < 3) return '密码需包含大小写字母、数字、特殊字符中至少 3 种'
  if (p.toLowerCase().includes(userStore.username.toLowerCase())) return '密码不能包含用户名'
  return '密码强度合格'
})

const validateNew = (_rule: any, value: string, callback: (e?: Error) => void) => {
  if (!value) return callback(new Error('请输入新密码'))
  const lenOk = value.length >= 8 && value.length <= 20
  let kinds = 0
  if (/[a-z]/.test(value)) kinds++
  if (/[A-Z]/.test(value)) kinds++
  if (/\d/.test(value)) kinds++
  if (/[^a-zA-Z0-9]/.test(value)) kinds++
  if (!lenOk) return callback(new Error('密码长度需 8-20 位'))
  if (kinds < 3) return callback(new Error('密码需包含大小写字母、数字、特殊字符中至少 3 种'))

  // 不能包含用户名 / 姓名拼音（第③条需求）
  const lower = value.toLowerCase()
  if (lower.includes(userStore.username.toLowerCase())) return callback(new Error('密码不能包含用户名'))
  const realName = userStore.realName || ''
  if (realName) {
    const full = pinyin(realName, { toneType: 'none', type: 'string' }).toLowerCase()
    const initial = pinyin(realName, { pattern: 'first', toneType: 'none', type: 'string' }).toLowerCase()
    if (lower.includes(full) || lower.includes(initial)) return callback(new Error('密码不能包含姓名拼音'))
  }
  callback()
}

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ validator: validateNew, trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_r: any, v: string, cb: (e?: Error) => void) =>
        v === form.newPassword ? cb() : cb(new Error('两次输入的新密码不一致')),
      trigger: 'blur',
    },
  ],
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      // 第 5 条需求：成功后自动注销，跳登录页并提示
      await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
      ElMessage.success('密码修改成功，请使用新密码重新登录')
      userStore.clearLogin()
      router.push('/login')
    } catch (e: any) {
      ElMessage.error(e?.message || '修改失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="change-password-page">
    <!-- 粒子背景（最底层，和登录页一样） -->
    <vue-particles
      id="tsparticles"
      :options="particlesOptions"
      class="particles-bg"
    />

    <!-- 居中容器 -->
    <div class="cp-container">
      <div class="glass-card">
        <div class="cp-header">
          <h1 class="cp-title">修改密码</h1>
          <p class="cp-subtitle">首次登录（或管理员重置后）须修改初始密码</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="80px"
          size="large"
          @keyup.enter="handleSubmit"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="form.oldPassword"
              type="password"
              show-password
              placeholder="请输入初始密码"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="form.newPassword"
              type="password"
              show-password
              placeholder="8-20位，至少3种字符"
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              placeholder="再次输入新密码"
            />
          </el-form-item>
        </el-form>

        <div class="strength" :class="{ ok: strengthText === '密码强度合格' }">{{ strengthText }}</div>

        <el-button
          type="primary"
          class="submit-btn"
          :loading="loading"
          @click="handleSubmit"
        >
          确认修改
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 页面根容器（和登录页一致） */
.change-password-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
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

/* 居中容器 */
.cp-container {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px;
}

/* 玻璃拟态卡片（和登录页一致，居中显示） */
.glass-card {
  width: 480px;
  max-width: 100%;
  padding: 36px 34px 28px;
  border-radius: 20px;

  /* 玻璃核心三件套 */
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(18px) saturate(200%) brightness(115%);
  -webkit-backdrop-filter: blur(28px) saturate(200%) brightness(115%);
  border: 1px solid rgba(255, 255, 255, 0.55);
  box-shadow: 0 8px 40px rgba(31, 38, 135, 0.28),
              inset 0 1px 0 rgba(255, 255, 255, 0.65),
              inset 0 -1px 0 rgba(255, 255, 255, 0.15);
}

/* 标题 */
.cp-header {
  text-align: center;
  margin-bottom: 22px;
}

.cp-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #1a2b4c;
  letter-spacing: 2px;
}

.cp-subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: #405a7a;
  letter-spacing: 1px;
}

/* 表单 label */
:deep(.el-form-item__label) {
  color: #2b3d5c;
  font-weight: 500;
}

/* 输入框（和登录页一致） */
:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.65);
  box-shadow: none !important;
  border-radius: 10px;
  transition: background-color 0.15s;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  background-color: rgba(255, 255, 255, 0.9);
}

:deep(.el-input__inner) {
  color: #1a2b4c;
}

/* 强度提示 */
.strength {
  min-height: 22px;
  font-size: 13px;
  color: #f56c6c;
  margin: 2px 0 14px;
  text-align: center;
}

.strength.ok {
  color: #67c23a;
}

/* 提交按钮（和登录页一致） */
.submit-btn {
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

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.45);
}
</style>
