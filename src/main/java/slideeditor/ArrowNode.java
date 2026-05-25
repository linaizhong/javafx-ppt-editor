package slideeditor;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

/**
 * Visual node for an arrow connector between two shapes.
 *
 * Endpoint handles
 * ────────────────
 * Double-clicking the arrow selects it and reveals two circular drag handles:
 *   • Start handle (hollow circle) — drag to reposition the arrow's start point
 *   • End handle   (filled circle) — drag to reposition the arrow's end point
 *
 * Once either handle is dragged, ArrowElement.manuallyPositioned is set to true
 * so the controller stops auto-calculating positions from the connected shapes.
 *
 * Single-clicking toggles selection and shows/hides handles.
 * Right-clicking opens a context menu with a working Delete and Reset option.
 */
public class ArrowNode extends Group {

    private static final double HANDLE_RADIUS = 6;
    private static final double HIT_EXTRA     = 6; // extra stroke width for easier clicking

    private final ArrowElement          arrow;
    private final SlideEditorController controller;

    // Core visuals
    private Shape   line;
    private Polygon arrowHead;

    // Endpoint drag handles (shown only when selected)
    private Circle startHandle;
    private Circle endHandle;

    // Drag bookkeeping
    private double dragStartSceneX, dragStartSceneY;
    private double dragStartElemX,  dragStartElemY;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ArrowNode(ArrowElement arrow, SlideEditorController controller) {
        this.arrow      = arrow;
        this.controller = controller;

        buildHandles();
        updateArrowPosition();
        setupEventHandlers();
    }

    // ── Handle creation ───────────────────────────────────────────────────────

    private void buildHandles() {
        // Start handle — hollow white circle with blue border
        startHandle = new Circle(HANDLE_RADIUS);
        startHandle.setFill(Color.WHITE);
        startHandle.setStroke(Color.web("#1a73e8"));
        startHandle.setStrokeWidth(2);
        startHandle.setCursor(Cursor.CROSSHAIR);
        startHandle.setVisible(false);

        // End handle — filled blue circle
        endHandle = new Circle(HANDLE_RADIUS);
        endHandle.setFill(Color.web("#1a73e8"));
        endHandle.setStroke(Color.WHITE);
        endHandle.setStrokeWidth(1.5);
        endHandle.setCursor(Cursor.CROSSHAIR);
        endHandle.setVisible(false);

        setupHandleDrag(startHandle, true);
        setupHandleDrag(endHandle,   false);
    }

    /**
     * Wire drag events on a handle.
     * @param handle  the Circle handle
     * @param isStart true → moves startX/Y; false → moves endX/Y
     */
    private void setupHandleDrag(Circle handle, boolean isStart) {
        handle.setOnMousePressed(e -> {
            dragStartSceneX = e.getSceneX();
            dragStartSceneY = e.getSceneY();
            dragStartElemX  = isStart ? arrow.getStartX() : arrow.getEndX();
            dragStartElemY  = isStart ? arrow.getStartY() : arrow.getEndY();
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            double newX = dragStartElemX + (e.getSceneX() - dragStartSceneX);
            double newY = dragStartElemY + (e.getSceneY() - dragStartSceneY);
            if (isStart) {
                arrow.setStartX(newX);
                arrow.setStartY(newY);
            } else {
                arrow.setEndX(newX);
                arrow.setEndY(newY);
            }
            // Lock so the controller stops auto-recalculating
            arrow.setManuallyPositioned(true);
            updateArrowPosition();
            e.consume();
        });

        handle.setOnMouseReleased(e -> e.consume());
    }

    // ── Arrow drawing ─────────────────────────────────────────────────────────

