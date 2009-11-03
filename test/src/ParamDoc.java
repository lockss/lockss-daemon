/*
 * $Id: ParamDoc.java,v 1.10.66.1 2009-11-03 23:44:55 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.jar.*;
import org.lockss.util.*;

public class ParamDoc {
  static final String blanks = "                                                                                ";
  static final String WDOG_PATTERN =
    org.lockss.daemon.LockssThread.PARAM_NAMED_WDOG_INTERVAL;

  static final String PRIORITY_PATTERN =
    org.lockss.daemon.LockssThread.PARAM_NAMED_THREAD_PRIORITY;

  private static Logger log = Logger.getLogger("ParamDoc");

  static Map paramMap = new TreeMap();
  static Map classMap = new TreeMap();
  static Map paramToSymbol = new HashMap();
  static Map paramSymToDefaultSym = new HashMap();
  static Map wdogSymbolToName = new HashMap();

  static Map defaultMap = new TreeMap();

  static PrintStream pout = System.out;

  public static void main(String argv[]) throws Exception {
    Vector jars = new Vector();
    String ofile = null;
    try {
      for (int ix=0; ix<argv.length; ix++) {
        String arg = argv[ix];
        if (arg.startsWith("-")) {
          if (arg.startsWith("-o")) {
            ofile = argv[++ix];
          } else {
            usage();
          }
        } else {
          jars.add(arg);
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      usage();
    }

    if (ofile != null) {
      FileOutputStream fos = new FileOutputStream(ofile);
      pout = new PrintStream(fos);
    }
    doJars(jars);
  }

  static void doJars(List jars) {
    for (Iterator iter = jars.iterator(); iter.hasNext(); ) {
      String jarname = (String)iter.next();
      File jarfile = find_jar(jarname);
      if (jarfile == null) {
        System.err.println("Couldn't find " + jarname);
      } else {
        JarFile jar;
        try {
          jar = new JarFile(jarfile);
        } catch (IOException e) {
          log.error("Couldn't open jar " + jarfile, e);
          return;
        }
        for (Enumeration en = jar.entries();
             en.hasMoreElements(); ) {
          JarEntry ent = (JarEntry)en.nextElement();
          doClass(ent);
        }
      }
    }
    pout.println("Parameters and default values");
    printDefaults();
//     pout.println("\nParameters by class");
//     printMap(classMap);
    pout.println("\nClasses using parameter");
    printMap(paramMap);
  }

  static void usage() {
    System.err.println("Usage: ParamDoc [-o outfile] <jars ...>");
    System.exit(1);
  }

  static final int COL = 40;

  static void printMap(Map map) {
    for (Iterator keyIter = map.keySet().iterator();
         keyIter.hasNext(); ) {
      String key = (String)keyIter.next();
      List list = (List)map.get(key);
      pout.print(key);
      Collections.sort(list);
      int len = key.length();
      for (Iterator iter = list.iterator(); iter.hasNext(); ) {
        if (len >= COL) {
          pout.println();
          len = 0;
        }
        pout.print(nblanks(COL - len));
        pout.println((String)iter.next());
        len = 0;
      }
    }
  }

  static void printDefaults() {
    for (Iterator paramNameIter = defaultMap.keySet().iterator();
         paramNameIter.hasNext(); ) {
      String paramName = (String)paramNameIter.next();
      Object defaultVal;

      defaultVal = defaultMap.get(paramName);

      pout.print(paramName);
      int len = paramName.length();
      if (len >= COL) {
        pout.println();
        len = 0;
      }
      pout.print(nblanks(COL - len));
      pout.print(defaultVal);
      String timeStr = "";
      if (defaultVal instanceof Long) {
        long val = ((Long)defaultVal).longValue();
        if (val > 0) {
          timeStr = " (" + StringUtil.timeIntervalToString(val) + ")";
        }         
      }
      pout.println(timeStr);
    }
  }

  static String classNameFromEntry(JarEntry ent) {
    String entname = ent.getName();
    if (entname.endsWith(".class") && (-1 == entname.indexOf("$"))) {
      String cname =
        StringUtil.replaceString(entname.substring(0, entname.length() - 6),
                                 File.separator, ".");
      return cname;
    }
    return null;
  }

  static void doClass(JarEntry ent) {
    Class cls;
    String cname = classNameFromEntry(ent);
    if (cname == null) return;
    try {
      cls = Class.forName(cname);
    } catch (ClassNotFoundException e) {
      log.error(cname, e);
      return;
    }
    Field flds[] = cls.getDeclaredFields();

    Map defaultSymToDefVal = new HashMap(); //def symbol to def value
    Map paramToSymbol = new HashMap(); //param symbol to param name

    for (int ix = 0; ix < flds.length; ix++) {
      doField(cls, flds[ix], paramToSymbol);
      doFieldDefault(cls, flds[ix], defaultSymToDefVal);
    }
    for (Iterator it = paramToSymbol.keySet().iterator(); it.hasNext();) {
      String paramName = (String)it.next();
      String paramSym = (String)paramToSymbol.get(paramName);
      String defaultSym = (String)paramSymToDefaultSym.get(paramSym);
      Object defaultVal = defaultSymToDefVal.get(defaultSym);

//       if (defaultVal != null) {
//      putIfNotDifferent(defaultMap, paramName, defaultVal,
//                        "Conflicting defaults");
//       }
      putIfNotDifferent(defaultMap, paramName,
                        defaultVal != null ? defaultVal : "(none)",
                        "Conflicting defaults");
    }
  }

  static void doField(Class cls, Field fld, Map paramToSymbol) {
    String fname = fld.getName();

    if (Modifier.isStatic(fld.getModifiers()) &&
        String.class == fld.getType()) {
      String paramName = null;
      if (fname.startsWith("PARAM_")) {
        paramName = getParamString(cls, fld, fname);
        paramSymToDefaultSym.put(fname, "DEFAULT"+fname.substring(5));
      } else if (fname.startsWith("WDOG_PARAM_")) {
        paramName = getParamString(cls, fld, fname);
        paramSymToDefaultSym.put(fname,
                                 StringUtil.replaceString(fname,
                                                          "_PARAM_",
                                                          "_DEFAULT_"));
        if (paramName != null) {
          paramName =
            StringUtil.replaceString(WDOG_PATTERN, "<name>", paramName);
          wdogSymbolToName.put(fname, paramName);
        }
      } else if (fname.startsWith("PRIORITY_PARAM_")) {
        paramName = getParamString(cls, fld, fname);
        paramSymToDefaultSym.put(fname,
                                 StringUtil.replaceString(fname,
                                                          "_PARAM_",
                                                          "_DEFAULT_"));
        if (paramName != null) {
          paramName =
            StringUtil.replaceString(PRIORITY_PATTERN, "<name>", paramName);
          wdogSymbolToName.put(fname, paramName);
        }
      }
      if (paramName != null) {
        addParam(paramMap, paramName, cls.getName());
        addParam(classMap, cls.getName(), paramName);
        putIfNotDifferent(paramToSymbol, paramName, fname,
                          "Multiple symbols used to define parameter name ");
      }
    }
  }

  static void doFieldDefault(Class enclosingClass, Field fld,
                             Map defaultSymToDefVal) {
    String fname = fld.getName();

    if (Modifier.isStatic(fld.getModifiers())) {
      if (fname.startsWith("DEFAULT_") ||
          fname.startsWith("WDOG_DEFAULT") ||
          fname.startsWith("PRIORITY_DEFAULT")) {
        Object defaultVal;
        try {
          fld.setAccessible(true);
          Class cls = fld.getType();
          if (int.class == cls) {
            defaultVal = new Integer(fld.getInt(null));
          } else if (long.class == cls) {
            defaultVal = new Long(fld.getLong(null));
          } else if (boolean.class == cls) {
            defaultVal = new Boolean(fld.getBoolean(null));
          } else {
            defaultVal = fld.get(null);
          }
        } catch (IllegalAccessException e) {
          log.error(fld.toString(), e);
          return;
        }

        defaultSymToDefVal.put(fname, defaultVal);
      }
    }
  }

  static String getParamString(Class cls, Field fld, String fname) {
    String paramName;
    try {
      fld.setAccessible(true);
      paramName = (String)fld.get(null);
    } catch (IllegalAccessException e) {
      log.error(fld.toString(), e);
      return null;
    }
    if (paramName.indexOf("..") >= 0 && !paramMap.containsKey(paramName)) {
      System.err.println("*** Suspicious parameter name: " + paramName);
    }
    return paramName;
  }

  static void putIfNotDifferent(Map map, Object key, Object val, String msg) {
    Object existingVal = map.get(key);
    if (existingVal != null && !existingVal.equals(val)) {
      System.err.println("*** " +msg+" "+key+" "+val+" "+existingVal);
    } else {
      map.put(key, val);
    }
  }

  static void addParam(Map map, String key, String val) {
    if (!map.containsKey(key)) {
      map.put(key, new ArrayList());
    }
    List list = (List)map.get(key);
    list.add(val);
  }

  static String nblanks(int n) {
    return blanks.substring(0, n);
  }

  public static final String CLASSPATH = System.getProperty("java.class.path");

  public static final File [] CLASSPATH_DIRS;
  static {
    StringTokenizer st = new StringTokenizer(CLASSPATH, File.pathSeparator);
    int count = st.countTokens();
    CLASSPATH_DIRS = new File[count];
    for (int i = 0; i<count; i++) {
      CLASSPATH_DIRS[i] = new File(st.nextToken());
    }
  }

  /**
   * Find a plain file or a directory in default classpath.
   *
   * @see #find_file(File[], String)
   */
  public static File find_file(String name) {
    return find_file(CLASSPATH_DIRS, name);
  }

  public static File find_jar(String name) {
    return find_jar(CLASSPATH_DIRS, name);
  }

  /**
   * Find a plain file or a directory.
   *
   * @param dirs search paths
   * @param name filename (basename with extension) or dirname
   * @return <code>null</code> if not found
   */
  public static File find_file(File[] dirs, String name) {
    for (int i=0; i<dirs.length; i++) {
      File file = new File(dirs[i], name);
      if (file.canRead()) return file;
    }
    return null;
  }
  /**
   * Find a plain file or a directory.
   *
   * @param dirs search paths
   * @param name filename (basename with extension) or dirname
   * @return <code>null</code> if not found
   */
  public static File find_jar(File[] dirs, String name) {
    for (int i=0; i<dirs.length; i++) {
      File file = dirs[i];
      if (file.getName().equals(name)) return file;
    }
    return null;
  }
}
