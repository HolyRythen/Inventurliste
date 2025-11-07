import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Inventurliste – pure Java Swing, eine Datei, CSV-Persistenz im Nutzerordner */
public class InventurListeSwing extends JFrame {
    // --- UI ---
    private final JTextField searchField = new JTextField();
    private final JButton addBtn = new JButton("Neu");
    private final JButton editBtn = new JButton("Bearbeiten");
    private final JButton delBtn = new JButton("Löschen");
    private final JButton importBtn = new JButton("Import CSV");
    private final JButton exportBtn = new JButton("Export CSV");
    private final JLabel status = new JLabel("Bereit.");

    private final ItemTableModel model = new ItemTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<ItemTableModel> sorter = new TableRowSorter<>(model);

    // Persistenz
    private final Path storage = Path.of(System.getProperty("user.home"), "inventurliste.csv");
    private boolean dirty = false;

    public InventurListeSwing() {
        super("Inventurliste (Swing, CSV) – by Robert");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 600));
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        // Topbar
        JPanel top = new JPanel(new BorderLayout(8,8));
        searchField.putClientProperty("JTextField.placeholderText", "Suche (Name/Kategorie/Standort/Notiz) …");
        top.add(new JLabel("Suche:"), BorderLayout.WEST);
        top.add(searchField, BorderLayout.CENTER);

        // Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(delBtn);
        actions.add(Box.createHorizontalStrut(16));
        actions.add(importBtn);
        actions.add(exportBtn);

        // Table
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);

        // Layout
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(8,8));
        south.add(actions, BorderLayout.WEST);
        south.add(status, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        // Events
        addBtn.addActionListener(this::onAdd);
        editBtn.addActionListener(this::onEdit);
        delBtn.addActionListener(this::onDelete);
        importBtn.addActionListener(this::onImport);
        exportBtn.addActionListener(this::onExport);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                String q = searchField.getText().trim().toLowerCase(Locale.ROOT);
                sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + PatternUtil.escapeRegex(q)));
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        // Daten laden
        loadFromDisk();
        pack();
        setLocationRelativeTo(null);

        // Autosave auf close
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (dirty) saveToDisk(false);
            }
        });
    }

    // --- Actions ---
    private void onAdd(ActionEvent e) {
        Item it = ItemDialog.show(this, null);
        if (it == null) return;
        model.add(it);
        dirty = true;
        setStatus("Hinzugefügt: " + it.name);
    }
    private void onEdit(ActionEvent e) {
        int view = table.getSelectedRow();
        if (view < 0) { msg("Bitte einen Eintrag auswählen."); return; }
        int idx = table.convertRowIndexToModel(view);
        Item cur = model.get(idx);
        Item edited = ItemDialog.show(this, cur);
        if (edited == null) return;
        model.set(idx, edited);
        dirty = true;
        setStatus("Aktualisiert: " + edited.name);
    }
    private void onDelete(ActionEvent e) {
        int view = table.getSelectedRow();
        if (view < 0) { msg("Bitte einen Eintrag auswählen."); return; }
        int ok = JOptionPane.showConfirmDialog(this, "Eintrag wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        int idx = table.convertRowIndexToModel(view);
        Item it = model.remove(idx);
        dirty = true;
        setStatus("Gelöscht: " + it.name);
    }
    private void onImport(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("CSV importieren");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                List<Item> items = CsvUtil.read(f.toPath());
                model.setAll(items);
                dirty = true;
                setStatus("Importiert: " + f.getName() + " (" + items.size() + ")");
            } catch (Exception ex) {
                msg("Import-Fehler: " + ex.getMessage());
            }
        }
    }
    private void onExport(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("CSV exportieren");
        fc.setSelectedFile(new File("inventur-export.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File out = fc.getSelectedFile();
            try {
                CsvUtil.write(out.toPath(), model.items);
                setStatus("Export gespeichert: " + out.getAbsolutePath());
            } catch (Exception ex) {
                msg("Export-Fehler: " + ex.getMessage());
            }
        }
    }

    // --- Persistenz ---
    private void loadFromDisk() {
        if (Files.isRegularFile(storage)) {
            try {
                List<Item> items = CsvUtil.read(storage);
                model.setAll(items);
                setStatus("Geladen: " + storage.toAbsolutePath());
            } catch (Exception ex) {
                setStatus("Konnte vorhandene CSV nicht lesen („" + storage.getFileName() + "“).");
            }
        } else {
            setStatus("Neues Verzeichnis – CSV wird unter " + storage.toAbsolutePath() + " gespeichert.");
        }
    }
    private void saveToDisk(boolean showMsg) {
        try {
            CsvUtil.write(storage, model.items);
            dirty = false;
            if (showMsg) msg("Gespeichert: " + storage.toAbsolutePath());
        } catch (Exception ex) {
            msg("Speicher-Fehler: " + ex.getMessage());
        }
    }

    private static void msg(String s) { JOptionPane.showMessageDialog(null, s); }
    private void setStatus(String s) { status.setText(s); }

    // --- Model/DTO ---
    private static class Item {
        String id;           // UUID
        String name;
        String kategorie;
        String standort;
        int menge;
        String notiz;
        String erstellt;     // ISO datetime
        String geändert;     // ISO datetime
        Item() {}
    }

    private static class ItemTableModel extends AbstractTableModel {
        final String[] cols = {"ID", "Name", "Kategorie", "Standort", "Menge", "Notiz", "Erstellt", "Geändert"};
        final List<Item> items = new ArrayList<>();
        public int getRowCount() { return items.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }
        public Object getValueAt(int r, int c) {
            Item i = items.get(r);
            return switch (c) {
                case 0 -> i.id;
                case 1 -> i.name;
                case 2 -> i.kategorie;
                case 3 -> i.standort;
                case 4 -> i.menge;
                case 5 -> i.notiz;
                case 6 -> i.erstellt;
                case 7 -> i.geändert;
                default -> "";
            };
        }
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 4 -> Integer.class;
                default -> String.class;
            };
        }
        void add(Item i) { items.add(0, i); fireTableRowsInserted(0,0); }
        Item get(int idx) { return items.get(idx); }
        void set(int idx, Item i) { items.set(idx, i); fireTableRowsUpdated(idx, idx); }
        Item remove(int idx) { Item it = items.remove(idx); fireTableRowsDeleted(idx, idx); return it; }
        void setAll(List<Item> list) { items.clear(); items.addAll(list); fireTableDataChanged(); }
    }

    // --- Dialog ---
    private static class ItemDialog extends JDialog {
        private final JTextField name = new JTextField();
        private final JTextField kat = new JTextField();
        private final JTextField ort = new JTextField();
        private final JSpinner menge = new JSpinner(new SpinnerNumberModel(1, 0, 1_000_000, 1));
        private final JTextArea note = new JTextArea(5, 30);
        private Item result;

        private ItemDialog(Frame owner, Item edit) {
            super(owner, true);
            setTitle(edit == null ? "Eintrag hinzufügen" : "Eintrag bearbeiten");
            setLayout(new BorderLayout(8,8));

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(4,4,4,4); g.anchor = GridBagConstraints.WEST; g.fill = GridBagConstraints.HORIZONTAL; g.weightx=1;

            int r=0;
            g.gridx=0; g.gridy=r; g.weightx=0; form.add(new JLabel("Name*"), g);
            g.gridx=1; g.gridy=r++; g.weightx=1; form.add(name, g);

            g.gridx=0; g.gridy=r; g.weightx=0; form.add(new JLabel("Kategorie"), g);
            g.gridx=1; g.gridy=r++; g.weightx=1; form.add(kat, g);

            g.gridx=0; g.gridy=r; g.weightx=0; form.add(new JLabel("Standort"), g);
            g.gridx=1; g.gridy=r++; g.weightx=1; form.add(ort, g);

            g.gridx=0; g.gridy=r; g.weightx=0; form.add(new JLabel("Menge"), g);
            g.gridx=1; g.gridy=r++; g.weightx=1; form.add(menge, g);

            g.gridx=0; g.gridy=r; g.weightx=0; form.add(new JLabel("Notiz"), g);
            g.gridx=1; g.gridy=r++; g.weightx=1; form.add(new JScrollPane(note), g);

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton ok = new JButton("OK");
            JButton cancel = new JButton("Abbrechen");
            btns.add(ok); btns.add(cancel);

            add(form, BorderLayout.CENTER);
            add(btns, BorderLayout.SOUTH);

            ok.addActionListener(ev -> {
                String n = name.getText().trim();
                if (n.isEmpty()) { JOptionPane.showMessageDialog(this, "Name ist Pflicht."); return; }
                Item out = (edit == null ? new Item() : edit);
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                if (edit == null) {
                    out.id = UUID.randomUUID().toString();
                    out.erstellt = fmt.format(now);
                }
                out.name = n;
                out.kategorie = kat.getText().trim();
                out.standort = ort.getText().trim();
                out.menge = ((Number)menge.getValue()).intValue();
                out.notiz = note.getText().trim();
                out.geändert = fmt.format(now);
                result = out;
                dispose();
            });
            cancel.addActionListener(ev -> { result = null; dispose(); });

            if (edit != null) {
                name.setText(edit.name);
                kat.setText(edit.kategorie);
                ort.setText(edit.standort);
                menge.setValue(edit.menge);
                note.setText(edit.notiz);
            }

            pack();
            setLocationRelativeTo(owner);
        }

        static Item show(Frame owner, Item edit) {
            ItemDialog d = new ItemDialog(owner, edit);
            d.setVisible(true);
            return d.result;
        }
    }

    // --- CSV Util ---
    private static class CsvUtil {
        static final String[] HEADER = {"id","name","kategorie","standort","menge","notiz","erstellt","geaendert"};

        static List<Item> read(Path p) throws IOException {
            List<Item> out = new ArrayList<>();
            try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                String line;
                boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] t = parseCsvLine(line);
                    if (first) { first = false; if (isHeader(t)) continue; } // Header überspringen
                    Item i = new Item();
                    i.id = val(t,0);
                    i.name = val(t,1);
                    i.kategorie = val(t,2);
                    i.standort = val(t,3);
                    i.menge = parseInt(val(t,4), 0);
                    i.notiz = val(t,5);
                    i.erstellt = val(t,6);
                    i.geändert = val(t,7);
                    if (i.id == null || i.id.isBlank()) i.id = UUID.randomUUID().toString();
                    if (i.erstellt == null || i.erstellt.isBlank()) i.erstellt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
                    if (i.geändert == null || i.geändert.isBlank()) i.geändert = i.erstellt;
                    out.add(i);
                }
            }
            return out;
        }

        static void write(Path p, List<Item> items) throws IOException {
            try (BufferedWriter bw = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                bw.write(String.join(",", HEADER)); bw.newLine();
                for (Item i : items) {
                    String[] row = {
                            i.id, esc(i.name), esc(i.kategorie), esc(i.standort),
                            Integer.toString(i.menge), esc(i.notiz),
                            i.erstellt, i.geändert
                    };
                    bw.write(String.join(",", row)); bw.newLine();
                }
            }
        }

        private static String[] parseCsvLine(String line) {
            List<String> cells = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            boolean inQ = false;
            for (int pos=0; pos<line.length(); pos++) {
                char ch = line.charAt(pos);
                if (inQ) {
                    if (ch=='"') {
                        if (pos+1<line.length() && line.charAt(pos+1)=='"') { cur.append('"'); pos++; }
                        else inQ = false;
                    } else cur.append(ch);
                } else {
                    if (ch=='"') inQ = true;
                    else if (ch==',') { cells.add(cur.toString()); cur.setLength(0); }
                    else cur.append(ch);
                }
            }
            cells.add(cur.toString());
            return cells.toArray(new String[0]);
        }
        private static String esc(String s) {
            if (s==null) return "";
            if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                return "\"" + s.replace("\"","\"\"") + "\"";
            }
            return s;
        }
        private static boolean isHeader(String[] t) {
            if (t.length < 2) return false;
            String a = t[0].toLowerCase(Locale.ROOT);
            String b = t[1].toLowerCase(Locale.ROOT);
            return a.contains("id") && b.contains("name");
        }
        private static String val(String[] t, int i) { return i < t.length ? t[i] : ""; }
        private static int parseInt(String s, int def) {
            try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
        }
    }

    // Regex Escape für Filter
    private static class PatternUtil {
        static String escapeRegex(String s) {
            StringBuilder out = new StringBuilder();
            for (char c : s.toCharArray()) {
                if ("[](){}.*+?$^|#\\".indexOf(c) >= 0) out.append('\\');
                out.append(c);
            }
            return out.toString();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventurListeSwing().setVisible(true));
    }
}
