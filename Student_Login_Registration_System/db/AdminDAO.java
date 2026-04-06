package db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

/**
 * ─────────────────────────────────────────────────────────────
 *  AdminDAO.java  –  Admin Authentication & DB Operations
 * ─────────────────────────────────────────────────────────────
 *
 *  WHAT THIS CLASS HANDLES:
 *  ─────────────────────────
 *  1. Creating the 'admins' table in MySQL
 *  2. Hashing passwords using SHA-256 + Salt
 *  3. Verifying login credentials
 *  4. Creating the default admin account
 *  5. Changing passwords
 *
 * ─────────────────────────────────────────────────────────────
 *  WHAT IS PASSWORD HASHING?
 * ─────────────────────────────────────────────────────────────
 *  NEVER store plain text passwords in a database!
 *  If your DB is hacked, all passwords are exposed.
 *
 *  Instead, we HASH the password — a one-way transformation:
 *    "mypassword123"  →  SHA-256  →  "ef92b778bafe771..."
 *
 *  One-way means: you CANNOT reverse the hash back to the password.
 *  To verify: hash the input again and compare the two hashes.
 *
 * ─────────────────────────────────────────────────────────────
 *  WHAT IS A SALT?
 * ─────────────────────────────────────────────────────────────
 *  Problem with plain hashing:
 *    Two users with password "abc123" would have the SAME hash.
 *    Hackers use "rainbow tables" (pre-computed hash lists) to
 *    reverse common passwords.
 *
 *  Solution — add a SALT:
 *    A salt is a random string added to the password before hashing.
 *    "abc123" + "xK9#mP2"  →  unique hash every time
 *
 *  We store the salt alongside the hash in the DB.
 *  On login: retrieve salt → hash(inputPassword + salt) → compare
 *
 *  STORED FORMAT in DB:
 *    password_hash  =  "ef92b778bafe771..."   (SHA-256 result)
 *    password_salt  =  "xK9#mP2qL..."         (random 16 bytes, Base64)
 */
public class AdminDAO {

    // ── Inner model class for Admin ──
    public static class Admin {
        public int    id;
        public String username;
        public String fullName;
        public String email;
        public String createdAt;

        public Admin(int id, String username, String fullName, String email, String createdAt) {
            this.id        = id;
            this.username  = username;
            this.fullName  = fullName;
            this.email     = email;
            this.createdAt = createdAt;
        }

        @Override
        public String toString() {
            return String.format("Admin{id=%d, username='%s', name='%s'}", id, username, fullName);
        }
    }

