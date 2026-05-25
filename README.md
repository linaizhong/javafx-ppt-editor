# Simple Slide Editor

A desktop presentation editor built with JavaFX, offering a familiar IDE-style layout for creating, editing, and presenting slideshows. Simple Slide Editor supports rich text shapes, image insertion, arrow connectors, slide-background customisation, and a full-screen slideshow player.

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Building from Source](#building-from-source)
- [Running the Application](#running-the-application)
- [Usage](#usage)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Slideshow Controls](#slideshow-controls)
- [File Format](#file-format)
- [Project Structure](#project-structure)

---

## Features

### Slides & Navigation
- **Multi-slide presentations** — add and delete slides freely; navigate with toolbar buttons, the Edit menu, or keyboard shortcuts.
- **Slide thumbnail panel** — a scrollable thumbnail strip on the left gives a visual overview of all slides and lets you jump directly to any slide.
- **Slide counter** — the status bar always shows the current slide number and total count.
- **Custom slide backgrounds** — pick any background colour per slide using the built-in colour picker.

### Shapes & Text
- **Nine shape types** — Rectangle, Rounded Rectangle, Circle, Callout, Diamond, Pentagon, Hexagon, Star, and Parallelogram.
- **Double-click to edit** — double-click any shape to enter its text inline.
- **Rich text formatting:**
  - Bold and Italic toggles
  - Font size selection
  - Font colour picker
  - Text alignment — Left, Centre, Right
  - Bullet list toggle
  - Indent increase / decrease
  - Text highlight colour and highlight text colour, with a one-click clear button
- **Shape fill** — set or clear a fill colour for any shape via the right-side Format panel.
- **Text Container tab** — a dedicated Format panel tab exposes per-shape font family, font size, font colour, bold, and italic controls independently of the main toolbar.

### Images
- **Insert images** — insert PNG, JPG, JPEG, GIF, BMP, or WebP images from disk onto any slide.
- **Auto-sizing** — inserted images are automatically scaled to fit within 400 × 300 px while preserving their aspect ratio, and are centred on the slide canvas.
- **Drag to move, handles to resize** — images support the same drag-and-drop positioning and resize handles as text shapes.
- **Delete** — select an image and press `Delete` to remove it from the slide.

### Arrow Connectors
- **Connect Mode** — toggle Connect Mode, click a source shape, then click a target shape to draw a connector arrow between them.
- **Three arrow styles** — Straight, Curved, and Elbow.
- **Connector colour and line width** — customise connector appearance via the toolbar colour picker and line-width combo box.

### Slideshow Player
- **Full-screen playback** — launches a dedicated slideshow window in full-screen mode.
- **Slide transitions** — fade-out / fade-in transitions between slides, with a scale animation for each text and image element as it appears.
- **Navigation** — advance and go back using keyboard keys, arrow keys, or left/right mouse clicks on the slide canvas.
- **Slide number indicator** — a semi-transparent counter is shown in the bottom-left corner during playback.
- **Auto-hiding controls overlay** — a hint bar fades in on mouse movement and fades out after 3 seconds.

### File Explorer
- **Built-in directory browser** — browse your file system from within the left panel; double-click any `.sle` file to open it directly.
- **Refresh** — re-scan the current directory with a single click.

### Format Panel (Right Side)
The right-side panel provides PowerPoint-style tabbed formatting:
- **Shape Fill tab** — set or clear the fill colour of the selected shape.
- **Text Container tab** — font family, font size, font colour, bold, and italic controls scoped to the selected shape's text container.

---

## Requirements

| Requirement | Version |
|---|---|
| Java (JDK) | 17 or later |
| JavaFX SDK | 17 or later |
| Build tool | Maven or Gradle (see below) |
| Operating System | Windows, macOS, or Linux |

---

## Building from Source

### Maven

```bash
git clone https://github.com/linaizhong/javafx-ppt-editor.git
cd javafx-ppt-editor
mvn clean package
```

### Running with Maven

```bash
mvn javafx:run
```

### Gradle

```bash
./gradlew run
```

---

## Running the Application

```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar javafx-ppt-editor.jar
```

The application window opens at 1280 × 800 px with a minimum size of 1024 × 768 px.

---

## Usage

### Creating a Presentation

1. Launch the application — a new empty presentation is created automatically.
2. Click **Add Slide** (toolbar or `Edit > Add Slide`) to add slides.
3. Select a shape from the **Shapes** section of the toolbar to add a text box to the current slide.
4. **Double-click** a shape to enter editing mode and type your content.
5. Use the **Format** toolbar controls to apply bold, italic, font size, colour, alignment, and highlight styles.
6. Click outside the shape to finish editing.

### Inserting Images

1. Click the **Insert Image** button in the toolbar.
2. Select a PNG, JPG, JPEG, GIF, BMP, or WebP file from the file chooser.
3. The image is placed centred on the current slide. Drag it to reposition; use the resize handles to adjust its size.

### Connecting Shapes with Arrows

1. Click the **Connect Mode** toggle button to enter connection mode.
2. Select an arrow style: **Straight**, **Curved**, or **Elbow**.
3. Click the source shape, then click the destination shape — an arrow is drawn between them.
4. Click **Connect Mode** again to exit.

### Changing the Slide Background

Select a colour from the **Background** colour picker in the toolbar. The change applies to the current slide only.

### Playing the Slideshow

Click the **▶ Play** button (toolbar or `Presentation > Play Slideshow`) or press `F5`. The slideshow opens in a new full-screen window.

### Saving and Opening

- **Save** — `Ctrl+S` saves to the current file. If no file has been chosen yet, a Save As dialog appears.
- **Save As** — `Ctrl+Shift+S` always opens the Save As dialog.
- **Open** — `Ctrl+O` opens a file chooser. You can also double-click a `.sle` file in the built-in File Explorer panel.

---

## Keyboard Shortcuts

### Editor

| Action | Shortcut |
|---|---|
| New Presentation | `Ctrl+N` |
| Open Presentation | `Ctrl+O` |
| Save | `Ctrl+S` |
| Save As | `Ctrl+Shift+S` |
| Previous Slide | `Ctrl+Left` |
| Next Slide | `Ctrl+Right` |
| Play Slideshow | `F5` |
| Delete selected element | `Delete` |
| Exit | `Alt+F4` |

### Slideshow Window

| Action | Key / Mouse |
|---|---|
| Next slide | `→` / `Space` / `↓` / `Page Down` / right-third click |
| Previous slide | `←` / `↑` / `Page Up` / left-third click |
| First slide | `Home` |
| Last slide | `End` |
| Toggle full screen | `F` |
| Exit slideshow | `Esc` |

---

## Slideshow Controls

During playback the slideshow window runs in full screen. Moving the mouse reveals a semi-transparent hint overlay at the bottom of the screen showing all available controls. The overlay fades out automatically after 3 seconds of inactivity.

Clicking the left third of the screen goes to the previous slide; clicking the right third advances to the next slide. The current slide number is always shown in the bottom-left corner.

---

## File Format

Presentations are saved as `.sle` files (Simple Slide Editor format). Each file stores all slides, text elements with their shape type, position, size, font, colour and formatting, image elements with their file path, size and position, arrow connectors between shapes, and slide background colours.

> **Note:** Image elements reference image files by their absolute path on disk. If you move a presentation to another machine, copy the referenced image files alongside it or re-insert them.

---

## Project Structure

```
slideeditor/
├── SlideEditorApp.java          # JavaFX Application entry point
├── SlideEditorController.java   # MVC controller — all event handling and business logic
├── SlideEditorView.java         # MVC view — UI layout, menu bar, toolbar, panels
├── SlideshowWindow.java         # Full-screen slideshow player
├── Presentation.java            # Presentation data model (collection of slides)
├── Slide.java                   # Single slide model (text elements, images, background)
├── TextElement.java             # Text/shape element model
├── ImageElement.java            # Image element model
├── ArrowElement.java            # Arrow connector model (STRAIGHT / CURVED / ELBOW)
├── ShapeTextBoxNode.java        # JavaFX node for editable text shapes
├── ImageNode.java               # JavaFX node for draggable/resizable images
└── resources/
    └── css/
        └── style.css            # Application stylesheet (optional — app runs without it)
```

---

## License

This project is licensed under the [APACHE License](LICENSE).

---

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
