<script setup lang="ts">
import { reactive, ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { pinyin } from 'pinyin-pro'
import { sendResetCode, resetPassword } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { particlesOptions } from '@/utils/particles'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let timer: number | null = null

const form = reactive({
  username: '',
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

// 发送验证码 + 倒计时
const handleSendCode = async () => {
  if (!form.username) return ElMessage.warning('请输入用户名')
  if (!form.email) return ElMessage.warning('请输入注册邮箱')
  sending.value = true
  try {
    await sendResetCode({ username: form.username, email: form.email })
    ElMessage.success('验证码已发送到邮箱，请查收')
    countdown.value = 60
    timer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0 && timer) { clearInterval(timer); timer = null }
    }, 1000)
  } catch (e: any) {
    ElMessage.error(e?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

// 新密码强度校验（与改密页一致）
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
  const lower = value.toLowerCase()
  if (lower.includes(form.username.toLowerCase())) return callback(new Error('密码不能包含用户名'))
  const realName = userStore.realName || ''
  if (realName) {
    const full = pinyin(realName, { toneType: 'none', type: 'string' }).toLowerCase()
    const initial = pinyin(realName, { pattern: 'first', toneType: 'none', type: 'string' }).toLowerCase()
    if (lower.includes(full) || lower.includes(initial)) return callback(new Error('密码不能包含姓名拼音'))
  }
  callback()
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入注册邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
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
      await resetPassword({
        username: form.username,
        email: form.email,
        code: form.code,
        newPassword: form.newPassword,
      })
      ElMessage.success('密码重置成功，请使用新密码登录')
      router.push('/login')
    } catch (e: any) {
      ElMessage.error(e?.message || '重置失败')
    } finally {
      loading.value = false
    }
  })
}

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<template>
  <div class="forgot-page">
    <vue-particles id="tsparticles" :options="particlesOptions" class="particles-bg" />

    <div class="fp-container">
      <div class="glass-card">
        <div class="fp-header">
          <h1 class="fp-title">找回密码</h1>
          <p class="fp-subtitle">通过注册邮箱验证重置密码</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" size="large">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="注册邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入注册邮箱" clearable />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <div class="code-row">
              <el-input v-model="form.code" placeholder="6位验证码" maxlength="6" />
              <el-button class="code-btn" :disabled="countdown > 0" :loading="sending" @click="handleSendCode">
                {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="8-20位，至少3种字符" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
        </el-form>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">
          重置密码
        </el-button>

        <div class="fp-footer">
          <a href="javascript:;" @click="router.push('/login')">返回登录</a>
          <a href="javascript:;" @click="ElMessage.info('请联系管理员重置密码')">无法自助找回？</a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forgot-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 50%, #43e97b 100%);
  background: url('@/assets/image/【哲风壁纸】二次元-动漫.png') center/cover no-repeat fixed;
}
.particles-bg {
  position: absolute; inset: 0; width: 100%; height: 100%; z-index: 0;
}
.fp-container {
  position: relative; z-index: 1;
  display: flex; justify-content: center; align-items: center;
  min-height: 100vh; padding: 24px;
}
.glass-card {
  width: 480px; max-width: 100%; padding: 36px 34px 28px; border-radius: 20px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(18px) saturate(200%) brightness(115%);
  -webkit-backdrop-filter: blur(28px) saturate(200%) brightness(115%);
  border: 1px solid rgba(255, 255, 255, 0.55);
  box-shadow: 0 8px 40px rgba(31, 38, 135, 0.28),
              inset 0 1px 0 rgba(255, 255, 255, 0.65),
              inset 0 -1px 0 rgba(255, 255, 255, 0.15);
}
.fp-header { text-align: center; margin-bottom: 22px; }
.fp-title { margin: 0; font-size: 26px; font-weight: 700; color: #1a2b4c; letter-spacing: 2px; }
.fp-subtitle { margin: 8px 0 0; font-size: 13px; color: #405a7a; letter-spacing: 1px; }
:deep(.el-form-item__label) { color: #2b3d5c; font-weight: 500; }
:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.65);
  box-shadow: none !important; border-radius: 10px;
  transition: background-color 0.15s;
}
:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) { background-color: rgba(255, 255, 255, 0.9); }
:deep(.el-input__inner) { color: #1a2b4c; }
.code-row { display: flex; gap: 10px; width: 100%; }
.code-row .el-input { flex: 1; }
.code-btn {
  width: 130px; height: 40px; border-radius: 10px;
  background-color: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(22, 119, 255, 0.4); color: #1677ff; font-size: 13px; white-space: nowrap;
}
.code-btn:hover:not(:disabled) { background-color: rgba(22, 119, 255, 0.1); }
.submit-btn {
  width: 100%; height: 44px; border-radius: 10px;
  font-size: 16px; font-weight: 600; letter-spacing: 4px;
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  border: none; box-shadow: 0 4px 12px rgba(22, 119, 255, 0.35);
  transition: transform 0.15s, box-shadow 0.15s;
}
.submit-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(22, 119, 255, 0.45); }
.fp-footer {
  margin-top: 14px; display: flex; justify-content: space-between; font-size: 13px; color: #405a7a;
}
.fp-footer a { color: #1677ff; text-decoration: none; }
.fp-footer a:hover { text-decoration: underline; }
</style>
