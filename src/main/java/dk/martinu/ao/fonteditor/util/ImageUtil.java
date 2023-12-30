package dk.martinu.ao.fonteditor.util;

import org.jetbrains.annotations.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Objects;

/**
 * Utility class for creating images.
 *
 * @author Adam Martinu
 * @since 1.0
 */
public class ImageUtil {

    /**
     * Tile image used as a source to create backdrop images for a requested
     * size.
     *
     * @see #getBackdropImage(int, int)
     */
    @NotNull
    private static final BufferedImage backdropSource = createBackdropSource();
    /**
     * Map of canonical mappings to backdrop images. Note that this is not
     * similar to a {@code WeakHashMap} because the reference weakness is
     * placed on the entry values, rather than the keys.
     */
    private static final HashMap<Integer, WeakReference<BufferedImage>> cache = new HashMap<>(64);

    /**
     * The width of a cell in pixels.
     */
    private static final int CELL_WIDTH = 5;
    /**
     * The height of a cell in pixels.
     */
    private static final int CELL_HEIGHT = 5;
    /**
     * The number of rows and columns of cells in the source backdrop image.
     *
     * @see #backdropSource
     */
    private static final int CELL_COUNT = 10;

    @Contract(pure = true)
    @Nullable
    public static BufferedImage createCursorImage(@NotNull File file) {
        Objects.requireNonNull(file, "file is null");
        BufferedImage source;
        try {
            source = ImageIO.read(file);
        }
        catch (IOException e) {
            Log.e("could not read cursor image {" + file.getName() + "}", e);
            return null;
        }
        Dimension cursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(source.getWidth(), source.getHeight());
        if (source.getWidth() == cursorSize.width && source.getHeight() == cursorSize.height) {
            return source;
        }
        else {
            BufferedImage img = new BufferedImage(cursorSize.width, cursorSize.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.drawImage(source, 0, 0, source.getWidth(), source.getHeight(), null);
            g.dispose();
            return img;
        }
    }

    /**
     * Returns a backdrop image of the requested size, painted as square cells
     * in a checkered pattern. The size of the backdrop may be larger than
     * requested. This is to ensure the backdrop will have an even cell count
     * both horizontally and vertically, and also not have incomplete cells.
     * This makes the backdrop suitable for painting repeatedly as tiles.
     * <p>
     * <b>NOTE:</b> created backdrop images are cached to be returned for
     * future calls, images returned by this method should therefore not be
     * altered.
     *
     * @param width  the requested width
     * @param height the requested height
     * @return a backdrop suited for repeated tile painting for the requested
     * size
     * @throws IllegalArgumentException if {@code width} or {@code height} is
     *                                  {@code < 1}
     */
    @Contract(pure = true)
    @NotNull
    public static BufferedImage getBackdropImage(int width, int height) {
        if (width < 1) {
            throw new IllegalArgumentException("width is less than 1");
        }
        if (height < 1) {
            throw new IllegalArgumentException("height is less than 1");
        }

        // adjust size to be a multiple of cell size
        if (width % CELL_WIDTH != 0) {
            width = CELL_WIDTH * (int) Math.ceil((double) width / (double) CELL_WIDTH);
        }
        if (height % CELL_HEIGHT != 0) {
            height = CELL_HEIGHT * (int) Math.ceil((double) height / (double) CELL_HEIGHT);
        }

        // adjust size so backdrop will not have an uneven cell count
        if ((width / CELL_WIDTH & 1) == 1) { width += CELL_WIDTH; }
        if ((height / CELL_HEIGHT & 1) == 1) { height += CELL_HEIGHT; }

        // check if cached image exists
        int key = (width + height) * height - width;
        WeakReference<BufferedImage> ref = cache.get(key);
        BufferedImage img = null;
        if (ref != null) {
            img = ref.get();
        }

        // create and store new backdrop image if cache was null or empty
        if (img == null) {
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            for (int x = 0; x < width; x += backdropSource.getWidth()) {
                for (int y = 0; y < height; y += backdropSource.getWidth()) {
                    g.drawImage(backdropSource, x, y, null);
                }
            }
            g.dispose();
            cache.put(key, new WeakReference<>(img));
        }

        return img;
    }

    /**
     * Helper method to initialize {@link #backdropSource}.
     */
    @Contract(value = "-> new", pure = true)
    @NotNull
    private static BufferedImage createBackdropSource() {
        int width = CELL_WIDTH * CELL_COUNT;
        int height = CELL_HEIGHT * CELL_COUNT;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.LIGHT_GRAY);
        for (int x = 0; x < CELL_COUNT; x++) {
            for (int y = (x & 1) == 0 ? 0 : 1; y < CELL_COUNT; y += 2) {
                g.fillRect(x * CELL_WIDTH, y * CELL_HEIGHT, CELL_WIDTH, CELL_HEIGHT);
            }
        }

        g.dispose();
        return img;
    }
}
