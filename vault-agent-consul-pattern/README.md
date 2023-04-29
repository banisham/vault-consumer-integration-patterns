# Vault and Consul Template Pattern
This project demonstrates how to use Vault and Consul Template to securely retrieve and manage secrets.

## Prerequisites
- Vault installed and configured
- Consul Template installed and configured
- Bash (for Unix) or Command Prompt (for Windows)

## Components Introduction

### 1. vault-agent-config.hcl
The configuration file for Vault Agent is written in HCL (Hashicorp Configuration Language), and it specifies which Vault secrets to watch, 
how often to check for updates, and where to write the secrets.

The `vault-agent-config.hcl` file contains the configuration for the Vault Agent. This file specifies the following:

- **method**: the authentication method used to authenticate to Vault.
- **sink**: the destination where the Vault token will be stored.
- **listener**: the address and port where the Vault Agent will listen for connections.
- **template**: the Consul Template configuration file used to retrieve secrets from Vault and write them to a file.


### 2. consul-template-config.hcl
Consul Template is a tool that allows you to dynamically generate configuration files from templates, using data from Consul and other sources.
It runs as a daemon, and watches for changes in the data sources it has been configured to watch. 
When it detects a change, it generates a new configuration file from the template.

The `consul-template-config.hcl` file contains the configuration for Consul Template. This file specifies the following:

- **vault**: the Vault address where secrets will be retrieved.
- **template**: the Consul Template configuration file used to retrieve secrets from Vault and write them to a file.


### 3. template.ctmpl
The template file specifies the format of the generated configuration file. 
It is a plain text file that contains placeholders for the dynamic data that will be populated by Consul Template. 
For example, it might contain placeholders for IP addresses, port numbers, or other configuration parameters.

The `template.ctmpl` file is a Consul Template file that retrieves secrets from Vault and writes them to a file. This file contains the following:

- **{{ with secret "path/to/secret" }}**: starts a block of Consul Template code that retrieves secrets from Vault at the specified path.
- **{{ .Data.field }}**: retrieves a field from the secret data.
- **{{ end }}**: ends the block of Consul Template code.

## Additional Security Measures
You can further apply additional security measures such as encryption and access control to the secrets written to the output file,


## Conclusion
In summary, Vault Agent and Consul Template are powerful tools for managing secrets and generating configuration files dynamically. 
By configuring them with HCL files, you can easily automate the retrieval of secrets and the generation of configuration files, 
making your application more secure and easier to manage.

Together, these files work in tandem to retrieve secrets from a Vault server, use them to generate a configuration file using Consul Template, 
and write the resulting configuration to an output file. The Vault agent is used to handle authentication with the Vault server and 
provide the necessary secrets to Consul Template for rendering the output. 
The Consul Template tool is used to generate the output file based on the template file and the data retrieved from Vault.

It's important to note that the configuration in these files is specific to the particular use case and environment in which they are used. 
The paths and authentication methods, for example, will vary depending on the specific Vault server and environment being used.


**-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------**

## Setup
1. Clone this repository to your local machine:
 ``` 
   git clone https://github.com/username/repo.git
 ```

2. Create a `config` directory under the root of the cloned repository.
3. Create a `resources` directory under the root of the cloned repository.
4. Create a `scripts` directory under the root of the cloned repository.
5. Copy `vault-agent-config.hcl`, `consul-template-config.hcl`, and `template.ctmpl` files to the `resources` directory.
6. Copy `run-vault-agent.sh` file to the scripts directory.

## Configuration
Before running the shell script or bat file, you need to configure the following environment variables:

For Unix
```
export VAULT_ADDR="http://vault.example.com:8200"
export ROLE_ID_FILE_PATH="/path/to/role_id_file"
export SECRET_ID_FILE_PATH="/path/to/secret_id_file"
export TOKEN_ID_FILE_PATH="/path/to/token_id_file"
export OUTPUT_FILE_PATH="/path/to/output_file"
export TEMPLATE_FILE_PATH="/path/to/template.ctmpl"
export CONSUL_TEMPLATE_CONFIG_PATH="/path/to/consul-template-config.hcl"
export VAULT_AGENT_CONFIG_PATH="/path/to/vault-agent-config.hcl"
export PID_FILE_PATH="/tmp/vault-agent.pid"

```


For Windows
```
set VAULT_ADDR=http://vault.example.com:8200
set ROLE_ID_FILE_PATH=C:\path\to\role_id_file
set SECRET_ID_FILE_PATH=C:\path\to\secret_id_file
set TOKEN_ID_FILE_PATH=C:\path\to\token_id_file
set OUTPUT_FILE_PATH=C:\path\to\output_file
set TEMPLATE_FILE_PATH=C:\path\to\template.ctmpl
set CONSUL_TEMPLATE_CONFIG_PATH=C:\path\to\consul-template-config.hcl
set VAULT_AGENT_CONFIG_PATH=C:\path\to\vault-agent-config.hcl
set PID_FILE_PATH=C:\tmp\vault-agent.pid
```

Make sure to replace the placeholders with the actual paths.


## Running
To start the Vault Agent with Consul Template, run the following command:

For Unix
```
./scripts/run-vault-agent.sh
```

For Windows
```
.\scripts\run-vault-agent.bat
```

This will start the Vault Agent, authenticate with Vault using the role ID and secret ID, retrieve a token, and run Consul Template to generate the output file with the secrets from Vault.

## Stopping

To stop the Vault Agent and Consul Template, press Ctrl+C in the terminal.


## Contributing

To contribute to the project, follow these steps:

1. Fork the repository.
2. Create a new branch for your feature.
3. Make your changes and commit them.
4. Push your changes to your fork.
5. Submit a pull request to the main repository.

## License

This project is licensed under the MIT License - see the LICENSE.md file for details.
