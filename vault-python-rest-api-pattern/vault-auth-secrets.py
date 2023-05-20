import http.client
import json
import time
import config
import ssl

from urllib.parse import urlparse

# Read parameters from the config file
VAULT_ADDR = config.VAULT_ADDR
AUTH_PATH = config.AUTH_PATH
MAX_RETRIES = config.MAX_RETRIES
ROLE_ID = config.ROLE_ID
SECRET_ID = config.SECRET_ID
SECRET_PATH = config.SECRET_PATH


def authenticate_with_vault():
    print(f"Values :{VAULT_ADDR} {AUTH_PATH} {ROLE_ID} {SECRET_ID}")
    if VAULT_ADDR.startswith("http://"):
        conn = http.client.HTTPConnection(urlparse(VAULT_ADDR).netloc)
    elif VAULT_ADDR.startswith("https://"):
        conn = http.client.HTTPSConnection(urlparse(VAULT_ADDR).netloc, context=ssl.create_default_context())
    else:
        print("The parameter does not start with http:// or https://")

    headers = {"Content-Type": "application/json"}
    payload = json.dumps({"role_id": ROLE_ID, "secret_id": SECRET_ID})

    for _ in range(MAX_RETRIES):
        try:
            conn.request("POST", AUTH_PATH, body=payload, headers=headers)
            response = conn.getresponse()

            if response.status == 200:
                data = json.loads(response.read().decode())
                token = data["auth"]["client_token"]
                return token

            elif response.status == 429:
                # Retry after backoff delay for rate limiting
                backoff_delay = int(response.getheader("Retry-After", 1))
                time.sleep(backoff_delay)

            else:
                print(f"Failed to authenticate with Vault. Status: {response.status}")
                break

        except Exception as e:
            print(f"An error occurred during authentication: {str(e)}")
            break

    return None


def read_kv_secret_from_vault(token, secret_path):
    conn = http.client.HTTPConnection(urlparse(VAULT_ADDR).netloc)
    headers = {"X-Vault-Token": token}

    try:
        if VAULT_ADDR.startswith("http://"):
            conn.request("GET", secret_path, headers=headers)
        elif VAULT_ADDR.startswith("https://"):
            conn = http.client.HTTPSConnection(urlparse(VAULT_ADDR).netloc, context=ssl.create_default_context())
        else:
            print("The parameter does not start with http:// or https://")

        response = conn.getresponse()

        if response.status == 200:
            data = json.loads(response.read().decode())
            secret = data["data"]
            print(f"Secret at path '{secret_path}':")
            for key, value in secret.items():
                print(f"{key}: {value}")
        else:
            print(f"Failed to read secret from Vault. Status: {response.status}")

    except Exception as e:
        print(f"An error occurred while reading secret from Vault: {str(e)}")


# Authenticate with Vault
vault_token = authenticate_with_vault()

if vault_token:
    print("Successfully authenticated with Vault!")
    print(f"Vault token: {vault_token}")

    # Read and display the secret from Vault
    read_kv_secret_from_vault(vault_token, SECRET_PATH)

else:
    print("Failed to authenticate with Vault.")
