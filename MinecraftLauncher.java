import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import java.util.zip.*;

public class MinecraftLauncher extends JFrame {

    // ── Constants ────────────────────────────────────────────
    static final String CLIENT_ID  = "9b7eb15d-5fc3-410b-8165-391bff5d483c";
    static final String MANIFEST   = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
    static final String MC_DIR     = getMCDir();
    static final File   CONFIG_DIR = new File(System.getProperty("user.home"), ".craftlaunch");
    static final File   CONFIG_FILE= new File(CONFIG_DIR, "config.properties");
    final  java.util.Properties config = new java.util.Properties();

    // ── Palette ──────────────────────────────────────────────
    static final Color BG     = new Color(10,  12, 16);
    static final Color BG2    = new Color(18,  22, 30);
    static final Color CARD   = new Color(24,  30, 42);
    static final Color GREEN  = new Color(74, 222,128);
    static final Color BLUE   = new Color(96, 165,250);
    static final Color RED    = new Color(248, 113,113);
    static final Color YELLOW = new Color(251, 191, 36);
    static final Color TEXT   = new Color(230,235,245);
    static final Color MUTED  = new Color(100,115,140);
    static final Color BORDER = new Color(40,  50, 70);

    // ── Auth state ───────────────────────────────────────────
    volatile String mcToken    = null;
    volatile String playerName = null;
    volatile String playerUUID = null;

    // ── UI components ────────────────────────────────────────
    JLabel            accountLabel;
    JTextField        usernameField;
    JButton           signInBtn;
    JComboBox<String> versionBox;
    JComboBox<String> loaderBox;
    JSlider           ramSlider;
    JLabel            ramLabel;
    JTextArea         logArea;
    JProgressBar      progress;
    JButton           launchBtn;
    JPanel            modsListPanel;
    JTextField        modSearchField;
    JPanel            modsResultPanel;
    JLabel            statusBar;
    javax.swing.Timer glowTimer;
    float             glowPhase = 0f;
    volatile boolean  launching = false;
    Process           gameProcess = null;

    // version display-name -> json-url
    final Map<String,String> versionMap = new LinkedHashMap<>();

    // ════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════
    public MinecraftLauncher() {
        setTitle("CraftLaunch — Minecraft Launcher");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1020, 680);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        try { setIconImage(Theme.makeAppIcon()); } catch (Exception ignored) {}

        // Wrap whole window in animated starfield background
        Theme.StarfieldPanel root = new Theme.StarfieldPanel();
        root.setLayout(new BorderLayout());
        setContentPane(root);

        loadConfig();

        buildUI();

        // Apply saved settings to UI
        String savedUser = config.getProperty("username");
        if (savedUser != null && !savedUser.isEmpty()) usernameField.setText(savedUser);

        String savedLoader = config.getProperty("loader");
        if (savedLoader != null) {
            for (int i = 0; i < loaderBox.getItemCount(); i++) {
                if (loaderBox.getItemAt(i).equals(savedLoader)) { loaderBox.setSelectedIndex(i); break; }
            }
        }

        String savedRam = config.getProperty("ram");
        if (savedRam != null) {
            try { ramSlider.setValue(Integer.parseInt(savedRam)); } catch (Exception ignored) {}
        }

