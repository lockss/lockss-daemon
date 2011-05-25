/*
 * $Id: CompositeComparator.java,v 1.1.4.1 2011-05-25 13:50:33 easyonthemayo Exp $
 */

/*

Copyright (c) 2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.kbart;

import java.util.Comparator;

/**
 * A way of composing comparators to allow sorting on multiple criteria.
 * If the first comparator (the major) cannot distinguish between the two 
 * arguments, the second (minor) is consulted. The class can be nested 
 * to provide an arbitrary depth of hierarchical sort criteria.
 * <p>
 * The <code>compose()</code> method allows the composition of a
 * chain of comparators at construction time, for example:
 * <br/>
 * <pre>
 * CompositeComparator = new CompositeComparator(comparator1).compose(comparator2);
 * </pre>
 * <p>
 * TODO: Store comparators as a linked list?
 *
 *
 * @author Neil Mayo
 */
public class CompositeComparator<T> implements Comparator<T> {
  
  /** The primary comparator. */
  protected Comparator<T> major;
  /** The secondary comparator. */
  protected Comparator<T> minor;
  
  /**
   * Create a composite comparator from the supplied comparators.
   * @param major the primary sorting comparator
   * @param minor the secondary sorting comparator
   */
  public CompositeComparator(Comparator<T> major, Comparator<T> minor) {
    this.major = major;
    this.minor = minor;
  }

  /**
   * Convenience constructor which returns a composed comparator
   * that effectively only sorts on a single (major) comparator. 
   * An identity comparator is created as the minor. If the
   * comparator resulting from this method is composed with others,
   * the identity comparator will be removed from the chain.
   *   
   * @param major the major (and only effective) comparator
   */
  public CompositeComparator(Comparator<T> major) {
    this(major, new IdentityComparator<T>());
  }
  
  /**
   * Create an identity composite comparator composed of two identity comparators.
   */
  public CompositeComparator() {
    this(new IdentityComparator<T>());
  }
  
  public static <T> CompositeComparator<T> identityComparator() {
    return new CompositeComparator<T>(new IdentityComparator<T>());
  }
  
  
  public int compare(T o1, T o2) {
    int result = major.compare(o1, o2);
    return result != 0 ? result : minor.compare(o1, o2);
  }
    
  /**
   * Compose this comparator with another one. This comparator becomes the 
   * major, while the argument becomes the minor. If an identity comparator
   * is found in the current instance, it is replaced. 
   * 
   * @param c the new minor comparator
   * @return a new composite comparator
   */
  public CompositeComparator<T> compose(Comparator<T> c) {
    if (this.major instanceof IdentityComparator<?>) {
      this.major = c;
      return this;
    }
    else if (this.minor instanceof IdentityComparator<?>) {
      this.minor = c;
      return this;
    }
    return new CompositeComparator<T>(this, c);
  }

  
  @Override
  public String toString() {
    return major.toString() + " | " + minor.toString();
  }
  
  
  /**
   * An identity comparator which just returns 0 (equal) for every pair of arguments.
   * @author Neil Mayo
   */
  public static class IdentityComparator<T> implements Comparator<T> {
    @Override
    public int compare(T o1, T o2) { return 0; }
    
    @Override
    public String toString() { return "IdentityComparator"; }

  }
  
  
}

