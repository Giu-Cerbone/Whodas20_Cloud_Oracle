@echo off
setlocal EnableExtensions

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

echo WHODAS backend startup (DEBUG mode)
echo Project: %PROJECT_DIR%
echo Java: %JAVA_BIN%
echo Database: jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%
echo Database user: %DB_USERNAME%
echo.
echo Each service starts with a JVM debug agent (suspend=n).
echo Attach IntelliJ IDEA -^> Run -^> Edit Configurations -^> Remote JVM Debug
echo to the corresponding port BEFORE triggering the breakpoint.
echo.
echo Debug ports (app port - 2000):
echo   Gateway:            5011  (app 7001)
echo   Patient Service:    5012  (app 7002)
echo   Questionnaire:      5013  (app 7003)
echo   Assessment Service: 5014  (app 7004)
echo   Certificate:        5015  (app 7005)
echo   Document Service:   5016  (app 7006)
echo.

set "GATEWAY_DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5011"
set "PATIENT_DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5012"
set "QUEST_DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5013"
set "ASSESS_DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5014"
set "CERT_DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5015"
set "DOC_DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5016"

start "WHODAS Patient Service [debug:5012]" /D "%PROJECT_DIR%whodas-patient-service" "%JAVA_BIN%" %PATIENT_DEBUG% -jar "%PROJECT_DIR%whodas-patient-service\target\whodas-patient-service-1.0.0.jar" --server.port=7002
start "WHODAS Questionnaire Service [debug:5013]" /D "%PROJECT_DIR%whodas-questionnaire-service" "%JAVA_BIN%" %QUEST_DEBUG% -jar "%PROJECT_DIR%whodas-questionnaire-service\target\whodas-questionnaire-service-1.0.0.jar" --server.port=7003
start "WHODAS Assessment Service [debug:5014]" /D "%PROJECT_DIR%whodas-assessment-service" "%JAVA_BIN%" %ASSESS_DEBUG% -jar "%PROJECT_DIR%whodas-assessment-service\target\whodas-assessment-service-1.0.0.jar" --server.port=7004
start "WHODAS Certificate Service [debug:5015]" /D "%PROJECT_DIR%whodas-certificate-service" "%JAVA_BIN%" %CERT_DEBUG% -jar "%PROJECT_DIR%whodas-certificate-service\target\whodas-certificate-service-1.0.0.jar" --server.port=7005
start "WHODAS Document Service [debug:5016]" /D "%PROJECT_DIR%whodas-document-service" "%JAVA_BIN%" %DOC_DEBUG% -jar "%PROJECT_DIR%whodas-document-service\target\whodas-document-service-1.0.0.jar" --server.port=7006
timeout /t 5 /nobreak >nul
start "WHODAS Gateway [debug:5011]" /D "%PROJECT_DIR%whodas-gateway" "%JAVA_BIN%" %GATEWAY_DEBUG% -jar "%PROJECT_DIR%whodas-gateway\target\whodas-gateway-1.0.0.jar" --server.port=7001

echo Backend services started in debug mode
echo Patient:        http://localhost:7002  (debug 5012)
echo Questionnaire:  http://localhost:7003  (debug 5013)
echo Assessment:     http://localhost:7004  (debug 5014)
echo Certificate:    http://localhost:7005  (debug 5015)
echo Document:       http://localhost:7006  (debug 5016)
echo Gateway:        http://localhost:7001  (debug 5011)
echo.
echo Per debuggare l'avvio completo, modifica suspend=n in suspend=y in questo script.
