package dk.martinu.ao.fonteditor.edit;

import org.jetbrains.annotations.*;

import java.util.*;

import static dk.martinu.ao.fonteditor.edit.EditEvent.Type.*;

/**
 * A double-linked queue implementation for storing {@link Edit}s. The queue
 * can be navigated back and forth by undoing and redoing edits that have been
 * added to the queue.
 *
 * @param <T> the parent type on which an edit is done
 * @author Adam Martinu
 * @see #add(Edit)
 * @see #redo()
 * @see #undo()
 * @since 1.0
 */
public class EditQueue<T> {

    /**
     * List of event listeners.
     */
    protected final ArrayList<EditQueueListener<T>> listeners = new ArrayList<>(1);
    /**
     * The current position in the queue. If the queue is empty, the cursor
     * will point to a {@link RootEdit} instance.
     */
    @NotNull
    protected Node<T> cursor = new Node<>(new RootEdit<>());

    /**
     * Adds the specified edit to the queue and notifies all event listeners
     * that the queue has changed. The event type depends on the current
     * position in the queue. The event type is {@code APPEND} if the edit is
     * added to the end of the queue, otherwise it is {@code INSERT}. When the
     * edit is inserted into the queue, all subsequent edits are dropped (they
     * can no longer be redone) and the specified edit becomes the new tail.
     *
     * @param edit the edit to add
     * @throws NullPointerException if {@code edit} is {@code null}
     */
    @Contract(mutates = "this")
    public void add(@NotNull final Edit<T> edit) {
        Objects.requireNonNull(edit, "edit is null");
        final boolean isAppend = cursor.next == null;
        cursor = new Node<>(edit, cursor);
        final EditEvent<T> event = new EditEvent<>(isAppend ? APPEND : INSERT, edit, cursor.position);
        listeners.forEach(listener -> listener.queueChanged(event));
    }

    /**
     * Adds the specified event listener to the queue to receive events when
     * the queue is changed.
     *
     * @param listener the listener to add
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    @Contract(mutates = "this")
    public void addListener(@NotNull final EditQueueListener<T> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener is null"));
    }

    /**
     * Returns the current cursor position in the queue. If the queue is empty
     * or all edits have been undone, then the returned value is {@code -1}.
     */
    public int position() {
        return cursor.position;
    }

    /**
     * Calls {@link Edit#redo()} on the edit that comes after the current
     * position, and updates the position to point at that edit.
     * <p>
     * <b>NOTE:</b> this method must only be called after {@link #undo()} has
     * been called at least once, and can never be called more times than
     * {@code undo} has been called.
     *
     * @throws NoSuchElementException if there is no edit in the queue to redo
     */
    @Contract(mutates = "this")
    public void redo() {
        if (cursor.next != null) {
            cursor = cursor.next;
            cursor.edit.redo();
            final EditEvent<T> event = new EditEvent<>(REDO, cursor.edit, cursor.position);
            listeners.forEach(listener -> listener.queueChanged(event));
        }
        else
            throw new NoSuchElementException("no edit in queue to redo");
    }

    /**
     * Removes the specified event listener from the queue so it will no longer
     * receive events when the queue is changed.
     *
     * @param listener the listener to remove
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    @Contract(mutates = "this")
    public void removeListener(@NotNull final EditQueueListener<T> listener) {
        listeners.remove(Objects.requireNonNull(listener, "listener is null"));
    }

    /**
     * Calls {@link Edit#undo()} on the edit at the current position, and
     * updates the position to point at the preceding edit.
     * <p>
     * <b>NOTE:</b> this method must only be called after {@link #add(Edit)}
     * has been called at least once, and can never be called more times than
     * {@code add} has been called.
     *
     * @throws NoSuchElementException if there is no edit in the queue to undo
     */
    @Contract(mutates = "this")
    public void undo() {
        if (cursor.previous != null) {
            final Edit<T> edit = cursor.edit;
            final int position = cursor.position;
            edit.undo();
            cursor = cursor.previous;
            final EditEvent<T> event = new EditEvent<>(UNDO, edit, position);
            listeners.forEach(listener -> listener.queueChanged(event));
        }
        else
            throw new NoSuchElementException("no edit in queue to undo");
    }

    /**
     * Double-linked node used for holding an {@link Edit} instance and
     * navigating the queue.
     */
    protected static class Node<T> {

        /**
         * The edit.
         */
        @NotNull
        protected final Edit<T> edit;
        /**
         * The previous node. If {@code null}, then this node is the head and
         * calling {@link #undo()} will fail.
         */
        @Nullable
        protected final Node<T> previous;
        /**
         * The next node. If {@code null}, then this node is the tail and
         * calling {@link #redo()} will fail.
         */
        @Nullable
        protected Node<T> next = null;
        /**
         * The position of this node in the queue, equal to
         * {@code previous.position + 1}. If this node does not have a
         * previous node then its position is {@code -1}.
         */
        protected final int position;

        /**
         * Constructs a new head node.
         *
         * @param edit the edit
         */
        protected Node(@NotNull final Edit<T> edit) {
            this(edit, null);
        }

        /**
         * Constructs a new node with the specified edit and preceding node,
         * linking the two nodes together.
         *
         * @param edit     the edit
         * @param previous the previous node, or {@code null}
         */
        protected Node(@NotNull final Edit<T> edit, @Nullable final Node<T> previous) {
            this.edit = edit;
            this.previous = previous;
            if (previous != null) {
                previous.next = this;
                position = previous.position + 1;
            }
            else
                position = -1;
        }
    }
}
