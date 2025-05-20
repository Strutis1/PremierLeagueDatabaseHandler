package services;

import helper.DatabaseLogger;
import helper.IdNamePair;
import helper.QueryExecutor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.util.List;

public class SquadService {
    private final Connection conn;

    public SquadService(Connection conn) {
        this.conn = conn;
    }

    public ObservableList<IdNamePair> getPlayersForSeason(String teamName, int seasonYear) {
        String query = "SELECT playerid, full_name FROM premierleague.player " +
                "WHERE team_name = ? AND season_end_year = ? ORDER BY full_name";


        List<IdNamePair> result = QueryExecutor.runIntStringQuery(conn, query, teamName, seasonYear);
        return FXCollections.observableArrayList(result);
    }

    public ObservableList<IdNamePair> getManagersForSeason(String teamName, int seasonYear) {
        String query = "SELECT DISTINCT c.coachid, c.first_name || ' ' || c.last_name AS coach_name " +
                "FROM premierleague.coach c " +
                "JOIN premierleague.season s ON s.season_end_year = ? " +
                "WHERE c.team_name = ? " +
                "AND c.start_date <= s.end_date " +
                "AND (c.end_date IS NULL OR c.end_date >= s.start_date)";

        DatabaseLogger.log("Running query: " + query);

        List<IdNamePair> result = QueryExecutor.runIntStringQuery(conn, query, seasonYear, teamName);
        return FXCollections.observableArrayList(result);
    }
}
