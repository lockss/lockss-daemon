package org.lockss.devtools.plugindef;

import java.util.*;
import org.lockss.util.*;

public class CrawlRuleTemplate extends PrintfTemplate {
  int m_ruleKind = 0;


  static final String[] RULE_KIND_STRINGS = {
      "Unknown", "Include", "Exclude", "Include No Match", "Exclude No Match",
      "Include Match, Else Exclude", "Exclude Match, Else Include"
  };

  public CrawlRuleTemplate() {
    m_ruleKind = 1;
    m_delimeter = "\n";
  }

  public CrawlRuleTemplate(String templateString, String token) {
    m_delimeter = token;
    if (templateString == null) {
      return;
    }
    StringTokenizer st = new StringTokenizer(templateString, token);
    int num_tokens = st.countTokens();
    if (num_tokens > 0) {
      m_ruleKind = Integer.parseInt(st.nextToken());
      m_format = st.nextToken();
      while (st.hasMoreTokens()) {
        m_tokens.add(st.nextToken());
      }
    }
  }

  int getRuleKind() {
    return m_ruleKind;
  }

  void setRuleKind(int kind) {
    m_ruleKind = kind;
  }

  public String getCrawlRuleString() {
    StringBuffer sb = new StringBuffer();
    sb.append(m_ruleKind);
    String template = getTemplateString();
    if(!StringUtil.isNullString(template)) {
      sb.append(m_delimeter);
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
