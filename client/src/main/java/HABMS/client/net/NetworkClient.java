package HABMS.client.net;

import HABMS.client.model.Request;
import HABMS.client.model.Response;
import HABMS.client.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000; // Server defaults to 9000

    private NetworkClient() {}

    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
    }

    public Response sendRequest(Request request) throws IOException {
        // Ensure connection
        // connect(); // In a real app, you might want to manage connection lifecycle better

        // For now, let's assume we connect for each request or maintain a long connection.
        // Given the requirement "login after each connection corresponds to an Account", 
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
    
    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
