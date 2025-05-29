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

    public void loadPlayerTable(Integer season, VBox matchesPanel,
                                String teamFilter, String positionFilter, String nameFilter) {
        matchesPanel.getChildren().clear();

        if (conn == null) {
            DatabaseLogger.log("No database connection.");
            return;
        }

        StringBuilder queryBuilder = new StringBuilder("""
        SELECT p.playerid, p.full_name, pos.position, p.team_name,
               p.goals, p.assists, p.appearances, p.minutes_played
        FROM premierleague.player AS p
        JOIN premierleague.playerposition AS pos ON p.playerid = pos.playerid
        WHERE p.season_end_year = ?
        """);

        if (teamFilter != null) {
            queryBuilder.append(" AND p.team_name = ?");
        }
        if (positionFilter != null) {
            queryBuilder.append(" AND pos.position = ?");
        }
        if (nameFilter != null) {
            queryBuilder.append(" AND LOWER(p.full_name) LIKE ?");
        }

        String query = queryBuilder.toString();
        DatabaseLogger.log("Running query: " + query);

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            int index = 1;
            stmt.setInt(index++, season);
            if (teamFilter != null) stmt.setString(index++, teamFilter);
            if (positionFilter != null) stmt.setString(index++, positionFilter);
            if (nameFilter != null) stmt.setString(index++, "%" + nameFilter.toLowerCase() + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                TableView<ObservableList<String>> table = buildPlayerTable(rs);
                matchesPanel.getChildren().add(table);
                VBox.setVgrow(table, Priority.ALWAYS);

                if (table.getItems().isEmpty()) {
                    matchesPanel.getChildren().clear();
                    matchesPanel.getChildren().add(new Label("No data found for the selected filters."));
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
        DataHandler.getInstance().setPlayersTable(table);

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

    private void configurePlayerSelection(TableView<ObservableList<String>> table) {
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            if (newRow != null) {
                DataHandler.getInstance().setSelectedPlayer(newRow);
            }
        });
    }
}
