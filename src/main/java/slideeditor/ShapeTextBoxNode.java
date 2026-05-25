package slideeditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

/**
 * Visual node for a shape-backed text element.
 *
 * Architecture (PowerPoint-style)
 * ────────────────────────────────
 *   ShapeTextBoxNode  (Pane)
 *     ├─ backgroundShape          – visual border & fill only
 *     ├─ textContainer (StackPane) – inner text box, inset by TEXT_PADDING;
 *     │    │                         clips content to its own bounds
 *     │    └─ textNode (Text)      – wraps inside the container
 *     └─ handles[]  (Rectangle×8) – resize handles, shown when selected
 *
 * The backgroundShape is purely decorative.  All text layout, padding,
 * alignment, and overflow clipping belong to textContainer.
 *
 * Resize handles
 * ──────────────
 * When the shape is selected, 8 small square handles appear at the corners
 * and edge midpoints (NW, N, NE, E, SE, S, SW, W).  Dragging any handle
 * resizes the shape and writes the new size back to the TextElement so it
 * survives a slide refresh or save/reload.
 *
 * Auto-size vs. manual size
 * ─────────────────────────
 * While element.getWidth() == 0 the shape grows automatically to fit the
 * text content (original behaviour).  The moment the user drags a handle
 * the element's width/height are set to positive values and the shape stays
 * at that size regardless of the text content.
 */
public class ShapeTextBoxNode extends Pane {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final double HANDLE_SIZE = 8;    // px side of each square handle
    private static final double HALF_HANDLE = HANDLE_SIZE / 2;
    private static final double MIN_WIDTH   = 60;
    private static final double MIN_HEIGHT  = 40;

    /**
     * Horizontal and vertical inset between the shape boundary and the inner
     * text container – equivalent to PowerPoint's internal margin.
     */
    private static final double TEXT_PADDING = 10;

    // ── State ─────────────────────────────────────────────────────────────────
    private final TextElement           element;
    private final SlideEditorController controller;

    private Shape     backgroundShape;
    private StackPane textContainer;
    private TextFlow  textFlow;    // replaces single Text node; holds per-line Text spans

    private boolean editing  = false;
    private boolean selected = false;
    private boolean resizing = false;

    // Kept while editing so the toolbar indent buttons can reach the live TextArea
    private TextArea liveTextArea = null;

    // Set to true by applyHighlightToSelectedLines so the next runLater cycle
    // skips finishEditing and returns focus to the TextArea instead.
    private boolean suppressNextFinishEditing = false;

    // Drag-to-move bookkeeping
    private double dragStartX, dragStartY;
    private double nodeStartX, nodeStartY;

    // Resize bookkeeping
    private double resizeDragStartX, resizeDragStartY;
    private double resizeStartW,     resizeStartH;
    private double resizeStartNodeX, resizeStartNodeY;
    private String activeHandle = "";

    /**
     * Dashed blue border drawn around the textContainer when text-editing is
     * active – matching PowerPoint's inner text-edit indicator.
     * Invisible at all other times.
     */
    private Rectangle containerBorderRect;

    // The 8 resize handles
    private final Rectangle[] handles = new Rectangle[8];
    private static final String[] HANDLE_NAMES =
            {"NW", "N", "NE", "E", "SE", "S", "SW", "W"};
    private static final Cursor[] HANDLE_CURSORS = {
            Cursor.NW_RESIZE, Cursor.N_RESIZE,  Cursor.NE_RESIZE,
            Cursor.E_RESIZE,  Cursor.SE_RESIZE, Cursor.S_RESIZE,
            Cursor.SW_RESIZE, Cursor.W_RESIZE
    };

    // ── Constructor ───────────────────────────────────────────────────────────

    public ShapeTextBoxNode(TextElement element, SlideEditorController controller) {
        this.element    = element;
        this.controller = controller;

        // 1. Background shape (purely decorative)
        createBackgroundShape();

        // 2. TextFlow — holds per-line Text spans, enabling per-line highlight colours
        textFlow = new TextFlow();
        textFlow.setCursor(Cursor.HAND);
        textFlow.setMouseTransparent(false);

        // Default cursor for the whole node is HAND (clickable / selectable)
        setCursor(Cursor.HAND);

        // 3. Inner text container (PowerPoint-style inner text box)
        textContainer = new StackPane(textFlow);
        textContainer.setPickOnBounds(false);
        // Clip the container so text never bleeds outside the shape bounds
        Rectangle clip = new Rectangle();
        textContainer.layoutBoundsProperty().addListener((obs, old, b) -> {
            clip.setWidth(b.getWidth());
            clip.setHeight(b.getHeight());
        });
        textContainer.setClip(clip);

        // 4. Dashed border overlay – sits on top of textContainer, invisible
        //    until text-editing starts (mirrors PowerPoint's inner edit indicator).
        containerBorderRect = new Rectangle();
        containerBorderRect.setFill(Color.TRANSPARENT);
        containerBorderRect.setStroke(Color.web("#1a73e8"));   // PowerPoint-style blue
        containerBorderRect.setStrokeWidth(1.5);
        containerBorderRect.getStrokeDashArray().addAll(4.0, 3.0); // dashed
        containerBorderRect.setMouseTransparent(true);             // never intercepts clicks
        containerBorderRect.setVisible(false);

        getChildren().addAll(backgroundShape, textContainer, containerBorderRect);

        setLayoutX(element.getX());
        setLayoutY(element.getY());

        createHandles();
        applySize();
        updateTextStyle();   // also calls applyFillColors()
        updateSelectionStyle(false);
        setupMainEventHandlers();
    }

    // ── Size management ───────────────────────────────────────────────────────

    /**
     * Apply the current dimensions to the background shape, size and position
     * the inner text container, and reposition the resize handles.
     */
    public void applySize() {
        double w = element.getWidth();
        double h = element.getHeight();
        boolean manual = (w > 0 && h > 0);

        if (!manual) {
            // Auto mode: derive from whichever display node is currently active.
            // rebuildTextFlow puts either a TextFlow or a VBox into textContainer.
            javafx.scene.Node displayNode = textContainer.getChildren().isEmpty()
                    ? textFlow : textContainer.getChildren().get(0);
            double tw = displayNode.getLayoutBounds().getWidth();
            double th = displayNode.getLayoutBounds().getHeight();
            w = Math.max(tw + TEXT_PADDING * 2 + 10, MIN_WIDTH);
            h = Math.max(th + TEXT_PADDING * 2,      MIN_HEIGHT);
        }

        applyShapeSize(w, h);
        layoutTextContainer(w, h);
        repositionHandles(w, h);

        setPrefSize(w, h);
        setMinSize(w, h);
        setMaxSize(w, h);
    }

