package eu.unitn.disi.db.excode.webserver.utils;

/**
 *
 * @author bluecopper
 */
public class Configuration {
    
    public String Name;
    public Dataset Dataset;
    public Task Task;    
    
    public Configuration() {
        Dataset = new Dataset();
        Task = new Task();
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
    
    public String toString() {
        return Dataset.Path + "\t" + Task.Correlation + "\t" + 
                Task.Density + "\t" + Task.DensityFunction + "\t" +
                Task.EdgesPerSnapshot + "\t" + Task.MaxSize + "\t" + 
                Task.Epsilon;
    }
    
}