        // Save on close
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { saveConfig(); }
        });

        glowTimer = new javax.swing.Timer(40, e -> { glowPhase += .06f; if (launchBtn!=null) launchBtn.repaint(); });
        glowTimer.start();
        setVisible(true);
        loadVersions();
        log("  CraftLaunch v5.0 — full visual redesign + Microsoft sign-in!");
        runDiagnostics();
        log("");
        log("  HOW TO PLAY:");
        log("    OFFLINE: Type a username (top-right) and click LAUNCH.");
        log("    ONLINE : Click 'Sign in with Microsoft' (one-time setup).");
        log("");
        log("");
        log("  HOW TO PLAY:");
        log("    1. Type your username in the top-right box");
        log("    2. Pick a version on the left");
        log("    3. Pick a mod loader: Vanilla or Fabric");
        log("    4. Click LAUNCH MINECRAFT");
        log("");
        log("  WANT MODS?");
        log("    - Set Mod Loader to 'Fabric (mods enabled)'");
        log("    - Open the MODS tab and search Modrinth");
        log("    - Click Install on the mods you want");
        log("    - Then launch the game!");
        log("");
        log("  Note: Microsoft online sign-in is broken (their old");
        log("  Xbox client ID was disabled). Offline mode works fine");
        log("  for singleplayer and most modded servers.");
        log("");
    }

    // ════════════════════════════════════════════════════════
    //  UI BUILD
    // ════════════════════════════════════════════════════════
    void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new Theme.EmeraldTabbedPaneUI());
        tabs.setOpaque(false);
        tabs.setFont(Theme.LABEL);
        tabs.addTab("PLAY",     buildPlayTab());
        tabs.addTab("MODS",     buildModsTab());
        tabs.addTab("SETTINGS", buildSettingsTab());
        add(tabs, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────
    JPanel buildHeader() {
        Theme.HeaderPanel h = new Theme.HeaderPanel();
        h.setLayout(new BorderLayout());
        h.setBorder(new EmptyBorder(16, 24, 16, 24));

        // ── Left: animated diamond + brand ──
        JPanel L = opaque(new FlowLayout(FlowLayout.LEFT, 12, 0));
        Theme.DiamondIcon icon = new Theme.DiamondIcon(28);
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel logo = Theme.hLabel("CRAFTLAUNCH", Theme.TITLE, Theme.EMERALD);
        JLabel tag  = Theme.hLabel("v4.0  Minecraft Java Edition", Theme.SMALL, Theme.TEXT_DIM);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        tag.setAlignmentX(LEFT_ALIGNMENT);
        brand.add(logo);
        brand.add(tag);
        L.add(icon);
        L.add(Box.createHorizontalStrut(2));
        L.add(brand);

        // ── Right: account section ──
        JPanel R = opaque(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JPanel userBox = new JPanel();
        userBox.setOpaque(false);
        userBox.setLayout(new BoxLayout(userBox, BoxLayout.Y_AXIS));
        accountLabel = Theme.hLabel("PLAYER NAME", Theme.LABEL, Theme.MUTED);
        accountLabel.setAlignmentX(RIGHT_ALIGNMENT);
        userBox.add(accountLabel);
        userBox.add(Box.createVerticalStrut(2));

        usernameField = new Theme.RoundedTextField("Player", Theme.BG_INPUT);
        usernameField.setForeground(Theme.GOLD);
        usernameField.setCaretColor(Theme.GOLD);
        usernameField.setFont(Theme.BODY_B);
        usernameField.setColumns(12);
        usernameField.setMaximumSize(new Dimension(180, 36));
        usernameField.setPreferredSize(new Dimension(180, 36));
        usernameField.setHorizontalAlignment(JTextField.CENTER);
        usernameField.setAlignmentX(RIGHT_ALIGNMENT);
        usernameField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { usernameField.selectAll(); }
            public void focusLost  (FocusEvent e) { setupOfflinePlayer(usernameField.getText()); }
        });
        userBox.add(usernameField);

        signInBtn = new Theme.PolishButton(
            "Sign in with Microsoft", Theme.BG_INPUT, Theme.DIAMOND, false);
        signInBtn.setFont(Theme.SMALL);
        signInBtn.addActionListener(e -> { if (mcToken!=null) signOut(); else startMicrosoftLogin(); });

        R.add(userBox);
        R.add(Box.createHorizontalStrut(8));
        R.add(signInBtn);

        h.add(L, BorderLayout.WEST);
        h.add(R, BorderLayout.EAST);
        return h;
    }

    // ── Play tab ──────────────────────────────────────────
    JPanel buildPlayTab() {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(22, 24, 18, 24));

        // ═══ Left: settings card ═══
        Theme.Card L = new Theme.Card();
        L.setLayout(new BoxLayout(L, BoxLayout.Y_AXIS));
        L.setPreferredSize(new Dimension(330, 0));
        L.setBorder(new EmptyBorder(22, 24, 22, 24));

        L.add(Theme.sectionLabel("◆  VERSION"));
        L.add(Box.createVerticalStrut(8));
        versionBox = new JComboBox<>();
        themeCombo(versionBox);
        L.add(versionBox);
        L.add(Box.createVerticalStrut(16));

        L.add(Theme.sectionLabel("◆  MOD LOADER"));
        L.add(Box.createVerticalStrut(8));
        loaderBox = new JComboBox<>(new String[]{"Vanilla (no mods)", "Fabric (mods enabled)"});
        themeCombo(loaderBox);
        L.add(loaderBox);
        L.add(Box.createVerticalStrut(16));

        L.add(Theme.sectionLabel("◆  ALLOCATED RAM"));
        L.add(Box.createVerticalStrut(8));
        JPanel ramRow = new JPanel(new BorderLayout(10, 0));
        ramRow.setOpaque(false);
        ramRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        ramRow.setAlignmentX(LEFT_ALIGNMENT);
        ramSlider = new JSlider(1, 16, 4);
        ramSlider.setOpaque(false);
        ramSlider.setUI(new Theme.EmeraldSliderUI(ramSlider));
        ramLabel = Theme.hLabel("4 GB", new Font("Monospaced", Font.BOLD, 14), Theme.DIAMOND);
        ramSlider.addChangeListener(e -> ramLabel.setText(ramSlider.getValue() + " GB"));
        ramRow.add(ramSlider, BorderLayout.CENTER);
        ramRow.add(ramLabel,  BorderLayout.EAST);
        L.add(ramRow);
        L.add(Box.createVerticalStrut(18));

        L.add(Theme.sectionLabel("◆  GAME FOLDER"));
        L.add(Box.createVerticalStrut(6));
        String shortDir = MC_DIR;
        if (shortDir.length() > 36) shortDir = "..." + shortDir.substring(shortDir.length() - 33);
        JLabel dirLbl = Theme.hLabel(shortDir, Theme.TINY, Theme.MUTED);
        dirLbl.setAlignmentX(LEFT_ALIGNMENT);
        L.add(dirLbl);
        L.add(Box.createVerticalStrut(8));

        JPanel dirBtns = new JPanel(new GridLayout(1, 2, 8, 0));
        dirBtns.setOpaque(false);
        dirBtns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        dirBtns.setAlignmentX(LEFT_ALIGNMENT);
        Theme.PolishButton openGameDirBtn = new Theme.PolishButton(
            "📁 .minecraft", Theme.BG_INPUT, Theme.DIAMOND, false);
        openGameDirBtn.setFont(Theme.TINY);
        openGameDirBtn.setBorder(new EmptyBorder(6, 6, 6, 6));
        openGameDirBtn.addActionListener(e -> {
            try { File d = new File(MC_DIR); d.mkdirs(); Desktop.getDesktop().open(d); }
            catch (Exception ex) { log("  Could not open folder."); }
        });
        Theme.PolishButton openCrashBtn = new Theme.PolishButton(
            "⚠ Crash Logs", Theme.BG_INPUT, Theme.RUBY, false);
        openCrashBtn.setFont(Theme.TINY);
        openCrashBtn.setBorder(new EmptyBorder(6, 6, 6, 6));
        openCrashBtn.addActionListener(e -> {
            try {
                File d = new File(MC_DIR, "crash-reports");
                if (!d.exists() || d.listFiles() == null || d.listFiles().length == 0) {
                    JOptionPane.showMessageDialog(this, "No crash reports yet — that's good!",
                        "Crash Reports", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                Desktop.getDesktop().open(d);
            } catch (Exception ex) { log("  Could not open crash reports."); }
        });
        dirBtns.add(openGameDirBtn);
        dirBtns.add(openCrashBtn);
        L.add(dirBtns);
        L.add(Box.createVerticalStrut(14));

        Theme.PolishButton killBtn = new Theme.PolishButton(
            "■  STOP GAME", new Color(75, 25, 25), Theme.RUBY, false);
        killBtn.setAlignmentX(CENTER_ALIGNMENT);
        killBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        killBtn.setVisible(false);
        killBtn.addActionListener(e -> { if (gameProcess != null) gameProcess.destroy(); });
        L.add(killBtn);

        L.add(Box.createVerticalGlue());

        // Hero launch button
        launchBtn = new Theme.LaunchButton();
        launchBtn.setAlignmentX(CENTER_ALIGNMENT);
        launchBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        wireLaunchAction(launchBtn, killBtn);
        L.add(launchBtn);

        // ═══ Right: log card ═══
        Theme.Card R = new Theme.Card();
        R.setLayout(new BorderLayout(0, 12));
        R.setBorder(new EmptyBorder(22, 24, 22, 24));

        JPanel logHead = new JPanel(new BorderLayout());
        logHead.setOpaque(false);
        logHead.add(Theme.sectionLabel("◆  LAUNCH LOG"), BorderLayout.WEST);
        JLabel statusDot = Theme.hLabel("●  READY", Theme.LABEL, Theme.EMERALD);
        logHead.add(statusDot, BorderLayout.EAST);
        R.add(logHead, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Theme.BG_DEEP);
        logArea.setForeground(new Color(160, 220, 180));
        logArea.setFont(Theme.BODY);
        logArea.setBorder(new EmptyBorder(12, 14, 12, 14));
        logArea.setCaretColor(Theme.EMERALD);

        JScrollPane sc = new JScrollPane(logArea);
        sc.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sc.getViewport().setBackground(Theme.BG_DEEP);
        sc.getVerticalScrollBar().setUI(new Theme.EmeraldScrollBarUI());
        sc.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        R.add(sc, BorderLayout.CENTER);

        progress = new JProgressBar(0, 100);
        progress.setStringPainted(true);
        progress.setString("Ready");
        progress.setOpaque(false);
        progress.setBorderPainted(false);
        progress.setUI(new Theme.EmeraldProgressBarUI());
        progress.setPreferredSize(new Dimension(0, 22));
        R.add(progress, BorderLayout.SOUTH);

        p.add(L, BorderLayout.WEST);
        p.add(R, BorderLayout.CENTER);
        return p;
    }

    void themeCombo(JComboBox<String> box) {
        box.setUI(new Theme.RoundedComboBoxUI());
        box.setBackground(Theme.BG_INPUT);
        box.setForeground(Theme.TEXT);
        box.setFont(Theme.BODY);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        box.setAlignmentX(LEFT_ALIGNMENT);
        box.setBorder(BorderFactory.createEmptyBorder());

        // Custom row renderer
        box.setRenderer(new javax.swing.ListCellRenderer<Object>() {
            JLabel l = new JLabel();
            { l.setOpaque(true); l.setFont(Theme.BODY); l.setBorder(new EmptyBorder(8, 12, 8, 12)); }
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int idx, boolean sel, boolean focus) {
                l.setText(value == null ? "" : value.toString());
                l.setBackground(sel ? Theme.BG_HOVER : Theme.BG_INPUT);
                l.setForeground(sel ? Theme.EMERALD : Theme.TEXT);
                return l;
            }
        });

        // Force the dropdown list itself to be dark — Swing leaks the system L&F here
        Object popup = box.getUI().getAccessibleChild(box, 0);
        if (popup instanceof javax.swing.plaf.basic.ComboPopup cp) {
            JList<?> list = cp.getList();
            list.setBackground(Theme.BG_INPUT);
            list.setForeground(Theme.TEXT);
            list.setSelectionBackground(Theme.BG_HOVER);
            list.setSelectionForeground(Theme.EMERALD);
        }
        if (popup instanceof Container popupCt) {
            popupCt.setBackground(Theme.BG_INPUT);
            for (Component c : popupCt.getComponents()) {
                if (c instanceof JScrollPane sp) {
                    sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER_BR));
                    sp.getViewport().setBackground(Theme.BG_INPUT);
                    sp.setBackground(Theme.BG_INPUT);
                    sp.getVerticalScrollBar().setUI(new Theme.EmeraldScrollBarUI());
                    sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
                }
            }
        }
    }

    void wireLaunchAction(JButton b, JButton killBtn) {
        b.addActionListener(e -> {
            if (mcToken == null) {
                setupOfflinePlayer(usernameField.getText());
                if (mcToken == null) {
                    JOptionPane.showMessageDialog(this,
                        "Please type a username (3-16 letters/numbers) in the top-right field.",
                        "Username Required", JOptionPane.WARNING_MESSAGE);
                    usernameField.requestFocus();
                    return;
                }
            }
            if (versionBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "No version selected.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String vid = ((String) versionBox.getSelectedItem()).split(" ")[0];
            b.setEnabled(false);
            killBtn.setVisible(true);
            launchGame(vid, b, killBtn);
        });
    }

    // ── Mods tab ──────────────────────────────────────────
    JPanel buildModsTab() {
        JPanel p = new JPanel(new GridLayout(1, 2, 20, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(22, 24, 18, 24));

        // ═══ Left: installed mods ═══
        Theme.Card inst = new Theme.Card();
        inst.setLayout(new BorderLayout(0, 12));
        inst.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel iTop = new JPanel(new BorderLayout(8, 0));
        iTop.setOpaque(false);
        iTop.add(Theme.sectionLabel("◆  INSTALLED MODS"), BorderLayout.WEST);
        Theme.PolishButton openFolderBtn = new Theme.PolishButton(
            "Open Folder", Theme.BG_INPUT, Theme.DIAMOND, false);
        openFolderBtn.setFont(Theme.TINY);
        openFolderBtn.addActionListener(e -> {
            try { File d = new File(MC_DIR, "mods"); d.mkdirs(); Desktop.getDesktop().open(d); }
            catch (Exception ex) { log("  Could not open folder."); }
        });
        iTop.add(openFolderBtn, BorderLayout.EAST);
        inst.add(iTop, BorderLayout.NORTH);

        modsListPanel = new JPanel();
        modsListPanel.setLayout(new BoxLayout(modsListPanel, BoxLayout.Y_AXIS));
        modsListPanel.setBackground(Theme.BG_DEEP);
        JScrollPane msc = new JScrollPane(modsListPanel);
        msc.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        msc.getViewport().setBackground(Theme.BG_DEEP);
        msc.getVerticalScrollBar().setUI(new Theme.EmeraldScrollBarUI());
        msc.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        inst.add(msc, BorderLayout.CENTER);

        JPanel modBtnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        modBtnRow.setOpaque(false);
        Theme.PolishButton refreshBtn = new Theme.PolishButton(
            "↻  REFRESH", Theme.BG_INPUT, Theme.EMERALD, false);
        refreshBtn.setFont(Theme.SMALL);
        refreshBtn.addActionListener(e -> loadMods());

        Theme.PolishButton clearBtn = new Theme.PolishButton(
            "✕  CLEAR ALL", new Color(50, 20, 20), Theme.RUBY, false);
        clearBtn.setFont(Theme.SMALL);
        clearBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this,
                "Delete ALL mods from the mods folder?\n\n(Useful for fixing crashes from broken mods.)",
                "Clear All Mods", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c == JOptionPane.YES_OPTION) {
                File dir = new File(MC_DIR, "mods");
                File[] mods = dir.listFiles();
                if (mods != null) {
                    int deleted = 0;
                    for (File f : mods) {
                        String n = f.getName();
                        if (n.endsWith(".jar") || n.endsWith(".jar.disabled")) {
                            if (f.delete()) deleted++;
                        }
                    }
                    log("  Cleared " + deleted + " mods from folder.");
                    loadMods();
                }
            }
        });
        modBtnRow.add(refreshBtn);
        modBtnRow.add(clearBtn);
        inst.add(modBtnRow, BorderLayout.SOUTH);

        // ═══ Right: Modrinth search ═══
        Theme.Card srch = new Theme.Card();
        srch.setLayout(new BorderLayout(0, 12));
        srch.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel sTop = new JPanel();
        sTop.setLayout(new BoxLayout(sTop, BoxLayout.Y_AXIS));
        sTop.setOpaque(false);
        JLabel modrinthLbl = Theme.hLabel("◆  SEARCH MODRINTH", Theme.LABEL, Theme.MUTED);
        modrinthLbl.setAlignmentX(LEFT_ALIGNMENT);
        sTop.add(modrinthLbl);
        sTop.add(Box.createVerticalStrut(10));

        JPanel sRow = new JPanel(new BorderLayout(10, 0));
        sRow.setOpaque(false);
        sRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        sRow.setAlignmentX(LEFT_ALIGNMENT);
        modSearchField = new Theme.RoundedTextField("", Theme.BG_INPUT);
        modSearchField.setText("");
        Theme.PolishButton goBtn = new Theme.PolishButton(
            "SEARCH", Theme.EMERALD, new Color(8, 18, 12), true);
        goBtn.setFont(Theme.BODY_B);
        goBtn.setPreferredSize(new Dimension(100, 38));
        Runnable doSearch = () -> searchModrinth(modSearchField.getText().trim());
        goBtn.addActionListener(e -> doSearch.run());
        modSearchField.addActionListener(e -> doSearch.run());
        sRow.add(modSearchField, BorderLayout.CENTER);
        sRow.add(goBtn, BorderLayout.EAST);
        sTop.add(sRow);
        srch.add(sTop, BorderLayout.NORTH);

        modsResultPanel = new JPanel();
        modsResultPanel.setLayout(new BoxLayout(modsResultPanel, BoxLayout.Y_AXIS));
        modsResultPanel.setBackground(Theme.BG_DEEP);
        JScrollPane rsc = new JScrollPane(modsResultPanel);
        rsc.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        rsc.getViewport().setBackground(Theme.BG_DEEP);
        rsc.getVerticalScrollBar().setUI(new Theme.EmeraldScrollBarUI());
        rsc.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        srch.add(rsc, BorderLayout.CENTER);

        p.add(inst);
        p.add(srch);
        SwingUtilities.invokeLater(this::loadMods);
        return p;
    }

    // ── Settings tab ──────────────────────────────────────
    JPanel buildSettingsTab() {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(22, 24, 18, 24));

        Theme.Card card = new Theme.Card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(540, 0));
        card.setBorder(new EmptyBorder(26, 28, 26, 28));

        JLabel header = Theme.hLabel("SETTINGS", Theme.HEADING, Theme.EMERALD_BRIGHT);
        header.setAlignmentX(LEFT_ALIGNMENT);
        card.add(header);
        JLabel sub = Theme.hLabel("Configure paths & launch options", Theme.SMALL, Theme.TEXT_DIM);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        card.add(sub);
        card.add(Box.createVerticalStrut(20));

        addSettingsRow(card, "JAVA PATH",
            System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        addSettingsRow(card, "GAME DIRECTORY", MC_DIR);
        addSettingsRow(card, "EXTRA JVM ARGS",
            "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200");

        card.add(Box.createVerticalStrut(8));
        Theme.PolishButton save = new Theme.PolishButton(
            "SAVE SETTINGS", Theme.EMERALD, new Color(8, 18, 12), true);
        save.setAlignmentX(LEFT_ALIGNMENT);
        save.setMaximumSize(new Dimension(180, 38));
        save.addActionListener(e -> { saveConfig(); status("Settings saved."); });
        card.add(save);

        card.add(Box.createVerticalStrut(28));

        // System info subsection
        JLabel sysHeader = Theme.hLabel("SYSTEM INFO", Theme.HEADING, Theme.DIAMOND);
        sysHeader.setAlignmentX(LEFT_ALIGNMENT);
        card.add(sysHeader);
        card.add(Box.createVerticalStrut(12));

        String[][] sysInfo = {
            {"Operating System", System.getProperty("os.name") + " " + System.getProperty("os.version")},
            {"Architecture",     System.getProperty("os.arch")},
            {"Java Version",     System.getProperty("java.version")},
            {"Java Vendor",      System.getProperty("java.vendor")},
            {"Max RAM (JVM)",    (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB"},
            {"User Home",        System.getProperty("user.home")},
            {"MC Directory",     MC_DIR}
        };
        for (String[] kv : sysInfo) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            row.setAlignmentX(LEFT_ALIGNMENT);
            JLabel k = Theme.hLabel(kv[0], Theme.SMALL, Theme.MUTED);
            JLabel v = Theme.hLabel(kv[1], Theme.SMALL, Theme.TEXT_DIM);
            row.add(k, BorderLayout.WEST);
            row.add(v, BorderLayout.EAST);
            card.add(row);
            card.add(Box.createVerticalStrut(6));
        }

        p.add(card, BorderLayout.WEST);
        return p;
    }

    void addSettingsRow(JPanel parent, String label, String value) {
        JLabel lbl = Theme.sectionLabel("◆  " + label);
        parent.add(lbl);
        parent.add(Box.createVerticalStrut(6));
        Theme.RoundedTextField f = new Theme.RoundedTextField(value, Theme.BG_INPUT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(LEFT_ALIGNMENT);
        parent.add(f);
        parent.add(Box.createVerticalStrut(14));
    }

    // ── Footer ────────────────────────────────────────────
    JPanel buildFooter() {
        JPanel f = new JPanel(new BorderLayout());
        f.setOpaque(false);
        f.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, Theme.BORDER),
            new EmptyBorder(10, 26, 10, 26)));
        JPanel L = opaque(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel pulse = Theme.hLabel("●", Theme.BODY_B, Theme.EMERALD);
        statusBar = Theme.hLabel("Ready", Theme.SMALL, Theme.TEXT_DIM);
        L.add(pulse);
        L.add(statusBar);
        JLabel cr = Theme.hLabel("CraftLaunch v4.0  ◆  Not affiliated with Mojang or Microsoft",
            Theme.TINY, new Color(50, 65, 85));
        f.add(L,  BorderLayout.WEST);
        f.add(cr, BorderLayout.EAST);
        return f;
    }

    // ── Launch button ─────────────────────────────────────


    // ════════════════════════════════════════════════════════
    //  MICROSOFT AUTH  (Device Code Flow)
    // ════════════════════════════════════════════════════════
    void startMicrosoftLogin() {
        String clientId = config.getProperty("azureClientId", CLIENT_ID);

        signInBtn.setEnabled(false);
        accountLabel.setText("Opening browser...");
        accountLabel.setForeground(BLUE);
        status("Opening Microsoft sign-in in your browser...");

        final String CID = clientId.trim();

        new Thread(() -> {
            com.sun.net.httpserver.HttpServer server = null;
            try {
                // ── Step 1: Start a local HTTP server to catch the redirect ──
                server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                int port = server.getAddress().getPort();
                String redirectUri = "http://localhost:" + port + "/";

                final java.util.concurrent.CompletableFuture<String> codeFuture = new java.util.concurrent.CompletableFuture<>();

                server.createContext("/", exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    String code = null, error = null;
                    if (query != null) {
                        for (String part : query.split("&")) {
                            String[] kv = part.split("=", 2);
                            if (kv.length == 2) {
                                String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                                if ("code".equals(kv[0]))  code  = v;
                                if ("error".equals(kv[0])) error = v;
                            }
                        }
                    }
                    String htmlOk = "<!DOCTYPE html><html><head><title>Signed in</title>" +
                        "<style>body{font-family:sans-serif;background:#0f1419;color:#4ade80;display:flex;" +
                        "align-items:center;justify-content:center;height:100vh;margin:0;text-align:center}" +
                        "div{padding:40px;border:2px solid #4ade80;border-radius:16px;background:#1a2332}" +
                        "h1{margin:0 0 16px;font-size:32px}p{color:#94a3b8;margin:0}</style></head>" +
                        "<body><div><h1>✓ Signed in!</h1><p>You can close this tab and return to CraftLaunch.</p></div></body></html>";
                    String htmlErr = "<!DOCTYPE html><html><body style='font-family:sans-serif;background:#0f1419;color:#f87171;text-align:center;padding:40px'>" +
                        "<h1>Sign-in failed</h1><p>Please try again in CraftLaunch.</p></body></html>";
                    byte[] bytes = (code != null ? htmlOk : htmlErr).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    if (code != null)        codeFuture.complete(code);
                    else if (error != null)  codeFuture.completeExceptionally(new RuntimeException("Microsoft returned: " + error));
                    else                     codeFuture.completeExceptionally(new RuntimeException("No code in redirect"));
                });
                server.start();
                log("  Local auth server listening on port " + port);

                // ── Step 2: Open browser to Microsoft sign-in ──
                String authUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize"
                    + "?client_id=" + CID
                    + "&response_type=code"
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode("XboxLive.signin offline_access", StandardCharsets.UTF_8)
                    + "&prompt=select_account";

                log("  Opening browser for sign-in...");
                boolean opened = false;
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(authUrl));
                        opened = true;
                    }
                } catch (Exception ignored) {}
                if (!opened) {
                    String os = System.getProperty("os.name", "").toLowerCase();
                    String[] cmd = os.contains("win") ? new String[]{"rundll32","url.dll,FileProtocolHandler",authUrl}
                                  : os.contains("mac") ? new String[]{"open", authUrl}
                                  : new String[]{"xdg-open", authUrl};
                    try { Runtime.getRuntime().exec(cmd); opened = true; } catch (Exception ignored) {}
                }
                if (!opened) {
                    log("  Could not open browser. Copy this URL into your browser:");
                    log("  " + authUrl);
                }

                SwingUtilities.invokeLater(() -> {
                    accountLabel.setText("Sign in in your browser...");
                    status("Waiting for browser sign-in...");
                });

                // ── Step 3: Wait up to 5 minutes for the redirect ──
                String code;
                try {
                    code = codeFuture.get(5, java.util.concurrent.TimeUnit.MINUTES);
                } catch (java.util.concurrent.TimeoutException te) {
                    throw new RuntimeException("Sign-in timed out (5 minutes). Try again.");
                }
                log("  Got auth code from browser, exchanging for token...");

                // ── Step 4: Exchange code for access token ──
                String tokenBody = "client_id=" + CID
                    + "&grant_type=authorization_code"
                    + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode("XboxLive.signin offline_access", StandardCharsets.UTF_8);
                String tokResp = post(
                    "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
                    tokenBody, "application/x-www-form-urlencoded", null);
                String msToken = jStr(tokResp, "access_token");
                if (msToken == null) {
                    String err = jStr(tokResp, "error_description");
                    if (err == null) err = jStr(tokResp, "error");
                    throw new RuntimeException("Microsoft token error: " + err);
                }
                log("  Microsoft token obtained.");

                // ── Skip ahead to Xbox/XSTS/Minecraft ──
                doMinecraftAuthFromMsToken(msToken);

            } catch (Exception ex) {
                log("  Auth error: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    accountLabel.setText("Login failed");
                    accountLabel.setForeground(RED);
                    signInBtn.setEnabled(true);
                    status("Login failed: " + ex.getMessage());
                });
            } finally {
                if (server != null) server.stop(0);
            }
        }, "ms-auth").start();
    }

    /** Continues the Microsoft → Xbox → XSTS → Minecraft chain after we have an MS token. */
    void doMinecraftAuthFromMsToken(String msToken) throws Exception {
        // Step 3: Xbox Live
        String xblBody = "{\"Properties\":{\"AuthMethod\":\"RPS\","
            + "\"SiteName\":\"user.auth.xboxlive.com\","
            + "\"RpsTicket\":\"d=" + msToken + "\"},"
            + "\"RelyingParty\":\"http://auth.xboxlive.com\","
            + "\"TokenType\":\"JWT\"}";
        String xblResp  = postJson("https://user.auth.xboxlive.com/user/authenticate", xblBody);
        String xblToken = jStr(xblResp, "Token");
        String uhs      = extractUhs(xblResp);
        if (xblToken == null) throw new RuntimeException("Xbox Live auth failed.");
        log("  Xbox Live authenticated.");

        // Step 4: XSTS
        String xstsBody = "{\"Properties\":{\"SandboxId\":\"RETAIL\","
            + "\"UserTokens\":[\"" + xblToken + "\"]},"
            + "\"RelyingParty\":\"rp://api.minecraftservices.com/\","
            + "\"TokenType\":\"JWT\"}";
        String xstsResp = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody);
        if (xstsResp.contains("2148916233"))
            throw new RuntimeException("No Xbox account on this Microsoft account. Create one at xbox.com first.");
        if (xstsResp.contains("2148916238"))
            throw new RuntimeException("Child account needs parental approval.");
        String xstsToken = jStr(xstsResp, "Token");
        if (xstsToken == null) throw new RuntimeException("XSTS auth failed.");
        log("  XSTS token obtained.");

        // Step 5: Minecraft auth
        String mcAuthResp = postJson(
            "https://api.minecraftservices.com/authentication/login_with_xbox",
            "{\"identityToken\":\"XBL3.0 x=" + uhs + ";" + xstsToken + "\"}");
        String accessTok = jStr(mcAuthResp, "access_token");
        if (accessTok == null) {
            log("  Minecraft API response: " + (mcAuthResp.length() > 400 ? mcAuthResp.substring(0,400) + "..." : mcAuthResp));
            String mcErr = jStr(mcAuthResp, "errorMessage");
            if (mcErr == null) mcErr = jStr(mcAuthResp, "error");
            if (mcErr == null) mcErr = "see log above";
            throw new RuntimeException("Minecraft auth failed: " + mcErr);
        }
        log("  Minecraft authenticated.");

        // Step 6: Profile
        String profile = get("https://api.minecraftservices.com/minecraft/profile", accessTok);
        if (!profile.contains("\"name\"") || profile.contains("NOT_FOUND"))
            throw new RuntimeException("This account does not own Minecraft Java Edition.");
        String name = jStr(profile, "name");
        String uuid = jStr(profile, "id");

        mcToken = accessTok; playerName = name; playerUUID = uuid;
        log("  Logged in as: " + name);

        SwingUtilities.invokeLater(() -> {
            accountLabel.setText("Signed in: " + name);
            accountLabel.setForeground(GREEN);
            signInBtn.setText("Sign Out");
            signInBtn.setEnabled(true);
            status("Signed in as " + name);
            usernameField.setText(name);
            usernameField.setEnabled(false);
        });
    }

    void startMicrosoftLogin_OLD_DEVICE_FLOW_DELETE_ME() {
        String clientId = config.getProperty("azureClientId", CLIENT_ID);
        signInBtn.setEnabled(false);
        accountLabel.setText("Connecting...");
        accountLabel.setForeground(BLUE);
        status("Starting Microsoft login...");
        final String CID = clientId.trim();
        new Thread(() -> {
            try {
                String dcResp = post(
                    "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode",
                    "client_id="+CID+"&scope=XboxLive.signin%20offline_access",
                    "application/x-www-form-urlencoded", null);

                String deviceCode = jStr(dcResp,"device_code");
                String userCode   = jStr(dcResp,"user_code");
                String verUrl     = jStr(dcResp,"verification_uri");
                int    interval   = jInt(dcResp,"interval",5);

                if (userCode == null || verUrl == null) {
                    log("  ERROR: Microsoft did not return a code.");
                    log("  Response: " + (dcResp.length() > 300 ? dcResp.substring(0,300)+"..." : dcResp));
                    String err = jStr(dcResp,"error_description");
                    if (err == null) err = jStr(dcResp,"error");
                    if (err == null) err = "Unknown error from Microsoft.";
                    throw new RuntimeException("Microsoft auth failed: " + err);
                }

                String verUrlComplete = jStr(dcResp,"verification_uri_complete");
                final String fullUrl = verUrlComplete != null ? verUrlComplete : verUrl;
                SwingUtilities.invokeLater(() -> showAuthDialog(userCode, verUrl, fullUrl));
                log("  Open: "+verUrl+"  Code: "+userCode);

                // Step 2: poll for MS token
                String pollBody = "client_id="+CID
                    +"&scope=XboxLive.signin%20offline_access"
                    +"&grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code"
                    +"&device_code="+URLEncoder.encode(deviceCode, StandardCharsets.UTF_8);

                String msToken = null;
                for (int i=0; i<120 && msToken==null; i++) {
                    Thread.sleep(interval*1000L);
                    String r = post("https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
                        pollBody,"application/x-www-form-urlencoded",null);
                    if (r.contains("access_token"))     msToken=jStr(r,"access_token");
                    else if (r.contains("slow_down"))   interval=Math.min(interval+5,30);
                    else if (r.contains("expired")||r.contains("access_denied"))
                        throw new RuntimeException("Login expired or denied.");
                }
                if (msToken==null) throw new RuntimeException("Login timed out.");
                log("  Microsoft token obtained.");

                // Step 3: Xbox Live
                String xblBody = "{\"Properties\":{\"AuthMethod\":\"RPS\","
                    +"\"SiteName\":\"user.auth.xboxlive.com\","
                    +"\"RpsTicket\":\"d="+msToken+"\"},"
                    +"\"RelyingParty\":\"http://auth.xboxlive.com\","
                    +"\"TokenType\":\"JWT\"}";
                String xblResp  = postJson("https://user.auth.xboxlive.com/user/authenticate",xblBody);
                String xblToken = jStr(xblResp,"Token");
                String uhs      = extractUhs(xblResp);
                log("  Xbox Live authenticated.");

                // Step 4: XSTS
                String xstsBody = "{\"Properties\":{\"SandboxId\":\"RETAIL\","
                    +"\"UserTokens\":[\""+xblToken+"\"]},"
                    +"\"RelyingParty\":\"rp://api.minecraftservices.com/\","
                    +"\"TokenType\":\"JWT\"}";
                String xstsResp = postJson("https://xsts.auth.xboxlive.com/xsts/authorize",xstsBody);
                if (xstsResp.contains("2148916233"))
                    throw new RuntimeException("No Xbox account. Create one at xbox.com.");
                if (xstsResp.contains("2148916238"))
                    throw new RuntimeException("Child account needs parental approval.");
                String xstsToken = jStr(xstsResp,"Token");
                log("  XSTS token obtained.");

                // Step 5: Minecraft auth
                String mcAuthResp = postJson(
                    "https://api.minecraftservices.com/authentication/login_with_xbox",
                    "{\"identityToken\":\"XBL3.0 x="+uhs+";"+xstsToken+"\"}");
                String accessTok = jStr(mcAuthResp,"access_token");
                log("  Minecraft authenticated.");

                // Step 6: Profile
                String profile = get("https://api.minecraftservices.com/minecraft/profile",accessTok);
                if (!profile.contains("\"name\"")||profile.contains("NOT_FOUND"))
                    throw new RuntimeException("Account does not own Minecraft Java Edition.");
                String name = jStr(profile,"name");
                String uuid = jStr(profile,"id");

                mcToken=accessTok; playerName=name; playerUUID=uuid;
                log("  Logged in as: "+name);

                SwingUtilities.invokeLater(() -> {
                    accountLabel.setText("Signed in: "+name);
                    accountLabel.setForeground(GREEN);
                    signInBtn.setText("Sign Out");
                    signInBtn.setEnabled(true);
                    status("Signed in as "+name);
                    for (Window w : Window.getWindows())
                        if (w instanceof JDialog d && d.getTitle().contains("Microsoft")) d.dispose();
                });

            } catch (Exception ex) {
                log("  Auth error: "+ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    accountLabel.setText("Login failed");
                    accountLabel.setForeground(RED);
                    signInBtn.setEnabled(true);
                    status("Login failed: "+ex.getMessage());
                });
            }
        },"ms-auth").start();
    }

    void showAuthDialog(String code, String url, String urlWithCode) {
        JDialog dlg = new JDialog(this, "Sign in with Microsoft", false);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setSize(520, 460);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(CARD);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(CARD);
        inner.setBorder(new EmptyBorder(24, 30, 24, 30));

        // ── Header ──
        JLabel title = lbl("Sign in to Microsoft", new Font("Monospaced", Font.BOLD, 18), GREEN);
        title.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(title);
        inner.add(Box.createVerticalStrut(14));

        // ── Easy path: one big button ──
        JLabel easyLabel = lbl("EASIEST  Click this button to open Microsoft sign-in:",
            new Font("Monospaced", Font.BOLD, 11), MUTED);
        easyLabel.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(easyLabel);
        inner.add(Box.createVerticalStrut(8));

        JButton bigSignIn = new JButton(">>  OPEN MICROSOFT SIGN-IN  <<");
        bigSignIn.setBackground(new Color(30, 100, 50));
        bigSignIn.setForeground(Color.WHITE);
        bigSignIn.setFont(new Font("Monospaced", Font.BOLD, 14));
        bigSignIn.setBorder(new EmptyBorder(14, 20, 14, 20));
        bigSignIn.setFocusPainted(false);
        bigSignIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bigSignIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        bigSignIn.setAlignmentX(LEFT_ALIGNMENT);
        bigSignIn.addActionListener(e -> openInBrowser(urlWithCode, dlg));
        inner.add(bigSignIn);
        inner.add(Box.createVerticalStrut(6));

        JLabel hint = lbl("(Opens browser with code already entered. Just sign in!)",
            new Font("Monospaced", Font.ITALIC, 10), new Color(120, 180, 130));
        hint.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(hint);
        inner.add(Box.createVerticalStrut(20));

        // ── Manual path ──
        JLabel manLabel = lbl("OR MANUALLY  Use these if the button above didn't work:",
            new Font("Monospaced", Font.BOLD, 11), MUTED);
        manLabel.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(manLabel);
        inner.add(Box.createVerticalStrut(8));

        // URL field
        JTextField urlField = new JTextField(url);
        urlField.setEditable(false);
        urlField.setBackground(new Color(10, 14, 22));
        urlField.setForeground(BLUE);
        urlField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        urlField.setCaretColor(BLUE);
        urlField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 70, 120)),
            new EmptyBorder(6, 10, 6, 10)));
        urlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        urlField.setAlignmentX(LEFT_ALIGNMENT);
        urlField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { urlField.selectAll(); }
        });
        urlField.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { urlField.requestFocus(); urlField.selectAll(); }
        });
        inner.add(urlField);
        inner.add(Box.createVerticalStrut(8));

        // Code field
        JTextField codeField = new JTextField(code);
        codeField.setEditable(false);
        codeField.setBackground(new Color(6, 18, 10));
        codeField.setForeground(GREEN);
        codeField.setFont(new Font("Monospaced", Font.BOLD, 24));
        codeField.setHorizontalAlignment(JTextField.CENTER);
        codeField.setCaretColor(GREEN);
        codeField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 120, 70), 2),
            new EmptyBorder(8, 16, 8, 16)));
        codeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        codeField.setAlignmentX(LEFT_ALIGNMENT);
        codeField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { codeField.selectAll(); }
        });
        codeField.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { codeField.requestFocus(); codeField.selectAll(); }
        });
        inner.add(codeField);
        inner.add(Box.createVerticalStrut(6));

        JLabel codeHint = lbl("Click the green box, press Ctrl+A to select all, then Ctrl+C to copy.",
            new Font("Monospaced", Font.ITALIC, 9), MUTED);
        codeHint.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(codeHint);
        inner.add(Box.createVerticalStrut(14));

        // Status
        JLabel waiting = lbl("Waiting for sign-in... (this dialog closes automatically)",
            new Font("Monospaced", Font.ITALIC, 10), new Color(150, 200, 150));
        waiting.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(waiting);

        dlg.add(inner);
        dlg.setVisible(true);

        // Auto-open browser after 500ms (best-effort)
        new javax.swing.Timer(500, e -> {
            ((javax.swing.Timer)e.getSource()).stop();
            openInBrowser(urlWithCode, dlg);
        }) {{ setRepeats(false); }}.start();
    }

    String showClientIdDialog(String saved) {
        JDialog dlg = new JDialog(this, "Microsoft Sign-In Setup", true);
        dlg.setSize(560, 480);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(Theme.BG_PANEL);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(Theme.BG_PANEL);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = Theme.hLabel("MICROSOFT SIGN-IN", Theme.HEADING, Theme.EMERALD_BRIGHT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(title);
        inner.add(Box.createVerticalStrut(6));

        JLabel sub = Theme.hLabel("To sign in, you need a free Microsoft Azure Client ID.",
            Theme.SMALL, Theme.TEXT_DIM);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(sub);
        inner.add(Box.createVerticalStrut(20));

        JLabel howTo = Theme.hLabel("HOW TO GET ONE (5 minutes, free):",
            Theme.LABEL, Theme.GOLD);
        howTo.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(howTo);
        inner.add(Box.createVerticalStrut(8));

        JTextArea steps = new JTextArea(
            "1. Sign in at https://portal.azure.com (free Microsoft account)\n" +
            "2. Search for 'App registrations'  →  click 'New registration'\n" +
            "3. Name: anything (e.g. 'My Launcher')\n" +
            "4. Supported account types:\n" +
            "     'Personal Microsoft accounts only'\n" +
            "5. Redirect URI: leave blank\n" +
            "6. Click 'Register'\n" +
            "7. Go to 'Authentication'  →  'Allow public client flows: YES'\n" +
            "8. Save → copy the 'Application (client) ID'\n" +
            "9. Paste it below ↓"
        );
        steps.setEditable(false);
        steps.setBackground(Theme.BG_DEEP);
        steps.setForeground(Theme.TEXT_DIM);
        steps.setFont(Theme.SMALL);
        steps.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            new EmptyBorder(10, 12, 10, 12)));
        steps.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        steps.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(steps);
        inner.add(Box.createVerticalStrut(16));

        JLabel pasteLbl = Theme.hLabel("◆  PASTE YOUR CLIENT ID HERE:",
            Theme.LABEL, Theme.MUTED);
        pasteLbl.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(pasteLbl);
        inner.add(Box.createVerticalStrut(6));

        Theme.RoundedTextField idField = new Theme.RoundedTextField(saved, Theme.BG_INPUT);
        idField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        idField.setAlignmentX(LEFT_ALIGNMENT);
        idField.setForeground(Theme.EMERALD_BRIGHT);
        inner.add(idField);
        inner.add(Box.createVerticalStrut(18));

        // Buttons
        final String[] result = {null};
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(LEFT_ALIGNMENT);

        Theme.PolishButton openPortal = new Theme.PolishButton(
            "Open Azure Portal", Theme.BG_INPUT, Theme.DIAMOND, false);
        openPortal.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new URI("https://portal.azure.com")); }
            catch (Exception ex) { /* ignore */ }
        });

        Theme.PolishButton continueBtn = new Theme.PolishButton(
            "Continue", Theme.EMERALD, new Color(8, 18, 12), true);
        continueBtn.setPreferredSize(new Dimension(120, 38));
        continueBtn.addActionListener(e -> {
            String txt = idField.getText().trim();
            if (txt.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please paste a client ID.",
                    "Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            result[0] = txt;
            dlg.dispose();
        });

        Theme.PolishButton cancelBtn = new Theme.PolishButton(
            "Cancel", Theme.BG_INPUT, Theme.MUTED, false);
        cancelBtn.addActionListener(e -> dlg.dispose());

        btnRow.add(continueBtn);
        btnRow.add(openPortal);
        btnRow.add(cancelBtn);
        inner.add(btnRow);

        dlg.add(inner);
        dlg.setVisible(true);
        return result[0];
    }

    void openInBrowser(String url, JDialog dlg) {
        // Try multiple methods for opening a browser
        boolean ok = false;

        // Method 1: Java Desktop API
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                ok = true;
            }
        } catch (Exception ignored) {}

        // Method 2: OS-specific commands
        if (!ok) {
            String os = System.getProperty("os.name", "").toLowerCase();
            String[] cmd = null;
            if (os.contains("win"))      cmd = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
            else if (os.contains("mac")) cmd = new String[]{"open", url};
            else                          cmd = new String[]{"xdg-open", url};
            try {
                Runtime.getRuntime().exec(cmd);
                ok = true;
            } catch (Exception ignored) {}
        }

        // Method 3: cmd /c start (Windows fallback)
        if (!ok && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", url});
                ok = true;
            } catch (Exception ignored) {}
        }

        if (!ok) {
            JOptionPane.showMessageDialog(dlg,
                "Could not open browser automatically.\n\n" +
                "Please manually:\n" +
                "1) Copy the URL from the box below\n" +
                "2) Open it in any browser\n" +
                "3) Enter the green code shown",
                "Open Browser Manually", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    void forceCopy(String text, JTextField field, JButton btn, JLabel status, JDialog dlg) {
        try {
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(text);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            field.selectAll();
            if (status != null) status.setText("Copied to clipboard!");
        } catch (Exception ex) {
            field.requestFocusInWindow();
            field.selectAll();
            if (status != null) status.setText("Press Ctrl+C to copy.");
        }
    }

    void signOut() {
        mcToken=null; playerName=null; playerUUID=null;
        accountLabel.setText("PLAYER NAME"); accountLabel.setForeground(MUTED);
        signInBtn.setText("Sign in with Microsoft");
        if (usernameField != null) usernameField.setEnabled(true);
        log("  Signed out."); status("Signed out.");
    }

    void setupOfflinePlayer(String rawName) {
        String name = rawName == null ? "" : rawName.trim().replaceAll("[^a-zA-Z0-9_]", "_");
        if (name.length() > 16) name = name.substring(0, 16);
        if (name.length() < 3)  return;

        playerName = name;
        playerUUID = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes()).toString().replace("-","");
        mcToken = "0";
        log("  Offline mode: " + name + "  UUID: " + playerUUID);
        accountLabel.setText("Offline: " + name);
        accountLabel.setForeground(YELLOW);
        status("Playing offline as " + name);
    }

    // ════════════════════════════════════════════════════════
    //  VERSION LIST
    // ════════════════════════════════════════════════════════
    void loadVersions() {
        new Thread(() -> {
            try {
                log("  Fetching version list from Mojang...");
                String manifest = get(MANIFEST, null);
                String latestId = jStr(manifest,"release");

                List<String> items = new ArrayList<>();
                int p = manifest.indexOf("\"versions\"");
                if (p<0) { log("  Could not parse version list."); return; }
                p = manifest.indexOf('[',p);
                int added=0;
                while (added<40 && p<manifest.length()) {
                    int s = manifest.indexOf('{',p); if(s<0) break;
                    int e = manifest.indexOf('}',s); if(e<0) break;
                    String obj  = manifest.substring(s,e+1);
                    String id   = jStr(obj,"id");
                    String type = jStr(obj,"type");
                    String url  = jStr(obj,"url");
                    if (id!=null && url!=null && ("release".equals(type)||"snapshot".equals(type))) {
                        String tag = "release".equals(type)?" (release)":" [snapshot]";
                        if (id.equals(latestId)) tag=" (Latest)";
                        versionMap.put(id, url);
                        items.add(id+tag);
                        added++;
                    }
                    p=e+1;
                }
                SwingUtilities.invokeLater(() -> {
                    for (String it : items) versionBox.addItem(it);
                    log("  Loaded "+items.size()+" versions.");
                    String savedVer = config.getProperty("version");
                    if (savedVer != null) {
                        for (int i = 0; i < versionBox.getItemCount(); i++) {
                            if (versionBox.getItemAt(i).equals(savedVer)) {
                                versionBox.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                });
            } catch (Exception ex) {
                log("  Could not load versions: "+ex.getMessage());
            }
        },"version-loader").start();
    }

    // ════════════════════════════════════════════════════════
    //  GAME LAUNCH
    // ════════════════════════════════════════════════════════
    void launchGame(String versionId, JButton launchB, JButton killB) {
        launching=true;
        log(""); log("--------------------------------------");
        log("Preparing Minecraft "+versionId+"..."); log("");

        new Thread(() -> {
            try {
                setProgress(0,"Loading version data...");
                String versionUrl = versionMap.get(versionId);
                if (versionUrl==null) throw new RuntimeException("Version URL missing.");

                String vJson = get(versionUrl, null);
                @SuppressWarnings("unchecked")
                Map<String,Object> vdata = (Map<String,Object>) Json.parse(vJson);
                String mainClass = (String) vdata.get("mainClass");
                setProgress(5,"Version data loaded.");

                // Directories
                Path verDir  = Path.of(MC_DIR,"versions",versionId);
                Path natDir  = verDir.resolve("natives");
                Path libDir  = Path.of(MC_DIR,"libraries");
                Path astDir  = Path.of(MC_DIR,"assets");
                Path clientJ = verDir.resolve(versionId+".jar");
                mkdirs(verDir,natDir,libDir,astDir.resolve("indexes"),astDir.resolve("objects"));

                // Client JAR
                @SuppressWarnings("unchecked")
                Map<String,Object> dls = (Map<String,Object>) vdata.get("downloads");
                if (dls!=null) {
                    @SuppressWarnings("unchecked")
                    Map<String,Object> cdl = (Map<String,Object>) dls.get("client");
                    if (cdl!=null && !Files.exists(clientJ)) {
                        setProgress(8,"Downloading client JAR...");
                        log("  Downloading client JAR...");
                        dlFile((String)cdl.get("url"), clientJ);
                        log("  Client JAR ready.");
                    }
                }

                // Libraries
                @SuppressWarnings("unchecked")
                List<Object> libs = (List<Object>) vdata.get("libraries");
                List<Path> classpath = new ArrayList<>();
                classpath.add(clientJ.toAbsolutePath());

                String osName = osName();
                int total = libs==null?0:libs.size(), done=0;
                setProgress(12,"Downloading libraries...");

                if (libs!=null) for (Object lo : libs) {
                    @SuppressWarnings("unchecked") Map<String,Object> lib = (Map<String,Object>) lo;
                    done++;
                    if (!rulePass(lib,osName)) continue;

                    @SuppressWarnings("unchecked") Map<String,Object> ldl = (Map<String,Object>) lib.get("downloads");
                    if (ldl==null) continue;

                    @SuppressWarnings("unchecked") Map<String,Object> art = (Map<String,Object>) ldl.get("artifact");
                    @SuppressWarnings("unchecked") Map<String,Object> nts = (Map<String,Object>) lib.get("natives");

                    if (art!=null) {
                        String ap=(String)art.get("path"), au=(String)art.get("url");
                        if (ap!=null && au!=null) {
                            Path dest = libDir.resolve(ap.replace("/",File.separator));
                            if (!Files.exists(dest)) { dest.getParent().toFile().mkdirs(); dlFile(au,dest); }
                            if (nts==null) classpath.add(dest.toAbsolutePath());
                        }
                    }
                    if (nts!=null) {
                        String nk = (String)nts.get(osName);
                        if (nk==null) nk=(String)nts.get("osx".equals(osName)?"macos":osName);
                        if (nk!=null) {
                            @SuppressWarnings("unchecked") Map<String,Object> cls = (Map<String,Object>) ldl.get("classifiers");
                            if (cls!=null) {
                                @SuppressWarnings("unchecked") Map<String,Object> ndl2 = (Map<String,Object>) cls.get(nk);
                                if (ndl2!=null) {
                                    String nu=(String)ndl2.get("url"), np=(String)ndl2.get("path");
                                    if (nu!=null && np!=null) {
                                        Path nd = libDir.resolve(np.replace("/",File.separator));
                                        if (!Files.exists(nd)) { nd.getParent().toFile().mkdirs(); dlFile(nu,nd); }
                                        extractZip(nd,natDir);
                                    }
                                }
                            }
                        }
                    }
                    setProgress(12+(int)(done/(float)total*38),"Libraries "+done+"/"+total);
                }
                log("  Libraries ready ("+classpath.size()+" entries).");

                // Assets
                @SuppressWarnings("unchecked") Map<String,Object> aiMap = (Map<String,Object>) vdata.get("assetIndex");
                String aiId  = aiMap!=null?(String)aiMap.get("id"):versionId;
                String aiUrl = aiMap!=null?(String)aiMap.get("url"):null;
                Path aiFile  = astDir.resolve("indexes").resolve(aiId+".json");
                setProgress(52,"Downloading asset index...");
                if (aiUrl!=null && !Files.exists(aiFile)) dlFile(aiUrl,aiFile);
                if (Files.exists(aiFile)) { setProgress(54,"Downloading assets..."); dlAssets(aiFile,astDir); }

                // === FABRIC LOADER ===
                List<String> fabricExtraJvm = new ArrayList<>();
                if (isFabricSelected()) {
                    setProgress(88, "Setting up Fabric mod loader...");
                    try {
                        log("  Setting up Fabric for " + versionId + "...");
                        // List loader versions
                        String loaderListJson = get(
                            "https://meta.fabricmc.net/v2/versions/loader/" + versionId, null);
                        @SuppressWarnings("unchecked")
                        List<Object> loaders = (List<Object>) Json.parse(loaderListJson);
                        if (loaders == null || loaders.isEmpty())
                            throw new RuntimeException("No Fabric loader for Minecraft "+versionId);

                        @SuppressWarnings("unchecked")
                        Map<String,Object> firstLoader = (Map<String,Object>) loaders.get(0);
                        @SuppressWarnings("unchecked")
                        Map<String,Object> loaderInfo  = (Map<String,Object>) firstLoader.get("loader");
                        String loaderVer = (String) loaderInfo.get("version");
                        log("  Fabric loader version: " + loaderVer);

                        // Get profile JSON
                        String fProfile = get(
                            "https://meta.fabricmc.net/v2/versions/loader/"
                            + versionId + "/" + loaderVer + "/profile/json", null);
                        @SuppressWarnings("unchecked")
                        Map<String,Object> fData = (Map<String,Object>) Json.parse(fProfile);

                        // Override mainClass
                        String fMain = (String) fData.get("mainClass");
                        if (fMain != null) {
                            mainClass = fMain;
                            log("  Main class: " + fMain);
                        }

                        // Download Fabric libraries -> classpath
                        @SuppressWarnings("unchecked")
                        List<Object> fLibs = (List<Object>) fData.get("libraries");
                        if (fLibs != null) {
                            int fdone = 0;
                            for (Object lo : fLibs) {
                                @SuppressWarnings("unchecked")
                                Map<String,Object> lib = (Map<String,Object>) lo;
                                String mvName = (String) lib.get("name");
                                String urlBase = (String) lib.get("url");
                                if (mvName == null) continue;

                                String relPath = mavenNameToPath(mvName);
                                if (relPath == null) continue;
                                Path dest = libDir.resolve(relPath.replace("/", File.separator));

                                if (!Files.exists(dest)) {
                                    dest.getParent().toFile().mkdirs();

                                    // Try multiple URL sources in order
                                    List<String> urls = new ArrayList<>();
                                    @SuppressWarnings("unchecked")
                                    Map<String,Object> dlInfo = (Map<String,Object>) lib.get("downloads");
                                    if (dlInfo != null) {
                                        @SuppressWarnings("unchecked")
                                        Map<String,Object> art = (Map<String,Object>) dlInfo.get("artifact");
                                        if (art != null) {
                                            String u = (String) art.get("url");
                                            if (u != null) urls.add(u);
                                        }
                                    }
                                    if (urlBase != null) {
                                        String b = urlBase.endsWith("/") ? urlBase : urlBase + "/";
                                        urls.add(b + relPath);
                                    }
                                    // Fallback maven repos
                                    urls.add("https://maven.fabricmc.net/" + relPath);
                                    urls.add("https://libraries.minecraft.net/" + relPath);
                                    urls.add("https://repo1.maven.org/maven2/" + relPath);

                                    boolean ok = false;
                                    for (String u : urls) {
                                        try { dlFile(u, dest); ok = true; break; }
                                        catch (Exception ignored) {}
                                    }
                                    if (!ok) {
                                        log("  Skipped " + mvName + " (could not download)");
                                        continue;
                                    }
                                }
                                classpath.add(dest.toAbsolutePath());
                                fdone++;
                            }
                            log("  Fabric libraries downloaded: " + fdone);
                        }

                        // Collect Fabric JVM args
                        @SuppressWarnings("unchecked")
                        Map<String,Object> fArgs = (Map<String,Object>) fData.get("arguments");
                        if (fArgs != null) {
                            @SuppressWarnings("unchecked")
                            List<Object> fJvm = (List<Object>) fArgs.get("jvm");
                            if (fJvm != null) for (Object a : fJvm) {
                                if (a instanceof String s) {
                                    if (s.contains("-cp")||s.contains("${classpath}")) continue;
                                    fabricExtraJvm.add(s);
                                }
                            }
                        }

                        // Make sure mods folder exists
                        File modsDir = new File(MC_DIR, "mods");
                        modsDir.mkdirs();

                        // Auto-install Fabric API (required by 99% of Fabric mods)
                        setProgress(92, "Installing Fabric API...");
                        try {
                            installFabricApi(versionId);
                        } catch (Exception faex) {
                            log("  Could not install Fabric API: " + faex.getMessage());
                            log("  Mods that require Fabric API will fail to load.");
                        }

                        // List the mods that will be loaded
                        File[] modsToLoad = modsDir.listFiles((d, n) -> n.endsWith(".jar"));
                        if (modsToLoad != null && modsToLoad.length > 0) {
                            log("  Mods that will be loaded (" + modsToLoad.length + "):");
                            for (File f : modsToLoad) {
                                log("    - " + f.getName() + " (" + (f.length()/1024) + " KB)");
                            }
                        } else {
                            log("  No mods in folder. Vanilla Fabric will launch (you can add mods later).");
                        }
                        log("  Fabric is ready!");
                    } catch (Exception fex) {
                        log("  Fabric setup failed: " + fex.getMessage());
                        log("  Continuing in vanilla mode (mods will NOT load).");
                    }
                }

                // Build command
                setProgress(95,"Building launch command...");
                String cp   = buildClasspath(classpath);
                String java = javaExec();
                List<String> cmd = new ArrayList<>();
                cmd.add(java);
                cmd.add("-Xmx"+ramSlider.getValue()+"G");
                cmd.add("-Xms512M");
                cmd.add("-Djava.library.path="+natDir.toAbsolutePath());
                cmd.add("-Dminecraft.launcher.brand=CraftLaunch");
                cmd.add("-Dminecraft.launcher.version=3.0");

                // JVM args from version JSON
                @SuppressWarnings("unchecked") Map<String,Object> args = (Map<String,Object>) vdata.get("arguments");
                if (args!=null) {
                    @SuppressWarnings("unchecked") List<Object> jvmArgs = (List<Object>) args.get("jvm");
                    if (jvmArgs!=null) for (Object a : jvmArgs) {
                        if (!(a instanceof String s)) continue;
                        if (s.contains("-cp")||s.contains("${classpath}")) continue;
                        cmd.add(resolve(s,versionId,aiId,natDir,astDir,cp));
                    }
                }
                for (String fa : fabricExtraJvm) cmd.add(fa);
                cmd.add("-cp"); cmd.add(cp);
                cmd.add(mainClass);

                // Game args
                String legacyArgs = (String) vdata.get("minecraftArguments");
                if (args!=null) {
                    @SuppressWarnings("unchecked") List<Object> gArgs = (List<Object>) args.get("game");
                    if (gArgs!=null) for (Object a : gArgs)
                        if (a instanceof String s) cmd.add(resolve(s,versionId,aiId,natDir,astDir,cp));
                } else if (legacyArgs!=null) {
                    for (String s : legacyArgs.split(" "))
                        cmd.add(resolve(s,versionId,aiId,natDir,astDir,cp));
                }

                log("  Command built. Starting Minecraft...");
                setProgress(100,"Launching!");

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(new File(MC_DIR));
                pb.redirectErrorStream(true);
                gameProcess = pb.start();
                log("  Minecraft "+versionId+" is running!");
                log("--------------------------------------");

                try (BufferedReader br = new BufferedReader(new InputStreamReader(gameProcess.getInputStream()))) {
                    String line;
                    while ((line=br.readLine())!=null) {
                        final String fl=line;
                        SwingUtilities.invokeLater(()->log("[MC] "+fl));
                    }
                }
                int exit = gameProcess.waitFor();
                if (exit == 0) {
                    log("  Game closed normally.");
                } else {
                    log("");
                    log("  ============================================");
                    log("  Game crashed (exit code " + exit + ")");
                    log("  ============================================");

                    // Look for the latest crash report
                    File crashDir = new File(MC_DIR, "crash-reports");
                    if (crashDir.exists()) {
                        File[] crashes = crashDir.listFiles((d, n) -> n.endsWith(".txt"));
                        if (crashes != null && crashes.length > 0) {
                            File latest = crashes[0];
                            for (File f : crashes) if (f.lastModified() > latest.lastModified()) latest = f;
                            log("  Crash report: " + latest.getName());
                            log("  Click 'Crash Reports' button on the left to view it.");
                        }
                    }

                    log("");
                    log("  COMMON FIXES:");
                    log("    1. Click 'Clear All Mods' in Mods tab, then try vanilla");
                    log("    2. Lower the RAM slider (8GB+ needs 64-bit Java)");
                    log("    3. Try a different MC version");
                    log("    4. Update your graphics drivers");
                }

            } catch (Exception ex) {
                log("  Launch error: "+ex.getMessage());
                ex.printStackTrace();
            } finally {
                launching=false; gameProcess=null;
                SwingUtilities.invokeLater(()->{
                    launchB.setEnabled(true); killB.setVisible(false);
                    setProgress(0,"Ready"); status("Ready.");
                });
            }
        },"game-launch").start();
    }

    // -- Launch helpers --
    String resolve(String s, String ver, String assetIdx, Path natDir, Path astDir, String cp) {
        return s
            .replace("${auth_player_name}",  playerName!=null?playerName:"Player")
            .replace("${version_name}",       ver)
            .replace("${game_directory}",     MC_DIR)
            .replace("${assets_root}",        astDir.toAbsolutePath().toString())
            .replace("${assets_index_name}",  assetIdx)
            .replace("${auth_uuid}",          playerUUID!=null?playerUUID:"00000000000000000000000000000000")
            .replace("${auth_access_token}",  mcToken!=null?mcToken:"null")
            .replace("${user_type}",          "microsoft")
            .replace("${version_type}",       "release")
            .replace("${natives_directory}",  natDir.toAbsolutePath().toString())
            .replace("${classpath}",          cp)
            .replace("${launcher_name}",      "CraftLaunch")
            .replace("${launcher_version}",   "3.0")
            .replace("${clientid}",           CLIENT_ID)
            .replace("${auth_xuid}",          "")
            .replace("${resolution_width}",   "1280")
            .replace("${resolution_height}",  "720");
    }

    boolean rulePass(Map<String,Object> lib, String osName) {
        @SuppressWarnings("unchecked") List<Object> rules = (List<Object>) lib.get("rules");
        if (rules==null) return true;
        boolean allow=false;
        for (Object ro : rules) {
            @SuppressWarnings("unchecked") Map<String,Object> rule = (Map<String,Object>) ro;
            String action = (String) rule.get("action");
            @SuppressWarnings("unchecked") Map<String,Object> os = (Map<String,Object>) rule.get("os");
            if (os==null) allow="allow".equals(action);
            else { String n=(String)os.get("name"); if(osName.equals(n)) allow="allow".equals(action); }
        }
        return allow;
    }

    void extractZip(Path zip, Path dest) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e;
            while ((e=zis.getNextEntry())!=null) {
                if (e.isDirectory()||e.getName().startsWith("META-INF")) continue;
                Path out = dest.resolve(e.getName());
                Files.createDirectories(out.getParent());
                Files.copy(zis,out,StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) { log("  Native extract warning: "+ex.getMessage()); }
    }

    void dlAssets(Path indexFile, Path astDir) throws Exception {
        String json = Files.readString(indexFile);
        @SuppressWarnings("unchecked") Map<String,Object> idx = (Map<String,Object>) Json.parse(json);
        @SuppressWarnings("unchecked") Map<String,Object> objs = (Map<String,Object>) idx.get("objects");
        if (objs==null) return;
        int tot=objs.size(), don=0;
        for (Map.Entry<String,Object> e : objs.entrySet()) {
            @SuppressWarnings("unchecked") Map<String,Object> asset = (Map<String,Object>) e.getValue();
            String hash=(String)asset.get("hash");
            if (hash==null||hash.length()<2) continue;
            String pre=hash.substring(0,2);
            Path dp = astDir.resolve("objects").resolve(pre).resolve(hash);
            if (!Files.exists(dp)) {
                dp.getParent().toFile().mkdirs();
                try { dlFile("https://resources.download.minecraft.net/"+pre+"/"+hash,dp); }
                catch (Exception ignored) {}
            }
            don++;
            if (don%100==0) setProgress(54+(int)(don/(float)tot*36),"Assets "+don+"/"+tot);
        }
        log("  Assets ready ("+don+" files).");
    }

    String buildClasspath(List<Path> paths) {
        StringBuilder sb = new StringBuilder();
        for (Path p : paths) {
            if (sb.length()>0) sb.append(File.pathSeparator);
            sb.append(p.toAbsolutePath());
        }
        return sb.toString();
    }

    String javaExec() {
        String java = System.getProperty("java.home")+File.separator+"bin"+File.separator+"java";
        if (System.getProperty("os.name","").toLowerCase().contains("win")) java+=".exe";
        return new File(java).exists()?java:"java";
    }

    // ════════════════════════════════════════════════════════
    //  MODS
    // ════════════════════════════════════════════════════════
    void loadMods() {
        File dir = new File(MC_DIR,"mods"); dir.mkdirs();
        File[] files = dir.listFiles();
        modsListPanel.removeAll();

        if (files==null||files.length==0) {
            JLabel e = lbl("No mods installed. Search Modrinth on the right.",
                new Font("Monospaced",Font.ITALIC,11), MUTED);
            e.setBorder(new EmptyBorder(14,12,14,12));
            modsListPanel.add(e);
        } else {
            Arrays.stream(files)
                .filter(f->f.getName().endsWith(".jar")||f.getName().endsWith(".jar.disabled"))
                .sorted(Comparator.comparing(File::getName))
                .forEach(f->modsListPanel.add(modRow(f)));
        }
        modsListPanel.revalidate(); modsListPanel.repaint();
    }

    JPanel modRow(File f) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Theme.BG_DEEP);
        row.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Theme.BORDER),
            new EmptyBorder(10, 14, 10, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        boolean on = f.getName().endsWith(".jar");
        String display = f.getName().replace(".jar.disabled", "").replace(".jar", "");
        if (display.length() > 38) display = display.substring(0, 35) + "...";

        JPanel namePanel = opaque(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel tag  = Theme.hLabel(on ? "● ON" : "○ OFF", Theme.LABEL, on ? Theme.EMERALD : Theme.MUTED);
        JLabel name = Theme.hLabel(display, Theme.BODY, on ? Theme.TEXT : Theme.MUTED);
        namePanel.add(tag);
        namePanel.add(name);

        JPanel R = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        R.setBackground(Theme.BG_DEEP);
        Theme.PolishButton tog = new Theme.PolishButton(
            on ? "Disable" : "Enable", Theme.BG_INPUT, on ? Theme.GOLD : Theme.EMERALD, false);
        tog.setFont(Theme.SMALL);
        tog.addActionListener(e -> toggleMod(f));
        Theme.PolishButton del = new Theme.PolishButton(
            "✕", new Color(50, 20, 20), Theme.RUBY, false);
        del.setFont(Theme.BODY_B);
        del.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Delete " + f.getName() + "?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                f.delete();
                loadMods();
            }
        });
        R.add(tog);
        R.add(del);

        row.add(namePanel, BorderLayout.CENTER);
        row.add(R,         BorderLayout.EAST);
        return row;
    }

    void toggleMod(File f) {
        String n = f.getName();
        File to = n.endsWith(".disabled")
            ? new File(f.getParent(), n.replace(".jar.disabled",".jar"))
            : new File(f.getParent(), n+".disabled");
        if (f.renameTo(to)) loadMods();
        else log("  Could not rename "+n);
    }

    void searchModrinth(String query) {
        if (query.isEmpty()) return;
        modsResultPanel.removeAll();
        JLabel wait = lbl("Searching for \""+query+"\"...",
            new Font("Monospaced",Font.ITALIC,11), MUTED);
        wait.setBorder(new EmptyBorder(14,12,14,12));
        modsResultPanel.add(wait);
        modsResultPanel.revalidate();

        String ver = versionBox.getSelectedItem()!=null
            ? ((String)versionBox.getSelectedItem()).split(" ")[0] : "";

        boolean fabric = isFabricSelected();
        new Thread(()-> {
            try {
                StringBuilder fb = new StringBuilder("[[%22project_type:mod%22]");
                if (!ver.isEmpty()) fb.append(",[%22game_versions:").append(ver).append("%22]");
                if (fabric)         fb.append(",[%22categories:fabric%22]");
                fb.append("]");
                String facets = fb.toString();
                String url = "https://api.modrinth.com/v2/search?limit=12&query="
                    +URLEncoder.encode(query,StandardCharsets.UTF_8)+"&facets="+facets;
                String resp = get(url, null);
                @SuppressWarnings("unchecked") Map<String,Object> data = (Map<String,Object>) Json.parse(resp);
                @SuppressWarnings("unchecked") List<Object> hits = (List<Object>) data.get("hits");

                SwingUtilities.invokeLater(()->{
                    modsResultPanel.removeAll();
                    if (hits==null||hits.isEmpty()) {
                        modsResultPanel.add(lbl("No results for \""+query+"\"",
                            new Font("Monospaced",Font.ITALIC,11),MUTED));
                    } else {
                        for (Object h : hits) {
                            @SuppressWarnings("unchecked") Map<String,Object> mod = (Map<String,Object>) h;
                            modsResultPanel.add(modResultRow(mod,ver));
                        }
                    }
                    modsResultPanel.revalidate(); modsResultPanel.repaint();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(()->{
                    modsResultPanel.removeAll();
                    modsResultPanel.add(lbl("Search failed: "+ex.getMessage(),
                        new Font("Monospaced",Font.PLAIN,11),RED));
                    modsResultPanel.revalidate();
                });
            }
        },"modrinth-search").start();
    }

    JPanel modResultRow(Map<String,Object> mod, String mcVer) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(Theme.BG_DEEP);
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Theme.BORDER),
            new EmptyBorder(12, 14, 12, 14)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        String title = (String) mod.getOrDefault("title", "Unknown");
        String desc  = (String) mod.getOrDefault("description", "");
        String slug  = (String) mod.getOrDefault("slug", "");
        Object dlRaw = mod.getOrDefault("downloads", 0);
        if (desc.length() > 70) desc = desc.substring(0, 67) + "...";

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel titleL = Theme.hLabel(title, Theme.BODY_B, Theme.EMERALD_BRIGHT);
        JLabel descL  = Theme.hLabel(desc, Theme.SMALL, Theme.TEXT_DIM);
        JLabel statsL = Theme.hLabel("↓ " + fmtNum(String.valueOf(dlRaw)) + " downloads",
            Theme.TINY, Theme.DIAMOND_DEEP);
        titleL.setAlignmentX(LEFT_ALIGNMENT);
        descL.setAlignmentX(LEFT_ALIGNMENT);
        statsL.setAlignmentX(LEFT_ALIGNMENT);
        info.add(titleL);
        info.add(Box.createVerticalStrut(2));
        info.add(descL);
        info.add(Box.createVerticalStrut(3));
        info.add(statsL);

        Theme.PolishButton instBtn = new Theme.PolishButton(
            "INSTALL", Theme.EMERALD, new Color(8, 18, 12), true);
        instBtn.setFont(Theme.SMALL);
        instBtn.setPreferredSize(new Dimension(90, 36));
        instBtn.addActionListener(e -> installMod(slug, title, mcVer, instBtn));

        p.add(info,    BorderLayout.CENTER);
        p.add(instBtn, BorderLayout.EAST);
        return p;
    }

    void installMod(String slug, String title, String mcVer, JButton btn) {
        btn.setEnabled(false); btn.setText("...");
        new Thread(()->{
            try {
                StringBuilder qb = new StringBuilder("https://api.modrinth.com/v2/project/"+slug+"/version");
                boolean has = false;
                if (!mcVer.isEmpty()) { qb.append("?game_versions=[%22").append(mcVer).append("%22]"); has = true; }
                if (isFabricSelected()) {
                    qb.append(has?"&":"?").append("loaders=[%22fabric%22]");
                }
                String url = qb.toString();
                String resp = get(url, null);
                @SuppressWarnings("unchecked") List<Object> vs = (List<Object>) Json.parse(resp);
                if (vs==null||vs.isEmpty()) {
                    resp = get("https://api.modrinth.com/v2/project/"+slug+"/version",null);
                    @SuppressWarnings("unchecked") List<Object> vs2 = (List<Object>) Json.parse(resp);
                    vs = vs2;
                }
                if (vs==null||vs.isEmpty()) throw new RuntimeException("No files found for "+title);

                @SuppressWarnings("unchecked") Map<String,Object> ver = (Map<String,Object>) vs.get(0);
                @SuppressWarnings("unchecked") List<Object> files = (List<Object>) ver.get("files");
                if (files==null||files.isEmpty()) throw new RuntimeException("Version has no files.");
                @SuppressWarnings("unchecked") Map<String,Object> file = (Map<String,Object>) files.get(0);
                String fUrl=(String)file.get("url"), fname=(String)file.get("filename");

                File modsDir = new File(MC_DIR,"mods"); modsDir.mkdirs();
                log("  Downloading mod: "+fname);
                dlFile(fUrl, modsDir.toPath().resolve(fname));
                log("  Installed: "+title);
                SwingUtilities.invokeLater(()->{
                    btn.setText("Done!"); btn.setBackground(new Color(20,60,25)); loadMods();
                });
            } catch (Exception ex) {
                log("  Mod install failed: "+ex.getMessage());
                SwingUtilities.invokeLater(()->{ btn.setEnabled(true); btn.setText("Install"); });
            }
        },"mod-install").start();
    }

    // ════════════════════════════════════════════════════════
    //  HTTP
    // ════════════════════════════════════════════════════════
    static String get(String url, String bearer) throws Exception {
        HttpClient c = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest.Builder rb = HttpRequest.newBuilder().uri(URI.create(url)).GET()
            .header("User-Agent","CraftLaunch/3.0");
        if (bearer!=null) rb.header("Authorization","Bearer "+bearer);
        return c.send(rb.build(),HttpResponse.BodyHandlers.ofString()).body();
    }
    static String post(String url, String body, String ct, String bearer) throws Exception {
        HttpClient c = HttpClient.newHttpClient();
        HttpRequest.Builder rb = HttpRequest.newBuilder().uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type",ct).header("User-Agent","CraftLaunch/3.0");
        if (bearer!=null) rb.header("Authorization","Bearer "+bearer);
        return c.send(rb.build(),HttpResponse.BodyHandlers.ofString()).body();
    }
    static String postJson(String url, String json) throws Exception {
        return post(url,json,"application/json",null);
    }
    static void dlFile(String url, Path dest) throws Exception {
        HttpClient c = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<InputStream> r = c.send(
            HttpRequest.newBuilder().uri(URI.create(url)).GET()
                .header("User-Agent","CraftLaunch/5.0").build(),
            HttpResponse.BodyHandlers.ofInputStream());
        if (r.statusCode() >= 400) {
            try { r.body().close(); } catch (Exception ignored) {}
            throw new IOException("HTTP " + r.statusCode() + " for " + url);
        }
        try (InputStream in = r.body()) { Files.copy(in,dest,StandardCopyOption.REPLACE_EXISTING); }
        // Sanity check: file must be at least 100 bytes (HTML error pages are tiny)
        if (Files.size(dest) < 100) {
            String head = Files.readString(dest, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (head.toLowerCase().contains("<html") || head.toLowerCase().contains("not found")) {
                Files.deleteIfExists(dest);
                throw new IOException("Got HTML error page from " + url);
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  JSON PARSER
    // ════════════════════════════════════════════════════════
    static class Json {
        private final String s; private int p;
        Json(String s){this.s=s.trim();}
        static Object parse(String json){
            if(json==null||json.isBlank())return null;
            return new Json(json.trim()).val();
        }
        Object val(){
            ws(); if(p>=s.length())return null;
            char c=s.charAt(p);
            if(c=='{') return obj();
            if(c=='[') return arr();
            if(c=='"') return str();
            if(c=='t'){p+=4;return Boolean.TRUE;}
            if(c=='f'){p+=5;return Boolean.FALSE;}
            if(c=='n'){p+=4;return null;}
            return num();
        }
        Map<String,Object> obj(){
            Map<String,Object> m=new LinkedHashMap<>();p++;ws();
            while(p<s.length()&&s.charAt(p)!='}'){
                String k=str();ws();if(p<s.length()&&s.charAt(p)==':')p++;ws();
                m.put(k,val());ws();if(p<s.length()&&s.charAt(p)==',')p++;ws();
            }
            if(p<s.length())p++;return m;
        }
        List<Object> arr(){
            List<Object> l=new ArrayList<>();p++;ws();
            while(p<s.length()&&s.charAt(p)!=']'){
                l.add(val());ws();if(p<s.length()&&s.charAt(p)==',')p++;ws();
            }
            if(p<s.length())p++;return l;
        }
        String str(){
            p++;StringBuilder sb=new StringBuilder();
            while(p<s.length()&&s.charAt(p)!='"'){
                if(s.charAt(p)=='\\'){
                    p++;
                    if(p<s.length()){
                        char esc=s.charAt(p);
                        if(esc=='n')sb.append('\n');
                        else if(esc=='t')sb.append('\t');
                        else if(esc=='r')sb.append('\r');
                        else sb.append(esc);
                    }
                } else sb.append(s.charAt(p));
                p++;
            }
            if(p<s.length())p++;return sb.toString();
        }
        Object num(){
            int st=p;
            while(p<s.length()&&"-0123456789.eE+".indexOf(s.charAt(p))>=0)p++;
            String n=s.substring(st,p);
            try{return n.contains(".")||n.contains("e")||n.contains("E")
                    ?Double.parseDouble(n):Long.parseLong(n);}
            catch(NumberFormatException e){return 0;}
        }
        void ws(){while(p<s.length()&&s.charAt(p)<=' ')p++;}
    }

    // ════════════════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════════════════
    static String jStr(String j, String k) {
        Matcher m = Pattern.compile("\""+k+"\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(j);
        if(m.find()) return m.group(1);
        m = Pattern.compile("\""+k+"\"\\s*:\\s*([\\d.]+)").matcher(j);
        if(m.find()) return m.group(1);
        return null;
    }
    static int jInt(String j, String k, int def) {
        String v=jStr(j,k); try{return v!=null?Integer.parseInt(v):def;}catch(Exception e){return def;}
    }
    static String extractUhs(String j) {
        Matcher m = Pattern.compile("\"uhs\"\\s*:\\s*\"([^\"]+)\"").matcher(j);
        return m.find()?m.group(1):null;
    }
    void runDiagnostics() {
        log("  --- DIAGNOSTICS ---");
        // Java version
        String jv = System.getProperty("java.version", "?");
        log("  Java version: " + jv);
        boolean jv17plus = false;
        try {
            int major = Integer.parseInt(jv.split("\\.")[0]);
            jv17plus = (major >= 17);
        } catch (Exception ignored) {}
        if (!jv17plus) {
            log("  WARNING: Java 17+ recommended for Minecraft 1.18+");
        }

        // Architecture
        String arch = System.getProperty("os.arch", "?");
        log("  Architecture: " + arch);
        if (!arch.contains("64")) {
            log("  WARNING: 32-bit Java limits RAM to ~1.5GB. Install 64-bit Java for larger RAM.");
        }

        // RAM
        long maxMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        log("  Available RAM: " + maxMb + " MB");

        // Disk space
        try {
            File mcRoot = new File(MC_DIR).getAbsoluteFile();
            File parent = mcRoot;
            while (parent != null && !parent.exists()) parent = parent.getParentFile();
            if (parent != null) {
                long freeMb = parent.getUsableSpace() / (1024 * 1024);
                log("  Free disk space: " + freeMb + " MB");
                if (freeMb < 1000) {
                    log("  WARNING: Less than 1GB free. Minecraft needs 1-2GB minimum.");
                }
            }
        } catch (Exception ignored) {}

        log("  -------------------");
    }

    void loadConfig() {
        try {
            if (CONFIG_FILE.exists()) {
                try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                    config.load(in);
                }
            }
        } catch (Exception ignored) {}
    }

    void saveConfig() {
        try {
            CONFIG_DIR.mkdirs();
            if (usernameField != null) config.setProperty("username", usernameField.getText());
            if (versionBox != null && versionBox.getSelectedItem() != null)
                config.setProperty("version", versionBox.getSelectedItem().toString());
            if (loaderBox != null && loaderBox.getSelectedItem() != null)
                config.setProperty("loader",  loaderBox.getSelectedItem().toString());
            if (ramSlider != null) config.setProperty("ram", String.valueOf(ramSlider.getValue()));
            try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                config.store(out, "CraftLaunch settings");
            }
        } catch (Exception ignored) {}
    }

    boolean isFabricSelected() {
        Object sel = loaderBox != null ? loaderBox.getSelectedItem() : null;
        return sel != null && sel.toString().toLowerCase().contains("fabric");
    }

    void installFabricApi(String mcVersion) throws Exception {
        File modsDir = new File(MC_DIR, "mods");
        modsDir.mkdirs();

        // Already installed for this version? skip.
        File[] existing = modsDir.listFiles();
        if (existing != null) {
            for (File f : existing) {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".jar")
                    && (n.startsWith("fabric-api-") || n.startsWith("fabric_api-"))
                    && n.contains(mcVersion)) {
                    log("  Fabric API already installed: " + f.getName());
                    return;
                }
            }
        }

        // Remove old fabric-api jars from other MC versions
        if (existing != null) {
            for (File f : existing) {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".jar")
                    && (n.startsWith("fabric-api-") || n.startsWith("fabric_api-"))) {
                    log("  Removing old Fabric API: " + f.getName());
                    f.delete();
                }
            }
        }

        // Fetch Fabric API for this MC version
        String url = "https://api.modrinth.com/v2/project/fabric-api/version"
            + "?game_versions=[%22" + mcVersion + "%22]&loaders=[%22fabric%22]";
        String resp = get(url, null);
        @SuppressWarnings("unchecked")
        List<Object> versions = (List<Object>) Json.parse(resp);
        if (versions == null || versions.isEmpty()) {
            log("  No Fabric API available for Minecraft " + mcVersion);
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String,Object> ver = (Map<String,Object>) versions.get(0);
        @SuppressWarnings("unchecked")
        List<Object> files = (List<Object>) ver.get("files");
        if (files == null || files.isEmpty()) return;
        @SuppressWarnings("unchecked")
        Map<String,Object> file = (Map<String,Object>) files.get(0);
        String fUrl  = (String) file.get("url");
        String fname = (String) file.get("filename");

        log("  Downloading Fabric API: " + fname);
        dlFile(fUrl, modsDir.toPath().resolve(fname));
        log("  Fabric API installed!");
    }


    static String mavenNameToPath(String name) {
        // "groupId:artifactId:version[:classifier]" -> "group/path/artifact/ver/artifact-ver[-cls].jar"
        String[] parts = name.split(":");
        if (parts.length < 3) return null;
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = parts.length > 3 ? "-" + parts[3] : "";
        String ext = ".jar";
        // Handle "name:ext" syntax (rare)
        int atIdx = version.indexOf('@');
        if (atIdx >= 0) {
            ext = "." + version.substring(atIdx+1);
            version = version.substring(0, atIdx);
        }
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifier + ext;
    }

    static String getMCDir() {
        String os=System.getProperty("os.name","").toLowerCase();
        if(os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            return (appdata!=null?appdata:System.getProperty("user.home"))+"\\.minecraft";
        }
        if(os.contains("mac")) return System.getProperty("user.home")+"/Library/Application Support/minecraft";
        return System.getProperty("user.home")+"/.minecraft";
    }
    static String osName() {
        String os=System.getProperty("os.name","").toLowerCase();
        if(os.contains("win")) return "windows";
        if(os.contains("mac")) return "osx";
        return "linux";
    }
    static void mkdirs(Path... paths){ for(Path p:paths) p.toFile().mkdirs(); }
    static String fmtNum(String n) {
        try{long v=Long.parseLong(n.replaceAll("[^0-9]",""));
            if(v>=1_000_000) return String.format("%.1fM",v/1_000_000.0);
            if(v>=1_000)     return String.format("%.1fK",v/1_000.0);
            return ""+v;}catch(Exception e){return n;}
    }
    void clipboard(String s) {
        try {
            java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(s);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
        } catch (Exception ex) { /* clipboard unavailable */ }
    }
    void log(String msg) {
        SwingUtilities.invokeLater(()->{
            logArea.append(msg+"\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    void setProgress(int v, String msg) {
        SwingUtilities.invokeLater(()->{
            progress.setValue(v);
            progress.setString(v>0?v+"% "+msg:msg);
        });
    }
    void status(String msg){ SwingUtilities.invokeLater(()->statusBar.setText(msg)); }

    // ════════════════════════════════════════════════════════
    //  UI HELPERS
    // ════════════════════════════════════════════════════════
    static JLabel  lbl(String t,Font f,Color c){ JLabel l=new JLabel(t);l.setFont(f);l.setForeground(c);return l; }
    static JLabel  sLbl(String t){ JLabel l=new JLabel(t);l.setFont(new Font("Monospaced",Font.BOLD,10));l.setForeground(MUTED);l.setAlignmentX(LEFT_ALIGNMENT);return l; }
    static JPanel  opaque(LayoutManager lm){ JPanel p=new JPanel(lm);p.setOpaque(false);return p; }
    static JButton btn(String t,Color bg,Color fg){
        JButton b=new JButton(t); b.setBackground(bg); b.setForeground(fg);
        b.setFont(new Font("Monospaced",Font.BOLD,11)); b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(5,14,5,14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    static void styleField(JTextField f){
        f.setBackground(new Color(8,10,14)); f.setForeground(TEXT);
        f.setCaretColor(GREEN); f.setFont(new Font("Monospaced",Font.PLAIN,12));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),new EmptyBorder(6,10,6,10)));
    }
    static Image makeDiamond(){
        BufferedImage img=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2=img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(GREEN); g2.fillPolygon(new int[]{16,28,16,4},new int[]{4,16,28,16},4);
        g2.dispose(); return img;
    }

    static class CardPanel extends JPanel {
        CardPanel(){ setOpaque(false); }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
            g2.setColor(BORDER); g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
            g2.dispose();
        }
    }
    static class GradPanel extends JPanel {
        final Color a,b;
        GradPanel(Color a,Color b){ this.a=a; this.b=b; setOpaque(false); }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setPaint(new GradientPaint(0,0,a,0,getHeight(),b));
            g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════
    //  MAIN
    // ════════════════════════════════════════════════════════
    public static void main(String[] args){
        try{ UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }catch(Exception ignored){}
        SwingUtilities.invokeLater(MinecraftLauncher::new);
    }
}
