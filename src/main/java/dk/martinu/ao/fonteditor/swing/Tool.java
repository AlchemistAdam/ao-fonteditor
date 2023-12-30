package dk.martinu.ao.fonteditor.swing;

import dk.martinu.ao.fonteditor.util.ImageUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Tool constants used when interacting with a glyph on a canvas.
 */
public enum Tool {

    /**
     * Eraser for removing pixels.
     */
    ERASER("res/images/cursor/eraser_cursor.png", 0, 9),
    /**
     * Move tool for moving the glyph image. This is the default tool.
     */
    MOVE("res/images/cursor/move_cursor.png", 6, 6),
    /**
     * Pencil for drawing freeform, one-pixel wide lines.
     */
    PENCIL("res/images/cursor/pencil_cursor.png", 0, 12),
    /**
     * Color picker for copying a color from the canvas.
     */
    PICKER("res/images/cursor/picker_cursor.png", 0, 12),
    /**
     * Selection tool for selecting parts of the canvas.
     */
    SELECT("res/images/cursor/select_cursor.png", 6, 6),
    /**
     * Magnifying glass for zooming in or out.
     */
    ZOOM("res/images/cursor/zoom_cursor.png", 0, 0);

    /**
     * The cursor of the tool.
     *
     * @see JComponent#setCursor(Cursor)
     */
    @Nullable
    public final Cursor cursor;

    /**
     * Constructs a tool with a cursor image at the specified path, and
     * using the specified coordinates as the hotspot for the cursor.
     *
     * @param path file path of the cursor image, or {@code null}
     * @param x    horizontal offset of the hotspot
     * @param y    vertical offset of the hotspot
     * @see Toolkit#createCustomCursor(Image, Point, String)
     */
    Tool(@Nullable String path, int x, int y) {
        BufferedImage img = null;
        if (path != null) {
            img = ImageUtil.createCursorImage(new File(path));
        }

        if (img != null) {
            cursor = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(x, y), name());
        }
        else {
            cursor = null;
        }
    }
}
