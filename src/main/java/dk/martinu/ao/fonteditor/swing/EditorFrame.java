package dk.martinu.ao.fonteditor.swing;

import dk.martinu.ao.client.text.Font;
import dk.martinu.ao.client.text.FontCodec;
import dk.martinu.ao.fonteditor.MutableFont;
import dk.martinu.ao.fonteditor.MutableGlyph;
import dk.martinu.ao.fonteditor.swing.EditorWizard.Option;
import dk.martinu.ao.fonteditor.util.Log;
import dk.martinu.kofi.*;
import dk.martinu.kofi.codecs.KofiCodec;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.*;

import static dk.martinu.ao.fonteditor.swing.EditorWizard.Option.*;
import static dk.martinu.ao.fonteditor.swing.GlyphCanvas.PROPERTY_DIRTY;
import static dk.martinu.ao.fonteditor.swing.Tool.*;
import static java.awt.BorderLayout.*;
import static java.awt.Font.MONOSPACED;
import static java.awt.Font.PLAIN;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;

/**
 * {@code JFrame} implementation that provides a GUI for displaying and editing
 * a {@link MutableFont}.
 *
 * @author Adam Martinu
 * @since 1.0
 */
// DOC constants
// TODO remove isDirty from canvas, use glyph only
public class EditorFrame extends JFrame implements PropertyChangeListener {

    public static final String ACTION_CLOSE_FILE = "ACTION_CLOSE_FILE";
    public static final String ACTION_DELETE_GLYPHS = "ACTION_DELETE_GLYPHS";
    public static final String ACTION_EDIT_FONT_PROPERTIES = "ACTION_EDIT_FONT_PROPERTIES";
    public static final String ACTION_EDIT_GLYPH = "ACTION_EDIT_GLYPH";
    public static final String ACTION_EDIT_GLYPH_PROPERTIES = "ACTION_EDIT_GLYPH_PROPERTIES";
    public static final String ACTION_EDIT_HORIZONTAL_OFFSETS = "ACTION_EDIT_HORIZONTAL_OFFSETS";
    public static final String ACTION_EXIT = "ACTION_EXIT";
    public static final String ACTION_IMPORT_GLYPH = "ACTION_IMPORT_GLYPH";
    public static final String ACTION_MOVE_DOWN = "ACTION_MOVE_DOWN";
    public static final String ACTION_MOVE_TO = "ACTION_MOVE_TO";
    public static final String ACTION_MOVE_TO_BOTTOM = "ACTION_MOVE_TO_BOTTOM";
    public static final String ACTION_MOVE_TO_TOP = "ACTION_MOVE_TO_TOP";
    public static final String ACTION_MOVE_UP = "ACTION_MOVE_UP";
    public static final String ACTION_NEW_FONT = "ACTION_NEW_FONT";
    public static final String ACTION_NEW_GLYPH = "ACTION_NEW_GLYPH";
    public static final String ACTION_OPEN_FILE = "ACTION_OPEN_FILE";
    public static final String ACTION_SAVE_FILE = "ACTION_SAVE_FILE";
    public static final String ACTION_SAVE_AS = "ACTION_SAVE_AS";
    public static final String ACTION_SETTINGS = "ACTION_SETTINGS";
    public static final String ACTION_TOOL_ERASER = "ACTION_TOOL_ERASER";
    public static final String ACTION_TOOL_MOVE = "ACTION_TOOL_MOVE";
    public static final String ACTION_TOOL_PENCIL = "ACTION_TOOL_PENCIL";
    public static final String ACTION_TOOL_PICKER = "ACTION_TOOL_PICKER";
    public static final String ACTION_TOOL_SELECT = "ACTION_TOOL_SELECT";
    public static final String ACTION_TOOL_ZOOM = "ACTION_TOOL_ZOOM";

    public static final String CK_ALPHA_BOX = "CK_ALPHA_BOX";
    public static final String CK_ALPHA_SLIDER = "alphaSlider";
    public static final String CK_ALPHA_SPINNER = "alphaSpinner";
    public static final String CK_B_TOOL_ERASER = "buttonToolEraser";
    public static final String CK_B_TOOL_PENCIL = "buttonToolPencil";
    public static final String CK_B_TOOL_PICKER = "buttonToolPicker";
    public static final String CK_B_TOOL_SELECT = "buttonToolSelect";
    public static final String CK_B_TOOL_ZOOM = "buttonToolZoom";
    public static final String CK_CONTENT_PANE = "contentPane";
    public static final String CK_GLYPH_LIST = "glyphList";
    public static final String CK_GLYPH_PANE = "glyphPane";
    public static final String CK_M_EDIT = "mEdit";
    public static final String CK_M_FILE = "mFile";
    public static final String CK_M_VIEW = "mView";
    public static final String CK_MENU_BAR = "menuBar";
    public static final String CK_SPLIT_PANE = "splitPane";
    public static final String CK_TABBED_PANE = "tabbedPane";
    public static final String CK_TOOL_BAR = "toolBar";

    /**
     * Constant to move the list selection to the top.
     *
     * @see #moveListSelectionTo(int)
     */
    public static final int MOVE_TO_TOP = -1;
    /**
     * Constant to move the list selection to the bottom.
     *
     * @see #moveListSelectionTo(int)
     */
    public static final int MOVE_TO_BOTTOM = -2;
    /**
     * Constant to increment the position of the list selection by one.
     *
     * @see #moveListSelectionTo(int)
     */
    public static final int MOVE_UP = -3;
    /**
     * Constant to decrease the position of the list selection by one.
     *
     * @see #moveListSelectionTo(int)
     */
    public static final int MOVE_DOWN = -4;

    /**
     * Property constant for the canvas border color. The color is used when
     * drawing a border around the glyph image on a canvas, if enabled.
     * Listeners registered to the property will receive a {@code Color} value
     * when the color changes.
     */
    public static final String PROPERTY_CANVAS_BORDER_COLOR = "dk.martinu.ao.fonteditor.swing.FontEditor.CANVAS_BORDER_COLOR";
    /**
     * Property constant for the font color. The font color is the base color
     * when rendering glyphs, i.e. the color of pixels that are completely
     * opaque. Listeners registered to the property will receive a
     * {@code Color} value when the color changes.
     */
    public static final String PROPERTY_FONT_COLOR = "dk.martinu.ao.fonteditor.swing.FontEditor.FONT_COLOR";
    /**
     * Property constant for the tool currently in use. Listeners registered to
     * the property wil receive a {@code Tool} value when the tool changes.
     */
    public static final String PROPERTY_TOOL = "dk.martinu.ao.fonteditor.swing.FontEditor.TOOL";
    /**
     * Property constant for the tool color. The tool color is the color used
     * when drawing on a canvas. Listeners registered to the property will
     * receive a {@code Color} value when the color changes.
     */
    public static final String PROPERTY_TOOL_COLOR = "dk.martinu.ao.fonteditor.swing.FontEditor.TOOL_COLOR";

