package services;

import helper.DatabaseLogger;
import helper.QueryExecutor;
import javafx.scene.control.ListView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SeasonService {

    private final Connection conn;

    public SeasonService(Connection conn) {
        this.conn = conn;
    }

    public void loadSeasons(ListView<Integer> seasonListView) {

        String query = "SELECT season_end_year FROM premierleague.season ORDER BY season_end_year DESC";

        List<Integer> seasons = QueryExecutor.runIntQuery(conn, query);
        seasonListView.getItems().addAll(seasons);
    }
}
