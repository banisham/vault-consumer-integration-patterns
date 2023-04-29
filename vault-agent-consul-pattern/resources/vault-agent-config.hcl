pid_file = "/tmp/vault-agent.pid"
log_level = "debug"

auto_auth {
  method "approle" {
    mount_path = "auth/approle"
    config = {
      role_id_file_path = "../input/role_id_file"
      secret_id_file_path = "../input/secret_id_file"
      remove_secret_id_file_after_reading = false

    }

  }

  sink "file" {
    config = {
      path = "../output/token_id_file"
    }
  }
}

listener "tcp" {
  address = "127.0.0.1:8202"
  tls_disable = true
}
