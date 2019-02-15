package eu.unitn.disi.db.excode.webserver.local;

import static eu.unitn.disi.db.excode.main.Main.runTask;
import eu.unitn.disi.db.excode.webserver.utils.Configuration;
import java.util.concurrent.Callable;

/**
 *
 * @author bluecopper
 */
public class TaskRunner implements Callable<String> {

    public Configuration conf;
    
    public TaskRunner(Configuration conf) {
        this.conf = conf;
    }
    
    @Override
    public String call() {
        return runTask(conf);
    }
    
}
