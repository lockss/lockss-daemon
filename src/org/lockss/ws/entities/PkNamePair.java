/*
 * $Id$
 */

/*

 Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.entities;

/**
 * A primary key/name pair.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class PkNamePair implements Comparable<PkNamePair> {
  private Long pk;
  private String name;

  /**
   * Default constructor.
   */
  public PkNamePair() {
  }

  /**
   * Constructor.
   * 
   * @param pk
   *          A Long with the primary key.
   * @param name
   *          A String with the name.
   */
  public PkNamePair(Long pk, String name) {
    this.pk = pk;
    this.name = name;
  }

  public Long getPk() {
    return pk;
  }
  public void setPk(Long pk) {
    this.pk = pk;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Provides an indication of whether two of these objects are the same.
   * 
   * @param one
   *          An object with the first object.
   * @param other
   *          An object with the other object.
   * @return a boolean with <code>true</code> if both objects are the same,
   *         <code>false</code> otherwise.
   */
  private static boolean equals(Object one, Object other) {
    return (one == null && other == null) || (one != null && one.equals(other));
  }

  /**
   * Provides an indication of whether some other object is equal to this one.
   * 
   * @param other
   *          An object with which to compare.
   * @return a boolean with <code>true</code> if both objects are the same,
   *         <code>false</code> otherwise.
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof PkNamePair && equals(pk, ((PkNamePair) other).pk) &&
      equals(name, ((PkNamePair) other).name);
  }

  /**
   * Provides a hash code value for the object.
   * 
   * @return an int with the hash code value for this object.
   */
  @Override
  public int hashCode() {
    if (pk == null) {
      if (name == null) return 643;
      return name.hashCode() + 1;
    }
    if (name == null) return pk.hashCode() + 2;
    return pk.hashCode() * name.hashCode();
  }

  /**
   * Compares this object with the specified object for order. Returns a
   * negative integer, zero, or a positive integer as this object is less than,
   * equal to, or greater than the specified object.
   * 
   * @param other
   *          A PkNamePair with the other object used in the comparison.
   * @return an int that is negative, zero, or positive as this object is to be
   *         sorted before, the same or after the other object.
   */
  @Override
  public int compareTo(PkNamePair other) {
    // Emptier objects appear earlier in the sort.
    if (other == null) {
      return 1;
    }

    int result = name.compareTo(other.getName());

    if (result == 0) {
      result = pk.compareTo(other.getPk());
    }

    return result;
  }

  @Override
  public String toString() {
    return "[PkNamePair pk=" + pk + ", name=" + name + "]";
  }
}
