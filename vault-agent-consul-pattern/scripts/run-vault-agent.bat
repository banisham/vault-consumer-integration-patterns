@echo off

rem Define environment variables
set VAULT_ADDR=http://vault.example.com:8200
set ROLE_ID_FILE_PATH=C:\path\to\role_id_file
set SECRET_ID_FILE_PATH=C:\path\to\secret_id_file
set TOKEN_ID_FILE_PATH=C:\path\to\token_id_file
set OUTPUT_FILE_PATH=C:\path\to\output_file
set TEMPLATE_FILE_PATH=C:\path\to\template.ctmpl
set CONSUL_TEMPLATE_CONFIG_PATH=C:\path\to\consul-template-config.hcl
set VAULT_AGENT_CONFIG_PATH=C:\path\to\vault-agent-config.hcl
set PID_FILE_PATH=C:\Temp\vault-agent.pid

rem Define a function to check if a process is running
:is_running
tasklist /fi "imagename eq %1" | findstr /i "%1" > nul && (
    exit /b 0
) || (
    exit /b 1
)

rem Check if vault-agent is running
if call :is_running "vault.exe" (
    echo Vault agent is already running.
    exit /b 1
)

rem Start vault-agent
echo Starting vault agent...
start /b /wait vault.exe agent -config="%VAULT_AGENT_CONFIG_PATH%" >nul 2>&1
echo %errorlevel% > "%PID_FILE_PATH%"

rem Wait for vault-agent to start
echo Waiting for vault agent to start...
:wait_for_vault_agent
call :is_running "vault.exe" || (
    ping -n 2 127.0.0.1 > nul
    goto wait_for_vault_agent
)

rem Wait for vault-agent to authenticate and retrieve a token
echo Waiting for vault agent to authenticate and retrieve a token...
:wait_for_token
if not exist "%TOKEN_ID_FILE_PATH%" (
    ping -n 2 127.0.0.1 > nul
    goto wait_for_token
)

rem Wait for consul-template to start
echo Waiting for consul-template to start...
:wait_for_consul_template
call :is_running "consul-template.exe" || (
    ping -n 2 127.0.0.1 > nul
    goto wait_for_consul_template
)

rem Run consul-template
echo Running consul-template...
start /b /wait consul-template.exe -config="%CONSUL_TEMPLATE_CONFIG_PATH%" >nul 2>&1

rem Wait for consul-template to exit
echo Waiting for consul-template to exit...
:wait_for_consul_template_exit
if call :is_running "consul-template.exe" (
    ping -n 2 127.0.0.1 > nul
    goto wait_for_consul_template_exit
)

rem Stop vault-agent
echo Stopping vault agent...
for /f "usebackq" %%i in ("%PID_FILE_PATH%") do taskkill /f /pid %%i >nul 2>&1
del "%PID_FILE_PATH%"

echo Done.
exit /b 0
