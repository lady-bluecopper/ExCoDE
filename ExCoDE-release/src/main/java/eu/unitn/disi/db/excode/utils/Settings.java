package eu.unitn.disi.db.excode.utils;

/**
 *
 * @author bluecopper
 */
public class Settings {
    // Folders and Input Files
    public static String dataFolder = "";
    public static String outputFolder = "";
    public static String edgeFile = "";
    // Graph Properties
    public static boolean isWeighted = false;
    public static boolean isLabeled = false;
    // Correlation
    public static double minCor = 0.6;
    // Approximate
    public static boolean isExact = true;
    public static int numHashRuns = 2;
    public static int numHashFuncs = 5;
    // Density
    public static double minDen = 4;
    public static int minEdgesInSnap = 15;
    /*If true, use the MA density function; use AA otherwise*/
    public static boolean isMA = false;
    public static int maxCCSize = 100;
    // Diverse Subgraphs
    public static double maxJac;
}
