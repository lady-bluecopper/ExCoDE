package eu.unitn.disi.db.excode.webserver;

import com.google.gson.Gson;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.collect.map.hash.HashObjObjMaps;
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

    static Gson parser = new Gson();
    static ExecutorService service = Executors.newFixedThreadPool(5);
    static int timeout = 5;
    static int counter = 0;
    
    static HashObjObjMap<String, String> outputs = HashObjObjMaps.newMutableMap();

    public static void main(String[] args) {
        port(8082);

        post("/launchTask", (request, response) -> {
            Configuration conf = parser.fromJson(request.body(), Configuration.class);
            String[] filePath = conf.Dataset.Path.split("/");
            conf.Dataset.Path = "/Users/bluecopper/Desktop/" + filePath[filePath.length - 1];
            System.out.println("Received Data: " + request.body());
            TaskRunner task = new TaskRunner(conf);
            System.out.println("Task Created with Conf: " + conf.toString());
            Future<String> output = service.submit(task);
            System.out.println("Task Submitted.");
            outputs.put(conf.Name, output.get());
            response.body("ack");
            return "ack";
        });
        
        post("/launchTaskFromForm", (request, response) -> {
            Form form = parser.fromJson(request.body(), Form.class);
            Configuration conf = form.createConfiguration();
            String[] filePath = conf.Dataset.Path.split("/");
            conf.Dataset.Path = "/Users/bluecopper/Desktop/" + filePath[filePath.length - 1];
            System.out.println("Received Data: " + request.body());
            TaskRunner task = new TaskRunner(conf);
            System.out.println("Task Created with Conf: " + conf.toString());
            Future<String> output = service.submit(task);
            System.out.println("Task Submitted.");
            outputs.put(conf.Name, output.get());
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
    }
    
}
