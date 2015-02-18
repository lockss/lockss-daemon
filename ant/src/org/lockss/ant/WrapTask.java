/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.ant;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.taskdefs.Javadoc;
import org.xml.sax.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

/**
 * <p>Title: WrapTask.java </p>
 * <p>Description: This class implements an Ant task that runs javadoc
 * with the <code>org.lockss.doclet.WrapperGenerator</code> doclet, but unlike
 * <i>javadoc</i>, only runs the doclet if the timestamps so warrant.
 *
 * Internally, <i>javadoc</i>'s attributes will be set to failonerror=true.
 *
 * Attributes:
 * srcDir - Source directory (defaults to working directory)
 * destDir - Destination directory (defaults to source directory)
 * template - Wrapper template file location.  Required.
 * interfaces - Whether <i>javadoc</i> wraps interfaces only or public classes
 * too.  Defaults to true.
 *
 * Nested tags:
 * classpath - standard Ant <code>&lt;classpath&gt;</code> tag.  It should
 * include both the source classes and the doclet class.  Only one permitted.
 * fileset - standard Ant <code>&lt;filesetgt;</code> tag.  Use these to specify
 * which other files should have their dates compared with the destination
 * files.  Can have more than one.
 *
 * Example invocation:
 * &lt;wrap srcdir=&quot;${tobewrapped.src}&quot;
 *          template=&quot;${plugin.wrapper.template}&quot;
 *          destdir=&quot;${generated.src}&quot;
 *          interface=&quot;true&quot; &gt;
 *    &lt;fileset dir=&quot;${tools.src}/org/lockss/doclet&quot;
 *                includes=&quot;WrapperGenerator.java&quot;/&gt;
 *    &lt;fileset dir=&quot;${ant.src}/org/lockss/ant"
 *                includes=&quot;WrapTask.java&quot; /&gt;
 *    &lt;classpath refid=&quot;tools.run.classpath&quot; /&gt;
 * &lt;/wrap&gt;
 * </p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

public class WrapTask extends Task {

  /* Javadoc is delegated rather than inherited to avoid exposing the wide
     variety of options it has */
  Javadoc javadoc;

  static final String WRAPPER_DOCLET = "org.lockss.doclet.WrapperGenerator";

  private String srcDir, destDir, template, prefix="Wrapped";
  private boolean interfaceOnly = true;
  private Path classpath;
  private List filesets = new ArrayList();
  private List targetFileDates = new ArrayList();

  public WrapTask() {
    super();
    javadoc = new Javadoc();
  }


  /*Ant uses introspection on these methods to set attributes and nested tags*/

  public void setSrcdir(String srcDir) {
    this.srcDir = srcDir;
  }

  public void setDestdir(String destDir) {
    this.destDir = destDir;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setInterface(boolean interfaceOnly) {
    this.interfaceOnly = interfaceOnly;
  }

  public Path createClasspath() {
    javadoc.setProject(getProject());
    classpath = javadoc.createClasspath();
    return classpath;
  }

  public void addFileset(FileSet fileset) {
    filesets.add(fileset);
  }

  /** Main running method called by Ant */
  public void execute() throws BuildException {
    int filecount=0;

    javadoc.setProject(getProject());

    checkJavadocAttributes();
    setJavadocAttributes();

    Set toBeHidden = getClassesFromTag("hideClass");

    loadDatesToCheck();
    log("Directory: " + srcDir,Project.MSG_INFO);
    File directory = new File(srcDir);
    String[] filelist = directory.list();

    FileSet fs = new FileSet();
    fs.setDir(directory);
    fs.setIncludes("*.java");

    // Determine which files in the srcDir should be excluded
    for (int i=0; i<Array.getLength(filelist); i++) {
      if (!filelist[i].endsWith(".java") || toBeHidden.contains(filelist[i])
          || !outOfDate(filelist[i])) {
        fs.setExcludes(filelist[i]);
        logIfVerbose("File " + filelist[i] + " up to date or hidden.");
      } else {
        filecount++;
      }
    }

    // Run javadoc if necessary
    if (filecount>0) {
      javadoc.addFileset(fs);
      javadoc.execute();
    } else {
      log("All files up to date: skipping javadoc.",Project.MSG_INFO);
      }
  }

  // Check for required attributes ("template" is the only mandatory one)
  // and sets defaults if necessary
  private void checkJavadocAttributes() throws BuildException {
    if (template==null) {
      logErrorAndThrow("Template file not specified.");
    }

    if (srcDir==null) {
      srcDir = (new Path(getProject())).toString();
    }

    if (destDir==null) {
      destDir = srcDir;
    }
  }

  // Configure the Javadoc task object
  private void setJavadocAttributes() {
    // Always private (needed for constructors)
    Javadoc.AccessType at = new Javadoc.AccessType();
    at.setValue("private");
    javadoc.setAccess(at);

    // Always fail on error, like a good ant component
    javadoc.setFailonerror(true);

    // show [wrap] in logged messages
    javadoc.setTaskName(this.getTaskName());

    // Specify the doclet and its path.  Javadoc will take care of it if null
    Javadoc.DocletInfo info = javadoc.createDoclet();
    info.setName(WRAPPER_DOCLET);
    info.setPath(classpath);

    // Specify template argument to doclet
    Javadoc.DocletParam templateParam = info.createParam();
    templateParam.setName("-template");
    templateParam.setValue(template);

    // Specify destination argument to doclet
    Javadoc.DocletParam dParam = info.createParam();
    dParam.setName("-d");
    dParam.setValue(destDir);

    // Specify prefix argument to doclet
    Javadoc.DocletParam prefixParam = info.createParam();
    prefixParam.setName("-prefix");
    prefixParam.setValue(prefix);

    // Specify interface argument to doclet
    if (interfaceOnly) {
      Javadoc.DocletParam interfaceParam = info.createParam();
      interfaceParam.setName("-interface");
    }
  }

  /* Load the template file; from it use the wrapClass and hideClass tags
     to determine which candidates are to be wrapped. */
  private Set getClassesFromTag(String tagname) throws BuildException {
    Set classes = new HashSet();
    Document doc = parseXML(template);
    extractFilesFromTag(classes,doc,tagname);
    /*    NodeList packages = doc.getElementsByTagName("wrapPackage");
    if (packages.getLength()>0) {
      throw new BuildException("wrapPackage tag not yet supported.");
      }*/
    return classes;
  }

  // Convenience method to load an XML file into a DOM tree
  Document parseXML(String path) throws BuildException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(new InputSource(new FileReader(path)));
    }
    catch (Exception e) {
      if (e instanceof ParserConfigurationException) {
        logErrorAndThrow("Error configuring XML parser");
      }
      else if (e instanceof SAXException) {
        logErrorAndThrow("Error parsing XML template file" + path);
      }
      else if (e instanceof FileNotFoundException) {
        logErrorAndThrow("Could not find file");
      }
      else if (e instanceof IOException) {
        logErrorAndThrow("Error reading file ");
      }
      throw new BuildException(e);
    }
  }

  // Gets the names of all items of the given tag name
  private void extractFilesFromTag(Collection coll, Document doc, String str) {
    NodeList wrappedClasses = doc.getElementsByTagName(str);
    for (int i=0; i<wrappedClasses.getLength(); i++) {
      Element elem = (Element) wrappedClasses.item(i);
      String fileToWrap = stripPrefix(elem.getAttribute("name")) + ".java";
      logIfVerbose("Wrapping " + fileToWrap + '.');
      coll.add(fileToWrap);
    }
  }

  // Remove the package prefix from a class name
  String stripPrefix(String str) {
    return str.substring(str.lastIndexOf('.')+1);
  }

  /* If the user has supplied filesets, load their timestamps into a list to
     be used later in the date check */
  private void loadDatesToCheck() throws BuildException {
      try {
        File templateFile = new File(template);
        Date templateStamp = new Date(templateFile.lastModified());
        targetFileDates.add(templateStamp);
        Iterator it = filesets.iterator();
        while (it.hasNext()) {
          FileSet fs = (FileSet) it.next();
          DirectoryScanner ds = fs.getDirectoryScanner(getProject());
          File basedir = ds.getBasedir();
          String[] strfiles = ds.getIncludedFiles();
          for (int i = 0; i < strfiles.length; i++) {
            File file = new File(basedir, strfiles[i]);
            if (!file.exists()) {
              logErrorAndThrow("File " + strfiles[i] + " not found.");
              throw new FileNotFoundException(strfiles[i]);
            }
            else {
              logIfVerbose("Comparing to file: " + file.getCanonicalPath());
              targetFileDates.add(new Date(file.lastModified()));
            }
          }
        }
      } catch (FileNotFoundException e) {
        logErrorAndThrow("Template file " + template + " not found.");
        throw new BuildException(e);
      } catch (IOException e) {
        logErrorAndThrow(e.getMessage());
        throw new BuildException(e);
      }
    }

  /* Returns true if the given filename needs a new wrapper generated */
  private boolean outOfDate(String filename) {
    File srcFile = new File(srcDir, filename);
    File destFile = new File(destDir, prefix + filename);
    if (!destFile.exists()) {
      return true;
    }
    Date srcModified = new Date(srcFile.lastModified());
    Date destModified = new Date(destFile.lastModified());
    return srcModified.after(destModified) ||
      moreRecentThanOtherSources(destModified);
  }

  /* Returns true if the given date is more recent than the filesets */
  private boolean moreRecentThanOtherSources(Date lastModified) {
    Iterator it = targetFileDates.iterator();
    while (it.hasNext()) {
      Date date = (Date) it.next();
      if (date.after(lastModified)) {
        return true;
      }
    }
    return false;
  }

  // Convenience method to write to log and throw exception
  void logErrorAndThrow(String str) throws BuildException {
    log(str,Project.MSG_ERR);
    throw new BuildException(str);
  }

  // Convenience method to write verbose-mode log messages
  void logIfVerbose(String str) {
    log(str,Project.MSG_VERBOSE);
  }

  /* For debugging; prints out formatted timestamp of a file */
  void printDate(File file) {
    java.text.DateFormat fmt = java.text.DateFormat.getDateTimeInstance();
    String datestr = fmt.format(new Date(file.lastModified()));
    System.out.println(file.getName() + " " + datestr);
  }


}




