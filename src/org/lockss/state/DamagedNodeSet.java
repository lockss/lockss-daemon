/*
 * $Id: DamagedNodeSet.java,v 1.5 2004-03-06 00:48:57 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * DamagedNodeMap is a write-through persistent wrapper for a hashmap.
 */
public class DamagedNodeSet {
  HashSet nodesWithDamage;
  // the form of this map is key: url String, value: ArrayList url Strings
  // while it is logical to use a Set instead of an ArrayList, it is not
  // possible due to Castor's inability to marshall groups of Sets correctly.
  HashMap cusToRepair;
  HistoryRepository repository;
  ArchivalUnit theAu;

  public DamagedNodeSet() {
    // initialize here so that if there's no set to load, it's not null
    nodesWithDamage = new HashSet();
    cusToRepair = new HashMap();
  }

  public DamagedNodeSet(ArchivalUnit au, HistoryRepository repository) {
    this.theAu = au;
    this.repository = repository;
    nodesWithDamage = new HashSet();
    cusToRepair = new HashMap();
  }

  public ArchivalUnit getArchivalUnit() {
    return theAu;
  }

  public void clear() {
    nodesWithDamage.clear();
    cusToRepair.clear();
  }

  public boolean containsWithDamage(String nodeUrl) {
    return nodesWithDamage.contains(nodeUrl);
  }

  public boolean containsToRepair(CachedUrlSet cus, String nodeUrl) {
    ArrayList urlArray = (ArrayList)cusToRepair.get(cus.getUrl());
    if (urlArray!=null) {
      return urlArray.contains(nodeUrl);
    }
    return false;
  }

  public void addToDamage(String nodeUrl) {
    nodesWithDamage.add(nodeUrl);
    repository.storeDamagedNodeSet(this);
  }

  public void addToRepair(CachedUrlSet cus, Collection nodeUrls) {
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

  public void removeFromDamage(String nodeUrl) {
    nodesWithDamage.remove(nodeUrl);
    repository.storeDamagedNodeSet(this);
  }

  public void removeFromRepair(CachedUrlSet cus, String nodeUrl) {
    Collection urls = (Collection)cusToRepair.get(cus.getUrl());
    if (urls!=null) {
      urls.remove(nodeUrl);
      if (urls.isEmpty()) {
        cusToRepair.remove(cus.getUrl());
      }
    }
    repository.storeDamagedNodeSet(this);
  }

  public Iterator damageIterator() {
    return nodesWithDamage.iterator();
  }

  public HashSet getDamagedNodes() {
    return nodesWithDamage;
  }

  public void setDamagedNodes(HashSet nodes) {
    this.nodesWithDamage = nodes;
  }

  public HashMap getNodesToRepair() {
    return cusToRepair;
  }

  public ExtMapBean getRepairNodeBean() {
    ExtMapBean bean = new ExtMapBean(cusToRepair);
    return bean;
  }

  public void setRepairNodeBean(ExtMapBean mapBean) {
    cusToRepair = mapBean.getMap();
  }
}
