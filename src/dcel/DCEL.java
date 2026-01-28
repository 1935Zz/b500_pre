package dcel;

import map.WorldPos;

import java.util.ArrayList;
import java.util.List;

public class DCEL {

    public final ArrayList<WorldPos> vertices = new ArrayList<>();
    public final ArrayList<ArrayList<Integer>> outgoing = new ArrayList<>();
    public final ArrayList<HalfEdge> edges = new ArrayList<>();
    public int nFaces = 0;
    public int exteriorFace = 0;

    public DCEL(float w, float h) {
        vertices.add(new WorldPos(0,0));
        vertices.add(new WorldPos(w,0));
        vertices.add(new WorldPos(0,h));
        outgoing.add(new ArrayList<>());
        outgoing.add(new ArrayList<>());
        outgoing.add(new ArrayList<>());

        nFaces = 2;
        int e1 = makeLinearEdge(0, 1, -1, -1, 1, 0);
        int e2 = makeLinearEdge(1, 2, -1, -1, 1, 0);
        int e3 = makeLinearEdge(2, 0, -1, -1, 1, 0);
        outgoing.get(0).add(e1);
        outgoing.get(1).add(e2);
        outgoing.get(2).add(e3);
        outgoing.get(0).add(e3+1);
        outgoing.get(1).add(e1+1);
        outgoing.get(2).add(e2+1);

        initOnlyLink(e1, e2);
        initOnlyLink(e2, e3);
        initOnlyLink(e3, e1);
        vertexOn(e2, new WorldPos(w,h));

        // Testing more edges etc
        int e = edgeBetween(0, 3);
        int v = vertexOn(e, new WorldPos(0.6f, 0.4f));
        edgeBetween(v, 1);
        v = vertexOn(e, new WorldPos(0.3f, 0.3f));
        e = edgeBetween(v, 2);

    }

    public DCEL(List<WorldPos> vs, List<List<Integer>> adjlist, int outerV1, int outerV2) {
        vertices.addAll(vs);
        if(adjlist.size() != vs.size()) {
            throw new IllegalArgumentException("Vertex list and adjlist should be the same size");
        }
        for (int v = 0; v < adjlist.size(); v++) {
            List<Integer> vAdjs = adjlist.get(v);
            ArrayList<Integer> es = new ArrayList<>();
            for (int vAdj : vAdjs) {
                edges.add(new HalfEdge(v, vAdj, -1, -1, -1, -1));
                es.add(edges.size()-1);
            }
            outgoing.add(es);
        }

        for (int eIdx = 0; eIdx < edges.size(); eIdx++) {
            HalfEdge e = edges.get(eIdx);
            if (e.twin != -1) {
                continue;
            }
            ArrayList<Integer> og = outgoing.get(e.to);
//            System.out.println(eIdx + " " + e + ": " + og.size() + " outgoing edges from v=" + e.to);
            for (int eIdxOut : og) {
                if (edges.get(eIdxOut).to == e.from) {
                    edges.set(eIdxOut, new HalfEdge(e.to, e.from, -1, -1, eIdx, -1));
                    edges.set(eIdx, new HalfEdge(e.from, e.to, -1, -1, eIdxOut, -1));
//                    System.out.println("\t\tTwinned " + edges.get(eIdxOut) + " to " + edges.get(eIdx));
                    break;
                }
            }
        }


        for (int eIdx = 0; eIdx < edges.size(); eIdx++) {
            HalfEdge e = edges.get(eIdx);
//            System.out.println("Starting from " + eIdx + ": " + e);
            if (e.next != -1) {
                continue;
            }
//            int prev = edges.get(nextEdge(e.from, vertices.get(e.to), false)).twin;
            int tempidx = nextEdge(e.from, vertices.get(e.to), false);
            int prev = edges.get(tempidx).twin;
//            System.out.println("\tFace + " + nFaces + ": Found next edge " + tempidx + ": " + edges.get(tempidx) + " with twin " + edges.get(prev));
            int curr = eIdx;
            int cnt = 0;
            while(prev != eIdx) {
                HalfEdge currE = edges.get(curr);
                if(currE.from == outerV1 && currE.to == outerV2) {
                    exteriorFace = nFaces;
                }
                HalfEdge prevE = edges.get(prev);
                edges.set(prev, prevE.linkNext(curr).reFace(nFaces));
                edges.set(curr, currE.linkPrev(prev).reFace(nFaces));
//                System.out.println("\tLinked " + prev + ": " + edges.get(prev) + " & " + curr + ": " + edges.get(curr));
                curr = prev;
                prev = edges.get(nextEdge(prevE.from, vertices.get(prevE.to), false)).twin;
                if(cnt++ > 1000) {
                    throw new RuntimeException("Looped too long linking adjlist edges");
                }
            }
            edges.set(prev, edges.get(prev).linkNext(curr).reFace(nFaces));
            edges.set(curr, edges.get(curr).linkPrev(prev).reFace(nFaces));
            nFaces += 1;
        }

    }

