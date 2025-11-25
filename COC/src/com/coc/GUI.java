package com.coc;

import controller.LogInController;
import model.data.LoadPlayerData;
import javafx.application.Application;
import javafx.stage.Stage;
import model.Player;

import view.*;

public class GUI extends Application {
    @Override
    public void start(Stage stage) throws InterruptedException {
        try {
            LoadPlayerData loadPlayerData = new LoadPlayerData();
            loadPlayerData.start();
            loadPlayerData.join();
            Player player = new LogInController(loadPlayerData.getPlayers()).getPlayer("", "");
            new PlayerPanel(player, loadPlayerData.getPlayers()).start(new Stage());
        } catch (Exception ex) {
            System.getLogger(GUI.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
