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
