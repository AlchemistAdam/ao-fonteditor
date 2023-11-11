package dk.martinu.ao.fonteditor.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Utility class for creating backdrop images. To get a backdrop image, call
 * {@link #getBackdrop(int, int)}.
 *
 * @author Adam Martinu
 * @since 1.0
 */
public class Backdrop {

    /**
     * Tile image used as a source to create backdrop images for a requested
     * size.
     *
     * @see #getBackdrop(int, int)
     */
    @NotNull
    private static final BufferedImage backdropSource;
    /**
     * Map of canonicalized mappings to backdrop images. Note that this is not
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

    // draws the gray-white checkered backdrop tile source image
    static {
        final int width = CELL_WIDTH * CELL_COUNT;
        final int height = CELL_HEIGHT * CELL_COUNT;
        backdropSource = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = backdropSource.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.LIGHT_GRAY);
        for (int x = 0; x < CELL_COUNT; x++)
            for (int y = (x & 1) == 0 ? 0 : 1; y < CELL_COUNT; y += 2)
                g.fillRect(x * CELL_WIDTH, y * CELL_HEIGHT, CELL_WIDTH, CELL_HEIGHT);

        g.dispose();
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
    public static BufferedImage getBackdrop(int width, int height) {
        if (width < 1)
            throw new IllegalArgumentException("width is less than 1");
        if (height < 1)
            throw new IllegalArgumentException("height is less than 1");

        // adjust size to be a multiple of cell size
        if (width % CELL_WIDTH != 0)
            width = CELL_WIDTH * (int) Math.ceil((double) width / (double) CELL_WIDTH);
        if (height % CELL_HEIGHT != 0)
            height = CELL_HEIGHT * (int) Math.ceil((double) height / (double) CELL_HEIGHT);
        // adjust size so backdrop will not have odd cell count
        if ((width / CELL_WIDTH & 1) == 1)
            width += CELL_WIDTH;
        if ((height / CELL_HEIGHT & 1) == 1)
            height += CELL_HEIGHT;

        final int hash = (width + height) * height - width;
        final WeakReference<BufferedImage> ref = cache.get(hash);
        BufferedImage img = null;
        if (ref != null)
            img = ref.get();
        if (img == null) {
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = img.createGraphics();
            for (int x = 0; x < width; x += backdropSource.getWidth())
                for (int y = 0; y < height; y += backdropSource.getWidth())
                    g.drawImage(backdropSource, x, y, null);
            g.dispose();
            cache.put(hash, new WeakReference<>(img));
        }
        return img;
    }
}
