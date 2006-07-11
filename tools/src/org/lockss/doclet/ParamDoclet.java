/*
 * $Id: ParamDoclet.java,v 1.7 2006-07-11 17:42:24 thib_gc Exp $
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

package org.lockss.doclet;

import org.lockss.util.StringUtil;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import com.sun.javadoc.*;

/**
 * A JavaDoc doclet that prints configuration parameter information.
 *
 */
public class ParamDoclet {

  private static PrintStream out = null;
  private static boolean closeOut = false;

  public static boolean start(RootDoc root) {

    handleOptions(root);

    if (out == null) {
      out = System.out;
    }

    ClassDoc[] classDocs = root.classes();

    HashMap params = new HashMap();
    TreeMap sortedParams = new TreeMap();

    for (int i = 0; i < classDocs.length; i++) {
      ClassDoc classDoc = classDocs[i];
      String className = classDoc.qualifiedName();

      FieldDoc[] fields = classDoc.fields();

      for (int j = 0; j < fields.length; j++) {
	FieldDoc field = fields[j];

	String name = field.name();

	if (isParam(name)) {

	  String key = getParamKey(classDoc, name);
	  ParamInfo info = (ParamInfo)params.get(key);

	  if (info == null) {
	    info = new ParamInfo();
	    params.put(key, info);
	  }

	  if (name.startsWith("PARAM_")) {
	    // This is a PARAM, not a DEFAULT
	    if (info.usedIn.size() == 0) {
	      // This is the first occurance we've encountered.
	      Object value = field.constantValue();

	      if (value instanceof String) {
		String paramName = escapeName((String)value);
		String comment = field.getRawCommentText();
		info.paramName = paramName;
		info.comment = comment;
		info.usedIn.add(className);
		// Add to the sorted map we'll use for printing.
		sortedParams.put(paramName, info);
	      }
	    } else {
	      // We've already visited this parameter before, this is
	      // just another use.
	      info.usedIn.add(className);
	    }
	  } else if (name.startsWith("DEFAULT_")) {
	    info.defaultValue = getDefaultValue(field, root);
	  }
	}
      }
    }

    printDocHeader();

    out.println("<h3>" + sortedParams.size() + " total parameters</h3>");

    for (Iterator iter = sortedParams.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      ParamInfo info = (ParamInfo)sortedParams.get(key);
      printParamInfo(info);
    }

    printDocFooter();

    if (closeOut) {
      out.close();
    }

    return true;
  }

  // The simplest possible way to escape < and > in param names.
  private static String escapeName(String name) {
    String returnVal = StringUtil.replaceString(name, "<", "&lt;");
    return StringUtil.replaceString(returnVal, ">", "&gt;");
  }

  private static void printDocHeader() {
    out.println("<html>");
    out.println("<head>");
    out.println(" <title>Parameters</title>");
    out.println(" <style type=\"text/css\">");
    out.println("  .paramName { font-weight: bold; font-family: sans-serif;");
    out.println("      font-size: 14pt; }");
    out.println("  .defaultValue { font-family: monospace; font-size: 14pt; }");
    out.println("  table { border-collapse: collapse; margin-left: 20px;");
    out.println("      margin-right: 20px; padding: 0px; width: auto; }");
    out.println("  tr { margin: 0px; padding: 0px; border: 0px; }");
    out.println("  td { margin: 0px; padding-left: 6px; padding-right: 6px;");
    out.println("      border: 0px; padding-left: 0px; padding-top: 0px; padding-right: 0px;}");
    out.println("  td.paramHeader { padding-top: 5px; }");
    out.println("  td.comment { }");
    out.println("  td.usedIn { font-family: monospace; }");
    out.println("  td.header { padding-left: 30px; padding-right: 10px; font-style: italic; text-align: right; }");

    out.println(" </style>");
    out.println("</head>");
    out.println("<body>");
    out.println("<div align=\"center\">");
    out.println("<h1>LOCKSS Configuration Parameters</h1>");
    out.println("<table>");
    out.flush();
  }

  private static void printDocFooter() {
    out.println("</table>");
    out.println("</div>");
    out.println("</body>");
    out.println("</html>");
    out.flush();
  }

