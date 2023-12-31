package dk.martinu.ao.fonteditor;

import dk.martinu.ao.client.text.Glyph;
import dk.martinu.ao.fonteditor.edit.EditQueue;
import org.jetbrains.annotations.*;

import java.awt.image.BufferedImage;
import java.util.*;

import static java.awt.image.BufferedImage.*;

/**
 * Implementation of a mutable {@link Glyph}.
 *
 * @author Adam Martinu
 * @see #convertToGlyph()
 * @since 1.0
 */
public class MutableGlyph {

    /**
     * Returns a name for the specified character.
     */
    @Contract(pure = true)
    @NotNull
    private static String createName(char c) {
        String value = Character.isISOControl(c) || Character.isWhitespace(c) ?
                "0x" + Integer.toHexString(c).toUpperCase(Locale.ROOT) :
                String.valueOf(c);
        return value + " <" + Character.getName(c) + ">";
    }

    /**
     * The character that this glyph represents.
     */
    public char value;
    /**
     * Width of the glyph in pixels.
     */
    public int width;
    /**
     * Height of the glyph in pixels.
     */
    public int height;
    /**
     * {@code true} if this glyph represents whitespace, otherwise
     * {@code false}.
     */
    public boolean isWhitespace;
    /**
     * Vertical offset of the glyph in pixels. When drawing the glyph, this is
     * the y-position of the glyph relative to the top of the line.
     */
    public int offsetY;
    /**
     * Horizontal offsets between the glyph and other preceding glyphs.
     */
    public int[] offsetX;
    /**
     * The image data (alpha values) of the glyph.
     */
    public byte[] data;
    /**
     * Flag set to {@code true} if changes has been made to this glyph since
     * the last time the font was saved, otherwise {@code false}.
     */
    public boolean isDirty = false;
    /**
     * A string representation of the glyph. This name is stored only for the
     * purpose of displaying glyphs as text in a user interface; it is not
     * saved to the font file.
     * <p>
     * The representation of a glyph is equal to
     * <pre>
     *     value + " &lt;" + name + "&gt;"
     * </pre>
     * where <i>value</i> is equal to the value of the glyph, unless the value
     * is an ISO control character or whitespace, in which case <i>value</i> is
     * equal to the numerical value in base 16 (hexadecimal) and prefixed with
     * {@code 0x}. <i>name</i> is equal to the Unicode name of the value.
     *
     * @see Character#isISOControl(char)
     * @see Character#isWhitespace(char)
     * @see Character#getName(int)
     */
    @NotNull
    public final String name;
    /**
     * The edit queue for storing mutations made to this glyph.
     */
    public final EditQueue<MutableGlyph> editQueue = new EditQueue<>();

    /**
     * Constructs a new mutable glyph that copies all fields from the specified
     * glyph.
     *
     * @param glyph the glyph to copy
     */
    public MutableGlyph(@NotNull Glyph glyph) {
        this(glyph.value, glyph.width, glyph.height, glyph.isWhitespace, glyph.offsetY,
                glyph.offsetX != null ? glyph.offsetX : new int[0],
                glyph.data != null ? glyph.data : new byte[glyph.width * glyph.height]);
    }

    /**
     * Constructs a new empty mutable glyph with the specified properties.
     *
     * @param value        the character value
     * @param width        the width
     * @param height       the height
     * @param isWhitespace {@code true} if the glyph is whitespace, otherwise
     *                     {@code false}
     * @param offsetY      the vertical offset
     */
    public MutableGlyph(char value, int width, int height, boolean isWhitespace, int offsetY) {
        this(value, width, height, isWhitespace, offsetY, new int[0], new byte[width * height]);
    }

