<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSidebarMenusByRoles } from '@/config/menu'
import { useUserStore } from '@/store/user'
import { getUnreadCount, getNotificationList, markRead, deleteReadNotifications, toggleTop, type NotificationItem } from '@/api/notification'
import { BellFilled, Clock, ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logout as apiLogout, fetchAvatarUrl, getMyProfile } from '@/api/user'
import { todoCount as fetchTodoCount } from '@/api/flow'
import { getOnlineCount } from '@/api/online'

const route = useRoute()
const router = useRouter()
const userStore=useUserStore()

// 访客只有公开公告页，工作台请求会 403，这里直接跳过
const isGuest = computed(() => userStore.role === 'guest')

// 在线用户明细只给 admin 或 system:online，其他人隐藏入口
const canSeeOnline = computed(() => {
  if (userStore.roles?.includes('admin')) return true
  return userStore.permissions?.includes('system:online') ?? false
})

// 消息中心
const unreadCount = ref(0);
const notifications = ref<NotificationItem[]>([]);

// 新消息提示：标题栏 + 声音
const originalTitle = document.title
function playBeep() {
  try {
    const ctx = new (window.AudioContext || (window as any).webkitAudioContext)()
    const o = ctx.createOscillator()
    const g = ctx.createGain()
    o.connect(g); g.connect(ctx.destination)
    o.frequency.value = 880; o.type = 'sine'
    g.gain.setValueAtTime(0.15, ctx.currentTime)
    g.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.3)
    o.start(); o.stop(ctx.currentTime + 0.3)
  } catch {}
}

