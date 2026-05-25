package slideeditor;

import javafx.scene.paint.Color;
import java.io.Serializable;

public class ArrowElement implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum ArrowType {
        STRAIGHT, CURVED, ELBOW
    }
    
    private String id;
    private String startElementId;
    private String endElementId;
    private ArrowType arrowType;
    private double startX, startY;
    private double endX, endY;
    private double lineWidth;
    private String color;
    private boolean selected;

    // Once the user drags an endpoint handle, this is set to true and the
    // controller stops auto-calculating positions from the connected shapes.
    private boolean manuallyPositioned = false;
    
    // Constructor for element-to-element connection
    public ArrowElement(String id, String startElementId, String endElementId, ArrowType arrowType) {
        this.id = id;
        this.startElementId = startElementId;
        this.endElementId = endElementId;
        this.arrowType = arrowType;
        this.lineWidth = 2.0;
        this.color = "#1a73e8";
        this.selected = false;
        this.startX = 0;
        this.startY = 0;
        this.endX = 0;
        this.endY = 0;
    }
    
    // Constructor for point-to-point connection
    public ArrowElement(double startX, double startY, double endX, double endY, ArrowType arrowType) {
        this.id = java.util.UUID.randomUUID().toString();
        this.startElementId = null;
        this.endElementId = null;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.arrowType = arrowType;
        this.lineWidth = 2.0;
        this.color = "#1a73e8";
        this.selected = false;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getStartElementId() { return startElementId; }
    public void setStartElementId(String startElementId) { this.startElementId = startElementId; }
    
    public String getEndElementId() { return endElementId; }
    public void setEndElementId(String endElementId) { this.endElementId = endElementId; }
    
    public ArrowType getArrowType() { return arrowType; }
    public void setArrowType(ArrowType arrowType) { this.arrowType = arrowType; }
    
    public double getStartX() { return startX; }
    public void setStartX(double startX) { this.startX = startX; }
    
    public double getStartY() { return startY; }
    public void setStartY(double startY) { this.startY = startY; }
    
    public double getEndX() { return endX; }
    public void setEndX(double endX) { this.endX = endX; }
    
    public double getEndY() { return endY; }
    public void setEndY(double endY) { this.endY = endY; }
    
    public double getLineWidth() { return lineWidth; }
    public void setLineWidth(double lineWidth) { this.lineWidth = lineWidth; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public boolean isManuallyPositioned() { return manuallyPositioned; }
    public void setManuallyPositioned(boolean manuallyPositioned) { this.manuallyPositioned = manuallyPositioned; }
    
    public Color getColorAsJavaFX() {
        return Color.web(color);
    }
}