    /**
     * Constructs a new mutable glyph with properties imported from the
     * specified image.
     *
     * @param image the image to import as a glyph
     * @throws NullPointerException if {@code image} is {@code null}
     */
    public MutableGlyph(@NotNull BufferedImage image) {
        Objects.requireNonNull(image, "image is null");
        value = 'A';
        width = image.getWidth();
        height = image.getHeight();
        isWhitespace = false;
        offsetY = 0;
        offsetX = new int[0];

        int len = width * height;
        data = new byte[len];
        switch (image.getType()) {
            // 4 components, rgb + alpha
            // transfer alpha samples directly into glyph data
            case TYPE_INT_ARGB,
                    TYPE_4BYTE_ABGR -> {
                int[] samples = new int[len];
                image.getRaster().getSamples(0, 0, width, height, 3, samples);
                for (int i = 0; i < len; i++) {
                    System.out.println("sample value: " + samples[i]);
                    data[i] = (byte) samples[i];
                }
            }
            // 3 components, no alpha
            // compute alpha as average distance from white (#FFFFFF) to pixel rgb
            case TYPE_INT_RGB,
                    TYPE_INT_BGR,
                    TYPE_3BYTE_BGR -> {
                int[] pixels = new int[len * 3];
                image.getRaster().getPixels(0, 0, width, height, pixels);
                for (int i = 0, pi; i < len; i++) {
                    // pixel index
                    pi = i * 3;
                    int rgb = pixels[pi] + pixels[pi + 1] + pixels[pi + 2];
                    data[i] = (byte) ((765 - rgb) / 3);
                }
            }
            // TESTME 4 components, pre-multiplied alpha
            // same process as 3 component images, but ignore alpha channel
            case TYPE_INT_ARGB_PRE,
                    TYPE_4BYTE_ABGR_PRE -> {
                int[] pixels = new int[len * 4];
                image.getRaster().getPixels(0, 0, width, height, pixels);
                for (int i = 0, pi; i < len; i++) {
                    // pixel index
                    pi = i * 4;
                    int rgb = pixels[pi] + pixels[pi + 1] + pixels[pi + 2];
                    data[i] = (byte) ((765 - rgb) / 3);
                }
            }
            // TODO implement solutions for remaining image types
            default ->
                    throw new IllegalArgumentException("unsupported image type {" + image.getType() + "}");
        }

        name = createName(value);
    }

    /**
     * Constructs a new mutable glyph with the specified properties.
     *
     * @param value        the character value
     * @param width        the width
     * @param height       the height
     * @param isWhitespace {@code true} if the glyph is whitespace, otherwise
     *                     {@code false}
     * @param offsetY      the vertical offset
     * @param offsetX      the horizontal offsets
     * @param data         the alpha data array
     */
    private MutableGlyph(
            char value,
            int width,
            int height,
            boolean isWhitespace,
            int offsetY,
            int[] offsetX,
            byte[] data) {
        this.value = value;
        this.width = width;
        this.height = height;
        this.isWhitespace = isWhitespace;
        this.offsetY = offsetY;
        this.offsetX = Objects.requireNonNull(offsetX, "offsetX is null");
        this.data = Objects.requireNonNull(data, "data is null");
        name = createName(value);
    }

    /**
     * Converts this instance to an immutable {@link Glyph} object.
     *
     * @return a new immutable glyph
     */
    @Contract(value = "-> new", pure = true)
    @NotNull
    public Glyph convertToGlyph() {
        return new Glyph(isWhitespace, width, height, value, offsetY, offsetX, data);
    }

    /**
     * Returns {@code true} if the specified obj is equal to this glyph.
     *
     * @param obj the object to compare for equality
     * @return {@code true} if {@code obj} is equal to this glyph, otherwise
     * {@code false}
     */
    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        else if (obj instanceof MutableGlyph mGlyph) {
            return mGlyph.isWhitespace == isWhitespace
                    && mGlyph.width == width
                    && mGlyph.height == height
                    && mGlyph.value == value
                    && mGlyph.offsetY == offsetY
                    && Arrays.equals(mGlyph.offsetX, offsetX)
                    && Arrays.equals(mGlyph.data, data);
        }
        else {
            return false;
        }
    }
}
