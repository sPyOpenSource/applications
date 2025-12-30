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
import java.util.ArrayList;
import java.util.Random;

import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;

import mazesolver.Director;
import mazesolver.Maze;
import mazesolver.Point;

/**
 *
 * @author xuyi
 */
class AStarPathFinder {
    private final Maze maze;
    private final Random random = new Random();
    private final int max;

    private ArrayList<Point> router;
    private Director director;
    
    AStarPathFinder(World world, int MAX_SEARCH_DISTANCE) {
        maze = new Maze(world.blockingEntityMap.blockingMap, null);
        max = MAX_SEARCH_DISTANCE;
    }

    Path findPath(MobileEntity me, int startX, int startY, int goalX, int goalY) {
        maze.setEnd(new Point(goalX, goalY));
        maze.setStart(new Point(startX, startY));
        director = new Director(maze, null);
        director.run();
        router = director.getBestRoute();
        Path path = new Path();
        MoveTo mv = new MoveTo(
                router.getFirst().getX() * 24 + random.nextInt(24), 
                router.getFirst().getY() * 24 + random.nextInt(24));
        path.getElements().add(mv);
        for(int i = 1; i < router.size(); i++){
            LineTo line = new LineTo(
                    router.get(i).getX() * 24 + random.nextInt(24), 
                    router.get(i).getY() * 24 + random.nextInt(24));
            path.getElements().add(line);
        }
                    /*PathTransition transition = new PathTransition();
                    transition.setDuration(Duration.millis(500 * router.size()));
                    router.removeAll(router);

                    transition.setCycleCount(1);
                    transition.setNode(getImageView());
                    transition.setAutoReverse(false);
                    transition.setPath(path);
                    Platform.runLater(transition::play);
        MoveTo moveTo = new MoveTo(me.boundingBox.getX(), me.boundingBox.getY());
        LineTo lineTo = new LineTo(goalX, goalY);
        Path path = new Path();
        path.getElements().addAll(moveTo, lineTo);*/
        return path;
    }
    
}
