@echo off
chcp 65001 >nul
echo ========================================
echo TeamUp 项目服务启动脚本
echo ========================================
echo.

echo [1/4] 检查 Redis 服务...
redis-cli ping >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Redis 未运行，尝试启动...
    net start Redis >nul 2>&1
    if %errorlevel% neq 0 (
        echo ⚠️  无法启动 Redis 服务
        echo    请手动启动 Redis 或安装 Redis
        echo    下载地址: https://github.com/microsoftarchive/redis/releases
        pause
        exit /b 1
    )
    echo ✅ Redis 服务已启动
) else (
    echo ✅ Redis 已在运行
)
echo.

echo [2/4] 启动后端服务...
start "TeamUp Backend" cmd /k "cd backend\teamup-server && mvn spring-boot:run"
echo ✅ 后端服务启动中... (端口 8080)
echo.

echo [3/4] 启动前端服务...
start "TeamUp Frontend" cmd /k "cd frontend && npm run dev"
echo ✅ 前端服务启动中... (端口 3000)
echo.

echo [4/4] 启动匹配服务...
start "TeamUp Matching Service" cmd /k "cd matching-service && python main.py"
echo ✅ 匹配服务启动中...
echo.

echo ========================================
echo 所有服务启动完成！
echo ========================================
echo.
echo 服务地址:
echo   前端: http://localhost:3000/
echo   后端: http://localhost:8080/api/
echo   SocketIO: http://localhost:9092/
echo.
echo 按任意键关闭此窗口...
pause >nul
