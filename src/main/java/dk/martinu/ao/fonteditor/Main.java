package dk.martinu.ao.fonteditor;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

import javax.swing.SwingUtilities;

import dk.martinu.ao.fonteditor.swing.EditorFrame;

/**
 * Main class for starting a font editor application.
 *
 * @author Adam Martinu
 * @since 1.0
 */
public class Main {

    public static void main(@NotNull String[] args) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                final EditorFrame editor = new EditorFrame();
                editor.setVisible(true);
            });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
