module com.mif.crew.premierleaguepostgres {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;


    opens com.mif.crew.premierleaguepostgres to javafx.fxml;
    exports com.mif.crew.premierleaguepostgres;
    exports popups;
    opens popups to javafx.fxml;
}