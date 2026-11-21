<template>
    <div class="manage-page">
        <div class="page-head">
            <div class="head-left">
                <span class="head-bar"></span>
                <div>
                    <h2>系统管理</h2>
                    <p>系统参数、菜单与权限码、导航图片、操作日志、异常日志、通知模板与推送记录</p>
                </div>
            </div>
            <el-button @click="reloadCurrent">刷新</el-button>
        </div>

        <el-tabs v-model="activeTab" class="manage-tabs" @tab-change="onTabChange">
            <!-- 系统参数 -->
            <el-tab-pane label="系统参数" name="params">
                <div class="tab-toolbar">
                    <el-input v-model="paramKeyword" placeholder="参数名 / 键" clearable style="width: 220px" @keyup.enter="loadParams" />
                    <el-button type="primary" @click="loadParams">查询</el-button>
                    <el-button type="primary" plain @click="openParamDialog()">新增参数</el-button>
                </div>
                <el-table v-loading="loading" :data="params" border stripe>
                    <el-table-column prop="paramKey" label="参数键" min-width="180" />
                    <el-table-column prop="paramName" label="参数名" min-width="140" />
                    <el-table-column prop="paramValue" label="参数值" min-width="140" show-overflow-tooltip />
                    <el-table-column prop="paramGroup" label="分组" width="130" />
                    <el-table-column label="类型" width="100">
                        <template #default="{ row }">{{ paramTypeText(row.paramType) }}</template>
                    </el-table-column>
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? "启用" : "停用" }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
                    <el-table-column label="操作" width="140" fixed="right">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="openParamDialog(row)">编辑</el-button>
                            <el-button link type="danger" @click="removeParam(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-tab-pane>

            <!-- 菜单与权限码 -->
            <el-tab-pane label="菜单与权限码" name="menus">
                <div class="tab-toolbar">
                    <el-input v-model="menuKeyword" placeholder="菜单名 / 权限码" clearable style="width: 220px" @keyup.enter="loadMenus" />
                    <el-button type="primary" @click="loadMenus">查询</el-button>
                    <el-button type="primary" plain @click="openMenuDialog()">新增菜单</el-button>
                    <span class="toolbar-tip">权限码改动后请同步到「权限管理」给角色勾选，否则前端入口仍不显示</span>
                </div>
                <el-table v-loading="loading" :data="menus" border stripe max-height="520">
                    <el-table-column prop="menuName" label="名称" min-width="160" />
                    <el-table-column prop="menuCode" label="权限码" min-width="180">
                        <template #default="{ row }"><span class="code-text">{{ row.menuCode || "-" }}</span></template>
                    </el-table-column>
                    <el-table-column label="类型" width="100">
                        <template #default="{ row }">
                            <el-tag :type="menuTypeTag(row.menuType)" size="small" effect="plain">{{ menuTypeText(row.menuType) }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="parentName" label="上级" width="140" />
                    <el-table-column prop="path" label="路由" min-width="150" show-overflow-tooltip />
                    <el-table-column prop="icon" label="图标" width="110" />
                    <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? "启用" : "停用" }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="140" fixed="right">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="openMenuDialog(row)">编辑</el-button>
                            <el-button link type="danger" @click="removeMenu(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-tab-pane>

            <!-- 导航图片 -->
            <el-tab-pane label="导航图片" name="nav">
                <div class="tab-toolbar">
                    <el-button type="primary" plain @click="openNavDialog()">新增图片</el-button>
                    <span class="toolbar-tip">登录页与首页的轮播 / 导航图，只保存图片地址，文件请先上传到附件服务</span>
                </div>
                <el-table v-loading="loading" :data="navImages" border stripe>
                    <el-table-column prop="title" label="标题" min-width="150" />
                    <el-table-column prop="imageUrl" label="图片地址" min-width="240" show-overflow-tooltip />
                    <el-table-column prop="linkUrl" label="跳转地址" min-width="160" show-overflow-tooltip />
                    <el-table-column prop="position" label="位置" width="110" />
                    <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
                    <el-table-column prop="clickCount" label="点击" width="80" align="center" />
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? "启用" : "停用" }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="140" fixed="right">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="openNavDialog(row)">编辑</el-button>
                            <el-button link type="danger" @click="removeNav(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-tab-pane>

            <!-- 操作日志 -->
            <el-tab-pane label="操作日志" name="oplogs">
                <div class="tab-toolbar">
                    <el-input v-model="opQuery.keyword" placeholder="账号 / 模块 / 地址" clearable style="width: 220px" @keyup.enter="searchOpLogs" />
                    <el-select v-model="opQuery.status" placeholder="全部结果" clearable style="width: 130px">
                        <el-option label="成功" :value="1" />
                        <el-option label="失败" :value="0" />
                    </el-select>
                    <el-date-picker v-model="opRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
                    <el-button type="primary" @click="searchOpLogs">查询</el-button>
                    <el-button @click="resetOpLogs">重置</el-button>
                </div>
                <el-table v-loading="loading" :data="opLogs" border stripe>
                    <el-table-column type="expand">
                        <template #default="{ row }">
                            <div class="expand-box">
                                <div><b>请求参数：</b>{{ row.requestParams || "-" }}</div>
                                <div><b>错误信息：</b>{{ row.errorMsg || "-" }}</div>
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column prop="username" label="账号" width="120" />
                    <el-table-column prop="module" label="模块" width="120" />
                    <el-table-column prop="action" label="操作" min-width="140" show-overflow-tooltip />
                    <el-table-column prop="method" label="方法" width="90" />
                    <el-table-column prop="url" label="接口" min-width="220" show-overflow-tooltip />
                    <el-table-column prop="ipAddress" label="IP" width="130" />
                    <el-table-column label="结果" width="90">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? "成功" : "失败" }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="costTime" label="耗时(ms)" width="100" align="center" />
                    <el-table-column prop="createdAt" label="时间" width="170" />
                </el-table>
                <div class="pager">
                    <el-pagination
                        v-model:current-page="opQuery.pageNum"
                        v-model:page-size="opQuery.pageSize"
                        :total="opTotal"
                        :page-sizes="[10, 20, 50]"
                        layout="total, sizes, prev, pager, next"
                        @current-change="loadOpLogs"
                        @size-change="searchOpLogs"
                    />
                </div>
            </el-tab-pane>

            <!-- 异常日志 -->
            <el-tab-pane label="异常日志" name="exlogs">
                <div class="tab-toolbar">
                    <el-input v-model="exQuery.keyword" placeholder="账号 / 地址 / 异常信息" clearable style="width: 240px" @keyup.enter="searchExLogs" />
                    <el-select v-model="exQuery.resolved" placeholder="全部处理状态" clearable style="width: 150px">
                        <el-option label="待处理" :value="0" />
                        <el-option label="已处理" :value="1" />
                    </el-select>
                    <el-date-picker v-model="exRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
                    <el-button type="primary" @click="searchExLogs">查询</el-button>
                    <el-button @click="resetExLogs">重置</el-button>
                    <el-button type="warning" plain @click="openManualException">手工登记异常</el-button>
                </div>
                <el-table v-loading="loading" :data="exLogs" border stripe>
                    <el-table-column prop="createdAt" label="时间" width="170" />
                    <el-table-column prop="username" label="账号" width="110" />
                    <el-table-column prop="exceptionType" label="异常类型" min-width="160" show-overflow-tooltip />
                    <el-table-column prop="errorMsg" label="异常信息" min-width="240" show-overflow-tooltip />
                    <el-table-column prop="url" label="接口" min-width="180" show-overflow-tooltip />
                    <el-table-column prop="ipAddress" label="IP" width="120" />
                    <el-table-column label="处理状态" width="110">
                        <template #default="{ row }">
                            <el-tag :type="row.resolved === 1 ? 'success' : 'warning'" size="small">{{ row.resolved === 1 ? "已处理" : "待处理" }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="180" fixed="right">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="viewException(row)">详情</el-button>
                            <el-button v-if="row.resolved !== 1" link type="success" @click="resolveException(row)">标记已处理</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination
                        v-model:current-page="exQuery.pageNum"
                        v-model:page-size="exQuery.pageSize"
                        :total="exTotal"
                        :page-sizes="[10, 20, 50]"
                        layout="total, sizes, prev, pager, next"
                        @current-change="loadExLogs"
                        @size-change="searchExLogs"
                    />
                </div>
            </el-tab-pane>

            <!-- 通知模板 -->
            <el-tab-pane label="通知模板" name="templates">
                <div class="tab-toolbar">
                    <el-input v-model="templateKeyword" placeholder="模板名 / 编码" clearable style="width: 220px" @keyup.enter="loadTemplates" />
                    <el-button type="primary" @click="loadTemplates">查询</el-button>
                    <el-button type="primary" plain @click="openTemplateDialog()">新增模板</el-button>
                    <span class="toolbar-tip">标题与内容里用 {} 占位，如「{title} 需要你审批」</span>
                </div>
                <el-table v-loading="loading" :data="templates" border stripe>
                    <el-table-column prop="templateCode" label="模板编码" min-width="150" />
                    <el-table-column prop="templateName" label="模板名" min-width="150" />
                    <el-table-column prop="bizType" label="业务类型" width="140" />
                    <el-table-column prop="titleTemplate" label="标题模板" min-width="200" show-overflow-tooltip />
                    <el-table-column prop="contentTemplate" label="内容模板" min-width="240" show-overflow-tooltip />
                    <el-table-column prop="channels" label="推送渠道" width="120" />
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? "启用" : "停用" }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="140" fixed="right">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="openTemplateDialog(row)">编辑</el-button>
                            <el-button link type="danger" @click="removeTemplate(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-tab-pane>

            <!-- 推送记录 -->
            <el-tab-pane label="推送记录" name="pushlogs">
                <div class="tab-toolbar">
                    <el-input v-model="pushQuery.keyword" placeholder="账号 / 标题" clearable style="width: 220px" @keyup.enter="searchPushLogs" />
                    <el-select v-model="pushQuery.pushStatus" placeholder="全部推送状态" clearable style="width: 160px">
                        <el-option label="未推送" :value="0" />
                        <el-option label="推送成功" :value="1" />
                        <el-option label="推送失败" :value="2" />
                    </el-select>
                    <el-date-picker v-model="pushRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
                    <el-button type="primary" @click="searchPushLogs">查询</el-button>
                    <el-button @click="resetPushLogs">重置</el-button>
                </div>
                <el-table v-loading="loading" :data="pushLogs" border stripe>
                    <el-table-column prop="userName" label="接收人" width="120" />
                    <el-table-column prop="title" label="通知标题" min-width="220" show-overflow-tooltip />
                    <el-table-column prop="bizType" label="业务类型" width="140" />
                    <el-table-column label="已读" width="80" align="center">
                        <template #default="{ row }">{{ row.isRead === 1 ? "是" : "否" }}</template>
                    </el-table-column>
                    <el-table-column label="推送状态" width="110">
                        <template #default="{ row }">
                            <el-tag :type="pushStatusTag(row.pushStatus)" size="small">{{ row.pushStatusText || "-" }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="pushChannel" label="渠道" width="110" />
                    <el-table-column prop="pushFailReason" label="失败原因" min-width="160" show-overflow-tooltip />
                    <el-table-column prop="createdAt" label="生成时间" width="170" />
                    <el-table-column label="操作" width="110" fixed="right">
                        <template #default="{ row }">
                            <el-button link type="primary" :disabled="row.pushStatus === 1" @click="resendPush(row)">重新推送</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination
                        v-model:current-page="pushQuery.pageNum"
                        v-model:page-size="pushQuery.pageSize"
                        :total="pushTotal"
                        :page-sizes="[10, 20, 50]"
                        layout="total, sizes, prev, pager, next"
                        @current-change="loadPushLogs"
                        @size-change="searchPushLogs"
                    />
                </div>
            </el-tab-pane>
        </el-tabs>

        <!-- 参数弹窗 -->
        <el-dialog v-model="paramDialog" :title="paramForm.id ? '编辑参数' : '新增参数'" width="560px">
            <el-form :model="paramForm" label-width="110px">
                <el-form-item label="参数键" required>
                    <el-input v-model="paramForm.paramKey" placeholder="如 attendance_geo_radius" :disabled="!!paramForm.id" />
                </el-form-item>
                <el-form-item label="参数名">
                    <el-input v-model="paramForm.paramName" placeholder="如 签到允许半径（米）" />
                </el-form-item>
                <el-form-item label="参数值">
                    <el-input v-model="paramForm.paramValue" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="分组">
                    <el-input v-model="paramForm.paramGroup" placeholder="如 attendance / system" />
                </el-form-item>
                <el-form-item label="值类型">
                    <el-select v-model="paramForm.paramType" style="width: 100%">
                        <el-option label="字符串" :value="1" />
                        <el-option label="数字" :value="2" />
                        <el-option label="布尔" :value="3" />
                        <el-option label="JSON" :value="4" />
                    </el-select>
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="paramForm.sortOrder" :min="0" :max="999" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="paramForm.status" :active-value="1" :inactive-value="0" />
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="paramForm.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="paramDialog = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitParam">保存</el-button>
            </template>
        </el-dialog>

        <!-- 菜单弹窗 -->
        <el-dialog v-model="menuDialog" :title="menuForm.id ? '编辑菜单' : '新增菜单'" width="600px">
            <el-form :model="menuForm" label-width="110px">
                <el-form-item label="上级菜单">
                    <el-select v-model="menuForm.parentId" placeholder="不选表示一级菜单" clearable style="width: 100%">
                        <el-option v-for="m in parentOptions" :key="m.id" :label="m.menuName" :value="m.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="名称" required>
                    <el-input v-model="menuForm.menuName" placeholder="如 权限管理" />
                </el-form-item>
                <el-form-item label="类型" required>
                    <el-radio-group v-model="menuForm.menuType">
                        <el-radio :value="1">目录</el-radio>
                        <el-radio :value="2">菜单</el-radio>
                        <el-radio :value="3">按钮/权限码</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="权限码">
                    <el-input v-model="menuForm.menuCode" placeholder="如 role:manage，按钮级必填" />
                </el-form-item>
                <el-form-item label="路由地址">
                    <el-input v-model="menuForm.path" placeholder="如 /home/system/role" />
                </el-form-item>
                <el-form-item label="组件路径">
                    <el-input v-model="menuForm.component" placeholder="如 views/system/role/index.vue" />
                </el-form-item>
                <el-form-item label="图标">
                    <el-input v-model="menuForm.icon" placeholder="Element Plus 图标名，如 Lock" />
                </el-form-item>
                <el-form-item label="是否导航">
                    <el-switch v-model="menuForm.isNav" :active-value="1" :inactive-value="0" />
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="menuForm.sortOrder" :min="0" :max="999" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="menuForm.status" :active-value="1" :inactive-value="0" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="menuDialog = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitMenu">保存</el-button>
            </template>
        </el-dialog>

        <!-- 导航图片弹窗 -->
        <el-dialog v-model="navDialog" :title="navForm.id ? '编辑导航图片' : '新增导航图片'" width="560px">
            <el-form :model="navForm" label-width="110px">
                <el-form-item label="标题">
                    <el-input v-model="navForm.title" />
                </el-form-item>
                <el-form-item label="图片地址" required>
                    <el-input v-model="navForm.imageUrl" placeholder="https://..." />
                </el-form-item>
                <el-form-item label="跳转地址">
                    <el-input v-model="navForm.linkUrl" placeholder="可选" />
                </el-form-item>
                <el-form-item label="位置">
                    <el-select v-model="navForm.position" style="width: 100%">
                        <el-option label="首页轮播 banner" value="banner" />
                        <el-option label="导航栏 nav" value="nav" />
                        <el-option label="侧边栏 sidebar" value="sidebar" />
                    </el-select>
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="navForm.sortOrder" :min="0" :max="999" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="navForm.status" :active-value="1" :inactive-value="0" />
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="navForm.remark" type="textarea" :rows="2" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="navDialog = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitNav">保存</el-button>
            </template>
        </el-dialog>

        <!-- 模板弹窗 -->
        <el-dialog v-model="templateDialog" :title="templateForm.id ? '编辑通知模板' : '新增通知模板'" width="620px">
            <el-form :model="templateForm" label-width="110px">
                <el-form-item label="模板编码" required>
                    <el-input v-model="templateForm.templateCode" placeholder="如 flow_arrive" :disabled="!!templateForm.id" />
                </el-form-item>
                <el-form-item label="模板名" required>
                    <el-input v-model="templateForm.templateName" placeholder="如 待办到达提醒" />
                </el-form-item>
                <el-form-item label="业务类型">
                    <el-input v-model="templateForm.bizType" placeholder="如 flow_instance / task / attendance" />
                </el-form-item>
                <el-form-item label="标题模板" required>
                    <el-input v-model="templateForm.titleTemplate" placeholder="如 新的待办流程：{title}" />
                </el-form-item>
                <el-form-item label="内容模板">
                    <el-input v-model="templateForm.contentTemplate" type="textarea" :rows="3" placeholder="如 {creator} 提交了「{title}」，请及时处理" />
                </el-form-item>
                <el-form-item label="推送渠道">
                    <el-input v-model="templateForm.channels" placeholder="如 inner / wecom,dingtalk" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="templateForm.status" :active-value="1" :inactive-value="0" />
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="templateForm.remark" type="textarea" :rows="2" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="templateDialog = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitTemplate">保存</el-button>
            </template>
        </el-dialog>

        <!-- 异常详情抽屉 -->
        <el-drawer v-model="exDrawer" title="异常详情" size="560px">
            <el-descriptions v-if="exDetail" :column="1" border>
                <el-descriptions-item label="时间">{{ exDetail.createdAt }}</el-descriptions-item>
                <el-descriptions-item label="账号">{{ exDetail.username || "-" }}</el-descriptions-item>
                <el-descriptions-item label="接口">{{ exDetail.method }} {{ exDetail.url }}</el-descriptions-item>
                <el-descriptions-item label="IP">{{ exDetail.ipAddress || "-" }}</el-descriptions-item>
                <el-descriptions-item label="环境">{{ exDetail.browser || "-" }} / {{ exDetail.os || "-" }}</el-descriptions-item>
                <el-descriptions-item label="异常类型">{{ exDetail.exceptionType || "-" }}</el-descriptions-item>
                <el-descriptions-item label="异常信息">{{ exDetail.errorMsg || "-" }}</el-descriptions-item>
                <el-descriptions-item label="请求参数">{{ exDetail.requestParams || "-" }}</el-descriptions-item>
                <el-descriptions-item label="处理状态">{{ exDetail.resolved === 1 ? "已处理" : "待处理" }}</el-descriptions-item>
                <el-descriptions-item label="处理说明">{{ exDetail.resolveNote || "-" }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="exDetail?.stackTrace" class="stack-box">
                <div class="stack-title">堆栈</div>
                <pre class="stack-text">{{ exDetail.stackTrace }}</pre>
            </div>
        </el-drawer>

        <!-- 手工登记异常 -->
        <el-dialog v-model="manualDialog" title="手工登记异常" width="560px">
            <el-form :model="manualForm" label-width="110px">
                <el-form-item label="异常类型" required>
                    <el-input v-model="manualForm.exceptionType" placeholder="如 数据核对异常 / 定时任务失败" />
                </el-form-item>
                <el-form-item label="异常信息" required>
                    <el-input v-model="manualForm.errorMsg" type="textarea" :rows="3" placeholder="发生了什么、影响范围" />
                </el-form-item>
                <el-form-item label="相关接口">
                    <el-input v-model="manualForm.url" placeholder="/api/..." />
                </el-form-item>
                <el-form-item label="请求参数">
                    <el-input v-model="manualForm.requestParams" type="textarea" :rows="2" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="manualDialog = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitManualException">登记</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
    createExceptionLog,
    createMenu,
    createNavImage,
    createNotificationTemplate,
    createSystemParam,
    deleteMenu,
    deleteNavImage,
    deleteNotificationTemplate,
    deleteSystemParam,
    getExceptionLog,
    listMenus,
    listNavImages,
    listNotificationTemplates,
    listSystemParams,
    pageExceptionLogs,
    pageOperationLogs,
    pagePushLogs,
    resendPushLog,
    resolveExceptionLog,
    updateMenu,
    updateNavImage,
    updateNotificationTemplate,
    updateSystemParam,
    type ExceptionLogItem,
    type MenuItemRow,
    type NavImageItem,
    type NotificationTemplateItem,
    type OperationLogItem,
    type PushLogItem,
    type SystemParamItem,
} from "@/api/system";

const activeTab = ref("params");
const loading = ref(false);
const saving = ref(false);

// 系统参数
const params = ref<SystemParamItem[]>([]);
const paramKeyword = ref("");
const paramDialog = ref(false);
const paramForm = reactive<Partial<SystemParamItem>>({});

// 菜单
const menus = ref<MenuItemRow[]>([]);
const menuKeyword = ref("");
const menuDialog = ref(false);
const menuForm = reactive<Partial<MenuItemRow>>({ menuType: 2, isNav: 1, status: 1, sortOrder: 10, parentId: 0 });
const parentOptions = computed(() => menus.value.filter((m) => m.menuType !== 3));

// 导航图片
const navImages = ref<NavImageItem[]>([]);
const navDialog = ref(false);
const navForm = reactive<Partial<NavImageItem>>({ position: "banner", status: 1, sortOrder: 1 });

// 操作日志
const opLogs = ref<OperationLogItem[]>([]);
const opTotal = ref(0);
const opRange = ref<string[]>([]);
const opQuery = reactive<{ pageNum: number; pageSize: number; keyword: string; status: number | null }>({
    pageNum: 1,
    pageSize: 10,
    keyword: "",
    status: null,
});

// 异常日志
const exLogs = ref<ExceptionLogItem[]>([]);
const exTotal = ref(0);
const exRange = ref<string[]>([]);
const exDrawer = ref(false);
const exDetail = ref<ExceptionLogItem | null>(null);
const manualDialog = ref(false);
const manualForm = reactive<Partial<ExceptionLogItem>>({});
const exQuery = reactive<{ pageNum: number; pageSize: number; keyword: string; resolved: number | null }>({
    pageNum: 1,
    pageSize: 10,
    keyword: "",
    resolved: null,
});

// 通知模板
const templates = ref<NotificationTemplateItem[]>([]);
const templateKeyword = ref("");
const templateDialog = ref(false);
const templateForm = reactive<Partial<NotificationTemplateItem>>({ status: 1 });

// 推送记录
const pushLogs = ref<PushLogItem[]>([]);
const pushTotal = ref(0);
const pushRange = ref<string[]>([]);
const pushQuery = reactive<{ pageNum: number; pageSize: number; keyword: string; pushStatus: number | null }>({
    pageNum: 1,
    pageSize: 10,
    keyword: "",
    pushStatus: null,
});

function paramTypeText(type?: number) {
    return ["-", "字符串", "数字", "布尔", "JSON"][type ?? 0] || "字符串";
}

function menuTypeText(type?: number) {
    return type === 1 ? "目录" : type === 2 ? "菜单" : "按钮";
}

function menuTypeTag(type?: number) {
    return type === 1 ? "warning" : type === 2 ? "primary" : "info";
}

function pushStatusTag(status?: number) {
    if (status === 1) return "success";
    if (status === 2) return "danger";
    return "info";
}

async function loadParams() {
    loading.value = true;
    try {
        params.value = await listSystemParams({ keyword: paramKeyword.value || undefined });
    } finally {
        loading.value = false;
    }
}

async function loadMenus() {
    loading.value = true;
    try {
        menus.value = await listMenus({ keyword: menuKeyword.value || undefined });
    } finally {
        loading.value = false;
    }
}

async function loadNavImages() {
    loading.value = true;
    try {
        navImages.value = await listNavImages({});
    } finally {
        loading.value = false;
    }
}

async function loadOpLogs() {
    loading.value = true;
    try {
        const res = await pageOperationLogs({
            pageNum: opQuery.pageNum,
            pageSize: opQuery.pageSize,
            keyword: opQuery.keyword || undefined,
            status: opQuery.status,
            startDate: opRange.value?.[0],
            endDate: opRange.value?.[1],
        });
        opLogs.value = res.records || [];
        opTotal.value = res.total || 0;
    } finally {
        loading.value = false;
    }
}

async function loadExLogs() {
    loading.value = true;
    try {
        const res = await pageExceptionLogs({
            pageNum: exQuery.pageNum,
            pageSize: exQuery.pageSize,
            keyword: exQuery.keyword || undefined,
            resolved: exQuery.resolved,
            startDate: exRange.value?.[0],
            endDate: exRange.value?.[1],
        });
        exLogs.value = res.records || [];
        exTotal.value = res.total || 0;
    } finally {
        loading.value = false;
    }
}

async function loadTemplates() {
    loading.value = true;
    try {
        templates.value = await listNotificationTemplates({ keyword: templateKeyword.value || undefined });
    } finally {
        loading.value = false;
    }
}

async function loadPushLogs() {
    loading.value = true;
    try {
        const res = await pagePushLogs({
            pageNum: pushQuery.pageNum,
            pageSize: pushQuery.pageSize,
            keyword: pushQuery.keyword || undefined,
            pushStatus: pushQuery.pushStatus,
            startDate: pushRange.value?.[0],
            endDate: pushRange.value?.[1],
        });
        pushLogs.value = res.records || [];
        pushTotal.value = res.total || 0;
    } finally {
        loading.value = false;
    }
}

function searchOpLogs() {
    opQuery.pageNum = 1;
    loadOpLogs();
}

function resetOpLogs() {
    opQuery.keyword = "";
    opQuery.status = null;
    opRange.value = [];
    searchOpLogs();
}

function searchExLogs() {
    exQuery.pageNum = 1;
    loadExLogs();
}

function resetExLogs() {
    exQuery.keyword = "";
    exQuery.resolved = null;
    exRange.value = [];
    searchExLogs();
}

function searchPushLogs() {
    pushQuery.pageNum = 1;
    loadPushLogs();
}

function resetPushLogs() {
    pushQuery.keyword = "";
    pushQuery.pushStatus = null;
    pushRange.value = [];
    searchPushLogs();
}

function onTabChange(name: string) {
    if (name === "params") loadParams();
    else if (name === "menus") loadMenus();
    else if (name === "nav") loadNavImages();
    else if (name === "oplogs") loadOpLogs();
    else if (name === "exlogs") loadExLogs();
    else if (name === "templates") loadTemplates();
    else if (name === "pushlogs") loadPushLogs();
}

function reloadCurrent() {
    onTabChange(activeTab.value);
}

// 参数
function openParamDialog(row?: SystemParamItem) {
    Object.keys(paramForm).forEach((k) => delete (paramForm as any)[k]);
    Object.assign(paramForm, row ? { ...row } : { paramType: 1, status: 1, sortOrder: 10 });
    paramDialog.value = true;
}

async function submitParam() {
    if (!paramForm.paramKey) {
        ElMessage.warning("请填写参数键");
        return;
    }
    saving.value = true;
    try {
        if (paramForm.id) await updateSystemParam(paramForm.id, paramForm);
        else await createSystemParam(paramForm);
        ElMessage.success("已保存");
        paramDialog.value = false;
        loadParams();
    } finally {
        saving.value = false;
    }
}

async function removeParam(row: SystemParamItem) {
    await ElMessageBox.confirm("确定删除参数「" + row.paramKey + "」吗？依赖它的功能会退回默认行为。", "删除参数", { type: "warning" });
    await deleteSystemParam(row.id);
    ElMessage.success("已删除");
    loadParams();
}

// 菜单
function openMenuDialog(row?: MenuItemRow) {
    Object.keys(menuForm).forEach((k) => delete (menuForm as any)[k]);
    Object.assign(menuForm, row ? { ...row } : { menuType: 2, isNav: 1, status: 1, sortOrder: 10, parentId: 0 });
    menuDialog.value = true;
    if (menus.value.length === 0) loadMenus();
}

async function submitMenu() {
    if (!menuForm.menuName) {
        ElMessage.warning("请填写菜单名称");
        return;
    }
    saving.value = true;
    try {
        const payload = { ...menuForm, parentId: menuForm.parentId || 0 };
        if (menuForm.id) await updateMenu(menuForm.id, payload);
        else await createMenu(payload);
        ElMessage.success("已保存，记得去「权限管理」把新权限勾给角色");
        menuDialog.value = false;
        loadMenus();
    } finally {
        saving.value = false;
    }
}

async function removeMenu(row: MenuItemRow) {
    await ElMessageBox.confirm("确定删除「" + row.menuName + "」吗？该权限码会同时从所有角色上移除。", "删除菜单", { type: "warning" });
    await deleteMenu(row.id);
    ElMessage.success("已删除");
    loadMenus();
}

// 导航图片
function openNavDialog(row?: NavImageItem) {
    Object.keys(navForm).forEach((k) => delete (navForm as any)[k]);
    Object.assign(navForm, row ? { ...row } : { position: "banner", status: 1, sortOrder: 1 });
    navDialog.value = true;
}

async function submitNav() {
    if (!navForm.imageUrl) {
        ElMessage.warning("请填写图片地址");
        return;
    }
    saving.value = true;
    try {
        if (navForm.id) await updateNavImage(navForm.id, navForm);
        else await createNavImage(navForm);
        ElMessage.success("已保存");
        navDialog.value = false;
        loadNavImages();
    } finally {
        saving.value = false;
    }
}

async function removeNav(row: NavImageItem) {
    await ElMessageBox.confirm("确定删除这张导航图片吗？", "删除图片", { type: "warning" });
    await deleteNavImage(row.id);
    ElMessage.success("已删除");
    loadNavImages();
}

// 异常日志
async function viewException(row: ExceptionLogItem) {
    exDetail.value = await getExceptionLog(row.id);
    exDrawer.value = true;
}

async function resolveException(row: ExceptionLogItem) {
    const res = await ElMessageBox.prompt("写下处理结论，便于以后查证", "标记已处理", {
        inputType: "textarea",
        inputPlaceholder: "如：已定位为附件超限，已调整限制并通知用户重试",
        inputValidator: (v: string) => (v && v.trim().length >= 2 ? true : "请至少写 2 个字"),
    });
    await resolveExceptionLog(row.id, res.value);
    ElMessage.success("已标记为已处理");
    loadExLogs();
}

function openManualException() {
    Object.keys(manualForm).forEach((k) => delete (manualForm as any)[k]);
    manualDialog.value = true;
}

async function submitManualException() {
    if (!manualForm.errorMsg) {
        ElMessage.warning("请填写异常信息");
        return;
    }
    saving.value = true;
    try {
        await createExceptionLog(manualForm);
        ElMessage.success("已登记");
        manualDialog.value = false;
        loadExLogs();
    } finally {
        saving.value = false;
    }
}

// 通知模板
function openTemplateDialog(row?: NotificationTemplateItem) {
    Object.keys(templateForm).forEach((k) => delete (templateForm as any)[k]);
    Object.assign(templateForm, row ? { ...row } : { status: 1, channels: "inner" });
    templateDialog.value = true;
}

async function submitTemplate() {
    if (!templateForm.templateCode || !templateForm.templateName || !templateForm.titleTemplate) {
        ElMessage.warning("模板编码、模板名、标题模板都要填");
        return;
    }
    saving.value = true;
    try {
        if (templateForm.id) await updateNotificationTemplate(templateForm.id, templateForm);
        else await createNotificationTemplate(templateForm);
        ElMessage.success("已保存");
        templateDialog.value = false;
        loadTemplates();
    } finally {
        saving.value = false;
    }
}

async function removeTemplate(row: NotificationTemplateItem) {
    await ElMessageBox.confirm("确定删除模板「" + row.templateName + "」吗？", "删除模板", { type: "warning" });
    await deleteNotificationTemplate(row.id);
    ElMessage.success("已删除");
    loadTemplates();
}

// 推送记录
async function resendPush(row: PushLogItem) {
    await resendPushLog(row.id);
    ElMessage.success("已重置为待推送");
    loadPushLogs();
}

onMounted(loadParams);
</script>

<style scoped>
.manage-page { padding: 4px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.head-left { display: flex; align-items: center; gap: 10px; }
.head-bar { width: 4px; height: 34px; border-radius: 2px; background: linear-gradient(180deg, #3b82f6, #6366f1); }
.page-head h2 { margin: 0; font-size: 20px; color: #1f2d3d; }
.page-head p { margin: 4px 0 0; font-size: 13px; color: #8a94a6; }
.manage-tabs :deep(.el-tabs__content) { overflow: visible; }
.tab-toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.toolbar-tip { font-size: 12px; color: #909399; }
.code-text { font-family: Consolas, Monaco, monospace; color: #475569; }
.expand-box { padding: 6px 16px; font-size: 12px; color: #5b6472; line-height: 1.7; word-break: break-all; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.stack-box { margin-top: 14px; }
.stack-title { font-size: 13px; font-weight: 600; color: #1f2d3d; margin-bottom: 6px; }
.stack-text { max-height: 320px; overflow: auto; background: #0f172a; color: #cbd5e1; font-size: 12px; line-height: 1.6; padding: 12px; border-radius: 6px; white-space: pre-wrap; word-break: break-all; }
</style>
