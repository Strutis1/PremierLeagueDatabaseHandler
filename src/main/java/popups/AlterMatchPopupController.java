package popups;

import com.mif.crew.premierleaguepostgres.DataHandler;
import helper.DatabaseLogger;
import helper.Utilz;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AlterMatchPopupController {
    @FXML
    private TextField awayGoalsText;

    @FXML
    private Label awayTeamLabel;

    @FXML
    private TextField homeGoalsText;

    @FXML
    private Label homeTeamLabel;

    @FXML
    private Button changeButton;

    ObservableList<String> selectedMatch;



    public void initialize(){
        selectedMatch = DataHandler.getInstance().getSelectedMatch();

        homeTeamLabel.setText(selectedMatch.get(3));
        awayTeamLabel.setText(selectedMatch.get(6));

        homeGoalsText.setOnAction(e ->{
            changeButton.setDisable(!Utilz.isPositiveNumber(homeGoalsText) || !Utilz.isPositiveNumber(awayGoalsText));
        });
        awayGoalsText.setOnAction(e ->{
            changeButton.setDisable(!Utilz.isPositiveNumber(homeGoalsText) || !Utilz.isPositiveNumber(awayGoalsText));
        });

        changeButton.setOnAction(this::changeMatch);


    }

    private void changeMatch(ActionEvent actionEvent) {
        int matchId = Integer.parseInt(selectedMatch.getFirst());
        String homeGoals = homeGoalsText.getText();
        String awayGoals = awayGoalsText.getText();

        Connection conn = DataHandler.getInstance().getConnection();

        String query = "UPDATE premierleague.matches SET home_goals = ?, away_goals = ? WHERE matchid = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, Integer.parseInt(homeGoals));
            stmt.setInt(2, Integer.parseInt(awayGoals));
            stmt.setInt(3, matchId);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                ObservableList<ObservableList<String>> table = DataHandler.getInstance().getMatchesTable().getItems();
                ObservableList<String> row = DataHandler.getInstance().getSelectedMatch();

                int rowIndex = table.indexOf(row);
                if (rowIndex != -1) {
                    row.set(4, homeGoals);
                    row.set(5, awayGoals);
                    table.set(rowIndex, row);
                }

                DatabaseLogger.log("Updated match: matchID=" + matchId + ", new result=" + homeGoals + "-" + awayGoals + " (" + affected + " rows affected)");
                changeButton.getScene().getWindow().hide();
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Failed to update match: " + e.getMessage());
        } catch (NumberFormatException e) {
            DatabaseLogger.log("Invalid input: " + e.getMessage());
        }
    }

}
