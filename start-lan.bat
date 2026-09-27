@echo off
title StudyFlow Same-Wi-Fi Server
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" -Lan
echo.
echo StudyFlow has stopped. Review any message above.
pause
