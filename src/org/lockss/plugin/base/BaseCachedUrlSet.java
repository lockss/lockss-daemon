/*
 * $Id: BaseCachedUrlSet.java,v 1.2 2003-05-03 00:45:51 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.base;

import java.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;

/**
 * Abstract base class for CachedUrlSets.
 * Plugins may extend this to get some common CachedUrlSet functionality.
 */
public abstract class BaseCachedUrlSet implements CachedUrlSet {
  protected ArchivalUnit au;
  protected CachedUrlSetSpec spec;

  /**
   * Must invoke this constructor in plugin subclass.
   * @param owner the AU to which it belongs
   * @param spec the CachedUrlSet's spec
   */
  public BaseCachedUrlSet(ArchivalUnit owner, CachedUrlSetSpec spec) {
    this.spec = spec;
    this.au = owner;
  }

  /**
   * Return the CachedUrlSetSpec
   * @return the spec
   */
  public CachedUrlSetSpec getSpec() {
    return spec;
  }

  /**
   * Return the enclosing ArchivalUnit
   * @return the AU
   */
  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  public boolean hasContent() {
    CachedUrl cu = makeCachedUrl(getUrl());
    return (cu == null ? false : cu.hasContent());
  }

  /**
   * Return true if the url falls within the scope of this CachedUrlSet,
   * whether it is present in the cache or not
   * @param url the url to test
   * @return true if is within the scope
   */
  public boolean containsUrl(String url) {
    return (makeCachedUrl(url) != null);
  }

  /**
   * Overridden to return the toString() method of the CachedUrlSetSpec.
   * @return the spec string
   */
  public String toString() {
    return "[BCUS: "+spec+"]";
  }

  public String getUrl() {
    return spec.getUrl();
  }

  public int getType() {
    return CachedUrlSetNode.TYPE_CACHED_URL_SET;
  }

  /**
   * Overrides Object.hashCode().
   * Returns the hashcode of the spec.
   * @return the hashcode
   */
  public int hashCode() {
    return spec.hashCode();
  }

  /**
   * Overrides Object.equals().
   * Returns the equals() of the specs.
   * @param obj the object to compare to
   * @return true if the specs are equal
   */
  public boolean equals(Object obj) {
    if (obj instanceof CachedUrlSet) {
      CachedUrlSet cus = (CachedUrlSet)obj;
      return spec.equals(cus.getSpec());
    } else {
      return false;
    }
  }

/**
 * Iterator over all the elements in a CachedUrlSet
 */
protected class CUSIterator implements Iterator {
  //Stack of flatSetIterators at each tree level
  LinkedList stack = new LinkedList();

  //if null, we have to look for nextElement
  private CachedUrlSetNode nextElement = null;

  public CUSIterator() {
    if (!((spec instanceof RangeCachedUrlSetSpec) &&
          (((RangeCachedUrlSetSpec)spec).getLowerBound()!=null))) {
      nextElement = BaseCachedUrlSet.this;
    }
    stack.addFirst(flatSetIterator());
  }

  public void remove() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean hasNext() {
    return findNextElement() != null;
  }

  public Object next() {
    Object element = findNextElement();
    nextElement = null;

    if (element != null) {
      return element;
    }
    throw new NoSuchElementException();
  }

  /**
   * Does a pre-order traversal of the CachedUrlSet tree
   * @return a {@link CachedUrlSetNode}
   */
  private CachedUrlSetNode findNextElement() {
    if (nextElement != null) {
      return nextElement;
    }
    while (true) {
      if (stack.isEmpty()) {
        return null;
      }
      Iterator it = (Iterator)stack.getFirst();
      if (!it.hasNext()) {
        //this iterator is exhausted, pop from stack
        stack.removeFirst();
      } else {
        CachedUrlSetNode curNode = (CachedUrlSetNode)it.next();

        if (!curNode.isLeaf()) {
          CachedUrlSet cus = (CachedUrlSet)curNode;
          //push the iterator of this child node onto the stack
          stack.addFirst(cus.flatSetIterator());
        }
        nextElement = curNode;
        return nextElement;
      }
    }
  }
}

}
