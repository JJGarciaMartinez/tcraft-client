package ui;

import service.FileSystemService;
import service.ModUpdater;
import util.LogMessageParser;

import javax.swing.*;
import java.util.List;

public class UIController {
    private final LauncherUI ui;
    private final FileSystemService fileSystemService;
    private Thread updateThread;
    private volatile boolean hasErrors = false;
    private volatile int completedMods = 0;
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
                ui.getHeaderPanel().updateModCount(modCount);

                if (modCount > 0) {
                    ui.getHeaderPanel().setStatusSuccess();
                    ui.getHeaderPanel().updateSubtitle(modCount + (modCount == 1 ? " mod detectado" : " mods detectados"));
                } else {
                    ui.getHeaderPanel().setStatusWarning();
                    ui.getHeaderPanel().updateSubtitle("No se encontraron mods instalados");
                }
            } catch (Exception e) {
                e.printStackTrace();
                ui.getHeaderPanel().setStatusError();
                ui.getHeaderPanel().updateSubtitle("Error al cargar mods");
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
        ui.getHeaderPanel().updateSubtitle("Refrescando lista...");
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
        ui.getHeaderPanel().updateSubtitle("Actualización en progreso");
        ui.getHeaderPanel().setStatusProcessing();

        // Reset counters
        hasErrors = false;
        completedMods = 0;
        failedMods = 0;

        updateThread = new Thread(this::executeUpdate);
        updateThread.start();
    }

    private void cancelUpdate() {
        if (updateThread != null && updateThread.isAlive()) {
            updateThread.interrupt();
            ui.getControlPanel().updateStatus("Actualización cancelada");
            ui.getHeaderPanel().updateSubtitle("Actualizador de Mods");
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
                    this::onProgress
            );
            updater.initUpdate();

            SwingUtilities.invokeLater(() -> {
                ui.getControlPanel().setUpdateInProgress(false);

                if (failedMods > 0) {
                    ui.getControlPanel().updateStatus(
                            String.format("Completado con errores (%d fallidos)", failedMods)
                    );
                    ui.getHeaderPanel().updateSubtitle("Actualización completada con errores");
                    ui.getHeaderPanel().setStatusWarning();

                    JOptionPane.showMessageDialog(ui,
                            String.format(
                                    "Actualización completada con algunos errores.\n\n" +
                                            "Mods exitosos: %d\n" +
                                            "Mods fallidos: %d\n\n" +
                                            "Revisa los detalles arriba.",
                                    completedMods, failedMods
                            ),
                            "Actualización Completada con Errores",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    ui.getControlPanel().updateStatus("Actualización completada exitosamente");
                    ui.getHeaderPanel().updateSubtitle("Mods actualizados correctamente");
                    ui.getHeaderPanel().setStatusSuccess();

                    JOptionPane.showMessageDialog(ui,
                            String.format(
                                    "Todos los mods han sido actualizados correctamente.\n\n" +
                                            "Total de mods: %d",
                                    completedMods
                            ),
                            "Actualización Completa",
                            JOptionPane.INFORMATION_MESSAGE);
                }

                // Actualizar contador de mods en el header
                ui.getHeaderPanel().updateModCount(completedMods);
            });

        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                ui.getControlPanel().updateStatus("Error crítico en la actualización");
                ui.getHeaderPanel().updateSubtitle("Error");
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

        // Parsear el mensaje para actualizar las tarjetas de mods
        if (message.contains("Verificando") || message.contains("---")) {
            // Mensajes de sistema, no crear tarjetas
            return;
        }

        if (message.contains("Descargando:")) {
            String modName = LogMessageParser.extractModName(message);
            if (modName != null) {
                ui.getModListPanel().addMod(modName);
                ui.getModListPanel().setModDownloading(modName);
            }
        } else if (message.contains("Completado:")) {
            String modName = LogMessageParser.extractModName(message);
            if (modName != null) {
                ui.getModListPanel().setModCompleted(modName, true);
                completedMods++;
            }
        } else if (message.contains("OK:")) {
            String modName = LogMessageParser.extractModName(message);
            if (modName != null) {
                // Agregar el mod y marcarlo como "ya instalado"
                ui.getModListPanel().addMod(modName);

                // Usar Timer para dar tiempo a que se cree la tarjeta antes de cambiar el estado
                Timer timer = new Timer(50, e -> {
                    ui.getModListPanel().setModChecking(modName);
                    // Segundo timer para marcar como "ya instalado"
                    Timer installedTimer = new Timer(100, e2 -> {
                        ui.getModListPanel().setModAlreadyInstalled(modName);
                        ((Timer)e2.getSource()).stop();
                    });
                    installedTimer.setRepeats(false);
                    installedTimer.start();
                    ((Timer)e.getSource()).stop();
                });
                timer.setRepeats(false);
                timer.start();
                completedMods++;
            }
        } else if (message.contains("Error en")) {
            String modName = LogMessageParser.extractModNameFromError(message);
            if (modName != null) {
                // Si el mod no existe en la lista, agregarlo primero
                ui.getModListPanel().addMod(modName);
                ui.getModListPanel().setModCompleted(modName, false);
                failedMods++;
                hasErrors = true;
            }
        } else if (message.contains("ERROR CRÍTICO") || message.contains("error") || message.contains("Error")) {
            hasErrors = true;
        }
    }

    private void onProgress(int current, int total) {
        int percentage = (int) ((current / (float) total) * 100);
        ui.getControlPanel().updateStatus(
                String.format("Progreso: %d/%d (%d%%)", current, total, percentage)
        );
    }

    public static UIController create() {
        LauncherUI ui = new LauncherUI();
        ui.setVisible(true);
        return new UIController(ui);
    }

    public boolean isHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }
}