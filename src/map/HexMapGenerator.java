package map;

import dcel.DCEL;

import java.util.ArrayList;
import java.util.Collections;

public class HexMapGenerator {
    
    private static final float IC30 = (float) (2f/Math.sqrt(3));
    
    final int width;
    final int height;
    final float r;
    final float d;
    
    public HexMapGenerator(int width, int height, float r) {
        this.width = width;
        this.height = height;
        this.r = r;
        this.d = r*2*IC30;
    }
    
    public DCEL generate() {
        ArrayList<WorldPos> vertices = new ArrayList<>();
        ArrayList<ArrayList<Integer>> adjList = new ArrayList<>();
        float s = (d-r)/2f;
        for(int i = 0; i < height; i++) {
            for(int j = 0; j < 2*width; j++) {
                float yoff = s;
                float rx = 0;
                float ry = 0;
                if(i == 0 || i == height-1 || (j + i) % 2 == 0) {
                    yoff = 0;
                }
                if(j != 0 && j != 2*width-1) {
                    rx = (float) (Math.random() - 0.5)*0.40f*r;
                }
                if(i != 0 && i != height-1) {
                    ry = (float) (Math.random() - 0.5)*0.40f*r;
                }
                vertices.add(new WorldPos(r*j + rx, (s + r)*i - yoff + ry));
                adjList.add(getAdjacencies(j, vertices, i));
            }
        }
        DCEL dcel = new DCEL(vertices, Collections.unmodifiableList(adjList), 1, 0);
        int ne = dcel.edges.size();
        for (int e = 0; e < ne; e++) {
            DCEL.HalfEdge he = dcel.edges.get(e);
            if(e > he.twin()) {
                continue;
            }
            if(onEdge(he.from()) && onEdge(he.to())) {
                continue;
            }
            float px1 = vertices.get(he.from()).x();
            float py1 = vertices.get(he.from()).y();
            float px2 = vertices.get(he.to()).x();
            float py2 = vertices.get(he.to()).y();
            float rnd = (float) (0.25f + (Math.random()*0.5f));
            float px = (float) (rnd*px1 + (1-rnd)*px2 + (Math.random() - 0.5)*r*0.4f);
            float py = (float) (rnd*py1 + (1-rnd)*py2 + (Math.random() - 0.5)*r*0.4f);
            dcel.vertexOn(e, new WorldPos(px, py));
        }
        return dcel;
    }

    private ArrayList<Integer> getAdjacencies(int j, ArrayList<WorldPos> vertices, int i) {
        ArrayList<Integer> a = new ArrayList<>();
        if(j == 0) {
            a.add(vertices.size());
            if(i > 0) {
                a.add(vertices.size()-1 - 2*width);
            }
            if(i < height-1) {
                a.add(vertices.size()-1 + 2*width);
            }
        } else if(j == 2*width-1) {
            a.add(vertices.size()-2);
            if(i > 0) {
                a.add(vertices.size()-1 - 2*width);
            }
            if(i < height-1) {
                a.add(vertices.size()-1 + 2*width);
            }
        } else {
            a.add(vertices.size()-2);
            a.add(vertices.size());
            if(i > 0 && (j + i) % 2 == 1) {
                a.add(vertices.size()-1 - 2*width);
            }

            if(i < height-1 && (j + i) % 2 == 0) {
                a.add(vertices.size()-1 + 2*width);
            }
        }
        return a;
    }

    private boolean onEdge(int v) {
        int i = v / (2*width);
        int j = v % (2*width);
        return (j == 0 || j == 2*width-1 || i == 0 || i == height-1);
    }
}
