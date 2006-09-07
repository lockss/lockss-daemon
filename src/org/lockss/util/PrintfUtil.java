/*
 * $Id: PrintfUtil.java,v 1.6 2006-09-07 18:30:55 thib_gc Exp $
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
    PrintfFormat printfFormat = new PrintfFormat(data.getFormat());
    Iterator csIter = printfFormat.getFormatElements().iterator();
    Iterator argIter = data.getArguments().iterator() ;
    ArrayList printfElements = new ArrayList();
    PrintfFormat.ConversionSpecification cs = null;
    char c = 0;

    while (csIter.hasNext()) {
      cs = (PrintfFormat.ConversionSpecification)csIter.next();
      switch (cs.getConversionCharacter()) {
        case CONVERSION_NONE:
          // is plain text
          printfElements.add(new PrintfElement(PrintfElement.FORMAT_NONE,
                                                  cs.getLiteral()));
          break;
        case CONVERSION_PERCENT:
          // is a percent sign; turn into plain text
          printfElements.add(new PrintfElement(PrintfElement.FORMAT_NONE,
                                                "%%"));
          break;
        default:
          // has a conversion code; use it
          printfElements.add(new PrintfElement(cs.getFormat(),
                                                (String)argIter.next()));
          break;
      }
    }

    return (PrintfElement[])printfElements.toArray(new PrintfElement[printfElements.size()]);
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

    public static final String FORMAT_NONE = "\0";

    String m_format = FORMAT_NONE;
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

  public static final char CONVERSION_PERCENT = '%';

  public static final char CONVERSION_NONE = '\0';



}
