package org.lockss.devtools.plugindef;

import java.util.*;
import org.lockss.util.*;
import org.lockss.util.PrintfUtil.*;
import org.lockss.util.PrintfFormat.*;

public class PrintfTemplate {
  String m_format = null;
  ArrayList m_tokens = new ArrayList();

  public PrintfTemplate() {
  }

  public PrintfTemplate(String templateString) {
    if (templateString == null) {
      return;
    }
    PrintfData data = PrintfUtil.stringToPrintf(templateString);
    m_format = data.getFormat();
    m_tokens = new ArrayList(data.getArguments());
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
    StringBuffer sb = new StringBuffer("\"");
    sb.append(m_format);
    sb.append("\"");
    for(Iterator it = m_tokens.iterator(); it.hasNext();) {
      sb.append(", ");
      sb.append((String)it.next());
    }
    return sb.toString();
  }

  public List getPrintfElements() {
    PrintfData data = PrintfUtil.stringToPrintf(getTemplateString());
    return ListUtil.fromArray(PrintfUtil.printfToElements(data));
  }

  public String toString() {
    return getViewableTemplate();
  }
}
