package de.keyruu.apifix;

import io.fabric8.kubernetes.api.model.ConfigMap;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class KubernetesStore {
    private List<ConfigMap> configMaps = new ArrayList<>();
    private final Map<String, String> secretMap = new HashMap<>();

    public List<ConfigMap> getConfigMaps() {
        return configMaps;
    }

    public void addConfigMap(ConfigMap configMap) {
        configMaps.add(configMap);
    }

    public void removeConfigMap(ConfigMap configMap) {
        configMaps.remove(configMap);
    }

    public Map<String, String> getSecretMap() {
        return secretMap;
    }

    public void setSecretMap(String key, String value) {
        secretMap.put(key, value);
    }

    public boolean containsSecret(String key) {
        return secretMap.containsKey(key);
    }
}