    /**
     * The path to write/read the editor configuration file.
     *
     * @see #config
     */
    public static final Path DATATOOL_CONFIG_PATH = Paths.get("font-editor.kofi");

    /**
     * The title of the editor frame.
     */
    protected static final String FRAME_TITLE = "FontEditor";

    /**
     * Reads the editor configuration file and returns a new document
     * containing all its properties. If the file does not exist, then an empty
     * document is returned.
     *
     * @return A new document containing the properties of the configuration
     * file
     */
    @Contract(value = "-> new", pure = true)
    @NotNull
    private static Document readConfig() {
        if (Files.exists(DATATOOL_CONFIG_PATH)) {
            Document doc;
            try {
                doc = KofiCodec.provider().readFile(DATATOOL_CONFIG_PATH);
            }
            catch (IOException e) {
                Log.e("could not read configuration file", e);
                throw new RuntimeException(e);
            }
            Log.i("successfully read configuration file");
            return doc;
        }
        else {
            Log.i("creating default configuration file");
            return new Document();
        }
    }

    /**
     * Helper method that requires the specified {@code state} to be satisfied
     * ({@code true}), otherwise an {@code IllegalStateException} will be
     * thrown with the specified message.
     *
     * @param state   the required state
     * @param message the error message
     * @throws IllegalStateException if {@code state} is {@code false}
     */
    @Contract(value = "false, _ -> fail", pure = true)
    private static void requireState(boolean state, @Nullable String message) throws IllegalStateException {
        if (!state) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Wizard to create and show dialogs.
     */
    protected final EditorWizard wizard = new EditorWizard(this);
    /**
     * Document for persisting the editor configuration.
     *
     * @see #loadPreferences()
     * @see #savePreferences()
     */
    @NotNull
    protected final Document config;
    /**
     * Map of components added to the content pane.
     *
     * @see #getComponent(String)
     */
    protected final HashMap<String, JComponent> componentMap = new HashMap<>(64);
    /**
     * Map of editor actions.
     *
     * @see #getAction(String)
     */
    protected final HashMap<String, EditorAction> actionMap = new HashMap<>(64);
    /**
     * Button group for tool buttons.
     */
    protected final ButtonGroup toolGroup = new ButtonGroup();
    /**
     * The current mutable font open in the editor, or {@code null}.
     */
    @Nullable
    protected MutableFont mFont = null;
    /**
     * The current tab open in the editor, or {@code null}.
     */
    @Nullable
    protected GlyphTab tab = null;
    /**
     * The glyph list model.
     */
    protected final DefaultListModel<MutableGlyph> glyphListModel = new DefaultListModel<>();
    protected final DefaultBoundedRangeModel alphaSliderModel = new DefaultBoundedRangeModel(255, 0, 0, 255);
    protected final SpinnerNumberModel alphaSpinnerModel = new SpinnerNumberModel(255, 0, 255, 1);
    protected final ArrayList<GlyphTab> tabList = new ArrayList<>();

    /**
     * The current tool for editing, the default value is {@code MOVE}.
     */
    @NotNull
    protected Tool tool = Tool.MOVE;
    /**
     * Components of the color value to use when drawing on the canvas.
     */
    protected final int[] rgba = new int[] {
            0, 0, 0, 255
    };

    /**
     * Creates a new, initially invisible, font editor frame.
     */
    public EditorFrame() {
        setLocale(Locale.ENGLISH);
        config = readConfig();
        createActions();
        createGUI();
        loadPreferences();
    }

    /**
     * Adds the specified glyph to the current font, adding it to the glyph
     * list and displaying it on the canvas.
     *
     * @param glyph the glyph to add
     * @throws NullPointerException  if {@code glyph} is {@code null}
     * @throws IllegalStateException if the current font is {@code null}
     * @see #setFont(MutableFont)
     */
    public void addGlyph(@NotNull MutableGlyph glyph) {
        Objects.requireNonNull(glyph, "glyph is null");
        requireState(mFont != null, "current font is null");
        int index = mFont.glyphList.size();
        // add glyph to font and list model
        mFont.glyphList.add(index, glyph);
        glyphListModel.add(index, glyph);
        // make glyph selected
        JList<MutableGlyph> glyphList = getComponent(CK_GLYPH_LIST);
        glyphList.setSelectedIndex(index);
        // display glyph on canvas
        showGlyphTab(glyph);
    }

    /**
     * Deletes the glyphs currently selected in the glyph list from the current
     * font.
     * <p>
     * This method will remove the glyphs from the font's list of glyphs and the
     * editor's glyph list, and clear the canvas if the current glyph is
     * selected.
     *
     * @throws IllegalStateException if no glyphs are selected or the current
     *                               font is {@code null}
     * @see #setFont(MutableFont)
     */
    public void deleteSelection() {
        JList<MutableGlyph> list = getComponent(CK_GLYPH_LIST);
        List<MutableGlyph> glyphs = list.getSelectedValuesList();
        requireState(glyphs.size() != 0, "no glyphs are selected");
        requireState(mFont != null, "current font is null");
        // ask user to confirm
        Option option = wizard.showConfirmationDialog(
                "Confirm Delete",
                "Are you sure you want to delete the current selection?",
                CANCEL,
                YES, CANCEL);
        // delete glyphs
        if (option == YES) {
            mFont.glyphList.removeAll(glyphs);
            mFont.isDirty = true;
            glyphs.forEach(glyph -> {
                glyphListModel.removeElement(glyph);
                for (int i = tabList.size() - 1; i >= 0; i--) {
                    if (tabList.get(i).canvas.glyph == glyph) {
                        tabList.get(i).close();
                    }
                }
            });
            getAction(ACTION_SAVE_FILE).setEnabled(true);
        }
    }

    // DOC editFontProperties
    public void editFontProperties() {
        requireState(mFont != null, "current font is null");
        MutableFont edit = wizard.showFontDialog(mFont);
        if (edit != null && !mFont.equals(edit)) {
            boolean dirty = false;

            if (mFont.name != edit.name) {
                mFont.name = edit.name;
                dirty = true;
            }
            if (mFont.height != edit.height) {
                mFont.height = edit.height;
                dirty = true;
            }

            if (dirty) {
                mFont.isDirty = true;
                if (!mFont.name.isBlank()) {
                    setTitle(FRAME_TITLE + " - " + mFont.name);
                }
                else {
                    setTitle(FRAME_TITLE);
                }
                getAction(ACTION_SAVE_FILE).setEnabled(true);
                // TODO update warnings for glyphs that are greater than font height
            }
        }
    }

    // DOC editGlyphProperties
    public void editGlyphProperties() {
        requireState(tab != null, "current glyph is null");
        MutableGlyph glyph = tab.canvas.glyph;
        MutableGlyph edit = wizard.showGlyphDialog(glyph);
        if (edit != null && !glyph.equals(edit)) {
            boolean dirty = false;

            boolean newSize = glyph.width != edit.width || glyph.height != edit.height;
            if (newSize) {
                // size of data that needs to be moved into new data array
                int dataWidth = Math.min(glyph.width, edit.width);
                int dataHeight = Math.min(glyph.height, edit.height);
                // transfer previous glyph.data into new array
                byte[] data = new byte[edit.width * edit.height];
                for (int y = 0; y < dataHeight; y++) {
                    System.arraycopy(glyph.data, y * glyph.width, data, y * dataWidth, dataWidth);
                }
                glyph.width = edit.width;
                glyph.height = edit.height;
                glyph.data = data;
                dirty = true;
            }

            if (glyph.isWhitespace != edit.isWhitespace) {
                glyph.isWhitespace = edit.isWhitespace;
                dirty = true;
            }
            if (glyph.value != edit.value) {
                glyph.value = edit.value;
                dirty = true;
            }
            if (glyph.offsetY != edit.offsetY) {
                glyph.offsetY = edit.offsetY;
                dirty = true;
            }

            if (dirty) {
                // only update image after ALL fields have been set
                if (newSize) {
                    tab.canvas.updateImage();
                }
                tab.canvas.repaint();
//                tab.canvas.glyph.isDirty = true; // forwards to property listener and marks font as dirty
            }
        }
    }

    // DOC editHorizontalOffsets
    // TODO editHorizontalOffsets
    public void editHorizontalOffsets() {
        requireState(mFont != null, "current font is null");
        requireState(tab != null, "current glyph is null");
        int[] edit = wizard.showHorizontalOffsetsDialog(mFont, tab.canvas.glyph);
        if (edit != null && !Arrays.equals(tab.canvas.glyph.offsetX, edit)) {

        }
    }

    /**
     * Retrieves the action from the editor action map with the specified key.
     *
     * @param key the action key
     * @return the action
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is unknown
     */
    @Contract(pure = true)
    @NotNull
    public EditorAction getAction(@NotNull String key) {
        EditorAction action = actionMap.get(Objects.requireNonNull(key, "key is null"));
        if (action != null) {
            return action;
        }
        else {
            throw new IllegalArgumentException("unknown action key {" + key + "}");
        }
    }

    /**
     * Retrieves the component from the editor component map with the specified
     * key.
     *
     * @param key the component key
     * @return the component
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is unknown
     */
    @Contract(pure = true)
    @NotNull
    public <T extends JComponent> T getComponent(@NotNull String key) {
        //noinspection unchecked
        T component = (T) componentMap.get(Objects.requireNonNull(key, "key is null"));
        if (component != null) {
            return component;
        }
        else {
            throw new IllegalArgumentException("unknown component key {" + key + "}");
        }
    }

    /**
     * Moves all selected glyphs in the glyph list to the specified index,
     * swapping places with the glyphs at that position.
     *
     * @param index the position in the list to move the selected glyphs to
     * @throws IllegalStateException if the current font is {@code null}
     */
    // TEST moveListSelectionTo
    public void moveListSelectionTo(int index) {
        requireState(mFont != null, "current font is null");
        if (index < 0 || index >= mFont.glyphList.size()) {
            throw new IndexOutOfBoundsException(index);
        }

        JList<MutableGlyph> glyphList = getComponent(CK_GLYPH_LIST);
        int[] indices = glyphList.getSelectedIndices();
        if (indices.length == 0 || indices[0] == index) {
            return;
        }
        // offset to move selected glyphs by
        int offset = index - indices[0];

        // TODO swap consecutive indices in bulk to minimize number of events fired
        // map of old index keys mapped to new index values
        TreeMap<Integer, Integer> indexMap = new TreeMap<>();
        // populate map and move selected glyphs
        for (int oldIndex : indices) {
            int newIndex = oldIndex + offset;

            indexMap.put(oldIndex, newIndex);
            // TODO determine new index for glyph at newIndex position
//            indexMap.put(newIndex, oldIndex);

            MutableGlyph glyph = glyphListModel.get(oldIndex);
            glyphListModel.remove(oldIndex);
            glyphListModel.add(newIndex, glyph);
        }

        // update indices in offsetX arrays of all glyphs
        for (MutableGlyph glyph : mFont.glyphList) {
            for (int i = 0; i < glyph.offsetX.length; i += 2) {
                Integer newIndex = indexMap.get(glyph.offsetX[i]);
                if (newIndex != null) {
                    glyph.offsetX[i] = newIndex;
//                    glyph.isDirty = true;
                }
            }
        }

        mFont.isDirty = true;
        getAction(ACTION_SAVE_FILE).setEnabled(true);
    }

    // DOC propertyChange
    @Override
    public void propertyChange(@NotNull PropertyChangeEvent event) {
        requireState(mFont != null, "current font is null");
        if (event.getPropertyName().equals(PROPERTY_DIRTY)) {
            GlyphCanvas canvas = (GlyphCanvas) event.getSource();
            int index = glyphListModel.indexOf(canvas.glyph);
            // force update in list to reflect dirty state
            glyphListModel.setElementAt(canvas.glyph, index);

            boolean isDirty = (boolean) event.getNewValue();
            if (isDirty) {
                mFont.isDirty = true;
                getAction(ACTION_SAVE_FILE).setEnabled(true);
            }
        }
    }

    /**
     * Saves to current font to a file. If {@code saveAs} is not {@code null},
     * then the font will be saved to the specified file. Otherwise, if the
     * file returned by {@link MutableFont#file} will be used if not
     * {@code null}. lastly, the {@code EditorWizard} will display a file
     * chooser dialog. If no file was selected to save to, then this method
     * returns without saving the font.
     *
     * @param saveAs the "save as" file to save to, or {@code null}
     * @throws IllegalStateException if the current font is {@code null}
     */
    public void saveFont(@Nullable File saveAs) {
        // TODO called when  closing, will drop saves on cancel, also add NO option
        requireState(mFont != null, "current font is null");

        File file;
        if (saveAs != null) {
            file = saveAs;
        }
        else {
            boolean hasFile = mFont.file != null;
            file = hasFile ? mFont.file : wizard.showSaveFontFileDialog();
            if (file == null) {
                return;
            }
        }

        // FIX this will only save glyphs that are opened in tabs.
        //  Closed glyphs that are dirty will not be saved
//        for (GlyphTab tab : tabList) {
//            if (tab.canvas.glyph.isDirty) {
//                tab.canvas.saveImageToData();
//            }
//        }

        Font font = mFont.convertToFont();
        boolean saved = false;
        while (!saved) {
            try {
                FontCodec.writeFile(font, file);
                saved = true;
            }
            catch (IOException e) {
                Log.e("could not save font", e);
                Option op = wizard.showConfirmationDialog(
                        "Error",
                        "An error occurred while saving the font. Do you want to try again?",
                        YES,
                        YES, CANCEL);
                if (op == CANCEL) {
                    return;
                }
            }
        }

        for (GlyphTab tab : tabList) {
//            if (tab.canvas.isDirty) {
//                tab.canvas.setDirty(false);
//            }
        }
//        mFont.glyphList.forEach(glyph -> glyph.setDirty(false));
        mFont.isDirty = false;
        mFont.file = file;
        getAction(ACTION_SAVE_FILE).setEnabled(false);
        config.addString("editor", "file", file.getAbsolutePath());
    }

    /**
     * Sets the alpha value of the current tool color. This method notifies any
     * property change listeners bound to {@code PROPERTY_TOOL_COLOR} if the
     * method call resulted in a new tool color.
     *
     * @param alpha the new alpha value
     * @throws IllegalArgumentException if {@code alpha}
     */
    @Contract(mutates = "this")
    public void setAlpha(int alpha) {
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("invalid alpha value");
        }
        if (alpha != rgba[3]) {
            alphaSliderModel.setValue(alpha); // updating slider will forward new value to alpha box and spinner
            firePropertyChange(PROPERTY_TOOL_COLOR,
                    new Color(rgba[0], rgba[1], rgba[2], rgba[3]),
                    new Color(rgba[0], rgba[1], rgba[2], rgba[3] = alpha));
        }
    }

