package dk.martinu.ao.fonteditor;

import org.jetbrains.annotations.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import dk.martinu.ao.client.text.Font;
import dk.martinu.ao.client.text.Glyph;

/**
 * Implementation of a mutable {@link Font}.
 *
 * @author Adam Martinu
 * @see #convertToFont()
 * @since 1.0
 */
public class MutableFont {

    /**
     * The file this font was read from or last saved to, or {@code null}.
     */
    @Nullable
    public File file;
    /**
     * The font name.
     */
    @NotNull
    public String name;
    /**
     * The line height in pixels.
     */
    public int height = 0;
    /**
     * List of all glyphs in the font.
     */
    public final ArrayList<MutableGlyph> glyphList = new ArrayList<>();
    /**
     * {@code true} if this font has been modified since it was created or
     * last saved, otherwise {@code false}.
     */
    public boolean isDirty = false;

    /**
     * Constructs a new mutable font with the specified name and line height.
     *
     * @param name   the font name
     * @param height the line height in pixels
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public MutableFont(@NotNull final String name, final int height) {
        file = null;
        this.name = Objects.requireNonNull(name, "name is null");
        this.height = height;
    }

    /**
     * Constructs a new mutable font that copies all fields from the specified
     * font.
     *
     * @param font the font to copy
     * @param file the file associated with {@code font}, or {@code null}
     * @throws NullPointerException if {@code font} is {@code null}
     */
    public MutableFont(@NotNull final Font font, @Nullable File file) {
        Objects.requireNonNull(font, "font is null");
        this.file = file;
        name = font.name;
        height = font.height;
        final int n = font.getGlyphCount();
        for (int i = 0; i < n; i++)
            glyphList.add(new MutableGlyph(font.getGlyph(i)));
    }

    /**
     * Converts this instance to an immutable {@link Font} object.
     *
     * @return a new immutable font
     */
    @Contract(value = "-> new", pure = true)
    @NotNull
    public Font convertToFont() {
        final Glyph[] glyphs = new Glyph[glyphList.size()];
        for (int i = 0; i < glyphList.size(); i++)
            glyphs[i] = glyphList.get(i).convertToGlyph();
        return new Font(name, height, glyphs);
    }

    /**
     * Returns {@code true} if the specified obj is equal to this font.
     *
     * @param obj the object to compare for equality
     * @return {@code true} if {@code obj} is equal to this font, otherwise
     * {@code false}
     */
    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(@Nullable final Object obj) {
        if (obj == this)
            return true;
        else if (obj instanceof MutableFont mFont) {
            return mFont.name.equals(name)
                    && mFont.height == height;
        }
        else
            return false;
    }
}
