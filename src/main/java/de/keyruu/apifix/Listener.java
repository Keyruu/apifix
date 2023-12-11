package de.keyruu.apifix;

import java.util.logging.Logger;

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
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

public class Listener
{
  private static final Logger LOG = Logger.getLogger(Listener.class.getName());

  @Inject
  ConfigWatcher _watcher;

  @Inject
  ConfigFilterProvider _filterProvider;

  void onStart(@Observes StartupEvent ev) throws JsonMappingException, JsonProcessingException
  {
    if (LaunchMode.current().equals(LaunchMode.TEST) == false)
    {
      FilterWatchListDeletable<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMapFilter = _filterProvider.get();
      LOG.info("Starting Apisix Config Watcher");
      configMapFilter.watch(_watcher);
    }
  }
}