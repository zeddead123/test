@echo off
setlocal
cd /d "%~dp0"
if not exist ".github\workflows" mkdir ".github\workflows"
copy /Y "BUILD_APK_WORKFLOW.yml" ".github\workflows\build-apk.yml" >nul
if errorlevel 1 (
  echo ERROR: could not create .github\workflows\build-apk.yml
  pause
  exit /b 1
)
echo OK: .github\workflows\build-apk.yml is ready
explorer "%CD%\.github\workflows"
pause
