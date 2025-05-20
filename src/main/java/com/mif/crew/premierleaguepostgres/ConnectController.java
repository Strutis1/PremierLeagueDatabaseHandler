package com.mif.crew.premierleaguepostgres;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class ConnectController {

    @FXML
    private PasswordField clientPassword;

    @FXML
    private TextField clientUsername;

    @FXML
    private Button joinButton;

    private String url;


    public void initialize(){
        clientUsername.setOnAction(this::checkInput);
        clientPassword.setOnAction(this::checkInput);
        joinButton.setOnAction(this::connect);
    }

    private void checkInput(ActionEvent actionEvent) {
        joinButton.setDisable(clientUsername.getText().isEmpty() || clientPassword.getText().isEmpty());
        if(!joinButton.isDisable()){
            url = "jdbc:postgresql://localhost:5432/PremierLeague";
        }
    }


    private void connect(ActionEvent actionEvent) {
        try (Connection conn = DriverManager.getConnection(url, clientUsername.getText(), clientPassword.getText())) {
            System.out.println("Connected to the database - Premier League!");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT version();");

            while (rs.next()) {
                System.out.println("PostgreSQL version: " + rs.getString(1));
            }
            openDBHandler();

        } catch (SQLException e) {
            System.out.println("Connection failed:");
            System.out.println("Error message: " + e.getMessage());
            System.out.println("SQL state: " + e.getSQLState());
            System.out.println("Error code: " + e.getErrorCode());
        }
    }

    private void openDBHandler() {
        try {
            FXMLLoader dbLoader = new FXMLLoader(getClass().getResource("database-view.fxml"));
            Parent dbHandlerRoot = dbLoader.load();

            DatabaseViewController controller = dbLoader.getController();
            controller.connectToDatabase(url, clientUsername.getText(), clientPassword.getText());

            Stage dbStage = new Stage();
            Scene dbScene = new Scene(dbHandlerRoot, 900, 700);
            dbScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            dbStage.setTitle("Premier League 2008-2024");
            dbStage.setScene(dbScene);
            dbStage.show();

            Platform.runLater(() -> {
                Stage currentStage = (Stage) joinButton.getScene().getWindow();
                currentStage.close();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
