package dk.martinu.ao.fonteditor.swing;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;

import dk.martinu.ao.client.text.Font;
import dk.martinu.ao.client.text.FontCodec;
import dk.martinu.ao.fonteditor.*;
import dk.martinu.ao.fonteditor.util.Log;
import dk.martinu.ao.fonteditor.util.Value;

import static dk.martinu.ao.fonteditor.swing.EditorWizard.Option.APPLY;
import static dk.martinu.ao.fonteditor.swing.EditorWizard.Option.FINISH;
import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static javax.swing.event.TableModelEvent.INSERT;

// DOC
public class EditorWizard {

    @NotNull
    public final EditorFrame editor;
    protected final JFileChooser fileChooser = new JFileChooser();

    public EditorWizard(@NotNull final EditorFrame editor) {
        this.editor = Objects.requireNonNull(editor, "editor is null");
    }

    @Nullable
    public String getDirectory() {
        final File dir = fileChooser.getCurrentDirectory();
        if (dir != null)
            return dir.getAbsolutePath();
        else
            return null;
    }

    public void setDirectory(@Nullable final String pathname) {
        if (pathname != null) {
            final File file = new File(pathname);
            if (file.exists())
                fileChooser.setCurrentDirectory(file);
        }
        else
            fileChooser.setCurrentDirectory(null);
    }

    public Option showConfirmationDialog(@NotNull final String title, @NotNull final String message,
            @Nullable final Option def, @NotNull final Option... options) throws
            NullPointerException {
        final DialogBuilder builder = new DialogBuilder(editor, title)
                .setMessage(message);
        for (Option option : options)
            builder.addOption(option, option == def);
        return builder.show();
    }

    public void showErrorDialog(@NotNull final String message, @Nullable Throwable throwable) throws
            NullPointerException {
        Objects.requireNonNull(message, "message is null");
        final String msg;
        if (throwable != null)
            msg = message + ":\n" + throwable;
        else
            msg = message;
        new DialogBuilder(editor, "Error!")
                .setMessage(msg)
                .addOption(Option.CLOSE, true)
                .show();
    }

    @Nullable
    public MutableFont showFontDialog(@Nullable final MutableFont mFont) {
        final Value<MutableFont> fontValue = new Value<>();

        final DialogBuilder builder = new DialogBuilder(editor, mFont != null ? "Edit Font" : "New Font");
        final JPanel content = new JPanel(new GridBagLayout(), builder.dialog.isDoubleBuffered());

        final JLabel nameLabel = new JLabel("Name:");
        final JTextField nameText = new JTextField();

        final JLabel heightLabel = new JLabel("Line Height:");
        final JTextField heightText = new JTextField();

        nameLabel.setLabelFor(nameText);

        if (mFont != null)
            nameText.setText(mFont.name);

        heightLabel.setLabelFor(heightText);

        if (mFont != null)
            heightText.setText(String.valueOf(mFont.height));

        // content layout
        {
            final GridBagConstraints con = new GridBagConstraints();
            con.anchor = GridBagConstraints.LINE_START;
            con.fill = GridBagConstraints.HORIZONTAL;
            con.gridx = 0;
            con.weightx = 1.0d;
            con.weighty = 0.0d;

            content.add(nameLabel, con);
            content.add(nameText, con);
            content.add(Box.createVerticalStrut(10), con);
            content.add(heightLabel, con);
            content.add(heightText, con);
        }

        final Supplier<Boolean> validator = () -> {
            final String height = heightText.getText();
            if (height.isBlank())
                return false;
            try {
                if (Integer.parseInt(height) <= 0)
                    return false;
            }
            catch (NumberFormatException e) {
                return false;
            }
            return true;
        };

        final Option option = builder
                .setContent(content)
                .addOption(mFont != null ? APPLY : FINISH)
                .addOption(Option.CANCEL)
                .setDefaultFocus(nameText)
                .setOptionHandler(op -> {
                    if (op == APPLY || op == FINISH) {
                        if (validator.get()) {
                            final String name = nameText.getText();
                            final int height = Integer.parseInt(heightText.getText());
                            fontValue.set(new MutableFont(name, height));
                        }
                        else {
                            editor.getToolkit().beep();
                            return false;
                        }
                    }
                    return true;
                })
                .show();

        if (option == APPLY || option == FINISH)
            return fontValue.get();
        else
            return null;
    }