  private static void printParamInfo(ParamInfo info) {
    out.println("<tr>\n  <td colspan=\"2\" class=\"paramHeader\">");
    out.print("    <span class=\"paramName\">" +
		info.paramName.trim() + "</span> &nbsp; ");
    out.print("<span class=\"defaultValue\">[");
    out.print(info.defaultValue == null ?
	      "" : info.defaultValue.toString());
    out.println("]</span>\n  </td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("  <td class=\"header\" valign=\"top\">Comment:</td>");
    out.print("  <td class=\"comment\">");
    if (info.comment.trim().length() == 0) {
      out.print("");
    } else {
      out.print(info.comment.trim());
    }
    out.println("</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("  <td class=\"header\" valign=\"top\">Used in:</td>");
    out.println("  <td class=\"usedIn\">");
    for (Iterator iter = info.usedIn.iterator(); iter.hasNext();) {
      out.println( (String)iter.next() + "<br/>");
    }
    out.println("  </td>");
    out.println("</tr>");
    //    out.println("<tr><td colspan=\"3\">&nbsp;</td></tr>");

    out.flush();
  }

  /**
   * Return true if the specified string is a parameter name.
   */
  private static boolean isParam(String s) {
    return (s.startsWith("PARAM_") || s.startsWith("DEFAULT_"));
  }

  /**
   * Given a parameter or default name, return the key used to look up
   * its info object in the unsorted hashmap.
   */
  private static String getParamKey(ClassDoc doc, String s) {
    StringBuffer sb = new StringBuffer(doc.qualifiedName() + ".");
    if (s.startsWith("DEFAULT_")) {
      sb.append(s.replaceFirst("DEFAULT_", ""));
    } else if (s.startsWith("PARAM_")) {
      sb.append(s.replaceFirst("PARAM_", ""));
    } else {
      sb.append(s);
    }
    return sb.toString();
  }

  /**
   * Cheesily use reflection to obtain the default value.
   */
  public static String getDefaultValue(FieldDoc field, RootDoc root) {
    String defaultVal = null;
    try {
      ClassDoc classDoc = field.containingClass();

      Class c = Class.forName(classDoc.qualifiedName());
      Field fld = c.getDeclaredField(field.name());
      fld.setAccessible(true);
      Class cls = fld.getType();
      if (int.class == cls) {
	defaultVal = (new Integer(fld.getInt(null))).toString();
      } else if (long.class == cls) {
	long timeVal = fld.getLong(null);
	defaultVal = timeVal + " (" +
	  StringUtil.timeIntervalToString(timeVal) + ")";
      } else if (boolean.class == cls) {
	defaultVal = (new Boolean(fld.getBoolean(null))).toString();
      } else {
	try {
	  // This will throw NPE if the field isn't static; don't know how
	  // to get initial value in that case
	  Object dval = fld.get(null);
	  defaultVal = (dval != null) ? dval.toString() : "(null)";
	} catch (NullPointerException e) {
	  defaultVal = "(unknown: non-static default)";
	}
      }
    } catch (Exception e) {
      root.printError(field.position(), field.name() + ": " + e);
      root.printError(StringUtil.stackTraceString(e));
    }

    return defaultVal;
  }

  /**
   * Required for Doclet options.
   */
  public static int optionLength(String option) {
    if (option.equals("-o")) {
      return 2;
    } else if (option.equals("-d")) {
      return 2;
    }
    return 0;
  }

  public static boolean validOptions(String[][] options,
				     DocErrorReporter reporter) {
    return true;
  }

  private static boolean handleOptions(RootDoc root) {
    String outDir = null;

    String[][] options = root.options();

    for (int i = 0 ; i < options.length; i++) {
      if (options[i][0].equals("-d")) {
	outDir = options[i][1];
      } else if (options[i][0].equals("-o")) {
	String outFile = options[i][1];
	try {
	  File f = null;
	  if (outDir != null) {
	    f = new File(outDir, outFile);
	  } else {
	    f = new File(outFile);
	  }

	  out = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)));
	  closeOut = true;
	  return true;
	} catch (IOException ex) {
	  root.printError("Unable to open output file: " + outFile);
	}
      }
    }
    return false;
  }

  /**
   * Simple wrapper class to hold information about parameters.
   */
  private static class ParamInfo {
    public String paramName = "";
    public Object defaultValue = null;
    public String comment = "";
    // Sorted list of uses.
    public Set usedIn = new TreeSet();
  }

}
