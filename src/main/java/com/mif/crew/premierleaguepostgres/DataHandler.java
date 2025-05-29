package com.mif.crew.premierleaguepostgres;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.sql.Connection;
import java.util.List;

public final class DataHandler {

    private Connection conn;

    private Integer selectedSeason;

    private ObservableList<String> selectedTeam;
    private TableView<ObservableList<String>> standingsTable;

    private ObservableList<String> selectedMatch;
    private TableView<ObservableList<String>> matchesTable;

    private ObservableList<String> selectedPlayer;
    private TableView<ObservableList<String>> playersTable;

    private DatabaseViewController mainController;



    private final static DataHandler INSTANCE = new DataHandler();

    private DataHandler() {}

    public static DataHandler getInstance() {
        return INSTANCE;
    }

    public void setConnection(Connection conn) {
        this.conn = conn;
    }

    public Connection getConnection() {
        return conn;
    }

    public Integer getSelectedSeason() {
        return selectedSeason;
    }

    public void setSelectedSeason(Integer selectedSeason) {
        this.selectedSeason = selectedSeason;
    }

    public ObservableList<String> getSelectedTeam() {
        return selectedTeam;
    }

    public void setSelectedTeam(ObservableList<String> selectedTeam) {
        this.selectedTeam = selectedTeam;
    }

    public TableView<ObservableList<String>> getStandingsTable() {
        return standingsTable;
    }

    public void setStandingsTable(TableView<ObservableList<String>> table) {
        this.standingsTable = table;
    }

    public ObservableList<String> getSelectedMatch() {
        return selectedMatch;
    }

    public void setSelectedMatch(ObservableList<String> selectedMatch) {
        this.selectedMatch = selectedMatch;
    }

    public TableView<ObservableList<String>> getMatchesTable() {
        return matchesTable;
    }

    public void setMatchesTable(TableView<ObservableList<String>> matchesTable) {
        this.matchesTable = matchesTable;
    }

    public ObservableList<String> getSelectedPlayer() {
        return selectedPlayer;
    }

    public void setSelectedPlayer(ObservableList<String> selectedPlayer) {
        this.selectedPlayer = selectedPlayer;
    }

    public TableView<ObservableList<String>> getPlayersTable() {
        return playersTable;
    }

    public void setPlayersTable(TableView<ObservableList<String>> playersTable) {
        this.playersTable = playersTable;
    }

    public DatabaseViewController getMainController() {
        return mainController;
    }

    public void setMainController(DatabaseViewController mainController) {
        this.mainController = mainController;
    }
}

