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
}

