module test {
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
    requires org.yaml.snakeyaml;
    requires hamcrest.core;
    requires org.json;
    requires junit;
    requires cr0s.javara.main;
    
    opens test to javafx.fxml;
    exports test;
}
    