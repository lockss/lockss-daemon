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
