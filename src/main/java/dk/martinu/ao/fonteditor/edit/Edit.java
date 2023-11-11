package dk.martinu.ao.fonteditor.edit;

/**
 * Base interface for edits which can be added to an {@link EditQueue}.
 *
 * @param <T> the parent type on which an edit is done
 * @author Adam Martinu
 * @since 1.0
 */
public interface Edit<T> {

    /**
     * Returns the parent object on which this edit was done.
     */
    T parent();

    /**
     * Redoes the edit.
     */
    void redo();

    /**
     * Undoes the edit.
     */
    void undo();
}
