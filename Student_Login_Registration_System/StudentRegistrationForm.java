
import db.AdminDAO.Admin;
import db.DBConnection;
import db.StudentDAO;
import db.StudentDAO.Student;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;

/**
 * ─────────────────────────────────────────────────────────────
 *  StudentRegistrationForm.java  –  Phase 2 Update
 * ─────────────────────────────────────────────────────────────
 *
 *  WHAT CHANGED FROM PHASE 1:
 *  ───────────────────────────
 *  ✔ Now receives a logged-in Admin object from LoginForm
 *  ✔ Header shows "Welcome, <AdminName>" and a logout button
 *  ✔ Logout returns the user back to LoginForm
 *  ✔ DB init moved to LoginForm (already done before this opens)
 *
 *  COMPILE & RUN (always via LoginForm now):
 *  ──────────────────────────────────────────
 *  javac -cp .;lib/* db/DBConnection.java db/AdminDAO.java db/StudentDAO.java LoginForm.java StudentRegistrationForm.java
 *  java  -cp .;lib/* LoginForm
 */
public class StudentRegistrationForm extends JFrame {

    private final StudentDAO studentDAO   = new StudentDAO();
    private final Admin      loggedInAdmin;   // ← NEW: who is logged in

    private JTextField     nameField, emailField, phoneField;
    private JPasswordField passwordField;
    private JComboBox<String> branchCombo, yearCombo;
    private JRadioButton   maleRadio, femaleRadio, otherRadio;
    private JCheckBox      termsCheck, newsletterCheck;
    private JTextArea      addressArea;
    private JButton        submitButton, resetButton, cancelButton, logoutButton, dashboardButton;
    private JLabel         statusLabel;
    private JProgressBar   progressBar;

    // ── Constructor now takes the logged-in Admin ──
    public StudentRegistrationForm(Admin admin) {
        this.loggedInAdmin = admin;
        initComponents();
        attachEventListeners();
        configureFrame();
    }

