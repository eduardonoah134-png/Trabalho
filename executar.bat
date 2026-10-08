@echo off
title Website Under Construction - Java
echo ===================================================
echo   Iniciando Tela em Construcao Animada com Java
echo ===================================================
echo.
"%~dp0oracleJdk-27\bin\javac.exe" -encoding UTF-8 "%~dp0UnderConstructionApp.java"
if %errorlevel% neq 0 (
    echo [ERRO] Falha na compilacao.
    pause
    exit /b %errorlevel%
)

start "" "%~dp0oracleJdk-27\bin\java.exe" UnderConstructionApp
echo Aplicacao iniciada com sucesso!
echo Servidor Web local: http://localhost:8080
timeout /t 3 >nul

