package dk.martinu.ao.fonteditor.swing;

import dk.martinu.ao.fonteditor.MutableGlyph;
import dk.martinu.ao.fonteditor.edit.ValueEdit;
import dk.martinu.ao.fonteditor.util.ImageUtil;
import org.jetbrains.annotations.*;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

import static dk.martinu.ao.fonteditor.swing.EditorFrame.*;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

/**
 * A {@code JComponent} implementation of a canvas for displaying and editing a
 * {@link MutableGlyph}.
 *
 * @author Adam Martinu
 * @since 1.0
 */
// TODO find out if canvas could benefit from using a volatile (accelerated)
//  image and if it's even possible
// TODO canvas bounds and scaled image bounds are computed many times, could be
//  stored in reusable variable, and updated when canvas is resized, image is
//  dragged or zoomed
public class GlyphCanvas extends JComponent implements PropertyChangeListener {

    /**
     * Key constant for the {@code isDirty} state of the canvas.
     *
     * @see #setDirty(boolean)
     */
    public static final String PROPERTY_DIRTY = "dk.martinu.ao.fonteditor.swing.GlyphCanvas.DIRTY";
    /**
     * The maximum zoom value.
     *
     * @see #setZoom(int)
     */
    public static final int MAX_ZOOM = 40;
    /**
     * The minimum zoom value.
     *
     * @see #setZoom(int)
     */
    public static final int MIN_ZOOM = 1;

    /**
     * Default background color to paint behind the glyph image.
     *
     * @see #setBackground(Color)
     */
    private static final Color DEFAULT_BACKGROUND_COLOR = new Color(210, 210, 210);
    /**
     * Default border color to paint around the glyph image.
     *
     * @see #setBorderColor(Color)
     */
    private static final Color DEFAULT_BORDER_COLOR = new Color(155, 155, 155);
    /**
     * Constant for a blank/empty pixel, with all color components set to
     * {@code 0}. When pixels are erased from the canvas, their color
     * components will be set to this constant.
     */
    private static final int[] BLANK_PIXEL = new int[] {0, 0, 0, 0};

    /**
     * The glyph this data represents.
     */
    @NotNull
    final MutableGlyph glyph;
    /**
     * The owner of the canvas.
     */
    private final EditorFrame editor;
    /**
     * Current color value to use when editing the canvas.
     *
     * @see #setToolColor(Color)
     * @see EditorFrame#PROPERTY_FONT_COLOR
     */
    private final int[] rgba;
    /**
     * Handler for mouse events.
     */
    private final EventHandler handler = new EventHandler();
    /**
     * A rendered image of the glyph. The size is equal to the glyph size.
     */
    @NotNull
    private BufferedImage image;
    /**
     * The backdrop image to draw behind the glyph image.
     */
    @NotNull
    private BufferedImage backdrop;
    /**
     * The color of the border to draw around the image.
     */
    @Nullable
    private Color borderColor = DEFAULT_BORDER_COLOR;
    /**
     * The current tool for editing, the default value is {@code MOVE}.
     *
     * @see #setTool(Tool)
     */
    @NotNull
    private Tool tool = Tool.MOVE;
    /**
     * The horizontal position of the glyph image. This value is changed when
     * dragging the glyph image with the {@code MOVE} tool.
     */
    private int x;
    /**
     * The vertical position of the glyph images. This value is changed when
     * dragging the glyph image with the {@code MOVE} tool.
     */
    private int y;
    /**
     * The current level of zoom. The glyph image will be scaled by this
     * value when drawn on the canvas (for example x1, x2, x3, ...).
     */
    private int zoom = 1;
    /**
     * {@code true} if the glyph has been modified and saving should be
     * enabled, otherwise {@code false}.
     */
    private boolean isDirty = false;

