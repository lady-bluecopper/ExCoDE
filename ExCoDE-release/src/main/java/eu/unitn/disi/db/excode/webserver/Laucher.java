package eu.unitn.disi.db.excode.webserver;

import com.google.gson.Gson;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import eu.unitn.disi.db.excode.webserver.local.ExplorationTaskRunner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import eu.unitn.disi.db.excode.webserver.local.TaskRunner;
import eu.unitn.disi.db.excode.webserver.utils.Configuration;
import eu.unitn.disi.db.excode.webserver.utils.Form;
import static spark.Spark.*;

/**
 *
 * @author bluecopper
 */
public class Laucher {

    static String DATA_FOLDER = "/Users/bluecopper/Documents/PhD/CorrelatedEvents/2018-correlated-events-in-temporal-networks-demo/densecorrelatededgefinderdemo/app/uploads/";
    static Gson parser = new Gson();
    static ExecutorService service = Executors.newFixedThreadPool(5);
    static int timeout = 5;
    static int counter = 0;
    
    static HashObjObjMap<String, Configuration> configurations = HashObjObjMaps.newMutableMap();
    static HashObjObjMap<String, String> outputs = HashObjObjMaps.newMutableMap();
    static String explorationOutput = "UNAVAILABLE";

    public static void main(String[] args) {
        port(8082);

        post("/launchTask", (request, response) -> {
            System.out.println("Received: " + request.body());
            Configuration conf = parser.fromJson(request.body(), Configuration.class);
            conf.Dataset.Path = DATA_FOLDER + conf.Dataset.Path;
            TaskRunner task = new TaskRunner(conf);
            System.out.println("Task Created with Conf: " + conf.toString());
            Future<String> output = service.submit(task);
            System.out.println("Task Submitted.");
            outputs.put(conf.Name, output.get());
            response.body("ack");
            return "ack";
        });
        
        post("/launchTaskFromForm", (request, response) -> {
            System.out.println("Received: " + request.body());
            Form form = parser.fromJson(request.body(), Form.class);
            Configuration conf = form.createConfiguration();
            configurations.put(conf.Name.toLowerCase(), conf);
            conf.Dataset.Path = DATA_FOLDER + conf.Dataset.Path;
            System.out.println(conf.Dataset.Path);
            TaskRunner task = new TaskRunner(conf);
            System.out.println("Task Created with Conf: " + conf.toString());
            Future<String> output = service.submit(task);
            System.out.println("Task Submitted.");
            outputs.put(conf.Name, output.get());
            response.body("ack");
            return "ack";
        });
        
        post("/launchExplorationTask", (request, response) -> {
            Configuration conf = parser.fromJson(request.body(), Configuration.class);
            conf.Dataset.Path = DATA_FOLDER + conf.Dataset.Path;
            ExplorationTaskRunner task = new ExplorationTaskRunner(conf);
            explorationOutput = "UNAVAILABLE";
            System.out.println("Exploration Task Created with Configuration: " + conf.toString());
            Future<String> output = service.submit(task);
            System.out.println("Exploration Task Submitted.");
            explorationOutput = output.get();
            response.body("ack");
            return "ack";
        });
        
        post("/launchExplorationTaskFromForm", (request, response) -> {
            Configuration temp = parser.fromJson(request.body(), Configuration.class);
            Configuration conf = configurations.get(temp.Name.toLowerCase());
            conf.Subgraph = temp.Subgraph;
            ExplorationTaskRunner task = new ExplorationTaskRunner(conf);
            explorationOutput = "UNAVAILABLE";
            System.out.println("Exploration Task Created with Configuration: " + conf.toString());
            Future<String> output = service.submit(task);
            System.out.println("Exploration Task Submitted.");
            explorationOutput = output.get();
            response.body("ack");
            return "ack";
        });
        
        get("/getResults", (request, response) -> {
            counter++;
            String configName = request.queryParams("name");
            if (outputs.containsKey(configName) || counter == timeout) {
                response.body(outputs.get(configName));
                return outputs.get(configName);
            } else {
                response.body("UNAVAILABLE");
                return "UNAVAILABLE";
            }
        });
        
        get("/getExplodedData", (request, response) -> {
            response.body(explorationOutput);
            return explorationOutput;
        });
    }
    
}