async function loadUnread() {
  try {
    const res = await getUnreadCount()
    const prev = unreadCount.value
    unreadCount.value = res.count || 0
    // 有新增未读才提示
    if (unreadCount.value > prev && prev >= 0) {
      playBeep()
    }
    // 标题栏提示
    if (unreadCount.value > 0) {
      document.title = `(${unreadCount.value}条未读) ${originalTitle}`
    } else {
      document.title = originalTitle
    }
  } catch {}
}
async function loadNotifications() {
  try { notifications.value = await getNotificationList(); } catch {}
}
async function handleClickItem(n: NotificationItem) {
  // 点开只跳转不标已读，事情办完后端会清掉通知
  // 免得点一下没了、事没办又冒出来
  if (n.jumpUrl) {
    const raw = n.jumpUrl.replace(/^\//, '');
    const [pathPart, queryPart] = raw.split('?');
    const extraQuery: Record<string, string> = {};
    if (queryPart) {
      queryPart.split('&').forEach((kv) => {
        const [k, v] = kv.split('=');
        if (k) extraQuery[k] = decodeURIComponent(v || '');
      });
    }
    router.push({
      path: '/' + pathPart,
      query: { highlightId: n.bizId, highlightType: n.bizType, ...extraQuery }
    });
  }
}
// 单条标记「已完成」：只是把这条从待处理挪走，不影响别的消息
async function handleDone(n: NotificationItem) {
  if (n.isRead === 0) {
    try {
      await markRead(n.id);
      n.isRead = 1;
      if (unreadCount.value > 0) unreadCount.value -= 1;
    } catch {
      /* 拦截器已提示 */
    }
  }
}
// 清空已完成：把标过完成的记录删掉，没完成的仍然留着
async function handleClearDone() {
  try {
    await ElMessageBox.confirm("只清理已标为完成的消息，未处理的会保留。确定清理？", "清空已完成", { type: "warning" });
  } catch {
    return;
  }
  try {
    await deleteReadNotifications();
    await loadNotifications();
    await loadUnread();
    ElMessage.success("已清理完成的消息");
  } catch {
    /* 拦截器已提示 */
  }
}
const laterVisible = ref(true);
const draggingItem = ref<NotificationItem | null>(null);
// 面板只列没完成的，点过「标记完成」的立刻从列表里消失
const pendingItems = computed(() => notifications.value.filter((n) => !n.isTop && n.isRead === 0));
const laterItems = computed(() => notifications.value.filter((n) => n.isTop));
function handleDragStart(n: NotificationItem) {
  draggingItem.value = n;
}
async function handleDropLater() {
  if (draggingItem.value && !draggingItem.value.isTop) {
    await toggleTop(draggingItem.value.id, true);
    draggingItem.value.isTop = true;
  }
  draggingItem.value = null;
}
async function handleDropMain() {
  if (draggingItem.value && draggingItem.value.isTop) {
    await toggleTop(draggingItem.value.id, false);
    draggingItem.value.isTop = false;
  }
  draggingItem.value = null;
}
const bellContextMenu = reactive({ show: false, x: 0, y: 0, item: null as NotificationItem | null });
function handleContextMenu(e: MouseEvent, n: NotificationItem) {
  bellContextMenu.show = true;
  bellContextMenu.x = e.clientX;
  bellContextMenu.y = e.clientY;
  bellContextMenu.item = n;
  setTimeout(() => window.addEventListener('click', closeBellContextMenu), 0);
}
function closeBellContextMenu() {
  bellContextMenu.show = false;
  window.removeEventListener('click', closeBellContextMenu);
}
async function handleToggleTop(n: NotificationItem | null) {
  if (!n) return;
  await toggleTop(n.id, !n.isTop);
  n.isTop = !n.isTop;
  // 重新排序
  notifications.value.sort((a, b) => Number(b.isTop) - Number(a.isTop));
  bellContextMenu.show = false;
}
function formatTime(t: string) {
  if (!t) return "";
  const d = new Date(t);
  const diff = (Date.now() - d.getTime()) / 1000;
  if (diff < 60) return "刚刚";
  if (diff < 3600) return Math.floor(diff / 60) + "分钟前";
  if (diff < 86400) return Math.floor(diff / 3600) + "小时前";
  return t.replace("T", " ").slice(0, 16);
}

// 待办数（铃铛角标）
const todoCount = ref(0)
async function refreshTodoCount() {
  try {
    todoCount.value = await fetchTodoCount()
  } catch { /* 未登录/接口异常时忽略 */ }
}

// 在线人数
const onlineCount = ref(0)
async function refreshOnlineCount() {
  try {
    const res = await getOnlineCount()
    onlineCount.value = res.count || 0
  } catch {}
}
function goOnline() {
  router.push('/home/online')
}
function goTodo() {
  router.push('/home/flow-todo')
}

// 退出登录
const handleLogout = async () => {
  document.title = originalTitle
  try {
    await apiLogout()
  } catch {
    // 忽略网络错误，本地退出照常进行
  }
  userStore.clearLogin()
  router.push('/login')
}

const avatarText = computed(() => (userStore.realName || userStore.username || '管').charAt(0))
const headerAvatar = ref<string | null>(null)
const goProfile = () => router.push('/home/profile')
const goChangePwd = () => router.push({ path: '/home/profile', query: { tab: 'password' } })

// 侧边栏折叠
const isCollapse = ref(false)
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 菜单配置
// 一个账号可能多角色，菜单取并集，次要角色入口才不会消失
const menus = getSidebarMenusByRoles(
  userStore.roles && userStore.roles.length ? userStore.roles : [userStore.role]
);

// 有子菜单的模块放头部，侧边栏只留叶子菜单
const headerModules = computed(() => (menus as any[]).filter((m: any) => m.children && m.children.length));
// 侧边栏同时放叶子菜单和带子项的模块，两边都能进
const openedGroups = computed(() =>
  (menus as any[])
    .filter((m: any) => (m.children || []).some((c: any) => c.path === route.path))
    .map((m: any) => 'group-' + m.title)
);
// 切换页面时把侧边栏当前项滚进可视区
watch(
  () => route.path,
  () => {
    nextTick(() => {
      const active = document.querySelector('.aside-menu .el-menu-item.is-active') as HTMLElement | null;
      if (active) active.scrollIntoView({ block: 'nearest' });
    });
  },
  { immediate: true }
);

// 当前路由属于哪个头部模块，用来给模块名加高亮
function moduleActive(m: any) {
  return (m.children || []).some((c: any) => c.path && route.path.indexOf(c.path) === 0);
}

function goModuleChild(path?: string) {
  if (path) router.push(path);
}

const menuTitleMap: Record<string, string> = {}
  const collectLeaf = (arr: typeof menus) => {
    for (const m of arr) {
      if (m.children) collectLeaf(m.children)
      else if (m.path) menuTitleMap[m.path] = m.title
    }
  }
  collectLeaf(menus)

// 标签页
interface TabItem {
  path: string
  title: string
}

const HOME_PATH = '/home'
const tabs = ref<TabItem[]>([{ path: HOME_PATH, title: '首页' }])
const activeTab = computed(() => route.path)

const currentTitle = computed(() => {
  return (route.meta?.title as string) || menuTitleMap[route.path] || '页面'
})

const addTab = () => {
  const path = route.path
  if (!path) return
  const title = (route.meta?.title as string) || menuTitleMap[path] || '页面'
  if (!tabs.value.find((t) => t.path === path)) {
    tabs.value.push({ path, title })
  }
}

const switchTab = (tab: TabItem) => {
  if (tab.path !== route.path) router.push(tab.path)
}

// 关闭单个标签：所有标签都可关闭
const closeTab = (tab: TabItem) => {
  const index = tabs.value.findIndex((t) => t.path === tab.path)
  if (index === -1) return

  tabs.value.splice(index, 1)

  // 关掉的是当前激活标签，就切到相邻一个
  if (tab.path === route.path) {
    const next = tabs.value[index] || tabs.value[index - 1]
    if (next) {
      router.push(next.path)
    }
    // 没有其他标签了：URL 不动，内容区显示空白
  }
}

// 菜单点击
const handleMenuSelect = (index: string) => {
  // 标签全空时，恢复一个
  if (tabs.value.length === 0) {
    const title = menuTitleMap[index] || '页面'
    tabs.value.push({ path: index, title })
  }
  if (route.path !== index) {
    router.push(index)
  }
}

// 右键菜单
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  target: null as TabItem | null,
})