    /**
     * Sets the current font.
     * <p>
     * This method will update the editor's glyph list and clear the canvas.
     *
     * @param newFont the new font, or {@code null}
     */
    public void setFont(@Nullable MutableFont newFont) {
        if (this.mFont == newFont) {
            return;
        }

        // ask user to save current font if dirty
        if (mFont != null && mFont.isDirty) {
            Option saveOp = wizard.showConfirmationDialog(
                    "Unsaved Changes",
                    "The current font has unsaved changes. Do you wish to "
                            + "save the current font before proceeding?",
                    CANCEL,
                    YES, NO, CANCEL);
            if (saveOp == YES) {
                File file = mFont.file != null ? mFont.file : wizard.showSaveFontFileDialog();
                if (file == null) {
                    return;
                }
                Font font = mFont.convertToFont();
                boolean saved = false;
                while (!saved) {
                    try {
                        FontCodec.writeFile(font, file);
                        saved = true;
                    }
                    catch (IOException e) {
                        Log.e("could not save font", e);
                        Option retryOp = wizard.showConfirmationDialog(
                                "Error",
                                "An error occurred while saving the font. Do you want to try again?",
                                YES,
                                YES, NO, CANCEL);
                        if (retryOp == NO) {
                            break;
                        }
                        if (retryOp == CANCEL) {
                            return;
                        }
                    }
                }
            }
            else if (saveOp == CANCEL) {
                return;
            }
        }

        // TODO ensure this will forward to selection listener and update move-to actions
        glyphListModel.clear();

        boolean hasFont = newFont != null;

        getAction(ACTION_NEW_GLYPH).setEnabled(hasFont);
        getAction(ACTION_IMPORT_GLYPH).setEnabled(hasFont);
        getAction(ACTION_CLOSE_FILE).setEnabled(hasFont);
        getAction(ACTION_SAVE_AS).setEnabled(hasFont);
        getAction(ACTION_SAVE_FILE).setEnabled(false);

        getAction(ACTION_EDIT_GLYPH).setEnabled(false);
        getAction(ACTION_DELETE_GLYPHS).setEnabled(false);
        getAction(ACTION_EDIT_HORIZONTAL_OFFSETS).setEnabled(false);
        getAction(ACTION_EDIT_GLYPH_PROPERTIES).setEnabled(false);
        getAction(ACTION_EDIT_FONT_PROPERTIES).setEnabled(hasFont);

        tabList.forEach(GlyphTab::close);
        // note: this notifies ChangeListener on tabbedPane to update enabled
        // state on tool actions, do not disable tool actions here
        tabList.clear();

        if (newFont != null) {
            if (!newFont.name.isBlank()) {
                setTitle(FRAME_TITLE + " - " + newFont.name);
            }
            else {
                setTitle(FRAME_TITLE);
            }
            glyphListModel.addAll(newFont.glyphList);
            // TODO add project to recent list
            if (newFont.file != null) {
                config.addString("editor", "file", newFont.file.getAbsolutePath());
            }
        }
        else {
            setTitle(FRAME_TITLE);
            config.addString("editor", "file", null);
        }
        this.mFont = newFont;
    }

