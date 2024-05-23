@echo off
chcp 936
title 下载器
cd /d %~dp0
rem 1.0.0
rem 0x2f649342688 https://prod-volcdn-gamestarter.kurogame.net/pcstarter/prod/game/G153/50004_obOHXFrFanqsaIEOmuKroCcbZkQRBC7c/index.json
rem 0x2f649342358 https://prod-alicdn-gamestarter.kurogame.com/pcstarter/prod/game/G153/50004_obOHXFrFanqsaIEOmuKroCcbZkQRBC7c/index.json
rem 0x25319014f28 https://prod-cn-alicdn-gamestarter.kurogame.com/pcstarter/prod/game/G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json
rem 0x25319044948 https://prod-volcdn-gamestarter.kurogame.com/pcstarter/prod/game/G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json

set IndexURL=https://prod-volcdn-gamestarter.kurogame.net/pcstarter/prod/game/G153/50004_obOHXFrFanqsaIEOmuKroCcbZkQRBC7c/index.json
set OUTPATH=Wuthering Waves Game
set selectCDN=1

setlocal ENABLEDELAYEDEXPANSION
REM 获取API信息
echo 下载 index.json
set GET_INDEX_ERROR=0
:GET_INDEX
set /a GET_INDEX_ERROR+=1
if !GET_INDEX_ERROR! GTR 5 (
	echo 下载 index.json 文件失败
    pause
	exit
)

DEL /F /Q index.json >nul 2>nul
(.\aria2c.exe -o "index.json" --connect-timeout=3 --file-allocation=prealloc --check-certificate=false --user-agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 Edg/125.0.0.0" "%IndexURL%")||(timeout /T 3 /NOBREAK >nul& goto :GET_INDEX)

REM 获取CDN信息
for /f "delims=*" %%a in ('jq.exe -r ".default.cdnList[%selectCDN%].url" "index.json"') do (set CDN=%%a)
echo CDN：%CDN%

REM 获取资源Json路径信息
for /f "delims=*" %%a in ('jq.exe -r ".default.resources" "index.json"') do (
	set resources=%%a
)
echo resources：%resources%

echo 下载 resources.json 文件...
:GET_RESOURCES
set /a GET_RESOURCES_ERROR+=1
if !GET_RESOURCES_ERROR! GTR 5 (
	echo 下载 resources.json 文件失败
	pause
	exit
)

DEL /F /Q resources.json >nul 2>nul
(.\aria2c.exe -o "resources.json" --connect-timeout=3 --file-allocation=prealloc --check-certificate=false --user-agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 Edg/125.0.0.0" "%CDN%%resources%")||(timeout /T 3 /NOBREAK >nul& goto :GET_RESOURCES)

REM 获取资源路径信息
for /f "delims=*" %%a in ('jq.exe -r ".default.resourcesBasePath" "index.json"') do (
    set resourcesBase=%%a
)
echo resourcesBasePath：%resourcesBase%

REM 获取文件数量
for /f "delims=*" %%a in ('jq.exe -r ".resource | length" "resources.json"') do (
    set amount=%%a
)
echo 文件数量：%amount%

pause

REM 获取文件列表
rmdir /S /Q "%OUTPATH%"
DEL /F /Q DownloadList.txt >nul 2>nul

set /a index=amount-1
for /l %%i in (0,1,%index%) do (
    echo 索引：%%i
    set /a jsonindex 
    for /F "tokens=* delims=*" %%b in ('jq.exe -r ".resource[%%i].dest" "resources.json"') do (
        set curFile=%%b
        echo 文件：!curFile!
        echo %CDN%%resourcesBase%!curFile! >> DownloadList.txt
        echo    out=%OUTPATH%!curFile! >> DownloadList.txt
    )
)

.\aria2c --input-file="DownloadList.txt" --continue=true --max-concurrent-downloads=20 --split=32 --file-allocation=prealloc --remote-time=true --check-certificate=false --user-agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 Edg/125.0.0.0"

DEL /F /Q index.json >nul 2>nul
DEL /F /Q resources.json >nul 2>nul
DEL /F /Q DownLoadList.txt >nul 2>nul

endlocal
pause
exit