const openContextMenu = (e: MouseEvent, tab: TabItem) => {
  contextMenu.visible = true
  bellContextMenu.x = e.clientX
  bellContextMenu.y = e.clientY
  contextMenu.target = tab
}

const closeContextMenu = () => {
  contextMenu.visible = false
}

const handleClose = () => {
  if (contextMenu.target) closeTab(contextMenu.target)
  closeContextMenu()
}

const handleCloseOthers = () => {
  if (!contextMenu.target) return
  const keep = contextMenu.target
  // 只留当前这一个
  tabs.value = [keep]
  if (route.path !== keep.path) {
    router.push(keep.path)
  }
  closeContextMenu()
}

const handleCloseAll = () => {
  clearAllTabs()
  closeContextMenu()
}

const clearAllTabs = () => {
  tabs.value = []
}

// 滚轮横向滚动
const tabsScrollRef = ref<HTMLElement | null>(null)
const handleWheel = (e: WheelEvent) => {
  const el = tabsScrollRef.value
  if (!el) return

  // 只有一个标签时没有意义，不处理
  if (tabs.value.length <= 1) return

  // 内容没有溢出时物理上没有滚动空间，不处理
  if (el.scrollWidth <= el.clientWidth) return

  // 换算不同滚轮模式：0=像素，1=行(约16px)，2=页(约容器宽度)
  let delta = e.deltaY
  if (e.deltaMode === 1) delta *= 16
  else if (e.deltaMode === 2) delta *= el.clientWidth

  // 滚轮向下向右移，向上向左移
  const max = el.scrollWidth - el.clientWidth
  el.scrollLeft = Math.max(0, Math.min(max, el.scrollLeft + delta * 1.5))
}

// 生命周期
let unreadTimer: number | null = null
onMounted(() => {
  addTab()
  document.addEventListener('click', closeContextMenu)
  if (isGuest.value) return
  refreshTodoCount()
  loadUnread()
  refreshOnlineCount()
  getMyProfile().then((p) => {
    if (p.avatar) fetchAvatarUrl(p.avatar).then((url) => { headerAvatar.value = url })
  }).catch(() => {})
  unreadTimer = window.setInterval(loadUnread, 60000)
  window.setInterval(refreshOnlineCount, 60000)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
})

