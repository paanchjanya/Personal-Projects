package db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────
 *  StudentDAO.java  –  Data Access Object (DAO Pattern)
 * ─────────────────────────────────────────────────────────────
 *
 *  WHAT IS THE DAO PATTERN?
 *  ─────────────────────────
 *  DAO (Data Access Object) is a design pattern that separates
 *  your database logic from your UI logic.
 *
 *  Without DAO:  Form → directly writes SQL → messy & hard to maintain
 *  With DAO:     Form → calls StudentDAO → StudentDAO handles all SQL
 *
 *  This means:
 *  ✔ Your form code stays clean (no SQL in UI classes)
 *  ✔ All DB logic is in one place — easy to fix/update
 *  ✔ You can reuse the same DAO from multiple forms
 *
 *  WHAT IS A PreparedStatement?
 *  ──────────────────────────────
 *  Instead of building SQL strings like:
 *    "INSERT INTO students VALUES('" + name + "')"  ← DANGEROUS (SQL Injection!)
 *
 *  We use placeholders:
 *    "INSERT INTO students VALUES(?)"               ← SAFE
 *
 *  Then we set the values separately. MySQL treats them as pure
 *  data, not executable code — this prevents SQL Injection attacks.
 *
 *  STUDENT MODEL (inner class):
 *  ─────────────────────────────
 *  A Student object holds all data for one student.
 *  Think of it like a row in the DB represented as a Java object.
 *  This pattern is called a "Model" or "Entity" class.
 */
public class StudentDAO {

    // ─────────────────────────────────────────────
    //  INNER MODEL CLASS  –  represents one student
    // ─────────────────────────────────────────────
    public static class Student {
        public int    id;
        public String name;
        public String email;
        public String phone;
        public String gender;
        public String branch;
        public String year;
        public String address;
        public boolean newsletter;
        public String  registeredAt;  // timestamp from DB

        // Constructor for creating a new student (no id yet — DB auto-assigns it)
        public Student(String name, String email, String phone, String gender,
                       String branch, String year, String address, boolean newsletter) {
            this.name       = name;
            this.email      = email;
            this.phone      = phone;
            this.gender     = gender;
            this.branch     = branch;
            this.year       = year;
            this.address    = address;
            this.newsletter = newsletter;
        }

        // Constructor for reading from DB (includes id and timestamp)
        public Student(int id, String name, String email, String phone, String gender,
                       String branch, String year, String address,
                       boolean newsletter, String registeredAt) {
            this(name, email, phone, gender, branch, year, address, newsletter);
            this.id           = id;
            this.registeredAt = registeredAt;
        }

        @Override
        public String toString() {
            return String.format("Student{id=%d, name='%s', email='%s', branch='%s', year='%s'}",
                    id, name, email, branch, year);
        }
    }

