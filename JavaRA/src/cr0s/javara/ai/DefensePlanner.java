package cr0s.javara.ai;

import java.util.ArrayList;
import java.util.List;

import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.building.Defensive;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.building.soviet.EntityFireTurret;
import cr0s.javara.entity.building.soviet.EntityTeslaCoil;
import cr0s.javara.render.World;
import cr0s.javara.render.map.TileSet;
import cr0s.javara.util.CellChooser;
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

    public static double threatMultiplier(Pos cell, Pos base, Pos threat) {
        if (base == null || threat == null || base.equals(threat)) {
            return 1.0;
        }

        if (cell.distanceTo(threat) <= 6 * 24.0) {
            return 2.0;
        }

        double baseX = base.getX();
        double baseY = base.getY();
        double threatX = threat.getX() - baseX;
        double threatY = threat.getY() - baseY;
        double cellX = cell.getX() - baseX;
        double cellY = cell.getY() - baseY;

        return threatX * cellX + threatY * cellY > 0 ? 1.5 : 1.0;
    }

    public static double spacingMultiplier(Pos candidate, List<Pos> existingCells, int minSpacing) {
        if (existingCells == null || existingCells.isEmpty()) {
            return 1.0;
        }

        double nearest = Double.MAX_VALUE;
        for (Pos existing : existingCells) {
            nearest = Math.min(nearest, candidate.distanceTo(existing) / 24.0);
        }

        if (nearest < minSpacing) {
            return 0.0;
        }
        if (nearest < minSpacing + 2) {
            return 0.6;
        }
        if (nearest >= 4 && nearest <= 8) {
            return 1.05;
        }
        return 1.0;
    }

    public static double roleMultiplier(Defensive prototype, int overlapLayers, boolean coversChokepoint) {
        double multiplier = 1.0;
        if (prototype instanceof EntityTeslaCoil) {
            multiplier *= 1.25;
            if (coversChokepoint) {
                multiplier *= 1.1;
            }
        } else if (prototype instanceof EntityFireTurret && overlapLayers > 0) {
            multiplier *= 1.15;
        }
        return multiplier;
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

    public Pos chooseCell(String actorType) {
        EntityBuilding prototype = resolvePrototype(actorType);
        if (prototype instanceof Defensive defense) {
            return chooseCell(defense);
        }
        return null;
    }

    public Pos chooseCell(Defensive prototype) {
        if (prototype == null || prototype.getRange() <= 0) {
            return null;
        }

        Pos base = this.ai.getPlayerSpawnPoint();
        if (base == null) {
            return null;
        }

        Pos threat = chooseThreatCenter(base);
        ArrayList<ApproachCell> approach = getApproachMap();

        ArrayList<Defensive> existingDefenses = new ArrayList<>();
        ArrayList<Pos> existingCells = new ArrayList<>();
        for (EntityBuilding building : this.ai.getBase().getBuildings()) {
            if (building instanceof Defensive defense && !defense.isDead()) {
                existingDefenses.add(defense);
                // EntityBuilding has no getCellX(); getPosition() adds the
                // building centre offset, so use the top-left bounding box.
                existingCells.add(new Pos(defense.boundingBox.getX(), defense.boundingBox.getY()));
            }
        }

        EntityBuilding buildingPrototype = (EntityBuilding) prototype;
        ArrayList<Pos> candidates = this.ai.world.chooseTilesInAnnulus(
                base,
                this.ai.defenseMinRadius,
                this.ai.defenseMaxRadius,
                new CellChooser() {
                    @Override
                    public boolean isCellChoosable(Pos cell) {
                        return ai.getBase().isPossibleToBuildHere(cell, buildingPrototype)
                                && ai.getBase().checkBuildingDistance(cell, false)
                                && countPassableNeighbours(cell) >= 3;
                    }
                });

        Pos best = null;
        double bestScore = 0.0;
        double range = prototype.getRange() * 24.0;

        for (Pos candidate : candidates) {
            double spacing = spacingMultiplier(candidate, existingCells, this.ai.defenseMinSpacing);
            if (spacing <= 0.0) {
                continue;
            }

            double marginal = 0.0;
            int overlapLayers = 0;
            boolean coversChokepoint = false;

            for (ApproachCell approachCell : approach) {
                if (approachCell.getCell().distanceTo(candidate) > range) {
                    continue;
                }

                int layers = 0;
                for (Defensive defense : existingDefenses) {
                    double defenseRange = defense.getRange() * 24.0;
                    if (approachCell.getCell().distanceTo(
                            new Pos(defense.boundingBox.getX(), defense.boundingBox.getY())) <= defenseRange) {
                        layers++;
                    }
                }

                if (layers >= this.ai.defenseOverlapTarget) {
                    continue;
                }

                double weight = approachCell.getWeight() * threatMultiplier(approachCell.getCell(), base, threat);
                marginal += (layers == 0) ? weight : weight * 0.5;
                overlapLayers = Math.max(overlapLayers, layers);
                if (approachCell.getPassableNeighbours() <= 5) {
                    coversChokepoint = true;
                }
            }

            if (marginal <= 0.0) {
                continue;
            }

            double score = marginal * roleMultiplier(prototype, overlapLayers, coversChokepoint);

            if (!base.equals(threat)) {
                double threatX = threat.getX() - base.getX();
                double threatY = threat.getY() - base.getY();
                double candidateX = candidate.getX() - base.getX();
                double candidateY = candidate.getY() - base.getY();
                if (threatX * candidateX + threatY * candidateY > 0) {
                    score *= 1.10;
                }
            }

            score *= spacing;

            double baseDistance = candidate.distanceTo(base) / 24.0;
            if (baseDistance >= 6 && baseDistance <= 12) {
                score *= 1.05;
            }

            if (score > bestScore + 0.000001) {
                best = candidate;
                bestScore = score;
            } else if (Math.abs(score - bestScore) <= 0.000001 && World.random.nextBoolean()) {
                best = candidate;
            }
        }

        return best;
    }

    private EntityBuilding resolvePrototype(String actorType) {
        if (actorType == null) {
            return null;
        }
        EntityActor actor = this.ai.getBase().getProductionQueue().getBuildables()
                .get(actorType + "icon.shp");
        return (actor instanceof EntityBuilding building) ? building : null;
    }

    private Pos chooseThreatCenter(Pos base) {
        Pos defenseCenter = this.ai.getDefenseCenter();
        if (defenseCenter != null && !defenseCenter.equals(base)) {
            return defenseCenter;
        }

        ArrayList<EntityActor> enemies = this.ai.findClosestEnemy(base);
        if (!enemies.isEmpty()) {
            Pos enemyCell = enemies.get(0).getCellPosition();
            if (enemyCell.distanceTo(base) <= this.ai.defenseThreatRadius * 24.0) {
                return enemyCell;
            }
        }

        return base;
    }
}
