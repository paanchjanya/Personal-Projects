
import db.AdminDAO;
import db.AdminDAO.Admin;
import db.DBConnection;
import db.StudentDAO;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * ─────────────────────────────────────────────────────────────
 *  LoginForm.java  –  Phase 2: Admin Login Window
 * ─────────────────────────────────────────────────────────────
 *
 *  FLOW OF THE APPLICATION (Phase 2 onwards):
 *  ────────────────────────────────────────────
 *
 *    App starts
 *        │
 *        ▼
 *    LoginForm  (this file)
 *        │
 *        ├── Wrong credentials  →  show error, stay on LoginForm
 *        │
 *        └── Correct credentials
 *                │
 *                ▼
 *            StudentRegistrationForm  (existing)
 *                │
 *                └── (Phase 3) → StudentDashboard
 *
 *  SESSION MANAGEMENT:
 *  ────────────────────
 *  When login succeeds, we store the logged-in Admin object in a
 *  static variable called "currentAdmin". This is a simple form
 *  of session management — any class can call:
 *      LoginForm.currentAdmin.fullName
 *  to know who is logged in, just like a web session variable.
 *
 *  FEATURES:
 *  ──────────
 *  ✔ SHA-256 + Salt password verification via AdminDAO
 *  ✔ Show/Hide password toggle
 *  ✔ Login attempt counter (locks after 5 failed attempts)
 *  ✔ SwingWorker for non-blocking DB verification
 *  ✔ Enter key submits the form
 *  ✔ Animated shake effect on wrong password
 *  ✔ Change Password dialog
 *  ✔ Nimbus Look & Feel with custom colors
 */
public class LoginForm extends JFrame {

    // ── Session: stores the currently logged-in admin ──
    // Static so any other class can access it: LoginForm.currentAdmin
    public static Admin currentAdmin = null;

    // ── DAO instances ──
    private final AdminDAO   adminDAO   = new AdminDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    // ── UI Components ──
    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JButton        loginButton;
    private JButton        showHideButton;
    private JLabel         statusLabel;
    private JLabel         attemptsLabel;
    private JCheckBox      rememberCheck;

    // ── State ──
    private int  failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 5;
    private boolean passwordVisible = false;

    public LoginForm() {
        applyNimbusLookAndFeel();
        initDatabase();
        initComponents();
        attachEventListeners();
        configureFrame();
    }

