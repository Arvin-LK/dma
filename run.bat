@echo off
chcp 65001 >nul
REM ============================================
REM  DMA Database Migration Assistant 启动脚本
REM ============================================

set JAVA_HOME=D:\DEV_Application\Java\jdk17
set PATH=%JAVA_HOME%\bin;%PATH%

echo.
echo ========================================
echo   DMA - Database Migration Assistant
echo   MVP v1.0.0
echo ========================================
echo.
echo JDK: %JAVA_HOME%
echo.

echo [1/2] 安装依赖并编译...
call mvn install -q -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)
echo 编译成功！

echo.
echo [2/2] 启动桌面端应用...
echo 窗口正在打开，首次启动约需 10 秒，请稍候...
echo.
call mvn javafx:run -pl dma-desktop

pause
