/*
 * $Id: WrapperGenerator.java,v 1.2 2003-07-18 00:37:13 tyronen Exp $
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
 * <p>Title: </p>
 * <p>Description:
 * Takes as input a Java class or interface and the name of a template.  Writes
 * to standard output every public method call in a wrapper based on the
 * template.  The DTD of the template is as follows:
 *
 * <!DOCTYPE template>
 * <!ELEMENT template (package?, imports?, constructor?, nonvoid?, void?
 *                     extra? special*)>
 * <!ELEMENT package (#BLANK)>
 * <!ATTLIST package
 *   name CDATA #REQUIRED>
 * <!ELEMENT imports (class_imports?, import*)>
 * <!ELEMENT class_imports (#BLANK)>
 * <!ELEMENT import (#BLANK)>
 * <!ATTLIST import
 *   name CDATA #REQUIRED>
 * <!ELEMENT constructor (#PCDATA)>
 * <!ELEMENT nonvoid (#PCDATA)>
 * <!ELEMENT void (#PCDATA)>
 * <!ELEMENT extra (#PCDATA)>
 * <!ATTLIST extra
 *   class CDATA #REQUIRED>
 * <!ELEMENT special (#PCDATA)>
 * <!ATTLIST special
 *   class CDATA #REQUIRED
 *   method CDATA #REQUIRED>
 *
 * The name parameter in the package tag indicates the package the generated
 * class is to belong to.  The name #BLANK indicates the anonymous package.
 * This is the default.
 *
 * The imports class contains the names of all classes and/or packages to be
 * imported in the generated class.  The special class_imports tag indicates
 * the same classes imported in the original are to be imported here too.
 *
 * The contents of the <constructor>, <void>, and <nonvoid> tags contain
 * templates used to generate constructors and public methods.  Void-returning
 * methods have a separate template.
 *
 * Each template is a body of Java-appearing code, with macro substitution:
 *
 * #CLASSNAME - name of class or interface
 * #METHODNAME is the name of the method
 * #RETTYPE is the return value type
 * #LISTPARAMS returns all parameters cast to a string and concatenated with
 * separating commas
 * #RUN invokeMethodREs the real, underlying method
 * #RETVAL substitutes the return value returned by #RUN, or null if void.
 *
 * If one of these three tags is omitted, a default template, which simply
 * calls the corresponding method of the original class, is used.
 *
 * Sample template:
 *
 * try {
 *   logger.log("Method #METHODNAME called",#LISTPARAMS);
 *    #RUN;
 *    return #RETVAL;
 * } catch (Throwable e) {
 *    logger.log("Method #METHODNAME threw " + e.getMessage());
 * }
 *
 * Non-static methods will have a local variable called xxinnerxx<classname>
 * representing the original class.
 *
 * The <extra> tag contains extra code to be placed at the beginning of the
 * class specified by the <code>class</code> attribute.
 *
 * The <special> tag is a substitute template for a particular method
 * (specified in the "class" and "method" attributes).
 *
 * Macro substitution is not available in the extra and special sections.
 *
 * To invoke, use:
 * <code>javadoc [sourcefile] -private -doclet org.lockss.doclet.WrapperGenerator
 * -template [template-file]</code>
 *
 * </p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

import java.io.*;
import java.util.*;
import java.text.*;
import java.lang.reflect.*;
import com.sun.javadoc.*;
import gnu.regexp.*;
import org.w3c.dom.*;
import org.lockss.util.*;

public class WrapperGenerator extends Doclet {

  /** Command-line option to specify template file name */
  static final String templateOption = "-template";

  /** Command-line option to specify prefix of generated files */
  static final String prefixOption = "-prefix";

  /** Command-line option to specify output directory */
  static final String directoryOption = "-d";

  /** Prefix, defaults to "Wrapper" */
  static String prefix = "Wrapper";

  /** Output directory, defaults to working directory */
  static String outputDir = "./";

  /** Name of variable to be used in generated class to hold delegated
   * original class
   */
  static final String delegateVariableName = "xxvariablexx";

  /** Variables holding GNU regular expression objects */
  static RE classnameRE, methodnameRE, returnTypeRE, invokeMethodRE,
  listparamsRE, beginLineRE, returnValueRE, modifyParamsRE;

  /** Part of the doclet API; called by Javadoc for each command-line option
   * to verify the number of arguments
   */
  public static int optionLength(String option) {
    if (option.equals(templateOption) || option.equals(prefixOption) ||
    option.equals(directoryOption)) {
      return 2;
    }
    return 0;
  }

  /** Called by Javadoc to validate each command-line option */
  public static boolean validOptions(String[][] options,
    DocErrorReporter reporter) {
    for (int i = 0; i < options.length; i++) {
        String[] opt = options[i];
        if (opt[0].equals(templateOption)) {
          return true;
        }
    }
    reporter.printError("Template file not specified.");
    return false;
  }

  /** Called by Javadoc to run the doclet; this is the 'main' method
   * @param root - Supplied by Javadoc; represents all sources files being
   * documented.
   * @return true if files generated without error
   */
  public static boolean start(RootDoc root) {
    try {
      initializeRE();
      String tempfile = readOptions(root.options());
      XmlDoc template = new XmlDoc(tempfile);
      WrapperGenerator gen = new WrapperGenerator(root,template);
      gen.writeFiles();
    } catch (IOException e) {
      root.printError("Bad template file, or bad output location.");
      return false;
    } catch (Throwable e) {
      root.printError(StringUtil.stackTraceString(e));
      return false;
    }
    return true;
  }

  /** Set up the regular expressions.  Exception should never be thrown. */
  static void initializeRE() throws REException {
    classnameRE = new RE("#CLASSNAME");
    methodnameRE = new RE("#METHODNAME");
    returnTypeRE = new RE("#RETTYPE");
    invokeMethodRE = new RE("#RUN");
    listparamsRE = new RE("#LISTPARAMS");
    modifyParamsRE = new RE("(\\w+)\\(#MODIFYPARAMS\\);");
    beginLineRE = new RE("^(.)",RE.REG_MULTILINE,RESyntax.RE_SYNTAX_PERL5_S);
    returnValueRE = new RE("#RETVAL");
  }

  /** Record the values of the command line options */
  static String readOptions(String[][] options) {
      String template = null;
      for (int i = 0; i < options.length; i++) {
          String[] opt = options[i];
          if (opt[0].equals(templateOption)) {
            template = opt[1];
          } else if (opt[0].equals(prefixOption)) {
            prefix = opt[1];
          } else if (opt[0].equals(directoryOption)) {
            outputDir = opt[1];
          }
      }
      return template;
  }

  /** Utility function: produces the name of the variable holding the
   * delegated inner class
   */
  static String makeInnerName(String name) {
    return "xxinner" + name + "xx";
  }

  /** Set of classes being parsed, provided by Javadoc */
  RootDoc root;

  /** Template file, as parsed XML */
  XmlDoc template;

  /** Package of the generated files */
  String packageName;

  /** Classes or packages to be imported in the generated files */
  List imports = new ArrayList();

  String constructorTemplate;
  String nonvoidTemplate;
  String voidTemplate;

  /** Contains code for all "special-case" methods, that have their own
   * template.  Key = fully qualified classname, value = another hash map
   * where key=method name, value=code for this method
   */
  Map specialmap = new HashMap();

  /** Contains extra code to be appended to the beginning of various classes.
   * Key=fully qualified classname, value=code
   */
  Map extramap = new HashMap();

  WrapperGenerator(RootDoc root, XmlDoc template) {
    this.root = root;
    this.template = template;
    loadPackageName();
    loadImportNames();
    loadTemplates();
    loadSpecials();
    loadExtras();
  }

  void loadPackageName() {
    packageName = template.getAttrText("package","name");
    if (packageName.equals("")) {
      packageName = "#BLANK";
    }
  }

  void loadImportNames() {
    NodeList importlist = template.getNodeList("import");
    for (int i = 0; i < importlist.getLength(); i++) {
      imports.add(XmlDoc.getNodeAttrText(importlist.item(i),"name"));
    }
  }

  void loadTemplates() {
    constructorTemplate = loadTemplate("constructor");
    nonvoidTemplate = loadTemplate("nonvoid");
    voidTemplate = loadTemplate("void");
  }

  String loadTemplate(String tagname) {
    String temp = StringUtil.trimBlankLines(template.getTagText(tagname));
    return (temp.equals("")) ? "  #RUN;" : temp;
  }

  void loadSpecials() {
    NodeList specials = template.getNodeList("special");
    for (int i=0; i<specials.getLength(); i++) {
      loadSpecial(specials.item(i));
    }
  }

  void loadSpecial(Node special) {
    String classname = XmlDoc.getNodeAttrText(special, "class");
    String methodname = XmlDoc.getNodeAttrText(special, "method");
    String methodcode = XmlDoc.getText(special);
    Map classmap;
    if (specialmap.containsKey(classname)) {
      classmap = (Map)specialmap.get(classname);
    } else {
      classmap = new HashMap();
      specialmap.put(classname,classmap);
    }
    classmap.put(methodname,methodcode);
  }

  void loadExtras() {
    // Snide remark: this code is nearly identical to loadSpecials.
    // If this were C++, I'd only have to write the code once!
    NodeList extras = template.getNodeList("extra");
    for (int i = 0; i < extras.getLength(); i++) {
      loadExtra(extras.item(i));
    }
  }

  void loadExtra(Node extra) {
   String classname = XmlDoc.getNodeAttrText(extra,"class");
   extramap.put(classname,XmlDoc.getText(extra));
  }

  void writeFiles() {
    try {
      ClassDoc[] classes = root.classes();
      for (int i = 0; i < classes.length; i++) {
        writeWrapperClass(classes[i]);
      }
    }
    catch (IOException e) {
      root.printError("Unable to write output classes.");
    }
  }

  void writeWrapperClass(ClassDoc cl) throws IOException {
    BufferedWriter wr = new BufferedWriter(new FileWriter(new File(
    outputDir, prefix + cl.name() + ".java")));
    writePackageLine(cl, wr);
    writeImportDirectives(cl, wr);
    String name = cl.name();
    wr.write("class " + prefix + name + " {\n\n");
    writeInnerObject(name, wr);
    writeExtra(cl, wr);
    writeConstructors(cl, wr);
    writeMethods(cl, wr);
    wr.write("\n}\n");
    wr.close();
  }

  /** Write the package declaration.  If #BLANK, omit.  If #CLASS_PACKAGE,
   * use the same package as the original class.
   */
  void writePackageLine(ClassDoc cl, Writer wr) throws IOException {
    if (!packageName.equals("#BLANK")) {
      wr.write("\npackage ");
      if (packageName.equals("#CLASS_PACKAGE")) {
        wr.write(cl.containingPackage().name());
      }
      else {
        wr.write(packageName);
      }
      wr.write(";\n\n");
    }
  }

  /** Writes out the import directives specified.  Also adds the required
   * ListUtil class.  Also imports the original class, if the anonymous
   * package is being used.
   */
  void writeImportDirectives(ClassDoc cl, Writer wr) throws IOException {
    importClassDirectives(cl,wr);
    Iterator it = imports.iterator();
    while (it.hasNext()) {
      String impline = (String) it.next();
      writeImportLine(impline, wr);
    }
    writeImportLine("org.lockss.util.ListUtil", wr);
    if (!packageName.startsWith("#CLASS_PACKAGE") &&
    !packageName.startsWith("#BLANK")) {
      writeImportLine(cl.qualifiedTypeName(), wr);
    }
    wr.write("\n");
  }

  /** Writes import directives of the source file if the <class_imports> tag
   * is present; skips java.lang.*;
   */
  void importClassDirectives(ClassDoc cl, Writer wr) throws IOException {
    if (template.hasTag("class_imports")) {
      PackageDoc[] packs = cl.importedPackages();
      for (int i = 0; i < packs.length; i++) {
        if (!packs[i].name().equals("java.lang")) {
          writeImportLine(packs[i].name() + ".*", wr);
        }
      }
      ClassDoc[] impcls = cl.importedClasses();
      for (int i = 0; i < impcls.length; i++) {
        writeImportLine(impcls[i].qualifiedTypeName(), wr);
      }
    }
  }

  void writeImportLine(String line, Writer wr) throws IOException {
    wr.write("import ");
    wr.write(line);
    wr.write(";\n");
  }

  /** Write the declaration of the instance of the delegated original class */
  void writeInnerObject(String name, Writer wr) throws IOException {
    wr.write("  private ");
    wr.write(name);
    wr.write(' ');
    wr.write(makeInnerName(name));
    wr.write(";");
  }

  void writeExtra(ClassDoc cl, Writer wr) throws IOException {
    String fullname = cl.qualifiedTypeName();
    if (extramap.containsKey(fullname)) {
        wr.write("\n\n");
        wr.write(StringUtil.trimBlankLines((String) extramap.get(fullname)));
    }
  }

  void writeConstructors(ClassDoc cl, Writer wr) throws IOException {
    ConstructorDoc[] cons = cl.constructors();
    for (int i = 0; i < cons.length; i++) {
      writeConstructor(cons[i], wr);
    }
  }

  void writeConstructor(ConstructorDoc con, Writer wr)
  throws IOException {
    SourceConstructor scon = new SourceConstructor(con);
    String output = substituteRE(constructorTemplate, scon);
    writeDecl(scon, wr);
    writeModifiedParms(output, scon, wr);
  }

  void writeMethods(ClassDoc cl, Writer wr) throws IOException {
    MethodDoc[] methods = cl.methods();
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].isPublic()) {
        SourceMethod srcMethod = new SourceMethod(methods[i]);
        if (isSpecialMethod(srcMethod)) {
          writeSpecialMethod(srcMethod, wr);
        } else {
          if (srcMethod.returnType.equals("void")) {
            writeVoidBody(srcMethod, wr);
          }
          else {
            writeNonvoidBody(srcMethod, wr);
          }
        }
      }
    }
  }

  boolean isSpecialMethod(SourceMethod method) {
    if (!specialmap.containsKey(method.fullclassname)) {
      return false;
    } else {
      Map classmap = (Map)specialmap.get(method.fullclassname);
      return (classmap.containsKey(method.methodname));
    }
  }

  void writeSpecialMethod(SourceMethod method,Writer wr) throws IOException {
    writeDecl(method,wr);
    Map classmap = (Map)specialmap.get(method.fullclassname);
    String txt = StringUtil.trimBlankLines(
        (String)classmap.get(method.methodname));
    wr.write(txt);
    wr.write("\n  }");
  }

  void writeVoidBody(SourceMethod method, Writer wr)
  throws IOException {
    String output = substituteRE(voidTemplate, method);
    output = returnTypeRE.substituteAll(output, method.returnType);
    writeDecl(method, wr);
    writeModifiedParms(output, method, wr);
  }

  void writeNonvoidBody(SourceMethod method, Writer wr)
  throws IOException {
    String output = substituteRE(nonvoidTemplate,method);
    output = returnTypeRE.substituteAll(output, method.returnType);
    output = returnValueRE.substituteAll(output, delegateVariableName);
    writeDecl(method, wr);
    wr.write("    ");
    wr.write(method.returnType);
    wr.write(' ');
    wr.write(delegateVariableName);
    wr.write(";\n");
    writeModifiedParms(output, method, wr);
  }

  /** Makes macro substitutions using the GNU RE objects */
  String substituteRE(String output, SourceExecMember method) {
    output = beginLineRE.substituteAll(output, "    $1");
    output = classnameRE.substituteAll(output, method.classname);
    output = methodnameRE.substituteAll(output, method.methodname);
    output = invokeMethodRE.substituteAll(output, method.runText());
    output = listparamsRE.substituteAll(output, method.paramsAsString());
    return output;
  }

  /** Write the declaration of a method */
  void writeDecl(SourceExecMember method, Writer wr) throws IOException {
    wr.write("\n\n  ");
    wr.write(method.modifiers);
    wr.write(method.declText());
    wr.write(" {\n");
  }

  /** Do the substitution for the #MODIFYPARAMS macro */
  void writeModifiedParms(String output, SourceExecMember method, Writer wr)
  throws IOException {
    String substr;
    if (method.params.length==0) {
      substr = "";
    } else {
      StringBuffer buf = new StringBuffer("List xxlistxx = $1(");
      buf.append(method.paramsAsString());
      buf.append(");");
      for (int i=0; i<method.params.length; i++) {
        Parameter param = method.params[i];
        buf.append("\n    ");
        buf.append(param.name());
        buf.append(" = (");
        String typename = param.type().typeName();
        if (LockssUtil.isPrimitive(typename)) {
          buf.append('(');
          if (typename.equals("int")) {
            buf.append("Integer");
          } else {
            buf.append(StringUtil.titleCase(typename));
          }
        } else {
          buf.append(typename);
        }
        buf.append(")xxlistxx.item(");
        buf.append(i);
        buf.append(')');
        if (LockssUtil.isPrimitive(typename)) {
          buf.append(").");
          buf.append(typename);
          buf.append("Value()");
        }
        buf.append(';');
      }
      substr = buf.toString();
    }
    wr.write(modifyParamsRE.substituteAll(output,substr));
    wr.write("\n  }");
  }

  /** This inner class represents a single method or constructor, each of
   * which has its own subclass.
   */
  private static abstract class SourceExecMember {
    String classname;
    String fullclassname;
    String methodname;
    String modifiers;
    String throwlist;
    Parameter[] params;

    SourceExecMember(ExecutableMemberDoc method) {
      classname = method.containingClass().typeName();
      fullclassname = method.containingClass().qualifiedTypeName();
      params = method.parameters();
      checkParams();
      modifiers = method.modifiers();
      if (!modifiers.equals("")) {
        modifiers += ' ';
      }
      throwlist = makeThrowList(method.thrownExceptions());
    }

    /** Check to make sure no array arguments in the parameter list */
    void checkParams() {
     for (int i=0; i<params.length; i++) {
       if (!params[i].type().dimension().equals("")) {
         throw new UnsupportedOperationException(
         "Array arguments not supported.");
       }
     }
    }

    /** Generate the text of the throws clause of a method */
    String makeThrowList(ClassDoc[] exceptions) {
      StringBuffer buf = new StringBuffer("");
      if (exceptions.length > 0) {
        buf.append(" throws ");
        for (int i = 0; i < exceptions.length; i++) {
          buf.append(exceptions[i].typeName());
          buf.append(", ");
        }
        buf.delete(buf.length() - 2, buf.length());
      }
      return buf.toString();
    }

    /** Generate the text of the method declaration */
    String declText() {
      StringBuffer buf = new StringBuffer();
      buf.append(methodname);
      buf.append("(");
      for (int i = 0; i < params.length - 1; i++) {
        writeParamDecl(buf, params[i]);
        buf.append(", ");
      }
      if (params.length > 0) {
        writeParamDecl(buf, params[params.length - 1]);
      }
      buf.append(')');
      buf.append(throwlist);
      return buf.toString();
    }

    /** Utility function to write a single parameter and its type */
    static void writeParamDecl(StringBuffer buf, Parameter param) {
      buf.append(param.type().typeName());
      buf.append(param.type().dimension());
      buf.append(' ');
      buf.append(param.name());
    }

    /** Actual invocation of the method */
    String runText() {
      StringBuffer buf = new StringBuffer();
      buf.append('(');
      for (int i = 0; i < params.length - 1; i++) {
        buf.append(params[i].name());
        buf.append(", ");
      }
      if (params.length > 0) {
        buf.append(params[params.length - 1].name());
      }
      buf.append(')');
      return buf.toString();
    }

    /** Writes out the parameters as arguments to the ListUtil.list() method */
    String paramsAsString() {
      StringBuffer buf = new StringBuffer();
      buf.append("ListUtil.list(");
      for (int i = 0; i < params.length - 1; i++) {
        buf.append(params[i].name());
        buf.append(", ");
      }
      if (params.length > 0) {
        buf.append(params[params.length - 1].name());
      }
      buf.append(')');
      return buf.toString();
    }

  }

  /** Refinements needed for methods */
  private static class SourceMethod extends SourceExecMember {
    String returnType;
    boolean isStatic;

    SourceMethod(MethodDoc method) {
      super(method);
      methodname = method.name();
      returnType = method.returnType().typeName();
      isStatic = method.isStatic();
    }

    String declText() {
      StringBuffer buf = new StringBuffer(returnType);
      buf.append(' ');
      buf.append(super.declText());
      return buf.toString();
    }

    String runText() {
      StringBuffer buf = new StringBuffer();
      if (!returnType.equals("void")) {
        buf.append(delegateVariableName);
        buf.append(" = ");
      }
      if (!isStatic) {
        buf.append(makeInnerName(classname));
      }
      else {
        buf.append(classname);
      }
      buf.append('.');
      buf.append(methodname);
      buf.append(super.runText());
      return buf.toString();
    }
  }

  /** Refinements for constructors */
  private static class SourceConstructor extends SourceExecMember {
    SourceConstructor(ConstructorDoc method) {
      super(method);
      methodname = prefix + method.name();
    }

    String runText() {
      StringBuffer buf = new StringBuffer();
      buf.append(makeInnerName(classname));
      buf.append(" = new ");
      buf.append(classname);
      buf.append(super.runText());
      return buf.toString();
    }


  } // static SourceMethod

}
