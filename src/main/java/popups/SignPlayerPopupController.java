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

public class SignPlayerPopupController {

    @FXML
    private TableView<ObservableList<String>> candidateTable;

    @FXML
    private Button signPlayerButton;


    public void initialize() {
        candidateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        candidateTable.setPrefHeight(Region.USE_COMPUTED_SIZE);
        candidateTable.setMaxHeight(Double.MAX_VALUE);
        int selectedSeason = DataHandler.getInstance().getSelectedSeason();
        Connection conn = DataHandler.getInstance().getConnection();
        String query = "SELECT playerid, full_name, team_name FROM premierleague.player WHERE season_end_year = ?";

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
            signPlayerButton.setDisable(candidateTable.getSelectionModel().getSelectedItems().isEmpty());
        });

        signPlayerButton.setOnAction(this::signPlayer);

    }

    private void signPlayer(ActionEvent actionEvent) {
        ObservableList<String> selectedRow = candidateTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) return;

        int playerId = Integer.parseInt(selectedRow.getFirst());

        String selectedTeamName = DataHandler.getInstance().getSelectedTeam().get(0);

        Connection conn = DataHandler.getInstance().getConnection();
        String query = "UPDATE premierleague.player SET team_name = ? WHERE playerid = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, selectedTeamName);
            stmt.setInt(2, playerId);
            int updated = stmt.executeUpdate();
            DatabaseLogger.log("Updated player team: playerID=" + playerId + ", new team=" + selectedTeamName + " (" + updated + " rows affected)");
            signPlayerButton.getScene().getWindow().hide();
        } catch (SQLException e) {
            DatabaseLogger.log("Failed to update player team: " + e.getMessage());
        }
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