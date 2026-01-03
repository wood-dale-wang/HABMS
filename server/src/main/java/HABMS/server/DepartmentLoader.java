package HABMS.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.IOException;
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
        if (!Files.exists(path)) {
            LOG.warning(() -> "department.json not found at " + path.toAbsolutePath());
            return Collections.emptyList();
        }

        ObjectMapper mapper = new ObjectMapper();
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, String.class);
        try {
            List<String> list = mapper.readValue(path.toFile(), type);
            return list.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to read departments, using empty list", e);
            return Collections.emptyList();
        }
    }
}
