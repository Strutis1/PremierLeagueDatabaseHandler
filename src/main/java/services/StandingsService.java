package services;

import com.mif.crew.premierleaguepostgres.DataHandler;
import helper.DatabaseLogger;
import helper.IdNamePair;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.util.stream.Collectors;

public class StandingsService {
    private final Connection conn;

    public StandingsService(Connection conn) {
        this.conn = conn;
    }

    public void loadStandingsTable(Integer season, VBox standingsPanel, SquadService squadService, Label squadTitle,
                                   ListView<IdNamePair> playerList, ListView<IdNamePair> managerList) {
        standingsPanel.getChildren().clear();

        if (conn == null) {
            DatabaseLogger.log("No database connection.");
            return;
        }

        String query = "select team_name, played, wins, draws, losses, goals_for, goals_against " +
                "from premierleague.teamseasonstats " +
                "where season_end_year = ?";

        DatabaseLogger.log("Running query: " + query);

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, season);

            try (ResultSet rs = stmt.executeQuery()) {
                TableView<ObservableList<String>> table = buildStandingsTable(rs);
                standingsPanel.getChildren().add(table);

                VBox.setVgrow(table, Priority.ALWAYS);

                if (table.getItems().isEmpty()) {
                    standingsPanel.getChildren().add(new Label("No data found for the selected season(s)."));
                } else {
                    configureStandingsSelection(table, squadService, squadTitle, playerList, managerList);
                }
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Failed to load data: " + e.getMessage());
            standingsPanel.getChildren().add(new Label("Failed to load data."));
        }
    }

    private TableView<ObservableList<String>> buildStandingsTable(ResultSet rs) throws SQLException {
        TableView<ObservableList<String>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Region.USE_COMPUTED_SIZE);
        table.setMaxHeight(Double.MAX_VALUE);
        DataHandler.getInstance().setStandingsTable(table);

        TableColumn<ObservableList<String>, String> posCol = new TableColumn<>("Position");
        posCol.setCellValueFactory(data -> {
            int index = table.getItems().indexOf(data.getValue()) + 1;
            return new ReadOnlyStringWrapper(String.valueOf(index));
        });
        table.getColumns().add(posCol);

        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            final int colIndex = i - 1;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(meta.getColumnName(i));
            col.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().get(colIndex)));
            table.getColumns().add(col);
        }

        while (rs.next()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getString(i));
            }
            table.getItems().add(row);
        }

        return table;
    }

    private void configureStandingsSelection(
            TableView<ObservableList<String>> table,
            SquadService squadService,
            Label squadTitle,
            ListView<IdNamePair> playerList,
            ListView<IdNamePair> managerList
    ) {
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            if (newRow != null) {
                String teamName;
                Integer season = DataHandler.getInstance().getSelectedSeason();

                teamName = newRow.getFirst();
                squadTitle.setText(teamName + " in " + season);
                playerList.setItems(squadService.getPlayersForSeason(teamName, season));
                managerList.setItems(squadService.getManagersForSeason(teamName,season));
                DataHandler.getInstance().setSelectedTeam(newRow);
            }
        });
    }
}