    public void updateArrowPosition() {
        double startX = arrow.getStartX();
        double startY = arrow.getStartY();
        double endX   = arrow.getEndX();
        double endY   = arrow.getEndY();

        // Remove old line and arrowhead, keep handles
        getChildren().removeIf(n -> n == line || n == arrowHead);

        Color  lineColor = arrow.getColorAsJavaFX();
        double lineWidth = arrow.getLineWidth();

        // ── Build line ────────────────────────────────────────────────────────
        switch (arrow.getArrowType()) {
            case CURVED: {
                double midX = (startX + endX) / 2;
                double midY = (startY + endY) / 2 - 50;
                Path path = new Path(
                    new MoveTo(startX, startY),
                    new QuadCurveTo(midX, midY, endX, endY)
                );
                line = path;
                break;
            }
            case ELBOW: {
                double midX = (startX + endX) / 2;
                Path path = new Path(
                    new MoveTo(startX, startY),
                    new LineTo(midX, startY),
                    new LineTo(midX, endY),
                    new LineTo(endX, endY)
                );
                line = path;
                break;
            }
            default: { // STRAIGHT
                line = new Line(startX, startY, endX, endY);
                break;
            }
        }

        line.setStroke(lineColor);
        line.setStrokeWidth(lineWidth);
        line.setFill(Color.TRANSPARENT);

        // ── Build arrowhead ───────────────────────────────────────────────────
        double angle     = Math.atan2(endY - startY, endX - startX);
        double arrowSize = 10;
        double x1 = endX - arrowSize * Math.cos(angle - Math.PI / 6);
        double y1 = endY - arrowSize * Math.sin(angle - Math.PI / 6);
        double x2 = endX - arrowSize * Math.cos(angle + Math.PI / 6);
        double y2 = endY - arrowSize * Math.sin(angle + Math.PI / 6);

        arrowHead = new Polygon(endX, endY, x1, y1, x2, y2);
        arrowHead.setFill(lineColor);
        arrowHead.setStroke(lineColor);

        // Insert line and arrowhead before handles so handles stay on top
        getChildren().add(0, arrowHead);
        getChildren().add(0, line);

        // ── Position handles ──────────────────────────────────────────────────
        startHandle.setCenterX(startX);
        startHandle.setCenterY(startY);
        endHandle.setCenterX(endX);
        endHandle.setCenterY(endY);

        if (!getChildren().contains(startHandle)) getChildren().add(startHandle);
        if (!getChildren().contains(endHandle))   getChildren().add(endHandle);

        // ── Selection glow + handle visibility ───────────────────────────────
        if (arrow.isSelected()) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#1a73e8", 0.5));
            glow.setRadius(10);
            setEffect(glow);
            startHandle.setVisible(true);
            endHandle.setVisible(true);
        } else {
            setEffect(null);
            startHandle.setVisible(false);
            endHandle.setVisible(false);
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private void setupEventHandlers() {

        // Widen hit area on hover so the line is easier to click
        setOnMouseEntered(e -> {
            if (line != null) line.setStrokeWidth(arrow.getLineWidth() + HIT_EXTRA);
        });
        setOnMouseExited(e -> {
            if (line != null) line.setStrokeWidth(arrow.getLineWidth());
        });

        setOnMouseClicked(e -> {
            // Don't interfere with handle clicks
            if (e.getTarget() == startHandle || e.getTarget() == endHandle) return;

            if (e.getButton() == MouseButton.PRIMARY) {
                arrow.setSelected(!arrow.isSelected());
                updateArrowPosition();
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                showContextMenu(e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

        // Deselect when clicking anywhere else on the slide
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(
                    javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                    e -> {
                        if (arrow.isSelected()      &&
                            e.getTarget() != this   &&
                            e.getTarget() != line   &&
                            e.getTarget() != arrowHead &&
                            e.getTarget() != startHandle &&
                            e.getTarget() != endHandle) {
                            arrow.setSelected(false);
                            updateArrowPosition();
                        }
                    }
                );
            }
        });
    }

    private void showContextMenu(double screenX, double screenY) {
        ContextMenu menu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("🗑 Delete Arrow");
        deleteItem.setOnAction(ev -> controller.deleteArrow(arrow));

        MenuItem resetItem = new MenuItem("↺ Reset to Auto Position");
        resetItem.setDisable(!arrow.isManuallyPositioned());
        resetItem.setOnAction(ev -> {
            arrow.setManuallyPositioned(false);
            controller.refreshArrowPositions();
        });

        menu.getItems().addAll(deleteItem, resetItem);
        menu.show(this, screenX, screenY);
    }

    // ── Public getter ─────────────────────────────────────────────────────────

    public ArrowElement getArrow() { return arrow; }
}