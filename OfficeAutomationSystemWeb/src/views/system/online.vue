<template>
  <div class="online-page">
    <el-card>
      <div class="toolbar">
        <el-select
          v-if="canSeeDetail"
          v-model="deptId"
          placeholder="按部门筛选"
          clearable
          style="width: 180px"
          @change="loadList"
        >
          <el-option v-for="d in depts" :key="d.id" :label="d.label" :value="d.id" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索姓名/用户名" clearable style="width: 220px" @keyup.enter="loadList" />
        <el-button type="primary" @click="loadList">查询</el-button>
        <el-button @click="loadList">刷新</el-button>
        <span class="tip">在线 {{ list.length }} 人（30分钟内有操作）</span>
      </div>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="username" label="姓名" min-width="120">
          <template #default="{ row }">
            <el-link v-if="canSeeDetail" type="primary" @click="showDetail(row)">{{ row.username }}</el-link>
            <span v-else>{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!canSeeDetail" prop="orgName" label="部门" min-width="130">
          <template #default="{ row }">{{ row.orgName || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="canSeeDetail" prop="ip" label="IP 地址" min-width="140" />
        <el-table-column v-if="canSeeDetail" prop="browser" label="浏览器" min-width="100" />
        <el-table-column v-if="canSeeDetail" prop="os" label="操作系统" min-width="100" />
        <el-table-column v-if="canSeeDetail" prop="device" label="设备" min-width="80" />
        <el-table-column prop="loginTime" label="登录时间" min-width="170">
          <template #default="{ row }">{{ fmt(row.loginTime) }}</template>
        </el-table-column>
        <el-table-column prop="lastActiveTime" label="最后活跃" min-width="170">
          <template #default="{ row }">
            <span>{{ fmt(row.lastActiveTime) }}</span>
            <el-tag size="small" type="success" style="margin-left: 8px">在线</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="foot-tip">
        说明：在线状态以最后活跃时间为准，超过30分钟无操作自动离线。
        <template v-if="!canSeeDetail">为了信息安全，这里只显示姓名与部门，IP、浏览器和设备信息仅管理员可见。</template>
        <template v-else>不支持强制下线，如需踢人请到"用户管理"禁用账号。</template>
      </div>
    </el-card>

    <!-- 用户详情：资料、最近操作、最近登录 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="720px">
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border v-if="detail">
          <el-descriptions-item label="姓名">{{ detail.realName || detail.username }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{ detail.username }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detail.orgName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="detail.online ? 'success' : 'info'">{{ detail.online ? '在线' : '离线' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="手机">{{ detail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登录时间">{{ fmt(detail.lastLoginTime) }}</el-descriptions-item>
          <el-descriptions-item label="登录 IP">{{ detail.lastLoginIp || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前 IP">{{ detail.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="浏览器 / 系统">{{ (detail.browser || '-') + ' / ' + (detail.os || '-') }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">最近操作记录</el-divider>
        <el-table :data="detail?.recentOperations || []" size="small" border max-height="220">
          <el-table-column prop="module" label="模块" width="100" />
          <el-table-column prop="action" label="操作" width="100" />
          <el-table-column prop="url" label="接口" min-width="160" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="125" />
          <el-table-column label="时间" width="175" class-name="nowrap">
            <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
        <el-divider content-position="left">最近登录</el-divider>
        <el-table :data="detail?.recentLogins || []" size="small" border max-height="180">
          <el-table-column label="登录时间" width="180" class-name="nowrap">
            <template #default="{ row }">{{ fmt(row.loginTime) }}</template>
          </el-table-column>
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column label="结果" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.result === 1 ? 'success' : 'danger'">{{ row.result === 1 ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="failReason" label="失败原因" min-width="160" show-overflow-tooltip />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { getOnlineList, getOnlineDirectory, getOnlineDetail, type OnlineUserItem, type OnlineUserDetail } from '@/api/online'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { hasPerm } from '@/utils/permission'
import request from '@/utils/request'

interface DeptNode { id: number; label: string }

const userStore = useUserStore()
// 明细含 IP、浏览器、设备，只有 admin 或 system:online 能看
const canSeeDetail = computed(() => {
  const roles = userStore.roles || []
  return roles.includes('admin') || hasPerm('system:online')
})

const list = ref<OnlineUserItem[]>([])
const keyword = ref('')
const deptId = ref<number | undefined>(undefined)
const depts = ref<DeptNode[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref<OnlineUserDetail | null>(null)
const detailLoading = ref(false)
let timer: number | undefined

function fmt(s?: string | null) {
  if (!s) return '-'
  return s.replace('T', ' ').substring(0, 19)
}

async function loadDepts() {
  if (!canSeeDetail.value) return
  try {
    const data = await request.get<any, DeptNode[]>('/users/org-tree')
    const flat: DeptNode[] = []
    const walk = (arr: any[]) => {
      for (const n of arr) {
        flat.push({ id: n.id, label: n.label })
        if (n.children) walk(n.children)
      }
    }
    walk(data || [])
    depts.value = flat
  } catch {}
}

async function loadList() {
  loading.value = true
  try {
    const params = { deptId: deptId.value, keyword: keyword.value || undefined }
    const data = canSeeDetail.value
      ? await getOnlineList(params)
      : await getOnlineDirectory(params)
    list.value = data || []
  } finally {
    loading.value = false
  }
}

async function showDetail(row: OnlineUserItem) {
  if (!canSeeDetail.value) return
  detailVisible.value = true
  detailLoading.value = true
  detail.value = { ...row } as OnlineUserDetail
  try {
    detail.value = await getOnlineDetail(row.userId)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载用户详情失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  loadDepts()
  loadList()
  timer = window.setInterval(loadList, 60000)
})

// 离开页面停掉轮询
onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.online-page { padding: 16px; }
.toolbar { display: flex; gap: 8px; align-items: center; margin-bottom: 12px; }
.tip { margin-left: 12px; color: #666; font-size: 13px; }
.foot-tip { margin-top: 12px; color: #999; font-size: 12px; }
/* 时间列不换行，折成两行会被行高裁掉 */
:deep(.nowrap .cell) { white-space: nowrap; }
</style>
