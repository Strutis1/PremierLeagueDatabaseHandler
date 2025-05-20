package com.mif.crew.premierleaguepostgres;

import helper.DatabaseLogger;
import helper.IdNamePair;
import helper.QueryExecutor;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.SeasonService;
import services.SquadService;
import services.StandingsService;

import java.io.IOException;
import java.sql.*;

public class DatabaseViewController {

    private StandingsService standingsService;
    private SquadService squadService;
    private SeasonService seasonsService;

    @FXML
    private Button addManagerButton;

    @FXML
    private Button addSeasonButton;

    @FXML
    private Button addSeasonButton1;

    @FXML
    private Button addTeamButton;

    @FXML
    private Button applySeasonButton;

    @FXML
    private TextField awayGoalsText;

    @FXML
    private ComboBox<String> awayTeamCombo;

    @FXML
    private Button changeMatchButton;

    @FXML
    private Button deleteMatchButton;

    @FXML
    private Button deleteSeasonButton;

    @FXML
    private Button deleteTeamButton;

    @FXML
    private TextField homeGoalsText;

    @FXML
    private ComboBox<String> homeTeamCombo;

    @FXML
    private TextArea infoArea;

    @FXML
    private Button letPlayerGoButton;

    @FXML
    private VBox mainBox;

    @FXML
    private BorderPane mainPane;

    @FXML
    private ListView<IdNamePair> managerList;

    @FXML
    private DatePicker matchDatePicker;

    @FXML
    private ComboBox<Integer> matchWeekCombo;

    @FXML
    private VBox matchesPanel;

    @FXML
    private Tab matchesTab;

    @FXML
    private TextField newSeasonText;

    @FXML
    private ListView<IdNamePair> playerList;

    @FXML
    private VBox playersPanel;

    @FXML
    private Tab playersTab;

    @FXML
    private Button sackManagerButton;

    @FXML
    private ListView<Integer> seasonListView;

    @FXML
    private VBox seasonPanel;

    @FXML
    private Button signPlayerButton;

    @FXML
    private VBox signPlayerPanel;

    @FXML
    private VBox squadPanel;

    @FXML
    private Label squadTitle;

    @FXML
    private VBox standingsPanel;

    @FXML
    private Tab standingsTab;

    @FXML
    private TabPane tableTabs;

    @FXML
    private VBox createMatchPanel;


    private Connection conn;



