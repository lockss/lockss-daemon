/*
 * $Id$
 *

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Copyright 2006 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lockss.util;

import java.util.*;

/**
 * A TreeSet that ensures it never grows beyond a max size.  
 * <code>last()</code> is removed if the <code>size()</code> 
 * gets bigger then <code>getMaxSize()</code>
 */
public class BoundedTreeSet<E> extends TreeSet<E> {
  private int maxSize = Integer.MAX_VALUE;
  public BoundedTreeSet(int maxSize) {
    super();
    this.setMaxSize(maxSize);
  }
  public BoundedTreeSet(int maxSize, Collection<? extends E> c) {
    super(c);
    this.setMaxSize(maxSize);
  }
  public BoundedTreeSet(int maxSize, Comparator<? super E> c) {
    super(c);
    this.setMaxSize(maxSize);
  }
  public BoundedTreeSet(int maxSize, SortedSet<E> s) {
    super(s);
    this.setMaxSize(maxSize);
  }
  public int getMaxSize() {
    return maxSize;
  }
  public void setMaxSize(int max) {
    maxSize = max;
    adjust();
  }
  private void adjust() {
    while (maxSize < size()) {
      remove(last());
    }
  }
  public boolean add(E item) {
    boolean out = super.add(item);
    adjust();
    return out;
  }
  public boolean addAll(Collection<? extends E> c) {
    boolean out = super.addAll(c);
    adjust();
    return out;
  }
}