    /**
     * Constructs a new glyph canvas.
     */
    public GlyphCanvas(@NotNull EditorFrame editor, @NotNull MutableGlyph glyph) {
        this.editor = Objects.requireNonNull(editor, "editor is null");
        this.glyph = Objects.requireNonNull(glyph, "glyph is null");
//        isDirty = glyph.isDirty;
        image = new BufferedImage(glyph.width, glyph.height, BufferedImage.TYPE_INT_ARGB);
        backdrop = ImageUtil.getBackdropImage(glyph.width, glyph.height);
        rgba = Arrays.copyOf(editor.getRGBA(), 4);
        renderGlyphToImage();
        setBackground(DEFAULT_BACKGROUND_COLOR);
        setOpaque(true);
        addMouseListener(handler);
        addMouseMotionListener(handler);
        addMouseWheelListener(handler);
        // notify canvas when tool or color changes in editor
        editor.addPropertyChangeListener(PROPERTY_TOOL_COLOR, this);
        editor.addPropertyChangeListener(PROPERTY_TOOL, this);
    }

    /**
     * Centers the glyph image on the canvas, using the current level of zoom,
     * and repaints the canvas.
     */
    public void centerImage() {
        this.x = getWidth() / 2 - glyph.width * zoom / 2;
        this.y = getHeight() / 2 - glyph.height * zoom / 2;
        repaint();
    }

    /**
     * Returns the border color, or {@code null} if the border is disabled.
     */
    @Contract(pure = true)
    @Nullable
    public Color getBorderColor() {
        return borderColor;
    }

    /**
     * Returns the glyph that is displayed.
     */
    @Contract(pure = true)
    @NotNull
    public MutableGlyph getGlyph() {
        return glyph;
    }

    /**
     * Returns the tool that is currently in use.
     */
    @Contract(pure = true)
    @NotNull
    public Tool getTool() {
        return tool;
    }

