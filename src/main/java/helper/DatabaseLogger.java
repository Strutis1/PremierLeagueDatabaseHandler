package helper;

import javafx.scene.control.TextArea;

public class DatabaseLogger {

    private static TextArea outputArea;

    public static void bind(TextArea area) {
        outputArea = area;
    }

    public static void log(String message) {
        if (outputArea != null) {
            outputArea.appendText(message + "\n");
        } else {
            System.out.println("LOG: " + message);
        }
    }
}
