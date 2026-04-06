import db.AdminDAO.Admin;
import db.DBConnection;
import db.StudentDAO;
import db.StudentDAO.Student;
import export.exportUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────
 *  StudentDashboard.java  –  Phase 3: JTable Student Viewer
 * ─────────────────────────────────────────────────────────────
 *
 *  WHAT THIS CLASS TEACHES:
 *  ─────────────────────────
 *  1. JTable + DefaultTableModel   → display tabular data
 *  2. TableRowSorter               → click column headers to sort
 *  3. RowFilter                    → live search / filter rows
 *  4. ListSelectionListener        → detect row clicks
 *  5. Custom cell renderers        → colour-code rows and cells
 *  6. Edit dialog (JOptionPane)    → update a student record
 *  7. Stats panel                  → live counts from DB
 *  8. SwingWorker                  → async DB load (no UI freeze)
 *
 *  KEY CONCEPT — JTable Architecture:
 *  ────────────────────────────────────
 *  JTable does NOT store data itself. It delegates to a TableModel.
 *
 *    JTable          ← handles rendering & user interaction
 *       │
 *       └─ DefaultTableModel   ← stores the actual data (rows/columns)
 *               │
 *               └─ TableRowSorter  ← sorts & filters WITHOUT touching DB
 *
 *  This means sorting/filtering is done in-memory — very fast.
 *  Only the initial load hits the database.
 *
 *  FLOW:
 *  ──────
 *  LoginForm → StudentRegistrationForm → [Dashboard button] → StudentDashboard
 *                                                                   │
 *                                             loadStudents() called on open
 *                                             SwingWorker fetches from MySQL
 *                                             populateTable() fills JTable
 *                                             Search bar → RowFilter applied
 *                                             Edit/Delete buttons act on selection
 */
public class StudentDashboard extends JFrame {

    // ── DAO & Session ────────────────────────────────────────
    private final StudentDAO studentDAO     = new StudentDAO();
    private final Admin      loggedInAdmin;

    // ── Table Components ─────────────────────────────────────
    private JTable            studentTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;

    // ── Toolbar Components ───────────────────────────────────
    private JTextField searchField;
    private JButton    refreshButton, addNewButton, editButton,
                       deleteButton, logoutButton,
                       exportCSVButton, exportPDFButton;
    private JLabel     statusLabel, totalLabel, filteredLabel;

    // ── Stats Labels ─────────────────────────────────────────
    private JLabel statTotalLabel, statCSLabel, statITLabel,
                   statMechLabel,  statCivilLabel, statECELabel,
                   statElecLabel;

    // ── Column names shown in the JTable header ──────────────
    private static final String[] COLUMNS = {
        "ID", "Full Name", "Email", "Phone",
        "Gender", "Branch", "Year", "Newsletter", "Registered At"
    };

    // ─────────────────────────────────────────────────────────
    public StudentDashboard(Admin admin) {
        this.loggedInAdmin = admin;
        applyNimbusLookAndFeel();
        initComponents();
        attachEventListeners();
        configureFrame();
        loadStudents();   // fetch data from DB on open
    }

    // ── Nimbus L&F ───────────────────────────────────────────
    private void applyNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
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

