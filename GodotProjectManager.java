import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GodotProjectManager extends JFrame {
    private JTabbedPane tabbedPane;
    private ProjectsPanel projectsPanel;
    private EnginesPanel enginesPanel;
    private SettingsPanel settingsPanel;
    private List<GodotEngine> engines;
    private List<GodotProject> projects;
    private String dataFile;
    private String defaultProjectLocation;
    private String defaultEngineLocation;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new GodotProjectManager().setVisible(true);
        });
    }

    public GodotProjectManager() {
        setTitle("Godot Project Manager");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set up data file path in Documents folder
        String documentsPath = System.getProperty("user.home") + File.separator + "Documents";
        String appDataDir = documentsPath + File.separator + "GodotProjectManager";
        
        // Create directory if it doesn't exist
        File appDir = new File(appDataDir);
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        
        dataFile = appDataDir + File.separator + "godot_manager_data.json";

        initializeData();
        initializeUI();
        loadData();
    }

    private void initializeData() {
        // Set default locations
        defaultProjectLocation = System.getProperty("user.home") + File.separator + "GodotProjects";
        defaultEngineLocation = System.getProperty("user.home") + File.separator + "Godot";
        
        engines = new ArrayList<>();
        engines.add(new GodotEngine("4.5", "103 MB", "https://github.com/godotengine/godot/releases/download/4.5-stable/Godot_v4.5-stable_win64.exe.zip"));
        engines.add(new GodotEngine("4.4.1", "100 MB", "https://github.com/godotengine/godot/releases/download/4.4-stable/Godot_v4.4.1-stable_win64.exe.zip"));
        engines.add(new GodotEngine("4.4", "100 MB", "https://github.com/godotengine/godot/releases/download/4.4-stable/Godot_v4.4-stable_win64.exe.zip"));
        engines.add(new GodotEngine("4.3", "95 MB", "https://github.com/godotengine/godot/releases/download/4.3-stable/Godot_v4.3-stable_win64.exe.zip"));
        engines.add(new GodotEngine("4.2.2", "92 MB", "https://github.com/godotengine/godot/releases/download/4.2.2-stable/Godot_v4.2.2-stable_win64.exe.zip"));
        engines.add(new GodotEngine("4.2.1", "91 MB", "https://github.com/godotengine/godot/releases/download/4.2.1-stable/Godot_v4.2.1-stable_win64.exe.zip"));
        engines.add(new GodotEngine("4.1.4", "88 MB", "https://github.com/godotengine/godot/releases/download/4.1.4-stable/Godot_v4.1.4-stable_win64.exe.zip"));
        engines.add(new GodotEngine("4.1.3", "88 MB", "https://github.com/godotengine/godot/releases/download/4.1.3-stable/Godot_v4.1.3-stable_win64.exe.zip"));
        engines.add(new GodotEngine("3.6.1", "45 MB", "https://github.com/godotengine/godot/releases/download/3.6.1-stable/Godot_v3.6-stable_win64.exe.zip"));
        engines.add(new GodotEngine("3.6", "45 MB", "https://github.com/godotengine/godot/releases/download/3.6-stable/Godot_v3.6-stable_win64.exe.zip"));
        engines.add(new GodotEngine("3.5.3", "44 MB", "https://github.com/godotengine/godot/releases/download/3.5.3-stable/Godot_v3.5.3-stable_win64.exe.zip"));

        projects = new ArrayList<>();
    }

    private void initializeUI() {
        tabbedPane = new JTabbedPane();
        projectsPanel = new ProjectsPanel();
        enginesPanel = new EnginesPanel();
        settingsPanel = new SettingsPanel();

        tabbedPane.addTab("Projects", projectsPanel);
        tabbedPane.addTab("Engines", enginesPanel);
        tabbedPane.addTab("Settings", settingsPanel);

        add(tabbedPane);
    }

    private void loadData() {
        File file = new File(dataFile);
        if (!file.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String section = "";
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                if (line.equals("[SETTINGS]")) {
                    section = "settings";
                    continue;
                } else if (line.equals("[ENGINES]")) {
                    section = "engines";
                    continue;
                } else if (line.equals("[PROJECTS]")) {
                    section = "projects";
                    continue;
                }
                
                if (section.equals("settings")) {
                    // Format: key=value
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        if (parts[0].equals("defaultProjectLocation")) {
                            defaultProjectLocation = parts[1];
                        } else if (parts[0].equals("defaultEngineLocation")) {
                            defaultEngineLocation = parts[1];
                        }
                    }
                } else if (section.equals("engines")) {
                    // Format: version|installed|path
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 3) {
                        for (GodotEngine engine : engines) {
                            if (engine.getVersion().equals(parts[0])) {
                                engine.setInstalled(Boolean.parseBoolean(parts[1]));
                                if (!parts[2].isEmpty()) {
                                    engine.setInstalledPath(parts[2]);
                                }
                                break;
                            }
                        }
                    }
                } else if (section.equals("projects")) {
                    // Format: name|path|engineVersion|lastOpened
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 4) {
                        GodotProject project = new GodotProject(parts[0], parts[1], parts[2]);
                        project.setLastOpened(parts[3]);
                        projects.add(project);
                    }
                }
            }
            
            // Refresh UI
            if (projectsPanel != null) {
                projectsPanel.refreshProjectTable();
            }
            if (enginesPanel != null) {
                enginesPanel.refreshEngineTable();
            }
            if (settingsPanel != null) {
                settingsPanel.refreshSettings();
            }
            
        } catch (IOException e) {
            System.err.println("Error loading data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile))) {
            writer.write("# Godot Project Manager Data File");
            writer.newLine();
            writer.write("# Do not edit manually");
            writer.newLine();
            writer.newLine();
            
            // Save settings
            writer.write("[SETTINGS]");
            writer.newLine();
            writer.write("defaultProjectLocation=" + defaultProjectLocation);
            writer.newLine();
            writer.write("defaultEngineLocation=" + defaultEngineLocation);
            writer.newLine();
            writer.newLine();
            
            // Save engines
            writer.write("[ENGINES]");
            writer.newLine();
            for (GodotEngine engine : engines) {
                // Format: version|installed|path
                writer.write(String.format("%s|%s|%s",
                    engine.getVersion(),
                    engine.isInstalled(),
                    engine.getInstalledPath()));
                writer.newLine();
            }
            
            writer.newLine();
            
            // Save projects
            writer.write("[PROJECTS]");
            writer.newLine();
            for (GodotProject project : projects) {
                // Format: name|path|engineVersion|lastOpened
                writer.write(String.format("%s|%s|%s|%s",
                    project.getName(),
                    project.getPath(),
                    project.getEngineVersion(),
                    project.getLastOpened()));
                writer.newLine();
            }
            
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error saving data: " + e.getMessage(),
                "Save Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Inner class for Projects Panel
    class ProjectsPanel extends JPanel {
        private DefaultTableModel tableModel;
        private JTable projectTable;

        public ProjectsPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(10, 10, 10, 10));

            // Top panel with buttons
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton newProjectBtn = new JButton("New Project");
            JButton importProjectBtn = new JButton("Import Project");
            JButton refreshBtn = new JButton("Refresh");

            newProjectBtn.addActionListener(e -> showNewProjectDialog());
            importProjectBtn.addActionListener(e -> importProject());
            refreshBtn.addActionListener(e -> refreshProjectTable());

            topPanel.add(newProjectBtn);
            topPanel.add(importProjectBtn);
            topPanel.add(refreshBtn);

            // Table for projects
            String[] columns = {"Project Name", "Path", "Engine Version", "Last Opened"};
            tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            projectTable = new JTable(tableModel);
            projectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            projectTable.setRowHeight(30);
            JScrollPane scrollPane = new JScrollPane(projectTable);

            // Bottom panel with action buttons
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton openBtn = new JButton("Open Project");
            JButton changeEngineBtn = new JButton("Change Engine Version");
            JButton removeBtn = new JButton("Remove");
            JButton showInExplorerBtn = new JButton("Show in Explorer");

            openBtn.addActionListener(e -> openSelectedProject());
            changeEngineBtn.addActionListener(e -> changeEngineVersion());
            removeBtn.addActionListener(e -> removeSelectedProject());
            showInExplorerBtn.addActionListener(e -> showInExplorer());

            bottomPanel.add(showInExplorerBtn);
            bottomPanel.add(removeBtn);
            bottomPanel.add(changeEngineBtn);
            bottomPanel.add(openBtn);

            add(topPanel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
            add(bottomPanel, BorderLayout.SOUTH);

            refreshProjectTable();
        }

        private void showNewProjectDialog() {
            JDialog dialog = new JDialog(GodotProjectManager.this, "Create New Project", true);
            dialog.setLayout(new GridBagLayout());
            dialog.setSize(500, 300);
            dialog.setLocationRelativeTo(GodotProjectManager.this);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Project Name
            gbc.gridx = 0; gbc.gridy = 0;
            dialog.add(new JLabel("Project Name:"), gbc);
            gbc.gridx = 1;
            JTextField nameField = new JTextField(20);
            dialog.add(nameField, gbc);

            // Project Path
            gbc.gridx = 0; gbc.gridy = 1;
            dialog.add(new JLabel("Project Path:"), gbc);
            gbc.gridx = 1;
            JTextField pathField = new JTextField(20);
            pathField.setText(defaultProjectLocation);
            JButton browseBtn = new JButton("Browse");
            JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
            pathPanel.add(pathField, BorderLayout.CENTER);
            pathPanel.add(browseBtn, BorderLayout.EAST);
            dialog.add(pathPanel, gbc);

            browseBtn.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                    pathField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            });

            // Engine Version
            gbc.gridx = 0; gbc.gridy = 2;
            dialog.add(new JLabel("Engine Version:"), gbc);
            gbc.gridx = 1;
            JComboBox<String> engineCombo = new JComboBox<>();
            for (GodotEngine engine : engines) {
                if (engine.isInstalled()) {
                    engineCombo.addItem(engine.getVersion());
                }
            }
            dialog.add(engineCombo, gbc);

            // Buttons
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton createBtn = new JButton("Create");
            JButton cancelBtn = new JButton("Cancel");

            createBtn.addActionListener(e -> {
                String name = nameField.getText().trim();
                String path = pathField.getText().trim();
                String engine = (String) engineCombo.getSelectedItem();

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Please enter a project name.");
                    return;
                }

                if (engine == null) {
                    JOptionPane.showMessageDialog(dialog, "Please install an engine first.");
                    return;
                }

                String fullPath = path + File.separator + name;
                GodotProject project = new GodotProject(name, fullPath, engine);
                projects.add(project);
                
                // Create project directory
                try {
                    Files.createDirectories(Paths.get(fullPath));
                    // Create project.godot file
                    String projectContent = String.format(
                        "; Engine configuration file.\n\n" +
                        "config_version=5\n\n" +
                        "[application]\n\n" +
                        "config/name=\"%s\"\n" +
                        "config/features=PackedStringArray(\"4.3\")\n",
                        name
                    );
                    Files.write(Paths.get(fullPath, "project.godot"), projectContent.getBytes());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog, "Error creating project: " + ex.getMessage());
                }

                refreshProjectTable();
                saveData();
                dialog.dispose();
            });

            cancelBtn.addActionListener(e -> dialog.dispose());

            buttonPanel.add(cancelBtn);
            buttonPanel.add(createBtn);
            dialog.add(buttonPanel, gbc);

            dialog.setVisible(true);
        }

        private void importProject() {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Godot Project Folder");

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File projectDir = chooser.getSelectedFile();
                File projectFile = new File(projectDir, "project.godot");

                if (!projectFile.exists()) {
                    JOptionPane.showMessageDialog(this, 
                        "No project.godot file found in selected directory.", 
                        "Invalid Project", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String name = projectDir.getName();
                String path = projectDir.getAbsolutePath();
                
                // Try to detect engine version from project.godot
                String engineVersion = "4.3.0"; // default
                try {
                    List<String> lines = Files.readAllLines(projectFile.toPath());
                    for (String line : lines) {
                        if (line.contains("config_version")) {
                            // Simple version detection
                            if (line.contains("5")) engineVersion = "4.3.0";
                            else if (line.contains("4")) engineVersion = "3.6.0";
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                GodotProject project = new GodotProject(name, path, engineVersion);
                projects.add(project);
                refreshProjectTable();
                saveData();
            }
        }

        private void openSelectedProject() {
            int row = projectTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a project.");
                return;
            }

            GodotProject project = projects.get(row);
            
            // Find the engine
            GodotEngine engine = null;
            for (GodotEngine e : engines) {
                if (e.getVersion().equals(project.getEngineVersion()) && e.isInstalled()) {
                    engine = e;
                    break;
                }
            }

            if (engine == null) {
                JOptionPane.showMessageDialog(this, 
                    "Engine version " + project.getEngineVersion() + " is not installed.\n" +
                    "Please install the engine first from the Engines tab.",
                    "Engine Not Found",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Verify the engine executable exists
            File engineFile = new File(engine.getInstalledPath());
            if (!engineFile.exists()) {
                JOptionPane.showMessageDialog(this, 
                    "Engine executable not found at:\n" + engine.getInstalledPath() + "\n\n" +
                    "Please reinstall the engine from the Engines tab.",
                    "Engine Not Found",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Verify the project file exists
            String projectFile = project.getPath() + File.separator + "project.godot";
            File projectFileObj = new File(projectFile);
            if (!projectFileObj.exists()) {
                JOptionPane.showMessageDialog(this, 
                    "Project file not found at:\n" + projectFile,
                    "Project Not Found",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Update last opened time
                project.updateLastOpened();
                
                // Launch Godot with the project (don't wait for it to close)
                ProcessBuilder pb = new ProcessBuilder(engine.getInstalledPath(), "--editor", "--path", project.getPath());
                pb.directory(new File(project.getPath()));
                
                // Redirect output to avoid blocking
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                
                Process process = pb.start();
                
                // Don't wait for process - let it run independently
                // Just save the updated last opened time
                refreshProjectTable();
                saveData();
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening project:\n" + e.getMessage() + "\n\n" +
                    "Engine path: " + engine.getInstalledPath() + "\n" +
                    "Project path: " + project.getPath(),
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }

        private void removeSelectedProject() {
            int row = projectTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a project.");
                return;
            }

            int choice = JOptionPane.showConfirmDialog(this, 
                "Remove this project from the list?\n(Project files will not be deleted)", 
                "Confirm Removal", 
                JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                projects.remove(row);
                refreshProjectTable();
                saveData();
            }
        }

        private void showInExplorer() {
            int row = projectTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a project.");
                return;
            }

            GodotProject project = projects.get(row);
            try {
                Desktop.getDesktop().open(new File(project.getPath()));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening folder: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }

        private void changeEngineVersion() {
            int row = projectTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a project.");
                return;
            }

            GodotProject project = projects.get(row);
            
            // Get list of installed engines
            List<String> installedEngines = new ArrayList<>();
            for (GodotEngine engine : engines) {
                if (engine.isInstalled()) {
                    installedEngines.add(engine.getVersion());
                }
            }
            
            if (installedEngines.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "No engines are installed.\nPlease install an engine from the Engines tab first.",
                    "No Engines Available",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Show warning dialog
            JDialog warningDialog = new JDialog(GodotProjectManager.this, "Change Engine Version - Warning", true);
            warningDialog.setSize(550, 400);
            warningDialog.setLocationRelativeTo(this);
            warningDialog.setLayout(new BorderLayout(10, 10));
            
            // Warning message panel
            JPanel messagePanel = new JPanel(new BorderLayout(10, 10));
            messagePanel.setBorder(new EmptyBorder(20, 20, 10, 20));
            
            JLabel warningIcon = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
            JPanel textPanel = new JPanel(new BorderLayout(5, 5));
            
            JLabel titleLabel = new JLabel("<html><b>⚠ WARNING: Back Up Your Project First!</b></html>");
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
            titleLabel.setForeground(new Color(255, 140, 0));
            
            JTextArea warningText = new JTextArea(
                "Changing the Godot engine version can cause:\n\n" +
                "• Project corruption or data loss\n" +
                "• Incompatible features or breaking changes\n" +
                "• Assets that no longer work correctly\n" +
                "• Scene and script errors\n\n" +
                "STRONGLY RECOMMENDED:\n" +
                "1. Back up your entire project folder\n" +
                "2. Test in a copy first\n" +
                "3. Read migration guides for major version changes\n\n" +
                "Current Version: " + project.getEngineVersion() + "\n" +
                "Project: " + project.getName()
            );
            warningText.setEditable(false);
            warningText.setBackground(messagePanel.getBackground());
            warningText.setFont(new Font("Dialog", Font.PLAIN, 12));
            warningText.setWrapStyleWord(true);
            warningText.setLineWrap(true);
            
            textPanel.add(titleLabel, BorderLayout.NORTH);
            textPanel.add(warningText, BorderLayout.CENTER);
            
            messagePanel.add(warningIcon, BorderLayout.WEST);
            messagePanel.add(textPanel, BorderLayout.CENTER);
            
            // Engine selection panel
            JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            selectionPanel.setBorder(new EmptyBorder(0, 20, 10, 20));
            selectionPanel.add(new JLabel("Select new engine version:"));
            JComboBox<String> engineCombo = new JComboBox<>(installedEngines.toArray(new String[0]));
            engineCombo.setSelectedItem(project.getEngineVersion());
            selectionPanel.add(engineCombo);
            
            // Buttons panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
            
            JButton confirmBtn = new JButton("I accept the risk");
            JButton cancelBtn = new JButton("Cancel");
            
            confirmBtn.setBackground(new Color(255, 140, 0));
            confirmBtn.setForeground(Color.RED);
            confirmBtn.setFocusPainted(false);
            
            confirmBtn.addActionListener(e -> {
                String selectedVersion = (String) engineCombo.getSelectedItem();
                if (selectedVersion != null && !selectedVersion.equals(project.getEngineVersion())) {
                    project.setEngineVersion(selectedVersion);
                    refreshProjectTable();
                    saveData();
                    JOptionPane.showMessageDialog(warningDialog,
                        "Engine version changed to " + selectedVersion + "\n\n" +
                        "Remember to test your project thoroughly!",
                        "Version Changed",
                        JOptionPane.INFORMATION_MESSAGE);
                }
                warningDialog.dispose();
            });
            
            cancelBtn.addActionListener(e -> warningDialog.dispose());
            
            buttonPanel.add(cancelBtn);
            buttonPanel.add(confirmBtn);
            
            warningDialog.add(messagePanel, BorderLayout.CENTER);
            warningDialog.add(selectionPanel, BorderLayout.SOUTH);
            
            JPanel bottomContainer = new JPanel(new BorderLayout());
            bottomContainer.add(selectionPanel, BorderLayout.NORTH);
            bottomContainer.add(buttonPanel, BorderLayout.SOUTH);
            warningDialog.add(bottomContainer, BorderLayout.SOUTH);
            
            warningDialog.setVisible(true);
        }

        private void refreshProjectTable() {
            tableModel.setRowCount(0);
            for (GodotProject project : projects) {
                tableModel.addRow(new Object[]{
                    project.getName(),
                    project.getPath(),
                    project.getEngineVersion(),
                    project.getLastOpened()
                });
            }
        }
    }

    // Inner class for Engines Panel
    class EnginesPanel extends JPanel {
        private DefaultTableModel tableModel;
        private JTable engineTable;

        public EnginesPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(10, 10, 10, 10));

            // Info label
            JLabel infoLabel = new JLabel("Manage Godot engine versions. Download and install engines to use with your projects.");
            add(infoLabel, BorderLayout.NORTH);

            // Table for engines
            String[] columns = {"Version", "Status", "Size", "Installation Path"};
            tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            engineTable = new JTable(tableModel);
            engineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            engineTable.setRowHeight(30);
            JScrollPane scrollPane = new JScrollPane(engineTable);

            // Bottom panel with action buttons
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton downloadBtn = new JButton("Download");
            JButton installBtn = new JButton("Install from File");
            JButton uninstallBtn = new JButton("Uninstall");
            JButton refreshBtn = new JButton("Refresh");

            downloadBtn.addActionListener(e -> downloadSelectedEngine());
            installBtn.addActionListener(e -> installFromFile());
            uninstallBtn.addActionListener(e -> uninstallSelectedEngine());
            refreshBtn.addActionListener(e -> refreshEngineTable());

            bottomPanel.add(refreshBtn);
            bottomPanel.add(uninstallBtn);
            bottomPanel.add(installBtn);
            bottomPanel.add(downloadBtn);

            add(scrollPane, BorderLayout.CENTER);
            add(bottomPanel, BorderLayout.SOUTH);

            refreshEngineTable();
        }

        private void downloadSelectedEngine() {
            int row = engineTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select an engine version.");
                return;
            }

            GodotEngine engine = engines.get(row);
            if (engine.isInstalled()) {
                JOptionPane.showMessageDialog(this, "This engine version is already installed.");
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Installation Directory");
            chooser.setSelectedFile(new File(defaultEngineLocation));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File installDir = new File(chooser.getSelectedFile(), engine.getVersion());
                
                // Show progress dialog
                JDialog progressDialog = new JDialog(GodotProjectManager.this, "Downloading", true);
                progressDialog.setSize(400, 150);
                progressDialog.setLocationRelativeTo(this);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                
                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                JLabel statusLabel = new JLabel("Preparing download...");
                
                JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
                contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
                contentPanel.add(statusLabel, BorderLayout.NORTH);
                contentPanel.add(progressBar, BorderLayout.CENTER);
                progressDialog.add(contentPanel);
                
                // Download in background thread
                SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            installDir.mkdirs();
                            
                            // Use modern HttpClient
                            HttpClient client = HttpClient.newBuilder()
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .build();
                            
                            HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(engine.getDownloadUrl()))
                                .GET()
                                .build();
                            
                            File tempZip = new File(installDir.getParent(), "temp_" + engine.getVersion() + ".zip");
                            
                            // Download with progress tracking
                            publish("Connecting...|0");
                            
                            HttpResponse<InputStream> response = client.send(request, 
                                HttpResponse.BodyHandlers.ofInputStream());
                            
                            long fileSize = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                            
                            try (InputStream in = response.body();
                                 FileOutputStream out = new FileOutputStream(tempZip)) {
                                
                                byte[] buffer = new byte[8192];
                                long totalRead = 0;
                                int bytesRead;
                                
                                while ((bytesRead = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, bytesRead);
                                    totalRead += bytesRead;
                                    if (fileSize > 0) {
                                        int progress = (int) ((totalRead * 100) / fileSize);
                                        publish("Downloading...|" + progress);
                                    }
                                }
                            }
                            
                            // Extract the ZIP file
                            publish("Extracting files...|100");
                            extractZipFile(tempZip, installDir);
                            
                            // Find the Godot executable in the extracted files
                            String exePath = findGodotExecutable(installDir);
                            if (exePath == null) {
                                throw new Exception("Could not find Godot executable in extracted files");
                            }
                            
                            engine.setInstalled(true);
                            engine.setInstalledPath(exePath);
                            
                            // Cleanup
                            tempZip.delete();
                            
                        } catch (Exception e) {
                            throw e;
                        }
                        return null;
                    }
                    
                    @Override
                    protected void process(List<String> chunks) {
                        String latest = chunks.get(chunks.size() - 1);
                        String[] parts = latest.split("\\|");
                        statusLabel.setText(parts[0]);
                        if (parts.length > 1) {
                            progressBar.setValue(Integer.parseInt(parts[1]));
                        }
                    }
                    
                    @Override
                    protected void done() {
                        progressDialog.dispose();
                        try {
                            get();
                            JOptionPane.showMessageDialog(EnginesPanel.this, 
                                "Engine " + engine.getVersion() + " downloaded successfully!");
                            refreshEngineTable();
                            saveData();
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(EnginesPanel.this, 
                                "Download failed: " + e.getMessage(), 
                                "Error", 
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                
                worker.execute();
                progressDialog.setVisible(true);
            }
        }

        private void installFromFile() {
            int row = engineTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select an engine version.");
                return;
            }

            GodotEngine engine = engines.get(row);
            
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Godot Executable");
            chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".exe");
                }
                public String getDescription() {
                    return "Godot Executable (*.exe)";
                }
            });

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File exeFile = chooser.getSelectedFile();
                engine.setInstalled(true);
                engine.setInstalledPath(exeFile.getAbsolutePath());
                refreshEngineTable();
                saveData();
                JOptionPane.showMessageDialog(this, "Engine registered successfully!");
            }
        }

        private void uninstallSelectedEngine() {
            int row = engineTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select an engine version.");
                return;
            }

            GodotEngine engine = engines.get(row);
            if (!engine.isInstalled()) {
                JOptionPane.showMessageDialog(this, "This engine is not installed.");
                return;
            }

            int choice = JOptionPane.showConfirmDialog(this, 
                "Uninstall Godot " + engine.getVersion() + "?\n(Files will not be deleted from disk)", 
                "Confirm Uninstall", 
                JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                engine.setInstalled(false);
                engine.setInstalledPath("");
                refreshEngineTable();
                saveData();
            }
        }

        private void refreshEngineTable() {
            tableModel.setRowCount(0);
            for (GodotEngine engine : engines) {
                tableModel.addRow(new Object[]{
                    engine.getVersion(),
                    engine.isInstalled() ? "Installed" : "Not Installed",
                    engine.getSize(),
                    engine.isInstalled() ? engine.getInstalledPath() : ""
                });
            }
        }
    }

    // Inner class for Settings Panel
    class SettingsPanel extends JPanel {
        private JTextField projectLocationField;
        private JTextField engineLocationField;

        public SettingsPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            // Title
            JLabel titleLabel = new JLabel("Settings");
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
            
            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            titlePanel.add(titleLabel);
            
            // Settings form
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.anchor = GridBagConstraints.WEST;

            // Default Project Location
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.weightx = 0;
            JLabel projectLocLabel = new JLabel("Default Project Location:");
            projectLocLabel.setFont(projectLocLabel.getFont().deriveFont(Font.BOLD));
            formPanel.add(projectLocLabel, gbc);

            gbc.gridy = 1;
            formPanel.add(new JLabel("New projects will be created in this folder"), gbc);

            gbc.gridy = 2;
            gbc.weightx = 1;
            projectLocationField = new JTextField(defaultProjectLocation);
            projectLocationField.setPreferredSize(new Dimension(400, 30));
            JButton browseProjectBtn = new JButton("Browse...");
            JPanel projectPanel = new JPanel(new BorderLayout(5, 0));
            projectPanel.add(projectLocationField, BorderLayout.CENTER);
            projectPanel.add(browseProjectBtn, BorderLayout.EAST);
            formPanel.add(projectPanel, gbc);

            browseProjectBtn.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select Default Project Location");
                chooser.setSelectedFile(new File(projectLocationField.getText()));
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    projectLocationField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            });

            // Spacing
            gbc.gridy = 3;
            gbc.insets = new Insets(30, 10, 10, 10);
            formPanel.add(new JSeparator(), gbc);

            // Default Engine Location
            gbc.gridy = 4;
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.weightx = 0;
            JLabel engineLocLabel = new JLabel("Default Engine Installation Location:");
            engineLocLabel.setFont(engineLocLabel.getFont().deriveFont(Font.BOLD));
            formPanel.add(engineLocLabel, gbc);

            gbc.gridy = 5;
            formPanel.add(new JLabel("Godot engines will be downloaded and installed in this folder"), gbc);

            gbc.gridy = 6;
            gbc.weightx = 1;
            engineLocationField = new JTextField(defaultEngineLocation);
            engineLocationField.setPreferredSize(new Dimension(400, 30));
            JButton browseEngineBtn = new JButton("Browse...");
            JPanel enginePanel = new JPanel(new BorderLayout(5, 0));
            enginePanel.add(engineLocationField, BorderLayout.CENTER);
            enginePanel.add(browseEngineBtn, BorderLayout.EAST);
            formPanel.add(enginePanel, gbc);

            browseEngineBtn.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select Default Engine Installation Location");
                chooser.setSelectedFile(new File(engineLocationField.getText()));
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    engineLocationField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            });

            // Buttons
            gbc.gridy = 7;
            gbc.insets = new Insets(30, 10, 10, 10);
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton saveBtn = new JButton("Save Settings");
            JButton resetBtn = new JButton("Reset to Defaults");
            
            saveBtn.setPreferredSize(new Dimension(140, 35));
            resetBtn.setPreferredSize(new Dimension(140, 35));
            
            saveBtn.addActionListener(e -> saveSettings());
            resetBtn.addActionListener(e -> resetSettings());
            
            buttonPanel.add(resetBtn);
            buttonPanel.add(saveBtn);
            formPanel.add(buttonPanel, gbc);

            // Info panel
            JPanel infoPanel = new JPanel(new BorderLayout(10, 10));
            infoPanel.setBorder(BorderFactory.createTitledBorder("Information"));
            JTextArea infoText = new JTextArea(
                "• Project Location: Where new projects will be created by default\n" +
                "• Engine Location: Where Godot engines will be installed when downloaded\n" +
                "• These paths can be changed at any time\n" +
                "• Existing projects and engines will not be moved"
            );
            infoText.setEditable(false);
            infoText.setBackground(formPanel.getBackground());
            infoText.setWrapStyleWord(true);
            infoText.setLineWrap(true);
            infoPanel.add(infoText, BorderLayout.CENTER);

            add(titlePanel, BorderLayout.NORTH);
            add(formPanel, BorderLayout.CENTER);
            add(infoPanel, BorderLayout.SOUTH);
        }

        private void saveSettings() {
            String newProjectLoc = projectLocationField.getText().trim();
            String newEngineLoc = engineLocationField.getText().trim();
            
            if (newProjectLoc.isEmpty() || newEngineLoc.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Paths cannot be empty.",
                    "Invalid Settings",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            defaultProjectLocation = newProjectLoc;
            defaultEngineLocation = newEngineLoc;
            
            saveData();
            
            JOptionPane.showMessageDialog(this,
                "Settings saved successfully!",
                "Settings Saved",
                JOptionPane.INFORMATION_MESSAGE);
        }

        private void resetSettings() {
            int choice = JOptionPane.showConfirmDialog(this,
                "Reset settings to default values?",
                "Reset Settings",
                JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                defaultProjectLocation = System.getProperty("user.home") + File.separator + "GodotProjects";
                defaultEngineLocation = System.getProperty("user.home") + File.separator + "Godot";
                refreshSettings();
                saveData();
                
                JOptionPane.showMessageDialog(this,
                    "Settings reset to defaults.",
                    "Settings Reset",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }

        public void refreshSettings() {
            projectLocationField.setText(defaultProjectLocation);
            engineLocationField.setText(defaultEngineLocation);
        }
    }

    // Helper method to extract ZIP files
    private void extractZipFile(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[8192];
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // Create parent directories if needed
                    newFile.getParentFile().mkdirs();
                    
                    // Extract file
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
    
    // Helper method to find Godot executable in extracted folder
    private String findGodotExecutable(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        
        // Look for .exe files (prioritize those with "godot" in the name)
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".exe")) {
                if (file.getName().toLowerCase().contains("godot")) {
                    return file.getAbsolutePath();
                }
            }
        }
        
        // If no godot.exe found, look in subdirectories
        for (File file : files) {
            if (file.isDirectory()) {
                String result = findGodotExecutable(file);
                if (result != null) return result;
            }
        }
        
        // If still not found, return any .exe file
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".exe")) {
                return file.getAbsolutePath();
            }
        }
        
        return null;
    }

    // Data classes
    static class GodotEngine {
        private String version;
        private String size;
        private String downloadUrl;
        private boolean installed;
        private String installedPath;

        public GodotEngine(String version, String size, String downloadUrl) {
            this.version = version;
            this.size = size;
            this.downloadUrl = downloadUrl;
            this.installed = false;
            this.installedPath = "";
        }

        public String getVersion() { return version; }
        public String getSize() { return size; }
        public String getDownloadUrl() { return downloadUrl; }
        public boolean isInstalled() { return installed; }
        public String getInstalledPath() { return installedPath; }
        public void setInstalled(boolean installed) { this.installed = installed; }
        public void setInstalledPath(String path) { this.installedPath = path; }
    }

    static class GodotProject {
        private String name;
        private String path;
        private String engineVersion;
        private String lastOpened;

        public GodotProject(String name, String path, String engineVersion) {
            this.name = name;
            this.path = path;
            this.engineVersion = engineVersion;
            this.lastOpened = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        }

        public String getName() { return name; }
        public String getPath() { return path; }
        public String getEngineVersion() { return engineVersion; }
        public String getLastOpened() { return lastOpened; }
        
        public void setEngineVersion(String engineVersion) {
            this.engineVersion = engineVersion;
        }
        
        public void setLastOpened(String lastOpened) {
            this.lastOpened = lastOpened;
        }
        
        public void updateLastOpened() {
            this.lastOpened = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        }
    }
}