    public MutableGlyph showGlyphDialog(@Nullable final MutableGlyph mGlyph) {
        final Value<MutableGlyph> glyphValue = new Value<>();

        final DialogBuilder builder = new DialogBuilder(editor, mGlyph != null ? "Edit Glyph" : "New Glyph");
        final JPanel content = new JPanel(new GridBagLayout(), builder.dialog.isDoubleBuffered());

        final JLabel valueLabel = new JLabel("Value:");
        final JTextField valueText = new JTextField();

        final JLabel widthLabel = new JLabel("Width:");
        final JTextField widthText = new JTextField();

        final JLabel heightLabel = new JLabel("Height:");
        final JTextField heightText = new JTextField();

        final JCheckBox whitespace = new JCheckBox("Whitespace");

        final JLabel offsetLabel = new JLabel("Vertical Offset:");
        final JTextField offsetText = new JTextField();

        valueLabel.setLabelFor(valueText);

        widthLabel.setLabelFor(widthText);

        heightLabel.setLabelFor(heightText);

        offsetLabel.setLabelFor(offsetText);

        // update component models if glyph is specified
        if (mGlyph != null) {
            if (Character.isISOControl(mGlyph.value) || Character.isWhitespace(mGlyph.value))
                valueText.setText("0x" + Integer.toHexString(mGlyph.value).toUpperCase(Locale.ROOT));
            else
                valueText.setText(String.valueOf(mGlyph.value));
            widthText.setText(String.valueOf(mGlyph.width));
            heightText.setText(String.valueOf(mGlyph.height));
            whitespace.setSelected(mGlyph.isWhitespace);
            offsetText.setText(String.valueOf(mGlyph.offsetY));
        }

        // content layout
        {
            final GridBagConstraints con = new GridBagConstraints();
            con.anchor = GridBagConstraints.LINE_START;
            con.fill = GridBagConstraints.HORIZONTAL;
            con.gridx = 0;
            con.weightx = 1.0d;
            con.weighty = 0.0d;

            content.add(valueLabel, con);
            content.add(valueText, con);
            content.add(Box.createVerticalStrut(10), con);
            content.add(widthLabel, con);
            content.add(widthText, con);
            content.add(Box.createVerticalStrut(10), con);
            content.add(heightLabel, con);
            content.add(heightText, con);
            content.add(Box.createVerticalStrut(10), con);
            content.add(whitespace, con);
            content.add(Box.createVerticalStrut(10), con);
            content.add(offsetLabel, con);
            content.add(offsetText, con);
        }

        final Supplier<Boolean> validator = () -> {
            final String sValue = valueText.getText();
            final String width = widthText.getText();
            final String height = heightText.getText();
            final String offset = offsetText.getText();
            if (width.isBlank() || height.isBlank()
                    || offset.isBlank() || sValue.isBlank())
                return false;
            try {
                final int value;
                if (sValue.length() == 1)
                    value = sValue.charAt(0);
                else if (sValue.toLowerCase(Locale.ROOT).startsWith("0x"))
                    value = Integer.parseInt(sValue.substring(2));
                else
                    return false;
                if (value < Character.MIN_VALUE || value > Character.MAX_VALUE)
                    return false;
                if (Integer.parseInt(width) <= 0 || Integer.parseInt(height) < 0 || Integer.parseInt(offset) < 0)
                    return false;
            }
            catch (NumberFormatException e) {
                return false;
            }
            return true;
        };

        final Option option = builder
                .setContent(content)
                .addOption(mGlyph != null ? APPLY : FINISH)
                .addOption(Option.CANCEL)
                .setDefaultFocus(valueText)
                .setOptionHandler(op -> {
                    if (op == APPLY || op == FINISH) {
                        if (validator.get()) {
                            final int width = Integer.parseInt(widthText.getText());
                            final int height = Integer.parseInt(heightText.getText());
                            final int offsetY = Integer.parseInt(offsetText.getText());
                            final boolean isWhitespace = whitespace.isSelected();
                            final String sValue = valueText.getText();
                            final char value;
                            if (sValue.length() == 1)
                                value = sValue.charAt(0);
                            else // if (chars.toLowerCase(Locale.ROOT).startsWith("0x"))
                                value = (char) Integer.parseInt(sValue.substring(2));
                            glyphValue.set(new MutableGlyph(value, width, height, isWhitespace, offsetY));
                        }
                        else {
                            editor.getToolkit().beep();
                            return false;
                        }
                    }
                    return true;
                })
                .show();

        if (option == APPLY || option == FINISH)
            return glyphValue.get();
        else
            return null;
    }

