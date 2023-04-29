# Shell Scripts using Vault REST APIs
This project contains shell script implementation leveraging Vault REST API for authentication and reading secrets. 

## 1. vault-auth.sh
This shell script authenticates with Vault and retrieves a token using the Vault REST API. It uses the AppRole authentication method with the provided role ID and secret ID. The script also includes backoff retry mechanism in case of authentication failures.

### Usage
To use this script, simply run it with the following command:
```
./vault-auth.sh <vault_url> <role_id> <secret_id> <auth_path>
```
Replace the following arguments:

- `<vault_url>`: the URL of your Vault server
- `<role_id>`: the Role ID for the AppRole
- `<secret_id>`: the Secret ID for the AppRole
- `<auth_path>`: the authentication path for the AppRole

The script will output the token to the console upon successful authentication.


### Prerequisites
This script requires the following tools to be installed:

- curl
- jq

### Configuration 

Before running the script, you may need to modify the following variables in the script:

- `MAX_RETRIES`: the maximum number of authentication retries in case of failures
- `BACKOFF_INTERVAL`: the interval between retry attempts in milliseconds
- `MAX_BACKOFF_INTERVAL`: the maximum interval between retry attempts in milliseconds


## License

This project is licensed under the MIT License - see the LICENSE.md file for details.
