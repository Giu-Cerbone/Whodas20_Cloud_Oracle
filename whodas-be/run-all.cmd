@echo off
setlocal EnableExtensions

set "KEYCLOAK_BASE_URL=http://localhost:8180"
set "KEYCLOAK_REALM=whodas-realm"
set "KEYCLOAK_CLIENT_ID=whodas20-client"
set "KEYCLOAK_CLIENT_SECRET=whodas20-client-key"

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%"

if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-21\bin\java.exe" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
)
if "%JAVA_BIN%"=="" (
    if defined JAVA_HOME (set "JAVA_BIN=%JAVA_HOME%\bin\java.exe") else set "JAVA_BIN=java.exe"
)
if not defined DB_HOST     set "DB_HOST=localhost"
if not defined DB_PORT     set "DB_PORT=5432"
if not defined DB_NAME     set "DB_NAME=whodas"
if not defined DB_USERNAME set "DB_USERNAME=postgres"
if not defined DB_PASSWORD set "DB_PASSWORD=postgres"

if not exist "%PROJECT_DIR%logs" mkdir "%PROJECT_DIR%logs"

echo WHODAS backend startup
echo Project: %PROJECT_DIR%
echo Java: %JAVA_BIN%
echo Database: jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%
echo Database user: %DB_USERNAME%

start "WHODAS Patient Service" /D "%PROJECT_DIR%whodas-patient-service" "%JAVA_BIN%" -jar "%PROJECT_DIR%whodas-patient-service\target\whodas-patient-service-1.0.0.jar" --server.port=7002
start "WHODAS Questionnaire Service" /D "%PROJECT_DIR%whodas-questionnaire-service" "%JAVA_BIN%" -jar "%PROJECT_DIR%whodas-questionnaire-service\target\whodas-questionnaire-service-1.0.0.jar" --server.port=7003
start "WHODAS Assessment Service" /D "%PROJECT_DIR%whodas-assessment-service" "%JAVA_BIN%" -jar "%PROJECT_DIR%whodas-assessment-service\target\whodas-assessment-service-1.0.0.jar" --server.port=7004
start "WHODAS Certificate Service" /D "%PROJECT_DIR%whodas-certificate-service" "%JAVA_BIN%" -jar "%PROJECT_DIR%whodas-certificate-service\target\whodas-certificate-service-1.0.0.jar" --server.port=7005
start "WHODAS Document Service" /D "%PROJECT_DIR%whodas-document-service" "%JAVA_BIN%" -jar "%PROJECT_DIR%whodas-document-service\target\whodas-document-service-1.0.0.jar" --server.port=7006
start "WHODAS Notification Service" /D "%PROJECT_DIR%whodas-notification-service" /B "%JAVA_BIN%" -jar "%PROJECT_DIR%whodas-notification-service\target\whodas-notification-service-1.0.0.jar" --server.port=7007
start "WHODAS User Service" /D "%PROJECT_DIR%whodas-user-service" "%JAVA_BIN%" -jar "%PROJECT_DIR%whodas-user-service\target\whodas-user-service-1.0.0.jar" --server.port=7008
timeout /t 5 /nobreak >nul
start "WHODAS Gateway" /D "%PROJECT_DIR%whodas-gateway" "%JAVA_BIN%" -jar "%PROJECT_DIR%whodas-gateway\target\whodas-gateway-1.0.0.jar" --server.port=7001

echo Backend services started
echo Patient: http://localhost:7002
echo Questionnaire: http://localhost:7003
echo Assessment: http://localhost:7004
echo Certificate: http://localhost:7005
echo Document: http://localhost:7006
echo Notification: http://localhost:7007
echo User: http://localhost:7008
echo Gateway: http://localhost:7001
