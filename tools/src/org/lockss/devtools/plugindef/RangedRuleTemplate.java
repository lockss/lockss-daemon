/*
 * $Id: RangedRuleTemplate.java,v 1.1 2004-09-28 00:54:53 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.devtools.plugindef;

/**
 * <p>RangedRuleTemplate: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 */

public class RangedRuleTemplate extends CrawlRuleTemplate {
  String m_min;
  String m_max;
  boolean m_isNumeric;

  public RangedRuleTemplate(String template, boolean isNumeric) {
    super(template);
    m_isNumeric = isNumeric;
  }

  public RangedRuleTemplate(String template, boolean isNumeric,
                            String min, String max) {
    this(template, isNumeric);
    m_min = min;
    m_max = max;
  }

  public void setMin(String min) {
    this.m_min = min;
  }

  public void setMax(String max) {
    this.m_max = max;
  }

  public String getMin() {
    return m_min;
  }

  public String getMax() {
    return m_max;
  }

  public boolean getNumeric() {
    return m_isNumeric;
  }

  public void setNumeric(boolean numeric) {
    m_isNumeric = numeric;
  }

  public boolean isValidRange() {
    if(m_isNumeric) {
      return Long.parseLong(m_min) < Long.parseLong(m_max);
    }
    else {
      return m_min.compareToIgnoreCase(m_max) < 0;
    }
  }
}
