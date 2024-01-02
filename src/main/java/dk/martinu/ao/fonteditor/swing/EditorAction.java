/*
 * Copyright (c) 2024, Adam Martinu. All rights reserved. Altering or removing
 * copyright notices or this file header is not allowed.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");  you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package dk.martinu.ao.fonteditor.swing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Convenience class to create and initialize {@code Action} instances.
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
        this(
                Objects.requireNonNull(name, "name is null"),
                null,
                enabled,
                mnemonic,
                null,
                action);
    }

    /**
     * Constructs a new action with the specified properties.
     *
     * @throws NullPointerException if {@code icon} or {@code action} is
     *                              {@code null}
     */
    public EditorAction(@NotNull final Icon icon, final boolean enabled, @NotNull final Consumer<ActionEvent> action) {
        this(
                null,
                Objects.requireNonNull(icon, "icon is null"),
                enabled,
                null,
                null,
                action);
    }

    /**
     * Constructs a new action with the specified properties.
     *
     * @throws NullPointerException if {@code name}, {@code accelerator} or
     *                              {@code action} is {@code null}
     */
    public EditorAction(@NotNull final String name, final boolean enabled, final int mnemonic,
            @NotNull final KeyStroke accelerator, @NotNull final Consumer<ActionEvent> action) {
        this(
                Objects.requireNonNull(name, "name is null"),
                null,
                enabled,
                mnemonic,
                Objects.requireNonNull(accelerator, "accelerator is null"),
                action);
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
        if (mnemonic != null) { putValue(Action.MNEMONIC_KEY, mnemonic); }
        if (accelerator != null) {
            putValue(Action.ACCELERATOR_KEY, accelerator);
        }
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
