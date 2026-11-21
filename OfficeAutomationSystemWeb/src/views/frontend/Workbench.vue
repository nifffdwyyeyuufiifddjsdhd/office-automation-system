<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell, Notebook, EditPen, AlarmClock, Clock, Document, Tickets, User,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { todoCount, todoInstances, type FlowInstanceRow } from '@/api/flow'
import { getStatistics } from '@/api/task'
import { getUnreadCount, getNotificationList, markRead, type NotificationItem } from '@/api/notification'
import { pageAnnouncements, type AnnouncementItem } from '@/api/announcement'
import { getTodayStatus, checkIn, checkOut, type TodayStatus } from '@/api/attendance'

const router = useRouter()
const userStore = useUserStore()

const roleName = computed(() => {
  const map: Record<string, string> = {
    admin: '系统管理员', manager: '部门经理', employee: '员工', hr: '人事专员', finance: '财务专员', gm: '总经理/高管',
  }
  return map[userStore.role] || '员工'
})
const today = new Date()
const week = ['日', '一', '二', '三', '四', '五', '六'][today.getDay()]
const dateText = `${today.getFullYear()}年${today.getMonth() + 1}月${today.getDate()}日 星期${week}`

// 汇总统计
const stats = reactive({ flowTodo: 0, taskTodo: 0, unread: 0, unreadAnn: 0 })

// 今日出勤
const attend = reactive<TodayStatus>({
  canCheckIn: false, canCheckOut: false, isHoliday: false,
  workStart: '', workEnd: '', checkInTime: null, checkOutTime: null,
  status: null, workHours: null,
})
const checking = ref(false)
const attendStatusText = computed(() => {
  if (attend.isHoliday) return '休息日 / 法定节假日'
  if (attend.checkOutTime) return '已完成上下班打卡'
  if (attend.checkInTime) return '已签到，待签退'
  return '尚未打卡'
})
async function loadAttend() {
  try { Object.assign(attend, await getTodayStatus()) } catch { /* 忽略 */ }
}
async function doCheckIn() {
  checking.value = true
  try { await checkIn(null, null); ElMessage.success('签到成功'); await loadAttend() }
  catch { /* 拦截器已提示 */ } finally { checking.value = false }
}
async function doCheckOut() {
  checking.value = true
  try { await checkOut(null, null); ElMessage.success('签退成功'); await loadAttend() }
  catch { /* 拦截器已提示 */ } finally { checking.value = false }
}

// 四面板列表
const todoList = ref<FlowInstanceRow[]>([])
const noticeList = ref<NotificationItem[]>([])
const annList = ref<AnnouncementItem[]>([])

async function loadAll() {
  // 待办流程
  try {
    stats.flowTodo = await todoCount()
    const list = await todoInstances()
    todoList.value = (list || []).slice(0, 5)
  } catch { /* 忽略 */ }
  // 待办任务
  try {
    const st: any = await getStatistics()
    const sc = st.status_count || []
    stats.taskTodo = (sc[2] || 0) + (sc[3] || 0) + (sc[4] || 0)
  } catch { /* 忽略 */ }
  // 未读通知
  try {
    stats.unread = (await getUnreadCount()).count || 0
    noticeList.value = ((await getNotificationList()) || []).slice(0, 5)
  } catch { /* 忽略 */ }
  // 公告（前 5 条 + 未读统计）
  try {
    const page = await pageAnnouncements({ pageNum: 1, pageSize: 50, status: 1 })
    const records = page.records || []
    stats.unreadAnn = records.filter((a) => a.is_read === false).length
    annList.value = records.slice(0, 5)
  } catch { /* 忽略 */ }
}

function go(path: string) { router.push(path) }
function goFlowDetail(id: number) { router.push({ path: '/home/flow-detail', query: { id } }) }
function goAnnouncement() { router.push('/home/announcement') }
async function openNotice(n: NotificationItem) {
  try { await markRead(n.id) } catch { /* 忽略 */ }
  if (n.jumpUrl && n.jumpUrl.startsWith('/home')) {
    router.push(n.jumpUrl)
  } else {
    loadAll()
  }
}
function fmtTime(t?: string | null) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}
function fmtHM(t?: string | null) {
  if (!t) return '--:--'
  return String(t).replace('T', ' ').substring(11, 16)
}

const entries = [
  { title: '公告通知', desc: '查看最新公告', path: '/home/announcement', icon: Bell },
  { title: '工作日志', desc: '撰写与查看日志', path: '/home/worklog', icon: Notebook },
  { title: '发起流程', desc: '提交申请审批', path: '/home/flow-start', icon: EditPen },
  { title: '待办流程', desc: '处理待审批事项', path: '/home/flow-todo', icon: AlarmClock },
  { title: '任务中心', desc: '协作与任务跟进', path: '/home/task', icon: Tickets },
  { title: '出勤打卡', desc: '上下班打卡与补签', path: '/home/attendance', icon: Clock },
]

