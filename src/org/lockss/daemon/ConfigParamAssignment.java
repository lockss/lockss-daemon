/*
 * $Id: ConfigParamAssignment.java,v 1.1 2004-01-03 06:14:58 tlipkis Exp $
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

package org.lockss.daemon;

import java.io.*;
import java.util.*;
import org.lockss.util.*;

/**
 * A parameter assignment entry from the title database, containing a
 * ConfigParamDescr, a (possibly default) value and some flags.
 */
public class ConfigParamAssignment {
  private ConfigParamDescr paramDescr;
  private String value = null;
  private boolean isDefault = false;

  /**
   * Create a ConfigParamAssignment for the supplied param descriptor.
   * @param paramDescr the ConfigParamDescr
   */
  public ConfigParamAssignment(ConfigParamDescr paramDescr) {
    if (paramDescr == null) {
      throw new IllegalArgumentException("paramDescr is required");
    }
    this.paramDescr = paramDescr;
  }

  /**
   * Create a ConfigParamAssignment for the supplied param descriptor and
   * value.
   * @param value the value
   */
  public ConfigParamAssignment(ConfigParamDescr paramDescr, String value) {
    this(paramDescr);
    this.value = value;
  }

  /**
   * @return the parameter descriptor
   */
  public ConfigParamDescr getParamDescr() {
    return paramDescr;
  }

  /**
   * Return true if the value is intended as a default only.
   */
  public boolean isDefault() {
    return isDefault;
  }

  /**
   * Set the default flag
   * @param isDefault the new value for the default flag
   */
  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  /**
   * Return the suggested input field value
   * @return the value int
   */
  public String getValue() {
    return value;
  }

  /**
   * Set the suggested input field value
   * @param value the new value
   */
  public void setValue(String value) {
    this.value = value;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(40);
    sb.append("[CPA: ");
    sb.append(paramDescr);
    sb.append(", val:");
    sb.append(value);
    sb.append("]");
    return sb.toString();
  }

  public boolean equals(Object o) {
    if (! (o instanceof ConfigParamAssignment)) {
      return false;
    }
    ConfigParamAssignment opa = (ConfigParamAssignment)o;
    return StringUtil.equalStrings(value, opa.getValue()) &&
      isDefault() == opa.isDefault() &&
      paramDescr.equals(getParamDescr());
  }

  public int hashCode() {
    int hash = 0x46600704;
    hash += paramDescr.hashCode();
    hash += value.hashCode();
    if (isDefault()) {
      hash += 637;
    }
    return hash;
  }

}
