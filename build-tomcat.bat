@echo off
chcp 65001 >nul
REM ==============================================================================
REM Apache Tomcat 9.0.90 一键构建脚本 (Windows)
REM 用法: build-tomcat.bat [选项]
REM ==============================================================================

setlocal EnableDelayedExpansion

set "TOMCAT_VERSION=9.0.90"
set "PROJECT_DIR=%~dp0"
set "BUILD_DIR=%PROJECT_DIR%output\build"

REM 默认选项
set "CLEAN_BUILD=false"
set "RUN_TESTS=false"

REM 显示帮助
if "%~1"=="-h" goto :show_help
if "%~1"=="--help" goto :show_help
if "%~1"=="/h" goto :show_help
if "%~1"=="/help" goto :show_help
goto :parse_args

:show_help
echo ===============================================================================
echo   Apache Tomcat %TOMCAT_VERSION% 一键构建脚本
echo ===============================================================================
echo.
echo 用法: %~nx0 [选项]
echo.
echo 选项:
echo     -c, --clean         清理之前的构建输出
echo     -t, --test          构建完成后运行测试
echo     -h, --help          显示此帮助信息
echo.
echo 示例:
echo     %~nx0               标准构建
echo     %~nx0 -c            清理并构建
echo     %~nx0 -c -t         清理、构建并运行测试
echo.
goto :eof

:parse_args
if "%~1"=="" goto :main
if "%~1"=="-c" set "CLEAN_BUILD=true" & shift & goto :parse_args
if "%~1"=="--clean" set "CLEAN_BUILD=true" & shift & goto :parse_args
if "%~1"=="-t" set "RUN_TESTS=true" & shift & goto :parse_args
if "%~1"=="--test" set "RUN_TESTS=true" & shift & goto :parse_args
echo [ERROR] 未知选项: %~1
goto :show_help

:main
echo ===============================================================================
echo   Apache Tomcat %TOMCAT_VERSION% 一键构建脚本
echo ===============================================================================
echo.

REM 检查环境
echo [INFO] 检查构建环境...

java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Java，请安装 JDK 11 或更高版本
    exit /b 1
)

ant -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Ant，请安装 Apache Ant 1.10.x 或更高版本
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VERSION=%%g"
    set "JAVA_VERSION=!JAVA_VERSION:"=!"
)
echo [INFO] Java 版本: %JAVA_VERSION%

echo [INFO] 环境检查通过
echo.

REM 检查/创建 build.properties
if not exist "%PROJECT_DIR%build.properties" (
    echo [INFO] 创建 build.properties...
    (
        echo # Apache Tomcat 构建配置
        echo base.path=C:/tomcat-build-libs
        echo skip.installer=true
        echo do.codesigning=false
    ) > "%PROJECT_DIR%build.properties"
    echo [SUCCESS] 已创建 build.properties
)

REM 清理（如果需要）
if "%CLEAN_BUILD%"=="true" (
    echo [INFO] 清理构建输出...
    cd /d "%PROJECT_DIR%"
    call ant clean >nul 2>&1
    if errorlevel 1 (
        echo [WARNING] 清理过程中出现问题，继续构建...
    ) else (
        echo [SUCCESS] 清理完成
    )
)

REM 执行构建
echo [INFO] 开始构建 Apache Tomcat %TOMCAT_VERSION%...
echo [INFO] 这可能需要几分钟时间，请耐心等待...
echo.

cd /d "%PROJECT_DIR%"
call ant

if errorlevel 1 (
    echo [ERROR] 构建失败！
    exit /b 1
)

echo.
echo [SUCCESS] 构建成功！

REM 验证构建结果
if not exist "%BUILD_DIR%" (
    echo [ERROR] 构建输出目录不存在
    exit /b 1
)

if not exist "%BUILD_DIR%\bin\catalina.bat" (
    echo [ERROR] 关键文件缺失
    exit /b 1
)

echo [INFO] 构建验证通过
echo.

REM 运行测试（如果需要）
if "%RUN_TESTS%"=="true" (
    echo [INFO] 运行单元测试...
    call ant test
    echo [INFO] 测试完成
)

REM 显示使用说明
echo ===============================================================================
echo   [SUCCESS] Apache Tomcat %TOMCAT_VERSION% 构建完成！
echo ===============================================================================
echo.
echo 📂 构建输出位置:
echo    %BUILD_DIR%
echo.
echo 🚀 启动 Tomcat:
echo    cd %BUILD_DIR%
echo    bin\startup.bat
echo.
echo 📝 前台运行（查看日志）:
echo    cd %BUILD_DIR%
echo    bin\catalina.bat run
echo.
echo 🛑 停止 Tomcat:
echo    cd %BUILD_DIR%
echo    bin\shutdown.bat
echo.
echo ===============================================================================

:eof
endlocal