const statCards = computed(() => [
  { label: '待办流程', value: stats.flowTodo, path: '/home/flow-todo', icon: AlarmClock, color: '#409eff' },
  { label: '待办任务', value: stats.taskTodo, path: '/home/task', icon: Tickets, color: '#67c23a' },
  { label: '未读通知', value: stats.unread, path: '/home/profile', icon: Bell, color: '#e6a23c' },
  { label: '未读公告', value: stats.unreadAnn, path: '/home/announcement', icon: Document, color: '#f56c6c' },
])

onMounted(() => {
  loadAttend()
  loadAll()
})
</script>

<template>
  <div class="workbench">
    <!-- 欢迎条 -->
    <div class="wb-welcome">
      <div class="welcome-left">
        <el-avatar :size="44" class="welcome-avatar">{{ userStore.realName?.charAt(0) || '员' }}</el-avatar>
        <div>
          <div class="welcome-name">欢迎回来，{{ userStore.realName || userStore.username }}（{{ roleName }}）</div>
          <div class="welcome-date">{{ dateText }}</div>
        </div>
      </div>
      <el-button type="primary" plain :icon="User" @click="go('/home/profile')">个人中心</el-button>
    </div>

    <!-- 今日出勤 + 统计卡 -->
    <el-row :gutter="16" class="wb-top">
      <el-col :xs="24" :sm="10" :md="8" :lg="7">
        <div class="attend-card">
          <div class="panel-title">
            <el-icon><Clock /></el-icon><span>今日出勤</span>
            <el-tag size="small" :type="attend.isHoliday ? 'info' : (attend.checkInTime ? 'success' : 'warning')" class="att-tag">
              {{ attendStatusText }}
            </el-tag>
          </div>
          <div class="attend-times">
            <div class="att-time"><span class="att-label">上班</span><b>{{ fmtHM(attend.checkInTime) }}</b><span class="att-sub">{{ attend.workStart || '' }}</span></div>
            <div class="att-time"><span class="att-label">下班</span><b>{{ fmtHM(attend.checkOutTime) }}</b><span class="att-sub">{{ attend.workEnd || '' }}</span></div>
            <div class="att-time"><span class="att-label">工时</span><b>{{ attend.workHours != null ? attend.workHours + 'h' : '--' }}</b></div>
          </div>
          <div class="attend-btns">
            <el-button v-if="attend.isHoliday" size="small" disabled>休息日无需打卡</el-button>
            <template v-else>
              <el-button v-if="attend.canCheckIn" size="small" type="primary" :loading="checking" @click="doCheckIn">上班签到</el-button>
              <el-button v-if="attend.canCheckOut" size="small" type="success" :loading="checking" @click="doCheckOut">下班签退</el-button>
              <el-button size="small" text @click="go('/home/attendance')">考勤详情</el-button>
            </template>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="14" :md="16" :lg="17">
        <div class="stat-row">
          <div v-for="c in statCards" :key="c.label" class="stat-card" @click="go(c.path)">
            <div class="stat-icon" :style="{ background: c.color + '1a', color: c.color }">
              <el-icon :size="22"><component :is="c.icon" /></el-icon>
            </div>
            <div>
              <div class="stat-value" :style="{ color: c.color }">{{ c.value }}</div>
              <div class="stat-label">{{ c.label }}</div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 四面板 -->
    <el-row :gutter="16" class="wb-panels">
      <el-col :xs="24" :md="12">
        <div class="panel">
          <div class="panel-title">
            <el-icon><AlarmClock /></el-icon><span>待办事项</span>
            <el-button link type="primary" size="small" class="more" @click="go('/home/flow-todo')">更多 ›</el-button>
          </div>
          <div v-if="todoList.length" class="item-list">
            <div v-for="t in todoList" :key="t.id" class="item" @click="goFlowDetail(t.id)">
              <div class="item-main">
                <div class="item-title">{{ t.title }}</div>
                <div class="item-sub">{{ t.flowName }} · {{ t.currentStepName || '待处理' }}</div>
              </div>
              <el-tag size="small" type="warning">{{ t.urgency === 2 ? '加急' : '普通' }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无待办流程" :image-size="60" />
        </div>
      </el-col>

      <el-col :xs="24" :md="12">
        <div class="panel">
          <div class="panel-title">
            <el-icon><Bell /></el-icon><span>系统通知</span>
            <el-button link type="primary" size="small" class="more" @click="go('/home/profile')">更多 ›</el-button>
          </div>
          <div v-if="noticeList.length" class="item-list">
            <div v-for="n in noticeList" :key="n.id" class="item" @click="openNotice(n)">
              <div class="item-main">
                <div class="item-title">
                  <span v-if="n.isRead === 0" class="dot"></span>{{ n.title }}
                </div>
                <div class="item-sub">{{ n.content }}</div>
              </div>
              <span class="item-time">{{ fmtTime(n.createdAt) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无未读通知" :image-size="60" />
        </div>
      </el-col>

      <el-col :xs="24" :md="12">
        <div class="panel">
          <div class="panel-title">
            <el-icon><Document /></el-icon><span>公告查看</span>
            <el-button link type="primary" size="small" class="more" @click="goAnnouncement">更多 ›</el-button>
          </div>
          <div v-if="annList.length" class="item-list">
            <div v-for="a in annList" :key="a.id" class="item" @click="goAnnouncement">
              <div class="item-main">
                <div class="item-title">
                  <el-tag v-if="a.is_top" size="small" type="danger" effect="plain" class="top-tag">置顶</el-tag>
                  {{ a.title }}
                </div>
                <div class="item-sub">{{ a.category_name || '公告' }}{{ a.author_name ? ' · ' + a.author_name : '' }}</div>
              </div>
              <span class="item-time">{{ fmtTime(a.publish_time) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无公告" :image-size="60" />
        </div>
      </el-col>

      <el-col :xs="24" :md="12">
        <div class="panel">
          <div class="panel-title">
            <el-icon><EditPen /></el-icon><span>快捷入口</span>
          </div>
          <div class="entry-grid">
            <div v-for="e in entries" :key="e.path" class="entry" @click="go(e.path)">
              <el-icon :size="24" class="entry-icon"><component :is="e.icon" /></el-icon>
              <div class="entry-text">
                <div class="entry-title">{{ e.title }}</div>
                <div class="entry-desc">{{ e.desc }}</div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.workbench { padding: 20px; box-sizing: border-box; height: 100%; overflow-y: auto; background: #f5f7fa; }

.wb-welcome {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, #ffffff 0%, #f0f6ff 100%);
  border: 1px solid #e8eef7; border-radius: 12px; padding: 18px 22px; margin-bottom: 16px;
}
.welcome-left { display: flex; align-items: center; gap: 14px; }
.welcome-avatar { background: #409eff; color: #fff; font-size: 20px; }
.welcome-name { font-size: 18px; font-weight: 700; color: #1f2d3d; }
.welcome-date { margin-top: 4px; font-size: 13px; color: #909399; }

.wb-top { margin-bottom: 4px; }
.attend-card {
  background: #fff; border: 1px solid #ebeef5; border-radius: 12px;
  padding: 16px 18px; height: 100%; box-sizing: border-box;
}
.attend-times { display: flex; gap: 18px; margin: 14px 0; }
.att-time { display: flex; flex-direction: column; gap: 2px; }
.att-label { font-size: 12px; color: #909399; }
.att-time b { font-size: 20px; color: #303133; }
.att-sub { font-size: 11px; color: #c0c4cc; }
.attend-btns { display: flex; gap: 8px; flex-wrap: wrap; }
.att-tag { margin-left: auto; }

.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; height: 100%; }
.stat-card {
  background: #fff; border: 1px solid #ebeef5; border-radius: 12px;
  padding: 16px; display: flex; align-items: center; gap: 12px; cursor: pointer;
  transition: box-shadow .2s, transform .2s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,.07); }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; }
.stat-value { font-size: 24px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 13px; color: #909399; }

.wb-panels { margin-top: 16px; }
.wb-panels .el-col { margin-bottom: 16px; }
.panel {
  background: #fff; border: 1px solid #ebeef5; border-radius: 12px;
  padding: 14px 16px; min-height: 280px; box-sizing: border-box;
}
.panel-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 15px; font-weight: 600; color: #303133; padding-bottom: 10px;
  border-bottom: 1px solid #f0f2f5;
}
.panel-title .more { margin-left: auto; }
.item-list { padding-top: 4px; }
.item {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  padding: 10px 6px; border-radius: 8px; cursor: pointer;
}
.item:hover { background: #f7f9fc; }
.item-main { min-width: 0; flex: 1; }
.item-title {
  font-size: 14px; color: #303133; white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis; display: flex; align-items: center;
}
.item-sub {
  font-size: 12px; color: #909399; margin-top: 3px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.item-time { font-size: 12px; color: #c0c4cc; flex-shrink: 0; }
.dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; background: #f56c6c; margin-right: 6px; flex-shrink: 0; }
.top-tag { margin-right: 6px; }

.entry-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding-top: 10px; }
.entry {
  display: flex; align-items: center; gap: 10px;
  padding: 12px; border: 1px solid #f0f2f5; border-radius: 10px; cursor: pointer;
  transition: all .2s;
}
.entry:hover { border-color: #409eff; background: #f7faff; }
.entry-icon { color: #409eff; }
.entry-title { font-size: 14px; font-weight: 600; color: #303133; }
.entry-desc { font-size: 12px; color: #909399; margin-top: 2px; }

@media (max-width: 1100px) {
  .stat-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
