# config.py

VAULT_ADDR = "http://127.0.0.1:8200"
AUTH_PATH = "/v1/auth/approle/login"
ROLE_ID = "2f953932-7dc7-df86-05ef-829774d7a046"
SECRET_ID = "b5b51a0e-c5ea-a991-54b6-4057e3a2d342"
MAX_RETRIES = 3

SECRET_PATH = "/v1/secret/my-secret"
