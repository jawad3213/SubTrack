package com.subtrack.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Runs at deployment: applies Flyway schema migrations, then seeds categories, config values
 * and the first admin. Uses the DataSource directly (plain JDBC) because CDI injection
 * is not guaranteed to be ready inside a @WebListener.
 */
@WebListener
public class DatabaseInitializer implements ServletContextListener {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DatabaseInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("DatabaseInitializer.contextInitialized called");
        javax.sql.DataSource ds;
        try {
            ds = (javax.sql.DataSource) new javax.naming.InitialContext().lookup("java:global/SubtrackDS");
        } catch (javax.naming.NamingException e) {
            throw new IllegalStateException("DataSource java:global/SubtrackDS not found", e);
        }

        // Schema changes must succeed: let a failed migration fail the deployment.
        migrateSchema(ds);

        try {
            seedData(ds, sce);
            LOGGER.info("DatabaseInitializer: initialization completed successfully");
        } catch (Exception e) {
            LOGGER.warn("DatabaseInitializer: initialization FAILED", e);
            LOGGER.error("Unexpected error", e);
        }
    }

    /**
     * Applies src/main/resources/db/migration. Databases created earlier by
     * hibernate.hbm2ddl.auto=update already contain the V1 tables, so they are baselined at 1.
     */
    private void migrateSchema(javax.sql.DataSource ds) {
        var result = org.flywaydb.core.Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .load()
            .migrate();
        LOGGER.info("Flyway: applied " + result.migrationsExecuted + " migration(s), schema version " + result.targetSchemaVersion);
    }

    private void seedData(javax.sql.DataSource ds, ServletContextEvent sce) throws Exception {
        java.util.Properties dotEnv = loadDotEnv(sce);
        try (java.sql.Connection conn = ds.getConnection()) {
            initSystemConfig(conn, dotEnv);
            initCategories(conn);
            initAdmin(conn, dotEnv);
        }
    }

    private void initCategories(java.sql.Connection conn) throws Exception {
        String[][] categories = {
            {"Work",          "Professional tools and software",        "briefcase",   "#007bff"},
            {"Cloud",         "Cloud storage and services",             "cloud",       "#17a2b8"},
            {"Entertainment", "Streaming and media services",           "play-circle", "#dc3545"},
            {"Productivity",  "Productivity and collaboration tools",   "tasks",       "#28a745"},
            {"Utilities",     "Utility and helper services",            "wrench",      "#6c757d"},
            {"Other",         "Miscellaneous subscriptions",            "folder",      "#ffc107"}
        };
        for (String[] cat : categories) {
            try (java.sql.PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM category WHERE name = ?")) {
                check.setString(1, cat[0]);
                java.sql.ResultSet rs = check.executeQuery();
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (java.sql.PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO category(id, name, description, icon, color, created_at) VALUES (gen_random_uuid(), ?, ?, ?, ?, NOW())")) {
                        ins.setString(1, cat[0]);
                        ins.setString(2, cat[1]);
                        ins.setString(3, cat[2]);
                        ins.setString(4, cat[3]);
                        ins.executeUpdate();
                        LOGGER.info("Created category: " + cat[0]);
                    }
                }
            }
        }
    }

    private java.util.Properties loadDotEnv(ServletContextEvent sce) {
        java.util.Properties dotEnv = new java.util.Properties();
        java.io.File envFile = new java.io.File(sce.getServletContext().getRealPath("/"), "../../.env");
        // Note: in a standard Maven/WildFly layout, the .env is usually 2 levels up from the deployment root during mvn wildfly:run
        if (!envFile.exists()) {
            envFile = new java.io.File(".env"); // Fallback for various run modes
        }

        if (envFile.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(envFile)) {
                dotEnv.load(fis);
            } catch (java.io.IOException e) {
                LOGGER.warn("Failed to load .env file", e);
            }
        }
        return dotEnv;
    }

    private static String envOrDotEnv(String key, java.util.Properties dotEnv) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) value = dotEnv.getProperty(key);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private void initSystemConfig(java.sql.Connection conn, java.util.Properties dotEnv) throws Exception {
        syncConfig(conn, "GOOGLE_OAUTH_CLIENT_ID", envOrDotEnv("GOOGLE_OAUTH_CLIENT_ID", dotEnv));
        syncConfig(conn, "GOOGLE_OAUTH_CLIENT_SECRET", envOrDotEnv("GOOGLE_OAUTH_CLIENT_SECRET", dotEnv));
        syncConfig(conn, "GEMINI_API_KEY", envOrDotEnv("GEMINI_API_KEY", dotEnv));
        syncConfig(conn, "APP_BASE_URL", envOrDotEnv("APP_BASE_URL", dotEnv));
    }

    private void syncConfig(java.sql.Connection conn, String key, String value) throws Exception {
        if (value == null || value.isEmpty() || value.startsWith("YOUR_")) return;

        try (java.sql.PreparedStatement check = conn.prepareStatement(
                "SELECT config_value FROM system_config WHERE config_key = ?")) {
            check.setString(1, key);
            java.sql.ResultSet rs = check.executeQuery();
            if (!rs.next()) {
                try (java.sql.PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO system_config (config_key, config_value, updated_at) VALUES (?, ?, NOW())")) {
                    ins.setString(1, key);
                    ins.setString(2, value);
                    ins.executeUpdate();
                    LOGGER.info("Set system config: " + key);
                }
            } else {
                String existingValue = rs.getString(1);
                if (!value.equals(existingValue)) {
                    try (java.sql.PreparedStatement upd = conn.prepareStatement(
                            "UPDATE system_config SET config_value = ?, updated_at = NOW() WHERE config_key = ?")) {
                        upd.setString(1, value);
                        upd.setString(2, key);
                        upd.executeUpdate();
                        LOGGER.info("Synchronized config from environment: " + key);
                    }
                }
            }
        }
    }

    /**
     * Creates the first admin account only when the database has no ADMIN yet, using
     * SUBTRACK_ADMIN_EMAIL / SUBTRACK_ADMIN_PASSWORD. Existing accounts are never modified,
     * so an admin's changed password survives restarts.
     */
    private void initAdmin(java.sql.Connection conn, java.util.Properties dotEnv) throws Exception {
        try (java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM client WHERE role = 'ADMIN'")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }

        String email = envOrDotEnv("SUBTRACK_ADMIN_EMAIL", dotEnv);
        String password = envOrDotEnv("SUBTRACK_ADMIN_PASSWORD", dotEnv);
        if (email == null || password == null) {
            LOGGER.warn("No admin account exists. Set SUBTRACK_ADMIN_EMAIL and SUBTRACK_ADMIN_PASSWORD to create one.");
            return;
        }
        if (!PasswordUtil.isPasswordStrong(password)) {
            LOGGER.warn("SUBTRACK_ADMIN_PASSWORD is too weak (min 8 chars, upper, lower, digit). Admin not created.");
            return;
        }
        email = email.toLowerCase();

        try (java.sql.PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM client WHERE email = ?")) {
            check.setString(1, email);
            try (java.sql.ResultSet rs = check.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    LOGGER.warn("SUBTRACK_ADMIN_EMAIL '" + email + "' already belongs to a non-admin account. Admin not created.");
                    return;
                }
            }
        }

        try (java.sql.PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO client(id, email, password, first_name, last_name, role, account_type, is_active, created_at, updated_at) " +
                "VALUES (gen_random_uuid(), ?, ?, 'System', 'Admin', 'ADMIN', 'B2C', true, NOW(), NOW())")) {
            ins.setString(1, email);
            ins.setString(2, PasswordUtil.hashPassword(password));
            ins.executeUpdate();
            LOGGER.info("Created initial admin account: " + email);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