    // ── DB Init ──────────────────────────────────────────────
    /**
     * Sets up both 'admins' and 'students' tables on app startup.
     * Also creates the default admin if none exists.
     */
    private void initDatabase() {
        try {
            adminDAO.createTableAndDefaultAdmin();
            studentDAO.createTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Database Error!\n\n" + e.getMessage() +
                    "\n\nPlease check:\n" +
                    "  1. MySQL server is running\n" +
                    "  2. Credentials in DBConnection.java are correct\n" +
                    "  3. mysql-connector-j.jar is in lib/ folder",
                    "DB Connection Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Nimbus L&F ───────────────────────────────────────────
    private void applyNimbusLookAndFeel() {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    UIManager.put("nimbusBase",                new Color(42, 94, 170));
                    UIManager.put("nimbusBlueGrey",            new Color(100, 120, 160));
                    UIManager.put("control",                   new Color(230, 235, 245));
                    UIManager.put("nimbusSelectionBackground", new Color(60, 120, 210));
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    // ── Build UI ─────────────────────────────────────────────
    private void initComponents() {
        setLayout(new BorderLayout());

        // ── Top banner ──
        JPanel banner = new JPanel(new GridLayout(2, 1, 0, 4));
        banner.setBackground(new Color(42, 94, 170));
        banner.setBorder(new EmptyBorder(25, 30, 20, 30));

        JLabel appTitle = new JLabel("🎓 Student Management System", SwingConstants.CENTER);
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        appTitle.setForeground(Color.WHITE);

        JLabel subTitle = new JLabel("Admin Login Portal", SwingConstants.CENTER);
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subTitle.setForeground(new Color(200, 215, 240));

        banner.add(appTitle);
        banner.add(subTitle);
        add(banner, BorderLayout.NORTH);

        // ── Center card ──
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(25, 40, 10, 40),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(8, 5, 8, 5);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;

        // Lock icon label
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel lockLabel = new JLabel("🔐  Sign in to your account", SwingConstants.CENTER);
        lockLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lockLabel.setForeground(new Color(50, 50, 80));
        lockLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(lockLabel, gbc);
        gbc.gridwidth = 1;

        // Username label
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userLabel.setForeground(new Color(70, 70, 100));
        card.add(userLabel, gbc);

        // Username field
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setPreferredSize(new Dimension(300, 38));
        usernameField.setToolTipText("Enter your admin username");
        card.add(usernameField, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0;

        // Password label
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        passLabel.setForeground(new Color(70, 70, 100));
        card.add(passLabel, gbc);

        // Password field + show/hide button in a panel
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0;
        JPanel passPanel = new JPanel(new BorderLayout(5, 0));
        passPanel.setOpaque(false);
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(250, 38));
        passwordField.setToolTipText("Enter your password");

        showHideButton = new JButton("👁");
        showHideButton.setPreferredSize(new Dimension(42, 38));
        showHideButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        showHideButton.setFocusPainted(false);
        showHideButton.setToolTipText("Show / Hide password");
        showHideButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        passPanel.add(passwordField,  BorderLayout.CENTER);
        passPanel.add(showHideButton, BorderLayout.EAST);
        card.add(passPanel, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0;

        // Remember me + Forgot password row
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.setOpaque(false);
        rememberCheck = new JCheckBox("Remember me");
        rememberCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rememberCheck.setOpaque(false);

        JButton forgotBtn = new JButton("<HTML><U>Change Password</U></HTML>");
        forgotBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        forgotBtn.setForeground(new Color(42, 94, 170));
        forgotBtn.setBorderPainted(false);
        forgotBtn.setContentAreaFilled(false);
        forgotBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotBtn.addActionListener(e -> showChangePasswordDialog());

        optionsPanel.add(rememberCheck, BorderLayout.WEST);
        optionsPanel.add(forgotBtn,     BorderLayout.EAST);
        card.add(optionsPanel, gbc);
        gbc.gridwidth = 1;

        // Login button
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.weightx = 1.0;
        loginButton = new JButton("Login  →");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginButton.setBackground(new Color(42, 94, 170));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(300, 42));
        card.add(loginButton, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0;

        // Status label (errors / success messages)
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        card.add(statusLabel, gbc);

        // Attempts label
        gbc.gridy = 8;
        attemptsLabel = new JLabel(" ", SwingConstants.CENTER);
        attemptsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        attemptsLabel.setForeground(new Color(180, 60, 60));
        card.add(attemptsLabel, gbc);

        add(card, BorderLayout.CENTER);

        // ── Footer ──
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(new Color(240, 243, 250));
        footer.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel footerLabel = new JLabel("Student Management System  •  Phase 2");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setForeground(new Color(130, 130, 150));
        footer.add(footerLabel);
        add(footer, BorderLayout.SOUTH);
    }

    // ── Event Listeners ──────────────────────────────────────
    private void attachEventListeners() {

        // Login button click
        loginButton.addActionListener(e -> attemptLogin());

        // Enter key on either field triggers login
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) attemptLogin();
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);

        // Show/Hide password toggle
        showHideButton.addActionListener(e -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                // Show password as plain text
                passwordField.setEchoChar((char) 0);
                showHideButton.setText("🙈");
                showHideButton.setToolTipText("Hide password");
            } else {
                // Hide with bullet character
                passwordField.setEchoChar('•');
                showHideButton.setText("👁");
                showHideButton.setToolTipText("Show password");
            }
            passwordField.requestFocus();
        });

        // Hover effect on login button
        loginButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (loginButton.isEnabled())
                    loginButton.setBackground(new Color(30, 70, 140));
            }
            @Override public void mouseExited(MouseEvent e) {
                if (loginButton.isEnabled())
                    loginButton.setBackground(new Color(42, 94, 170));
            }
        });

        // Clear error status when user starts typing again
        usernameField.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) { clearStatus(); }
        });
        passwordField.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) { clearStatus(); }
        });

        // Window close
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                DBConnection.closeConnection();
                System.exit(0);
            }
        });
    }

    // ── Login Logic ──────────────────────────────────────────
    /**
     * Handles login attempt with:
     *  1. Basic input validation
     *  2. Lockout check (max 5 attempts)
     *  3. SwingWorker for async DB verification
     *  4. Success → open StudentRegistrationForm
     *  5. Failure → shake animation + increment counter
     */
    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // ── Input validation ──
        if (username.isEmpty()) {
            showStatus("Please enter your username.", Color.RED);
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showStatus("Please enter your password.", Color.RED);
            passwordField.requestFocus();
            return;
        }

        // ── Lockout check ──
        if (failedAttempts >= MAX_ATTEMPTS) {
            showStatus("Account locked! Too many failed attempts.", new Color(180, 0, 0));
            loginButton.setEnabled(false);
            return;
        }

        // ── Disable UI during verification ──
        loginButton.setEnabled(false);
        loginButton.setText("Verifying...");
        showStatus("Checking credentials...", new Color(42, 94, 170));

        // ── Background DB verification via SwingWorker ──
        new SwingWorker<Admin, Void>() {

            @Override
            protected Admin doInBackground() throws Exception {
                // This runs on a background thread — safe to do DB work here
                return adminDAO.verifyLogin(username, password);
            }

            @Override
            protected void done() {
                // This runs back on the EDT — safe to update UI here
                try {
                    Admin admin = get();

                    if (admin != null) {
                        // ✔ LOGIN SUCCESS
                        currentAdmin = admin;  // store in session
                        showStatus("✔ Welcome, " + admin.fullName + "!", new Color(30, 150, 60));

                        // Small delay so user sees the welcome message
                        Timer timer = new Timer(800, evt -> openMainApp(admin));
                        timer.setRepeats(false);
                        timer.start();

                    } else {
                        // ✖ LOGIN FAILED
                        failedAttempts++;
                        int remaining = MAX_ATTEMPTS - failedAttempts;

                        shakeWindow();   // visual feedback

                        if (remaining > 0) {
                            showStatus("✖ Invalid username or password.", Color.RED);
                            attemptsLabel.setText("⚠ " + remaining + " attempt(s) remaining before lockout");
                        } else {
                            showStatus("🔒 Account locked after " + MAX_ATTEMPTS + " failed attempts.", new Color(180, 0, 0));
                            attemptsLabel.setText("Contact your system administrator to reset.");
                        }

                        passwordField.setText("");
                        passwordField.requestFocus();

                        // Re-enable button only if not locked
                        loginButton.setEnabled(failedAttempts < MAX_ATTEMPTS);
                        loginButton.setText("Login  →");
                    }

                } catch (Exception ex) {
                    showStatus("DB Error: " + ex.getCause().getMessage(), Color.RED);
                    loginButton.setEnabled(true);
                    loginButton.setText("Login  →");
                }
            }
        }.execute();
    }

    // ── Open Main App ────────────────────────────────────────
    /**
     * Called on successful login.
     * Closes the LoginForm and opens StudentRegistrationForm.
     * Passes the logged-in admin info to the registration form.
     */
    private void openMainApp(Admin admin) {
        dispose(); // close login window
        // Open the registration form, passing the logged-in admin
        SwingUtilities.invokeLater(() -> new StudentRegistrationForm(admin));
    }

    // ── Change Password Dialog ───────────────────────────────
    /**
     * Shows a dialog for changing the admin password.
     * Verifies old password before allowing the change.
     */
    private void showChangePasswordDialog() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField    userField    = new JTextField(15);
        JPasswordField oldPassField = new JPasswordField(15);
        JPasswordField newPassField = new JPasswordField(15);
        JPasswordField confPassField= new JPasswordField(15);

        panel.add(new JLabel("Username:"));       panel.add(userField);
        panel.add(new JLabel("Current Password:")); panel.add(oldPassField);
        panel.add(new JLabel("New Password:"));   panel.add(newPassField);
        panel.add(new JLabel("Confirm New:"));    panel.add(confPassField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String uname   = userField.getText().trim();
            String oldPass = new String(oldPassField.getPassword());
            String newPass = new String(newPassField.getPassword());
            String confPass= new String(confPassField.getPassword());

            if (uname.isEmpty() || oldPass.isEmpty() || newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!newPass.equals(confPass)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (newPass.length() < 8) {
                JOptionPane.showMessageDialog(this, "New password must be at least 8 characters!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                boolean changed = adminDAO.changePassword(uname, oldPass, newPass);
                if (changed) {
                    JOptionPane.showMessageDialog(this,
                            "✔ Password changed successfully!\nPlease login with your new password.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "✖ Incorrect username or current password.",
                            "Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Shake Animation ──────────────────────────────────────
    /**
     * Shakes the window left-right on failed login.
     * This gives immediate visual feedback that the login failed.
     *
     * Uses javax.swing.Timer to schedule repeated small movements.
     * Each tick moves the window ±10px horizontally, then restores.
     */
    private void shakeWindow() {
        Point originalLocation = getLocation();
        int   shakeDistance    = 10;
        int   shakeDuration    = 30; // ms per move
        int[] moves            = {-shakeDistance, shakeDistance, -shakeDistance,
                                   shakeDistance, -shakeDistance, shakeDistance, 0};
        final int[] idx = {0};

        Timer shakeTimer = new Timer(shakeDuration, null);
        shakeTimer.addActionListener(e -> {
            if (idx[0] < moves.length) {
                setLocation(originalLocation.x + moves[idx[0]], originalLocation.y);
                idx[0]++;
            } else {
                setLocation(originalLocation); // restore exact original position
                ((Timer) e.getSource()).stop();
            }
        });
        shakeTimer.start();
    }

    // ── Helpers ──────────────────────────────────────────────
    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private void clearStatus() {
        statusLabel.setText(" ");
    }

    // ── Frame Config ─────────────────────────────────────────
    private void configureFrame() {
        setTitle("Login – Student Management System");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        pack();
        setMinimumSize(new Dimension(420, 480));
        setSize(420, 500);
        setLocationRelativeTo(null);
        setVisible(true);

        // Auto-focus username field
        SwingUtilities.invokeLater(() -> usernameField.requestFocusInWindow());
    }

    // ── Entry Point ──────────────────────────────────────────
    /**
     * main() is now in LoginForm — this is the app's entry point.
     * LoginForm opens first; on success it opens StudentRegistrationForm.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginForm::new);
    }
}
