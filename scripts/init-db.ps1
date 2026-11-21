<#
.SYNOPSIS
  一键初始化 OA 数据库：建库 + 导入基线结构与演示数据。

.DESCRIPTION
  sql-init-20261024.sql 是完整基线（59 张表 + 12 个演示账号 + 菜单权限 + 流程定义）。
  sql-migration-*.sql 是历次增量升级脚本，已包含在基线里，只在升级旧库时才需要按顺序执行。

.PARAMETER Force
  目标库已存在时直接删除重建，不再询问。

.EXAMPLE
  .\scripts\init-db.ps1 -Password 861202
  .\scripts\init-db.ps1 -Database oa_system -Force
#>
param(
    [string]$User = "root",
    [string]$Password = "861202",
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$Database = "oa_system",
    [string]$MySqlExe = "",
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$initSql = Join-Path $root "sql-init-20261024.sql"
if (-not (Test-Path $initSql)) {
    throw "找不到基线脚本：$initSql"
}

if (-not $MySqlExe) {
    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "C:\xampp\mysql\bin\mysql.exe"
    )
    $MySqlExe = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $MySqlExe) {
        $cmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
        if ($cmd) { $MySqlExe = $cmd.Source }
    }
}
if (-not $MySqlExe -or -not (Test-Path $MySqlExe)) {
    throw "找不到 mysql.exe，请用 -MySqlExe 指定路径"
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

# 密码写进临时配置文件，避免命令行告警，也避免密码出现在进程列表
$cnf = Join-Path ([IO.Path]::GetTempPath()) ("oa-db-" + [guid]::NewGuid().ToString("N") + ".cnf")
$cnfLines = @("[client]", "user=$User", "password=$Password", "host=$HostName", "port=$Port", "default-character-set=utf8mb4")
Set-Content -Path $cnf -Value $cnfLines -Encoding ASCII

# 统一的执行封装：SqlFile 从 stdin 灌进去，返回退出码
function Invoke-MySql {
    param([string]$Db, [string]$SqlFile, [string]$OutFile)
    $argList = @("--defaults-extra-file=$cnf", "--default-character-set=utf8mb4")
    if ($Db) { $argList += $Db }
    if (-not $OutFile) { $OutFile = [IO.Path]::GetTempFileName() }
    $errFile = [IO.Path]::GetTempFileName()
    $p = Start-Process -FilePath $MySqlExe -ArgumentList $argList -NoNewWindow -Wait -PassThru -RedirectStandardInput $SqlFile -RedirectStandardOutput $OutFile -RedirectStandardError $errFile
    $err = Get-Content $errFile -ErrorAction SilentlyContinue | Where-Object { $_ -notmatch "Using a password" }
    if ($err) { Write-Host ($err -join " | ") -ForegroundColor DarkYellow }
    return $p.ExitCode
}

# 临时 SQL 一律 UTF-8 无 BOM，否则 mysql 客户端会把首行当乱码
function Write-TempSql([string]$content) {
    $file = Join-Path ([IO.Path]::GetTempPath()) ("oa-" + [guid]::NewGuid().ToString("N") + ".sql")
    [IO.File]::WriteAllText($file, $content, $utf8NoBom)
    return $file
}

try {
    $checkFile = Write-TempSql "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$Database';"
    $checkOut = [IO.Path]::GetTempFileName()
    Invoke-MySql -SqlFile $checkFile -OutFile $checkOut | Out-Null
    $hits = @(Get-Content $checkOut -ErrorAction SilentlyContinue | Where-Object { $_.Trim() -eq $Database })
    $existed = $hits.Count -gt 0

    if ($existed -and -not $Force) {
        $answer = Read-Host "数据库 $Database 已存在，继续会删除并重建，是否继续？(y/N)"
        if ($answer -ne "y") { Write-Host "已取消"; return }
    }

    Write-Host "创建数据库 $Database ..." -ForegroundColor Cyan
    $createFile = Write-TempSql "DROP DATABASE IF EXISTS $Database; CREATE DATABASE $Database DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    if ((Invoke-MySql -SqlFile $createFile) -ne 0) { throw "建库失败，请检查账号、密码与端口" }

    Write-Host "导入基线结构与演示数据（约 320KB）..." -ForegroundColor Cyan
    if ((Invoke-MySql -Db $Database -SqlFile $initSql) -ne 0) { throw "导入基线数据失败" }

    $countFile = Write-TempSql "SELECT CONCAT('users=', COUNT(*)) FROM users; SELECT CONCAT('roles=', COUNT(*)) FROM roles; SELECT CONCAT('menus=', COUNT(*)) FROM menus; SELECT CONCAT('flow_definitions=', COUNT(*)) FROM flow_definitions;"
    $countOut = [IO.Path]::GetTempFileName()
    Invoke-MySql -Db $Database -SqlFile $countFile -OutFile $countOut | Out-Null
    Get-Content $countOut
    Write-Host "初始化完成：$Database（演示账号 admin / 123456，其余见 README）" -ForegroundColor Green
}
finally {
    Remove-Item $cnf -Force -ErrorAction SilentlyContinue
}
