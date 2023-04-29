#!/bin/bash

# Parse JSON file using jq
secrets=$(jq -c '.["static-secrets"][]' ../output/output_file.json)

# Loop through secrets and print values
while read -r secret; do
    secret_type=$(echo "${secret}" | jq -r '.["secret-type"]')
    username=$(echo "${secret}" | jq -r '.["username"]')
    password=$(echo "${secret}" | jq -r '.["password"]')

    echo "Secret type: ${secret_type}"
    echo "Username: ${username}"
    echo "Password: ${password}"
    echo
done <<< "${secrets}"
