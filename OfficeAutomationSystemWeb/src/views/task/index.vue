<template>
  <div class="task-page">
    <div class="page-header">
      <h2>任务中心</h2>
      <el-tag v-if="!canAccessTask" type="info" size="small">未开通</el-tag>
      <div class="head-right">
        <el-radio-group v-if="canAccessTask" v-model="viewMode" size="small" @change="onViewModeChange">
          <el-radio-button value="list">列表</el-radio-button>
          <el-radio-button value="board">看板</el-radio-button>
        </el-radio-group>
        <el-button v-if="canCreateTask" type="primary" @click="openCreate">新建任务</el-button>
      </div>
    </div>

    <el-empty v-if="!canAccessTask" description="任务中心还没有对您开放">
      <div class="no-access-tip">
        被项目经理或部门主管指派任务后，这里会自动开通，只显示与您相关的任务。<br />
        如果需要提前开通，请联系项目经理或系统管理员在「用户管理 / 项目成员」里给您发放任务权限。
      </div>
    </el-empty>

    <div v-show="canAccessTask && viewMode === 'list'">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="我创建的" name="created" />
      <el-tab-pane label="我负责的" name="assigned" />
      <el-tab-pane label="我参与的" name="joined" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

    <!-- 筛选栏 -->
    <el-form :inline="true" class="filter-bar" @submit.prevent>
      <el-form-item label="关键词">
        <el-input v-model="filters.keyword" placeholder="任务名称" clearable style="width: 180px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option v-for="(t, i) in STATUS_TEXT" :key="i" :label="t" :value="i" />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级">
        <el-select v-model="filters.priority" placeholder="全部优先级" clearable style="width: 120px">
          <el-option label="低" :value="1" />
          <el-option label="中" :value="2" />
          <el-option label="高" :value="3" />
          <el-option label="紧急" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="projects.length" label="项目">
        <el-select v-model="filters.projectId" placeholder="全部项目" clearable filterable style="width: 180px">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="isAdminUser" label="范围">
        <el-radio-group v-model="filters.archived" size="small" @change="onSearch">
          <el-radio-button :value="false">正常</el-radio-button>
          <el-radio-button :value="true">已归档</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="task_no" label="编号" width="170" />
      <el-table-column prop="title" label="任务名称" min-width="180">
        <template #default="{ row }">
          <div class="fade-overflow" @click="openDetail(row)">{{ row.title }}</div>
        </template>
      </el-table-column>
      <el-table-column label="所属项目" width="150">
        <template #default="{ row }">
          <span v-if="row.project_name" class="project-name">{{ row.project_name }}</span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="priorityType(row.priority)" size="small">{{ priorityText(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          <el-tag v-if="row.archive_status === 1" type="info" size="small" effect="plain" style="margin-top: 2px">已归档</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="130">
        <template #default="{ row }">
          <el-progress :percentage="row.progress || 0" :stroke-width="14" />
        </template>
      </el-table-column>
      <el-table-column prop="creator_name" label="创建人" width="100" />
      <el-table-column prop="assignee_name" label="负责人" width="100" />
      <el-table-column label="截止时间" width="170">
        <template #default="{ row }">
          <div>{{ formatDate(row.plan_end_time) }}</div>
          <el-tag v-if="row.is_overdue" type="danger" size="small">逾期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button v-if="canDeleteTask(row)" link type="danger" size="small" @click="doDeleteTask(row)">删除</el-button>
          <el-button v-if="canEdit(row)" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canClaim(row)" link type="success" size="small" @click="doClaim(row)">认领</el-button>
          <el-button v-if="canAccept(row)" link type="success" size="small" @click="doAccept(row)">接受</el-button>
          <el-button v-if="canReject(row)" link type="danger" size="small" @click="doReject(row)">拒绝</el-button>
          <el-button v-if="canProgress(row)" link type="primary" size="small" @click="openProgress(row)">更新进度</el-button>
          <el-button v-if="canSubmit(row)" link type="warning" size="small" @click="openSubmit(row)">提交完成</el-button>
          <el-button v-if="canApprove(row)" link type="success" size="small" @click="doApprove(row)">验收通过</el-button>
          <el-button v-if="canRejectApprove(row)" link type="danger" size="small" @click="doRejectApprove(row)">驳回</el-button>
          <el-button v-if="canReassign(row)" link type="primary" size="small" @click="openReassign(row)">重新指派</el-button>
          <el-button v-if="canRevoke(row)" link type="info" size="small" @click="doRevoke(row)">撤销</el-button>
          <el-button v-if="canArchive(row)" link type="info" size="small" @click="doArchive(row)">归档</el-button>
          <el-button v-if="canRestore(row)" link type="warning" size="small" @click="doRestore(row)">恢复</el-button>
          <el-button v-if="canUnarchive(row)" link type="success" size="small" @click="doUnarchive(row)">恢复归档</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      :total="total"
      :page-size="pageSize"
      layout="total, prev, pager, next"
      @current-change="loadData"
    />
    </div>

    <!-- 看板视图 -->
    <div v-show="canAccessTask && viewMode === 'board'" class="board-view">
      <div class="stat-bar">
        <div class="stat-item"><div class="stat-num">{{ stats.total }}</div><div class="stat-label">任务总数</div></div>
        <div class="stat-item"><div class="stat-num">{{ stats.completion_rate }}%</div><div class="stat-label">完成率（{{ stats.finished }}）</div></div>
        <div class="stat-item danger"><div class="stat-num">{{ stats.overdue }}</div><div class="stat-label">逾期</div></div>
        <div class="stat-item"><div class="stat-num">{{ statusCount[2] || 0 }}</div><div class="stat-label">进行中</div></div>
        <div class="stat-item"><div class="stat-num">{{ stats.avg_actual_hours }}</div><div class="stat-label">平均实际工时</div></div>
        <div class="stat-item">
          <div class="stat-num" :class="{ 'diff-up': Number(stats.avg_hour_diff) > 0, 'diff-down': Number(stats.avg_hour_diff) < 0 }">
            {{ Number(stats.avg_hour_diff) > 0 ? '+' : '' }}{{ stats.avg_hour_diff }}
          </div>
          <div class="stat-label">平均工时偏差(h)</div>
        </div>
      </div>

      <div class="board">
        <div v-for="col in BOARD_COLS" :key="col.s" class="board-col">
          <div class="board-col-head" :class="col.cls">
            <span>{{ col.name }}</span>
            <span class="board-count">{{ colTasks(col.s).length }}</span>
          </div>
          <div class="board-col-body"
            :class="{ 'drag-over': dragOverCol === col.s }"
            @dragover.prevent="dragOverCol = col.s"
            @dragleave.prevent="dragOverCol = null"
            @drop.prevent="onDrop(col.s)">
            <div v-for="t in colTasks(col.s)" :key="t.id" class="board-card"
              draggable="true"
              @dragstart="onDragStart(t, $event)"
              @dragend="dragOverCol = null"
              @click="openDetail(t)">
              <div class="board-card-title">{{ t.title }}</div>
              <div class="board-card-meta">
                <el-tag :type="priorityType(t.priority)" size="small">{{ priorityText(t.priority) }}</el-tag>
                <el-tag v-if="t.is_overdue" type="danger" size="small">逾期</el-tag>
              </div>
              <el-progress :percentage="t.progress || 0" :stroke-width="8" :show-text="false" />
              <div class="board-card-foot">
                <span>负责人：{{ t.assignee_name || '未指派' }}</span>
              </div>
              <div class="board-card-due">截止：{{ formatDate(t.plan_end_time) }}</div>
            </div>
            <div v-if="colTasks(col.s).length === 0" class="board-empty">拖拽任务到此处</div>
          </div>
        </div>
      </div>
      <p class="board-tip">提示：待分配卡片拖到「进行中」即认领；待接受拖到「进行中」即接受；进行中拖到「待验收」需填写完成说明；待验收拖到「已完成」即验收。其他跨列流转不支持，请在详情内操作。</p>
    </div>

    <!-- 新建 / 编辑任务弹窗 -->
    <el-dialog v-model="formVisible" :title="editMode ? '编辑任务' : '新建任务'" width="600px">
      <el-form :model="taskForm" label-width="90px">
        <el-form-item label="任务名称" required>
          <el-input v-model="taskForm.title" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="taskForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item v-if="projects.length" label="所属项目">
          <el-select v-model="taskForm.project_id" clearable filterable style="width: 100%"
            placeholder="不选则为独立任务" @change="onProjectChange">
            <el-option v-for="p in projects" :key="p.id" :label="p.project_no + '　' + p.name" :value="p.id" />
          </el-select>
          <div v-if="taskForm.project_id" class="form-tip">负责人与参与人只能从该项目成员中选，任务进度会汇总到项目</div>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="taskForm.priority">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
            <el-option label="紧急" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item :label="editMode ? '负责人' : '负责人'">
          <el-select v-model="taskForm.assignee_id" clearable filterable
            :disabled="editMode"
            :placeholder="editMode ? '编辑页不可改负责人，请到详情重新指派' : '不指派则任务进入待分配'">
            <el-option v-for="u in peopleOptions" :key="u.id" :label="u.real_name" :value="u.id" />
          </el-select>
          <div v-if="editMode" class="form-tip">如需更换负责人，请在详情中使用「重新指派」</div>
        </el-form-item>
        <el-form-item label="参与人">
          <el-select v-model="taskForm.member_ids" multiple filterable clearable
            placeholder="选择参与人（可多选）" style="width: 100%">
            <el-option v-for="u in peopleOptions" :key="u.id" :label="u.real_name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开始">
          <el-date-picker v-model="taskForm.plan_start_time" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择开始时间" />
        </el-form-item>
        <el-form-item label="计划结束">
          <el-date-picker v-model="taskForm.plan_end_time" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择结束时间" />
        </el-form-item>
        <el-form-item label="计划工时">
          <el-input-number v-model="taskForm.plan_work_hours" :min="0" :precision="1" :step="0.5" controls-position="right" placeholder="小时数" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskSubmitting" @click="submitTaskForm">{{ editMode ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- 重新指派弹窗 -->
    <el-dialog v-model="reassignVisible" title="重新指派负责人" width="440px">
      <el-form label-width="80px">
        <el-form-item label="任务">
          <span>{{ reassignForm.title }}</span>
        </el-form-item>
        <el-form-item label="负责人" required>
          <el-select v-model="reassignForm.assignee_id" filterable placeholder="请选择负责人">
            <el-option v-for="u in users" :key="u.id" :label="u.real_name" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reassignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReassign">确认指派</el-button>
      </template>
    </el-dialog>

    <!-- 更新进度弹窗 -->
    <el-dialog v-model="progressVisible" title="更新任务进度" width="460px">
      <div class="progress-dialog">
        <div class="progress-task">{{ progressForm.title }}</div>
        <el-slider v-model="progressForm.progress" show-input :min="0" :max="100" :step="1" />
        <el-input v-model="progressForm.work_note" type="textarea" :rows="3" class="work-note"
          placeholder="工作备注（选填，如：已完成登录模块，联调中）" />
      </div>
      <template #footer>
        <el-button @click="progressVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProgress">保存</el-button>
      </template>
    </el-dialog>

    <!-- 提交完成弹窗 -->
    <el-dialog v-model="submitVisible" title="提交完成" width="480px">
      <el-form label-width="90px">
        <el-form-item label="任务">
          <span>{{ submitForm.title }}</span>
        </el-form-item>
        <el-form-item label="完成说明" required>
          <el-input v-model="submitForm.complete_note" type="textarea" :rows="4"
            placeholder="请填写本次完成的内容、交付物、遗留问题等，供验收人判断" />
        </el-form-item>
        <el-form-item label="实际工时">
          <el-input-number v-model="submitForm.actual_work_hours" :min="0" :precision="1" :step="0.5"
            controls-position="right" placeholder="小时数（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFinish">提交验收</el-button>
      </template>
    </el-dialog>

    <!-- 添加成员弹窗 -->
    <el-dialog v-model="memberVisible" title="添加任务成员" width="420px">
      <el-form label-width="80px">
        <el-form-item label="成员" required>
          <el-select v-model="memberForm.user_id" filterable clearable placeholder="选择用户" style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :label="u.real_name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="memberForm.role">
            <el-option label="参与人" :value="2" />
            <el-option label="关注人" :value="3" />
            <el-option label="验收人" :value="4" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddMember">添加</el-button>
      </template>
    </el-dialog>

    <!-- 任务详情弹窗 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="760px" top="6vh">
      <div v-if="detail" class="detail-box">
        <div class="detail-head">
          <h3>{{ detail.title }}</h3>
          <el-tag :type="statusType(detail.status)">{{ statusText(detail.status) }}</el-tag>
          <el-tag v-if="detail.is_overdue" type="danger" size="small">逾期</el-tag>
          <el-tag v-if="archived(detail)" type="info" size="small" effect="plain">已归档（只读）</el-tag>
          <el-button v-if="canEdit(detail)" link type="primary" size="small" class="head-edit" @click="openEdit(detail)">编辑</el-button>
        </div>
        <el-alert v-if="archived(detail)" type="info" :closable="false" show-icon class="reject-alert"
          title="该任务已归档，仅可查看；管理员可恢复归档。" />
        <p class="meta">{{ detail.task_no }} · 创建人：{{ detail.creator_name }} · 负责人：{{ detail.assignee_name }}</p>

        <el-alert v-if="detail.status === 6 && detail.reject_reason" type="error" :closable="false" class="reject-alert"
          :title="'拒绝原因：' + detail.reject_reason" />

        <p v-if="detail.description" class="desc">{{ detail.description }}</p>

        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="所属项目">{{ detail.project_name || '独立任务' }}</el-descriptions-item>
<el-descriptions-item label="优先级">{{ priorityText(detail.priority) }}</el-descriptions-item>
          <el-descriptions-item label="进度">{{ detail.progress }}%</el-descriptions-item>
          <el-descriptions-item label="计划开始">{{ formatDate(detail.plan_start_time) }}</el-descriptions-item>
          <el-descriptions-item label="计划结束">{{ formatDate(detail.plan_end_time) }}</el-descriptions-item>
          <el-descriptions-item label="实际开始">{{ formatDate(detail.actual_start_time || detail.accept_time) }}</el-descriptions-item>
          <el-descriptions-item label="实际结束">{{ formatDate(detail.actual_end_time || detail.submit_time) }}</el-descriptions-item>
          <el-descriptions-item label="计划工时">{{ detail.plan_work_hours != null ? detail.plan_work_hours + ' 小时' : '—' }}</el-descriptions-item>
          <el-descriptions-item label="实际工时">{{ detail.actual_work_hours != null ? detail.actual_work_hours + ' 小时' : '—' }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="detail.complete_note">
          <el-divider content-position="left">完成说明</el-divider>
          <div class="complete-note">{{ detail.complete_note }}</div>
        </template>

        <!-- 详情内操作按钮 -->
        <div class="detail-actions">
          <el-button v-if="canDeleteTask(detail)" type="danger" size="small" @click="doDeleteTask(detail)">删除任务</el-button>
          <el-button v-if="canClaim(detail)" type="success" size="small" @click="doClaim(detail)">认领任务</el-button>
          <el-button v-if="canAccept(detail)" type="success" size="small" @click="doAccept(detail)">接受任务</el-button>
          <el-button v-if="canReject(detail)" type="danger" size="small" @click="doReject(detail)">拒绝任务</el-button>
          <el-button v-if="canProgress(detail)" type="primary" size="small" @click="openProgress(detail)">更新进度</el-button>
          <el-button v-if="canSubmit(detail)" type="warning" size="small" @click="openSubmit(detail)">提交完成</el-button>
          <el-button v-if="canApprove(detail)" type="success" size="small" @click="doApprove(detail)">验收通过</el-button>
          <el-button v-if="canRejectApprove(detail)" type="danger" size="small" @click="doRejectApprove(detail)">验收驳回</el-button>
          <el-button v-if="canReassign(detail)" type="primary" size="small" @click="openReassign(detail)">重新指派</el-button>
          <el-button v-if="canRevoke(detail)" size="small" @click="doRevoke(detail)">撤销任务</el-button>
          <el-button v-if="canRestore(detail)" type="warning" size="small" @click="doRestore(detail)">恢复任务</el-button>
          <el-button v-if="canArchive(detail)" size="small" @click="doArchive(detail)">归档</el-button>
          <el-button v-if="canUnarchive(detail)" type="success" size="small" @click="doUnarchive(detail)">恢复归档</el-button>
        </div>

        <!-- 成员 -->
        <el-divider content-position="left">任务成员（{{ detail.members ? detail.members.length : 0 }}）</el-divider>
        <div class="member-list">
          <el-tag v-for="m in detail.members" :key="m.user_id" class="member-tag" size="small"
            :type="m.member_role === 1 ? 'success' : 'info'"
            :closable="canRemoveMember(m)" @close="doRemoveMember(m)">
            {{ m.real_name }}（{{ memberRoleText(m.member_role) }}）
          </el-tag>
          <el-button v-if="canManageMember(detail) && [0,1,2,3,6].includes(detail.status)"
            link type="primary" size="small" @click="openAddMember">+ 添加成员</el-button>
        </div>

        <!-- 操作日志 -->
        <el-divider content-position="left">操作记录</el-divider>
        <el-timeline class="log-timeline">
          <el-timeline-item v-for="(l, i) in detail.logs" :key="i" :timestamp="formatDate(l.operation_time)" placement="top">
            <span class="log-name">{{ l.operator_name }}</span> {{ actionText(l.action) }}
            <span v-if="l.remark" class="log-remark">（{{ l.remark }}）</span>
          </el-timeline-item>
        </el-timeline>

        <!-- 任务附件 -->
        <el-divider content-position="left">任务附件（{{ detail.attachments ? detail.attachments.length : 0 }}/3）</el-divider>
        <div class="att-list">
          <div v-for="a in detail.attachments" :key="a.id" class="att-row">
            <span class="att-name">📎 {{ a.file_name }}</span>
            <span class="att-size">{{ formatSize(a.file_size) }}</span>
            <span class="att-meta">{{ a.uploader_name }} · {{ formatDate(a.created_at) }}</span>
            <el-button link type="primary" size="small" @click="downloadAtt(a.id)">下载</el-button>
            <el-button v-if="canDeleteAtt(a)" link type="danger" size="small" @click="removeAtt(a)">删除</el-button>
          </div>
          <el-upload v-if="canUploadAtt(detail)" :show-file-list="false" :before-upload="beforeAttUpload"
            :http-request="handleTaskUpload">
            <el-button size="small" :loading="taskUploading">+ 上传附件（最多3个）</el-button>
          </el-upload>
        </div>

        <!-- 评论 -->
        <el-divider content-position="left">评论（{{ detail.comments ? detail.comments.length : 0 }}）</el-divider>
        <div class="comment-box">
          <div v-for="c in detail.comments" :key="c.id" class="comment-item" :class="{ 'comment-reply': c.parent_id > 0 }">
            <b>{{ c.real_name }}</b>
            <span v-if="c.parent_id > 0" class="reply-to">回复 @{{ parentName(c.parent_id) }}</span>
            <span class="time">{{ formatDate(c.comment_time || c.created_at) }}</span>
            <el-button link type="primary" size="small" @click="setReply(c)">回复</el-button>
            <el-button v-if="canDeleteComment(c)" link type="danger" size="small" @click="doDeleteComment(c)">删除</el-button>
            <p>{{ c.content }}</p>
            <div v-if="c.attachment" class="comment-att">
              <el-button link type="primary" size="small" @click="downloadAtt(c.attachment.id)">
                📎 {{ c.attachment.file_name }}（{{ formatSize(c.attachment.file_size) }}）
              </el-button>
            </div>
          </div>
          <div v-if="replyTo && !archived(detail)" class="reply-tip">
            正在回复 <b>@{{ replyTo.name }}</b>
            <el-button link type="info" size="small" @click="cancelReply">取消</el-button>
          </div>
          <div class="mention-bar" v-if="canCollaborateTask(detail) && detail.members && detail.members.length">
            <span class="mention-label">@成员：</span>
            <el-button v-for="m in detail.members" :key="m.user_id" link size="small" @click="mentionMember(m)">@{{ m.real_name }}</el-button>
          </div>
          <div class="comment-input" v-if="canCollaborateTask(detail)">
            <el-input v-model="commentText" :placeholder="replyTo ? '回复 @' + replyTo.name + '：' : '发表评论...'" type="textarea" :rows="2" />
            <div class="comment-actions">
              <input ref="commentFileRef" type="file" style="display:none" @change="onCommentFileChange" />
              <el-button size="small" @click="commentFileRef?.click()" :loading="commentUploading">📎 附件</el-button>
              <el-tag v-if="commentAttachment" closable size="small" type="success" @close="commentAttachment = null">
                {{ commentAttachment.fileName }}
              </el-tag>
              <el-button type="primary" @click="submitComment">发送</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getTasks, getTaskDetail, createTask, updateTask, reassignTask, acceptTask, rejectTask,
  claimTask, updateProgress, submitTask, approveTask, rejectApproveTask, revokeTask,
  addComment, addTaskMember, removeTaskMember, getBoard, getStatistics, moveTask,
  uploadTaskAttachment, downloadTaskAttachment, deleteTaskAttachment,
  archiveTask, unarchiveTask, restoreTask, deleteTaskComment, deleteTask
} from '@/api/task'
import { listUserOptions } from '@/api/user'
import { getProjectOptions, getProjectDetail } from '@/api/project'
import { useUserStore } from '@/store/user'
import { hasPerm } from '@/utils/permission'

const route = useRoute()
const userStore = useUserStore()
const uid = Number(userStore.userId) || Number(sessionStorage.getItem('userId')) || 0
// 全部任务、统计、归档凭 task:view-all，总经理用这个
const isAdminUser = computed(() => hasPerm('task:view-all'))
// 管理员有全部任务权限含恢复归档，gm 全局只读
const isTaskAdmin = computed(() => (userStore.roles || []).includes('admin'))
// 任务中心访问权默认只给管理角色，员工被派任务时后端自动开
const canAccessTask = computed(() => hasPerm('task:access'))
// 能建任务的有管理员、部门主管和持 task:add 的项目经理
const canCreateTask = computed(() => {
  const roles = userStore.roles || []
  return roles.includes('admin') || roles.includes('manager') || hasPerm('task:add')
})

const activeTab = ref('created')
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const users = ref<any[]>([])
const filters = ref<any>({ keyword: '', status: null, priority: null, projectId: null, archived: false })

// 看板 / 统计
const viewMode = ref<'list' | 'board'>('list')
const BOARD_COLS = [
  { s: 0, name: '待分配', cls: 'c-pending' },
  { s: 1, name: '待接受', cls: 'c-wait' },
  { s: 2, name: '进行中', cls: 'c-doing' },
  { s: 3, name: '待验收', cls: 'c-check' },
  { s: 4, name: '已完成', cls: 'c-done' }
]
const boardColumns = ref<Record<number, any[]>>({ 0: [], 1: [], 2: [], 3: [], 4: [] })
const stats = ref<any>({ total: 0, finished: 0, completion_rate: 0, overdue: 0, avg_actual_hours: 0, avg_hour_diff: 0, status_count: [0, 0, 0, 0, 0, 0, 0] })
const statusCount = computed(() => stats.value.status_count || [])
const dragTask = ref<any>(null)
const dragOverCol = ref<number | null>(null)

const formVisible = ref(false)
const editMode = ref(false)
const editId = ref<number | null>(null)
const taskForm = ref<any>({})

const reassignVisible = ref(false)
const reassignForm = ref<any>({ id: null, title: '', assignee_id: null })

const progressVisible = ref(false)
const progressForm = ref<any>({ id: null, title: '', progress: 0, work_note: '' })

const submitVisible = ref(false)
const submitForm = ref<any>({ id: null, title: '', complete_note: '', actual_work_hours: undefined })

const memberVisible = ref(false)
const memberForm = ref<any>({ user_id: null, role: 2 })

const detailVisible = ref(false)
const detail = ref<any>(null)
const commentText = ref('')
const replyTo = ref<any>(null)
const mentionUserIds = ref<number[]>([])

// 状态：0待分配 1待接受 2进行中 3待验收 4已完成 5已撤销 6已拒绝
const STATUS_TEXT = ['待分配', '待接受', '进行中', '待验收', '已完成', '已撤销', '已拒绝']
const STATUS_TYPE = ['info', 'warning', 'primary', 'warning', 'success', 'info', 'danger']
function statusText(s: number) { return STATUS_TEXT[s] || '未知' }
function statusType(s: number) { return STATUS_TYPE[s] || 'info' }

const PRIORITY_TEXT = ['', '低', '中', '高', '紧急']
const PRIORITY_TYPE = ['', 'info', 'primary', 'warning', 'danger']
function priorityText(p: number) { return PRIORITY_TEXT[p] || '中' }
function priorityType(p: number) { return PRIORITY_TYPE[p] || 'primary' }

const MEMBER_ROLE_TEXT = ['', '负责人', '参与人', '关注人', '验收人']
function memberRoleText(r: number) { return MEMBER_ROLE_TEXT[r] || '成员' }

const ACTION_TEXT: Record<string, string> = {
  create: '创建了任务', assign: '重新指派了负责人', accept: '接受了任务',
  claim: '认领了任务', reject: '拒绝了任务', update_progress: '更新了进度',
  submit: '提交了验收', approve: '验收通过', reject_approve: '驳回了验收',
  revoke: '撤销了任务', comment: '发表了评论', update: '编辑了任务',
  add_member: '添加了成员', remove_member: '移除了成员'
}
function actionText(a: string) { return ACTION_TEXT[a] || a }

function formatDate(d: any) {
  return d ? String(d).replace('T', ' ').slice(0, 16) : '—'
}

function isAssignee(row: any) { return row && row.assignee_id === uid }
function isCreator(row: any) { return row && row.creator_id === uid }
function archived(row: any) { return row && row.archive_status === 1 }
// 后端返回的可管理标志，已含管理员、创建人、本部门主管
function deptManageable(row: any) { return row && row.dept_manageable === true }
// 编辑、指派、验收、归档、成员管理：创建人、管理员或本部门主管
function canManage(row: any) { return isCreator(row) || isTaskAdmin.value || deptManageable(row) }
// 是否任务相关人员，gm 不是成员时只读
function isTaskMember(row: any) {
  if (!row) return false
  if (row.creator_id === uid || row.assignee_id === uid) return true
  return (row.members || []).some((m: any) => m.user_id === uid)
}
// 能否评论和传附件：管理员或任务相关人员，且未归档
function canCollaborateTask(row: any) {
  return row && !archived(row) && (isTaskAdmin.value || isTaskMember(row))
}
function canAccept(row: any) { return row && !archived(row) && row.status === 1 && isAssignee(row) }
function canReject(row: any) { return row && !archived(row) && row.status === 1 && isAssignee(row) }
function canProgress(row: any) { return row && !archived(row) && row.status === 2 && isAssignee(row) }
function canSubmit(row: any) { return row && !archived(row) && row.status === 2 && isAssignee(row) }
function canApprove(row: any) { return row && !archived(row) && row.status === 3 && canManage(row) }
function canRejectApprove(row: any) { return row && !archived(row) && row.status === 3 && canManage(row) }
function canClaim(row: any) { return row && !archived(row) && row.status === 0 && !isCreator(row) }
// 删任务要管理员或 task:delete，后端还会查是否已开工
function canDeleteTask(row: any) {
  return !!(row && row.id && hasPerm('task:delete'))
}

async function doDeleteTask(row: any) {
  try {
    await ElMessageBox.confirm(
      '确认删除任务「' + row.title + '」？删除后任务、成员与评论都会隐藏，列表里无法恢复。',
      '删除任务',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteTask(row.id)
    ElMessage.success('任务已删除')
    if (detail.value && detail.value.id === row.id) detail.value = null
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

function canEdit(row: any) {
  return row && !archived(row) && ![4, 5].includes(row.status) && canManage(row)
}
function canReassign(row: any) {
  return row && !archived(row) && [0, 1, 2, 6].includes(row.status) && canManage(row)
}
function canRevoke(row: any) { return row && !archived(row) && [0, 1, 2, 3].includes(row.status) && canManage(row) }
function canArchive(row: any) {
  return row && !archived(row) && [4, 6].includes(row.status) && canManage(row)
}
function canRestore(row: any) {
  return row && !archived(row) && row.status === 5 && canManage(row)
}
function canUnarchive(row: any) { return row && archived(row) && isTaskAdmin.value }
function canManageMember(row: any) {
  return row && !archived(row) && (isAssignee(row) || canManage(row))
}
function canDeleteComment(c: any) {
  return c && detail.value && !archived(detail.value) && (isTaskAdmin.value || c.user_id === uid)
}
function canRemoveMember(m: any) {
  if (!detail.value || !canManageMember(detail.value)) return false
  if (m.member_role === 1) return false // 负责人走改派
  if (m.user_id === detail.value.creator_id) return false // 创建人不移除
  return true
}

// 项目下拉只能选自己负责或参与的项目
const projects = ref<any[]>([])
const projectMembers = ref<any[]>([])
// 选了项目后负责人和参与人只能从项目成员里挑
const peopleOptions = computed(() => (taskForm.value.project_id ? projectMembers.value : users.value))

async function loadProjects() {
  try {
    const res: any = await getProjectOptions()
    projects.value = res || []
  } catch { /* 没有项目权限时静默 */ }
}

async function onProjectChange(pid: number | null) {
  projectMembers.value = []
  taskForm.value.assignee_id = null
  taskForm.value.member_ids = []
  if (!pid) return
  try {
    const d: any = await getProjectDetail(pid)
    projectMembers.value = d.members || []
  } catch { /* 忽略 */ }
}

async function loadUsers() {
  try {
    const res: any = await listUserOptions()
    users.value = res || []
  } catch { /* 忽略 */ }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getTasks({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      view: activeTab.value === 'all' ? '' : activeTab.value,
      keyword: filters.value.keyword || undefined,
      status: filters.value.status,
      priority: filters.value.priority,
      projectId: filters.value.projectId || undefined,
      archived: isAdminUser.value && filters.value.archived ? true : undefined
    })
    list.value = res.list || []
    total.value = res.total || 0
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  pageNum.value = 1
  loadData()
}
function onSearch() {
  pageNum.value = 1
  loadData()
}
function onReset() {
  filters.value = { keyword: '', status: null, priority: null, projectId: null, archived: false }
  pageNum.value = 1
  loadData()
}

function defaultForm() {
  return {
    title: '', description: '', priority: 2, assignee_id: null, member_ids: [], project_id: null,
    plan_start_time: '', plan_end_time: '', plan_work_hours: undefined
  }
}

function openCreate() {
  editMode.value = false
  editId.value = null
  taskForm.value = defaultForm()
  projectMembers.value = []
  formVisible.value = true
  loadUsers()
  loadProjects()
}

async function openEdit(row: any) {
  editMode.value = true
  editId.value = row.id
  if (detailVisible.value) detailVisible.value = false
  await loadUsers()
  const d = await getTaskDetail(row.id)
  const memberIds = (d.members || [])
    .filter((m: any) => m.member_role === 2)
    .map((m: any) => m.user_id)
  if (d.project_id) {
    try {
      const p: any = await getProjectDetail(d.project_id)
      projectMembers.value = p.members || []
    } catch { projectMembers.value = [] }
  } else {
    projectMembers.value = []
  }
  taskForm.value = {
    title: d.title, description: d.description || '', priority: d.priority, project_id: d.project_id || null,
    assignee_id: d.assignee_id, member_ids: memberIds,
    plan_start_time: d.plan_start_time ? String(d.plan_start_time).slice(0, 19) : '',
    plan_end_time: d.plan_end_time ? String(d.plan_end_time).slice(0, 19) : '',
    plan_work_hours: d.plan_work_hours ?? undefined
  }
  formVisible.value = true
}

// 提交中锁住按钮，双击不会连发两条创建请求
const taskSubmitting = ref(false)

async function submitTaskForm() {
  if (taskSubmitting.value) return
  if (!taskForm.value.title?.trim()) {
    ElMessage.warning('请输入任务名称')
    return
  }
  const payload: any = { ...taskForm.value }
  if (!payload.plan_start_time) payload.plan_start_time = null
  if (!payload.plan_end_time) payload.plan_end_time = null
  if (payload.plan_work_hours == null) delete payload.plan_work_hours
  taskSubmitting.value = true
  try {
    if (editMode.value && editId.value) {
      await updateTask(editId.value, payload)
      ElMessage.success('已保存')
    } else {
      await createTask(payload)
      ElMessage.success('任务已创建')
    }
    formVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    taskSubmitting.value = false
  }
}

function openReassign(row: any) {
  reassignForm.value = { id: row.id, title: row.title, assignee_id: null }
  reassignVisible.value = true
  loadUsers()
}
async function submitReassign() {
  if (!reassignForm.value.assignee_id) {
    ElMessage.warning('请选择负责人')
    return
  }
  await reassignTask(reassignForm.value.id, { assignee_id: reassignForm.value.assignee_id })
  ElMessage.success('已重新指派')
  reassignVisible.value = false
  if (detailVisible.value && detail.value.id === reassignForm.value.id) await refreshDetail()
  loadData()
}

function openProgress(row: any) {
  progressForm.value = { id: row.id, title: row.title, progress: row.progress || 0, work_note: '' }
  progressVisible.value = true
}
async function submitProgress() {
  await updateProgress(progressForm.value.id, {
    progress: progressForm.value.progress,
    work_note: progressForm.value.work_note || ''
  })
  ElMessage.success('进度已更新')
  progressVisible.value = false
  if (detailVisible.value && detail.value.id === progressForm.value.id) await refreshDetail()
  loadData()
}

function openSubmit(row: any) {
  submitForm.value = { id: row.id, title: row.title, complete_note: '', actual_work_hours: undefined, fromBoard: false }
  submitVisible.value = true
}
function openSubmitForBoard(task: any) {
  dragTask.value = null
  dragOverCol.value = null
  openSubmit({ id: task.id, title: task.title })
  submitForm.value.fromBoard = true
}
async function submitFinish() {
  if (!submitForm.value.complete_note?.trim()) {
    ElMessage.warning('请填写完成说明')
    return
  }
  const payload: any = { complete_note: submitForm.value.complete_note }
  if (submitForm.value.actual_work_hours != null) payload.actual_work_hours = submitForm.value.actual_work_hours
  const id = submitForm.value.id
  if (submitForm.value.fromBoard) {
    payload.target_status = 3
    await moveTask(id, payload)
  } else {
    await submitTask(id, payload)
  }
  ElMessage.success('已提交，等待验收')
  submitVisible.value = false
  await afterAction({ id })
}

function openAddMember() {
  memberForm.value = { user_id: null, role: 2 }
  memberVisible.value = true
  loadUsers()
}
async function submitAddMember() {
  if (!memberForm.value.user_id) {
    ElMessage.warning('请选择成员')
    return
  }
  await addTaskMember(detail.value.id, { user_id: memberForm.value.user_id, role: memberForm.value.role })
  ElMessage.success('已添加成员')
  memberVisible.value = false
  await refreshDetail()
}
async function doRemoveMember(m: any) {
  await ElMessageBox.confirm(`确定将 ${m.real_name} 移出该任务？`, '移除成员', { type: 'warning' })
  await removeTaskMember(detail.value.id, m.user_id)
  ElMessage.success('已移除成员')
  await refreshDetail()
}

async function openDetail(row: any) {
  detail.value = null
  commentText.value = ''
  cancelReply()
  await refreshDetail(row.id)
  detailVisible.value = true
}
async function refreshDetail(id?: number) {
  const taskId = id || (detail.value && detail.value.id)
  detail.value = await getTaskDetail(taskId)
}

async function doClaim(row: any) {
  await ElMessageBox.confirm('认领后你将成为该任务的负责人，状态变为进行中，确认认领？', '认领任务', { type: 'success' })
  await claimTask(row.id)
  ElMessage.success('已认领任务')
  await afterAction(row)
}
async function doAccept(row: any) {
  await acceptTask(row.id)
  ElMessage.success('已接受任务')
  await afterAction(row)
}
async function doReject(row: any) {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因（必填）', '拒绝任务', {
    confirmButtonText: '确认拒绝', cancelButtonText: '取消',
    inputValidator: (v) => (v && v.trim() ? true : '拒绝原因不能为空')
  })
  await rejectTask(row.id, { reject_reason: value })
  ElMessage.success('已拒绝')
  await afterAction(row)
}
async function doApprove(row: any) {
  await ElMessageBox.confirm('确认验收通过此任务？', '验收', { type: 'success' })
  await approveTask(row.id)
  ElMessage.success('已验收通过')
  await afterAction(row)
}
async function doRejectApprove(row: any) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因（必填）', '验收驳回', {
    confirmButtonText: '确认驳回', cancelButtonText: '取消',
    inputValidator: (v) => (v && v.trim() ? true : '驳回原因不能为空')
  })
  await rejectApproveTask(row.id, { reason: value })
  ElMessage.success('已驳回，任务退回进行中')
  await afterAction(row)
}
async function doRevoke(row: any) {
  await ElMessageBox.confirm('确认撤销此任务？撤销后可由创建人/管理员恢复', '撤销任务', { type: 'warning' })
  await revokeTask(row.id)
  ElMessage.success('任务已撤销')
  await afterAction(row)
}
async function doArchive(row: any) {
  await ElMessageBox.confirm('确认归档此任务？归档后为只读，可在“已归档”中查看，管理员可恢复。', '归档任务', { type: 'warning' })
  await archiveTask(row.id)
  ElMessage.success('任务已归档')
  await afterAction(row)
}
async function doRestore(row: any) {
  await ElMessageBox.confirm('确认恢复此已撤销任务？', '恢复任务', { type: 'warning' })
  await restoreTask(row.id)
  ElMessage.success('任务已恢复')
  await afterAction(row)
}
async function doUnarchive(row: any) {
  await ElMessageBox.confirm('确认将该任务从归档中恢复？', '恢复归档', { type: 'warning' })
  await unarchiveTask(row.id)
  ElMessage.success('已恢复归档任务')
  await afterAction(row)
}
async function doDeleteComment(c: any) {
  if (!detail.value) return
  await ElMessageBox.confirm('确认删除该条评论？', '删除评论', { type: 'warning' })
  await deleteTaskComment(detail.value.id, c.id)
  ElMessage.success('评论已删除')
  await refreshDetail()
}
async function afterAction(row: any) {
  if (detailVisible.value && detail.value && detail.value.id === row.id) await refreshDetail()
  if (viewMode.value === 'board') await loadBoard()
  loadData()
}

function onViewModeChange(v: string) {
  if (v === 'board') loadBoard()
}
function colTasks(s: number): any[] {
  return boardColumns.value[s] || []
}
async function loadBoard() {
  try {
    const [b, s]: any = await Promise.all([getBoard(), getStatistics()])
    boardColumns.value = b.columns || { 0: [], 1: [], 2: [], 3: [], 4: [] }
    stats.value = s || stats.value
  } catch {
    ElMessage.error('看板加载失败')
  }
}
function onDragStart(t: any, e: DragEvent) {
  dragTask.value = t
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(t.id))
  }
}
async function onDrop(target: number) {
  const t = dragTask.value
  dragOverCol.value = null
  dragTask.value = null
  if (!t || t.status === target) return
  await doMove(t, target)
}
async function doMove(task: any, target: number) {
  const cur = task.status
  try {
    if (target === 2) {
      if (cur === 0) {
        await ElMessageBox.confirm('认领后你将成为该任务的负责人，确认认领？', '认领任务', { type: 'success' })
        await moveTask(task.id, { target_status: 2 })
        ElMessage.success('已认领任务')
      } else if (cur === 1) {
        await ElMessageBox.confirm('确认接受该任务？', '接受任务', { type: 'success' })
        await moveTask(task.id, { target_status: 2 })
        ElMessage.success('已接受任务')
      } else {
        ElMessage.warning('当前状态不能移动到进行中')
        return
      }
    } else if (target === 3) {
      if (cur !== 2) {
        ElMessage.warning('仅进行中任务可提交验收')
        return
      }
      openSubmitForBoard(task)
      return
    } else if (target === 4) {
      if (cur !== 3) {
        ElMessage.warning('仅待验收任务可验收')
        return
      }
      await ElMessageBox.confirm('确认验收通过此任务？', '验收', { type: 'success' })
      await moveTask(task.id, { target_status: 4 })
      ElMessage.success('已验收通过')
    } else {
      ElMessage.warning('不支持的看板流转')
      return
    }
    await afterAction({ id: task.id })
  } catch (e: any) {
    if (e === 'cancel' || e?.action === 'cancel') return
    ElMessage.error(e?.message || '操作失败')
    await loadBoard()
  }
}

