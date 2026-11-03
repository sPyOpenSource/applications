package cr0s.javara.ai;

import java.util.ArrayList;

import cr0s.javara.render.map.TileSet;
import cr0s.javara.util.Pos;

public class DefensePlanner {
    public static final class ApproachCell {
        private final Pos cell;
        private final int passableNeighbours;

        public ApproachCell(Pos cell, int passableNeighbours) {
            this.cell = cell;
            this.passableNeighbours = passableNeighbours;
        }

        public Pos getCell() {
            return this.cell;
        }

        public int getPassableNeighbours() {
            return this.passableNeighbours;
        }

        public double getWeight() {
            return chokepointFactor(this.passableNeighbours);
        }
    }

    private final AIPlayer ai;
    private ArrayList<ApproachCell> approachMap;
    private Pos cachedBase;

    public DefensePlanner(AIPlayer ai) {
        this.ai = ai;
    }

    public static double chokepointFactor(int passableNeighbours) {
        if (passableNeighbours <= 3) {
            return 3.0;
        }
        if (passableNeighbours <= 5) {
            return 2.0;
        }
        return 1.0;
    }

    public ArrayList<ApproachCell> getApproachMap() {
        Pos base = this.ai.getPlayerSpawnPoint();
        if (base == null) {
            return new ArrayList<>();
        }
        if (this.approachMap == null || this.cachedBase == null
                || !this.cachedBase.equals(base)) {
            rebuildApproachMap(base);
        }
        return this.approachMap;
    }

    private void rebuildApproachMap(Pos base) {
        ArrayList<ApproachCell> cells = new ArrayList<>();
        int minRange = Math.max(1, this.ai.defenseMinRadius - 2);
        int maxRange = this.ai.defenseMaxRadius + 8;

        for (Pos cell : this.ai.world.chooseTilesInAnnulus(base, minRange, maxRange)) {
            if (!isTerrainPassable(cell)) {
                continue;
            }
            cells.add(new ApproachCell(cell, countPassableNeighbours(cell)));
        }

        this.approachMap = cells;
        this.cachedBase = base;
    }

    public boolean isTerrainPassable(Pos cell) {
        if (!this.ai.world.getMap().isInMap(cell.getX(), cell.getY())) {
            return false;
        }
        int surfaceId = this.ai.world.getMap().getSurfaceIdAt(cell);
        return surfaceId != TileSet.SURFACE_WATER_ID
                && surfaceId != TileSet.SURFACE_RIVER_ID
                && surfaceId != TileSet.SURFACE_ROCK_ID;
    }

    public int countPassableNeighbours(Pos cell) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                if (isTerrainPassable(new Pos(cell.getCellX() + dx, cell.getCellY() + dy))) {
                    count++;
                }
            }
        }
        return count;
    }
}
