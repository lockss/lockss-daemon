/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.state;

import java.util.*;
import org.lockss.plugin.*;
import org.lockss.util.ExtMapBean;
import org.lockss.util.LockssSerializable;

/**
 * DamagedNodeMap is a write-through persistent wrapper for a hashmap and a set.
 * It stores a Set of nodes with damage and a map of CachedUrlSets which need
 * repair.
 */
public class DamagedNodeSet implements LockssSerializable {
  HashSet nodesWithDamage = new HashSet();
  // the form of this map is key: url String, value: ArrayList url Strings
  // while it is logical to use a Set instead of an ArrayList, it is not
  // possible due to Castor's inability to marshall groups of Sets correctly.
  HashMap cusToRepair = new HashMap();
  transient HistoryRepository repository;
  transient ArchivalUnit theAu;

  /**
   * Simple constructor to allow bean creation during unmarshalling.
   */
  public DamagedNodeSet() { }

  /**
   * Standard constructor to create AU-specific DamagedNodeSet
   * @param au ArchivalUnit
   * @param repository HistoryRepository
   */
  public DamagedNodeSet(ArchivalUnit au, HistoryRepository repository) {
    this.theAu = au;
    this.repository = repository;
  }

  public ArchivalUnit getArchivalUnit() {
    return theAu;
  }

  /**
   * Clears both the damaged nodes and the CUSs to repair.  Does not write this
   * to disc.
   */
  protected void clear() {
    nodesWithDamage.clear();
    cusToRepair.clear();
  }

  /**
   * Checks if the given url is damaged.
   * @param nodeUrl the url
   * @return boolean true iff damaged
   */
  public boolean containsWithDamage(String nodeUrl) {
    return nodesWithDamage.contains(nodeUrl);
  }

  /**
   * Checks if the url needs repair.  'To repair' urls are associated with
   * a specific CUS, as several child repairs can be called by one lost poll.
   * @param cus the repairing CUS
   * @param nodeUrl the url to repair
   * @return boolean true iff the url needs repair under that CUS
   */
  public boolean containsToRepair(CachedUrlSet cus, String nodeUrl) {
    // get the url list associated with that CUS url, if any
    ArrayList urlArray = (ArrayList)cusToRepair.get(cus.getUrl());
    if (urlArray!=null) {
      return urlArray.contains(nodeUrl);
    }
    return false;
  }

  /**
   * Returns true if the CUS has damage.
   * @param cus CachedUrlSet
   * @return boolean true iff has damage.
   */
  synchronized public boolean hasDamage(CachedUrlSet cus) {
    // check if the CUS contains any of the other damaged nodes
    Iterator damagedIt = nodesWithDamage.iterator();
    while (damagedIt.hasNext()) {
      String url = (String) damagedIt.next();
      if (cus.containsUrl(url)) {
        return true;
      }
    }
    return false;
  }

  synchronized public boolean hasDamage(String nodeUrl) {
    return nodesWithDamage.contains(nodeUrl);
  }

  synchronized public boolean hasLocalizedDamage(CachedUrlSet cus) {
    if(cus.getSpec().isSingleNode()) {
      Iterator damagedIt = nodesWithDamage.iterator();
      while (damagedIt.hasNext()) {
        String url = (String) damagedIt.next();
        if(cus.containsUrl(url)) {
          return true;
        }
      }
    }
    return false;
  }

  synchronized public void clearDamage(CachedUrlSet cus) {
      Iterator damagedIt = nodesWithDamage.iterator();
      ArrayList clearList = new ArrayList();
      while (damagedIt.hasNext()) {
        String url = (String) damagedIt.next();
        if (cus.containsUrl(url)) {
          clearList.add(url);
        }
      }
      if(!clearList.isEmpty()) {
      nodesWithDamage.removeAll(clearList);
      repository.storeDamagedNodeSet(this);
    }
  }

  /**
   * Add this url to the list of nodes with damage, and write-through.
   * @param nodeUrl the damaged url
   */
  synchronized public void addToDamage(String nodeUrl) {
    if (nodesWithDamage.add(nodeUrl)) {
      repository.storeDamagedNodeSet(this);
    }
  }

  /**
   * Add the collection of url Strings to the CUS's repair list, merging
   * with any already present.  Writes through.
   * @param cus the repairing CUS
   * @param nodeUrls the urls to repair
   */
  synchronized public void addToRepair(CachedUrlSet cus, Collection nodeUrls) {
    // all repairs are associated with a specific CUS
    ArrayList urlArray = (ArrayList)cusToRepair.get(cus.getUrl());
    // merge the urls with any already stored
    if (urlArray==null) {
      urlArray = new ArrayList(nodeUrls.size());
    }
    Iterator iter = nodeUrls.iterator();
    while (iter.hasNext()) {
      Object url = iter.next();
      if (!urlArray.contains(url)) {
        urlArray.add(url);
      }
    }

    cusToRepair.put(cus.getUrl(), urlArray);
    repository.storeDamagedNodeSet(this);
  }



  /**
   * Remove the url from the damage list.
   * @param nodeUrl url to remove
   */
  synchronized public void removeFromDamage(String nodeUrl) {
    if(nodesWithDamage.remove(nodeUrl)) {
      repository.storeDamagedNodeSet(this);
    }
  }

  /**
   * Remove the url from the 'to repair' list.
   * @param cus the repairing CUS
   * @param nodeUrl the repaired url
   */
  synchronized public void removeFromRepair(CachedUrlSet cus, String nodeUrl) {
    Collection urls = (Collection)cusToRepair.get(cus.getUrl());
    if (urls!=null) {
      urls.remove(nodeUrl);
      if (urls.isEmpty()) {
        cusToRepair.remove(cus.getUrl());
      }
    }
    repository.storeDamagedNodeSet(this);
  }

  /**
   * Iterator of damaged url Strings.
   * @return Iterator the url Strings
   */
  public Iterator damageIterator() {
    return nodesWithDamage.iterator();
  }

  // accessors used for marshalling

  /**
   * Returns the Set of damaged url Strings.
   * @return HashSet the damaged urls
   */
  public HashSet getDamagedNodes() {
    return nodesWithDamage;
  }

  /**
   * Sets the Set of damaged url Strings
   * @param nodes HashSet the new Set
   */
  public void setDamagedNodes(HashSet nodes) {
    if(nodes == null) {
      nodesWithDamage = new HashSet();
    }
    else {
      this.nodesWithDamage = nodes;
    }
  }

  /**
   * Returns the map of CUS-List entries for nodes to repair.
   * @return HashMap the map
   */
  public HashMap getNodesToRepair() {
    return cusToRepair;
  }

  /**
   * Accessor for marshalling the repair map.  Converts to {@link ExtMapBean}.
   * @return ExtMapBean the map bean
   */
  public ExtMapBean getRepairNodeBean() {
    ExtMapBean bean = new ExtMapBean(cusToRepair);
    return bean;
  }

  /**
   * Accessor for unmarshalling the repair map.  Converts from {@link ExtMapBean}.
   * @param mapBean ExtMapBean the map bean
   */
  public void setRepairNodeBean(ExtMapBean mapBean) {
    cusToRepair = mapBean.getMap();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[DamagedNodeSet: ");
    sb.append("nodesWithDamage="+cusToRepair);
    sb.append("]");
    return sb.toString();
  }

}
