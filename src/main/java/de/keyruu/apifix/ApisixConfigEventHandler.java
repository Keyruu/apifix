package de.keyruu.apifix;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class ApisixConfigEventHandler implements ResourceEventHandler<ConfigMap>
{
  private static final Logger LOG = Logger.getLogger(ApisixConfigEventHandler.class.getName());

  @Inject
  KubernetesClient _client;

  @ConfigProperty(name = "apifix.config.path")
  String configPath;

  @Inject
  KubernetesStore _kubernetesStore;

  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  @Override
  public void onAdd(ConfigMap configMap) {
    LOG.info("Add ConfigMap " + configMap.getMetadata().getName());
    _kubernetesStore.addConfigMap(configMap);
    writeConfig(mergeConfigMaps(_kubernetesStore.getConfigMaps()));
  }

  @Override
  public void onUpdate(ConfigMap oldConfigMap, ConfigMap newConfigMap) {
    LOG.info("Update ConfigMap " + newConfigMap.getMetadata().getName());
    _kubernetesStore.removeConfigMap(oldConfigMap);
    _kubernetesStore.addConfigMap(newConfigMap);
    writeConfig(mergeConfigMaps(_kubernetesStore.getConfigMaps()));
  }

  @Override
  public void onDelete(ConfigMap configMap, boolean b) {
    LOG.info("Delete ConfigMap " + configMap.getMetadata().getName());
    _kubernetesStore.removeConfigMap(configMap);
    writeConfig(mergeConfigMaps(_kubernetesStore.getConfigMaps()));
  }

  public void writeConfig(ApisixConfig config)
  {
    try
    {
      String configString = mapper.writeValueAsString(config);
      String replacedString = replaceVars(configString);
      try (PrintWriter out = new PrintWriter(configPath))
      {
        out.println(replacedString + "#END");
        LOG.info("Config written to " + configPath);
      }
    }
    catch (JsonProcessingException | FileNotFoundException e)
    {
      throw new RuntimeException(e);
    }
  }

  String replaceVars(String input)
  {
    String output = input;
    int startIndex = input.indexOf("${");
    while (startIndex != -1)
    {
      int endIndex = output.indexOf("}", startIndex);
      if (endIndex == -1)
      {
        break; // Stop if matching "}" is not found
      }
      String variable = output.substring(startIndex + 2, endIndex);
      String value = getVariableValue(variable);
      output = output.substring(0, startIndex) + value + output.substring(endIndex + 1);
      startIndex = output.indexOf("${", startIndex + value.length());
    }

    return output;
  }

  String getVariableValue(String variable)
  {
    String[] splitVariable = variable.split(":");
    if (splitVariable.length != 2)
    {
      throw new RuntimeException("Wrong variable syntax! Variable: " + variable);
    }
    String mode = splitVariable[0], variableName = splitVariable[1];
    if (mode.equals("secret"))
    {
      return getSecretValueForVariable(variableName);
    }
    else if (mode.equals("env"))
    {
      return System.getenv(variableName);
    }
    else
    {
      throw new RuntimeException("This mode is not supported. Mode: " + mode + " Variable: " + variableName);
    }
  }

  String getSecretValueForVariable(String variableName)
  {
    String[] secretPath = variableName.split("\\.");
    if (secretPath.length != 2)
    {
      throw new RuntimeException("Wrong secret path syntax! Variable name: " + variableName);
    }
    String secretName = secretPath[0], secretKey = secretPath[1];

    if (_kubernetesStore.containsSecret(variableName))
    {
      return _kubernetesStore.getSecretMap().get(variableName);
    }

    Secret secret = _client.secrets().withName(secretName).get();
    if (secret == null)
    {
      throw new RuntimeException("Could not find secret! Secret name: " + secretName + " Variable name: " + variableName);
    }

    var decodedSecret = new String(Base64.getDecoder().decode(secret.getData().get(secretKey)));
    _kubernetesStore.setSecretMap(variableName, decodedSecret);
    return decodedSecret;
  }

  public ApisixConfig mergeConfigMaps(List<ConfigMap> configMaps)
  {
    ApisixConfig apisixConfig = new ApisixConfig();
    for (ConfigMap configMap : configMaps)
    {
      apisixConfig.merge(mapConfig(configMap));
    }
    return apisixConfig;
  }

  ApisixConfig mapConfig(ConfigMap cfgMap)
  {
    String config = cfgMap.getData().get("config");
    try
    {
      return mapper.readValue(config, ApisixConfig.class);
    }
    catch (JsonProcessingException e)
    {
      throw new RuntimeException(e);
    }
  }
}
