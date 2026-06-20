import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.text.DecimalFormat;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class MotorPHGUI extends JFrame {
    
    private static final Color PRIMARY = new Color(52, 152, 219);
    private static final Color PRIMARY_DARK = new Color(41, 128, 185);
    private static final Color BG_DARK = new Color(25, 25, 28);
    private static final Color BG_PANEL = new Color(37, 37, 42);
    private static final Color BG_INPUT = new Color(50, 50, 58);
    private static final Color BG_HOVER = new Color(60, 60, 70);
    private static final Color TEXT_WHITE = new Color(245, 245, 245);
    private static final Color TEXT_LIGHT = new Color(210, 210, 215);
    private static final Color TEXT_DIM = new Color(160, 160, 170);
    private static final Color BORDER = new Color(65, 65, 75);
    private static final Color GREEN = new Color(46, 204, 113);
    private static final Color RED = new Color(231, 76, 60);
    private static final Color TABLE_HDR = new Color(35, 55, 75);
    private static final Color TABLE_ALT = new Color(42, 42, 48);
    
    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    private static final LocalTime GRACE_END = LocalTime.of(8, 10);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat HOURS_FORMAT = new DecimalFormat("#0.00");
    private static final String EMPLOYEES_FILE = "employees.csv";
    private static final String ATTENDANCE_FILE = "attendance.csv";
    private static final String BACKUP_DIR = "backups/";
    private static final String CSV_HEADER = "Employee #,Last Name,First Name,Birthday,Status,Position,Immediate Supervisor,Basic Salary,Gross Semi-monthly Rate,Hourly Rate";
    
    private static final String[] AVAILABLE_MONTHS = {
        "All Months", "June 2024", "July 2024", "August 2024", "September 2024", 
        "October 2024", "November 2024", "December 2024"
    };
    
    private static final Map<String, Integer> MONTH_MAP = new HashMap<>();
    static {
        MONTH_MAP.put("January", 1); MONTH_MAP.put("February", 2);
        MONTH_MAP.put("March", 3); MONTH_MAP.put("April", 4);
        MONTH_MAP.put("May", 5); MONTH_MAP.put("June", 6);
        MONTH_MAP.put("July", 7); MONTH_MAP.put("August", 8);
        MONTH_MAP.put("September", 9); MONTH_MAP.put("October", 10);
        MONTH_MAP.put("November", 11); MONTH_MAP.put("December", 12);
    }
    
    private DefaultTableModel tableModel;
    private JTable employeeTable;
    private List<Employee> employees;
    private Map<String, AttendanceRecord> attendanceMap;
    private File employeesFile;
    private String selectedMonth = "All Months";
    private JComboBox<String> monthComboBox;
    private JLabel statusLabel;
    private JLabel recordCountLabel;
    private JLabel attendanceStatusLabel;
    private JDialog addEditDialog;
    private JTextField searchField;
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog(null);
            loginDialog.setVisible(true);
            if (loginDialog.isAuthenticated()) {
                new MotorPHGUI().setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
    
    public MotorPHGUI() {
        employees = new ArrayList<>();
        attendanceMap = new HashMap<>();
        employeesFile = new File(EMPLOYEES_FILE);
        
        loadData();
        initializeUI();
        refreshTable();
    }
    
    private void loadData() {
        loadEmployees();
        loadAttendanceRecords();
    }
    
    private void loadEmployees() {
        employees.clear();
        if (!employeesFile.exists()) return;
        
        try (BufferedReader br = new BufferedReader(new FileReader(employeesFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    Employee emp = parseEmployeeLine(line);
                    if (emp != null) employees.add(emp);
                } catch (Exception e) {}
            }
        } catch (IOException e) {
            showError("Error", "Could not read employees.csv");
        }
    }
    
    private Employee parseEmployeeLine(String line) {
        try {
            String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            if (data.length < 10) return null;
            
            String id = cleanString(data[0]);
            if (id.isEmpty()) return null;
            
            return new Employee(id, cleanString(data[2]), cleanString(data[1]), 
                cleanString(data[3]), cleanString(data[4]), cleanString(data[5]), 
                cleanString(data[6]),
                Double.parseDouble(cleanString(data[7]).replace(",", "")),
                Double.parseDouble(cleanString(data[8]).replace(",", "")),
                Double.parseDouble(cleanString(data[9]).replace(",", "")));
        } catch (Exception e) { return null; }
    }
    
    private String cleanString(String s) { return s.replace("\"", "").trim(); }
    
    private void loadAttendanceRecords() {
        attendanceMap.clear();
        File attendanceFile = new File(ATTENDANCE_FILE);
        if (!attendanceFile.exists()) return;
        
        try (BufferedReader br = new BufferedReader(new FileReader(attendanceFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    String[] data = line.split("[,\t](?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (data.length < 6) continue;
                    
                    String empId = cleanString(data[0]);
                    String dateString = cleanString(data[3]);
                    
                    LocalDate date = null;
                    try {
                        date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                    } catch (Exception e) {
                        try {
                            String[] parts = dateString.split("/");
                            if (parts.length == 3) {
                                int m = Integer.parseInt(parts[0]);
                                int d = Integer.parseInt(parts[1]);
                                int y = Integer.parseInt(parts[2]);
                                date = LocalDate.of(y, m, d);
                            }
                        } catch (Exception e2) { continue; }
                    }
                    if (date == null) continue;
                    
                    LocalTime timeIn = parseTime(cleanString(data[4]));
                    LocalTime timeOut = parseTime(cleanString(data[5]));
                    if (timeIn == null || timeOut == null) continue;
                    
                    attendanceMap.put(empId + "_" + date.toString(), 
                        new AttendanceRecord(empId, date, timeIn, timeOut));
                } catch (Exception e) {}
            }
        } catch (IOException e) {
            System.err.println("Error reading attendance: " + e.getMessage());
        }
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return null;
        try { return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:mm")); }
        catch (Exception e) {
            try { return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm")); }
            catch (Exception e2) { return null; }
        }
    }
    
    private boolean isInSelectedMonth(LocalDate date) {
        if (selectedMonth.equals("All Months")) return true;
        String[] parts = selectedMonth.split(" ");
        if (parts.length < 2) return true;
        Integer monthNumber = MONTH_MAP.get(parts[0]);
        if (monthNumber == null) return true;
        return date.getMonthValue() == monthNumber && date.getYear() == Integer.parseInt(parts[1]);
    }
    
    private int getMonthsCount() { return selectedMonth.equals("All Months") ? 7 : 1; }
    
    private String getMonthLabel() {
        return selectedMonth.equals("All Months") ? "All Months (Jun-Dec 2024)" : selectedMonth;
    }
    
    private void initializeUI() {
        setTitle("MotorPH Payroll Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        setMinimumSize(new Dimension(1400, 750));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        
        JPanel mainContent = new JPanel(new BorderLayout(15, 15));
        mainContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainContent.setBackground(BG_DARK);
        
        mainContent.add(createHeaderPanel(), BorderLayout.NORTH);
        mainContent.add(createContentPanel(), BorderLayout.CENTER);
        mainContent.add(createStatusPanel(), BorderLayout.SOUTH);
        
        add(mainContent, BorderLayout.CENTER);
        setVisible(true);
    }
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(BG_PANEL);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(18, 25, 18, 25)));
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setBackground(BG_PANEL);
        
        JLabel iconLabel = new JLabel("\u25C6");
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        iconLabel.setForeground(PRIMARY);
        titlePanel.add(iconLabel);
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        textPanel.setBackground(BG_PANEL);
        
        JLabel titleLabel = new JLabel("MotorPH Payroll System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_WHITE);
        textPanel.add(titleLabel);
        
        JLabel subLabel = new JLabel("Professional Payroll Management");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(TEXT_DIM);
        textPanel.add(subLabel);
        
        titlePanel.add(textPanel);
        header.add(titlePanel, BorderLayout.WEST);
        
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        controls.setBackground(BG_PANEL);
        
        JLabel periodLabel = new JLabel("Period:");
        periodLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        periodLabel.setForeground(TEXT_LIGHT);
        controls.add(periodLabel);
        
        monthComboBox = new JComboBox<>(AVAILABLE_MONTHS);
        monthComboBox.setSelectedItem("All Months");
        styleComboBox(monthComboBox);
        monthComboBox.addActionListener(e -> {
            selectedMonth = (String) monthComboBox.getSelectedItem();
            refreshTable();
            updateStatus("Viewing: " + getMonthLabel(), false);
        });
        controls.add(monthComboBox);
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchLabel.setForeground(TEXT_LIGHT);
        controls.add(searchLabel);
        
        searchField = new JTextField(18);
        styleTextField(searchField);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshTable(); }
            public void removeUpdate(DocumentEvent e) { refreshTable(); }
            public void changedUpdate(DocumentEvent e) { refreshTable(); }
        });
        controls.add(searchField);
        
        JButton clearBtn = makeButton("Clear", false);
        clearBtn.setPreferredSize(new Dimension(70, 32));
        clearBtn.addActionListener(e -> { searchField.setText(""); refreshTable(); });
        controls.add(clearBtn);
        
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 30));
        sep.setForeground(BORDER);
        controls.add(sep);
        
        JButton logoutBtn = makeButton("Logout", false);
        logoutBtn.setPreferredSize(new Dimension(90, 32));
        logoutBtn.setForeground(RED);
        logoutBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                logoutBtn.setBackground(new Color(80, 30, 30));
                logoutBtn.setForeground(new Color(255, 100, 100));
            }
            public void mouseExited(MouseEvent e) {
                logoutBtn.setBackground(BG_INPUT);
                logoutBtn.setForeground(RED);
            }
        });
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(MotorPHGUI.this,
                "Are you sure you want to logout?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                MotorPHGUI.this.dispose();
                LoginDialog loginDialog = new LoginDialog(null);
                loginDialog.setVisible(true);
                if (loginDialog.isAuthenticated()) {
                    new MotorPHGUI().setVisible(true);
                } else {
                    System.exit(0);
                }
            }
        });
        controls.add(logoutBtn);
        
        header.add(controls, BorderLayout.EAST);
        return header;
    }
    
    private void styleComboBox(JComboBox<String> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setPreferredSize(new Dimension(170, 32));
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_WHITE);
        cb.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        cb.setFocusable(false);
        
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBackground(isSelected ? PRIMARY : BG_INPUT);
                label.setForeground(TEXT_WHITE);
                label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                label.setOpaque(true);
                return label;
            }
        });
        
        cb.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton("\u25BC");
                button.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                button.setBackground(BG_INPUT);
                button.setForeground(TEXT_WHITE);
                button.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                button.setFocusPainted(false);
                button.setBorderPainted(false);
                return button;
            }
            
            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = (BasicComboPopup) super.createPopup();
                popup.getList().setBackground(BG_INPUT);
                popup.getList().setForeground(TEXT_WHITE);
                popup.getList().setSelectionBackground(PRIMARY);
                popup.getList().setSelectionForeground(Color.WHITE);
                popup.getList().setFont(new Font("Segoe UI", Font.PLAIN, 12));
                popup.setBorder(BorderFactory.createLineBorder(BORDER, 1));
                return popup;
            }
            
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(BG_INPUT);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
    }
    
    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setPreferredSize(new Dimension(200, 32));
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_WHITE);
        tf.setCaretColor(TEXT_WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
    }
    
    private JButton makeButton(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        Color bg = primary ? PRIMARY : BG_INPUT;
        Color fg = primary ? Color.WHITE : TEXT_LIGHT;
        Color hoverBg = primary ? PRIMARY_DARK : BG_HOVER;
        
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(hoverBg); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        
        return btn;
    }
    
    private JPanel createContentPanel() {
        JPanel content = new JPanel(new BorderLayout(15, 0));
        content.setBackground(BG_DARK);
        content.add(createSidebarPanel(), BorderLayout.WEST);
        content.add(createTablePanel(), BorderLayout.CENTER);
        return content;
    }
    
    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_PANEL);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(20, 12, 20, 12)));
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setMaximumSize(new Dimension(210, Integer.MAX_VALUE));
        
        JLabel actionsLabel = new JLabel("  ACTIONS");
        actionsLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        actionsLabel.setForeground(PRIMARY);
        actionsLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        sidebar.add(actionsLabel);
        
        String[] names = {"Add Employee", "Edit Employee", "Delete Employee", 
            "Refresh Data", "Backup Data", "Import CSV", "Export CSV", "Generate Report"};
        
        for (String name : names) {
            JButton btn = makeSidebarBtn(name);
            btn.addActionListener(e -> handleButtonClick(name));
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(6));
        }
        
        sidebar.add(Box.createVerticalStrut(20));
        
        JLabel statsLabel = new JLabel("  STATISTICS");
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statsLabel.setForeground(PRIMARY);
        statsLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        sidebar.add(statsLabel);
        
        JPanel statsPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        statsPanel.setBackground(BG_INPUT);
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(15, 12, 15, 12)));
        
        recordCountLabel = new JLabel("Total Employees: 0");
        recordCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        recordCountLabel.setForeground(TEXT_WHITE);
        statsPanel.add(recordCountLabel);
        
        attendanceStatusLabel = new JLabel("Attendance: 0");
        attendanceStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        attendanceStatusLabel.setForeground(TEXT_LIGHT);
        statsPanel.add(attendanceStatusLabel);
        
        sidebar.add(statsPanel);
        sidebar.add(Box.createVerticalGlue());
        
        return sidebar;
    }
    
    private JButton makeSidebarBtn(String text) {
        JButton btn = new JButton("  " + text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(186, 36));
        btn.setPreferredSize(new Dimension(186, 36));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(BG_INPUT);
        btn.setForeground(TEXT_LIGHT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(BG_HOVER);
                btn.setForeground(TEXT_WHITE);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY, 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(BG_INPUT);
                btn.setForeground(TEXT_LIGHT);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }
        });
        
        return btn;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        
        String[] columns = {"Employee #", "Name", "Birthday", "Status", "Position", 
            "Supervisor", "Basic Salary", "Semi-Monthly", "Rate/Hour", 
            "Hours Worked", "Gross Pay", "Deductions", "Net Pay"};
        
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        
        employeeTable = new JTable(tableModel);
        employeeTable.setRowHeight(34);
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeeTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        employeeTable.setGridColor(new Color(55, 55, 62));
        employeeTable.setShowGrid(true);
        employeeTable.setIntercellSpacing(new Dimension(0, 0));
        employeeTable.setSelectionBackground(PRIMARY);
        employeeTable.setSelectionForeground(Color.WHITE);
        
        JTableHeader header = employeeTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(TABLE_HDR);
        header.setForeground(TEXT_WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY));
        header.setPreferredSize(new Dimension(0, 42));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(TABLE_HDR);
                c.setForeground(TEXT_WHITE);
                ((JLabel)c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
        
        int[] widths = {80, 180, 100, 90, 120, 150, 100, 100, 85, 90, 110, 110, 110};
        TableColumnModel colModel = employeeTable.getColumnModel();
        for (int i = 0; i < colModel.getColumnCount(); i++) {
            colModel.getColumn(i).setPreferredWidth(widths[i]);
        }
        
        employeeTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(PRIMARY);
                    setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? BG_PANEL : TABLE_ALT);
                    setForeground(TEXT_LIGHT);
                }
                setHorizontalAlignment(column >= 6 ? JLabel.RIGHT : JLabel.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
        
        employeeTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showEditEmployeeDialog();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(employeeTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BG_PANEL);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(10, 25, 10, 25)));
        panel.setPreferredSize(new Dimension(0, 45));
        
        statusLabel = new JLabel("\u2713 Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(GREEN);
        panel.add(statusLabel, BorderLayout.WEST);
        
        JLabel timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(TEXT_DIM);
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
            timeLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) + "  |  " + 
                java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });
        timer.start();
        panel.add(timeLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void handleButtonClick(String cmd) {
        switch (cmd) {
            case "Add Employee": showEmployeeDialog(null, "Add New Employee"); break;
            case "Edit Employee": showEditEmployeeDialog(); break;
            case "Delete Employee": deleteEmployee(); break;
            case "Refresh Data": refreshAllData(); break;
            case "Backup Data": createBackup(); break;
            case "Import CSV": importCSV(); break;
            case "Export CSV": exportCSV(); break;
            case "Generate Report": generateReport(); break;
        }
    }
    
    private void refreshAllData() {
        loadData();
        refreshTable();
        updateStatus("Data refreshed - " + attendanceMap.size() + " attendance records", false);
        showMessage("Success", employees.size() + " employees loaded\n" + attendanceMap.size() + " attendance records");
    }
    
    private void refreshTable() {
        tableModel.setRowCount(0);
        String search = searchField.getText().trim().toLowerCase();
        
        for (Employee emp : employees) {
            if (search.isEmpty() || emp.getFullName().toLowerCase().contains(search) || emp.getId().contains(search)) {
                PayrollData p = calculatePayroll(emp.getId());
                tableModel.addRow(new Object[] {
                    emp.getId(), emp.getFullName(), emp.getBirthday(), emp.getStatus(),
                    emp.getPosition(), emp.getSupervisor(),
                    MONEY_FORMAT.format(emp.getBasicSalary()),
                    MONEY_FORMAT.format(emp.getGrossSemiMonthly()),
                    MONEY_FORMAT.format(emp.getHourlyRate()),
                    HOURS_FORMAT.format(p.getTotalHours()),
                    MONEY_FORMAT.format(p.getGrossPay()),
                    MONEY_FORMAT.format(p.getTotalDeductions()),
                    MONEY_FORMAT.format(p.getNetPay())
                });
            }
        }
        
        recordCountLabel.setText("Total Employees: " + tableModel.getRowCount());
        attendanceStatusLabel.setText("Attendance: " + attendanceMap.size());
    }
    
    private PayrollData calculatePayroll(String empId) {
        double totalHours = 0;
        int monthsCount = getMonthsCount();
        Employee emp = findEmployee(empId);
        if (emp == null) return new PayrollData(0, 0, 0, 0);
        
        int filteredCount = 0;
        for (AttendanceRecord rec : attendanceMap.values()) {
            if (rec.empId.equals(empId) && isInSelectedMonth(rec.date)) {
                filteredCount++;
                totalHours += calculateDailyHours(rec.timeIn, rec.timeOut);
            }
        }
        
        double totalGross, expectedHours = 0;
        if (filteredCount > 0 && totalHours > 0) {
            totalGross = totalHours * emp.getHourlyRate();
            expectedHours = 8.0 * filteredCount;
        } else {
            totalGross = emp.getGrossSemiMonthly() * monthsCount;
            totalHours = (emp.getGrossSemiMonthly() / emp.getHourlyRate()) * monthsCount;
        }
        
        double ms = emp.getBasicSalary();
        double sss = computeSSS(ms), ph = computePhilHealth(ms), pi = computePagIbig(ms);
        double preTax = sss + ph + pi;
        double tax = computeIncomeTax(Math.max(ms - preTax, 0));
        double monthlyDed = preTax + tax;
        
        double totalDed;
        if (filteredCount > 0 && totalHours > 0 && expectedHours > 0) {
            totalDed = monthlyDed * monthsCount * (totalHours / expectedHours);
        } else {
            totalDed = monthlyDed * monthsCount;
        }
        
        return new PayrollData(totalHours, totalGross, totalDed, totalGross - totalDed);
    }
    
    private double calculateDailyHours(LocalTime in, LocalTime out) {
        LocalTime adj = (!in.isBefore(WORK_START) && in.isBefore(GRACE_END)) ? WORK_START : in;
        LocalTime start = adj.isBefore(WORK_START) ? WORK_START : adj;
        LocalTime end = out.isAfter(WORK_END) ? WORK_END : out;
        if (!end.isAfter(start)) return 0.0;
        
        long mins = ChronoUnit.MINUTES.between(start, end);
        long lunch = 0;
        if (start.isBefore(LUNCH_END) && end.isAfter(LUNCH_START)) {
            LocalTime os = start.isAfter(LUNCH_START) ? start : LUNCH_START;
            LocalTime oe = end.isBefore(LUNCH_END) ? end : LUNCH_END;
            lunch = ChronoUnit.MINUTES.between(os, oe);
        }
        return Math.max((mins - lunch) / 60.0, 0);
    }
    
    private double computeSSS(double s) {
        if (s < 3250) return 135; else if (s < 3750) return 157.5; else if (s < 4250) return 180;
        else if (s < 4750) return 202.5; else if (s < 5250) return 225; else if (s < 5750) return 247.5;
        else if (s < 6250) return 270; else if (s < 6750) return 292.5; else if (s < 7250) return 315;
        else if (s < 7750) return 337.5; else if (s < 8250) return 360; else if (s < 8750) return 382.5;
        else if (s < 9250) return 405; else if (s < 9750) return 427.5; else if (s < 10250) return 450;
        else if (s < 10750) return 472.5; else if (s < 11250) return 495; else if (s < 11750) return 517.5;
        else if (s < 12250) return 540; else if (s < 12750) return 562.5; else if (s < 13250) return 585;
        else if (s < 13750) return 607.5; else if (s < 14250) return 630; else if (s < 14750) return 652.5;
        else if (s < 15250) return 675; else if (s < 15750) return 697.5; else if (s < 16250) return 720;
        else if (s < 16750) return 742.5; else if (s < 17250) return 765; else if (s < 17750) return 787.5;
        else if (s < 18250) return 810; else if (s < 18750) return 832.5; else if (s < 19250) return 855;
        else if (s < 19750) return 877.5; else if (s < 20250) return 900; else if (s < 20750) return 922.5;
        else if (s < 21250) return 945; else if (s < 21750) return 967.5; else if (s < 22250) return 990;
        else if (s < 22750) return 1012.5; else if (s < 23250) return 1035; else if (s < 23750) return 1057.5;
        else if (s < 24250) return 1080; else if (s < 24750) return 1102.5; else return 1125;
    }
    
    private double computePhilHealth(double s) {
        double p = s * 0.03;
        if (p < 300) p = 300; else if (p > 1800) p = 1800;
        return p / 2;
    }
    
    private double computePagIbig(double s) {
        double c = (s >= 1000 && s <= 1500) ? s * 0.01 : (s > 1500 ? s * 0.02 : 0);
        return Math.min(c, 100);
    }
    
    private double computeIncomeTax(double t) {
        if (t <= 20832) return 0;
        else if (t < 33333) return (t - 20833) * 0.20;
        else if (t < 66667) return 2500 + (t - 33333) * 0.25;
        else if (t < 166667) return 10833 + (t - 66667) * 0.30;
        else if (t < 666667) return 40833.33 + (t - 166667) * 0.32;
        else return 200833.33 + (t - 666667) * 0.35;
    }
    
    private Employee findEmployee(String id) {
        return employees.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
    }
    
    private void showEditEmployeeDialog() {
        int row = employeeTable.getSelectedRow();
        if (row == -1) { showError("No Selection", "Please select an employee to edit."); return; }
        Employee emp = findEmployee((String) tableModel.getValueAt(row, 0));
        if (emp != null) showEmployeeDialog(emp, "Edit Employee");
    }
    
    private void showEmployeeDialog(Employee emp, String title) {
        addEditDialog = new JDialog(this, title, true);
        addEditDialog.setLayout(new BorderLayout(0, 0));
        addEditDialog.setMinimumSize(new Dimension(580, 620));
        addEditDialog.setLocationRelativeTo(this);
        addEditDialog.getContentPane().setBackground(BG_DARK);
        
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hdr.setBackground(PRIMARY);
        hdr.setBorder(BorderFactory.createEmptyBorder(18, 25, 18, 25));
        JLabel tLabel = new JLabel(title);
        tLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        tLabel.setForeground(Color.WHITE);
        hdr.add(tLabel);
        addEditDialog.add(hdr, BorderLayout.NORTH);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(BG_PANEL);
        formPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JTextField[] fields = new JTextField[10];
        String[] labels = {"Employee #:", "First Name:", "Last Name:", "Birthday (MM/dd/yyyy):",
            "Status:", "Position:", "Supervisor:", "Basic Salary:", "Gross Semi-monthly:", "Hourly Rate:"};
        
        for (int i = 0; i < 10; i++) {
            fields[i] = new JTextField();
            styleTextField(fields[i]);
            fields[i].setPreferredSize(new Dimension(280, 36));
        }
        
        if (emp != null) {
            fields[0].setText(emp.getId());
            fields[1].setText(emp.getFirstName());
            fields[2].setText(emp.getLastName());
            fields[3].setText(emp.getBirthday());
            fields[4].setText(emp.getStatus());
            fields[5].setText(emp.getPosition());
            fields[6].setText(emp.getSupervisor());
            fields[7].setText(String.valueOf(emp.getBasicSalary()));
            fields[8].setText(String.valueOf(emp.getGrossSemiMonthly()));
            fields[9].setText(String.valueOf(emp.getHourlyRate()));
            fields[0].setEditable(false);
            fields[0].setBackground(new Color(40, 40, 45));
            fields[0].setForeground(TEXT_DIM);
        }
        
        for (int i = 0; i < 10; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0.35;
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setForeground(TEXT_WHITE);
            formPanel.add(lbl, gbc);
            
            gbc.gridx = 1; gbc.weightx = 0.65;
            formPanel.add(fields[i], gbc);
        }
        
        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setBackground(BG_PANEL);
        formWrap.add(formPanel, BorderLayout.NORTH);
        addEditDialog.add(formWrap, BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 15));
        btnPanel.setBackground(BG_PANEL);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        
        JButton saveBtn = makeButton("Save", true);
        saveBtn.setPreferredSize(new Dimension(100, 36));
        JButton cancelBtn = makeButton("Cancel", false);
        cancelBtn.setPreferredSize(new Dimension(100, 36));
        
        saveBtn.addActionListener(e -> {
            try {
                for (JTextField f : fields) {
                    if (f.getText().trim().isEmpty()) {
                        showError("Validation Error", "All fields are required."); return;
                    }
                }
                
                String id = fields[0].getText().trim();
                String fn = fields[1].getText().trim();
                String ln = fields[2].getText().trim();
                String bd = fields[3].getText().trim();
                String st = fields[4].getText().trim();
                String pos = fields[5].getText().trim();
                String sup = fields[6].getText().trim();
                double bs = Double.parseDouble(fields[7].getText().trim());
                double gsm = Double.parseDouble(fields[8].getText().trim());
                double hr = Double.parseDouble(fields[9].getText().trim());
                
                if (emp == null) {
                    if (employees.stream().anyMatch(ex -> ex.getId().equals(id))) {
                        showError("Duplicate", "Employee # already exists!"); return;
                    }
                    employees.add(new Employee(id, fn, ln, bd, st, pos, sup, bs, gsm, hr));
                    showMessage("Success", "Employee added!");
                } else {
                    emp.setFirstName(fn); emp.setLastName(ln); emp.setBirthday(bd);
                    emp.setStatus(st); emp.setPosition(pos); emp.setSupervisor(sup);
                    emp.setBasicSalary(bs); emp.setGrossSemiMonthly(gsm); emp.setHourlyRate(hr);
                    showMessage("Success", "Employee updated!");
                }
                
                saveEmployeesToCSV();
                refreshTable();
                addEditDialog.dispose();
            } catch (NumberFormatException ex) {
                showError("Invalid Input", "Please enter valid numeric values.");
            }
        });
        
        cancelBtn.addActionListener(e -> addEditDialog.dispose());
        
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);
        addEditDialog.add(btnPanel, BorderLayout.SOUTH);
        addEditDialog.setVisible(true);
    }
    
    private void deleteEmployee() {
        int row = employeeTable.getSelectedRow();
        if (row == -1) { showError("No Selection", "Please select an employee."); return; }
        Employee emp = findEmployee((String) tableModel.getValueAt(row, 0));
        if (emp != null && JOptionPane.showConfirmDialog(this, "Delete " + emp.getFullName() + "?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            employees.remove(emp);
            saveEmployeesToCSV();
            refreshTable();
            showMessage("Success", "Employee deleted!");
        }
    }
    
    private void saveEmployeesToCSV() {
        try {
            File tmp = new File(EMPLOYEES_FILE + ".tmp");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmp))) {
                bw.write(CSV_HEADER); bw.newLine();
                for (Employee e : employees) {
                    bw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%.2f,%.2f,%.2f",
                        e.getId(), e.getLastName(), e.getFirstName(), e.getBirthday(),
                        e.getStatus(), e.getPosition(), e.getSupervisor(),
                        e.getBasicSalary(), e.getGrossSemiMonthly(), e.getHourlyRate()));
                    bw.newLine();
                }
            }
            if (employeesFile.exists()) employeesFile.delete();
            tmp.renameTo(employeesFile);
        } catch (IOException e) { showError("Save Error", "Could not save."); }
    }
    
    private void createBackup() {
        try {
            File dir = new File(BACKUP_DIR);
            if (!dir.exists()) dir.mkdirs();
            String ts = LocalDate.now() + "_" + java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            Files.copy(new File(EMPLOYEES_FILE).toPath(), new File(BACKUP_DIR + "employees_" + ts + ".csv").toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(new File(ATTENDANCE_FILE).toPath(), new File(BACKUP_DIR + "attendance_" + ts + ".csv").toPath(), StandardCopyOption.REPLACE_EXISTING);
            showMessage("Backup", "Backup created in " + BACKUP_DIR);
        } catch (Exception e) { showError("Error", e.getMessage()); }
    }
    
    private void importCSV() {
        JFileChooser c = new JFileChooser(".");
        c.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV", "csv"));
        if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = c.getSelectedFile();
            if (f.getName().toLowerCase().contains("employee")) {
                employeesFile = f; loadEmployees(); refreshTable();
            } else if (f.getName().toLowerCase().contains("attendance")) {
                try { Files.copy(f.toPath(), new File(ATTENDANCE_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING); loadAttendanceRecords(); refreshTable(); }
                catch (IOException e) { showError("Error", "Import failed"); }
            }
        }
    }
    
    private void exportCSV() {
        JFileChooser c = new JFileChooser(".");
        c.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV", "csv"));
        if (c.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(c.getSelectedFile()))) {
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    bw.write(tableModel.getColumnName(i)); if (i < tableModel.getColumnCount()-1) bw.write(",");
                }
                bw.newLine();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        bw.write(tableModel.getValueAt(i,j).toString()); if (j < tableModel.getColumnCount()-1) bw.write(",");
                    }
                    bw.newLine();
                }
                showMessage("Export", "Done!");
            } catch (Exception e) { showError("Error", "Export failed"); }
        }
    }
    
    private void generateReport() {
        if (employees.isEmpty()) { showError("No Data", "No employees."); return; }
        StringBuilder r = new StringBuilder();
        r.append("=".repeat(80)).append("\n              MOTORPH PAYROLL REPORT\n").append("=".repeat(80)).append("\n");
        r.append("Period: ").append(getMonthLabel()).append("\n\n");
        double tG=0,tD=0,tN=0;
        for (Employee e : employees) {
            PayrollData p = calculatePayroll(e.getId());
            tG+=p.getGrossPay(); tD+=p.getTotalDeductions(); tN+=p.getNetPay();
            r.append(String.format("%-8s %-25s %8s %14s %14s %14s\n", e.getId(), e.getFullName(),
                HOURS_FORMAT.format(p.getTotalHours()), MONEY_FORMAT.format(p.getGrossPay()),
                MONEY_FORMAT.format(p.getTotalDeductions()), MONEY_FORMAT.format(p.getNetPay())));
        }
        r.append("\n").append("=".repeat(80)).append("\n");
        r.append(String.format("TOTAL GROSS: %30s\n", MONEY_FORMAT.format(tG)));
        r.append(String.format("TOTAL DEDUCTIONS: %25s\n", MONEY_FORMAT.format(tD)));
        r.append(String.format("TOTAL NET PAY: %30s\n", MONEY_FORMAT.format(tN)));
        
        JTextArea ta = new JTextArea(r.toString());
        ta.setEditable(false);
        ta.setFont(new Font("Consolas", Font.PLAIN, 12));
        ta.setBackground(BG_PANEL);
        ta.setForeground(TEXT_LIGHT);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(800, 500));
        JOptionPane.showMessageDialog(this, sp, "Payroll Report", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void updateStatus(String msg, boolean warn) {
        if (statusLabel != null) {
            statusLabel.setText((warn ? "\u26A0 " : "\u2713 ") + msg);
            statusLabel.setForeground(warn ? RED : GREEN);
        }
    }
    
    private void showMessage(String t, String m) { JOptionPane.showMessageDialog(this, m, t, JOptionPane.INFORMATION_MESSAGE); }
    private void showError(String t, String m) { JOptionPane.showMessageDialog(this, m, t, JOptionPane.ERROR_MESSAGE); }
    
    private static class Employee {
        String id, firstName, lastName, birthday, status, position, supervisor;
        double basicSalary, grossSemiMonthly, hourlyRate;
        Employee(String id, String fn, String ln, String bd, String st, String pos, String sup, double bs, double gsm, double hr) {
            this.id=id; this.firstName=fn; this.lastName=ln; this.birthday=bd;
            this.status=st; this.position=pos; this.supervisor=sup;
            this.basicSalary=bs; this.grossSemiMonthly=gsm; this.hourlyRate=hr;
        }
        String getId(){return id;} String getFirstName(){return firstName;} void setFirstName(String s){firstName=s;}
        String getLastName(){return lastName;} void setLastName(String s){lastName=s;}
        String getBirthday(){return birthday;} void setBirthday(String s){birthday=s;}
        String getStatus(){return status;} void setStatus(String s){status=s;}
        String getPosition(){return position;} void setPosition(String s){position=s;}
        String getSupervisor(){return supervisor;} void setSupervisor(String s){supervisor=s;}
        double getBasicSalary(){return basicSalary;} void setBasicSalary(double d){basicSalary=d;}
        double getGrossSemiMonthly(){return grossSemiMonthly;} void setGrossSemiMonthly(double d){grossSemiMonthly=d;}
        double getHourlyRate(){return hourlyRate;} void setHourlyRate(double d){hourlyRate=d;}
        String getFullName(){return firstName+" "+lastName;}
    }
    
    private static class AttendanceRecord {
        String empId; LocalDate date; LocalTime timeIn, timeOut;
        AttendanceRecord(String e, LocalDate d, LocalTime i, LocalTime o) { empId=e; date=d; timeIn=i; timeOut=o; }
    }
    
    private static class PayrollData {
        double totalHours, grossPay, totalDeductions, netPay;
        PayrollData(double h, double g, double d, double n) { totalHours=h; grossPay=g; totalDeductions=d; netPay=n; }
        double getTotalHours(){return totalHours;} double getGrossPay(){return grossPay;}
        double getTotalDeductions(){return totalDeductions;} double getNetPay(){return netPay;}
    }
    
    private static class LoginDialog extends JDialog {
        private boolean authenticated = false;
        private JTextField usernameField;
        private JPasswordField passwordField;
        
        LoginDialog(JFrame parent) {
            super(parent, "MotorPH Login", true);
            setUndecorated(true);
            initializeUI();
        }
        
        private void initializeUI() {
            setLayout(new BorderLayout());
            setSize(420, 520);
            setLocationRelativeTo(getParent());
            getRootPane().setBorder(BorderFactory.createLineBorder(BORDER, 1));
            
            JPanel top = new JPanel(null);
            top.setBackground(PRIMARY);
            top.setPreferredSize(new Dimension(0, 160));
            
            JLabel title = new JLabel("MotorPH");
            title.setFont(new Font("Segoe UI", Font.BOLD, 36));
            title.setForeground(Color.WHITE);
            title.setBounds(30, 40, 360, 50);
            top.add(title);
            
            JLabel sub = new JLabel("Payroll Management System");
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            sub.setForeground(new Color(200, 225, 250));
            sub.setBounds(30, 95, 360, 25);
            top.add(sub);
            
            JLabel closeBtn = new JLabel("  X  ");
            closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.setBounds(380, 10, 30, 25);
            closeBtn.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { System.exit(0); }
                public void mouseEntered(MouseEvent e) { closeBtn.setForeground(RED); }
                public void mouseExited(MouseEvent e) { closeBtn.setForeground(Color.WHITE); }
            });
            top.add(closeBtn);
            
            add(top, BorderLayout.NORTH);
            
            JPanel form = new JPanel();
            form.setBackground(BG_PANEL);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setBorder(BorderFactory.createEmptyBorder(35, 40, 35, 40));
            
            JLabel userLbl = new JLabel("Username");
            userLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            userLbl.setForeground(TEXT_LIGHT);
            userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(userLbl);
            form.add(Box.createVerticalStrut(6));
            
            usernameField = new JTextField();
            usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            usernameField.setBackground(BG_INPUT);
            usernameField.setForeground(TEXT_WHITE);
            usernameField.setCaretColor(TEXT_WHITE);
            usernameField.setMaximumSize(new Dimension(340, 42));
            usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(usernameField);
            
            form.add(Box.createVerticalStrut(22));
            
            JLabel passLbl = new JLabel("Password");
            passLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            passLbl.setForeground(TEXT_LIGHT);
            passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(passLbl);
            form.add(Box.createVerticalStrut(6));
            
            passwordField = new JPasswordField();
            passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            passwordField.setBackground(BG_INPUT);
            passwordField.setForeground(TEXT_WHITE);
            passwordField.setCaretColor(TEXT_WHITE);
            passwordField.setMaximumSize(new Dimension(340, 42));
            passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(passwordField);
            
            form.add(Box.createVerticalStrut(30));
            
            JButton loginBtn = new JButton("Sign In");
            loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
            loginBtn.setBackground(PRIMARY);
            loginBtn.setForeground(Color.WHITE);
            loginBtn.setFocusPainted(false);
            loginBtn.setBorderPainted(false);
            loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            loginBtn.setMaximumSize(new Dimension(340, 45));
            loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            loginBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { loginBtn.setBackground(PRIMARY_DARK); }
                public void mouseExited(MouseEvent e) { loginBtn.setBackground(PRIMARY); }
            });
            
            loginBtn.addActionListener(e -> attemptLogin());
            form.add(loginBtn);
            
            form.add(Box.createVerticalStrut(18));
            
            JLabel hint = new JLabel("Default: payroll_staff / 12345");
            hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            hint.setForeground(TEXT_DIM);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(hint);
            
            add(form, BorderLayout.CENTER);
            
            getRootPane().setDefaultButton(loginBtn);
        }
        
        private void attemptLogin() {
            String u = usernameField.getText().trim();
            String p = new String(passwordField.getPassword());
            
            if (u.equals("payroll_staff") && p.equals("12345")) {
                authenticated = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                passwordField.requestFocus();
            }
        }
        
        boolean isAuthenticated() { return authenticated; }
    }
}