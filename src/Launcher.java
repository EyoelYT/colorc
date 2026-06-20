// Plain (non-Application) entry point used by the packaged app.
//
// When the JVM's main class extends javafx.application.Application and JavaFX
// is only on the classpath (as it is in a jpackage bundle), the launcher aborts
// with "JavaFX runtime components are missing". So we route startup through a
// class that does not extend Application bypasses. Main.main() then calls
// launch() programmatically, which works fine off the classpath.
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
