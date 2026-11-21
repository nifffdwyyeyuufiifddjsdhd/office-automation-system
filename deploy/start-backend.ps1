<#
.SYNOPSIS
  生产环境启动后端（Windows）。读取 deploy/.env.prod 里的键值写入环境变量，再用 java -jar 启动。

.EXAMPLE
  .\deploy\start-backend.ps1
  .\deploy\start-backend.ps1 -EnvFile .\deploy\.env.prod -Xmx 2g
#>
param(
    [string]$EnvFile = "",
    [string]$Xmx = "2g",
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"
$deployDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $deployDir
$api = Join-Path $root "OfficeAutomationSystemAPI"

if (-not $EnvFile) { $EnvFile = Join-Path $deployDir ".env.prod" }
if (-not (Test-Path $EnvFile)) {
    throw "找不到环境变量文件：$EnvFile（先复制 deploy/env.prod.example 改名成 .env.prod 再填值）"
}

# 逐行 KEY=VALUE，忽略空行和 # 注释
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -le 0) { return }
    $key = $line.Substring(0, $idx).Trim()
    $val = $line.Substring($idx + 1).Trim()
    [Environment]::SetEnvironmentVariable($key, $val, "Process")
    Write-Host ("  " + $key + " = " + $val)
}

if ($Rebuild) {
    Write-Host "编译打包中（跳过测试）..." -ForegroundColor Cyan
    Push-Location $api
    try {
        & ".\mvnw.cmd" -o -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw "打包失败" }
    }
    finally { Pop-Location }
}

$jar = Get-ChildItem (Join-Path $api "target") -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*.original" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $jar) {
    throw "target 下没有 jar，先用 -Rebuild 或手动执行 .\mvnw.cmd -DskipTests package"
}

Write-Host ("启动：" + $jar.FullName) -ForegroundColor Green
$java = "java"
if ($env:JAVA_HOME) { $java = Join-Path $env:JAVA_HOME "bin\java.exe" }
& $java "-Xmx$Xmx" -jar $jar.FullName
