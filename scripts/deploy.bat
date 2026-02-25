@echo off
REM Deploy waveai-worker to AWS Elastic Beanstalk from Windows.
REM Usage: scripts\deploy.bat [region]
REM        scripts\deploy.bat eu-west-1

if "%WAVEAI_API_TOKEN%"=="" (
    echo ERROR: WAVEAI_API_TOKEN environment variable is not set.
    echo Run:  set WAVEAI_API_TOKEN=your-token-here
    exit /b 1
)

bash scripts/deploy.sh %1