watch(
  () => route.path,
  () => {
    if (tabs.value.length > 0 || route.path) {
      addTab()
    }
    nextTick(() => {
      const el = document.querySelector('.tab.active') as HTMLElement | null
      el?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' })
    })
  }
)
</script>

<template>
  <div class="common-layout">
    <el-container class="layout-container">
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '200px'" class="aside">
        <div class="logo">
          <span v-if="!isCollapse">OA 办公系统</span>
          <span v-else>OA</span>
        </div>

        <h3 class="menu-title" v-if="!isCollapse">菜单</h3>

        <el-menu
          :default-active="route.path"
          :collapse="isCollapse"
          :collapse-transition="false"
          class="aside-menu"
          :default-openeds="openedGroups"
          @select="handleMenuSelect"
        >
          <template v-for="m in menus" :key="m.path || m.title">
            <el-sub-menu v-if="m.children && m.children.length" :index="'group-' + m.title">
              <template #title>
                <el-icon>
                  <component :is="m.icon" />
                </el-icon>
                <span class="menu-text">{{ m.title }}</span>
              </template>
              <el-menu-item v-for="c in m.children" :key="c.path" :index="c.path!">
                <el-icon v-if="c.icon">
                  <component :is="c.icon" />
                </el-icon>
                <template #title>
                  <span class="menu-text">{{ c.title }}</span>
                </template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="m.path!">
              <el-icon>
                <component :is="m.icon" />
              </el-icon>
              <template #title>
                <span class="menu-text">{{ m.title }}</span>
              </template>
            </el-menu-item>
          </template>
        </el-menu>

        <div class="collapse-btn" @click="toggleCollapse">
          {{ isCollapse ? '》》' : '《《' }}
        </div>
      </el-aside>

      <!-- 右侧主区域 -->
      <el-container class="right-container">
        <!-- 白色头部 -->
        <el-header class="header">
          <div class="header-left">
            <div class="header-modules">
              <el-dropdown
                v-for="m in headerModules"
                :key="m.title"
                trigger="hover"
                popper-class="header-module-dropdown"
              >
                <span class="header-module" :class="{ active: moduleActive(m) }">
                  <el-icon><component :is="m.icon" /></el-icon>
                  <span>{{ m.title }}</span>
                  <el-icon class="caret"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="c in m.children"
                      :key="c.path || c.title"
                      @click="goModuleChild(c.path)"
                    >
                      {{ c.title }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <div class="header-right">
            <el-breadcrumb separator="/" class="header-crumb">
              <el-breadcrumb-item>OA</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
            <!-- 在线人数 -->
            <el-button v-if="canSeeOnline" text class="online-btn" @click="goOnline" title="查看在线用户">
              <span class="online-dot"></span>
              <span class="online-text">{{ onlineCount }} 人在线</span>
            </el-button>
            <el-popover placement="bottom-end" :width="560" trigger="click" popper-class="bell-popover" @show="loadNotifications">
            <template #reference>
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="bell-badge">
                <el-button text class="bell-btn">🔔</el-button>
              </el-badge>
            </template>
            <div class="bell-panel">
              <div class="bell-header">
                <span style="font-weight:600">消息通知</span>
                <el-button link type="primary" size="small" @click="handleClearDone">清空已完成</el-button>
              </div>
              <div class="bell-body">
                <!-- 左边：正常消息 -->
                <div class="bell-col bell-col-main" @dragover.prevent @drop.prevent="handleDropMain">
                  <div v-if="pendingItems.length" class="bell-col-label">待处理</div>
                  <div v-if="!pendingItems.length" class="bell-empty">待处理的都处理完了</div>
                  <div
                    v-for="n in pendingItems"
                    :key="n.id"
                    class="bell-item"
                    :class="{ unread: n.isRead === 0, read: n.isRead === 1 }"
                    draggable="true"
                    @dragstart="handleDragStart(n)"
                    @click="handleClickItem(n)"
                    @contextmenu.prevent="handleContextMenu($event, n)"
                  >
                    <div class="bell-item-icon">
                      <el-icon size="18"><BellFilled /></el-icon>
                    </div>
                    <div class="bell-item-body">
                      <div class="bell-item-title-row">
                        <span class="bell-item-title">{{ n.title }}</span>
                        <span v-if="n.count > 1" class="bell-item-count">+{{ n.count - 1 }}</span>
                        <el-button
                          v-if="n.isRead === 0"
                          link
                          type="primary"
                          size="small"
                          class="bell-done"
                          @click.stop="handleDone(n)"
                        >标记完成</el-button>
                      </div>
                      <div class="bell-item-content">{{ n.content }}</div>
                      <div class="bell-item-time">{{ formatTime(n.createdAt) }}</div>
                    </div>
                    <span v-if="n.isRead === 0" class="bell-item-dot"></span>
                  </div>
                </div>
                <!-- 右边：稍后处理（可收起） -->
                <div v-show="laterVisible" class="bell-col bell-col-later" @dragover.prevent @drop.prevent="handleDropLater">
                  <div class="bell-col-label later">
                    <span>稍后处理</span>
                    <el-icon style="cursor:pointer" @click="laterVisible = false"><ArrowDown /></el-icon>
                  </div>
                  <div v-if="!laterItems.length" class="bell-empty">拖消息到这里</div>
                  <div
                    v-for="n in laterItems"
                    :key="n.id"
                    class="bell-item later-item"
                    :class="{ read: n.isRead === 1 }"
                    draggable="true"
                    @dragstart="handleDragStart(n)"
                    @click="handleClickItem(n)"
                    @contextmenu.prevent="handleContextMenu($event, n)"
                  >
                    <div class="bell-item-icon">
                      <el-icon size="18"><Clock /></el-icon>
                    </div>
                    <div class="bell-item-body">
                      <div class="bell-item-title-row">
                        <span class="bell-item-title">{{ n.title }}</span>
                        <span v-if="n.count > 1" class="bell-item-count">+{{ n.count - 1 }}</span>
                      </div>
                      <div class="bell-item-content">{{ n.content }}</div>
                      <div class="bell-item-time">{{ formatTime(n.createdAt) }}</div>
                    </div>
                  </div>
                </div>
                <!-- 收起状态 -->
                <div v-if="!laterVisible" class="bell-later-collapsed" @click="laterVisible = true">
                  <el-icon size="14"><ArrowUp /></el-icon>
                  <span>稍后处理 ({{ laterItems.length }})</span>
                </div>
              </div>
            </div>
            <ul v-if="bellContextMenu.show" class="bell-context-menu" :style="{ left: bellContextMenu.x + 'px', top: bellContextMenu.y + 'px' }" @click.stop>
              <li @click="handleToggleTop(bellContextMenu.item)">{{ bellContextMenu.item?.isTop ? '移回待处理' : '稍后处理' }}</li>
            </ul>
          </el-popover>
            <!-- 头像 + 气泡：显示当前用户，提供退出登录 -->
            <el-popover placement="bottom-end" :width="150" trigger="click" popper-class="user-popover">
              <template #reference>
                <el-avatar :size="32" :src="headerAvatar || undefined" class="avatar-btn">{{ avatarText }}</el-avatar>
              </template>
              <div class="user-pop">
                <div class="pop-name">{{ userStore.realName || userStore.username }}</div>
                <div class="pop-role">{{ ({admin:'系统管理员',manager:'部门经理',hr:'人事专员',finance:'财务专员',gm:'总经理/高管',employee:'普通员工'} as Record<string,string>)[userStore.role] || '普通用户' }}</div>
                <el-divider style="margin:8px 0" />
                <el-button text style="width:100%" @click="goProfile">个人中心</el-button>
                <el-button text style="width:100%" @click="goChangePwd">修改密码</el-button>
                <el-button text type="danger" style="width:100%" @click="handleLogout">退出登录</el-button>
              </div>
            </el-popover>
          </div>
        </el-header>

        <!-- 灰色标签栏 -->
        <div class="tabs-bar">
          <div
            v-if="tabs.length > 0"
            ref="tabsScrollRef"
            class="tabs-scroll"
            @wheel.prevent="handleWheel"
          >
            <div
              v-for="tab in tabs"
              :key="tab.path"
              class="tab"
              :class="{ active: tab.path === activeTab }"
              @click="switchTab(tab)"
              @contextmenu.prevent="openContextMenu($event, tab)"
            >
              <span class="tab-title">{{ tab.title }}</span>
              <!-- 所有标签都有 × -->
              <span
                class="tab-close"
                @click.stop="closeTab(tab)"
              >×</span>
              <span class="tab-tooltip">{{ tab.title }}</span>
            </div>
          </div>

          <div
            v-if="tabs.length > 0"
            class="tabs-clear"
            title="清空全部"
            @click.stop="clearAllTabs"
          >✕</div>
        </div>

        <!-- 内容区 -->
        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <!-- 右键气泡菜单 -->
    <teleport to="body">
      <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ left: bellContextMenu.x + 'px', top: bellContextMenu.y + 'px' }"
      >
        <div class="context-item" @click="handleClose">关闭</div>
        <div class="context-item" @click="handleCloseOthers">关闭其他</div>
        <div class="context-item" @click="handleCloseAll">关闭全部</div>
      </div>
    </teleport>
  </div>
