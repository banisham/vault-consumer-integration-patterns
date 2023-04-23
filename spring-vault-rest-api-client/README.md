# REST API based Vault Integration for Spring Applications.

### VaultAuthenticator
This is a Java-based library that provides methods to authenticate with HashiCorp Vault using the AppRole and TLS authentication methods.

### VaultDBSecrets 
This is a Java-based library that provides methods to read the database secrets from Vault which are vaulted using Database Secret Engine.

### VaultDirectorySecrets
This is a Java-based library that provides methods to read the directory (AD, OUD) secrets from Vault which are vaulted via 
the Active Directory & LDAP Secret Engines.

### VaultStaticSecrets
This is a Java-based library that provides methods to read the static secrets from Vault which are vaulted via
the KV Secret Engine.


## Table of Contents
- [Requirements](#installation)
- [Installation](#installation)
- [Usage](#usage)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

## Requiremnts
- Java 8 or later
- Spring Framework version 5 or later
- Apache HTTPComponents version 5 or later
- SLF4J version 1.7.30 or later

## Installation

To install the project, follow these steps:

1. Clone the repository to your local machine.
2. Build the project using `mvn clean install`.
3. Start the server using `mvn spring-boot:run`.

## Usage



This is a Java-based library that provides methods to authenticate with HashiCorp Vault using the AppRole and TLS authentication methods.
                                     |
## Testing


## Contributing

To contribute to the project, follow these steps:

1. Fork the repository.
2. Create a new branch for your feature.
3. Make your changes and commit them.
4. Push your changes to your fork.
5. Submit a pull request to the main repository.

## License

This project is licensed under the MIT License - see the LICENSE.md file for details.
