package core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ConfigLoader {

    public static class BackendConfig {
        @JsonProperty("url")
        private String url;

        @JsonProperty("weight")
        private int weight = 1;

        public String getUrl() {
            return url;
        }

        public int getWeight() {
            return weight;
        }
    }

    public static class Config {
        @JsonProperty("port")
        private int port;

        @JsonProperty("algorithm")
        private String algorithm = "weighted_least_conn";

        @JsonProperty("backends")
        private List<BackendConfig> backends;

        public int getPort() {
            return port;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public List<BackendConfig> getBackends() {
            return backends;
        }
    }

    public static Config loadConfig(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(fileName);
        if (is != null) {
            return mapper.readValue(is, Config.class);
        }

        File file = new File(fileName);
        if (file.exists()) {
            return mapper.readValue(file, Config.class);
        }

        throw new IOException("Khong tim thay file cau hinh: " + fileName);
    }
}