/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.iterators.SingletonIterator;
import org.apache.commons.collections4.BidiMap;

/**
 * <p>
 * Provides a concrete implementation of {@link NamespaceContext} (as none are
 * readily available from the JDK or Apache Commons) that encapsulates a
 * one-to-one mapping between prefixes and namespace URIs.
 * </p>
 * <p>
 * The {@link #put(String, String)} and {@link #remove(String)} methods return
 * the instance itself so that calls can be chained to quickly build an instance
 * without a prior map.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67.5
 */
public class OneToOneNamespaceContext implements NamespaceContext {

  /**
   * @since 1.67.5
   */
  protected BidiMap<String, String> bidiMap;

  /**
   * <p>
   * Builds a new instance that initially maps
   * {@link XMLConstants#DEFAULT_NS_PREFIX} to {@link XMLConstants#NULL_NS_URI}.
   * </p>
   * 
   * @since 1.67.5
   */
  public OneToOneNamespaceContext() {
    this.bidiMap = new DualHashBidiMap<String, String>();
    put(XMLConstants.DEFAULT_NS_PREFIX, XMLConstants.NULL_NS_URI);
  }
  
  /**
   * <p>
   * Builds a new instance that initially has the same mappings as the given
   * map, and adds a mapping from {@link XMLConstants#DEFAULT_NS_PREFIX} to
   * {@link XMLConstants#NULL_NS_URI} if there is no mapping for it initially.
   * </p>
   * 
   * @param map
   *          An initial mapping of prefixes to namespace URIs.
   * @since 1.67.5
   */
  public OneToOneNamespaceContext(Map<String, String> map) {
    this.bidiMap = new DualHashBidiMap<String, String>(map);
    if (!bidiMap.containsKey(XMLConstants.DEFAULT_NS_PREFIX)) {
      put(XMLConstants.DEFAULT_NS_PREFIX, XMLConstants.NULL_NS_URI);
    }
  }
  
  /**
   * @since 1.67.5
   */
  public OneToOneNamespaceContext put(String prefix, String namespaceURI) {
    bidiMap.put(prefix, namespaceURI);
    return this;
  }

  /**
   * @since 1.67.5
   */
  public OneToOneNamespaceContext remove(String prefix) {
    bidiMap.remove(prefix);
    return this;
  }

  @Override
  public String getNamespaceURI(String prefix) {
    return bidiMap.get(prefix);
  }

  @Override
  public String getPrefix(String namespaceURI) {
    return bidiMap.getKey(namespaceURI);
  }

  @Override
  public Iterator<String> getPrefixes(String namespaceURI) {
    return new SingletonIterator<String>(getPrefix(namespaceURI));
  }
  
}
