package model.data;

import model.Map.*;
import model.Player;

import java.sql.*;
import java.util.ArrayList;

public class LoadData {

    private final ArrayList<Player> players = new ArrayList<>();
    
    public ArrayList<Player> getPlayers() {
        /*try {
            String Url = "jdbc:mysql://localhost/game";
            String name = "root";
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(Url, name, "");
            String sqlCommand = "SELECT ID,password,level,win,lose,mapId from player";

            Statement statement = connection.prepareStatement(sqlCommand);
            ResultSet resultSet = statement.executeQuery(sqlCommand);
            while(resultSet.next())
            {
                String username = resultSet.getString("ID");
                String password = resultSet.getString("password");
                String level = resultSet.getString("level");
                String win = resultSet.getString("win");
                String lose = resultSet.getString("lose");
                String mapId = resultSet.getString("mapId");

                Player player = switch (mapId) {
                    case "1" -> new Player(username, password, new Map1());
                    case "2" -> new Player(username, password, new Map2());
                    case "3" -> new Player(username, password, new Map3());
                    default -> new Player(username, password, new Map4());
                };
                player.setLevel(Integer.parseInt(level));
                player.setNumberWin(Integer.parseInt(win));
                player.setNumberLose(Integer.parseInt(lose));

                players.add(player);
            }
        }
        catch (ClassNotFoundException | NumberFormatException | SQLException e) {
            e.printStackTrace();
        }*/
        return players;
    }
    
}
