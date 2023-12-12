package de.keyruu.apifix;

import java.util.logging.Logger;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

public class Listener
{
  private static final Logger LOG = Logger.getLogger(Listener.class.getName());

  @Inject
  KubernetesClient client;

  @Inject
  ApisixConfigEventHandler eventHandler;

  void onStart(@Observes StartupEvent ev)
  {
    if (LaunchMode.current().equals(LaunchMode.TEST) == false)
    {
      FilterWatchListDeletable<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMapFilter = client
              .configMaps()
              .withLabel("apisix.config", "true");

      LOG.info("Starting Apisix Config Informer");
      SharedIndexInformer<ConfigMap> configMapInformer = configMapFilter.inform(eventHandler);

      for (ConfigMap configMap : configMapInformer.getIndexer().list()) {
        LOG.info("Started Informer for ConfigMap: " + configMap.getMetadata().getName());
      }
    }
  }
}