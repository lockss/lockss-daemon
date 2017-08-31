/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import com.sun.javadoc.*;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import org.lockss.util.*;

/**
 * A JavaDoc doclet that prints configuration parameter information.
 *
 * static String variables with the prefix <bold><tt>PARAM_</tt></bold>
 * designate configuration parameters.  The value of the variable is the
 * name of the parameter.  The default value of the parameter is the value
 * of the similarly named variable with the prefix
 * <bold><tt>DEFAULT_</tt></bold>, if any.  The variable's javadoc is the
 * parameter's documentation.  That javadoc may contain one or more
 * &#064;ParamCategory tags, whose value is a comma-separated list of
 * category names.  &#064;ParamCategory may also appear in the javadoc for
 * the class, in which case it establishes the default category for all
 * param definitions in the class.  &#064;ParamCategoryDoc <i>name</i>
 * <i>string</i> provides an explanation of the category.
 *
 * The &#064;ParamRelevance tag specifies the relevance/importance of the
 * parameter.  It should be one of:<ul>
 * <li>Required - Must be set in order for daemon to run; there is no
 *   sensible default value.</li>
 * <li>Common - Commonly used.</li>
 * <li>LessCommon - Less commonly used.</li>
 * <li>Unknown - Relevance of parameter not specified.  This is the default
 *   if no relevance is specified.</li>
 * <li>Rare - Rarely used.</li>
 * <li>BackwardCompatibility - Used to restore some aspect of the daemon's
 *   behavior to what is was before some change or new feature was
 *   implemented.  Included primarily as a failsafe in case of unforseen
 *   consequences of the change.</li>
 * <li>Obsolescent - Enables or controls a feature that is no longer used.</li>
 * <li>Never - Included for some internal purpose, should never be set by
 *   users.</li>
 * </ul>
 *
 * Invoke the doclet with <tt>-f Category</tt> to have the parameters
 * grouped by Category, with <tt>-f Relevance</tt> to have the parameters
 * grouped by Relevance.
 */
public class ParamDoclet {

  private static PrintStream out = null;
  private static boolean closeOut = false;
  private String releaseHeader = null;

  RootDoc root;
  String classParamCategory;	 // category of class being processed
  // Maps category name to set of (ParamInfo of) params belonging to it
  SetValuedMap<String,ParamInfo> categoryMap =
    new HashSetValuedHashMap<String,ParamInfo>();
  // Maps category name to its doc string
  Map<String,String> catDocMap = new HashMap<String,String>();

  // One of alpha, category, relevance
  String fmt = "alpha";

  // Maps (key derived from) symbol name (PARAM_XXX) to ParamInfo
  Map<String,ParamInfo> params = new HashMap<String,ParamInfo>();
  // Maps param name to ParamInfo
  TreeMap<String,ParamInfo> sortedParams = new TreeMap<String,ParamInfo>();

  public static boolean start(RootDoc root) {
    return new ParamDoclet(root).doStart();
  }

  ParamDoclet(RootDoc root) {
    this.root = root;
  }

  boolean doStart() {
    handleOptions();

    if (out == null) {
      out = System.out;
    }

    processClassDocs();

    printDoc();

    if (closeOut) {
      out.close();
    }

    return true;
  }

  void processClassDocs() {
    for (ClassDoc classDoc : root.classes()) {
      classParamCategory = null;
      for (Tag tag : classDoc.tags()) {
	switch (tag.name()) {
	case "@ParamCategory":
	  classParamCategory = tag.text().trim();
	  break;
	case "@ParamCategoryDoc":
	  processCategoryDoc(tag);
	  break;
	}
      }
      for (FieldDoc field : classDoc.fields()) {
	String name = field.name();

	if (isParam(name)) {

// 	  for (AnnotationDesc ad : field.annotations()) {
// 	    root.printNotice(field.position(), "annotation: " + ad);
// 	  }

	  addParam(root, classDoc, field);
	}
      }
    }
  }

