<template>
  <div class="user-page">
    <!-- 左侧：部门树 -->
    <div class="org-panel">
      <div class="org-title">
        <span>企业组织架构</span>
      </div>
      <el-tabs v-model="orgTab" class="org-tabs">
        <!-- 查看页签只有树，不显示增删改按钮 -->
        <el-tab-pane label="组织架构" name="view">
          <el-tree
            :data="orgNodes"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            :props="{ label: 'label', children: 'children' }"
            @node-click="handleOrgClick"
          >
            <template #default="{ node }">
              <span class="tree-node">
                <span class="tree-label">{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </el-tab-pane>
        <!-- 维护页签：要改架构就切到这里，按钮只在悬停时出现 -->
        <el-tab-pane v-if="canManageOrg" label="架构维护" name="edit">
          <div class="org-edit-bar">
            <el-button size="small" type="primary" @click="openDeptDialog(null)">+ 新增一级部门</el-button>
            <span class="org-edit-tip">改动会立刻影响部门可见范围，谨慎操作</span>
          </div>
          <el-tree
            :data="orgNodes"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            :props="{ label: 'label', children: 'children' }"
            @node-click="handleOrgClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span class="tree-label">{{ node.label }}</span>
                <span v-if="data.id !== 0" class="tree-actions">
                  <el-button link type="primary" size="small" @click.stop="openDeptDialog(data)" title="新增子部门">新增</el-button>
                  <el-button link type="primary" size="small" @click.stop="openDeptDialog(data, true)">编辑</el-button>
                  <el-button link type="danger" size="small" @click.stop="removeDept(data)">删除</el-button>
                </span>
              </span>
            </template>
          </el-tree>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 右侧：员工列表 -->
    <div class="user-content">
      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="用户名 / 姓名" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" placeholder="账号状态" clearable style="width: 130px">
          <el-option label="正常" :value="1" />
          <el-option label="已停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <div class="spacer" />
        <el-button v-permission="'user:onboard'" type="success" @click="openOnboard">入职办理</el-button>
        <el-button v-permission="'user:transfer'" @click="openHandovers">交接记录</el-button>
        <el-button v-permission="'user:add'" type="primary" @click="openAdd">新增用户</el-button>
        <el-button v-permission="'user:add'" @click="importVisible = true">批量导入</el-button>
        <el-button v-permission="'user:export'" :loading="exporting" @click="handleExport">导出</el-button>
        <el-button v-permission="'position:manage'" @click="positionDrawerVisible = true">职位管理</el-button>
        <el-button v-permission="'title:manage'" @click="titleDrawerVisible = true">职称管理</el-button>
        <el-button v-permission="'group:manage'" @click="groupDrawerVisible = true">用户群组</el-button>
      </div>

      <!-- 用户列表 -->
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="头像" width="60" align="center">
          <template #default="{ row }">
            <el-avatar :size="28" :src="row.avatar || undefined">{{ (row.real_name || row.username || '?').charAt(0) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="100">
          <template #default="{ row }">
            <div class="fade-overflow" :title="row.username">{{ row.username }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="real_name" label="姓名" min-width="80">
          <template #default="{ row }">
            <div class="fade-overflow" :title="row.real_name">{{ row.real_name }}</div>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="55">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="105">
          <template #default="{ row }">
            <div class="fade-overflow" :title="row.phone">{{ row.phone || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column v-if="canViewSalary" label="月薪" width="110" align="right">
          <template #default="{ row }">
            <span v-if="row.salary != null">{{ Number(row.salary).toFixed(2) }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="140">
          <template #default="{ row }">
            <div class="fade-overflow" :title="row.email || ''">{{ row.email || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="org_name" label="所属部门" min-width="100">
          <template #default="{ row }">
            <div class="fade-overflow" :title="row.org_name || ''">{{ row.org_name || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag v-for="n in row.roleNames || []" :key="n" size="small" style="margin-right:4px">{{ n }}</el-tag>
            <span v-if="!(row.roleNames || []).length" class="muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '已停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号锁定" width="85">
          <template #default="{ row }">
            <el-tag v-if="row.lock_time" type="warning" size="small">已锁定</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="entry_date" label="入职时间" width="105" />
        <el-table-column label="最后登录" width="180">
          <template #default="{ row }">
            <span v-if="!row.last_login_time" class="muted">—</span>
            <el-tag v-else-if="loginTimeTag(row.last_login_time).full" size="small" type="info">
              {{ loginTimeTag(row.last_login_time).text }}
            </el-tag>
            <template v-else>
              <el-tag size="small" :type="loginTimeTag(row.last_login_time).type">
                {{ loginTimeTag(row.last_login_time).label }}
              </el-tag>
              &nbsp;&nbsp;{{ loginTimeTag(row.last_login_time).time }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="430" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'user:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'user:transfer'" link type="warning" @click="openHandover(row)">离职交接</el-button>
            <el-button v-permission="'user:reset'" link type="primary" @click="openReset(row)">重置密码</el-button>
            <el-button v-if="isSysAdmin" link type="success" @click="openExtraPerm(row)">任务权限</el-button>
            <el-button v-if="row.lock_time" v-permission="'user:unlock'" link type="warning" @click="handleUnlock(row)">解锁</el-button>
            <el-button v-permission="'user:status'" link :type="row.status === 1 ? 'danger' : 'success'" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button v-permission="'user:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top:12px; justify-content:flex-end"
        @size-change="load"
        @current-change="load"
      />
    </div>

    <!-- 新增 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :disabled="isEdit" placeholder="登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="real_name">
              <el-input v-model="form.real_name" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :label="0">未知</el-radio>
                <el-radio :label="1">男</el-radio>
                <el-radio :label="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职时间">
              <el-date-picker v-model="form.entry_date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col v-if="!isEdit" :span="12">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="8-20位，首次登录将强制修改" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-tree-select
                v-model="form.org_id"
                :data="orgNodes"
                node-key="id"
                :props="{ label: 'label', children: 'children' }"
                check-strictly
                default-expand-all
                placeholder="选择部门（选填）"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width:100%">
                <el-option v-for="r in roleOptions" :key="r.id" :label="r.role_name" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 员工档案：联系方式、紧急联系人、工资卡 -->
        <el-divider content-position="left">员工档案</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="紧急联系人">
              <el-input v-model="form.emergency_contact" placeholder="如：父亲 / 配偶" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急电话">
              <el-input v-model="form.emergency_phone" placeholder="紧急情况联系号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="银行卡号">
              <el-input v-model="form.bank_account" placeholder="工资卡号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开户行">
              <el-input v-model="form.bank_name" placeholder="如：中国工商银行天河支行" />
            </el-form-item>
          </el-col>
          <el-col v-if="canViewSalary" :span="12">
            <el-form-item label="月薪">
              <el-input v-model.number="form.salary" type="number" placeholder="按月，单位元" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importVisible" title="批量导入用户" width="560px" destroy-on-close>
      <div class="import-tip">
        <p>支持 .xlsx / .xls 文件，第一行为表头，从第二行开始导入数据。</p>
        <p>表头固定为：<b>用户名、姓名、性别、手机号、邮箱、部门名称、角色名称、初始密码、入职时间</b></p>
        <p>性别填「男/女」；部门/角色填系统内已有的名称（选填）；初始密码不填默认 <b>Admin@123</b>；入职时间格式 <b>yyyy-MM-dd</b>。</p>
      </div>
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
        :on-remove="() => (importFile = null)"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击选择</em></div>
      </el-upload>
      <div v-if="importResult" class="import-result">
        <el-alert :title="`导入完成：成功 ${importResult.success} 条`" :type="importResult.errors.length ? 'warning' : 'success'" :closable="false" show-icon />
        <el-collapse v-if="importResult.errors.length" class="import-errors">
          <el-collapse-item :title="`失败明细（${importResult.errors.length} 条）`">
            <ul>
              <li v-for="(e, i) in importResult.errors" :key="i">{{ e }}</li>
            </ul>
          </el-collapse-item>
        </el-collapse>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="handleImportSubmit">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 部门新增/编辑对话框 -->
    <el-dialog v-model="deptDialogVisible" :title="deptIsEdit ? '编辑部门' : '新增部门'" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="上级部门">
          <el-input :model-value="deptForm.parent_name" disabled />
        </el-form-item>
        <el-form-item label="部门名称" required>
          <el-input v-model="deptForm.org_name" placeholder="如：技术部、市场部" />
        </el-form-item>
        <el-form-item label="部门类型">
          <el-select v-model="deptForm.org_type" style="width: 100%">
            <el-option label="公司" :value="1" />
            <el-option label="部门" :value="2" />
            <el-option label="小组" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="deptForm.sort_order" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="deptSaving" @click="saveDept">保存</el-button>
      </template>
    </el-dialog>

    <!-- 职位管理抽屉 -->
    <el-drawer v-model="positionDrawerVisible" title="职位管理" size="720px">
      <PositionPanel />
    </el-drawer>

    <!-- 职称管理抽屉 -->
    <el-drawer v-model="titleDrawerVisible" title="职称管理" size="720px">
      <TitlePanel />
    </el-drawer>

    <!-- 用户群组抽屉 -->
    <el-drawer v-model="groupDrawerVisible" title="用户群组" size="860px">
      <GroupPanel />
    </el-drawer>

    <!-- 任务中心权限默认没有，派任务自动开，这里可手工发放回收 -->
    <el-dialog v-model="extraPermVisible" title="任务中心权限" width="600px" destroy-on-close>
      <div class="perm-user">
        用户：{{ extraPermUser?.real_name }}（{{ extraPermUser?.username }}）
      </div>
      <el-switch v-model="extraPermTaskAccess" class="perm-switch" active-text="任务中心访问（task:access）"
        :disabled="extraPermLoading" @change="toggleTaskAccess" />
      <el-switch v-model="extraPermCanGrant" class="perm-switch" active-text="可发放任务权限（task:grant）"
        :disabled="extraPermLoading || !extraPermCanGrantEnabled" @change="toggleGrantPermission" />
      <div class="perm-hint">开通「任务中心访问」后该用户才能看到任务中心；普通员工被指派任务时系统会自动开通，无需手工操作。</div>
      <el-table v-if="extraPermItems.length" :data="extraPermItems" size="small" style="margin-top:12px">
        <el-table-column prop="menuName" label="权限" width="150" />
        <el-table-column prop="sourceText" label="来源" width="150" />
        <el-table-column prop="reason" label="说明" show-overflow-tooltip />
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleRevokeExtraPerm(row)">回收</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="extraPermVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="440px" destroy-on-close>
      <el-form ref="resetRef" :model="resetForm" :rules="resetRules" label-width="90px">
        <el-form-item label="用户名">
          <span>{{ resetForm.username }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="8-20位，用户下次登录将强制修改" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleResetSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- 入职办理：账号、部门、职位、档案一次填完，密码留空自动生成 -->
    <el-dialog v-model="onboardVisible" title="入职办理" width="700px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px"
        title="开通账号后员工需用初始密码首次登录并强制改密；初始密码留空由系统生成随机密码。" />
      <el-form ref="onboardRef" :model="onboardForm" :rules="onboardRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="onboardForm.username" placeholder="如 zhangsan" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="real_name">
              <el-input v-model="onboardForm.real_name" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="onboardForm.gender">
                <el-radio :label="0">未知</el-radio>
                <el-radio :label="1">男</el-radio>
                <el-radio :label="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期">
              <el-date-picker v-model="onboardForm.entry_date" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="onboardForm.phone" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="onboardForm.email" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门" prop="org_id">
              <el-tree-select
                v-model="onboardForm.org_id"
                :data="orgNodes"
                node-key="id"
                :props="{ label: 'label', children: 'children' }"
                check-strictly
                default-expand-all
                placeholder="必选"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-select v-model="onboardForm.position_id" clearable placeholder="选填" style="width:100%">
                <el-option v-for="p in positionOptions" :key="p.id" :label="p.position_name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="onboardForm.roleIds" multiple placeholder="留空默认普通员工" style="width:100%">
                <el-option v-for="r in roleOptions" :key="r.id" :label="r.role_name" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="初始密码">
              <el-input v-model="onboardForm.password" type="password" show-password placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">员工档案</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="紧急联系人">
              <el-input v-model="onboardForm.emergency_contact" placeholder="如：父亲 / 配偶" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急电话">
              <el-input v-model="onboardForm.emergency_phone" placeholder="紧急情况联系号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="银行卡号">
              <el-input v-model="onboardForm.bank_account" placeholder="工资卡号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开户行">
              <el-input v-model="onboardForm.bank_name" placeholder="如：中国工商银行天河支行" />
            </el-form-item>
            <el-form-item v-if="canViewSalary" label="月薪">
              <el-input v-model.number="onboardForm.salary" type="number" placeholder="按月，单位元" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="onboardVisible = false">取消</el-button>
        <el-button type="primary" :loading="onboarding" @click="handleOnboard">开通账号</el-button>
      </template>
    </el-dialog>

    <!-- 离职交接：先看清单，再选接收人 -->
    <el-dialog v-model="handoverVisible" title="离职交接" width="780px" destroy-on-close>
      <div v-if="handoverPreviewData" v-loading="handoverLoading">
        <el-descriptions :column="3" border size="small" style="margin-bottom:12px">
          <el-descriptions-item label="离职人">{{ handoverPreviewData.realName }}（{{ handoverPreviewData.username }}）</el-descriptions-item>
          <el-descriptions-item label="所属部门">{{ handoverPreviewData.orgName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="账号状态">
            <el-tag :type="handoverPreviewData.status === 1 ? 'success' : 'danger'" size="small">
              {{ handoverPreviewData.status === 1 ? '正常' : '已停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="待办流程">
            <b style="color:#e6a23c">{{ handoverPreviewData.todoCount }}</b> 条{{ handoverModeHint }}
          </el-descriptions-item>
          <el-descriptions-item label="审批中发起">
            <b style="color:#409eff">{{ handoverPreviewData.runningCount }}</b> 条
          </el-descriptions-item>
          <el-descriptions-item label="历史已办">
            {{ handoverPreviewData.doneCount }} 条（只留档不转交）
          </el-descriptions-item>
          <el-descriptions-item label="未结束任务">
            <b style="color:#67c23a">{{ handoverPreviewData.taskCount }}</b> 个{{ handoverModeHint }}
          </el-descriptions-item>
          <el-descriptions-item label="参与项目">
            <b style="color:#909399">{{ handoverPreviewData.projectCount }}</b> 个{{ handoverModeHint }}
          </el-descriptions-item>
        </el-descriptions>
        <el-table v-if="handoverPreviewData.todoList.length" :data="handoverPreviewData.todoList" border size="small" max-height="200" style="margin-bottom:12px">
          <el-table-column prop="instanceNo" label="流程编号" width="180" />
          <el-table-column prop="title" label="流程标题" min-width="200" />
          <el-table-column prop="currentStepName" label="当前环节" width="130" />
        </el-table>
        <el-table v-if="handoverPreviewData.taskList.length" :data="handoverPreviewData.taskList" border size="small" max-height="200" style="margin-bottom:12px">
          <el-table-column prop="taskNo" label="任务编号" width="180" />
          <el-table-column prop="title" label="任务名称" min-width="200" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">{{ taskStatusText(row.status) }}</template>
          </el-table-column>
        </el-table>
        <el-form label-width="100px">
          <el-form-item label="处理方式">
            <el-radio-group v-model="handoverForm.mode">
              <el-radio-button label="transfer">转给指定同事</el-radio-button>
              <el-radio-button label="auto">自动转直属上级</el-radio-button>
              <el-radio-button label="keep">只停用账号保留待办</el-radio-button>
            </el-radio-group>
            <div class="muted" style="margin-top:4px">
              <template v-if="handoverForm.mode === 'auto'">
                将沿部门层级向上找主管：{{ handoverPreviewData.autoReceiverName || '没找到主管，将交给系统管理员' }}
              </template>
              <template v-else-if="handoverForm.mode === 'keep'">
                待办、任务、项目身份都留在原账号名下，账号停用后不再接收新单据，需要时管理员可以再手动交接
              </template>
              <template v-else>待办、任务、项目身份全部转给你选的这位同事</template>
            </div>
          </el-form-item>
          <el-form-item v-if="handoverForm.mode === 'transfer'" label="交接接收人" required>
            <el-select v-model="handoverForm.receiverId" filterable placeholder="选择接手待办的同事" style="width:100%">
              <el-option v-for="u in receiverOptions" :key="u.id" :label="`${u.real_name}（${u.username}）`" :value="u.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="交接原因">
            <el-input v-model="handoverForm.reason" type="textarea" :rows="2" placeholder="如：离职 / 调岗，说明备查" />
          </el-form-item>
          <el-form-item label="生效日期">
            <el-date-picker v-model="handoverForm.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" style="width:200px" />
          </el-form-item>
          <el-form-item v-if="handoverForm.mode !== 'keep'" label="转交审批中">
            <el-switch v-model="handoverForm.transferRunning" />
            <span class="muted" style="margin-left:8px">他发起、还在审批中的流程也改由接收人跟进</span>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="handoverVisible = false">取消</el-button>
        <el-button type="danger" :loading="handoverSubmitting" :disabled="handoverForm.mode === 'transfer' && !handoverForm.receiverId" @click="submitHandover">
          确认交接并停用账号
        </el-button>
      </template>
    </el-dialog>

    <!-- 交接记录 -->
    <el-drawer v-model="handoverRecordsVisible" title="离职交接记录" size="1120px">
      <el-table :data="handoverRecords" border stripe v-loading="handoverRecordsLoading">
        <el-table-column label="离职人" width="110">
          <template #default="{ row }">
            {{ row.user_name || '—' }}
            <span v-if="row.org_name" class="muted">（{{ row.org_name }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="接收人" width="100">
          <template #default="{ row }">{{ row.receiver_name || '—' }}</template>
        </el-table-column>
        <el-table-column label="待办/发起/已办" width="120">
          <template #default="{ row }">{{ row.todo_count }} / {{ row.running_count }} / {{ row.done_count }}</template>
        </el-table-column>
        <el-table-column label="任务/项目" width="95">
          <template #default="{ row }">{{ row.task_count }} / {{ row.project_count }}</template>
        </el-table-column>
        <el-table-column label="生效日期" width="105">
          <template #default="{ row }">{{ row.effective_date || '—' }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="110" show-overflow-tooltip />
        <el-table-column label="交接内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.item_summary }}</template>
        </el-table-column>
        <el-table-column label="办理人" width="85">
          <template #default="{ row }">{{ row.operator_name || '—' }}</template>
        </el-table-column>
        <el-table-column label="办理时间" width="140">
          <template #default="{ row }">{{ (row.created_at || '').replace('T', ' ') }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="handoverQuery.pageNum"
        v-model:page-size="handoverQuery.pageSize"
        :total="handoverTotal"
        layout="total, prev, pager, next"
        style="margin-top:12px; justify-content:flex-end"
        @current-change="loadHandoverRecords"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { UploadFilled } from "@element-plus/icons-vue";
import { pageUsers, listRoles, addUser, updateUser, deleteUser, resetUserPassword, changeUserStatus, unlockUser, orgTree, importUsers, exportUsers, orgTreeAdmin, orgTreeEnabled, createOrg, updateOrg, deleteOrg, listPositions, listUserOptions, onboardUser, handoverPreview, userHandover, pageHandovers, type UserItem, type RoleOption, type OrgNode, type OrgForm, type ImportResult, type HandoverPreview, type HandoverRecord, type PositionItem } from "@/api/user";
import { hasPerm } from "@/utils/permission";
import { useUserStore } from "@/store/user";
import { getUserExtraPermissions, grantUserExtraPermission, revokeUserExtraPermission, type ExtraPermissionItem } from "@/api/user";
import PositionPanel from "./PositionPanel.vue";
import TitlePanel from "./TitlePanel.vue";
import GroupPanel from "./GroupPanel.vue";

const userStore = useUserStore();
// 只有管理员能在这里发任务权限，项目经理走项目成员页
const isSysAdmin = computed(() => (userStore.roles || []).includes("admin"));
// 薪资单独一个权限码，没权限的连列都不显示
const canViewSalary = computed(() => isSysAdmin.value || hasPerm("user:salary:view"));
// 组织架构默认只读，有权限可切到维护
const orgTab = ref<"view" | "edit">("view");
const canManageOrg = computed(() => hasPerm("org:manage"));

// 任务中心权限
const extraPermVisible = ref(false);
const extraPermLoading = ref(false);
const extraPermUser = ref<UserItem | null>(null);
const extraPermItems = ref<ExtraPermissionItem[]>([]);
const extraPermTaskAccess = ref(false);
const extraPermCanGrant = ref(false);
const extraPermCanGrantEnabled = ref(false);

async function openExtraPerm(row: UserItem) {
  extraPermUser.value = row;
  extraPermVisible.value = true;
  await loadExtraPerm();
}

async function loadExtraPerm() {
  if (!extraPermUser.value) return;
  extraPermLoading.value = true;
  try {
    const info = await getUserExtraPermissions(extraPermUser.value.id);
    extraPermItems.value = info.items || [];
    extraPermTaskAccess.value = !!info.taskAccess;
    extraPermCanGrant.value = extraPermItems.value.some((it) => it.menuCode === "task:grant");
    extraPermCanGrantEnabled.value = !!info.canGrantPermission;
  } finally {
    extraPermLoading.value = false;
  }
}

async function toggleTaskAccess(val: boolean | string | number) {
  await togglePermission("task:access", !!val);
}

async function toggleGrantPermission(val: boolean | string | number) {
  await togglePermission("task:grant", !!val);
}

async function togglePermission(code: string, on: boolean) {
  if (!extraPermUser.value) return;
  const target = extraPermUser.value;
  extraPermLoading.value = true;
  try {
    if (on) {
      await grantUserExtraPermission(target.id, code);
      ElMessage.success("已开通");
    } else {
      const item = extraPermItems.value.find((it) => it.menuCode === code);
      if (item) await revokeUserExtraPermission(target.id, item.menuId);
      ElMessage.success("已回收");
    }
    await loadExtraPerm();
  } catch (e) {
    await loadExtraPerm();
  } finally {
    extraPermLoading.value = false;
  }
}

async function handleRevokeExtraPerm(row: ExtraPermissionItem) {
  if (!extraPermUser.value) return;
  await ElMessageBox.confirm(`确定回收「${row.menuName}」吗？`, "回收权限", { type: "warning" });
  await revokeUserExtraPermission(extraPermUser.value.id, row.menuId);
  ElMessage.success("已回收");
  await loadExtraPerm();
}

const loading = ref(false);
const list = ref<UserItem[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, keyword: "", status: null as number | null, orgId: null as number | null });

const roleOptions = ref<RoleOption[]>([]);
const positionOptions = ref<PositionItem[]>([]);

// 入职办理
const onboardVisible = ref(false);
const onboarding = ref(false);
const onboardRef = ref<FormInstance>();
const onboardForm = reactive<Partial<UserItem>>({ gender: 0, roleIds: [], org_id: 0 });
const onboardRules: FormRules = {
  username: [{ required: true, message: "请输入登录用户名", trigger: "blur" }],
  real_name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  org_id: [{ required: true, message: "请选择入职部门", trigger: "change" }],
};

// 离职交接
const handoverVisible = ref(false);
const handoverLoading = ref(false);
const handoverSubmitting = ref(false);
const handoverPreviewData = ref<HandoverPreview | null>(null);
const handoverLeaver = ref<UserItem | null>(null);
const receiverOptions = ref<UserItem[]>([]);
const handoverForm = reactive<{ mode: "transfer" | "auto" | "keep"; receiverId: number | null; reason: string; effectiveDate: string | null; transferRunning: boolean }>({
  mode: "transfer",
  receiverId: null,
  reason: "",
  effectiveDate: null,
  transferRunning: true,
});

// 说明跟着处理方式变，转交时得有接收人
const handoverModeHint = computed(() => {
  if (handoverForm.mode === "keep") return "（仍留在原账号，需管理员后续转交）";
  if (handoverForm.mode === "auto") return "（自动转给直属上级）";
  return "（转给接收人）";
});

// 交接记录
const handoverRecordsVisible = ref(false);
const handoverRecordsLoading = ref(false);
const handoverRecords = ref<HandoverRecord[]>([]);
const handoverTotal = ref(0);
const handoverQuery = reactive({ pageNum: 1, pageSize: 10 });

// 部门树（含“全部部门”根节点，id=0 表示不筛选）
const orgNodes = ref<OrgNode[]>([{ id: 0, label: "全部部门", children: [] }]);

const dialogVisible = ref(false);
const isEdit = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<Partial<UserItem>>({ gender: 0, roleIds: [], org_id: 0 });

const resetVisible = ref(false);
const resetting = ref(false);
const resetRef = ref<FormInstance>();
const resetForm = reactive<{ userId: number | null; username: string; newPassword: string }>({ userId: null, username: "", newPassword: "" });

// 批量导入
const importVisible = ref(false);
const importing = ref(false);
const importFile = ref<File | null>(null);
const importResult = ref<ImportResult | null>(null);

function handleFileChange(uploadFile: { raw?: File }) {
    importFile.value = uploadFile.raw || null;
    importResult.value = null;
}

async function handleImportSubmit() {
    if (!importFile.value) return;
    importing.value = true;
    try {
        importResult.value = await importUsers(importFile.value);
        load();
    } finally {
        importing.value = false;
    }
}

const formRules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  real_name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  password: [{ required: true, message: "请输入初始密码", trigger: "blur" }],
};
const resetRules: FormRules = {
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 8, max: 20, message: "长度需为 8-20 位", trigger: "blur" },
  ],
};

// 最近登录：今天、昨天、前天用彩色标签，更早显示完整时间
function loginTimeTag(t?: string) {
  const base = { full: true, label: "", type: "info" as const, time: "", text: t || "—" };
  if (!t) return base;
  const d = new Date(t);
  if (isNaN(d.getTime())) return { ...base, text: t.replace("T", " ") };
  const pad = (n: number) => String(n).padStart(2, "0");
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const dayStart = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const diff = Math.round((startOfToday - dayStart) / 86400000);
  if (diff === 0) return { full: false, label: "今天", type: "success" as const, time, text: "" };
  if (diff === 1) return { full: false, label: "昨天", type: "warning" as const, time, text: "" };
  if (diff === 2) return { full: false, label: "前天", type: "info" as const, time, text: "" };
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  return { ...base, text: `${date} ${time}` };
}

const genderText = (g: number) => (g === 1 ? "男" : g === 2 ? "女" : "未知");

async function load() {
  loading.value = true;
  try {
    const res = await pageUsers({ pageNum: query.pageNum, pageSize: query.pageSize, keyword: query.keyword || undefined, status: query.status, orgId: query.orgId });
    list.value = res.records;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

// 部门维护
const deptDialogVisible = ref(false);
const deptIsEdit = ref(false);
const deptSaving = ref(false);
const deptForm = reactive<OrgForm & { parent_name?: string }>({
  org_name: "",
  org_type: 2,
  sort_order: 0,
  parent_id: null,
  parent_name: "（顶级部门）",
});

// 部门树既筛用户也做维护，没有 org:manage 只能读启用部门
async function reloadOrgTree() {
  try {
    const tree = hasPerm("org:manage") ? await orgTreeAdmin() : await orgTreeEnabled();
    orgNodes.value = [{ id: 0, label: "全部部门", children: tree || [] }];
  } catch {
    orgNodes.value = [{ id: 0, label: "全部部门", children: [] }];
  }
}

function openDeptDialog(node: OrgNode | null, isEdit = false) {
  deptIsEdit.value = isEdit;
  deptForm.id = isEdit && node ? node.id : undefined;
  deptForm.org_name = isEdit && node ? node.label : "";
  deptForm.org_type = 2;
  deptForm.sort_order = 0;
  if (!node) {
    deptForm.parent_id = null;
    deptForm.parent_name = "（顶级部门）";
  } else if (isEdit) {
    deptForm.parent_id = null;
    deptForm.parent_name = "（保持原上级）";
  } else {
    deptForm.parent_id = node.id;
    deptForm.parent_name = node.label;
  }
  deptDialogVisible.value = true;
}

async function saveDept() {
  if (!deptForm.org_name || !deptForm.org_name.trim()) {
    ElMessage.warning("请输入部门名称");
    return;
  }
  deptSaving.value = true;
  try {
    if (deptIsEdit.value) {
      await updateOrg({
        id: deptForm.id,
        org_name: deptForm.org_name,
        org_type: deptForm.org_type,
        sort_order: deptForm.sort_order,
      });
      ElMessage.success("部门已更新");
    } else {
      await createOrg({
        parent_id: deptForm.parent_id,
        org_name: deptForm.org_name,
        org_type: deptForm.org_type,
        sort_order: deptForm.sort_order,
      });
      ElMessage.success("部门已创建");
    }
    deptDialogVisible.value = false;
    await reloadOrgTree();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "保存失败");
  } finally {
    deptSaving.value = false;
  }
}

async function removeDept(node: OrgNode) {
  try {
    await ElMessageBox.confirm(
      `确定删除部门「${node.label}」吗？如有子部门或部门下有员工将无法删除。`,
      "删除部门",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" }
    );
  } catch {
    return;
  }
  try {
    await deleteOrg(node.id);
    ElMessage.success("已删除");
    await reloadOrgTree();
    query.orgId = null;
    query.pageNum = 1;
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "删除失败");
  }
}

// 点击部门节点：id=0 表示全部部门
function handleOrgClick(data: OrgNode) {
  query.orgId = data.id === 0 ? null : data.id;
  query.pageNum = 1;
  load();
}

const exporting = ref(false);
const positionDrawerVisible = ref(false);
const titleDrawerVisible = ref(false);
const groupDrawerVisible = ref(false);

// 按当前筛选导出 Excel，手机号后端已脱敏
async function handleExport() {
  exporting.value = true;
  try {
    const blob = await exportUsers({
      keyword: query.keyword || undefined,
      status: query.status,
      orgId: query.orgId,
    });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "用户列表.xlsx";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    ElMessage.success("导出成功");
  } catch (e) {
    ElMessage.error("导出失败");
  } finally {
    exporting.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  load();
}
function handleReset() {
  query.keyword = "";
  query.status = null;
  query.pageNum = 1;
  load();
}

function openAdd() {
  isEdit.value = false;
  Object.assign(form, { id: undefined, username: "", real_name: "", gender: 0, phone: "", email: "", password: "", entry_date: null, roleIds: [], org_id: 0 });
  dialogVisible.value = true;
}

function openEdit(row: UserItem) {
  isEdit.value = true;
  Object.assign(form, { ...row, roleIds: [...(row.roleIds || [])] });
  dialogVisible.value = true;
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    if (isEdit.value) {
      await updateUser(form);
      ElMessage.success("保存成功");
    } else {
      await addUser(form);
      ElMessage.success("新增成功，用户首次登录需修改密码");
    }
    dialogVisible.value = false;
    load();
  } finally {
    saving.value = false;
  }
}

function openReset(row: UserItem) {
  resetForm.userId = row.id;
  resetForm.username = row.username;
  resetForm.newPassword = "";
  resetVisible.value = true;
}

async function handleResetSave() {
  const valid = await resetRef.value?.validate().catch(() => false);
  if (!valid) return;
  resetting.value = true;
  try {
    await resetUserPassword(resetForm.userId!, resetForm.newPassword);
    ElMessage.success("密码已重置，用户下次登录需修改密码");
    resetVisible.value = false;
  } finally {
    resetting.value = false;
  }
}

async function handleUnlock(row: UserItem) {
  await unlockUser(row.id);
  ElMessage.success("已解锁");
  load();
}

async function handleToggleStatus(row: UserItem) {
  const next = row.status === 1 ? 0 : 1;
  try {
    await ElMessageBox.confirm(`确定${next === 0 ? "停用" : "启用"}用户「${row.real_name}」吗？`, "提示", { type: "warning" });
  } catch {
    return;
  }
  await changeUserStatus(row.id, next);
  ElMessage.success(next === 0 ? "已停用，该用户将无法登录" : "已启用");
  load();
}

async function handleDelete(row: UserItem) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.real_name}」吗？删除后其历史数据保留，账号将无法登录。`, "提示", { type: "warning" });
  } catch {
    return;
  }
  await deleteUser(row.id);
  ElMessage.success("已删除");
  load();
}

// 入职办理
function openOnboard() {
  Object.assign(onboardForm, {
    username: "", real_name: "", gender: 0, phone: "", email: "", password: "",
    entry_date: null, roleIds: [], org_id: 0,
    emergency_contact: "", emergency_phone: "", bank_account: "", bank_name: "",
  });
  onboardVisible.value = true;
  if (!positionOptions.value.length) loadPositions();
}

async function loadPositions() {
  try {
    positionOptions.value = await listPositions();
  } catch {
    // 没有职位维护权限时下拉留空，不影响开通账号
    positionOptions.value = [];
  }
}

async function handleOnboard() {
  const valid = await onboardRef.value?.validate().catch(() => false);
  if (!valid) return;
  onboarding.value = true;
  try {
    const result = await onboardUser(onboardForm);
    onboardVisible.value = false;
    load();
    await ElMessageBox.alert(
      `账号 ${result.username} 已开通，初始密码：${result.initPassword}\n请转交本人，首次登录会强制修改密码。`,
      "入职办理成功",
      { confirmButtonText: "我已记录", dangerouslyUseHTMLString: false }
    );
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "开通失败");
  } finally {
    onboarding.value = false;
  }
}

// 离职交接
// 任务状态文案，交接清单直接显示中文
function taskStatusText(status?: number | null) {
  const map: Record<number, string> = { 0: "待接收", 1: "待验收", 2: "进行中", 3: "待确认", 4: "已完成", 5: "已撤销", 6: "已拒绝" };
  if (status === null || status === undefined) return "—";
  return map[status] || "未知";
}

async function openHandover(row: UserItem) {
  handoverLeaver.value = row;
  handoverPreviewData.value = null;
  handoverForm.mode = "transfer";
  handoverForm.receiverId = null;
  handoverForm.reason = "";
  handoverForm.effectiveDate = null;
  handoverForm.transferRunning = true;
  handoverVisible.value = true;
  handoverLoading.value = true;
  try {
    handoverPreviewData.value = await handoverPreview(row.id);
    // 接收人候选排除离职人自己，停用账号不能接手待办
    const options = await listUserOptions();
    receiverOptions.value = options.filter((u) => u.id !== row.id && u.status === 1);
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "读取交接清单失败");
  } finally {
    handoverLoading.value = false;
  }
}

async function submitHandover() {
  if (!handoverLeaver.value) return;
  if (handoverForm.mode === "transfer" && !handoverForm.receiverId) return;
  const receiverName = handoverForm.mode === "auto"
    ? (handoverPreviewData.value?.autoReceiverName || "直属上级（没找到时交给系统管理员）")
    : handoverForm.mode === "keep"
      ? "原账号"
      : (receiverOptions.value.find((u) => u.id === handoverForm.receiverId)?.real_name || "");
  const todoCount = handoverPreviewData.value?.todoCount ?? 0;
  const taskCount = handoverPreviewData.value?.taskCount ?? 0;
  const projectCount = handoverPreviewData.value?.projectCount ?? 0;
  const actionText = handoverForm.mode === "keep"
    ? `保留在原账号名下，停用账号`
    : `转给「${receiverName}」`;
  try {
    await ElMessageBox.confirm(
      `确认把「${handoverLeaver.value.real_name}」的 ${todoCount} 条待办、${taskCount} 个未结束任务、${projectCount} 个项目`
    + `${actionText}吗？账号会立即停用。`,
      "确认交接",
      { type: "warning", confirmButtonText: "确认交接", cancelButtonText: "取消" }
    );
  } catch {
    return;
  }
  handoverSubmitting.value = true;
  try {
    const result = await userHandover(handoverLeaver.value.id, {
      mode: handoverForm.mode,
      receiverId: handoverForm.mode === "transfer" ? handoverForm.receiverId : undefined,
      reason: handoverForm.reason || undefined,
      effectiveDate: handoverForm.effectiveDate || undefined,
      transferRunning: handoverForm.mode === "keep" ? false : handoverForm.transferRunning,
    });
    handoverVisible.value = false;
    load();
    const resultText = result.receiverName
      ? `待办 ${result.todoCount} 条、审批中发起 ${result.runningCount} 条、任务 ${result.taskCount} 个、项目 ${result.projectCount} 个已转给 ${result.receiverName}，账号已停用。`
      : `待办 ${result.todoCount} 条、任务 ${result.taskCount} 个、项目 ${result.projectCount} 个仍留在原账号名下（已停用），需要时可再次交接。`;
    ElMessageBox.alert(`交接完成：${resultText}`, "交接结果", { confirmButtonText: "知道了" });
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "交接失败");
  } finally {
    handoverSubmitting.value = false;
  }
}

// 交接记录
function openHandovers() {
  handoverRecordsVisible.value = true;
  handoverQuery.pageNum = 1;
  loadHandoverRecords();
}

async function loadHandoverRecords() {
  handoverRecordsLoading.value = true;
  try {
    const data: any = await pageHandovers({ pageNum: handoverQuery.pageNum, pageSize: handoverQuery.pageSize });
    handoverRecords.value = data.records || [];
    handoverTotal.value = data.total || 0;
  } finally {
    handoverRecordsLoading.value = false;
  }
}

onMounted(async () => {
  load();
  roleOptions.value = await listRoles();
  // 部门树：默认全部展开
  await reloadOrgTree();
  loadPositions();
});
</script>

<style scoped>
.perm-user { font-size: 13px; color: #303133; margin-bottom: 10px; }
.perm-switch { display: block; margin: 6px 0; }
.perm-hint { font-size: 12px; color: #909399; line-height: 1.6; margin-top: 4px; }
.user-page {
  display: flex;
  gap: 16px;
  padding: 16px;
  align-items: flex-start;
}
.org-panel {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 8px;
  max-height: calc(100vh - 180px);
  overflow: auto;
}
.org-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.org-tabs :deep(.el-tabs__header) {
  margin: 0 0 6px;
}
.org-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  height: 30px;
  line-height: 30px;
}
.org-tabs :deep(.el-tabs__item.is-active) {
  font-weight: 600;
}
.org-edit-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.org-edit-tip {
  font-size: 11px;
  color: #e6a23c;
  line-height: 1.3;
}
.org-panel :deep(.el-tree) {
  min-width: max-content;
}
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: max-content;
  min-width: 100%;
  padding-right: 4px;
  gap: 4px;
}
.tree-actions {
  display: none;
  gap: 2px;
}
.tree-node:hover .tree-actions {
  display: inline-flex;
}
.tree-node:hover .tree-label {
  display: none;
}
.org-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding: 4px 6px 10px;
}
.user-content {
  flex: 1;
  min-width: 0;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.toolbar .spacer {
  flex: 1;
}
.muted {
  color: #c0c4cc;
  font-size: 12px;
}
/* 邮箱等长文本：溢出部分渐变消失（不用省略号） */
.fade-overflow {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: clip;
  -webkit-mask-image: linear-gradient(to right, #000 88%, transparent 100%);
  mask-image: linear-gradient(to right, #000 88%, transparent 100%);
}
.import-tip {
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
}
.import-tip p {
  margin: 4px 0;
}
.import-result {
  margin-top: 12px;
}
.import-errors {
  margin-top: 8px;
}
.import-errors ul {
  margin: 0;
  padding-left: 20px;
  font-size: 12px;
  color: #e6a23c;
  max-height: 160px;
  overflow: auto;
}
/* 无权限或未填时显示的占位符 */
.text-muted {
  color: #c0c4cc;
}
</style>
