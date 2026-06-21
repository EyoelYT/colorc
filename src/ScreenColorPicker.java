
import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Full-screen overlay color picker.
 *
 * <p>Instead of installing a global mouse hook (which requires Accessibility
 * permission on macOS and captures every click on the desktop), this snapshots
 * each display once, shows it in a borderless full-screen window per monitor,
 * and lets the user click <em>inside our own window</em>. The click is an
 * ordinary JavaFX mouse event, and the picked color is read from the captured
 * image rather than re-querying the screen.
 *
 * <p>One overlay window is created per physical display so the picker spans all
 * monitors and each display is captured at its own scale (correct for mixed
 * HiDPI / non-HiDPI setups).
 *
 * <p>Screen-capture permission (Screen Recording on macOS Catalina+) is
 * required for {@link Robot#createScreenCapture} to return the real contents of
 * other apps' windows. Without it, the capture shows only the desktop wallpaper.
 */
public final class ScreenColorPicker {

    private static final int LOUPE_SIZE = 132;   // on-screen size of the magnifier, px
    private static final int LOUPE_ZOOM = 11;     // magnification factor
    private static final int LOUPE_OFFSET = 24;   // gap between cursor and loupe

    private ScreenColorPicker() {}

    /**
     * Open the picker across all displays. {@code onPicked} is invoked once on
     * the JavaFX thread with the chosen color, or with {@code null} if the user
     * cancels (Esc) or capture fails. All overlays close before the callback.
     */
    public static void pick(Consumer<java.awt.Color> onPicked) {
        final Robot robot;
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            System.err.println("Cannot create Robot: " + ex.getMessage());
            onPicked.accept(null);
            return;
        }

        final List<Stage> stages = new ArrayList<>();
        final boolean[] done = { false };

        // Fires the result exactly once and tears down every overlay window.
        final Consumer<java.awt.Color> finish = color -> {
            if (done[0]) return;
            done[0] = true;
            for (Stage s : stages) s.close();
            onPicked.accept(color);
        };

        GraphicsDevice[] devices =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        for (GraphicsDevice device : devices) {
            java.awt.Rectangle bounds = device.getDefaultConfiguration().getBounds();
            try {
                Stage overlay = buildOverlay(robot, bounds, finish);
                if (overlay != null) stages.add(overlay);
            } catch (SecurityException ex) {
                System.err.println("Screen capture blocked: " + ex.getMessage());
            }
        }

        if (stages.isEmpty()) {
            onPicked.accept(null);
            return;
        }

        for (Stage s : stages) s.show();
        Platform.runLater(() -> stages.get(0).requestFocus());
    }

    /** Build one overlay window covering a single display. */
    private static Stage buildOverlay(Robot robot, java.awt.Rectangle bounds,
                                      Consumer<java.awt.Color> finish) {
        final BufferedImage shot = robot.createScreenCapture(bounds);
        final Image fxImage = SwingFXUtils.toFXImage(shot, null);

        // Captured image can be larger than the logical bounds on HiDPI displays;
        // map this display's logical coordinates onto its image pixels.
        final double scaleX = shot.getWidth()  / (double) bounds.width;
        final double scaleY = shot.getHeight() / (double) bounds.height;

        ImageView background = new ImageView(fxImage);
        background.setFitWidth(bounds.width);
        background.setFitHeight(bounds.height);

        // Magnifier loupe: a second view of the same image, scaled up via viewport.
        ImageView loupe = new ImageView(fxImage);
        loupe.setFitWidth(LOUPE_SIZE);
        loupe.setFitHeight(LOUPE_SIZE);
        loupe.setVisible(false);

        Rectangle loupeFrame = new Rectangle(LOUPE_SIZE, LOUPE_SIZE);
        loupeFrame.setFill(Color.TRANSPARENT);
        loupeFrame.setStroke(Color.WHITE);
        loupeFrame.setStrokeWidth(2);
        loupeFrame.setVisible(false);

        Line crossH = new Line();
        Line crossV = new Line();
        for (Line l : new Line[] { crossH, crossV }) {
            l.setStroke(Color.rgb(0, 0, 0, 0.5));
            l.setVisible(false);
        }

        Rectangle swatch = new Rectangle(LOUPE_SIZE, 22);
        swatch.setVisible(false);
        Text label = new Text();
        label.setFill(Color.WHITE);
        label.setStroke(Color.BLACK);
        label.setStrokeWidth(0.4);
        label.setVisible(false);

        Pane root = new Pane(background, loupe, loupeFrame, swatch, label, crossH, crossV);
        root.setStyle("-fx-cursor: crosshair;");

        Scene scene = new Scene(root, bounds.width, bounds.height);
        scene.setFill(Color.BLACK);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setX(bounds.x);
        stage.setY(bounds.y);
        stage.setWidth(bounds.width);
        stage.setHeight(bounds.height);
        stage.setAlwaysOnTop(true);

        // Read the pixel under the cursor straight from this display's capture.
        java.util.function.BiFunction<Double, Double, java.awt.Color> sample = (sx, sy) -> {
            int px = clamp((int) Math.round(sx * scaleX), 0, shot.getWidth() - 1);
            int py = clamp((int) Math.round(sy * scaleY), 0, shot.getHeight() - 1);
            return new java.awt.Color(shot.getRGB(px, py), true);
        };

        scene.setOnMouseMoved(e -> {
            double sx = e.getX();
            double sy = e.getY();
            java.awt.Color c = sample.apply(sx, sy);
            Color fx = Color.rgb(c.getRed(), c.getGreen(), c.getBlue());

            // Loupe viewport centered on the cursor in image space.
            double viewSize = LOUPE_SIZE / (double) LOUPE_ZOOM;
            double half = viewSize / 2.0;
            loupe.setViewport(new Rectangle2D(sx * scaleX - half, sy * scaleY - half,
                                              viewSize, viewSize));

            // Keep the loupe on-screen near the cursor.
            double lx = sx + LOUPE_OFFSET;
            double ly = sy + LOUPE_OFFSET;
            if (lx + LOUPE_SIZE > bounds.width)  lx = sx - LOUPE_OFFSET - LOUPE_SIZE;
            if (ly + LOUPE_SIZE + 22 > bounds.height) ly = sy - LOUPE_OFFSET - LOUPE_SIZE - 22;

            loupe.relocate(lx, ly);
            loupeFrame.relocate(lx, ly);
            swatch.relocate(lx, ly + LOUPE_SIZE);
            swatch.setFill(fx);
            label.relocate(lx + 6, ly + LOUPE_SIZE + 16);
            label.setText(Main.getHexString(c.getRed(), c.getGreen(), c.getBlue()).toUpperCase());

            crossH.setStartX(lx); crossH.setEndX(lx + LOUPE_SIZE);
            crossH.setStartY(ly + LOUPE_SIZE / 2.0); crossH.setEndY(ly + LOUPE_SIZE / 2.0);
            crossV.setStartY(ly); crossV.setEndY(ly + LOUPE_SIZE);
            crossV.setStartX(lx + LOUPE_SIZE / 2.0); crossV.setEndX(lx + LOUPE_SIZE / 2.0);

            for (Node n : new Node[] { loupe, loupeFrame, swatch, label, crossH, crossV }) {
                n.setVisible(true);
            }
        });

        scene.setOnMouseClicked(e -> finish.accept(sample.apply(e.getX(), e.getY())));

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) finish.accept(null);
        });

        return stage;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
