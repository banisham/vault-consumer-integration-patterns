#!/bin/bash

VAULT_ADDR="http://127.0.0.1:8200"
APP_ROLE_PATH="auth/approle/login"
ROLE_ID="b1c4b907-18b8-c65b-b93c-89e54ebb7ec6"
SECRET_ID="820c33ea-6860-18b9-5579-4c2a15f7658d"
MAX_RETRIES=3
BACKOFF_INTERVAL=1000
MAX_BACKOFF_INTERVAL=10000
KV_PATH=secret/myapp/config

wait_until_condition_met() {
  local condition=$1
  local interval=$2
  local max_wait_time=$3
  local current_wait_time=0

  while [[ $current_wait_time -lt $max_wait_time ]]; do
    if eval "$condition"; then
      return 0
    fi

    sleep $interval
    current_wait_time=$((current_wait_time + interval))
  done

  return 1
}

num_retries=0
vault_token=""

while [[ $num_retries -lt $MAX_RETRIES ]]; do
  echo curl -s -X POST -d '{"role_id":"'"$ROLE_ID"'","secret_id":"'"$SECRET_ID"'"}' "$VAULT_ADDR/v1/$APP_ROLE_PATH"
  response=$(curl -s -X POST -d '{"role_id":"'"$ROLE_ID"'","secret_id":"'"$SECRET_ID"'"}' "$VAULT_ADDR/v1/$APP_ROLE_PATH")
  status=$(echo "$response" | jq -r ".auth == null | not")

  if [[ $status == true ]]; then
    vault_token=$(echo "$response" | jq -r ".auth.client_token")
    echo "Vault authentication successful"
    echo "Vault token: $vault_token"
    ./vault-read-secrets.sh "$vault_token" "$VAULT_ADDR" "$KV_PATH"
    exit 0
  else
    error_code=$(echo "$response" | jq -r ".errors[0]" 2>/dev/null || echo "unknown")
    error_message=$(echo "$response" | jq -r ".errors[1]" 2>/dev/null || echo "unknown")
    echo "Vault authentication failed. Error code: $error_code. Error message: $error_message"

    if [[ $error_code == "permission denied" ]]; then
      exit 1
    else
      num_retries=$((num_retries + 1))
      echo "Retrying Vault authentication after $BACKOFF_INTERVAL milliseconds"
      wait_until_condition_met "false" $BACKOFF_INTERVAL $MAX_BACKOFF_INTERVAL
    fi
  fi
done

echo "Max retries exceeded for Vault authentication"
exit 1
