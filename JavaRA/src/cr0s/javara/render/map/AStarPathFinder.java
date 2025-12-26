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
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;

/**
 *
 * @author xuyi
 */
class AStarPathFinder {

    AStarPathFinder(World world, int MAX_SEARCH_DISTANCE, boolean b) {
    }

    Path findPath(MobileEntity me, int startX, int startY, int goalX, int goalY) {
        MoveTo moveTo = new MoveTo(me.boundingBox.getX(), me.boundingBox.getY());
        LineTo lineTo = new LineTo(goalX, goalY);
        Path path = new Path();
        path.getElements().addAll(moveTo, lineTo);
        return path;
    }
    
}