    // ─────────────────────────────────────────────────────────
    //  BUILD UI
    // ─────────────────────────────────────────────────────────
    private void initComponents() {
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    // ── Header ───────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(42, 94, 170));
        header.setBorder(new EmptyBorder(10, 15, 10, 15));

        // Left: title
        JLabel title = new JLabel("📋  Student Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        // Right: admin info + logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JLabel adminLabel = new JLabel("👤 " + loggedInAdmin.fullName);
        adminLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        adminLabel.setForeground(new Color(200, 215, 240));

        logoutButton = new JButton("Logout");
        styleSmallButton(logoutButton, new Color(190, 50, 50));
        rightPanel.add(adminLabel);
        rightPanel.add(logoutButton);

        header.add(title,      BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // ── Center (Stats + Toolbar + Table) ─────────────────────
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBorder(new EmptyBorder(10, 12, 5, 12));
        center.setBackground(new Color(240, 243, 250));

        center.add(buildStatsPanel(), BorderLayout.NORTH);
        center.add(buildToolbar(),    BorderLayout.CENTER);  // search + buttons

        // Table panel wraps toolbar + table together
        JPanel tableWrapper = new JPanel(new BorderLayout(0, 6));
        tableWrapper.setOpaque(false);
        tableWrapper.add(buildToolbar2(), BorderLayout.NORTH);  // action buttons
        tableWrapper.add(buildTable(),    BorderLayout.CENTER);

        center.add(tableWrapper, BorderLayout.SOUTH);
        return center;
    }

    // ── Stats Panel (card-style counts) ──────────────────────
    /**
     * Shows summary cards at the top of the dashboard.
     * Each card shows a count from the database.
     * Updated whenever loadStudents() is called.
     */
    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 7, 8, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 6, 0));

        statTotalLabel = createStatCard("Total",    "0", new Color(42, 94, 170));
        statCSLabel    = createStatCard("Comp Sci", "0", new Color(60, 140, 80));
        statITLabel    = createStatCard("IT",        "0", new Color(130, 80, 180));
        statMechLabel  = createStatCard("Mech",      "0", new Color(200, 120, 30));
        statCivilLabel = createStatCard("Civil",     "0", new Color(170, 60, 60));
        statECELabel   = createStatCard("ECE",       "0", new Color(30, 150, 170));
        statElecLabel  = createStatCard("Electrical","0", new Color(100, 130, 60));