</template>

<style scoped>
/* 根容器 */
.common-layout {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.layout-container {
  height: 100%;
}

/* 侧边栏 */
.aside {
  position: relative;
  height: 100%;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #1a2b4c;
  letter-spacing: 2px;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}

.menu-title {
  margin: 0;
  padding: 12px 16px 6px;
  font-size: 12px;
  color: #999;
  font-weight: 500;
  letter-spacing: 1px;
}

.aside-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  overflow-x: hidden;
}

.aside-menu :deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
  margin: 4px 8px;
  border-radius: 8px;
  color: #333;
}

.aside-menu :deep(.el-menu-item:hover) {
  background: #f0f6ff;
  color: #1677ff;
}

.aside-menu :deep(.el-menu-item.is-active) {
  background: #e8f2ff;
  color: #1677ff;
  font-weight: 600;
}

/* 收起时只留图标，宽度、边距、图标盒都按同一套尺寸算，展开和收起才对得齐 */
.aside-menu.el-menu--collapse {
  width: 64px;
}

.aside-menu.el-menu--collapse :deep(.el-menu-item),
.aside-menu.el-menu--collapse :deep(.el-sub-menu__title) {
  display: flex !important;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 46px;
  line-height: 1;
  margin: 4px 8px;
  padding: 0 !important;
  border-radius: 8px;
  overflow: visible;
}

