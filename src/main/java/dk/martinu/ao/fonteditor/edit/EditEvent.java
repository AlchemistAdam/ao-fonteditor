package dk.martinu.ao.fonteditor.edit;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An event which indicates a structural change made to an {@link EditQueue}.
 * The queue was changed either by appending or inserting an edit, or by
 * undoing or redoing an edit already in the queue.
 *
 * @param type     the type of event
 * @param edit     the edit
 * @param position the position of the edit in the queue
 * @param <T>      the parent type on which an edit is done
 * @author Adam Martinu
 * @since 1.0
 */
public record EditEvent<T>(@NotNull Type type, @NotNull Edit<T> edit, int position) {

    /**
     * Constructs a new event.
     *
     * @param type     the type of event
     * @param edit     the edit
     * @param position the position of the edit in the queue
     * @throws NullPointerException if {@code type} or {@code edit} is
     *                              {@code null}
     */
    public EditEvent(@NotNull final Type type, @NotNull final Edit<T> edit, final int position) {
        this.type = Objects.requireNonNull(type, "type is null");
        this.edit = Objects.requireNonNull(edit, "edit is null");
        this.position = position;
    }

    /**
     * Event type constants.
     */
    public enum Type {
        /**
         * The edit was appended to the queue.
         *
         * @see EditQueue#add(Edit)
         */
        APPEND,
        /**
         * The edit was inserted into the queue.
         *
         * @see EditQueue#add(Edit)
         */
        INSERT,
        /**
         * The edit that was previously undone was redone.
         *
         * @see EditQueue#redo()
         */
        REDO,
        /**
         * The edit was undone.
         *
         * @see EditQueue#undo()
         */
        UNDO;
    }
}
