package de.keyruu.apifix;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;

public class Listener
{
  @Inject
  KubernetesClient _client;

  @Inject
  ConfigWatcher _watcher;

  @Inject
  ConfigFilterProvider _filterProvider;

  ApisixConfig _config = new ApisixConfig();
  ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  void onStart(@Observes StartupEvent ev) throws JsonMappingException, JsonProcessingException
  {
    if (LaunchMode.current().equals(LaunchMode.TEST) == false)
    {
      FilterWatchListDeletable<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMapFilter = _filterProvider.get();

      configMapFilter.watch(_watcher);
    }
  }
}