function setReply(c: any) {
  replyTo.value = { id: c.id, name: c.real_name, user_id: c.user_id }
}
function cancelReply() {
  replyTo.value = null
}
function parentName(parentId: number) {
  const p = detail.value?.comments?.find((c: any) => c.id === parentId)
  return p ? p.real_name : '成员'
}
function mentionMember(m: any) {
  commentText.value += `@${m.real_name} `
  if (!mentionUserIds.value.includes(m.user_id)) mentionUserIds.value.push(m.user_id)
}

async function submitComment() {
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  const payload: any = { content: commentText.value }
  if (replyTo.value) payload.parent_id = replyTo.value.id
  if (mentionUserIds.value.length) payload.mention_users = mentionUserIds.value.join(',')
  if (commentAttachment.value) payload.attachment_id = commentAttachment.value.id
  await addComment(detail.value.id, payload)
  commentText.value = ''
  commentAttachment.value = null
  if (commentFileRef.value) commentFileRef.value.value = ''
  mentionUserIds.value = []
  cancelReply()
  await refreshDetail()
}

// 附件
const ATT_EXTS = ['doc','docx','xls','xlsx','ppt','pptx','pdf','txt','md','jpg','jpeg','png','gif','bmp','zip','rar','java','js','ts','py']
const taskUploading = ref(false)
const commentUploading = ref(false)
const commentFileRef = ref<HTMLInputElement | null>(null)
const commentAttachment = ref<any>(null)

