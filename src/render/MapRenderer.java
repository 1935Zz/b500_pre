package render;

import map.Province;
import map.ProvinceMap;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class MapRenderer {
    private Shader shader;
    private int vao;
    private int vbo;
    private int ebo;

    public void init(ResourceManager rm) {
        shader = rm.loadShader("edge", "src/mapvert.glsl", "src/mapfrag.glsl");

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // Configure vertex attributes
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        ebo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    // For drawing lines over the map
    private void beginLineOverlayDrawing(float[] colour, float thickness) {
        shader.use();
        glBindVertexArray(vao);

        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(thickness);  // Thicker lines for visibility
        glEnable(GL_POLYGON_OFFSET_LINE);
        glPolygonOffset(-2.0f, -2.0f);  // Slightly more offset to ensure visibility over map

        shader.createUniform3f("color", colour);
    }

    // Call between beginLineOverlayDrawing and endLineOverlayDrawing
    private void drawLineBetweenProvinces(List<Province> path) {
        if (path.size() < 2) return;

        // Build vertices: Connect centroids of consecutive provinces in the path
        FloatBuffer lineVertices = BufferUtils.createFloatBuffer((path.size() - 1) * 2 * 2);  // x,y per point, 2 points per segment
        for (int i = 0; i < path.size() - 1; i++) {
            float[] startCentroid = calculateCentroid(path.get(i).getVertices());
            float[] endCentroid = calculateCentroid(path.get(i + 1).getVertices());
            lineVertices.put(startCentroid[0]).put(startCentroid[1]);
            lineVertices.put(endCentroid[0]).put(endCentroid[1]);
        }
        lineVertices.flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, lineVertices, GL_DYNAMIC_DRAW);

        // Draw lines for this path
        glDrawArrays(GL_LINES, 0, (path.size() - 1) * 2);
    }

    // Restore OpenGL state
    private void endLineOverlayDrawing() {
        glDisable(GL_POLYGON_OFFSET_LINE);
        glLineWidth(1.0f);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    // Renders roads as white lines between province centroids
    public void renderRoads(ProvinceMap map) {
        List<Province> provinces = map.getProvinces();
        List<Integer> roadProvinces = new ArrayList<>();
        for (int i = 0; i < provinces.size(); i++) {
            if (provinces.get(i).hasRoad()) {
                roadProvinces.add(i);
            }
        }

        if (roadProvinces.isEmpty()) return;

        // Set line as white
        beginLineOverlayDrawing(new float[]{1.0f, 1.0f, 1.0f}, 4.0f);

        // For each road province, draw lines to adjacent road provinces
        for (int p : roadProvinces) {
            for (int adj : map.getAdjacentProvinces(p)) {
                Province adjProvince = provinces.get(adj);
                if(adjProvince.hasRoad()) {
                    drawLineBetweenProvinces(List.of(provinces.get(p), adjProvince));
                }
            }
        }

        endLineOverlayDrawing();
    }

    // Renders a list of provinces
    public void renderMap(ProvinceMap map) {
        if (map == null) {
            return;
        }
        List<Province> provinces = map.getProvinces();
        if (provinces.isEmpty()) {
            return;
        }

        shader.use();
        glBindVertexArray(vao);

        // PASS 1: Render Filled Provinces
        for (Province province : provinces) {
            List<float[]> rawVertices = province.getVertices();
            List<Integer> indices = province.getTriangulatedVertices();

            // Flatten the vertices into a float array
            float[] vertices = new float[rawVertices.size() * 2];
            for (int i = 0, j = 0; i < rawVertices.size(); i++) {
                vertices[j++] = rawVertices.get(i)[0];
                vertices[j++] = rawVertices.get(i)[1];
            }

            IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.size());
            for (int index : indices) {
                indexBuffer.put(index);
            }
            indexBuffer.flip();

            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_DYNAMIC_DRAW);

            shader.createUniform3f("color", province.getColour());

            glDrawElements(GL_TRIANGLES, indices.size(), GL_UNSIGNED_INT, 0);
        }

        // PASS 2: Render Province Borders
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(2.0f);

        // Enable polygon offset to push lines slightly forward, avoiding z-fighting
        glEnable(GL_POLYGON_OFFSET_LINE);
        glPolygonOffset(-1.0f, -1.0f); // Factor and units, negative values push "towards" the camera

        shader.createUniform3f("color", new float[]{0.0f, 0.0f, 0.0f});

        for (Province p : provinces) {
            float[] vertices = new float[p.getVertices().size() * 2];
            int index = 0;
            for (float[] vertex : p.getVertices()) {
                vertices[index++] = vertex[0];
                vertices[index++] = vertex[1];
            }

            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

            glDrawArrays(GL_POLYGON, 0, vertices.length / 2);
        }

        // Render roads after provinces and borders
        renderRoads(map);

        glDisable(GL_POLYGON_OFFSET_LINE);
        glLineWidth(1.0f); // Reset to default line width
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        glBindVertexArray(0);
    }

    // Renders a white line through the centroids of provinces in the path
    public void renderPath(List<Province> path) {
        if (path.isEmpty()) return;

        beginLineOverlayDrawing(new float[]{1.0f, 1.0f, 1.0f}, 4.0f);
        drawLineBetweenProvinces(path);
        endLineOverlayDrawing();
    }

    // Helper method: Calculates the centroid (average position) of a province's vertices
    private float[] calculateCentroid(List<float[]> vertices) {
        float xSum = 0, ySum = 0;
        for (float[] vertex : vertices) {
            xSum += vertex[0];
            ySum += vertex[1];
        }
        int count = vertices.size();
        return new float[]{xSum / count, ySum / count};
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
