vault {
  address = "http://127.0.0.1:8200"
}

template {
  source = "template.ctmpl"
  destination = "../output/output_file"
  command = "systemctl restart myapp.service"
  perms = 0644

  data = {
    db_acct_role1_secret = <<EOH
{{ with secret "secret/myapp/config" }}
  {{ .Data.username }}:{{ .Data.password }}
{{ end }}
EOH
    db_acct_role2_secret = <<EOH
{{ with secret "secret/myapp/sample-data2" }}
  {{ .Data.username }}:{{ .Data.password }}
{{ end }}
EOH
  }

  error_on_missing_key = true

  wait {
    min = "15s"
    max = "1m"
  }
}