    /** Push new dimensions into the background shape geometry. */
    private void applyShapeSize(double w, double h) {
        if (backgroundShape instanceof Rectangle) {
            Rectangle r = (Rectangle) backgroundShape;
            r.setWidth(w);
            r.setHeight(h);
            if ("rounded".equals(element.getShapeType())) {
                r.setArcWidth(15);
                r.setArcHeight(15);
            }
        } else if (backgroundShape instanceof Circle) {
            ((Circle) backgroundShape).setRadius(Math.max(w, h) / 2);
        } else if (backgroundShape instanceof Polygon) {
            updatePolygonShape(w, h);
        }
    }

    /**
     * Size and position the inner text container.
     *
     * The container is inset by TEXT_PADDING on every side so text has the
     * same visual "breathing room" as in PowerPoint.  The text node's
     * wrapping width is set to the container width so it wraps naturally.
     * Vertical alignment is centred (matching PowerPoint's default); the
     * StackPane alignment property maps the horizontal axis.
     */
    private void layoutTextContainer(double shapeW, double shapeH) {
        double containerW = shapeW - TEXT_PADDING * 2;
        double containerH = shapeH - TEXT_PADDING * 2;

        textContainer.setLayoutX(TEXT_PADDING);
        textContainer.setLayoutY(TEXT_PADDING);
        textContainer.setPrefSize(containerW, containerH);
        textContainer.setMinSize(containerW, containerH);
        textContainer.setMaxSize(containerW, containerH);

        // Keep the dashed border overlay exactly aligned with the container
        containerBorderRect.setLayoutX(TEXT_PADDING);
        containerBorderRect.setLayoutY(TEXT_PADDING);
        containerBorderRect.setWidth(containerW);
        containerBorderRect.setHeight(containerH);

        // TextFlow wraps to the container width
        textFlow.setPrefWidth(containerW);
        textFlow.setMaxWidth(containerW);

        // Align textFlow inside container: horizontal from element alignment
        Pos alignment;
        switch (element.getAlignment()) {
            case "CENTER": alignment = Pos.TOP_CENTER; break;
            case "RIGHT":  alignment = Pos.TOP_RIGHT;  break;
            default:       alignment = Pos.TOP_LEFT;   break;
        }
        textContainer.setAlignment(alignment);

        // Also propagate text alignment into the TextFlow itself
        switch (element.getAlignment()) {
            case "CENTER": textFlow.setTextAlignment(TextAlignment.CENTER); break;
            case "RIGHT":  textFlow.setTextAlignment(TextAlignment.RIGHT);  break;
            default:       textFlow.setTextAlignment(TextAlignment.LEFT);   break;
        }
    }

    // ── Handle creation & positioning ─────────────────────────────────────────

    private void createHandles() {
        for (int i = 0; i < 8; i++) {
            Rectangle h = new Rectangle(HANDLE_SIZE, HANDLE_SIZE);
            h.setFill(Color.WHITE);
            h.setStroke(Color.web("#1a73e8"));
            h.setStrokeWidth(1.5);
            h.setCursor(HANDLE_CURSORS[i]);
            h.setUserData(HANDLE_NAMES[i]);
            setupHandleEvents(h);
            handles[i] = h;
        }
    }

    private void repositionHandles(double w, double h) {
        if (handles[0] == null) return;
        double[][] positions = {
            {0,     0    },   // NW
            {w / 2, 0    },   // N
            {w,     0    },   // NE
            {w,     h / 2},   // E
            {w,     h    },   // SE
            {w / 2, h    },   // S
            {0,     h    },   // SW
            {0,     h / 2}    // W
        };
        for (int i = 0; i < 8; i++) {
            handles[i].setLayoutX(positions[i][0] - HALF_HANDLE);
            handles[i].setLayoutY(positions[i][1] - HALF_HANDLE);
        }
    }

    private void showHandles(boolean show) {
        if (show) {
            for (Rectangle h : handles) {
                if (!getChildren().contains(h)) getChildren().add(h);
            }
        } else {
            getChildren().removeAll(handles);
        }
    }

    // ── Handle drag events ────────────────────────────────────────────────────

    private void setupHandleEvents(Rectangle handle) {
        handle.setOnMousePressed(e -> {
            if (editing) return;
            resizing         = true;
            activeHandle     = (String) handle.getUserData();
            resizeDragStartX = e.getSceneX();
            resizeDragStartY = e.getSceneY();
            resizeStartW     = backgroundShape.getLayoutBounds().getWidth();
            resizeStartH     = backgroundShape.getLayoutBounds().getHeight();
            resizeStartNodeX = getLayoutX();
            resizeStartNodeY = getLayoutY();
            toFront();
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (!resizing) return;
            double dx = e.getSceneX() - resizeDragStartX;
            double dy = e.getSceneY() - resizeDragStartY;

            double newW = resizeStartW;
            double newH = resizeStartH;
            double newX = resizeStartNodeX;
            double newY = resizeStartNodeY;

            switch (activeHandle) {
                case "SE": newW += dx;        newH += dy;        break;
                case "E":  newW += dx;                           break;
                case "S":                     newH += dy;        break;
                case "NW": newW -= dx; newX += dx; newH -= dy; newY += dy; break;
                case "N":              newH -= dy; newY += dy;  break;
                case "NE": newW += dx; newH -= dy; newY += dy;  break;
                case "SW": newW -= dx; newX += dx; newH += dy;  break;
                case "W":  newW -= dx; newX += dx;              break;
            }

            newW = Math.max(newW, MIN_WIDTH);
            newH = Math.max(newH, MIN_HEIGHT);

            element.setWidth(newW);
            element.setHeight(newH);
            element.setX(newX);
            element.setY(newY);

            setLayoutX(newX);
            setLayoutY(newY);
            applyShapeSize(newW, newH);
            layoutTextContainer(newW, newH);
            repositionHandles(newW, newH);
            setPrefSize(newW, newH);
            setMinSize(newW, newH);
            setMaxSize(newW, newH);

            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            resizing     = false;
            activeHandle = "";
            e.consume();
        });
    }

