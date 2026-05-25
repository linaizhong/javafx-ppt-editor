package slideeditor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SlideEditorApp extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        SlideEditorController controller = new SlideEditorController();
        controller.setStage(primaryStage); // Set the stage reference
        
        Scene scene = new Scene(controller.getView(), 1280, 800);
        
        // Load CSS with null check
        try {
            String cssPath = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            System.out.println("CSS loaded successfully from: " + cssPath);
        } catch (Exception e) {
            System.out.println("Warning: CSS file not found. Using default styling.");
        }
        
        primaryStage.setTitle("Simple Slide Editor - Professional Presentation Tool");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}