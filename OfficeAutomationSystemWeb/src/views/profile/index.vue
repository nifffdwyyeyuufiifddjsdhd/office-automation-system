<template>
  <div class="profile-page">
    <el-card shadow="never" class="profile-card">
      <el-tabs v-model="activeTab" class="profile-tabs">

        <!-- 个人资料 -->
        <el-tab-pane label="个人资料" name="profile">
          <div class="profile-body">
            <div class="avatar-area">
              <el-avatar :size="96" :src="avatarUrl || undefined" class="avatar-img">
                {{ avatarText }}
              </el-avatar>
              <el-upload
                class="avatar-upload"
                :show-file-list="false"
                accept="image/png,image/jpeg,image/gif,image/webp"
                :http-request="handleAvatarUpload"
                :before-upload="beforeAvatarUpload"
              >
                <el-button type="primary" plain size="small">更换头像</el-button>
              </el-upload>
              <div class="avatar-tip">支持 jpg/png/gif/webp，不超过 2MB</div>
            </div>

            <el-form :model="form" label-width="92px" class="profile-form" @submit.prevent>
              <el-form-item label="用户名">
                <el-input v-model="form.username" placeholder="3-20 位，字母开头，可含数字与下划线" maxlength="20" show-word-limit clearable />
                <div class="field-tip">用户名就是登录账号，改完下次用新用户名登录；显示名称请改下面的「姓名」</div>
              </el-form-item>
              <el-form-item label="姓名">
                <el-input v-model="form.real_name" placeholder="请输入姓名" maxlength="20" show-word-limit />
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="form.gender">
                  <el-radio :label="1">男</el-radio>
                  <el-radio :label="2">女</el-radio>
                  <el-radio :label="0">保密</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="手机号码">
                <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" clearable />
              </el-form-item>
              <el-form-item label="电子邮箱">
                <el-input v-model="form.email" placeholder="请输入邮箱" clearable />
              </el-form-item>
              <el-form-item label="所属部门">
                <el-input :model-value="form.org_name || '—'" disabled />
              </el-form-item>
              <el-form-item label="职位">
                <el-input :model-value="form.position_name || '—'" disabled />
              </el-form-item>
              <el-form-item label="职称">
                <el-input :model-value="form.title_name || '—'" disabled />
              </el-form-item>
              <el-form-item label="入职日期">
                <el-input :model-value="form.entry_date || '—'" disabled />
              </el-form-item>
              <el-form-item label="角色">
                <el-input :model-value="(form.roleNames && form.roleNames.length) ? form.roleNames.join('、') : '—'" disabled />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
                <el-button @click="loadProfile">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form :model="pwdForm" label-width="92px" class="pwd-form" @submit.prevent>
            <el-form-item label="原密码">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="8-20位，须含字母和数字" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdSaving" @click="savePassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 我的待办 -->
        <el-tab-pane label="我的待办" name="todo">
          <div class="todo-grid">
            <div class="todo-card" @click="goTo('/home/flow-todo')">
              <div class="todo-num">{{ summary.flowTodo > 99 ? '99+' : summary.flowTodo }}</div>
              <div class="todo-label">待办流程</div>
            </div>
            <div class="todo-card" @click="goTo('/home/task')">
              <div class="todo-num">{{ summary.taskTodo }}</div>
              <div class="todo-label">待办任务</div>
            </div>
            <div class="todo-card" @click="activeTab = 'notice'">
              <div class="todo-num">{{ summary.unread }}</div>
              <div class="todo-label">未读通知</div>
            </div>
            <div class="todo-card">
              <div class="todo-num">{{ attendanceText }}</div>
              <div class="todo-label">今日出勤</div>
            </div>
          </div>
          <el-descriptions title="今日打卡" :column="1" border class="att-desc">
            <el-descriptions-item label="打卡状态">{{ attendanceStatusText }}</el-descriptions-item>
            <el-descriptions-item label="上班时间">{{ today.workStart || '—' }} ~ {{ today.workEnd || '—' }}</el-descriptions-item>
            <el-descriptions-item label="签到时间">{{ today.checkInTime || '未签到' }}</el-descriptions-item>
            <el-descriptions-item label="签退时间">{{ today.checkOutTime || '未签退' }}</el-descriptions-item>
            <el-descriptions-item label="工作时长">{{ today.workHours != null ? today.workHours + ' 小时' : '—' }}</el-descriptions-item>
          </el-descriptions>
          <div class="todo-actions">
            <el-button type="primary" @click="goTo('/home/attendance')">前往考勤打卡</el-button>
            <el-button @click="goTo('/home/flow-todo')">处理待办流程</el-button>
            <el-button @click="goTo('/home/task')">处理待办任务</el-button>
          </div>
        </el-tab-pane>

        <!-- 系统通知 -->
        <el-tab-pane label="系统通知" name="notice">
          <div class="notice-toolbar">
            <el-radio-group v-model="noticeRead" size="small" @change="reloadNotices">
              <el-radio-button :value="null">全部</el-radio-button>
              <el-radio-button :value="0">未读</el-radio-button>
              <el-radio-button :value="1">已读</el-radio-button>
            </el-radio-group>
            <el-select v-model="noticeType" size="small" placeholder="全部类型" clearable style="width:130px" @change="reloadNotices">
              <el-option label="系统通知" :value="1" />
              <el-option label="审批待办" :value="2" />
              <el-option label="任务通知" :value="3" />
              <el-option label="出勤提醒" :value="4" />
              <el-option label="日志提醒" :value="5" />
            </el-select>
            <el-input v-model="noticeKeyword" size="small" placeholder="搜索标题/内容" clearable style="width:200px" @keyup.enter="reloadNotices" @clear="reloadNotices" />
            <div class="toolbar-right">
              <el-button size="small" @click="readAll">全部已读</el-button>
              <el-button size="small" type="danger" plain @click="clearRead">清空已读</el-button>
            </div>
          </div>

          <el-table :data="notices" v-loading="noticeLoading" stripe>
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="typeTag(row.type)" size="small">{{ typeName(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="标题" min-width="200">
              <template #default="{ row }">
                <span :class="{ unread: row.isRead === 0 }">
                  <el-icon v-if="row.isTop" color="#f56c6c"><Top /></el-icon>
                  {{ row.title }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
            <el-table-column label="时间" width="168">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="70">
              <template #default="{ row }">
                <el-tag v-if="row.isRead === 0" type="danger" size="small">未读</el-tag>
                <el-tag v-else type="info" size="small">已读</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button v-if="row.isRead === 0" link type="primary" size="small" @click="readOne(row)">标记已读</el-button>
                <el-popconfirm title="确定删除这条通知？" @confirm="removeNotice(row)">
                  <template #reference><el-button link type="danger" size="small">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty>暂无通知</template>
          </el-table>

          <el-pagination
            class="notice-pager"
            background
            layout="total, prev, pager, next"
            :total="noticeTotal"
            :current-page="noticePage"
            :page-size="noticeSize"
            @current-change="onNoticePage"
          />
        </el-tab-pane>

        <!-- 通知偏好 -->
        <el-tab-pane label="通知偏好" name="pref">
          <el-form label-width="180px" class="pref-form">
            <el-form-item label="系统通知">
              <el-switch v-model="pref.system" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="审批待办通知">
              <el-switch v-model="pref.flow" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="任务通知">
              <el-switch v-model="pref.task" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="出勤提醒">
              <el-switch v-model="pref.attendance" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="工作日志提醒">
              <el-switch v-model="pref.worklog" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="新消息声音/标题提醒">
              <el-switch v-model="pref.sound" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="prefSaving" @click="savePref">保存偏好</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 第三方绑定 -->
        <el-tab-pane label="第三方绑定" name="binding">
          <div class="bind-tip">
            绑定企业微信 / 钉钉后，审批待办、任务提醒可以按平台推送给本人；这里只保存账号标识，不会保存任何第三方密码或令牌。
          </div>
          <el-table :data="bindings" v-loading="bindingLoading" stripe>
            <el-table-column prop="platformName" label="平台" width="140" />
            <el-table-column label="绑定状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.bound" type="success" size="small">已绑定</el-tag>
                <el-tag v-else type="info" size="small">未绑定</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="账号标识" min-width="180">
              <template #default="{ row }">{{ row.bound ? (row.externalId || '—') : '—' }}</template>
            </el-table-column>
            <el-table-column label="显示名" min-width="140">
              <template #default="{ row }">{{ row.bound ? (row.externalName || '—') : '—' }}</template>
            </el-table-column>
            <el-table-column label="绑定时间" width="170">
              <template #default="{ row }">{{ row.bound ? formatTime(row.bindTime) : '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openBind(row)">{{ row.bound ? '改绑' : '去绑定' }}</el-button>
                <el-popconfirm v-if="row.bound" title="确定解绑这个账号？" @confirm="handleUnbind(row)">
                  <template #reference><el-button link type="danger" size="small">解绑</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty>暂无绑定</template>
          </el-table>
        </el-tab-pane>

      </el-tabs>
    </el-card>

    <el-dialog v-model="bindDialog" :title="bindForm.platformName + ' 绑定'" width="460px">
      <el-form :model="bindForm" label-width="100px">
        <el-form-item :label="bindForm.platformName + '账号'">
          <el-input
            v-model="bindForm.externalId"
            maxlength="64"
            show-word-limit
            :placeholder="bindForm.platform === 'wecom' ? '企业微信 UserID' : '钉钉 userId'"
          />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="bindForm.externalName" maxlength="50" show-word-limit placeholder="选填，方便核对信息" />
        </el-form-item>
      </el-form>
      <div class="bind-tip">同一个第三方账号只能绑定一个人；UserID 可以在企业微信 / 钉钉管理后台的成员详情里查到。</div>
      <template #footer>
        <el-button @click="bindDialog = false">取消</el-button>
        <el-button type="primary" :loading="bindSaving" @click="submitBind">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { Top } from "@element-plus/icons-vue";
import {
  getMyProfile, updateMyProfile, uploadAvatar, fetchAvatarUrl, type ProfileItem,
  getMyBindings, bindThirdParty, unbindThirdParty, type ThirdPartyBindingItem,
} from "@/api/user";
import { changePassword } from "@/api/auth";
import {
  pageNotifications, deleteNotification, deleteReadNotifications,
  getNotificationPreferences, saveNotificationPreferences,
  markRead, markAllRead, getUnreadCount,
  type NotificationPageItem, type NotificationPreferences,
} from "@/api/notification";
import { todoCount } from "@/api/flow";
import { getStatistics } from "@/api/task";
import { getTodayStatus, type TodayStatus } from "@/api/attendance";
import { useUserStore } from "@/store/user";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const activeTab = ref("profile");

// 个人资料
const form = reactive<ProfileItem>({
  id: 0, username: "", real_name: "", gender: 0,
  phone: "", email: "", org_name: "", position_name: "", title_name: "",
  entry_date: "", roleNames: [],
});
const saving = ref(false);
const avatarUrl = ref<string | null>(null);
const avatarText = computed(() => (form.real_name || userStore.realName || "?").charAt(0));

async function loadProfile() {
  const p = await getMyProfile();
  Object.assign(form, p);
  if (p.avatar) {
    const url = await fetchAvatarUrl(p.avatar);
    if (url) avatarUrl.value = url;
  }
}

function beforeAvatarUpload(file: File) {
  const okType = ["image/jpeg", "image/png", "image/gif", "image/webp"].includes(file.type);
  if (!okType) { ElMessage.error("仅支持 jpg/png/gif/webp 格式"); return false; }
  if (file.size > 2 * 1024 * 1024) { ElMessage.error("头像不能超过 2MB"); return false; }
  return true;
}

async function handleAvatarUpload(opt: any) {
  try {
    const res = await uploadAvatar(opt.file);
    ElMessage.success("头像已更新");
    if (avatarUrl.value) URL.revokeObjectURL(avatarUrl.value);
    form.avatar = res.avatar;
    avatarUrl.value = await fetchAvatarUrl(res.avatar);
  } catch { /* 拦截器已提示 */ }
}

async function saveProfile() {
  if (!form.real_name || !form.real_name.trim()) { ElMessage.warning("姓名不能为空"); return; }
  if (form.username && !/^[A-Za-z][A-Za-z0-9_]{2,19}$/.test(form.username)) {
    ElMessage.warning("用户名需 3-20 位，字母开头，只能包含字母、数字、下划线");
    return;
  }
  if (form.phone && !/^1\d{10}$/.test(form.phone)) { ElMessage.warning("手机号格式不正确"); return; }
  if (form.email && !/^[\w.+-]+@[\w-]+\.[\w.-]+$/.test(form.email)) { ElMessage.warning("邮箱格式不正确"); return; }
  saving.value = true;
  try {
    await updateMyProfile({
      username: form.username,
      real_name: form.real_name, gender: form.gender,
      phone: form.phone, email: form.email,
    } as any);
    ElMessage.success("资料已保存");
    await loadProfile();
  } catch { /* 拦截器已提示 */ } finally { saving.value = false; }
}

// 第三方绑定（企业微信 / 钉钉）
const bindings = ref<ThirdPartyBindingItem[]>([]);
const bindingLoading = ref(false);
const bindDialog = ref(false);
const bindSaving = ref(false);
const bindForm = reactive({ platform: "wecom", platformName: "企业微信", externalId: "", externalName: "" });
async function loadBindings() {
  bindingLoading.value = true;
  try {
    bindings.value = await getMyBindings();
  } catch { /* 拦截器已提示 */ } finally {
    bindingLoading.value = false;
  }
}
function openBind(row: ThirdPartyBindingItem) {
  bindForm.platform = row.platform;
  bindForm.platformName = row.platformName;
  bindForm.externalId = row.externalId || "";
  bindForm.externalName = row.externalName || "";
  bindDialog.value = true;
}
async function submitBind() {
  if (!bindForm.externalId.trim()) { ElMessage.warning("请填写账号标识"); return; }
  bindSaving.value = true;
  try {
    await bindThirdParty({
      platform: bindForm.platform,
      externalId: bindForm.externalId.trim(),
      externalName: bindForm.externalName.trim(),
    });
    ElMessage.success(bindForm.platformName + " 已绑定");
    bindDialog.value = false;
    await loadBindings();
  } catch { /* 拦截器已提示 */ } finally {
    bindSaving.value = false;
  }
}
async function handleUnbind(row: ThirdPartyBindingItem) {
  try {
    await unbindThirdParty(row.platform);
    ElMessage.success("已解绑");
    await loadBindings();
  } catch { /* 拦截器已提示 */ }
}

// 修改密码
const pwdForm = reactive({ oldPassword: "", newPassword: "", confirm: "" });
const pwdSaving = ref(false);
async function savePassword() {
  const { oldPassword, newPassword, confirm } = pwdForm;
  if (!oldPassword || !newPassword) { ElMessage.warning("请填写原密码和新密码"); return; }
  if (newPassword.length < 8 || newPassword.length > 20) { ElMessage.warning("新密码长度需为 8-20 位"); return; }
  if (!/[a-zA-Z]/.test(newPassword) || !/\d/.test(newPassword)) { ElMessage.warning("新密码须同时包含字母和数字"); return; }
  if (newPassword !== confirm) { ElMessage.warning("两次输入的新密码不一致"); return; }
  pwdSaving.value = true;
  try {
    const res: any = await changePassword({ oldPassword, newPassword });
    if (res && res.token) { sessionStorage.setItem("token", res.token); userStore.token = res.token; }
    ElMessage.success("密码修改成功");
    pwdForm.oldPassword = ""; pwdForm.newPassword = ""; pwdForm.confirm = "";
  } catch { /* 拦截器已提示 */ } finally { pwdSaving.value = false; }
}

// 我的待办汇总
const summary = reactive({ flowTodo: 0, taskTodo: 0, unread: 0 });
const today = reactive<TodayStatus>({
  canCheckIn: false, canCheckOut: false, isHoliday: false,
  workStart: "", workEnd: "", checkInTime: null, checkOutTime: null,
  status: null, workHours: null,
});
const attendanceText = computed(() => {
  if (today.isHoliday) return "休";
  if (today.checkOutTime) return "已签退";
  if (today.checkInTime) return "已签到";
  return "未打卡";
});
const attendanceStatusText = computed(() => {
  if (today.isHoliday) return "节假日/休息日，无需打卡";
  const map: Record<number, string> = { 1: "正常", 2: "迟到", 3: "早退", 4: "迟到且早退", 5: "缺勤", 6: "请假", 7: "外出", 8: "未签退" };
  return today.status == null ? "尚未打卡" : (map[today.status] || "状态 " + today.status);
});
async function loadSummary() {
  try { summary.flowTodo = await todoCount(); } catch { /* 忽略 */ }
  try {
    const st: any = await getStatistics();
    const sc = st.status_count || [];
    summary.taskTodo = (sc[2] || 0) + (sc[3] || 0) + (sc[4] || 0);
  } catch { /* 忽略 */ }
  try { summary.unread = (await getUnreadCount()).count || 0; } catch { /* 忽略 */ }
  try { Object.assign(today, await getTodayStatus()); } catch { /* 忽略 */ }
}
function goTo(path: string) { router.push(path); }

// 系统通知
const notices = ref<NotificationPageItem[]>([]);
const noticeLoading = ref(false);
const noticeRead = ref<number | null>(null);
const noticeType = ref<number | null>(null);
const noticeKeyword = ref("");
const noticePage = ref(1);
const noticeSize = ref(10);
const noticeTotal = ref(0);

async function reloadNotices() {
  noticePage.value = 1;
  await loadNotices();
}
async function loadNotices() {
  noticeLoading.value = true;
  try {
    const res = await pageNotifications({
      pageNum: noticePage.value, pageSize: noticeSize.value,
      isRead: noticeRead.value, type: noticeType.value,
      keyword: noticeKeyword.value || undefined,
    });
    notices.value = res.records || [];
    noticeTotal.value = res.total || 0;
  } catch { /* 拦截器已提示 */ } finally { noticeLoading.value = false; }
}
function onNoticePage(p: number) { noticePage.value = p; loadNotices(); }
function typeName(t: number) {
  return { 1: "系统", 2: "审批", 3: "任务", 4: "出勤", 5: "日志" }[t] || "通知";
}
function typeTag(t: number) {
  return ({ 1: "info", 2: "warning", 3: "primary", 4: "success", 5: "" } as Record<number, string>)[t] || "info";
}
function formatTime(t?: string | null) {
  if (!t) return "—";
  return String(t).replace("T", " ").substring(0, 16);
}
async function readOne(row: NotificationPageItem) {
  try { await markRead(row.id); await loadNotices(); } catch { /* 忽略 */ }
}
async function readAll() {
  try { await markAllRead(); ElMessage.success("已全部标记为已读"); await loadNotices(); } catch { /* 忽略 */ }
}
async function removeNotice(row: NotificationPageItem) {
  try { await deleteNotification(row.id); await loadNotices(); } catch { /* 忽略 */ }
}
async function clearRead() {
  try { await deleteReadNotifications(); ElMessage.success("已清空全部已读通知"); await loadNotices(); } catch { /* 忽略 */ }
}

// 通知偏好
const pref = reactive<NotificationPreferences>({ system: 1, flow: 1, task: 1, attendance: 1, worklog: 1, sound: 1 });
const prefSaving = ref(false);
async function loadPref() {
  try { Object.assign(pref, await getNotificationPreferences()); } catch { /* 忽略 */ }
}
async function savePref() {
  prefSaving.value = true;
  try { await saveNotificationPreferences({ ...pref }); ElMessage.success("偏好已保存"); }
  catch { /* 拦截器已提示 */ } finally { prefSaving.value = false; }
}

onMounted(() => {
  if (route.query.tab) activeTab.value = String(route.query.tab);
  loadProfile();
  loadSummary();
  loadNotices();
  loadPref();
  loadBindings();
});
</script>

<style scoped>
.profile-page { padding: 4px; }
.profile-card { border-radius: 8px; }
.profile-body {
  display: flex;
  gap: 40px;
  padding: 10px 20px 20px;
  flex-wrap: wrap;
}
.avatar-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding-top: 6px;
}
.avatar-img { background: #409eff; color: #fff; font-size: 40px; }
.avatar-tip { font-size: 12px; color: #909399; }
.field-tip { font-size: 12px; color: #909399; line-height: 1.4; margin-top: 2px; }
.profile-form { flex: 1; min-width: 360px; max-width: 520px; }
.pwd-form { max-width: 460px; padding: 10px 20px; }
.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  padding: 10px 4px 20px;
}
.todo-card {
  background: #f7f9fc;
  border: 1px solid #eef1f6;
  border-radius: 10px;
  padding: 24px 12px;
  text-align: center;
  cursor: pointer;
  transition: box-shadow .2s;
}
.todo-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.08); }
.todo-num { font-size: 30px; font-weight: 700; color: #409eff; }
.todo-label { margin-top: 6px; color: #606266; }
.att-desc { max-width: 640px; margin: 0 4px 20px; }
.todo-actions { padding: 4px; }
.notice-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.toolbar-right { margin-left: auto; }
.notice-pager { margin-top: 14px; justify-content: flex-end; }
.unread { font-weight: 600; color: #303133; }
.pref-form { max-width: 460px; padding: 10px 20px; }
.bind-tip { margin: 4px 0 14px; font-size: 12px; line-height: 1.7; color: var(--el-text-color-secondary); }
</style>