/* 收起后图标外面还包了一层 tooltip 触发层，它自带缩进，会把图标顶偏，这里压平并居中 */
.aside-menu.el-menu--collapse :deep(.el-menu-tooltip__trigger) {
  display: flex !important;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 0 !important;
  margin: 0 !important;
}

/* 子菜单右侧的小箭头在 64px 里会把图标顶偏，收起时藏掉 */
.aside-menu.el-menu--collapse :deep(.el-sub-menu__icon-arrow) {
  display: none;
}

/* 有子项的标题要和普通条目同高同缩进，图标才对得齐 */
.aside-menu :deep(.el-sub-menu__title) {
  height: 46px;
  line-height: 46px;
  margin: 4px 8px;
  border-radius: 8px;
  color: #333;
}

.aside-menu :deep(.el-sub-menu__title:hover) {
  background: #f0f6ff;
  color: #1677ff;
}

.aside-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #1677ff;
  font-weight: 600;
}

/* 图标固定宽度并垂直居中 */
.aside-menu :deep(.el-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  margin-right: 10px;
  font-size: 18px;
}

.aside-menu :deep(.el-sub-menu .el-menu-item) {
  padding-left: 44px !important;
}

.aside-menu.el-menu--collapse :deep(.el-icon) {
  margin: 0 !important;
  width: 20px;
  height: 20px;
  font-size: 18px;
}

