### Relevant Articles:

- [Using Custom User Providers with Keycloak](https://www.baeldung.com/java-keycloak-custom-user-providers)

### Description

this keycloak custom user storage provider lets you federate users from database, currently it supports mssql and postgres. in case you want to user different db provider add the module in jboss-deployment-structure.xml
and add its jar in the `\keycloak-16.1.1\modules\system\layers\keycloak\com` directory.