    /**
     * Sets the RGB values of the current color for rendering glyphs. This
     * method notifies any property change listeners bound to
     * {@code PROPERTY_FONT_COLOR} if the method call resulted in a new font
     * color.
     *
     * @param color the color to get the RGB components from
     * @throws NullPointerException if {@code color} is {@code null}
     */
    @Contract(mutates = "this")
    public void setFontColor(@NotNull Color color) {
        Objects.requireNonNull(color, "fontColor is null");
        int[] rgb = Util.getRGB(color, new int[3]);
        if (rgb[0] != rgba[0] || rgb[1] != rgba[1] || rgb[2] != rgba[2]) {
            firePropertyChange(PROPERTY_FONT_COLOR,
                    new Color(rgba[0], rgba[1], rgba[2], 255),
                    new Color(rgba[0] = rgb[0], rgba[1] = rgb[1], rgba[2] = rgb[2], 255));
        }
    }

    /**
     * Sets the tool to use on the canvas.
     *
     * @param tool the tool to use
     * @throws NullPointerException  if {@code tool} is {@code null}
     * @throws IllegalStateException if the current glyph is {@code null}
     */
    public void setTool(@NotNull Tool tool) {
        Objects.requireNonNull(tool, "tool is null");
        Tool oldTool = this.tool;
        if (tool != this.tool) {
            switch (tool) {
                case MOVE -> toolGroup.clearSelection();
                case SELECT ->
                        toolGroup.setSelected(((JToggleButton) getComponent(CK_B_TOOL_SELECT)).getModel(), true);
                case PENCIL ->
                        toolGroup.setSelected(((JToggleButton) getComponent(CK_B_TOOL_PENCIL)).getModel(), true);
                case ERASER ->
                        toolGroup.setSelected(((JToggleButton) getComponent(CK_B_TOOL_ERASER)).getModel(), true);
                case PICKER ->
                        toolGroup.setSelected(((JToggleButton) getComponent(CK_B_TOOL_PICKER)).getModel(), true);
                case ZOOM ->
                        toolGroup.setSelected(((JToggleButton) getComponent(CK_B_TOOL_ZOOM)).getModel(), true);
            }
            this.tool = tool;
        }
        else if (this.tool != MOVE) {
            toolGroup.clearSelection();
            this.tool = MOVE;
        }
        if (oldTool != this.tool) {
            firePropertyChange(PROPERTY_TOOL, oldTool, this.tool);
        }
    }

