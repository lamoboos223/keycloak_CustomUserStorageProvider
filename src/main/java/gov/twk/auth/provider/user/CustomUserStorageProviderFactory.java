package gov.twk.auth.provider.user;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import static java.util.Arrays.asList;

@Slf4j
public class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider> {

    protected final List<ProviderConfigProperty> configMetadata;


    public CustomUserStorageProviderFactory() {
        configMetadata = ProviderConfigurationBuilder.create()
            .property()
                    .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_TYPE)
                    .label("Database Type")
                    .type(ProviderConfigProperty.LIST_TYPE)
//                .defaultValue("MSSQL")
                .defaultValue("Postgres")
                    .options(asList("MSSQL", "Postgres", "MySQL"))
                    .helpText("Database type")
                    .add()
            .property()
                    .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_NAME)
                    .label("Database Name")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("myolddb")
                    .helpText("Database name for connecting")
                    .add()
            .property()
                    .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_IP)
                    .label("Database IP Address")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("127.0.0.1")
                    .helpText("Database IP Address")
                    .add()
            .property()
                    .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_PORT)
                    .label("Database Port")
                    .type(ProviderConfigProperty.STRING_TYPE)
//                .defaultValue("1433")
                .defaultValue("5432")
                    .helpText("Database Port")
                    .add()
            .property()
                    .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_USERNAME)
                    .label("Database User")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .helpText("Username used to connect to the database")
//                    .defaultValue("sa")
                .defaultValue("postgres")
                .add()
            .property()
                    .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_PASSWORD)
                    .label("Database Password")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .helpText("Password used to connect to the database")
//                .defaultValue("Keycloak@123")
                .defaultValue("postgres")
                    .secret(true)
                    .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_USERS_TABLE_NAME)
                .label("Users Table Name")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("us_users1")
                .helpText("Name of the users table in the database")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_ROLE_USER_ID_FIELD_NAME)
                .label("Role-User ID field")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("usernameid")
                .helpText("Name of the foreign key column in the roles table")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_USER_FIRST_NAME_FIELD_NAME)
                .label("Firstname Column")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("first_name")
                .helpText("The Column name that represents firstname of the user")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_USER_LAST_NAME_FIELD_NAME)
                .label("Lastname Column")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("last_name")
                .helpText("The Column name that represents lastname of the user")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_USER_USERNAME_FIELD_NAME)
                .label("Username Column")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("user_username")
                .helpText("The Column name that represents username of the user")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_USER_EMAIL_FIELD_NAME)
                .label("Email Column")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("email_address")
                .helpText("The Column name that represents email of the user")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_USER_PASSWORD_FIELD_NAME)
                .label("Password Column")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("school")
                .helpText("The Column name that represents password of the user")
                .add()
            .property().name(CustomUserStorageProviderConstants.CONFIG_KEY_USER_PASSWORD_HASH_NAME)
                .label("Password Hash Function")
                .type(ProviderConfigProperty.LIST_TYPE)
                .defaultValue("MD5")
                .options(asList("PlainText", "MD5", "SHA1", "SHA256", "SHA512"))
                .helpText("Password Hash Function for encryption when authentication")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_ROLES_TABLE_NAME)
                .label("Roles Table Name")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("us_roles")
                .helpText("Name of the roles table in the database, keep it empty if no roles apply")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_ROLE_NAME_FIELD_NAME)
                .label("Role Field Name")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("roleName")
                .helpText("Name of the role column in the roles table, keep it empty if no roles apply")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_VALIDATION_QUERY)
                .label("SQL Validation Query")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("SQL query used to validate a connection")
                .defaultValue("select 1")
                .add()
          .build();   
          
    }

    @Override
    public CustomUserStorageProvider create(KeycloakSession ksession, ComponentModel model) {
        Connection connection = null;
//        try {
//            connection = DbUtil.getConnection(model);
//        } catch(SQLException ex) {
//            throw new RuntimeException("Database error:" + ex.getMessage(),ex);
//        }
        connection = DbUtil.getConnection(model);

        return new CustomUserStorageProvider(ksession,model, connection);
    }

    @Override
    public String getId() {
        return "JDBC User Provider";
    }

    
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
       try (Connection c = DbUtil.getConnection(config)) {
           c.createStatement().execute(config.get(CustomUserStorageProviderConstants.CONFIG_KEY_VALIDATION_QUERY));
       }
       catch(Exception ex) {
           throw new ComponentValidationException("Unable to validate database connection\n",ex.getMessage());
       }
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
    }
}