.menu-text {
  line-height: 1;
}

.aside-menu.el-menu--collapse .menu-text {
  display: none;
}

.collapse-btn {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid #f0f0f0;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  letter-spacing: 2px;
  user-select: none;
  transition: background 0.2s, color 0.2s;
}

.collapse-btn:hover {
  background: #f5f7fa;
  color: #1677ff;
}

/* 右侧主区域 */
.right-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 白色头部 */
.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 80px;
  flex-shrink: 0;
}

:deep(.el-header) {
  --el-header-height: 80px;
}

.header-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

/* 头部模块菜单：有子项的模块横着排在白头上 */
.header-modules {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}

.header-modules::-webkit-scrollbar {
  display: none;
}

.header-module {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  cursor: pointer;
  white-space: nowrap;
  outline: none;
  transition: background 0.15s, color 0.15s;
}

.header-module:hover {
  background: #f0f6ff;
  color: #1677ff;
}

.header-module.active {
  background: #e8f2ff;
  color: #1677ff;
}

.header-module .caret {
  font-size: 12px;
  color: #909399;
}

/* 面包屑挪到右侧，字号和字重按用户要求调大 */
.header-crumb {
  margin-right: 4px;
  font-size: 15px;
}

.header-crumb :deep(.el-breadcrumb__inner) {
  font-weight: 600;
  color: #303133;
}

.header-crumb :deep(.el-breadcrumb__item:first-child .el-breadcrumb__inner) {
  color: #909399;
  font-weight: 500;
}

