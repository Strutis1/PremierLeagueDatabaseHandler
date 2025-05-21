package helper;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class Utilz {

    public static void fillIntegerComboBox(ComboBox<Integer> comboBox, int start, int end) {
        comboBox.getItems().clear();
        for (int i = start; i <= end; i++) {
            comboBox.getItems().add(i);
        }
    }

    public static void fillStringComboBox(ComboBox<String> comboBox, String... values) {
        comboBox.getItems().clear();
        comboBox.getItems().addAll(values);
    }

    public static boolean isPositiveNumber(TextField textField) {
        String text = textField.getText().trim();
        return text.matches("^[1-9]\\d*$");
    }
}
