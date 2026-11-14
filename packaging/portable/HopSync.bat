@echo off
rem Starts HopSync with the Java 21 found on the PATH
cd /d "%~dp0"
start "" javaw -jar HopSync.jar
