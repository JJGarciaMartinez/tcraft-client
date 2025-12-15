package ui;

import model.ModInfo;
import service.FileSystemService;
import service.ModUpdater;
import util.LogMessageParser;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UIController {
    private final LauncherUI ui;
    private final FileSystemService fileSystemService;
    private Thread updateThread;
    private volatile boolean hasErrors = false;
    private final AtomicInteger completedMods = new AtomicInteger(0);
    private volatile int failedMods = 0;

    public UIController(LauncherUI ui) {
        this.ui = ui;
        this.fileSystemService = new FileSystemService();
        setupListeners();
        loadCurrentMods();
    }

    /**
     * Carga y muestra los mods actualmente instalados
     */
    private void loadCurrentMods() {
        // Cargar mods en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                List<String> currentMods = fileSystemService.getCurrentMods();
                int modCount = currentMods.size();

                // Actualizar UI
                ui.getModListPanel().showCurrentMods(currentMods);

                if (modCount > 0) {
                    ui.getHeaderPanel().setStatusSuccess();
                    ui.getHeaderPanel().updateModsStatus(modCount + (modCount == 1 ? " mod instalado" : " mods instalados"));
                } else {
                    ui.getHeaderPanel().setStatusWarning();
                    ui.getHeaderPanel().updateModsStatus("No se encontraron mods instalados");
                }
            } catch (Exception e) {
                System.err.println("Error al cargar mods: " + e.getMessage());
                ui.getHeaderPanel().setStatusError();
                ui.getHeaderPanel().updateModsStatus("Error al cargar mods");
            }
        }).start();
    }

    private void setupListeners() {
        ui.getControlPanel().setUpdateButtonListener(_ -> startUpdate());
        ui.getControlPanel().setCancelButtonListener(_ -> cancelUpdate());
        ui.getControlPanel().setRefreshButtonListener(_ -> refreshModList());
    }

    /**
     * Refresca la lista de mods instalados
     */
    private void refreshModList() {
        ui.getHeaderPanel().setStatusProcessing();
        ui.getHeaderPanel().updateModsStatus("Refrescando lista...");
        ui.getControlPanel().updateStatus("Actualizando información...");

        loadCurrentMods();

        // Pequeño delay para mostrar el feedback visual
        Timer timer = new Timer(500, e -> {
            ui.getControlPanel().updateStatus("Lista actualizada");
            ((Timer)e.getSource()).stop();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void startUpdate() {
        ui.getModListPanel().clear();
        ui.getControlPanel().setUpdateInProgress(true);
        ui.getControlPanel().updateStatus("Actualizando...");
        ui.getHeaderPanel().updateModsStatus("Actualización en progreso");
        ui.getHeaderPanel().setStatusProcessing();

        // Reset counters
        hasErrors = false;
        completedMods.set(0);
        failedMods = 0;

        updateThread = new Thread(this::executeUpdate);
        updateThread.start();
    }

    private void cancelUpdate() {
        if (updateThread != null && updateThread.isAlive()) {
            updateThread.interrupt();
            ui.getControlPanel().updateStatus("Actualización cancelada");
            ui.getHeaderPanel().updateModsStatus("Actualizador de Mods");
            ui.getHeaderPanel().setStatusWarning();
            ui.getControlPanel().setUpdateInProgress(false);

            // Recargar los mods actuales
            loadCurrentMods();
        }
    }

    private void executeUpdate() {
        try {
            ModUpdater updater = new ModUpdater(
                    this::onLog,
                    this::onProgress,
                    this::onModInfo  // Nuevo callback para ModInfo completo
            );
            updater.initUpdate();

            SwingUtilities.invokeLater(() -> {
                ui.getControlPanel().setUpdateInProgress(false);

                if (failedMods > 0) {
                    ui.getControlPanel().updateStatus(
                            String.format("Completado con errores (%d fallidos)", failedMods)
                    );
                    ui.getHeaderPanel().updateModsStatus("Actualización completada con errores");
                    ui.getHeaderPanel().setStatusWarning();

                    JOptionPane.showMessageDialog(ui,
                            String.format(
                                    """
                                            Actualización completada con algunos errores.
                                            
                                            Mods exitosos: %d
                                            Mods fallidos: %d
                                            
                                            Revisa los detalles arriba.""",
                                    completedMods.get(), failedMods
                            ),
                            "Actualización Completada con Errores",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    ui.getControlPanel().updateStatus("Actualización completada exitosamente");
                    ui.getHeaderPanel().updateModsStatus("Mods actualizados correctamente");
                    ui.getHeaderPanel().setStatusSuccess();

                    JOptionPane.showMessageDialog(ui,
                            String.format(
                                    """
                                            Todos los mods han sido actualizados correctamente.
                                            
                                            Total de mods: %d""",
                                    completedMods.get()
                            ),
                            "Actualización Completa",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });

        } catch (Exception e) {
            System.err.println("Error crítico durante la actualización: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                ui.getControlPanel().updateStatus("Error crítico en la actualización");
                ui.getHeaderPanel().updateModsStatus("Error");
                ui.getHeaderPanel().setStatusError();
                ui.getControlPanel().setUpdateInProgress(false);

                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = e.getClass().getSimpleName();
                }

                JOptionPane.showMessageDialog(ui,
                        "Error crítico durante la actualización:\n\n" + errorMessage,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);

                // Recargar los mods actuales tras el error
                loadCurrentMods();
            });
        }
    }

    private void onLog(String message) {
        System.out.println(message); // Para debugging

        // Parsear el mensaje para actualizar estado de las tarjetas
        if (message.contains("Verificando") || message.contains("---")) {
            // Mensajes de sistema, no crear tarjetas
            return;
        }

        if (message.contains("Descargando:")) {
            String modName = LogMessageParser.extractModName(message);
            if (modName != null) {
                System.out.println("🔄 Actualizando estado 'Descargando' para: '" + modName + "'");
                ui.getModListPanel().setModDownloading(modName);
            }
        } else if (message.contains("Completado:")) {
            String modName = LogMessageParser.extractModName(message);
            if (modName != null) {
                System.out.println("🔄 Actualizando estado 'Completado' para: '" + modName + "'");
                ui.getModListPanel().setModCompleted(modName, true);
                incrementCompletedMods();
            }
        } else if (message.contains("OK:")) {
            String modName = LogMessageParser.extractModName(message);
            if (modName != null) {
                System.out.println("🔄 Actualizando estado 'OK' para: '" + modName + "'");
                ui.getModListPanel().setModChecking(modName);
                ui.getModListPanel().setModAlreadyInstalled(modName);
                incrementCompletedMods();
            }
        } else if (message.contains("Error en")) {
            String modName = LogMessageParser.extractModNameFromError(message);
            if (modName != null) {
                ui.getModListPanel().setModCompleted(modName, false);
                failedMods++;
                hasErrors = true;
            }
        } else if (message.contains("ERROR CRÍTICO") || message.contains("error") || message.contains("Error")) {
            hasErrors = true;
        }
    }

    /**
     * Callback que recibe la información completa del mod cuando se procesa
     */
    private void onModInfo(ModInfo modInfo) {
        // Usar addModSync para asegurar que la tarjeta se crea ANTES de procesar logs
        ui.getModListPanel().addModSync(modInfo);
    }

    private void incrementCompletedMods() {
        completedMods.addAndGet(1);
    }

    private void onProgress(int current, int total) {
        int percentage = (int) ((current / (float) total) * 100);
        ui.getControlPanel().updateStatus(
                String.format("Progreso: %d/%d (%d%%)", current, total, percentage)
        );
    }

    public static void create() {
        LauncherUI ui = new LauncherUI();
        ui.setVisible(true);
        new UIController(ui);
    }

    public boolean isHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }
}