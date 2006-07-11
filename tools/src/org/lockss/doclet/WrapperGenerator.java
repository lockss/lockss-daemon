/*
 * $Id: WrapperGenerator.java,v 1.5 2006-07-11 17:42:24 thib_gc Exp $
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

import java.io.*;
import java.util.*;
import java.text.*;
import java.lang.reflect.*;
import com.sun.javadoc.*;
import org.apache.oro.text.regex.*;
import org.w3c.dom.*;
import org.lockss.util.*;


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
 *   class CDATA #IMPLIED>
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
 * The <extra> tag contains extra code to be placed either at the end of
 * every class, or at the beginning of the class specified by the
 * <code>class</code> attribute, if present.
 *
 * The <special> tag is a substitute template for a particular method
 * (specified in the "class" and "method" attributes).
 *
 * Macro substitution is not available in the extra and special sections.
 *
 * To invoke, use:
 * <code>javadoc [sourcefile] -private -doclet
 * org.lockss.doclet.WrapperGenerator -template [template-file]</code>
 *
 * </p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

public class WrapperGenerator extends Doclet {
  static Logger log = Logger.getLogger("WrapperGenerator");

  /** Command-line option to specify template file name */
  static final String templateOption = "-template";

  /** Command-line option to specify prefix of generated files */
  static final String prefixOption = "-prefix";

  /** Command-line option to specify output directory */
  static final String directoryOption = "-d";

  /** Command-line option; if specified, do interfaces only  */
  static final String interfaceOption = "-interface";

  /** Prefix, defaults to "Wrapped" */
  static String prefix = "Wrapped";

  /** Output directory, defaults to working directory */
  static String outputDir = "./";

  /** Controls if only interfaces are wrapped, defaults to false */
  static boolean interfacesOnly = false;

  /** Name of variable to be used in generated class to hold delegated
   * original class  */
  static final String returnValue = "returnValue";

  /** Name of variable to be used in generated class to hold wrapped class */
  static final String wrappedVariableName = "wrappedReturnValue";

  /** Variables holding regular expression patterns objects */
  static Pattern classnameRE, fullclassnameRE, methodnameRE, innerobjectRE,
      returnTypeRE, invokeMethodRE, throwRE, listparamsRE, beginLineRE,
      returnValueRE, modifyParamsRE, publicRE, nullRE, wrappedRetvalRE;

  /** Part of the doclet API; called by Javadoc for each command-line option
   * to verify the number of arguments
   */
  public static int optionLength(String option) {
    if (option.equals(templateOption) || option.equals(prefixOption) ||
    option.equals(directoryOption)) {
      return 2;
    } else if (option.equals(interfaceOption)) {
      return 1;
    }
    return 0;
  }

  /** Called by Javadoc to validate command-line option.  This one checks
   * to make sure the -template option is in fact specified. */
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
  static void initializeRE() throws MalformedPatternException {
    Perl5Compiler comp = RegexpUtil.getCompiler();
    classnameRE = comp.compile("#CLASSNAME");
    fullclassnameRE = comp.compile("#FULLCLASSNAME");
    methodnameRE = comp.compile("#METHODNAME");
    innerobjectRE = comp.compile("#INNEROBJECT");
    returnTypeRE = comp.compile("#RETTYPE");
    invokeMethodRE = comp.compile("#RUN");
    listparamsRE = comp.compile("#LISTPARAMS");
    modifyParamsRE = comp.compile("(\\w+)\\(#MODIFYPARAMS\\);");
    throwRE = comp.compile("#THROW (\\w+);");
    beginLineRE = comp.compile("(^)", Perl5Compiler.MULTILINE_MASK);
    returnValueRE = comp.compile("#RETVAL");
    wrappedRetvalRE = comp.compile("#WRAPPED_RETVAL");
    publicRE = comp.compile("public ");
    nullRE = comp.compile("#NULLVAL");
  }

  String substituteAll(Pattern pat, String inString, String withString) {
    Substitution subst = new Perl5Substitution(withString);
    return Util.substitute(RegexpUtil.getMatcher(), pat, subst, inString,
			   Util.SUBSTITUTE_ALL);
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
          } else if (opt[0].equals(interfaceOption)) {
            interfacesOnly = true;
          }
      }
      return template;
  }

  /** Utility function: produces the name of the variable holding the
   * delegated inner class
   */
  static String makeInnerName(String name) {
    return "inner" + name;
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
  Map classExtraMap = new HashMap();

  /** Contains extra code that goes at the end of every generated class */
  List generalExtras = new ArrayList();

  /** Whether to write an extra constructor taking the wrapped class as parameter */
  boolean useWrapperConstructor;

  WrapperGenerator(RootDoc root, XmlDoc template) {
    this.root = root;
    this.template = template;
    loadPackageName();
    loadImportNames();
    loadTemplates();
    loadSpecials();
    loadExtras();
    loadWrappeds();
    loadWrapperConstructor();
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
    String txt = XmlDoc.getText(extra);
    if (classname!=null && !classname.equals("")) {
      classExtraMap.put(classname,txt);
    } else {
      generalExtras.add(txt);
    }
  }

  Set wrappedPackages = new HashSet();
  Set wrappedClasses = new HashSet();
  Set hiddenClasses = new HashSet();

  void loadWrappeds() {
    if (!template.hasTag("toBeWrapped")) {
      return;
    }
    NodeList plist = template.getNodeList("wrapPackage");
    NodeList wclist = template.getNodeList("wrapClass");
    NodeList hclist = template.getNodeList("hideClass");
    XmlDoc.putAttrFromNodeListIntoSet(plist,wrappedPackages,"name");
    XmlDoc.putAttrFromNodeListIntoSet(wclist,wrappedClasses,"name");
    XmlDoc.putAttrFromNodeListIntoSet(hclist,hiddenClasses,"name");
  }

  void loadWrapperConstructor() {
    useWrapperConstructor = template.hasTag("useWrapperConstructor");
  }

  boolean isWrapped(String qrettype) {
    int pos = qrettype.lastIndexOf('.');
    String pkg = (pos>=0) ? qrettype.substring(0,pos) : qrettype;
    return  wrappedClasses.contains(qrettype) ||
            (wrappedPackages.contains(pkg) &&
            !hiddenClasses.contains(qrettype));
  }

  void writeFiles() {
    try {
      ClassDoc[] classes = root.classes();
      for (int i = 0; i < classes.length; i++) {
        ClassDoc cl = classes[i];
        if (cl.isPublic() && (!interfacesOnly || cl.isInterface())){
          writeWrapperClass(classes[i]);
        }
      }
    }
    catch (IOException e) {
      root.printError("Unable to write output classes.");
    }
  }

  String classname;
  String fullclassname;

  /** Name of wrapped inner variable */
  String innerObjectName;

  void writeWrapperClass(ClassDoc cl) throws IOException {
    classname = cl.name();
    fullclassname = cl.qualifiedTypeName();
    innerObjectName = makeInnerName(classname);
    BufferedWriter wr = new BufferedWriter(new FileWriter(new File(
    outputDir, prefix + classname + ".java")));
    writePackageLine(cl, wr);
    writeImportDirectives(cl, wr);
    writeComments(wr);
    writeClassDecl(cl, wr);
   // writeStaticInitializer(wr);
    if (useWrapperConstructor) {
      writeInnerObject(wr);
    }
    writeClassExtra(cl, wr);
    alreadyWrittenConstructors.clear();
    alreadyWrittenMethods.clear();
    writeAllConstructors(cl, wr);
    if (useWrapperConstructor) {
      writeWrapperConstructor(wr);
    }
    writeAllMethods(cl, wr);
    writeGeneralExtras(wr);
    wr.write("\n}\n");
    wr.close();
  }

  /** Write class declaration*/
  void writeClassDecl(ClassDoc cl, Writer wr) throws IOException {
    wr.write("public class ");
    wr.write(prefix);
    wr.write(classname);
    if (!cl.isInterface()) {
      wr.write(" extends ");
      wr.write(classname);
    }
    wr.write(" implements Wrapped");
    if (cl.isInterface()) {
      wr.write(", ");
      wr.write(classname);
    }
    wr.write(" {\n\n");
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
    writeImportLine("org.lockss.util.WrapperLogger", wr);
    writeImportLine("org.lockss.plugin.WrapperState",wr);
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

  void writeComments(Writer wr) throws IOException {
    wr.write("/** *****************************************************\n");
    wr.write(" *                 --   DO NOT EDIT   --                *\n");
    wr.write(" *           THIS CLASS CONSISTS OF GENERATED CODE      *\n");
    wr.write(" *  Edit the WrapperGenerator class or template file    *\n");
    wr.write(" *             if you wish to make changes              *\n");
    wr.write(" ********************************************************/\n\n");
  }

  /** Write a static initializer to register with WrapperState */
  void writeStaticInitializer(Writer wr) throws IOException {
    wr.write("  static {\n");
    wr.write("    WrapperState.register(\"");
    wr.write(fullclassname);
    wr.write("\");\n  }\n\n");
  }

  /** Write the declaration of the instance of the delegated original class */
  void writeInnerObject(Writer wr) throws IOException {
    wr.write("  private ");
    wr.write(classname);
    wr.write(' ');
    wr.write(innerObjectName);
    wr.write(";");
  }

  void writeClassExtra(ClassDoc cl, Writer wr) throws IOException {
    String fullname = cl.qualifiedTypeName();
    if (classExtraMap.containsKey(fullname)) {
        wr.write("\n\n");
        wr.write(StringUtil.trimBlankLines((String)
                                           classExtraMap.get(fullname)));
    }
  }

  void writeAllConstructors(ClassDoc cl, Writer wr) throws IOException {
    ClassDoc cptr = cl;
    do {
      writeConstructors(cptr, wr);
      cptr = cptr.superclass();
    } while (cptr != null && !cptr.qualifiedTypeName().equals(
        "java.lang.Object"));
  }

  Set alreadyWrittenConstructors = new HashSet();

  void writeConstructors(ClassDoc cl, Writer wr) throws IOException {
    ConstructorDoc[] cons = cl.constructors();
    for (int i = 0; i < cons.length; i++) {
      ConstructorDoc con = cons[i];
      String signature = con.flatSignature();
      if (!alreadyWrittenConstructors.contains(signature)) {
        alreadyWrittenConstructors.add(signature);
        SourceConstructor scon = new SourceConstructor(con);
        writeConstructor(scon, wr);
      }
    }
  }

  void writeConstructor(SourceConstructor scon, Writer wr)
  throws IOException {
    String output = substituteRE_method(constructorTemplate, scon);
    writeDecl(scon, wr);
    writeModifiedMethodBody(output, scon, wr);
  }

  /** write constructor that takes original class as argument */
  void writeWrapperConstructor(Writer wr) throws IOException {
    wr.write("\n\n  public ");
    wr.write(prefix);
    wr.write(classname);
    wr.write('(');
    wr.write(classname);
    wr.write(" original) {\n");
    wr.write("    ");
    wr.write(innerObjectName);
    wr.write(" = original;\n");
    wr.write("  }");
  }

  Set alreadyWrittenMethods = new HashSet();

  void writeAllMethods(ClassDoc cl, Writer wr) throws IOException {
    ClassDoc cptr = cl;
    boolean isLeaf = true;
    do {
      writeMethods(cptr.methods(), wr, isLeaf);
      cptr = cptr.superclass();
      isLeaf = false;
    } while (cptr!=null && !cptr.qualifiedTypeName().equals("java.lang.Object"));
    ClassDoc[] interfaces = cl.interfaces();
    for (int i=0; i<interfaces.length; i++) {
      writeAllMethods(interfaces[i],wr);
    }
  }

  boolean isStandardMethod(MethodDoc method) {
    return (
    (method.name().equals("equals") &&
    method.returnType().typeName().equals("boolean")
    && method.parameters().length==1
    && method.parameters()[0].typeName().equals("java.lang.Object"))
    ||
    (method.name().equals("hashCode") &&
    method.returnType().typeName().equals("int")
    && method.parameters().length==0)
    ||
    (method.name().equals("finalize") &&
   method.returnType().typeName().equals("void")
   && method.parameters().length==0)
   );

  }

  void writeMethods(MethodDoc[] methods, Writer wr, boolean isLeaf)
      throws IOException {
    for (int i = 0; i < methods.length; i++) {
      MethodDoc method = methods[i];
      if (!alreadyWrittenMethods.contains(method)) {
        alreadyWrittenMethods.add(method);
        if (method.isPublic() && method.isIncluded() &&
            (isLeaf || !isStandardMethod(method))) {
          SourceMethod srcMethod = new SourceMethod(method);
            if (srcMethod.returnType.equals("void")) {
              writeVoidBody(srcMethod, wr);
            }
            else {
              writeNonvoidBody(srcMethod, wr);
            }
            if (isSpecialMethod(srcMethod)) {
              writeSpecialMethod(srcMethod, wr);
            }
        }
      }
    }
  }

  void writeVoidBody(SourceMethod method, Writer wr)
  throws IOException {
    String output = substituteRE_method(voidTemplate, method);
    output = substituteAll(returnTypeRE, output, method.returnType);
    writeDecl(method, wr);
    writeModifiedMethodBody(output, method, wr);
  }

  void writeNonvoidBody(SourceMethod method, Writer wr)
  throws IOException {
    String output = substituteRE_method(nonvoidTemplate,method);
    output = substituteAll(returnTypeRE, output, method.returnType);
    output = substituteAll(returnValueRE, output, returnValue);
    String substName = (method.returnsWrapped) ? wrappedVariableName :
        returnValue;
    output = substituteAll(wrappedRetvalRE, output, substName);
    output = substituteAll(nullRE, output, method.nullText());
    writeDecl(method, wr);
    writeLocalDecl(method.returnType + method.returnDimension, returnValue, wr);
    if (method.returnsWrapped) {
      writeLocalDecl(prefix + method.returnType, wrappedVariableName, wr);
    }
    writeModifiedMethodBody(output, method, wr);
  }

  /** Writes a declaration of a local variable */
  void writeLocalDecl(String type, String name, Writer wr) throws IOException {
    wr.write("    ");
    wr.write(type);
    wr.write(' ');
    wr.write(name);
    wr.write(";\n");
  }

  String substituteRE_common(String output) {
    output = substituteAll(classnameRE, output, classname);
    output = substituteAll(fullclassnameRE, output,fullclassname);
    output = substituteAll(innerobjectRE, output, innerObjectName);
    return output;
  }

  /** Makes macro substitutions using the GNU RE objects */
  String substituteRE_general(String output) {
    output = substituteAll(beginLineRE, output, "  $1");
    output = substituteRE_common(output);
    return output;
  }

  /** Makes macro substitutions using the GNU RE objects, local to a method */
  String substituteRE_method(String output, SourceExecMember method) {
    output = substituteAll(beginLineRE, output, "    $1");
    output = substituteRE_common(output);
    output = substituteAll(methodnameRE, output, method.methodname);
    output = substituteAll(invokeMethodRE, output, method.runText());
    output = substituteAll(listparamsRE, output, method.paramsAsString());
    output = substituteAll(throwRE, output, method.throwText());
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
  void writeModifiedMethodBody(String output, SourceExecMember method,
                               Writer wr)
  throws IOException {
    String substr;
    if (method.params.length==0) {
      substr = "";
    } else {
      StringBuffer buf = new StringBuffer("List paramsList = $1(");
      buf.append(method.paramsAsString());
      buf.append(");");
      for (int i=0; i<method.params.length; i++) {
        Parameter param = method.params[i];
        buf.append("\n    ");
        buf.append(param.name());
        buf.append(" = (");
        String typename = param.type().typeName();
        if (ClassUtil.isPrimitive(typename)) {
          buf.append('(');
          if (typename.equals("int")) {
            buf.append("Integer");
          } else {
            buf.append(StringUtil.titleCase(typename));
          }
        } else {
          buf.append(typename);
        }
        buf.append(")paramsList.item(");
        buf.append(i);
        buf.append(')');
        if (ClassUtil.isPrimitive(typename)) {
          buf.append(").");
          buf.append(typename);
          buf.append("Value()");
        }
        buf.append(';');
      }
      substr = buf.toString();
    }
    wr.write(substituteAll(modifyParamsRE, output,substr));
    wr.write("\n  }");
  }

  boolean isSpecialMethod(SourceMethod method) {
    if (!specialmap.containsKey(fullclassname)) {
      return false;
    } else {
      Map classmap = (Map)specialmap.get(fullclassname);
      return (classmap.containsKey(method.methodname));
    }
  }


  void writeSpecialMethod(SourceMethod method,Writer wr) throws IOException {
    wr.write("\n\n  ");
    String nopublic = substituteAll(publicRE, method.modifiers,"");
    wr.write(nopublic);
    String origname = method.methodname;
    method.methodname = prefix + '_' + method.methodname;
    wr.write(method.declText());
    method.methodname = origname;
    wr.write(" {\n");
    Map classmap = (Map)specialmap.get(fullclassname);
    String txt = StringUtil.trimBlankLines(
        (String)classmap.get(method.methodname));
    wr.write(txt);
    wr.write("\n  }");
  }

  void writeGeneralExtras(Writer wr) throws IOException {
    Iterator it = generalExtras.iterator();
    while (it.hasNext()) {
      String txt = (String)it.next();
      wr.write("\n");
      wr.write(StringUtil.trimBlankLines(substituteRE_general(txt)));
    }
  }

  /** This inner class represents a single method or constructor, each of
   * which has its own subclass.
   */
  private abstract class SourceExecMember {
    String methodname;
    String modifiers;
    String throwlist;
    Parameter[] params;
    ClassDoc[] exceptions;

    SourceExecMember(ExecutableMemberDoc method) {
      params = method.parameters();
      checkParams();
      modifiers = method.modifiers();
      if (!modifiers.equals("")) {
        modifiers += ' ';
      }
      exceptions = method.thrownExceptions();
      makeThrowList();
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
    void makeThrowList() {
      StringBuffer buf = new StringBuffer("");
      if (exceptions.length > 0) {
        buf.append(" throws ");
        for (int i = 0; i < exceptions.length; i++) {
          buf.append(exceptions[i].typeName());
          buf.append(", ");
        }
        buf.delete(buf.length() - 2, buf.length());
      }
      throwlist = buf.toString();
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
    void writeParamDecl(StringBuffer buf, Parameter param) {
      buf.append(param.type().typeName());
      buf.append(param.type().dimension());
      buf.append(' ');
      buf.append(param.name());
    }

    String makeUnWrappedParamName(String name) {
      return "unwrapped" + name;
    }

    String wrapParams() {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < params.length; i++) {
        Parameter param = params[i];
        String typename = param.type().typeName();
        if (isWrapped(param.typeName())) {
          buf.append(typename);
          buf.append(' ');
          buf.append(makeUnWrappedParamName(param.name()));
          buf.append(" = (");
          buf.append(typename);
          buf.append(")WrapperState.getOriginal(");
          buf.append(param.name());
          buf.append(");\n      ");
        }
      }
      return buf.toString();
    }

    String unwrapNameIfNecessary(Parameter param) {
      if (isWrapped(param.typeName())) {
       return makeUnWrappedParamName(param.name());
     } else {
       return param.name();
     }
    }

    /** Actual invocation of the method */
    String runText() {
      StringBuffer buf = new StringBuffer();
      buf.append('(');
      for (int i = 0; i < params.length - 1; i++) {
        Parameter param = params[i];
        buf.append(unwrapNameIfNecessary(param));
        buf.append(", ");
      }
      if (params.length > 0) {
        buf.append(unwrapNameIfNecessary(params[params.length - 1]));
      }
      buf.append(')');
      return buf.toString();
    }

    void writeParamAsObject(StringBuffer buf, Parameter param) {
      if (ClassUtil.isPrimitive(param.typeName())) {
       buf.append("new ");
       buf.append(ClassUtil.objectTypeName(param.typeName()));
       buf.append('(');
     }
     buf.append(param.name());
     if (ClassUtil.isPrimitive(param.typeName())) {
       buf.append(')');
     }
    }

    /** Writes out the parameters as arguments to the ListUtil.list() method */
    String paramsAsString() {
      StringBuffer buf = new StringBuffer();
      buf.append("ListUtil.list(");
      for (int i = 0; i < params.length - 1; i++) {
        writeParamAsObject(buf,params[i]);
        buf.append(", ");
      }
      if (params.length > 0) {
        writeParamAsObject(buf, params[params.length-1]);
      }
      buf.append(')');
      return buf.toString();
    }

    String throwText() {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println("if ($1 instanceof RuntimeException) {");
      pw.println("        throw (RuntimeException)$1;");
      for (int i=0; i<exceptions.length; i++) {
        pw.print("      } else if ($1 instanceof ");
        pw.print(exceptions[i].name());
        pw.println(") {");
        pw.print("        throw (");
        pw.print(exceptions[i].name());
        pw.println(")$1;");
      }
      pw.println("      } else {");
      pw.println("        throw new RuntimeException($1.getMessage());");
      pw.print("      }");
      return sw.toString();
    }

  }

  /** Refinements needed for methods */
  private class SourceMethod extends SourceExecMember {
    String returnType;
    String qualifiedReturnType;
    String returnDimension;
    boolean isStatic;
    boolean returnsWrapped;

    SourceMethod(MethodDoc method) {
      super(method);
      returnType = method.returnType().typeName();
      returnDimension = method.returnType().dimension();
      qualifiedReturnType = method.returnType().qualifiedTypeName();
      returnsWrapped = isWrapped(qualifiedReturnType);
      isStatic = method.isStatic();
      methodname = method.name();
    }

    String declText() {
      StringBuffer buf = new StringBuffer(returnType);
      buf.append(returnDimension);
      buf.append(' ');
      buf.append(super.declText());
      return buf.toString();
    }

    String runText() {
      StringBuffer buf = new StringBuffer(wrapParams());
      if (!returnType.equals("void")) {
        buf.append(returnValue);
        buf.append(" = ");
      }
      if (isSpecialMethod(this)) {
        buf.append(prefix);
        buf.append('_');
      } else {
        if (!isStatic) {
          buf.append(innerObjectName);
        }
        else {
          buf.append(classname);
        }
        buf.append('.');
      }
      buf.append(methodname);
      buf.append(super.runText());
      if (returnsWrapped) {
        buf.append(";\n      ");
        buf.append(wrappedVariableName);
        buf.append(" = (");
        buf.append(prefix);
        buf.append(returnType);
        buf.append(")WrapperState.getWrapper(");
        buf.append(returnValue);
        buf.append(')');
      }
      return buf.toString();
    }

    String nullText() {
     if (!ClassUtil.isPrimitive(returnType) || !returnDimension.equals("")) {
       return "null";
     } else if (returnType.equals("float") || returnType.equals("double")) {
       return "0.0";
     } else if (returnType.equals("boolean")) {
       return "false";
     } else {
       return "0";
     }
    }

  }

  /** Refinements for constructors */
  private class SourceConstructor extends SourceExecMember {
    SourceConstructor(ConstructorDoc method) {
      super(method);
      methodname = classname;
    }

    String runText() {
      StringBuffer buf = new StringBuffer(wrapParams());
      if (WrapperGenerator.this.useWrapperConstructor) {
        buf.append(innerObjectName);
        buf.append(" = new ");
        buf.append(classname);
        buf.append(super.runText());
      }
      return buf.toString();
    }

    String declText() {
      return prefix + super.declText();
    }


  } // static SourceMethod

}
