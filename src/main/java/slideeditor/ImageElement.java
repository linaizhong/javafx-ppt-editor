package slideeditor;

import java.io.Serializable;

/**
 * Data model for an image placed on a slide.
 *
 * Stores only the absolute path to the source image file — not the pixel data —
 * so .sle files remain small.  The controller renders the image from disk when
 * the slide is displayed.
 *
 * Geometry (x, y, width, height) follows the same pixel convention used by
 * TextElement: x/y is the top-left corner of the image frame on the slide pane.
 */
public class ImageElement implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String imagePath;   // absolute path to the source image file
    private double x;
    private double y;
    private double width;
    private double height;

    public ImageElement(String id, String imagePath,
                        double x, double y,
                        double width, double height) {
        this.id        = id;
        this.imagePath = imagePath;
        this.x         = x;
        this.y         = y;
        this.width     = width;
        this.height    = height;
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    public String getId()        { return id; }
    public void   setId(String id) { this.id = id; }

    public String getImagePath()             { return imagePath; }
    public void   setImagePath(String path)  { this.imagePath = path; }

    public double getX()             { return x; }
    public void   setX(double x)    { this.x = x; }

    public double getY()             { return y; }
    public void   setY(double y)    { this.y = y; }

    public double getWidth()              { return width; }
    public void   setWidth(double width)  { this.width = width; }

    public double getHeight()               { return height; }
    public void   setHeight(double height)  { this.height = height; }
}