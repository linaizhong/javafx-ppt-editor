package slideeditor;

import java.io.Serializable;

public class TextElement implements Serializable {
    private static final long serialVersionUID = 9L;  // bumped: lineHighlightTextColors added

    private String id;
    private String text;
    private double x;
    private double y;
    private double fontSize;
    private String fontFamily;
    private String color;
    private boolean bold;
    private boolean italic;
    private String shapeType;

    // alignment ("LEFT" | "CENTER" | "RIGHT") and bullet-list toggle
    private String alignment;   // null → treated as "LEFT" by getter
    private boolean bulletList;

    // Manual resize dimensions.  0 means "auto-size from text content".
    // No initializers so older serialized files deserialize safely;
    // getters return 0 when the field was never written.
    private double width;
    private double height;

    // Fill colours for the outer shape and inner text container.
    // Null means "use the default" so older serialized files deserialize safely.
    // Getters return safe defaults when the field was never written.
    private String shapeFillColor;    // null → "#f0f0f0"
    private String textBoxFillColor;  // null → "transparent"

    // When true this element is a PowerPoint-style slide placeholder.
    // Rendered with a dashed border instead of a solid one.
    // Defaults to false for all existing serialized files.
    private boolean placeholder;

    // When true, x/y/width/height are derived from these ratios relative to the
    // live slide-pane size, so the placeholder resizes with the window.
    // Fields default to 0/false for old serialised files (safe: only read when
    // relativeLayout == true, which also defaults to false).
    private boolean relativeLayout;
    private double xRatio;
    private double yRatio;
    private double widthRatio;
    private double heightRatio;

    // Per-line indent levels (0 = no indent, 1–8 = nested levels).
    // Null means "all lines are at level 0" — safe default for old serialized files.
    // The array is always resized to match the number of lines in getText().
    private int[] indentLevels;

    // Per-line highlight colours (e.g. "#FFFF00" for yellow).
    // Null entry or null array = no highlight on that line.
    // Safe default for old serialized files.
    private String[] lineHighlightColors;

    // Per-line text colours for highlighted lines (e.g. "#000000" for black).
    // Null entry or null array = use the element's global text colour.
    // Only meaningful when the corresponding lineHighlightColors entry is non-null.
    // Safe default for old serialized files.
    private String[] lineHighlightTextColors;

    public TextElement(String text, double x, double y, double fontSize, String color) {
        this.id         = java.util.UUID.randomUUID().toString();
        this.text       = text;
        this.x          = x;
        this.y          = y;
        this.fontSize   = fontSize;
        this.fontFamily = "Arial";
        this.color      = color;
        this.bold       = false;
        this.italic     = false;
        this.shapeType  = "rectangle";
        this.alignment  = "LEFT";
        this.bulletList = false;
        this.width      = 0;
        this.height     = 0;
        this.shapeFillColor   = "#f0f0f0";
        this.textBoxFillColor = "transparent";
        this.placeholder      = false;
    }

    // ── Existing getters / setters ────────────────────────────────────────────

    public String getId()              { return id; }
    public void   setId(String id)     { this.id = id; }

    public String getText()             { return text; }
    public void   setText(String text)  { this.text = text; }

    public double getX()               { return x; }
    public void   setX(double x)       { this.x = x; }

    public double getY()               { return y; }
    public void   setY(double y)       { this.y = y; }

    public double getFontSize()                  { return fontSize; }
    public void   setFontSize(double fontSize)   { this.fontSize = fontSize; }

    public String getFontFamily()                      { return fontFamily; }
    public void   setFontFamily(String fontFamily)     { this.fontFamily = fontFamily; }

    public String getColor()                { return color; }
    public void   setColor(String color)    { this.color = color; }

    public boolean isBold()               { return bold; }
    public void    setBold(boolean bold)   { this.bold = bold; }

    public boolean isItalic()                 { return italic; }
    public void    setItalic(boolean italic)  { this.italic = italic; }

    public String getShapeType()                     { return shapeType; }
    public void   setShapeType(String shapeType)     { this.shapeType = shapeType; }

    /** Returns "LEFT" as a safe default for files saved before this field existed. */
    public String getAlignment() { return (alignment == null) ? "LEFT" : alignment; }
    public void   setAlignment(String alignment) { this.alignment = alignment; }

    public boolean isBulletList()                    { return bulletList; }
    public void    setBulletList(boolean bulletList)  { this.bulletList = bulletList; }

