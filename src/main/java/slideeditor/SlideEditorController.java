package slideeditor;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.*;
import java.util.UUID;

public class SlideEditorController {
    private Presentation presentation;
    private SlideEditorView view;
    private int currentSlideIndex = 0;
    private TextElement selectedElement = null;
    private ImageNode   selectedImageNode = null;   // ← NEW: currently selected image
    private File currentDirectory = null;
    private File currentFile = null;
    private Stage primaryStage;
    
    // Arrow connection mode
    private boolean connectMode = false;
    private TextElement selectedStartElement = null;
    private ArrowElement.ArrowType currentArrowType = ArrowElement.ArrowType.STRAIGHT;
    
    public SlideEditorController() {
        this.presentation = new Presentation();
        this.view = new SlideEditorView();
        setupEventHandlers();
        updateSlideDisplay();
        updateThumbnails();
        updateStatus("Ready - New presentation created");
    }
    
    public void setStage(Stage stage) {
        this.primaryStage = stage;
        updateWindowTitle();
    }
    
    private void setupEventHandlers() {
        // Menu actions
        view.getNewMenuItem().setOnAction(e -> newPresentation());
        view.getOpenMenuItem().setOnAction(e -> openPresentation());
        view.getSaveMenuItem().setOnAction(e -> savePresentation());
        view.getSaveAsMenuItem().setOnAction(e -> saveAsPresentation());
        view.getExitMenuItem().setOnAction(e -> Platform.exit());
        
        view.getAddSlideMenuItem().setOnAction(e -> addSlide());
        view.getDeleteSlideMenuItem().setOnAction(e -> deleteSlide());
        view.getPreviousSlideMenuItem().setOnAction(e -> previousSlide());
        view.getNextSlideMenuItem().setOnAction(e -> nextSlide());
        
        view.getPlayMenuItem().setOnAction(e -> playSlideshow());
        view.getAboutMenuItem().setOnAction(e -> showAboutDialog());
        view.getHelpMenuItem().setOnAction(e -> showHelpDialog());
        
        // Toolbar actions
        view.getNewButton().setOnAction(e -> newPresentation());
        view.getOpenButton().setOnAction(e -> openPresentation());
        view.getSaveButton().setOnAction(e -> savePresentation());
        view.getAddSlideButton().setOnAction(e -> addSlide());
        view.getDeleteSlideButton().setOnAction(e -> deleteSlide());
        view.getPrevButton().setOnAction(e -> previousSlide());
        view.getNextButton().setOnAction(e -> nextSlide());
        view.getPlayButton().setOnAction(e -> playSlideshow());
        view.getAddTextButton().setOnAction(e -> addTextBox("rectangle"));
        view.getAddImageButton().setOnAction(e -> insertImage());   // ← NEW
        
        // Shape actions
        view.getRectangleShapeButton().setOnAction(e -> addTextBox("rectangle"));
        view.getRoundedRectShapeButton().setOnAction(e -> addTextBox("rounded"));
        view.getCircleShapeButton().setOnAction(e -> addTextBox("circle"));
        view.getCalloutShapeButton().setOnAction(e -> addTextBox("callout"));
        view.getDiamondShapeButton().setOnAction(e -> addTextBox("diamond"));
        view.getPentagonShapeButton().setOnAction(e -> addTextBox("pentagon"));
        view.getHexagonShapeButton().setOnAction(e -> addTextBox("hexagon"));
        view.getStarShapeButton().setOnAction(e -> addTextBox("star"));
        view.getParallelogramShapeButton().setOnAction(e -> addTextBox("parallelogram"));
        
        // Formatting actions (using unified toolbar)
        view.getBoldButton().setOnAction(e -> toggleBold());
        view.getItalicButton().setOnAction(e -> toggleItalic());
        view.getFontSizeCombo().setOnAction(e -> changeFontSize());
        view.getFontColorPicker().setOnAction(e -> changeFontColor());   // ← NEW

        // NEW: Alignment actions
        view.getAlignLeftButton().setOnAction(e -> setAlignment("LEFT"));
        view.getAlignCenterButton().setOnAction(e -> setAlignment("CENTER"));
        view.getAlignRightButton().setOnAction(e -> setAlignment("RIGHT"));

        // NEW: Bullet list toggle
        view.getBulletListButton().setOnAction(e -> toggleBulletList());

        // Indent buttons
        view.getIndentIncreaseButton().setOnMousePressed(e -> e.consume());
        view.getIndentDecreaseButton().setOnMousePressed(e -> e.consume());
        view.getIndentIncreaseButton().setOnAction(e -> updateStatus("Indent increased"));
        view.getIndentDecreaseButton().setOnAction(e -> updateStatus("Indent decreased"));

        // Highlight colour picker — onAction fires after the user commits a colour.
        // The TextArea focus listener in ShapeTextBoxNode skips finishEditing when
        // focus moves to this picker (userData="highlight"), so liveTextArea is still
        // valid when onAction fires.
        view.getHighlightColorPicker().setOnAction(e -> applyHighlightColor());

        // Highlight text colour picker — same focus-listener pattern.
        view.getHighlightTextColorPicker().setOnAction(e -> applyHighlightColor());

        // Clear highlight button — same focus-listener pattern as indent buttons.
        view.getClearHighlightButton().setOnMousePressed(e -> e.consume());
        view.getClearHighlightButton().setOnAction(e -> clearHighlightColor());

        // Connector actions
        view.getConnectModeButton().selectedProperty().addListener((obs, oldVal, newVal) -> {
            connectMode = newVal;
            if (connectMode) {
                selectedStartElement = null;
                updateStatus("Connect mode enabled - Click on a shape to start connection");
            } else {
                selectedStartElement = null;
                updateStatus("Connect mode disabled");
            }
        });
        
        view.getStraightArrowButton().setOnAction(e -> {
            currentArrowType = ArrowElement.ArrowType.STRAIGHT;
            updateStatus("Arrow type: Straight");
        });
        
        view.getCurvedArrowButton().setOnAction(e -> {
            currentArrowType = ArrowElement.ArrowType.CURVED;
            updateStatus("Arrow type: Curved");
        });
        
        view.getElbowArrowButton().setOnAction(e -> {
            currentArrowType = ArrowElement.ArrowType.ELBOW;
            updateStatus("Arrow type: Elbow");
        });
        
        // File explorer actions
        view.getBrowseButton().setOnAction(e -> browseDirectory());
        view.getRefreshButton().setOnAction(e -> refreshFileTree());

        // Slide background colour picker
        view.getSlideBackgroundPicker().setOnAction(e -> changeSlideBackground());

        // Format panel — shape fill
        view.getShapeFillPicker().setOnAction(e -> changeShapeFill());
        view.getShapeFillNoneCheck().setOnAction(e -> changeShapeFill());

        // Format panel — text box fill
        view.getTextBoxFillPicker().setOnAction(e -> changeTextBoxFill());
        view.getTextBoxFillNoneCheck().setOnAction(e -> changeTextBoxFill());

        // Text Container tab — font controls
        view.getTcFontFamilyCombo().setOnAction(e -> changeTcFontFamily());
        view.getTcFontSizeCombo().setOnAction(e -> changeTcFontSize());
        view.getTcFontColorPicker().setOnAction(e -> changeTcFontColor());
        view.getTcBoldButton().setOnAction(e -> changeTcBold());
        view.getTcItalicButton().setOnAction(e -> changeTcItalic());
        
        // File tree double-click
        view.getFileTreeView().setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                TreeItem<String> item = view.getFileTreeView().getSelectionModel().getSelectedItem();
                if (item != null && item.getValue() != null && item.getValue().endsWith(".sle")) {
                    openPresentationFile(new File(item.getValue()));
                }
            }
        });
        
        // Resize listeners — reposition placeholder elements proportionally
        // whenever the slide pane changes size (window resize, divider drag, etc.)
        view.getSlidePane().widthProperty().addListener(
            (obs, oldW, newW) -> resizePlaceholders(newW.doubleValue(), view.getSlidePane().getHeight()));
        view.getSlidePane().heightProperty().addListener(
            (obs, oldH, newH) -> resizePlaceholders(view.getSlidePane().getWidth(), newH.doubleValue()));

        // Slide editing area click to clear selection (only when clicking pane background, not shapes)
        view.getSlidePane().setOnMouseClicked(e -> {
            if (e.getTarget() == view.getSlidePane()) {
                // Visually deselect every node on the canvas
                view.getSlidePane().getChildren().forEach(child -> {
                    if (child instanceof ShapeTextBoxNode) {
                        ((ShapeTextBoxNode) child).deselect();
                    }
                    if (child instanceof ImageNode) {        // ← NEW
                        ((ImageNode) child).deselect();
                    }
                });
                selectedElement   = null;
                selectedImageNode = null;                   // ← NEW
                updateFormattingControls();
                updateStatus("Selection cleared");
                view.getSlidePane().requestFocus();
            }
        });
        
        // Delete key handler for removing selected elements
        view.getSlidePane().setFocusTraversable(true);
        view.getSlidePane().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                if (selectedElement != null) {
                    deleteSelectedElement();
                } else if (selectedImageNode != null) {   // ← NEW
                    deleteImageElement(selectedImageNode);
                }
            }
        });
        
        // Track mouse position for status bar
        view.getSlidePane().setOnMouseMoved(e -> {
            HBox statusBar = (HBox) view.getStatusLabel().getParent();
            if (statusBar != null && statusBar.getChildren().size() > 4) {
                Label positionLabel = (Label) statusBar.getChildren().get(4);
                if (positionLabel != null) {
                    positionLabel.setText(String.format("X: %.0f, Y: %.0f", e.getX(), e.getY()));
                }
            }
        });
        
        // Thumbnail click handler
        view.getThumbnailListView().setOnMouseClicked(e -> {
            int index = view.getThumbnailListView().getSelectionModel().getSelectedIndex();
            if (index >= 0 && index < presentation.getSlideCount()) {
                currentSlideIndex = index;
                updateSlideDisplay();
                updateThumbnails();
                updateStatus("Showing slide " + (currentSlideIndex + 1));
            }
        });
    }
    
    private void deleteSelectedElement() {
        if (selectedElement != null) {
            Slide currentSlide = presentation.getSlide(currentSlideIndex);
            currentSlide.removeTextElement(selectedElement);
            selectedElement = null;
            updateSlideDisplay();
            updateFormattingControls();
            updateStatus("Element deleted");
        }
    }
    
    private void browseDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");
        if (currentDirectory != null) {
            directoryChooser.setInitialDirectory(currentDirectory);
        }
        
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            currentDirectory = selectedDirectory;
            refreshFileTree();
            updateStatus("Browsing: " + selectedDirectory.getAbsolutePath());
        }
    }
    
    private void refreshFileTree() {
        if (currentDirectory == null) {
            currentDirectory = new File(System.getProperty("user.home"));
        }
        
        TreeItem<String> rootItem = new TreeItem<>(currentDirectory.getName());
        rootItem.setExpanded(true);
        
        try {
            File[] files = currentDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        TreeItem<String> dirItem = new TreeItem<>(file.getName());
                        dirItem.setExpanded(false);
                        dirItem.getChildren().add(new TreeItem<>("..."));
                        rootItem.getChildren().add(dirItem);
                    } else if (file.getName().endsWith(".sle")) {
                        TreeItem<String> fileItem = new TreeItem<>(file.getAbsolutePath());
                        fileItem.setValue(file.getName());
                        rootItem.getChildren().add(fileItem);
                    }
                }
            }
        } catch (SecurityException e) {
            updateStatus("Error accessing directory: " + e.getMessage());
        }
        
        view.getFileTreeView().setRoot(rootItem);
        updateStatus("File explorer refreshed - " + rootItem.getChildren().size() + " items found");
    }
    
    private void openPresentationFile(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            presentation = (Presentation) ois.readObject();
            currentSlideIndex = 0;
            currentFile = file;
            updateSlideDisplay();
            updateThumbnails();
            updateStatus("Opened: " + file.getName());
            updateWindowTitle();
            showAlert("Success", "Presentation opened successfully from:\n" + file.getAbsolutePath());
        } catch (IOException | ClassNotFoundException e) {
            showAlert("Error", "Could not open presentation:\n" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Called whenever the slide pane is resized (window resize, divider drag).
     * Iterates all elements on the current slide that have relativeLayout==true
     * and recomputes their pixel x/y/width/height from their stored ratios.
     * Then updates the live ShapeTextBoxNode on the canvas without a full rebuild.
     */
    private void resizePlaceholders(double paneW, double paneH) {
        if (paneW <= 0 || paneH <= 0) return;
        if (presentation.getSlideCount() == 0) return;

        Slide currentSlide = presentation.getSlide(currentSlideIndex);

        for (TextElement element : currentSlide.getTextElements()) {
            if (!element.isRelativeLayout()) continue;

            // Recompute pixel geometry from stored ratios
            double newX = element.getXRatio()      * paneW;
            double newY = element.getYRatio()      * paneH;
            double newW = element.getWidthRatio()  * paneW;
            double newH = element.getHeightRatio() * paneH;

            element.setX(newX);
            element.setY(newY);
            element.setWidth(newW);
            element.setHeight(newH);

            // Push the new geometry directly into the live node (no full rebuild)
            for (javafx.scene.Node child : view.getSlidePane().getChildren()) {
                if (child instanceof ShapeTextBoxNode) {
                    ShapeTextBoxNode node = (ShapeTextBoxNode) child;
                    if (node.getElement() == element) {
                        node.setLayoutX(newX);
                        node.setLayoutY(newY);
                        node.applySize();
                        break;
                    }
                }
            }
        }
    }

    private void updateSlideDisplay() {
        view.getSlidePane().getChildren().clear();
        if (presentation.getSlideCount() > 0 && currentSlideIndex < presentation.getSlideCount()) {
            Slide currentSlide = presentation.getSlide(currentSlideIndex);
            
            // Set background
            view.getSlidePane().setStyle("-fx-background-color: " + currentSlide.getBackground() + ";");

            // Sync the background colour picker to this slide's stored colour
            try {
                view.getSlideBackgroundPicker().setValue(Color.web(currentSlide.getBackground()));
            } catch (Exception ignored) {
                view.getSlideBackgroundPicker().setValue(Color.WHITE);
            }
            
            // Add all text elements
            for (TextElement element : currentSlide.getTextElements()) {
                ShapeTextBoxNode textNode = new ShapeTextBoxNode(element, this);
                view.getSlidePane().getChildren().add(textNode);
            }

            // Add all images  ← NEW
            for (ImageElement img : currentSlide.getImages()) {
                ImageNode imgNode = new ImageNode(img, this);
                view.getSlidePane().getChildren().add(imgNode);
                // Re-select the image if it was selected before the refresh
                if (selectedImageNode != null
                        && selectedImageNode.getElement() == img) {
                    imgNode.select();
                    selectedImageNode = imgNode;
                }
            }
            
            // Add all arrows
            for (ArrowElement arrow : currentSlide.getArrows()) {
                // Update arrow positions based on connected elements
                if (arrow.getStartElementId() != null && arrow.getEndElementId() != null) {
                    updateArrowPositions(arrow, currentSlide);
                }
                ArrowNode arrowNode = new ArrowNode(arrow, this);
                view.getSlidePane().getChildren().add(arrowNode);
            }
        }
        
        // Update slide counter
        view.getSlideCounter().setText("Slide " + (currentSlideIndex + 1) + " of " + presentation.getSlideCount());
        
        // Update window title
        updateWindowTitle();
        
        // Update delete button state
        view.getDeleteSlideButton().setDisable(presentation.getSlideCount() <= 1);
        view.getDeleteSlideMenuItem().setDisable(presentation.getSlideCount() <= 1);
        
        // Enable/disable only the formatting controls based on selection
        view.setFormattingControlsDisabled(selectedElement == null);

        // If an element is still selected, re-apply its visual selection state
        // on the freshly-built nodes (updateSlideDisplay recreates all nodes).
        if (selectedElement != null) {
            for (javafx.scene.Node child : view.getSlidePane().getChildren()) {
                if (child instanceof ShapeTextBoxNode) {
                    ShapeTextBoxNode node = (ShapeTextBoxNode) child;
                    if (node.getElement() == selectedElement) {
                        node.restoreSelection();
                        break;
                    }
                }
            }
            updateFormattingControls();
        }
    }
    
    private void updateArrowPositions(ArrowElement arrow, Slide slide) {
        // If the user has manually dragged the endpoints, don't overwrite them
        if (arrow.isManuallyPositioned()) return;

        TextElement startElement = null;
        TextElement endElement   = null;
        
        for (TextElement element : slide.getTextElements()) {
            if (element.getId().equals(arrow.getStartElementId())) {
                startElement = element;
            }
            if (element.getId().equals(arrow.getEndElementId())) {
                endElement = element;
            }
        }
        
        if (startElement != null && endElement != null) {
            // Calculate connection points (center of elements)
            // In a real implementation, you'd calculate the closest edge points
            arrow.setStartX(startElement.getX() + 50); // Approximate center
            arrow.setStartY(startElement.getY() + 25);
            arrow.setEndX(endElement.getX() + 50);
            arrow.setEndY(endElement.getY() + 25);
        }
    }
    
    private void updateThumbnails() {
        view.getThumbnailListView().getItems().clear();
        for (int i = 0; i < presentation.getSlideCount(); i++) {
            final int slideIndex = i;
            HBox thumbnail = createThumbnail(slideIndex);
            view.getThumbnailListView().getItems().add(thumbnail);
        }
    }
    
    private HBox createThumbnail(int slideIndex) {
        HBox thumbnail = new HBox();
        thumbnail.setAlignment(Pos.CENTER);
        thumbnail.setPrefSize(120, 80);
        thumbnail.setStyle("-fx-background-color: " + (slideIndex == currentSlideIndex ? "#e3f2fd" : "white") + 
                          "; -fx-border-color: #e0e0e0; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        thumbnail.setPadding(new Insets(5));
        
        Label slideLabel = new Label("Slide " + (slideIndex + 1));
        slideLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        thumbnail.getChildren().add(slideLabel);
        
        thumbnail.setOnMouseClicked(e -> {
            currentSlideIndex = slideIndex;
            updateSlideDisplay();
            updateThumbnails();
            updateStatus("Showing slide " + (slideIndex + 1));
        });
        
        return thumbnail;
    }
    
    private void updateWindowTitle() {
        if (primaryStage != null) {
            String title = "Simple Slide Editor";
            if (currentFile != null) {
                title += " - " + currentFile.getName();
            } else {
                title += " - Untitled Presentation";
            }
            primaryStage.setTitle(title);
        }
    }
    
    private void newPresentation() {
        presentation = new Presentation();
        currentSlideIndex = 0;
        currentFile = null;
        updateSlideDisplay();
        updateThumbnails();
        updateStatus("New presentation created");
        showAlert("New Presentation", "Created a new presentation.");
    }
    
    private void savePresentation() {
        if (currentFile == null) {
            saveAsPresentation();
        } else {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(currentFile))) {
                oos.writeObject(presentation);
                updateStatus("Saved: " + currentFile.getName());
                showAlert("Success", "Presentation saved successfully to:\n" + currentFile.getAbsolutePath());
            } catch (IOException e) {
                showAlert("Error", "Could not save presentation:\n" + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void saveAsPresentation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Presentation");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Slide Editor Files", "*.sle")
        );
        
        if (currentDirectory != null) {
            fileChooser.setInitialDirectory(currentDirectory);
        }
        
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            currentFile = file;
            currentDirectory = file.getParentFile();
            savePresentation();
            refreshFileTree();
        }
    }
    
    private void openPresentation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Presentation");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Slide Editor Files", "*.sle")
        );
        
        if (currentDirectory != null) {
            fileChooser.setInitialDirectory(currentDirectory);
        }
        
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            openPresentationFile(file);
            currentDirectory = file.getParentFile();
            refreshFileTree();
        }
    }
    
    private void addSlide() {
        presentation.addSlide();
        currentSlideIndex = presentation.getSlideCount() - 1;
        updateSlideDisplay();
        updateThumbnails();
        updateStatus("Added slide " + (currentSlideIndex + 1));
    }
    
    private void deleteSlide() {
        if (presentation.getSlideCount() > 1) {
            presentation.removeSlide(currentSlideIndex);
            if (currentSlideIndex >= presentation.getSlideCount()) {
                currentSlideIndex = presentation.getSlideCount() - 1;
            }
            updateSlideDisplay();
            updateThumbnails();
            updateStatus("Deleted slide " + (currentSlideIndex + 1));
        } else {
            showAlert("Cannot Delete", "You must have at least one slide.");
        }
    }
    
    private void previousSlide() {
        if (currentSlideIndex > 0) {
            currentSlideIndex--;
            updateSlideDisplay();
            updateThumbnails();
            updateStatus("Showing slide " + (currentSlideIndex + 1));
        }
    }
    
    private void nextSlide() {
        if (currentSlideIndex < presentation.getSlideCount() - 1) {
            currentSlideIndex++;
            updateSlideDisplay();
            updateThumbnails();
            updateStatus("Showing slide " + (currentSlideIndex + 1));
        }
    }
    
    private void playSlideshow() {
        if (presentation.getSlideCount() > 0) {
            SlideshowWindow slideshow = new SlideshowWindow(presentation);
            slideshow.show();
            updateStatus("Slideshow started");
        } else {
            showAlert("No Slides", "Please add some slides first.");
        }
    }
    
    private void addTextBox(String shapeType) {
        Slide currentSlide = presentation.getSlide(currentSlideIndex);
        double x = 100 + (currentSlide.getTextElements().size() * 20);
        double y = 100 + (currentSlide.getTextElements().size() * 20);
        
        String defaultText = "Double-click to edit";
        
        TextElement newElement = new TextElement(defaultText, x, y, 20, "black");
        newElement.setWidth(240);
        newElement.setHeight(60);
        newElement.setShapeType(shapeType);
        currentSlide.addTextElement(newElement);
        updateSlideDisplay();
        updateStatus("Added " + shapeType + " shape text box");
    }
    
    /**
     * Removes an arrow from the current slide and refreshes the display.
     * Called by ArrowNode's right-click context menu.
     */
    public void deleteArrow(ArrowElement arrow) {
        if (presentation.getSlideCount() == 0) return;
        presentation.getSlide(currentSlideIndex).removeArrow(arrow);
        updateSlideDisplay();
        updateStatus("Arrow deleted");
    }

    /**
     * Re-calculates auto positions for all non-manually-positioned arrows on
     * the current slide, then refreshes the display.
     * Called by ArrowNode's "Reset to Auto Position" context menu item.
     */
    public void refreshArrowPositions() {
        if (presentation.getSlideCount() == 0) return;
        Slide slide = presentation.getSlide(currentSlideIndex);
        for (ArrowElement arrow : slide.getArrows()) {
            if (!arrow.isManuallyPositioned()) {
                updateArrowPositions(arrow, slide);
            }
        }
        updateSlideDisplay();
        updateStatus("Arrow position reset");
    }

    public void updateTextElement(TextElement element, String newText) {
        element.setText(newText);
        updateSlideDisplay();
    }
    
    public void moveTextElement(TextElement element, double deltaX, double deltaY) {
        element.setX(element.getX() + deltaX);
        element.setY(element.getY() + deltaY);
        updateSlideDisplay();
    }
    
    public void selectElement(TextElement element) {
        this.selectedElement = element;
        view.getSlidePane().requestFocus();
        updateFormattingControls();
        if (connectMode && selectedStartElement == null && element != null) {
            selectedStartElement = element;
            updateStatus("Select target shape to create arrow");
        } else if (connectMode && selectedStartElement != null && element != null && selectedStartElement != element) {
            // Create arrow
            ArrowElement arrow = new ArrowElement(
                UUID.randomUUID().toString(),
                selectedStartElement.getId(),
                element.getId(),
                currentArrowType
            );
            arrow.setLineWidth(Double.parseDouble(view.getLineWidthCombo().getValue()));
            arrow.setColor(String.format("#%02X%02X%02X",
                (int)(view.getConnectorColorPicker().getValue().getRed() * 255),
                (int)(view.getConnectorColorPicker().getValue().getGreen() * 255),
                (int)(view.getConnectorColorPicker().getValue().getBlue() * 255)));
            
            Slide currentSlide = presentation.getSlide(currentSlideIndex);
            currentSlide.addArrow(arrow);
            updateSlideDisplay();
            selectedStartElement = null;
            updateStatus("Arrow created");
        } else if (connectMode) {
            selectedStartElement = null;
            updateStatus("Connect mode - Click on a shape to start");
        }
        updateStatus(selectedElement != null ? "Selected text element" : "Selection cleared");
    }
    
    private void updateFormattingControls() {
        if (selectedElement != null) {
            view.getFontSizeCombo().setValue(String.valueOf((int) selectedElement.getFontSize()));
            view.getFontColorPicker().setValue(Color.web(selectedElement.getColor()));

            // Reflect alignment state on the toggle buttons
            String alignment = selectedElement.getAlignment();
            view.getAlignLeftButton().setSelected("LEFT".equals(alignment));
            view.getAlignCenterButton().setSelected("CENTER".equals(alignment));
            view.getAlignRightButton().setSelected("RIGHT".equals(alignment));

            // Reflect bullet state
            view.getBulletListButton().setSelected(selectedElement.isBulletList());

            // Sync format panel — shape fill
            String shapeFill = selectedElement.getShapeFillColor();
            boolean shapeNone = "transparent".equalsIgnoreCase(shapeFill);
            view.getShapeFillNoneCheck().setSelected(shapeNone);
            if (!shapeNone) {
                view.getShapeFillPicker().setValue(Color.web(shapeFill));
            }

            // Sync format panel — text box fill
            String tbFill = selectedElement.getTextBoxFillColor();
            boolean tbNone = "transparent".equalsIgnoreCase(tbFill);
            view.getTextBoxFillNoneCheck().setSelected(tbNone);
            if (!tbNone) {
                view.getTextBoxFillPicker().setValue(Color.web(tbFill));
            }

            // Sync Text Container tab — font controls
            view.getTcFontFamilyCombo().setValue(selectedElement.getFontFamily());
            view.getTcFontSizeCombo().setValue(String.valueOf((int) selectedElement.getFontSize()));
            view.getTcFontColorPicker().setValue(Color.web(selectedElement.getColor()));
            view.getTcBoldButton().setSelected(selectedElement.isBold());
            view.getTcItalicButton().setSelected(selectedElement.isItalic());

            view.setFormattingControlsDisabled(false);
            view.setFormatPanelVisible(true);
        } else {
            view.setFormattingControlsDisabled(true);
            view.setFormatPanelVisible(false);
        }
    }
    
    private void toggleBold() {
        if (selectedElement != null) {
            selectedElement.setBold(!selectedElement.isBold());
            updateSlideDisplay();
            updateStatus(selectedElement.isBold() ? "Bold applied" : "Bold removed");
        }
    }
    
    private void toggleItalic() {
        if (selectedElement != null) {
            selectedElement.setItalic(!selectedElement.isItalic());
            updateSlideDisplay();
            updateStatus(selectedElement.isItalic() ? "Italic applied" : "Italic removed");
        }
    }
    
    private void changeFontSize() {
        if (selectedElement != null) {
            try {
                double size = Double.parseDouble(view.getFontSizeCombo().getValue());
                selectedElement.setFontSize(size);
                updateSlideDisplay();
                updateStatus("Font size changed to " + (int) size);
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        }
    }

    /**
     * Applies the toolbar font colour picker's chosen colour to the selected
     * element.  Works correctly whether or not the element is currently being
     * edited in its inline TextArea:
     *
     *  • Not editing  → update the element model and refresh the slide display.
     *  • Editing      → update the element model AND re-style the live TextArea
     *                   so the user sees the new colour immediately, WITHOUT
     *                   calling finishEditing (which would wipe unsaved changes).
     *
     * The focus listener in ShapeTextBoxNode skips finishEditing when focus
     * moves to this picker (userData="fontColor" + isFontColorPickerShowing()),
     * so liveTextArea is still valid when this onAction fires.
     */
    private void changeFontColor() {
        if (selectedElement == null) return;
        Color c = view.getFontColorPicker().getValue();
        String hex = String.format("#%02X%02X%02X",
            (int)(c.getRed() * 255),
            (int)(c.getGreen() * 255),
            (int)(c.getBlue() * 255));
        selectedElement.setColor(hex);
        // Keep the Text Container tab colour picker in sync
        view.getTcFontColorPicker().setValue(c);

        // If a TextArea is currently live, re-style it directly so colour appears
        // immediately without a full slide rebuild (which would exit editing mode).
        boolean appliedToLive = false;
        for (javafx.scene.Node child : view.getSlidePane().getChildren()) {
            if (child instanceof ShapeTextBoxNode) {
                ShapeTextBoxNode node = (ShapeTextBoxNode) child;
                if (node.getElement() == selectedElement && node.isEditing()) {
                    node.applyLiveFontColor(hex);
                    appliedToLive = true;
                    break;
                }
            }
        }
        if (!appliedToLive) {
            updateSlideDisplay();
        }
        updateStatus("Font colour changed");
    }
    
    private void changeSlideBackground() {
        if (presentation.getSlideCount() == 0) return;
        Color color = view.getSlideBackgroundPicker().getValue();
        String hex = String.format("#%02X%02X%02X",
            (int)(color.getRed()   * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue()  * 255));
        presentation.getSlide(currentSlideIndex).setBackground(hex);
        view.getSlidePane().setStyle("-fx-background-color: " + hex + ";");
        updateThumbnails();
        updateStatus("Slide background changed");
    }

    private void changeShapeFill() {
        if (selectedElement == null) return;
        String fill;
        if (view.getShapeFillNoneCheck().isSelected()) {
            fill = "transparent";
        } else {
            Color color = view.getShapeFillPicker().getValue();
            fill = String.format("#%02X%02X%02X",
                (int)(color.getRed()   * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue()  * 255));
        }
        selectedElement.setShapeFillColor(fill);
        // Apply directly to the live node without a full slide rebuild
        applyFillToSelectedNode();
        updateStatus("Shape fill changed");
    }

    private void changeTextBoxFill() {
        if (selectedElement == null) return;
        String fill;
        if (view.getTextBoxFillNoneCheck().isSelected()) {
            fill = "transparent";
        } else {
            Color color = view.getTextBoxFillPicker().getValue();
            fill = String.format("#%02X%02X%02X",
                (int)(color.getRed()   * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue()  * 255));
        }
        selectedElement.setTextBoxFillColor(fill);
        // Apply directly to the live node without a full slide rebuild
        applyFillToSelectedNode();
        updateStatus("Text box fill changed");
    }

    /**
     * Finds the ShapeTextBoxNode for the currently selected element and calls
     * applyFillColors() on it directly — avoiding a full updateSlideDisplay()
     * which would lose selection state and flicker.
     */
    private void applyFillToSelectedNode() {
        for (javafx.scene.Node child : view.getSlidePane().getChildren()) {
            if (child instanceof ShapeTextBoxNode) {
                ShapeTextBoxNode node = (ShapeTextBoxNode) child;
                if (node.getElement() == selectedElement) {
                    node.applyFillColors();
                    break;
                }
            }
        }
    }

    // ── Text Container tab handlers ───────────────────────────────────────────

    private void changeTcFontFamily() {
        if (selectedElement == null) return;
        String family = view.getTcFontFamilyCombo().getValue();
        if (family == null || family.isBlank()) return;
        selectedElement.setFontFamily(family);
        // Sync the main toolbar font-size combo so the two don't drift
        updateSlideDisplay();
        updateStatus("Font family changed to " + family);
    }

    private void changeTcFontSize() {
        if (selectedElement == null) return;
        try {
            double size = Double.parseDouble(view.getTcFontSizeCombo().getValue());
            selectedElement.setFontSize(size);
            // Keep the main toolbar size combo in sync
            view.getFontSizeCombo().setValue(String.valueOf((int) size));
            updateSlideDisplay();
            updateStatus("Font size changed to " + (int) size);
        } catch (NumberFormatException ignored) {}
    }

    private void changeTcFontColor() {
        if (selectedElement == null) return;
        Color c = view.getTcFontColorPicker().getValue();
        String hex = String.format("#%02X%02X%02X",
            (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
        selectedElement.setColor(hex);
        // Keep the main toolbar colour picker in sync
        view.getFontColorPicker().setValue(c);
        updateSlideDisplay();
        updateStatus("Font colour changed");
    }

    private void changeTcBold() {
        if (selectedElement == null) return;
        selectedElement.setBold(view.getTcBoldButton().isSelected());
        // Keep the main toolbar bold button in sync visually
        updateSlideDisplay();
        updateStatus(selectedElement.isBold() ? "Bold applied" : "Bold removed");
    }

    private void changeTcItalic() {
        if (selectedElement == null) return;
        selectedElement.setItalic(view.getTcItalicButton().isSelected());
        updateSlideDisplay();
        updateStatus(selectedElement.isItalic() ? "Italic applied" : "Italic removed");
    }

    // NEW: apply one of LEFT / CENTER / RIGHT to the selected element
    private void setAlignment(String alignment) {
        if (selectedElement != null) {
            selectedElement.setAlignment(alignment);
            updateSlideDisplay();
            updateStatus("Alignment set to " + alignment.charAt(0) + alignment.substring(1).toLowerCase());
        }
    }

    // NEW: toggle bullet-list mode on the selected element
    private void toggleBulletList() {
        if (selectedElement != null) {
            selectedElement.setBulletList(!selectedElement.isBulletList());
            updateSlideDisplay();
            updateStatus(selectedElement.isBulletList() ? "Bullet list enabled" : "Bullet list disabled");
        }
    }

    /**
     * Called when either highlight colour picker commits a colour (onAction).
     * Reads BOTH the background highlight picker and the text colour picker,
     * then applies them together so highlighted lines get their own text colour
     * independently of the element's global font colour.
     *
     * The TextArea focus listener skips finishEditing when focus moves to either
     * picker (userData "highlight" / "highlightText"), so liveTextArea is still
     * valid when this fires.
     */
    private void applyHighlightColor() {
        if (selectedElement == null) return;
        System.out.println("[APPLY-HIGHLIGHT] called, selectedElement=" + selectedElement.getId());

        javafx.scene.paint.Color bg = view.getHighlightColorPicker().getValue();
        String bgHex = String.format("#%02X%02X%02X",
                (int)(bg.getRed()   * 255),
                (int)(bg.getGreen() * 255),
                (int)(bg.getBlue()  * 255));

        javafx.scene.paint.Color tc = view.getHighlightTextColorPicker().getValue();
        String textHex = String.format("#%02X%02X%02X",
                (int)(tc.getRed()   * 255),
                (int)(tc.getGreen() * 255),
                (int)(tc.getBlue()  * 255));

        System.out.println("[APPLY-HIGHLIGHT] scanning " + view.getSlidePane().getChildren().size() + " children");
        for (javafx.scene.Node child : view.getSlidePane().getChildren()) {
            if (child instanceof ShapeTextBoxNode) {
                ShapeTextBoxNode node = (ShapeTextBoxNode) child;
                System.out.println("[APPLY-HIGHLIGHT] node=" + node.getElement().getId()
                    + " isEditing=" + node.isEditing()
                    + " matches=" + (node.getElement() == selectedElement));
                if (node.getElement() == selectedElement) {
                    if (node.isEditing()) {
                        System.out.println("[APPLY-HIGHLIGHT] --> EDITING path");
                        node.applyHighlightToSelectedLines(bgHex, textHex);
                        javafx.application.Platform.runLater(() -> node.requestEditingFocus());
                    } else {
                        System.out.println("[APPLY-HIGHLIGHT] --> NOT EDITING path -> calling updateSlideDisplay");
                        selectedElement.syncHighlightColors();
                        String[] lines2 = selectedElement.getText() == null
                                ? new String[]{""} : selectedElement.getText().split("\n", -1);
                        for (int i = 0; i < lines2.length; i++) {
                            selectedElement.setLineHighlightColor(i, bgHex);
                            selectedElement.setLineHighlightTextColor(i, textHex);
                        }
                        updateSlideDisplay();
                    }
                    updateStatus("Highlight colour applied");
                    return;
                }
            }
        }
        System.out.println("[APPLY-HIGHLIGHT] WARNING: no matching node found!");
    }

    /** Clears the highlight background and text colour from selected lines (or all lines if not editing). */
    private void clearHighlightColor() {
        if (selectedElement == null) return;
        for (javafx.scene.Node child : view.getSlidePane().getChildren()) {
            if (child instanceof ShapeTextBoxNode) {
                ShapeTextBoxNode node = (ShapeTextBoxNode) child;
                if (node.getElement() == selectedElement) {
                    if (node.isEditing()) {
                        node.applyHighlightToSelectedLines(null, null);
                    } else {
                        selectedElement.clearAllHighlights();
                        selectedElement.clearAllHighlightTextColors();
                        updateSlideDisplay();
                    }
                    updateStatus("Highlight cleared");
                    return;
                }
            }
        }
    }

    /**
     * Called from the indent buttons' onMousePressed handlers — fires while
     * the TextArea still has focus, so applyIndentFromToolbar can read the
     * live selection directly.
     */
    private void indentSelectedElement(int delta) {
        System.out.println("[INDENT] indentSelectedElement called, delta=" + delta
                + ", selectedElement=" + (selectedElement != null ? "SET" : "NULL"));
        if (selectedElement == null) return;
        for (javafx.scene.Node child : view.getSlidePane().getChildren()) {
            if (child instanceof ShapeTextBoxNode) {
                ShapeTextBoxNode node = (ShapeTextBoxNode) child;
                if (node.getElement() == selectedElement) {
                    System.out.println("[INDENT] Found matching node, isEditing=" + node.isEditing());
                    boolean applied = node.applyIndentFromToolbar(delta);
                    updateStatus(applied
                            ? (delta > 0 ? "Indent increased" : "Indent decreased")
                            : "Double-click the text box, highlight lines, then indent");
                    return;
                }
            }
        }
    }
    
    private void updateStatus(String message) {
        view.getStatusLabel().setText(message);
    }
    
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Simple Slide Editor");
        alert.setHeaderText("Simple Slide Editor");
        alert.setContentText("Version 3.0\n\n" +
                            "A professional presentation tool\n" +
                            "Built with JavaFX\n\n" +
                            "Features:\n" +
                            "• Create and edit slides with thumbnails\n" +
                            "• 9 different shape types for text boxes\n" +
                            "• Text formatting (Bold, Italic, Font Size, Color)\n" +
                            "• Arrow connectors between shapes\n" +
                            "• Drag and drop shapes\n" +
                            "• Delete elements with Delete key\n" +
                            "• File explorer integration\n" +
                            "• Save/Open presentations\n" +
                            "• Full-screen slideshow\n\n" +
                            "© 2024 All Rights Reserved");
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }
    
    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("Simple Slide Editor Help");
        alert.setContentText(
            "Getting Started:\n" +
            "1. Create a new presentation (Ctrl+N)\n" +
            "2. Add slides using the toolbar or Edit menu\n" +
            "3. Add text boxes with different shapes from the Shapes section\n" +
            "4. Double-click to edit text\n" +
            "5. Format text using the Format section\n" +
            "6. Create connections:\n" +
            "   - Click 'Connect Mode' button\n" +
            "   - Click first shape, then click second shape\n" +
            "   - Choose arrow type (Straight, Curved, Elbow)\n" +
            "7. Delete elements with Delete key\n" +
            "8. Save your work (Ctrl+S)\n" +
            "9. Play slideshow (F5)\n\n" +
            "Keyboard Shortcuts:\n" +
            "• Ctrl+N - New Presentation\n" +
            "• Ctrl+O - Open Presentation\n" +
            "• Ctrl+S - Save\n" +
            "• Ctrl+Shift+S - Save As\n" +
            "• Ctrl+Left/Right - Previous/Next Slide\n" +
            "• F5 - Play Slideshow\n" +
            "• Delete - Delete selected element\n" +
            "• Esc - Exit Full Screen"
        );
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }
    
    public BorderPane getView() {
        return view.getMainLayout();
    }
    
    public TextElement getSelectedElement() {
        return selectedElement;
    }

    /**
     * Returns true while either highlight colour-picker popup is open.
     * Called by ShapeTextBoxNode's TextArea focus listener to suppress
     * finishEditing while the user is choosing a colour inside the popup.
     */
    public boolean isHighlightPickerShowing() {
        return view.isHighlightPickerShowing();
    }

    /**
     * Returns true while either font-colour picker popup is open.
     * Called by ShapeTextBoxNode's TextArea focus listener to suppress
     * finishEditing while the user is choosing a colour inside the popup.
     */
    public boolean isFontColorPickerShowing() {
        return view.isFontColorPickerShowing();
    }

    // ── Image insertion ───────────────────────────────────────────────────────

    /**
     * Opens a FileChooser filtered to common image formats, creates an
     * {@link ImageElement} sized to fit the slide (up to 400×300 px),
     * adds it to the current slide, and refreshes the display.
     *
     * Keyboard shortcut: Ctrl+Shift+I (wired in setupEventHandlers via the
     * toolbar button; the button tooltip advertises the shortcut).
     */
    private void insertImage() {
        if (presentation.getSlideCount() == 0) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Insert Image");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        if (currentDirectory != null) {
            chooser.setInitialDirectory(currentDirectory);
        }

        File file = chooser.showOpenDialog(primaryStage);
        if (file == null) return;   // user cancelled

        // ── Determine a sensible default size ────────────────────────────────
        double defaultW = 400;
        double defaultH = 300;
        try {
            javafx.scene.image.Image probe =
                new javafx.scene.image.Image(file.toURI().toString(), true);
            // Wait for the image to finish loading so width/height are valid
            double iw = probe.getWidth();
            double ih = probe.getHeight();
            if (iw > 0 && ih > 0) {
                // Scale down to fit inside 400×300 while preserving aspect ratio
                double scale = Math.min(defaultW / iw, defaultH / ih);
                if (scale < 1.0) {
                    defaultW = iw * scale;
                    defaultH = ih * scale;
                } else {
                    defaultW = iw;
                    defaultH = ih;
                }
            }
        } catch (Exception ignored) {
            // Use defaults if the image can't be probed
        }

        // ── Place image roughly centred on the slide ──────────────────────────
        double paneW = view.getSlidePane().getWidth();
        double paneH = view.getSlidePane().getHeight();
        double x = Math.max(10, (paneW - defaultW) / 2);
        double y = Math.max(10, (paneH - defaultH) / 2);

        String id = "img_" + java.util.UUID.randomUUID();
        ImageElement img = new ImageElement(id, file.getAbsolutePath(),
                                            x, y, defaultW, defaultH);

        Slide currentSlide = presentation.getSlide(currentSlideIndex);
        currentSlide.addImage(img);
        updateSlideDisplay();
        updateStatus("Image inserted: " + file.getName());
    }

    /**
     * Called by {@link ImageNode} when it is clicked.
     * Clears any text-element selection, then selects the image node.
     */
    public void selectImageElement(ImageNode node) {
        // Deselect all text nodes
        view.getSlidePane().getChildren().forEach(child -> {
            if (child instanceof ShapeTextBoxNode) ((ShapeTextBoxNode) child).deselect();
            if (child instanceof ImageNode && child != node) ((ImageNode) child).deselect();
        });
        selectedElement   = null;
        selectedImageNode = node;
        node.select();
        updateFormattingControls();   // disables text-specific toolbar controls
        view.getSlidePane().requestFocus();
        updateStatus("Image selected — drag to move, handles to resize, Delete to remove");
    }

    /**
     * Removes an image from the current slide and refreshes the display.
     * Called from {@link ImageNode}'s Delete key / context menu, and from
     * the slide-pane Delete key handler in this controller.
     */
    public void deleteImageElement(ImageNode node) {
        if (presentation.getSlideCount() == 0) return;
        Slide currentSlide = presentation.getSlide(currentSlideIndex);
        currentSlide.removeImage(node.getElement());
        if (selectedImageNode == node) selectedImageNode = null;
        updateSlideDisplay();
        updateStatus("Image deleted");
    }
}