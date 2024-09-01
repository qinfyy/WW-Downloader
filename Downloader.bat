@echo off
chcp 936
title Wuthering Waves Downloader
cd /d %~dp0
rem 1.0.0
rem 0x2f649342688 https://prod-volcdn-gamestarter.kurogame.net/pcstarter/prod/game/G153/50004_obOHXFrFanqsaIEOmuKroCcbZkQRBC7c/index.json
rem 0x2f649342358 https://prod-alicdn-gamestarter.kurogame.com/pcstarter/prod/game/G153/50004_obOHXFrFanqsaIEOmuKroCcbZkQRBC7c/index.json
rem 0x25319014f28 https://prod-cn-alicdn-gamestarter.kurogame.com/pcstarter/prod/game/G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json
rem 0x25319044948 https://prod-volcdn-gamestarter.kurogame.com/pcstarter/prod/game/G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json

REM 1.2.0
REM 0x24a65203138 (244): https://prod-cn-alicdn-gamestarter.kurogame.com/pcstarter/prod/game/G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json
REM 0x24a651f50e8 (238): https://prod-volcdn-gamestarter.kurogame.com/pcstarter/prod/game/G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json

set IndexURL=https://prod-cn-alicdn-gamestarter.kurogame.com/pcstarter/prod/game/G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json
set OUTPATH=Wuthering Waves Game
set selectCDN=1

call :DelCache
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

call :GzipDecompression index.json
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

call :GzipDecompression resources.json
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
echo 开始下载文件
pause

REM 获取文件列表
rmdir /S /Q "%OUTPATH%"
DEL /F /Q DownloadList.txt >nul 2>nul
set /a index=amount-1
for /l %%i in (0,1,%index%) do (
    echo 索引：%%i
    for /F "tokens=* delims=*" %%b in ('jq.exe -r ".resource[%%i].dest" "resources.json"') do (
        set curFile=%%b
    )
    echo 文件：!curFile!
    echo %CDN%%resourcesBase%!curFile! >> DownloadList.txt
    echo    out=%OUTPATH%!curFile! >> DownloadList.txt
)

.\aria2c.exe --input-file=DownloadList.txt --continue=true --max-concurrent-downloads=20 --split=32 --file-allocation=prealloc --remote-time=true --check-certificate=false --user-agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 Edg/125.0.0.0"
echo 文件下载完成
echo 开始校验文件
pause

REM 文件校验部分
set ErrorCount=0
for /l %%i in (0,1,%index%) do (
    echo 索引：%%i

    REM 读取 dest、size 和 md5
    for /f "delims=" %%b in ('jq.exe -r ".resource[%%i].dest" "resources.json"') do (
        set "dest=%OUTPATH%%%b"
    )
    for /f "delims=" %%c in ('jq.exe -r ".resource[%%i].size" "resources.json"') do (
        set "expected_size=%%c"
    )
    for /f "delims=" %%d in ('jq.exe -r ".resource[%%i].md5" "resources.json"') do (
        set "expected_md5=%%d"
    )

    REM 将 JSON 中的 dest 转换为 Windows 格式路径
    set "dest=!dest:/=\!"

    if exist "!dest!" (
        echo [OK] 文件存在: !dest!

        REM 读取文件大小
        for /f "delims=" %%d in ('powershell.exe -command "(Get-Item '!dest!').length"') do (
            set "actual_size=%%d"
        )

        if "!actual_size!"=="!expected_size!" (
            echo [OK] 文件大小正确: !dest!

            REM 计算文件的 MD5
            for /f "delims=" %%e in ('powershell.exe -command "(Get-FileHash '!dest!' -Algorithm MD5).Hash"') do (
                set "actual_md5=%%e"
            )

            set "actual_md5=!actual_md5: =!"
            if /i "!actual_md5!"=="!expected_md5!" (
                echo [OK] 文件 MD5 正确: !dest!
            ) else (
                echo [ERROR] 文件 MD5 不正确: !dest! 预期: !expected_md5!, 实际: !actual_md5!
            	set /a ErrorCount=!ErrorCount!+1

                REM 错误文件处理
                for /F "tokens=* delims=*" %%b in ('jq.exe -r ".resource[%%i].dest" "resources.json"') do (
                   set curFile=%%b
                )
                echo 文件 MD5 不正确: !dest! >> 错误文件.txt
                echo 预期: !expected_md5!，实际: !actual_md5! >> 错误文件.txt
                echo 下载地址：%CDN%%resourcesBase%!curFile! >> 错误文件.txt
                echo. >> 错误文件.txt
            )
        ) else (
            echo [ERROR] 文件大小不正确: !dest! 预期: !expected_size!, 实际: !actual_size!
            set /a ErrorCount=!ErrorCount!+1

            REM 错误文件处理
            for /F "tokens=* delims=*" %%b in ('jq.exe -r ".resource[%%i].dest" "resources.json"') do (
                set curFile=%%b
            )
            echo 文件大小不正确: !dest! >> 错误文件.txt
            echo 预期: !expected_size!，实际: !actual_size! >> 错误文件.txt
            echo 下载地址：%CDN%%resourcesBase%!curFile! >> 错误文件.txt
            echo. >> 错误文件.txt
        )
    ) else (
        echo [ERROR] 文件不存在: !dest!
    	set /a ErrorCount=!ErrorCount!+1

        REM 错误文件处理
        for /F "tokens=* delims=*" %%b in ('jq.exe -r ".resource[%%i].dest" "resources.json"') do (
            set curFile=%%b
        )
        echo 文件不存在: !dest! >> 错误文件.txt
        echo 下载地址：%CDN%%resourcesBase%!curFile! >> 错误文件.txt
        echo. >> 错误文件.txt
    )
)

echo 文件校验完成
if %ErrorCount%==0 (
    echo 没有错误
    call :DelCache
) else (
    echo 有 %ErrorCount% 个文件校验未通过
    echo 请查看 错误文件.txt 手动下载错误的文件
)
endlocal
pause
exit

:DelCache
DEL /F /Q index.json >nul 2>nul
DEL /F /Q resources.json >nul 2>nul
DEL /F /Q DownLoadList.txt >nul 2>nul
DEL /F /Q 错误文件.txt >nul 2>nul
goto :eof

:GzipDecompression
set file=%1
REM 检查文件是否为Gzip (0x1F 0x8B)
for /f %%i in ('powershell.exe -command "if ((Get-Content -Path '%file%' -Raw -Encoding Byte)[0] -eq 0x1F -and (Get-Content -Path '%file%' -Raw -Encoding Byte)[1] -eq 0x8B) { 'true' } else { 'false' }"') do (
    set isGzip=%%i
)

REM 如果是Gzip，则解压
if "%isGzip%"=="true" (
    powershell.exe -command "Add-Type -A 'System.IO.Compression.FileSystem'; [IO.Compression.CompressionMode]::Decompress; [IO.Compression.GzipStream]::new([IO.File]::OpenRead('%file%'), [IO.Compression.CompressionMode]::Decompress).CopyTo([IO.File]::Create('%file%_decompressed'));"
    DEL /F /Q %file% >nul 2>nul
    ren %file%_decompressed %file%
)
goto :eof
