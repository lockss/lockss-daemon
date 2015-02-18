/*
 * $Id$
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;

public class CrawlRuleTemplate extends PrintfTemplate {
  int m_ruleKind = 0;


  static final String[] RULE_KIND_STRINGS = {
      "Unknown", "Include", "Exclude", "Include No Match", "Exclude No Match",
      "Include Match, Else Exclude", "Exclude Match, Else Include"
  };

  public CrawlRuleTemplate() {
    m_ruleKind = 1;
  }

  public CrawlRuleTemplate(String templateString) {
    super(templateString);
    String val_str = templateString.substring(0, templateString.indexOf(","));
    m_ruleKind = Integer.valueOf(val_str).intValue();
  }

  int getRuleKind() {
    return m_ruleKind;
  }

  void setRuleKind(int kind) {
    m_ruleKind = kind;
  }

  void setRuleKind(String kindString) {
    m_ruleKind = 0;
    for (int i=0; i< RULE_KIND_STRINGS.length; i++) {
      if(RULE_KIND_STRINGS[i].equals(kindString)) {
        m_ruleKind = i;
        break;
      }
    }

  }

  public String getCrawlRuleString() {
    StringBuffer sb = new StringBuffer();
    sb.append(m_ruleKind);
    String template = getTemplateString();
    if(!StringUtil.isNullString(template)) {
      sb.append(",");
      sb.append(template);
      return sb.toString();
    }
    return null;
  }

  String getKindString() {
    if(m_ruleKind < 1 || m_ruleKind > RULE_KIND_STRINGS.length) {
      return RULE_KIND_STRINGS[0];
    }
    return RULE_KIND_STRINGS[m_ruleKind];
  }

}
