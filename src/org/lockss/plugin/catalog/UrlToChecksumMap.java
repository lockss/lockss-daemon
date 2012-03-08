/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/
package org.lockss.plugin.catalog;

import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Represents the Url-to-checksum mappings for all Urls of 
 * single {@link ArchivalUnit}
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
