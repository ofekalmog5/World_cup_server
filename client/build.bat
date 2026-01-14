@echo off
echo Compiling C++ STOMP Client...

g++ -c -Wall -Weffc++ -g -std=c++11 -Iinclude -o bin/ConnectionHandler.o src/ConnectionHandler.cpp
if %errorlevel% neq 0 exit /b %errorlevel%

g++ -c -Wall -Weffc++ -g -std=c++11 -Iinclude -o bin/StompClient.o src/StompClient.cpp
if %errorlevel% neq 0 exit /b %errorlevel%

g++ -c -Wall -Weffc++ -g -std=c++11 -Iinclude -o bin/StompProtocol.o src/StompProtocol.cpp
if %errorlevel% neq 0 exit /b %errorlevel%

g++ -c -Wall -Weffc++ -g -std=c++11 -Iinclude -o bin/event.o src/event.cpp
if %errorlevel% neq 0 exit /b %errorlevel%

echo Linking...
g++ -o bin/StompWCIClient.exe bin/ConnectionHandler.o bin/StompClient.o bin/StompProtocol.o bin/event.o -lpthread -lws2_32
if %errorlevel% neq 0 exit /b %errorlevel%

echo.
echo Build successful! Binary: bin\StompWCIClient.exe
