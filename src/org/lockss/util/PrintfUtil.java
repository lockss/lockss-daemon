/*
 * $Id: PrintfUtil.java,v 1.5 2006-07-10 16:54:46 thib_gc Exp $
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

package org.lockss.util;

import java.util.*;

/**
 * @author Claire Griffin
 */
public class PrintfUtil {

  static public PrintfData stringToPrintf(String printfStr) {
    PrintfData data = new PrintfData();
    int firstQuote = printfStr.indexOf("\"");
    int lastQuote = printfStr.lastIndexOf("\"");
    if(lastQuote > firstQuote) {
      String format = printfStr.substring(firstQuote+1, lastQuote);
      data.setFormat(format);
      String args = printfStr.substring(lastQuote+1);
      StringTokenizer tokenizer = new StringTokenizer(args, ", ", false);
      while (tokenizer.hasMoreTokens()) {
        data.addArgument(tokenizer.nextToken());
      }
    }
    return data;
  }

  static public String printfToString(PrintfData data) {
    StringBuffer printf_buf = new StringBuffer();
    printf_buf.append("\"");
    printf_buf.append(data.getFormat());
    printf_buf.append("\"");
    Collection args = data.getArguments();
    for(Iterator it = args.iterator(); it.hasNext();) {
      printf_buf.append(", ");
      printf_buf.append((String)it.next());
    }
    return printf_buf.toString();
  }

  public static PrintfElement[] printfToElements(PrintfData data) {
    ArrayList printf_elements = new ArrayList();
    PrintfFormat pf = new PrintfFormat(data.getFormat());
    Iterator it_frm = pf.getFormatElements().iterator();
    PrintfFormat.ConversionSpecification cs = null;
    char c = 0;
    Iterator it_args = data.getArguments().iterator();
    while (it_frm.hasNext()) {
      cs = (PrintfFormat.ConversionSpecification) it_frm.next();
      c = cs.getConversionCharacter();
      if (c == '\0') {
        // Last ConversionSpecification is not useful
        if (!cs.getLiteral().equals("") && it_frm.hasNext()) {
          printf_elements.add(new PrintfElement(PrintfElement.NONE, cs.getLiteral()));
        }
      }
      else if (c == '%') {
        printf_elements.add(new PrintfElement(PrintfElement.NONE, "%%"));
      }
      else {
        printf_elements.add(new PrintfElement(cs.getFormat(),
                                              (String) it_args.next()));
      }
    }

    PrintfElement[] ret = new PrintfElement[printf_elements.size()];
    return (PrintfElement[])printf_elements.toArray(ret);
  }

  public static class PrintfData {
    String m_format;
    ArrayList m_arguments;

    public PrintfData() {
      m_format = "";
      m_arguments = new ArrayList();
    }

    public void setFormat(String format) {
      m_format = format;
    }

    public void setArguments(String[] arg) {
      m_arguments.clear();
      for (int i = 0; i < arg.length; i++) {
        m_arguments.add(arg[i]);
      }
    }

    public void addArgument(String arg) {
      m_arguments.add(arg);
    }

    public String getFormat() {
      return m_format;
    }

    public Collection getArguments() {
      return m_arguments;
    }

    public String toString() {
      return printfToString(this);
    }

  }

  public static class PrintfElement {

    public static final String NONE = "\0";

    String m_format = NONE;
    String m_element = "";

    public PrintfElement(String format, String element) {
      m_format = format;
      m_element = element;
    }

    public String getFormat() {
      return m_format;
    }

    public String getElement() {
      return m_element;
    }
  }

}
