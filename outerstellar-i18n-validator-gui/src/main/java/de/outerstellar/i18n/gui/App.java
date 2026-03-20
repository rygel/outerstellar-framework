package de.outerstellar.i18n.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import de.outerstellar.i18n.core.Config;
import de.outerstellar.i18n.core.I18nValidator;
import de.outerstellar.i18n.core.Statistics;
import de.outerstellar.i18n.core.ValidationResult;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Swing GUI for the i18n validator.
 */
public class App extends JFrame {

    private final JTextField resourcesField = new JTextField(30);
    private final JTextField projectField = new JTextField(30);
    private final JTextArea logArea = new JTextArea(8, 60);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Type", "Status", "Key", "Details"}, 0
    );
    private final JTable resultsTable = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel("Ready");

    public App() {
        super("i18n Validator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        initUI();
        pack();
        setLocationRelativeTo(null);
    }

    private void initUI() {
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        top.add(new JLabel("Resources:"), c);
        c.gridx = 1; c.weightx = 1;
        top.add(resourcesField, c);
        c.gridx = 2; c.weightx = 0;
        JButton browseRes = new JButton("Browse");
        browseRes.addActionListener(e -> browse(resourcesField));
        top.add(browseRes, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        top.add(new JLabel("Project:"), c);
        c.gridx = 1; c.weightx = 1;
        top.add(projectField, c);
        c.gridx = 2; c.weightx = 0;
        JButton browseProj = new JButton("Browse");
        browseProj.addActionListener(e -> browse(projectField));
        top.add(browseProj, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 3;
        JButton validateBtn = new JButton("Validate");
        validateBtn.addActionListener(e -> runValidation());
        top.add(validateBtn, c);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(resultsTable), new JScrollPane(logArea));
        split.setDividerLocation(300);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void browse(JTextField field) {
        JFileChooser fc = new JFileChooser(field.getText().isBlank() ? "." : field.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void runValidation() {
        tableModel.setRowCount(0);
        logArea.setText("");
        statusLabel.setText("Validating...");

        SwingWorker<List<ValidationResult>, String> worker = new SwingWorker<>() {
            private Statistics stats;

            @Override
            protected List<ValidationResult> doInBackground() {
                Config config = Config.builder()
                        .resourcesPath(resourcesField.getText())
                        .projectPath(projectField.getText().isBlank() ? null : projectField.getText())
                        .build();

                I18nValidator validator = new I18nValidator(config);
                List<ValidationResult> results = validator.validate(this::publish);
                stats = validator.getStatistics();
                return results;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    logArea.append(msg + "\n");
                }
            }

            @Override
            protected void done() {
                try {
                    List<ValidationResult> results = get();
                    for (ValidationResult r : results) {
                        if (r.status() != ValidationResult.Status.OK) {
                            tableModel.addRow(new Object[]{r.type(), r.status(), r.key(), r.details()});
                        }
                    }
                    statusLabel.setText(stats.isValid() ? "Validation passed! " + stats : "Issues found: " + stats);
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}
