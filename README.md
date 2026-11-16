# keycloak-sha1-salted
Add salted SHA1 hashing support to Keycloak. ie. you're migrating to Keycloak and need to import legacy passwords stored as SHA-1 salted hashes. This password hash matches Kong API Gateway's basic auth hashing algorithm.

Forked from: https://github.com/nicolabeghin/keycloak-sha1-salted

## Building

- Run `docker build --no-cache --platform=linux/amd64 --output out --build-arg VERSION=version_value .`
- JAR archive is generated in `./out/app/avenza-keycloak-sha1-{version}.jar`

Version `0.0.3` removes all credential logging (salt, generated hash, stored
hash, result) from `verify()`. Its build is reproducible: building with
`maven@sha256:f58d59b6273e785ac0a4477f6e9b5ba1d7731c75b906c0f7b34076f1851318cc`
(Maven 3.9.9, Temurin 17) and `mvn -B clean verify` produces a JAR with SHA-256
`6dcfea14596c2cd6d6d7471c5281bbce8d2f9d67ebd060f74f08667973104f0b`.

## Deploying to Keycloak

1. Move the built JAR file to Keycloak's directory `providers/` (on Keycloak under Docker: `/opt/jboss/keycloak/providers`)
2. Watch the `providers/` for the file `avenza-keycloak-sha1.jar.deployed`

:warning: If you find instead the file `avenza-keycloak-sha1.jar.failed`, you can run the command `cat keycloak-sha1.jar.failed` to find out what went wrong with your deployment.

## How to use
Use algorithm `sha1-salted` when importing users through JSON. Below an example with
* password `123456789`
* salt `qwerty` (base64-encoded: `cXdlcnR5`)

JSON:

    {
      "realm": "master",
      "users": [
        {
          "username": "user1",
          "enabled": true,
          "totp": false,
          "emailVerified": true,
          "firstName": "user1",
          "lastName": "user1",
          "email": "user1@test.com",
          "credentials": [
            {
              "algorithm": "sha1-salted",
              "hashedSaltedValue": "9282d06b77e03989da6c0d86479ba73ac8691cfc",
              "salt": "cXdlcnR5",
              "hashIterations": 1,
              "type": "password"
            }
          ],
          "disableableCredentialTypes": [],
          "requiredActions": [],
          "realmRoles": [
            "offline_access",
            "uma_authorization"
          ],
          "clientRoles": {
            "account": [
              "manage-account",
              "view-profile"
            ]
          }
        }
      ]
    }