    // ── Indent helpers ────────────────────────────────────────────────────────

    /** Bullet character for each indent level (cycles after level 2). */
    private static String bulletForLevel(int level) {
        switch (level % 3) {
            case 0:  return "•";   // filled circle   – level 0, 3, 6 …
            case 1:  return "◦";   // open circle     – level 1, 4, 7 …
            default: return "▪";   // filled square   – level 2, 5, 8 …
        }
    }

    /**
     * Indent prefix for the read-only Text display node.
     * Uses em-spaces (U+2003) which are font-relative and visually wide enough
     * to show clear nesting — regular ASCII spaces are too narrow in JavaFX Text.
     * Two em-spaces per level gives a comfortable PowerPoint-like indent step.
     */
    private static String indentPrefix(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) sb.append("\u2003\u2003\u2003");  // 3 em-spaces per level
        return sb.toString();
    }

    // ── Text style ────────────────────────────────────────────────────────────

    private TextAlignment resolveTextAlignment() {
        switch (element.getAlignment()) {
            case "CENTER": return TextAlignment.CENTER;
            case "RIGHT":  return TextAlignment.RIGHT;
            default:       return TextAlignment.LEFT;
        }
    }

    /**
     * Rebuilds the display content inside textContainer.
     *
     * Two rendering paths are used depending on whether any line carries a
     * highlight background colour:
     *
     * PATH A — No highlights at all
     *   Uses the existing TextFlow with plain Text nodes.  TextFlow gives us
     *   correct inline text wrapping and alignment at no extra cost.
     *
     * PATH B — At least one line is highlighted
     *   Switches textContainer's content to a VBox of Label nodes, one per line.
     *   Labels are Region subclasses and therefore honour -fx-background-color in
     *   CSS, which is the only reliable way to paint a background behind text in
     *   JavaFX without the {OBJ} artefact that TextFlow produces for any non-Text
     *   child node (e.g. StackPane).
     *   Each Label also carries its own text colour, so highlighted and plain lines
     *   can have independent colours within the same element.
     */
    private void rebuildTextFlow() {
        FontWeight  fw = element.isBold()   ? FontWeight.BOLD    : FontWeight.NORMAL;
        FontPosture fp = element.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;
        Font font = Font.font(element.getFontFamily(), fw, fp, element.getFontSize());

        String rawText        = element.getText();
        boolean isPlaceholder = element.isPlaceholder();

        // ── Placeholder / empty ───────────────────────────────────────────────
        if (isPlaceholder || rawText == null || rawText.isEmpty()) {
            textFlow.getChildren().clear();
            Text hint = new Text(isPlaceholder && rawText != null ? rawText : "");
            hint.setFont(Font.font(element.getFontFamily(), FontWeight.NORMAL,
                    FontPosture.ITALIC, element.getFontSize()));
            hint.setFill(Color.web("#aaaaaa"));
            hint.setCursor(Cursor.HAND);
            textFlow.getChildren().add(hint);
            textContainer.getChildren().setAll(textFlow);
            return;
        }

        String[] lines     = rawText.split("\n", -1);
        String   textColor = element.getColor();

        // ── Decide which path to use ──────────────────────────────────────────
        boolean anyHighlight = false;
        for (int i = 0; i < lines.length; i++) {
            String hc = element.getLineHighlightColor(i);
            if (hc != null && !hc.isEmpty()) { anyHighlight = true; break; }
        }

        if (!anyHighlight) {
            // ── PATH A: plain TextFlow ────────────────────────────────────────
            textFlow.getChildren().clear();
            for (int i = 0; i < lines.length; i++) {
                String lineDisplay = buildLineDisplay(lines[i], i);
                Text span = new Text(lineDisplay);
                span.setFont(font);
                span.setFill(Color.web(textColor));
                span.setCursor(Cursor.HAND);
                textFlow.getChildren().add(span);
                if (i < lines.length - 1) {
                    Text nl = new Text("\n");
                    nl.setFont(font);
                    textFlow.getChildren().add(nl);
                }
            }
            textContainer.getChildren().setAll(textFlow);

        } else {
            // ── PATH B: VBox of Labels ────────────────────────────────────────
            // Build the font CSS fragment once — Label uses CSS for font styling.
            String fontWeight  = element.isBold()   ? "bold"   : "normal";
            String fontStyle   = element.isItalic() ? "italic" : "normal";
            double containerW  = textContainer.getPrefWidth();
            // Fall back to element width (minus padding) when the container hasn't
            // been laid out yet (e.g. on first construction).
            if (containerW <= 0) {
                double ew = element.getWidth();
                containerW = ew > 0 ? ew - TEXT_PADDING * 2 : MIN_WIDTH;
            }

            // Resolve text alignment for Label
            javafx.geometry.Pos labelPos;
            switch (element.getAlignment()) {
                case "CENTER": labelPos = Pos.CENTER_LEFT; break;
                case "RIGHT":  labelPos = Pos.CENTER_RIGHT; break;
                default:       labelPos = Pos.TOP_LEFT;     break;
            }

            VBox vbox = new VBox(0);
            vbox.setCursor(Cursor.HAND);
            vbox.setFillWidth(true);

            for (int i = 0; i < lines.length; i++) {
                String lineDisplay = buildLineDisplay(lines[i], i);

                String highlightColor     = element.getLineHighlightColor(i);
                String highlightTextColor = element.getLineHighlightTextColor(i);

                boolean hasHighlight = highlightColor != null && !highlightColor.isEmpty();
                String  lineTextColor = (hasHighlight
                        && highlightTextColor != null && !highlightTextColor.isEmpty())
                        ? highlightTextColor : textColor;

                Label lbl = new Label(lineDisplay);
                lbl.setWrapText(true);
                lbl.setMaxWidth(containerW);
                lbl.setPrefWidth(containerW);
                lbl.setCursor(Cursor.HAND);
                lbl.setAlignment(labelPos);

                // Font + text colour via inline CSS (Label honours these reliably)
                String bgCss = hasHighlight
                        ? "-fx-background-color: " + highlightColor + ";"
                        : "-fx-background-color: transparent;";
                lbl.setStyle(
                    "-fx-font-size: "    + element.getFontSize()   + "px;" +
                    "-fx-font-family: '" + element.getFontFamily() + "';" +
                    "-fx-font-weight: "  + fontWeight + ";" +
                    "-fx-font-style: "   + fontStyle  + ";" +
                    "-fx-text-fill: "    + lineTextColor + ";" +
                    bgCss
                );

                vbox.getChildren().add(lbl);
            }
            textContainer.getChildren().setAll(vbox);
        }
    }

    /**
     * Build the display string for one line: indent prefix + bullet (if any) + content.
     * Blank or whitespace-only lines are always returned as empty strings in bullet
     * mode — this prevents orphan bullet dots appearing on separator/empty lines.
     */
    private String buildLineDisplay(String lineContent, int lineIndex) {
        if (!element.isBulletList()) {
            int lvl = element.getIndentLevel(lineIndex);
            return lvl > 0 ? indentPrefix(lvl) + lineContent : lineContent;
        }
        // Bullet mode: never show a bullet character on a line that has no real content
        if (lineContent == null || lineContent.trim().isEmpty()) return "";
        int lvl = element.getIndentLevel(lineIndex);
        return indentPrefix(lvl) + bulletForLevel(lvl) + " " + lineContent;
    }

    private void updateTextStyle() {
        rebuildTextFlow();
        applyFillColors();
        applySize();
    }

    /**
     * Apply the shape fill and text-container fill from the element model.
     * Called whenever colours change and on initial construction.
     */
    public void applyFillColors() {
        // Outer shape fill
        backgroundShape.setFill(Color.web(element.getShapeFillColor()));

        // Inner text container fill
        String tbFill = element.getTextBoxFillColor();
        if ("transparent".equalsIgnoreCase(tbFill)) {
            textContainer.setBackground(Background.EMPTY);
        } else {
            textContainer.setBackground(new Background(
                new BackgroundFill(Color.web(tbFill), CornerRadii.EMPTY, Insets.EMPTY)
            ));
        }
    }

    // ── Selection style ───────────────────────────────────────────────────────

    private void updateSelectionStyle(boolean isSelected) {
        if (isSelected) {
            // Selected state: solid green border + glow + handles (same for all elements)
            backgroundShape.setStroke(Color.web("#4CAF50"));
            backgroundShape.setStrokeWidth(2.5);
            backgroundShape.getStrokeDashArray().clear();
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#4CAF50", 0.4));
            glow.setRadius(10);
            setEffect(glow);
            showHandles(true);
        } else {
            // Deselected state:
            //   • Placeholders keep their dashed grey hint border so users can
            //     see where to click (matching PowerPoint's placeholder behaviour).
            //   • Regular shapes have a fully transparent border — invisible until
            //     selected, just like PowerPoint.
            setEffect(null);
            showHandles(false);
            if (element.isPlaceholder()) {
                backgroundShape.setStroke(Color.web("#aaaaaa"));
                backgroundShape.setStrokeWidth(1.5);
                backgroundShape.getStrokeDashArray().setAll(6.0, 4.0);
            } else {
                backgroundShape.setStroke(Color.TRANSPARENT);
                backgroundShape.setStrokeWidth(0);
                backgroundShape.getStrokeDashArray().clear();
            }
        }
    }

    // ── Background shape creation ─────────────────────────────────────────────

    private void createBackgroundShape() {
        String st = element.getShapeType();
        if (st == null) st = "rectangle";

        switch (st) {
            case "rounded":
                Rectangle rr = new Rectangle(120, 70);
                rr.setArcWidth(15); rr.setArcHeight(15);
                rr.setFill(Color.web(element.getShapeFillColor())); rr.setStroke(Color.web("#cccccc")); rr.setStrokeWidth(1.5);
                backgroundShape = rr; break;
            case "circle":
                Circle c = new Circle(60);
                c.setCenterX(60); c.setCenterY(60);
                c.setFill(Color.web(element.getShapeFillColor())); c.setStroke(Color.web("#cccccc")); c.setStrokeWidth(1.5);
                backgroundShape = c; break;
            case "callout":
                Polygon callout = new Polygon();
                callout.setFill(Color.web(element.getShapeFillColor())); callout.setStroke(Color.web("#cccccc")); callout.setStrokeWidth(1.5);
                backgroundShape = callout; updateCalloutShape(120, 70); break;
            case "diamond":
                Polygon diamond = new Polygon();
                diamond.setFill(Color.web(element.getShapeFillColor())); diamond.setStroke(Color.web("#cccccc")); diamond.setStrokeWidth(1.5);
                backgroundShape = diamond; updateDiamondShape(120, 70); break;
            case "pentagon":
                Polygon pentagon = new Polygon();
                pentagon.setFill(Color.web(element.getShapeFillColor())); pentagon.setStroke(Color.web("#cccccc")); pentagon.setStrokeWidth(1.5);
                backgroundShape = pentagon; updatePentagonShape(120, 70); break;
            case "hexagon":
                Polygon hexagon = new Polygon();
                hexagon.setFill(Color.web(element.getShapeFillColor())); hexagon.setStroke(Color.web("#cccccc")); hexagon.setStrokeWidth(1.5);
                backgroundShape = hexagon; updateHexagonShape(120, 70); break;
            case "star":
                Polygon star = new Polygon();
                star.setFill(Color.web(element.getShapeFillColor())); star.setStroke(Color.web("#cccccc")); star.setStrokeWidth(1.5);
                backgroundShape = star; updateStarShape(120, 70); break;
            case "parallelogram":
                Polygon para = new Polygon();
                para.setFill(Color.web(element.getShapeFillColor())); para.setStroke(Color.web("#cccccc")); para.setStrokeWidth(1.5);
                backgroundShape = para; updateParallelogramShape(120, 70); break;
            default:
                Rectangle rect = new Rectangle(120, 70);
                rect.setFill(Color.web(element.getShapeFillColor())); rect.setStroke(Color.web("#cccccc")); rect.setStrokeWidth(1.5);
                backgroundShape = rect; break;
        }
        backgroundShape.setLayoutX(0);
        backgroundShape.setLayoutY(0);
        backgroundShape.setPickOnBounds(false);
    }

    // ── Polygon shape updaters ────────────────────────────────────────────────

    private void updateCalloutShape(double width, double height) {
        if (!(backgroundShape instanceof Polygon)) return;
        Polygon p = (Polygon) backgroundShape;
        double w = Math.max(width, 80), h = Math.max(height, 50), px = w * 0.7;
        p.getPoints().setAll(0.0,0.0, w,0.0, w,h-20, px,h-20, px-15,h, px-30,h-20, 0.0,h-20);
    }

    private void updateDiamondShape(double width, double height) {
        if (!(backgroundShape instanceof Polygon)) return;
        Polygon p = (Polygon) backgroundShape;
        double w = Math.max(width, 80), h = Math.max(height, 60);
        p.getPoints().setAll(w/2,0.0, w,h/2, w/2,h, 0.0,h/2);
    }

    private void updatePentagonShape(double width, double height) {
        if (!(backgroundShape instanceof Polygon)) return;
        Polygon p = (Polygon) backgroundShape;
        double w = Math.max(width, 80), h = Math.max(height, 70);
        p.getPoints().setAll(w/2,0.0, w,h*0.4, w*0.8,h, w*0.2,h, 0.0,h*0.4);
    }

    private void updateHexagonShape(double width, double height) {
        if (!(backgroundShape instanceof Polygon)) return;
        Polygon p = (Polygon) backgroundShape;
        double w = Math.max(width, 80), h = Math.max(height, 70);
        p.getPoints().setAll(w*0.25,0.0, w*0.75,0.0, w,h/2, w*0.75,h, w*0.25,h, 0.0,h/2);
    }

    private void updateStarShape(double width, double height) {
        if (!(backgroundShape instanceof Polygon)) return;
        Polygon p = (Polygon) backgroundShape;
        double w = Math.max(width, 80), h = Math.max(height, 70);
        p.getPoints().setAll(
            w/2,0.0, w*0.6,h*0.35, w,h*0.4, w*0.7,h*0.6,
            w*0.8,h, w/2,h*0.75, w*0.2,h, w*0.3,h*0.6,
            0.0,h*0.4, w*0.4,h*0.35);
    }

    private void updateParallelogramShape(double width, double height) {
        if (!(backgroundShape instanceof Polygon)) return;
        Polygon p = (Polygon) backgroundShape;
        double w = Math.max(width, 80), h = Math.max(height, 60), sk = w * 0.2;
        p.getPoints().setAll(sk,0.0, w,0.0, w-sk,h, 0.0,h);
    }

    private void updatePolygonShape(double w, double h) {
        String st = element.getShapeType();
        if (st == null) return;
        switch (st) {
            case "callout":       updateCalloutShape(w, h);       break;
            case "diamond":       updateDiamondShape(w, h);       break;
            case "pentagon":      updatePentagonShape(w, h);      break;
            case "hexagon":       updateHexagonShape(w, h);       break;
            case "star":          updateStarShape(w, h);          break;
            case "parallelogram": updateParallelogramShape(w, h); break;
        }
    }

    // ── Main event handlers (move + select + hover) ───────────────────────────

    private void setupMainEventHandlers() {

        setOnMouseClicked(e -> {
            if (resizing) { e.consume(); return; }
            if (e.getClickCount() == 2 && !editing) {
                startEditing(); e.consume();
            } else if (e.getClickCount() == 1 && !editing) {
                controller.selectElement(element);
                updateSelectionStyle(true);
                selected = true;
                toFront();
                e.consume();
            }
        });

        setOnMousePressed(e -> {
            if (resizing || editing) return;
            dragStartX = e.getSceneX();
            dragStartY = e.getSceneY();
            nodeStartX = getLayoutX();
            nodeStartY = getLayoutY();
            setCursor(Cursor.MOVE);
            toFront();
            e.consume();
        });

        setOnMouseDragged(e -> {
            if (resizing || editing) return;
            double newX = nodeStartX + (e.getSceneX() - dragStartX);
            double newY = nodeStartY + (e.getSceneY() - dragStartY);
            setLayoutX(newX);
            setLayoutY(newY);
            element.setX(newX);
            element.setY(newY);
            e.consume();
        });

        setOnMouseReleased(e -> {
            if (resizing || editing) return;
            // Restore to HAND once the drag is finished
            setCursor(Cursor.HAND);
            e.consume();
        });

        setOnMouseEntered(e -> {
            if (!selected && !editing)
                backgroundShape.setStroke(Color.web("#4CAF50", 0.6));
            if (!editing)
                setCursor(Cursor.HAND);
        });
        setOnMouseExited(e -> {
            if (!selected && !editing) {
                if (element.isPlaceholder()) {
                    backgroundShape.setStroke(Color.web("#aaaaaa"));
                    backgroundShape.getStrokeDashArray().setAll(6.0, 4.0);
                } else {
                    backgroundShape.setStroke(Color.TRANSPARENT);
                    backgroundShape.setStrokeWidth(0);
                    backgroundShape.getStrokeDashArray().clear();
                }
            }
        });
    }

    // ── Inline text editing ───────────────────────────────────────────────────

    /**
     * Replace the text container's content with a TextArea sized to match
     * the container exactly – this is the PowerPoint "click inside the shape
     * to type" experience.
     */
    private void startEditing() {
        if (editing) return;
        editing = true;
        showHandles(false);

        // Ensure the controller knows this element is selected while editing,
        // so the formatting toolbar (including alignment buttons) stays enabled.
        if (!selected) {
            controller.selectElement(element);
            updateSelectionStyle(true);
            selected = true;
        }

        setCursor(Cursor.TEXT);

        // Show the PowerPoint-style dashed blue border around the text container
        containerBorderRect.setVisible(true);

        double containerW = textContainer.getWidth();
        double containerH = textContainer.getHeight();
        // Fall back to element dimensions if the container hasn't been laid out yet
        double pw = containerW > 0 ? containerW : Math.max(200, element.getWidth()  - TEXT_PADDING * 2);
        double ph = containerH > 0 ? containerH : Math.max( 60, element.getHeight() - TEXT_PADDING * 2);

        // Build the TextArea's initial content.
        // Placeholders start empty (or with a single bullet) — matching PowerPoint.
        // Non-placeholder elements are encoded with tab-based indent + bullet prefix.
        String initialText;
        if (element.isPlaceholder()) {
            initialText = element.isBulletList() ? "\u2022 " : "";
        } else {
            initialText = buildBulletedEditText(element.getText());
        }

        TextArea textArea = new TextArea(initialText);
        textArea.setPrefSize(pw, ph);
        textArea.setMinSize(pw, ph);
        textArea.setMaxSize(pw, ph);
        textArea.setWrapText(true);
        // Position caret at end so the user can type immediately after the bullet
        textArea.positionCaret(initialText.length());
        // Use the element's real text colour (black by default) while editing
        textArea.setStyle(
            "-fx-font-size: "    + element.getFontSize()   + "px;" +
            "-fx-font-family: '" + element.getFontFamily() + "';" +
            "-fx-text-fill: "    + element.getColor()      + ";" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;"
        );

        // Swap the Text node for the TextArea inside the container
        textContainer.getChildren().setAll(textArea);
        liveTextArea = textArea;
        textArea.requestFocus();

        textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                javafx.scene.Node focusOwner = textArea.getScene() != null
                        ? textArea.getScene().getFocusOwner() : null;
                System.out.println("[FOCUS-LOST] focusOwner=" +
                    (focusOwner == null ? "null" :
                     focusOwner.getClass().getSimpleName() +
                     " userData=" + focusOwner.getUserData()));

                // ── Indent buttons ────────────────────────────────────────────
                if (focusOwner instanceof javafx.scene.control.Button) {
                    Object userData = focusOwner.getUserData();
                    if ("increase".equals(userData)) {
                        adjustLineIndent(textArea, +1);
                        return;
                    } else if ("decrease".equals(userData)) {
                        adjustLineIndent(textArea, -1);
                        return;
                    }
                }

                // ── Colour pickers: deferred check ────────────────────────────
                final String snapshot = textArea.getText();
                System.out.println("[FOCUS-LOST] snapshot length=" + snapshot.length() +
                    "  scheduling runLater check...");
                javafx.application.Platform.runLater(() -> {
                    boolean highlightOpen = controller.isHighlightPickerShowing();
                    boolean fontColorOpen = controller.isFontColorPickerShowing();
                    System.out.println("[RUNLATER] highlightOpen=" + highlightOpen +
                        "  fontColorOpen=" + fontColorOpen +
                        "  suppress=" + suppressNextFinishEditing +
                        "  editing=" + editing);
                    if (highlightOpen) return;
                    if (fontColorOpen) return;
                    if (suppressNextFinishEditing) {
                        suppressNextFinishEditing = false;
                        System.out.println("[RUNLATER] --> suppressed: commit+reopen editing");
                        // Commit the current TextArea text (saves it to element model
                        // and renders highlights), then immediately re-open editing
                        // so the user can continue typing without losing focus.
                        if (editing) {
                            finishEditing(snapshot);
                            // finishEditing sets editing=false; re-open it
                            startEditing();
                        }
                        return;
                    }
                    if (editing) {
                        System.out.println("[RUNLATER] --> calling finishEditing");
                        finishEditing(snapshot);
                    }
                });
            }
        });
        textArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.CONTROL) return;

            if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                // Ctrl+Enter → commit
                finishEditing(textArea.getText());

            } else if (e.getCode() == KeyCode.ESCAPE) {
                finishEditing(element.getText());

            } else if (e.getCode() == KeyCode.ENTER && element.isBulletList()) {
                // Enter in bullet mode → new line inheriting current indent + bullet
                e.consume();
                int caret  = textArea.getCaretPosition();
                String all = textArea.getText();
                // Find the start of the current line to count its leading tabs
                int lineStart = all.lastIndexOf('\n', caret - 1) + 1;
                String curLine = all.substring(lineStart, caret);
                int lvl = 0;
                while (lvl < curLine.length() && curLine.charAt(lvl) == '\t') lvl++;
                // Build the new-line prefix: tabs + bullet
                StringBuilder prefix = new StringBuilder("\n");
                for (int t = 0; t < lvl; t++) prefix.append('\t');
                if (element.isBulletList()) prefix.append(bulletForLevel(lvl)).append(' ');
                String inserted = prefix.toString();
                textArea.setText(all.substring(0, caret) + inserted + all.substring(caret));
                textArea.positionCaret(caret + inserted.length());

            } else if (e.getCode() == KeyCode.TAB && !e.isShiftDown()) {
                // Tab → increase indent of the current line
                e.consume();
                adjustLineIndent(textArea, +1);

            } else if (e.getCode() == KeyCode.TAB && e.isShiftDown()) {
                // Shift+Tab → decrease indent of the current line
                e.consume();
                adjustLineIndent(textArea, -1);
            }
        });
    }

    /**
     * Convert stored plain text into the TextArea's editing representation.
     * Each line is prefixed with '\t' characters equal to its indent level,
     * then a bullet character + space if bulletList is on.
     * This encoding lets us round-trip indent levels through the TextArea.
     *
     * Format per line:  <TAB×level><bullet> <text>   (bullet mode)
     *                   <TAB×level><text>             (non-bullet mode)
     */
    private String buildBulletedEditText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return element.isBulletList() ? "\u2022 " : "";
        }
        StringBuilder sb = new StringBuilder();
        String[] lines = raw.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int lvl = element.getIndentLevel(i);
            // indent tabs
            for (int t = 0; t < lvl; t++) sb.append('\t');
            if (element.isBulletList()) {
                sb.append(bulletForLevel(lvl)).append(' ');
            }
            sb.append(lines[i]);
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Parse the TextArea content back into (plain text, indentLevels[]).
     * Each line may start with any number of '\t' characters (the indent),
     * optionally followed by a bullet char + space (stripped in bullet mode).
     * Returns the plain-text string; writes the recovered levels into the element.
     */
    private String stripBulletsAndRecoverIndent(String editText) {
        if (editText == null) return "";
        String[] lines = editText.split("\n", -1);
        int[] levels = new int[lines.length];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Count leading tabs → indent level
            int lvl = 0;
            while (lvl < line.length() && line.charAt(lvl) == '\t') lvl++;
            levels[i] = Math.min(lvl, 8);
            line = line.substring(lvl); // strip tabs
            // Strip bullet prefix if present (any of our three bullet chars).
            // Accept both "• text" (bullet+space+text) and "•" (lone bullet on empty line).
            if (element.isBulletList() && line.length() >= 1) {
                char first = line.charAt(0);
                if (first == '\u2022' || first == '\u25E6' || first == '\u25AA') {
                    // Strip the bullet and any immediately following space
                    line = (line.length() >= 2 && line.charAt(1) == ' ')
                            ? line.substring(2)
                            : line.substring(1);
                }
            }
            sb.append(line);
            if (i < lines.length - 1) sb.append("\n");
        }
        element.setIndentLevels(levels);
        return sb.toString();
    }

    /**
     * Legacy alias kept so finishEditing compiles — delegates to the full version.
     */
    private String stripBullets(String bulleted) {
        return stripBulletsAndRecoverIndent(bulleted);
    }

    /**
     * Increase (delta=+1) or decrease (delta=-1) the indent of every line that
     * is touched by the current selection (or just the caret line if nothing is
     * selected) — matching PowerPoint’s Tab / Shift+Tab behaviour exactly.
     *
     * Fixes vs. previous version:
     *  • Strict overlap check so a caret at a line boundary never double-indents.
     *  • selStart/selEnd shifts computed independently for caret and range cases.
     *  • Outdenting (tabDiff < 0) shifts caret correctly without going past line start.
     */
    private void adjustLineIndent(TextArea textArea, int delta) {
        String all = textArea.getText();
        if (all == null) all = "";

        javafx.scene.control.IndexRange sel = textArea.getSelection();
        int selStart = sel.getStart();
        int selEnd   = sel.getEnd();
        boolean hasSelection = (selStart != selEnd);
        if (!hasSelection) {
            selStart = textArea.getCaretPosition();
            selEnd   = selStart;
        }

        // Split into lines, recording the absolute start offset of each line
        String[] lines       = all.split("\n", -1);
        int[]    lineOffsets = new int[lines.length];
        int offset = 0;
        for (int i = 0; i < lines.length; i++) {
            lineOffsets[i] = offset;
            offset += lines[i].length() + 1; // +1 for '\n'
        }

        StringBuilder rebuilt  = new StringBuilder();
        int charsDelta = 0; // shift accumulated before selStart
        int charsAfter = 0; // shift accumulated between selStart and selEnd

        for (int i = 0; i < lines.length; i++) {
            int lineStart = lineOffsets[i];
            int lineEnd   = lineStart + lines[i].length(); // exclusive (before \n)

            if (i > 0) rebuilt.append('\n');

            // Strict overlap: caret sitting at lineEnd belongs to the next line
            boolean touched;
            if (!hasSelection) {
                touched = (selStart >= lineStart && selStart <= lineEnd);
                if (selStart == lineEnd && i < lines.length - 1) touched = false;
            } else {
                touched = (lineStart < selEnd) && (lineEnd > selStart);
            }

            if (!touched) {
                rebuilt.append(lines[i]);
                continue;
            }

            String line = lines[i];

            // Count existing leading tabs
            int tabs = 0;
            while (tabs < line.length() && line.charAt(tabs) == '\t') tabs++;
            int newTabs = Math.max(0, Math.min(8, tabs + delta));
            int tabDiff = newTabs - tabs;

            // Build the new line
            StringBuilder newLine = new StringBuilder();
            for (int t = 0; t < newTabs; t++) newLine.append('\t');
            String afterTabs = line.substring(tabs);

            // Swap bullet character for the new level in bullet mode
            if (element.isBulletList() && afterTabs.length() >= 1) {
                char first = afterTabs.charAt(0);
                if (first == '\u2022' || first == '\u25E6' || first == '\u25AA') {
                    afterTabs = bulletForLevel(newTabs) + afterTabs.substring(1);
                }
            }
            newLine.append(afterTabs);
            rebuilt.append(newLine);

            // ── Caret/selection shift accounting ─────────────────────────────
            if (lineEnd <= selStart) {
                // This line is entirely before selStart
                charsDelta += tabDiff;
            } else if (lineStart <= selStart) {
                // selStart is ON this line — shift it, but not past line start
                int maxRemove = selStart - lineStart;
                charsDelta += (tabDiff < 0) ? Math.max(tabDiff, -maxRemove) : tabDiff;
                // Lines past selStart but inside the selection contribute to charsAfter
                charsAfter += tabDiff;
            } else {
                // This line is entirely inside the selection (after selStart)
                charsAfter += tabDiff;
            }
        }

        String newText = rebuilt.toString();
        textArea.setText(newText);

        int newLen = newText.length();
        int ns = Math.max(0, Math.min(newLen, selStart + charsDelta));
        int ne = Math.max(ns,  Math.min(newLen, selEnd   + charsDelta + charsAfter));

        if (!hasSelection || ns == ne) {
            textArea.positionCaret(ns);
        } else {
            textArea.selectRange(ns, ne);
        }
    }

    private void finishEditing(String rawFromTextArea) {
        System.out.println("[FINISH-EDITING] called, raw length=" +
            (rawFromTextArea == null ? "null" : rawFromTextArea.length()) +
            "  editing=" + editing);
        // Parse the TextArea text: recover indent levels AND strip bullet prefixes.
        // stripBulletsAndRecoverIndent writes the recovered levels into the element.
        String newText = stripBulletsAndRecoverIndent(rawFromTextArea);

        // For placeholders: if the user typed nothing meaningful, restore the
        // hint text so the dashed placeholder box reappears — matching PowerPoint.
        boolean isEmpty = newText.trim().isEmpty();
        if (element.isPlaceholder() && isEmpty) {
            editing = false;
            liveTextArea = null;
            setCursor(Cursor.HAND);
            containerBorderRect.setVisible(false);
            textContainer.getChildren().setAll(textFlow);
            updateTextStyle();
            if (selected) updateSelectionStyle(true);
            return;
        }
        // Clear placeholder flag BEFORE syncing so syncHighlightColors sizes
        // the array to the actual new line count, not the old placeholder text.
        if (!isEmpty) {
            if (element.isPlaceholder()) element.setPlaceholder(false);
            element.setRelativeLayout(false);
        }
        // Sync indent levels and highlight colours to the final line count
        element.setText(newText);
        element.syncIndentLevels();
        element.syncHighlightColors();
        element.syncHighlightTextColors();
        controller.updateTextElement(element, newText);
        editing = false;
        liveTextArea = null;
        setCursor(Cursor.HAND);
        containerBorderRect.setVisible(false);
        textContainer.getChildren().setAll(textFlow);
        updateTextStyle();
        if (selected) updateSelectionStyle(true);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public TextElement getElement() { return element; }

    /** True while the inline TextArea is open (double-click edit mode). */
    public boolean isEditing() { return editing; }

    /**
     * Re-styles the live TextArea's text colour without exiting editing mode.
     * Called by the controller's changeFontColor() when a colour is chosen
     * while the TextArea is open, so the user sees the colour change immediately.
     */
    public void applyLiveFontColor(String hexColor) {
        if (!editing || liveTextArea == null) return;
        liveTextArea.setStyle(
            "-fx-font-size: "    + element.getFontSize()   + "px;" +
            "-fx-font-family: '" + element.getFontFamily() + "';" +
            "-fx-text-fill: "    + hexColor                + ";" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;"
        );
    }

    /**
     * Restores focus to the live TextArea after a colour picker popup closes,
     * so editing continues without the user needing to click back in.
     * Called via Platform.runLater from the controller after colour is applied.
     */
    public void requestEditingFocus() {
        if (editing && liveTextArea != null) {
            liveTextArea.requestFocus();
        }
    }

    /**
     * Called from the indent button's onMousePressed handler — BEFORE JavaFX
     * transfers focus away from the TextArea.  At this moment editing==true and
     * liveTextArea is still set, so we read the live selection directly and apply
     * the indent.  We return true so the caller can consume the mouse event and
     * prevent onAction from firing a second time.
     *
     * If not currently editing, returns false and does nothing.
     */
    public boolean applyIndentFromToolbar(int delta) {
        System.out.println("[INDENT] applyIndentFromToolbar called, delta=" + delta
                + ", editing=" + editing + ", liveTextArea=" + (liveTextArea != null ? "SET" : "NULL"));
        if (!editing || liveTextArea == null) return false;
        System.out.println("[INDENT] Selection: start=" + liveTextArea.getSelection().getStart()
                + " end=" + liveTextArea.getSelection().getEnd());
        adjustLineIndent(liveTextArea, delta);
        return true;
    }

    /**
     * Applies or clears a highlight colour on all lines touched by the current
    /**
     * Applies or clears a highlight background colour AND a text colour on all
     * lines touched by the current TextArea selection.
     * Called from the controller's highlight picker onAction (both pickers share
     * the same handler so both values are always applied together).
     * Pass null for both arguments to clear highlights on the selected lines.
     */
    public void applyHighlightToSelectedLines(String color, String textColor) {
        if (liveTextArea == null) return;
        TextArea textArea = liveTextArea;
        String all = textArea.getText();
        if (all == null) all = "";

        javafx.scene.control.IndexRange sel = textArea.getSelection();
        int selStart     = sel.getStart();
        int selEnd       = sel.getEnd();
        boolean hasSel   = (selStart != selEnd);
        if (!hasSel) {
            // No selection — apply to caret line only
            selStart = textArea.getCaretPosition();
            selEnd   = selStart;
        }

        // Split edit text into lines and compute offsets
        String[] editLines    = all.split("\n", -1);
        int[]    lineOffsets  = new int[editLines.length];
        int off = 0;
        for (int i = 0; i < editLines.length; i++) {
            lineOffsets[i] = off;
            off += editLines[i].length() + 1;
        }

        // Do NOT call syncHighlightColors here — it resets the array to match
        // element.getText() (the OLD saved text during editing), wiping any colours
        // we are about to set. Syncing happens in finishEditing after setText().

        for (int i = 0; i < editLines.length; i++) {
            int lineStart = lineOffsets[i];
            int lineEnd   = lineStart + editLines[i].length();

            boolean touched;
            if (!hasSel) {
                touched = (selStart >= lineStart && selStart <= lineEnd);
                if (selStart == lineEnd && i < editLines.length - 1) touched = false;
            } else {
                touched = (lineStart < selEnd) && (lineEnd > selStart);
            }
            if (!touched) continue;
            // NOTE: no guard against element.getText() line count — during editing
            // element.getText() is the OLD saved text, so the guard skips all lines.

            System.out.println("[APPLY-H] setting line[" + i + "] bg=" + color + " text=" + textColor);
            element.setLineHighlightColor(i, color);
            element.setLineHighlightTextColor(i, textColor);
        }
        if (!editing) {
            rebuildTextFlow();
        } else {
            // While editing, the picker's onAction fires and then the TextArea
            // immediately loses focus (picker closes). The runLater check would
            // normally call finishEditing at that point — but the highlight has
            // just been applied and we want to stay in editing mode. Set this
            // flag so the next runLater cycle skips finishEditing and instead
            // returns focus to the TextArea, keeping editing alive.
            suppressNextFinishEditing = true;
            System.out.println("[HIGHLIGHT] set suppressNextFinishEditing=true");
        }
    }
    /**
     * Called by the controller (e.g. when the slide background is clicked) to
     * clear the visual selection state without going through selectElement().
     */
    public void deselect() {
        if (selected) {
            selected = false;
            updateSelectionStyle(false);
        }
    }

    /**
     * Called by the controller after updateSlideDisplay() rebuilds all nodes,
     * to re-apply the green selection border on the node that matches the
     * still-selected element.  Keeps the toolbar enabled during and after editing.
     */
    public void restoreSelection() {
        selected = true;
        updateSelectionStyle(true);
        toFront();
    }
}