package dk.martinu.ao.fonteditor.swing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.*;

/**
 * Convenience class to create and initialize actions with very few lines of
 * code.
 *
 * @author Adam Martinu
 * @since 1.0
 */
public class EditorAction extends AbstractAction {

    /**
     * The action to perform.
     */
    @NotNull
    public final Consumer<ActionEvent> action;

    /**
     * Constructs a new action with the specified properties.
     *
     * @throws NullPointerException if {@code name} or {@code action} is
     *                              {@code null}
     */
    public EditorAction(@NotNull final String name, final boolean enabled, final int mnemonic,
            @NotNull final Consumer<ActionEvent> action) {
        this(Objects.requireNonNull(name, "name is null"), null, enabled, mnemonic, null, action);
    }

    /**
     * Constructs a new action with the specified properties.
     *
     * @throws NullPointerException if {@code icon} or {@code action} is
     *                              {@code null}
     */
    public EditorAction(@NotNull final Icon icon, final boolean enabled, @NotNull final Consumer<ActionEvent> action) {
        this(null, Objects.requireNonNull(icon, "icon is null"), enabled, null, null, action);
    }

    /**
     * Constructs a new action with the specified properties.
     *
     * @throws NullPointerException if {@code name}, {@code accelerator} or
     *                              {@code action} is {@code null}
     */
    public EditorAction(@NotNull final String name, final boolean enabled, final int mnemonic,
            @NotNull final KeyStroke accelerator, @NotNull final Consumer<ActionEvent> action) {
        this(Objects.requireNonNull(name, "name is null"), null, enabled, mnemonic,
                Objects.requireNonNull(accelerator, "accelerator is null"), action);
    }

    /**
     * Constructs a new action with the specified properties.
     *
     * @throws NullPointerException if {@code action} is {@code null}
     */
    private EditorAction(@Nullable final String name, @Nullable final Icon icon, final boolean enabled,
            @Nullable final Integer mnemonic, @Nullable final KeyStroke accelerator,
            @NotNull final Consumer<ActionEvent> action) throws NullPointerException {
        super(name, icon);
        setEnabled(enabled);
        if (mnemonic != null)
            putValue(Action.MNEMONIC_KEY, mnemonic);
        if (accelerator != null)
            putValue(Action.ACCELERATOR_KEY, accelerator);
        this.action = Objects.requireNonNull(action, "action is null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(@NotNull final ActionEvent event) {
        action.accept(event);
    }
}
