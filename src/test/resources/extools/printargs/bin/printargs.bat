@echo off
setlocal enabledelayedexpansion

set argCount=0
for %%x in (%*) do (
   set /A argCount+=1
   set "argVec[!argCount!]=%%~x"
)

echo Number of processed arguments: %argCount%

for /L %%i in (1,1,%argCount%) do echo ARG%%i="!argVec[%%i]!"
