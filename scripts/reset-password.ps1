<#
.SYNOPSIS
  重置某个账号的登录密码（运维兜底用）。走管理员接口 PUT /api/users/{id}/reset-password，密码用 BCrypt 存库。

.DESCRIPTION
  不需要知道任何人的密码：脚本用 OA_JWT_SECRET 现场签一个管理员 token 调接口。
  仅限本机/内网应急使用，密钥泄露等于管理员权限泄露，用完记得改密钥。

.PARAMETER Username
  要重置的账号。

.PARAMETER NewPassword
  新密码，至少 8 位。

.EXAMPLE
  .\scripts\reset-password.ps1 -Username admin -NewPassword 'Oa@2026#Init'
#>
param(
    [Parameter(Mandatory = $true)][string]$Username,
    [Parameter(Mandatory = $true)][string]$NewPassword,
    [string]$BaseUrl = "http://localhost:4040",
    [string]$Secret = ""
)

$ErrorActionPreference = "Stop"
if (-not $Secret) {
    $Secret = $env:OA_JWT_SECRET
    if (-not $Secret) { $Secret = "jingtian2026nian9yue18haoxingqiwuqing" }
}
if ($NewPassword.Length -lt 8) { throw "新密码至少 8 位" }

function ConvertTo-Base64Url([byte[]]$bytes) {
    return [Convert]::ToBase64String($bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function New-AdminToken([string]$secret, [int]$seconds = 600) {
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $header = @{ alg = "HS256"; typ = "JWT" } | ConvertTo-Json -Compress
    $payload = @{ sub = "1"; username = "admin"; iat = $now; exp = $now + $seconds } | ConvertTo-Json -Compress
    $h = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($header))
    $p = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payload))
    $data = "$h.$p"
    $hmac = New-Object System.Security.Cryptography.HMACSHA256 -ArgumentList (,[Text.Encoding]::UTF8.GetBytes($secret))
    $sig = ConvertTo-Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($data)))
    return "$data.$sig"
}

function Invoke-Api([string]$method, [string]$path, $body, [string]$token) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{ Uri = ($BaseUrl + $path); Method = $method; Headers = $headers; UseBasicParsing = $true; TimeoutSec = 30 }
    if ($body) {
        $params["ContentType"] = "application/json; charset=utf-8"
        $params["Body"] = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Depth 6 -Compress))
    }
    try {
        $resp = Invoke-WebRequest @params
        return ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json)
    }
    catch {
        $msg = $_.ErrorDetails.Message
        if ($msg) { return ($msg | ConvertFrom-Json) }
        throw
    }
}

$token = New-AdminToken $Secret
$found = Invoke-Api GET ("/api/users/page?pageNum=1&pageSize=20&keyword=" + [uri]::EscapeDataString($Username)) $null $token
if ($found.code -ne 200) { throw ("查询账号失败：" + $found.message) }
$user = @($found.data.records) | Where-Object { $_.username -eq $Username } | Select-Object -First 1
if (-not $user) { throw "找不到账号：$Username" }

$reset = Invoke-Api PUT ("/api/users/" + $user.id + "/reset-password") @{ newPassword = $NewPassword } $token
if ($reset.code -ne 200) { throw ("重置失败：" + $reset.message) }

$check = Invoke-Api POST "/api/auth/login" @{ username = $Username; password = $NewPassword } $null
Write-Host ("账号 " + $Username + "（id=" + $user.id + "）密码已重置，登录验证 code=" + $check.code) -ForegroundColor Green
