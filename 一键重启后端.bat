@echo off
set "BASE=D:\VSC\Employment Project\office-automation-system"
set "API=%BASE%\OfficeAutomationSystemAPI"
set "JAVA_HOME=C:\Program Files\Java\jdk-17"

echo [1/4] 停掉占用 4040 端口的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":4040" ^| findstr "LISTENING"') do (
    echo     kill pid=%%a
    taskkill /f /pid %%a >nul 2>&1
)
call :sleep 2

echo [2/4] 清空 target\classes，避免半截 class 文件导致 ClassFormatError...
if exist "%API%\target\classes" rmdir /s /q "%API%\target\classes"
echo     已清空

echo [3/4] 全量编译中，请稍等...
cd /d "%API%"
call .\mvnw.cmd -o -DskipTests clean compile
if errorlevel 1 (
    echo.
    echo 编译失败，后端没有启动，请把上面的报错发给鱼。
    pause
    exit /b 1
)

echo [4/4] 启动后端，日志在 .backup\backend-run-20261019d.log ...
start "" /d "%BASE%" cmd /c ".backup\run-backend.cmd"

for /l %%i in (1,1,40) do (
    call :sleep 3
    netstat -ano | findstr ":4040" | findstr "LISTENING" >nul 2>&1
    if not errorlevel 1 goto :up
)
echo 等待超时，端口 4040 没起来，看日志：%BASE%\.backup\backend-run-20261019d.log
pause
exit /b 1

:up
echo.
echo 后端已启动：http://localhost:4040
echo 本窗口可以关闭，不影响后端运行。
call :sleep 2
exit /b 0

:sleep
ping -n %1 127.0.0.1 >nul 2>&1
exit /b 0