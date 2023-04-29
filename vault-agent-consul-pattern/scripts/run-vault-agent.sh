#!/bin/bash

# Define environment variables
export VAULT_ADDR="http://127.0.0.1:8200"
OUTPUT_DIR=../output
INPUT_DIR=../input
TEMPLATE_DIR=../resources
export ROLE_ID_FILE_PATH="$INPUT_DIR/role_id_file"
export SECRET_ID_FILE_PATH="$INPUT_DIR/secret_id_file"
export TOKEN_ID_FILE_PATH="$OUTPUT_DIR/token_id_file"
export OUTPUT_FILE_PATH="$OUTPUT_DIR/output_file"
export TEMPLATE_FILE_PATH="$TEMPLATE_DIR/template.ctmpl"
export CONSUL_TEMPLATE_CONFIG_PATH="$TEMPLATE_DIR/consul-template-config.hcl"
export VAULT_AGENT_CONFIG_PATH="$TEMPLATE_DIR/vault-agent-config.hcl"
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
fi

# Run consul-template
echo "Running consul-template..."
consul-template -config="$CONSUL_TEMPLATE_CONFIG_PATH" >/dev/null 2>&1 &

# Wait for consul-template to exit
echo "Waiting for consul-template to exit..."
while is_running "consul-template"; do
    sleep 1
done

# Stop vault-agent
echo "Stopping vault agent..."
kill $(cat "$PID_FILE_PATH")
rm "$PID_FILE_PATH"

echo "Done."

