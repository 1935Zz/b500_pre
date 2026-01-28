package map;

import java.util.ArrayList;
import java.util.List;

public class Triangulator {
    
    public static List<Integer> triangulate(List<float[]> vertices) {
        List<Integer> indices = new ArrayList<>();
        List<Integer> vertexIndices = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            vertexIndices.add(i);
        }

        while (vertexIndices.size() > 3) {
            boolean earFound = false;

            for (int i = 0; i < vertexIndices.size(); i++) {
                int prev = vertexIndices.get((i + vertexIndices.size() - 1) % vertexIndices.size());
                int curr = vertexIndices.get(i);
                int next = vertexIndices.get((i + 1) % vertexIndices.size());

                float[] a = vertices.get(prev);
                float[] b = vertices.get(curr);
                float[] c = vertices.get(next);

                if (isConvex(a, b, c)) {
                    boolean isEar = true;
                    for (int j = 0; j < vertices.size(); j++) {
                        if (j == prev || j == curr || j == next) continue;
                        if (pointInTriangle(vertices.get(j), a, b, c)) {
                            isEar = false;
                            break;
                        }
                    }

                    if (isEar) {
                        indices.add(prev);
                        indices.add(curr);
                        indices.add(next);
                        vertexIndices.remove(i);
                        earFound = true;
                        break;
                    }
                }
            }

            if (!earFound) {
                for (int i = 0; i < vertices.size(); i++) {
                    System.out.println(vertices.get(i)[0] + ", " + vertices.get(i)[1]);
                }

                throw new RuntimeException("Polygon is not simple or has colinear points.");
            }
        }

        indices.add(vertexIndices.get(0));
        indices.add(vertexIndices.get(1));
        indices.add(vertexIndices.get(2));

        return indices;
    }

    private static boolean isConvex(float[] a, float[] b, float[] c) {
        return ((b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])) < 0;
    }

    private static boolean pointInTriangle(float[] p, float[] a, float[] b, float[] c) {
        float d1 = sign(p, a, b);
        float d2 = sign(p, b, c);
        float d3 = sign(p, c, a);

        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);

        return !(hasNeg && hasPos);
    }

    private static float sign(float[] p1, float[] p2, float[] p3) {
        return (p1[0] - p3[0]) * (p2[1] - p3[1]) - (p2[0] - p3[0]) * (p1[1] - p3[1]);
    }
}
