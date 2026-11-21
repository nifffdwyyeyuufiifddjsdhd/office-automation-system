<script setup lang="ts">
// 通用流程图：发起人、各审批步骤、结束，有分支也画出来
// 流程定义编辑、流程详情、浏览流程图小窗都用它
interface FlowChartBranch {
  to: string;
  condition?: string;
  isDefault?: boolean;
}

interface FlowChartNode {
  name: string;
  role?: string;
  extra?: string;
  status?: 'wait' | 'current' | 'done' | 'reject';
  branches?: FlowChartBranch[];
}

// editable 为真时每个步骤上多出上移、下移、定位、删除按钮，节点之间可以插入新步骤
const props = withDefaults(
  defineProps<{
    creator?: string;
    creatorRole?: string;
    nodes: FlowChartNode[];
    endText?: string;
    compact?: boolean;
    editable?: boolean;
    activeIndex?: number;
  }>(),
  { creator: '', creatorRole: '', endText: '流程结束', compact: false, editable: false, activeIndex: -1 }
);

const emit = defineEmits<{
  (e: 'op', action: 'up' | 'down' | 'edit' | 'remove' | 'insert', index: number): void;
}>();

function op(action: 'up' | 'down' | 'edit' | 'remove' | 'insert', index: number) {
  emit('op', action, index);
}
</script>

<template>
  <div class="flow-chart" :class="{ compact }">
    <div class="fc-node fc-start">
      <span class="fc-badge">发起</span>
      <div class="fc-body">
        <div class="fc-name">{{ creator || '发起人（提交时确定）' }}</div>
        <div v-if="creatorRole" class="fc-meta">{{ creatorRole }}</div>
      </div>
    </div>

    <template v-for="(n, i) in nodes" :key="i">
      <div class="fc-link" :class="{ 'has-op': editable }">
        <button v-if="editable" class="fc-insert" type="button" @click.stop="op('insert', i)">+ 插入步骤</button>
      </div>
      <div
        class="fc-node"
        :class="[n.status ? 'is-' + n.status : '', { 'is-active': editable && activeIndex === i }]"
      >
        <span class="fc-badge">{{ i + 1 }}</span>
        <div class="fc-body">
          <div class="fc-name">{{ n.name || '未命名步骤' }}</div>
          <div v-if="n.role" class="fc-meta">{{ n.role }}</div>
          <div v-if="n.extra" class="fc-extra">{{ n.extra }}</div>
        </div>
        <div v-if="editable" class="fc-ops">
          <button class="fc-op" type="button" title="往上挪一步" @click.stop="op('up', i)">↑</button>
          <button class="fc-op" type="button" title="往下挪一步" @click.stop="op('down', i)">↓</button>
          <button class="fc-op" type="button" title="在左边编辑这一步" @click.stop="op('edit', i)">编辑</button>
          <button class="fc-op is-danger" type="button" title="删掉这一步" @click.stop="op('remove', i)">删</button>
        </div>
      </div>
      <div v-if="n.branches && n.branches.length" class="fc-branches">
        <div v-for="(b, bi) in n.branches" :key="bi" class="fc-branch">
          <span class="fc-branch-tag" :class="{ 'is-default': b.isDefault }">
            {{ b.isDefault ? '都不满足时' : b.condition || '任何情况' }}
          </span>
          <span class="fc-branch-arrow">→</span>
          <span class="fc-branch-to">{{ b.to }}</span>
        </div>
      </div>
    </template>

    <div class="fc-link" :class="{ 'has-op': editable }">
      <button v-if="editable" class="fc-insert" type="button" @click.stop="op('insert', nodes.length)">+ 插入步骤</button>
    </div>
    <div class="fc-node fc-end">
      <span class="fc-badge">结束</span>
      <div class="fc-body">
        <div class="fc-name">{{ endText }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.flow-chart {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0;
  padding: 4px 2px 8px;
}

.fc-node {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  border: 1px solid #dcdfe6;
  background: #fff;
  border-radius: 8px;
  padding: 8px 10px;
}

.fc-start {
  border-color: #b3d8ff;
  background: #f2f9ff;
}

.fc-end {
  border-style: dashed;
  color: #909399;
}

.fc-node.is-current {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.fc-node.is-done {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.fc-node.is-reject {
  border-color: #fab6b6;
  background: #fef0f0;
}

/* 编辑时点过的那一步高亮，方便知道左边滚到了哪张卡片 */
.fc-node.is-active {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.fc-badge {
  flex-shrink: 0;
  min-width: 34px;
  height: 20px;
  line-height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #e8f2ff;
  color: #1677ff;
  font-size: 11px;
  text-align: center;
}

.fc-end .fc-badge {
  background: #f4f4f5;
  color: #909399;
}

.fc-body {
  flex: 1;
  min-width: 0;
}

.fc-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  line-height: 20px;
  word-break: break-all;
}

.fc-meta {
  font-size: 12px;
  color: #606266;
  margin-top: 2px;
  word-break: break-all;
}

.fc-extra {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

/* 图上操作：默认淡一点，鼠标移到这一步才明显 */
.fc-ops {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
  opacity: 0.45;
  transition: opacity 0.15s;
}

.fc-node:hover .fc-ops {
  opacity: 1;
}

.fc-op {
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  font-size: 11px;
  line-height: 18px;
  height: 20px;
  padding: 0 5px;
  border-radius: 4px;
  cursor: pointer;
  font-family: inherit;
}

.fc-op:hover {
  border-color: #1677ff;
  color: #1677ff;
  background: #f0f7ff;
}

.fc-op.is-danger:hover {
  border-color: #f56c6c;
  color: #f56c6c;
  background: #fef0f0;
}

.fc-link {
  width: 2px;
  height: 14px;
  margin-left: 26px;
  background: #dcdfe6;
}

/* 编辑模式下连线变成一条浅色虚线槽，中间放插入按钮 */
.fc-link.has-op {
  width: auto;
  height: 22px;
  margin-left: 0;
  background: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fc-insert {
  border: 1px dashed #c8d6e5;
  background: #fff;
  color: #7a8ca3;
  font-size: 11px;
  line-height: 18px;
  padding: 0 8px;
  border-radius: 10px;
  cursor: pointer;
  font-family: inherit;
  opacity: 0;
  transition: opacity 0.15s;
}

.fc-link.has-op:hover .fc-insert {
  opacity: 1;
}

.fc-insert:hover {
  border-color: #1677ff;
  color: #1677ff;
  background: #f0f7ff;
}

.fc-branches {
  margin: 6px 0 0 26px;
  padding-left: 10px;
  border-left: 2px dashed #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fc-branch {
  font-size: 11px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.fc-branch-tag {
  background: #f4f8ff;
  border: 1px solid #d9ecff;
  color: #1677ff;
  border-radius: 4px;
  padding: 0 5px;
  line-height: 18px;
}

.fc-branch-tag.is-default {
  background: #fdf6ec;
  border-color: #faecd8;
  color: #e6a23c;
}

.fc-branch-arrow {
  color: #c0c4cc;
}

.fc-branch-to {
  color: #303133;
}

/* 小窗里用紧凑版 */
.compact .fc-node {
  padding: 6px 8px;
}

.compact .fc-name {
  font-size: 12px;
}

.compact .fc-link {
  height: 10px;
}
</style>
