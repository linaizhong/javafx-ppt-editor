package slideeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Slide implements Serializable {
    private static final long serialVersionUID = 2L;

    // ── Default slide dimensions — used only to seed the initial pixel values.
    //    The controller recalculates from the live pane size on every resize.
    static final double DEFAULT_W = 900;
    static final double DEFAULT_H = 600;

    // ── Proportional margins (fractions of slide width / height) ─────────────
    private static final double MARGIN_X_RATIO = 40.0 / DEFAULT_W;   // ≈ 0.0444
    private static final double MARGIN_Y_RATIO = 30.0 / DEFAULT_H;   // ≈ 0.0500

    // Title height = 15 % of slide height
    private static final double TITLE_H_RATIO  = 90.0 / DEFAULT_H;   // = 0.15
    // Gap between title bottom and content top
    private static final double GAP_RATIO       = 20.0 / DEFAULT_H;   // ≈ 0.0333

    private List<TextElement>  textElements;
    private List<ArrowElement> arrows;
    private List<ImageElement> images;          // ← NEW: images on this slide
    private String background;
    private int idCounter;

    public Slide() {
        this.textElements = new ArrayList<>();
        this.arrows       = new ArrayList<>();
        this.images       = new ArrayList<>();  // ← NEW
        this.background   = "white";
        this.idCounter    = 0;

        double xR = MARGIN_X_RATIO;
        double wR = 1.0 - MARGIN_X_RATIO * 2;

        double titleYR   = MARGIN_Y_RATIO;
        double titleHR   = TITLE_H_RATIO;

        double contentYR = titleYR + titleHR + GAP_RATIO;
        double contentHR = 1.0 - contentYR - MARGIN_Y_RATIO;

        // ── Title placeholder ─────────────────────────────────────────────────
        TextElement title = createPlaceholder(
            "Click to add title",
            xR, titleYR, wR, titleHR,
            36, true, "CENTER", false
        );
        textElements.add(title);

        // ── Content placeholder ───────────────────────────────────────────────
        TextElement content = createPlaceholder(
            "Click to add text",
            xR, contentYR, wR, contentHR,
            20, false, "LEFT", true
        );
        textElements.add(content);
    }

    /**
     * Build a placeholder whose geometry is specified as fractions of the slide
     * dimensions.  Pixel values are seeded from DEFAULT_W/H for the initial
     * render; the controller updates them on every pane resize.
     */
    private TextElement createPlaceholder(
            String hint,
            double xRatio, double yRatio,
            double widthRatio, double heightRatio,
            double fontSize, boolean bold,
            String alignment, boolean bulletList) {

        double px = xRatio      * DEFAULT_W;
        double py = yRatio      * DEFAULT_H;
        double pw = widthRatio  * DEFAULT_W;
        double ph = heightRatio * DEFAULT_H;

        TextElement el = new TextElement(hint, px, py, fontSize, "#aaaaaa");
        el.setWidth(pw);
        el.setHeight(ph);
        el.setBold(bold);
        el.setAlignment(alignment);
        el.setBulletList(bulletList);
        el.setShapeType("rectangle");
        el.setShapeFillColor("transparent");
        el.setTextBoxFillColor("transparent");
        el.setPlaceholder(true);

        // Store proportional ratios so the controller can recompute on resize
        el.setRelativeLayout(true);
        el.setXRatio(xRatio);
        el.setYRatio(yRatio);
        el.setWidthRatio(widthRatio);
        el.setHeightRatio(heightRatio);

        el.setId(generateId());
        return el;
    }

    public void addTextElement(TextElement element) {
        if (element.getId() == null || element.getId().isEmpty()) {
            element.setId(generateId());
        }
        textElements.add(element);
    }

    public void removeTextElement(TextElement element) {
        textElements.remove(element);
        arrows.removeIf(arrow ->
            element.getId().equals(arrow.getStartElementId()) ||
            element.getId().equals(arrow.getEndElementId())
        );
    }

    public void addArrow(ArrowElement arrow)    { arrows.add(arrow); }
    public void removeArrow(ArrowElement arrow) { arrows.remove(arrow); }

    // ── Image element API ─────────────────────────────────────────────────────

    /** Add an image to this slide. */
    public void addImage(ImageElement img) {
        if (images == null) images = new ArrayList<>(); // guard for old .sle files
        images.add(img);
    }

    /** Remove an image from this slide. */
    public void removeImage(ImageElement img) {
        if (images != null) images.remove(img);
    }

    /** Returns the (possibly empty) list of images on this slide. */
    public List<ImageElement> getImages() {
        if (images == null) images = new ArrayList<>(); // guard for old .sle files
        return images;
    }

    // ─────────────────────────────────────────────────────────────────────────

    public List<TextElement> getTextElements() { return textElements; }
    public List<ArrowElement> getArrows()      { return arrows; }

    public String getBackground()                  { return background; }
    public void   setBackground(String background) { this.background = background; }

    private String generateId() {
        return "element_" + (idCounter++);
    }
}