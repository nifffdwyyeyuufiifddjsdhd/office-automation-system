<#
数据库不变量自检：24 条跨表一致性规则，任何一条 COUNT > 0 就说明业务数据出现了自相矛盾的状态。
用法示例（只读，不会改任何数据）：
  .\scripts\check-db-invariants.ps1
  .\scripts\check-db-invariants.ps1 -Database oa_system -Password 861202
退出码：0 全部通过；1 至少一条有违例（可直接接进 CI 或部署前检查）；2 连不上数据库。
#>
param(
    [string]$Server = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root',
    [string]$Password = '861202',
    [string]$Database = 'oa_system',
    [string]$MySqlExe = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
)

if (-not (Test-Path $MySqlExe)) {
    Write-Host ('找不到 mysql 客户端：' + $MySqlExe + '（可用 -MySqlExe 指定）') -ForegroundColor Red
    exit 2
}

# 密码写在命令行会被 stderr 刷警告，改成临时配置文件，跑完删除
$cnf = Join-Path $env:TEMP ('oa-invariants-' + [guid]::NewGuid().ToString('N') + '.cnf')
$lines = @('[client]', ('user=' + $User), ('password=' + $Password), 'default-character-set=utf8mb4')
Set-Content -Path $cnf -Value $lines -Encoding ascii

function SqlOne([string]$sql) {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $args = @(('--defaults-extra-file=' + $cnf), ('-h' + $Server), ('-P' + $Port), ('-D' + $Database), '-N', '-B', '-e', $sql)
        $out = & $MySqlExe $args 2>$null
        return ($out | Where-Object { $_ -ne '' } | Select-Object -First 1)
    } finally {
        $ErrorActionPreference = $prev
    }
}

if (-not (SqlOne 'SELECT 1')) {
    Remove-Item $cnf -Force -ErrorAction SilentlyContinue
    Write-Host ('连不上数据库 ' + $Database + '@' + $Server + ':' + $Port + '，请检查参数或 MySQL 服务') -ForegroundColor Red
    exit 2
}

