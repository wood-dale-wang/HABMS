package HABMS.server;

import HABMS.db.HABMSDB;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 简易 TCP 服务器：接受客户端连接并为每个连接分配一个 Service 处理。 */
final class ServerRuntime {
    private static final Logger LOG = Logger.getLogger(ServerRuntime.class.getName());

    private final int port;
    private final HABMSDB db;
    private final List<String> departments;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "habms-client");
        t.setDaemon(true);
        return t;
    });

    private ServerSocket serverSocket;

    ServerRuntime(int port, HABMSDB db, List<String> departments) {
        this.port = port;
        this.db = Objects.requireNonNull(db);
        this.departments = List.copyOf(departments);
    }

    /** 启动监听循环，接收新连接并交给线程池。 */
    void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        serverSocket = new ServerSocket(port);
        LOG.info(() -> "Listening on port " + port);

        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                executor.submit(new Service(socket, db, departments));
            } catch (IOException acceptError) {
                if (running.get()) {
                    LOG.log(Level.WARNING, "Accept failed", acceptError);
                }
            }
        }
    }

    /** 停止监听并关闭线程池。 */
    void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error while closing server socket", e);
        }
        executor.shutdownNow();
        LOG.info("ServerRuntime stopped");
    }
}
