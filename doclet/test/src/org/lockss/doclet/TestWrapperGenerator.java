/*
 * $Id: TestWrapperGenerator.java,v 1.2 2003-07-18 00:37:14 tyronen Exp $
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
import com.sun.tools.javadoc.*;
import junit.framework.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestWrapperGenerator extends LockssTestCase {

  private String tempDirPath, tempDirPath2;

  private String templateName = "template.xml";
  private String sourceName = "TestClass.java";
  private String testName = "expected.java";
  private String prefix = "Wrapper";
  private String expectedOutput;

  private boolean debug = false;

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
      String[] javadocargs = {
         sourceName, "-private",
         "-doclet", "org.lockss.doclet.WrapperGenerator",
         "-template", templateName,
         "-prefix", prefix,
         "-d", tempDirPath};
      Main.execute(javadocargs);
      String output = tempDirPath + prefix + sourceName;
      areFilesIdentical(expectedOutput, output);
    } catch (Exception e) {
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
    pw.println("  <import name=\"org.lockss.util.VariableTimedMap\"/>");
    pw.println("</imports>");
    pw.println("<constructor>");
    pw.println("record_call(#METHODNAME, #LISTPARAMS);");
    pw.println("modify_params(#MODIFYPARAMS);");
    pw.println("try {");
    pw.println("  #RUN;");
    pw.println("} catch (Throwable throwable) {");
    pw.println("  record_throwable(#METHODNAME, throwable);");
    pw.println("}");
    pw.println("</constructor>");
    pw.println("<nonvoid>");
    pw.println("record_call(#METHODNAME, #LISTPARAMS);");
    pw.println("modify_params(#MODIFYPARAMS);");
    pw.println("try {");
    pw.println("  #RUN;");
    pw.println("} catch (Throwable throwable) {");
    pw.println("  record_throwable(#METHODNAME, throwable);");
    pw.println("}");
    pw.println("record_val(#METHODNAME,#RETVAL);");
    pw.println("return custom_#METHODNAME_val(xxvariablexx);");
    pw.println("</nonvoid>");
    pw.println("<void>");
    pw.println("record_call(#METHODNAME, #LISTPARAMS);");
    pw.println("modify_params(#MODIFYPARAMS);");
    pw.println("try {");
    pw.println("  #RUN;");
    pw.println("} catch (Throwable throwable) {");
    pw.println("  record_throwable(#METHODNAME, throwable);");
    pw.println("}");
    pw.println("custom_#METHODNAME_val();");
    pw.println("</void>");
    pw.println("<special class=\"TestClass\" method=\"ret3\">");
    pw.println("    return 32767;");
    pw.println("</special>");
    pw.println("<extra class=\"TestClass\">");
    pw.println("  Map wrappermap = new WeakHashMap();\n");
    pw.println("  private void makeMap() {");
    pw.println("  }");
    pw.println("</extra>");
 /*   pw.println("<tobewrapped>");
    pw.println("<wrapPackage name=\"java.util\"/>");
    pw.println("<wrapClass name=\"java.io.File\"/>");
    pw.println("</tobewrapped>");*/
    pw.println("</template>");
    pw.close();
    fw.close();
  }

  void makeSourceFile() throws IOException {
    FileWriter fw = new FileWriter(tempDirPath + sourceName);
    PrintWriter pw = new PrintWriter(fw);
    pw.println("import java.io.*;");
    pw.println("import java.util.*;");
    pw.println("public class TestClass {");
    pw.println("  public TestClass() {}");
    pw.println("  TestClass(String joe) {}");
    pw.println("  private TestClass(double hoe) {}");
    pw.println("  public static int ret3() {");
    pw.println("    return 3;");
    pw.println("  }");
    pw.println("  private static int ret4() {");
    pw.println("    return 4;");
    pw.println("  }");
    pw.println("  public synchronized float testFloat(float x, int k) {");
    pw.println("    return x;");
    pw.println("  }");
    pw.println("  public void exception() throws IOException {");
    pw.println("    throw new IOException();");
    pw.println("  }");
 /*   pw.println("  public File useFile(Map map) {");
    pw.println("    return new File();");
    pw.println("  }");*/
    pw.println("}");
    pw.close();
    fw.close();
 }

  void makeExpectedOutput() throws IOException {
    expectedOutput = tempDirPath + testName;
    FileWriter fw = new FileWriter(expectedOutput);
    PrintWriter pw = new PrintWriter(fw);
    pw.println("import java.io.*;");
    pw.println("import java.util.*;");
    pw.println("import org.lockss.test.*;");
    pw.println("import org.lockss.util.VariableTimedMap;");
    pw.println("import org.lockss.util.ListUtil;\n");
    pw.println("class " + prefix + "TestClass {\n");
    pw.println("  private TestClass xxinnerTestClassxx;\n");
    pw.println("  Map wrappermap = new WeakHashMap();\n");
    pw.println("  private void makeMap() {");
    pw.println("  }\n");
    pw.println("  public " + prefix + "TestClass() {");
    pw.println("    record_call(" + prefix + "TestClass, ListUtil.list());");
    pw.println("    ");
    pw.println("    try {");
    pw.println("      xxinnerTestClassxx = new TestClass();");
    pw.println("    } catch (Throwable throwable) {");
    pw.println("      record_throwable(" + prefix + "TestClass, throwable);");
    pw.println("    }");
    pw.println("  }\n");
    pw.println("  " + prefix + "TestClass(String joe) {");
    pw.println("    record_call(" + prefix + "TestClass, ListUtil.list(joe));");
    pw.println("    List xxlistxx = modify_params(ListUtil.list(joe));");
    pw.println("    joe = (String)xxlistxx.item(0);");
    pw.println("    try {");
    pw.println("      xxinnerTestClassxx = new TestClass(joe);");
    pw.println("    } catch (Throwable throwable) {");
    pw.println("      record_throwable(" + prefix + "TestClass, throwable);");
    pw.println("    }");
    pw.println("  }\n");
    pw.println("  private " + prefix + "TestClass(double hoe) {");
    pw.println("    record_call(" + prefix + "TestClass, ListUtil.list(hoe));");
    pw.println("    List xxlistxx = modify_params(ListUtil.list(hoe));");
    pw.println("    hoe = ((Double)xxlistxx.item(0)).doubleValue();");
    pw.println("    try {");
    pw.println("      xxinnerTestClassxx = new TestClass(hoe);");
    pw.println("    } catch (Throwable throwable) {");
    pw.println("      record_throwable(" + prefix + "TestClass, throwable);");
    pw.println("    }");
    pw.println("  }\n");
    pw.println("  public static int ret3() {");
    pw.println("    return 32767;");
    pw.println("  }\n");
    pw.println("  public synchronized float testFloat(float x, int k) {");
    pw.println("    float xxvariablexx;");
    pw.println("    record_call(testFloat, ListUtil.list(x, k));");
    pw.println("    List xxlistxx = modify_params(ListUtil.list(x, k));");
    pw.println("    x = ((Float)xxlistxx.item(0)).floatValue();");
    pw.println("    k = ((Integer)xxlistxx.item(1)).intValue();");
    pw.println("    try {");
    pw.println("      xxvariablexx = xxinnerTestClassxx.testFloat(x, k);");
    pw.println("    } catch (Throwable throwable) {");
    pw.println("      record_throwable(testFloat, throwable);");
    pw.println("    }");
    pw.println("    record_val(testFloat,xxvariablexx);");
    pw.println("    return custom_testFloat_val(xxvariablexx);");
    pw.println("  }\n");
    pw.println("  public void exception() throws IOException {");
    pw.println("    record_call(exception, ListUtil.list());");
   // pw.println("    List xxlistxx = modify_params(ListUtil.list(list));");
    pw.println("    ");/* + prefix + "List zzlistzz = (" + prefix);
    pw.println("List)xxlistxx.item(0);");*/
    pw.println("    try {");
    pw.println("      xxinnerTestClassxx.exception();");
    pw.println("    } catch (Throwable throwable) {");
    pw.println("      record_throwable(exception, throwable);");
    pw.println("    }");
    pw.println("    custom_exception_val();");
    pw.println("  }");
 /*   pw.println("  public File useFile(Map map) {");
    pw.println("    File xxvariablexx;");
    pw.println("    " + prefix + "File zzvariablezz;");
    pw.println("    record_call(useFile,ListUtil.list(map));");
    pw.println("    " + prefix + "Map zzmapzz = (" + prefix);
    pw.println("Map)xxlistxx.item(0);");
    pw.println("    try {");
    pw.println("      xxvariablexx = xxinnerTestClassxx.useFile(zzmapzz);");
    pw.println("      zzvariablezz = makeWrapper(xxvariablexx)");
    pw.println("    } catch (Throwable throwable) {");
    pw.println("      record_throwable(exception, throwable);");
    pw.println("    }");
    pw.println("    return custom_useFile_val(zzvariablezz)");
    pw.println("  }");*/
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