@echo off
REM Start Java STOMP Server and keep it running
cd /d "c:\Users\ofeka\OneDrive\Desktop\school\semester_c\spl\Assignment_3\Assignment 3 SPL_v2\Assignment 3 SPL\server"
echo Starting STOMP Server on port 7777...
mvn exec:java "-Dexec.mainClass=bgu.spl.net.impl.stomp.StompServer" "-Dexec.args=7777 reactor"
pause