    // ─────────────────────────────────────────────
    //  1. CREATE TABLE
    // ─────────────────────────────────────────────
    /**
     * Creates the 'admins' table if it doesn't exist.
     * Also inserts a default admin account on first run.
     *
     * DEFAULT CREDENTIALS (change after first login!):
     *   Username : admin
     *   Password : Admin@123
     */
    public void createTableAndDefaultAdmin() throws SQLException {
        String createSQL = """
                CREATE TABLE IF NOT EXISTS admins (
                    id             INT AUTO_INCREMENT PRIMARY KEY,
                    username       VARCHAR(50)   NOT NULL UNIQUE,
                    full_name      VARCHAR(100)  NOT NULL,
                    email          VARCHAR(150),
                    password_hash  VARCHAR(255)  NOT NULL,
                    password_salt  VARCHAR(100)  NOT NULL,
                    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement()) {

            stmt.executeUpdate(createSQL);
            System.out.println("✔ Table 'admins' is ready.");

            // Insert default admin only if table is empty
            if (getAdminCount() == 0) {
                createAdmin("admin", "Administrator", "admin@studentapp.com", "Admin@123");
                System.out.println("✔ Default admin created → username: admin | password: Admin@123");
            }
        }
    }

    // ─────────────────────────────────────────────
    //  2. PASSWORD HASHING  (SHA-256 + Salt)
    // ─────────────────────────────────────────────

    /**
     * Generates a cryptographically random salt.
     *
     * SecureRandom is stronger than Random — it uses OS-level
     * entropy sources (mouse movements, timing, etc.) to generate
     * truly unpredictable random bytes.
     *
     * We encode to Base64 so it's safe to store as a String in DB.
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];   // 16 bytes = 128-bit salt
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /**
     * Hashes a password with a given salt using SHA-256.
     *
     * SHA-256 PROCESS:
     *  1. Combine: password + salt  →  "Admin@123xK9#mP2..."
     *  2. Convert to bytes          →  byte array
     *  3. MessageDigest.digest()    →  32-byte hash
     *  4. Convert to hex string     →  "ef92b778bafe771a..."
     *
     * The result is always 64 characters (32 bytes × 2 hex chars each).
     *
     * @param password  plain text password
     * @param salt      random salt string
     * @return          64-character hex hash string
     */
    public String hashPassword(String password, String salt) {
        try {
            // Get SHA-256 MessageDigest instance
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Combine password + salt before hashing
            String saltedPassword = password + salt;

            // Perform the hash — returns raw bytes
            byte[] hashBytes = md.digest(saltedPassword.getBytes());

            // Convert bytes to hex string for DB storage
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                // "%02x" formats each byte as 2-digit hex (e.g., 0x0F → "0f")
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in standard Java — this should never happen
            throw new RuntimeException("SHA-256 algorithm not available!", e);
        }
    }

    // ─────────────────────────────────────────────
    //  3. CREATE ADMIN
    // ─────────────────────────────────────────────
    /**
     * Creates a new admin account with a hashed password.
     *
     * FLOW:
     *  1. Generate a random salt
     *  2. Hash the plain text password with that salt
     *  3. Store username, hash, and salt in DB
     *  (Plain text password is NEVER stored)
     */
    public void createAdmin(String username, String fullName,
                             String email, String plainPassword) throws SQLException {
        String salt = generateSalt();
        String hash = hashPassword(plainPassword, salt);

        String sql = """
                INSERT INTO admins (username, full_name, email, password_hash, password_salt)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setString(4, hash);   // store hash, NOT plain password
            ps.setString(5, salt);   // store salt for future verification

            ps.executeUpdate();
            System.out.println("✔ Admin '" + username + "' created successfully.");
        }
    }

    // ─────────────────────────────────────────────
    //  4. VERIFY LOGIN
    // ─────────────────────────────────────────────
    /**
     * Verifies login credentials.
     *
     * VERIFICATION FLOW:
     *  1. Fetch the stored hash + salt for the given username
     *  2. Hash the input password using the SAME stored salt
     *  3. Compare: if hash(inputPassword + storedSalt) == storedHash → valid!
     *
     * Returns the Admin object if valid, null if invalid.
     * This is safe — we never compare plain text passwords.
     *
     * @param username      entered username
     * @param plainPassword entered plain text password
     * @return              Admin object if credentials match, null otherwise
     */
    public Admin verifyLogin(String username, String plainPassword) throws SQLException {
        String sql = "SELECT * FROM admins WHERE username = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Retrieve stored hash and salt from DB
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("password_salt");

                    // Hash the entered password with the SAME salt
                    String inputHash = hashPassword(plainPassword, storedSalt);

                    // Compare hashes (never compare plain text!)
                    if (inputHash.equals(storedHash)) {
                        // Credentials match — return the Admin object
                        return new Admin(
                                rs.getInt   ("id"),
                                rs.getString("username"),
                                rs.getString("full_name"),
                                rs.getString("email"),
                                rs.getString("created_at")
                        );
                    }
                }
            }
        }
        return null; // login failed
    }

    // ─────────────────────────────────────────────
    //  5. CHANGE PASSWORD
    // ─────────────────────────────────────────────
    /**
     * Changes an admin's password.
     * Generates a NEW salt and rehashes the new password.
     *
     * @param username        admin's username
     * @param oldPassword     current password (for verification)
     * @param newPassword     new password to set
     * @return                true if changed successfully
     */
    public boolean changePassword(String username,
                                   String oldPassword,
                                   String newPassword) throws SQLException {
        // First verify the old password
        if (verifyLogin(username, oldPassword) == null) {
            return false; // old password incorrect
        }

        // Generate fresh salt and hash for new password
        String newSalt = generateSalt();
        String newHash = hashPassword(newPassword, newSalt);

        String sql = "UPDATE admins SET password_hash=?, password_salt=? WHERE username=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newHash);
            ps.setString(2, newSalt);
            ps.setString(3, username);

            return ps.executeUpdate() > 0;
        }
    }

    // ─────────────────────────────────────────────
    //  6. HELPERS
    // ─────────────────────────────────────────────
    private int getAdminCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM admins";
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM admins WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