    public int[] showHorizontalOffsetsDialog(@NotNull final MutableFont font, @NotNull final MutableGlyph glyph) {
        Objects.requireNonNull(font, "font is null");
        Objects.requireNonNull(glyph, "glyph is null");

        final Value<int[]> arrayValue = new Value<>();
        final OffsetsTableModel tableModel = new OffsetsTableModel(glyph);

        final DialogBuilder builder = new DialogBuilder(editor, "Edit Horizontal Offsets");
        final JPanel content = new JPanel(null, builder.dialog.isDoubleBuffered());

        final JPanel offsetPanel = new JPanel(new BorderLayout());
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        final JButton bAdd = new JButton("Add");
        final JButton bRemove = new JButton("Remove");
        final JScrollPane tablePane = new JScrollPane();
        final JTable table = new JTable(tableModel);

        final JComponent preview = new JPanel(); // TODO implement preview component

        bAdd.addActionListener(event -> {
            // TODO show add dialog
        });

        bRemove.addActionListener(event -> {
            // TODO remove selected offsets
        });

        buttonPanel.add(bAdd);
        buttonPanel.add(bRemove);

        tablePane.setViewportView(table);

        offsetPanel.setBorder(BorderFactory.createTitledBorder("Offsets"));
        offsetPanel.add(buttonPanel, NORTH);
        offsetPanel.add(tablePane, CENTER);

        preview.setBorder(BorderFactory.createTitledBorder("Preview"));

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(offsetPanel);
        content.add(preview);


        final Option option = builder
                .setContent(content)
                .addOption(APPLY)
                .addOption(Option.CANCEL)
                .setDefaultFocus(bAdd)
                .setOptionHandler(op -> {
                    if (op == APPLY) {
                        // TODO update arrayValue
                    }
                    return true;
                })
                .show();

        if (option == APPLY)
            return arrayValue.get();
        else
            return null;
    }

    @Nullable
    public MutableGlyph showImportGlyphDialog() {
        fileChooser.setDialogTitle("Open Image File");
        fileChooser.resetChoosableFileFilters();
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        final FileFilter imageFileFilter = new FileSuffixFilter("Image files", ImageIO.getReaderFileSuffixes());
        fileChooser.addChoosableFileFilter(imageFileFilter);
        fileChooser.setFileFilter(imageFileFilter);

        if (fileChooser.showOpenDialog(editor) != JFileChooser.APPROVE_OPTION)
            return null;

        final File file = fileChooser.getSelectedFile();
        try {
            final BufferedImage image = ImageIO.read(file);
            return new MutableGlyph(image);
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e("could not read image file", e);
            return null;
        }
    }

