package HABMS.client.net;

import HABMS.client.model.Request;
import HABMS.client.model.Response;
import HABMS.client.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

/**
 * TCP 客户端单例：维护与服务器的长连接并以 JSON 行协议收发。
 */
public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final String SERVER_HOST;
    private static final int SERVER_PORT;

    /** 支持通过环境变量配置host:port */
    static {
        String v1 = System.getenv("SERVER_HOST");
        SERVER_HOST = (v1 == null || v1.isBlank() ? "localhost" : v1);
        String v2 = System.getenv("SERVER_PORT");
        SERVER_PORT = (v2 == null || v2.isBlank() ? 9000 : Integer.parseInt(v2));
        System.out.println("server:" + v1 + ':' + v2);
    }

    private NetworkClient() {
    }

    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    /** 建立到服务器的 socket 连接。 */
    public void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            Socket s = new Socket(Proxy.NO_PROXY);
            s.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            socket = s;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
    }

    /** 序列化请求，发送后读取单行响应并反序列化。 */
    public Response sendRequest(Request request) throws IOException {
        // Ensure connection
        // connect(); // In a real app, you might want to manage connection lifecycle
        // better

        // For now, let's assume we connect for each request or maintain a long
        // connection.
        // Given the requirement "login after each connection corresponds to an
        // Account",
        // we should maintain the connection.
        if (socket == null || socket.isClosed()) {
            // Try to connect, if fails, throw exception
            connect();
        }

        String jsonReq = JsonUtil.toJson(request);
        System.out.println("Sending: " + jsonReq);
        out.println(jsonReq);

        String jsonResp = in.readLine();
        System.out.println("Received: " + jsonResp);
        if (jsonResp == null) {
            throw new IOException("Server closed connection");
        }

        return JsonUtil.fromJson(jsonResp, Response.class);
    }

    /** 关闭底层 socket。 */
    public void close() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