    /**
     * Internal helper method to expose the current color component values used
     * by the editor. The values are in range 0-255 inclusive and ordered as
     * red, green, blue, alpha.
     *
     * @return and array of the color components
     */
    int[] getRGBA() {
        return rgba;
    }

    /**
     * Creates the actions for this editor and puts them in the editor action
     * map.
     */
    protected void createActions() {

        ////// FILE MENU ACTIONS //////

        actionMap.put(ACTION_NEW_FONT, new EditorAction(
                "New Font File...",
                true,
                KeyEvent.VK_F,
                event -> Optional.ofNullable(wizard.showFontDialog(null)).ifPresent(this::setFont)
        ));
        actionMap.put(ACTION_NEW_GLYPH, new EditorAction(
                "New Glyph...",
                false,
                KeyEvent.VK_N,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, CTRL_DOWN_MASK, true),
                event -> Optional.ofNullable(wizard.showGlyphDialog(null)).ifPresent(this::addGlyph)
        ));
        actionMap.put(ACTION_IMPORT_GLYPH, new EditorAction(
                "Import Glyph From Image...",
                false,
                KeyEvent.VK_I,
                KeyStroke.getKeyStroke(KeyEvent.VK_I, CTRL_DOWN_MASK, true),
                event -> Optional.ofNullable(wizard.showImportGlyphDialog()).ifPresent(this::addGlyph)
        ));
        actionMap.put(ACTION_OPEN_FILE, new EditorAction(
                "Open File...",
                true,
                KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, CTRL_DOWN_MASK, true),
                event -> Optional.ofNullable(wizard.showOpenFontFileDialog()).ifPresent(this::setFont)
        ));
        actionMap.put(ACTION_CLOSE_FILE, new EditorAction(
                "Close File",
                false,
                KeyEvent.VK_C,
                event -> setFont((MutableFont) null)
        ));
        actionMap.put(ACTION_SAVE_FILE, new EditorAction(
                "Save File",
                false,
                KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, CTRL_DOWN_MASK, true),
                event -> saveFont(null)
        ));
        actionMap.put(ACTION_SAVE_AS, new EditorAction(
                "Save As...",
                false,
                KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, CTRL_DOWN_MASK | SHIFT_DOWN_MASK, true),
                event -> Optional.ofNullable(wizard.showSaveFontFileDialog()).ifPresent(this::saveFont)
        ));
        actionMap.put(ACTION_SETTINGS, new EditorAction(
                "Settings",
                true,
                KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, CTRL_DOWN_MASK | SHIFT_DOWN_MASK, true),
                event -> wizard.showSettingsDialog()
        ));
        // TODO remove key accelerator
        actionMap.put(ACTION_EXIT, new EditorAction(
                "Exit",
                true,
                KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, CTRL_DOWN_MASK | SHIFT_DOWN_MASK, true),
                event -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        ));


        ////// EDIT MENU ACTIONS //////

        actionMap.put(ACTION_EDIT_GLYPH, new EditorAction(
                "Edit Selected Glyph",
                false,
                KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, CTRL_DOWN_MASK, true),
                event -> {
                    JList<MutableGlyph> glyphList = getComponent(CK_GLYPH_LIST);
                    showGlyphTab(glyphList.getSelectedValue());
                }
        ));
        actionMap.put(ACTION_DELETE_GLYPHS, new EditorAction(
                "Delete Selected Glyphs",
                false,
                KeyEvent.VK_D,
                event -> deleteSelection()
        ));
        actionMap.put(ACTION_EDIT_HORIZONTAL_OFFSETS, new EditorAction(
                "Horizontal Offsets...",
                false,
                KeyEvent.VK_A,
                event -> editHorizontalOffsets()
        ));
        actionMap.put(ACTION_EDIT_GLYPH_PROPERTIES, new EditorAction(
                "Glyph Properties...",
                false,
                KeyEvent.VK_G,
                event -> editGlyphProperties()
        ));
        actionMap.put(ACTION_EDIT_FONT_PROPERTIES, new EditorAction(
                "Font Properties...",
                false,
                KeyEvent.VK_F,
                event -> editFontProperties()
        ));


        ////// VIEW MENU ACTIONS //////


        ////// TOOLBAR ACTIONS //////

        actionMap.put(ACTION_MOVE_TO_TOP, new EditorAction(
                new ImageIcon("res/images/icon/move_to_top_icon.png"),
                false,
                event -> moveListSelectionTo(MOVE_TO_TOP)
        ));
        actionMap.put(ACTION_MOVE_UP, new EditorAction(
                new ImageIcon("res/images/icon/move_up_icon.png"),
                false,
                event -> moveListSelectionTo(MOVE_UP)
        ));
        actionMap.put(ACTION_MOVE_DOWN, new EditorAction(
                new ImageIcon("res/images/icon/move_down_icon.png"),
                false,
                event -> moveListSelectionTo(MOVE_DOWN)
        ));
        actionMap.put(ACTION_MOVE_TO_BOTTOM, new EditorAction(
                new ImageIcon("res/images/icon/move_to_bottom_icon.png"),
                false,
                event -> moveListSelectionTo(MOVE_TO_BOTTOM)
        ));
        actionMap.put(ACTION_MOVE_TO, new EditorAction(
                new ImageIcon("res/images/icon/move_to_icon.png"),
                false,
                event -> {
                    assert mFont != null;
                    int index = wizard.showMoveIndexDialog(mFont.glyphList.size());
                    if (index != -1) {
                        moveListSelectionTo(index);
                    }
                }
        ));
        actionMap.put(ACTION_TOOL_MOVE, new EditorAction(
                "Move",
                false,
                KeyEvent.VK_M,
                event -> setTool(MOVE)
        ));
        actionMap.put(ACTION_TOOL_SELECT, new EditorAction(
                new ImageIcon("res/images/icon/select_icon.png"),
                false,
                event -> setTool(SELECT)
        ));
        actionMap.put(ACTION_TOOL_PENCIL, new EditorAction(
                new ImageIcon("res/images/icon/pencil_icon.png"),
                false,
                event -> setTool(PENCIL)
        ));
        actionMap.put(ACTION_TOOL_ERASER, new EditorAction(
                new ImageIcon("res/images/icon/eraser_icon.png"),
                false,
                event -> setTool(ERASER)
        ));
        actionMap.put(ACTION_TOOL_PICKER, new EditorAction(
                new ImageIcon("res/images/icon/picker_icon.png"),
                false,
                event -> setTool(PICKER)
        ));
        actionMap.put(ACTION_TOOL_ZOOM, new EditorAction(
                new ImageIcon("res/images/icon/zoom_icon.png"),
                false,
                actionEvent -> setTool(ZOOM)
        ));
    }

    /**
     * Creates the GUI for this editor and puts the components in the editor
     * component map.
     */
    protected void createGUI() {

        ////// DECLARATIONS //////

        JPanel contentPane = new JPanel(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        JScrollPane glyphPane = new JScrollPane();
        JList<MutableGlyph> glyphList = new JList<>(glyphListModel);

        JTabbedPane tabbedPane = new JTabbedPane();

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        JButton bMoveToTop = new JButton(getAction(ACTION_MOVE_TO_TOP));
        JButton bMoveUp = new JButton(getAction(ACTION_MOVE_UP));
        JButton bMoveDown = new JButton(getAction(ACTION_MOVE_DOWN));
        JButton bMoveToBottom = new JButton(getAction(ACTION_MOVE_TO_BOTTOM));
        JButton bMoveTo = new JButton(getAction(ACTION_MOVE_TO));
        JToggleButton bToolSelect = new JToggleButton(getAction(ACTION_TOOL_SELECT));
        JToggleButton bToolPencil = new JToggleButton(getAction(ACTION_TOOL_PENCIL));
        JToggleButton bToolEraser = new JToggleButton(getAction(ACTION_TOOL_ERASER));
        JToggleButton bToolPicker = new JToggleButton(getAction(ACTION_TOOL_PICKER));
        JToggleButton bToolZoom = new JToggleButton(getAction(ACTION_TOOL_ZOOM));
        AlphaBox alphaBox = new AlphaBox();
        JSlider alphaSlider = new JSlider(alphaSliderModel);
        JSpinner alphaSpinner = new JSpinner(alphaSpinnerModel);

        JMenuBar menuBar = new JMenuBar();
        JMenu mFile = new JMenu("File");
        JMenu mEdit = new JMenu("Edit");
        JMenu mView = new JMenu("View");


        ////// INITIALIZATION //////

        componentMap.put(CK_GLYPH_LIST, glyphList);
        glyphList.setName(CK_GLYPH_LIST);
        glyphList.setFont(new java.awt.Font(MONOSPACED, PLAIN, 14));
        glyphList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                MutableGlyph glyph = (MutableGlyph) value;
                JLabel label =
                        (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText(index + ": " + (glyph.isDirty ? "*" + glyph.name : glyph.name));
                return label;
            }
        });
        glyphList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            boolean hasSelection = !glyphList.isSelectionEmpty();
            getAction(ACTION_MOVE_TO_TOP).setEnabled(hasSelection);
            getAction(ACTION_MOVE_UP).setEnabled(hasSelection);
            getAction(ACTION_MOVE_DOWN).setEnabled(hasSelection);
            getAction(ACTION_MOVE_TO_BOTTOM).setEnabled(hasSelection);
            getAction(ACTION_MOVE_TO).setEnabled(hasSelection);
            getAction(ACTION_EDIT_GLYPH).setEnabled(hasSelection);
            getAction(ACTION_DELETE_GLYPHS).setEnabled(hasSelection);
        });
        glyphList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                // displays glyph on double click
                if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2) {
                    MutableGlyph glyph = glyphList.getSelectedValue();
                    if (glyph != null) {
                        showGlyphTab(glyph);
                    }
                }
            }
        });

        componentMap.put(CK_GLYPH_PANE, glyphPane);
        glyphPane.setName(CK_GLYPH_PANE);
        glyphPane.setViewportView(glyphList);

        componentMap.put(CK_TABBED_PANE, tabbedPane);
        tabbedPane.setName(CK_TABBED_PANE);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addChangeListener(event -> {
            int index = tabbedPane.getSelectedIndex();
            tab = index != -1 ? tabList.get(index) : null;
            boolean hasTab = tabbedPane.getTabCount() != 0;
            getAction(ACTION_EDIT_HORIZONTAL_OFFSETS).setEnabled(hasTab);
            getAction(ACTION_EDIT_GLYPH_PROPERTIES).setEnabled(hasTab);
            getAction(ACTION_TOOL_SELECT).setEnabled(hasTab);
            getAction(ACTION_TOOL_PENCIL).setEnabled(hasTab);
            getAction(ACTION_TOOL_ERASER).setEnabled(hasTab);
            getAction(ACTION_TOOL_PICKER).setEnabled(hasTab);
            getAction(ACTION_TOOL_ZOOM).setEnabled(hasTab);
            if (!hasTab) {
                setTool(MOVE);
            }
        });

        componentMap.put(CK_B_TOOL_SELECT, bToolSelect);
        bToolSelect.setName(CK_B_TOOL_SELECT);
        bToolSelect.setVerticalAlignment(SwingConstants.CENTER);
        bToolSelect.setAlignmentY(CENTER_ALIGNMENT);

        componentMap.put(CK_B_TOOL_PENCIL, bToolPencil);
        bToolPencil.setName(CK_B_TOOL_PENCIL);

        componentMap.put(CK_B_TOOL_ERASER, bToolEraser);
        bToolEraser.setName(CK_B_TOOL_ERASER);

        componentMap.put(CK_B_TOOL_PICKER, bToolPicker);
        bToolPicker.setName(CK_B_TOOL_PICKER);

        componentMap.put(CK_B_TOOL_ZOOM, bToolZoom);
        bToolZoom.setName(CK_B_TOOL_ZOOM);

        toolGroup.add(bToolSelect);
        toolGroup.add(bToolPencil);
        toolGroup.add(bToolEraser);
        toolGroup.add(bToolPicker);
        toolGroup.add(bToolZoom);

        componentMap.put(CK_ALPHA_BOX, alphaBox);
        alphaBox.setName(CK_ALPHA_BOX);
        alphaBox.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        componentMap.put(CK_ALPHA_SLIDER, alphaSlider);
        alphaSlider.setName(CK_ALPHA_SLIDER);
        alphaSlider.setMaximumSize(alphaSlider.getPreferredSize());

        alphaSliderModel.addChangeListener(event -> {
            int value = alphaSliderModel.getValue();
            if (value != alphaSpinnerModel.getNumber().intValue()) {
                alphaSpinnerModel.setValue(value);
                alphaBox.setAlphaImpl(value);
            }
            if (!alphaSliderModel.getValueIsAdjusting()) {
                setAlpha(value);
            }
        });

        componentMap.put(CK_ALPHA_SPINNER, alphaSpinner);
        alphaSpinner.setName(CK_ALPHA_SPINNER);
        alphaSpinner.setMaximumSize(alphaSpinner.getPreferredSize());
        alphaSpinnerModel.addChangeListener(event -> {
            int value = alphaSpinnerModel.getNumber().intValue();
            if (value != alphaSliderModel.getValue()) {
                alphaSliderModel.setValue(value);
                alphaBox.setAlphaImpl(value);
                setAlpha(value);
            }
        });

        componentMap.put(CK_SPLIT_PANE, splitPane);
        splitPane.setName(CK_SPLIT_PANE);
        splitPane.setLeftComponent(glyphPane);
        splitPane.setRightComponent(tabbedPane);
        splitPane.setDividerSize(7);

        componentMap.put(CK_TOOL_BAR, contentPane);
        toolBar.setName(CK_TOOL_BAR);
        toolBar.setFloatable(false);
