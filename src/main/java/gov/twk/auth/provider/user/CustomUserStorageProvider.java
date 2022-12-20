/**
 * 
 */
package gov.twk.auth.provider.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;


@Slf4j
public class CustomUserStorageProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator, UserQueryProvider, UserRegistrationProvider {

    private KeycloakSession ksession;
    private ComponentModel model;
    private Connection connection;

    public CustomUserStorageProvider(KeycloakSession ksession, ComponentModel model, Connection connection) {
        this.ksession = ksession;
        this.model = model;
        this.connection = connection;
    }

    @Override
    public void close() {
//        log.info("[I30] close()");
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        log.info("[I35] getUserById({})",id);
        StorageId sid = new StorageId(id);
        return getUserByUsername(sid.getExternalId(),realm);
    }



    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        log.info("[I48] getUserByEmail({})",email);
        try{
            String query = String.format("select * from users where %s = ?",
                    CustomUserStorageProviderConstants.CONFIG_KEY_USER_EMAIL_FIELD_NAME);
            PreparedStatement st = connection.prepareStatement(query);
            st.setString(1, email);
            st.execute();
            ResultSet rs = st.getResultSet();
            if ( rs.next()) {
                return mapUser(realm,rs);
            }
            else {
                return null;
            }
        }
        catch(SQLException ex) {
            throw new RuntimeException("Database error:" + ex.getMessage(),ex);
        }
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        log.info("[I57] supportsCredentialType({})",credentialType);
        return PasswordCredentialModel.TYPE.endsWith(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        log.info("[I57] isConfiguredFor(realm={},user={},credentialType={})",realm.getName(), user.getUsername(), credentialType);
        // In our case, password is the only type of credential, so we always return 'true' if
        // this is the credentialType
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        log.info("[I57] isValid(realm={},user={},credentialInput.type={})",realm.getName(), user.getUsername(), credentialInput.getType());
        if( !this.supportsCredentialType(credentialInput.getType())) {
            return false;
        }
        StorageId sid = new StorageId(user.getId());
        String username = sid.getExternalId();

        try{
            String query = String.format("select %s from %s where %s = ?",
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_PASSWORD_FIELD_NAME),
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_DB_USERS_TABLE_NAME),
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_USERNAME_FIELD_NAME));
            PreparedStatement st = connection.prepareStatement(query);
            st.setString(1, username);
            st.execute();
            ResultSet rs = st.getResultSet();
            if ( rs.next()) {
                String pwd = rs.getString(1);
                return pwd.equals(credentialInput.getChallengeResponse());
            }
            else {
                return false;
            }
        }
        catch(SQLException ex) {
            throw new RuntimeException("Database error:" + ex.getMessage(),ex);
        }
    }

    // UserQueryProvider implementation

    @Override
    public int getUsersCount(RealmModel realm) {
        log.info("[I93] getUsersCount: realm={}", realm.getName() );
        try{
            String query = String.format("select count(*) from %s",
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_DB_USERS_TABLE_NAME));
            Statement st = connection.createStatement();
            st.execute(query);
            ResultSet rs = st.getResultSet();
            rs.next();
            return rs.getInt(1);
        }
        catch(SQLException ex) {
            throw new RuntimeException("Database error:" + ex.getMessage(),ex);
        }
    }



    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search,realm,0,10);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        log.info("[I139] searchForUser: realm={}", realm.getName());

        try{
//            PreparedStatement st = c.prepareStatement("select username, firstName,lastName, email, birthDate from users where username like ? order by username limit ? offset ?");
            String query = String.format("select * from %s where %s like ? order by %s limit ? offset ?",
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_DB_USERS_TABLE_NAME),
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_USERNAME_FIELD_NAME),
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_DB_USERS_TABLE_NAME)
                    );
            PreparedStatement st = connection.prepareStatement(query);
            st.setString(1, search);
            st.setInt(2, maxResults);
            st.setInt(3, firstResult);
            st.execute();
            ResultSet rs = st.getResultSet();
            List<UserModel> users = new ArrayList<>();
            while(rs.next()) {
                users.add(mapUser(realm,rs));
            }
            return users;
        }
        catch(SQLException ex) {
            throw new RuntimeException("Database error:" + ex.getMessage(),ex);
        }
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return searchForUser(params,realm,0,5000);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        return getUsers(realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm,0, 10); // Keep a reasonable maxResults
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        log.info("[I113] getUsers: realm={}", realm.getName());

        try{
            String query = String.format("select * from %s order by %s",
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_DB_USERS_TABLE_NAME),
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_USERNAME_FIELD_NAME));
            PreparedStatement st = connection.prepareStatement(query);
            st.execute();
            ResultSet rs = st.getResultSet();
            List<UserModel> users = new ArrayList<>();
            while(rs.next()) {
                users.add(mapUser(realm,rs));
            }
            return users;
        }
        catch(SQLException ex) {
            throw new RuntimeException("Database error:" + ex.getMessage(),ex);
        }
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        log.info("[I41] getUserByUsername({})",username);
        try{
            String query = String.format("select * from %s where %s = ?",
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_DB_USERS_TABLE_NAME),
                    model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_USERNAME_FIELD_NAME));
            PreparedStatement st = connection.prepareStatement(query);
            st.setString(1, username);
            st.execute();
            ResultSet rs = st.getResultSet();

            if (rs.next()) {
//                return createAdapter(realm, rs);
                return mapGetUser(realm,rs);
            }
            else {
                return null;
            }
        }
        catch(SQLException ex) {
            throw new RuntimeException("Database error:" + ex.getMessage(),ex);
        }
    }

    private UserModel mapUser(RealmModel realm, ResultSet rs) throws SQLException {
        UserRepresentation userRepresentation = new UserRepresentation(ksession, realm, model);
        int columns = rs.getMetaData().getColumnCount();

        for (int i = 1; i <= columns; i++) {
            String key = rs.getMetaData().getColumnName(i);
            String value = rs.getString(i);

            if(key.equals(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_USERNAME_FIELD_NAME))) {
                userRepresentation.setUsername(value);
            }
            else if(key.equals(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_PASSWORD_FIELD_NAME)) || key.equals("password")) {
                continue;
            }
            else if(key.equals(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_FIRST_NAME_FIELD_NAME))) {
                userRepresentation.setFirstName(value);
            }
            else if(key.equals(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_LAST_NAME_FIELD_NAME))) {
                userRepresentation.setLastName(value);
            }

            else if(key.equals(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_EMAIL_FIELD_NAME))) {
                userRepresentation.setEmail(value);
            }