    public void initialize() {
        DatabaseLogger.bind(infoArea);
//        seasonListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
//        playerList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        applySeasonButton.setOnAction(e -> {
            ObservableList<Integer> selected = seasonListView.getSelectionModel().getSelectedItems();
            if (!selected.isEmpty()) {
                DataHandler.getInstance().setSelectedSeason(seasonListView.getSelectionModel().getSelectedItem());
                createMatchPanel.setVisible(false);
                refreshCurrentTab();
            } else {
                log("Please select at least one season.");
            }
        });

        seasonListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            deleteSeasonButton.setDisable(seasonListView.getSelectionModel().getSelectedItems().isEmpty());
        });


        sackManagerButton.setOnAction(e -> {
            IdNamePair selected = managerList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String query = "DELETE FROM premierleague.coach WHERE coachid = ?";
                if(QueryExecutor.runDDLQuery(conn, query, selected.getId()))
                    managerList.getItems().remove(selected);
            }
        });

        letPlayerGoButton.setOnAction(e -> {
            IdNamePair selected = playerList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String deletePositions = "DELETE FROM premierleague.playerposition WHERE playerid = ?";
                if(QueryExecutor.runDDLQuery(conn, deletePositions, selected.getId())) {

                    String query = "DELETE FROM premierleague.player WHERE playerid = ?";
                    if (QueryExecutor.runDDLQuery(conn, query, selected.getId()))
                        playerList.getItems().remove(selected);
                }
            }
        });

        addSeasonButton.setOnAction(e ->{
            try {
                if (!newSeasonText.getText().isEmpty() && Integer.parseInt(newSeasonText.getText()) > 1991 ) {
                    String query = "INSERT INTO premierleague.season (season_end_year) VALUES (?)";
                    if(QueryExecutor.runDDLQuery(conn, query, Integer.parseInt(newSeasonText.getText())))
                        seasonListView.getItems().add(Integer.parseInt(newSeasonText.getText()));
                }else {
                    DatabaseLogger.log("Season should be after the year 1992(the year premier league was founded");
                }
            }catch(NumberFormatException ex){
                DatabaseLogger.log("Season should be a number");
            }
        });

        managerList.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            sackManagerButton.setDisable(managerList.getSelectionModel().getSelectedItems().isEmpty());
        });

        seasonListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            applySeasonButton.setDisable(seasonListView.getSelectionModel().getSelectedItems().isEmpty());
        });

        playerList.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            letPlayerGoButton.setDisable(playerList.getSelectionModel().getSelectedItems().isEmpty());
        });

        deleteSeasonButton.setOnAction(e ->{
            Integer selected = seasonListView.getSelectionModel().getSelectedItem();
            String query = "DELETE FROM premierleague.season WHERE season_end_year = ?";
            if(QueryExecutor.runDDLQuery(conn, query, selected))
                seasonListView.getItems().remove(selected);
        });

        deleteTeamButton.setOnAction(this::deleteTeam);

        addTeamButton.setOnAction(e -> popupHandle("TEAM"));
        addManagerButton.setOnAction(e -> popupHandle("MANAGER"));
        signPlayerButton.setOnAction(e ->  popupHandle("PLAYER"));

    }

    private void deleteTeam(ActionEvent actionEvent) {
        TableView<ObservableList<String>> standingsTable = DataHandler.getInstance().getStandingsTable();
        ObservableList<String> selectedRow = standingsTable.getSelectionModel().getSelectedItem();

        String teamName = selectedRow.get(1);
        int season = Integer.parseInt(selectedRow.get(0));

        String query = "DELETE FROM PremierLeague.TeamSeason WHERE team_name = ? AND season_end_year = ?";

        try (PreparedStatement stmt = DataHandler.getInstance().getConnection().prepareStatement(query)) {
            stmt.setString(1, teamName);
            stmt.setInt(2, season);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                standingsTable.getItems().remove(selectedRow);
                DatabaseLogger.log("Deleted team " + teamName + " from season " + season);
            } else {
                DatabaseLogger.log("Team not found or already deleted.");
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Error deleting team from TeamSeason: " + e.getMessage());
        }
    }

    private void popupHandle(String name) {
        String fxmlFile = switch (name) {
            case "TEAM" -> "/popups/add_team_popup.fxml";
            case "MANAGER" -> "/popups/add_manager_popup.fxml";
            case "PLAYER" -> "/popups/sign_player_popup.fxml";
            default -> null;
        };

        if (fxmlFile != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Add " + name);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void connectToDatabase(String url, String username, String password) {
        try {
            conn = DriverManager.getConnection(url, username, password);
            DataHandler.getInstance().setConnection(conn);
            log("Connected to DB.");

            standingsService = new StandingsService(conn);
            squadService = new SquadService(conn);
            seasonsService = new SeasonService(conn);


            seasonsService.loadSeasons(seasonListView);
            handleTabs();
        } catch (SQLException e) {
            log("Connection failed: \n" );
            log("Error message: " + e.getMessage() + "\n");
            log("SQL state: " + e.getSQLState() + "\n");
            log("Error code: " + e.getErrorCode() + "\n");
        }
    }


    public void handleTabs(){
        try{
            standingsTab.setOnSelectionChanged(event -> {
                if (standingsTab.isSelected()) {
                    loadStandingsView();
                    log("Switched to standings");
                }
            });
            matchesTab.setOnSelectionChanged(event -> {
                if (matchesTab.isSelected()) {
                    loadMatchesView();
                    log("Switched to matches");
                }
            });
            playersTab.setOnSelectionChanged(event -> {
                if (playersTab.isSelected()) {
                    loadPlayersView();
                    log("Switched to players");
                }
            });
        } catch (Exception e) {
            log("Failed to load table tabs: " + e.getMessage());
        }
    }

    public void loadStandingsView() {
        Integer season = seasonListView.getSelectionModel().getSelectedItem();

        if (season == null) {
            standingsPanel.getChildren().setAll(new Label("Please select one of the seasons."));
            return;
        }

        standingsService.loadStandingsTable(season, standingsPanel, squadService, squadTitle, playerList, managerList);

        TableView<ObservableList<String>> standingsTable = DataHandler.getInstance().getStandingsTable();
        deleteTeamButton.setDisable(true);
        standingsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            deleteTeamButton.setDisable(newSel == null);
        });
    }



    public void loadMatchesView() {
        squadPanel.getChildren().setAll(createMatchDetailsPanel());
        configureTableForMatches();
    }

    private void configureTableForMatches() {

    }

    private Node createMatchDetailsPanel() {
        return null;
    }

    private Node createMatchFilters() {
        return null;
    }


    public void loadPlayersView() {
        squadPanel.getChildren().setAll(createPlayerDetailsPanel());
        configureTableForPlayers();
    }

    private void configureTableForPlayers() {

    }

    private Node createPlayerDetailsPanel() {
        return null;
    }

    private Node createPlayerFilters() {
        return null;
    }

    public void refreshCurrentTab() {
        Tab selectedTab = tableTabs.getSelectionModel().getSelectedItem();
        if (selectedTab.getText().equalsIgnoreCase("STANDINGS")) {
            loadStandingsView();
        } else if (selectedTab.getText().equalsIgnoreCase("MATCHES")) {
            loadMatchesView();
        } else if (selectedTab.getText().equalsIgnoreCase("PLAYERS")) {
            loadPlayersView();
        }
    }

    public void log(String message) {
        infoArea.appendText(message + "\n");
    }


}
