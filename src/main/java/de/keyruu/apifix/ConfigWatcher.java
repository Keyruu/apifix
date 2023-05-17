package de.keyruu.apifix;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

@ApplicationScoped
public class ConfigWatcher implements Watcher<ConfigMap>
{
  @Inject
  KubernetesClient _client;

  @ConfigProperty(name = "apifix.config.path")
  String configPath;

  @Inject
  ConfigFilterProvider _filterProvider;

  private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  @Override
  public void eventReceived(Action action, ConfigMap resource)
  {
    if (action.equals(Action.ADDED) || action.equals(Action.MODIFIED) || action.equals(Action.DELETED))
    {
      writeConfig(mergeConfigMaps(_filterProvider.get().list()));
    }
  }

  @Override
  public void onClose(WatcherException cause)
  {
    return;
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
      startIndex = output.indexOf("${", endIndex);
    }

    return output;
  }

  String getVariableValue(String variable)
  {
    String[] splitVariable = variable.split(":");
    if (splitVariable.length != 2)
    {
      throw new RuntimeException("Wrong variable syntax!");
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
      throw new RuntimeException("This mode is not supported.");
    }
  }

  String getSecretValueForVariable(String variableName)
  {
    String[] secretPath = variableName.split("\\.");
    if (secretPath.length != 2)
    {
      throw new RuntimeException("Wrong secret path syntax!");
    }
    String secretName = secretPath[0], secretKey = secretPath[1];
    Secret secret = _client.secrets().withName(secretName).get();
    if (secret == null)
    {
      throw new RuntimeException("Could not find secret!");
    }
    return new String(Base64.getDecoder().decode(secret.getData().get(secretKey)));
  }

  public ApisixConfig mergeConfigMaps(ConfigMapList configMaps)
  {
    ApisixConfig apisixConfig = new ApisixConfig();
    for (ConfigMap configMap : configMaps.getItems())
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