function formatSize(size?: number) {
  if (!size) return '0B'
  if (size < 1024) return size + 'B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + 'KB'
  return (size / 1024 / 1024).toFixed(1) + 'MB'
}
function canUploadAtt(d: any) {
  return d && [0, 1, 2, 3].includes(d.status) && (isTaskAdmin.value || isTaskMember(d))
}
function canDeleteAtt(a: any) {
  return detail.value && !archived(detail.value) &&
    (detail.value.creator_id === uid || a.uploader_id === uid || isTaskAdmin.value)
}
function beforeAttUpload(file: File) {
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  if (!ATT_EXTS.includes(ext)) {
    ElMessage.error('该文件格式不允许上传')
    return false
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小超过20MB')
    return false
  }
  if ((detail.value.attachments || []).length >= 3) {
    ElMessage.error('每个任务最多上传3个附件')
    return false
  }
  return true
}
async function handleTaskUpload(options: any) {
  const file = options.file as File
  taskUploading.value = true
  try {
    await uploadTaskAttachment(file, detail.value.id)
    ElMessage.success('上传成功')
    await refreshDetail()
  } catch { /* 拦截器提示 */ } finally {
    taskUploading.value = false
  }
}
async function onCommentFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files && input.files[0]
  if (!file) return
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  if (!ATT_EXTS.includes(ext)) { ElMessage.error('该文件格式不允许上传'); input.value = ''; return }
  if (file.size > 20 * 1024 * 1024) { ElMessage.error('文件大小超过20MB'); input.value = ''; return }
  commentUploading.value = true
  try {
    const res = await uploadTaskAttachment(file)
    commentAttachment.value = res
    ElMessage.success('附件已就绪，发送评论后生效')
  } catch {
    input.value = ''
  } finally {
    commentUploading.value = false
  }
}
async function downloadAtt(aid: number) {
  try {
    const blob = await downloadTaskAttachment(aid)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '附件'
    a.click()
    URL.revokeObjectURL(url)
  } catch { /* 拦截器提示 */ }
}
async function removeAtt(a: any) {
  try {
    await ElMessageBox.confirm(`确认删除附件「${a.file_name}」？`, '删除附件', { type: 'warning' })
  } catch { return }
  await deleteTaskAttachment(a.id)
  ElMessage.success('附件已删除')
  await refreshDetail()
}

