package org.lockss.plugin.catalog;

import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Represents the Url-to-checksum mappings for all Urls of single {@link ArchivalUnit}>
 */
public class UrlToChecksumMap {
  private TreeMap<String, String> map;

  public UrlToChecksumMap() {
    map = new TreeMap<String, String>();
  }

  /**
   * A copy constructor performing a deep copy of the class and its accompanying
   * map
   */
  public UrlToChecksumMap(UrlToChecksumMap old) {
    map.putAll(old.map);
  }

  /**
   * Returns a SortedSet of the keys in this map
   */
  public SortedSet<String> keySet() {
    return (SortedSet<String>) map.keySet();
  }

  /**
   * Returns the checksum corresponding to the specified Url
   */
  public String get(String url) {
    return map.get(url);
  }

  /**
   * Adds (or replaces) the specified url-to-checksum mapping
   */
  public void put(String url, String checksum) {
    map.put(url, checksum);
  }

  /**
   * Performs a deep comparison with the specified object
   */
  @Override
  public boolean equals(Object obj) {
    if (obj.getClass().equals(this.getClass())) {
      if (obj == this)
        return true;
      else
        return ((UrlToChecksumMap) obj).map.equals(map);
    } else
      return false;
  }

  /**
   * Overridden because equals was also overridden, to provide a meaningful hash
   * code
   */
  @Override
  public int hashCode() {
    return map.hashCode();
  }
}
