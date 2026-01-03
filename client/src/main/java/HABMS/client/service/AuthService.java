package HABMS.client.service;

import HABMS.client.model.Request;
import HABMS.client.model.Response;
import HABMS.client.net.NetworkClient;
import javafx.concurrent.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证相关的 Task 工厂，统一封装网络请求构造。
 */
public class AuthService {

    /** 构建一次性后台任务，发送指定类型请求。 */
    private Task<Response> request(String type, Object data) {
        return new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request(type, data));
            }
        };
    }

    /**
     * 患者登录（输入为身份证或手机号）。
     */
    public Task<Response> loginAccount(String inputId, String passwordHex) {
        Map<String, String> data = new HashMap<>();
        data.put("passwordHex", passwordHex);

        if (inputId != null) {
            String trimmed = inputId.trim();
            if (trimmed.length() == 11) {
                data.put("phone", trimmed);
            } else {
                data.put("pid", trimmed);
            }
        }

        return request("account_login", data);
    }

    /** 医生/管理员登录。 */
    public Task<Response> loginDoctor(String name, String department, String passwordHex) {
        Map<String, String> data = new HashMap<>();
        data.put("passwordHex", passwordHex);
        data.put("name", name);
        data.put("department", department);
        return request("doctor_login", data);
    }

    /** 患者注册。 */
    public Task<Response> registerAccount(String name, String pid, String phone, String passwordHex, String sex) {
        Map<String, String> data = new HashMap<>();
        data.put("name", name);
        data.put("pid", pid);
        data.put("phone", phone);
        data.put("passwordHex", passwordHex);
        data.put("sex", sex);
        return request("account_register", data);
    }

    /** 在守护线程中启动任务。 */
    public void run(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
