package core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ConfigLoader {

    public static class Config {
        @JsonProperty("port")
        private int port;

        @JsonProperty("backends")
        private List<String> backends;

        public int getPort() {
            return port;
        }

        public List<String> getBackends() {
            return backends;
        }
    }

    public static Config loadConfig(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // 1. Uu tien tim trong thu muc resources (Classpath)
        InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(fileName);
        if (is != null) {
            return mapper.readValue(is, Config.class);
        }

        // 2. Thu tim truc tiep o thu muc goc cua project
        File file = new File(fileName);
        if (file.exists()) {
            return mapper.readValue(file, Config.class);
        }

        throw new IOException("Khong tim thay file cau hinh: " + fileName);
    }
}