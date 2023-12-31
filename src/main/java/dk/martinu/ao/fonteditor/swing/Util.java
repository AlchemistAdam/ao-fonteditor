package dk.martinu.ao.fonteditor.swing;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Objects;

public class Util {

    @Contract(value = "null -> fail", pure = true)
    public static int getColor(final int[] rgba) {
        return rgba[0] << 16 | rgba[1] << 8 | rgba[2] | rgba[3] << 24;
    }

    @Contract(mutates = "param2", value = "_, _ -> param2")
    public static int[] getRGB(@NotNull final Color color, final int[] array) {
        final int argb = color.getRGB();
        //@formatter:off
        array[0] = argb >> 16 & 0xFF;
        array[1] = argb >> 8  & 0xFF;
        array[2] = argb       & 0xFF;
        //@formatter:on
        return array;
    }

    @Contract(mutates = "param2", value = "_, _ -> param2")
    public static int[] getRGBA(@NotNull final Color color, final int[] array) {
        final int argb = color.getRGB();
        //@formatter:off
        array[0] = argb >> 16 & 0xFF;
        array[1] = argb >> 8  & 0xFF;
        array[2] = argb       & 0xFF;
        array[3] = argb >> 24 & 0xFF;
        //@formatter:on
        return array;
    }

    @Contract(value = "_, _ -> new", pure = true)
    @NotNull
    public static Dimension union(@NotNull final Dimension d0, @NotNull final Dimension d1) throws NullPointerException {
        Objects.requireNonNull(d0, "d0 is null");
        Objects.requireNonNull(d1, "d1 is null");
        return new Dimension(Math.max(d0.width, d1.width), Math.max(d0.height, d1.height));
    }
}
