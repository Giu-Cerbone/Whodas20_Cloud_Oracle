@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem ---------------------------------------------------------------------
rem WHODAS 2.0 - applicazione schema e caricamento dati di test
rem
rem Uso:  seed-all.cmd
rem
rem Si connette a PostgreSQL via JDBC (nessuna dipendenza da Docker o psql).
rem Esegue, nell'ordine:
rem    001..006  creazione tabelle e indici (idempotenti)
rem    009       tipo di compilazione del questionario
rem    010       tabelle del document service (import documenti)
rem    013       tabella registered_users (registrazione nuova utenza)
rem    008       allineamento dello schema (vincoli CHECK, NOT NULL)
rem    007       generazione dei dati di test
rem
rem Variabili di ambiente opzionali:
rem   DB_HOST      (default: localhost)
rem   DB_PORT      (default: 5432)
rem   DB_NAME      (default: whodas)
rem   DB_USERNAME  (default: postgres)
rem   DB_PASSWORD  (default: postgres)
rem   JAVA_BIN     (default: JAVA_HOME\bin\java.exe, oppure java.exe)
rem ---------------------------------------------------------------------

set "SCRIPT_DIR=%~dp0"
set "SQL_DIR=%SCRIPT_DIR%DLLDB"
set "TOOLS_DIR=%SCRIPT_DIR%tools"

if "%DB_HOST%"==""     set "DB_HOST=localhost"
if "%DB_PORT%"==""     set "DB_PORT=5432"
if "%DB_NAME%"==""     set "DB_NAME=whodas"
if "%DB_USERNAME%"=="" set "DB_USERNAME=postgres"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=postgres"

if "%JAVA_BIN%"=="" (
    if defined JAVA_HOME (
        set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
    ) else if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
        set "JAVA_BIN=C:\Program Files\Java\jdk-21\bin\java.exe"
    ) else (
        set "JAVA_BIN=java.exe"
    )
)

rem --- localizzazione del driver JDBC PostgreSQL -----------------------------
set "PG_JAR="
for %%P in (
    "%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.5\postgresql-42.7.5.jar"
) do if exist %%P set "PG_JAR=%%~P"

if not defined PG_JAR (
    echo [ERRORE] Driver JDBC PostgreSQL non trovato.
    echo         Atteso: %%USERPROFILE%%\.m2\repository\org\postgresql\postgresql\42.7.5\postgresql-42.7.5.jar
    echo         Eseguire una build del progetto per popolare il repository Maven:
    echo             mvn -DskipTests clean install
    exit /b 1
)

set "JDBC_URL=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%"

echo WHODAS database seeding
echo   URL     : %JDBC_URL%
echo   User    : %DB_USERNAME%
echo   Driver  : %PG_JAR%
echo.

set "STEP=0"
set "TOTAL=11"

rem --- 1. schema ------------------------------------------------------------
for %%N in (001_patients 002_questionnaire_items 003_assessments 004_item_responses 005_certificates 006_certificate_domain_scores) do (
    set /a STEP+=1
    call :run "!SQL_DIR!\%%N.sql"
    if errorlevel 1 exit /b 1
)

rem --- 2. tipo compilazione (tabella + FK + seed) -----------------------------
set /a STEP+=1
call :run "!SQL_DIR!\009_type_compile_quest.sql"
if errorlevel 1 exit /b 1

rem --- 3. tabelle del document service ---------------------------------------
set /a STEP+=1
call :run "!SQL_DIR!\010_document_imports.sql"
if errorlevel 1 exit /b 1

rem --- 4. registrazione nuova utenza ----------------------------------------
set /a STEP+=1
call :run "!SQL_DIR!\013_registered_users.sql"
if errorlevel 1 exit /b 1

rem --- 5. allineamento schema ----------------------------------------------
set /a STEP+=1
call :run "!SQL_DIR!\008_align_schema.sql"
if errorlevel 1 exit /b 1

rem --- 6. dati di test ------------------------------------------------------
set /a STEP+=1
call :run "!SQL_DIR!\007_seed_test_data.sql"
if errorlevel 1 exit /b 1

echo.
echo [OK] Database pronto: %DB_HOST%:%DB_PORT%/%DB_NAME%
echo     Avvia il backend con run-all.cmd
endlocal
exit /b 0

rem ---------------------------------------------------------------------------
:run
echo [!STEP!/!TOTAL!] %~nx1 ...
"%JAVA_BIN%" -cp "%PG_JAR%" "%TOOLS_DIR%\SqlRunner.java" "%JDBC_URL%" "%DB_USERNAME%" "%DB_PASSWORD%" "%~1"
exit /b %ERRORLEVEL%
