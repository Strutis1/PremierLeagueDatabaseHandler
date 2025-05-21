package services;

import com.mif.crew.premierleaguepostgres.DataHandler;
import helper.DatabaseLogger;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.*;

public class PlayerService {
    private final Connection conn;

    public PlayerService(Connection conn) {
        this.conn = conn;
    }

    public void loadPlayerTable(Integer season, VBox matchesPanel) {
        matchesPanel.getChildren().clear();

        if (conn == null) {
            DatabaseLogger.log("No database connection.");
            return;
        }

        String query = "SELECT matchid, week, match_date, home_team_name, home_goals, away_goals, away_team_name, result FROM premierleague.matches " +
                "WHERE season_end_year = ? ORDER BY match_date DESC";

        DatabaseLogger.log("Running query: " + query);

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, season);

            try (ResultSet rs = stmt.executeQuery()) {
                TableView<ObservableList<String>> table = buildPlayerTable(rs);
                matchesPanel.getChildren().add(table);

                VBox.setVgrow(table, Priority.ALWAYS);

                if (table.getItems().isEmpty()) {
                    matchesPanel.getChildren().clear();
                    matchesPanel.getChildren().add(new Label("No data found for the selected season(s)."));
                } else {
                    configurePlayerSelection(table);
                }
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Failed to load data: " + e.getMessage());
            matchesPanel.getChildren().add(new Label("Failed to load data."));
        }
    }

    private TableView<ObservableList<String>> buildPlayerTable(ResultSet rs) throws SQLException {
        TableView<ObservableList<String>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Region.USE_COMPUTED_SIZE);
        table.setMaxHeight(Double.MAX_VALUE);
        DataHandler.getInstance().setStandingsTable(table);

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

    private void configurePlayerSelection(
            TableView<ObservableList<String>> table) {
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            if (newRow != null) {
                DataHandler.getInstance().setSelectedMatch(newRow);
            }
        });
    }
}
