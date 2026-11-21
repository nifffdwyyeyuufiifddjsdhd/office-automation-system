<template>
  <div class="public-page">
    <el-card shadow="never">
      <template #header>
        <div class="public-head">
          <span class="public-title">公开公告</span>
          <span class="public-desc">访客可查看的对外公告，无需内部权限</span>
        </div>
      </template>
      <div v-loading="loading" class="public-list">
        <el-empty v-if="!loading && list.length === 0" description="暂无公开公告" />
        <div v-for="a in list" :key="a.id" class="public-item" @click="openDetail(a.id)">
          <div class="public-item-main">
            <span class="public-item-title">{{ a.title }}</span>
            <el-tag v-if="a.isTop" size="small" type="danger" style="margin-left: 6px">置顶</el-tag>
          </div>
          <div class="public-item-meta">
            <span v-if="a.categoryName">{{ a.categoryName }}</span>
            <span>{{ a.publishTime || "" }}</span>
          </div>
          <div v-if="a.summary" class="public-item-summary">{{ a.summary }}</div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" :title="detail?.title || '公告详情'" width="720px" destroy-on-close>
      <div v-loading="detailLoading">
        <div class="public-item-meta" style="margin-bottom: 10px">
          <span v-if="detail?.categoryName">{{ detail.categoryName }}</span>
          <span>{{ detail?.publishTime || "" }}</span>
          <span v-if="detail?.viewCount != null">浏览 {{ detail.viewCount }}</span>
        </div>
        <div :class="themeClass(detail?.theme)" v-html="sanitizeHtml(detail?.content || '')" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { sanitizeHtml } from '@/utils/safeHtml'
import { themeClass } from '@/constants/announcementContent'
import { onMounted, ref } from "vue";
import { getPublicAnnouncement, getPublicAnnouncements, type PublicAnnouncementBrief, type PublicAnnouncementDetail } from "@/api/announcement";

const loading = ref(false);
const list = ref<PublicAnnouncementBrief[]>([]);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PublicAnnouncementDetail | null>(null);

async function load() {
  loading.value = true;
  try {
    list.value = await getPublicAnnouncements(50);
  } catch {
    list.value = [];
  } finally {
    loading.value = false;
  }
}

async function openDetail(id: number) {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = null;
  try {
    detail.value = await getPublicAnnouncement(id);
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.public-page { padding: 16px; }
.public-head { display: flex; align-items: baseline; gap: 10px; }
.public-title { font-size: 16px; font-weight: 600; color: #303133; }
.public-desc { font-size: 12px; color: #909399; }
.public-list { min-height: 120px; }
.public-item { padding: 12px 4px; border-bottom: 1px solid #f0f2f5; cursor: pointer; }
.public-item:hover { background: #f5f7fa; }
.public-item-main { display: flex; align-items: center; }
.public-item-title { font-size: 14px; font-weight: 600; color: #303133; }
.public-item-meta { display: flex; gap: 12px; font-size: 12px; color: #909399; margin-top: 4px; }
.public-item-summary { font-size: 13px; color: #606266; margin-top: 6px; line-height: 1.6; }
/* 正文样式见 announcement-body.css，这里只管间距 */
.ann-body { margin-top: 4px; }
</style>
