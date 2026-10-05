@echo off
rem One-click start. Needs Rancher Desktop (dockerd engine), Java 25 and Maven on PATH.
cd /d "%~dp0"
echo Building the application jar...
call mvn -B -q -DskipTests package
if errorlevel 1 goto :failed
echo Starting the stack...
docker compose up --build -d --wait
if errorlevel 1 goto :failed
echo.
echo Started. Swagger UI: http://localhost:8080/swagger-ui.html
start "" http://localhost:8080/swagger-ui.html
pause
exit /b 0

:failed
echo.
echo Start failed. Check: Rancher Desktop is running with the dockerd engine, and java/mvn work.
pause
exit /b 1