onMounted(() => {
  // 没有任务中心访问权就不请求，免得一进来弹 403
  if (!canAccessTask.value) return
  // 不能建任务就不加载项目与人员下拉
  if (canCreateTask.value) {
    loadProjects()
    loadUsers()
  }
  const t = route.query.type as string
  if (t === 'assigned' || t === 'created' || t === 'joined' || t === 'all') activeTab.value = t
  loadData()
})
</script>

<style scoped>
.project-name { color: #1f6feb; font-size: 12px; }
.muted { color: #c0c4cc; }

.task-page { padding: 20px; }
.no-access-tip { color: #909399; font-size: 13px; line-height: 1.8; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.filter-bar { margin-bottom: 4px; }
.filter-bar :deep(.el-form-item) { margin-bottom: 10px; }
.fade-overflow { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; max-width: 180px; }
.form-tip { font-size: 12px; color: #999; line-height: 1.4; }
.meta { color: #999; font-size: 12px; margin: 8px 0; }
.desc { margin: 10px 0; line-height: 1.7; color: #333; }
.complete-note { background: #f8f9fb; border-radius: 6px; padding: 10px 12px; line-height: 1.7; color: #333; white-space: pre-wrap; }
.reject-alert { margin-bottom: 12px; }
.detail-head { display: flex; align-items: center; gap: 10px; }
.detail-head h3 { margin: 0; }
.head-edit { margin-left: auto; }
.detail-actions { margin: 14px 0; padding: 12px; background: #f8f9fb; border-radius: 6px; }
.member-list { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.member-tag { margin: 0; }
.log-timeline { padding-left: 6px; max-height: 220px; overflow-y: auto; }
.log-name { color: #409eff; font-weight: 600; }
.log-remark { color: #999; font-size: 12px; }
.comment-item { padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.comment-reply { margin-left: 28px; background: #fafafa; padding: 6px 10px; border-radius: 4px; }
.comment-item .time { color: #999; font-size: 12px; margin-left: 8px; }
.reply-to { color: #e6a23c; font-size: 12px; margin-left: 6px; }
.reply-tip { font-size: 12px; color: #e6a23c; margin: 6px 0; }
.mention-bar { margin: 6px 0; font-size: 12px; }
.mention-label { color: #999; }
.comment-input { display: flex; gap: 8px; align-items: flex-start; margin-top: 6px; }
.comment-input .el-button { height: 40px; }
.progress-dialog { padding: 0 10px; }
.progress-task { margin-bottom: 16px; font-weight: 600; }
.work-note { margin-top: 14px; }
.head-right { display: flex; align-items: center; gap: 12px; }

/* 统计概览 */
.stat-bar { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.stat-item { flex: 1; min-width: 120px; background: #fff; border: 1px solid #ebeef5; border-radius: 8px;
  padding: 14px 16px; text-align: center; }
.stat-num { font-size: 22px; font-weight: 700; color: #303133; }
.stat-item.danger .stat-num { color: #f56c6c; }
.stat-num.diff-up { color: #f56c6c; }
.stat-num.diff-down { color: #67c23a; }
.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }

/* 看板 */
.board { display: flex; gap: 10px; align-items: flex-start; }
.board-col { flex: 1; min-width: 0; background: #f5f7fa; border-radius: 8px; display: flex; flex-direction: column; }
.board-col-head { display: flex; justify-content: space-between; align-items: center;
  padding: 10px 12px; font-weight: 600; font-size: 13px; border-radius: 8px 8px 0 0; color: #fff; }
.board-col-head.c-pending { background: #909399; }
.board-col-head.c-wait { background: #e6a23c; }
.board-col-head.c-doing { background: #409eff; }
.board-col-head.c-check { background: #b37feb; }
.board-col-head.c-done { background: #67c23a; }
.board-count { background: rgba(255,255,255,.25); border-radius: 10px; padding: 0 8px; font-size: 12px; }
.board-col-body { padding: 8px; min-height: 120px; flex: 1; }
.board-col-body.drag-over { outline: 2px dashed #409eff; outline-offset: -2px; border-radius: 4px; }
.board-card { background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 9px 10px;
  margin-bottom: 8px; cursor: grab; box-shadow: 0 1px 2px rgba(0,0,0,.04); }
.board-card:active { cursor: grabbing; }
.board-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.1); border-color: #c6e2ff; }
.board-card-title { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 6px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.board-card-meta { display: flex; gap: 6px; margin-bottom: 6px; }
.board-card-foot { font-size: 12px; color: #606266; margin-top: 6px; }
.board-card-due { font-size: 11px; color: #909399; margin-top: 2px; }
.board-empty { text-align: center; color: #c0c4cc; font-size: 12px; padding: 18px 0; }
.board-tip { font-size: 12px; color: #909399; margin-top: 12px; line-height: 1.6; }

/* 附件 */
.att-list { display: flex; flex-direction: column; gap: 6px; }
.att-row { display: flex; align-items: center; gap: 10px; background: #f5f7fa; border-radius: 6px;
  padding: 6px 10px; font-size: 13px; }
.att-row .att-name { color: #303133; font-weight: 500; }
.att-row .att-size { color: #909399; font-size: 12px; }
.att-row .att-meta { color: #c0c4cc; font-size: 12px; margin-left: auto; }
.comment-att { margin-top: 4px; }
.comment-input { flex-direction: column; align-items: stretch; }
.comment-actions { display: flex; gap: 8px; align-items: center; justify-content: flex-end; margin-top: 8px; }
</style>
