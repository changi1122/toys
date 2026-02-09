@echo off
pushd "%~dp0"

:: 1. 오늘 날짜를 변수에 담기 (따옴표로 감싸서 공백 문제 방지)
set "current_date=%date%"

:: 2. 기록 파일이 있다면 읽어오기
set "last_run=none"
if exist "last_run_date.txt" (
    set /p last_run=<"last_run_date.txt"
)

echo [DEBUG] Current Date: %current_date%
echo [DEBUG] Last Run Date: %last_run%

:: 3. 비교 (단순 문자열 비교)
if "%last_run%"=="%current_date%" (
    echo [INFO] Today is %current_date%.
    echo [INFO] Already executed today. Closing in 3 seconds...
    timeout /t 3
    exit
)

:: 4. 실행 기록 저장 (괄호를 사용해 뒤쪽 공백이 안 들어가게 함)
(echo %current_date%)> "last_run_date.txt"

:: 5. Gradle 실행
echo [INFO] First run today. Starting Gradle...
call gradlew.bat run

:: 6. 완료 후 창 유지 (테스트용)
echo [INFO] Process finished.
timeout /t 3
popd