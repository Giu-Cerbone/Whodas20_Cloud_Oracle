@echo off
setlocal
REM Wrapper: la logica unica e' in setup-keycloak.js (Admin REST API dirette).
set "SCRIPT_DIR=%~dp0"
call "%SCRIPT_DIR%setup-keycloak.bat" %*
exit /b %ERRORLEVEL%