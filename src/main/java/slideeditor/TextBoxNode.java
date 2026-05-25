package slideeditor;

import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class TextBoxNode extends StackPane {
    private TextElement element;
    private SlideEditorController controller;
    private Text textNode;
    private boolean editing = false;
    private double dragStartX, dragStartY;
    private boolean selected = false;
    private Rectangle borderRect;
    
    public TextBoxNode(TextElement element, SlideEditorController controller) {
        this.element = element;
        this.controller = controller;
        
        textNode = new Text(element.getText());
        updateTextStyle();
        
        // Create border rectangle
        borderRect = new Rectangle();
        borderRect.setArcWidth(8);
        borderRect.setArcHeight(8);
        borderRect.setFill(Color.TRANSPARENT);
        borderRect.setStroke(Color.web("#e0e0e0"));
        borderRect.setStrokeWidth(1.5);
        borderRect.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);
        
        getChildren().addAll(borderRect, textNode);
        
        setLayoutX(element.getX());
        setLayoutY(element.getY());
        
        updateSelectionStyle(false);
        
        setupEventHandlers();
        
        // Bind rectangle size to text bounds
        textNode.boundsInLocalProperty().addListener((obs, old, bounds) -> {
            borderRect.setWidth(bounds.getWidth() + 20);
            borderRect.setHeight(bounds.getHeight() + 12);
            borderRect.setX(-10);
            borderRect.setY(-6);
        });
    }
    
    private void updateTextStyle() {
        textNode.setText(element.getText());
        FontWeight fontWeight = element.isBold() ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture fontPosture = element.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;
        textNode.setFont(Font.font(element.getFontFamily(), fontWeight, fontPosture, element.getFontSize()));
        textNode.setFill(Color.web(element.getColor()));
        
        // Update cursor based on interaction mode
        textNode.setCursor(javafx.scene.Cursor.HAND);
    }
    
    private void updateSelectionStyle(boolean isSelected) {
        if (isSelected) {
            borderRect.setStroke(Color.web("#4CAF50"));
            borderRect.setStrokeWidth(2);
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#4CAF50", 0.4));
            glow.setRadius(10);
            setEffect(glow);
        } else {
            borderRect.setStroke(Color.web("#e0e0e0"));
            borderRect.setStrokeWidth(1.5);
            setEffect(null);
        }
    }
    
    private void setupEventHandlers() {
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                startEditing();
            } else {
                controller.selectElement(element);
                updateSelectionStyle(true);
                selected = true;
                toFront();
            }
            e.consume();
        });
        
        setOnMousePressed(e -> {
            dragStartX = e.getSceneX();
            dragStartY = e.getSceneY();
            toFront();
            e.consume();
        });
        
        setOnMouseDragged(e -> {
            double deltaX = e.getSceneX() - dragStartX;
            double deltaY = e.getSceneY() - dragStartY;
            controller.moveTextElement(element, deltaX, deltaY);
            dragStartX = e.getSceneX();
            dragStartY = e.getSceneY();
            e.consume();
        });
        
        // Clear selection when clicking elsewhere
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnMouseClicked(e -> {
                    if (e.getTarget() != this && !editing && selected) {
                        updateSelectionStyle(false);
                        selected = false;
                        controller.selectElement(null);
                    }
                });
            }
        });
        
        // Hover effect
        setOnMouseEntered(e -> {
            if (!selected) {
                borderRect.setStroke(Color.web("#4CAF50", 0.6));
            }
        });
        
        setOnMouseExited(e -> {
            if (!selected) {
                borderRect.setStroke(Color.web("#e0e0e0"));
            }
        });
    }
    
    private void startEditing() {
        if (editing) return;
        editing = true;
        
        TextArea textArea = new TextArea(element.getText());
        textArea.setStyle(
            "-fx-font-size: " + element.getFontSize() + "px;" +
            "-fx-font-family: '" + element.getFontFamily() + "';" +
            "-fx-background-color: white;" +
            "-fx-border-color: #4CAF50;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 4px;" +
            "-fx-background-radius: 4px;"
        );
        textArea.setPrefSize(Math.max(250, textNode.getLayoutBounds().getWidth() + 40), 
                            Math.max(80, textNode.getLayoutBounds().getHeight() + 30));
        textArea.setWrapText(true);
        
        getChildren().clear();
        getChildren().add(textArea);
        textArea.requestFocus();
        
        textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                finishEditing(textArea.getText());
            }
        });
        
        textArea.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER") && e.isControlDown()) {
                finishEditing(textArea.getText());
            } else if (e.getCode().toString().equals("ESCAPE")) {
                finishEditing(element.getText());
            }
        });
    }
    
    private void finishEditing(String newText) {
        controller.updateTextElement(element, newText);
        editing = false;
        getChildren().clear();
        getChildren().addAll(borderRect, textNode);
        updateTextStyle();
        if (selected) {
            updateSelectionStyle(true);
        }
    }
}