package controller;

import model.IdPlayerException;
import model.Player;

import java.util.ArrayList;

public class SignUpController {
    public SignUpController(ArrayList<Player> players) {
        this.players = players;
    }
    
    private final ArrayList<Player> players;
    
    public void checkId(String idPlayer) throws IdPlayerException{
        for (Player player : players){
            if (player.getIdPlayer().equals(idPlayer)){
                throw new IdPlayerException();
            }
        }
    }
}
