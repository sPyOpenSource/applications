module cr0s.javara.main {
    requires java.sql;
    requires transitive java.logging;
    requires assets;
    requires org.yaml.snakeyaml;
    requires javafx.swt;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires javafx.swing;
    requires javafx.web;
    requires jdk.jsobject;
    requires jfx.incubator.input;
    requires jfx.incubator.richtext;

    opens cr0s.javara.main to javafx.fxml;
    exports cr0s.javara.main;
    exports cr0s.javara.resources;
    exports cr0s.javara.render;
    exports cr0s.javara.render.map;
    exports cr0s.javara.ui;
    exports cr0s.javara.entity.aircraft;
    exports cr0s.javara.entity;
    exports cr0s.javara.util;
    exports cr0s.javara.entity.actor.activity;
    exports cr0s.javara.entity.infantry;
    exports cr0s.javara.entity.infantry.china;
    exports cr0s.javara.entity.building;
    exports cr0s.javara.entity.building.common;
    exports cr0s.javara.entity.building.soviet;
    exports cr0s.javara.entity.building.china;
    exports cr0s.javara.entity.actor;
    exports cr0s.javara.entity.vehicle.common;
    exports cr0s.javara.gameplay;
    exports cr0s.javara.ai;
    exports cr0s.javara.ai.states;
    exports mazesolver;
}
    