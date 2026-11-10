package cr0s.javara.render;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.perfomance.Profiler;
import cr0s.javara.render.EntityBlockingMap.SubCell;
import cr0s.javara.render.map.TileSet;
import cr0s.javara.util.Log;
import cr0s.javara.util.Pos;

public class EntityBlockingMap {
    private static final Log LOG = Log.of(EntityBlockingMap.class);

    private final World world;
    
    public enum SubCell { FULL_CELL, TOP_LEFT, TOP_RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_RIGHT, FULL_CELL_PASSABLE };
    public enum FillsSpace { ONE_OR_MORE_CELLS, ONE_CELL, ONE_SUBCELL, DONT_FILLS }
    public static int blockingMap[][];
    
    // Per-cell queue of entity influences for collision detection.
    // Separate from blockingMap (which stores terrain values) so that
    // multiple entities can be tracked per cell and terrain is preserved.
    private final ConcurrentLinkedQueue<Influence>[][] entityMap;

    public EntityBlockingMap(World w) {
        this.world = w;
        
        this.blockingMap = new int[w.getMap().getWidth()][w.getMap().getHeight()];
        
        this.entityMap = new ConcurrentLinkedQueue[w.getMap().getWidth()][w.getMap().getHeight()];
        for (int x = 0; x < w.getMap().getWidth(); x++) {
            for (int y = 0; y < w.getMap().getHeight(); y++) {
                entityMap[x][y] = new ConcurrentLinkedQueue<>();
            }
        }
    }
    
    private void clearMap() {
        for (int x = 0; x < world.getMap().getWidth(); x++) {
            for (int y = 0; y < world.getMap().getHeight(); y++) {
                if (entityMap[x][y] != null) {
                    entityMap[x][y].clear();
                }
                // Reset building occupation marker (-1) so cells can be re-occupied.
                // Terrain values (positive IDs) are preserved.
                if (blockingMap[x][y] == -1) {
                    blockingMap[x][y] = 0;
                }
            }
        }
    }
    
    public void update() {
        Profiler.getInstance().startForSection("World: blocking map clear");
        clearMap();
        Profiler.getInstance().stopForSection("World: blocking map clear");
    }
    
    public void occupyForMobileEntity(MobileEntity me) {
        switch (me.fillsSpace) {
        case ONE_CELL:
            occupyCell(me.getCellPos(), me);
            if (me.isMovingToCell) {
                occupyCell(me.targetCell, me);
            }
            break;
        case DONT_FILLS:
            break;
        case ONE_OR_MORE_CELLS:
            // TODO:
            break;
        case ONE_SUBCELL:
            occupySubCell(me.getCellPos(), me.currentSubcell, me);
            if (me.isMovingToCell) {
                occupySubCell(me.targetCell, me.desiredSubcell, me);
            }
            break;
        default:
            break;
        }
    }
    
    public void freeForMobileEntity(MobileEntity me) {
        switch (me.fillsSpace) {
        case ONE_CELL:
            freeCell(me.getCellPos());

            if (me.isMovingToCell) {
                freeCell(me.targetCell);
            }
            break;
        case DONT_FILLS:
            break;
        case ONE_OR_MORE_CELLS:
            // TODO:
            break;
        case ONE_SUBCELL:
            freeSubCell(me.getCellPos(), me.currentSubcell);
            if (me.isMovingToCell) {
                freeSubCell(me.targetCell, me.desiredSubcell);
            }
            break;
        default:
            break;
        }
    }    
    
    public void freeCell(Pos cellPos) {
        freeSubCell(cellPos, SubCell.FULL_CELL);
    }
    
    public void freeSubCell(Pos cellPos, SubCell sc) {
        int cx = cellPos.getCellX();
        int cy = cellPos.getCellY();
        
        if (cx < 0 || cx >= entityMap.length || cy < 0 || cy >= entityMap[0].length) {
            return;
        }
        
        if (sc == SubCell.FULL_CELL) {
            entityMap[cx][cy].clear();
            this.blockingMap[cx][cy] = 0;
            return;
        }
        
        ConcurrentLinkedQueue<Influence> infList = entityMap[cx][cy];
        Influence toRemove = null;
        for (Influence inf : infList) {
            if (inf.subcell == sc) {
                toRemove = inf;
                break;
            }
        }
        if (toRemove != null) {
            infList.remove(toRemove);
        }
        
        // If no more influences and no terrain blocking, clear the cell
        if (infList.isEmpty()) {
            this.blockingMap[cx][cy] = 0;
        }
    }
    
    public void occupySubCell(Pos cellPos, SubCell sc, Entity e) {
        if (sc == SubCell.FULL_CELL) {
            occupyCell(cellPos, e);
            return;
        }
        
        int cx = cellPos.getCellX();
        int cy = cellPos.getCellY();
        
        if (cx < 0 || cx >= entityMap.length || cy < 0 || cy >= entityMap[0].length) {
            return;
        }
        
        entityMap[cx][cy].add(new Influence(sc, e));
    }    
    