  void addParam(RootDoc root, ClassDoc classDoc, FieldDoc field) {
    String className = classDoc.qualifiedName();
    String name = field.name();
    String key = getParamKey(classDoc, name);
    ParamInfo info = params.get(key);
    if (info == null) {
      info = new ParamInfo();
      params.put(key, info);
    }

    if (name.startsWith("PARAM_")) {
      // This is a PARAM, not a DEFAULT
      Object value = field.constantValue();

      if (!(value instanceof String)) {
	root.printWarning(field.position(), "Non-string value for " + name);
	root.printWarning("value: " + value);
	if (value != null) {
	  root.printWarning("value class: " + value.getClass());
	}
	return;
      }

      if (info.definedIn.size() == 0) {
	// This is the first occurance we've encountered.
	  String paramName = (String)value;
	  info.paramName = paramName;
	  info.setComment(field.commentText());
	  info.addDefinedIn(className);
	  // Add to the sorted map we'll use for printing.
	  sortedParams.put(paramName, info);

	  boolean hasCat = false;
	  for (Tag tag : field.tags()) {
	    switch (tag.name()) {
	    case "@ParamCategory":
	      for (String cat :
		     StringUtil.breakAt(tag.text(), ",", -1, true, true)) {
		info.addCategory(cat);
		hasCat = true;
	      }

	      break;
	    case "@ParamCategoryDoc":
	      processCategoryDoc(tag);
	      break;
	    case "@ParamRelevance":
	      info.setRelevance(field, tag.text().trim());
	      break;
	    default: info.addTag(tag);
	    }
// 	    root.printNotice("tag: " + tag);
// 	    root.printNotice("kind: " + tag.kind());
// 	    root.printNotice("name: " + tag.name());
// 	    root.printNotice("text: " + tag.text());
// 	    root.printNotice(tag.position(), "pos: " + tag.position());
	  }
	  if (!hasCat && classParamCategory != null) {
	    info.addCategory(classParamCategory);
	  }

      } else {
	// We've already visited this parameter before, this is
	// just another definer.
	info.addDefinedIn(className);
      }
    } else if (name.startsWith("DEFAULT_")) {
      info.defaultValue = getDefaultValue(field, root);
    }
  }

  void processCategoryDoc(Tag tag) {
    String[] sa = divideAtWhite(tag.text());
    String cat = sa[0];
    if (catDocMap.containsKey(cat)) {
      root.printWarning(tag.position(),
			"Duplicate Category doc for " + cat);
    } else {
      catDocMap.put(cat, sa[1]);
    }
  }

  void printDoc() {
    printDocHeader();

    out.println("<h3>" + sortedParams.size() + " total parameters</h3>");

    switch (fmt) {
    case "alpha":
      printDocV1();
      break;
    case "category":
      printDocByCat();
      break;
    case "relevance":
      printDocByRel();
      break;
    }
    printDocFooter();
  }

