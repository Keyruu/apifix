package de.keyruu.apifix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.ConfigMapListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfigWatcherTests
{
  @Inject
  KubernetesClient _client;

  @Inject
  ConfigFilterProvider _filterProvider;

  @Inject
  ConfigWatcher _configWatcher;

  @ConfigProperty(name = "apifix.config.path")
  String configPath;

  @BeforeEach
  public void before() throws FileNotFoundException
  {
    _client.resource(new FileInputStream("src/test/resources/configmap1.yaml")).createOrReplace();
    _client.resource(new FileInputStream("src/test/resources/configmap2.yaml")).createOrReplace();
    _client.resource(new FileInputStream("src/test/resources/secret1.yaml")).createOrReplace();
    _client.resource(new FileInputStream("src/test/resources/secret2.yaml")).createOrReplace();
  }

  @Test
  public void testConfigMerging() throws IOException
  {
    FilterWatchListDeletable<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMapFilter = _filterProvider.get();

    _configWatcher.writeConfig(_configWatcher.mergeConfigMaps(configMapFilter.list()));

    assertArrayEquals(Files.readAllBytes(Path.of("src/test/resources/expectedApisixConfig.yaml")), Files.readAllBytes(Path.of(configPath)));
  }

  // @Test
  // public void testEventReceived()
  // {
  // ConfigMap configMap = new ConfigMapBuilder()
  // .withNewMetadata()
  // .withName("test-config")
  // .endMetadata()
  // .addToData("config", "some-data")
  // .build();

  // ConfigWatcher spy = spy(_configWatcher);

  // spy.eventReceived(Action.ADDED, configMap);

  // verify(spy, times(1)).eventReceived(Action.ADDED, configMap);
  // }

  @Test
  public void testMergeConfigMaps()
  {
    ConfigMap configMap1 = new ConfigMapBuilder()
      .withNewMetadata()
      .withName("config1")
      .endMetadata()
      .addToData("config", "routes:\n  - name: route1\n    uri: /hello\n    upstream:\n      nodes:\n        - host: localhost\n          port: 8080\n          weight: 1\n      type: roundrobin")
      .build();

    ConfigMap configMap2 = new ConfigMapBuilder()
      .withNewMetadata()
      .withName("config2")
      .endMetadata()
      .addToData("config", "routes:\n  - name: route2\n    uri: /world\n    upstream:\n      nodes:\n        - host: localhost\n          port: 8081\n          weight: 1\n      type: roundrobin")
      .build();

    ApisixConfig mergedConfig = _configWatcher.mergeConfigMaps(new ConfigMapListBuilder().addToItems(configMap1, configMap2).build());

    assertEquals(2, mergedConfig.getRoutes().size());
  }

  @Test
  public void testMapConfig()
  {
    ConfigMap configMap = new ConfigMapBuilder()
      .withNewMetadata()
      .withName("test-config")
      .endMetadata()
      .addToData("config", "routes:\n  - name: test-route\n    uri: /hello\n    upstream:\n      nodes:\n        - host: localhost\n          port: 8080\n          weight: 1\n      type: roundrobin")
      .build();

    ApisixConfig apisixConfig = _configWatcher.mapConfig(configMap);

    assertEquals(1, apisixConfig.getRoutes().size());
  }

  @Test
  public void testReplaceVars_withUnsupportedMode()
  {
    String input = "This is a ${unsupported:var} test.";
    assertThrows(RuntimeException.class, () -> _configWatcher.replaceVars(input));
  }

  @Test
  public void testReplaceVars_withWrongVariableSyntax()
  {
    String input = "This is a ${wrongVarSyntax} test.";
    assertThrows(RuntimeException.class, () -> _configWatcher.replaceVars(input));
  }

  @Test
  public void testReplaceVars_withWrongSecretPathSyntax()
  {
    String input = "This is a ${secret:wrongSecretPath} test.";

    assertThrows(RuntimeException.class, () -> _configWatcher.replaceVars(input));
  }

  @Test
  public void testGetVariableValue_withUnsupportedMode()
  {
    String variable = "unsupported:var";
    assertThrows(RuntimeException.class, () -> _configWatcher.getVariableValue(variable));
  }

  @Test
  public void testGetVariableValue_withWrongVariableSyntax()
  {
    String variable = "wrongVarSyntax";
    assertThrows(RuntimeException.class, () -> _configWatcher.getVariableValue(variable));
  }

  @Test
  public void testGetSecretValueForVariable_withWrongSecretPathSyntax()
  {
    String variableName = "wrongSecretPath";
    assertThrows(RuntimeException.class, () -> _configWatcher.getSecretValueForVariable(variableName));
  }
}