//        toolBar.setOpaque(false);
        toolBar.add(bMoveToTop);
        toolBar.add(bMoveUp);
        toolBar.add(bMoveDown);
        toolBar.add(bMoveToBottom);
        toolBar.add(bMoveTo);
        toolBar.addSeparator();
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(bToolSelect);
        toolBar.add(bToolPencil);
        toolBar.add(bToolEraser);
        toolBar.add(bToolPicker);
        toolBar.add(bToolZoom);
        toolBar.addSeparator();
        toolBar.add(alphaBox);
        toolBar.add(Box.createHorizontalStrut(2));
        toolBar.add(alphaSlider);
        toolBar.add(Box.createHorizontalStrut(2));
        toolBar.add(alphaSpinner);
        toolBar.addSeparator();

        componentMap.put(CK_CONTENT_PANE, contentPane);
        contentPane.setName(CK_CONTENT_PANE);
        contentPane.add(splitPane, CENTER);
        contentPane.add(toolBar, NORTH);
        setContentPane(contentPane);


        ////// MENUS //////

        componentMap.put(CK_M_FILE, mFile);
        mFile.setName(CK_M_FILE);
        mFile.setMnemonic(KeyEvent.VK_F);
        mFile.add(getAction(ACTION_NEW_FONT));
        mFile.add(getAction(ACTION_NEW_GLYPH));
        mFile.add(getAction(ACTION_IMPORT_GLYPH));
        mFile.addSeparator();
        mFile.add(getAction(ACTION_OPEN_FILE));
        mFile.add(getAction(ACTION_CLOSE_FILE));
        mFile.addSeparator();
        mFile.add(getAction(ACTION_SAVE_FILE));
        mFile.add(getAction(ACTION_SAVE_AS));
        mFile.addSeparator();
        mFile.add(getAction(ACTION_EXIT));

        componentMap.put(CK_M_EDIT, mEdit);
        mEdit.setName(CK_M_EDIT);
        mEdit.setMnemonic(KeyEvent.VK_E);
        mEdit.add(getAction(ACTION_EDIT_GLYPH));
