/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.tdb;

import java.util.*;

/**
 * <p>
 * A lightweight class (struct) to represent a publisher during TDB processing.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class Publisher {
  
  /**
   * <p>
   * Makes a new publisher instance (useful for tests).
   * </p>
   * 
   * @since 1.67
   */
  protected Publisher() {
    this(new HashMap<String, String>());
  }
  
  /**
   * <p>
   * Makes a new publisher instance with the given map.
   * </p>
   * 
   * @param map A map of key-value pairs for the publisher.
   * @since 1.67
   */
  public Publisher(Map<String, String>map) {
    this.map = map;
  }
  
  /**
   * <p>
   * Internal storage map.
   * </p>
   * 
   * @since 1.67
   */
  protected Map<String, String> map;
  
  /**
   * <p>
   * Retrieves a value from the internal storage map.
   * </p>
   * 
   * @param key A key.
   * @return The value for the key, or <code>null</code> if none is set.
   * @since 1.67
   */
  public String getArbitraryValue(String key) {
    return map.get(key);
  }
  
  /**
   * <p>
   * Publisher's name (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String NAME = "name";
  
  /**
   * <p>
   * Publisher's name (flag).
   * </p>
   * 
   * @since 1.67
   */
  protected boolean _name = false;
  
  /**
   * <p>
   * Publisher's name (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String name = null;
  
  /**
   * <p>
   * Retrieves the publisher's name.
   * </p>
   * 
   * @return The publisher name.
   */
  public String getName() {
    if (!_name) {
      _name = true;
      name = map.get(NAME);
    }
    return name;
  }
  
}