package org.lockss.util;

import java.util.*;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
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

    public void setArguments(Object[] arg) {
      m_arguments.clear();
      for (int i = 0; i < arg.length; i++) {
        m_arguments.add(arg[i]);
      }
    }

    public void addArgument(Object arg) {
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
}