        panel.add(statTotalLabel);
        panel.add(statCSLabel);
        panel.add(statITLabel);
        panel.add(statMechLabel);
        panel.add(statCivilLabel);
        panel.add(statECELabel);
        panel.add(statElecLabel);
        return panel;
    }

    /**
     * Creates a single stat card — a small colored panel with a title + number.
     * We return the count JLabel so we can update it later when data loads.
     */
    private JLabel createStatCard(String title, String count, Color color) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
        card.setBackground(color);
        card.setBorder(new EmptyBorder(8, 10, 8, 10));
        card.setPreferredSize(new Dimension(100, 55));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLbl.setForeground(new Color(220, 230, 255));

        JLabel countLbl = new JLabel(count, SwingConstants.CENTER);
        countLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        countLbl.setForeground(Color.WHITE);
        countLbl.setName(title); // tag for identification

        card.add(titleLbl);
        card.add(countLbl);

        // We need to add the card to a wrapper to return the label
        // The label is what gets updated — so we return it
        // But we also need the card in the layout — handled by caller
        // Trick: embed card as client property of the label
        countLbl.putClientProperty("card", card);
        return countLbl;
    }

    // Override buildStatsPanel to properly add cards
    // (Replaced above approach with a cleaner panel-based one)

    // ── Toolbar (Search bar) ──────────────────────────────────
    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(4, 0, 4, 0));

        // Search
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchPanel.setOpaque(false);

        JLabel searchIcon = new JLabel("🔍  Search:");
        searchIcon.setFont(new Font("Segoe UI", Font.BOLD, 13));

        searchField = new JTextField(28);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(280, 32));
        searchField.setToolTipText("Search by name, email, or branch...");

        JButton clearBtn = new JButton("✖");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearBtn.setPreferredSize(new Dimension(32, 32));
        clearBtn.setFocusPainted(false);
        clearBtn.setToolTipText("Clear search");
        clearBtn.addActionListener(e -> searchField.setText(""));

        searchPanel.add(searchIcon);
        searchPanel.add(searchField);
        searchPanel.add(clearBtn);

        // Right side: total + filtered counts
        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        countPanel.setOpaque(false);
        totalLabel    = new JLabel("Total: 0");
        filteredLabel = new JLabel("Showing: 0");
        totalLabel   .setFont(new Font("Segoe UI", Font.BOLD, 12));
        filteredLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        totalLabel   .setForeground(new Color(42, 94, 170));
        filteredLabel.setForeground(new Color(100, 100, 120));
        countPanel.add(totalLabel);
        countPanel.add(filteredLabel);

        toolbar.add(searchPanel, BorderLayout.WEST);
        toolbar.add(countPanel,  BorderLayout.EAST);
        return toolbar;
    }

    // ── Toolbar 2 (Action buttons) ────────────────────────────
    private JPanel buildToolbar2() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setOpaque(false);

        refreshButton   = new JButton("🔄 Refresh");
        addNewButton    = new JButton("➕ Add New");
        editButton      = new JButton("✏ Edit");
        deleteButton    = new JButton("🗑 Delete");
        exportCSVButton = new JButton("📄 Export CSV");
        exportPDFButton = new JButton("📑 Export PDF");

        styleButton(refreshButton,   new Color(42, 94, 170),   Color.WHITE);
        styleButton(addNewButton,    new Color(42, 140, 70),   Color.WHITE);
        styleButton(editButton,      new Color(200, 140, 40),  Color.WHITE);
        styleButton(deleteButton,    new Color(190, 50,  50),  Color.WHITE);
        styleButton(exportCSVButton, new Color(30, 140, 120),  Color.WHITE);
        styleButton(exportPDFButton, new Color(120, 60, 170),  Color.WHITE);

        // Edit and Delete are disabled until a row is selected
        editButton  .setEnabled(false);
        deleteButton.setEnabled(false);

        toolbar.add(refreshButton);
        toolbar.add(addNewButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);

        // Visual separator
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(2, 28));
        sep.setForeground(new Color(180, 190, 210));
        toolbar.add(sep);

        toolbar.add(exportCSVButton);
        toolbar.add(exportPDFButton);
        return toolbar;
    }

    // ── Table ─────────────────────────────────────────────────
    /**
     * JTABLE SETUP EXPLAINED:
     * ────────────────────────
     * 1. DefaultTableModel  → stores data, prevents direct cell editing
     * 2. TableRowSorter     → enables column header click to sort
     * 3. Custom renderer    → alternating row colors + center alignment
     * 4. Column widths      → set preferred widths for readability
     * 5. Selection model    → SINGLE_SELECTION (one row at a time)
     */
    private JScrollPane buildTable() {
        // DefaultTableModel with column names
        // Override isCellEditable() → returns false = table is READ-ONLY
        // Users must use the Edit button to modify data
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // prevent inline editing — use Edit button instead
            }
        };

        studentTable = new JTable(tableModel);
        studentTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        studentTable.setRowHeight(28);
        studentTable.setShowGrid(true);
        studentTable.setGridColor(new Color(210, 215, 230));
        studentTable.setSelectionBackground(new Color(180, 205, 250));
        studentTable.setSelectionForeground(Color.BLACK);
        studentTable.setFillsViewportHeight(true);

        // SINGLE_SELECTION: only one row can be selected at a time
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // TableRowSorter: enables sorting by clicking column headers
        // Works on the model data — does NOT re-query the DB
        rowSorter = new TableRowSorter<>(tableModel);
        studentTable.setRowSorter(rowSorter);

        // Header styling
        JTableHeader header = studentTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(42, 94, 170));
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false); // prevent column drag-reorder

        // Column widths (in pixels)
        int[] colWidths = {45, 150, 180, 100, 65, 160, 80, 80, 140};
        for (int i = 0; i < colWidths.length; i++) {
            studentTable.getColumnModel().getColumn(i)
                        .setPreferredWidth(colWidths[i]);
        }

        // Custom cell renderer: alternating row colors
        studentTable.setDefaultRenderer(Object.class, new AlternatingRowRenderer());

        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setPreferredSize(new Dimension(900, 380));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(180, 190, 210)));
        return scrollPane;
    }

    // ── Footer ───────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(220, 225, 240));
        footer.setBorder(new EmptyBorder(5, 12, 5, 12));

        statusLabel = new JLabel("Ready.");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(new Color(80, 80, 110));

        JLabel phaseLabel = new JLabel("Phase 4 – Double-click a row for details  •  Export CSV/PDF respects active search filter");
        phaseLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        phaseLabel.setForeground(new Color(130, 130, 150));

        footer.add(statusLabel,  BorderLayout.WEST);
        footer.add(phaseLabel,   BorderLayout.EAST);
        return footer;
    }

    // ─────────────────────────────────────────────────────────
    //  EVENT LISTENERS
    // ─────────────────────────────────────────────────────────
    private void attachEventListeners() {

        // ── Live search via DocumentListener + RowFilter ──
        /**
         * HOW SEARCH WORKS:
         * ──────────────────
         * DocumentListener fires on every keystroke.
         * We pass the text to RowFilter.regexFilter() which
         * hides rows that don't match — without removing them from the model.
         * The data stays in memory; only visibility changes.
         *
         * "(?i)" = case-insensitive regex flag
         * -1     = search ALL columns
         */
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate (DocumentEvent e) { applySearch(); }
            public void removeUpdate (DocumentEvent e) { applySearch(); }
            public void changedUpdate(DocumentEvent e) { applySearch(); }
        });

        // ── Row selection → enable/disable Edit & Delete ──
        /**
         * ListSelectionListener fires when the user clicks a row.
         * We enable the Edit and Delete buttons only when a row is selected.
         * getValueIsAdjusting() prevents double-firing during drag selection.
         */
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean rowSelected = studentTable.getSelectedRow() != -1;
                editButton  .setEnabled(rowSelected);
                deleteButton.setEnabled(rowSelected);
            }
        });

        // ── Double-click row → view full details ──
        studentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showStudentDetails();
                }
            }
        });

        // ── Buttons ──
        refreshButton.addActionListener(e -> loadStudents());
        addNewButton .addActionListener(e -> openRegistrationForm());
        editButton   .addActionListener(e -> showEditDialog());
        deleteButton .addActionListener(e -> deleteSelectedStudent());
        logoutButton .addActionListener(e -> handleLogout());

        // ── Export buttons ──
        exportCSVButton.addActionListener(e ->
            exportUtil.exportToCSV(this, tableModel, rowSorter));
        exportPDFButton.addActionListener(e ->
            exportUtil.exportToPDF(this, tableModel, rowSorter));

        // ── Hover effects ──
        addHoverEffect(refreshButton,   new Color(42,94,170),   new Color(30,70,140));
        addHoverEffect(addNewButton,    new Color(42,140,70),   new Color(30,110,55));
        addHoverEffect(editButton,      new Color(200,140,40),  new Color(170,110,20));
        addHoverEffect(deleteButton,    new Color(190,50,50),   new Color(150,30,30));
        addHoverEffect(exportCSVButton, new Color(30,140,120),  new Color(20,110,95));
        addHoverEffect(exportPDFButton, new Color(120,60,170),  new Color(95,40,140));

        // ── Window close ──
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(StudentDashboard.this,
                        "Exit the application?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    DBConnection.closeConnection();
                    System.exit(0);
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  LOAD STUDENTS FROM DB → POPULATE TABLE
    // ─────────────────────────────────────────────────────────
    /**
     * Fetches all students from MySQL and fills the JTable.
     *
     * Uses SwingWorker to keep the UI responsive during DB fetch.
     *
     * JTABLE POPULATION STEPS:
     * 1. tableModel.setRowCount(0)  → clear existing rows
     * 2. For each Student: tableModel.addRow(Object[])
     *    → Object[] maps directly to COLUMNS array by index
     * 3. Update stats labels
     * 4. Update total/filtered count labels
     */
    private void loadStudents() {
        showStatus("Loading students...", new Color(42, 94, 170));
        refreshButton.setEnabled(false);

        new SwingWorker<List<Student>, Void>() {

            @Override
            protected List<Student> doInBackground() throws Exception {
                return studentDAO.getAllStudents();
            }

            @Override
            protected void done() {
                try {
                    List<Student> students = get();

                    // Clear existing table rows
                    tableModel.setRowCount(0);

                    // Add each student as a row
                    for (Student s : students) {
                        tableModel.addRow(new Object[]{
                            s.id,
                            s.name,
                            s.email,
                            s.phone != null ? s.phone : "",
                            s.gender,
                            s.branch,
                            s.year,
                            s.newsletter ? "✔ Yes" : "No",
                            s.registeredAt != null ? s.registeredAt : ""
                        });
                    }

                    // Update stat cards
                    updateStats(students);

                    // Update count labels
                    int total = students.size();
                    totalLabel   .setText("Total: " + total);
                    filteredLabel.setText("Showing: " + total);

                    showStatus("✔ Loaded " + total + " student(s).", new Color(30, 150, 60));

                    // Re-apply any active search filter
                    applySearch();

                } catch (Exception ex) {
                    showStatus("✖ Error loading: " + ex.getMessage(), Color.RED);
                } finally {
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────
    //  SEARCH / FILTER
    // ─────────────────────────────────────────────────────────
    /**
     * Applies a RowFilter to the TableRowSorter.
     *
     * RowFilter.regexFilter(pattern, columns...):
     *   - pattern: regex string to match
     *   - columns: which column indices to search (-1 = all)
     *
     * Setting filter to null removes all filtering (shows all rows).
     *
     * After filtering, we update the "Showing: N" label by counting
     * visible rows using table.getRowCount() (which respects the filter).
     */
    private void applySearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            rowSorter.setRowFilter(null); // remove filter → show all
        } else {
            try {
                // "(?i)" makes it case-insensitive
                // -1 searches all columns
                rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, -1));
            } catch (java.util.regex.PatternSyntaxException ex) {
                // Invalid regex (e.g. user typed "[") — just ignore
                rowSorter.setRowFilter(null);
            }
        }
        // Update the "Showing: N" label
        filteredLabel.setText("Showing: " + studentTable.getRowCount());
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE STATS CARDS
    // ─────────────────────────────────────────────────────────
    /**
     * Counts students per branch and updates the stat card labels.
     * This iterates the in-memory list — no extra DB queries needed.
     */
    private void updateStats(List<Student> students) {
        int total = students.size();
        int cs=0, it=0, mech=0, civil=0, ece=0, elec=0;

        for (Student s : students) {
            switch (s.branch) {
                case "Computer Science"            -> cs++;
                case "Information Technology"      -> it++;
                case "Mechanical"                  -> mech++;
                case "Civil"                       -> civil++;
                case "Electronics & Communication" -> ece++;
                case "Electrical"                  -> elec++;
            }
        }

        statTotalLabel.setText(String.valueOf(total));
        statCSLabel   .setText(String.valueOf(cs));
        statITLabel   .setText(String.valueOf(it));
        statMechLabel .setText(String.valueOf(mech));
        statCivilLabel.setText(String.valueOf(civil));
        statECELabel  .setText(String.valueOf(ece));
        statElecLabel .setText(String.valueOf(elec));
    }

    // ─────────────────────────────────────────────────────────
    //  VIEW FULL STUDENT DETAILS (double-click)
    // ─────────────────────────────────────────────────────────
    private void showStudentDetails() {
        int viewRow = studentTable.getSelectedRow();
        if (viewRow == -1) return;

        // Convert view row → model row (important when sorted/filtered!)
        int modelRow = studentTable.convertRowIndexToModel(viewRow);

        StringBuilder sb = new StringBuilder();
        sb.append("─────────────────────────────\n");
        sb.append("  STUDENT DETAILS\n");
        sb.append("─────────────────────────────\n");
        for (int i = 0; i < COLUMNS.length; i++) {
            Object val = tableModel.getValueAt(modelRow, i);
            sb.append(String.format("  %-16s: %s%n", COLUMNS[i], val != null ? val : ""));
        }
        sb.append("─────────────────────────────");

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setEditable(false);
        area.setBackground(new Color(245, 247, 255));

        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Student Details – ID: " + tableModel.getValueAt(modelRow, 0),
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ─────────────────────────────────────────────────────────
    //  EDIT SELECTED STUDENT
    // ─────────────────────────────────────────────────────────
    /**
     * Opens a dialog pre-filled with the selected student's data.
     * On save, calls studentDAO.updateStudent() to UPDATE the DB row.
     *
     * IMPORTANT: convertRowIndexToModel()
     * ─────────────────────────────────────
     * When a table is sorted or filtered, the VIEW row index differs
     * from the MODEL row index. Always convert before reading model data!
     *
     * Example:
     *   User sorts by name descending.
     *   They click row 0 (visually first).
     *   View row 0 might be model row 7.
     *   tableModel.getValueAt(0, 0) would give WRONG data.
     *   tableModel.getValueAt(modelRow, 0) gives CORRECT data.
     */
    private void showEditDialog() {
        int viewRow = studentTable.getSelectedRow();
        if (viewRow == -1) return;

        int modelRow = studentTable.convertRowIndexToModel(viewRow);

        // Read current values from model
        int    id     = (int)    tableModel.getValueAt(modelRow, 0);
        String name   = (String) tableModel.getValueAt(modelRow, 1);
        String email  = (String) tableModel.getValueAt(modelRow, 2);
        String phone  = (String) tableModel.getValueAt(modelRow, 3);
        String gender = (String) tableModel.getValueAt(modelRow, 4);
        String branch = (String) tableModel.getValueAt(modelRow, 5);
        String year   = (String) tableModel.getValueAt(modelRow, 6);
        boolean newsletter = "✔ Yes".equals(tableModel.getValueAt(modelRow, 7));

        // ── Build edit dialog form ──
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField    nameField   = new JTextField(name,  20);
        JTextField    emailField  = new JTextField(email, 20);
        JTextField    phoneField  = new JTextField(phone, 20);
        JComboBox<String> genderCombo = new JComboBox<>(new String[]{"Male","Female","Other"});
        JComboBox<String> branchCombo = new JComboBox<>(new String[]{
            "Computer Science","Information Technology",
            "Electronics & Communication","Mechanical","Civil","Electrical"});
        JComboBox<String> yearCombo   = new JComboBox<>(new String[]{
            "1st Year","2nd Year","3rd Year","4th Year"});
        JCheckBox newsletterCheck = new JCheckBox("Subscribe to newsletter", newsletter);

        genderCombo.setSelectedItem(gender);
        branchCombo.setSelectedItem(branch);
        yearCombo  .setSelectedItem(year);

        panel.add(new JLabel("Full Name *:")); panel.add(nameField);
        panel.add(new JLabel("Email *:"));     panel.add(emailField);
        panel.add(new JLabel("Phone:"));       panel.add(phoneField);
        panel.add(new JLabel("Gender *:"));    panel.add(genderCombo);
        panel.add(new JLabel("Branch *:"));    panel.add(branchCombo);
        panel.add(new JLabel("Year *:"));      panel.add(yearCombo);
        panel.add(new JLabel("Newsletter:"));  panel.add(newsletterCheck);

        JPanel wrapper = new JPanel(new BorderLayout());
        JLabel header  = new JLabel("  ✏  Editing Student ID: " + id, SwingConstants.LEFT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(new Color(42, 94, 170));
        header.setBorder(new EmptyBorder(0, 0, 10, 0));
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(panel,  BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, wrapper,
                "Edit Student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newName  = nameField.getText().trim();
            String newEmail = emailField.getText().trim();

            if (newName.isEmpty() || newEmail.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and Email are required!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Build updated Student object
            Student updated = new Student(
                    id, newName, newEmail,
                    phoneField.getText().trim(),
                    (String) genderCombo.getSelectedItem(),
                    (String) branchCombo.getSelectedItem(),
                    (String) yearCombo.getSelectedItem(),
                    "",   // address not shown in table — preserve existing
                    newsletterCheck.isSelected(),
                    ""
            );

            // Save via SwingWorker
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() throws Exception {
                    return studentDAO.updateStudent(updated);
                }
                @Override protected void done() {
                    try {
                        if (get()) {
                            showStatus("✔ Student ID " + id + " updated.", new Color(30,150,60));
                            loadStudents(); // refresh table
                        }
                    } catch (Exception ex) {
                        showStatus("✖ Update failed: " + ex.getMessage(), Color.RED);
                    }
                }
            }.execute();
        }
    }

    // ─────────────────────────────────────────────────────────
    //  DELETE SELECTED STUDENT
    // ─────────────────────────────────────────────────────────
    private void deleteSelectedStudent() {
        int viewRow = studentTable.getSelectedRow();
        if (viewRow == -1) return;

        int modelRow = studentTable.convertRowIndexToModel(viewRow);
        int    id   = (int)    tableModel.getValueAt(modelRow, 0);
        String name = (String) tableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete:\n\n" +
                "  ID   : " + id   + "\n" +
                "  Name : " + name + "\n\n" +
                "⚠ This action cannot be undone!",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() throws Exception {
                    return studentDAO.deleteStudent(id);
                }
                @Override protected void done() {
                    try {
                        if (get()) {
                            showStatus("✔ Student '" + name + "' deleted.", new Color(30,150,60));
                            loadStudents(); // refresh table
                        }
                    } catch (Exception ex) {
                        showStatus("✖ Delete failed: " + ex.getMessage(), Color.RED);
                    }
                }
            }.execute();
        }
    }

    // ─────────────────────────────────────────────────────────
    //  OPEN REGISTRATION FORM (Add New)
    // ─────────────────────────────────────────────────────────
    /**
     * Opens the StudentRegistrationForm as a modal-style window.
     * After it closes, we refresh the table to show the new student.
     */
    private void openRegistrationForm() {
        StudentRegistrationForm form = new StudentRegistrationForm(loggedInAdmin);

        // Add a window listener on the registration form:
        // when it closes, refresh our table
        form.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                loadStudents(); // pick up any newly added student
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  LOGOUT
    // ─────────────────────────────────────────────────────────
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            LoginForm.currentAdmin = null;
            dispose();
            SwingUtilities.invokeLater(LoginForm::new);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  CUSTOM CELL RENDERER  –  alternating row colors
    // ─────────────────────────────────────────────────────────
    /**
     * A TableCellRenderer controls how each cell is drawn.
     *
     * DefaultTableCellRenderer already extends JLabel, so we
     * just override getTableCellRendererComponent() to change
     * the background color based on the row index.
     *
     * Even rows → white
     * Odd rows  → very light blue
     * Selected  → uses table's selection color
     */
    private static class AlternatingRowRenderer extends DefaultTableCellRenderer {

        private static final Color ROW_EVEN     = Color.WHITE;
        private static final Color ROW_ODD      = new Color(240, 245, 255);
        private static final Color ROW_SELECTED = new Color(180, 205, 250);

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            // Call super to get the default label component
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setHorizontalAlignment(column == 0 ? CENTER : LEFT); // center ID column

            if (isSelected) {
                setBackground(ROW_SELECTED);
                setForeground(Color.BLACK);
            } else {
                setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                setForeground(Color.BLACK);

                // Special color for "✔ Yes" newsletter column
                if ("✔ Yes".equals(value)) {
                    setForeground(new Color(30, 130, 60));
                }
            }
            return this;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────
    private void showStatus(String msg, Color c) {
        statusLabel.setText(msg);
        statusLabel.setForeground(c);
    }

    private void styleButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg); b.setForeground(fg);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(110, 32));
    }

    private void styleSmallButton(JButton b, Color bg) {
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(72, 28));
    }

    private void addHoverEffect(JButton b, Color normal, Color hover) {
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(hover);  }
            public void mouseExited (MouseEvent e) { if (b.isEnabled()) b.setBackground(normal); }
        });
    }

    private void configureFrame() {
        setTitle("Student Dashboard – " + loggedInAdmin.username);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(960, 600));
        pack();
        setSize(1050, 650);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