    public ArrayList<ArrayList<WorldPos>> convertToProvinces() {
        ArrayList<ArrayList<WorldPos>> a = new ArrayList<>();
        boolean[] seen = new boolean[nFaces];
        seen[exteriorFace] = true;

        for (int i = 0; i < edges.size(); i++) {
            HalfEdge e = edges.get(i);
            if(seen[e.face]) {
                continue;
            }
            seen[e.face] = true;
            ArrayList<WorldPos> pa = new ArrayList<>();
            pa.add(vertices.get(e.from));
            int next = e.prev;
            int j = 0;
            while(next != i) {
                HalfEdge e2 = edges.get(next);
                pa.add(vertices.get(e2.from));
                next = e2.prev;
                if(j++ > 100) {
                    throw new RuntimeException("Looped too long following DCEL pointers!");
                }
            }
            a.add(pa);
        }

        return a;
    }

    public ArrayList<Integer> computeFaceIDs() {
        ArrayList<Integer> a = new ArrayList<>();
        boolean[] seen = new boolean[nFaces];
        seen[exteriorFace] = true;

        for (int i = 0; i < edges.size(); i++) {
            HalfEdge e = edges.get(i);
            if(seen[e.face]) {
                continue;
            }
            a.add(e.face);
            seen[e.face] = true;
        }

        return a;
    }

    public ArrayList<ArrayList<Integer>> computeFaceAdjacencies() {
        ArrayList<ArrayList<Integer>> a = new ArrayList<>();
        for (int i = 0; i < nFaces; i++) {
            a.add(new ArrayList<>());
        }
        for (HalfEdge e : edges) {
            a.get(e.face).add(edges.get(e.twin).face);
        }

        return a;
    }

    public int vertexOn(int e, WorldPos pos) {
        int v = vertices.size();
        vertices.add(pos);

        HalfEdge old = edges.get(e);
        HalfEdge oldt = edges.get(old.twin);

        int e2 = edges.size();

        edges.add(new HalfEdge(v, old.to, old.next, e, e2+1, old.face));
        edges.add(new HalfEdge(old.to, v, old.twin, oldt.prev, e2, oldt.face));

        edges.set(e, new HalfEdge(old.from, v, e2, old.prev, old.twin, old.face));
        edges.set(old.twin, new HalfEdge(v, oldt.to, oldt.next, e2+1, e, oldt.face));

        edges.set(old.next, edges.get(old.next).linkPrev(e2));
        edges.set(oldt.prev, edges.get(oldt.prev).linkNext(e2+1));

        outgoing.add(new ArrayList<>());
        outgoing.get(v).add(e2);
        outgoing.get(v).add(old.twin);

        ArrayList<Integer> og = outgoing.get(old.to);
        for(int i = 0; i < og.size(); i++) {
            if(og.get(i) == old.twin) {
                og.set(i, e2+1);
            }
        }

        return v;
    }

