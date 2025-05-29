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

import static helper.DatabaseLogger.log;

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
    private Button clearFiltersButton;

    @FXML
    private Button deleteMatchButton;

    @FXML
    private Button deleteSeasonButton;

    @FXML
    private Button deleteTeamButton;

    @FXML
    private Button deletePlayerButton;

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
    private TextField playerNameFilter;

    @FXML
    private ComboBox<String> playerPositionCombo;

    @FXML
    private ComboBox<String> playerTeamCombo;

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
    private VBox filterPlayerPanel;

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
        DataHandler.getInstance().setMainController(this);

        ObservableList<String> playerPositions = FXCollections.observableArrayList("All", "GK", "DF", "MF", "FW");
        playerPositionCombo.setItems(playerPositions);
        applySeasonButton.setOnAction(e -> {
            Integer selected = seasonListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                DataHandler.getInstance().setSelectedSeason(selected);
                deleteTeamButton.setDisable(true);
                deleteMatchButton.setDisable(true);
                changeMatchButton.setDisable(true);
                deletePlayerButton.setDisable(true);
                addTeamButton.setDisable(false);

                squadPanel.setDisable(false);
                createMatchPanel.setDisable(false);
                filterPlayerPanel.setDisable(false);
                refreshCurrentTab();
                fillTeams(playerTeamCombo, selected);
                playerTeamCombo.getItems().addFirst("All Teams");
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
                    log("Season should be after the year 1992(the year premier league was founded");
                }
            }catch(NumberFormatException ex){
                log("Season should be a number");
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
                        .and(awayGoalsText.textProperty().isNotEmpty())
                        .and(homeTeamCombo.valueProperty().isNotEqualTo(awayTeamCombo.valueProperty().get()));

        clearFiltersButton.setOnAction(e -> {
            playerTeamCombo.getSelectionModel().clearSelection();
            playerPositionCombo.getSelectionModel().clearSelection();
            playerNameFilter.clear();
        });

        registerMatchButton.disableProperty().bind(allFieldsFilled.not());

        registerMatchButton.setOnAction(this::registerMatch);

        deleteMatchButton.setOnAction(this::deleteMatch);

        deleteTeamButton.setOnAction(this::deleteTeam);

        deletePlayerButton.setOnAction(this::deletePlayer);


        addTeamButton.setOnAction(e -> popupHandle("TEAM"));
        addManagerButton.setOnAction(e -> popupHandle("MANAGER"));
        signPlayerButton.setOnAction(e ->  popupHandle("PLAYER"));
        changeMatchButton.setOnAction(e -> popupHandle("MATCH"));

    }

    private void deletePlayer(ActionEvent actionEvent) {
        ObservableList<String> selected = DataHandler.getInstance().getSelectedPlayer();
        if (selected != null) {
            String deletePositions = "DELETE FROM premierleague.playerposition WHERE playerid = ?";
            if(QueryExecutor.runDDLQuery(conn, deletePositions, Integer.parseInt(selected.getFirst()))){
                String query = "DELETE FROM premierleague.player WHERE playerid = ?";
                if (QueryExecutor.runDDLQuery(conn, query, Integer.parseInt(selected.getFirst())))
                    DataHandler.getInstance().getPlayersTable().getItems().removeIf(row -> row.getFirst().equals(selected.getFirst()));
            }
        }
    }

    private void registerMatch(ActionEvent actionEvent) {
        String query = "INSERT INTO PremierLeague.matches (home_team_Name, away_team_name, week, match_date, home_goals, away_goals, Season_End_Year)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DataHandler.getInstance().getConnection();
        int season = DataHandler.getInstance().getSelectedSeason();

        int homeGoals = Integer.parseInt(homeGoalsText.getText());
        int awayGoals = Integer.parseInt(awayGoalsText.getText());

        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, homeTeamCombo.getValue());
            stmt.setString(2, awayTeamCombo.getValue());
            stmt.setInt(3, matchWeekCombo.getValue());
            stmt.setDate(4, Date.valueOf(matchDatePicker.getValue()));
            stmt.setInt(5, Integer.parseInt(homeGoalsText.getText()));
            stmt.setInt(6, Integer.parseInt(awayGoalsText.getText()));
            stmt.setInt(7, season);

            int affected = stmt.executeUpdate();

            Integer matchId = null;
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                matchId = generatedKeys.getInt(1);

            }

            if (affected > 0) {
                ObservableList<String> row = FXCollections.observableArrayList();
                if(matchId == null){
                    log("Couldn't generate a match id for the new match");
                    return;
                }
                char result;
                if(homeGoals > awayGoals) result = 'H';
                else if(homeGoals < awayGoals) result = 'A';
                else result = 'D';
                row.add(String.valueOf(matchId));
                row.add(String.valueOf(matchWeekCombo.getValue()));
                row.add(String.valueOf(matchDatePicker.getValue()));
                row.add(homeTeamCombo.getValue());
                row.add(homeGoalsText.getText());
                row.add(awayGoalsText.getText());
                row.add(awayTeamCombo.getValue());
                row.add(String.valueOf(result));


                DataHandler.getInstance().getMatchesTable().getItems().add(row);
                log("Match added successfully");
            }
        } catch (SQLException e) {
            log("Failed to insert into TeamSeason: " + e.getMessage());
        }
    }

    private void fillWeeks(ComboBox<Integer> matchWeekCombo, Integer selected) {
        if (conn == null) {
            log("No database connection.");
            return;
        }

        String query = "SELECT count(distinct team_name) FROM premierleague.teamseason " +
                "WHERE season_end_year = ?";

        log("Running query: " + query);

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
            log("Failed to load data: " + e.getMessage());
            matchesPanel.getChildren().add(new Label("Failed to load data."));
        }

    }

    private void fillTeams(ComboBox<String> teamCombo, Integer selected) {
        if (conn == null) {
            log("No database connection.");
            return;
        }

        String query = "SELECT team_name FROM premierleague.teamseason " +
                "WHERE season_end_year = ?";

        log("Running query: " + query);

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
            log("Failed to load data: " + e.getMessage());
            matchesPanel.getChildren().add(new Label("Failed to load data."));
        }
    }

    private void deleteMatch(ActionEvent actionEvent) {
        TableView<ObservableList<String>> matchesTable = DataHandler.getInstance().getMatchesTable();
        ObservableList<String> selectedRow = matchesTable.getSelectionModel().getSelectedItem();

        int matchId = Integer.parseInt(selectedRow.getFirst());

        String query = "DELETE FROM PremierLeague.Matches WHERE matchId = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, matchId);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                matchesTable.getItems().remove(selectedRow);
                log("Deleted match" + matchId);
            } else {
                log("Team not found or already deleted.");
            }
        } catch (SQLException e) {
            log("Error deleting team from TeamSeason: " + e.getMessage());
        }

    }

    private void deleteTeam(ActionEvent actionEvent) {
        TableView<ObservableList<String>> standingsTable = DataHandler.getInstance().getStandingsTable();
        ObservableList<String> selected = standingsTable.getSelectionModel().getSelectedItem();

        try {
            if (selected != null) {
                conn.setAutoCommit(false);
                int season = DataHandler.getInstance().getSelectedSeason();
                String teamName = selected.getFirst();
                String deleteTeam = "DELETE FROM PremierLeague.TeamSeason WHERE team_name = ? AND season_end_year = ?";
                if (QueryExecutor.runDDLQuery(conn, deleteTeam, teamName, season)) {
                    DatabaseLogger.log("Deleted team " + teamName);
                    String deleteMatches = "DELETE FROM premierleague.matches WHERE home_team_name = ? OR away_team_name = ?";
                    if (QueryExecutor.runDDLQuery(conn, deleteMatches, teamName, teamName)) {
                        DatabaseLogger.log("Deleted matches associated with " + teamName);
                        conn.commit();
                        refreshCurrentTab();
                    }
                }
            }
        } catch (SQLException e) {
            DatabaseLogger.log(e.getMessage());
            try {
                conn.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }finally{
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
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
                log(e.getMessage());
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
        Integer season = DataHandler.getInstance().getSelectedSeason();

        if (season == null) {
            standingsPanel.getChildren().setAll(new Label("Please select one of the seasons."));
            return;
        }

        standingsService.loadStandingsTable(season, standingsPanel, squadService, squadTitle, playerList, managerList);

        TableView<ObservableList<String>> standingsTable = DataHandler.getInstance().getStandingsTable();
        standingsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            deleteTeamButton.setDisable(newSel == null);
        });
    }



    public void loadMatchesView() {
        Integer season = DataHandler.getInstance().getSelectedSeason();

        if (season == null) {
            matchesPanel.getChildren().setAll(new Label("Please select one of the seasons."));
            return;
        }

        matchService.loadMatchTable(season, matchesPanel);

        TableView<ObservableList<String>> matchesTable = DataHandler.getInstance().getMatchesTable();
        matchesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            deleteMatchButton.setDisable(newSel == null);
            changeMatchButton.setDisable(newSel == null);
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
            log("Failed to fetch season date range: " + e.getMessage());
        }
        return null;
    }


    public void loadPlayersView() {
        Integer season = DataHandler.getInstance().getSelectedSeason();

        if (season == null) {
            playersPanel.getChildren().setAll(new Label("Please select one of the seasons."));
            return;
        }


        updatePlayerTable(season);

        playerTeamCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePlayerTable(season));
        playerPositionCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePlayerTable(season));
        playerNameFilter.textProperty().addListener((obs, oldVal, newVal) -> updatePlayerTable(season));

        TableView<ObservableList<String>> playersTable = DataHandler.getInstance().getPlayersTable();
        playersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            deletePlayerButton.setDisable(newSel == null);
        });
    }

    private void updatePlayerTable(int season) {
        String selectedTeam = playerTeamCombo.getValue();
        String selectedPosition = playerPositionCombo.getValue();
        String nameFilter = playerNameFilter.getText();

        if ("All Teams".equals(selectedTeam)) selectedTeam = null;
        if ("All".equals(selectedPosition)) selectedPosition = null;
        if (nameFilter != null && nameFilter.isBlank()) nameFilter = null;

        playerService.loadPlayerTable(season, playersPanel, selectedTeam, selectedPosition, nameFilter);
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


}
