package com.mif.crew.premierleaguepostgres;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

//TODO make addition possible(players, managers)
//TODO continue with tabs(matches, players)
//still need search/filter, real transaction(player/manager transfer), id-> name(manager signing), some sort of join
public class PremierLeagueDatabase extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PremierLeagueDatabase.class.getResource("connect-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 478, 286);
        stage.setTitle("Database connection");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}