    private int edgeBetween(int v1, int v2) {
        int e = edges.size();
        int adjFromIdx = nextEdge(v1, vertices.get(v2), true);
        HalfEdge adjFrom = edges.get(adjFromIdx);
        int adjToIdx = nextEdge(v2, vertices.get(v1), true);
        HalfEdge adjTo = edges.get(adjToIdx);

        edges.add(new HalfEdge(v1, v2, adjToIdx, adjFrom.prev, e+1, adjFrom.face));
        edges.add(new HalfEdge(v2, v1, adjFromIdx, adjTo.prev, e, nFaces));

        linkPrev(adjToIdx, e);
        linkNext(adjFrom.prev, e);

        linkPrev(adjFromIdx, e+1);
        linkNext(adjTo.prev, e+1);

        int next = edges.get(e+1).next;
        int j = 0;
        while(next != e+1) {
            HalfEdge theEdge = edges.get(next);
            edges.set(next, theEdge.reFace(nFaces));
            next = theEdge.next;
            if(j++ > 1000) {
                throw new RuntimeException("Looped too long following DCEL pointers!");
            }
        }
        nFaces++;

        return e;
    }

    private int nextEdge(int originVert, WorldPos rayEnd, boolean clockwise) {
        WorldPos fromPos = vertices.get(originVert);
        WorldPos finalPos = rayEnd;
        double edgeAngle = fromPos.angleTo(finalPos);
        double closest = 7;
        int adjFromIdx = 0;
        ArrayList<Integer> og = outgoing.get(originVert);
        for (int eIdxOut : og) {
            WorldPos toPos = vertices.get(edges.get(eIdxOut).to);
            double angle = fromPos.angleTo(toPos);
            double diff = angle - edgeAngle;
            if (clockwise) {
                diff = -diff;
            }
            if (diff < 0) {
                diff += Math.PI * 2;
            }
//            System.out.println("Edge with angle " + diff + " is " + eIdxOut + " " +  edges.get(eIdxOut));
            if (diff < closest && diff > 0) {
                closest = diff;
                adjFromIdx = eIdxOut;
            }
        }
        return adjFromIdx;
    }

    private void linkNext(int idx, int newNext) {
        edges.set(idx, edges.get(idx).linkNext(newNext));
    }


    private void linkPrev(int idx, int newPrev) {
        edges.set(idx, edges.get(idx).linkPrev(newPrev));
    }

    private int makeLinearEdge(int from, int to, int next, int prev, int lface, int rface) {
        int i = edges.size();
        edges.add(new HalfEdge(from, to, next, prev, i+1, lface));
        if(next >= 0) {
            edges.add(new HalfEdge(to, from, edges.get(prev).twin, edges.get(next).twin, i, rface));
        } else {
            edges.add(new HalfEdge(to, from, -1, -1, i, rface));
        }

        return i;
    }

    private void initOnlyLink(int e1, int e2) {
        int t1 = edges.get(e1).twin;
        int t2 = edges.get(e2).twin;
        edges.set(e1, edges.get(e1).linkNext(e2));
        edges.set(e2, edges.get(e2).linkPrev(e1));
        edges.set(t1, edges.get(t1).linkPrev(t2));
        edges.set(t2, edges.get(t2).linkNext(t1));
    }

    public record HalfEdge(int from, int to, int next, int prev, int twin, int face) {

        public HalfEdge linkNext(int newNext) {
            return new HalfEdge(from, to, newNext, prev, twin, face);
        }

        public HalfEdge linkPrev(int newPrev) {
            return new HalfEdge(from, to, next, newPrev, twin, face);
        }

        public HalfEdge reFace(int newFace) {
            return new HalfEdge(from, to, next, prev, twin, newFace);
        }

        @Override
        public String toString() {
            return "HalfEdge{" +
                    "from " + from +
                    " to " + to +
                    ", next " + next +
                    " prev " + prev +
                    ", twin " + twin +
                    ", face " + face +
                    '}';
        }
    }

}
