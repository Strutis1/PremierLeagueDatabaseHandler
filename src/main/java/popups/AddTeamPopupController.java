package popups;

import com.mif.crew.premierleaguepostgres.DataHandler;
import helper.DatabaseLogger;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;

import java.sql.*;

public class AddTeamPopupController {

    @FXML
    private TableView<ObservableList<String>> candidateTable;

    @FXML
    private Button addTeamButton;

    public void initialize() {
        candidateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        candidateTable.setPrefHeight(Region.USE_COMPUTED_SIZE);
        candidateTable.setMaxHeight(Double.MAX_VALUE);
        int selectedSeason = DataHandler.getInstance().getSelectedSeason();
        Connection conn = DataHandler.getInstance().getConnection();
        String query = "SELECT name from premierleague.team as t " +
                "EXCEPT " +
                "SELECT DISTINCT team_name from premierleague.teamseason " +
                "WHERE season_end_year = ?";

        DatabaseLogger.log("Running query: " + query);

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, selectedSeason);
            try (ResultSet rs = stmt.executeQuery()) {
                fillCandidateTable(rs);
            }
        } catch (SQLException e) {
            DatabaseLogger.log(e.getMessage());
        }

        candidateTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            addTeamButton.setDisable(candidateTable.getSelectionModel().getSelectedItems().isEmpty());
        });

        addTeamButton.setOnAction(e->{
            boolean success = addTeam();

            if (success) {
                DatabaseLogger.log("Team added to Season successfully.");
                addTeamButton.getScene().getWindow().hide();
            } else {
                DatabaseLogger.log("Error occurred.");
            }
        });

    }

    private boolean addTeam() {
        String query = "INSERT INTO PremierLeague.TeamSeason (Team_Name, Season_End_Year) VALUES (?, ?)";
        Connection conn = DataHandler.getInstance().getConnection();
        int season = DataHandler.getInstance().getSelectedSeason();

        ObservableList<String> selectedRow = candidateTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) return false;

        String teamName = selectedRow.getFirst();

        DatabaseLogger.log("Running query: " + query);

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, teamName);
            stmt.setInt(2, season);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(teamName);
                row.add("0");

                DataHandler.getInstance().getStandingsTable().getItems().add(row);
                return true;
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Failed to insert into TeamSeason: " + e.getMessage());
        }

        return false;
    }


    private void fillCandidateTable(ResultSet rs) throws SQLException {
        candidateTable.getColumns().clear();
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();

        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        for (int i = 1; i <= colCount; i++) {
            final int colIndex = i - 1;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(meta.getColumnName(i));
            col.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().get(colIndex)));
            candidateTable.getColumns().add(col);
        }

        while (rs.next()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for (int i = 1; i <= colCount; i++) {
                row.add(rs.getString(i));
            }
            data.add(row);
        }

        candidateTable.setItems(data);
    }
}
