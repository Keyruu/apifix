package de.keyruu.apifix;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ApisixConfig
{
  private List<Object> routes = new ArrayList<>();
  private List<Object> services = new ArrayList<>();
  private List<Object> upstreams = new ArrayList<>();
  private List<Object> plugins = new ArrayList<>();
  @JsonProperty("global_rules")
  private List<Object> globalRules = new ArrayList<>();
  private List<Object> consumers = new ArrayList<>();
  @JsonProperty("plugin_metadata")
  private List<Object> pluginMetadata = new ArrayList<>();
  @JsonProperty("stream_routes")
  private List<Object> streamRoutes = new ArrayList<>();
  @JsonProperty("plugin_configs")
  private List<Object> pluginConfigs = new ArrayList<>();

  public void merge(ApisixConfig toBeMerged)
  {
    routes.addAll(toBeMerged.getRoutes());
    services.addAll(toBeMerged.getServices());
    upstreams.addAll(toBeMerged.getUpstreams());
    plugins.addAll(toBeMerged.getPlugins());
    globalRules.addAll(toBeMerged.getGlobalRules());
    consumers.addAll(toBeMerged.getConsumers());
    pluginMetadata.addAll(toBeMerged.getPluginMetadata());
    streamRoutes.addAll(toBeMerged.getStreamRoutes());
    pluginConfigs.addAll(toBeMerged.getPluginConfigs());
  }

  public List<Object> getRoutes()
  {
    return routes;
  }

  public void setRoutes(List<Object> routes)
  {
    this.routes = routes;
  }

  public List<Object> getServices()
  {
    return services;
  }

  public void setServices(List<Object> services)
  {
    this.services = services;
  }

  public List<Object> getUpstreams()
  {
    return upstreams;
  }

  public void setUpstreams(List<Object> upstreams)
  {
    this.upstreams = upstreams;
  }

  public List<Object> getPlugins()
  {
    return plugins;
  }

  public void setPlugins(List<Object> plugins)
  {
    this.plugins = plugins;
  }

  public List<Object> getGlobalRules()
  {
    return globalRules;
  }

  public void setGlobalRules(List<Object> globalRules)
  {
    this.globalRules = globalRules;
  }

  public List<Object> getConsumers()
  {
    return consumers;
  }

  public void setConsumers(List<Object> consumers)
  {
    this.consumers = consumers;
  }

  public List<Object> getPluginMetadata()
  {
    return pluginMetadata;
  }

  public void setPluginMetadata(List<Object> pluginMetadata)
  {
    this.pluginMetadata = pluginMetadata;
  }

  public List<Object> getStreamRoutes()
  {
    return streamRoutes;
  }

  public void setStreamRoutes(List<Object> streamRoutes)
  {
    this.streamRoutes = streamRoutes;
  }

  public List<Object> getPluginConfigs()
  {
    return pluginConfigs;
  }

  public void setPluginConfigs(List<Object> pluginConfigs)
  {
    this.pluginConfigs = pluginConfigs;
  }
}