    // ── Build UI ─────────────────────────────────────────────
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // ── Header with welcome message + logout ──
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(42, 94, 170));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("Student Registration Form", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        // Welcome + logout panel on the right
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel("👤 " + loggedInAdmin.fullName);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        welcomeLabel.setForeground(new Color(200, 215, 240));

        logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        logoutButton.setBackground(new Color(190, 50, 50));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutButton.setPreferredSize(new Dimension(75, 28));

        rightPanel.add(welcomeLabel);
        rightPanel.add(logoutButton);

        headerPanel.add(titleLabel,  BorderLayout.CENTER);
        headerPanel.add(rightPanel,  BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // ── Main form panel ──
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Personal Info
        addSectionTitle(mainPanel, gbc, "Personal Information", 0);
        addLabel(mainPanel, gbc, "Full Name *", 1, 0);
        nameField = new JTextField(20);
        addComponent(mainPanel, gbc, nameField, 1, 1);

        addLabel(mainPanel, gbc, "Email Address *", 2, 0);
        emailField = new JTextField(20);
        addComponent(mainPanel, gbc, emailField, 2, 1);

        addLabel(mainPanel, gbc, "Phone Number", 3, 0);
        phoneField = new JTextField(20);
        addComponent(mainPanel, gbc, phoneField, 3, 1);

        addLabel(mainPanel, gbc, "Password *", 4, 0);
        passwordField = new JPasswordField(20);
        addComponent(mainPanel, gbc, passwordField, 4, 1);

        addLabel(mainPanel, gbc, "Gender *", 5, 0);
        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        genderPanel.setOpaque(false);
        ButtonGroup bg = new ButtonGroup();
        maleRadio   = new JRadioButton("Male");
        femaleRadio = new JRadioButton("Female");
        otherRadio  = new JRadioButton("Other");
        bg.add(maleRadio); bg.add(femaleRadio); bg.add(otherRadio);
        genderPanel.add(maleRadio); genderPanel.add(femaleRadio); genderPanel.add(otherRadio);
        addComponent(mainPanel, gbc, genderPanel, 5, 1);

        // Academic Info
        addSectionTitle(mainPanel, gbc, "Academic Information", 6);
        addLabel(mainPanel, gbc, "Branch *", 7, 0);
        branchCombo = new JComboBox<>(new String[]{
            "-- Select Branch --", "Computer Science", "Information Technology",
            "Electronics & Communication", "Mechanical", "Civil", "Electrical"});
        addComponent(mainPanel, gbc, branchCombo, 7, 1);

        addLabel(mainPanel, gbc, "Year of Study *", 8, 0);
        yearCombo = new JComboBox<>(new String[]{
            "-- Select Year --", "1st Year", "2nd Year", "3rd Year", "4th Year"});
        addComponent(mainPanel, gbc, yearCombo, 8, 1);

        addLabel(mainPanel, gbc, "Address", 9, 0);
        addressArea = new JTextArea(3, 20);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        addComponent(mainPanel, gbc, new JScrollPane(addressArea), 9, 1);

        // Preferences
        addSectionTitle(mainPanel, gbc, "Preferences", 10);
        termsCheck      = new JCheckBox("I agree to the Terms & Conditions *");
        newsletterCheck = new JCheckBox("Subscribe to newsletter");
        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 2; mainPanel.add(termsCheck, gbc);
        gbc.gridy = 12; mainPanel.add(newsletterCheck, gbc); gbc.gridwidth = 1;

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Form completion: 0%");
        gbc.gridx = 0; gbc.gridy = 13; gbc.gridwidth = 2; mainPanel.add(progressBar, gbc);
        gbc.gridwidth = 1;

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        submitButton    = new JButton("✔ Submit");
        resetButton     = new JButton("↺ Reset");
        dashboardButton = new JButton("📋 Dashboard");
        cancelButton    = new JButton("✖ Cancel");
        styleButton(submitButton,    new Color(42, 140, 70),   Color.WHITE);
        styleButton(resetButton,     new Color(200, 140, 40),  Color.WHITE);
        styleButton(dashboardButton, new Color(42, 94, 170),   Color.WHITE);
        styleButton(cancelButton,    new Color(190, 50, 50),   Color.WHITE);
        buttonPanel.add(submitButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(dashboardButton);
        buttonPanel.add(cancelButton);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
    }

    // ── Event Listeners ──────────────────────────────────────
    private void attachEventListeners() {
        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            public void insertUpdate (javax.swing.event.DocumentEvent e) { updateProgress(); }
            public void removeUpdate (javax.swing.event.DocumentEvent e) { updateProgress(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateProgress(); }
        };
        nameField.getDocument()    .addDocumentListener(dl);
        emailField.getDocument()   .addDocumentListener(dl);
        phoneField.getDocument()   .addDocumentListener(dl);
        passwordField.getDocument().addDocumentListener(dl);
        addressArea.getDocument()  .addDocumentListener(dl);

        ActionListener gl = e -> updateProgress();
        maleRadio.addActionListener(gl);
        femaleRadio.addActionListener(gl);
        otherRadio.addActionListener(gl);
        branchCombo.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) updateProgress(); });
        yearCombo.addItemListener  (e -> { if (e.getStateChange() == ItemEvent.SELECTED) updateProgress(); });

        termsCheck.addItemListener(e -> {
            updateProgress();
            submitButton.setEnabled(termsCheck.isSelected());
            showStatus(termsCheck.isSelected() ? "Terms accepted ✔" : "You must accept Terms & Conditions.",
                       termsCheck.isSelected() ? new Color(30,150,60) : Color.RED);
        });

        emailField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String t = emailField.getText().trim();
                boolean ok = t.isEmpty() || t.matches("^[\\w.%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
                emailField.setBackground(ok ? UIManager.getColor("TextField.background") : new Color(255,220,220));
                if (!ok) showStatus("Invalid email format!", Color.RED);
            }
            public void focusGained(FocusEvent e) {
                emailField.setBackground(UIManager.getColor("TextField.background"));
            }
        });

        phoneField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String t = phoneField.getText().trim();
                boolean ok = t.isEmpty() || t.matches("\\d{10}");
                phoneField.setBackground(ok ? UIManager.getColor("TextField.background") : new Color(255,220,220));
                if (!ok) showStatus("Phone must be exactly 10 digits!", Color.RED);
            }
        });

        passwordField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int len = new String(passwordField.getPassword()).length();
                if      (len == 0) showStatus(" ", Color.BLACK);
                else if (len <  6) showStatus("Password strength: Weak 🔴",   Color.RED);
                else if (len < 10) showStatus("Password strength: Medium 🟡", new Color(200,130,0));
                else               showStatus("Password strength: Strong 🟢", new Color(30,150,60));
            }
        });

        addHoverEffect(submitButton,    new Color(42,140,70),  new Color(30,110,55));
        addHoverEffect(resetButton,     new Color(200,140,40), new Color(170,110,20));
        addHoverEffect(dashboardButton, new Color(42,94,170),  new Color(30,70,140));
        addHoverEffect(cancelButton,    new Color(190,50,50),  new Color(150,30,30));

        submitButton.addActionListener(e -> handleSubmit());
        resetButton .addActionListener(e -> handleReset());
        cancelButton.addActionListener(e -> confirmExit());

        // ── Dashboard button → open StudentDashboard ──
        dashboardButton.addActionListener(e -> {
            new StudentDashboard(loggedInAdmin);
        });

        // ── Logout → back to LoginForm ──
        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to logout?", "Confirm Logout",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                LoginForm.currentAdmin = null;  // clear session
                dispose();                      // close this window
                SwingUtilities.invokeLater(LoginForm::new); // back to login
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { confirmExit(); }
        });

        submitButton.setEnabled(false);
    }

    // ── Submit → MySQL ────────────────────────────────────────
    private void handleSubmit() {
        if (!validateForm()) return;
        submitButton.setEnabled(false);
        showStatus("Saving to database...", new Color(42, 94, 170));

        Student student = new Student(
                nameField.getText().trim(),
                emailField.getText().trim(),
                phoneField.getText().trim(),
                getSelectedGender(),
                (String) branchCombo.getSelectedItem(),
                (String) yearCombo.getSelectedItem(),
                addressArea.getText().trim(),
                newsletterCheck.isSelected()
        );

        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                if (studentDAO.emailExists(student.email))
                    throw new SQLException("This email is already registered!");
                return studentDAO.saveStudent(student);
            }
            @Override protected void done() {
                try {
                    int id = get();
                    showStatus("✔ Saved! Student ID: " + id, new Color(30, 150, 60));
                    JOptionPane.showMessageDialog(StudentRegistrationForm.this,
                            "Registration Successful!\n\n" +
                            "Student ID : " + id + "\n" +
                            "Name       : " + student.name + "\n" +
                            "Email      : " + student.email + "\n" +
                            "Branch     : " + student.branch + "\n" +
                            "Registered by: " + loggedInAdmin.fullName,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    handleReset();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    showStatus("✖ " + msg, Color.RED);
                    JOptionPane.showMessageDialog(StudentRegistrationForm.this,
                            "Failed to save:\n" + msg, "Error", JOptionPane.ERROR_MESSAGE);
                    submitButton.setEnabled(termsCheck.isSelected());
                }
            }
        }.execute();
    }

    // ── Validation ───────────────────────────────────────────
    private boolean validateForm() {
        if (nameField.getText().trim().isEmpty())    { showError("Full Name is required.", nameField);     return false; }
        if (emailField.getText().trim().isEmpty())   { showError("Email is required.", emailField);        return false; }
        if (!emailField.getText().trim().matches("^[\\w.%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$"))
                                                     { showError("Enter a valid email.", emailField);      return false; }
        if (!phoneField.getText().trim().isEmpty() &&
            !phoneField.getText().trim().matches("\\d{10}"))
                                                     { showError("Phone: 10 digits only.", phoneField);   return false; }
        if (new String(passwordField.getPassword()).length() < 8)
                                                     { showError("Password: min 8 chars.", passwordField); return false; }
        if (!maleRadio.isSelected() && !femaleRadio.isSelected() && !otherRadio.isSelected())
                                                     { showStatus("Select gender.", Color.RED);            return false; }
        if (branchCombo.getSelectedIndex() == 0)     { showStatus("Select branch.", Color.RED);            return false; }
        if (yearCombo.getSelectedIndex() == 0)       { showStatus("Select year.", Color.RED);              return false; }
        return true;
    }

    // ── Reset ────────────────────────────────────────────────
    private void handleReset() {
        nameField.setText(""); emailField.setText(""); phoneField.setText("");
        passwordField.setText(""); addressArea.setText("");
        maleRadio.setSelected(false); femaleRadio.setSelected(false); otherRadio.setSelected(false);
        branchCombo.setSelectedIndex(0); yearCombo.setSelectedIndex(0);
        termsCheck.setSelected(false); newsletterCheck.setSelected(false);
        progressBar.setValue(0); progressBar.setString("Form completion: 0%");
        submitButton.setEnabled(false);
        showStatus("Form reset.", new Color(100, 100, 100));
    }

    // ── Helpers ──────────────────────────────────────────────
    private void confirmExit() {
        if (JOptionPane.showConfirmDialog(this, "Exit application?", "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            DBConnection.closeConnection();
            System.exit(0);
        }
    }

    private void updateProgress() {
        int f = 0;
        if (!nameField.getText().trim().isEmpty()) f++;
        if (!emailField.getText().trim().isEmpty()) f++;
        if (new String(passwordField.getPassword()).length() >= 8) f++;
        if (maleRadio.isSelected() || femaleRadio.isSelected() || otherRadio.isSelected()) f++;
        if (branchCombo.getSelectedIndex() > 0) f++;
        if (yearCombo.getSelectedIndex() > 0) f++;
        if (termsCheck.isSelected()) f++;
        int pct = (int)((f / 7.0) * 100);
        progressBar.setValue(pct);
        progressBar.setString("Form completion: " + pct + "%");
        progressBar.setForeground(pct < 40 ? Color.RED : pct < 75 ? new Color(200,140,0) : new Color(42,170,90));
    }

    private void showStatus(String msg, Color c) { statusLabel.setText(msg); statusLabel.setForeground(c); }
    private void showError(String msg, JComponent f) { showStatus(msg, Color.RED); f.requestFocus(); }
    private String getSelectedGender() {
        if (maleRadio.isSelected())   return "Male";
        if (femaleRadio.isSelected()) return "Female";
        if (otherRadio.isSelected())  return "Other";
        return "Not specified";
    }

    private void addSectionTitle(JPanel p, GridBagConstraints g, String t, int r) {
        g.gridx=0; g.gridy=r; g.gridwidth=2;
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(new Color(42,94,170));
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0,new Color(42,94,170)),
                BorderFactory.createEmptyBorder(8,0,4,0)));
        p.add(l, g); g.gridwidth=1;
    }
    private void addLabel(JPanel p, GridBagConstraints g, String t, int r, int c) {
        g.gridx=c; g.gridy=r;
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(l, g);
    }
    private void addComponent(JPanel p, GridBagConstraints g, JComponent c, int r, int col) {
        g.gridx=col; g.gridy=r; g.weightx=1.0; p.add(c, g); g.weightx=0;
    }
    private void styleButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg); b.setForeground(fg);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(120, 36));
    }
    private void addHoverEffect(JButton b, Color n, Color h) {
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(h); }
            public void mouseExited (MouseEvent e) { b.setBackground(n); }
        });
    }
    private void configureFrame() {
        setTitle("Student Registration – Logged in as: " + loggedInAdmin.username);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(540, 670));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
