package com.subtrack.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * DatabaseInitializer uses its own EntityManagerFactory (resource-local)
 * so it works reliably at servlet startup without CDI injection issues.
 * The main app still uses JTA via the container data source.
 */
@WebListener
public class DatabaseInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println(">>> DatabaseInitializer.contextInitialized called");
        // We use a separate persistence unit with RESOURCE_LOCAL for init-time work
        // because CDI @Inject is not guaranteed to be ready inside @WebListener.
        // The application itself uses the JTA persistence unit (subtrackPU).
        try {
            // Attempt initialization via JTA EntityManager from JNDI
            initViaJndi();
            System.out.println(">>> DatabaseInitializer: initialization completed successfully");
        } catch (Exception e) {
            System.err.println(">>> DatabaseInitializer: initialization FAILED - " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void initViaJndi() throws Exception {
        javax.naming.InitialContext ctx = new javax.naming.InitialContext();
        javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup("java:global/SubtrackDS");
        try (java.sql.Connection conn = ds.getConnection()) {
            initCategories(conn);
            initUsers(conn);
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
                        System.out.println(">>> Created category: " + cat[0]);
                    }
                }
            }
        }
    }

    private void initUsers(java.sql.Connection conn) throws Exception {
        // Admin user
        createUserIfNotExists(conn,
            "admin@subtrack.com",
            PasswordUtil.hashPassword("Admin@123!"),
            "System", "Admin",
            "ADMIN", "B2C");

        // Test user
        createUserIfNotExists(conn,
            "user@subtrack.com",
            PasswordUtil.hashPassword("User@123!"),
            "Normal", "User",
            "CLIENT", "B2C");

        // Verify admin account is healthy
        verifyAdminAccount(conn, "admin@subtrack.com", "Admin@123!");
    }

    private void verifyAdminAccount(java.sql.Connection conn, String email, String expectedPassword) throws Exception {
        try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                "SELECT password, is_active, role FROM users WHERE email = ?")) {
            stmt.setString(1, email);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                System.err.println(">>> ADMIN VERIFICATION FAILED: account '" + email + "' does not exist in the database!");
            } else {
                String storedHash = rs.getString("password");
                boolean isActive  = rs.getBoolean("is_active");
                String role       = rs.getString("role");
                boolean pwOk      = PasswordUtil.verifyPassword(expectedPassword, storedHash);

                if (!pwOk) {
                    System.err.println(">>> ADMIN VERIFICATION FAILED: password hash for '" + email + "' is invalid or corrupted! Resetting...");
                    try (java.sql.PreparedStatement upd = conn.prepareStatement(
                            "UPDATE users SET password = ?, is_active = true WHERE email = ?")) {
                        upd.setString(1, PasswordUtil.hashPassword(expectedPassword));
                        upd.setString(2, email);
                        upd.executeUpdate();
                        System.out.println(">>> Admin password hash reset successfully for: " + email);
                    }
                } else if (!isActive) {
                    System.err.println(">>> ADMIN VERIFICATION WARNING: account '" + email + "' exists but is deactivated! Re-activating...");
                    try (java.sql.PreparedStatement upd = conn.prepareStatement(
                            "UPDATE users SET is_active = true WHERE email = ?")) {
                        upd.setString(1, email);
                        upd.executeUpdate();
                        System.out.println(">>> Admin account re-activated: " + email);
                    }
                } else if (!"ADMIN".equals(role)) {
                    System.err.println(">>> ADMIN VERIFICATION FAILED: account '" + email + "' exists but has role '" + role + "' instead of ADMIN!");
                } else {
                    System.out.println(">>> Admin account verified OK: " + email + " | active=" + isActive + " | role=" + role + " | passwordHash=valid");
                }
            }
        }
    }

    private void createUserIfNotExists(java.sql.Connection conn,
            String email, String hashedPassword,
            String firstName, String lastName,
            String role, String accountType) throws Exception {
        try (java.sql.PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE email = ?")) {
            check.setString(1, email);
            java.sql.ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                java.util.UUID userId = java.util.UUID.randomUUID();
                try (java.sql.PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO users(id, email, password, first_name, last_name, role, is_active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, true, NOW(), NOW())")) {
                    ins.setObject(1, userId);
                    ins.setString(2, email);
                    ins.setString(3, hashedPassword);
                    ins.setString(4, firstName);
                    ins.setString(5, lastName);
                    ins.setString(6, role);
                    ins.executeUpdate();
                }

                if ("ADMIN".equals(role)) {
                    try (java.sql.PreparedStatement insAdmin = conn.prepareStatement(
                            "INSERT INTO admin(id) VALUES (?)")) {
                        insAdmin.setObject(1, userId);
                        insAdmin.executeUpdate();
                    }
                } else {
                    try (java.sql.PreparedStatement insClient = conn.prepareStatement(
                            "INSERT INTO client(id, account_type, timezone, email_notifications, telegram_enabled, whatsapp_enabled) " +
                            "VALUES (?, ?, 'UTC', true, false, false)")) {
                        insClient.setObject(1, userId);
                        insClient.setString(2, accountType);
                        insClient.executeUpdate();
                    }
                }
                System.out.println(">>> Created user: " + email);
            } else {
                // Ensure existing admin is active
                if ("ADMIN".equals(role)) {
                    try (java.sql.PreparedStatement upd = conn.prepareStatement(
                            "UPDATE users SET is_active = true WHERE email = ? AND is_active = false")) {
                        upd.setString(1, email);
                        int updated = upd.executeUpdate();
                        if (updated > 0) {
                            System.out.println(">>> Re-activated user: " + email);
                        }
                    }
                }
                System.out.println(">>> User already exists: " + email);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
