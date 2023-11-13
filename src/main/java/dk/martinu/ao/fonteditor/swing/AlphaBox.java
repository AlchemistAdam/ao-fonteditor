package dk.martinu.ao.fonteditor.swing;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.JComponent;

import dk.martinu.ao.fonteditor.util.ImageUtil;

/**
 * Simple component that paints a colored square. Used to show a visual
 * representation of an alpha value applied to an opaque color which is then
 * painted over a backdrop.
 *
 * @author Adam Martinu
 * @since 1.0
 */
public class AlphaBox extends JComponent implements ComponentListener {

    /**
     * The current alpha value.
     *
     * @see #setAlpha(int)
     */
    protected int alpha = 255;
    /**
     * The last alpha value used by {@link #paintComponent(Graphics)}.
     */
    protected int paintedAlpha = 255;
    /**
     * The color to paint the box.
     *
     * @see #updateColor()
     */
    protected Color color = Color.BLACK;
    /**
     * The opaque color used by the font.
     *
     * @see #setFontColor(Color)
     */
    protected Color fontColor = Color.BLACK;
    /**
     * The backdrop image to draw behind the glyph image.
     */
    protected BufferedImage backdrop;

    /**
     * Constructs a new {@code AlphaBox} with an alpha value of {@code 255}.
     */
    public AlphaBox() {
        setPreferredSize(new Dimension(60, 29));
        setMaximumSize(new Dimension(60, 29));
        setMinimumSize(new Dimension(30, 29));
        addComponentListener(this);
    }

    /**
     * Unused.
     */
    @Override
    public void componentHidden(@NotNull final ComponentEvent event) {

    }

    /**
     * Unused.
     */
    @Override
    public void componentMoved(@NotNull final ComponentEvent event) {

    }

    /**
     * Adjusts the backdrop to fit the new size.
     */
    @Override
    public void componentResized(@NotNull final ComponentEvent event) {
        final Insets insets = getInsets();
        backdrop = ImageUtil.getBackdropImage(getWidth() - 2 - insets.left - insets.right,
                getHeight() - 2 - insets.top - insets.bottom);
    }

    /**
     * Unused.
     */
    @Override
    public void componentShown(@NotNull final ComponentEvent event) {

    }

    /**
     * Sets the current alpha value.
     *
     * @param alpha the new alpha value
     * @throws IllegalArgumentException if {@code alpha} is not in range
     *                                  {@code 0}-{@code 255} (inclusive)
     */
    @Contract(mutates = "this")
    public void setAlpha(final int alpha) {
        if (alpha < 0 || alpha > 255)
            throw new IllegalArgumentException("invalid alpha value");
        setAlphaImpl(alpha);
    }

    /**
     * Sets the font color. This will be the color of the box when alpha is set
     * to {@code 255}.
     *
     * @param fontColor the new font color
     * @throws NullPointerException if {@code fontColor} is {@code null}
     */
    @Contract(mutates = "this")
    public void setFontColor(@NotNull final Color fontColor) {
        this.fontColor = Objects.requireNonNull(fontColor, "fontColor is null");
        // invalidate alpha value to ensure color is updated in paint method
        paintedAlpha = -1;
        repaint();
    }

    /**
     * Paints the box.
     */
    @Override
    protected void paintComponent(@NotNull final Graphics g) {
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        // only compute the box color when alpha has changed
        if (alpha != paintedAlpha) {
            updateColor();
            paintedAlpha = alpha;
        }
        final Insets insets = getInsets();
        final int width = getWidth() - insets.left - insets.right;
        final int height = getHeight() - insets.top - insets.bottom;
        final Graphics g2d = g.create();
        g2d.setClip(insets.left, insets.top, width, height);
        g2d.translate(insets.left, insets.top);
        // draw backdrop
        g2d.drawImage(backdrop, 1, 1, null);
        // fill box color
        g2d.setColor(color);
        g2d.fillRect(1, 1, width - 2, height - 2);
        // draw contrasting outline
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, width - 1, height - 1);
        g2d.dispose();
    }

    /**
     * Sets the current alpha value. This method skips parameter checking and
     * is for internal use only.
     *
     * @param alpha the new alpha value
     */
    @Contract(mutates = "this")
    protected void setAlphaImpl(final int alpha) {
        // this method is called in quick succession by ChangeListeners on the
        // editor's alpha slider and spinner, do NOT update color here
        this.alpha = alpha;
        repaint();
    }

    /**
     * Updates the color.
     * <p>
     * <b>NOTE:</b> this method does not repaint the box. It is only used to
     * ensure the correct color is used before painting the component.
     */
    @Contract(mutates = "this")
    protected void updateColor() {
        if (alpha == 255)
            color = fontColor;
        else {
            final int[] rgb = Util.getRGB(fontColor, new int[3]);
            color = new Color(rgb[0], rgb[1], rgb[2], alpha);
        }
    }
}