.bell-panel { padding: 0; }
.bell-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #f0f2f5; }
.bell-body { display: flex; height: 460px; }
/* 每条消息之间留出间隔，看起来是一条条卡片而不是一整块 */
.bell-col { flex: 1; overflow-y: auto; padding: 10px; display: flex; flex-direction: column; gap: 8px; }
.bell-col-main { border-right: 1px solid #f0f2f5; }
.bell-col-later { flex: 0 0 216px; background: #fafbfc; }
.bell-col-label { padding: 0 4px 2px; font-size: 12px; color: #909399; font-weight: 500; }
.bell-col-label.later { display: flex; justify-content: space-between; align-items: center; }
.bell-empty { text-align: center; color: #c0c4cc; padding: 26px 0; font-size: 12px; }
.bell-item { display: flex; align-items: flex-start; gap: 10px; padding: 10px 12px; border: 1px solid transparent; border-radius: 8px; background: #f6f7f9; cursor: pointer; position: relative; transition: background 0.15s, border-color 0.15s; }
.bell-item:hover { background: #e8f2ff; border-color: #b9d7ff; }
.bell-item.unread { background: #f6f7f9; }
.bell-item.unread .bell-item-title { color: #303133; font-weight: 600; }
.bell-item.read { background: #f7f8fa; }
.bell-item.read .bell-item-title { color: #8b909a; font-weight: 400; }
.bell-item.read .bell-item-content { color: #a8abb2; }
.bell-item-icon { flex-shrink: 0; width: 28px; height: 28px; border-radius: 6px; background: #e9edf3; color: #909399; display: flex; align-items: center; justify-content: center; }
.bell-item.unread .bell-item-icon { background: #409eff; color: #fff; }
.bell-item:hover .bell-item-icon { background: #409eff; color: #fff; }
.later-item { background: #fff8ec; border-color: #f6e2be; }
.later-item .bell-item-icon { background: #fdf0d8; color: #e6a23c; }
.bell-item-body { flex: 1; min-width: 0; }
.bell-item-title-row { display: flex; align-items: center; gap: 6px; }
.bell-item-title { font-size: 13px; font-weight: 500; color: #303133; }
.bell-item-count { font-size: 11px; color: #fff; background: #f56c6c; border-radius: 10px; padding: 0 6px; line-height: 18px; }
.bell-item-content { font-size: 12px; color: #606266; margin-top: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bell-item-time { font-size: 11px; color: #909399; margin-top: 4px; }
/* 标记完成按钮只在悬停时露出来，避免每条都挂一个按钮显得乱 */
.bell-done {
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.15s;
}

.bell-item:hover .bell-done {
  opacity: 1;
}

.bell-item-dot { position: absolute; top: 12px; right: 10px; width: 8px; height: 8px; border-radius: 50%; background: #f56c6c; }
.bell-later-collapsed { display: flex; align-items: center; gap: 4px; padding: 8px 12px; font-size: 12px; color: #e6a23c; cursor: pointer; border-left: 1px solid #f0f2f5; align-self: flex-start; background: #fff8ec; border-radius: 8px; margin: 10px 10px 0 0; }
.bell-context-menu { position: fixed; z-index: 9999; background: #fff; border: 1px solid #e4e7ed; border-radius: 6px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); padding: 4px 0; min-width: 100px; margin: 0; list-style: none; }
.bell-context-menu li { padding: 8px 16px; font-size: 13px; cursor: pointer; color: #303133; }
.bell-context-menu li:hover { background: #f5f7fa; color: #409eff; }
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.online-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #52c41a;
}
.online-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c41a;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2);
}
.online-text { color: #52c41a; }

.avatar-btn {
  cursor: pointer;
  background: #1677ff;
  color: #fff;
  flex-shrink: 0;
}

.user-pop {
  text-align: center;
}

.pop-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.pop-role {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

/* 灰色标签栏 */
.tabs-bar {
  height: 40px;
  background: #e8eaed;
  display: flex;
  align-items: stretch;
  flex-shrink: 0;
  position: relative;
  border-bottom: 1px solid #d0d3d6;
  padding: 0;
}

.tabs-scroll {
  display: flex;
  gap: 0;
  overflow-x: auto;
  overflow-y: hidden;
  flex: 1;
  align-items: stretch;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.tabs-scroll::-webkit-scrollbar {
  display: none;
}

/* 单个标签 */
.tab {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px 0 12px;
  height: 100%;
  min-width: 100px;
  max-width: 200px;
  background: transparent;
  font-size: 13px;
  color: #5f6368;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
  border-right: 1px solid #d0d3d6;
  transition: background 0.15s, color 0.15s;
}

.tab:hover {
  background: rgba(255, 255, 255, 0.5);
  color: #202124;
}

.tab.active {
  background: #fff;
  color: #1677ff;
  font-weight: 500;
  border-right-color: transparent;
}

.tab.active::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: #fff;
}

.tab-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 关闭按钮：默认隐藏，hover / active 时显示 */
.tab-close {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  font-size: 15px;
  line-height: 1;
  color: inherit;
  opacity: 0;
  transition: background 0.15s, opacity 0.15s;
}

.tab:hover .tab-close,
.tab.active .tab-close {
  opacity: 1;
}

.tab-close:hover {
  background: rgba(0, 0, 0, 0.12);
}

.tab.active .tab-close:hover {
  background: rgba(22, 119, 255, 0.18);
}

/* 悬停显示完整标题 */
.tab-tooltip {
  position: absolute;
  top: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%) translateY(-4px);
  background: #303133;
  color: #fff;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.15s, transform 0.15s;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.tab-tooltip::before {
  content: '';
  position: absolute;
  top: -4px;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 8px;
  height: 8px;
  background: #303133;
}

.tab:hover .tab-tooltip {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

/* 清空全部 */
.tabs-clear {
  flex-shrink: 0;
  width: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #909399;
  border-left: 1px solid #d0d3d6;
  transition: color 0.15s, background 0.15s;
  font-size: 14px;
}

.tabs-clear:hover {
  color: #f56c6c;
  background: rgba(255, 255, 255, 0.5);
}

/* 内容区 */
.main {
  background: #fff;
  padding: 0;
  overflow: auto;
  flex: 1;
  min-height: 0;
}

.blank-main {
  width: 100%;
  height: 100%;
  background: #fff;
}

/* 右键气泡菜单 */
.context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 120px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  padding: 6px;
  animation: fadeIn 0.12s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.context-item {
  padding: 8px 14px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}

.context-item:hover {
  background: #f0f6ff;
  color: #1677ff;
}
</style>
