package HABMS.client.service;

import HABMS.client.model.Request;
import HABMS.client.model.Response;
import HABMS.client.net.NetworkClient;
import javafx.concurrent.Task;

/**
 * 只读查询类接口（例如科室列表）。
 */
public class LookupService {

    /** 构建后台查询任务。 */
    private Task<Response> request(String type, Object data) {
        return new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request(type, data));
            }
        };
    }

    /** 拉取科室列表。 */
    public Task<Response> departmentList() {
        return request("department_list", null);
    }

    /** 在守护线程中启动任务。 */
    public void run(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
