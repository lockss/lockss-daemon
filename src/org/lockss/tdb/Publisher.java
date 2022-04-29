/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University,
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.tdb;

import java.io.Serializable;
import java.util.*;

/**
 * <p>
 * A lightweight class (struct) to represent a publisher during TDB processing.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class Publisher implements Serializable {
  
  /**
   * <p>
   * Makes a new publisher instance (useful for tests).
   * </p>
   * 
   * @since 1.67
   */
  protected Publisher() {
    this(new LinkedHashMap<String, String>());
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