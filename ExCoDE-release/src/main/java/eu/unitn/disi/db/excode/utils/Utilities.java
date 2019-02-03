package eu.unitn.disi.db.excode.utils;

import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashObjSet;
import java.util.Collection;

/**
 *
 * @author bluecopper
 */
public class Utilities {
    
    public static int intersectionSize(HashIntSet s1, HashIntSet s2) {
        return (int) s1.stream().filter((n) -> (s2.contains(n))).count();
    }
    
    public static double jaccardSimilarity(HashIntSet first, HashIntSet second) {
        double intersec = intersectionSize(first, second);
        return intersec / (first.size() + second.size() - intersec);
    }
    
    public static double[] convertToDouble(String[] a) {
        double[] target = new double[a.length];
        int index = 0;
        for (String s : a) {
            target[index] = Double.parseDouble(s);
            index++;
        }
        return target;
    }
    
    public static void printComponent(Collection<Integer> edges) {
        StringBuilder builder = new StringBuilder().append("{ ");
        edges.stream().forEach((e) -> {
            builder.append(e).append(" ");
        });
        builder.append("}");
        System.out.println(builder.toString());
    }
    
    public static void printComponent(HashIntSet edges) {
        StringBuilder builder = new StringBuilder().append("{ ");
        edges.stream().forEach((e) -> {
            builder.append(e).append(" ");
        });
        builder.append("}");
        System.out.println(builder.toString());
    }
    
    public static void printComponent(HashObjSet edges) {
        StringBuilder builder = new StringBuilder().append("{ ");
        edges.stream().forEach((e) -> {
            builder.append(e).append(" ");
        });
        builder.append("}");
        System.out.println(builder.toString());
    }
    
    public static void printArrayPositive(double[] a) {
        StringBuilder builder = new StringBuilder().append("{ ");
        for (double d : a) {
            if (d > 0) {
                builder.append(d).append(" ");
            }
        }
        builder.append("}");
        System.out.println(builder.toString());
    }
    
}
