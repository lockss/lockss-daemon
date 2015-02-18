/*
 * $Id$
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import org.apache.commons.collections.*;
import org.apache.commons.collections.map.*;
import org.lockss.util.*;

/**
 * Utilities for display, searching, finding duplicates on classpath; see
 * usage() for instructions
 */
public class ClassPathUtil {
  static Logger log = Logger.getLogger("ClassPathUtil");

  private static List m_classpath;

  /** Find a resourse with the given name */
  public static void whichResource(String resourceName, String msg) {
    URL resUrl = findResource(resourceName);

    if (resUrl == null) {
      System.out.println(msg + " not found.");
    } else {
      System.out.println(msg + " found in \n" + resUrl);
    }
  }

  /** Find a resourse with the given name */
  public static void whichResource(String resourceName) {
    resourceName = fixResourceName(resourceName);
    whichResource(resourceName, "Resource " + resourceName);
  }

  /** Find a class with the given name */
  public static void whichClass(String className) {
    String resourceName = asResourceName(className);
    whichResource(resourceName, "Class " + className);
  }

  private static URL findResource(String resourceName) {
    return ClassPathUtil.class.getResource(resourceName);
  }

  // ensure leading /
  private static String fixResourceName(String resourceName) {
    if (!resourceName.startsWith("/")) {
      resourceName = "/" + resourceName;
    }
    return resourceName;
  }

  // convert class name to resource name
  private static String asResourceName(String className) {
    String resource = className;
    resource = resource.replace('.', '/');
    resource = resource + ".class";
    return fixResourceName(resource);
  }

  // build map of resource name -> list of jars that contain it
  private static Map m_map;
  private static void buildMap() {
    m_map = new MultiValueMap();
    for (Iterator iter = getClasspath().iterator(); iter.hasNext(); ) {
      String element = (String)iter.next();
      File file = new File(element);
      processJar(file);
    }
  }

  // add the entries for one jar
  private static void processJar(File file) {
    try {
      JarFile jf = new JarFile(file);
      for (Enumeration en = jf.entries(); en.hasMoreElements(); ) {
	ZipEntry ent = (ZipEntry)en.nextElement();
	if (ent.isDirectory()) {
	  continue;
	}
	m_map.put(ent.getName(), file);
      }
    } catch (IOException e) {
      log.warning("reading jar " + file, e);
    }
  }

  /** Display all the resurces that are found in more than one place on
   * the classpath.  Only works on jars currently */
  public static void showConflicts() {
    buildMap();
    for (Map.Entry ent : (Collection<Map.Entry>)m_map.entrySet()) {
      List jars = (List)ent.getValue();
      if (jars.size() > 1) {
	String key = (String)ent.getKey();
	System.out.println(key + ": " + jars);
      }
    }
  }

  /** Search for all occurrances of a class given name in any
   * package.  Only works on jars currently */
  public static void searchClass(String className) {
    String resource = className + ".class";
    searchResource(resource);
  }

  /** Search for all occurrances of a resurce with the given name in any
   * package.  Only works on jars currently */


  public static void searchResource(String resourceName) {
    buildMap();
    for (Map.Entry ent : (Collection<Map.Entry>)m_map.entrySet()) {
      String key = (String)ent.getKey();
      File resFile = new File(key);
      String name = resFile.getName();
      if (resourceName.equals(name)) {
	List jars = (List)ent.getValue();
	System.out.println(key + ": " + jars);
      }
    }
  }

  /**
   * Validate the class path and report any non-existent
   * or invalid class path entries.
   * <p>
   * Valid class path entries include directories, <code>.zip</code>
   * files, and <code>.jar</code> files.
   */
  public static void validate() {
    for (Iterator iter = getClasspath().iterator(); iter.hasNext(); ) {
      String element = (String)iter.next();
      File f = new File(element);

      if (!f.exists()) {
	System.out.println("Classpath element " + element +
			   " does not exist.");
      } else if ( (!f.isDirectory()) &&
		  (!StringUtil.endsWithIgnoreCase(element, ".jar")) &&
		  (!StringUtil.endsWithIgnoreCase(element, ".zip")) ) {
	System.out.println("Classpath element " + element +
			   "is not a directory, .jar file, or .zip file.");
      }
    }
  }

  public static void printClasspath() {
    System.out.println("Classpath:");
    for (Iterator iter = getClasspath().iterator(); iter.hasNext(); ) {
      System.out.println((String)iter.next());
    }
  }

  public static void setClasspath(String classpath) {
    m_classpath = StringUtil.breakAt(classpath, File.pathSeparator);
  }

  public static void addClasspath(String path) {
    getClasspath().addAll(StringUtil.breakAt(path, File.pathSeparator));
  }

  private static List getClasspath() {
    if (m_classpath == null) {
      setClasspath(System.getProperty("java.class.path"));
    }
    return m_classpath;
  }

  private static void usage() {
    System.out.println("java ClassPathUtil [ actions ... ]");
    System.out.println("  Actions are executed sequentially:");
    System.out.println("   -cp <classpath>      Set classpath");
    System.out.println("   -ap <classpath       Append to classpath");
    System.out.println("   -c  <className>      Locate class on classpath");
    System.out.println("   -r  <resourceName>   Locate resource on classpath");
    System.out.println("   -sc <unqualifiedClassName> Search for all occurrences of class on classpath");
    System.out.println("   -sr <unqualifiedResourceName> Ditto for resource");
    System.out.println("   -p                   Print classpath");
    System.out.println("   -x                   Show duplicate resources on classpath");
    System.out.println("   -v                   Validate classpath (check that all jars exist)");
    System.exit(0);
  }

  public static void main(String args[]) {
    if (args.length == 0) {
      usage();
    }

    try {
      for (int ix = 0; ix < args.length; ix++) {
	String a = args[ix];
	if ("-cp".equals(a) || "-classpath".equals(a)) {
	  setClasspath(args[++ix]);
	  continue;
	}
	if ("-ap".equals(a) || "-addpath".equals(a)) {
	  addClasspath(args[++ix]);
	  continue;
	}
	if ("-r".equals(a)) {
	  whichResource(args[++ix]);
	  continue;
	}
	if ("-c".equals(a)) {
	  whichClass(args[++ix]);
	  continue;
	}
	if ("-sc".equals(a)) {
	  searchClass(args[++ix]);
	  continue;
	}
	if ("-sr".equals(a)) {
	  searchResource(args[++ix]);
	  continue;
	}
	if ("-p".equals(a)) {
	  printClasspath();
	  continue;
	}
	if ("-x".equals(a)) {
	  showConflicts();
	  continue;
	}
	if ("-v".equals(a)) {
	  validate();
	  continue;
	}
	usage();
      }
    } catch (Exception e) {
      usage();
    }
  }
}
