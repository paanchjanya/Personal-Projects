package export;



import db.StudentDAO.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────
 *  ExportUtil.java  –  Phase 4: CSV & PDF Export
 * ─────────────────────────────────────────────────────────────
 *
 *  WHAT THIS CLASS TEACHES:
 *  ─────────────────────────
 *  1. CSV Export  →  FileWriter + BufferedWriter (pure Java, no libs)
 *  2. PDF Export  →  java.awt.print + Graphics2D (pure Java, no libs)
 *  3. JFileChooser →  save dialog with file type filter
 *  4. SwingWorker  →  async file writing (no UI freeze)
 *  5. Desktop.open →  open the exported file automatically
 *
 *  WHY NO EXTERNAL LIBRARY FOR PDF?
 *  ──────────────────────────────────
 *  Libraries like iText or Apache PDFBox are excellent for complex
 *  PDFs, but require additional JARs. Since you already have the
 *  MySQL JAR to manage, we use Java's built-in:
 *
 *    java.awt.print.PrinterJob  → manages the print/PDF pipeline
 *    java.awt.Graphics2D        → draws text, lines, rectangles
 *    javax.print                → routes output to a PDF file
 *
 *  This produces a clean, professional PDF with:
 *    ✔ Styled header with app title and timestamp
 *    ✔ Column headers with background color
 *    ✔ Alternating row shading
 *    ✔ Footer with page numbers
 *    ✔ Auto-pagination (multiple pages if needed)
 *
 * ─────────────────────────────────────────────────────────────
 *  CSV FORMAT EXPLAINED:
 * ─────────────────────────────────────────────────────────────
 *  CSV = Comma Separated Values.  Each row is a line of text.
 *  Each column value is separated by a comma.
 *
 *  Example:
 *    ID,Name,Email,Branch
 *    1,"John Doe",john@mail.com,Computer Science
 *    2,"Jane, Smith",jane@mail.com,IT
 *
 *  IMPORTANT: if a value contains a comma, we wrap it in quotes.
 *  This is the standard RFC 4180 CSV format.
 *
 *  Opens in: Microsoft Excel, Google Sheets, LibreOffice Calc
 */
public class exportUtil {

    // ── Column headers for export ──
    private static final String[] COLUMNS = {
        "ID", "Full Name", "Email", "Phone",
        "Gender", "Branch", "Year", "Newsletter", "Registered At"
    };

    // ── Timestamp format for filenames ──
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TIMESTAMP =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ─────────────────────────────────────────────────────────
    //  1. EXPORT TO CSV
    // ─────────────────────────────────────────────────────────

