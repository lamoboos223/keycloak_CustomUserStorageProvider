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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Slf4j
public class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider> {
    protected final List<ProviderConfigProperty> configMetadata;
    
    public CustomUserStorageProviderFactory() {
        log.info("[I24] CustomUserStorageProviderFactory created");
        
        
        // Create config metadata
        configMetadata = ProviderConfigurationBuilder.create()
          .property()
            .name(CustomUserStorageProviderConstants.CONFIG_KEY_JDBC_DRIVER)
            .label("JDBC Driver Class")
            .type(ProviderConfigProperty.STRING_TYPE)
            .defaultValue("com.microsoft.sqlserver.jdbc.SQLServerDriver")
            .helpText("Fully qualified class name of the JDBC driver")
            .add()
          .property()
            .name(CustomUserStorageProviderConstants.CONFIG_KEY_JDBC_URL)
            .label("JDBC URL")
            .type(ProviderConfigProperty.STRING_TYPE)
//                .defaultValue("jdbc:sqlserver://<ip>:<port>;DatabaseName=<your db name>;")
                .defaultValue("jdbc:sqlserver://127.0.0.1:1433;databaseName=myolddb;")
            .helpText("JDBC URL used to connect to the user database")
            .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_USERS_TABLE_NAME)
                .label("Users Table Name")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("us_users")
                .helpText("Name of the users table in the database")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_ROLES_TABLE_NAME)
                .label("Roles Table Name")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("roles")
                .helpText("Name of the roles table in the database")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_USER_FIRST_NAME_FIELD_NAME)
                .label("Firstname Column")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("first_name_1")
                .helpText("The Column name that represents firstname of the user")
                .add()
            .property()
                .name(CustomUserStorageProviderConstants.CONFIG_KEY_USER_LAST_NAME_FIELD_NAME)
                .label("Last Column")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("last_name_1")
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
                .defaultValue("psd")
                .helpText("The Column name that represents password of the user")
                .add()
          .property()
            .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_USERNAME)
            .label("Database User")
            .type(ProviderConfigProperty.STRING_TYPE)
            .helpText("Username used to connect to the database")
                .defaultValue("sa")
            .add()
          .property()
            .name(CustomUserStorageProviderConstants.CONFIG_KEY_DB_PASSWORD)
            .label("Database Password")
            .type(ProviderConfigProperty.STRING_TYPE)
            .helpText("Password used to connect to the database")
                .defaultValue("Keycloak@123")
            .secret(true)
            .add()
          .property()
            .name(CustomUserStorageProviderConstants.CONFIG_KEY_VALIDATION_QUERY)
            .label("SQL Validation Query")
            .type(ProviderConfigProperty.STRING_TYPE)
            .helpText("SQL query used to validate a connection")
            .defaultValue("select * from users")
            .add()
          .build();   
          
    }

    @Override
    public CustomUserStorageProvider create(KeycloakSession ksession, ComponentModel model) {
        log.info("[I63] creating new CustomUserStorageProvider");
        Connection connection = null;
        try {
            connection = DbUtil.getConnection(model);
        } catch(SQLException ex) {
            throw new RuntimeException("Database error:" + ex.getMessage(),ex);
        }
        return new CustomUserStorageProvider(ksession,model, connection);
    }

    @Override
    public String getId() {
        return "JDBC User Provider";
    }

    
    // Configuration support methods
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
       try (Connection c = DbUtil.getConnection(config)) {
           log.info("[I84] Testing connection..." );
           c.createStatement().execute(config.get(CustomUserStorageProviderConstants.CONFIG_KEY_VALIDATION_QUERY));
           log.info("[I92] Connection OK !" );
       }
       catch(Exception ex) {
           log.warn("[W94] Unable to validate connection: ex={}", ex.getMessage());
           throw new ComponentValidationException("Unable to validate database connection",ex);
       }
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        log.info("[I94] onUpdate()" );
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        log.info("[I99] onCreate()" );
    }
}
