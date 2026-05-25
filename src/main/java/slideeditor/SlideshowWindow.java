package slideeditor;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SlideshowWindow {
    private Presentation presentation;
    private int currentIndex = 0;
    private Stage stage;
    private StackPane root;
    private VBox controlsOverlay;
    
    public SlideshowWindow(Presentation presentation) {
        this.presentation = presentation;
        this.root = new StackPane();
        this.stage = new Stage();
        
        setupSlideshow();
    }
    
    private void setupSlideshow() {
        stage.setTitle("Slideshow - " + presentation.getTitle());
        stage.setFullScreen(true);
        
        Scene scene = new Scene(root, 1024, 768);
        root.setStyle("-fx-background-color: black;");
        
        // Create controls overlay that appears on mouse move
        createControlsOverlay();
        
        setupKeyboardControls(scene);
        setupMouseControls();
        
        stage.setScene(scene);
        showCurrentSlide();
    }
    
    private void createControlsOverlay() {
        controlsOverlay = new VBox();
        controlsOverlay.setAlignment(Pos.BOTTOM_CENTER);
        controlsOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-border-color: rgba(255,255,255,0.2); -fx-border-width: 1 0 0 0;");
        controlsOverlay.setPadding(new javafx.geometry.Insets(15));
        controlsOverlay.setSpacing(10);
        controlsOverlay.setOpacity(0);
        
        Text hint = new Text("← →  |  ESC to exit  |  F for fullscreen  |  Home/End for first/last");
        hint.setFont(Font.font("Arial", 12));
        hint.setFill(Color.WHITE);
        
        controlsOverlay.getChildren().add(hint);
        root.getChildren().add(controlsOverlay);
        
        // Fade in controls on mouse move
        root.setOnMouseMoved(e -> {
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), controlsOverlay);
            fadeIn.setToValue(0.8);
            fadeIn.play();
            
            // Fade out after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    javafx.application.Platform.runLater(() -> {
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), controlsOverlay);
                        fadeOut.setToValue(0);
                        fadeOut.play();
                    });
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }).start();
        });
    }
    
    private void setupKeyboardControls(Scene scene) {
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case RIGHT:
                case SPACE:
                case DOWN:
                case PAGE_DOWN:
                    nextSlide();
                    break;
                case LEFT:
                case UP:
                case PAGE_UP:
                    previousSlide();
                    break;
                case HOME:
                    goToFirstSlide();
                    break;
                case END:
                    goToLastSlide();
                    break;
                case ESCAPE:
                    stage.close();
                    break;
                case F:
                    stage.setFullScreen(!stage.isFullScreen());
                    break;
                default:
                    break;
            }
        });
    }
    
    private void setupMouseControls() {
        root.setOnMouseClicked(e -> {
            double x = e.getX();
            double width = root.getWidth();
            
            if (x < width / 3) {
                previousSlide();
            } else if (x > 2 * width / 3) {
                nextSlide();
            }
        });
    }
    
    private void goToFirstSlide() {
        currentIndex = 0;
        showCurrentSlide();
    }
    
    private void goToLastSlide() {
        currentIndex = presentation.getSlideCount() - 1;
        showCurrentSlide();
    }
    
    private void showCurrentSlide() {
        // Fade out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), root);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            root.getChildren().removeIf(node -> node instanceof Text && node != controlsOverlay);
            
            if (presentation.getSlideCount() > 0 && currentIndex < presentation.getSlideCount()) {
                Slide slide = presentation.getSlide(currentIndex);
                root.setStyle("-fx-background-color: " + slide.getBackground() + ";");
                
                // Add all text elements with animation
                for (TextElement element : slide.getTextElements()) {
                    Text text = new Text(element.getText());
                    text.setFont(Font.font(element.getFontFamily(), element.getFontSize()));
                    text.setFill(Color.web(element.getColor()));
                    text.setLayoutX(element.getX());
                    text.setLayoutY(element.getY());
                    
                    // Add drop shadow for better readability
                    DropShadow shadow = new DropShadow();
                    shadow.setColor(Color.rgb(0, 0, 0, 0.3));
                    shadow.setRadius(5);
                    text.setEffect(shadow);
                    
                    root.getChildren().add(text);
                    
                    // Animate each text element
                    ScaleTransition scale = new ScaleTransition(Duration.millis(300), text);
                    scale.setFromX(0.8);
                    scale.setFromY(0.8);
                    scale.setToX(1);
                    scale.setToY(1);
                    scale.play();
                }

                // Add all images  ← NEW
                for (ImageElement img : slide.getImages()) {
                    try {
                        java.io.File f = new java.io.File(img.getImagePath());
                        if (f.exists()) {
                            javafx.scene.image.Image image =
                                new javafx.scene.image.Image(f.toURI().toString(), true);
                            javafx.scene.image.ImageView iv =
                                new javafx.scene.image.ImageView(image);
                            iv.setFitWidth(img.getWidth());
                            iv.setFitHeight(img.getHeight());
                            iv.setPreserveRatio(false);
                            iv.setSmooth(true);
                            iv.setLayoutX(img.getX());
                            iv.setLayoutY(img.getY());

                            // Fade-in animation matching text elements
                            ScaleTransition scale = new ScaleTransition(Duration.millis(300), iv);
                            scale.setFromX(0.8);
                            scale.setFromY(0.8);
                            scale.setToX(1);
                            scale.setToY(1);

                            root.getChildren().add(iv);
                            scale.play();
                        }
                    } catch (Exception ignored) { /* skip unreadable images */ }
                }
            }
            
            // Add slide number indicator
            Text slideNumber = new Text((currentIndex + 1) + " / " + presentation.getSlideCount());
            slideNumber.setFont(Font.font("Arial", 14));
            slideNumber.setFill(Color.web("#ffffff", 0.7));
            slideNumber.setLayoutX(20);
            slideNumber.setLayoutY(stage.getHeight() - 30);
            root.getChildren().add(slideNumber);
            
            // Fade in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
        
        // Update stage title
        stage.setTitle("Slideshow - Slide " + (currentIndex + 1) + " of " + presentation.getSlideCount());
    }
    
    private void nextSlide() {
        if (currentIndex < presentation.getSlideCount() - 1) {
            currentIndex++;
            showCurrentSlide();
        }
    }
    
    private void previousSlide() {
        if (currentIndex > 0) {
            currentIndex--;
            showCurrentSlide();
        }
    }
    
    public void show() {
        stage.show();
    }
}