
// Clipboard
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

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
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

// Settings persistence
import java.util.prefs.Preferences;

public class Main extends Application {

    // Public UI
    private static TextField hexColorCodeText;
    private static TextField smolHexColorCodeText;
    private static Rectangle colorDisplay;

    // Persisted Settings
    private static final Preferences PREFS = Preferences.userNodeForPackage(Main.class);
    private static final String HIDE_WHILE_PICKING_KEY = "hideWindowWhilePicking";

    // How long to wait after hiding the window before grabbing the screen
    private static final Duration HIDE_SETTLE_DELAY = Duration.millis(250);

    // Application primary stage here
    @Override
    public void start(Stage primaryStage) {
        // Scene components
        final int HORIZONTAL_SIZE = 112;
        final int VH_BOX_PADDINGS = 10;

        Button extractButton = new Button("Pick Color from Screen");

        CheckBox hideWhilePicking = new CheckBox("Hide this window while picking");
        hideWhilePicking.setSelected(PREFS.getBoolean(HIDE_WHILE_PICKING_KEY, false));
        hideWhilePicking.selectedProperty()
            .addListener((observable, was, isSelected) ->
                         PREFS.putBoolean(HIDE_WHILE_PICKING_KEY, isSelected));

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
                             hideWhilePicking,
                             smolHexColorDisplay,
                             hexColorDisplay,
                             colorDisplay);

        Insets rootPadding = new Insets(20);
        root.setPadding(rootPadding);

        Scene scene = new Scene(root, 800, 600);

        // EVENT HANDLERS
        // Open the full-screen overlay picker. A null result means the user
        // cancelled (Esc) or the OS blocked screen capture.
        extractButton.setOnAction(event -> {
            extractButton.setDisable(true);
            final boolean hide = hideWhilePicking.isSelected();

            Runnable startPick = () -> ScreenColorPicker.pick(color -> {
                if (hide) {
                    primaryStage.show();
                    Platform.setImplicitExit(true);
                }
                extractButton.setDisable(false);
                if (color != null) {
                    applyColor(color);
                }
            });

            if (hide) {
                Platform.setImplicitExit(false);
                primaryStage.hide();
                PauseTransition settle = new PauseTransition(HIDE_SETTLE_DELAY);
                settle.setOnFinished(e -> startPick.run());
                settle.play();
            } else {
                startPick.run();
            }
        });

        // Button press to copy Color Text Shower text into system clipboard
        copyHexColorCodeTextButton.setOnAction(event -> copyToClipboard(hexColorCodeText.getText()));
        copySmolHexColorCodeTextButton.setOnAction(event -> copyToClipboard(smolHexColorCodeText.getText()));

        primaryStage.setTitle("Color Extract");
        primaryStage.setScene(scene);
        primaryStage.show();

        printOSInfo();
    }

    private static void copyToClipboard(String text) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private static void printOSInfo() {
        // Print OS information
        String osName = System.getProperty("os.name");
        System.out.println("Operating System: " + osName);
    }

    public static void main(String args[]) {
        launch(args);
    }

    // Push a picked color into the UI fields and swatch.
    private static void applyColor(java.awt.Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        System.out.println("Picked color: " + getHexString(r, g, b));
        hexColorCodeText.setText(getHexString(r, g, b));
        smolHexColorCodeText.setText(getRGBShortString(r, g, b));
        colorDisplay.setFill(convertAwtColorToJfx(color));
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
