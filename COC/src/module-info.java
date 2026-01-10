module com.coc {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    //requires mysql.connector.j;
    requires javafx.swt;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.swing;
    requires javafx.web;
    requires assets;
    requires snakeyaml;
    requires cr0s.javara.main;
    
    opens com.coc to javafx.fxml;
    exports com.coc;
}