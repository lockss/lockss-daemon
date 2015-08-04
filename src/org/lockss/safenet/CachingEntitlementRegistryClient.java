package org.lockss.safenet;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.MultiKeyMap;

import org.lockss.app.BaseLockssManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.config.Configuration;

/*
 * A very basic cache which just stores the results of the last 100 calls to the Entitlement Registry.
 * There's a strong chance this will need to become something more complicated down the line.
 */
public class CachingEntitlementRegistryClient extends BaseLockssManager implements EntitlementRegistryClient, ConfigurableManager {
  private BaseEntitlementRegistryClient client;
  private MultiKeyMap cache;

  public CachingEntitlementRegistryClient() {
      this(new BaseEntitlementRegistryClient(), 100);
  }

  protected CachingEntitlementRegistryClient(BaseEntitlementRegistryClient client, int size) {
      this.client = client;
      this.cache = MultiKeyMap.decorate(new LRUMap(size));
  }

  public void setConfig(Configuration config, Configuration oldConfig, Configuration.Differences diffs) {
    client.setConfig(config, oldConfig, diffs);
  }

  public boolean isUserEntitled(String issn, String institution, String start, String end) throws IOException {
    Object result = this.cache.get("isUserEntitled", issn, institution, start, end);
    if(result == null) {
        result = this.client.isUserEntitled(issn, institution, start, end);
        this.cache.put("isUserEntitled", issn, institution, start, end, result);
    }
    return (Boolean) result;
  }

  public String getPublisher(String issn, String institution, String start, String end) throws IOException {
    Object result = this.cache.get("getPublisher", issn, institution, start, end);
    if(result == null) {
        result = this.client.getPublisher(issn, institution, start, end);
        this.cache.put("getPublisher", issn, institution, start, end, result);
    }
    return (String) result;
  }

  public PublisherWorkflow getPublisherWorkflow(String publisherName) throws IOException {
    Object result = this.cache.get("getPublisherWorkflow", publisherName);
    if(result == null) {
        result = this.client.getPublisherWorkflow(publisherName);
        this.cache.put("getPublisherWorkflow", publisherName, result);
    }
    return (PublisherWorkflow) result;
  }

  public String getInstitution(String scope) throws IOException {
    Object result = this.cache.get("getInstitution", scope);
    if(result == null) {
        result = this.client.getInstitution(scope);
        this.cache.put("getInstitution", scope, result);
    }
    return (String) result;
  }

}