# 每条规则：标签、含义、违例计数 SQL
$checks = @(
    @('task_overdue_flag_mismatch', '到期未完成却没被打逾期标记', "SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL AND status NOT IN (4,5,6) AND plan_end_time IS NOT NULL AND plan_end_time < NOW() AND (is_overdue IS NULL OR is_overdue=0)"),
    @('task_overdue_stale', '逾期标记还在但已不成立', "SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL AND is_overdue=1 AND (plan_end_time IS NULL OR plan_end_time >= NOW() OR status IN (4,5,6))"),
    @('task_running_no_assignee', '进行中的任务没有负责人', "SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL AND status IN (1,2,3) AND (assignee_id IS NULL OR assignee_id=0)"),
    @('task_assignee_inactive', '未结任务挂在停用账号上', "SELECT COUNT(*) FROM tasks t JOIN users u ON u.id=t.assignee_id WHERE t.deleted_at IS NULL AND u.status<>1 AND t.status NOT IN (4,5,6)"),
    @('flow_running_no_auditor', '审批中的流程没有当前节点', "SELECT COUNT(*) FROM flow_instances WHERE deleted_at IS NULL AND status=1 AND (current_step_id IS NULL OR current_step_id=0)"),
    @('flow_done_no_finish_time', '已完成的流程没有完成时间', "SELECT COUNT(*) FROM flow_instances WHERE deleted_at IS NULL AND status=2 AND finish_time IS NULL"),
    @('flow_running_auditor_inactive', '审批中的流程停在停用账号', "SELECT COUNT(*) FROM flow_instances f JOIN users u ON u.id=f.current_auditor_id WHERE f.deleted_at IS NULL AND f.status=1 AND f.current_auditor_id IS NOT NULL AND u.status<>1"),
    @('flow_approval_orphan', '审批记录找不到所属流程', "SELECT COUNT(*) FROM flow_approvals a LEFT JOIN flow_instances f ON f.id=a.instance_id AND f.deleted_at IS NULL WHERE a.deleted_at IS NULL AND f.id IS NULL"),
    @('payment_orphan', '付款单找不到所属流程', "SELECT COUNT(*) FROM payment_records p LEFT JOIN flow_instances f ON f.id=p.instance_id AND f.deleted_at IS NULL WHERE p.deleted_at IS NULL AND f.id IS NULL"),
    @('payment_paid_no_pay_time', '已付款却没有付款时间', "SELECT COUNT(*) FROM payment_records WHERE deleted_at IS NULL AND status=1 AND pay_time IS NULL"),
    @('project_done_with_open_tasks', '项目已结项还有未完成任务', "SELECT COUNT(*) FROM projects p WHERE p.deleted_at IS NULL AND p.status=3 AND EXISTS (SELECT 1 FROM tasks t WHERE t.project_id=p.id AND t.deleted_at IS NULL AND t.status NOT IN (4,5,6))"),
    @('project_progress_mismatch', '项目进度与任务进度算不出一样', "SELECT COUNT(*) FROM projects p WHERE p.deleted_at IS NULL AND p.status<>3 AND p.progress <> (SELECT CASE WHEN COUNT(*)=0 THEN CASE WHEN SUM(CASE WHEN t2.status=4 THEN 1 ELSE 0 END)>0 THEN 100 ELSE 0 END ELSE LEAST(100, ROUND(AVG(t2.progress))) END FROM tasks t2 WHERE t2.project_id=p.id AND t2.deleted_at IS NULL AND t2.archive_status=0 AND t2.status NOT IN (4,5,6))"),
    @('project_manager_inactive', '项目经理是停用账号', "SELECT COUNT(*) FROM projects p JOIN users u ON u.id=p.manager_id WHERE p.deleted_at IS NULL AND u.status<>1"),
    @('project_member_inactive', '项目成员是停用账号', "SELECT COUNT(*) FROM project_members m JOIN users u ON u.id=m.user_id WHERE m.deleted_at IS NULL AND u.status<>1"),
    @('task_access_for_inactive', '停用账号还留着任务中心权限', "SELECT COUNT(*) FROM user_permissions up JOIN users u ON u.id=up.user_id JOIN menus m ON m.id=up.menu_id WHERE up.deleted_at IS NULL AND u.status<>1 AND m.menu_code='task:access'"),
    @('menu_code_dup', '菜单权限码重复', "SELECT COUNT(*) FROM (SELECT menu_code FROM menus WHERE deleted_at IS NULL AND menu_code IS NOT NULL AND menu_code<>'' GROUP BY menu_code HAVING COUNT(*)>1) x"),
    @('nav_menu_without_perm', '导航菜单没有权限码', "SELECT COUNT(*) FROM menus WHERE deleted_at IS NULL AND menu_type=2 AND is_nav=1 AND (menu_code IS NULL OR menu_code='')"),
    @('role_no_permission', '启用中的角色一个权限都没有', "SELECT COUNT(*) FROM roles r WHERE r.deleted_at IS NULL AND r.status=1 AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.deleted_at IS NULL)"),
    @('makeup_final_missing_approver', '补签终审没有审批人', "SELECT COUNT(*) FROM attendance_makeup WHERE deleted_at IS NULL AND status=1 AND (approver_id IS NULL OR approver_id=0)"),
    @('makeup_stage2_no_first', '补签进入二审却没有初审人', "SELECT COUNT(*) FROM attendance_makeup WHERE deleted_at IS NULL AND status=0 AND current_stage=2 AND (first_approver_id IS NULL OR first_approver_id=0)"),
    @('appeal_approved_no_handler', '申诉已处理却没有处理人', "SELECT COUNT(*) FROM attendance_appeals WHERE deleted_at IS NULL AND status<>0 AND (approver_id IS NULL OR approver_id=0)"),
    @('notify_biz_flow_dead', '通知指向已不存在的流程', "SELECT COUNT(*) FROM notification_records n WHERE n.deleted_at IS NULL AND n.biz_type='flow_instance' AND n.biz_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM flow_instances f WHERE f.id=n.biz_id AND f.deleted_at IS NULL)"),
    @('notify_biz_task_dead', '通知指向已不存在的任务', "SELECT COUNT(*) FROM notification_records n WHERE n.deleted_at IS NULL AND n.biz_type='task' AND n.biz_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.id=n.biz_id AND t.deleted_at IS NULL)"),
    @('worklog_supplement_expired_pending', '补写申请已过期还挂在待审', "SELECT COUNT(*) FROM worklog_supplement_approvals WHERE status=0 AND expire_at IS NOT NULL AND expire_at < NOW()")
)

Write-Host ''
Write-Host ('数据库：' + $Database + '@' + $Server + ':' + $Port + '  规则数：' + $checks.Count) -ForegroundColor Cyan
Write-Host ('{0,-40} {1,-30} {2,6}' -f '规则', '含义', '违例')

$bad = 0
foreach ($c in $checks) {
    $n = SqlOne $c[2]
    if ($null -eq $n -or $n -eq '') { $n = '?' }
    $num = 0
    [void][int]::TryParse([string]$n, [ref]$num)
    if ($num -gt 0) { $bad++ }
    if ($num -gt 0) {
        Write-Host ('{0,-40} {1,-30} {2,6}' -f $c[0], $c[1], [string]$num) -ForegroundColor Red
    } else {
        Write-Host ('{0,-40} {1,-30} {2,6}' -f $c[0], $c[1], '0 OK') -ForegroundColor DarkGray
    }
}

Remove-Item $cnf -Force -ErrorAction SilentlyContinue
Write-Host ''
if ($bad -gt 0) {
    Write-Host ('有 ' + $bad + ' 条规则存在违例，请先修数据再交付') -ForegroundColor Red
    exit 1
}
Write-Host '全部规则通过：没有发现自相矛盾的业务数据' -ForegroundColor Green
exit 0
