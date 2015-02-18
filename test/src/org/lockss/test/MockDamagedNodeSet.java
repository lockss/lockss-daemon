/*
 * $Id$
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


package org.lockss.test;

import java.util.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.util.ExtMapBean;

public class MockDamagedNodeSet extends DamagedNodeSet {
  private Set damageSet = new HashSet();


  public ArchivalUnit getArchivalUnit() {
    throw new UnsupportedOperationException("not implemented");
  }

  protected void clear() {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean containsWithDamage(String nodeUrl) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean containsToRepair(CachedUrlSet cus, String nodeUrl) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean hasDamage(CachedUrlSet cus) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean hasDamage(String url) {
    return damageSet.contains(url);
  }

  public boolean hasLocalizedDamage(CachedUrlSet cus) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void clearDamage(CachedUrlSet cus) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void addToDamage(String nodeUrl) {
    damageSet.add(nodeUrl);
  }

  public void addToRepair(CachedUrlSet cus, Collection nodeUrls) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void removeFromDamage(String nodeUrl) {
    damageSet.remove(nodeUrl);
  }

  public void removeFromRepair(CachedUrlSet cus, String nodeUrl) {
    throw new UnsupportedOperationException("not implemented");
  }

  public Iterator damageIterator() {
    throw new UnsupportedOperationException("not implemented");
  }

  public HashSet getDamagedNodes() {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setDamagedNodes(HashSet nodes) {
    throw new UnsupportedOperationException("not implemented");
  }

  public HashMap getNodesToRepair() {
    throw new UnsupportedOperationException("not implemented");
  }

  public ExtMapBean getRepairNodeBean() {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setRepairNodeBean(ExtMapBean mapBean) {
    throw new UnsupportedOperationException("not implemented");
  }

  public String toString() {
    throw new UnsupportedOperationException("not implemented");
//     StringBuffer sb = new StringBuffer();
//     sb.append("[MockDamagedNodeSet: ");
//     sb.append("nodesWithDamage="+cusToRepair);
//     sb.append("]");
//     return sb.toString();
  }

}
