/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;

/**
 * A parameter assignment entry from the title database, containing a
 * ConfigParamDescr, a value and an {@link #isEditable()} boolean which
 * defaults to true if the value is empty, else false.  It can be
 * explicitly set with {@link #setEditable(boolean)}.
 */
public class ConfigParamAssignment {
  private ConfigParamDescr paramDescr;
  private String value = null;
  private boolean isEditable = true;

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
    this.setValue(value);
  }

  /**
   * @return the parameter descriptor
   */
  public ConfigParamDescr getParamDescr() {
    return paramDescr;
  }

  /**
   * Return true if the value is editable.
   */
  public boolean isEditable() {
    return isEditable;
  }

  /**
   * Set the editable flag
   * @param isEditable the new value for the editable flag
   */
  public void setEditable(boolean isEditable) {
    this.isEditable = isEditable;
  }

  /**
   * Return the suggested input field value
   * @return the value int
   */
  public String getValue() {
    return value;
  }

  /**
   * Set the input field value, reset the default editability (true iff
   * value is empty)
   * @param value the new value
   */
  public void setValue(String value) {
    this.value = value;
    isEditable = StringUtil.isNullString(value);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(40);
    if (isEditable()) {
      sb.append("[CPA:E: ");
    } else {
      sb.append("[CPA: ");
    }
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
      isEditable() == opa.isEditable() &&
      paramDescr.equals(getParamDescr());
  }

  public int hashCode() {
    int hash = 0x46600704;
    hash += paramDescr.hashCode();
    hash += value.hashCode();
    if (isEditable()) {
      hash += 637;
    }
    return hash;
  }

}
