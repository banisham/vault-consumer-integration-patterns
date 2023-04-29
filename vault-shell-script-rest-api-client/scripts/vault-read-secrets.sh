#!/bin/bash

set -e

VAULT_TOKEN="$1"
VAULT_URL="$2"
KV_PATH="$3"

MAX_RETRIES=3
BACKOFF_INTERVAL=1000
MAX_BACKOFF_INTERVAL=5000

echo "$VAULT_TOKEN $VAULT_URL $KV_PATH"

if [ -z "$VAULT_TOKEN" ] || [ -z "$VAULT_URL" ] || [ -z "$KV_PATH" ]; then
    echo "USAGE: $0 <VAULT_TOKEN> <VAULT_URL> <KV_PATH>"
    exit 1
fi

# wait until condition is met
wait_until_condition_met() {
    local sleep_time=$1
    local max_sleep_time=$2

    if [ $sleep_time -gt $max_sleep_time ]; then
        sleep_time=$max_sleep_time
    fi

    echo "Sleeping for ${sleep_time}ms"
    sleep $(echo "${sleep_time} / 1000" | bc -l)
}

# get secrets from Vault KV path
get_kv_secrets() {
    local retries=0
    local backoff_interval=$BACKOFF_INTERVAL
    local SECRET_PATH="$VAULT_URL/v1/$KV_PATH"

    while true; do
        echo "Retrieving secrets from $SECRET_PATH (attempt: $((retries + 1)))"
        echo curl --silent --show-error --header "X-Vault-Token: $VAULT_TOKEN" "$SECRET_PATH"
        local secrets=$(curl --silent --show-error --header "X-Vault-Token: $VAULT_TOKEN" "$SECRET_PATH" )
        echo "$secrets"

        local status_code=$(echo "$secrets" | jq -r '.errors[0] | .status')

        if [ "$status_code" = "200" ]; then
            local data=$(echo "$secrets" | jq -r '.data')
            for key in $(echo "$data" | jq -r 'keys[]'); do
                local value=$(echo "$data" | jq -r --arg key "$key" '.[$key]')
                echo "$key=$value"
            done
            return 0
        else
            local error_message=$(echo "$secrets" | jq -r '.errors[0] | .message')
            echo "Failed to get secrets from $KV_PATH: $error_message"

            retries=$((retries + 1))
            if [ "$retries" -ge "$MAX_RETRIES" ]; then
                echo "Maximum number of retries reached"
                return 1
            fi

            wait_until_condition_met "$backoff_interval" "$MAX_BACKOFF_INTERVAL"
            backoff_interval=$((backoff_interval * 2))
        fi
    done
}

# execute main function
get_kv_secrets
