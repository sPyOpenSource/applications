module cr0s.javara.main {
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

    opens cr0s.javara.main to javafx.fxml;
    exports cr0s.javara.main;
    exports cr0s.javara.render.map;
    exports cr0s.javara.ui;
    exports cr0s.javara.entity.aircraft;
    exports cr0s.javara.entity;
    exports cr0s.javara.util;
    exports cr0s.javara.entity.actor.activity;
    exports cr0s.javara.entity.infantry;
    exports cr0s.javara.entity.building;
    exports cr0s.javara.entity.building.common;
    exports mazesolver;
}