//            when the key is a user attribute --> it will be saved in this format user.attribute.gender when actually the db column was only gender
            else
                userRepresentation.setSingleAttribute(key, value);
        }
        return userRepresentation;
    }

    private UserModel mapGetUser(RealmModel realm, ResultSet rs) throws SQLException {
        UserRepresentation userRepresentation = new UserRepresentation(ksession, realm, model);
        int columns = rs.getMetaData().getColumnCount();

        for (int i = 1; i <= columns; i++) {
            String key = rs.getMetaData().getColumnName(i);
            String value = rs.getString(i);

            if(key.equals(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_USERNAME_FIELD_NAME))) {
                userRepresentation.setUsername(value);
            }
            else if(key.equals(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_PASSWORD_FIELD_NAME)) || key.equals("password") ||
                    key.equals(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_FIRST_NAME_FIELD_NAME)) ||
                    key.equals(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_LAST_NAME_FIELD_NAME)) ||
                    key.equals(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_EMAIL_FIELD_NAME))) {
                continue;
            }
//            when the key is a user attribute --> it will be saved in this format user.attribute.gender when actually the db column was only gender
            else
                userRepresentation.setSingleAttribute(key, value);
        }
        userRepresentation.setSingleAttribute("firstName", rs.getString(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_FIRST_NAME_FIELD_NAME)));
        userRepresentation.setSingleAttribute("lastName", rs.getString(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_LAST_NAME_FIELD_NAME)));
        userRepresentation.setSingleAttribute("email", rs.getString(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_EMAIL_FIELD_NAME)));

        log.info(String.valueOf(userRepresentation.getAttributes()));
        return userRepresentation;
    }

    @Override
    public UserModel addUser(RealmModel realmModel, String s) {
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realmModel, UserModel userModel) {
        log.warn(String.format("TODO: %s has been deleted", userModel.getUsername()));
        return true;
    }

    protected UserModel createAdapter(RealmModel realm, ResultSet rs) {
        return new AbstractUserAdapter(ksession, realm, model) {
            @Override
            public String getUsername() {
                try {
                    return rs.getString(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_USERNAME_FIELD_NAME));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public String getFirstName() {
                try {
                    return rs.getString(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_FIRST_NAME_FIELD_NAME));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public String getLastName() {
                try {
                    return rs.getString(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_LAST_NAME_FIELD_NAME));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public String getEmail() {
                try {
                    return rs.getString(model.get(CustomUserStorageProviderConstants.CONFIG_KEY_USER_EMAIL_FIELD_NAME));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }
}
