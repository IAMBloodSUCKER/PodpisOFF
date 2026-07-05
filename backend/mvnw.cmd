@echo off
setlocal enabledelayedexpansion

set "MVNW_PROJECT_DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_BASE_DIR=%MVNW_PROJECT_DIR%.mvn"
set "MAVEN_ZIP=%MAVEN_BASE_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_DIR=%MAVEN_BASE_DIR%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_DIR%\bin\mvn.cmd"

if exist "%MAVEN_CMD%" goto run

echo Downloading Maven %MAVEN_VERSION%...
if not exist "%MAVEN_BASE_DIR%" mkdir "%MAVEN_BASE_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ProgressPreference='SilentlyContinue';" ^
  "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%MAVEN_ZIP%'"
if %errorlevel% neq 0 (
  echo Failed to download Maven distribution.
  exit /b 1
)

echo Extracting Maven...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_BASE_DIR%' -Force"
if %errorlevel% neq 0 (
  echo Failed to extract Maven distribution.
  exit /b 1
)

:run
"%MAVEN_CMD%" %*
