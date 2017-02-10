/*
 * $Id$
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.List;
import org.lockss.util.*;

/**
 * A simple wrapper to decorate a KbartTitle with a health value based on the
 * health of its ArchivalUnits. The health value is placed on the end of the 
 * field value list.
 * 
 * @author Neil Mayo
 */
public class KbartTitleHealthWrapper extends KbartTitle {

  protected static Logger log = Logger.getLogger("KbartTitleHealthWrapper");   
  
  /**
   * A health value, to 2 decimal places.
   */
  private double health;
  
  public KbartTitleHealthWrapper(KbartTitle kbt, double health) {
    super(kbt);
    this.health = NumberUtil.roundToNDecimals(health, 2);
  }
  
  public double getHealth() { return health; }

  @Override
  public List<String> fieldValues() {
    List<String> res = super.fieldValues();
    // Add the health rating to the list of field values; 
    // it can only go on the end of the list right now.
    res.add(""+health);
    return res;
  }
  
  @Override
  public List<String> fieldValues(final List<Field> fieldIds) {
    List<String> res = super.fieldValues(fieldIds);
    res.add(""+health);
    return res;
  }
  
}
