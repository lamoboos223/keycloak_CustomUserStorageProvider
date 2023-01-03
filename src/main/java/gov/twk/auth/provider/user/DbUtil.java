package gov.twk.auth.provider.user;

import java.sql.*;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.component.ComponentModel;

import static gov.twk.auth.provider.user.CustomUserStorageProviderConstants.*;

@Slf4j
public class DbUtil {

    public static Connection getConnection(ComponentModel config){
        try {
            String dbConnectionUrl = null;
            String ip = config.get(CONFIG_KEY_DB_IP);
            String port = config.get(CONFIG_KEY_DB_PORT);
            String dbName = config.get(CONFIG_KEY_DB_NAME);

            switch (config.get(CONFIG_KEY_DB_TYPE)) {
                case ("MSSQL"):
                    dbConnectionUrl = String.format("jdbc:sqlserver://%s:%s;databaseName=%s;", ip, port, dbName);
                    break;
                case ("MySQL"):
                    dbConnectionUrl = String.format("jdbc:mysql://%s:%s/%s", ip, port, dbName);
                    break;
                case ("Postgres"):
                    dbConnectionUrl = String.format("jdbc:postgresql://%s:%s/%s", ip, port, dbName);
                    break;
            }

            return DriverManager.getConnection(dbConnectionUrl,
                    config.get(CONFIG_KEY_DB_USERNAME),
                    config.get(CONFIG_KEY_DB_PASSWORD));
        }catch (SQLException e){
//            log.error(e.getMessage());
        }
        return null;
    }
}
