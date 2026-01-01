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

/** Loads department list from a JSON array file. */
final class DepartmentLoader {
    private static final Logger LOG = Logger.getLogger(DepartmentLoader.class.getName());

    private DepartmentLoader() {
    }

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