    public Integer showMoveIndexDialog(final int maxIndex) {
        final Value<Integer> indexValue = new Value<>();

        final DialogBuilder builder = new DialogBuilder(editor, "Move Selection To");
        final JPanel content = new JPanel(new GridBagLayout(), builder.dialog.isDoubleBuffered());

        final JLabel indexLabel = new JLabel("New Position:");
        final JTextField indexText = new JTextField();

        indexLabel.setLabelFor(indexText);


        // content layout
        {
            final GridBagConstraints con = new GridBagConstraints();
            con.anchor = GridBagConstraints.LINE_START;
            con.fill = GridBagConstraints.HORIZONTAL;
            con.gridx = 0;
            con.weightx = 1.0d;
            con.weighty = 0.0d;

            content.add(indexLabel, con);
            content.add(indexText, con);
        }

        final Supplier<Boolean> validator = () -> {
            final String index = indexText.getText();
            if (index.isBlank())
                return false;
            try {
                final int i = Integer.parseInt(index);
                if (i < 0 || i >= maxIndex)
                    return false;
            }
            catch (NumberFormatException e) {
                return false;
            }
            return true;
        };

        final Option option = builder
                .setContent(content)
                .addOption(APPLY)
                .addOption(Option.CANCEL)
                .setDefaultFocus(indexText)
                .setOptionHandler(op -> {
                    if (op == APPLY) {
                        if (validator.get()) {
                            final int index = Integer.parseInt(indexText.getText());
                            indexValue.set(index);
                        }
                        else {
                            editor.getToolkit().beep();
                            return false;
                        }
                    }
                    return true;
                })
                .show();

        if (option != APPLY)
            return null;

        return indexValue.get();
    }

    @Nullable
    public MutableFont showOpenFontFileDialog() {
        fileChooser.setDialogTitle("Open Font File");
        fileChooser.resetChoosableFileFilters();
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        final FileFilter fontFileFilter = new FileSuffixFilter("Abaddon Online font (*.font)", "font");
        fileChooser.addChoosableFileFilter(fontFileFilter);
        fileChooser.setFileFilter(fontFileFilter);

        if (fileChooser.showOpenDialog(editor) != JFileChooser.APPROVE_OPTION)
            return null;

        final File file = fileChooser.getSelectedFile();
        try {
            final Font font = FontCodec.readFile(file);
            return new MutableFont(font, file);
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e("could not read font file", e);
            return null;
        }
    }

    @Nullable
    public File showSaveFontFileDialog() {
        fileChooser.setDialogTitle("Save Font File");
        fileChooser.resetChoosableFileFilters();
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        final FileFilter fontFileFilter = new FileSuffixFilter("Abaddon Online font (*.font)", "font");
        fileChooser.addChoosableFileFilter(fontFileFilter);
        fileChooser.setFileFilter(fontFileFilter);

        if (fileChooser.showSaveDialog(editor) == JFileChooser.APPROVE_OPTION)
            return fileChooser.getSelectedFile();
        else
            return null;
    }

    // TODO show settings dialog
    public void showSettingsDialog() {
    }

    public enum Option {

        CLOSE("Close"),
        CANCEL("Cancel"),
        NO("No"),
        CONTINUE("Continue"),
        YES("Yes"),
        ACCEPT("Accept"),
        APPLY("Apply"),
        FINISH("Finish");

        @NotNull
        public final String text;

