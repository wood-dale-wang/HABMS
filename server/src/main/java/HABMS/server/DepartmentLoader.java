package HABMS.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 从 JSON 数组文件加载科室列表，忽略空值并给出日志。 */
final class DepartmentLoader {
    private static final Logger LOG = Logger.getLogger(DepartmentLoader.class.getName());

    private DepartmentLoader() {
    }

    /** 读取科室文件；缺失或错误时返回空列表。 */
    static List<String> load(Path path) {
        ObjectMapper mapper = new ObjectMapper();
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, String.class);

        try {
            if (Files.exists(path)) {
                // 从文件系统读取
                LOG.info(() -> "Loading department.json from: " + path.toAbsolutePath());
                List<String> list = mapper.readValue(path.toFile(), type);
                return list.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
            } else {
                // 尝试从 classpath（JAR 内部）读取
                LOG.warning(() -> "department.json not found at " + path.toAbsolutePath() + ", trying to load from JAR");
                try (InputStream is = HABMS.server.ServerMain.class.getClassLoader().getResourceAsStream("department.json")) {
                    if (is == null) {
                        LOG.warning("department.json not found in classpath");
                        return Collections.emptyList();
                    }
                    List<String> list = mapper.readValue(is, type);
                    return list.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to read departments, using empty list", e);
            return Collections.emptyList();
        }
    }
}
