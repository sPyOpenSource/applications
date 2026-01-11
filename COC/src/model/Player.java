package model;

import cr0s.javara.render.map.Map;

import java.io.Serializable;

public class Player implements Serializable {
    public Player(Map map, String idPlayer, String password) {
        this.idPlayer = idPlayer;
        this.password = password;
        this.level = 4;
        this.numberWin = 0;
        this.numberLose = 0;
        this.map = map;
    }

    private final String idPlayer;
    private final String password;

    public Map getMap() {
        return map;
    }

    private final Map map;
    private int level;
    private int numberWin;
    private int numberLose;

    public String getIdPlayer() {
        return idPlayer;
    }

    public String getPassword() {
        return password;
    }

    public int getLevel() {
        return level;
    }

    public int getNumberWin() {
        return numberWin;
    }

    public void Win() {
        this.numberWin++;
        if(level < 4) level++;
    }

    public int getNumberLose() {
        return numberLose;
    }

    public void Lose() {
        this.numberLose++;
        if(level > 1) level--;
    }
}
