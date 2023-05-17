package de.keyruu.apifix;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.Resource;

@ApplicationScoped
public class ConfigFilterProvider implements Provider<FilterWatchListDeletable<ConfigMap, ConfigMapList, Resource<ConfigMap>>>
{
  @Inject
  KubernetesClient _client;

  @Override
  public FilterWatchListDeletable<ConfigMap, ConfigMapList, Resource<ConfigMap>> get()
  {
    return _client.configMaps().withLabel("apisix.config", "true");
  }
}
