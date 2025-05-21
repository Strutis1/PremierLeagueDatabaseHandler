package com.mif.crew.premierleaguepostgres;

import helper.DatabaseLogger;
import helper.IdNamePair;
import helper.QueryExecutor;
import helper.Utilz;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
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
import javafx.util.Pair;
import services.*;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class DatabaseViewController {

    private StandingsService standingsService;
    private SquadService squadService;
    private SeasonService seasonsService;
    private MatchService matchService;
    private PlayerService playerService;

    @FXML
    private Button addManagerButton;

    @FXML
    private Button addSeasonButton;

    @FXML
    private Button registerMatchButton;

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
            Integer selected = seasonListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                DataHandler.getInstance().setSelectedSeason(seasonListView.getSelectionModel().getSelectedItem());
                createMatchPanel.setDisable(false);
                refreshCurrentTab();
                fillTeams(homeTeamCombo, selected);
                fillTeams(awayTeamCombo,selected);
                fillWeeks(matchWeekCombo, selected);

                Pair<LocalDate, LocalDate> range = getSeasonDateRange(selected);
                if (range != null) {
                    matchDatePicker.setDayCellFactory(picker -> new DateCell() {
                        @Override
                        public void updateItem(LocalDate date, boolean empty) {
                            super.updateItem(date, empty);
                            setDisable(empty || date.isBefore(range.getKey()) || date.isAfter(range.getValue()));
                        }
                    });
                }
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

        BooleanBinding allFieldsFilled =
                homeTeamCombo.valueProperty().isNotNull()
                        .and(awayTeamCombo.valueProperty().isNotNull())
                        .and(matchWeekCombo.valueProperty().isNotNull())
                        .and(matchDatePicker.valueProperty().isNotNull())
                        .and(homeGoalsText.textProperty().isNotEmpty())
                        .and(awayGoalsText.textProperty().isNotEmpty());

        registerMatchButton.disableProperty().bind(allFieldsFilled.not());

        registerMatchButton.setOnAction(this::registerMatch);

        deleteMatchButton.setOnAction(this::deleteMatch);

        deleteTeamButton.setOnAction(this::deleteTeam);


        addTeamButton.setOnAction(e -> popupHandle("TEAM"));
        addManagerButton.setOnAction(e -> popupHandle("MANAGER"));
        signPlayerButton.setOnAction(e ->  popupHandle("PLAYER"));
        changeMatchButton.setOnAction(e -> popupHandle("MATCH"));

    }

    private void registerMatch(ActionEvent actionEvent) {
        String query = "INSERT INTO PremierLeague.matches (home_team_Name, away_team_name, week, match_date, home_goals, away_goals, Season_End_Year) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DataHandler.getInstance().getConnection();
        int season = DataHandler.getInstance().getSelectedSeason();



        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, homeTeamCombo.getValue());
            stmt.setString(2, awayTeamCombo.getValue());
            stmt.setInt(3, matchWeekCombo.getValue());
            stmt.setDate(4, Date.valueOf(matchDatePicker.getValue()));
            stmt.setInt(5, Integer.parseInt(homeGoalsText.getText()));
            stmt.setInt(6, Integer.parseInt(awayGoalsText.getText()));
            stmt.setInt(7, season);

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            Integer matchId = null;
            if (generatedKeys.next()) {
                matchId = generatedKeys.getInt(1);
            }

            int affected = stmt.executeUpdate();

            if (affected > 0) {
                ObservableList<String> row = FXCollections.observableArrayList();
                if(matchId == null){
                    DatabaseLogger.log("Couldn't generate a match id for the new match");
                }
                row.add(String.valueOf(matchId));
                row.add(homeTeamCombo.getValue());
                row.add(awayTeamCombo.getValue());
                row.add(String.valueOf(matchWeekCombo.getValue()));
                row.add(String.valueOf(matchDatePicker.getValue()));
                row.add(homeGoalsText.getText());
                row.add(awayGoalsText.getText());
                row.add(String.valueOf(season));

                DataHandler.getInstance().getMatchesTable().getItems().add(row);
                DatabaseLogger.log("Match added successfully");
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Failed to insert into TeamSeason: " + e.getMessage());
        }
    }

    private void fillWeeks(ComboBox<Integer> matchWeekCombo, Integer selected) {
        if (conn == null) {
            DatabaseLogger.log("No database connection.");
            return;
        }

        String query = "SELECT count(distinct team_name) FROM premierleague.teamseason " +
                "WHERE season_end_year = ?";

        DatabaseLogger.log("Running query: " + query);

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, selected);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int teamCount = rs.getInt(1);
                    matchWeekCombo.getItems().clear();
                    Utilz.fillIntegerComboBox(matchWeekCombo, 1, (teamCount - 1) * 2);
                }
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Failed to load data: " + e.getMessage());
            matchesPanel.getChildren().add(new Label("Failed to load data."));
        }

    }

    private void fillTeams(ComboBox<String> teamCombo, Integer selected) {
        if (conn == null) {
            DatabaseLogger.log("No database connection.");
            return;
        }

        String query = "SELECT team_name FROM premierleague.team " +
                "WHERE season_end_year = ?";

        DatabaseLogger.log("Running query: " + query);

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, selected);

            try (ResultSet rs = stmt.executeQuery()) {
                ObservableList<String> teams = FXCollections.observableArrayList();
                while (rs.next()) {
                    teams.add(rs.getString("team_name"));
                }
                teamCombo.setItems(teams);
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Failed to load data: " + e.getMessage());
            matchesPanel.getChildren().add(new Label("Failed to load data."));
        }
    }

    private void deleteMatch(ActionEvent actionEvent) {
        TableView<ObservableList<String>> standingsTable = DataHandler.getInstance().getStandingsTable();
        ObservableList<String> selectedRow = standingsTable.getSelectionModel().getSelectedItem();

        int matchId = Integer.parseInt(selectedRow.getFirst());

        String query = "DELETE FROM PremierLeague.Matches WHERE matchId = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, matchId);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                standingsTable.getItems().remove(selectedRow);
                DatabaseLogger.log("Deleted match" + matchId);
            } else {
                DatabaseLogger.log("Team not found or already deleted.");
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Error deleting team from TeamSeason: " + e.getMessage());
        }
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
            case "MATCH" -> "/popups/alter_match_popup.fxml";
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
                DatabaseLogger.log(e.getMessage());
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
            matchService = new MatchService(conn);
            playerService = new PlayerService(conn);



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
        Integer season = seasonListView.getSelectionModel().getSelectedItem();

        if (season == null) {
            standingsPanel.getChildren().setAll(new Label("Please select one of the seasons."));
            return;
        }

        matchService.loadMatchTable(season, matchesPanel);

        TableView<ObservableList<String>> matchesTable = DataHandler.getInstance().getMatchesTable();
        deleteMatchButton.setDisable(true);
        matchesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            deleteMatchButton.setDisable(newSel == null);
        });
    }

    private Pair<LocalDate, LocalDate> getSeasonDateRange(int seasonId) {
        String query = "SELECT start_date, end_date FROM premierleague.season WHERE season_end_year = ?";
        try (PreparedStatement stmt = DataHandler.getInstance().getConnection().prepareStatement(query)) {
            stmt.setInt(1, seasonId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                LocalDate start = rs.getDate("start_date").toLocalDate();
                LocalDate end = rs.getDate("end_date").toLocalDate();
                return new Pair<>(start, end);
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Failed to fetch season date range: " + e.getMessage());
        }
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
