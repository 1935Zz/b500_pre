import dcel.DCEL;
import map.HexMapGenerator;
import map.Province;
import map.ProvinceMap;
import map.WorldPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapGenerator {

    private static void adjlink(List<List<Integer>> adjlist, int v1, int v2) {
        adjlist.get(v1).add(v2);
        adjlist.get(v2).add(v1);
    }

    // Testing method for DCEL
    public static ProvinceMap generateTestAdjListDCELMap() {
        List<WorldPos> verts = new ArrayList<>();
        List<List<Integer>> adjlist = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                float rx = (float) (Math.random()*0.10f);
                float ry = (float) (Math.random()*0.10f);
                verts.add(new WorldPos(0.1f + 0.25f*i + rx, 0.1f + 0.25f*j + ry));
                adjlist.add(new ArrayList<>());
            }
        }
        adjlink(adjlist, 0, 1);
        adjlink(adjlist, 1, 2);
        adjlink(adjlist, 2, 5);
        adjlink(adjlist, 5, 8);
        adjlink(adjlist, 8, 7);
        adjlink(adjlist, 6, 7);
        adjlink(adjlist, 6, 3);
        adjlink(adjlist, 3, 0);
        adjlink(adjlist, 1, 4);
        adjlink(adjlist, 7, 4);
        adjlink(adjlist, 7, 3);
        DCEL dcel = new DCEL(verts, adjlist, 0, 1);
        return convertDCELMap(dcel);
    }

    // Generate a map with perturbed hexagonal cells
    public static ProvinceMap generateHexMap() {
        DCEL dcel = new HexMapGenerator(40,50, 0.012f).generate();
        return convertDCELMap(dcel);
    }

    // Get a ProvinceMap from a DCEL
    public static ProvinceMap convertDCELMap(DCEL dcel) {
        List<Province> provinces = new ArrayList<>();
        List<List<Integer>> adjacencyList = new ArrayList<>();

        ArrayList<ArrayList<WorldPos>> provinceVertList = dcel.convertToProvinces();
        ArrayList<Integer> faceIds = dcel.computeFaceIDs();
        ArrayList<ArrayList<Integer>> adjs = dcel.computeFaceAdjacencies();

        for (int i = 0; i < provinceVertList.size(); i++) {
            int faceId = faceIds.get(i);
            ArrayList<Integer> adjacentFaces = adjs.get(faceId);
            boolean onEdge = false;

            // Convert face adjacencies to province indices
            List<Integer> adjacentProvinceIndices = new ArrayList<>();
            for (Integer adjFace : adjacentFaces) {
                // Skip exterior face, but mark as edge
                if (adjFace == dcel.exteriorFace) {
                    onEdge = true;
                    continue;
                }

                // Find the province index for this face
                int adjProvinceIndex = faceIds.indexOf(adjFace);
                if (adjProvinceIndex != -1) {
                    adjacentProvinceIndices.add(adjProvinceIndex);
                }
            }
            adjacencyList.add(adjacentProvinceIndices);

            ArrayList<float[]> vertices = new ArrayList<>();
            for (WorldPos v : provinceVertList.get(i)) {
                vertices.add(new float[] {v.x()*2 - 0.95f, v.y()*2 - 0.95f});
            }
            float r = (float) Math.random();
            float g = (float) Math.random();
            float b = (float) Math.random();
            provinces.add(new Province("Province " + i, vertices, new float[] {r, g, b}, onEdge));
        }

        ProvinceMap provinceMap = new ProvinceMap(provinces, adjacencyList);
        return provinceMap;
    }

    public static void show(List<Province> provinces) {
        for (Province province : provinces) {
            System.out.println("Province: " + province.getName());
            for (float[] vertex : province.getVertices()) {
                System.out.println("Vertex: (" + vertex[0] + ", " + vertex[1] + ")");
            }
        }
    }
}