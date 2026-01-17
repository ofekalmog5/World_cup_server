@echo off
REM Comprehensive STOMP System Test Script
REM This script manages the full testing workflow

setlocal enabledelayedexpansion

cd /d "c:\Users\ofeka\OneDrive\Desktop\school\semester_c\spl\Assignment_3\Assignment 3 SPL_v2\Assignment 3 SPL"

cls
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║                                                                ║
echo ║       STOMP COMPLETE SYSTEM TEST & DEMO                        ║
echo ║                                                                ║
echo ║    Java Server + C++ Client + Python Infrastructure            ║
echo ║                                                                ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

echo Verification Phase:
echo ─────────────────────────────────────────────────────────────────

REM Check Java Server status
echo [1/4] Checking Java STOMP Server...
if exist "server\target\classes\bgu\spl\net\impl\stomp\StompServer.class" (
    echo   ✓ Server compiled
) else (
    echo   ✗ Server NOT compiled
    echo   Compiling...
    call cd server
    call mvn compile
    cd ..
)

REM Check C++ Client status
echo [2/4] Checking C++ STOMP Client...
if exist "client\bin\StompWCIClient.exe" (
    echo   ✓ Client binary found
) else (
    echo   ✗ Client NOT compiled
    echo   Compiling...
    cd client
    call make StompWCIClient
    cd ..
)

REM Check Python test infrastructure
echo [3/4] Checking Python test scripts...
if exist "quick_test.py" (
    echo   ✓ quick_test.py found
) else (
    echo   ✗ quick_test.py NOT found
)

if exist "debug_client.py" (
    echo   ✓ debug_client.py found
) else (
    echo   ✗ debug_client.py NOT found
)

REM Check SQL server
echo [4/4] Checking SQL server...
if exist "data\sql_server.py" (
    echo   ✓ sql_server.py found
) else (
    echo   ✗ sql_server.py NOT found
)

echo.
echo Next Steps:
echo ─────────────────────────────────────────────────────────────────
echo.
echo 1. START THE SERVER (in separate terminal):
echo    Option A (TPC mode):
echo      start "STOMP Server" cmd /k "cd server && mvn exec:java "-Dexec.mainClass=bgu.spl.net.impl.stomp.StompServer" "-Dexec.args=7777 tpc""
echo.
echo    Option B (Direct batch file):
echo      start_server.bat
echo.
echo 2. TEST SERVER CONNECTIVITY (after server starts):
echo    python quick_test.py
echo.
echo 3. RUN C++ CLIENT (interactive):
echo    run_client.bat
echo      or
echo    client\bin\StompWCIClient.exe
echo.
echo 4. RUN FULL INTEGRATION TEST:
echo    python full_test.py
echo.
echo ─────────────────────────────────────────────────────────────────
echo.
pause
