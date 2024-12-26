@echo off
echo Post-backup script executed for TestDatabase
echo Restarting dependent services...
timeout /t 1 >nul
echo Services restarted successfully.
