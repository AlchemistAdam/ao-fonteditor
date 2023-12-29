package dk.martinu.ao.fonteditor.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Handle to a mutable object reference. This class is useful when dealing
 * with non-final references inside lambda expressions.
 *
 * @param <T> The runtime type of the reference to store
 * @author Adam Martinu
 * @since 1.0
 */
public class Value<T> {

    /**
     * The current value, can be {@code null}.
     */
    @Nullable
    private T value;

    /**
     * Constructs a new {@code Value} instance with an initial value of
     * {@code null}.
     */
    public Value() {
        this(null);
    }

    /**
     * Constructs a new {@code Value} instance with the specified initial
     * value.
     */
    public Value(@Nullable T value) {
        this.value = value;
    }

    /**
     * Returns the current value.
     */
    @Contract(pure = true)
    @Nullable
    public T get() {
        return value;
    }

    /**
     * Sets the value.
     */
    @Contract(mutates = "this")
    public void set(@Nullable final T value) {
        this.value = value;
    }
}
