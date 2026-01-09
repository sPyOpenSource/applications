/*
 * Copyright (C) 2025 xuyi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cr0s.javara.render.map;

import cr0s.javara.entity.MobileEntity;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;

import java.util.ArrayList;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;

import mazesolver.Director;
import mazesolver.Helper;
import mazesolver.Maze;

/**
 *
 * @author xuyi
 */
class AStarPathFinder {
    
    private final Maze maze;
    private final int max;

    private ArrayList<Pos> router;
    private Director director;
    
    AStarPathFinder(World world, int MAX_SEARCH_DISTANCE) {
        maze = new Maze(world.blockingEntityMap.blockingMap, null);
        max = MAX_SEARCH_DISTANCE;
    }

    Path findPath(MobileEntity me, Pos start, Pos goal) {
        maze.setEnd(goal);
        maze.setStart(start, max);
        director = new Director(maze, null);
        director.run();
        router = director.getBestRoute();
        //Helper.optimizePath(router, maze);
        Path path = new Path();
        for(Pos point:router){
            MoveTo line = new MoveTo(
                    point.getX(),// + random.nextInt(24), 
                    point.getY()// + random.nextInt(24)
            );
            path.getElements().add(line);
        }
        path.getElements().add(new MoveTo(goal.getX(), goal.getY()));
                    /*PathTransition transition = new PathTransition();
                    transition.setDuration(Duration.millis(500 * router.size()));
                    router.removeAll(router);

                    transition.setCycleCount(1);
                    transition.setNode(getImageView());
                    transition.setAutoReverse(false);
                    transition.setPath(path);
                    Platform.runLater(transition::play);*/
        return path;
    }
    
}
