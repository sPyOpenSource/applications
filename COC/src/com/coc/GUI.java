package com.coc;

import controller.LogInController;
import javafx.application.Application;
import javafx.stage.Stage;

import model.Player;
import model.data.LoadData;
import view.*;

public class GUI extends Application {
    @Override
    public void start(Stage stage) throws InterruptedException {
        try {
            LoadData playerData = new LoadData();
            Player player = new LogInController(playerData.getPlayers()).getPlayer("", "");
            new PlayerPanel(player, playerData.getPlayers()).start(new Stage());
        } catch (Exception ex) {
            System.getLogger(GUI.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
