#!/bin/bash

# Define environment variables
export VAULT_ADDR="http://127.0.0.1:8200"
export ROLE_ID_FILE_PATH="../input/role_id_file"
export SECRET_ID_FILE_PATH="../input/secret_id_file"
export TOKEN_ID_FILE_PATH="../output/token_id_file"
export OUTPUT_FILE_PATH="../output/output_file"
export TEMPLATE_FILE_PATH="../resources/template.ctmpl"
export VAULT_AGENT_CONFIG_PATH="../resources/vault-agent-config.hcl"
 PID_FILE_PATH="/tmp/vault-agent.pid"

# Define a function to check if a process is running
function is_running() {
    pgrep -f "$1" >/dev/null 2>&1
}

file_content_changed() {
    local file="$1"
    local oldsum=$(md5 "$file")
    while true; do
        sleep 5
        local newsum=$(md5 "$file")
        if [[ "$newsum" != "$oldsum" ]]; then
            echo "File content has changed"
            return 0
        fi
    done
    return 1
}

# Check if vault-agent is running
if is_running "vault agent"; then
    echo "Vault agent is already running."
    exit 1
fi

# Start vault-agent
echo "Starting vault agent..."
nohup vault agent -config="$VAULT_AGENT_CONFIG_PATH" >/dev/null 2>&1 &
echo $! > "$PID_FILE_PATH"

# Wait for vault-agent to start
echo "Waiting for vault agent to start..."
while ! is_running "vault agent"; do
    sleep 1
done

# Wait for vault-agent to authenticate and retrieve a token
echo "Waiting for vault agent to authenticate and retrieve a token..."

if file_content_changed "$TOKEN_ID_FILE_PATH"; then
  echo "Vault agent successfully authenticated and retrieve token..."
else
    echo "Vault agent NOT able to retrieve the latest token..."
    kill $(cat "$PID_FILE_PATH")
    rm "$PID_FILE_PATH"
    exit 1
fi


# Stop vault-agent
echo "Stopping vault agent..."
kill $(cat "$PID_FILE_PATH")
rm "$PID_FILE_PATH"

echo "Done."

