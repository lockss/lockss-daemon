package org.lockss.devtools.plugindef;

import java.util.*;

public class PrintfTemplate {
  String m_format = null;
  ArrayList m_tokens = new ArrayList();
  String m_delimeter = ", ";

  public PrintfTemplate() {
  }

  public PrintfTemplate(String templateString, String token) {
    if (templateString == null) {
      return;
    }
    this.m_delimeter = token;
    StringTokenizer st = new StringTokenizer(templateString, token);
    int num_tokens = st.countTokens();
    if (num_tokens > 0) {
      m_format = st.nextToken();
      while (st.hasMoreTokens()) {
        m_tokens.add(st.nextToken());
      }
    }
  }


  public String getFormat() {
    return m_format;
  }

  public List getTokens() {
    return m_tokens;
  }

  public void setFormat(String newformat) {
    m_format = newformat;
  }

  public void setParameters(String parameters) {
    m_tokens.clear();
    StringTokenizer tokenizer = new StringTokenizer(parameters, ", ", false);
    while(tokenizer.hasMoreTokens()) {
      m_tokens.add(tokenizer.nextToken());
    }
  }

  public void addToken(int pos, String token) {
    m_tokens.add(pos,token);
  }


  public void setTokens(List newtokens) {
    m_tokens = (ArrayList)newtokens;
  }

  public String getTokenString() {
    StringBuffer buf = new StringBuffer("");
    for(Iterator it = m_tokens.iterator(); it.hasNext();) {
      buf.append(", ");
      buf.append(it.next());
    }
    return buf.toString();
  }

  public String getViewableTemplate() {
    if (m_format != null) {
      StringBuffer buf = new StringBuffer("\"");
      buf.append(m_format);
      buf.append("\"");
      buf.append(getTokenString());
      return buf.toString();
    }
    return "NONE";
  }

  public String getTemplateString() {
    if(m_format == null) return "";
    StringBuffer sb = new StringBuffer(m_format);
    for(Iterator it = m_tokens.iterator(); it.hasNext();) {
      sb.append(m_delimeter);
      sb.append((String)it.next());
    }
    return sb.toString();
  }

  public String toString() {
    return getViewableTemplate();
  }
}
