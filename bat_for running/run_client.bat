@echo off
REM Test the C++ STOMP Client with the Java Server
REM Prerequisites:
REM   1. Java STOMP Server running on port 7777 (use start_server.bat)
REM   2. C++ Client compiled (client\bin\StompWCIClient.exe)

echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║        STOMP C++ CLIENT INTERACTIVE TEST                       ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
echo Prerequisites:
echo   ✓ Java STOMP Server running on port 7777
echo   ✓ C++ Client binary available at: client\bin\StompWCIClient.exe
echo.

cd /d "c:\Users\ofeka\OneDrive\Desktop\school\semester_c\spl\Assignment_3\Assignment 3 SPL_v2\Assignment 3 SPL"

REM Check if client exists
if not exist "client\bin\StompWCIClient.exe" (
    echo ✗ Client binary not found!
    echo   Expected: client\bin\StompWCIClient.exe
    echo.
    echo To compile the client, run:
    echo   cd client
    echo   make StompWCIClient
    pause
    exit /b 1
)

echo ✓ Client binary found
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║ Client is now running. Enter commands:                         ║
echo ║                                                                ║
echo ║ Examples:                                                      ║
echo ║   login 127.0.0.1:7777 testuser testpass                      ║
echo ║   join game_channel1                                           ║
echo ║   report event_file.json                                       ║
echo ║   exit                                                         ║
echo ║                                                                ║
echo ║ Type 'exit' or 'quit' to disconnect                           ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

client\bin\StompWCIClient.exe

echo.
echo Client disconnected
pause