    // ─────────────────────────────────────────────
    //  1. CREATE TABLE  –  run once to set up DB
    // ─────────────────────────────────────────────
    /**
     * Creates the 'students' table if it doesn't already exist.
     *
     * SQL CONCEPTS USED:
     * ──────────────────
     * - INT AUTO_INCREMENT  → DB auto-assigns unique ID (1, 2, 3...)
     * - VARCHAR(n)          → variable-length string, max n characters
     * - TEXT                → long text (for address)
     * - BOOLEAN             → stores true/false (0 or 1 in MySQL)
     * - TIMESTAMP           → stores date + time
     * - DEFAULT CURRENT_TIMESTAMP → auto-fills with current time on INSERT
     * - UNIQUE(email)       → prevents duplicate email registrations
     * - IF NOT EXISTS       → safe to run multiple times, won't error
     */
    public void createTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS students (
                    id             INT AUTO_INCREMENT PRIMARY KEY,
                    name           VARCHAR(100)  NOT NULL,
                    email          VARCHAR(150)  NOT NULL UNIQUE,
                    phone          VARCHAR(15),
                    gender         VARCHAR(10)   NOT NULL,
                    branch         VARCHAR(100)  NOT NULL,
                    year           VARCHAR(20)   NOT NULL,
                    address        TEXT,
                    newsletter     BOOLEAN       DEFAULT FALSE,
                    registered_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
                )
                """;

        // try-with-resources: automatically closes 'stmt' when done (even if exception occurs)
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement()) {

            stmt.executeUpdate(sql);
            System.out.println("✔ Table 'students' is ready.");
        }
    }

    // ─────────────────────────────────────────────
    //  2. INSERT  –  save a new student
    // ─────────────────────────────────────────────
    /**
     * Inserts a new student record into the database.
     *
     * RETURNS: the auto-generated ID assigned by MySQL
     *
     * PREPARED STATEMENT STEPS:
     * 1. Write SQL with '?' placeholders
     * 2. Call ps.setString(1, value) — 1 = first '?', 2 = second, etc.
     * 3. Call ps.executeUpdate() — runs the INSERT
     * 4. Get generated keys — retrieves the auto-assigned ID
     */
    public int saveStudent(Student student) throws SQLException {
        String sql = """
                INSERT INTO students (name, email, phone, gender, branch, year, address, newsletter)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        // RETURN_GENERATED_KEYS tells JDBC to give us back the auto-generated ID
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Set each '?' placeholder (index starts at 1, not 0)
            ps.setString (1, student.name);
            ps.setString (2, student.email);
            ps.setString (3, student.phone);
            ps.setString (4, student.gender);
            ps.setString (5, student.branch);
            ps.setString (6, student.year);
            ps.setString (7, student.address);
            ps.setBoolean(8, student.newsletter);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Insert failed — no rows affected.");
            }

            // Retrieve the auto-generated student ID
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    System.out.println("✔ Student saved with ID: " + newId);
                    return newId;
                }
            }
        } catch (SQLException e) {
            // Check for duplicate email (MySQL error code 1062)
            if (e.getErrorCode() == 1062) {
                throw new SQLException("This email address is already registered!", e);
            }
            throw e;
        }
        return -1;
    }

    // ─────────────────────────────────────────────
    //  3. SELECT ALL  –  fetch all students
    // ─────────────────────────────────────────────
    /**
     * Retrieves all students from the database, ordered by newest first.
     *
     * ResultSet: Think of it like a cursor that points to rows one at a time.
     * rs.next()       → moves to next row, returns false when no more rows
     * rs.getString()  → reads a column value as String
     * rs.getInt()     → reads a column value as int
     * rs.getBoolean() → reads a BOOLEAN column
     */
    public List<Student> getAllStudents() throws SQLException {
        String sql = "SELECT * FROM students ORDER BY registered_at DESC";
        List<Student> students = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             Statement stmt  = con.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            // Loop through each row in the result
            while (rs.next()) {
                students.add(new Student(
                        rs.getInt    ("id"),
                        rs.getString ("name"),
                        rs.getString ("email"),
                        rs.getString ("phone"),
                        rs.getString ("gender"),
                        rs.getString ("branch"),
                        rs.getString ("year"),
                        rs.getString ("address"),
                        rs.getBoolean("newsletter"),
                        rs.getString ("registered_at")
                ));
            }
        }
        System.out.println("✔ Fetched " + students.size() + " student(s) from DB.");
        return students;
    }

    // ─────────────────────────────────────────────
    //  4. SELECT BY ID  –  fetch one student
    // ─────────────────────────────────────────────
    /**
     * Finds a student by their unique ID.
     * Returns null if not found.
     */
    public Student getStudentById(int id) throws SQLException {
        String sql = "SELECT * FROM students WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getInt    ("id"),
                            rs.getString ("name"),
                            rs.getString ("email"),
                            rs.getString ("phone"),
                            rs.getString ("gender"),
                            rs.getString ("branch"),
                            rs.getString ("year"),
                            rs.getString ("address"),
                            rs.getBoolean("newsletter"),
                            rs.getString ("registered_at")
                    );
                }
            }
        }
        return null; // not found
    }

    // ─────────────────────────────────────────────
    //  5. SEARCH  –  find by name or email
    // ─────────────────────────────────────────────
    /**
     * Searches students by name or email (partial match using LIKE).
     *
     * SQL LIKE operator:
     * '%keyword%' → matches anything containing "keyword" anywhere
     * '%keyword'  → ends with keyword
     * 'keyword%'  → starts with keyword
     */
    public List<Student> searchStudents(String keyword) throws SQLException {
        String sql = """
                SELECT * FROM students
                WHERE name LIKE ? OR email LIKE ? OR branch LIKE ?
                ORDER BY name ASC
                """;
        List<Student> students = new ArrayList<>();
        String pattern = "%" + keyword + "%";  // wrap with wildcards

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    students.add(new Student(
                            rs.getInt    ("id"),
                            rs.getString ("name"),
                            rs.getString ("email"),
                            rs.getString ("phone"),
                            rs.getString ("gender"),
                            rs.getString ("branch"),
                            rs.getString ("year"),
                            rs.getString ("address"),
                            rs.getBoolean("newsletter"),
                            rs.getString ("registered_at")
                    ));
                }
            }
        }
        return students;
    }

    // ─────────────────────────────────────────────
    //  6. UPDATE  –  edit a student record
    // ─────────────────────────────────────────────
    /**
     * Updates an existing student's details.
     * The WHERE id = ? ensures we only update the correct row.
     *
     * Returns true if update was successful.
     */
    public boolean updateStudent(Student student) throws SQLException {
        String sql = """
                UPDATE students
                SET name=?, email=?, phone=?, gender=?, branch=?, year=?, address=?, newsletter=?
                WHERE id=?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString (1, student.name);
            ps.setString (2, student.email);
            ps.setString (3, student.phone);
            ps.setString (4, student.gender);
            ps.setString (5, student.branch);
            ps.setString (6, student.year);
            ps.setString (7, student.address);
            ps.setBoolean(8, student.newsletter);
            ps.setInt    (9, student.id);         // WHERE clause — matches by ID

            int rows = ps.executeUpdate();
            System.out.println("✔ Student ID " + student.id + " updated. Rows affected: " + rows);
            return rows > 0;
        }
    }

    // ─────────────────────────────────────────────
    //  7. DELETE  –  remove a student
    // ─────────────────────────────────────────────
    /**
     * Deletes a student by their ID.
     * Returns true if deletion was successful.
     */
    public boolean deleteStudent(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println("✔ Student ID " + id + " deleted. Rows affected: " + rows);
            return rows > 0;
        }
    }

    // ─────────────────────────────────────────────
    //  8. COUNT  –  total number of students
    // ─────────────────────────────────────────────
    /**
     * Returns the total count of students in the DB.
     * Uses SQL aggregate function COUNT(*).
     */
    public int getTotalCount() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM students";

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        }
        return 0;
    }

    // ─────────────────────────────────────────────
    //  9. CHECK DUPLICATE EMAIL
    // ─────────────────────────────────────────────
    /**
     * Checks if an email is already registered.
     * Used for pre-validation before INSERT.
     */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }
}
