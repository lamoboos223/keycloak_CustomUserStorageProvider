package gov.twk.auth.provider.user;


import lombok.extern.slf4j.Slf4j;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import javax.management.relation.Role;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class UserRepresentation extends AbstractUserAdapterFederatedStorage {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;



    public UserRepresentation(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel) {
        super(session, realm, storageProviderModel);
    }

    @Override
    public String getId() {
        return StorageId.keycloakId(storageProviderModel, username);
    }

    @Override
    public List<String> getAttribute(String name) {
        return super.getAttribute(name);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        super.setSingleAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        super.removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        super.setAttribute(name, values);
    }

    @Override
    public String getFirstAttribute(String name) {
        return super.getFirstAttribute(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attrs = super.getAttributes();
        MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
        all.putAll(attrs);
        return all;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    protected Set<RoleModel> getRoleMappingsInternal() {
        try {
            if (getRoles() != null) {
                return getRoles().stream().map(roleName -> new UserRoleModel(roleName, realm)).collect(Collectors.toSet());
            }
            return Set.of();
        }catch (Exception e){
            log.error("[ROLE Mapping Exception] " + e.getMessage());
        }
        return null;
    }
}