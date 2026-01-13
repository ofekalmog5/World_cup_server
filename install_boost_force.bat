@echo off
REM Force fix MSYS2 toolchain

echo Forcing removal of old GCC...
C:\msys64\usr\bin\bash.exe -lc "pacman -R --noconfirm mingw-w64-ucrt-x86_64-gcc mingw-w64-ucrt-x86_64-gcc-libs 2>nul"

echo Installing fresh toolchain...
C:\msys64\usr\bin\bash.exe -lc "pacman -S --noconfirm mingw-w64-ucrt-x86_64-gcc"

echo Installing Boost...
C:\msys64\usr\bin\bash.exe -lc "pacman -S --noconfirm mingw-w64-ucrt-x86_64-boost"

echo.
echo SUCCESS! Boost has been installed.
pause
