package helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QueryExecutor {

    public static List<String> runStringQuery(Connection conn, String query, Object... params) {
        List<String> result = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            DatabaseLogger.log("Running query: " + query);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Query failed: " + e.getMessage());
        }

        return result;
    }

    public static List<Integer> runIntQuery(Connection conn, String query, Object... params) {
        List<Integer> result = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            DatabaseLogger.log("Running query: " + query);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Query failed: " + e.getMessage());
        }

        return result;
    }

    public static boolean runDDLQuery(Connection conn, String query, Object... params) {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            DatabaseLogger.log("Running DDL/DML query: " + query);
            int affectedRows = stmt.executeUpdate();
            if(affectedRows > 0);{
                stmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            DatabaseLogger.log("DDL/DML query failed: " + e.getMessage());
            return false;
        }
    }



    public static List<IdNamePair> runIntStringQuery(Connection conn, String query, Object... params) {
        List<IdNamePair> result = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            DatabaseLogger.log("Running query: " + query);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new IdNamePair(rs.getInt(1),rs.getString(2)));
                }
            }
        } catch (SQLException e) {
            DatabaseLogger.log("Query failed: " + e.getMessage());
        }

        return result;
    }
}