    /**
     * Exports visible (filtered) table rows to a CSV file.
     *
     * WHY USE THE TABLE MODEL instead of re-querying the DB?
     * ────────────────────────────────────────────────────────
     * The dashboard may have an active search filter.
     * Exporting from the TableModel respects that filter —
     * "what you see is what you get" in the export.
     *
     * BUFFEREDWRITER:
     * ────────────────
     * FileWriter writes character by character — slow for large files.
     * BufferedWriter wraps it and accumulates characters in a buffer,
     * flushing to disk in one shot — much faster.
     *
     * @param parent     parent frame (for dialog positioning)
     * @param tableModel the JTable's model (source of data)
     * @param rowSorter  the row sorter (to respect active filter)
     */
    public static void exportToCSV(JFrame parent,
                                    DefaultTableModel tableModel,
                                    javax.swing.table.TableRowSorter<?> rowSorter) {

        // ── Step 1: Show JFileChooser save dialog ──
        JFileChooser chooser = createFileChooser("CSV Files (*.csv)", "csv");
        chooser.setSelectedFile(new File("students_" +
                LocalDateTime.now().format(FILE_TIMESTAMP) + ".csv"));

        int choice = chooser.showSaveDialog(parent);
        if (choice != JFileChooser.APPROVE_OPTION) return;

        File file = enforceExtension(chooser.getSelectedFile(), "csv");

        // ── Step 2: Confirm overwrite if file exists ──
        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(parent,
                    "File already exists. Overwrite?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        // ── Step 3: Write file on background thread ──
        final File finalFile = file;
        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                // UTF-8 with BOM — ensures Excel opens it correctly
                // BOM = Byte Order Mark = 3 special bytes at file start
                // Without BOM, Excel may garble characters like é, ñ, etc.
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(finalFile), StandardCharsets.UTF_8))) {

                    // Write UTF-8 BOM for Excel compatibility
                    writer.write('\uFEFF');

                    // ── Write header row ──
                    writer.write(String.join(",", COLUMNS));
                    writer.newLine();

                    // ── Write data rows ──
                    // Use rowSorter to get only VISIBLE (filtered) rows
                    // in their current SORTED order
                    int rowCount = (rowSorter != null)
                            ? rowSorter.getViewRowCount()
                            : tableModel.getRowCount();

                    for (int viewRow = 0; viewRow < rowCount; viewRow++) {
                        // Convert view index → model index (respects sort/filter)
                        int modelRow = (rowSorter != null)
                                ? rowSorter.convertRowIndexToModel(viewRow)
                                : viewRow;

                        StringBuilder line = new StringBuilder();
                        for (int col = 0; col < COLUMNS.length; col++) {
                            if (col > 0) line.append(",");
                            Object val = tableModel.getValueAt(modelRow, col);
                            line.append(escapeCSV(val != null ? val.toString() : ""));
                        }
                        writer.write(line.toString());
                        writer.newLine();
                    }

                    // ── Write summary footer ──
                    writer.newLine();
                    writer.write("# Exported by Student Management System");
                    writer.newLine();
                    writer.write("# Date: " + LocalDateTime.now().format(DISPLAY_TIMESTAMP));
                    writer.newLine();
                    writer.write("# Total Records: " + rowCount);
                    writer.newLine();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // re-throws any exception from doInBackground
                    showSuccessDialog(parent, finalFile, "CSV");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(parent,
                            "Failed to export CSV:\n" + ex.getCause().getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────
    //  2. EXPORT TO PDF  (Pure Java — no external library)
    // ─────────────────────────────────────────────────────────

    /**
     * Exports the student table to a multi-page PDF file.
     *
     * HOW PURE JAVA PDF WORKS:
     * ─────────────────────────
     * Java's print API uses a "Printable" interface.
     * We implement print(Graphics g, PageFormat pf, int pageIndex)
     * which is called once per page.
     *
     * We route the output to a file (not a printer) using:
     *   PrintService → finds the "Microsoft Print to PDF" service
     *   PrinterJob.setPageable() → our Printable implementation
     *   job.print(attributes) → writes bytes to the file
     *
     * Graphics2D lets us:
     *   g2d.drawString()    → write text
     *   g2d.fillRect()      → draw filled rectangles (header BG)
     *   g2d.drawLine()      → draw separator lines
     *   g2d.setFont()       → change font size/style
     *   g2d.setColor()      → change drawing color
     *
     * PAGINATION:
     * ────────────
     * We calculate how many rows fit per page based on row height.
     * print() is called with pageIndex = 0, 1, 2...
     * Return Printable.PAGE_EXISTS to print the page.
     * Return Printable.NO_SUCH_PAGE when all pages are done.
     */
    public static void exportToPDF(JFrame parent,
                                    DefaultTableModel tableModel,
                                    javax.swing.table.TableRowSorter<?> rowSorter) {

        // ── Step 1: Show JFileChooser ──
        JFileChooser chooser = createFileChooser("PDF Files (*.pdf)", "pdf");
        chooser.setSelectedFile(new File("students_" +
                LocalDateTime.now().format(FILE_TIMESTAMP) + ".pdf"));

        int choice = chooser.showSaveDialog(parent);
        if (choice != JFileChooser.APPROVE_OPTION) return;

        File file = enforceExtension(chooser.getSelectedFile(), "pdf");

        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(parent,
                    "File already exists. Overwrite?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        // ── Step 2: Collect visible rows into a 2D String array ──
        // We snapshot the visible data NOW (before background thread runs)
        // because Swing components are not thread-safe
        int rowCount = (rowSorter != null)
                ? rowSorter.getViewRowCount()
                : tableModel.getRowCount();

        String[][] data = new String[rowCount][COLUMNS.length];
        for (int viewRow = 0; viewRow < rowCount; viewRow++) {
            int modelRow = (rowSorter != null)
                    ? rowSorter.convertRowIndexToModel(viewRow)
                    : viewRow;
            for (int col = 0; col < COLUMNS.length; col++) {
                Object val = tableModel.getValueAt(modelRow, col);
                data[viewRow][col] = val != null ? val.toString() : "";
            }
        }

        final File   finalFile = file;
        final int    totalRows = rowCount;
        final String timestamp = LocalDateTime.now().format(DISPLAY_TIMESTAMP);

        // ── Step 3: Generate PDF on background thread ──
        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                generatePDF(finalFile, data, totalRows, timestamp);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    showSuccessDialog(parent, finalFile, "PDF");
                } catch (Exception ex) {
                    // PDF via PrinterJob requires PDF printer — fallback to HTML
                    try {
                        File htmlFile = new File(
                                finalFile.getParent(),
                                finalFile.getName().replace(".pdf", ".html"));
                        generateHTMLReport(htmlFile, data, totalRows, timestamp);
                        JOptionPane.showMessageDialog(parent,
                                "PDF printer not found on this system.\n\n" +
                                "✔ Exported as styled HTML instead:\n" + htmlFile.getName() +
                                "\n\nYou can open it in a browser and Print → Save as PDF.",
                                "Exported as HTML", JOptionPane.INFORMATION_MESSAGE);
                        openFile(htmlFile);
                    } catch (Exception fallbackEx) {
                        JOptionPane.showMessageDialog(parent,
                                "Export failed:\n" + fallbackEx.getMessage(),
                                "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────
    //  PDF GENERATION  (java.awt.print)
    // ─────────────────────────────────────────────────────────
    private static void generatePDF(File file, String[][] data,
                                     int totalRows, String timestamp) throws Exception {

        // Page layout constants (in points: 1 point = 1/72 inch)
        final int PAGE_W   = 792;  // A4 landscape width  (11 inches)
        final int PAGE_H   = 612;  // A4 landscape height (8.5 inches)
        final int MARGIN   = 36;
        final int COL_W    = (PAGE_W - 2 * MARGIN) / COLUMNS.length;
        final int ROW_H    = 18;
        final int HEADER_H = 70;   // top banner height
        final int COL_HDR_H= 22;   // column header row height
        final int FOOTER_H = 25;
        final int USABLE_H = PAGE_H - MARGIN - HEADER_H - COL_HDR_H - FOOTER_H - MARGIN;
        final int ROWS_PER_PAGE = USABLE_H / ROW_H;
        final int TOTAL_PAGES   = (int) Math.ceil((double) totalRows / ROWS_PER_PAGE);

        // Column widths (proportional — some cols are wider)
        int[] colWidths = {30, 110, 140, 75, 50, 120, 60, 60, 100};

        // Use PrinterJob to write to PDF file
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();

        // Find PDF print service
        javax.print.PrintService pdfService = null;
        for (javax.print.PrintService ps : javax.print.PrintServiceLookup.lookupPrintServices(null, null)) {
            if (ps.getName().toLowerCase().contains("pdf")) {
                pdfService = ps;
                break;
            }
        }
        if (pdfService == null) throw new Exception("No PDF printer found");

        job.setPrintService(pdfService);

        // Page format: landscape A4
        java.awt.print.PageFormat pf = job.defaultPage();
        pf.setOrientation(java.awt.print.PageFormat.LANDSCAPE);
        java.awt.print.Paper paper = new java.awt.print.Paper();
        paper.setSize(PAGE_W, PAGE_H);
        paper.setImageableArea(MARGIN, MARGIN, PAGE_W - 2*MARGIN, PAGE_H - 2*MARGIN);
        pf.setPaper(paper);

        // Build PrintAttributes to route output to file
        javax.print.attribute.HashPrintRequestAttributeSet attrs =
                new javax.print.attribute.HashPrintRequestAttributeSet();
        attrs.add(new javax.print.attribute.standard.Destination(file.toURI()));
        attrs.add(javax.print.attribute.standard.MediaSizeName.ISO_A4);
        attrs.add(javax.print.attribute.standard.OrientationRequested.LANDSCAPE);

        // Implement Printable — called once per page
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex >= TOTAL_PAGES) return java.awt.print.Printable.NO_SUCH_PAGE;

            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int x = MARGIN;
            int y = MARGIN;

            // ── Page Header Banner ──
            g.setColor(new Color(42, 94, 170));
            g.fillRect(x, y, PAGE_W - 2*MARGIN, HEADER_H - 10);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g.drawString("Student Management System", x + 10, y + 22);

            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.drawString("Student Records Report", x + 10, y + 38);
            g.drawString("Generated: " + timestamp, x + 10, y + 52);
            g.drawString("Total Records: " + totalRows +
                         "   |   Page " + (pageIndex + 1) + " of " + TOTAL_PAGES,
                         x + 10, y + 64);

            y += HEADER_H;

            // ── Column Header Row ──
            g.setColor(new Color(230, 235, 245));
            g.fillRect(x, y, PAGE_W - 2*MARGIN, COL_HDR_H);
            g.setColor(new Color(42, 94, 170));
            g.drawRect(x, y, PAGE_W - 2*MARGIN, COL_HDR_H);

            g.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g.setColor(new Color(30, 50, 120));
            int cx = x + 3;
            for (int col = 0; col < COLUMNS.length; col++) {
                g.drawString(COLUMNS[col], cx, y + 15);
                cx += colWidths[col];
            }
            y += COL_HDR_H;

            // ── Data Rows ──
            int startRow = pageIndex * ROWS_PER_PAGE;
            int endRow   = Math.min(startRow + ROWS_PER_PAGE, totalRows);

            for (int row = startRow; row < endRow; row++) {
                // Alternating row background
                if (row % 2 == 0) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(new Color(240, 245, 255));
                }
                g.fillRect(x, y, PAGE_W - 2*MARGIN, ROW_H);

                // Row border
                g.setColor(new Color(210, 215, 230));
                g.drawRect(x, y, PAGE_W - 2*MARGIN, ROW_H);

                // Cell text
                g.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                g.setColor(Color.BLACK);
                cx = x + 3;
                for (int col = 0; col < COLUMNS.length; col++) {
                    String val = data[row][col];
                    // Truncate long text to fit cell
                    if (val.length() > getMaxChars(colWidths[col]))
                        val = val.substring(0, getMaxChars(colWidths[col]) - 1) + "…";
                    // Color "Yes" newsletter green
                    if ("✔ Yes".equals(data[row][col])) {
                        g.setColor(new Color(30, 130, 60));
                    } else {
                        g.setColor(Color.BLACK);
                    }
                    g.drawString(val, cx, y + 13);
                    cx += colWidths[col];
                }
                y += ROW_H;
            }

            // ── Page Footer ──
            int footerY = PAGE_H - MARGIN - 15;
            g.setColor(new Color(180, 190, 210));
            g.drawLine(MARGIN, footerY - 5, PAGE_W - MARGIN, footerY - 5);
            g.setFont(new Font("Segoe UI", Font.ITALIC, 8));
            g.setColor(new Color(120, 130, 150));
            g.drawString("Student Management System  •  Confidential", MARGIN, footerY + 8);
            g.drawString("Page " + (pageIndex + 1) + " / " + TOTAL_PAGES,
                         PAGE_W - MARGIN - 60, footerY + 8);

            return java.awt.print.Printable.PAGE_EXISTS;
        }, pf);

        job.print(attrs);
    }

    // ─────────────────────────────────────────────────────────
    //  HTML FALLBACK REPORT  (if no PDF printer installed)
    // ─────────────────────────────────────────────────────────
    /**
     * Generates a styled HTML report as a fallback when no PDF
     * printer is available on the system.
     *
     * WHY HTML?
     * ──────────
     * HTML is just a text file with markup tags.
     * Any browser can open it, and users can then:
     *   File → Print → Save as PDF
     * to get a proper PDF with full formatting.
     *
     * We use inline CSS (no external stylesheets) so the file
     * is completely self-contained — one file, works anywhere.
     */
    private static void generateHTMLReport(File file, String[][] data,
                                            int totalRows, String timestamp) throws Exception {
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            w.write("<!DOCTYPE html>\n<html lang='en'>\n<head>\n");
            w.write("<meta charset='UTF-8'>\n");
            w.write("<title>Student Report</title>\n");
            w.write("<style>\n");
            w.write("  body { font-family: 'Segoe UI', sans-serif; margin: 30px; color: #222; }\n");
            w.write("  .header { background: #2a5eaa; color: white; padding: 18px 24px; border-radius: 6px; margin-bottom: 20px; }\n");
            w.write("  .header h1 { margin: 0 0 4px; font-size: 22px; }\n");
            w.write("  .header p  { margin: 2px 0; font-size: 13px; opacity: 0.85; }\n");
            w.write("  table { width: 100%; border-collapse: collapse; font-size: 13px; }\n");
            w.write("  th { background: #2a5eaa; color: white; padding: 9px 10px; text-align: left; }\n");
            w.write("  td { padding: 7px 10px; border-bottom: 1px solid #dde; }\n");
            w.write("  tr:nth-child(even) td { background: #f0f5ff; }\n");
            w.write("  tr:hover td { background: #dce8ff; }\n");
            w.write("  .yes { color: #1e8240; font-weight: bold; }\n");
            w.write("  .footer { margin-top: 20px; font-size: 11px; color: #888; border-top: 1px solid #ccc; padding-top: 10px; }\n");
            w.write("  @media print { .no-print { display: none; } }\n");
            w.write("</style>\n</head>\n<body>\n");

            // Header
            w.write("<div class='header'>\n");
            w.write("  <h1>🎓 Student Management System</h1>\n");
            w.write("  <p>Student Records Report</p>\n");
            w.write("  <p>Generated: " + timestamp + " &nbsp;|&nbsp; Total Records: " + totalRows + "</p>\n");
            w.write("</div>\n");

            // Print button (hidden when printing)
            w.write("<div class='no-print' style='margin-bottom:14px;'>\n");
            w.write("  <button onclick='window.print()' style='padding:8px 18px;background:#2a5eaa;color:white;border:none;border-radius:4px;cursor:pointer;font-size:13px;'>🖨 Print / Save as PDF</button>\n");
            w.write("</div>\n");

            // Table
            w.write("<table>\n<thead><tr>\n");
            for (String col : COLUMNS) {
                w.write("  <th>" + col + "</th>\n");
            }
            w.write("</tr></thead>\n<tbody>\n");

            for (String[] row : data) {
                w.write("<tr>\n");
                for (int c = 0; c < row.length; c++) {
                    String val = row[c] != null ? htmlEscape(row[c]) : "";
                    String cls = "✔ Yes".equals(row[c]) ? " class='yes'" : "";
                    w.write("  <td" + cls + ">" + val + "</td>\n");
                }
                w.write("</tr>\n");
            }

            w.write("</tbody>\n</table>\n");
            w.write("<div class='footer'>Student Management System &nbsp;•&nbsp; Confidential &nbsp;•&nbsp; " + timestamp + "</div>\n");
            w.write("</body>\n</html>\n");
        }
    }

    // ─────────────────────────────────────────────────────────
    //  SHARED HELPERS
    // ─────────────────────────────────────────────────────────

    /**
     * Creates a JFileChooser with a specific file type filter.
     * Sets the initial directory to the user's home folder.
     */
    private static JFileChooser createFileChooser(String description, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                description, extension));
        chooser.setDialogTitle("Save Export File");
        return chooser;
    }

    /**
     * Ensures the file has the correct extension.
     * If the user didn't type ".csv" or ".pdf", we add it automatically.
     */
    private static File enforceExtension(File file, String ext) {
        String path = file.getAbsolutePath();
        if (!path.toLowerCase().endsWith("." + ext)) {
            return new File(path + "." + ext);
        }
        return file;
    }

    /**
     * Escapes a value for CSV format (RFC 4180):
     * - If value contains comma, newline, or double-quote → wrap in double-quotes
     * - Any existing double-quotes inside → doubled ("" escaping)
     *
     * Example:
     *   Input:  He said "hello", bye
     *   Output: "He said ""hello"", bye"
     */
    private static String escapeCSV(String value) {
        if (value.contains(",") || value.contains("\"") ||
            value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Escapes special HTML characters to prevent broken HTML output.
     * < → &lt;   > → &gt;   & → &amp;   " → &quot;
     */
    private static String htmlEscape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Estimates max characters that fit in a PDF cell of given pixel width.
     * Approximate: 1 char ≈ 5.5px at font size 8.
     */
    private static int getMaxChars(int pixelWidth) {
        return Math.max(3, (int)(pixelWidth / 5.5));
    }

    /**
     * Shows a success dialog with the file path.
     * Offers to open the file immediately using Desktop.open().
     *
     * Desktop.open() uses the OS default app:
     *   .csv  → Microsoft Excel / Google Sheets
     *   .pdf  → Adobe Reader / browser
     *   .html → default browser
     */
    private static void showSuccessDialog(JFrame parent, File file, String type) {
        int open = JOptionPane.showConfirmDialog(parent,
                "✔ " + type + " exported successfully!\n\n" +
                "File: " + file.getName() + "\n" +
                "Location: " + file.getParent() + "\n\n" +
                "Open the file now?",
                type + " Export Complete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (open == JOptionPane.YES_OPTION) {
            openFile(file);
        }
    }

    /**
     * Opens a file using the operating system's default application.
     * Runs on a background thread to avoid blocking the UI.
     */
    private static void openFile(File file) {
        new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
            } catch (Exception ex) {
                System.err.println("Could not open file: " + ex.getMessage());
            }
        }).start();
    }
}
