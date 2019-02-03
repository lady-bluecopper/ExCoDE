package eu.unitn.disi.db.excode.maxclique;

import com.google.common.collect.Lists;
import com.koloboke.collect.IntCursor;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import eu.unitn.disi.db.excode.graph.GPCorrelationGraph;
import eu.unitn.disi.db.excode.utils.Pair;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 *
 * @author bluecopper
 */
public class DegreeMap {
    
    private List<Integer> degrees;
    private HashIntObjMap<HashIntSet> nodeDegrees;
    
    public List<Integer> getDegrees() {
        return degrees;
    }
    
    public HashIntObjMap<HashIntSet> getDegreeMap() {
        return nodeDegrees;
    }
    
    public DegreeMap(GPCorrelationGraph graph, HashIntSet cands) {
        initialize(graph, cands);
    }
    
    private void addDeg(int degree) {
        if (degrees.isEmpty()) {
            degrees.add(degree);
            return;
        }
        if (degrees.get(degrees.size() - 1) < degree) {
            degrees.add(degree);
            return;
        }
        int index = 0;
        while (degrees.get(index) < degree) {
            index++;
        }
        degrees.add(index, degree);
    }
    
    public int getLowDegNode() {
        if (!degrees.isEmpty()) {
            IntCursor c = nodeDegrees.get(degrees.get(0)).cursor();
            c.moveNext();
            return c.elem();
        }
        return -1;
    }
    
    public int getHighDegNode() {
        if (!degrees.isEmpty()) {
            IntCursor c = nodeDegrees.get(degrees.get(degrees.size() - 1)).cursor();
            c.moveNext();
            return c.elem();
        }
        return -1;
    }
    
    public HashIntSet getNodesWithDeg(int deg) {
        return nodeDegrees.getOrDefault(deg, HashIntSets.newMutableSet());
    }
    
    public int removeLowDegNode() {
        if (!degrees.isEmpty()) {
            IntCursor it = nodeDegrees.get(degrees.get(0)).cursor();
            it.moveNext();
            int next = it.elem();
            it.remove();
            return next;
        }
        return -1;
    }
    
    public int removeHighDegNode() {
        if (!degrees.isEmpty()) {
            IntCursor it = nodeDegrees.get(degrees.get(degrees.size() - 1)).cursor();
            it.moveNext();
            int next = it.elem();
            it.remove();
            return next;
        }
        return -1;
    }    
    
    public final void initialize(GPCorrelationGraph graph, HashIntSet cands) {
        degrees = Lists.newArrayList();
        nodeDegrees = HashIntObjMaps.newMutableMap(cands.size());
        
        cands.stream().forEach((node) -> {
            int currentDeg = graph.getDegreeInSet(node, cands);
            HashIntSet current = nodeDegrees.getOrDefault(currentDeg, HashIntSets.newMutableSet(cands.size()));
            if (current.isEmpty()) {
                degrees.add(currentDeg);
            }
            current.add(node);
            nodeDegrees.put(currentDeg, current);
        });
        Collections.sort(degrees);
    }
    
    public void updateMap(HashIntSet neighsInCand) {
        List<Pair<Integer, HashIntSet>> toInsert = Lists.newArrayList();
        HashIntSet degreesToInsert = HashIntSets.newMutableSet(degrees.size());
        HashIntSet degreesToRemove = HashIntSets.newMutableSet(degrees.size());
        int cursor = 0;
        for (int i = degrees.size() - 1; i > -1; i --) {
            int currentDeg = degrees.get(i);
            if (cursor < toInsert.size()) {
                if (toInsert.get(cursor).getA().equals(currentDeg)) {
                    HashIntSet current = nodeDegrees.get(currentDeg);
                    current.addAll(toInsert.get(cursor).getB());
                    nodeDegrees.put(currentDeg, current);
                    cursor++;
                } else if (toInsert.get(cursor).getA() > currentDeg) {
                    while (cursor < toInsert.size()) {
                        Pair<Integer, HashIntSet> p = toInsert.get(cursor);
                        cursor++;
                        if (p.getA() > currentDeg) {
                            degreesToInsert.add(p.getA());
                            nodeDegrees.put(p.getA(), p.getB());
                        } else if (p.getA().equals(currentDeg)) {
                            HashIntSet current = nodeDegrees.get(currentDeg);
                            current.addAll(p.getB());
                            nodeDegrees.put(currentDeg, current);
                            break;
                        }
                    }
                }
            }
            HashIntSet changed = removeAndReturn(neighsInCand, nodeDegrees.get(currentDeg));
            if (!changed.isEmpty()) {
                toInsert.add(new Pair(currentDeg - 1, changed));
            }
            if (nodeDegrees.get(currentDeg).isEmpty()) {
                degreesToRemove.add(currentDeg);
                nodeDegrees.remove(currentDeg);
            }
        }
        while (cursor < toInsert.size()) {
            nodeDegrees.put(toInsert.get(cursor).getA(), toInsert.get(cursor).getB());
            degreesToInsert.add(toInsert.get(cursor).getA());
            cursor ++;
        }
        degreesToRemove.stream().forEach((deg) -> {
            degrees.remove(deg);
        });
        degreesToInsert.stream().forEach((deg) -> {
            addDeg(deg);
        });
    }
    
    private HashIntSet removeAndReturn(HashIntSet first, HashIntSet second) {
        HashIntSet intersection = HashIntSets.newMutableSet(first.size());
        first.stream().filter((e) -> (second.contains(e))).forEach((e) -> {
            intersection.add(e);
        });
        if (!intersection.isEmpty()) {
            first.removeAll(intersection);
            second.removeAll(intersection);
        }
        return intersection;
    }
}