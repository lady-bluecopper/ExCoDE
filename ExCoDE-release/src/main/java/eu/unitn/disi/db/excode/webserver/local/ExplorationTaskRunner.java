package eu.unitn.disi.db.excode.webserver.local;

import static eu.unitn.disi.db.excode.main.Main.runExplorationTask;
import eu.unitn.disi.db.excode.webserver.utils.Configuration;
import java.util.concurrent.Callable;

/**
 *
 * @author bluecopper
 */
public class ExplorationTaskRunner implements Callable<String> {
    
    Configuration config;
    
    public ExplorationTaskRunner(Configuration config) {
        this.config = config;
    }

    @Override
    public String call() throws Exception {
        return runExplorationTask(config);
    }
    
}
