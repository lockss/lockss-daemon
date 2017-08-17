package org.lockss.entitlement;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.MultiKeyMap;

import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.config.Configuration;

/*
 * A very basic cache which just stores the results of the last 100 calls to the Entitlement Registry.
 * There's a strong chance this will need to become something more complicated down the line if performance isn't acceptable.
 * In this case though, it would probably be best to replace it with something like Guava's caching, rather than building something custom.
 */
public class CachingEntitlementRegistryClient extends BaseLockssDaemonManager implements EntitlementRegistryClient, ConfigurableManager {
  public static final String PREFIX = Configuration.PREFIX + "entitlement.cache.";
  public static final String PARAM_CACHE_SIZE = PREFIX + "size";
  static final int DEFAULT_CACHE_SIZE = 100;
  private EntitlementRegistryClient client;
  private MultiKeyMap cache;

  public void startService() {
    super.startService();
    this.client = getDaemon().getEntitlementRegistryClient();
  }

  public void setConfig(Configuration config, Configuration oldConfig, Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      int size = config.getInt(PARAM_CACHE_SIZE, DEFAULT_CACHE_SIZE);
      this.cache = MultiKeyMap.decorate(new LRUMap(size));
    }
  }

  public synchronized boolean isUserEntitled(String issn, String institution, String start, String end) throws IOException {
    Object result = this.cache.get("isUserEntitled", issn, institution, start, end);
    if(result == null) {
        result = this.client.isUserEntitled(issn, institution, start, end);
        this.cache.put("isUserEntitled", issn, institution, start, end, result);
    }
    return (Boolean) result;
  }

  public synchronized String getPublisher(String issn, String institution, String start, String end) throws IOException {
    Object result = this.cache.get("getPublisher", issn, institution, start, end);
    if(result == null) {
        result = this.client.getPublisher(issn, institution, start, end);
        this.cache.put("getPublisher", issn, institution, start, end, result);
    }
    return (String) result;
  }

  public synchronized PublisherWorkflow getPublisherWorkflow(String publisherName) throws IOException {
    Object result = this.cache.get("getPublisherWorkflow", publisherName);
    if(result == null) {
        result = this.client.getPublisherWorkflow(publisherName);
        this.cache.put("getPublisherWorkflow", publisherName, result);
    }
    return (PublisherWorkflow) result;
  }

  public synchronized String getInstitution(String scope) throws IOException {
    Object result = this.cache.get("getInstitution", scope);
    if(result == null) {
        result = this.client.getInstitution(scope);
        this.cache.put("getInstitution", scope, result);
    }
    return (String) result;
  }
}

