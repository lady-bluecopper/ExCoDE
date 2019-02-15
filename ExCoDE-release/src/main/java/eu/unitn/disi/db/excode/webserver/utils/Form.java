package eu.unitn.disi.db.excode.webserver.utils;

/**
 *
 * @author bluecopper
 */
public class Form {
    
    public String config;
    public String dataset;
    public String path;
    public Double correlation;
    public String densityGroup;
    public Double density;
    public Integer edgesxsnaps;
    public Integer sizeRange;
    public Integer SizeButton;
    public Double epsilon;
    public String action;
    
    public Configuration createConfiguration() {
        Configuration conf = new Configuration();
        conf.Name = config;
        conf.Dataset.Name = dataset;
        conf.Dataset.Path = path;
        conf.Dataset.Properties.Labeled = false;
        conf.Dataset.Properties.Weighted = false;
        conf.Task.Correlation = correlation;
        conf.Task.Density = density;
        conf.Task.DensityFunction = densityGroup;
        conf.Task.EdgesPerSnapshot = edgesxsnaps;
        if (SizeButton != null) {
            conf.Task.MaxSize = SizeButton;
        } else {
            conf.Task.MaxSize = sizeRange;
        }
        conf.Task.Epsilon = epsilon;
        return conf; 
    }
    
}