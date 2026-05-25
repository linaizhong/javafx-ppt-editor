package slideeditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class SlideEditorView {
    private BorderPane mainLayout;
    private MenuBar menuBar;

    // Single merged toolbar (replaces topToolBar + formattingToolBar + shapesToolBar)
    private ToolBar mainToolBar;

    private SplitPane mainSplitPane;
    private TabPane leftTabPane;
    private ListView<HBox> thumbnailListView;
    private TreeView<String> fileTreeView;
    private ListView<String> elementsListView;
    private Pane slidePane;
    private ScrollPane slideScrollPane;
    private HBox statusBar;

    // Menu items
    private MenuItem newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, exitMenuItem;
    private MenuItem addSlideMenuItem, deleteSlideMenuItem, previousSlideMenuItem, nextSlideMenuItem;
    private MenuItem playMenuItem, exitFullScreenMenuItem;
    private MenuItem aboutMenuItem, helpMenuItem;

    // Toolbar buttons — file / slide / navigation
    private Button newButton, openButton, saveButton;
    private Button addSlideButton, deleteSlideButton;
    private Button prevButton, nextButton, playButton;
    private Button addTextButton;
    private Button addImageButton;             // ← NEW: Insert Image from file
    private ProgressIndicator progressIndicator;

    // Formatting controls (disabled when no element is selected)
    private Button boldButton, italicButton, underlineButton;
    private ComboBox<String> fontSizeCombo;
    private ColorPicker fontColorPicker;
    private ToggleButton alignLeftButton, alignCenterButton, alignRightButton;
    private ToggleButton bulletListButton;
    private Button indentIncreaseButton, indentDecreaseButton;
    private ColorPicker highlightColorPicker;
    private ColorPicker highlightTextColorPicker;
    private Button      clearHighlightButton;

    // Shape buttons
    private Button rectangleShapeButton, roundedRectShapeButton, circleShapeButton, calloutShapeButton;
    private Button diamondShapeButton, pentagonShapeButton, hexagonShapeButton, starShapeButton, parallelogramShapeButton;

    // Connector buttons
    private ToggleButton connectModeButton;
    private Button straightArrowButton, curvedArrowButton, elbowArrowButton;
    private ColorPicker connectorColorPicker;
    private ComboBox<String> lineWidthCombo;

    // Slide background colour picker (always enabled — not tied to element selection)
    private ColorPicker slideBackgroundPicker;

    // File explorer buttons
    private Button browseButton, refreshButton;

    // Slide counter and status labels
    private Label slideCounter, statusLabel;

    // ── Right-side Format Panel (PowerPoint-style, tabbed) ───────────────────
    private TabPane  formatTabPane;          // replaces old VBox formatPanel
    private Tab      shapeTab;               // "Shape Fill" tab
    private Tab      textContainerTab;       // "Text Container" tab
    private VBox     shapeFillContent;       // content shown inside shapeTab
    private VBox     textContainerContent;   // content shown inside textContainerTab
    private ColorPicker shapeFillPicker;
    private CheckBox    shapeFillNoneCheck;
    private ColorPicker textBoxFillPicker;
    private CheckBox    textBoxFillNoneCheck;
    // per-tab font / text-container controls
    private ColorPicker tcFontColorPicker;
    private ComboBox<String> tcFontSizeCombo;
    private ComboBox<String> tcFontFamilyCombo;
    private ToggleButton tcBoldButton;
    private ToggleButton tcItalicButton;

    // Formatting controls that must be individually toggled when selection changes
    private javafx.scene.Node[] formattingNodes;

    public SlideEditorView() {
        initializeUI();
        applyProfessionalStyling();
    }

    private void initializeUI() {
        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f3f6f9;");

        createMenuBar();
        createMainToolBar();   // single merged toolbar
        createSplitPanes();
        createStatusBar();

        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(menuBar, mainToolBar);
        mainLayout.setTop(topContainer);
        mainLayout.setCenter(mainSplitPane);
        mainLayout.setRight(createFormatTabPane());
        mainLayout.setBottom(statusBar);
    }

    private void applyProfessionalStyling() {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        mainLayout.setEffect(shadow);
    }

    // ── Menu bar ─────────────────────────────────────────────────────────────

    private void createMenuBar() {
        menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa); -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        String menuStyle     = "-fx-text-fill: #495057; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6px 12px;";
        String menuItemStyle = "-fx-text-fill: #495057; -fx-font-size: 12px; -fx-padding: 6px 20px;";

        // File Menu
        Menu fileMenu = new Menu("File");
        fileMenu.setStyle(menuStyle);
        newMenuItem    = new MenuItem("📄 New Presentation");    newMenuItem.setStyle(menuItemStyle);
        newMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
        openMenuItem   = new MenuItem("📂 Open Presentation..."); openMenuItem.setStyle(menuItemStyle);
        openMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        saveMenuItem   = new MenuItem("💾 Save");                saveMenuItem.setStyle(menuItemStyle);
        saveMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        saveAsMenuItem = new MenuItem("💾 Save As...");          saveAsMenuItem.setStyle(menuItemStyle);
        saveAsMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+S"));
        exitMenuItem   = new MenuItem("🚪 Exit");                exitMenuItem.setStyle(menuItemStyle);
        exitMenuItem.setAccelerator(KeyCombination.keyCombination("Alt+F4"));
        fileMenu.getItems().addAll(newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem,
                new SeparatorMenuItem(), exitMenuItem);

        // Edit Menu
        Menu editMenu = new Menu("Edit");
        editMenu.setStyle(menuStyle);
        addSlideMenuItem      = new MenuItem("➕ Add Slide");      addSlideMenuItem.setStyle(menuItemStyle);
        addSlideMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+A"));
        deleteSlideMenuItem   = new MenuItem("➖ Delete Slide");   deleteSlideMenuItem.setStyle(menuItemStyle);
        deleteSlideMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+D"));
        previousSlideMenuItem = new MenuItem("◀ Previous Slide"); previousSlideMenuItem.setStyle(menuItemStyle);
        previousSlideMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Left"));
        nextSlideMenuItem     = new MenuItem("▶ Next Slide");     nextSlideMenuItem.setStyle(menuItemStyle);
        nextSlideMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Right"));
        editMenu.getItems().addAll(addSlideMenuItem, deleteSlideMenuItem,
                new SeparatorMenuItem(), previousSlideMenuItem, nextSlideMenuItem);

        // View Menu
        Menu viewMenu = new Menu("View");
        viewMenu.setStyle(menuStyle);
        playMenuItem           = new MenuItem("▶▶ Play Slideshow");  playMenuItem.setStyle(menuItemStyle);
        playMenuItem.setAccelerator(KeyCombination.keyCombination("F5"));
        exitFullScreenMenuItem = new MenuItem("⛶ Exit Full Screen"); exitFullScreenMenuItem.setStyle(menuItemStyle);
        exitFullScreenMenuItem.setAccelerator(KeyCombination.keyCombination("Esc"));
        exitFullScreenMenuItem.setDisable(true);
        viewMenu.getItems().addAll(playMenuItem, exitFullScreenMenuItem);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        helpMenu.setStyle(menuStyle);
        helpMenuItem  = new MenuItem("❓ Help Topics"); helpMenuItem.setStyle(menuItemStyle);
        helpMenuItem.setAccelerator(KeyCombination.keyCombination("F1"));
        aboutMenuItem = new MenuItem("ℹ️ About");      aboutMenuItem.setStyle(menuItemStyle);
        helpMenu.getItems().addAll(helpMenuItem, new SeparatorMenuItem(), aboutMenuItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, helpMenu);
    }

    // ── Single merged toolbar ─────────────────────────────────────────────────

    private void createMainToolBar() {
        mainToolBar = new ToolBar();
        mainToolBar.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f8f8); " +
                             "-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        mainToolBar.setPadding(new Insets(3, 8, 3, 8));

        // ── Section 1: File ──────────────────────────────────────────────────
        newButton         = createProfessionalButton("📄", "New Presentation (Ctrl+N)",           "#4CAF50");
        openButton        = createProfessionalButton("📂", "Open Presentation (Ctrl+O)",          "#2196F3");
        saveButton        = createProfessionalButton("💾", "Save (Ctrl+S)",                       "#FF9800");

        // ── Section 2: Slides ────────────────────────────────────────────────
        addSlideButton    = createProfessionalButton("➕", "Add New Slide (Ctrl+Shift+A)",         "#9C27B0");
        deleteSlideButton = createProfessionalButton("➖", "Delete Current Slide (Ctrl+Shift+D)", "#f44336");
        prevButton        = createProfessionalButton("◀",  "Previous Slide (Ctrl+Left)",          "#607D8B");
        nextButton        = createProfessionalButton("▶",  "Next Slide (Ctrl+Right)",             "#607D8B");
        playButton        = createProfessionalButton("▶▶", "Play Slideshow (F5)",                 "#00BCD4");
        addTextButton     = createProfessionalButton("📝", "Add Text Box",                        "#FF5722");
        addImageButton    = createProfessionalButton("🖼", "Insert Image (Ctrl+Shift+I)",          "#009688");

        // ── Slide background colour ──────────────────────────────────────────
        Label bgLabel = new Label("🎨");
        bgLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #5f6368;");
        bgLabel.setTooltip(new Tooltip("Slide Background Color"));

        slideBackgroundPicker = new ColorPicker(Color.WHITE);
        slideBackgroundPicker.setPrefWidth(58);
        slideBackgroundPicker.setTooltip(new Tooltip("Slide Background Color"));
        slideBackgroundPicker.setStyle("-fx-color-label-visible: false;");
        Label formatLabel = new Label("✏️");
        formatLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5f6368;");
        formatLabel.setTooltip(new Tooltip("Text Formatting"));

        boldButton      = createFormattingButton("B", "Bold (Ctrl+B)",      true);
        italicButton    = createFormattingButton("I", "Italic (Ctrl+I)",    false);
        underlineButton = createFormattingButton("U", "Underline (Ctrl+U)", false);
        underlineButton.setDisable(true); // not yet implemented

        Label sizeLabel = new Label("Size:");
        sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");
        fontSizeCombo = new ComboBox<>();
        fontSizeCombo.getItems().addAll("8","9","10","11","12","14","16","18","20","24","28","32","36","48","72");
        fontSizeCombo.setValue("20");
        fontSizeCombo.setPrefWidth(65);
        fontSizeCombo.setEditable(true);

        Label colorLabel = new Label("Color:");
        colorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");
        fontColorPicker = new ColorPicker(Color.BLACK);
        fontColorPicker.setPrefWidth(58);
        fontColorPicker.setUserData("fontColor");   // guards finishEditing in ShapeTextBoxNode

        ToggleGroup alignGroup = new ToggleGroup();
        alignLeftButton   = createAlignButton("⇤", "Align Left",   alignGroup);
        alignCenterButton = createAlignButton("≡", "Align Center", alignGroup);
        alignRightButton  = createAlignButton("⇥", "Align Right",  alignGroup);
        alignLeftButton.setSelected(true);

        bulletListButton = createAlignButton("☰", "Toggle Bullet List", new ToggleGroup());

        indentIncreaseButton = createFormattingButton("→|", "Increase Indent (Tab)",       false);
        indentDecreaseButton = createFormattingButton("|←", "Decrease Indent (Shift+Tab)", false);
        indentIncreaseButton.setUserData("increase");
        indentDecreaseButton.setUserData("decrease");

        // ── Highlight colour ─────────────────────────────────────────────────
        // Double-click into the text box, highlight lines, then pick a colour.
        // Uses the same focus-listener interception pattern as the indent buttons.
        Label highlightLabel = new Label("🖊");
        highlightLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5f6368;");
        highlightLabel.setTooltip(new Tooltip("Line Highlight Color"));

        highlightColorPicker = new ColorPicker(javafx.scene.paint.Color.YELLOW);
        highlightColorPicker.setPrefWidth(58);
        highlightColorPicker.setStyle("-fx-color-label-visible: false;");
        highlightColorPicker.setTooltip(new Tooltip("Highlight background colour for selected lines"));
        // userData "highlight" lets the TextArea focus listener detect this picker
        highlightColorPicker.setUserData("highlight");

        // Text colour applied to highlighted lines (independent of global font colour)
        Label highlightTextLabel = new Label("A");
        highlightTextLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #5f6368;");
        highlightTextLabel.setTooltip(new Tooltip("Highlight text colour"));

        highlightTextColorPicker = new ColorPicker(javafx.scene.paint.Color.BLACK);
        highlightTextColorPicker.setPrefWidth(58);
        highlightTextColorPicker.setStyle("-fx-color-label-visible: false;");
        highlightTextColorPicker.setTooltip(new Tooltip("Text colour for highlighted lines"));
        highlightTextColorPicker.setUserData("highlightText");

        clearHighlightButton = createFormattingButton("✕H", "Clear Highlight", false);
        clearHighlightButton.setTooltip(new Tooltip("Remove highlight from selected lines"));
        clearHighlightButton.setUserData("clearHighlight");

        // Collect all formatting controls so the controller can enable/disable them together
        formattingNodes = new javafx.scene.Node[]{
            formatLabel, boldButton, italicButton,
            sizeLabel, fontSizeCombo,
            alignLeftButton, alignCenterButton, alignRightButton, bulletListButton,
            indentIncreaseButton, indentDecreaseButton,
            highlightLabel, highlightColorPicker,
            highlightTextLabel, highlightTextColorPicker, clearHighlightButton
        };
        setFormattingControlsDisabled(true); // disabled until a shape is selected

        // ── Section 4: Shapes ────────────────────────────────────────────────
        Label shapeLabel = new Label("🔷");
        shapeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5f6368;");
        shapeLabel.setTooltip(new Tooltip("Insert Shape"));

        rectangleShapeButton     = createShapeButton("⬜", "Rectangle",         "#4CAF50");
        roundedRectShapeButton   = createShapeButton("🔲", "Rounded Rectangle", "#2196F3");
        circleShapeButton        = createShapeButton("⚪", "Circle",            "#FF9800");
        calloutShapeButton       = createShapeButton("💬", "Callout",           "#9C27B0");
        diamondShapeButton       = createShapeButton("🔶", "Diamond",           "#f44336");
        pentagonShapeButton      = createShapeButton("⬟", "Pentagon",          "#00BCD4");
        hexagonShapeButton       = createShapeButton("⬡", "Hexagon",           "#795548");
        starShapeButton          = createShapeButton("⭐", "Star",              "#FFC107");
        parallelogramShapeButton = createShapeButton("🔷", "Parallelogram",     "#607D8B");

        // ── Section 5: Connectors ────────────────────────────────────────────
        Label connectorLabel = new Label("🔗");
        connectorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5f6368;");
        connectorLabel.setTooltip(new Tooltip("Connectors"));

        connectModeButton = new ToggleButton("🔌 Connect");
        connectModeButton.setStyle("-fx-background-color: #e8f0fe; -fx-border-color: #1a73e8; " +
                                   "-fx-border-radius: 4px; -fx-padding: 4px 10px; -fx-font-size: 11px;");

        straightArrowButton = createConnectorButton("➡️", "Straight Arrow", "#1a73e8");
        curvedArrowButton   = createConnectorButton("〰️", "Curved Arrow",   "#ea4335");
        elbowArrowButton    = createConnectorButton("↪️", "Elbow Arrow",    "#34a853");

        Label lineLabel = new Label("Width:");
        lineLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");
        lineWidthCombo = new ComboBox<>();
        lineWidthCombo.getItems().addAll("1","2","3","4","5");
        lineWidthCombo.setValue("2");
        lineWidthCombo.setPrefWidth(50);

        Label connectorColorLabel = new Label("Line:");
        connectorColorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");
        connectorColorPicker = new ColorPicker(Color.BLUE);
        connectorColorPicker.setPrefWidth(50);

        // ── Progress indicator (right-aligned) ───────────────────────────────
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.setMaxSize(20, 20);
        progressIndicator.setVisible(false);

        // ── Assemble all sections with separators ────────────────────────────
        mainToolBar.getItems().addAll(
            // File
            newButton, openButton, saveButton,
            new Separator(),
            // Slides
            addSlideButton, deleteSlideButton, prevButton, nextButton, playButton, addTextButton, addImageButton,
            new Separator(),
            // Slide background
            bgLabel, slideBackgroundPicker,
            new Separator(),
            // Formatting
            formatLabel, boldButton, italicButton,
            sizeLabel, fontSizeCombo,
            alignLeftButton, alignCenterButton, alignRightButton, bulletListButton,
            indentDecreaseButton, indentIncreaseButton,
            highlightLabel, highlightColorPicker, highlightTextLabel, highlightTextColorPicker, clearHighlightButton,
            new Separator(),
            // Shapes
            shapeLabel,
            rectangleShapeButton, roundedRectShapeButton, circleShapeButton,
            calloutShapeButton, diamondShapeButton, pentagonShapeButton,
            hexagonShapeButton, starShapeButton, parallelogramShapeButton,
            new Separator(),
            // Connectors
            connectorLabel, connectModeButton,
            straightArrowButton, curvedArrowButton, elbowArrowButton,
            lineLabel, lineWidthCombo, connectorColorLabel, connectorColorPicker,
            // Spacer + progress
            spacer, progressIndicator
        );
    }

    /**
     * Enables or disables only the formatting controls, leaving the rest of the
     * toolbar (shapes, connectors, file/slide buttons) always interactive.
     * Called by the controller when selection changes.
     */
    public void setFormattingControlsDisabled(boolean disabled) {
        for (javafx.scene.Node node : formattingNodes) {
            node.setDisable(disabled);
        }
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private void createSplitPanes() {
        mainSplitPane = new SplitPane();
        mainSplitPane.setDividerPositions(0.22);
        mainSplitPane.setStyle("-fx-background-color: #f3f6f9;");

        leftTabPane = createTabbedPanel();
        VBox slideEditorPanel = createSlideEditorPanel();

        mainSplitPane.getItems().addAll(leftTabPane, slideEditorPanel);
    }

    private TabPane createTabbedPanel() {
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0; -fx-border-width: 0 1 0 0;");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab slidesTab = new Tab("📋 Slides");   slidesTab.setClosable(false); slidesTab.setContent(createSlidesPanel());
        Tab filesTab  = new Tab("📁 Files");    filesTab.setClosable(false);  filesTab.setContent(createFileBrowserPanel());
        Tab elemTab   = new Tab("🔧 Elements"); elemTab.setClosable(false);   elemTab.setContent(createElementsPanel());

        tabPane.getTabs().addAll(slidesTab, filesTab, elemTab);
        return tabPane;
    }

    private VBox createSlidesPanel() {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: #fafafa;");

        HBox headerBox = new HBox(8);
        headerBox.setPadding(new Insets(12));
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle("-fx-background-color: #2c3e50;");
        Label icon = new Label("📋"); icon.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");
        Label hdr  = new Label("Slide Thumbnails"); hdr.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        headerBox.getChildren().addAll(icon, hdr);

        thumbnailListView = new ListView<>();
        thumbnailListView.setStyle("-fx-background-color: #fafafa;");
        thumbnailListView.setCellFactory(lv -> new ThumbnailCell());

        VBox.setVgrow(thumbnailListView, Priority.ALWAYS);
        panel.getChildren().addAll(headerBox, thumbnailListView);
        return panel;
    }

    private VBox createFileBrowserPanel() {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: #fafafa;");

        HBox headerBox = new HBox(8);
        headerBox.setPadding(new Insets(12));
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle("-fx-background-color: #3498db;");
        Label icon = new Label("📁"); icon.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");
        Label hdr  = new Label("File Browser"); hdr.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        headerBox.getChildren().addAll(icon, hdr);

        fileTreeView = new TreeView<>();
        fileTreeView.setStyle("-fx-background-color: #fafafa;");
        fileTreeView.setShowRoot(true);

        HBox buttonBar = new HBox(8);
        buttonBar.setPadding(new Insets(10));
        buttonBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");
        buttonBar.setAlignment(Pos.CENTER);
        browseButton  = createSmallButton("📂 Browse",  "#2196F3");
        refreshButton = createSmallButton("🔄 Refresh", "#4CAF50");
        buttonBar.getChildren().addAll(browseButton, refreshButton);

        VBox.setVgrow(fileTreeView, Priority.ALWAYS);
        panel.getChildren().addAll(headerBox, fileTreeView, buttonBar);
        return panel;
    }

    private VBox createElementsPanel() {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: #fafafa;");

        HBox headerBox = new HBox(8);
        headerBox.setPadding(new Insets(12));
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle("-fx-background-color: #9C27B0;");
        Label icon = new Label("🔧"); icon.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");
        Label hdr  = new Label("Elements on this slide"); hdr.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        headerBox.getChildren().addAll(icon, hdr);

        elementsListView = new ListView<>();
        elementsListView.setStyle("-fx-background-color: #fafafa;");

        VBox.setVgrow(elementsListView, Priority.ALWAYS);
        panel.getChildren().addAll(headerBox, elementsListView);
        return panel;
    }

    private VBox createSlideEditorPanel() {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: #f3f6f9;");

        // FIX: replaced the broken StackPane+Rectangle approach with a plain HBox
        // that carries its own dark background — white label text is now always readable.
        HBox counterBar = new HBox(10);
        counterBar.setPadding(new Insets(12, 20, 12, 20));
        counterBar.setAlignment(Pos.CENTER_LEFT);
        counterBar.setStyle("-fx-background-color: linear-gradient(to right, #2c3e50, #34495e);");

        Label slideIcon = new Label("📊");
        slideIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

        slideCounter = new Label("Slide 1 of 1");
        slideCounter.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        counterBar.getChildren().addAll(slideIcon, slideCounter, spacer);

        slidePane = new Pane();
        slidePane.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 2px; " +
                           "-fx-border-radius: 12px; -fx-background-radius: 12px; " +
                           "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4);");
        slidePane.setPrefSize(900, 600);
        slidePane.setMinSize(700, 500);

        slideScrollPane = new ScrollPane(slidePane);
        slideScrollPane.setFitToWidth(true);
        slideScrollPane.setFitToHeight(true);
        slideScrollPane.setStyle("-fx-background-color: #e8f0f5; -fx-border-color: transparent;");
        slideScrollPane.setPadding(new Insets(20));

        VBox.setVgrow(slideScrollPane, Priority.ALWAYS);
        panel.getChildren().addAll(counterBar, slideScrollPane);
        return panel;
    }

    // ── Right-side Format Panel (tabbed) ─────────────────────────────────────

    /**
     * Builds the right-side panel as a TabPane with two tabs:
     *   • "Shape"          – outer shape fill colour
     *   • "Text Container" – inner text-box fill colour, font family/size/style
     * Each tab shows a "select a shape" placeholder when nothing is selected,
     * and reveals its controls when an element is selected.
     */
    private TabPane createFormatTabPane() {
        formatTabPane = new TabPane();
        formatTabPane.setPrefWidth(220);
        formatTabPane.setMinWidth(220);
        formatTabPane.setMaxWidth(220);
        formatTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        formatTabPane.setStyle(
            "-fx-background-color: #f8f9fa;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-width: 0 0 0 1;"
        );

        // ── Tab 1: Shape ──────────────────────────────────────────────────────
        shapeTab = new Tab("⬜ Shape");
        shapeTab.setClosable(false);
        shapeTab.setContent(buildShapeTabContent());

        // ── Tab 2: Text Container ─────────────────────────────────────────────
        textContainerTab = new Tab("📝 Text Container");
        textContainerTab.setClosable(false);
        textContainerTab.setContent(buildTextContainerTabContent());

        formatTabPane.getTabs().addAll(shapeTab, textContainerTab);
        return formatTabPane;
    }

    /** Placeholder shown in both tabs when no element is selected. */
    private VBox buildPlaceholder(String message) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        Label icon  = new Label("🖱️");
        icon.setStyle("-fx-font-size: 28px;");
        Label lbl = new Label(message);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #999; -fx-text-alignment: center;");
        lbl.setWrapText(true);
        box.getChildren().addAll(icon, lbl);
        return box;
    }

    // ── Shape tab ─────────────────────────────────────────────────────────────

    private ScrollPane buildShapeTabContent() {
        // No-selection placeholder
        VBox placeholder = buildPlaceholder("Select a shape to\nformat its fill colour");

        // ── Shape Fill section ────────────────────────────────────────────────
        VBox fillSection = new VBox(8);
        fillSection.setPadding(new Insets(14, 12, 8, 12));
        fillSection.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #e8e8e8;" +
            "-fx-border-width: 0 0 1 0;"
        );
        Label fillHeader = new Label("⬜  Shape Fill");
        fillHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3c4043;");

        shapeFillNoneCheck = new CheckBox("No Fill (Transparent)");
        shapeFillNoneCheck.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");

        HBox fillRow = new HBox(8);
        fillRow.setAlignment(Pos.CENTER_LEFT);
        Label fillLabel = new Label("Color:");
        fillLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");
        shapeFillPicker = new ColorPicker(Color.web("#f0f0f0"));
        shapeFillPicker.setPrefWidth(110);
        shapeFillPicker.setStyle("-fx-color-label-visible: false;");
        fillRow.getChildren().addAll(fillLabel, shapeFillPicker);

        shapeFillNoneCheck.selectedProperty().addListener((obs, wasOn, isOn) ->
            fillRow.setDisable(isOn));

        fillSection.getChildren().addAll(fillHeader, shapeFillNoneCheck, fillRow);

        // Content VBox: starts with placeholder, swapped on selection
        shapeFillContent = new VBox();
        shapeFillContent.getChildren().add(placeholder);
        shapeFillContent.setUserData(new VBox[]{placeholder, fillSection});

        ScrollPane sp = new ScrollPane(shapeFillContent);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: transparent;");
        return sp;
    }

    // ── Text Container tab ────────────────────────────────────────────────────

    private ScrollPane buildTextContainerTabContent() {
        // No-selection placeholder
        VBox placeholder = buildPlaceholder("Select a shape to\nformat its text container");

        // ── Text Box Fill section ─────────────────────────────────────────────
        VBox tbFillSection = new VBox(8);
        tbFillSection.setPadding(new Insets(14, 12, 8, 12));
        tbFillSection.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #e8e8e8;" +
            "-fx-border-width: 0 0 1 0;"
        );
        Label tbFillHeader = new Label("🎨  Background Fill");
        tbFillHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3c4043;");

        textBoxFillNoneCheck = new CheckBox("No Fill (Transparent)");
        textBoxFillNoneCheck.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");
        textBoxFillNoneCheck.setSelected(true);

        HBox tbFillRow = new HBox(8);
        tbFillRow.setAlignment(Pos.CENTER_LEFT);
        Label tbFillLabel = new Label("Color:");
        tbFillLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");
        textBoxFillPicker = new ColorPicker(Color.WHITE);
        textBoxFillPicker.setPrefWidth(110);
        textBoxFillPicker.setStyle("-fx-color-label-visible: false;");
        tbFillRow.getChildren().addAll(tbFillLabel, textBoxFillPicker);

        textBoxFillNoneCheck.selectedProperty().addListener((obs, wasOn, isOn) ->
            tbFillRow.setDisable(isOn));
        tbFillRow.setDisable(true);

        tbFillSection.getChildren().addAll(tbFillHeader, textBoxFillNoneCheck, tbFillRow);

        // ── Font section ──────────────────────────────────────────────────────
        VBox fontSection = new VBox(8);
        fontSection.setPadding(new Insets(14, 12, 8, 12));
        fontSection.setStyle("-fx-background-color: white;");

        Label fontHeader = new Label("✏️  Text Formatting");
        fontHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3c4043;");

        // Font family
        Label familyLabel = new Label("Font:");
        familyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");
        tcFontFamilyCombo = new ComboBox<>();
        tcFontFamilyCombo.getItems().addAll(
            "Arial", "Calibri", "Cambria", "Comic Sans MS", "Courier New",
            "Georgia", "Helvetica", "Impact", "Tahoma", "Times New Roman",
            "Trebuchet MS", "Verdana");
        tcFontFamilyCombo.setValue("Arial");
        tcFontFamilyCombo.setPrefWidth(180);
        tcFontFamilyCombo.setStyle("-fx-font-size: 11px;");

        // Font size row
        HBox sizeRow = new HBox(8);
        sizeRow.setAlignment(Pos.CENTER_LEFT);
        Label sizeLabel = new Label("Size:");
        sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");
        tcFontSizeCombo = new ComboBox<>();
        tcFontSizeCombo.getItems().addAll(
            "8","9","10","11","12","14","16","18","20","24","28","32","36","48","72");
        tcFontSizeCombo.setValue("20");
        tcFontSizeCombo.setPrefWidth(70);
        tcFontSizeCombo.setEditable(true);
        tcFontSizeCombo.setStyle("-fx-font-size: 11px;");
        sizeRow.getChildren().addAll(sizeLabel, tcFontSizeCombo);

        // Font colour row
        HBox colorRow = new HBox(8);
        colorRow.setAlignment(Pos.CENTER_LEFT);
        Label colorLabel2 = new Label("Color:");
        colorLabel2.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");
        tcFontColorPicker = new ColorPicker(Color.BLACK);
        tcFontColorPicker.setPrefWidth(145);
        tcFontColorPicker.setStyle("-fx-color-label-visible: false;");
        tcFontColorPicker.setUserData("fontColor");   // guards finishEditing in ShapeTextBoxNode
        colorRow.getChildren().addAll(colorLabel2, tcFontColorPicker);

        // Bold / Italic toggle buttons
        HBox styleRow = new HBox(6);
        styleRow.setAlignment(Pos.CENTER_LEFT);
        Label styleLabel = new Label("Style:");
        styleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368;");

        ToggleGroup styleGroup = new ToggleGroup();    // independent toggles — no group enforcement
        tcBoldButton   = new ToggleButton("B");
        tcItalicButton = new ToggleButton("I");
        String btnBase = "-fx-font-size: 12px; -fx-pref-width: 32px; -fx-pref-height: 26px;" +
                         "-fx-cursor: hand; -fx-border-radius: 4px; -fx-background-radius: 4px;" +
                         "-fx-background-color: #f1f3f4; -fx-border-color: #cccccc;";
        String btnSel  = "-fx-font-size: 12px; -fx-pref-width: 32px; -fx-pref-height: 26px;" +
                         "-fx-cursor: hand; -fx-border-radius: 4px; -fx-background-radius: 4px;" +
                         "-fx-background-color: #e8f0fe; -fx-border-color: #1a73e8; -fx-text-fill: #1a73e8;";
        tcBoldButton.setStyle(btnBase);
        tcBoldButton.setStyle("-fx-font-weight: bold;" + btnBase);
        tcItalicButton.setStyle("-fx-font-style: italic;" + btnBase);
        tcBoldButton.selectedProperty().addListener((obs, o, n) ->
            tcBoldButton.setStyle(n ? "-fx-font-weight: bold;" + btnSel : "-fx-font-weight: bold;" + btnBase));
        tcItalicButton.selectedProperty().addListener((obs, o, n) ->
            tcItalicButton.setStyle(n ? "-fx-font-style: italic;" + btnSel : "-fx-font-style: italic;" + btnBase));

        styleRow.getChildren().addAll(styleLabel, tcBoldButton, tcItalicButton);

        fontSection.getChildren().addAll(fontHeader, familyLabel, tcFontFamilyCombo, sizeRow, colorRow, styleRow);

        // Assemble inner VBox (placeholder until element is selected)
        textContainerContent = new VBox();
        textContainerContent.getChildren().add(placeholder);
        textContainerContent.setUserData(new VBox[]{placeholder, tbFillSection, fontSection});

        ScrollPane sp = new ScrollPane(textContainerContent);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: transparent;");
        return sp;
    }

    /**
     * Show the format controls (shape selected) or the placeholder (nothing selected).
     * Called by the controller on selection change.
     */
    public void setFormatPanelVisible(boolean elementSelected) {
        // ── Shape tab ─────────────────────────────────────────────────────────
        {
            VBox[] parts = (VBox[]) shapeFillContent.getUserData();
            shapeFillContent.getChildren().clear();
            if (elementSelected) {
                shapeFillContent.getChildren().add(parts[1]); // fillSection
            } else {
                shapeFillContent.getChildren().add(parts[0]); // placeholder
            }
        }
        // ── Text Container tab ────────────────────────────────────────────────
        {
            VBox[] parts = (VBox[]) textContainerContent.getUserData();
            textContainerContent.getChildren().clear();
            if (elementSelected) {
                textContainerContent.getChildren().addAll(parts[1], parts[2]); // tbFill + font
            } else {
                textContainerContent.getChildren().add(parts[0]); // placeholder
            }
        }
    }

    private void createStatusBar() {
        statusBar = new HBox(15);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #1a2632; -fx-border-width: 1 0 0 0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        Label statusIcon = new Label("✅"); statusIcon.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
        statusLabel = new Label("Ready");   statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ecf0f1;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label positionIcon  = new Label("📍"); positionIcon.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
        Label positionLabel = new Label("X: 0, Y: 0"); positionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ecf0f1;");

        Label clockIcon  = new Label("🕐"); clockIcon.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
        Label clockLabel = new Label("00:00:00"); clockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ecf0f1;");

        Thread clockThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    java.time.LocalTime now = java.time.LocalTime.now();
                    javafx.application.Platform.runLater(() ->
                        clockLabel.setText(String.format("%02d:%02d:%02d",
                                now.getHour(), now.getMinute(), now.getSecond())));
                } catch (InterruptedException e) { break; }
            }
        });
        clockThread.setDaemon(true);
        clockThread.start();

        statusBar.getChildren().addAll(statusIcon, statusLabel, spacer,
                positionIcon, positionLabel, new Separator(), clockIcon, clockLabel);

        statusLabel.setUserData("status");
        positionLabel.setUserData("position");
    }

    // ── Button factory helpers ────────────────────────────────────────────────

    private Button createProfessionalButton(String icon, String tooltip, String color) {
        Button button = new Button(icon);
        button.setTooltip(new Tooltip(tooltip));
        String base  = "-fx-background-color: transparent; -fx-padding: 2px 6px; -fx-cursor: hand; -fx-font-size: 18px; -fx-text-fill: " + color + "; -fx-border-radius: 6px; -fx-background-radius: 6px;";
        String hover = "-fx-background-color: rgba(0,0,0,0.05); -fx-padding: 2px 6px; -fx-cursor: hand; -fx-font-size: 18px; -fx-text-fill: " + color + "; -fx-border-radius: 6px; -fx-background-radius: 6px;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
        Glow glow = new Glow(0.5);
        button.setOnMousePressed(e -> button.setEffect(glow));
        button.setOnMouseReleased(e -> button.setEffect(null));
        return button;
    }

    private Button createShapeButton(String icon, String tooltip, String color) {
        Button button = new Button(icon);
        button.setTooltip(new Tooltip(tooltip));
        String base  = "-fx-background-color: transparent; -fx-padding: 4px 8px; -fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: " + color + "; -fx-border-radius: 4px; -fx-background-radius: 4px;";
        String hover = "-fx-background-color: #f1f3f4; -fx-padding: 4px 8px; -fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: " + color + "; -fx-border-radius: 4px; -fx-background-radius: 4px;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
        return button;
    }

    private Button createConnectorButton(String icon, String tooltip, String color) {
        Button button = new Button(icon);
        button.setTooltip(new Tooltip(tooltip));
        String base  = "-fx-background-color: transparent; -fx-padding: 4px 8px; -fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: " + color + "; -fx-border-radius: 4px; -fx-background-radius: 4px;";
        String hover = "-fx-background-color: #f1f3f4; -fx-padding: 4px 8px; -fx-cursor: hand; -fx-font-size: 15px; -fx-text-fill: " + color + "; -fx-border-radius: 4px; -fx-background-radius: 4px;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
        return button;
    }

    private Button createFormattingButton(String text, String tooltip, boolean isBold) {
        Button button = new Button(text);
        button.setTooltip(new Tooltip(tooltip));
        String fw    = isBold ? "bold" : "normal";
        String base  = "-fx-background-color: transparent; -fx-padding: 4px 8px; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: " + fw + "; -fx-text-fill: #5f6368; -fx-border-radius: 4px; -fx-background-radius: 4px;";
        String hover = "-fx-background-color: #f1f3f4; -fx-padding: 4px 8px; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: " + fw + "; -fx-text-fill: #5f6368; -fx-border-radius: 4px; -fx-background-radius: 4px;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
        return button;
    }

    private ToggleButton createAlignButton(String text, String tooltip, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setTooltip(new Tooltip(tooltip));
        button.setToggleGroup(group);
        String base     = "-fx-background-color: transparent; -fx-padding: 4px 8px; -fx-cursor: hand; -fx-font-size: 12px; -fx-text-fill: #5f6368; -fx-border-color: #cccccc; -fx-border-radius: 4px; -fx-background-radius: 4px;";
        String selected = "-fx-background-color: #e8f0fe; -fx-padding: 4px 8px; -fx-cursor: hand; -fx-font-size: 12px; -fx-text-fill: #1a73e8; -fx-border-color: #1a73e8; -fx-border-radius: 4px; -fx-background-radius: 4px;";
        button.setStyle(base);
        button.selectedProperty().addListener((obs, wasOn, isOn) -> button.setStyle(isOn ? selected : base));
        return button;
    }

    private Button createSmallButton(String text, String color) {
        Button button = new Button(text);
        String base  = "-fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 6px 12px; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-background-radius: 4px;";
        String hover = "-fx-background-color: derive(" + color + ", -10%); -fx-text-fill: white; -fx-padding: 6px 12px; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-background-radius: 4px;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
        return button;
    }

    private class ThumbnailCell extends ListCell<HBox> {
        @Override
        protected void updateItem(HBox item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setGraphic(null); }
            else { setGraphic(item); setStyle("-fx-padding: 5px; -fx-background-color: transparent;"); }
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public BorderPane getMainLayout() { return mainLayout; }

    public MenuItem getNewMenuItem()           { return newMenuItem; }
    public MenuItem getOpenMenuItem()          { return openMenuItem; }
    public MenuItem getSaveMenuItem()          { return saveMenuItem; }
    public MenuItem getSaveAsMenuItem()        { return saveAsMenuItem; }
    public MenuItem getExitMenuItem()          { return exitMenuItem; }
    public MenuItem getAddSlideMenuItem()      { return addSlideMenuItem; }
    public MenuItem getDeleteSlideMenuItem()   { return deleteSlideMenuItem; }
    public MenuItem getPreviousSlideMenuItem() { return previousSlideMenuItem; }
    public MenuItem getNextSlideMenuItem()     { return nextSlideMenuItem; }
    public MenuItem getPlayMenuItem()          { return playMenuItem; }
    public MenuItem getAboutMenuItem()         { return aboutMenuItem; }
    public MenuItem getHelpMenuItem()          { return helpMenuItem; }

    public Button getNewButton()         { return newButton; }
    public Button getOpenButton()        { return openButton; }
    public Button getSaveButton()        { return saveButton; }
    public Button getAddSlideButton()    { return addSlideButton; }
    public Button getDeleteSlideButton() { return deleteSlideButton; }
    public Button getPrevButton()        { return prevButton; }
    public Button getNextButton()        { return nextButton; }
    public Button getPlayButton()        { return playButton; }
    public Button getAddTextButton()     { return addTextButton; }
    public Button getAddImageButton()    { return addImageButton; }   // ← NEW
    public Button getBoldButton()        { return boldButton; }
    public Button getItalicButton()      { return italicButton; }

    public ComboBox<String> getFontSizeCombo() { return fontSizeCombo; }
    public ColorPicker getFontColorPicker()    { return fontColorPicker; }

    public ToggleButton getAlignLeftButton()   { return alignLeftButton; }
    public ToggleButton getAlignCenterButton() { return alignCenterButton; }
    public ToggleButton getAlignRightButton()  { return alignRightButton; }
    public ToggleButton getBulletListButton()  { return bulletListButton; }
    public Button getIndentIncreaseButton()    { return indentIncreaseButton; }
    public Button getIndentDecreaseButton()    { return indentDecreaseButton; }
    public ColorPicker getHighlightColorPicker()     { return highlightColorPicker; }
    public ColorPicker getHighlightTextColorPicker() { return highlightTextColorPicker; }
    public Button      getClearHighlightButton()     { return clearHighlightButton; }

    /**
     * Returns true while either highlight colour picker popup is open.
     * Used by ShapeTextBoxNode's TextArea focus listener to suppress finishEditing
     * while the user is choosing a colour inside the popup — internal popup nodes
     * receive focus but carry no userData, so the normal userData-based skip fails.
     */
    public boolean isHighlightPickerShowing() {
        return highlightColorPicker.isShowing() || highlightTextColorPicker.isShowing();
    }

    /** Returns true while either font-colour picker popup is open. */
    public boolean isFontColorPickerShowing() {
        return fontColorPicker.isShowing() || tcFontColorPicker.isShowing();
    }

    public Button getRectangleShapeButton()     { return rectangleShapeButton; }
    public Button getRoundedRectShapeButton()   { return roundedRectShapeButton; }
    public Button getCircleShapeButton()        { return circleShapeButton; }
    public Button getCalloutShapeButton()       { return calloutShapeButton; }
    public Button getDiamondShapeButton()       { return diamondShapeButton; }
    public Button getPentagonShapeButton()      { return pentagonShapeButton; }
    public Button getHexagonShapeButton()       { return hexagonShapeButton; }
    public Button getStarShapeButton()          { return starShapeButton; }
    public Button getParallelogramShapeButton() { return parallelogramShapeButton; }

    public ToggleButton getConnectModeButton()   { return connectModeButton; }
    public Button getStraightArrowButton()       { return straightArrowButton; }
    public Button getCurvedArrowButton()         { return curvedArrowButton; }
    public Button getElbowArrowButton()          { return elbowArrowButton; }
    public ColorPicker getConnectorColorPicker() { return connectorColorPicker; }
    public ComboBox<String> getLineWidthCombo()  { return lineWidthCombo; }

    public Pane getSlidePane()                     { return slidePane; }
    public Label getSlideCounter()                 { return slideCounter; }
    public Label getStatusLabel()                  { return statusLabel; }
    public ProgressIndicator getProgressIndicator(){ return progressIndicator; }

    // Kept for backward compatibility with the controller
    public ToolBar getUnifiedToolBar()             { return mainToolBar; }

    public TreeView<String> getFileTreeView()      { return fileTreeView; }
    public ListView<HBox> getThumbnailListView()   { return thumbnailListView; }
    public ListView<String> getElementsListView()  { return elementsListView; }

    public Button getBrowseButton()  { return browseButton; }
    public Button getRefreshButton() { return refreshButton; }

    public ColorPicker getSlideBackgroundPicker() { return slideBackgroundPicker; }

    public ColorPicker getShapeFillPicker()         { return shapeFillPicker; }
    public CheckBox    getShapeFillNoneCheck()       { return shapeFillNoneCheck; }
    public ColorPicker getTextBoxFillPicker()        { return textBoxFillPicker; }
    public CheckBox    getTextBoxFillNoneCheck()     { return textBoxFillNoneCheck; }

    // Text Container tab — font controls
    public ColorPicker      getTcFontColorPicker()   { return tcFontColorPicker; }
    public ComboBox<String> getTcFontSizeCombo()     { return tcFontSizeCombo; }
    public ComboBox<String> getTcFontFamilyCombo()   { return tcFontFamilyCombo; }
    public ToggleButton     getTcBoldButton()        { return tcBoldButton; }
    public ToggleButton     getTcItalicButton()      { return tcItalicButton; }
}