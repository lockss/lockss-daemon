/*
 * $Id: MaxSizeRecordingMap.java,v 1.1 2007-01-14 08:52:46 tlipkis Exp $
 */

/*

Copyright (c) 2006-2007 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.collections.map.AbstractMapDecorator;
import java.util.*;

/**
 * A Map decorator that keeps track of the maximum number of items in the
 * map
 */
public class MaxSizeRecordingMap extends AbstractMapDecorator {
  private int maxSize;

  /** Construct a MaxSizeRecordingMap decorating an empty HashMap */
  public MaxSizeRecordingMap() {
    this(new HashMap());
  }

  /** Construct a MaxSizeRecordingMap decorating the supplied map */
  public MaxSizeRecordingMap(Map map) {
    super(map);
    maxSize = size();
  }

  public Object put(Object key, Object val) {
    Object ret = super.put(key, val);
    if (size() > maxSize) maxSize = size();
    return ret;
  }

  /** Return the maximum size of the map */
  public int getMaxSize() {
    return maxSize;
  }
}
