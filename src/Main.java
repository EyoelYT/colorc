
// Mouse Pointer and Color Extraction
import java.awt.Robot;
import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;

// Clipboard
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

// Mouse Controls
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

// JavaFX stuff
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class Main extends Application {

    private static boolean hookRegistered = false;
    private static boolean extractColor = false;
    private static boolean appJustOpened = true;
    private static boolean isHoveringOverButton = false;

    // Public UI
    private static TextField hexColorCodeText;
    private static TextField smolHexColorCodeText;
    private static Rectangle colorDisplay;

    // Application primary stage here
    @Override
    public void start(Stage primaryStage) {
        // Scene components
        final int HORIZONTAL_SIZE = 112;
        final int VH_BOX_PADDINGS = 10;

        Button extractButton = new Button("Start Color Extract");

        hexColorCodeText = new TextField("#000000");
        hexColorCodeText.setPrefWidth(HORIZONTAL_SIZE);
        hexColorCodeText.setMaxWidth(HORIZONTAL_SIZE);
        Button copyHexColorCodeTextButton = new Button("Copy");

        smolHexColorCodeText = new TextField("#000");
        smolHexColorCodeText.setPrefWidth(HORIZONTAL_SIZE);
        smolHexColorCodeText.setMaxWidth(HORIZONTAL_SIZE);
        Button copySmolHexColorCodeTextButton = new Button("Copy");

        colorDisplay = new Rectangle(HORIZONTAL_SIZE, HORIZONTAL_SIZE);

        HBox hexColorDisplay = new HBox(VH_BOX_PADDINGS, hexColorCodeText, copyHexColorCodeTextButton);
        HBox smolHexColorDisplay = new HBox(VH_BOX_PADDINGS, smolHexColorCodeText, copySmolHexColorCodeTextButton);

        // Initial configuration of components
        hexColorCodeText.setEditable(false);
        smolHexColorCodeText.setEditable(false);

        // LAYOUT
        VBox root = new VBox(VH_BOX_PADDINGS,
                             extractButton,
                             smolHexColorDisplay,
                             hexColorDisplay,
                             colorDisplay);

        Insets rootPadding = new Insets(20);
        root.setPadding(rootPadding);

        Scene scene = new Scene(root, 800, 600);

        // EVENT HANDLERS
        // Button to initiate extraction procedure
        extractButton.setOnAction(event -> {
            extractColor = !extractColor;
            extractButton.setText(extractColor ? "Stop Extracting" : "Start Color Extract");
            if (extractColor) {
                startGetColorAtMouseLocation();
            } else {
                System.out.println("Color Extraction has been paused");
            }
        });

        extractButton.setOnMouseEntered(event -> isHoveringOverButton = true);
        extractButton.setOnMouseExited(event -> isHoveringOverButton = false);

        // Button press to copy Color Text Shower text into system clipboard
        copyHexColorCodeTextButton.setOnAction(event -> {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(hexColorCodeText.getText());
            clipboard.setContent(content);
        });

        copySmolHexColorCodeTextButton.setOnAction(event -> {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(smolHexColorCodeText.getText());
            clipboard.setContent(content);
        });

        copyHexColorCodeTextButton.setOnMouseEntered(event -> isHoveringOverButton = true);
        copyHexColorCodeTextButton.setOnMouseExited(event -> isHoveringOverButton = false);

        primaryStage.setTitle("Color Extract");
        primaryStage.setScene(scene);
        primaryStage.show();

        printOSInfo();

        if (appJustOpened) {
            registerGlobalNativeHook();
            appJustOpened = false;
        }
    }

    private static void printOSInfo() {
        // Print OS information
        String osName = System.getProperty("os.name");
        System.out.println("Operating System: " + osName);
    }

    public static void main(String args[]) {
        launch(args);
    }

    public static void registerGlobalNativeHook() {
        if (hookRegistered) return;
        // Register JnativeHook to the global screen for getting mouse coordinates
        try {
            GlobalScreen.registerNativeHook();
            hookRegistered = true;
        } catch (NativeHookException ex) {
            System.err.println("Failed to register native hook: " + ex.getMessage());
            System.exit(1);
        }
    }

    // Start procedure where mouse clicks generate color reports
    public static void startGetColorAtMouseLocation() {

        // Add mouse click event listener
        GlobalScreen.addNativeMouseListener(new NativeMouseListener() {

            // Print color at mouse location when mouse button is pressed (unless over buttons on the desktop)
            @Override
            public void nativeMouseClicked(NativeMouseEvent e) {
                if (extractColor) {
                    // If extractColor button is true (pressed) and cursor is not over any button
                    if (!isHoveringOverButton) {
                        Platform.runLater(() -> {
                            printColorAtMouseLocation();
                            System.out.println("Mouse Clicked: " + e.getClickCount());
                        });
                    } else {
                        System.out.println("Hovering over button. Skipped getting mouse position desktop color.");
                    }
                } else {
                    System.out.println("Color Extraction is Paused");
                }
            }
        });
    }

    // Print the color at current mouse coordinates
    private static void printColorAtMouseLocation() {
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        int x = mouseLocation.x;
        int y = mouseLocation.y;
        try {
            Robot robot = new Robot();
            java.awt.Color color = robot.getPixelColor(x, y);

            System.out.println("Color at [" + x + "," + y + "]: " + color);
            System.out.println("HexColor = " + getHexString(color.getRed(), color.getGreen(), color.getBlue()));
            hexColorCodeText.setText(getHexString(color.getRed(), color.getGreen(), color.getBlue()));
            smolHexColorCodeText.setText(getRGBShortString(color.getRed(), color.getGreen(), color.getBlue()));
            colorDisplay.setFill(convertAwtColorToJfx(color));
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static String getRGBShortString(int r, int g, int b) {
        String rHex = Integer.toHexString(r / 16);
        String gHex = Integer.toHexString(g / 16);
        String bHex = Integer.toHexString(b / 16);

        return "#" + rHex + gHex + bHex;
    }

    // https://www.tabnine.com/code/java/methods/java.awt.Robot/getPixelColor
    // Turns 255,255,255 color value into #RRGGBB hex format
    public static String getHexString(int r, int g, int b) {
        String rHex = String.format("%02x", r);
        String gHex = String.format("%02x", g);
        String bHex = String.format("%02x", b);

        return "#" + rHex + gHex + bHex;
    }

    // Function that ends the terminal application when the javafx is closed
    @Override
    public void stop() {
        System.exit(0);
    }

    private static javafx.scene.paint.Color convertAwtColorToJfx(java.awt.Color awtColor) {
        int r = awtColor.getRed();
        int g = awtColor.getGreen();
        int b = awtColor.getBlue();
        int a = awtColor.getAlpha();

        return javafx.scene.paint.Color.rgb(r, g, b, a / 255.0);
    }

}
