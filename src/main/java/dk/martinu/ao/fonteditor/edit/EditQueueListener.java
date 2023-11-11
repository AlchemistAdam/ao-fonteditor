package dk.martinu.ao.fonteditor.edit;

import org.jetbrains.annotations.NotNull;

/**
 * Listener interface to receive {@link EditEvent}s when changes are made to an
 * {@link EditQueue}.
 *
 * @param <T> the parent type on which an edit is done
 * @author Adam Martinu
 * @since 1.0
 */
public interface EditQueueListener<T> {

    /**
     * Called when the queue has changed.
     *
     * @param event the event
     */
    void queueChanged(@NotNull final EditEvent<T> event);
}
