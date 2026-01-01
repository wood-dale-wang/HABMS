package HABMS.server;

import HABMS.db.HABMSDB;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Entry point that wires configuration, database and server runtime. */
public final class ServerMain {
    private static final Logger LOG = Logger.getLogger(ServerMain.class.getName());

    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(env("HABMS_PORT", "9000"));
        String url = env("HABMS_URL", "jdbc:mariadb://localhost:3306/HABMSDB?useSSL=false&allowPublicKeyRetrieval=true");
        String user = env("HABMS_USER", "rjava");
        String pass = env("HABMS_PASS", "rjava");
        Path departmentsPath = Paths.get(env("HABMS_DEPARTMENTS", "department.json"));

        List<String> departments = DepartmentLoader.load(departmentsPath);
        LOG.info(() -> "Loaded departments: " + departments.size());

        HABMSDB db = new HABMSDB(url, user, pass);
        ServerRuntime runtime = new ServerRuntime(port, db, departments);

        Runtime.getRuntime().addShutdownHook(new Thread(runtime::stop, "habms-server-stop"));
        LOG.info(() -> "Starting HABMS server on port " + port);
        try {
            runtime.start();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Server stopped unexpectedly", e);
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? def : v;
    }
}