    /**
     * Returns {@code true} if the canvas image has been modified, otherwise
     * {@code false} is returned.
     */
    @Contract(pure = true)
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Draws the glyph on the canvas.
     */
    // TODO draw pixel grid when enabled
    @Override
    public void paintComponent(@NotNull Graphics g) {
        // honor opaque property
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        // half canvas size
        int hWidth = getWidth() / 2;
        int hHeight = getHeight() / 2;
        // scaled glyph image bounds
        int scaledX = (x - hWidth) * zoom + hWidth;
        int scaledY = (y - hHeight) * zoom + hHeight;
        int scaledWidth = image.getWidth() * zoom;
        int scaledHeight = image.getHeight() * zoom;
        // create graphics copy
        Graphics2D g2 = (Graphics2D) g.create();
        // intersect clip with image bounds
        g2.clipRect(scaledX - 1, scaledY - 1, scaledWidth + 2, scaledHeight + 2);
        // draw backdrop
        for (int y = 0; y < scaledHeight; y += backdrop.getHeight()) {
            for (int x = 0; x < scaledWidth; x += backdrop.getWidth()) {
                g2.drawImage(backdrop, x + scaledX, y + scaledY, backdrop.getWidth(), backdrop.getHeight(), null);
            }
        }
        // draw border if enabled
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRect(scaledX - 1, scaledY - 1, scaledWidth + 1, scaledHeight + 1);
        }
        // render glyph
        if (!glyph.isWhitespace) {
            g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(image, scaledX, scaledY, scaledWidth, scaledHeight, null);
        }
        g2.dispose();
    }

    // DOC propertyChange
    @Override
    public void propertyChange(@NotNull PropertyChangeEvent event) {
        switch (event.getPropertyName()) {
            case PROPERTY_CANVAS_BORDER_COLOR -> {
                borderColor = (Color) event.getNewValue();
                if (isVisible()) { repaint(); }
            }
            case PROPERTY_FONT_COLOR -> {
                Util.getRGB((Color) event.getNewValue(), rgba);
                if (!glyph.isWhitespace) {
                    renderGlyphToImage();

                    // TODO test if image always needs to be repainted, even if not visible
                    //  (fx when swapping between panes after changing the color)
                    if (isVisible()) { repaint(); }
                }
            }
            case PROPERTY_TOOL -> setTool((Tool) event.getNewValue());
            case PROPERTY_TOOL_COLOR ->
                    setToolColor((Color) event.getNewValue());
        }
    }

    /**
     * Sets the border color to the specified color. The colored border is
     * disabled if {@code color} is {@code null}.
     * <p>
     * <b>NOTE:</b> this method is not to be confused with
     * {@link #setBorder(Border)}. The colored border is only drawn around the
     * glyph image, not around the entire component.
     *
     * @param color the new border color, or {@code null}
     */
    @Contract(mutates = "this")
    public void setBorderColor(@Nullable Color color) {
        if (!Objects.equals(borderColor, color)) {
            borderColor = color;
            repaint();
        }
    }

    /**
     * Sets the dirty state of the canvas. This method notifies any property
     * change listeners bound to {@code PROPERTY_DIRTY} if the method call
     * resulted in a new dirty state.
     *
     * @param b the new dirty state value
     * @see #isDirty()
     */
    @Contract(mutates = "this")
    public void setDirty(boolean b) {
        if (b != isDirty) {
            firePropertyChange(PROPERTY_DIRTY, isDirty, isDirty = b);
        }
    }

    /**
     * Sets the current tool for editing. The default value is {@code MOVE}.
     *
     * @param tool the new tool
     * @throws NullPointerException if {@code tool} is {@code null}
     */
    @Contract(mutates = "this")
    public void setTool(@NotNull Tool tool) {
        this.tool = Objects.requireNonNull(tool, "tool is null");
        setCursor(tool.cursor);
        handler.reset();
    }

    /**
     * Sets the current tool color for editing.
     *
     * @param color the new color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    @Contract(mutates = "this")
    public void setToolColor(@NotNull Color color) {
        Util.getRGBA(Objects.requireNonNull(color, "color is null"), rgba);
    }

    /**
     * Sets the level of zoom when viewing the glyph image. The default value
     * is {@code MIN_ZOOM}. The zoom value cannot be set to a value lower than
     * {@code MIN_ZOOM} or greater than {@code MAX_ZOOM}.
     *
     * @param zoom level of zoom
     * @see #zoomIn()
     * @see #zoomOut()
     */
    @Contract(mutates = "this")
    public void setZoom(int zoom) {
        if (zoom > MAX_ZOOM) { zoom = MAX_ZOOM; }
        else if (zoom < MIN_ZOOM) { zoom = MIN_ZOOM; }
        if (zoom == this.zoom) { return; }
        this.zoom = zoom;
        repaint();
    }

    /**
     * Increments the current zoom level by {@code 1}.
     *
     * @see #setZoom(int)
     */
    @Contract(mutates = "this")
    public void zoomIn() {
        setZoom(zoom + 1);
    }

    /**
     * Decrements the current zoom level by {@code 1}.
     *
     * @see #setZoom(int)
     */
    @Contract(mutates = "this")
    public void zoomOut() {
        setZoom(zoom - 1);
    }

    /**
     * Updates the canvas image and backdrop. Call this method if the glyph
     * size has changed.
     * <p>
     * <b>NOTE:</b> calling this method will not schedule the canvas to be
     * repainted, and must be done by the caller after all changes to the
     * canvas have been made.
     */
    void updateImage() {
        BufferedImage image = new BufferedImage(glyph.width, glyph.height, BufferedImage.TYPE_INT_ARGB);
        int width = Math.min(glyph.width, this.image.getWidth());
        int height = Math.min(glyph.height, this.image.getHeight());
        image.getRaster().setDataElements(0, 0, width, height,
                this.image.getRaster().getDataElements(0, 0, width, height, null));
        this.image = image;
        backdrop = ImageUtil.getBackdropImage(glyph.width, glyph.height);
    }

    /**
     * Renders the glyph to the canvas image using the current
     * {@link #rgba color}. If the glyph is whitespace, then the image will
     * have all color components set to {@code 0}.
     * <p>
     * <b>NOTE:</b> calling this method will not schedule the canvas to be
     * repainted.
     */
    private void renderGlyphToImage() {
        WritableRaster raster = image.getRaster();
        if (!glyph.isWhitespace) {
            int[] rgba = Arrays.copyOf(this.rgba, 4);
            for (int y = 0; y < glyph.height; y++) {
                for (int x = 0; x < glyph.width; x++) {
                    rgba[3] = glyph.data[x + y * glyph.width];
                    raster.setPixel(x, y, rgba);
                }
            }
        }
        else {
            for (int y = 0; y < glyph.height; y++) {
                for (int x = 0; x < glyph.width; x++) {
                    raster.setPixel(x, y, BLANK_PIXEL);
                }
            }
        }
    }

    /**
     * Stores the canvas image alpha values in the glyph data array.
     */
    private void saveImageToData() {
        int width = glyph.width;
        int height = glyph.height;
        int len = width * height;
        int[] samples = image.getRaster().getSamples(0, 0, width, height, 3, new int[len]);
        for (int i = 0; i < len; i++) { glyph.data[i] = (byte) samples[i]; }
    }

    /**
     * Event listener that handles move, zoom and edit events.
     */
    // TODO implement selection
    // TODO pencil/eraser leaves gaps if mouse is moving too fast
    private final class EventHandler implements MouseListener, MouseMotionListener, MouseWheelListener {

        private static final int UPSCALE_FACTOR = 256;
        /**
         * The horizontal position of the glyph image before dragging.
         */
        private int startX = 0;
        /**
         * The Vertical position of the glyph image before dragging.
         */
        private int startY = 0;
        /**
         * The event that started dragging (or other tool operation), or
         * {@code null}.
         */
        @Nullable
        private MouseEvent startEvent = null;
        /**
         * Private reusable buffer for storing pixel coordinates.
         *
         * @see #getPixel(int, int, int[])
         */
        private final int[] pixelBuffer = new int[2];

        /**
         * Zooms in or out when using the zoom tool.
         */
        @Override
        public void mouseClicked(@NotNull MouseEvent event) {
            // TODO translate image after zoom so pixel beneath cursor remains in same position
            if (tool == Tool.ZOOM) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    zoomIn();
                }
                else if (SwingUtilities.isRightMouseButton(event)) {
                    zoomOut();
                }
            }
        }

        /**
         * Drags the glyph image across the canvas or changes the image,
         * depending on the currently selected tool.
         */
        @Override
        public void mouseDragged(@NotNull MouseEvent event) {
            if (startEvent == null) {
                return;
            }
            switch (tool) {
                case MOVE -> moveCanvas(event.getX(), event.getY());
                case PENCIL -> paintPixel(event.getX(), event.getY());
                case ERASER -> erasePixel(event.getX(), event.getY());
                case PICKER -> copyPixel(event.getX(), event.getY());
            }
        }

        /**
         * does nothing.
         */
        @Contract(pure = true)
        @Override
        public void mouseEntered(@NotNull MouseEvent event) { }

        /**
         * does nothing.
         */
        @Contract(pure = true)
        @Override
        public void mouseExited(@NotNull MouseEvent event) { }

        /**
         * does nothing.
         */
        @Contract(pure = true)
        @Override
        public void mouseMoved(@NotNull MouseEvent event) { }

        /**
         * Prepares the event handler for dragging.
         */
        @Contract(mutates = "this")
        @Override
        public void mousePressed(@NotNull MouseEvent event) {
            startX = x;
            startY = y;
            startEvent = event;
            // perform initial action for tool, if any
            switch (tool) {
                case PENCIL -> paintPixel(event.getX(), event.getY());
                case ERASER -> erasePixel(event.getX(), event.getY());
                case PICKER -> copyPixel(event.getX(), event.getY());
            }
        }

        /**
         * Resets dragging state.
         */
        @Contract(mutates = "this")
        @Override
        public void mouseReleased(@NotNull MouseEvent event) {
            startEvent = null;
        }

        /**
         * Zooms in or out.
         */
        @Override
        public void mouseWheelMoved(@NotNull MouseWheelEvent event) {
            // TODO translate image after zoom so pixel beneath cursor remains in same position
            int rotation = event.getWheelRotation();
            // scroll up
            if (rotation < 0) {
                zoomIn();
            }
            // scroll down
            else if (rotation > 0) {
                zoomOut();
            }
        }

        /**
         * Copies the alpha value of the pixel at the position beneath the
         * specified mouse coordinates on the current image.
         *
         * @param mouseX x coordinate of the mouse
         * @param mouseY y coordinate of the mouse
         */
        private void copyPixel(int mouseX, int mouseY) {
            int[] pixel = getPixel(mouseX, mouseY, pixelBuffer);
            if (pixel[0] != -1) {
                WritableRaster raster = image.getRaster();
                int alpha = raster.getSample(pixel[0], pixel[1], 3);
                if (alpha != rgba[3]) {
                    editor.setAlpha(alpha); // the editor will forward the new alpha to all canvases
                }
            }
        }

        /**
         * Erases a single pixel at the position beneath the specified mouse
         * coordinates on the current image.
         *
         * @param mouseX x coordinate of the mouse
         * @param mouseY y coordinate of the mouse
         */
        private void erasePixel(int mouseX, int mouseY) {
            int[] pixel = getPixel(mouseX, mouseY, pixelBuffer);
            if (pixel[0] != -1) {
                WritableRaster raster = image.getRaster();
                raster.setPixel(pixel[0], pixel[1], new int[4]);
                repaint();
                glyph.isDirty = true;
                if (!isDirty) {
                    setDirty(true);
                }
            }
        }

        // DOC getDiagonalPixels
        // TODO implement getDiagonalPixels
        @Contract(pure = true)
        private int[] getDiagonalPixels(int x0, int y0, int x1, int y1) {
            // half canvas size
            int hWidth = getWidth() / 2;
            int hHeight = getHeight() / 2;
            // scaled glyph image bounds
            int minX = (x - hWidth) * zoom + hWidth;
            int scaledY = (y - hHeight) * zoom + hHeight;
            int scaledWidth = image.getWidth() * zoom;
            int scaledHeight = image.getHeight() * zoom;
            int maxX = minX + scaledWidth - 1;
            // initial Point-in-Rect checks
            boolean x0inBounds = x0 >= minX && x0 <= maxX;
            boolean y0inBounds = y0 >= scaledY && y0 < scaledY + scaledHeight;
            boolean x1inBounds = x1 >= minX && x1 <= maxX;
            boolean y1inBounds = y1 >= scaledY && y1 < scaledY + scaledHeight;

            // linear function constants
            double slope = (double) (y1 - y0) / (double) (x1 - x0);
            double intercept = y0 - slope * x0;

            // check if first point is in image bounds
            if (x0inBounds && y0inBounds) {
                // adjust x1 and y1 to be inside image bounds
                if (!x1inBounds) {
                    if (x1 < minX) { x1 = minX; }
                    else { x1 = maxX; }
                    x1inBounds = true;
                    // TEST use ceil or floor on y?
                    y1 = (int) (slope * x1 + intercept);
                    y1inBounds = y1 >= scaledY && y1 < scaledY + scaledHeight;
                }
                if (!y1inBounds) {
                    if (y1 < scaledY) { y1 = scaledY; }
                    else { y1 = scaledY + scaledHeight - 1; }
                    // TEST use ceil or floor on x?
                    x1 = (int) ((y1 - intercept) / slope);
                    x1inBounds = x1 >= minX && x1 <= maxX;
                }
            }
            // check if second point is in image bounds
            else if (x1inBounds && y1inBounds) {
                // adjust x0 and y0 to be inside image bounds
                if (!x0inBounds) {
                    if (x0 < minX) { x0 = minX; }
                    else { x0 = maxX; }
                    x0inBounds = true;
                    // TEST use ceil or floor on y?
                    y0 = (int) (slope * x0 + intercept);
                    y0inBounds = y0 >= scaledY && y0 < scaledY + scaledHeight;
                }
                if (!y0inBounds) {
                    if (y0 < scaledY) { y0 = scaledY; }
                    else { y0 = scaledY + scaledHeight - 1; }
                    // TEST use ceil or floor on x?
                    x0 = (int) ((y0 - intercept) / slope);
                    x0inBounds = x0 >= minX && x0 <= maxX;
                }
            }
            // determine if line segment intersects image bounds
            else {
                // TODO determine if line segment intersects image bounds
            }

            if (x0inBounds && y0inBounds && x1inBounds && y1inBounds) {
                // ensure line segment goes from left to right
                if (x1 < x0) {
                    int temp = x0;
                    x0 = x1;
                    x1 = temp;
                    temp = y0;
                    y0 = y1;
                    y1 = temp;
                }
                // adjust max x
                maxX = Math.min(maxX, x1);

                // TODO need to check if a (slope) is an integer,
                //  resulting in pixel-perfect lines

                // upscale all values to improve precision when determining if
                // the line segment passes through a pixel
//                minX *= UPSCALE_FACTOR;
//                scaledY *= UPSCALE_FACTOR;
//                scaledWidth *= UPSCALE_FACTOR;
//                scaledHeight *= UPSCALE_FACTOR;
                x0 *= UPSCALE_FACTOR;
                y0 *= UPSCALE_FACTOR;
                x1 *= UPSCALE_FACTOR;
                y1 *= UPSCALE_FACTOR;
                // used to check if the line segment passes through the border
                // between neighboring pixels
//                 int pxs = zoom * UPSCALE_FACTOR;
//                 int pxs_2 = pxs / 2;

                // TODO fill and return pixels array
                // line segment is ascending
                if (slope > 0) {
                    if (slope < 1) {
                        /*
                        Delta between x values when the line segment crosses
                        the vertical pixel border.
                        yn = y0 + pixelSize/2.
                        xn = (yn - intercept) / slope
                        dx = xn - x0
                         */
                        // TODO maybe dx needs to be incremented by half a pixel (like yn)
                        double dx = (y0 + (zoom * UPSCALE_FACTOR / 2d) - intercept * UPSCALE_FACTOR) / slope - x0;
                        // starting x value as a floating point to retain
                        // partial x value increments
                        double xf = x0;
                        // starting pixel x value for row
                        int px0;
                        // last pixel x value for row
                        int px1 = x0;
                        // starting pixel y value (row)
                        int py = y0;

                        while (py <= y1) {
                            // advance x value by delta
                            xf += dx;
                            // previous last pixel is new first pixel
                            px0 = px1;
                            // get new last pixel
                            px1 = Math.min((int) Math.floor((xf) / UPSCALE_FACTOR), maxX);
                            for (int i = px0; i <= px1; i++) {

                            }
                            py++;
                        }
                    }
                    else {

                    }
                }
                // line segment is descending
                else {

                }
            }

            // line segment does not intersect or is outside image bounds
            return null;
        }

        /**
         * Returns an array that contains the pixel coordinates for a
         * horizontal line segment with the specified mouse coordinates if it
         * intersects with the image bounds. Otherwise, {@code null} is
         * returned. The array length is a multiple of 3 (not 2) to make space
         * for storing the color components of each pixel.
         *
         * @param mouseY the vertical mouse coordinate
         * @param x0     the first horizontal mouse coordinate
         * @param x1     the second horizontal mouse coordinate
         * @return an array containing the pixel coordinates, or {@code null}
         */
        @Contract(pure = true)
        private int[] getHorizontalPixels(int mouseY, int x0, int x1) {
            // half canvas size
            int hWidth = getWidth() / 2;
            int hHeight = getHeight() / 2;
            // scaled glyph image bounds
            int scaledX = (x - hWidth) * zoom + hWidth;
            int scaledY = (y - hHeight) * zoom + hHeight;
            int scaledWidth = image.getWidth() * zoom;
            int scaledHeight = image.getHeight() * zoom;
            // ensure line segment is within vertical bounds
            if (mouseY >= scaledY && mouseY < scaledY + scaledHeight) {
                // ensure line segment goes from left to right
                if (x1 < x0) {
                    int temp = x0;
                    x0 = x1;
                    x1 = temp;
                }
                // ensure line segment is within horizontal bounds
                if (x1 >= scaledX && x0 < scaledX + scaledWidth) {
                    // adjust x0 and x1 to be inside image bounds
                    if (x0 < scaledX) { x0 = scaledX; }
                    if (x1 >= scaledX + scaledWidth) {
                        x1 = scaledX + scaledWidth - 1;
                    }
                    // ensure line segment spans more than a single pixel
                    int p0 = x0 / zoom;
                    int p1 = x1 / zoom;
                    if (p0 == p1) { return null; }
                    // array length is multiplied with 3 because it will be
                    // reused later to include the pixels' color components
                    int len = (p1 - p0) * 3;
                    // fill and return pixel coordinates
                    int[] pixels = new int[len];
                    int y = (mouseY - scaledY) / zoom;
                    for (int i = 0, x = (x0 - scaledX) / zoom + 1; i < len; i += 3, x++) {
                        pixels[i] = x;
                        pixels[i + 1] = y;
                    }
                    return pixels;
                }
            }

            // line segment is outside image bounds
            return null;
        }

        /**
         * Stores the pixel coordinates for the pixel beneath the specified
         * mouse position in the {@code pixel} array and returns it. If the
         * mouse position is outside the image, then the element at index
         * {@code 0} is set to {@code -1}.
         *
         * @param mouseX x coordinate of the mouse
         * @param mouseY y coordinate of the mouse
         * @param pixel  the array to store the coordinates in
         * @return the pixel coordinates
         */
        @Contract(value = "_, _, _ -> param3", mutates = "param3")
        private int[] getPixel(int mouseX, int mouseY, int[] pixel) {
            // half canvas size
            int hWidth = getWidth() / 2;
            int hHeight = getHeight() / 2;
            // scaled glyph image bounds
            int scaledX = (x - hWidth) * zoom + hWidth;
            int scaledY = (y - hHeight) * zoom + hHeight;
            int scaledWidth = image.getWidth() * zoom;
            int scaledHeight = image.getHeight() * zoom;

            if (mouseX >= scaledX
                    && mouseX < scaledX + scaledWidth
                    && mouseY >= scaledY
                    && mouseY < scaledY + scaledHeight) {
                pixel[0] = (mouseX - scaledX) / zoom;
                pixel[1] = (mouseY - scaledY) / zoom;
            }
            else { pixel[0] = -1; }
            return pixel;
        }

        /**
         * Returns an array that contains the pixel coordinates for a line
         * segment with the specified mouse coordinates if it intersects with
         * the image bounds. Otherwise, {@code null} is returned. The array
         * length is a multiple of 3 (not 2) to make space for storing the
         * color components of each pixel.
         *
         * @param x0 the first horizontal mouse coordinate
         * @param x1 the second horizontal mouse coordinate
         * @param y0 the first vertical mouse coordinate
         * @param y1 the second vertical mouse coordinate
         * @return an array containing the pixel coordinates, or {@code null}
         */
        @Contract(pure = true)
        private int[] getPixels(int x0, int y0, int x1, int y1) {
            if (x0 == x1) { return getVerticalPixels(x0, y0, y1); }
            else if (y0 == y1) { return getHorizontalPixels(y0, x0, x1); }
            else { return getDiagonalPixels(x0, y0, x1, y1); }
        }

        /**
         * Returns an array that contains the pixel coordinates for a vertical
         * line segment with the specified mouse coordinates if it intersects
         * with the image bounds. Otherwise, {@code null} is returned. The
         * array length is a multiple of 3 (not 2) to make space for storing
         * the color components of each pixel.
         *
         * @param mouseX the horizontal mouse coordinate
         * @param y0     the first vertical mouse coordinate
         * @param y1     the second vertical mouse coordinate
         * @return an array containing the pixel coordinates, or {@code null}
         */
        @Contract(pure = true)
        private int[] getVerticalPixels(int mouseX, int y0, int y1) {
            // half canvas size
            int hWidth = getWidth() / 2;
            int hHeight = getHeight() / 2;
            // scaled glyph image bounds
            int scaledX = (x - hWidth) * zoom + hWidth;
            int scaledY = (y - hHeight) * zoom + hHeight;
            int scaledWidth = image.getWidth() * zoom;
            int scaledHeight = image.getHeight() * zoom;
            // ensure line segment is within horizontal bounds
            if (mouseX >= scaledX && mouseX < scaledX + scaledWidth) {
                // ensure line segment goes from top to bottom (vertical axis is inverted)
                if (y1 < y0) {
                    int temp = y0;
                    y0 = y1;
                    y1 = temp;
                }
                // ensure line segment is within vertical bounds
                if (y1 >= scaledY && y0 < scaledY + scaledHeight) {
                    // adjust y0 and y1 to be inside image bounds
                    if (y0 < scaledY) {
                        y0 = scaledY;
                    }
                    if (y1 >= scaledY + scaledHeight) {
                        y1 = scaledY + scaledHeight - 1;
                    }
                    // ensure line segment spans more than a single pixel
                    int p0 = y0 / zoom;
                    int p1 = y1 / zoom;
                    if (p0 == p1) {
                        return null;
                    }
                    // array length is multiplied with 3 because it will be
                    // reused later to include the pixels' color components
                    int[] pixels = new int[(p1 - p0) * 3];
                    // fill and return pixel coordinates
                    int x = (mouseX - scaledX) / zoom;
                    for (int i = 0, y = (y0 - scaledY) / zoom + 1; i < pixels.length; i += 3, y++) {
                        pixels[i] = x;
                        pixels[i + 1] = y;
                    }
                    return pixels;
                }
            }
            // line segment is outside image bounds
            return null;
        }

        private void moveCanvas(int eventX, int eventY) {
            assert startEvent != null;
            // determine move delta
            int newX = startX;
            int newY = startY;
            if (zoom == 1) {
                newX += eventX - startEvent.getX();
                newY += eventY - startEvent.getY();
            }
            else {
                newX += Math.round((float) (eventX - startEvent.getX()) / (float) zoom);
                newY += Math.round((float) (eventY - startEvent.getY()) / (float) zoom);
            }
            // ensure newX stays within bounds
            int halfWidth = getWidth() / 2;
            int scaledWidth = image.getWidth() * zoom;
            if (scaledWidth >= getWidth()) {
                if (newX + image.getWidth() < halfWidth) {
                    newX = halfWidth - image.getWidth();
                }
                else if (newX > halfWidth) {
                    newX = halfWidth;
                }
            }
            else {
                int scaledCenterX = (newX - halfWidth) * zoom + halfWidth + scaledWidth / 2;
                if (scaledCenterX < 0) {
                    newX = (-scaledWidth / 2 - halfWidth) / zoom + halfWidth;
                }
                else if (scaledCenterX > getWidth()) {
                    newX = (halfWidth - scaledWidth / 2) / zoom + halfWidth;
                }
            }
            // ensure newY stays within bounds
            int halfHeight = getHeight() / 2;
            int scaledHeight = image.getHeight() * zoom;
            if (scaledHeight >= getHeight()) {
                if (newY + image.getHeight() < halfHeight) {
                    newY = halfHeight - image.getHeight();
                }
                else if (newY > halfHeight) {
                    newY = halfHeight;
                }
            }
            else {
                int scaledCenterY = (newY - halfHeight) * zoom + halfHeight + scaledHeight / 2;
                if (scaledCenterY < 0) {
                    newY = (-scaledHeight / 2 - halfHeight) / zoom + halfHeight;
                }
                else if (scaledCenterY > getHeight()) {
                    newY = (halfHeight - scaledHeight / 2) / zoom + halfHeight;
                }
            }
            // update image position
            x = newX;
            y = newY;
            repaint();
        }

        /**
         * Paints a single pixel at the position beneath the specified mouse
         * coordinates on the current image using the current color of the
         * canvas.
         *
         * @param mouseX x coordinate of the mouse
         * @param mouseY y coordinate of the mouse
         */
        private void paintPixel(int mouseX, int mouseY) {
            int[] pixel = getPixel(mouseX, mouseY, pixelBuffer);
            if (pixel[0] != -1) {
                // FIX
//                 int oldColor
                WritableRaster raster = image.getRaster();
                raster.setPixel(pixel[0], pixel[1], rgba);
                repaint();
//                glyph.isDirty = true;
//                if (!isDirty)
//                    setDirty(true);
            }
        }

        /**
         * Resets event state.
         */
        @Contract(mutates = "this")
        private void reset() {
            startEvent = null;
        }
    }

    // TODO implement Recycle API
    private static class RasterEdit extends ValueEdit<WritableRaster, int[]> implements BiConsumer<WritableRaster, int[]> {

        public RasterEdit(@NotNull WritableRaster parent, int[] rgba, int[] pixels) {
            // FIX
            super(parent, rgba, rgba, (raster, value) -> {
                for (int i = 0; i < pixels.length; i += 2) {
                    raster.setPixel(pixels[i], pixels[i + 1], value);
                }
            });
//            this.oldColor = oldColor;
//            this.newColor = newColor;
            /*, oldValue, newValue, (raster, value) -> {
                for (int i = 0; i < pixels.length; i+=2)
                    raster.setPixel(pixels[i], pixels[i + 1], value);
            });*/
            Objects.requireNonNull(pixels, "pixels array is null");
        }

        @Override
        public void accept(WritableRaster writableRaster, int[] ints) {

        }

        @Override
        public void redo() {
//            accept(parent(), );
        }

        @Override
        public void undo() {

        }
    }
}