    public void occupyCell(Pos cellPos, Entity e) {
        int cx = cellPos.getCellX();
        int cy = cellPos.getCellY();
        
        if (cx < 0 || cx >= entityMap.length || cy < 0 || cy >= entityMap[0].length) {
            return;
        }
        
        this.blockingMap[cx][cy] = -1;
        entityMap[cx][cy].add(new Influence(SubCell.FULL_CELL, e));
    }
    
    public boolean isSubcellFree(Pos pos, SubCell sub) {
        if (sub == null) {
            LOG.error("Null subcell");
            return false;
        }
        
        int cx = pos.getCellX();
        int cy = pos.getCellY();
        
        if (cx < 0 || cx >= entityMap.length || cy < 0 || cy >= entityMap[0].length) {
            return false;
        }
        
        // If full cell is occupied by any entity, nothing is free
        if (isFullCellOccupied(pos)) {
            return false;
        }
        
        ConcurrentLinkedQueue<Influence> infList = entityMap[cx][cy];
        
        // Check if any influence occupies this subcell or FULL_CELL
        if (!infList.isEmpty()) {
            for (Influence cellInf : infList) {
                if (cellInf.subcell == sub || cellInf.subcell == SubCell.FULL_CELL) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public boolean isFullCellOccupied(Pos pos) {
        int cx = pos.getCellX();
        int cy = pos.getCellY();
        
        if (cx < 0 || cx >= entityMap.length || cy < 0 || cy >= entityMap[0].length) {
            return false;
        }
        
        ConcurrentLinkedQueue<Influence> infList = entityMap[cx][cy];
        for (Influence cellInf : infList) {
            if (cellInf.subcell == SubCell.FULL_CELL) {
                return true;
            }
        }
        
        return false;
    }    
    
    public boolean isAnyInfluenceInCell(Pos pos) {
        int cx = pos.getCellX();
        int cy = pos.getCellY();
        
        if (cx < 0 || cx >= entityMap.length || cy < 0 || cy >= entityMap[0].length) {
            return false;
        }
        
        return !entityMap[cx][cy].isEmpty();
    }
    
    public boolean isEntityInCell(Pos pos, Entity e) {
        if (!isAnyInfluenceInCell(pos)) {
            return false;
        }
        
        int cx = pos.getCellX();
        int cy = pos.getCellY();
        
        ConcurrentLinkedQueue<Influence> infList = entityMap[cx][cy];
        for (Influence cellInf : infList) {
            if (cellInf.entity == e) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns true if any entity except building is have influence to specified cell
     * @param cellPos cell position
     * @return result
     */
    public boolean isAnyUnitInCell(Pos cellPos) {
        if (!isAnyInfluenceInCell(cellPos)) {
            return false;
        }
        
        int cx = cellPos.getCellX();
        int cy = cellPos.getCellY();
        
        ConcurrentLinkedQueue<Influence> infList = entityMap[cx][cy];
        for (Influence inf : infList) {
            if (inf.entity != null && !(inf.entity instanceof EntityBuilding)) {
                return true;
            }
        }
        
        return false;
    }
    
    public int getCellInfluences(Pos pos) {
        return blockingMap[pos.getCellX()][pos.getCellY()];
    }
    
    public ConcurrentLinkedQueue<Influence> getEntityInfluences(Pos pos) {
        int cx = pos.getCellX();
        int cy = pos.getCellY();
        
        if (cx < 0 || cx >= entityMap.length || cy < 0 || cy >= entityMap[0].length) {
            return null;
        }
        
        return entityMap[cx][cy];
    }

    public void occupyForBuilding(EntityBuilding eb) {
        for (int by = 0; by < eb.getHeightInTiles(); by++) {
            for (int bx = 0; bx < eb.getWidthInTiles(); bx++) {
                int x = (((int)eb.boundingBox.getX() + 12) / 24) + bx;
                int y = (((int)eb.boundingBox.getY() + 12) / 24) + by;
                int blockType = eb.getBlockingCells()[bx][by];
                
                switch (blockType) {
                case TileSet.SURFACE_BUILDING:
                    occupySubCell(new Pos(x, y), SubCell.FULL_CELL, eb);
                    break;

                case TileSet.SURFACE_BUILDING_CLEAR_ID:
                //case TileSet.SURFACE_CLEAR_ID:
                    occupySubCell(new Pos(x, y), SubCell.FULL_CELL_PASSABLE, eb);
                    break;
                }
            }
        }
    }
    
    public SubCell getFreeSubCell(Pos pos, SubCell currentSubcell) {
        if (this.isSubcellFree(pos, currentSubcell)) {
            return currentSubcell;
        }
        
        if (isFullCellOccupied(pos)) {
            return null;
        }
        
        for (SubCell sc : SubCell.values()) {
            if (sc == SubCell.FULL_CELL || sc == SubCell.FULL_CELL_PASSABLE) {
                continue;
            }
            
            if (this.isSubcellFree(pos, sc)) {
                return sc;
            }
        }
        
        return null;
    }

    public class Influence {
        public SubCell subcell;
        public Entity entity;
        
        public Influence(SubCell sc, Entity e) {
            this.subcell = sc;
            this.entity = e;
        }
    }
}
