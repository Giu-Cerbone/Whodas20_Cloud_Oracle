@echo off
setlocal
set "SCRIPT_DIR=%~dp0"

where node >nul 2>nul
if errorlevel 1 (
  echo [ERRORE] Node.js non trovato nel PATH.
  echo Installa Node.js 18+ oppure esegui direttamente: powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%setup-keycloak.ps1"
  exit /b 1
)

node "%SCRIPT_DIR%setup-keycloak.js" %*
set "EXITCODE=%ERRORLEVEL%"

if not "%EXITCODE%"=="0" (
  echo.
  echo [FALLITO] setup non completato. Vedi i messaggi [FAIL] sopra.
)
exit /b %EXITCODE%