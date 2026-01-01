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

/** Simple TCP server that accepts a socket per client and delegates to {@link Service}. */
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