//        mEdit.add(getAction(ACTION_DELETE_GLYPH));
        mEdit.add(getAction(ACTION_DELETE_GLYPHS));
        mEdit.addSeparator();
        mEdit.add(getAction(ACTION_EDIT_HORIZONTAL_OFFSETS));
        mEdit.add(getAction(ACTION_EDIT_GLYPH_PROPERTIES));
        mEdit.add(getAction(ACTION_EDIT_FONT_PROPERTIES));

        componentMap.put(CK_M_VIEW, mView);
        mView.setName(CK_M_VIEW);

        componentMap.put(CK_MENU_BAR, menuBar);
        menuBar.setName(CK_MENU_BAR);
        menuBar.add(mFile);
        menuBar.add(mEdit);
        menuBar.add(mView);
        setJMenuBar(menuBar);


        ////// FRAME SETUP //////

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        setTitle(FRAME_TITLE);
        setIconImages(List.of(new ImageIcon("res/images/app_icon.png").getImage()));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                Option exitOption = wizard.showConfirmationDialog(
                        "Confirm Exit",
                        "Are you sure you want to exit?",
                        CANCEL,
                        YES, CANCEL);
                if (exitOption != YES) {
                    return;
                }

                if (mFont != null && mFont.isDirty) {
                    Option saveOption = wizard.showConfirmationDialog(
                            "Confirm Save",
                            "Do you want to save the file before closing?",
                            YES,
                            YES, NO, CANCEL);
                    if (saveOption == CANCEL) {
                        return;
                    }
                    else if (saveOption == YES) {
                        saveFont(null);
                    }
                }

                savePreferences();
                dispose();
                try {
                    DocumentIO.writeFile(DATATOOL_CONFIG_PATH, config);
                }
                catch (Exception e) {
                    Log.e("could not save editor configuration", e);
                    e.printStackTrace();
                }
            }
        });

        pack();
        setLocationRelativeTo(null);
        splitPane.setDividerLocation(200);

        // setup actions for non-menu key shortcuts
        {
            ActionMap actionMap = contentPane.getActionMap();
            actionMap.put(ACTION_TOOL_MOVE, getAction(ACTION_TOOL_MOVE));
            actionMap.put(ACTION_TOOL_SELECT, getAction(ACTION_TOOL_SELECT));
            actionMap.put(ACTION_TOOL_PENCIL, getAction(ACTION_TOOL_PENCIL));
            actionMap.put(ACTION_TOOL_ERASER, getAction(ACTION_TOOL_ERASER));
            actionMap.put(ACTION_TOOL_PICKER, getAction(ACTION_TOOL_PICKER));
            actionMap.put(ACTION_TOOL_ZOOM, getAction(ACTION_TOOL_ZOOM));

            InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), ACTION_TOOL_MOVE);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), ACTION_TOOL_SELECT);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), ACTION_TOOL_PENCIL);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), ACTION_TOOL_ERASER);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), ACTION_TOOL_PICKER);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0), ACTION_TOOL_ZOOM);
        }
    }

    /**
     * Helper method that returns the index of the tab owning a canvas that is
     * showing the specified glyph, or {@code -1}.
     *
     * @param glyph the glyph
     * @return the index of the tab, or {@code -1}
     */
    @Contract(pure = true)
    protected int getTabIndex(@NotNull MutableGlyph glyph) {
        for (int i = 0; i < tabList.size(); i++) {
            if (glyph == tabList.get(i).canvas.glyph) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Loads preferences and state from the editor configuration document.
     * Preferences and state which are not contained in the document will use
     * default values.
     */
    protected void loadPreferences() {
        config.acceptObject("editor", "bounds", obj -> {
            try {
                Rectangle bounds = obj.construct(Rectangle.class);
                setBounds(bounds);
                Log.i("bounds set to " + bounds);
            }
            catch (Exception e) {
                Log.e("could not set bounds from config", e);
                e.printStackTrace();
            }
        });
        config.acceptInt("editor", "state", state -> {
            setExtendedState(state);
            Log.i("extended state set to " + state);
        });

        config.acceptInt("editor", "dividerSize", size -> {
            ((JSplitPane) getComponent(CK_SPLIT_PANE)).setDividerSize(size);
            Log.i("divider size set to " + size);
        });
        config.acceptInt("editor", "dividerLocation", location -> {
            ((JSplitPane) getComponent(CK_SPLIT_PANE)).setDividerLocation(location);
            Log.i("divider location set to " + location);
        });

        config.acceptString("editor", "dir", dir -> {
            wizard.setDirectory(dir);
            Log.i("wizard directory set to " + dir);
        });
    }

    /**
     * Stores current preferences and state in the editor configuration
     * document.
     */
    protected void savePreferences() {
        int state = getExtendedState();
        // frame bounds
        if (state != MAXIMIZED_BOTH) {
            config.addObject("editor", "bounds", KofiObject.reflect(getBounds()));
        }
        else {
            config.removeProperty("editor", "bounds");
        }
        // extended state
        if (state != ICONIFIED) {
            config.addInt("editor", "state", state);
        }
        else {
            config.removeProperty("editor", "state");
        }

        // divider size
        config.addInt("editor", "dividerSize",
                ((JSplitPane) getComponent(CK_SPLIT_PANE)).getDividerSize());
        // divider location
        config.addInt("editor", "dividerLocation",
                ((JSplitPane) getComponent(CK_SPLIT_PANE)).getDividerLocation());

        // wizard file chooser directory
        String dir = wizard.getDirectory();
        if (dir != null) {
            config.addString("editor", "dir", dir);
        }
        else {
            config.removeProperty("editor", "dir");
        }
    }

    /**
     * Shows a canvas for the specified glyph in the tabbed pane. If a canvas
     * for the glyph already exists, the tab owning that canvas is selected.
     * Otherwise, a new tab and canvas will be created for the glyph and
     * selected.
     *
     * @param glyph the glyph to show
     * @see GlyphCanvas
     */
    protected void showGlyphTab(@NotNull MutableGlyph glyph) {
        JTabbedPane tabbedPane = getComponent(CK_TABBED_PANE);
        int tabIndex = getTabIndex(glyph);
        // select existing tab if possible
        if (tabIndex != -1) {
            if (tabbedPane.getSelectedIndex() != tabIndex) {
                tabbedPane.setSelectedIndex(tabIndex);
            }
        }
        // create and select new tab
        else {
            int i = tabbedPane.getTabCount();
            GlyphTab tab = new GlyphTab(glyph);
            tabList.add(i, tab); // add to list first, or ChangeListener on tabbedPane will throw
            tabbedPane.insertTab(null, null, tab.canvas, null, i);
            tabbedPane.setTabComponentAt(i, tab);
            tabbedPane.setSelectedIndex(i);
            // center AFTER canvas is added to pane; it must be laid out
            tab.canvas.centerImage();
        }
    }

    // DOC GlyphTab
    protected class GlyphTab extends JPanel implements PropertyChangeListener {

        @NotNull
        public final GlyphCanvas canvas;
        @NotNull
        protected final JLabel titleLabel;
        @NotNull
        protected final JButton bClose;

        public GlyphTab(@NotNull MutableGlyph glyph) {
            super(new BorderLayout());
            Objects.requireNonNull(glyph, "glyph is null");

            canvas = new GlyphCanvas(EditorFrame.this, glyph);
            // notify editor and tab when canvas becomes dirty
            canvas.addPropertyChangeListener(PROPERTY_DIRTY, EditorFrame.this);
            canvas.addPropertyChangeListener(PROPERTY_DIRTY, this);

            titleLabel = new JLabel(/*glyph.isDirty ? "*" + glyph.name : glyph.name*/);
            bClose = new JButton(new ImageIcon("res/image/icon/close.png"));

            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
            titleLabel.setBackground(null);
            titleLabel.setOpaque(false);

            bClose.setBorderPainted(false);
            bClose.setFocusPainted(false);
            bClose.setOpaque(false);
            bClose.setPreferredSize(new Dimension(9, 9));
            bClose.setPressedIcon(new ImageIcon("res/image/icon/close_active.png"));
            bClose.setRolloverEnabled(true);
            bClose.setRolloverIcon(new ImageIcon("res/image/icon/close_active.png"));
            bClose.addActionListener(event -> close());

            add(titleLabel, CENTER);
            add(bClose, EAST);
        }

        public void close() {
            int tabIndex = getTabIndex(canvas.glyph);
            tabList.remove(tabIndex);
            JTabbedPane tabbedPane = EditorFrame.this.getComponent(CK_TABBED_PANE);
            tabbedPane.remove(canvas);
            canvas.removePropertyChangeListener(PROPERTY_DIRTY, EditorFrame.this);
            canvas.removePropertyChangeListener(PROPERTY_DIRTY, this);
            EditorFrame.this.removePropertyChangeListener(PROPERTY_TOOL_COLOR, canvas);
            EditorFrame.this.removePropertyChangeListener(PROPERTY_TOOL, canvas);
        }

        @Override
        public void propertyChange(@NotNull PropertyChangeEvent event) {
            if (event.getPropertyName().equals(PROPERTY_DIRTY)) {
                boolean isDirty = (boolean) event.getNewValue();
                titleLabel.setText(isDirty ? "*" + canvas.glyph.name : canvas.glyph.name);
            }
        }
    }
}
