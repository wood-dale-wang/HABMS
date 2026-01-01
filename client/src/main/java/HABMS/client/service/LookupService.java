package HABMS.client.service;

import HABMS.client.model.Request;
import HABMS.client.model.Response;
import HABMS.client.net.NetworkClient;
import javafx.concurrent.Task;

public class LookupService {

    private Task<Response> request(String type, Object data) {
        return new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request(type, data));
            }
        };
    }

    public Task<Response> departmentList() {
        return request("department_list", null);
    }

    public void run(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
