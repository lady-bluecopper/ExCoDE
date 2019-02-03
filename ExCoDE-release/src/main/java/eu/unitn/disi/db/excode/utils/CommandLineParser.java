package eu.unitn.disi.db.excode.utils;

/**
 *
 * @author bluecopper
 */
public class CommandLineParser {

    public static void parse(String[] args) {

        if (args != null && args.length > 0) {
            parseArgs(args);
        }
    }

    private static void parseArgs(String[] args) {
        for (String arg : args) {
            String[] parts = arg.split("=");
            parseArg(parts[0], parts[1]);
        }
    }

    private static void parseArg(String key, String value) {
        if (key.compareTo("isWeighted") == 0) {
            Settings.isWeighted = (value.compareTo("true") == 0);
        } else if (key.compareTo("isLabeled") == 0) {
            Settings.isLabeled = (value.compareTo("true") == 0);
        } else if (key.compareTo("minCor") == 0) {
            Settings.minCor = Double.parseDouble(value);
        } else if (key.compareTo("minDen") == 0) {
            Settings.minDen = Double.parseDouble(value);
        } else if (key.compareTo("dataFolder") == 0) {
            Settings.dataFolder = value;
        } else if (key.compareTo("edgeFile") == 0) {
            Settings.edgeFile = value;
        } else if (key.compareTo("outputFolder") == 0) {
            Settings.outputFolder = value;
        } else if (key.compareTo("isMA") == 0) {
            Settings.isMA = (value.compareTo("true") == 0);
        } else if (key.compareTo("maxCCSize") == 0) {
            Settings.maxCCSize = Integer.parseInt(value);
        } else if (key.compareTo("numHashRuns") == 0) {
            Settings.numHashRuns = Integer.parseInt(value);
        } else if (key.compareTo("numHashFuncs") == 0) {
            Settings.numHashFuncs = Integer.parseInt(value);
        } else if (key.compareTo("maxJac") == 0) {
            Settings.maxJac = Double.parseDouble(value);
        } else if (key.compareTo("isExact") == 0) {
            Settings.isExact = (value.compareTo("true") == 0);
        } else if (key.compareTo("minEdgesInSnap") == 0) {
            Settings.minEdgesInSnap = Integer.parseInt(value);
        }
    }
}