        Option(@NotNull final String text) {
            this.text = text;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class DialogBuilder {

        protected static final int MESSAGE_WIDTH = 250;
        protected static final int PREFERRED_WIDTH = 260;
        protected static final int PREFERRED_HEIGHT = 100;

        @Contract(value = "_, _, _ -> new", pure = true)
        @NotNull
        public static Dimension union(@NotNull final Component c, final int width, final int height) {
            return Util.union(c.getPreferredSize(), new Dimension(width, height));
        }

        protected JDialog dialog;
        protected JPanel contentPane;
        protected JComponent buttonPanel;
        @Nullable
        protected JComponent defaultFocus = null;
        @Nullable
        protected Function<Option, Boolean> optionHandler = null;
        protected volatile Value<Option> optionValue = new Value<>();

        public DialogBuilder(@Nullable final Window parent, @NotNull final String title) {
            createDialog(parent, Objects.requireNonNull(title, "title is null"));
        }

        @Contract(value = "_ -> this")
        public DialogBuilder addOption(@NotNull final Option option) {
            return addOption(option, false, true);
        }

        @Contract(value = "_, _ -> this")
        public DialogBuilder addOption(@NotNull final Option option, final boolean focused) {
            return addOption(option, focused, true);
        }

        @Contract(value = "_, _, _ -> this")
        public DialogBuilder addOption(@NotNull final Option option, final boolean focused, final boolean enabled) {
            Objects.requireNonNull(option, "option is null");
            final JButton button = new JButton(option.text);
            button.setName(option.name());
            button.setEnabled(enabled);
            button.addActionListener(event -> {
                optionValue.set(option);
                if (optionHandler == null || optionHandler.apply(option))
                    dialog.dispose();
            });
            if (buttonPanel.getComponentCount() > 0)
                buttonPanel.add(Box.createHorizontalStrut(5));
            buttonPanel.add(button);
            if (focused)
                defaultFocus = button;
            return this;
        }

        @Contract(value = "_ -> this")
        public DialogBuilder setContent(@NotNull final JComponent content) {
            Objects.requireNonNull(content, "content is null");
            contentPane.add(content, CENTER);
            return this;
        }

        @Contract(value = "_ -> this")
        public DialogBuilder setDefaultFocus(@Nullable final JComponent component) {
            defaultFocus = component;
            return this;
        }

        @Contract(value = "_ -> this")
        public DialogBuilder setMessage(@NotNull final String message) {
            Objects.requireNonNull(message, "message is null");
            final JTextArea textArea = new JTextArea(message);
            textArea.setEditable(false);
            textArea.setHighlighter(null);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setOpaque(false);
//            textArea.setPreferredSize(union(textArea, MESSAGE_WIDTH, 0));
            dialog.getContentPane().add(textArea, CENTER);
            return this;
        }

        @Contract(value = "_, _ -> this")
        public DialogBuilder setOptionEnabled(@NotNull final Option option, final boolean enabled) {
            final int n = buttonPanel.getComponentCount();
            for (int i = 0; i < n; i++) {
                final Component c = buttonPanel.getComponent(i);
                if (c.getName().equals(option.name())) {
                    c.setEnabled(enabled);
                    break;
                }
            }
            return this;
        }

        @Contract(value = "_ -> this")
        public DialogBuilder setOptionHandler(@Nullable final Function<Option, Boolean> function) {
            optionHandler = function;
            return this;
        }

        public Option show() {
            // set default option
            optionValue.set(Option.CLOSE);

            // add space between option buttons and content
            if (buttonPanel.getComponentCount() != 0 && contentPane.getComponentCount() > 1)
                buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

            // dialog size
            dialog.pack();
            dialog.setMinimumSize(dialog.getPreferredSize());
            dialog.setSize(union(dialog, PREFERRED_WIDTH, PREFERRED_HEIGHT));

            // dialog location
            final Container parent = dialog.getParent();
            if (parent instanceof Frame)
                dialog.setLocationRelativeTo(parent);
            else if (parent instanceof Dialog)
                dialog.setLocationByPlatform(true);

            // default focus
            if (defaultFocus != null)
                defaultFocus.requestFocus();

            // block thread and return option when resumed
            dialog.setVisible(true);
            return optionValue.get();
        }

        protected void createDialog(@Nullable final Window parent, @NotNull final String title) {
            dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL,
                    parent != null ? parent.getGraphicsConfiguration() : null);
            contentPane = new JPanel(new BorderLayout(0, 0), dialog.isDoubleBuffered());
            buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0), dialog.isDoubleBuffered());

            contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            contentPane.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setContentPane(contentPane);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setResizable(true);
        }
    }

    /**
     * File filter implementation that only accepts files which match a set of
     * suffixes, while also accepting directory files.
     */
    public static class FileSuffixFilter extends FileFilter {

        /**
         * see {@link #getDescription()}
         */
        public final String description;
        /**
         * Set of allowed suffixes.
         */
        protected Set<String> suffixes;

        /**
         * Constructs a new file filter.
         *
         * @param description the description
         * @param suffixes    the set of accepted suffixes
         * @throws NullPointerException if {@code description} or {@code suffixes}
         *                              is {@code null}
         */
        public FileSuffixFilter(@NotNull final String description, @NotNull final String... suffixes) {
            this.description = Objects.requireNonNull(description, "description is null");
            this.suffixes = Set.of(Objects.requireNonNull(suffixes, "suffixes is null"));
        }

        /**
         * Returns {@code true} if the specified file is a directory, or is a file
         * and its suffix matches one of the suffixes of this filter. Otherwise
         * {@code false} is returned.
         *
         * @param file the File to test
         * @return {@code true} if the file is to be accepted, otherwise
         * {@code false}
         * @throws NullPointerException if {@code file} is {@code null}
         */
        @Contract(pure = true)
        @Override
        public boolean accept(@NotNull final File file) {
            Objects.requireNonNull(file, "file is null");
            if (file.isDirectory())
                return true;
            if (file.isFile()) {
                final int index = file.getName().lastIndexOf('.');
                if (index != -1)
                    return suffixes.contains(file.getName().substring(index + 1));
            }

            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Contract(pure = true)
        @NotNull
        @Override
        public String getDescription() {
            return description;
        }
    }

    public static class OffsetsTableModel implements TableModel {

        protected final ArrayList<Integer> offsets;
        protected final ArrayList<TableModelListener> listeners = new ArrayList<>(1);

        public OffsetsTableModel(@NotNull final MutableGlyph glyph) {
            Objects.requireNonNull(glyph, "glyph is null");
            offsets = new ArrayList<>(glyph.offsetX.length);
            for (int i = 0; i < glyph.offsetX.length; i++)
                offsets.add(glyph.offsetX[i]);
        }

        @Contract(mutates = "this")
        public void addOffset(final int id, final int offset) {
            // get row and insert
            final int row = offsets.size() / 2;
            offsets.add(id);
            offsets.add(offset);
            // notify listeners
            final TableModelEvent event = new TableModelEvent(this, row, row, ALL_COLUMNS, INSERT);
            for (final TableModelListener listener : listeners)
                listener.tableChanged(event);
        }

        @Contract(mutates = "this, param1")
        public void removeOffsets(final int... rows) {
            Objects.requireNonNull(rows, "rows varargs is null");
            Arrays.sort(rows);

        }

        @Contract(mutates = "this")
        @Override
        public void addTableModelListener(@Nullable final TableModelListener l) {
            if (l != null)
                listeners.add(l);
        }

        @Contract(pure = true)
        @NotNull
        @Override
        public Class<Integer> getColumnClass(final int index) {
            return Integer.class;
        }

        @Contract(pure = true)
        @Override
        public int getColumnCount() {
            return 2;
        }

        @Contract(pure = true)
        @NotNull
        @Nls
        @Override
        public String getColumnName(@MagicConstant(intValues = {0, 1}) final int index) {
            if (index == 0)
                return "ID";
            else if (index == 1)
                return "Horizontal Offset";
            else
                throw new IllegalArgumentException("invalid index {" + index + "}");
        }

        @Contract(pure = true)
        @Override
        public int getRowCount() {
            return offsets.size() / 2;
        }

        @Contract(pure = true)
        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            return offsets.get(rowIndex * 2 + columnIndex);
        }

        @Contract(pure = true)
        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return true;
        }

        @Contract(mutates = "this")
        @Override
        public void removeTableModelListener(@Nullable final TableModelListener l) {
            if (l != null)
                listeners.remove(l);
        }

        @Contract(mutates = "this")
        @Override
        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
            // TODO update value in offsets
        }
    }
}
