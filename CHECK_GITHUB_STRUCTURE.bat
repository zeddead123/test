@echo off
cd /d "%~dp0"
echo Checking GitHub Actions structure...
if exist ".github\workflows\build-apk.yml" (
  echo [OK] .github\workflows\build-apk.yml exists
) else (
  echo [ERROR] .github\workflows\build-apk.yml is missing
)
if exist "app\build.gradle" (echo [OK] app\build.gradle exists) else (echo [ERROR] app\build.gradle missing)
if exist "build.gradle" (echo [OK] build.gradle exists) else (echo [ERROR] build.gradle missing)
if exist "settings.gradle" (echo [OK] settings.gradle exists) else (echo [ERROR] settings.gradle missing)
pause
