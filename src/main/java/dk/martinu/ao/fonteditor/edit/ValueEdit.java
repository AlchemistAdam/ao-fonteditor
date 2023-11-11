package dk.martinu.ao.fonteditor.edit;

import org.jetbrains.annotations.*;

import java.util.Objects;
import java.util.function.BiConsumer;

// DOC
public class ValueEdit<T, V> extends AbstractEdit<T> {

    protected final V oldValue;
    protected final V newValue;
    @NotNull
    protected final BiConsumer<T, V> function;

    public ValueEdit( final T parent,  final V oldValue,  final V newValue,
            @NotNull final BiConsumer<T, V> function) {
        super(parent);
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.function = Objects.requireNonNull(function, "function is null");
    }

    @Override
    public void redo() {
        function.accept(parent, newValue);
    }

    @Override
    public void undo() {
        function.accept(parent, oldValue);
    }
}