  private void printDocHeader() {
    out.println("<html>");
    out.println("<head>");
    out.println(" <title>Parameters</title>");
    out.println(" <style type=\"text/css\">");
    out.println("  .sectionName { font-weight: bold; font-family: sans-serif;");
    out.println("      font-size: 16pt; }");
    out.println("  .paramName { font-weight: bold; font-family: sans-serif;");
    out.println("      font-size: 14pt; }");

    out.println("  .defaultValue { font-family: monospace; font-size: 14pt; }");
    out.println("  table { border-collapse: collapse; margin-left: 20px;");
    out.println("      margin-right: 20px; padding: 0px; width: auto; }");
    out.println("  tr { margin: 0px; padding: 0px; border: 0px; }");
    out.println("  td { margin: 0px; padding-left: 6px; padding-right: 6px;");
    out.println("      border: 0px; padding-left: 0px; padding-top: 0px; padding-right: 0px;}");
    out.println("  td.paramHeader { padding-top: 5px; }");
    out.println("  td.sectionHeader { padding-top: 5px; font-size: 16pt; }");
    out.println("  td.comment { }");
    out.println("  td.categoryComment { padding-left: 20px; }");
    out.println("  td.definedIn { font-family: monospace; }");
    out.println("  td.header { padding-left: 30px; padding-right: 10px; font-style: italic; text-align: right; }");

    out.println(" </style>");
    out.println("</head>");
    out.println("<body>");
    out.println("<div align=\"center\">");
    out.println("<h1>LOCKSS Configuration Parameters</h1>");
    switch (fmt) {
    case "category":
      out.println("<h2>by Category</h2>");
      break;
    case "relevance":
      out.println("<h2>by Relevance</h2>");
      break;
    default:
    }
    if (!StringUtil.isNullString(releaseHeader)) {
      out.println("<h2>");
      out.println(releaseHeader);
      out.println("</h2>");
    }
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


  private void printDocV1() {
    for (ParamInfo info : sortedParams.values()) {
      printParamInfo(info);
    }
  }

  private  void printDocByCat() {
    Set<ParamInfo> done = new HashSet<ParamInfo>();
//     System.err.println("all cat: " +
// 		       CollectionUtil.asSortedList(categoryMap.keySet()));

    for (String cat : CollectionUtil.asSortedList(categoryMap.keySet())) {
      printCategoryHeader(cat);
      for (Relevance rel : Relevance.values()) {
	Set<ParamInfo> catSet = categoryMap.get(cat);
	for (ParamInfo info :
	       CollectionUtil.asSortedList(catSet)) {
	  if (info.getRelevance() == rel) {
	    printParamInfo(info);
	    done.add(info);
	  }
	}
      }
    }

    printCategoryHeader("None");
    for (Relevance rel : Relevance.values()) {
      for (ParamInfo info : sortedParams.values()) {
	if (info.getRelevance() == rel && !done.contains(info)) {
	  printParamInfo(info);
	  done.add(info);
	}
      }
    }
  }

  private  void printDocByRel() {
//     System.err.println("all cat: " +
// 		       CollectionUtil.asSortedList(categoryMap.keySet()));

    for (Relevance rel : Relevance.values()) {
      printRelevanceHeader(rel);
      for (ParamInfo info : sortedParams.values()) {
	if (info.getRelevance() == rel) {
	  printParamInfo(info);
	}
      }
    }
  }

  private void printCategoryHeader(String cat) {
    out.println("<tr>\n  <td colspan=\"2\" class=\"sectionHeader\">");
    out.print("    <span class=\"sectionName\" id=\"" + cat + "\">" +
	      "Category: " + cat + "</span></td>");
    out.println("</tr>");
    if (catDocMap.containsKey(cat)) {
      out.println("<tr>\n  <td colspan=\"2\" class=\"categoryComment\">");
      out.print(catDocMap.get(cat));
      out.print("</td>");
      out.println("</tr>");
    }
  }

  private void printRelevanceHeader(Relevance rel) {
    out.println("<tr>\n  <td colspan=\"2\" class=\"sectionHeader\">");
    out.print("    <span class=\"sectionName\" id=\"" + rel + "\">" +
	      "Relevance: " + rel + "</span></td>");
    out.println("</tr>");
    if (true) {
      out.println("<tr>\n  <td colspan=\"2\" class=\"categoryComment\">");
      out.print(rel.getExplanation());
      out.print("</td>");
      out.println("</tr>");
    }
  }

  String htmlParamName(ParamInfo info) {
    // Angle brackets in param names are used for meta-symbols.
    // Turn <foo> into <i>foo</i>
    String res = info.getParamName().replaceAll("<([a-zA-Z_-]+)>", "<i>$1</i>");
    return res;
//     String res = HtmlUtil.htmlEncode(info.getParamName().trim());
  }

  private void printParamInfo(ParamInfo info) {
    String pnameId = HtmlUtil.htmlEncode(info.getParamName().trim());
    String pname = htmlParamName(info);

    out.println("<tr>\n  <td colspan=\"2\" class=\"paramHeader\">");
    out.print("    <span class=\"paramName\" id=\"" + pnameId + "\">" +
	      pname + "</span> &nbsp; ");
    out.print("<span class=\"defaultValue\">[");
    out.print(info.defaultValue == null ?
	      "" : HtmlUtil.htmlEncode(info.defaultValue));
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
    for (Tag tag : info.tags) {
      out.print("<br>");
      out.print(tag.name());
      out.print(" ");
      out.print(tag.text());
    }
    out.println("</td>");
    out.println("</tr>");

    if (!info.getCategories().isEmpty()) {
      out.println("<tr>");
      out.println("  <td class=\"header\" valign=\"top\">Categories:</td>");
      out.print("  <td class=\"categories\">");
      for (String cat : info.getCategories()) {
	out.println( cat + "<br/>");
      }
      out.println("</td>");
      out.println("</tr>");
    }

    if (info.getRelevance() != Relevance.Unknown && !fmt.equals("relevance")) {
      out.println("<tr>");
      out.println("  <td class=\"header\" valign=\"top\">Relevance:</td>");
      out.print("  <td class=\"relevance\">");
      out.println(info.getRelevance());
      out.println("<br/>");
      out.println("</td>");
      out.println("</tr>");
    }

    out.println("<tr>");
    out.println("  <td class=\"header\" valign=\"top\">Defined in:</td>");
    out.println("  <td class=\"definedIn\">");
    for (String def : info.getDefinedIn()) {
      out.println( def + "<br/>");
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

      Class c = Class.forName(getClassName(classDoc));
      Field fld = c.getDeclaredField(field.name());
      fld.setAccessible(true);
      Class cls = fld.getType();
      if (int.class == cls) {
	defaultVal = (new Integer(fld.getInt(null))).toString();
      } else if (long.class == cls) {
	long timeVal = fld.getLong(null);
	if (timeVal > 0) {
	  defaultVal = timeVal + " (" +
	    StringUtil.timeIntervalToString(timeVal) + ")";
	} else {
	  defaultVal = Long.toString(timeVal);
	}
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

  /** Convert the package-qualified name of a (possibly) nested class into
   * the actual class name.  I.e., replace dots separating a nested class
   * from its parent with $ */
  static String getClassName(ClassDoc classDoc) {
    String cname = classDoc.qualifiedName();
    ClassDoc parentDoc;
    while ((parentDoc = classDoc.containingClass()) != null) {
      cname = StringUtil.replaceLast(cname, ".", "$");
      classDoc = parentDoc;
    }
    return cname;
  }

  /**
   * Required for Doclet options.
   */
  public static int optionLength(String option) {
    switch (option) {
    case "-o": return 2;
    case "-d": return 2;
    case "-h": return 2;
    case "-f": return 2;
    default: return 0;
    }
  }

  public static boolean validOptions(String[][] options,
				     DocErrorReporter reporter) {
    return true;
  }

  private boolean handleOptions() {
    boolean ok = false;
    String outDir = null;

    String[][] options = root.options();

    for (int i = 0 ; i < options.length; i++) {
      String opt = options[i][0];
      String val = null;
      if (optionLength(opt) >= 2) {
	val = options[i][1];
      }
      switch (opt) {
      case "-o":
	String outFile = val;
	try {
	  File f = null;
	  if (outDir != null) {
	    f = new File(outDir, outFile);
	  } else {
	    f = new File(outFile);
	  }
	  out =
	    new PrintStream(new BufferedOutputStream(new FileOutputStream(f)));
	  closeOut = true;
	  ok = true;
	} catch (IOException ex) {
	  root.printError("Unable to open output file: " + outFile);
	}
	break;
      case "-d":
	outDir = val;
	break;
      case "-h":
	releaseHeader = val;
	break;
      case "-f":
	fmt = val.toLowerCase().trim();
	if (StringUtil.isNullString(fmt)) {
	  fmt = "Alpha";
	}
	break;
      default:
      }
    }
    return ok;
  }

  /**
   * for use by subclasses which have two part tag text.
   */
  String[] divideAtWhite(String text) {
    String[] sa = new String[2];
    int len = text.length();
    // if no white space found
    sa[0] = text;
    sa[1] = "";
    for (int inx = 0; inx < len; ++inx) {
      char ch = text.charAt(inx);
      if (Character.isWhitespace(ch)) {
	sa[0] = text.substring(0, inx);
	for (; inx < len; ++inx) {
	  ch = text.charAt(inx);
	  if (!Character.isWhitespace(ch)) {
	    sa[1] = text.substring(inx, len);
	    break;
	  }
	}
	break;
      }
    }
    return sa;
  }

  enum Relevance {
    Required("Must be set in order for daemon to run; there is no sensible default value"),
    Common("Commonly used"),
    LessCommon("Less commonly used"),
    Unknown("Relevance of parameter not specified"),
    Rare("Rarely used"),
    BackwardCompatibility("Used to restore some aspect of the daemon's behavior to what is was before some change or new feature was implemented.  Included primarily as a failsafe in case of unforseen consequences of the change."),
    Obsolescent("Enables or controls a feature that is no longer used"),
    Never("Included for some internal purpose, should never be set by users."),
    ;

    private String expl;
    Relevance(String exp) {
      this.expl = exp;
    }
    String getExplanation() {
      return expl;
    }
  }
  /**
   * Simple wrapper class to hold information about a parameter.
   */
  private class ParamInfo implements Comparable<ParamInfo> {
    public String paramName = "";
    public String defaultValue = null;
    public boolean isDeprecated;
    public String comment = "";
    public List<Tag> tags = new ArrayList(5);
    public Set<String> categories = new HashSet<String>(2);
    public Relevance rel = Relevance.Unknown;
    // Sorted list of uses.
    public Set<String> definedIn = new TreeSet();

    @Override
    public int compareTo(ParamInfo other) {
      return paramName.compareTo(other.getParamName());
    }

    String getParamName() {
      return paramName;
    }

    ParamInfo setComment(String comment) {
      this.comment = comment.trim();
      return this;
    }


    ParamInfo setDefaultValue(String val) {
      this.defaultValue = val;
      return this;
    }

    ParamInfo setRelevance(FieldDoc field, String relName) {
      try {
	this.rel = Relevance.valueOf(relName);
      } catch (IllegalArgumentException e) {
	root.printError(field.position(),
			"@ParamRelevance value must be a Relevance");
      }
      return this;
    }

    Relevance getRelevance() {
      return rel;
    }

    ParamInfo addDefinedIn(String cls) {
      definedIn.add(cls);
      return this;
    }

    Set<String> getDefinedIn() {
      return definedIn;
    }

    ParamInfo addTag(Tag tag) {
      tags.add(tag);
      return this;
    }

    ParamInfo addCategory(String cat) {
      categories.add(cat);
      categoryMap.put(cat, this);
      return this;
    }

    Set<String> getCategories() {
      return categories;
    }

  }


  /*
   * Common paramdoc strings
   * @ParamRelevance Required
   * @ParamRelevance Common
   * @ParamRelevance Rare
   * @ParamRelevance BackwardCompatibility



   * @ParamCategory Tuning
   * @ParamCategory Crawler
   * @ParamCategory Poller
   * @ParamCategory

   */

}
