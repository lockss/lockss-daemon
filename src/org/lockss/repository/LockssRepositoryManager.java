/**
 * 
 */
package org.lockss.repository;

import java.util.Map;

/**
 * @author edwardsb
 *
 * This interface holds the map from AUID to the repositories.
 */
public interface LockssRepositoryManager {
  public String getRepositoryPath();
  public Map<String, String> getAuMap();
  public void resetMap();
}
