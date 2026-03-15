@echo off
cls
gradlew build
C:\users\varoa\AppData\Local\Android\Sdk\platform-tools\adb.exe install app\build\outputs\apk\debug\app-debug.apk
pause