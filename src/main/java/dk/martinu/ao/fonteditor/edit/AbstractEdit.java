package dk.martinu.ao.fonteditor.edit;

import org.jetbrains.annotations.Contract;

// DOC
public abstract class AbstractEdit<T> implements Edit<T> {

    protected T parent;

    @Contract(pure = true)
    public AbstractEdit(final T parent) {
        this.parent = parent;
    }

    @Contract(pure = true)
    @Override
    public T parent() {
        return parent;
    }
}
