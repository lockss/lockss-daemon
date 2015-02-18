/*
 * $Id$
 */

/*
Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/* Note: This interface comes very close to 'implement Set<PeerIdentity>'.
 * However, it isn't an implementation.

 * Each of the set-like functions can throw an IOException.  Since this
 * exception is not part of the java.util.Set interface, it's not really
 * a Set.
 */


public interface PersistentPeerIdSet extends Iterable<PeerIdentity>  {
  /* To handle direct loading and saving. */
  public void load() throws IOException;
  public void store() throws IOException;
  public void store(boolean release) throws IOException;
  public void release();

  /* These methods are equivalents to the functions of java.util.Set. */
  public boolean add(PeerIdentity pi) throws IOException;
  public boolean addAll(Collection<? extends PeerIdentity> cpi) throws IOException;
  public void clear() throws IOException;
  public boolean contains(Object o) throws IOException;
  public boolean containsAll(Collection<?> co) throws IOException;
  public boolean equals(Object o);
  public int hashCode();
  public boolean isEmpty() throws IOException;
  public Iterator<PeerIdentity> iterator();
  public boolean remove(Object o) throws IOException;
  public boolean removeAll(Collection<?> c) throws IOException;
  public boolean retainAll(Collection<?> c) throws IOException;
  public int size() throws IOException;
  public Object[] toArray() throws IOException;
  // public <T> T[] toArray(T[] a) throws IOException;  // Reinsert if you use it.
}
