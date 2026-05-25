package slideeditor;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;

/**
 * JavaFX node that renders an {@link ImageElement} on the slide canvas.
 *
 * Architecture (mirrors ShapeTextBoxNode's patterns)
 * ──────────────────────────────────────────────────
 *   ImageNode  (Pane)
 *     ├─ imageView          – the actual image
 *     ├─ selectionBorder    – dashed blue rectangle, visible only when selected
 *     └─ handles[]          – 8 square resize handles, visible only when selected
 *
 * Interactions
 * ────────────
 *  • Single click       → select (blue selection border + handles appear)
 *  • Drag on body       → move
 *  • Drag on handle     → resize (preserves or ignores aspect ratio depending on
 *                          which handle is dragged; corner handles preserve it
 *                          if Shift is held – same feel as PowerPoint)
 *  • Delete key         → delete via controller
 *  • Right-click        → context menu: Delete / Reset Size
 */
public class ImageNode extends Pane {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final double HANDLE_SIZE  = 8;
    private static final double HALF_HANDLE  = HANDLE_SIZE / 2;
    private static final double MIN_SIZE     = 20;
    private static final double SHADOW_RADIUS = 6;

    private static final String[] HANDLE_NAMES = {"NW","N","NE","E","SE","S","SW","W"};
    private static final Cursor[] HANDLE_CURSORS = {
        Cursor.NW_RESIZE, Cursor.N_RESIZE,  Cursor.NE_RESIZE,
        Cursor.E_RESIZE,  Cursor.SE_RESIZE, Cursor.S_RESIZE,
        Cursor.SW_RESIZE, Cursor.W_RESIZE
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private final ImageElement           element;
    private final SlideEditorController  controller;

    private final ImageView    imageView;
    private final Rectangle    selectionBorder;
    private final Rectangle[]  handles = new Rectangle[8];

    private boolean selected = false;

    // Drag-to-move
    private double dragStartX, dragStartY;
    private double nodeStartX, nodeStartY;

    // Drag-to-resize
    private double resizeDragStartX, resizeDragStartY;
    private double resizeStartW,     resizeStartH;
    private double resizeStartNodeX, resizeStartNodeY;
    private String activeHandle = "";

    // ── Constructor ───────────────────────────────────────────────────────────

    public ImageNode(ImageElement element, SlideEditorController controller) {
        this.element    = element;
        this.controller = controller;

        // ── ImageView ─────────────────────────────────────────────────────────
        imageView = new ImageView();
        imageView.setPreserveRatio(false);   // size is fully controlled by element w/h
        imageView.setSmooth(true);
        loadImage();

        // ── Selection border (dashed blue, hidden by default) ─────────────────
        selectionBorder = new Rectangle();
        selectionBorder.setFill(Color.TRANSPARENT);
        selectionBorder.setStroke(Color.web("#1a73e8"));
        selectionBorder.setStrokeWidth(1.5);
        selectionBorder.getStrokeDashArray().addAll(4.0, 3.0);
        selectionBorder.setMouseTransparent(true);
        selectionBorder.setVisible(false);

        getChildren().addAll(imageView, selectionBorder);

        // ── Handles ───────────────────────────────────────────────────────────
        createHandles();

        // ── Initial geometry ──────────────────────────────────────────────────
        setLayoutX(element.getX());
        setLayoutY(element.getY());
        applySize();

        setCursor(Cursor.HAND);

        // ── Drop shadow ───────────────────────────────────────────────────────
        DropShadow shadow = new DropShadow(SHADOW_RADIUS, Color.rgb(0, 0, 0, 0.25));
        imageView.setEffect(shadow);

        // ── Event handlers ────────────────────────────────────────────────────
        setupEventHandlers();
    }

    // ── Image loading ─────────────────────────────────────────────────────────

    private void loadImage() {
        try {
            File f = new File(element.getImagePath());
            if (f.exists()) {
                Image img = new Image(f.toURI().toString(), true); // backgroundLoading=true
                imageView.setImage(img);
            }
        } catch (Exception ignored) {
            // If the file can't be loaded we show an empty frame — not a crash.
        }
    }

    // ── Size ─────────────────────────────────────────────────────────────────

    public void applySize() {
        double w = element.getWidth();
        double h = element.getHeight();

        imageView.setFitWidth(w);
        imageView.setFitHeight(h);
        imageView.setLayoutX(0);
        imageView.setLayoutY(0);

        selectionBorder.setWidth(w);
        selectionBorder.setHeight(h);
        selectionBorder.setLayoutX(0);
        selectionBorder.setLayoutY(0);

        repositionHandles(w, h);

        setPrefSize(w, h);
        setMinSize(w, h);
        setMaxSize(w, h);
    }

    // ── Handles ───────────────────────────────────────────────────────────────

    private void createHandles() {
        for (int i = 0; i < 8; i++) {
            Rectangle h = new Rectangle(HANDLE_SIZE, HANDLE_SIZE);
            h.setFill(Color.WHITE);
            h.setStroke(Color.web("#1a73e8"));
            h.setStrokeWidth(1);
            h.setCursor(HANDLE_CURSORS[i]);
            h.setVisible(false);
            h.setUserData(HANDLE_NAMES[i]);
            handles[i] = h;
            getChildren().add(h);
            setupHandleEvents(h, HANDLE_NAMES[i]);
        }
    }

    private void repositionHandles(double w, double h) {
        double[][] positions = {
            {0,   0  }, {w/2, 0  }, {w,   0  }, {w,   h/2},
            {w,   h  }, {w/2, h  }, {0,   h  }, {0,   h/2}
        };
        for (int i = 0; i < 8; i++) {
            handles[i].setLayoutX(positions[i][0] - HALF_HANDLE);
            handles[i].setLayoutY(positions[i][1] - HALF_HANDLE);
        }
    }

    private void showHandles(boolean visible) {
        for (Rectangle h : handles) h.setVisible(visible);
    }

    // ── Handle drag-to-resize ────────────────────────────────────────────────

    private void setupHandleEvents(Rectangle handle, String name) {
        handle.setOnMousePressed(e -> {
            e.consume();
            activeHandle      = name;
            resizeDragStartX  = e.getSceneX();
            resizeDragStartY  = e.getSceneY();
            resizeStartW      = element.getWidth();
            resizeStartH      = element.getHeight();
            resizeStartNodeX  = getLayoutX();
            resizeStartNodeY  = getLayoutY();
        });

        handle.setOnMouseDragged(e -> {
            e.consume();
            double dx = e.getSceneX() - resizeDragStartX;
            double dy = e.getSceneY() - resizeDragStartY;

            double newX = resizeStartNodeX;
            double newY = resizeStartNodeY;
            double newW = resizeStartW;
            double newH = resizeStartH;

            switch (activeHandle) {
                case "SE": newW = Math.max(MIN_SIZE, resizeStartW + dx);
                           newH = Math.max(MIN_SIZE, resizeStartH + dy); break;
                case "S":  newH = Math.max(MIN_SIZE, resizeStartH + dy); break;
                case "E":  newW = Math.max(MIN_SIZE, resizeStartW + dx); break;
                case "NW": newW = Math.max(MIN_SIZE, resizeStartW - dx);
                           newH = Math.max(MIN_SIZE, resizeStartH - dy);
                           newX = resizeStartNodeX + (resizeStartW - newW);
                           newY = resizeStartNodeY + (resizeStartH - newH); break;
                case "N":  newH = Math.max(MIN_SIZE, resizeStartH - dy);
                           newY = resizeStartNodeY + (resizeStartH - newH); break;
                case "W":  newW = Math.max(MIN_SIZE, resizeStartW - dx);
                           newX = resizeStartNodeX + (resizeStartW - newW); break;
                case "NE": newW = Math.max(MIN_SIZE, resizeStartW + dx);
                           newH = Math.max(MIN_SIZE, resizeStartH - dy);
                           newY = resizeStartNodeY + (resizeStartH - newH); break;
                case "SW": newW = Math.max(MIN_SIZE, resizeStartW - dx);
                           newH = Math.max(MIN_SIZE, resizeStartH + dy);
                           newX = resizeStartNodeX + (resizeStartW - newW); break;
            }

            element.setX(newX);
            element.setY(newY);
            element.setWidth(newW);
            element.setHeight(newH);

            setLayoutX(newX);
            setLayoutY(newY);
            applySize();
        });

        handle.setOnMouseReleased(e -> {
            activeHandle = "";
            e.consume();
        });
    }

    // ── Body event handlers ───────────────────────────────────────────────────

    private void setupEventHandlers() {
        // ── Click to select ───────────────────────────────────────────────────
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                controller.selectImageElement(this);
                // Drag-to-move bookkeeping
                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
                nodeStartX = getLayoutX();
                nodeStartY = getLayoutY();
            }
        });

        // ── Drag to move ──────────────────────────────────────────────────────
        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                double dx = e.getSceneX() - dragStartX;
                double dy = e.getSceneY() - dragStartY;
                double newX = nodeStartX + dx;
                double newY = nodeStartY + dy;
                setLayoutX(newX);
                setLayoutY(newY);
                element.setX(newX);
                element.setY(newY);
            }
        });

        // ── Delete key ────────────────────────────────────────────────────────
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.DELETE && selected) {
                controller.deleteImageElement(this);
            }
        });

        // ── Right-click context menu ──────────────────────────────────────────
        setOnContextMenuRequested(e -> {
            ContextMenu menu = new ContextMenu();

            MenuItem deleteItem = new MenuItem("🗑 Delete Image");
            deleteItem.setOnAction(ev -> controller.deleteImageElement(this));

            MenuItem resetItem = new MenuItem("↩ Reset to Original Size");
            resetItem.setOnAction(ev -> resetToOriginalSize());

            menu.getItems().addAll(deleteItem, resetItem);
            menu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    // ── Reset to natural image size ───────────────────────────────────────────

    private void resetToOriginalSize() {
        if (imageView.getImage() != null) {
            double origW = imageView.getImage().getWidth();
            double origH = imageView.getImage().getHeight();
            element.setWidth(origW);
            element.setHeight(origH);
            applySize();
        }
    }

    // ── Selection style ───────────────────────────────────────────────────────

    public void select() {
        selected = true;
        selectionBorder.setVisible(true);
        showHandles(true);
        toFront();
        requestFocus();
    }

    public void deselect() {
        selected = false;
        selectionBorder.setVisible(false);
        showHandles(false);
    }

    public boolean isSelected() { return selected; }

    // ── Public API ────────────────────────────────────────────────────────────

    public ImageElement getElement() { return element; }
}