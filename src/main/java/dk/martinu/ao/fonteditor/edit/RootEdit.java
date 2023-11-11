package dk.martinu.ao.fonteditor.edit;

import org.jetbrains.annotations.Contract;

/**
 * Special {@link Edit} implementation used only by {@link EditQueue}s to mark
 * the root of the queue. The edit cannot be undone nor redone; doing so will
 * throw an {@code UnsupportedOperationException}.
 *
 * @param <T> the parent type on which an edit is done
 * @author Adam Martinu
 * @since 1.0
 */
class RootEdit<T> implements Edit<T> {

    /**
     * Constructs a new {@code RootEdit} instance.
     */
    @Contract(pure = true)
    public RootEdit() { }

    /**
     * Returns {@code null}.
     */
    @Contract("-> null")
    @Override
    public T parent() {
        return null;
    }

    /**
     * Throws an {@code UnsupportedOperationException}.
     */
    @Contract("-> fail")
    @Override
    public void redo() {
        throw new UnsupportedOperationException("redo");
    }

    /**
     * Throws an {@code UnsupportedOperationException}.
     */
    @Contract("-> fail")
    @Override
    public void undo() {
        throw new UnsupportedOperationException("undo");
    }
}
