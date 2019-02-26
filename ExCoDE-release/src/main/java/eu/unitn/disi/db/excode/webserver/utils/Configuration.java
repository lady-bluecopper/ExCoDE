package eu.unitn.disi.db.excode.webserver.utils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bluecopper
 */
public class Configuration {

    public String Name;
    public Dataset Dataset;
    public Task Task;
    public Subgraph Subgraph;

    public Configuration() {
        Dataset = new Dataset();
        Task = new Task();
        Subgraph = new Subgraph();
    }

    public class Dataset {

        public String Name;
        public String Path;
        public Properties Properties;

        public Dataset() {
            Properties = new Properties();
        }
    }

    public class Properties {

        public boolean Weighted;
        public boolean Labeled;
    }

    public class Task {

        public Double Correlation;
        public Double Density;
        public String DensityFunction;
        public Integer EdgesPerSnapshot;
        public Integer MaxSize;
        public Double Epsilon;
    }

    public class Subgraph {

        public List<JEdge> edges;
        
        public Subgraph() {
            edges = new ArrayList<JEdge>();
        }

        public class JEdge {

            public Integer source;
            public Integer target;

            public String toString() {
                return "(" + source + "," + target + ")";
            }
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            edges.stream().forEach(edge -> builder.append(edge.toString()));
            builder.append("}");
            return builder.toString();
        }

    }

    public String toString() {
        return Dataset.Path + "\t" + Task.Correlation + "\t"
                + Task.Density + "\t" + Task.DensityFunction + "\t"
                + Task.EdgesPerSnapshot + "\t" + Task.MaxSize + "\t"
                + Task.Epsilon + "\t" + Subgraph.toString();
    }

}
