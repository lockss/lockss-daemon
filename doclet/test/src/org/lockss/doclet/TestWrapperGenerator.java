/*
 * $Id: TestWrapperGenerator.java,v 1.4 2003-07-28 23:00:25 tyronen Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * <p>Title: TestWrapperGenerator </p>
 * <p>Description: Test of the WrapperGenerator class.  Set the debug
 * variable to TRUE if doing interactive debugging.  Runs javadoc in a
 * separate process otherwise. </p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

import java.io.*;
import java.util.*;
import com.sun.tools.javadoc.*;
import junit.framework.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestWrapperGenerator extends LockssTestCase {

  private String tempDirPath, tempDirPath2;

  private String templateName = "template.xml";
  private String sourceName = "TestClass.java";
  private String testName = "expected.java";
  private String prefix = "Wrapped";
  private String expectedOutput;

  private final boolean debug = true;
  private final boolean INTERFACE_ONLY = false;

  public void setUp() throws Exception {
    super.setUp();
    if (debug) {
      tempDirPath = "/home/tyronen/lockss-daemon/doclet/";
    } else {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    System.out.println("Using " + tempDirPath);
  }

  public void tearDown() throws Exception {
    super.tearDown();
    if (!debug) {
      File tempDir = new File(tempDirPath);
      tempDir.delete();
    }
  }

  public void testAll() {

    try {
      makeTemplateFile(USE_PACKAGE_NONE,true);
      makeSourceFile();
      makeExpectedOutput();
      String interfaceText = (INTERFACE_ONLY) ? "-interface" : "";
      String[] javadocargs = {
         sourceName, "child" + sourceName,
         "-private",
       //  "-classpath",tempDirPath,
         "-doclet", "org.lockss.doclet.WrapperGenerator",
         "-template", templateName,
         "-prefix", prefix,
         "-d", tempDirPath/*,
         interfaceText*/
         };
      Main.execute(javadocargs);
      String output = tempDirPath + prefix + "child" + sourceName;
      areFilesIdentical(expectedOutput, output);
    } catch (Throwable e) {
      System.err.println(e.getMessage());
      fail(e.getMessage());
    }

  }

  static final int USE_PACKAGE_NONE = 0;
  static final int USE_PACKAGE_BLANK = 1;
  static final int USE_PACKAGE_SPECIFIED = 2;

  private String packageName;

  void makeTemplateFile(int usePackage, boolean useClassImport)
  throws IOException {
    templateName = tempDirPath + templateName;
    FileWriter fw = new FileWriter(templateName);
    PrintWriter pw = new PrintWriter(fw);
    pw.println("<template>");
    switch (usePackage) {
      case USE_PACKAGE_NONE:
        break;
      case USE_PACKAGE_BLANK:
        pw.println("<package name=\"blank\"/>");
      case USE_PACKAGE_SPECIFIED:
        pw.println("<package name=\"" + packageName + "\"/>");
        break;
    }
    pw.println("<imports>");
    if (useClassImport) {
      pw.println("  <class_imports/>");
    }
    pw.println("  <import name=\"org.lockss.test.*\"/>");
    pw.println("  <import name=\"java.io.*\"/>");
    pw.println("</imports>");
    pw.println("<constructor>");
    pw.println("WrapperLogger.record_call(\"#CLASSNAME\", \"#METHODNAME\", #LISTPARAMS);");
    pw.println("try {");
    pw.println("  #RUN;");
    pw.println("} catch (Throwable throwable) {");
    pw.println("  WrapperLogger.record_throwable(\"#CLASSNAME\", \"#METHODNAME\", throwable);");
    pw.println("}");
    pw.println("</constructor>");
    pw.println("<nonvoid>");
    pw.println("WrapperLogger.record_call(\"#CLASSNAME\", \"#METHODNAME\", #LISTPARAMS);");
    pw.println("try {");
    pw.println("  #RUN;");
    pw.println("  WrapperLogger.record_val(\"#CLASSNAME\", \"#METHODNAME\", #RETVAL);");
    pw.println("} catch (Throwable throwable) {");
    pw.println("  WrapperLogger.record_throwable(\"#CLASSNAME\", \"#METHODNAME\", throwable);");
    pw.println("  return #NULLVAL;");
    pw.println("}");
    pw.println("return custom_#METHODNAME_val(#RETVAL);");
    pw.println("</nonvoid>");
    pw.println("<void>");
    pw.println("WrapperLogger.record_call(\"#CLASSNAME\", \"#METHODNAME\", #LISTPARAMS);");
    pw.println("try {");
    pw.println("  #RUN;");
    pw.println("} catch (Throwable throwable) {");
    pw.println("  WrapperLogger.record_throwable(\"#CLASSNAME\", \"#METHODNAME\", throwable);");
    pw.println("}");
    pw.println("custom_#METHODNAME_val();");
    pw.println("</void>");
    pw.println("<special class=\"childTestClass\" method=\"ret3\">");
    pw.println("    return {32767};");
    pw.println("</special>");
    pw.println("<extra class=\"childTestClass\">");
    pw.println("  Map wrappermap = new WeakHashMap();\n");
    pw.println("  private void makeMap() {");
    pw.println("  }");
    pw.println("</extra>");
    pw.println("<toBeWrapped>");
    pw.println("  <wrapPackage name=\"java.util\"/>");
    pw.println("  <wrapClass name=\"java.io.File\"/>");
    pw.println("</toBeWrapped>");
    pw.println("</template>");
    pw.close();
    fw.close();
  }

  void makeSourceFile() throws IOException {
    FileWriter fw = new FileWriter(tempDirPath + sourceName);
    PrintWriter pw = new PrintWriter(fw);
    pw.println("import java.io.*;");
    pw.println("import java.util.*;");
    if (!INTERFACE_ONLY) {
      pw.println("public class TestClass {");
      pw.println("  public TestClass() {}");
      pw.println("  TestClass(String joe) {}");
      pw.println("  private TestClass(double hoe) {}");
      pw.println("  public static int[] ret3() {");
      pw.println("    int[] x = {3,3,3};");
      pw.println("    return x;");
      pw.println("  }");
      pw.println("  private static int ret4() {");
      pw.println("    return 4;");
      pw.println("  }");
      pw.println("  public synchronized float testFloat(float x, int k) {");
      pw.println("    return x;");
      pw.println("  }");
      pw.println("  public void exception(List list) throws IOException {");
      pw.println("    throw new IOException();");
      pw.println("  }");
      pw.println("  public File useFile(Map map, Set set) {");
      pw.println("    return new File();");
      pw.println("  }");
    } else {
      pw.println("public interface TestClass {");
      pw.println("  public void exception(List list) throws IOException;");
      pw.println("  public File useFile(Map map, Set set);");
    }
    pw.println("}");
    pw.close();
    fw.close();
    fw = new FileWriter(tempDirPath + "child" + sourceName);
    pw = new PrintWriter(fw);
    pw.print("public ");
    if (INTERFACE_ONLY) {
      pw.print("interface");
    } else {
      pw.print("class");
    }
    pw.println(" childTestClass extends TestClass {");
    pw.print("  public boolean returnTrue()");
    if (!INTERFACE_ONLY) {
      pw.println(" {\n    return true;\n}");
    } else {
      pw.println(";");
    }
    pw.println("}");
    pw.close();
    fw.close();
 }

  void makeExpectedOutput() throws IOException {
    expectedOutput = tempDirPath + testName;
    FileWriter fw = new FileWriter(expectedOutput);
    PrintWriter pw = new PrintWriter(fw);
    pw.println("import org.lockss.test.*;");
    pw.println("import java.io.*;");
    pw.println("import org.lockss.util.ListUtil;\n");
    pw.print  ("public class " + prefix + "childTestClass ");
    if (INTERFACE_ONLY) {
      pw.print("implements");
    } else {
     pw.print("extends");
    }
    pw.println(" childTestClass {\n");
    pw.println("  private childTestClass innerchildTestClass;\n");
    pw.println("  Map wrappermap = new WeakHashMap();\n");
    pw.println("  private void makeMap() {");
    pw.println("  }\n");
    if (!INTERFACE_ONLY) {
      pw.println("  public " + prefix + "childTestClass() {");
      pw.println("    WrapperLogger.record_call(\"childTestClass\", \"childTestClass\", ListUtil.list());");
      pw.println("    try {");
      pw.println("      innerchildTestClass = new childTestClass();");
      pw.println("    } catch (Throwable throwable) {");
      pw.println("      WrapperLogger.record_throwable(\"childTestClass\", \"childTestClass\", throwable);");
      pw.println("    }");
      pw.println("  }\n");
      pw.println("  " + prefix + "childTestClass(String joe) {");
      pw.println("    WrapperLogger.record_call(\"childTestClass\", \"childTestClass\", ListUtil.list(joe));");
      pw.println("    try {");
      pw.println("      innerchildTestClass = new childTestClass(joe);");
      pw.println("    } catch (Throwable throwable) {");
      pw.println("      WrapperLogger.record_throwable(\"childTestClass\", \"childTestClass\", throwable);");
      pw.println("    }");
      pw.println("  }\n");
      pw.println("  private " + prefix + "childTestClass(double hoe) {");
      pw.println("    WrapperLogger.record_call(\"childTestClass\", \"childTestClass\", ListUtil.list(new Double(hoe)));");
      pw.println("    try {");
      pw.println("      innerchildTestClass = new childTestClass(hoe);");
      pw.println("    } catch (Throwable throwable) {");
      pw.println("      WrapperLogger.record_throwable(\"childTestClass\", \"childTestClass\", throwable);");
      pw.println("    }");
      pw.println("  }\n");
    }
    pw.println("  public " + prefix + "childTestClass(childTestClass original) {");
    pw.println("    innerchildTestClass = original;");
    pw.println("  }\n");
    pw.println("  public boolean returnTrue() {");
    pw.println("    boolean returnValue;");
    pw.println("    WrapperLogger.record_call(\"childTestClass\", \"returnTrue\", ListUtil.list());");
    pw.println("    try {");
    pw.println("      returnValue = innerchildTestClass.returnTrue();");
    pw.println("      WrapperLogger.record_val(\"childTestClass\", \"returnTrue\", returnValue);");
    pw.println("    } catch (Throwable throwable) {");
    pw.println("      WrapperLogger.record_throwable(\"childTestClass\", \"returnTrue\", throwable);");
    pw.println("      return false;");
    pw.println("    }");
    pw.println("    return custom_returnTrue_val(returnValue);");
    pw.println("  }\n");
    if (!INTERFACE_ONLY) {
      pw.println("  public static int[] ret3() {");
      pw.println("    int[] returnValue;");
      pw.println(
          "    WrapperLogger.record_call(\"childTestClass\", \"ret3\", ListUtil.list());");
      pw.println("    try {");
      pw.println("      returnValue = " + prefix + "ret3();");
      pw.println(
          "      WrapperLogger.record_val(\"childTestClass\", \"ret3\", returnValue);");
      pw.println("    } catch (Throwable throwable) {");
      pw.println(
          "      WrapperLogger.record_throwable(\"childTestClass\", \"ret3\", throwable);");
      pw.println("      return null;");
      pw.println("    }");
      pw.println("    return custom_ret3_val(returnValue);");
      pw.println("  }\n");
      pw.println("  static int[] " + prefix + "ret3() {");
      pw.println("    return {32767};");
      pw.println("  }\n");
      pw.println("  public synchronized float testFloat(float x, int k) {");
      pw.println("    float returnValue;");
      pw.println("    WrapperLogger.record_call(\"childTestClass\", \"testFloat\", ListUtil.list(new Float(x), new Integer(k)));");
      pw.println("    try {");
      pw.println(
          "      returnValue = innerchildTestClass.testFloat(x, k);");
      pw.println("      WrapperLogger.record_val(\"childTestClass\", \"testFloat\", returnValue);");
      pw.println("    } catch (Throwable throwable) {");
      pw.println("      WrapperLogger.record_throwable(\"childTestClass\", \"testFloat\", throwable);");
      pw.println("      return 0.0;");
      pw.println("    }");
      pw.println("    return custom_testFloat_val(returnValue);");
      pw.println("  }\n");
    }
    pw.println("  public void exception(List list) throws IOException {");
    pw.println("    WrapperLogger.record_call(\"childTestClass\", \"exception\", ListUtil.list(list));");
    pw.println("    try {");
    pw.println("      "+prefix+"List wrappedlist = ("+prefix+"List)WrapperState.getWrapper(list);");
    pw.println("      innerchildTestClass.exception(wrappedlist);");
    pw.println("    } catch (Throwable throwable) {");
    pw.println("      WrapperLogger.record_throwable(\"childTestClass\", \"exception\", throwable);");
    pw.println("    }");
    pw.println("    custom_exception_val();");
    pw.println("  }\n");
    pw.println("  public File useFile(Map map, Set set) {");
    pw.println("    File returnValue;");
    pw.println("    " + prefix + "File wrappedReturnValue;");
    pw.println("    WrapperLogger.record_call(\"childTestClass\", \"useFile\", ListUtil.list(map, set));");
    pw.println("    try {");
    pw.println("      "+prefix+"Map wrappedmap = ("+prefix+"Map)WrapperState.getWrapper(map);");
    pw.println("      "+prefix+"Set wrappedset = ("+prefix+"Set)WrapperState.getWrapper(set);");
    pw.println("      returnValue = innerchildTestClass.useFile(wrappedmap, wrappedset);");
    pw.print  ("      wrappedReturnValue = (" + prefix + "File)");
    pw.println("WrapperState.getWrapper(returnValue);");
    pw.println("      WrapperLogger.record_val(\"childTestClass\", \"useFile\", wrappedReturnValue);");
    pw.println("    } catch (Throwable throwable) {");
    pw.println("      WrapperLogger.record_throwable(\"childTestClass\", \"useFile\", throwable);");
    pw.println("      return null;");
    pw.println("    }");
    pw.println("    return custom_useFile_val(wrappedReturnValue);");
    pw.println("  }");
    pw.println("}");
    pw.close();
    fw.close();
  }

  void areFilesIdentical(String path1, String path2) throws IOException {
    String str1 = StringUtil.fromFile(path1);
    String str2 = StringUtil.fromFile(path2);
    assertEquals(str1,str2);
  }


}