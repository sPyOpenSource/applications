package model.data;

import model.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class UpdatePlayerData extends Thread {
    public UpdatePlayerData(Player player) {
        this.player = player;
    }

    private final Player player;
    
    @Override
    public void run() {
        try {
            String Url = "jdbc:mysql://localhost/game";
            String name = "root";
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(Url, name, "");

            String query = String.format("UPDATE player SET level='%d', win='%d', lose='%d' WHERE ID='%s'",
                    player.getLevel(), player.getNumberWin(), player.getNumberLose(), player.getIdPlayer());

            connection.prepareStatement(query).execute();
            connection.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
}
