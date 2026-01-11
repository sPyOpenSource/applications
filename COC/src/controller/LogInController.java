package controller;

import model.Player;

import java.util.ArrayList;
import model.Map.MapLoader;

public class LogInController {
    public LogInController(ArrayList<Player> players) {
        this.players = players;
    }

    private final ArrayList<Player> players;

    public Player getPlayer(String idPlayer, String password) throws Exception {
        for (Player player : players){
            if (player.getIdPlayer().equals(idPlayer) && player.getPassword().equals(password)){
                return player;
            }
        }
        return new Player(new MapLoader(4, "map4"), "", "");
        //throw new Exception("username or password is wrong!");
    }
}
