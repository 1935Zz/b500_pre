package map;

import java.util.List;

public class Province {
    private final List<float[]> vertices;
    private final List<Integer> triangulated;
    private final String name;
    private float[] colour;
    private TerrainType terrain;
    private boolean hasRoad = false;
    private boolean isEdge;

    public enum TerrainType {
        OPEN(1.0f, true),
        FOREST(3.0f, true),
        CITY(0.5f, false),
        RIVER(10.0f, false);

        private final float movementCost;
        private final boolean supportsCity;

        TerrainType(float movementCost, boolean supportsCity) {
            this.movementCost = movementCost;
            this.supportsCity = supportsCity;
        }

        // Whether a city can replace this terrain type
        public boolean canPlaceCityOn() {
            return supportsCity;
        }

        public float getMovementCost() {
            return movementCost;
        }
    }

    public Province(String name, List<float[]> vertices, float[] colour, boolean edge) {
        this.name = name;
        this.vertices = vertices;
        this.triangulated = Triangulator.triangulate(vertices);
        this.colour = colour;
        this.terrain = TerrainType.OPEN;
        this.hasRoad = false; // Initialize road flag
        this.isEdge = edge;
    }

    public TerrainType getTerrainType() {
        return terrain;
    }

    public void setTerrainType(TerrainType terrainType) {
        this.terrain = terrainType;
        // Automatically set colour based on terrain type for consistency
        if (terrainType == TerrainType.CITY) {
            setColour(new float[]{1.0f, 0.0f, 0.0f});  // Red for cities
        } else if (terrainType == TerrainType.FOREST) {
            setColour(new float[]{0.0f, 0.5f, 0.0f});  // Dark green for forests
        } else if (terrainType == TerrainType.RIVER) {
            setColour(new float[]{0.0f, 0.0f, 1.0f});  // Blue for rivers
        } else {
            setColour(new float[]{0.4f, 0.85f, 0.3f});  // Light green for open terrain
        }
    }

    public boolean hasRoad() {
        return hasRoad;
    }

    public void setHasRoad(boolean hasRoad) {
        this.hasRoad = hasRoad;
    }

    public List<float[]> getVertices() {
        return vertices;
    }

    public List<Integer> getTriangulatedVertices() {
        return triangulated;
    }

    public boolean isEdge() {
        return isEdge;
    }

    public String getName() {
        return name;
    }

    public float[] getColour() {
        return colour;
    }

    private void setColour(float[] colour) {
        this.colour = colour;
    }
}
