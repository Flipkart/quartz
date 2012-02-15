package org.quartz.impl.jdbcjobstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;

public final class JdbcQuartzTestUtilities {

    private static final String DATABASE_DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String DATABASE_CONNECTION_PREFIX = "jdbc:derby:memory:";
    private static final List<String> DATABASE_SETUP_STATEMENTS;
    static {
        try {
            Class.forName(DATABASE_DRIVER_CLASS).newInstance();
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        } catch (InstantiationException e) {
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }

        List<String> setup = new ArrayList<String>();
        String setupScript;
        try {
            InputStream setupStream = DerbyEmbeddedConnectionProvider.class
                    .getClassLoader().getResourceAsStream("tables_derby.sql");
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(setupStream, "US-ASCII"));
                StringBuilder sb = new StringBuilder();
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    } else if (!line.startsWith("--")) {
                        sb.append(line).append("\n");
                    }
                }
                setupScript = sb.toString();
            } finally {
                setupStream.close();
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        for (String command : setupScript.split(";")) {
            if (!command.matches("\\s*")) {
                setup.add(command);
            }
        }
        DATABASE_SETUP_STATEMENTS = setup;
    }

    public static void createDatabase(String name) throws SQLException {
        DBConnectionManager.getInstance().addConnectionProvider(name,
                new DerbyEmbeddedConnectionProvider(name));
    }

    public static void destroyDatabase(String name) throws SQLException {
        try {
            DriverManager.getConnection(
                    DATABASE_CONNECTION_PREFIX + name + ";drop=true").close();
        } catch (SQLException e) {
            if (!("Database 'memory:" + name + "' dropped.").equals(e.getMessage())) {
                throw e;
            }
        }
    }

    static class DerbyEmbeddedConnectionProvider implements ConnectionProvider {

        private final String databaseName;

        DerbyEmbeddedConnectionProvider(String name) throws SQLException {
            this.databaseName = name;
            Connection conn = DriverManager.getConnection(DATABASE_CONNECTION_PREFIX + databaseName + ";create=true");
            try {
                Statement statement = conn.createStatement();
                for (String command : DATABASE_SETUP_STATEMENTS) {
                    statement.addBatch(command);
                }
                statement.executeBatch();
            } finally {
                conn.close();
            }
        }

        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(DATABASE_CONNECTION_PREFIX + databaseName);
        }

        public void shutdown() throws SQLException {
            // nothing to do
        }
    }

    private JdbcQuartzTestUtilities() {
        // not instantiable
    }
}
