package hopsync.desktop.ui;

import hopsync.core.failure.SyncFailure;
import hopsync.core.failure.SyncFailures;
import hopsync.core.manifest.Manifest;
import hopsync.core.manifest.PackInfo;
import hopsync.core.pack.Pack;
import hopsync.core.pack.PackRegistry;
import hopsync.desktop.i18n.Messages;
import hopsync.desktop.kit.Palette;
import hopsync.platform.manifest.ManifestService;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

/**
 * Lets the player choose the modpack (manifest URL) and the game folder to keep in sync. Edits the active
 * pack, or adds the first one.
 */
public final class SettingsDialog extends JDialog {
    private final PackRegistry registry;
    private final Pack pack;
    private final Path settingsFile;
    private final ManifestService manifestService;
    private final JTextField urlField = new JTextField(40);
    private final JTextField gameDirField = new JTextField(40);
    private final JLabel checkResult = new JLabel(" ");
    private final JButton checkButton = new JButton("Comprobar");
    private boolean saved;

    private SettingsDialog(Window owner, PackRegistry registry, Pack pack, Path settingsFile,
                           ManifestService manifestService, String url, Path gameDir, boolean firstRun) {
        super(owner, "Ajustes del pack", ModalityType.APPLICATION_MODAL);
        this.registry = registry;
        this.pack = pack;
        this.settingsFile = settingsFile;
        this.manifestService = manifestService;

        urlField.setText(url);
        gameDirField.setText(gameDir.toString());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        if (firstRun) {
            c.gridx = 0; c.gridy = 0; c.gridwidth = 3;
            form.add(new JLabel("<html>Elige el pack que quieres mantener sincronizado y la carpeta del juego.<br>"
                + "Si usas Prism, CurseForge o Modrinth App, elige la carpeta de la instancia.</html>"), c);
            c.gridwidth = 1;
        }

        c.gridx = 0; c.gridy = 1;
        form.add(new JLabel("URL del manifest:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        form.add(urlField, c);
        c.gridx = 2; c.fill = GridBagConstraints.NONE; c.weightx = 0;
        form.add(checkButton, c);

        c.gridx = 1; c.gridy = 2; c.gridwidth = 2;
        form.add(checkResult, c);
        c.gridwidth = 1;

        c.gridx = 0; c.gridy = 3;
        form.add(new JLabel("Carpeta del juego:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        form.add(gameDirField, c);
        c.gridx = 2; c.fill = GridBagConstraints.NONE; c.weightx = 0;
        JButton browse = new JButton("Examinar…");
        form.add(browse, c);

        JButton save = new JButton("Guardar");
        JButton cancel = new JButton("Cancelar");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(cancel);

        checkButton.addActionListener(e -> checkManifest());
        browse.addActionListener(e -> chooseGameDir());
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(save);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    /**
     * Shows the dialog and returns true if new settings were saved. Must run on the EDT.
     *
     * @param firstRun shows an introduction for players opening the app for the first time
     */
    public static boolean show(Window owner, PackRegistry registry, ManifestService manifestService,
                               String presetManifestUrl, Path settingsFile, Path defaultGameDir, boolean firstRun) {
        Pack pack = registry.active().orElse(null);
        String url = pack != null ? pack.manifestUrl() : presetManifestUrl;
        Path gameDir = pack != null ? pack.gameDir() : defaultGameDir;
        SettingsDialog dialog = new SettingsDialog(owner, registry, pack, settingsFile, manifestService, url, gameDir,
            firstRun);
        dialog.setVisible(true);
        return dialog.saved;
    }

    private void chooseGameDir() {
        JFileChooser chooser = new JFileChooser(gameDirField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Carpeta del juego");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            gameDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void checkManifest() {
        String url = urlField.getText().trim();
        checkButton.setEnabled(false);
        showResult("Comprobando…", false);
        new SwingWorker<Manifest, Void>() {
            @Override
            protected Manifest doInBackground() throws IOException {
                return manifestService.fetchManifest(url);
            }

            @Override
            protected void done() {
                checkButton.setEnabled(true);
                try {
                    PackInfo pack = get().pack();
                    showResult("✔ " + pack.name() + " " + pack.version() + " · Minecraft " + pack.minecraft()
                        + " · " + pack.loader() + " · " + get().mods().size() + " mods", false);
                } catch (ExecutionException e) {
                    showResult("✖ " + Messages.failure(SyncFailures.from(e.getCause(), "")), true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void save() {
        String url = urlField.getText().trim();
        String error = validateUrl(url);
        if (error == null) {
            error = validateGameDir(gameDirField.getText().trim());
        }
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Ajustes no válidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Path gameDir = Path.of(gameDirField.getText().trim()).toAbsolutePath();
        try {
            if (pack == null) {
                registry.add(url, gameDir);
            } else {
                // A different link may be a different pack: its name comes from the next manifest
                Pack changed = url.equals(pack.manifestUrl()) ? pack : pack.withManifestUrl(url).withName("");
                registry.update(changed.withGameDir(gameDir));
            }
            saved = true;
            dispose();
        } catch (IOException e) {
            SyncFailure failure = SyncFailures.from(e, settingsFile.toString());
            if (failure instanceof SyncFailure.FolderUsedByPack) {
                JOptionPane.showMessageDialog(this, Messages.failure(failure), "Ajustes no válidos",
                    JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, Messages.get("settings.saveFailed", Messages.failure(failure)),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static String validateUrl(String url) {
        if (url.isEmpty()) return "Escribe la URL del manifest del pack.";
        if (!url.startsWith("https://")) return "La URL del manifest debe empezar por https://";
        return null;
    }

    static String validateGameDir(String dir) {
        if (dir.isEmpty()) return "Elige la carpeta del juego.";
        try {
            Path path = Path.of(dir);
            if (!Files.isDirectory(path)) return "La carpeta del juego no existe: " + dir;
            if (!Files.isWritable(path)) return "No se puede escribir en la carpeta del juego: " + dir;
        } catch (InvalidPathException e) {
            return "Ruta no válida: " + dir;
        }
        return null;
    }

    private void showResult(String text, boolean error) {
        checkResult.setText(text);
        checkResult.setForeground(error ? Palette.error() : UIManager.getColor("Label.foreground"));
        pack();
    }
}