    // ── Manual resize dimensions ──────────────────────────────────────────────
    // 0 = "auto-size from text"; any positive value = manually fixed size.

    public double getWidth()              { return width; }
    public void   setWidth(double width)  { this.width = width; }

    public double getHeight()               { return height; }
    public void   setHeight(double height)  { this.height = height; }

    /** Returns true if this element is a slide placeholder (dashed border style). */
    public boolean isPlaceholder()                      { return placeholder; }
    public void    setPlaceholder(boolean placeholder)  { this.placeholder = placeholder; }

    // ── Fill colours ──────────────────────────────────────────────────────────

    /** Background fill of the outer shape. Defaults to "#f0f0f0" for old files. */
    public String getShapeFillColor() {
        return (shapeFillColor == null) ? "#f0f0f0" : shapeFillColor;
    }
    public void setShapeFillColor(String shapeFillColor) {
        this.shapeFillColor = shapeFillColor;
    }

    /** Background fill of the inner text container. Defaults to "transparent" for old files. */
    public String getTextBoxFillColor() {
        return (textBoxFillColor == null) ? "transparent" : textBoxFillColor;
    }
    public void setTextBoxFillColor(String textBoxFillColor) {
        this.textBoxFillColor = textBoxFillColor;
    }

    // ── Relative layout (proportional placeholder sizing) ─────────────────────

    /**
     * When true, the element's x/y/width/height are computed from xRatio,
     * yRatio, widthRatio, heightRatio multiplied by the current slide-pane size.
     * The controller recalculates the pixel values on every pane resize.
     */
    public boolean isRelativeLayout()                        { return relativeLayout; }
    public void    setRelativeLayout(boolean relativeLayout) { this.relativeLayout = relativeLayout; }

    public double getXRatio()               { return xRatio; }
    public void   setXRatio(double xRatio)  { this.xRatio = xRatio; }

    public double getYRatio()               { return yRatio; }
    public void   setYRatio(double yRatio)  { this.yRatio = yRatio; }

    public double getWidthRatio()                 { return widthRatio; }
    public void   setWidthRatio(double widthRatio) { this.widthRatio = widthRatio; }

    public double getHeightRatio()                  { return heightRatio; }
    public void   setHeightRatio(double heightRatio) { this.heightRatio = heightRatio; }

    // ── Per-line indent levels ────────────────────────────────────────────────

    /**
     * Returns the indent level (0–8) for the given line index.
     * Returns 0 safely if indentLevels is null (old files) or index is out of range.
     */
    public int getIndentLevel(int lineIndex) {
        if (indentLevels == null || lineIndex < 0 || lineIndex >= indentLevels.length) return 0;
        return indentLevels[lineIndex];
    }

    /**
     * Sets the indent level for a specific line, growing the array as needed.
     * The level is clamped to [0, 8].
     */
    public void setIndentLevel(int lineIndex, int level) {
        if (lineIndex < 0) return;
        int lineCount = text == null ? 1 : (text.split("\n", -1).length);
        int capacity  = Math.max(lineIndex + 1, lineCount);
        if (indentLevels == null || indentLevels.length < capacity) {
            int[] grown = new int[capacity];
            if (indentLevels != null) System.arraycopy(indentLevels, 0, grown, 0, indentLevels.length);
            indentLevels = grown;
        }
        indentLevels[lineIndex] = Math.max(0, Math.min(8, level));
    }

    /** Raw accessor — may be null for elements created before this feature existed. */
    public int[] getIndentLevels()               { return indentLevels; }
    public void  setIndentLevels(int[] levels)   { this.indentLevels = levels; }

    /**
     * Trims or grows indentLevels to exactly match the current line count,
     * padding with 0 for any new lines.  Call after text changes.
     */
    public void syncIndentLevels() {
        int lineCount = (text == null || text.isEmpty()) ? 1 : text.split("\n", -1).length;
        if (indentLevels == null) {
            indentLevels = new int[lineCount];
        } else if (indentLevels.length != lineCount) {
            int[] synced = new int[lineCount];
            System.arraycopy(indentLevels, 0, synced, 0, Math.min(indentLevels.length, lineCount));
            indentLevels = synced;
        }
    }

    // ── Per-line highlight colours ────────────────────────────────────────────

