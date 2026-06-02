@echo off
chcp 65001 >nul
REM ============================================
REM  DMA — Windows 可执行程序打包脚本
REM  使用 JDK 17 内置 jpackage 工具
REM  输出: dist\DMA\DMA.exe (自包含，无需安装 JDK)
REM ============================================

set JAVA_HOME=D:\DEV_Application\Java\jdk17
set PATH=%JAVA_HOME%\bin;%PATH%

echo.
echo ╔══════════════════════════════════════════╗
echo ║   DMA - Database Migration Assistant    ║
echo ║   Windows .exe 打包工具                  ║
echo ╚══════════════════════════════════════════╝
echo.
echo JDK: %JAVA_HOME%
echo.

REM ── Step 1: 编译安装项目 ──
echo [1/4] 编译安装项目模块...
call mvn clean install -q -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo X 编译失败！
    pause
    exit /b 1
)
echo OK 编译安装成功
echo.

REM ── Step 2: 打包 + 收集依赖 ──
echo [2/4] 打包 dma-desktop（thin JAR + 依赖）...
call mvn package -q -DskipTests -pl dma-desktop
if %ERRORLEVEL% NEQ 0 (
    echo X 打包失败！
    pause
    exit /b 1
)
echo OK 打包完成
echo.

REM ── Step 3: 准备 staging 目录 ──
echo [3/4] 准备 JAR 文件（thin JAR + 所有依赖）...
set STAGE=target\stage
if exist "%STAGE%" rmdir /s /q "%STAGE%"
mkdir "%STAGE%"

REM 复制 thin JAR（含 DmaLauncher 在根目录）
copy dma-desktop\target\dma-desktop-1.0.0-SNAPSHOT.jar "%STAGE%\" >nul

REM 复制所有运行时依赖
copy dma-desktop\target\libs\*.jar "%STAGE%\" >nul

REM 也复制内部模块 JAR（dma-core, dma-common）—— 它们在依赖目录中
echo    共 %STAGE% 下有文件数:
dir "%STAGE%"\*.jar 2>nul | find ".jar" /c
echo OK 就绪
echo.

REM ── Step 4: jpackage 生成原生应用 ──
echo [4/4] 生成 Windows 可执行程序 (jpackage)...
echo       这可能需要 1-3 分钟...

REM 清理旧输出
if exist "dist\DMA" rmdir /s /q "dist\DMA"
if not exist "dist" mkdir dist

jpackage ^
  --name "DMA" ^
  --type app-image ^
  --input "%STAGE%" ^
  --main-jar "dma-desktop-1.0.0-SNAPSHOT.jar" ^
  --main-class com.dma.desktop.DmaLauncher ^
  --dest dist ^
  --vendor "DMA Team" ^
  --app-version 1.0.0 ^
  --copyright "2024 DMA Team" ^
  --description "Database Migration Assistant - 数据库迁移兼容性分析工具" ^
  --java-options "-Dprism.order=sw" ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --java-options "-Dspring.devtools.restart.enabled=false" ^
  --java-options "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED" ^
  --java-options "--add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED" ^
  --java-options "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED"

if %ERRORLEVEL% NEQ 0 (
    echo X jpackage 生成失败！
    pause
    exit /b 1
)

echo.
echo ============================================
echo  打包完成！
echo ============================================
echo.
echo 输出目录: %CD%\dist\DMA\
echo 可执行文件: dist\DMA\DMA.exe
echo 大小:
dir dist\DMA /s 2>nul | find "File(s)"
echo.
echo 使用方式:
echo   1. 双击 dist\DMA\DMA.exe 直接运行
echo   2. 将 dist\DMA\ 目录打包为 ZIP 分发给他人
echo   3. 用户无需安装 Java，解压即用！
echo.
pause
