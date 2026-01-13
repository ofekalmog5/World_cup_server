@echo off
REM Install Boost in MSYS2 UCRT64

echo Removing conflicting packages...
C:\msys64\usr\bin\bash.exe -lc "pacman -R --noconfirm mingw-w64-ucrt-x86_64-crt-git mingw-w64-ucrt-x86_64-headers-git mingw-w64-ucrt-x86_64-libmangle-git mingw-w64-ucrt-x86_64-libwinpthread-git mingw-w64-ucrt-x86_64-tools-git mingw-w64-ucrt-x86_64-winpthreads-git mingw-w64-ucrt-x86_64-winstorecompat-git 2>nul"

echo Installing Boost...
C:\msys64\usr\bin\bash.exe -lc "pacman -S --noconfirm mingw-w64-ucrt-x86_64-boost"

echo Done!
pause