    /**
     * Returns the highlight colour for the given line, or null if none.
     * Safe for old serialized files (returns null when array is absent).
     */
    public String getLineHighlightColor(int lineIndex) {
        if (lineHighlightColors == null || lineIndex < 0 || lineIndex >= lineHighlightColors.length)
            return null;
        return lineHighlightColors[lineIndex];
    }

    /**
     * Sets the highlight colour for a specific line, growing the array as needed.
     * Pass null to clear the highlight on that line.
     */
    public void setLineHighlightColor(int lineIndex, String color) {
        if (lineIndex < 0) return;
        // Use lineIndex+1 as capacity — do NOT use text.split() line count here,
        // because during editing text still holds the OLD saved value, which can
        // be shorter than the live TextArea content. Using text length would cap
        // the array too small and lose highlights on lines beyond the old text.
        int capacity = lineIndex + 1;
        if (lineHighlightColors == null || lineHighlightColors.length < capacity) {
            String[] grown = new String[capacity];
            if (lineHighlightColors != null)
                System.arraycopy(lineHighlightColors, 0, grown, 0, lineHighlightColors.length);
            lineHighlightColors = grown;
        }
        lineHighlightColors[lineIndex] = color;
    }

    /** Clears the highlight colour on all lines. */
    public void clearAllHighlights() { lineHighlightColors = null; }

    /** Raw accessor — may be null for old files. */
    public String[] getLineHighlightColors()                     { return lineHighlightColors; }
    public void     setLineHighlightColors(String[] colors)      { this.lineHighlightColors = colors; }

    /**
     * Trims or grows lineHighlightColors to match the current line count.
     * Call after text changes (alongside syncIndentLevels).
     */
    public void syncHighlightColors() {
        int lineCount = (text == null || text.isEmpty()) ? 1 : text.split("\n", -1).length;
        if (lineHighlightColors == null) return;
        if (lineHighlightColors.length < lineCount) {
            // Grow to fit new line count, preserving existing colours
            String[] synced = new String[lineCount];
            System.arraycopy(lineHighlightColors, 0, synced, 0, lineHighlightColors.length);
            lineHighlightColors = synced;
        }
        // Do NOT shrink — trimming would discard highlight colours that were set
        // during editing when text still held the old (shorter) saved value.
    }

    // ── Per-line highlight text colours ──────────────────────────────────────

    /**
     * Returns the text colour for a highlighted line, or null if none is set
     * (meaning: use the element's global colour).
     * Safe for old serialized files (returns null when array is absent).
     */
    public String getLineHighlightTextColor(int lineIndex) {
        if (lineHighlightTextColors == null || lineIndex < 0 || lineIndex >= lineHighlightTextColors.length)
            return null;
        return lineHighlightTextColors[lineIndex];
    }

    /**
     * Sets the text colour for a specific highlighted line, growing the array as needed.
     * Pass null to revert to the element's global text colour on that line.
     */
    public void setLineHighlightTextColor(int lineIndex, String color) {
        if (lineIndex < 0) return;
        // Same fix as setLineHighlightColor — use lineIndex+1 only.
        int capacity = lineIndex + 1;
        if (lineHighlightTextColors == null || lineHighlightTextColors.length < capacity) {
            String[] grown = new String[capacity];
            if (lineHighlightTextColors != null)
                System.arraycopy(lineHighlightTextColors, 0, grown, 0, lineHighlightTextColors.length);
            lineHighlightTextColors = grown;
        }
        lineHighlightTextColors[lineIndex] = color;
    }

    /** Clears the highlight text colour on all lines (reverts to global colour). */
    public void clearAllHighlightTextColors() { lineHighlightTextColors = null; }

    /** Raw accessor — may be null for old files. */
    public String[] getLineHighlightTextColors()                  { return lineHighlightTextColors; }
    public void     setLineHighlightTextColors(String[] colors)   { this.lineHighlightTextColors = colors; }

    /**
     * Trims or grows lineHighlightTextColors to match the current line count.
     * Call after text changes alongside syncHighlightColors.
     */
    public void syncHighlightTextColors() {
        int lineCount = (text == null || text.isEmpty()) ? 1 : text.split("\n", -1).length;
        if (lineHighlightTextColors == null) return;
        if (lineHighlightTextColors.length < lineCount) {
            String[] synced = new String[lineCount];
            System.arraycopy(lineHighlightTextColors, 0, synced, 0, lineHighlightTextColors.length);
            lineHighlightTextColors = synced;
        }
        // Do NOT shrink — same reason as syncHighlightColors.
    }
}